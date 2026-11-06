package com.koscribe.prototype

import android.accessibilityservice.InputMethod
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import android.text.Spanned
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

internal object RelativeEmptyInsertMode {
    const val PACKAGE = "com.koscribe.smoke"
    val enabled: Boolean get() = BuildConfig.DEBUG && BuildConfig.RELATIVE_EMPTY_INSERT
}

/** Owned-only relative observer. The service owns the shared single commit path. */
internal class RelativeEmptyInsert(
    private val service: KoScribeService,
    private val input: () -> Input?,
    private val evidence: () -> RelativeEmptyPolicy.Selection?,
    private val safeType: (Int) -> Boolean
) {
    data class Input(val generation: Int, val info: EditorInfo,
        val connection: InputMethod.AccessibilityInputConnection)
    data class Frame(val input: Input, val root: AccessibilityNodeInfo, val node: AccessibilityNodeInfo,
        val window: Int, val field: Int, val type: Int, val autofill: Any?,
        val nodeClass: String, val nodeViewId: String?, val nodeUnique: String?,
        val proof: RelativeEmptyPolicy.Attempt)
    @Volatile private var outcome = "STATE=IDLE REASON=NONE ORIGIN=NONE"
    @Volatile private var reads = 0
    @Volatile private var lastPostRead: RelativeEmptyPolicy.PostRead? = null
    @Volatile private var eventWitness = RelativeEmptyPolicy.EventWitness.NONE
    @Volatile private var confirmationSource = RelativeEmptyPolicy.ConfirmationSource.NONE
    fun recordPostRead(result: String, sample: RelativeEmptyPolicy.Sample?) {
        lastPostRead = RelativeEmptyPolicy.classifyPostRead(result, sample)
    }
    fun record(state: String, reason: String, frame: Frame? = null) {
        if (frame != null) recordEvent(frame)
        outcome = "STATE=$state REASON=$reason ORIGIN=${frame?.proof?.admissionSource ?: "NONE"}"
    }
    fun recordEvent(frame: Frame) {
        eventWitness = frame.proof.eventWitness
        confirmationSource = frame.proof.confirmationSource
    }
    fun status(): String {
        val latest = evidence()
        val shape = latest?.let { RelativeReadPolicy.shape(it.start, it.end).name } ?: "UNKNOWN"
        val position = when {
            latest == null || latest.start < 0 || latest.end < 0 -> "UNKNOWN"
            latest.start != latest.end -> "RANGE"
            latest.start == 0 -> "ORIGIN"
            else -> "NONZERO"
        }
        return "RELATIVE_EMPTY_V1 ${if (RelativeEmptyInsertMode.enabled) outcome else "STATE=DISABLED REASON=FLAG_OFF ORIGIN=NONE"}" +
            " READS=$reads CURRENT_SOURCE=${latest?.source ?: "NONE"} CURRENT_SHAPE=$shape" +
            " CURRENT_POSITION=$position CURRENT_COMPOSITION=${latest?.composition ?: "UNKNOWN"}" +
            " GENERATION=${latest?.generation ?: -1} UPDATES=${latest?.updates ?: 0}" +
            " UNRESOLVED=${if (latest?.unresolvedMutation == true) "YES" else "NO"}" +
            " LAST_POSTREAD=${lastPostRead ?: "NONE"}" +
            " EVENT_WITNESS=$eventWitness CONFIRMATION_SOURCE=$confirmationSource" +
            " SCOPE=SIGNED_FIXTURE_ONLY"
    }
    private fun ready(): Boolean = RelativeEmptyInsertMode.enabled &&
        Process.myUserHandle() == UserHandle.getUserHandleForUid(Process.ROOT_UID) &&
        (service.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive &&
        !(service.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
    private fun autofill(info: EditorInfo): Any? = if (Build.VERSION.SDK_INT >= 36) info.autofillId else null
    private fun nodeSafe(node: AccessibilityNodeInfo, window: Int): Boolean = node.windowId == window &&
        node.packageName?.toString() == RelativeEmptyInsertMode.PACKAGE && node.isEnabled &&
        node.isVisibleToUser && !node.isPassword && (node.inputType == 0 || safeType(node.inputType))
    private fun focusedWindow(): AccessibilityWindowInfo? = service.windows.filter {
        it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
    }.singleOrNull()?.takeIf { it.displayId == Display.DEFAULT_DISPLAY }

    fun capture(result: String): Frame? {
        lastPostRead = null
        eventWitness = RelativeEmptyPolicy.EventWitness.NONE
        confirmationSource = RelativeEmptyPolicy.ConfirmationSource.NONE
        return runCatching { captureFrame(result) }.getOrElse {
            record("REFUSED", "METADATA_UNAVAILABLE")
            null
        }
    }

    private fun captureFrame(result: String): Frame? {
        val started = SystemClock.uptimeMillis()
        record("REFUSED", "CAPTURE")
        if (!ready() || !RelativeEmptyPolicy.resultAllowed(result) ||
            service.packageManager.checkSignatures(service.packageName, RelativeEmptyInsertMode.PACKAGE) !=
                PackageManager.SIGNATURE_MATCH) return null
        val served = input() ?: return null
        if (served.info.packageName != RelativeEmptyInsertMode.PACKAGE || !safeType(served.info.inputType)) return null
        val field = served.info.fieldId
        val type = served.info.inputType
        val autofill = autofill(served.info)
        val selection = evidence()?.takeIf { it.generation == served.generation } ?: return null
        if (!RelativeEmptyPolicy.admits(selection)) { record("REFUSED", "SELECTION_EVIDENCE"); return null }
        val window = focusedWindow() ?: return null
        val root = window.getRoot(0) ?: return null
        if (!root.refresh() || root.windowId != window.id || root.packageName?.toString() != RelativeEmptyInsertMode.PACKAGE)
            return null
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (!node.refresh() || !nodeSafe(node, window.id)) return null
        val frame = Frame(served, root, node, window.id, field, type,
            autofill, node.className?.toString()?.takeIf { it.isNotBlank() } ?: return null,
            node.viewIdResourceName, node.uniqueId, RelativeEmptyPolicy.Attempt(selection, result, started))
        val first = read(frame, 1, 1, precommit = true)
        if (!frame.proof.captureFirst(first, SystemClock.uptimeMillis())) { record("REFUSED", "FIRST_READ", frame); return null }
        record("CAPTURED", "EMPTY_RELATIVE", frame)
        return frame
    }

    private fun currentMatches(frame: Frame): Boolean {
        if (!ready()) return false
        val now = input() ?: return false
        return now.generation == frame.input.generation && now.info === frame.input.info &&
            now.info.packageName == RelativeEmptyInsertMode.PACKAGE && now.info.fieldId == frame.field &&
            now.info.inputType == frame.type && safeType(now.info.inputType) && autofill(now.info) == frame.autofill
    }
    fun stable(frame: Frame): Boolean {
        if (!currentMatches(frame)) return false
        val window = focusedWindow() ?: return false
        val root = window.getRoot(0) ?: return false
        if (window.id != frame.window || root != frame.root || !root.refresh() || root.windowId != frame.window ||
            root.packageName?.toString() != RelativeEmptyInsertMode.PACKAGE) return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return node == frame.node && node.refresh() && nodeSafe(node, frame.window) &&
            node.className?.toString() == frame.nodeClass && node.viewIdResourceName == frame.nodeViewId &&
            node.uniqueId == frame.nodeUnique && currentMatches(frame)
    }

    /** Current source identity only; never inspect event text or treat a parent as the field. */
    fun eventBinding(frame: Frame, event: AccessibilityEvent): RelativeEmptyPolicy.EventBinding = runCatching {
        if (event.packageName?.toString() != RelativeEmptyInsertMode.PACKAGE || event.windowId != frame.window)
            return RelativeEmptyPolicy.EventBinding.OTHER_SOURCE
        if (!frame.proof.postcommitLive(SystemClock.uptimeMillis()) || !stable(frame) ||
            !frame.proof.postcommitLive(SystemClock.uptimeMillis()))
            return RelativeEmptyPolicy.EventBinding.TARGET_CHANGED
        val source = event.getSource(0)
        if (!frame.proof.postcommitLive(SystemClock.uptimeMillis()) || !stable(frame) ||
            !frame.proof.postcommitLive(SystemClock.uptimeMillis()))
            return RelativeEmptyPolicy.EventBinding.TARGET_CHANGED
        when {
            source == null -> RelativeEmptyPolicy.EventBinding.NO_SOURCE
            source == frame.node && source.windowId == frame.window &&
                source.packageName?.toString() == RelativeEmptyInsertMode.PACKAGE -> RelativeEmptyPolicy.EventBinding.MATCH
            else -> RelativeEmptyPolicy.EventBinding.OTHER_SOURCE
        }
    }.getOrDefault(RelativeEmptyPolicy.EventBinding.TARGET_CHANGED)

    fun read(frame: Frame, before: Int, after: Int, precommit: Boolean): RelativeEmptyPolicy.Sample? = runCatching {
        fun live(): Boolean = if (precommit) frame.proof.precommitLive(SystemClock.uptimeMillis())
            else frame.proof.postcommitLive(SystemClock.uptimeMillis())
        if (!live() || !stable(frame) || !live()) return null
        reads++
        val surrounding = frame.input.connection.getSurroundingText(before, after, InputConnection.GET_TEXT_WITH_STYLES)
            ?: return null
        if (!live() || !stable(frame) || !live()) return null
        val text = surrounding.text
        // Selected text can exceed the requested bounds. Never retain oversized context.
        if (text.length > 514 || (precommit && text.isNotEmpty())) return null
        val composing = (text as? Spanned)?.let { styled -> styled.getSpans(0, styled.length, Any::class.java)
            .any { styled.getSpanFlags(it) and Spanned.SPAN_COMPOSING != 0 } } ?: false
        RelativeEmptyPolicy.Sample(text.toString(), surrounding.offset, surrounding.selectionStart,
            surrounding.selectionEnd, composing)
    }.getOrNull()
}
