package com.koscribe.prototype

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class SpeechTranscriberPolicyTest {
    @Test fun httpFailuresExposeOnlyFixedErrorCategories() {
        assertEquals(SpeechTranscriber.Code.AUTH, SpeechTranscriber.classifyHttp(401))
        assertEquals(SpeechTranscriber.Code.AUTH, SpeechTranscriber.classifyHttp(403))
        assertEquals(SpeechTranscriber.Code.AUDIO_TOO_LARGE, SpeechTranscriber.classifyHttp(413))
        assertEquals(SpeechTranscriber.Code.RATE_LIMIT, SpeechTranscriber.classifyHttp(429))
        assertEquals(SpeechTranscriber.Code.SERVER, SpeechTranscriber.classifyHttp(500))
        assertEquals(SpeechTranscriber.Code.SERVER, SpeechTranscriber.classifyHttp(302))
    }

    @Test fun responseParserAcceptsEnglishUnicodeAndRejectsUnsafeShapes() {
        fun parse(json: String) = SpeechTranscriber.parseResponse(json.toByteArray(Charsets.UTF_8))
        assertEquals(SpeechTranscriber.Result.Text("Hello, 민지."),
            parse("{\"text\":\"Hello, 민지.\"}"))
        assertEquals(SpeechTranscriber.Result.Error(SpeechTranscriber.Code.INVALID_RESPONSE),
            parse("{\"text\":17}"))
        assertEquals(SpeechTranscriber.Result.Error(SpeechTranscriber.Code.INVALID_RESPONSE),
            parse("{\"other\":\"hello\"}"))
        assertEquals(SpeechTranscriber.Result.Error(SpeechTranscriber.Code.INVALID_RESPONSE),
            parse("not JSON"))
        assertEquals(SpeechTranscriber.Result.Error(SpeechTranscriber.Code.EMPTY_TRANSCRIPT),
            parse("{\"text\":\"  \"}"))
        assertEquals(SpeechTranscriber.Result.Error(SpeechTranscriber.Code.TRANSCRIPT_TOO_LONG),
            parse("{\"text\":\"${"x".repeat(513)}\"}"))
    }

    @Test fun multipartIsOneShotAndContainsOnlyAudioAndFixedSpeechFields() {
        val body = SpeechTranscriber.multipart(byteArrayOf(1, 2, 3, 4)) { true }
        assertTrue(body.isOneShot())
        val sink = Buffer()
        body.writeTo(sink)
        val sent = sink.readUtf8()
        assertTrue(sent.contains("name=\"model\"\r\n\r\ngpt-transcribe"))
        assertTrue(sent.contains("name=\"languages[]\"\r\n\r\nen"))
        assertTrue(sent.contains("name=\"file\"; filename=\"recording.wav\""))
        assertTrue(sent.contains("Content-Type: audio/wav"))
        assertFalse(sent.contains("destination"))
        assertFalse(sent.contains("editor"))
    }

    @Test fun redirectAndRetryableStatusNeverReplayAudio() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", "/elsewhere"))
            server.enqueue(MockResponse().setResponseCode(200).setBody("should not reach"))
            val client = SpeechTranscriber.newClient()
            assertFalse(client.followRedirects)
            assertFalse(client.followSslRedirects)
            assertFalse(client.retryOnConnectionFailure)
            assertEquals(45_000, client.callTimeoutMillis)
            assertEquals(20_000, client.writeTimeoutMillis)
            val redirect = Request.Builder().url(server.url("/transcribe"))
                .post(SpeechTranscriber.multipart(byteArrayOf(1, 2)) { true }).build()
            client.newCall(redirect).execute().use { assertEquals(302, it.code) }
            assertEquals(1, server.requestCount)
        }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0"))
            server.enqueue(MockResponse().setResponseCode(200).setBody("should not reach"))
            val retryable = Request.Builder().url(server.url("/transcribe"))
                .post(SpeechTranscriber.multipart(byteArrayOf(1, 2)) { true }).build()
            SpeechTranscriber.newClient().newCall(retryable).execute().use { assertEquals(503, it.code) }
            assertEquals(1, server.requestCount)
        }
    }
}
