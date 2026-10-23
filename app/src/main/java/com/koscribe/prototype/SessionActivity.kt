package com.koscribe.prototype

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Arrays

/** Visible microphone owner. Audio frames are discarded and never written to disk. */
class SessionActivity : Activity() {
    companion object {
        const val EXTRA_ID = "session_id"
        const val EXTRA_MODE = "mode"
        private const val MIC_REQUEST = 40
        private const val MAX_RECORDING_MS = 60_000L
        @Volatile var isVisible = false
            private set
    }

    private enum class Stage { WAITING, RECORDING, REVIEW, FINISHED }
    private var stage = Stage.WAITING
    private var sessionId = ""
    private var mode = OutputMode.ENGLISH
    private var recorder: AudioRecord? = null
    @Volatile private var reading = false
    private var readThread: Thread? = null
    private var handedToService = false
    private val timeout = Runnable { if (stage == Stage.RECORDING) stopAndReview() }
    private lateinit var content: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = intent.getStringExtra(EXTRA_ID) ?: ""
        mode = OutputMode.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_MODE) }
            ?: OutputMode.ENGLISH
        if (savedInstanceState != null || KoScribeService.active()?.isSessionActive(sessionId) != true) {
            finish()
            return
        }
        content = column()
        content.addView(caption("KoScribe · Phase 1", true))
        content.addView(caption("Selected mode: ${mode.label}", true))
        status = caption("Preparing microphone…")
        content.addView(status)
        content.addView(action("Cancel") { cancel() })
        setContentView(insetScroll(content))
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginRecording()
        } else {
            status.text = "Microphone permission is required for this test."
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MIC_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != MIC_REQUEST) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) beginRecording()
        else status.text = "Microphone denied. Cancel and allow it in Settings to try again."
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    private fun beginRecording() {
        if (stage != Stage.WAITING) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status.text = "Microphone permission is no longer granted. Cancel this session."
            return
        }
        val minimum = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) {
            status.text = "Microphone format unavailable. Cancel this session."
            return
        }
        val created = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(16_000).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(maxOf(minimum, 4096))
                .build()
        } catch (_: SecurityException) { null }
          catch (_: Exception) { null }
        if (created == null || created.state != AudioRecord.STATE_INITIALIZED) {
            created?.release()
            status.text = "Microphone unavailable or busy. Cancel this session."
            return
        }
        try { created.startRecording() } catch (_: Exception) {
            created.release()
            status.text = "Could not start the microphone. Cancel this session."
            return
        }
        recorder = created
        reading = true
        stage = Stage.RECORDING
        status.text = "Recording ${mode.label}. Audio is discarded; Stop loads a fixed sample."
        content.addView(action("Stop") { stopAndReview() })
        content.postDelayed(timeout, MAX_RECORDING_MS)
        readThread = Thread({
            val frame = ByteArray(maxOf(minimum, 4096))
            try {
                var emptyReads = 0
                while (reading) {
                    val count = created.read(frame, 0, frame.size)
                    if (count < 0) {
                        runOnUiThread { microphoneFailed() }
                        break
                    }
                    if (count == 0) {
                        emptyReads++
                        if (emptyReads > 100) {
                            runOnUiThread { microphoneFailed() }
                            break
                        }
                        Thread.sleep(20)
                    } else emptyReads = 0
                    Arrays.fill(frame, 0)
                }
            } catch (_: Exception) {
                if (reading) runOnUiThread { microphoneFailed() }
            } finally {
                Arrays.fill(frame, 0)
            }
        }, "KoScribeDiscardAudio").also { it.start() }
    }

    private fun releaseMic() {
        if (::content.isInitialized) content.removeCallbacks(timeout)
        reading = false
        val old = recorder
        recorder = null
        if (old != null) {
            try { if (old.recordingState == AudioRecord.RECORDSTATE_RECORDING) old.stop() } catch (_: Exception) { }
            try { old.release() } catch (_: Exception) { }
        }
        readThread?.interrupt()
        readThread = null
    }

    private fun microphoneFailed() {
        if (stage != Stage.RECORDING) return
        releaseMic()
        stage = Stage.FINISHED
        KoScribeService.active()?.cancelSession(sessionId)
        status.text = "Microphone stopped unexpectedly. Cancel and try again."
    }

    private fun stopAndReview() {
        if (stage != Stage.RECORDING) return
        releaseMic()
        stage = Stage.REVIEW
        KoScribeService.active()?.reviewStarted(sessionId)
        content.removeAllViews()
        content.addView(caption("${mode.label} · fixed sample review", true))
        content.addView(caption("No speech recognition ran. Edit this English sample. Korean insertion is available only while the sample stays unchanged."))
        val english = EditText(this).apply {
            setText(OutputMode.SAMPLE_ENGLISH)
            minLines = 3
            isSaveEnabled = false
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        content.addView(english, LinearLayout.LayoutParams(-1, dp(130)))
        content.addView(action("Edit English") { english.requestFocus() })
        val feedback = caption("")
        content.addView(feedback)
        val label = if (mode == OutputMode.ENGLISH) "Insert English sample" else "Translate & insert sample"
        content.addView(action(label) {
            if (stage != Stage.REVIEW) return@action
            val confirmed = english.text.toString()
            if (confirmed.isBlank()) {
                feedback.text = "Enter text before insertion."
                return@action
            }
            if (mode != OutputMode.ENGLISH && confirmed != OutputMode.SAMPLE_ENGLISH) {
                feedback.text = "Phase 1 cannot translate edits. Restore the exact sample or use EN mode."
                return@action
            }
            val output = if (mode == OutputMode.ENGLISH) confirmed else mode.sampleOutput
            val service = KoScribeService.active()
            if (service?.isSessionActive(sessionId) != true) {
                feedback.text = "Session ended. Start again from the bubble."
                return@action
            }
            handedToService = true
            stage = Stage.FINISHED
            service.requestInsert(sessionId, output)
            finish()
        })
        content.addView(action("Cancel") { cancel() })
    }

    private fun cancel() {
        releaseMic()
        stage = Stage.FINISHED
        KoScribeService.active()?.cancelSession(sessionId)
        finish()
    }

    override fun onStop() {
        isVisible = false
        if (!handedToService && (stage == Stage.RECORDING || stage == Stage.REVIEW)) cancel()
        super.onStop()
    }

    override fun onPause() {
        if (!handedToService && stage == Stage.RECORDING) cancel()
        super.onPause()
    }

    override fun onDestroy() {
        releaseMic()
        if (!handedToService && stage != Stage.FINISHED) KoScribeService.active()?.cancelSession(sessionId)
        super.onDestroy()
    }
}
