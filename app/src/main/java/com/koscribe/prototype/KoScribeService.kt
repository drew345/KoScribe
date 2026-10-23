package com.koscribe.prototype

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.text.InputType
import android.text.Spanned
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Button
import java.io.FileDescriptor
import java.io.PrintWriter
import java.lang.ref.WeakReference

class KoScribeService : AccessibilityService() {
    companion object {
        private var instance = WeakReference<KoScribeService>(null)
        fun active(): KoScribeService? = instance.get()
    }

    private val guard = SessionGuard()
    private val startupExperiment = StartupExperiment.configured
    private val main = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var bubble: LinearLayout? = null
    private var expanded = false
    private var status = "Focus a text field, then choose a mode."
    private var recoveryText: String? = null
    private var pendingId: String? = null
    private var reviewVisible = false
    private var returningToEditor = false
    private var ownTransition = false
    private var targetNode: AccessibilityNodeInfo? = null
    private var targetRootNode: AccessibilityNodeInfo? = null
    private val returnContinuity = ReturnContinuity()
    private var modalOcclusion: ModalOcclusionGate? = null
    private enum class ObservationCode {
        NOT_CHECKED, LOCKED, NO_INPUT_METHOD, INPUT_NOT_STARTED, NO_EDITOR_INFO,
        NO_INPUT_CONNECTION, UNSUPPORTED_EDITOR_TYPE, NO_FOCUSED_APP_WINDOW,
        MULTIPLE_FOCUSED_APP_WINDOWS, NO_FOCUSED_NODE, NODE_EDITOR_MISMATCH,
        PROTECTED_OR_NON_TEXT_NODE, SURROUNDING_UNAVAILABLE, UNKNOWN_TEXT_OFFSET,
        SELECTION_NOT_COLLAPSED, NODE_CARET_MISMATCH, ACTIVE_COMPOSITION,
        CALLBACK_CARET_MISMATCH, NODE_CLASS_UNKNOWN, SNAPSHOT_INVALID, READY
    }
    @Volatile
    private var observationCode = ObservationCode.NOT_CHECKED
    private val trace = ArrayDeque<String>()
    private var traceSequence = 0
    private val invalidationCodes = mutableSetOf<String>()
    private var lastInvalidationCode: String? = null
    private var captureStartUptimeMs: Long? = null
    private var captureCompleteUptimeMs: Long? = null
    @Volatile private var experimentCaptureCount = 0
    @Volatile private var experimentPhase = "NOT_CAPTURED"
    private enum class OwnMilestone {
        COLLAPSED_BUBBLE_ADD_RETURNED, PANEL_LAUNCH_REQUESTED, PANEL_BIND_ACCEPTED,
        PANEL_RESUME_CALLBACK, PANEL_FOCUS_KNOWN_WINDOW_CALLBACK, OWN_FOCUS_WINDOW_VERIFIED,
        EXPERIMENT_HOLD_COMPLETE
    }
    /** One-shot callback times for the latest successful capture, retained through Cancel. */
    private val ownMilestoneUptimeMs = mutableMapOf<OwnMilestone, Long>()
    @Volatile
    private var firstTargetContentEventUptimeMs: Long? = null
    private var firstTargetContentTimingLogged = false
    @Volatile
    private var firstTargetContentTimeCode: String? = null
    @Volatile
    private var firstTargetContentStateCode: String? = null

    private fun note(code: String) {
        synchronized(trace) {
            traceSequence++
            if (trace.size == 32) trace.removeFirst()
            trace.addLast("$traceSequence $code")
        }
    }

    private fun markOwnMilestone(id: String, milestone: OwnMilestone) {
        if (pendingId != id) return
        synchronized(trace) {
            ownMilestoneUptimeMs.putIfAbsent(milestone, SystemClock.uptimeMillis())
        }
    }

    /** Signed by label; only elapsed uptime differences leave the service. */
    private fun relativeUptime(at: Long, reference: Long?): String = when {
        reference == null -> "UNKNOWN"
        at < reference -> "BEFORE_${reference - at}"
        at > reference -> "AFTER_${at - reference}"
        else -> "AT_0"
    }

    private fun ownMilestoneDiagnostics(): String = OwnMilestone.entries.joinToString("\n") { milestone ->
        val at = ownMilestoneUptimeMs[milestone]
        if (at == null) "${milestone.name}_NOT_OBSERVED"
        else "${milestone.name}_CAPTURE_${relativeUptime(at, captureCompleteUptimeMs)}" +
            "_FIRST_CONTENT_${relativeUptime(at, firstTargetContentEventUptimeMs)}"
    }

    private fun refuse(code: ObservationCode): Observation? {
        observationCode = code
        note("OBS_${code.name}")
        return null
    }

    private fun invalidate(id: String, code: String) {
        if (pendingId == id) modalOcclusion?.invalidate()
        if (invalidationCodes.add(code)) {
            note("INVALID_$code")
            modalOcclusion?.let { note("MODAL_GATE_${it.diagnosticCode()}") }
            if (lastInvalidationCode == null) lastInvalidationCode = code
        }
        guard.invalidate(id)
    }

    /** First captured-target content event only; uptime, fixed codes, and cached panel state. */
    private fun noteFirstTargetContentTiming(
        event: AccessibilityEvent, sourceKind: String, deliveredUptimeMs: Long,
        session: SessionGuard.Session
    ) {
        if (firstTargetContentTimingLogged || session.invalidated) return
        firstTargetContentTimingLogged = true
        val start = captureStartUptimeMs
        val complete = captureCompleteUptimeMs
        val occurred = event.eventTime
        val validEventTime = occurred > 0L && occurred <= deliveredUptimeMs
        firstTargetContentEventUptimeMs = occurred.takeIf { validEventTime }
        fun deltaFrom(reference: Long?): String = when {
            !validEventTime || reference == null -> "UNKNOWN"
            occurred < reference -> "BEFORE_${reference - occurred}"
            occurred > reference -> "AFTER_${occurred - reference}"
            else -> "AT_0"
        }
        val relative = when {
            occurred == 0L -> "UNKNOWN_ZERO"
            occurred < 0L -> "UNKNOWN_NEGATIVE"
            occurred > deliveredUptimeMs -> "UNKNOWN_FUTURE"
            start == null || complete == null -> "CAPTURE_UNKNOWN"
            occurred < start -> "BEFORE"
            occurred <= complete -> "DURING"
            else -> "AFTER"
        }
        val deliveryDelay = when {
            occurred <= 0L -> "UNKNOWN"
            deliveredUptimeMs < occurred -> "FUTURE_${occurred - deliveredUptimeMs}"
            else -> (deliveredUptimeMs - occurred).toString()
        }
        val captureDuration = if (start != null && complete != null && complete >= start)
            (complete - start).toString() else "UNKNOWN"
        val timeCode = "FIRST_CONTENT_TIME_CD_${captureDuration}" +
            "_ES_${deltaFrom(start)}_EC_${deltaFrom(complete)}" +
            "_REL_${relative}_DELAY_${deliveryDelay}"
        val stateCode = "FIRST_CONTENT_STATE_SRC_${sourceKind}_FLAGS_${event.contentChangeTypes}" +
            "_VISIBLE_${if (SessionActivity.isVisible) 1 else 0}" +
            "_REVIEW_${if (reviewVisible) 1 else 0}_RETURN_${if (returningToEditor) 1 else 0}" +
            "_GATE_${modalOcclusion?.diagnosticCode() ?: "NONE"}" +
            (if (startupExperiment == StartupExperiment.OFF) ""
             else "_EXPERIMENT_${startupExperiment.name}_DELIVERY_PHASE_$experimentPhase")
        firstTargetContentTimeCode = timeCode
        firstTargetContentStateCode = stateCode
        note(timeCode)
        note(stateCode)
    }

    fun diagnostics(): String = synchronized(trace) {
        "Last observation: ${observationCode.name}\n" +
            "First invalidation: ${lastInvalidationCode ?: "none"}\n" +
            "First content timing: ${firstTargetContentTimeCode ?: "none"}\n" +
            "First content state: ${firstTargetContentStateCode ?: "none"}\n" +
            "Experiment arm: ${startupExperiment.name}\n" +
            "Experiment capture count: $experimentCaptureCount\n" +
            "Experiment phase: $experimentPhase\n" +
            "Own UI milestones (callback uptime vs capture completion / first content occurrence):\n" +
            ownMilestoneDiagnostics() + "\n" +
            "Recent guard events (codes only):\n" +
            if (trace.isEmpty()) "none" else trace.joinToString("\n")
    }

    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<out String>) {
        writer.println(diagnostics())
    }

    private inner class TrackingInputMethod(service: AccessibilityService) : InputMethod(service) {
        var generation = 0
            private set
        private val composition = CompositionEvidence()

        override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
            generation++
            composition.reset()
            inputStarted(attribute)
        }

        override fun onFinishInput() {
            composition.reset()
            inputFinished()
            // Do not call the default implementation: it can clear composition
            // in the editor owned by SwiftKey.
        }

        override fun onUpdateSelection(
            oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
            candidatesStart: Int, candidatesEnd: Int
        ) {
            composition.update(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        }

        fun compositionAt(caret: Int): CompositionState = composition.stateAt(caret)
    }

    private fun inputStarted(info: EditorInfo) {
        val id = pendingId ?: return
        val session = guard.session(id) ?: return
        note(if (info.packageName == session.target.packageName) "INPUT_STARTED_TARGET_APP"
            else if (info.packageName == packageName) "INPUT_STARTED_OWN_APP" else "INPUT_STARTED_OTHER_APP")
        if (returningToEditor) {
            if (info.packageName == session.target.packageName &&
                info.fieldId == session.target.editorFieldId) {
                if (!returnContinuity.returnedInput()) invalidate(id, "RETURN_INPUT_SEQUENCE")
            } else if (info.packageName != packageName) invalidate(id, "RETURN_OTHER_INPUT")
        } else if (reviewVisible) {
            if (info.packageName != packageName ||
                (session.target.packageName == packageName &&
                    info.fieldId == session.target.editorFieldId)) invalidate(id, "REVIEW_INPUT")
            if (modalOcclusion?.focusedPanelMatches(focusedPanelWindowId(windows)) != true)
                invalidate(id, "REVIEW_INPUT_OUTSIDE_PANEL")
        } else {
            // A new or restarted editor while the recording screen is open
            // breaks continuity with the editor captured at mode selection.
            invalidate(id, "INPUT_RESTART")
        }
    }

    private fun inputFinished() {
        val id = pendingId ?: return
        note("INPUT_FINISHED")
        if (returningToEditor && !returnContinuity.finishedInput()) invalidate(id, "INPUT_FINISH_SEQUENCE")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        renderBubble()
    }

    override fun onCreateInputMethod(): InputMethod = TrackingInputMethod(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val deliveredUptimeMs = SystemClock.uptimeMillis()
        val id = pendingId ?: return
        val session = guard.session(id) ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            onWindowsChanged(event, session)
            return
        }
        val packageName = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId) {
                val sourceKind = when (event.source) {
                    null -> "NO_SOURCE"
                    targetNode -> "EDITOR"
                    targetRootNode -> "ROOT"
                    else -> "OTHER_NODE"
                }
                val reason = when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TARGET_TEXT_CHANGED_$sourceKind"
                    AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "TARGET_SELECTION_CHANGED_$sourceKind"
                    else -> "TARGET_CONTENT_CHANGED_$sourceKind"
                }
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    noteFirstTargetContentTiming(event, sourceKind, deliveredUptimeMs, session)
                    note("CONTENT_FLAGS_${event.contentChangeTypes}")
                    if (session.target.packageName == this.packageName &&
                        session.target.viewId == "$packageName:id/local_test_editor")
                        note(localFixtureContentSource(event.source))
                }
                invalidate(id, reason)
            }
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId && event.source == targetNode) {
                note("TARGET_EDITOR_FOCUSED")
                if (!returningToEditor && modalOcclusion?.hasAcquiredFocus == true)
                    invalidate(id, "TARGET_FOCUSED_DURING_PANEL")
            }
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId && event.source != targetNode) {
                invalidate(id, "TARGET_FOCUS_NODE")
            }
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId) {
                val sourceKind = when (event.source) {
                    null -> "NO_SOURCE"
                    targetRootNode -> "ROOT"
                    else -> "OTHER_NODE"
                }
                note("TARGET_STATE_${sourceKind}_FLAGS_${event.contentChangeTypes}")
            }
            if (packageName == session.target.packageName &&
                event.windowId != session.target.windowId && packageName != this.packageName) {
                invalidate(id, "OTHER_SAME_APP_WINDOW")
            }
            if (packageName == session.target.packageName && event.windowId == session.target.windowId) {
                if (returningToEditor) {
                    if (!returnContinuity.returnedStateWindow()) invalidate(id, "RETURN_WINDOW_SEQUENCE")
                } else {
                    invalidate(id, "TARGET_WINDOW_STATE")
                }
            }
            if (packageName != this.packageName && packageName != session.target.packageName) {
                val window = windows.firstOrNull { it.id == event.windowId }
                if (window?.type == AccessibilityWindowInfo.TYPE_APPLICATION) invalidate(id, "OTHER_APP_WINDOW")
            }
        }
    }

    private fun onWindowsChanged(event: AccessibilityEvent, session: SessionGuard.Session) {
        val changes = event.windowChanges
        val id = session.id
        val currentWindows = windows
        if (event.windowId == session.target.windowId) {
            // Every visibility edge must alternate and pass the session gate.
            // Modal ownership uses the unchanged own-package panel selector;
            // final return has a separate exact target package/window selector.
            if (changes and AccessibilityEvent.WINDOWS_CHANGE_REMOVED != 0) {
                note(targetWindowTransition("REMOVED", event, session, currentWindows))
                if (modalOcclusion?.targetRemoved(
                        event.windowId, focusedPanelWindowId(currentWindows),
                        changes == AccessibilityEvent.WINDOWS_CHANGE_REMOVED,
                        currentWindows.none { it.id == session.target.windowId }
                    ) != true) invalidate(id, "UNVERIFIED_MODAL_REMOVAL")
            }
            if (changes and AccessibilityEvent.WINDOWS_CHANGE_ADDED != 0) {
                note(targetWindowTransition("ADDED", event, session, currentWindows))
                val targetWindow = currentWindows.singleOrNull { it.id == session.target.windowId }
                val identity = reappearedTargetIdentity(targetWindow, session.target)
                val verifiedFocusedWindow = if (returningToEditor)
                    focusedReturnWindowId(currentWindows, session.target)
                else focusedPanelWindowId(currentWindows)
                val allowed = modalOcclusion?.targetAdded(
                    event.windowId, verifiedFocusedWindow,
                    changes == AccessibilityEvent.WINDOWS_CHANGE_ADDED,
                    targetWindow?.type == AccessibilityWindowInfo.TYPE_APPLICATION,
                    targetWindow?.isFocused != false, identity.first, identity.second
                ) == true
                if (allowed) note(if (returningToEditor) "RETURN_TARGET_REAPPEARANCE_VERIFIED"
                    else "MODAL_TARGET_REAPPEARANCE_VERIFIED")
                else invalidate(id, "TARGET_WINDOW_REAPPEARED")
            }
            if (changes and AccessibilityEvent.WINDOWS_CHANGE_BOUNDS != 0 && !ownTransition)
                invalidate(id, "TARGET_WINDOW_BOUNDS")
            val structural = AccessibilityEvent.WINDOWS_CHANGE_TITLE or
                AccessibilityEvent.WINDOWS_CHANGE_CHILDREN or AccessibilityEvent.WINDOWS_CHANGE_PARENT or
                AccessibilityEvent.WINDOWS_CHANGE_PIP
            if (changes and structural != 0) invalidate(id, "TARGET_WINDOW_STRUCTURE")
        }
        // These system events often have no package name. Resolve the current
        // focused application window instead of silently dropping them.
        val focusedApps = currentWindows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedApps.size > 1) invalidate(id, "MULTIPLE_FOCUSED_WINDOWS")
        val focusedPackage = focusedApps.singleOrNull()?.root?.packageName?.toString()
        if (focusedPackage != null && focusedPackage != packageName &&
            focusedPackage != session.target.packageName) invalidate(id, "OTHER_FOCUSED_APP")
        if (!returningToEditor && modalOcclusion?.hasAcquiredFocus == true) {
            val focusedPanelWindow = focusedPanelWindowId(currentWindows)
            if (modalOcclusion?.focusedPanelMatches(focusedPanelWindow) == true)
                markOwnMilestone(id, OwnMilestone.OWN_FOCUS_WINDOW_VERIFIED)
            else invalidate(id, "PANEL_FOCUSED_WINDOW_CHANGED")
        }
    }

    private fun focusedPanelWindowId(currentWindows: List<AccessibilityWindowInfo>): Int? =
        currentWindows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }
            .singleOrNull()?.takeIf { it.root?.packageName?.toString() == packageName }?.id

    /** Used only after explicit confirmation; both target package and window must match. */
    private fun focusedReturnWindowId(
        currentWindows: List<AccessibilityWindowInfo>, target: EditorSnapshot
    ): Int? = currentWindows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }
        .singleOrNull()?.takeIf {
            it.id == target.windowId && it.root?.packageName?.toString() == target.packageName
        }?.id

    /** Compare fresh identities without reading the destination's text while it is covered. */
    private fun reappearedTargetIdentity(
        targetWindow: AccessibilityWindowInfo?, original: EditorSnapshot
    ): Pair<Boolean, Boolean> = runCatching {
        val root = targetWindow?.root ?: return@runCatching false to false
        val rootMatches = root.refresh() && root == targetRootNode &&
            root.windowId == original.windowId && root.packageName?.toString() == original.packageName
        if (!rootMatches) return@runCatching false to false
        val editor = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return@runCatching true to false
        val editorMatches = editor.refresh() && editor == targetNode && editor.isFocused &&
            editor.isEditable && !editor.isPassword && editor.windowId == original.windowId &&
            editor.packageName?.toString() == original.packageName &&
            editor.viewIdResourceName == original.viewId && editor.className?.toString() == original.className &&
            editor.inputType == original.inputType &&
            (editor.uniqueId?.takeIf(String::isNotBlank) ?: "source-node") == original.uniqueNodeId
        true to editorMatches
    }.getOrDefault(false to false)

    /** Synthetic diagnostic categories only; no package names, IDs, or node text. */
    private fun targetWindowTransition(
        phase: String, event: AccessibilityEvent, session: SessionGuard.Session,
        currentWindows: List<AccessibilityWindowInfo>
    ): String {
        val focusedApps = currentWindows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val focus = when {
            focusedApps.isEmpty() -> "NONE"
            focusedApps.size > 1 -> "MULTIPLE"
            else -> when (focusedApps[0].root?.packageName?.toString()) {
                packageName -> "OWN"
                session.target.packageName -> "TARGET"
                null -> "NO_ROOT"
                else -> "OTHER"
            }
        }
        val targetWindow = currentWindows.firstOrNull { it.id == session.target.windowId }
        val currentRoot = targetWindow?.root
        val root = when {
            targetWindow == null -> "ABSENT"
            currentRoot == null -> "NO_ROOT"
            currentRoot == targetRootNode -> "SAME"
            else -> "DIFFERENT"
        }
        val source = when (event.source) {
            null -> "NONE"
            targetNode -> "EDITOR"
            targetRootNode -> "ROOT"
            else -> "OTHER"
        }
        val panel = if (SessionActivity.isVisible) "VISIBLE" else "HIDDEN"
        val review = if (reviewVisible) "REVIEW" else "RECORD"
        return "TARGET_WINDOW_${phase}_F${event.windowChanges}_PANEL_${panel}_${review}_FOCUS_${focus}_ROOT_${root}_SOURCE_${source}" +
            "_GATE_${modalOcclusion?.diagnosticCode() ?: "NONE"}"
    }

    /** Fixed categories for the synthetic editor only; never export source IDs or text. */
    private fun localFixtureContentSource(source: AccessibilityNodeInfo?): String = runCatching {
        if (source == null) return@runCatching "LOCAL_CONTENT_NO_SOURCE"
        val relation = when {
            source == targetNode -> "EDITOR"
            source == targetRootNode -> "ROOT"
            source.viewIdResourceName == "$packageName:id/local_test_editor" -> "EDITOR_ID_OTHER_NODE"
            else -> {
                var ancestor = targetNode?.parent
                var found = false
                repeat(12) {
                    if (ancestor == null) return@repeat
                    if (ancestor == source) found = true
                    ancestor = ancestor?.parent
                }
                if (found) "EDITOR_ANCESTOR" else "OTHER_NODE"
            }
        }
        val kind = when (source.className?.toString()) {
            "android.widget.ScrollView" -> "SCROLL"
            "android.widget.LinearLayout" -> "LINEAR"
            "android.widget.FrameLayout" -> "FRAME"
            "android.widget.TextView" -> "TEXT"
            "android.widget.EditText" -> "EDIT"
            else -> "OTHER_CLASS"
        }
        "LOCAL_CONTENT_${relation}_$kind"
    }.getOrDefault("LOCAL_CONTENT_UNKNOWN")

    override fun onInterrupt() { pendingId?.let { invalidate(it, "SERVICE_INTERRUPTED") } }

    override fun onDestroy() {
        pendingId?.let { guard.cancel(it); guard.end(it) }
        targetNode = null
        targetRootNode = null
        modalOcclusion = null
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        bubble = null
        instance = WeakReference(null)
        super.onDestroy()
    }

    fun isSessionActive(id: String): Boolean = pendingId == id && guard.session(id) != null
    fun hasPendingSession(): Boolean = pendingId != null

    fun cancelSession(id: String) {
        guard.cancel(id)
        if (pendingId != id) return
        guard.end(id)
        pendingId = null
        if (startupExperiment != StartupExperiment.OFF) experimentPhase = "CANCELLED"
        reviewVisible = false
        returningToEditor = false
        ownTransition = false
        targetNode = null
        targetRootNode = null
        modalOcclusion = null
        status = "Session cancelled."
        renderBubble()
    }

    fun bindSessionPanel(id: String, owner: Any): Boolean {
        if (pendingId != id || guard.session(id) == null) return false
        val bound = modalOcclusion?.bind(owner) == true
        if (!bound) invalidate(id, "PANEL_OWNER_CHANGED")
        else {
            markOwnMilestone(id, OwnMilestone.PANEL_BIND_ACCEPTED)
            if (startupExperiment == StartupExperiment.PANEL) experimentPhase = "PANEL_BOUND"
        }
        return bound
    }

    fun updateSessionPanel(
        id: String, owner: Any, phase: PanelPhase, resumed: Boolean,
        focused: Boolean, windowId: Int, interrupted: Boolean
    ) {
        // Old Activity callbacks must never poison a later session.
        if (pendingId != id || guard.session(id) == null) return
        if (modalOcclusion?.owns(owner) == true) {
            if (resumed) markOwnMilestone(id, OwnMilestone.PANEL_RESUME_CALLBACK)
            if (focused && windowId >= 0)
                markOwnMilestone(id, OwnMilestone.PANEL_FOCUS_KNOWN_WINDOW_CALLBACK)
        }
        if (modalOcclusion?.updatePanel(owner, phase, resumed, focused, windowId, interrupted) != true)
            invalidate(id, "PANEL_LIFECYCLE_INTERRUPTED")
        else if (startupExperiment == StartupExperiment.PANEL)
            experimentPhase = "PANEL_CALLBACK_${phase.name}"
        if (phase == PanelPhase.REVIEW && !reviewVisible) {
            reviewVisible = true
            note("REVIEW_STARTED")
        }
    }

    fun requestInsert(id: String, owner: Any, output: String) {
        if (pendingId != id) return
        if (startupExperiment != StartupExperiment.OFF) {
            invalidate(id, "EXPERIMENT_INSERT_BLOCKED")
            cancelSession(id)
            return
        }
        if (modalOcclusion?.focusedPanelMatches(focusedPanelWindowId(windows)) != true)
            invalidate(id, "CONFIRMATION_OUTSIDE_PANEL")
        if (modalOcclusion?.beginReturn(owner) != true) invalidate(id, "UNVERIFIED_PANEL_CONFIRMATION")
        if (!guard.publish(id, output)) {
            status = "Session expired or changed. Start again."
            recoveryText = output
            pendingId = null
            reviewVisible = false
            returningToEditor = false
            ownTransition = false
            targetNode = null
            targetRootNode = null
            modalOcclusion = null
            guard.end(id)
            renderBubble()
            return
        }
        recoveryText = output
        returningToEditor = true
        note("RETURN_REQUESTED")
        returnContinuity.beginReturn()
        status = "Returning to editor…"
        renderBubble()
        // The Activity calls finish immediately after this; never commit into its review editor.
        main.postDelayed({ attemptInsertion(id) }, 750)
    }

    private fun startMode(mode: OutputMode) {
        if (pendingId != null || SessionActivity.isVisible) {
            status = "Finish or cancel the current review before starting another session."
            renderBubble()
            return
        }
        val captureStart = SystemClock.uptimeMillis()
        val target = observeEditor()
        val captureComplete = SystemClock.uptimeMillis()
        if (target == null) {
            status = "Editor refused: ${observationCode.name}. Open KoScribe for diagnostic codes."
            renderBubble()
            return
        }
        val session = guard.begin(mode, target.snapshot, SystemClock.elapsedRealtime()) ?: return
        if (startupExperiment != StartupExperiment.OFF) {
            experimentCaptureCount++
            experimentPhase = "CAPTURED"
        }
        captureStartUptimeMs = captureStart
        captureCompleteUptimeMs = captureComplete
        synchronized(trace) { ownMilestoneUptimeMs.clear() }
        firstTargetContentEventUptimeMs = null
        firstTargetContentTimingLogged = false
        firstTargetContentTimeCode = null
        firstTargetContentStateCode = null
        pendingId = session.id
        invalidationCodes.clear()
        lastInvalidationCode = null
        note("MODE_SELECTED_${mode.name}")
        targetNode = target.node
        targetRootNode = target.root
        modalOcclusion = ModalOcclusionGate(session.target.windowId)
        returnContinuity.capture(target.generation)
        recoveryText = null
        reviewVisible = false
        returningToEditor = false
        ownTransition = true
        if (startupExperiment == StartupExperiment.HOLD) {
            experimentPhase = "EXPANDED_HOLD"
            scheduleExperimentHold(session.id)
            return
        }
        if (startupExperiment != StartupExperiment.PANEL) {
            if (startupExperiment == StartupExperiment.COLLAPSE)
                experimentPhase = "COLLAPSE_REQUESTED"
            expanded = false
            renderBubble()
            if (bubble != null)
                markOwnMilestone(session.id, OwnMilestone.COLLAPSED_BUBBLE_ADD_RETURNED)
        }
        if (startupExperiment == StartupExperiment.COLLAPSE) {
            experimentPhase = if (bubble != null) "COLLAPSED_HOLD" else "COLLAPSE_ADD_FAILED_HOLD"
            scheduleExperimentHold(session.id)
            return
        }
        val intent = Intent(this, SessionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SessionActivity.EXTRA_ID, session.id)
            putExtra(SessionActivity.EXTRA_MODE, mode.name)
        }
        if (startupExperiment == StartupExperiment.PANEL)
            experimentPhase = "PANEL_LAUNCH_REQUESTED"
        markOwnMilestone(session.id, OwnMilestone.PANEL_LAUNCH_REQUESTED)
        try {
            startActivity(intent)
            if (startupExperiment == StartupExperiment.PANEL)
                main.postDelayed({
                    if (pendingId == session.id) {
                        experimentPhase = "SERVICE_DEADLINE"
                        cancelSession(session.id)
                    }
                }, 10_000)
        } catch (_: Exception) {
            if (startupExperiment == StartupExperiment.PANEL) experimentPhase = "LAUNCH_FAILED"
            guard.cancel(session.id)
            guard.end(session.id)
            pendingId = null
            targetNode = null
            targetRootNode = null
            modalOcclusion = null
            ownTransition = false
            status = "Could not open recording screen."
            renderBubble()
        }
    }

    private fun scheduleExperimentHold(id: String) {
        main.postDelayed({
            if (pendingId == id) {
                markOwnMilestone(id, OwnMilestone.EXPERIMENT_HOLD_COMPLETE)
                experimentPhase = "HOLD_COMPLETE"
                cancelSession(id)
            }
        }, 1_500)
    }

    private fun attemptInsertion(id: String) {
        if (pendingId != id) return
        if (startupExperiment != StartupExperiment.OFF) {
            invalidate(id, "EXPERIMENT_INSERT_BLOCKED")
            cancelSession(id)
            return
        }
        val session = guard.session(id)
        if (session == null) {
            status = "Session ended; use Copy if a result is available."
            pendingId = null
            targetNode = null
            targetRootNode = null
            modalOcclusion = null
            ownTransition = false
            guard.end(id)
            renderBubble()
            return
        }
        val observed = observeEditor()
        reviewVisible = false
        returningToEditor = false
        ownTransition = false
        // AccessibilityNodeInfo equality compares the underlying window/source identity.
        // Apps need not provide uniqueId; the source node must still match exactly.
        val sameNode = targetNode != null && observed?.node == targetNode
        val sameRoot = targetRootNode != null && observed?.root == targetRootNode
        val continuousInput = observed != null && returnContinuity.allows(observed.generation)
        val visibilityVerified = modalOcclusion?.readyForCommit() == true
        if (observed != null && !sameNode) note("INSERT_NODE_IDENTITY_CHANGED")
        if (observed != null && !sameRoot) note("INSERT_ROOT_IDENTITY_CHANGED")
        if (observed != null && !continuousInput) note("INSERT_INPUT_CONTINUITY_FAILED")
        if (!visibilityVerified) note("INSERT_TARGET_VISIBILITY_UNVERIFIED")
        val decision = guard.reserveCommit(id,
            if (sameNode && sameRoot && continuousInput && visibilityVerified) observed?.snapshot else null,
            SystemClock.elapsedRealtime())
        if (decision != GuardDecision.Ready) {
            val reason = (decision as GuardDecision.Refused).reason
            val detail = if (observed == null) observationCode.name else reason.name
            note("INSERT_REFUSED_${reason.name}")
            val invalidation = lastInvalidationCode?.let { " (first guard event: $it)" } ?: ""
            status = "Automatic insertion refused: $detail$invalidation. Use Copy or start again."
            pendingId = null
            targetNode = null
            targetRootNode = null
            modalOcclusion = null
            guard.end(id)
            renderBubble()
            return
        }
        // Consumed before this IPC. A thrown exception or unconfirmed result is ambiguous: no retry.
        val connection = observed?.connection
        if (connection == null) {
            note("INSERT_CONNECTION_MISSING")
            status = "Insertion outcome uncertain. Check the editor before any manual paste."
        } else {
            try {
                note("COMMIT_REQUESTED_ONCE")
                connection.commitText(session.result!!, 1, null)
                status = if (observed?.compositionState == CompositionState.UNKNOWN)
                    "Commit requested once; composition state was unreported. Check the editor before further input. KoScribe never sends."
                else "Commit requested once. Check the editor; KoScribe never sends."
            } catch (_: Exception) {
                note("COMMIT_OUTCOME_UNCERTAIN")
                status = "Insertion outcome uncertain. Check the editor before any manual paste."
            }
        }
        pendingId = null
        targetNode = null
        targetRootNode = null
        modalOcclusion = null
        guard.end(id)
        renderBubble()
    }

    private data class Observation(
        val snapshot: EditorSnapshot,
        val node: AccessibilityNodeInfo,
        val root: AccessibilityNodeInfo,
        val connection: android.accessibilityservice.InputMethod.AccessibilityInputConnection,
        val generation: Int,
        val compositionState: CompositionState
    )

    private fun observeEditor(): Observation? {
        observationCode = ObservationCode.NOT_CHECKED
        val locked = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        if (locked) return refuse(ObservationCode.LOCKED)
        val method = inputMethod as? TrackingInputMethod
            ?: return refuse(ObservationCode.NO_INPUT_METHOD)
        if (!method.currentInputStarted) return refuse(ObservationCode.INPUT_NOT_STARTED)
        val info = method.currentInputEditorInfo ?: return refuse(ObservationCode.NO_EDITOR_INFO)
        val connection = method.currentInputConnection
            ?: return refuse(ObservationCode.NO_INPUT_CONNECTION)
        if (!safeInputType(info.inputType)) return refuse(ObservationCode.UNSUPPORTED_EDITOR_TYPE)
        val focusedWindows = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedWindows.isEmpty()) return refuse(ObservationCode.NO_FOCUSED_APP_WINDOW)
        if (focusedWindows.size > 1) return refuse(ObservationCode.MULTIPLE_FOCUSED_APP_WINDOWS)
        val root = focusedWindows.single().root ?: return refuse(ObservationCode.NO_FOCUSED_NODE)
        if (!root.refresh()) return refuse(ObservationCode.NO_FOCUSED_NODE)
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return refuse(ObservationCode.NO_FOCUSED_NODE)
        if (!focus.isFocused || !focus.isEditable ||
            focus.packageName?.toString() != info.packageName)
            return refuse(ObservationCode.NODE_EDITOR_MISMATCH)
        if (focus.isPassword || !safeInputType(focus.inputType))
            return refuse(ObservationCode.PROTECTED_OR_NON_TEXT_NODE)
        val unique = focus.uniqueId?.takeIf(String::isNotBlank) ?: "source-node"
        val surrounding = try {
            connection.getSurroundingText(64, 64, InputConnection.GET_TEXT_WITH_STYLES)
        } catch (_: Exception) { null } ?: return refuse(ObservationCode.SURROUNDING_UNAVAILABLE)
        val text = surrounding.text
        val composing = (text as? Spanned)?.let { spanText ->
            spanText.getSpans(0, spanText.length, Any::class.java)
                .any { spanText.getSpanFlags(it) and Spanned.SPAN_COMPOSING != 0 }
        } ?: false
        val caret = surrounding.offset + surrounding.selectionStart
        if (surrounding.offset < 0) return refuse(ObservationCode.UNKNOWN_TEXT_OFFSET)
        if (surrounding.selectionStart != surrounding.selectionEnd)
            return refuse(ObservationCode.SELECTION_NOT_COLLAPSED)
        val connectionEmptyAtZero = text.isEmpty() && surrounding.offset == 0 &&
            surrounding.selectionStart == 0 && surrounding.selectionEnd == 0
        val nodeUnknownPair = focus.textSelectionStart == -1 && focus.textSelectionEnd == -1
        val nodeEmptyOrHint = focus.isShowingHintText || focus.text.isNullOrEmpty()
        if (!EditorCaretAgreement.matches(
                focus.textSelectionStart, focus.textSelectionEnd, surrounding.offset,
                surrounding.selectionStart, surrounding.selectionEnd, text.isEmpty(), nodeEmptyOrHint
            )) {
            note("CARET_EMPTY_PROBE_C${if (connectionEmptyAtZero) "Y" else "N"}" +
                "_N${if (nodeUnknownPair) "Y" else "N"}" +
                "_H${if (nodeEmptyOrHint) "Y" else "N"}")
            return refuse(ObservationCode.NODE_CARET_MISMATCH)
        }
        if (nodeUnknownPair) note("OBS_VERIFIED_EMPTY_CARET")
        val composition = method.compositionAt(caret)
        if (composition == CompositionState.COMPOSING || composing) {
            return refuse(ObservationCode.ACTIVE_COMPOSITION)
        }
        if (composition == CompositionState.SELECTION_MISMATCH) {
            return refuse(ObservationCode.CALLBACK_CARET_MISMATCH)
        }
        val snapshot = EditorSnapshot(
            info.packageName, focus.windowId, unique, focus.viewIdResourceName,
            focus.className?.toString() ?: return refuse(ObservationCode.NODE_CLASS_UNKNOWN),
            info.fieldId, info.inputType,
            text.toString(), surrounding.offset, surrounding.selectionStart,
            surrounding.selectionEnd, composing
        )
        if (!snapshot.isSafe()) return refuse(ObservationCode.SNAPSHOT_INVALID)
        observationCode = ObservationCode.READY
        note("OBS_READY_${composition.name}")
        return Observation(snapshot, focus, root, connection, method.generation, composition)
    }

    private fun safeInputType(type: Int): Boolean {
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        val variation = type and InputType.TYPE_MASK_VARIATION
        return variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    private fun copyRecovery() {
        if (startupExperiment != StartupExperiment.OFF) return
        val result = recoveryText ?: return
        val clip = ClipData.newPlainText("KoScribe sample", result)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        status = "Copied. Clipboard contents were replaced; paste manually after checking the target."
        renderBubble()
    }

    private fun renderBubble() {
        if (!::windowManager.isInitialized) return
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (expanded) {
                setBackgroundColor(0xFFF7F7F7.toInt())
                setPadding(dp(8), dp(8), dp(8), dp(8))
                elevation = dp(4).toFloat()
            }
        }
        if (expanded) {
            holder.addView(caption("KoScribe · sample only", true))
            holder.addView(caption(status))
            for (mode in OutputMode.entries) holder.addView(action(mode.label) { startMode(mode) })
            if (startupExperiment == StartupExperiment.OFF && recoveryText != null)
                holder.addView(action("Copy result manually") { copyRecovery() })
            holder.addView(action("Close") { expanded = false; renderBubble() })
        } else {
            val pill = Button(this).apply {
                text = "Ko"
                isAllCaps = false
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, 0)
                backgroundTintList = null
                background = GradientDrawable().apply {
                    setColor(0xDD263449.toInt())
                    cornerRadius = dp(24).toFloat()
                }
                stateListAnimator = null
                elevation = 0f
                setOnClickListener { expanded = true; renderBubble() }
            }
            holder.addView(pill, LinearLayout.LayoutParams(dp(56), dp(48)))
        }
        val params = WindowManager.LayoutParams(
            if (expanded) dp(260) else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(8); y = dp(110) }
        try {
            windowManager.addView(holder, params)
            bubble = holder
        } catch (_: Exception) { bubble = null }
    }
}
