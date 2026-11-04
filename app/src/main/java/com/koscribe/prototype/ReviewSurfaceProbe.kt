package com.koscribe.prototype

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Build-wide boundary: diagnostic builds cannot invoke legacy effects, even via stale callbacks. */
internal object ReviewSurfaceProbeMode {
    val enabled: Boolean get() = BuildConfig.DEBUG && BuildConfig.REVIEW_SURFACE_PROBE != "OFF"
    private val blocks = AtomicInteger()
    fun block(): Boolean = enabled.also { if (it) blocks.incrementAndGet() }
    val blocked: Int get() = blocks.get()
}

/** An observation experiment, deliberately independent of all destination/write state machines. */
internal class ReviewSurfaceProbe(
    private val service: KoScribeService,
    private val capture: () -> Target?,
    private val input: () -> Input,
    private val usable: () -> Boolean,
    private val finished: () -> Unit
) {
    data class Target(val snapshot: EditorSnapshot, val node: AccessibilityNodeInfo,
        val root: AccessibilityNodeInfo, val generation: Int)
    data class Input(val role: String, val generation: Int, val restarting: Boolean)
    companion object {
        const val FIXTURE = "com.koscribe.smoke"
        const val EXTRA = "review_surface_probe_id"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var policy: ReviewSurfaceProbePolicy? = null
    private var target: Target? = null
    private var id: String? = null
    private var owner: Any? = null
    private var rootView: View? = null
    private var editView: EditText? = null
    private var closeUi: (() -> Unit)? = null
    private var hostId: Int? = null
    private var resolveHost: (() -> Int?)? = null
    private var launched = false
    private var hostObservedAt: Long? = null
    private var opened = 0L
    private var returnStarted = 0L
    private var returnSample = 0
    private var samples = 0
    private var lastSample = -1L
    private var reviewSamples = 0
    private var reviewRoots = 0
    private var reviewNodes = 0
    private var inputStarts = 0
    private var inputFinishes = 0
    private var ownStarts = 0
    private var targetStarts = 0
    private var restartingStarts = 0
    private var inventorySampled = false
    private var firstNull = "UNKNOWN"
    private var targetCode = "window=UNKNOWN root=UNKNOWN node=UNKNOWN ownFocused=false"
    private var comparison = ReviewSurfaceProbePolicy.Comparison()
    private var returnInput = "NONE"
    private var outcome = "NOT_STARTED"
    private var returnMismatchSeen = false
    private var lateReturnRead = false
    @Volatile private var published = ""
    private var cleanupFailed = false
    val active: Boolean get() = policy?.active == true

    fun diagnostics(): String = published.replace(Regex("blocked=\\d+"),
        "blocked=${ReviewSurfaceProbeMode.blocked}").ifEmpty {
        "Review surface probe: arm=${BuildConfig.REVIEW_SURFACE_PROBE} state=IDLE samples=0 edits=0 blocked=${ReviewSurfaceProbeMode.blocked}\n" +
            "Probe outcome: NOT_STARTED\n"
    }

    fun start(captured: Target?, started: Long, host: () -> Int?) {
        if (!ReviewSurfaceProbeMode.enabled || active || cleanupFailed) return
        policy = ReviewSurfaceProbePolicy(started)
        opened = started
        id = UUID.randomUUID().toString()
        owner = null
        samples = 0; reviewSamples = 0; reviewRoots = 0; reviewNodes = 0
        inputStarts = 0; inputFinishes = 0; ownStarts = 0; targetStarts = 0; restartingStarts = 0
        lastSample = -1; returnSample = 0; inventorySampled = false; firstNull = "UNKNOWN"
        comparison = ReviewSurfaceProbePolicy.Comparison(); returnInput = "NONE"
        returnMismatchSeen = false; lateReturnRead = false; hostId = null; resolveHost = host
        launched = false; hostObservedAt = null
        targetCode = "window=UNKNOWN root=UNKNOWN node=UNKNOWN ownFocused=false"
        outcome = "OPENING"
        if (!usable() || input().role != "TARGET") {
            terminate("BASELINE_NOT_OWNED", true); return
        }
        target = captured // The service discarded the baseline connection before collapsing its host.
        if (target == null) { terminate("BASELINE_UNAVAILABLE", true); return }
        publish()
        handler.post(pulse)
    }

    private fun launch() {
        if (!active || launched) return
        launched = true
        val session = id ?: return
        try {
            if (BuildConfig.REVIEW_SURFACE_PROBE == "ACTIVITY") {
                service.startActivity(Intent(service, ReviewActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(EXTRA, session)
                })
            } else openOverlay(session)
        } catch (_: Exception) { terminate("OPEN_FAILED", true); return }
    }

    fun bindActivity(activity: Activity, session: String?): Boolean {
        if (!active || session != id || owner != null ||
            BuildConfig.REVIEW_SURFACE_PROBE != "ACTIVITY") return false
        owner = activity
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        activity.window.setBackgroundDrawableResource(android.R.color.transparent)
        val content = content(activity, session!!)
        closeUi = { activity.finish() }
        activity.setContentView(content)
        activity.window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT)
        focusEditor(session)
        return true
    }

    private fun openOverlay(session: String) {
        val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val content = content(service, session)
        owner = content
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        }
        closeUi = { if (content.isAttachedToWindow) wm.removeViewImmediate(content) }
        wm.addView(content, params)
        focusEditor(session)
    }

    private fun content(context: Context, session: String): LinearLayout {
        fun dp(n: Int) = (n * context.resources.displayMetrics.density).toInt()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F8F6.toInt()) // Entire touchable surface is visibly occupied.
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        root.addView(TextView(context).apply {
            text = "English review comparison · no insertion"
            textSize = 18f
            setTextColor(Color.BLACK)
        })
        root.addView(Button(context).apply {
            text = "Dismiss probe"; contentDescription = "Dismiss probe"; isAllCaps = false
            setOnClickListener {
                if (cleanupFailed && session == this@ReviewSurfaceProbe.id) cleanup() else dismiss(session)
            }
        }, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(TextView(context).apply {
            text = "Append ‘ test’ using SwiftKey, then tap Dismiss probe. This comparison cannot insert, copy, record, or translate. It closes after two minutes."
            setTextColor(Color.DKGRAY)
        })
        val editor = EditText(context).apply {
            contentDescription = "Review surface probe English"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
            filters = arrayOf(InputFilter.LengthFilter(512))
            gravity = Gravity.TOP
            minLines = 3; maxLines = 6
            setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE)
            setText(OutputMode.SAMPLE_ENGLISH)
            setSelection(text.length)
        }
        root.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))
        rootView = root; editView = editor
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (id == session && active) {
                    policy?.edited(SystemClock.uptimeMillis())
                    if (!active) completeTimed() else publish()
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            // Use actual overlap: adjust-resize may already have removed the keyboard area.
            val screen = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .currentWindowMetrics.bounds
            val xy = IntArray(2).also(view::getLocationOnScreen)
            val bottom = (xy[1] + view.height - (screen.bottom - maxOf(bars.bottom, ime.bottom)))
                .coerceAtLeast(0)
            view.setPadding(dp(16) + bars.left, dp(16) + (bars.top - xy[1]).coerceAtLeast(0),
                dp(16) + bars.right, dp(16) + bottom)
            insets
        }
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (id == session && active) onSignal()
        }
        return root
    }

    private fun focusEditor(session: String) {
        val initialEditor = editView ?: return
        var requested = false
        fun showWhenFocused() {
            if (id == session && active) {
                if (!requested && initialEditor === editView && initialEditor.isAttachedToWindow &&
                    initialEditor.hasWindowFocus() && initialEditor.isFocused) {
                    requested = true
                    (initialEditor.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(initialEditor, InputMethodManager.SHOW_IMPLICIT)
                }
                onSignal()
            }
        }
        initialEditor.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) showWhenFocused()
        }
        initialEditor.requestFocus()
        initialEditor.post { showWhenFocused() }
    }

    fun activityLost(activity: Activity) {
        if (owner === activity && active && policy?.state != ReviewSurfaceProbePolicy.State.RETURNING)
            terminate("ACTIVITY_INTERRUPTED")
    }

    fun onInput(started: Boolean, role: String, restarting: Boolean = false) {
        if (!active) return
        if (started) {
            inputStarts++
            if (role == "OWN") ownStarts++
            if (role == "TARGET") targetStarts++
            if (restarting) restartingStarts++
        } else inputFinishes++
        if (started && role == "OTHER") { terminate("FOREIGN_INPUT"); return }
        val session = id
        handler.post { if (id == session && active) onSignal() }
    }

    fun onSignal() {
        if (!active) return
        if (!usable() || input().role == "OTHER") { terminate("INTERRUPTED"); return }
        val now = SystemClock.uptimeMillis()
        if (!policy!!.tick(now)) { completeTimed(); return }
        if (!launched) { publish(); return }
        val focused = editView?.let { it.isAttachedToWindow && it.isFocused && it.hasWindowFocus() } == true &&
            input().role == "OWN"
        if (focused) policy?.focused(now)
        if (policy?.state != ReviewSurfaceProbePolicy.State.RETURNING &&
            samples < 64 && (lastSample < 0 || now - lastSample >= 100)) observeTarget(focused)
        publish()
    }

    private val pulse = object : Runnable {
        override fun run() {
            if (!active) return
            if (!usable() || input().role == "OTHER") { terminate("INTERRUPTED"); return }
            val now = SystemClock.uptimeMillis()
            if (!policy!!.tick(now)) { completeTimed(); return }
            if (!launched) {
                val served = input()
                if (served.role != "TARGET" || served.generation != target?.generation) {
                    terminate("PREPARATION_INPUT_CHANGED", true); return
                }
                val currentHost = runCatching { resolveHost?.invoke() }.getOrNull()
                val afterRead = SystemClock.uptimeMillis()
                if (afterRead - opened >= 500L) { terminate("PREPARATION_TIMEOUT", true); return }
                if (currentHost != null) {
                    if (hostId != null && hostId != currentHost) {
                        terminate("PREPARATION_HOST_CHANGED", true); return
                    }
                    hostId = currentHost
                    val first = hostObservedAt
                    if (first == null) hostObservedAt = afterRead
                    else if (afterRead - first >= 50L) launch()
                } else if (hostId != null) {
                    terminate("PREPARATION_HOST_CHANGED", true); return
                }
                publish()
                if (active) handler.postDelayed(this, 50L)
                return
            }
            if (runCatching { resolveHost?.invoke() }.getOrNull() != hostId) {
                terminate("HOST_CHANGED"); return
            }
            if (policy?.state == ReviewSurfaceProbePolicy.State.RETURNING) {
                val due = longArrayOf(250L, 900L, 1_700L)
                if (returnSample < due.size && now - returnStarted >= due[returnSample]) {
                    returnSample++
                    observeReturn()
                }
            } else if (now - opened <= 1_500L) onSignal()
            publish()
            if (active) handler.postDelayed(this, 100L)
        }
    }

    private fun observeTarget(ownFocused: Boolean) {
        val original = target ?: return
        lastSample = SystemClock.uptimeMillis(); samples++
        val windows = runCatching { service.windows }.getOrNull()
        val targetWindow = windows?.singleOrNull { it.id == original.snapshot.windowId }
        var root: AccessibilityNodeInfo? = null
        var targetRootRead = false
        if (ownFocused && input().role == "OWN" && !inventorySampled) {
            inventorySampled = true
            firstNull = if (windows != null && windows.size <= 16) "NONE" else "UNKNOWN"
            val reviewId = runCatching { editView?.createAccessibilityNodeInfo()?.windowId }.getOrNull()
            for (window in windows?.takeIf { it.size <= 16 }.orEmpty()) {
                val read = runCatching { window.getRoot(0) }.getOrNull()
                if (window.id == original.snapshot.windowId) { root = read; targetRootRead = true }
                if (read == null && firstNull == "NONE") firstNull = when {
                    window.id == original.snapshot.windowId -> "ORIGINAL"
                    window.id == reviewId -> "REVIEW"
                    window.id == hostId -> "HOST"
                    window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "IME"
                    else -> "OTHER"
                }
            }
        } else if (targetWindow != null) {
            targetRootRead = true
            root = runCatching { targetWindow.getRoot(0) }.getOrNull()
        }
        val rootCode = when {
            windows == null || (targetWindow != null && !targetRootRead) -> "UNKNOWN"
            root == null -> "UNAVAILABLE"
            root == original.root && root.packageName?.toString() == FIXTURE -> "SAME"
            else -> "DIFFERENT"
        }
        // Refresh only the originally captured owned node, never search a foreign tree or read text.
        val nodeAvailable = rootCode == "SAME" && runCatching {
            original.node.refresh() && original.node.packageName?.toString() == FIXTURE &&
                original.node.windowId == original.snapshot.windowId
        }.getOrDefault(false)
        targetCode = "window=${if (windows == null) "UNKNOWN" else if (targetWindow == null) "ABSENT" else "PRESENT"}" +
            " root=$rootCode node=${if (rootCode == "UNKNOWN") "UNKNOWN" else if (nodeAvailable) "AVAILABLE" else "UNAVAILABLE"} ownFocused=$ownFocused"
        if (ownFocused) { reviewSamples++; if (rootCode == "SAME") reviewRoots++; if (nodeAvailable) reviewNodes++ }
    }

    private fun dismiss(session: String) {
        if (session != id || !active) return
        if (!policy!!.dismiss(SystemClock.uptimeMillis())) { completeTimed(); return }
        outcome = "OBSERVING_RETURN"
        returnStarted = SystemClock.uptimeMillis()
        if (!closeSurface()) { terminate("UI_REMOVE_FAILED", true); return }
        // No original requestFocus, selection change, keyboard action, or write.
        publish()
    }

    private fun observeReturn() {
        val original = target ?: return
        returnInput = input().role
        if (returnInput == "OTHER") { terminate("FOREIGN_INPUT"); return }
        val observed = if (returnInput == "TARGET") runCatching { capture() }.getOrNull() else null
        // A blocking accessibility read cannot extend this diagnostic's observation interval.
        if (!policy!!.tick(SystemClock.uptimeMillis())) {
            lateReturnRead = true
            comparison = ReviewSurfaceProbePolicy.Comparison()
            completeTimed()
            return
        }
        comparison = ReviewSurfaceProbePolicy.compare(original.snapshot, observed?.snapshot,
            observed?.node == original.node, observed?.root == original.root,
            original.generation, observed?.generation)
        if (comparison.identity == "MISMATCH" || comparison.caret == "MISMATCH" ||
            comparison.context == "MISMATCH") returnMismatchSeen = true
        samples++
    }

    private fun completeTimed() {
        outcome = if (policy?.state == ReviewSurfaceProbePolicy.State.COMPLETE)
            "OBSERVATION_COMPLETE_NOT_ELIGIBILITY"
        else if (policy?.state == ReviewSurfaceProbePolicy.State.REFUSED) "CLOCK_REGRESSION"
        else "DEADLINE"
        cleanup()
    }
    fun terminate(code: String, refused: Boolean = false) {
        if (!active) { if (cleanupFailed) cleanup(); return }
        policy?.cancel(refused); outcome = code; cleanup()
    }
    private fun closeSurface(): Boolean {
        val close = closeUi
        val oldOwner = owner
        owner = null // Activity pause/destroy caused by our finish is not an interruption.
        if (runCatching { close?.invoke() }.isFailure) {
            owner = oldOwner; cleanupFailed = true
            return false
        }
        closeUi = null; editView = null; rootView = null; cleanupFailed = false
        return true
    }
    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        if (!closeSurface()) outcome = "UI_REMOVE_FAILED"
        target = null; hostId = null; resolveHost = null
        if (!cleanupFailed) id = null
        publish()
        finished()
    }
    private fun publish() {
        published = "Review surface probe: arm=${BuildConfig.REVIEW_SURFACE_PROBE}" +
            " state=${policy?.state?.name ?: "IDLE"} samples=$samples edits=${policy?.edits ?: 0}" +
            " blocked=${ReviewSurfaceProbeMode.blocked}\n" +
            "Probe target: $targetCode\n" +
            "Probe return: ${comparison.code(returnInput)}\n" +
            "Probe outcome: $outcome\n" +
            "Probe inventory: firstNull=$firstNull sampled=$inventorySampled\n" +
            "Probe review samples/root/node: $reviewSamples/$reviewRoots/$reviewNodes\n" +
            "Probe input starts/finishes/own/target/restarting: $inputStarts/$inputFinishes/$ownStarts/$targetStarts/$restartingStarts\n" +
            "Probe return mismatch observed: $returnMismatchSeen\n" +
            "Probe return late read discarded: $lateReturnRead\n" +
            "Probe effects: microphone=NEVER_STARTED authority=NONE cleanup=${if (cleanupFailed) "FAILED" else if (active) "ACTIVE" else "CLEAN"}\n"
    }
}
