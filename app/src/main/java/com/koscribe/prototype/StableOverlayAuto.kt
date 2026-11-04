package com.koscribe.prototype

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.text.InputFilter
import android.text.InputType
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Both automatic adapters must satisfy the same original-intent checks inside the one writer. */
internal interface OriginalWriteBinding {
    val draftId: String?
    val policy: OriginalDestinationIntent
    val node: AccessibilityNodeInfo
    val root: AccessibilityNodeInfo
    val generation: Int
}

internal object StableFixtureField {
    const val PACKAGE = "com.koscribe.smoke"
    const val ID = "$PACKAGE:id/stable_auto_editor"
    fun eligible(snapshot: EditorSnapshot, editorResourceName: String?) =
        snapshot.isSafe() && snapshot.packageName == PACKAGE && snapshot.viewId == ID &&
            snapshot.editorFieldId > 0 && editorResourceName == ID
    fun caretContradicts(start: Int, end: Int, original: Int) =
        !(start == -1 && end == -1) && (start != original || end != original)
}

/** Owned fixture only. Original context is read at capture and in the shared returned-editor writer. */
internal class StableOverlayAuto(
    private val service: KoScribeService,
    private val original: EditorSnapshot,
    override val node: AccessibilityNodeInfo,
    override val root: AccessibilityNodeInfo,
    override val generation: Int,
    override val policy: OriginalDestinationIntent,
    override val draftId: String,
    private val capturedUptime: Long,
    private val host: () -> Int?,
    private val input: () -> Served?,
    private val usable: () -> Boolean
) : OriginalWriteBinding {
    data class Served(val info: EditorInfo, val generation: Int)
    private val handler = Handler(Looper.getMainLooper())
    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var panel: LinearLayout? = null
    private var editor: EditText? = null
    private var notice: TextView? = null
    private var ownToken: IBinder? = null
    private var ownId: Int? = null
    private var ownRoot: AccessibilityNodeInfo? = null
    private var ownNode: AccessibilityNodeInfo? = null
    private var hostId: Int? = null
    private var hostSeenAt: Long? = null
    private var launchedAt: Long? = null
    private var returningAt: Long? = null
    private var approved = false
    private var sawReturnInput = false
    private var writerCalled = false
    private var closed = false
    private var stoppingReason: String? = null
    private var fallbackReported = false
    private val inputTrace = StableOverlayInputTrace(capturedUptime)
    val phase: String get() = if (policy.phase == OriginalDestinationIntent.Phase.ORIGINAL)
        "PREPARING" else policy.phase.name
    val windowAttached: Boolean get() = panel != null

    fun start() { publishInputTrace(); handler.post(pulse) }

    private fun publishInputTrace() = service.stableInputDiagnostic(this, inputTrace.summary())

    private fun traceMark(code: String, atPhase: String = phase) {
        inputTrace.mark(code, atPhase, SystemClock.uptimeMillis()); publishInputTrace()
    }

    private fun traceRefusal(code: String, atPhase: String) {
        inputTrace.refuse(code, atPhase, SystemClock.uptimeMillis()); publishInputTrace()
    }

    private fun sessionCode(info: EditorInfo?, gen: Int): String {
        val role = when {
            info == null -> "UNKNOWN"
            originalInfo(info) -> "TARGET_EXACT"
            info.packageName == StableFixtureField.PACKAGE -> "TARGET_OTHER"
            info.packageName == service.packageName && info.fieldId == R.id.auto_overlay_english &&
                info.inputType == editor?.inputType -> "OWN_EXACT"
            info.packageName == service.packageName -> "OWN_OTHER"
            else -> "FOREIGN"
        }
        val relative = when (gen) {
            generation -> "G0"
            generation + 1 -> "G1"
            generation + 2 -> "G2"
            else -> "GX"
        }
        return "${role}_$relative"
    }

    fun inputStarted(info: EditorInfo, gen: Int, restarting: Boolean) {
        if (closed || stoppingReason != null) return
        val before = phase
        val code = "START_${sessionCode(info, gen)}_${if (restarting) "RESTART" else "NEW"}"
        if (fallbackReported) {
            inputTrace.followingStart(code, before, SystemClock.uptimeMillis()); publishInputTrace()
        } else traceMark(code, before)
        if (info.packageName !in setOf(service.packageName, StableFixtureField.PACKAGE)) {
            interrupt("FOREIGN_INPUT"); return
        }
        if (fallbackReported) return
        when (policy.phase) {
            OriginalDestinationIntent.Phase.ENTERING_REVIEW -> {
                if (info.packageName != service.packageName || info.fieldId != R.id.auto_overlay_english ||
                    info.inputType != editor?.inputType || !policy.reviewInputStarted(gen, restarting, true))
                    disarm("INPUT_REVIEW_CHANGED", before)
            }
            OriginalDestinationIntent.Phase.RETURNING -> {
                if (!originalInfo(info)) { disarm("RETURN_FIELD_CHANGED"); return }
                if (!policy.originalInputReturned(gen, restarting)) disarm("INPUT_RETURN_CHANGED", before)
                else sawReturnInput = true
            }
            else -> disarm("INPUT_EXTRA_START")
        }
    }

    fun inputFinished(gen: Int, endedInfo: EditorInfo?) {
        if (closed || stoppingReason != null || fallbackReported) return
        val before = phase
        val ending = sessionCode(endedInfo, gen)
        val refused = policy.phase == OriginalDestinationIntent.Phase.REVIEW || !policy.inputFinished(gen, false)
        traceMark("FINISH_${ending}_${if (refused) "REFUSED" else "ACCEPTED"}", before)
        if (refused) {
            // Preserve the actual terminating callback even if the bounded prefix was already full.
            traceRefusal("INPUT_UNEXPECTED_FINISH_$ending", before)
            disarm("INPUT_UNEXPECTED_FINISH", before)
        }
    }

    fun selection(gen: Int, start: Int, end: Int, composingStart: Int, composingEnd: Int) {
        if (closed || stoppingReason != null || fallbackReported || (gen != generation && gen != generation + 2)) return
        val caret = original.offset + original.selectionStart
        if (start != caret || end != caret || (composingStart >= 0 && composingEnd > composingStart))
            disarm("TARGET_SELECTION_CHANGED")
    }

    fun event(event: AccessibilityEvent) {
        if (closed || stoppingReason != null || event.eventTime < capturedUptime) return
        val type = event.eventType
        val focusNotice = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED && event.windowChanges and
                (AccessibilityEvent.WINDOWS_CHANGE_FOCUSED or AccessibilityEvent.WINDOWS_CHANGE_ACTIVE) != 0)
        if (focusNotice && !runCatching { knownWindowOwner(event.windowId, service.windows) }.getOrDefault(false)) {
            interrupt("UNEXPECTED_WINDOW_OWNER"); return
        }
        if (fallbackReported) return
        if (event.windowId == original.windowId) {
            if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                event.windowChanges and (AccessibilityEvent.WINDOWS_CHANGE_REMOVED or AccessibilityEvent.WINDOWS_CHANGE_ADDED) != 0) {
                disarm("TARGET_WINDOW_REPLACED"); return
            }
            if (event.packageName?.toString() == StableFixtureField.PACKAGE) when (type) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> { disarm("TARGET_TEXT_CHANGED"); return }
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                    val caret = original.offset + original.selectionStart
                    if (event.getSource(0) != node || event.fromIndex != caret || event.toIndex != caret) {
                        disarm("TARGET_SELECTION_CHANGED"); return
                    }
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> if (event.getSource(0) != node ||
                    policy.phase == OriginalDestinationIntent.Phase.REVIEW) {
                    disarm("RETURN_FIELD_CHANGED"); return
                }
            }
        }
        if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            (event.windowId == original.windowId && type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
            inspect()
    }

    private val pulse = object : Runnable {
        override fun run() {
            if (closed || stoppingReason != null) return
            if (!usable() || !withinLifetime()) {
                interrupt("INTERRUPTED_OR_EXPIRED"); return
            }
            if (!fallbackReported) inspect()
            if (!closed) handler.postDelayed(this, if (phase in setOf("REVIEW", "FALLBACK")) 250 else 50)
        }
    }

    private fun originalInfo(info: EditorInfo) = info.packageName == original.packageName &&
        info.fieldId == original.editorFieldId && info.inputType == original.inputType

    private fun withinLifetime() = SystemClock.uptimeMillis() - capturedUptime in 0L until 120_000L

    private fun knownWindowOwner(id: Int, windows: List<AccessibilityWindowInfo>): Boolean {
        if (id < 0) return false
        if (id == original.windowId || id == hostId || id == ownId ||
            id == editor?.takeIf { it.isAttachedToWindow }?.createAccessibilityNodeInfo()?.windowId) return true
        return windows.singleOrNull { it.id == id }?.let {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD && it.displayId == Display.DEFAULT_DISPLAY
        } == true
    }

    /** One bounded scope: original window/root/pinned node, local host, and authenticated own overlay. */
    private fun targetProblem(windows: List<AccessibilityWindowInfo>): String? {
        val window = windows.singleOrNull { it.id == original.windowId } ?: return "TARGET_WINDOW_MISSING"
        if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION || window.displayId != Display.DEFAULT_DISPLAY)
            return "TARGET_WINDOW_CHANGED"
        val freshRoot = window.getRoot(0) ?: return "TARGET_ROOT_MISSING"
        if (freshRoot != root || freshRoot.packageName?.toString() != StableFixtureField.PACKAGE ||
            !node.refresh() || node.windowId != original.windowId || !node.isEnabled || !node.isEditable ||
            node.isPassword || node.packageName?.toString() != StableFixtureField.PACKAGE ||
            node.viewIdResourceName != StableFixtureField.ID || node.className?.toString() != original.className ||
            node.inputType != original.inputType ||
            (node.uniqueId?.takeIf(String::isNotBlank) ?: "source-node") != original.uniqueNodeId)
            return "TARGET_IDENTITY_CHANGED"
        if (StableFixtureField.caretContradicts(node.textSelectionStart, node.textSelectionEnd,
                original.offset + original.selectionStart)) return "TARGET_CARET_CHANGED"
        if (window.isFocused && input()?.info?.packageName == StableFixtureField.PACKAGE &&
            freshRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) != node) return "RETURN_FIELD_CHANGED"
        return null
    }

    private fun ownFocused(windows: List<AccessibilityWindowInfo>): Boolean {
        val local = editor ?: return false
        val hostView = panel ?: return false
        val token = local.windowToken ?: return false
        if (!local.isAttachedToWindow || !local.hasWindowFocus() || !local.isFocused || !local.isShown ||
            !hostView.isAttachedToWindow || hostView.windowToken != token) return false
        val served = input() ?: return false
        if (served.generation != generation + 1 || served.info.packageName != service.packageName ||
            served.info.fieldId != R.id.auto_overlay_english || served.info.inputType != local.inputType) return false
        val localNode = local.createAccessibilityNodeInfo()
        val window = windows.singleOrNull { it.isFocused } ?: return false
        if (window.id != localNode.windowId || window.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY ||
            window.displayId != Display.DEFAULT_DISPLAY) return false
        val remoteRoot = window.getRoot(0) ?: return false
        if (remoteRoot.packageName?.toString() != service.packageName || remoteRoot.windowId != window.id) return false
        val remote = remoteRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (remote != localNode || !remote.isEditable || !remote.isFocused || remote.windowId != window.id ||
            remote.viewIdResourceName != "${service.packageName}:id/auto_overlay_english") return false
        if (ownToken != null && (ownToken != token || ownId != window.id ||
                ownRoot != remoteRoot || ownNode != remote)) return false
        ownToken = token; ownId = window.id; ownRoot = remoteRoot; ownNode = remote
        return true
    }

    private fun inspect() {
        if (closed || stoppingReason != null || fallbackReported) return
        try { inspectCurrent() } catch (_: Exception) { disarm("EVIDENCE_UNAVAILABLE") }
    }

    private fun inspectCurrent() {
        if (!usable()) { interrupt("INTERRUPTED"); return }
        val windows = service.windows
        targetProblem(windows)?.let { disarm(it); return }
        val currentHost = host()
        if (hostId != null && currentHost != hostId) { disarm("HOST_CHANGED"); return }
        val focused = windows.filter { it.isFocused }
        val localId = editor?.takeIf { it.isAttachedToWindow }?.createAccessibilityNodeInfo()?.windowId
        if (focused.size > 1 || focused.any { it.id != original.windowId && it.id != (ownId ?: localId) }) {
            interrupt("FOREIGN_FOCUS"); return
        }
        when (policy.phase) {
            OriginalDestinationIntent.Phase.ORIGINAL -> {
                val served = input()
                if (served == null || served.generation != generation || !originalInfo(served.info) ||
                    focused.singleOrNull()?.id != original.windowId) { disarm("PREPARATION_TARGET_CHANGED"); return }
                val now = SystemClock.uptimeMillis()
                if (now - capturedUptime >= 500) { disarm("PREPARATION_TIMEOUT"); return }
                if (currentHost != null) {
                    hostId = currentHost
                    val first = hostSeenAt
                    if (first == null) hostSeenAt = now
                    else if (now - first >= 50 && policy.beginReview(draftId, SystemClock.elapsedRealtime())) {
                        launchedAt = now
                        traceMark("LAUNCH")
                        open()
                    }
                }
            }
            OriginalDestinationIntent.Phase.ENTERING_REVIEW -> {
                val proved = ownFocused(windows)
                if (SystemClock.uptimeMillis() - (launchedAt ?: capturedUptime) >= 2_000) {
                    disarm("REVIEW_FOCUS_UNPROVED"); return
                }
                if (proved && !policy.reviewFocusAttested && policy.reviewFocused(draftId, SystemClock.elapsedRealtime()))
                    traceMark("OWN_FOCUS_ATTESTED")
            }
            OriginalDestinationIntent.Phase.REVIEW -> if (!ownFocused(windows)) disarm("REVIEW_OWNER_CHANGED")
            OriginalDestinationIntent.Phase.RETURNING -> {
                if (SystemClock.uptimeMillis() - (returningAt ?: capturedUptime) >= 2_000) { disarm("RETURN_TIMEOUT"); return }
                if (sawReturnInput && focused.singleOrNull()?.id == original.windowId && panel == null &&
                    !writerCalled && input()?.let { it.generation == generation + 2 && originalInfo(it.info) } == true) {
                    writerCalled = true
                    handler.post {
                        if (!closed && !fallbackReported) {
                            if (canWrite()) service.stableWrite(this) else disarm("RETURN_BOUNDARY_CHANGED")
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    /** Final local/metadata gate used by BOTH fresh observations and immediately before consume. */
    fun canWrite(): Boolean = !closed && stoppingReason == null && !fallbackReported && approved && writerCalled &&
        policy.phase == OriginalDestinationIntent.Phase.RETURNING && runCatching {
            val windows = service.windows
            usable() && panel == null && hostId != null && host() == hostId &&
                windows.singleOrNull { it.isFocused }?.id == original.windowId &&
                targetProblem(windows) == null && input()?.let {
                    it.generation == generation + 2 && originalInfo(it.info)
                } == true && withinLifetime() && SystemClock.uptimeMillis() - (returningAt ?: 0L) in 0L until 2_000L
        }.getOrDefault(false)

    fun classify(observed: EditorSnapshot?): String = when {
        observed == null -> "RETURN_OBSERVATION_MISSING"
        observed.windowId != original.windowId || observed.viewId != original.viewId ||
            observed.editorFieldId != original.editorFieldId -> "RETURN_FIELD_CHANGED"
        observed.surroundingText != original.surroundingText || observed.offset != original.offset -> "TARGET_TEXT_CHANGED"
        observed.selectionStart != original.selectionStart || observed.selectionEnd != original.selectionEnd -> "TARGET_CARET_CHANGED"
        else -> "RETURN_TARGET_MISMATCH"
    }

    private fun open() {
        fun dp(n: Int) = (n * service.resources.displayMetrics.density).toInt()
        val holder = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(12)); setBackgroundColor(0xFFF3F7F4.toInt())
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        val message = TextView(service).apply {
            text = "Review English. Approval inserts the fixed Korean test sample."; setTextColor(Color.DKGRAY)
        }
        notice = message; holder.addView(message)
        holder.addView(Button(service).apply {
            id = R.id.auto_overlay_confirm; text = "Confirm & insert test sample"; isAllCaps = false
            setOnClickListener { confirm() }
        })
        holder.addView(Button(service).apply {
            id = R.id.auto_overlay_cancel; text = "Cancel"; isAllCaps = false
            setOnClickListener { interrupt("USER_CANCELLED") }
        })
        val edit = EditText(service).apply {
            id = R.id.auto_overlay_english; contentDescription = "English test sample review"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            filters = arrayOf(InputFilter.LengthFilter(512)); isSaveEnabled = false
            setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE); gravity = Gravity.TOP
            setText(OutputMode.SAMPLE_ENGLISH); setSelection(text.length)
        }
        holder.addView(edit, LinearLayout.LayoutParams(-1, 0, 1f))
        editor = edit; panel = holder
        edit.setOnFocusChangeListener { _, focused ->
            if (!closed && stoppingReason == null) traceMark(if (focused) "LOCAL_FOCUS_GAIN" else "LOCAL_FOCUS_LOSS")
        }
        val params = WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        }
        try { wm.addView(holder, params) } catch (failure: Exception) {
            if (!holder.isAttachedToWindow) { panel = null; editor = null; notice = null }
            throw failure
        }
        var traced = false
        fun traceShowOpportunity() {
            if (!closed && !approved && !traced && edit.isAttachedToWindow && edit.isFocused && edit.hasWindowFocus()) {
                traced = true
                traceMark("IME_SHOW_SUPPRESSED")
            }
        }
        edit.viewTreeObserver.addOnWindowFocusChangeListener { focused ->
            if (!closed && stoppingReason == null) traceMark(if (focused) "WINDOW_FOCUS_GAIN" else "WINDOW_FOCUS_LOSS")
            if (focused) traceShowOpportunity()
            else if (!closed && stoppingReason == null && !approved && ownToken != null) disarm("REVIEW_OWNER_LOST_FOCUS")
        }
        edit.requestFocus(); edit.post { traceShowOpportunity() }
    }

    private fun confirm() {
        if (closed || stoppingReason != null || approved) return
        if (!usable() || !withinLifetime()) { interrupt("INTERRUPTED_OR_EXPIRED"); return }
        val edit = editor ?: return
        if (!edit.isAttachedToWindow || !edit.hasWindowFocus()) { interrupt("OWNER_LOST"); return }
        if (!fallbackReported) {
            inspect()
            if (closed) return
            if (policy.phase != OriginalDestinationIntent.Phase.REVIEW) disarm("REVIEW_NOT_ATTESTED")
        }
        val ownerFocused = !fallbackReported && runCatching { ownFocused(service.windows) }.getOrDefault(false)
        if (!usable() || !withinLifetime()) { interrupt("INTERRUPTED_OR_EXPIRED"); return }
        if (!service.stablePrepareResult(this, edit.text.toString())) {
            notice?.text = "Enter English before approving the sample."
            return
        }
        approved = true
        if (!fallbackReported && !policy.beginReturn(draftId, SystemClock.elapsedRealtime(), ownerFocused))
            disarm("RETURN_NOT_ATTESTED")
        if (closed) return
        returningAt = SystemClock.uptimeMillis()
        if (!removePanel()) { interrupt("PANEL_REMOVE_FAILED"); return }
        if (fallbackReported) finishFallback()
    }

    fun disarm(reason: String, atPhase: String = phase) {
        if (closed || fallbackReported) return
        traceRefusal(reason, atPhase)
        fallbackReported = true
        policy.invalidate(reason)
        service.stableDisarmed(this, reason)
        if (approved || panel == null) finishFallback()
        else notice?.text = "Automatic insertion unavailable. Approve the sample to select its destination manually."
    }

    private fun finishFallback() {
        if (!removePanel()) { interrupt("PANEL_REMOVE_FAILED"); return }
        closeCallbacks()
        service.stableFinished(this, if (approved) "MANUAL_READY" else "PREPARATION_REFUSED")
    }
    fun interrupt(reason: String) {
        if (closed) return
        val terminal = stoppingReason ?: reason
        if (terminal != "USER_CANCELLED") traceRefusal(terminal, phase)
        stoppingReason = terminal // Removing a focused panel can synchronously notify local focus loss.
        policy.invalidate(terminal)
        if (!removePanel()) { service.stableRemovalFailed(this); return }
        closeCallbacks(); service.stableFinished(this, terminal)
    }
    fun consumed() { closeCallbacks() }
    private fun closeCallbacks() { closed = true; handler.removeCallbacksAndMessages(null) }
    private fun removePanel(): Boolean {
        val holder = panel ?: return true
        if (runCatching { if (holder.isAttachedToWindow) wm.removeViewImmediate(holder) }.isFailure) return false
        panel = null; editor = null; notice = null
        return true
    }
}
