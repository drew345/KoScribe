package com.koscribe.prototype

import android.accessibilityservice.InputMethod
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal object RelativeReadProbeMode {
    val enabled: Boolean get() = BuildConfig.DEBUG && BuildConfig.RELATIVE_READ_PROBE_ARM != "OFF"
    val selectionOnly: Boolean get() = enabled && RelativeProbeAccess.selectionOnly(BuildConfig.RELATIVE_READ_PROBE_ARM)
}

/** A deliberate one-read diagnostic. This class has no draft, microphone, clipboard or writer API. */
internal class RelativeReadProbe(
    private val service: KoScribeService,
    private val input: () -> Input?,
    private val idle: () -> Boolean,
    private val safeType: (Int) -> Boolean
) {
    data class Input(val generation: Int, val info: EditorInfo,
        val connection: InputMethod.AccessibilityInputConnection)
    private data class Frame(val input: Input, val window: Int,
        val root: AccessibilityNodeInfo, val node: AccessibilityNodeInfo,
        val field: Int, val type: Int, val autofill: Any?,
        val selection: RelativeReadPolicy.Selection?)
    private class Token(val deadline: Long) {
        val terminal = AtomicBoolean(false)
        val workerPending = AtomicBoolean(false)
        val call = AtomicReference("NOT_STARTED")
        val code = AtomicReference("STATE=RUNNING REASON=QUEUED ELIGIBLE=NEVER")
        var receiver: BroadcastReceiver? = null // main thread only
    }
    private val handler = Handler(Looper.getMainLooper())
    private val active = AtomicReference<Token?>(null)
    private val last = AtomicReference<Token?>(null)
    private val selections = RelativeSelectionEvidence() // main thread only
    private val calibration = RelativeSelectionCalibration()
    @Volatile private var readiness = "CURRENT_INPUT_STARTED=NO CURRENT_INPUT_PACKAGE=NONE"
    private val target: String get() = if (BuildConfig.RELATIVE_READ_PROBE_ARM in setOf("FIXTURE", "FIXTURE_SELECTION"))
        "com.koscribe.smoke" else "com.towneers.www"

    fun inputStarted(generation: Int, info: EditorInfo, restarting: Boolean) {
        if (!RelativeReadProbeMode.enabled) return
        interrupt("INPUT_START")
        readiness = "CURRENT_INPUT_STARTED=YES CURRENT_INPUT_PACKAGE=" + when (info.packageName) {
            "com.koscribe.smoke" -> "FIXTURE"
            "com.towneers.www" -> "KARROT"
            else -> "OTHER"
        }
        if (RelativeReadProbeMode.selectionOnly) {
            val packageKind = when (info.packageName) {
                "com.koscribe.smoke" -> RelativeSelectionCalibration.Package.FIXTURE
                "com.towneers.www" -> RelativeSelectionCalibration.Package.KARROT
                else -> RelativeSelectionCalibration.Package.OTHER
            }
            if (info.packageName == target && safeType(info.inputType))
                calibration.start(generation, packageKind, restarting, info.initialSelStart,
                    info.initialSelEnd, SystemClock.uptimeMillis())
            else calibration.outOfScope(packageKind)
            return
        }
        if (info.packageName == target)
            selections.start(generation, info.initialSelStart, info.initialSelEnd, SystemClock.uptimeMillis())
        else selections.clear()
    }
    fun inputFinished() {
        if (!RelativeReadProbeMode.enabled) return
        interrupt("INPUT_FINISH")
        readiness = "CURRENT_INPUT_STARTED=NO CURRENT_INPUT_PACKAGE=NONE"
        if (RelativeReadProbeMode.selectionOnly) calibration.finish()
        selections.clear()
    }
    fun selection(generation: Int, start: Int, end: Int, candidateStart: Int, candidateEnd: Int) {
        if (!RelativeReadProbeMode.enabled) return
        if (RelativeReadProbeMode.selectionOnly) {
            calibration.update(generation, start, end, candidateStart, candidateEnd, SystemClock.uptimeMillis())
            return
        }
        selections.update(generation, start, end, candidateStart, candidateEnd, SystemClock.uptimeMillis())
        interrupt("INPUT_SELECTION")
    }
    fun event(event: AccessibilityEvent) {
        if (!RelativeReadProbeMode.enabled) return
        if (RelativeReadProbeMode.selectionOnly) {
            if (event.packageName?.toString() == target) {
                val signal = when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> RelativeSelectionCalibration.Signal.TEXT
                    AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> RelativeSelectionCalibration.Signal.SELECTION
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> RelativeSelectionCalibration.Signal.CONTENT
                    else -> RelativeSelectionCalibration.Signal.NONE
                }
                calibration.signal(signal)
            }
            return
        }
        val reason = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "VIEW_TEXT_SELECTION_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
            else -> return
        }
        if (event.packageName?.toString() == target && event.eventType in setOf(
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) selections.signal()
        interrupt(reason)
    }

    fun interrupt(reason: String) {
        active.get()?.let { finish(it, "INTERRUPTED", reason) }
    }

    fun status(): String = if (!RelativeReadProbeMode.enabled) "RELATIVE_READ_V1 STATE=DISABLED" else
        (last.get()?.let { "RELATIVE_READ_V1 ${it.code.get()} CALL=${it.call.get()}" }
            ?: "RELATIVE_READ_V1 STATE=NO_REQUEST ELIGIBLE=NEVER CALL=NOT_STARTED") + " $readiness"

    /** Only an immutable cached callback snapshot and a local clock; no input() or Android editor access. */
    fun selectionStatus(): String = if (RelativeReadProbeMode.selectionOnly)
        calibration.snapshot().code(SystemClock.uptimeMillis())
        else "RELATIVE_SELECTION_V1 STATE=DISABLED CONTENT_READ=DISABLED ELIGIBLE=NEVER"

    /** Called only by the explicit dump command. Status and idle observation never read text. */
    fun request(): String = RelativeProbeAccess.requestContent(BuildConfig.DEBUG,
        BuildConfig.RELATIVE_READ_PROBE_ARM,
        if (RelativeReadProbeMode.selectionOnly)
            "RELATIVE_READ_V1 STATE=REFUSED REASON=SELECTION_ONLY CONTENT_READ=DISABLED ELIGIBLE=NEVER CALL=NOT_STARTED"
        else status()) { requestRead() }

    private fun requestRead(): String {
        val token = Token(SystemClock.uptimeMillis() + 3000L)
        if (!active.compareAndSet(null, token)) return "RELATIVE_READ_V1 STATE=BUSY ELIGIBLE=NEVER"
        last.set(token)
        if (!handler.post {
                runCatching { start(token) }.onFailure { finish(token, "UNAVAILABLE", "PREFLIGHT_FAILED") }
            }) finish(token, "UNAVAILABLE", "POST_FAILED")
        if (!handler.postDelayed({ finish(token, "TIMEOUT", "DEADLINE") }, 3000L))
            finish(token, "UNAVAILABLE", "TIMER_FAILED")
        return status()
    }

    private fun live(token: Token): Boolean = active.get() === token && !token.terminal.get() &&
        SystemClock.uptimeMillis() < token.deadline

    private fun finish(token: Token, state: String, reason: String, observation: String? = null) {
        if (token.terminal.compareAndSet(false, true)) {
            token.code.set("STATE=$state REASON=$reason " + (observation ?: "ELIGIBLE=NEVER"))
            val cleanup = Runnable {
                token.receiver?.let { runCatching { service.unregisterReceiver(it) } }
                token.receiver = null
                if (!token.workerPending.get()) active.compareAndSet(token, null)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) cleanup.run() else handler.post(cleanup)
        }
    }

    private fun deviceReady(): Boolean =
        (service.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive &&
            !(service.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    private fun autofill(info: EditorInfo): Any? = if (Build.VERSION.SDK_INT >= 36) info.autofillId else null

    private fun nodeSafe(node: AccessibilityNodeInfo, window: Int): Boolean =
        node.windowId == window && node.packageName?.toString() == target &&
            node.isEnabled && node.isVisibleToUser && !node.isPassword &&
            (node.inputType == 0 || safeType(node.inputType))

    private fun emptyScope(selection: RelativeReadPolicy.Selection?, info: EditorInfo,
        node: AccessibilityNodeInfo): Boolean = BuildConfig.RELATIVE_READ_PROBE_ARM == "FIXTURE" ||
        (RelativeReadPolicy.permitsEmptyRead(selection) &&
            (selection != null ||
                KarrotEmptyReadPolicy.permitsPreReadSelection(info.initialSelStart, info.initialSelEnd)) &&
            KarrotEmptyReadPolicy.permitsPreReadSelection(node.textSelectionStart, node.textSelectionEnd))

    private fun start(token: Token) {
        if (!live(token)) return
        if (Process.myUserHandle() != UserHandle.getUserHandleForUid(Process.ROOT_UID) ||
            !idle() || !deviceReady()) return finish(token, "REFUSED", "SERVICE_OR_DEVICE_NOT_READY")
        if (BuildConfig.RELATIVE_READ_PROBE_ARM == "FIXTURE" &&
            service.packageManager.checkSignatures(service.packageName, target) != PackageManager.SIGNATURE_MATCH)
            return finish(token, "REFUSED", "FIXTURE_SIGNER")
        val served = input() ?: return finish(token, "REFUSED", "NO_INPUT")
        if (served.info.packageName != target || !safeType(served.info.inputType))
            return finish(token, "REFUSED", "EDITOR_SCOPE_OR_TYPE")
        val originalField = served.info.fieldId
        val originalType = served.info.inputType
        val originalAutofill = autofill(served.info)
        val window = service.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }.singleOrNull() ?: return finish(token, "REFUSED", "FOCUSED_WINDOW")
        if (window.displayId != Display.DEFAULT_DISPLAY)
            return finish(token, "REFUSED", "DISPLAY")
        val root = window.getRoot(0) ?: return finish(token, "REFUSED", "NO_ROOT")
        if (!root.refresh() || root.windowId != window.id || root.packageName?.toString() != target)
            return finish(token, "REFUSED", "ROOT")
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return finish(token, "REFUSED", "NO_INPUT_NODE")
        if (!node.refresh() || !nodeSafe(node, window.id))
            return finish(token, "REFUSED", "NODE_SCOPE_OR_TYPE")
        val selected = selections.get(served.generation)
        if (!emptyScope(selected, served.info, node))
            return finish(token, "REFUSED", "EMPTY_SCOPE_SELECTION")
        val frame = Frame(served, window.id, root, node, originalField,
            originalType, originalAutofill, selected)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) finish(token, "INTERRUPTED", "SCREEN_OFF")
            }
        }
        token.receiver = receiver
        if (runCatching { service.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF),
                Context.RECEIVER_NOT_EXPORTED) }.isFailure)
            return finish(token, "UNAVAILABLE", "SCREEN_WATCH")
        // Drain notifications produced by the metadata reads before the sole content request.
        if (!handler.post {
                if (live(token)) {
                    if (!runCatching { stable(frame) }.getOrDefault(false))
                        finish(token, "INTERRUPTED", "PREREAD_CHANGED")
                    else if (!handler.post {
                            if (live(token)) {
                                if (!runCatching { quickStable(frame) }.getOrDefault(false))
                                    finish(token, "INTERRUPTED", "PREREAD_CHANGED")
                                else read(token, frame)
                            }
                        }) finish(token, "UNAVAILABLE", "POST_FAILED")
                }
            }) finish(token, "UNAVAILABLE", "POST_FAILED")
    }

    private fun quickStable(frame: Frame): Boolean {
        if (!idle() || !deviceReady()) return false
        val now = input() ?: return false
        return now.generation == frame.input.generation && now.info === frame.input.info &&
            now.info.packageName == target && now.info.fieldId == frame.field && now.info.inputType == frame.type &&
            safeType(now.info.inputType) && nodeSafe(frame.node, frame.window) &&
            autofill(now.info) == frame.autofill && selections.get(now.generation) == frame.selection &&
            emptyScope(frame.selection, now.info, frame.node)
    }

    private fun stable(frame: Frame): Boolean {
        if (!quickStable(frame)) return false
        val window = service.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }.singleOrNull() ?: return false
        if (window.id != frame.window || window.displayId != Display.DEFAULT_DISPLAY) return false
        val root = window.getRoot(0) ?: return false
        if (root != frame.root || !root.refresh() || root.windowId != frame.window ||
            root.packageName?.toString() != target) return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return node == frame.node && node.refresh() && nodeSafe(node, frame.window) && quickStable(frame)
    }

    private fun read(token: Token, frame: Frame) {
        if (!live(token)) return
        token.workerPending.set(true)
        val worker = runCatching { Thread({
            var outcome: String? = null
            var reason = "READ_UNAVAILABLE"
            try {
                // Device-state queries can block on IPC; recheck the fixed deadline afterwards.
                if (live(token) && deviceReady() && live(token) &&
                    token.call.compareAndSet("NOT_STARTED", "ATTEMPTED")) {
                    val surrounding = frame.input.connection.getSurroundingText(1, 1, InputConnection.GET_TEXT_WITH_STYLES)
                    token.call.set("RETURNED")
                    if (live(token) && surrounding != null) {
                        val text = surrounding.text
                        val length = text.length
                        // The API includes all selected text despite bounded before/after requests.
                        // External nonempty or oversized content is never inspected, copied or retained.
                        val inspect = length <= 64 && (length == 0 || BuildConfig.RELATIVE_READ_PROBE_ARM == "FIXTURE")
                        val style = when {
                            !inspect -> RelativeReadPolicy.Style.NOT_INSPECTED
                            text !is Spanned -> RelativeReadPolicy.Style.UNSTYLED
                            text.getSpans(0, length, Any::class.java).any {
                                text.getSpanFlags(it) and Spanned.SPAN_COMPOSING != 0
                            } -> RelativeReadPolicy.Style.COMPOSING
                            else -> RelativeReadPolicy.Style.NO_COMPOSING_REPORTED
                        }
                        outcome = RelativeReadPolicy.classify(length, surrounding.offset,
                            surrounding.selectionStart, surrounding.selectionEnd, style,
                            frame.selection, SystemClock.uptimeMillis())
                        reason = if (BuildConfig.RELATIVE_READ_PROBE_ARM == "KARROT" && length != 0)
                            "NONEMPTY_DISCARDED" else if (length > 64) "OVERSIZE_DISCARDED" else "READ_COMPLETE"
                    }
                }
            } catch (_: Throwable) {
                token.call.compareAndSet("ATTEMPTED", "THREW")
                reason = "READ_FAILED"
            } finally {
                // Keep the busy latch until the IPC really returned, including after timeout.
                token.workerPending.set(false)
                val fixed = outcome
                val fixedReason = reason
                if (!handler.post {
                        if (live(token)) {
                            val same = runCatching { stable(frame) }.getOrDefault(false)
                            if (!live(token)) finish(token, "TIMEOUT", "DEADLINE")
                            else if (!same) finish(token, "INTERRUPTED", "POSTREAD_CHANGED")
                            else if (fixed == null) finish(token, "UNAVAILABLE", fixedReason)
                            else finish(token, "OBSERVED", fixedReason, fixed)
                        } else if (!token.terminal.get()) finish(token, "TIMEOUT", "DEADLINE")
                        active.compareAndSet(token, null)
                    }) {
                    finish(token, "UNAVAILABLE", "POST_FAILED")
                    active.compareAndSet(token, null)
                }
            }
        }, "KoScribe-relative-read") }.getOrElse {
            token.workerPending.set(false)
            finish(token, "UNAVAILABLE", "WORKER_FAILED")
            return
        }
        if (runCatching { worker.start() }.isFailure) {
            token.workerPending.set(false)
            finish(token, "UNAVAILABLE", "WORKER_FAILED")
        }
    }
}
