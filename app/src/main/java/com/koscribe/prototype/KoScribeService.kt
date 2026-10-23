package com.koscribe.prototype

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
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
import java.lang.ref.WeakReference

class KoScribeService : AccessibilityService() {
    companion object {
        private var instance = WeakReference<KoScribeService>(null)
        fun active(): KoScribeService? = instance.get()
    }

    private val guard = SessionGuard()
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
    private var targetHidden = false
    private var targetNode: AccessibilityNodeInfo? = null
    private val returnContinuity = ReturnContinuity()
    private var observationFailure = "Editor unavailable or unsafe."

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
        if (returningToEditor) {
            if (info.packageName == session.target.packageName &&
                info.fieldId == session.target.editorFieldId) {
                if (!returnContinuity.returnedInput()) guard.invalidate(id)
            } else if (info.packageName != packageName) guard.invalidate(id)
        } else if (reviewVisible) {
            if (info.packageName != packageName ||
                (session.target.packageName == packageName &&
                    info.fieldId == session.target.editorFieldId)) guard.invalidate(id)
        } else {
            // A new or restarted editor while the recording screen is open
            // breaks continuity with the editor captured at mode selection.
            guard.invalidate(id)
        }
    }

    private fun inputFinished() {
        val id = pendingId ?: return
        if (returningToEditor && !returnContinuity.finishedInput()) guard.invalidate(id)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        renderBubble()
    }

    override fun onCreateInputMethod(): InputMethod = TrackingInputMethod(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
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
                event.windowId == session.target.windowId) guard.invalidate(id)
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId && event.source != targetNode) {
                guard.invalidate(id)
            }
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName == session.target.packageName &&
                event.windowId != session.target.windowId && packageName != this.packageName) {
                guard.invalidate(id)
            }
            if (packageName == session.target.packageName && event.windowId == session.target.windowId) {
                if (returningToEditor) {
                    if (!returnContinuity.returnedStateWindow()) guard.invalidate(id)
                } else {
                    guard.invalidate(id)
                }
            }
            if (packageName != this.packageName && packageName != session.target.packageName) {
                val window = windows.firstOrNull { it.id == event.windowId }
                if (window?.type == AccessibilityWindowInfo.TYPE_APPLICATION) guard.invalidate(id)
            }
        }
    }

    private fun onWindowsChanged(event: AccessibilityEvent, session: SessionGuard.Session) {
        val changes = event.windowChanges
        val id = session.id
        if (event.windowId == session.target.windowId) {
            if (changes and AccessibilityEvent.WINDOWS_CHANGE_REMOVED != 0) {
                if (ownTransition) targetHidden = true else guard.invalidate(id)
            }
            if (changes and AccessibilityEvent.WINDOWS_CHANGE_ADDED != 0 && targetHidden) {
                if (!returningToEditor) guard.invalidate(id)
                targetHidden = false
            }
            if (changes and AccessibilityEvent.WINDOWS_CHANGE_BOUNDS != 0 && !ownTransition)
                guard.invalidate(id)
            val structural = AccessibilityEvent.WINDOWS_CHANGE_TITLE or
                AccessibilityEvent.WINDOWS_CHANGE_CHILDREN or AccessibilityEvent.WINDOWS_CHANGE_PARENT or
                AccessibilityEvent.WINDOWS_CHANGE_PIP
            if (changes and structural != 0) guard.invalidate(id)
        }
        // These system events often have no package name. Resolve the current
        // focused application window instead of silently dropping them.
        val focusedApps = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedApps.size > 1) guard.invalidate(id)
        val focusedPackage = focusedApps.singleOrNull()?.root?.packageName?.toString()
        if (focusedPackage != null && focusedPackage != packageName &&
            focusedPackage != session.target.packageName) guard.invalidate(id)
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        pendingId?.let { guard.cancel(it); guard.end(it) }
        targetNode = null
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        bubble = null
        instance = WeakReference(null)
        super.onDestroy()
    }

    fun isSessionActive(id: String): Boolean = pendingId == id && guard.session(id) != null

    fun cancelSession(id: String) {
        guard.cancel(id)
        if (pendingId != id) return
        guard.end(id)
        pendingId = null
        reviewVisible = false
        returningToEditor = false
        ownTransition = false
        targetNode = null
        status = "Session cancelled."
        renderBubble()
    }

    fun reviewStarted(id: String) {
        if (pendingId != id || guard.session(id) == null) return
        reviewVisible = true
    }

    fun requestInsert(id: String, output: String) {
        if (pendingId != id) return
        if (!guard.publish(id, output)) {
            status = "Session expired or changed. Start again."
            recoveryText = output
            pendingId = null
            reviewVisible = false
            returningToEditor = false
            ownTransition = false
            targetNode = null
            guard.end(id)
            renderBubble()
            return
        }
        recoveryText = output
        returningToEditor = true
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
        val target = observeEditor()
        if (target == null) {
            status = observationFailure
            renderBubble()
            return
        }
        val session = guard.begin(mode, target.snapshot, SystemClock.elapsedRealtime()) ?: return
        pendingId = session.id
        targetNode = target.node
        returnContinuity.capture(target.generation)
        recoveryText = null
        reviewVisible = false
        returningToEditor = false
        ownTransition = true
        targetHidden = false
        expanded = false
        renderBubble()
        val intent = Intent(this, SessionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SessionActivity.EXTRA_ID, session.id)
            putExtra(SessionActivity.EXTRA_MODE, mode.name)
        }
        try { startActivity(intent) } catch (_: Exception) {
            guard.cancel(session.id)
            guard.end(session.id)
            pendingId = null
            targetNode = null
            ownTransition = false
            status = "Could not open recording screen."
            renderBubble()
        }
    }

    private fun attemptInsertion(id: String) {
        if (pendingId != id) return
        val session = guard.session(id)
        if (session == null) {
            status = "Session ended; use Copy if a result is available."
            pendingId = null
            targetNode = null
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
        val continuousInput = observed != null && returnContinuity.allows(observed.generation)
        val decision = guard.reserveCommit(id, if (sameNode && continuousInput) observed?.snapshot else null,
            SystemClock.elapsedRealtime())
        if (decision != GuardDecision.Ready) {
            val detail = if (observed == null) observationFailure
                else (decision as GuardDecision.Refused).reason.toString()
            status = "Automatic insertion refused: $detail Use Copy or start again."
            pendingId = null
            targetNode = null
            guard.end(id)
            renderBubble()
            return
        }
        // Consumed before this IPC. A thrown exception or unconfirmed result is ambiguous: no retry.
        val connection = observed?.connection
        if (connection == null) {
            status = "Insertion outcome uncertain. Check the editor before any manual paste."
        } else {
            try {
                connection.commitText(session.result!!, 1, null)
                status = if (observed?.compositionState == CompositionState.UNKNOWN)
                    "Commit requested once; composition state was unreported. Check the editor before further input. KoScribe never sends."
                else "Commit requested once. Check the editor; KoScribe never sends."
            } catch (_: Exception) {
                status = "Insertion outcome uncertain. Check the editor before any manual paste."
            }
        }
        pendingId = null
        targetNode = null
        guard.end(id)
        renderBubble()
    }

    private data class Observation(
        val snapshot: EditorSnapshot,
        val node: AccessibilityNodeInfo,
        val connection: android.accessibilityservice.InputMethod.AccessibilityInputConnection,
        val generation: Int,
        val compositionState: CompositionState
    )

    private fun observeEditor(): Observation? {
        observationFailure = "Editor unavailable or unsafe. Focus a normal text field with a collapsed caret."
        val locked = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        if (locked) return null
        val method = inputMethod as? TrackingInputMethod ?: return null
        if (!method.currentInputStarted) return null
        val info = method.currentInputEditorInfo ?: return null
        val connection = method.currentInputConnection ?: return null
        if (!safeInputType(info.inputType)) return null
        val focusedWindows = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedWindows.size != 1) return null
        val focus = focusedWindows.single().root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.takeIf { it.isFocused && it.isEditable &&
                it.packageName?.toString() == info.packageName } ?: return null
        if (focus.isPassword || !safeInputType(focus.inputType)) return null
        val unique = focus.uniqueId?.takeIf(String::isNotBlank) ?: "source-node"
        val surrounding = try {
            connection.getSurroundingText(64, 64, InputConnection.GET_TEXT_WITH_STYLES)
        } catch (_: Exception) { null } ?: return null
        val text = surrounding.text
        val composing = (text as? Spanned)?.let { spanText ->
            spanText.getSpans(0, spanText.length, Any::class.java)
                .any { spanText.getSpanFlags(it) and Spanned.SPAN_COMPOSING != 0 }
        } ?: false
        val caret = surrounding.offset + surrounding.selectionStart
        if (surrounding.offset < 0 || surrounding.selectionStart != surrounding.selectionEnd ||
            focus.textSelectionStart != caret || focus.textSelectionEnd != caret) return null
        val composition = method.compositionAt(caret)
        if (composition == CompositionState.COMPOSING || composing) {
            observationFailure = "Active SwiftKey composition detected. Finish or cancel it before using KoScribe."
            return null
        }
        if (composition == CompositionState.SELECTION_MISMATCH) {
            observationFailure = "Editor selection report changed. Move the caret and try again."
            return null
        }
        val snapshot = EditorSnapshot(
            info.packageName, focus.windowId, unique, focus.viewIdResourceName,
            focus.className?.toString() ?: return null, info.fieldId, info.inputType,
            text.toString(), surrounding.offset, surrounding.selectionStart,
            surrounding.selectionEnd, composing
        )
        return if (snapshot.isSafe()) Observation(snapshot, focus, connection, method.generation,
            composition) else null
    }

    private fun safeInputType(type: Int): Boolean {
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        val variation = type and InputType.TYPE_MASK_VARIATION
        return variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    private fun copyRecovery() {
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
            setBackgroundColor(0xFFF7F7F7.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            elevation = dp(4).toFloat()
        }
        if (expanded) {
            holder.addView(caption("KoScribe · sample only", true))
            holder.addView(caption(status))
            for (mode in OutputMode.entries) holder.addView(action(mode.label) { startMode(mode) })
            if (recoveryText != null) holder.addView(action("Copy result manually") { copyRecovery() })
            holder.addView(action("Close") { expanded = false; renderBubble() })
        } else {
            holder.addView(action("Ko") { expanded = true; renderBubble() })
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
