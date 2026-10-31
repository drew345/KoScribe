package com.koscribe.prototype

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Arrays
import java.util.Locale

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
    private val sessionTimeoutMs: Long = 8_000L,
    private val speechMode: Boolean = false,
    private val onSpeechAudio: ((ByteArray) -> Unit)? = null
) {
    private enum class Stage { PREPARING, RECORDING, PROCESSING, REVIEW, CLOSED }

    private val main = Handler(Looper.getMainLooper())
    private var stage = Stage.PREPARING
    private var recorder: AudioRecord? = null
    private var callback: AudioManager.AudioRecordingCallback? = null
    @Volatile private var reading = false
    private var readThread: Thread? = null
    private val speechBuffer = if (speechMode) SpeechAudioBuffer() else null
    @Volatile private var speechReadFailed = false
    private var positiveRead = false
    private var unsilenced = false
    private var ready = false
    private val capturedWidth = host.width
    private val capturedHeight = host.height
    private val capturedToken = host.windowToken
    private val capturedWindowId = ownWindowId()
    private lateinit var card: LinearLayout
    private lateinit var status: TextView
    private var recordingBanner: LinearLayout? = null
    private var recordingTimer: TextView? = null
    private var recordingStartedAtMs = 0L
    private val recordingPulseAnimators = mutableListOf<ObjectAnimator>()
    private var speechActions: LinearLayout? = null
    private var stopAction: Button? = null
    private val ink = 0xFF203B36.toInt()
    private val muted = 0xFF536A5F.toInt()
    private val accent = 0xFF2E7569.toInt()
    private val recordingTick = object : Runnable {
        override fun run() {
            if (stage != Stage.RECORDING) return
            val elapsedSeconds = ((SystemClock.elapsedRealtime() - recordingStartedAtMs)
                .coerceAtLeast(0L) / 1_000L)
            val minutes = elapsedSeconds / 60L
            val seconds = elapsedSeconds % 60L
            recordingTimer?.apply {
                text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                contentDescription = "Elapsed recording time: $minutes minutes, $seconds seconds"
            }
            main.postDelayed(this, 1_000L)
        }
    }
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
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
            isClickable = true
        }
        val heading = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label("KOSCRIBE · RECORDING", true).apply {
                textSize = 11f
                setTextColor(accent)
                letterSpacing = 0.07f
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(label(modeDisplay(), true).apply {
                textSize = 12f
                setTextColor(accent)
                setPadding(dp(8), dp(5), dp(8), dp(5))
                background = rounded(0xFFE9F4ED.toInt(), 8)
            })
        }
        card.addView(heading)
        status = label("Preparing microphone…", false)
        card.addView(status)
        val interiorHeight = (capturedHeight - host.paddingTop - host.paddingBottom).coerceAtLeast(1)
        val viewport = ScrollView(context).apply {
            isFillViewport = false
            addView(card)
        }
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 16)
            addView(viewport, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            val actions = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10), dp(3), dp(10), dp(7))
            }
            actions.addView(button("Cancel", false) { cancel() },
                LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(4) })
            speechActions = actions
            addView(actions, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(60)
            ))
        }
        return try {
            host.addOnAttachStateChangeListener(attachedListener)
            host.addOnLayoutChangeListener(layoutListener)
            host.removeAllViews()
            host.addView(panel, LinearLayout.LayoutParams(
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
                    if (count > 0 && reading && speechBuffer?.append(frame, count) == false) {
                        speechReadFailed = true
                        Arrays.fill(frame, 0)
                        main.post { if (recorder === created) fail("SPEECH_AUDIO_LIMIT") }
                        break
                    }
                    Arrays.fill(frame, 0)
                    if (!reading) break
                    if (count < 0) {
                        if (speechMode) speechReadFailed = true
                        main.post { if (recorder === created) fail("OVERLAY_MIC_READ_FAILED") }
                        break
                    }
                    if (count == 0) {
                        emptyReads++
                        if (emptyReads > 100) {
                            if (speechMode) speechReadFailed = true
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
                if (reading && speechMode) speechReadFailed = true
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
        showRecordingBanner()
        status.visibility = View.GONE
        val stop = button("Stop", true) { stopAndReview() }.apply {
            contentDescription = "Stop recording"
        }
        val actions = speechActions ?: return fail("SPEECH_ACTION_ROW_LOST")
        stopAction = stop
        actions.addView(stop, 0, LinearLayout.LayoutParams(0, dp(48), 1.25f).apply {
            marginEnd = dp(4)
        })
        onReady()
    }

    fun pollMic() {
        if (stage == Stage.CLOSED || stage == Stage.REVIEW || stage == Stage.PROCESSING) return
        val own = recorder ?: return fail("OVERLAY_MIC_LOST")
        if (own.recordingState != AudioRecord.RECORDSTATE_RECORDING)
            return fail("OVERLAY_MIC_NOT_RECORDING")
        checkOwnConfiguration(own)
    }

    private fun stopAndReview() {
        if (stage != Stage.RECORDING) return
        stopAction?.isEnabled = false
        onStopEntry()
        val own = recorder ?: return fail("OVERLAY_MIC_LOST")
        val config = runCatching { own.activeRecordingConfiguration }.getOrNull()
        if (!positiveRead || own.recordingState != AudioRecord.RECORDSTATE_RECORDING ||
            config?.isClientSilenced != false) {
            fail("OVERLAY_MIC_STOP_PROOF_FAILED")
            return
        }
        val reader = readThread
        stage = if (speechMode) Stage.PROCESSING else Stage.REVIEW
        hideRecordingBanner()
        if (!releaseMic()) {
            fail("OVERLAY_MIC_RELEASE_FAILED")
            return
        }
        if (speechMode) {
            main.removeCallbacks(sessionDeadline)
            stopAction?.visibility = View.GONE
            status.visibility = View.VISIBLE
            status.text = "Stopping microphone…"
            Thread({
                try { reader?.join(1_500) } catch (_: InterruptedException) { }
                if (reader == null || reader.isAlive) {
                    main.post { fail("SPEECH_READER_NOT_STOPPED") }
                    return@Thread
                }
                val wav = if (!speechReadFailed) speechBuffer?.finishWav() else null
                main.post {
                    if (stage != Stage.PROCESSING || wav == null) {
                        wav?.fill(0)
                        if (stage == Stage.PROCESSING) fail("SPEECH_AUDIO_EMPTY")
                    } else {
                        status.text = "Transcribing English… Cancel to discard."
                        onSpeechAudio?.invoke(wav) ?: run { wav.fill(0); fail("SPEECH_HANDLER_MISSING") }
                    }
                }
            }, "KoScribeSpeechReaderJoin").start()
            return
        }
        card.removeAllViews()
        stopAction?.visibility = View.GONE
        card.addView(label("ENGLISH REVIEW · ${modeDisplay()}", true).apply {
            textSize = 12f
            setTextColor(accent)
        })
        card.addView(label("Fixed English sample · no recognition or insertion", false))
        card.addView(label(OutputMode.SAMPLE_ENGLISH, false))
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
        if (stage == Stage.CLOSED && recorder == null && readThread?.isAlive != true) return
        stage = Stage.CLOSED
        main.removeCallbacks(proofDeadline)
        main.removeCallbacks(sessionDeadline)
        hideRecordingBanner()
        releaseMic() // Host stays attached until the service ends this captured session.
        speechBuffer?.clear()
        host.removeOnAttachStateChangeListener(attachedListener)
        host.removeOnLayoutChangeListener(layoutListener)
    }

    fun hasPendingCleanup(): Boolean = recorder != null || (speechMode && readThread?.isAlive == true)


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
        if (!speechMode || readThread?.isAlive != true) readThread = null
        return clean
    }

    private fun label(value: String, heading: Boolean): TextView = TextView(context).apply {
        text = value
        textSize = if (heading) 16f else 14f
        setTextColor(if (heading) ink else muted)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun showRecordingBanner() {
        val title = label("Speak naturally.", true).apply {
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            textSize = 18f
            contentDescription = "Recording ${modeDisplay()}. Microphone is active."
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val timer = label("00:00", true).apply {
            setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            textSize = 15f
            contentDescription = "Elapsed recording time: 0 minutes, 0 seconds"
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(timer)
        }
        val pulse = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(0xFFE9F4ED.toInt(), 11)
            val ring = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFF7E6E5.toInt())
                    setStroke(dp(1), 0xFFD7AAA7.toInt())
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                addView(View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0xFFB5444E.toInt())
                    }
                }, FrameLayout.LayoutParams(dp(9), dp(9), Gravity.CENTER))
            }
            addView(ring, LinearLayout.LayoutParams(dp(27), dp(27)))
            // A slow decorative pulse signals activity; it is not a voice level.
            if (ValueAnimator.areAnimatorsEnabled()) {
                recordingPulseAnimators += ObjectAnimator.ofFloat(ring, View.SCALE_X, 0.88f, 1.12f).apply {
                    duration = 1_200L
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
                recordingPulseAnimators += ObjectAnimator.ofFloat(ring, View.SCALE_Y, 0.88f, 1.12f).apply {
                    duration = 1_200L
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
            }
            addView(label("Recording is active", false).apply {
                textSize = 13f
                setTextColor(ink)
                setPadding(dp(10), 0, 0, 0)
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            })
        }
        val banner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
            addView(header)
            addView(pulse, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(8) })
        }
        recordingBanner = banner
        recordingTimer = timer
        card.addView(banner, 1)
        recordingStartedAtMs = SystemClock.elapsedRealtime()
        main.post(recordingTick)
    }

    private fun hideRecordingBanner() {
        main.removeCallbacks(recordingTick)
        recordingPulseAnimators.forEach { it.cancel() }
        recordingPulseAnimators.clear()
        recordingBanner?.visibility = View.GONE
        recordingBanner = null
        recordingTimer = null
    }

    private fun modeDisplay(): String = when (mode) {
        OutputMode.POLITE -> "요 · Polite"
        OutputMode.CASUAL -> "반말 · Informal"
        OutputMode.ENGLISH -> "EN · English"
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun button(value: String, primary: Boolean, action: () -> Unit): Button = Button(context).apply {
        text = value
        isAllCaps = false
        textSize = 14f
        setTextColor(if (primary) Color.WHITE else muted)
        background = rounded(if (primary) accent else 0xFFF7FAF8.toInt(), 10).apply {
            if (!primary) setStroke(dp(1), 0xFFDCE6DF.toInt())
        }
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
