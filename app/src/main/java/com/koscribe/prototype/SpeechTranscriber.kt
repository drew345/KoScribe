package com.koscribe.prototype

import android.content.Context
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.InterruptedIOException
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Fixed-endpoint, one-attempt transcription. The caller owns result/session validity. */
internal class SpeechTranscriber(private val context: Context) {
    sealed class Result {
        data class Text(val value: String) : Result()
        data class Error(val code: Code) : Result()
    }
    enum class Code { KEY_UNAVAILABLE, AUTH, RATE_LIMIT, AUDIO_TOO_LARGE, SERVER, NETWORK, TIMEOUT,
        RESPONSE_TOO_LARGE, INVALID_RESPONSE, EMPTY_TRANSCRIPT, TRANSCRIPT_TOO_LONG }

    companion object {
        private const val ENDPOINT = "https://api.openai.com/v1/audio/transcriptions"
        private const val MAX_RESPONSE = 65_536
        private const val MAX_TRANSCRIPT = 512
        private const val DEADLINE_MS = 45_000L
        private val client by lazy { newClient() }

        internal fun newClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(DEADLINE_MS, TimeUnit.MILLISECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()

        internal fun classifyHttp(status: Int): Code = when (status) {
            401, 403 -> Code.AUTH
            413 -> Code.AUDIO_TOO_LARGE
            429 -> Code.RATE_LIMIT
            else -> Code.SERVER
        }

        internal fun parseResponse(bytes: ByteArray, length: Int = bytes.size): Result {
            val value = try {
                val raw = JSONObject(String(bytes, 0, length, StandardCharsets.UTF_8)).opt("text")
                if (raw !is String) return Result.Error(Code.INVALID_RESPONSE)
                raw
            }
                catch (_: Exception) { return Result.Error(Code.INVALID_RESPONSE) }
            if (value.isBlank()) return Result.Error(Code.EMPTY_TRANSCRIPT)
            if (value.length > MAX_TRANSCRIPT) return Result.Error(Code.TRANSCRIPT_TOO_LONG)
            return Result.Text(value)
        }

        /** Entire multipart entity is one-shot, preventing status-driven replay. */
        internal fun multipart(wav: ByteArray, live: () -> Boolean): RequestBody {
            val boundary = "KoScribe${UUID.randomUUID().toString().replace("-", "")}" // not secret
            val prefix = ("--$boundary\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\ngpt-transcribe\r\n" +
                "--$boundary\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\njson\r\n" +
                "--$boundary\r\nContent-Disposition: form-data; name=\"languages[]\"\r\n\r\nen\r\n" +
                "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n" +
                "Content-Type: audio/wav\r\n\r\n").toByteArray(StandardCharsets.UTF_8)
            val suffix = "\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8)
            return object : RequestBody() {
                override fun contentType(): MediaType = "multipart/form-data; boundary=$boundary".toMediaType()
                override fun contentLength(): Long = (prefix.size + wav.size + suffix.size).toLong()
                override fun isOneShot(): Boolean = true
                override fun writeTo(sink: BufferedSink) {
                    if (!live()) throw InterruptedIOException()
                    sink.write(prefix)
                    var offset = 0
                    while (offset < wav.size) {
                        if (!live()) throw InterruptedIOException()
                        val count = minOf(8192, wav.size - offset)
                        sink.write(wav, offset, count)
                        offset += count
                    }
                    if (!live()) throw InterruptedIOException()
                    sink.write(suffix)
                }
            }
        }
    }

    inner class Job(private val wav: ByteArray, private val callback: (Result) -> Unit) {
        private val outcome = SpeechJobOutcome()
        @Volatile private var call: Call? = null
        @Volatile private var worker: Thread? = null
        @Volatile private var deadline: Thread? = null

        private fun live() = outcome.isActive()

        fun start() {
            deadline = Thread({
                try { Thread.sleep(DEADLINE_MS) } catch (_: InterruptedException) { return@Thread }
                if (outcome.timeOut()) {
                    worker?.interrupt()
                    Arrays.fill(wav, 0)
                    callback(Result.Error(Code.TIMEOUT))
                    call?.cancel() // deadline thread, never UI
                }
            }, "KoScribeTranscriptionDeadline").also { it.isDaemon = true; it.start() }
            worker = Thread({ run() }, "KoScribeTranscription").also { it.start() }
        }

        fun cancel() {
            if (outcome.cancel()) {
                worker?.interrupt()
                deadline?.interrupt()
                Arrays.fill(wav, 0)
                val activeCall = call
                if (activeCall != null) Thread({ activeCall.cancel() }, "KoScribeTranscriptionCancel")
                    .also { it.isDaemon = true; it.start() }
            }
        }

        private fun run() {
            val result = try {
                if (!live()) Result.Error(Code.NETWORK)
                else {
                    val key = ApiKeyStore(context).readKey()
                    if (key.isNullOrBlank()) Result.Error(Code.KEY_UNAVAILABLE)
                    else if (!live()) Result.Error(Code.NETWORK)
                    else request(key)
                }
            } catch (_: InterruptedIOException) {
                Result.Error(Code.TIMEOUT)
            } catch (_: Exception) {
                Result.Error(Code.NETWORK)
            }
            Arrays.fill(wav, 0)
            if (outcome.deliver()) {
                deadline?.interrupt()
                callback(result)
            }
        }

        private fun request(key: String): Result {
            if (!live()) return Result.Error(Code.NETWORK)
            val request = Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Bearer $key")
                .header("Accept", "application/json")
                .post(multipart(wav, ::live))
                .build()
            val activeCall = client.newCall(request)
            call = activeCall
            if (!live()) { activeCall.cancel(); return Result.Error(Code.NETWORK) }
            activeCall.execute().use { response ->
                if (!live()) return Result.Error(Code.NETWORK)
                if (response.code != 200) return Result.Error(classifyHttp(response.code))
                val responseBody = response.body ?: return Result.Error(Code.INVALID_RESPONSE)
                val bytes = ByteArray(MAX_RESPONSE)
                try {
                    var size = 0
                    responseBody.byteStream().use { input ->
                        while (true) {
                            if (!live()) return Result.Error(Code.NETWORK)
                            if (size == MAX_RESPONSE) {
                                if (input.read() >= 0) return Result.Error(Code.RESPONSE_TOO_LARGE)
                                break
                            }
                            val count = input.read(bytes, size, minOf(4096, MAX_RESPONSE - size))
                            if (count < 0) break
                            size += count
                        }
                    }
                    return parseResponse(bytes, size)
                } finally {
                    Arrays.fill(bytes, 0)
                }
            }
        }
    }

    fun newJob(wav: ByteArray, callback: (Result) -> Unit): Job = Job(wav, callback)
}
