package com.koscribe.prototype

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Arrays

/** Debug-only panel inside the already expanded accessibility bubble. No new window is created. */
internal class OverlayRecordingPanel(
    private val context: Context,
    private val host: LinearLayout,
    private val mode: OutputMode,
    private val onFailure: (String) -> Unit,
    private val onReady: () -> Unit,
    private val onStopEntry: () -> Unit,
    private val onReview: () -> Unit,
    private val onCancel: () -> Unit,
    private val sessionTimeoutMs: Long = 8_000L
) {
    private enum class Stage { PREPARING, RECORDING, REVIEW, CLOSED }

    private val main = Handler(Looper.getMainLooper())
    private var stage = Stage.PREPARING
    private var recorder: AudioRecord? = null
    private var callback: AudioManager.AudioRecordingCallback? = null
    @Volatile private var reading = false
    private var readThread: Thread? = null
    private var positiveRead = false
    private var unsilenced = false
    private var ready = false
    private val capturedWidth = host.width
    private val capturedHeight = host.height
    private val capturedToken = host.windowToken
    private val capturedWindowId = ownWindowId()
    private lateinit var card: LinearLayout
    private lateinit var status: TextView
    private val attachedListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit
        override fun onViewDetachedFromWindow(view: View) {
            if (stage != Stage.CLOSED) fail("OVERLAY_HOST_DETACHED")
        }
    }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        if (stage != Stage.CLOSED && !hostStable()) fail("OVERLAY_HOST_GEOMETRY_CHANGED")
    }
    private val proofDeadline = Runnable {
        if (stage == Stage.PREPARING && !ready) fail("OVERLAY_MIC_PROOF_UNAVAILABLE")
    }
    private val sessionDeadline = Runnable {
        if (stage != Stage.CLOSED) fail("OVERLAY_SESSION_DEADLINE")
    }

    fun showAndStart(): Boolean {
        if (!hostStable()) return false
        card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                setColor(0xEE263449.toInt())
                cornerRadius = dp(12).toFloat()
            }
            isClickable = true
        }
        card.addView(label("KoScribe · sample only", true))
        card.addView(label("Selected mode: ${mode.label}", true))
        status = label("Preparing microphone…", false)
        card.addView(status)
        card.addView(button("Cancel") { cancel() })
        val interiorHeight = (capturedHeight - host.paddingTop - host.paddingBottom).coerceAtLeast(1)
        val viewport = ScrollView(context).apply {
            isFillViewport = true
            addView(card)
        }
        return try {
            host.addOnAttachStateChangeListener(attachedListener)
            host.addOnLayoutChangeListener(layoutListener)
            host.removeAllViews()
            host.addView(viewport, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, interiorHeight
            ))
            main.postDelayed(sessionDeadline, sessionTimeoutMs)
            host.post {
                if (stage != Stage.CLOSED) {
                    if (hostStable()) beginRecording() else fail("OVERLAY_HOST_CHANGED_BEFORE_MIC")
                }
            }
            true
        } catch (_: Exception) {
            close()
            false
        }
    }

    private fun ownWindowId(): Int? = runCatching {
        host.createAccessibilityNodeInfo().windowId.takeIf { it >= 0 }
    }.getOrNull()

    /** Same attached view, token, window ID, and measured outer dimensions. */
    fun hostStable(): Boolean = host.isAttachedToWindow && capturedToken != null &&
        capturedWidth > 0 && capturedHeight > 0 && host.windowToken == capturedToken &&
        capturedWindowId != null && ownWindowId() == capturedWindowId &&
        host.width == capturedWidth && host.height == capturedHeight

    private fun beginRecording() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail("OVERLAY_MIC_PERMISSION_MISSING")
            return
        }
        val minimum = AudioRecord.getMinBufferSize(
            16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minimum <= 0) {
            fail("OVERLAY_MIC_FORMAT_UNAVAILABLE")
            return
        }
        val created = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(16_000)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(maxOf(minimum, 4096)).build()
        } catch (_: SecurityException) { null }
          catch (_: Exception) { null }
        if (created == null || created.state != AudioRecord.STATE_INITIALIZED) {
            created?.release()
            fail("OVERLAY_MIC_CREATE_FAILED")
            return
        }
        recorder = created
        val ownCallback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                if (stage == Stage.CLOSED || recorder !== created) return
                checkOwnConfiguration(created)
            }
        }
        callback = ownCallback
        try {
            created.registerAudioRecordingCallback(context.mainExecutor, ownCallback)
            created.startRecording()
        } catch (_: SecurityException) {
            fail("OVERLAY_MIC_START_DENIED")
            return
        } catch (_: Exception) {
            fail("OVERLAY_MIC_START_FAILED")
            return
        }
        if (created.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            fail("OVERLAY_MIC_NOT_RECORDING")
            return
        }
        reading = true
        main.postDelayed(proofDeadline, 2_000)
        checkOwnConfiguration(created)
        readThread = Thread({
            val frame = ByteArray(maxOf(minimum, 4096))
            try {
                var emptyReads = 0
                while (reading) {
                    val count = created.read(frame, 0, frame.size)
                    Arrays.fill(frame, 0)
                    if (!reading) break
                    if (count < 0) {
                        main.post { if (recorder === created) fail("OVERLAY_MIC_READ_FAILED") }
                        break
                    }
                    if (count == 0) {
                        emptyReads++
                        if (emptyReads > 100) {
                            main.post { if (recorder === created) fail("OVERLAY_MIC_EMPTY_READS") }
                            break
                        }
                        Thread.sleep(20)
                    } else {
                        emptyReads = 0
                        main.post {
                            if (recorder === created && stage == Stage.PREPARING) {
                                positiveRead = true
                                maybeReady(created)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                if (reading) main.post { if (recorder === created) fail("OVERLAY_MIC_READ_EXCEPTION") }
            } finally {
                Arrays.fill(frame, 0)
            }
        }, "KoScribeOverlayDiscardAudio").also { it.start() }
    }

    /** The callback argument may describe other apps; inspect only our own recorder. */
    private fun checkOwnConfiguration(own: AudioRecord) {
        if (stage == Stage.CLOSED || recorder !== own) return
        val configuration = runCatching { own.activeRecordingConfiguration }.getOrNull()
        if (configuration?.isClientSilenced == true) {
            fail("OVERLAY_MIC_SILENCED")
            return
        }
        if (ready && configuration == null) {
            fail("OVERLAY_MIC_CONFIG_LOST")
            return
        }
        unsilenced = configuration?.isClientSilenced == false
        maybeReady(own)
    }

    private fun maybeReady(own: AudioRecord) {
        if (stage != Stage.PREPARING || !positiveRead || !unsilenced ||
            own.recordingState != AudioRecord.RECORDSTATE_RECORDING) return
        ready = true
        stage = Stage.RECORDING
        main.removeCallbacks(proofDeadline)
        status.text = "Recording ${mode.label}. Audio is discarded; Stop shows a fixed sample."
        card.addView(button("Stop") { stopAndReview() })
        onReady()
    }

    fun pollMic() {
        if (stage == Stage.CLOSED || stage == Stage.REVIEW) return
        val own = recorder ?: return fail("OVERLAY_MIC_LOST")
        if (own.recordingState != AudioRecord.RECORDSTATE_RECORDING)
            return fail("OVERLAY_MIC_NOT_RECORDING")
        checkOwnConfiguration(own)
    }

    private fun stopAndReview() {
        if (stage != Stage.RECORDING) return
        onStopEntry()
        val own = recorder ?: return fail("OVERLAY_MIC_LOST")
        val config = runCatching { own.activeRecordingConfiguration }.getOrNull()
        if (!positiveRead || own.recordingState != AudioRecord.RECORDSTATE_RECORDING ||
            config?.isClientSilenced != false) {
            fail("OVERLAY_MIC_STOP_PROOF_FAILED")
            return
        }
        if (!releaseMic()) {
            fail("OVERLAY_MIC_RELEASE_FAILED")
            return
        }
        stage = Stage.REVIEW
        card.removeAllViews()
        card.addView(label("Selected mode: ${mode.label}", true))
        card.addView(label("Fixed English sample · no recognition or insertion", true))
        card.addView(label(OutputMode.SAMPLE_ENGLISH, false))
        card.addView(button("Cancel") { cancel() })
        onReview()
    }

    private fun cancel() {
        if (stage == Stage.CLOSED) return
        close()
        onCancel()
    }

    private fun fail(code: String) {
        if (stage == Stage.CLOSED) return
        close()
        onFailure(code)
    }

    fun close() {
        if (stage == Stage.CLOSED && recorder == null) return
        stage = Stage.CLOSED
        main.removeCallbacks(proofDeadline)
        main.removeCallbacks(sessionDeadline)
        releaseMic() // Host stays attached until the service ends this captured session.
        host.removeOnAttachStateChangeListener(attachedListener)
        host.removeOnLayoutChangeListener(layoutListener)
    }

    fun hasPendingCleanup(): Boolean = recorder != null


    /** A thrown stop/release is an uncertain outcome, never a successful Stop proof. */
    private fun releaseMic(): Boolean {
        reading = false
        val old = recorder
        val oldCallback = callback
        callback = null
        var clean = true
        if (old != null) {
            if (oldCallback != null && runCatching {
                    old.unregisterAudioRecordingCallback(oldCallback)
                }.isFailure) clean = false
            if (runCatching {
                if (old.recordingState == AudioRecord.RECORDSTATE_RECORDING) old.stop()
            }.isFailure) clean = false
            if (runCatching { old.release() }.isFailure) clean = false
            else recorder = null
        }
        readThread?.interrupt()
        readThread = null
        return clean
    }

    private fun label(value: String, heading: Boolean): TextView = TextView(context).apply {
        text = value
        textSize = if (heading) 17f else 15f
        setTextColor(Color.WHITE)
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun button(value: String, action: () -> Unit): Button = Button(context).apply {
        text = value
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
