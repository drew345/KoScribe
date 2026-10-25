package com.koscribe.prototype

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Insets
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.PersistableBundle
import android.os.PowerManager
import android.os.SystemClock
import android.text.InputType
import android.text.Spanned
import android.view.Gravity
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import java.io.FileDescriptor
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.UUID

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
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private val idleBubblePolicy = IdleBubblePolicy()
    private var idleBubbleVisible = false
    private var idleRefreshQueued = false
    private var idleDragging = false
    private var idleBubbleGeometry: IdleBubbleGeometry? = null
    private var serviceConnected = false
    private enum class IdleRefreshReason {
        NONE, SERVICE_CONNECTED, INPUT_STARTED, INPUT_FINISHED, WINDOW_EVENT,
        FOCUS_EVENT, OTHER_EVENT, OTHER_SCHEDULED, DIRECT
    }
    private enum class IdleRefreshGate {
        NONE, ARM_OFF, DISCONNECTED, NO_WINDOW_MANAGER, RECORDING,
        DESTINATION, DRAFT_REVIEW, DRAFT_RESULT, IDLE_CHECKED
    }
    private enum class IdleCheckStage {
        NOT_CHECKED, LOCKED, WINDOWS_UNAVAILABLE, FOCUSED_APP_COUNT,
        FOCUSED_APP_DISPLAY, IME_MULTIPLE_OR_WRONG_DISPLAY, IME_ABSENT,
        IME_BOUNDS_EMPTY, INPUT_METHOD_UNAVAILABLE, INPUT_NOT_STARTED,
        EDITOR_INFO_UNAVAILABLE, UNSAFE_INPUT_TYPE, ROOT_PACKAGE_UNAVAILABLE,
        PACKAGE_MISMATCH, READY, CHECK_ERROR
    }
    private enum class IdleRenderOutcome {
        NOT_ATTEMPTED, BLOCKED_RECORDING, BLOCKED_DESTINATION, REMOVE_FAILED,
        HIDDEN_BY_POLICY_OR_REVIEW, AREA_TOO_SMALL, AWAIT_LAYOUT,
        FRAME_CHANGED, PLACEMENT_FAILED, VISIBLE, ADD_OR_SETUP_FAILED
    }
    private data class IdleRefreshSnapshot(
        val count: Int, val reason: IdleRefreshReason, val gate: IdleRefreshGate,
        val stage: IdleCheckStage, val ime: IdleBubblePolicy.Ime,
        val connected: Boolean, val inputStarts: Int,
        val unlocked: Boolean?, val wanted: Boolean?, val suppressed: Boolean,
        val policyVisible: Boolean, val hostExists: Boolean,
        val hostAttached: Boolean, val hostShown: Boolean
    ) {
        fun code(): String = "count=$count reason=$reason gate=$gate stage=$stage ime=$ime" +
            " connected=$connected inputStarts=$inputStarts" +
            " unlocked=${unlocked ?: "UNKNOWN"} wanted=${wanted ?: "UNKNOWN"}" +
            " suppressed=$suppressed policyVisible=$policyVisible" +
            " hostExists=$hostExists attached=$hostAttached shown=$hostShown"
    }
    private var idleRefreshReason = IdleRefreshReason.OTHER_SCHEDULED
    private var idleRefreshCount = 0
    private var idleInputStartCount = 0
    @Volatile private var lastIdleRefresh: IdleRefreshSnapshot? = null
    @Volatile private var idleRenderCount = 0
    @Volatile private var idleRenderOutcome = IdleRenderOutcome.NOT_ATTEMPTED
    private val bubblePrefs by lazy { getSharedPreferences("bubble_position", Context.MODE_PRIVATE) }
    private var status = "Focus a text field, then choose a mode."
    private var compactResultNotice: String? = null
    private var recoveryText: String? = null
    private var pendingId: String? = null
    private var reviewVisible = false
    private var returningToEditor = false
    private var ownTransition = false
    private var targetNode: AccessibilityNodeInfo? = null
    private var targetRootNode: AccessibilityNodeInfo? = null
    private val returnContinuity = ReturnContinuity()
    private var modalOcclusion: ModalOcclusionGate? = null
    private var overlayPanel: OverlayRecordingPanel? = null
    private var overlayCleanupPending: OverlayRecordingPanel? = null
    private var overlayScreenReceiver: BroadcastReceiver? = null
    private var overlayHeartbeat: Runnable? = null
    private var overlayInputGeneration: Int? = null
    private val unboundRecording = UnboundRecordingState()
    private val unboundDraft = UnboundDraftState()
    private val speechGate = SpeechTransferGate()
    private var speechJob: SpeechTranscriber.Job? = null
    private var translationJob: KoreanTranslator.Job? = null
    private var translationGeneration = 0
    private var translationStartedUptimeMs: Long? = null
    private var translationRequests = 0
    private var translationSuccesses = 0
    private var translationErrors = 0
    private var translationCancels = 0
    private var translationLastDurationMs: Long? = null
    private var translationLastUsage: KoreanTranslator.Usage? = null
    private var speechStopEntryUptimeMs: Long? = null
    private var speechLastStopToReviewMs: Long? = null
    private var speechKeyCheckGeneration = 0
    private var speechKeyCheckPending = false
    private var draftReviewOwner: Any? = null
    private var draftScreenReceiver: BroadcastReceiver? = null
    private var draftDeadline: Runnable? = null
    private data class ReviewEntryWait(
        val draftId: String,
        val owner: Any,
        val decor: View,
        val editor: android.widget.EditText,
        val token: IBinder,
        val windowId: Int,
        val startedMs: Long,
        var checks: Int = 0,
        var callback: Runnable? = null
    )
    private data class AutoDestinationIntent(
        val nonce: String,
        val mode: OutputMode,
        val policy: OriginalDestinationIntent,
        val original: EditorSnapshot,
        val node: AccessibilityNodeInfo,
        val root: AccessibilityNodeInfo,
        val generation: Int,
        val benignWindowIds: MutableSet<Int>,
        var recordingId: String? = null,
        var draftId: String? = null,
        var reviewOwner: Any? = null,
        var reviewWindowId: Int? = null,
        var reviewRoot: AccessibilityNodeInfo? = null,
        var reviewNode: AccessibilityNodeInfo? = null,
        var entryWait: ReviewEntryWait? = null,
        var expiry: Runnable? = null,
        var returnPoll: Runnable? = null,
        var returnStartedMs: Long? = null,
        var returnAttempted: Boolean = false
    )
    private var autoDestination: AutoDestinationIntent? = null
    private var autoFallbackDraftId: String? = null
    private var autoLastReason = "NONE"
    private var autoLastPhase = "NONE"
    private var autoCapturedCount = 0
    private var autoAttemptCount = 0
    private var autoFallbackCount = 0
    private enum class DestinationPhase { PRECOMMIT, OBSERVING }
    private data class FreshDestination(
        val draftId: String,
        val id: String,
        val node: AccessibilityNodeInfo,
        val root: AccessibilityNodeInfo,
        val generation: Int,
        val expected: CommitConfirmation.Expected,
        val host: View,
        val hostToken: IBinder?,
        val hostWindowId: Int,
        val autoIntent: AutoDestinationIntent? = null,
        var phase: DestinationPhase = DestinationPhase.PRECOMMIT,
        var commitRequestedUptimeMs: Long = 0L
    )
    private var freshDestination: FreshDestination? = null
    private enum class PostCommitProbeKind(val prefix: String) {
        CARET("POST_DIAG"), COMPOSITION("POST_COMPOSE")
    }
    private data class PostCommitProbe(
        val kind: PostCommitProbeKind,
        val draftId: String,
        val node: AccessibilityNodeInfo,
        val root: AccessibilityNodeInfo,
        val windowId: Int,
        val fieldId: Int,
        val inputType: Int,
        val generation: Int,
        val expectedOffset: Int,
        val expectedCaret: Int,
        val expectedLength: Int,
        val expectedDigest: ByteArray,
        val commitRequestedUptimeMs: Long,
        var startedUptimeMs: Long = 0L,
        var delayed: Runnable? = null
    )
    private var postCommitProbe: PostCommitProbe? = null

    private data class ComposingSpanMetrics(
        val count: Int, val firstStart: Int, val firstEnd: Int,
        val firstZeroLength: Boolean, val more: Boolean, val error: Boolean
    ) {
        private fun Boolean.bit(): Int = if (this) 1 else 0
        fun code(): String = "_SP${(count > 0).bit()}_SC${count}" +
            "_SS${firstStart}_SE${firstEnd}_SZ${firstZeroLength.bit()}" +
            "_SM${more.bit()}_SX${error.bit()}"
    }
    @Volatile private var commitRequestCount = 0
    @Volatile private var commitConfirmation = "NONE"
    @Volatile private var unboundRecordingProofCount = 0
    @Volatile private var destinationCaptureCount = 0

    private fun unboundArm(): Boolean = startupExperiment == StartupExperiment.RESELECT ||
        startupExperiment == StartupExperiment.REVIEW

    private fun autoEnabled(mode: OutputMode): Boolean = BuildConfig.DEBUG &&
        BuildConfig.AUTO_INSERT && startupExperiment == StartupExperiment.REVIEW &&
        !VisualTestMode.enabled && mode != OutputMode.ENGLISH

    /** Read-only intent at the mode click; no InputConnection or insertion session is retained. */
    private fun captureAutoIntent(mode: OutputMode) {
        if (!autoEnabled(mode) || pendingId != null || unboundRecording.current() != null ||
            unboundDraft.current() != null || SessionActivity.isVisible ||
            overlayCleanupPending != null) return
        val editorPackage = (inputMethod as? TrackingInputMethod)
            ?.currentInputEditorInfo?.packageName
        if (editorPackage != "com.koscribe.smoke") {
            autoLastReason = "CAPTURE_NON_FIXTURE"
            autoLastPhase = "NONE"
            return
        }
        val observed = observeEditor()
        if (observed == null) {
            autoLastReason = "CAPTURE_${observationCode.name}"
            autoLastPhase = "NONE"
            return
        }
        val policy = OriginalDestinationIntent.capture(observed.snapshot, observed.generation,
            SystemClock.elapsedRealtime(), mode)
        if (policy == null) {
            autoLastReason = "CAPTURE_INELIGIBLE"
            autoLastPhase = "NONE"
            return
        }
        val ownBubbleId = ownHostWindowId()
        val benignWindows = windows.filter {
            it.displayId == Display.DEFAULT_DISPLAY && !it.isFocused &&
                (it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD ||
                    (it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
                        it.id == ownBubbleId))
        }.mapTo(mutableSetOf()) { it.id }
        val binding = AutoDestinationIntent(UUID.randomUUID().toString(), mode, policy,
            observed.snapshot, observed.node, observed.root, observed.generation, benignWindows)
        autoDestination = binding
        autoFallbackDraftId = null
        val expiry = Runnable {
            if (autoDestination === binding) clearAutoIntent("INTENT_DEADLINE")
        }
        binding.expiry = expiry
        main.postDelayed(expiry, 180_000L)
        autoCapturedCount++
        autoLastReason = "NONE"
        autoLastPhase = policy.phase.name
        note("AUTO_INTENT_CAPTURED")
    }

    private fun clearAutoIntent(reason: String, fallback: Boolean = true,
        renderReady: Boolean = true) {
        val binding = autoDestination ?: return
        clearReviewEntryWait(binding)
        binding.expiry?.let(main::removeCallbacks)
        binding.expiry = null
        binding.returnPoll?.let(main::removeCallbacks)
        binding.returnPoll = null
        if (fallback && binding.policy.phase != OriginalDestinationIntent.Phase.CONSUMED) {
            binding.policy.invalidate(reason)
            autoFallbackDraftId = binding.draftId
            autoFallbackCount++
            autoLastPhase = "FALLBACK"
            autoLastReason = reason
            note("AUTO_FALLBACK_$reason")
            freshDestination?.takeIf {
                it.draftId == binding.draftId && it.phase == DestinationPhase.PRECOMMIT
            }?.let { guard.invalidate(it.id) }
            val ready = unboundDraft.current()?.takeIf {
                it.id == binding.draftId && it.phase == UnboundDraftState.Phase.READY
            }
            if (ready != null) {
                status = "Check the intended field and cursor, then tap Insert."
                compactResultNotice = "Automatic insertion unavailable. Choose the field, then tap Insert."
                if (renderReady && draftReviewOwner == null)
                    refreshIdleBubble(forceRender = true)
            }
        } else {
            autoLastPhase = binding.policy.phase.name
            autoLastReason = reason
        }
        autoDestination = null
        binding.draftId?.let(ReviewActivity::autoUnavailable)
    }

    private fun clearReviewEntryWait(binding: AutoDestinationIntent) {
        binding.entryWait?.let { wait ->
            wait.callback?.let(main::removeCallbacks)
            wait.callback = null
        }
        binding.entryWait = null
    }

    private fun autoForRecording(id: String): AutoDestinationIntent? =
        autoDestination?.takeIf { it.recordingId == id }

    private fun autoForDraft(id: String): AutoDestinationIntent? =
        autoDestination?.takeIf { it.draftId == id }

    private fun clearPendingAuto(reason: String) {
        if (autoDestination?.recordingId == null) clearAutoIntent(reason)
    }

    private fun autoGenerationClass(binding: AutoDestinationIntent, generation: Int): String =
        when (generation) {
            binding.generation -> "ORIGINAL"
            binding.generation + 1 -> "REVIEW"
            binding.generation + 2 -> "RETURN"
            else -> "OTHER"
        }

    private fun autoWindowType(window: AccessibilityWindowInfo?): String = when (window?.type) {
        null -> "ABSENT"
        AccessibilityWindowInfo.TYPE_APPLICATION -> "APP"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "IME"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "ACCESSIBILITY_OVERLAY"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "SYSTEM"
        else -> "OTHER"
    }

    /** One code-only snapshot at the first terminal event. Never reads node text or context. */
    private fun noteAutoEventRefusal(binding: AutoDestinationIntent, reason: String,
        event: AccessibilityEvent, expectedReviewWindowId: Int?,
        rootWasRead: Boolean = false, checkedRoot: AccessibilityNodeInfo? = null,
        sourceWasRead: Boolean = false, checkedSource: AccessibilityNodeInfo? = null) {
        try {
        val currentWindows = windows
        val current = currentWindows.firstOrNull { it.id == event.windowId }
        val localReviewWindowId = binding.draftId?.let { id ->
            binding.reviewOwner?.let { owner -> ReviewActivity.expectedWindowId(id, owner) }
        }
        val expectedSource = when {
            binding.reviewWindowId != null -> "BOUND"
            expectedReviewWindowId != null && binding.entryWait != null -> "LOCAL_DURING_WAIT"
            expectedReviewWindowId != null -> "LOCAL"
            binding.entryWait != null -> "ENTRY_ONLY"
            else -> "NONE"
        }
        val role = when {
            event.windowId == binding.original.windowId -> "ORIGINAL"
            expectedReviewWindowId != null && event.windowId == expectedReviewWindowId ->
                "EXPECTED_REVIEW"
            event.windowId in binding.benignWindowIds -> "KNOWN_BENIGN"
            else -> "OTHER"
        }
        val focused = currentWindows.filter { it.isFocused }
        val focusedApps = focused.count { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val focusClass = when {
            focused.isEmpty() -> "NONE"
            focused.size > 1 -> "MULTIPLE"
            focused[0].id == binding.original.windowId -> "ORIGINAL"
            expectedReviewWindowId != null && focused[0].id == expectedReviewWindowId ->
                "EXPECTED_REVIEW"
            focused[0].id in binding.benignWindowIds -> "KNOWN_BENIGN"
            else -> "OTHER_${autoWindowType(focused[0])}"
        }
        val rootClass = when {
            !rootWasRead -> "UNKNOWN"
            checkedRoot == null -> "NULL"
            checkedRoot == binding.root -> "ORIGINAL"
            binding.reviewRoot != null && checkedRoot == binding.reviewRoot -> "REVIEW"
            checkedRoot.packageName?.toString() == packageName -> "OWN_OTHER"
            else -> "OTHER"
        }
        val sourceClass = when {
            !sourceWasRead -> "UNKNOWN"
            checkedSource == null -> "NULL"
            checkedSource == binding.node -> "ORIGINAL"
            binding.reviewNode != null && checkedSource == binding.reviewNode -> "REVIEW"
            else -> "OTHER"
        }
        val eventClass = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "STATE"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUS"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "SELECTION"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "CONTENT"
            else -> "OTHER"
        }
        val packageClass = when (event.packageName?.toString()) {
            null -> "NONE"
            packageName -> "OWN"
            binding.original.packageName -> "ORIGINAL"
            else -> "OTHER"
        }
        val wait = binding.entryWait
        val waitAgeMs = wait?.let {
            (SystemClock.elapsedRealtime() - it.startedMs).coerceIn(0L, 2_000L)
        }
        val eventAgeMs = (SystemClock.uptimeMillis() - event.eventTime)
            .coerceIn(0L, 60_000L)
        fun bit(value: Boolean) = if (value) 1 else 0
        fun agreement(first: Int?, second: Int?): String = when {
            first == null || second == null -> "UNKNOWN"
            first == second -> "YES"
            else -> "NO"
        }
        note("AUTO_REFUSE_${reason}_PHASE_${binding.policy.phase.name}" +
            "_SNAP_POST_DECISION" +
            "_WAIT_${bit(wait != null)}_WAIT_C_${wait?.checks ?: 0}" +
            "_WAIT_MS_${waitAgeMs ?: "NONE"}_REVIEW_BOUND_${bit(binding.reviewWindowId != null)}" +
            "_EVENT_${eventClass}_AGE_MS_${eventAgeMs}" +
            "_WC_${event.windowChanges}_CT_${event.contentChangeTypes}" +
            "_ROLE_${role}_PACKAGE_${packageClass}_EXPECTED_SOURCE_${expectedSource}" +
            "_ID_ORIGINAL_${bit(event.windowId == binding.original.windowId)}" +
            "_ID_BOUND_${bit(binding.reviewWindowId != null &&
                event.windowId == binding.reviewWindowId)}" +
            "_ID_ENTRY_${bit(wait != null && event.windowId == wait.windowId)}" +
            "_ID_LOCAL_${bit(localReviewWindowId != null &&
                event.windowId == localReviewWindowId)}" +
            "_ID_BENIGN_${bit(event.windowId in binding.benignWindowIds)}" +
            "_BOUND_LOCAL_${agreement(binding.reviewWindowId, localReviewWindowId)}" +
            "_ENTRY_LOCAL_${agreement(wait?.windowId, localReviewWindowId)}" +
            "_PRESENT_${bit(current != null)}_TYPE_${autoWindowType(current)}" +
            "_FOCUSED_${bit(current?.isFocused == true)}_ACTIVE_${bit(current?.isActive == true)}" +
            "_DEFAULT_DISPLAY_${bit(current?.displayId == Display.DEFAULT_DISPLAY)}" +
            "_EXPECTED_ID_${bit(expectedReviewWindowId != null &&
                event.windowId == expectedReviewWindowId)}" +
            "_ORIGINAL_PRESENT_${bit(currentWindows.any { it.id == binding.original.windowId })}" +
            "_REVIEW_PRESENT_${bit(expectedReviewWindowId != null &&
                currentWindows.any { it.id == expectedReviewWindowId })}" +
            "_ROOT_${rootClass}_SOURCE_${sourceClass}" +
            "_FOCUS_ALL_${if (focused.size > 1) "MULTIPLE" else focused.size}" +
            "_FOCUS_APP_${if (focusedApps > 1) "MULTIPLE" else focusedApps}" +
            "_FOCUS_${focusClass}")
        } catch (_: Exception) {
            note("AUTO_REFUSE_${reason}_DIAGNOSTIC_UNAVAILABLE")
        }
    }

    private fun noteAutoStartRefusal(binding: AutoDestinationIntent,
        phase: OriginalDestinationIntent.Phase, info: EditorInfo,
        generation: Int, restarting: Boolean) {
        fun bit(value: Boolean) = if (value) 1 else 0
        val packageClass = when (info.packageName) {
            packageName -> "OWN"
            binding.original.packageName -> "ORIGINAL"
            else -> "OTHER"
        }
        note("AUTO_REFUSE_INPUT_START_PHASE_${phase.name}" +
            "_GEN_${autoGenerationClass(binding, generation)}_RESTART_${bit(restarting)}" +
            "_PACKAGE_${packageClass}_REVIEW_FIELD_${bit(info.fieldId == R.id.review_english)}" +
            "_ORIGINAL_FIELD_${bit(info.fieldId == binding.original.editorFieldId)}" +
            "_ORIGINAL_TYPE_${bit(info.inputType == binding.original.inputType)}" +
            "_DRAFT_BOUND_${bit(binding.draftId != null)}" +
            "_OWNER_BOUND_${bit(binding.reviewOwner != null)}")
    }

    private fun noteAutoFinishRefusal(binding: AutoDestinationIntent,
        phase: OriginalDestinationIntent.Phase, generation: Int, authenticatedOwn: Boolean) {
        fun bit(value: Boolean) = if (value) 1 else 0
        note("AUTO_REFUSE_INPUT_FINISH_PHASE_${phase.name}" +
            "_GEN_${autoGenerationClass(binding, generation)}" +
            "_OWN_AUTH_${bit(authenticatedOwn)}" +
            "_REVIEW_BOUND_${bit(binding.reviewWindowId != null)}" +
            "_OWNER_BOUND_${bit(binding.reviewOwner != null)}")
    }

    private fun trackAutoInputStarted(info: EditorInfo, generation: Int, restarting: Boolean) {
        val binding = autoDestination ?: return
        val phase = binding.policy.phase
        val accepted = when (phase) {
            OriginalDestinationIntent.Phase.ENTERING_REVIEW ->
                info.packageName == packageName && info.fieldId == R.id.review_english &&
                    binding.draftId != null && binding.reviewOwner != null &&
                    binding.policy.reviewInputStarted(generation, restarting)
            OriginalDestinationIntent.Phase.RETURNING ->
                info.packageName == binding.original.packageName &&
                    info.fieldId == binding.original.editorFieldId &&
                    info.inputType == binding.original.inputType &&
                    binding.policy.originalInputReturned(generation, restarting)
            else -> false
        }
        if (!accepted) {
            noteAutoStartRefusal(binding, phase, info, generation, restarting)
            clearAutoIntent("INPUT_START_UNEXPECTED")
        }
        else note(if (info.packageName == packageName) "AUTO_OWN_INPUT_STARTED"
            else "AUTO_ORIGINAL_INPUT_RETURNED")
    }

    private fun trackAutoInputFinished(generation: Int) {
        val binding = autoDestination ?: return
        val phase = binding.policy.phase
        val authenticatedOwn = if (phase == OriginalDestinationIntent.Phase.REVIEW)
            authenticatedAutoReview(binding) else false
        if (!binding.policy.inputFinished(generation, authenticatedOwn)) {
            noteAutoFinishRefusal(binding, phase, generation, authenticatedOwn)
            clearAutoIntent("INPUT_FINISH_UNEXPECTED")
        }
        else note("AUTO_EXPECTED_INPUT_FINISHED")
    }

    private fun trackAutoSelection(generation: Int, info: EditorInfo?,
        selectionStart: Int, selectionEnd: Int, composingStart: Int, composingEnd: Int) {
        val binding = autoDestination ?: return
        if (info?.packageName != binding.original.packageName ||
            info.fieldId != binding.original.editorFieldId ||
            generation !in setOf(binding.generation, binding.generation + 2)) return
        val caret = binding.original.offset + binding.original.selectionStart
        if (selectionStart != caret || selectionEnd != caret ||
            composingStart >= 0 || composingEnd >= 0) {
            fun bit(value: Boolean) = if (value) 1 else 0
            note("AUTO_REFUSE_SELECTION_PHASE_${binding.policy.phase.name}" +
                "_GEN_${autoGenerationClass(binding, generation)}" +
                "_START_MATCH_${bit(selectionStart == caret)}" +
                "_END_MATCH_${bit(selectionEnd == caret)}" +
                "_COMPOSING_${bit(composingStart >= 0 || composingEnd >= 0)}")
            clearAutoIntent("ORIGINAL_SELECTION_CALLBACK_CHANGED")
        }
    }

    private fun trackAutoEvent(event: AccessibilityEvent) {
        val binding = autoDestination ?: return
        if ((getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) {
            noteAutoEventRefusal(binding, "DEVICE_LOCKED", event,
                binding.reviewWindowId ?: binding.entryWait?.windowId)
            clearAutoIntent("DEVICE_LOCKED")
            return
        }
        val eventPackage = event.packageName?.toString()
        val fromOriginal = eventPackage == binding.original.packageName &&
            event.windowId == binding.original.windowId
        val phase = binding.policy.phase
        val expectedReviewWindowId = binding.reviewWindowId ?: binding.draftId?.let { id ->
            binding.reviewOwner?.let { owner -> ReviewActivity.expectedWindowId(id, owner) }
        }
        if (event.windowId == binding.original.windowId &&
            (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) {
            noteAutoEventRefusal(binding, "ORIGINAL_CONTENT_OR_SELECTION_EVENT", event,
                expectedReviewWindowId)
            clearAutoIntent("ORIGINAL_CONTENT_OR_SELECTION_EVENT")
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val focusedApps = windows.filter {
                it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
            }
            val focused = focusedApps.singleOrNull()
            val expectedFocus = focused == null ||
                (focused.displayId == Display.DEFAULT_DISPLAY &&
                    (focused.id == binding.original.windowId &&
                        focused.getRoot(0) == binding.root ||
                        expectedReviewWindowId != null && focused.id == expectedReviewWindowId &&
                        focused.getRoot(0)?.packageName?.toString() == packageName &&
                        (binding.reviewRoot == null ||
                            focused.getRoot(0) == binding.reviewRoot)))
            if (focusedApps.size > 1 || !expectedFocus) {
                noteAutoEventRefusal(binding, "UNEXPECTED_FOCUSED_APP", event,
                    expectedReviewWindowId)
                clearAutoIntent("UNEXPECTED_FOCUSED_APP")
                return
            }
            val originalWindow = windows.firstOrNull { it.id == binding.original.windowId }
            val originalRoot = originalWindow?.getRoot(0)
            if (originalRoot != null && originalRoot != binding.root) {
                noteAutoEventRefusal(binding, "ORIGINAL_ROOT_CHANGED", event,
                    expectedReviewWindowId, rootWasRead = true, checkedRoot = originalRoot)
                clearAutoIntent("ORIGINAL_ROOT_CHANGED")
                return
            }
        }
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val current = windows.firstOrNull { it.id == event.windowId }
                val verifiedBenignWindow = current != null &&
                    current.displayId == Display.DEFAULT_DISPLAY && !current.isFocused &&
                    (current.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD ||
                        (current.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
                            current.id == ownHostWindowId()))
                if (verifiedBenignWindow) {
                    binding.benignWindowIds.add(event.windowId)
                    return
                }
                if (current == null && binding.benignWindowIds.remove(event.windowId)) return
                if (event.windowId == binding.original.windowId) {
                    val focusOnly = AccessibilityEvent.WINDOWS_CHANGE_ACTIVE or
                        AccessibilityEvent.WINDOWS_CHANGE_FOCUSED
                    if (event.windowChanges != 0 && event.windowChanges and focusOnly == event.windowChanges &&
                        phase in setOf(OriginalDestinationIntent.Phase.ENTERING_REVIEW,
                            OriginalDestinationIntent.Phase.RETURNING)) return
                }
                val ownReviewWindow = current?.takeIf {
                    it.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                        it.displayId == Display.DEFAULT_DISPLAY &&
                        it.getRoot(0)?.packageName?.toString() == packageName &&
                        phase in setOf(OriginalDestinationIntent.Phase.ENTERING_REVIEW,
                            OriginalDestinationIntent.Phase.REVIEW,
                            OriginalDestinationIntent.Phase.RETURNING) &&
                        expectedReviewWindowId != null && it.id == expectedReviewWindowId &&
                        (binding.reviewRoot == null || it.getRoot(0) == binding.reviewRoot)
                }
                if (ownReviewWindow != null ||
                    (phase == OriginalDestinationIntent.Phase.RETURNING &&
                        event.windowId == binding.reviewWindowId && current == null)) return
                noteAutoEventRefusal(binding, "UNEXPECTED_WINDOW_CHANGE", event,
                    expectedReviewWindowId)
                clearAutoIntent("UNEXPECTED_WINDOW_CHANGE")
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.windowId == binding.original.windowId && fromOriginal &&
                    phase == OriginalDestinationIntent.Phase.RETURNING) return
                if (eventPackage == packageName &&
                    phase in setOf(OriginalDestinationIntent.Phase.ENTERING_REVIEW,
                        OriginalDestinationIntent.Phase.REVIEW,
                        OriginalDestinationIntent.Phase.RETURNING) &&
                    expectedReviewWindowId != null &&
                    event.windowId == expectedReviewWindowId &&
                    (binding.reviewRoot == null ||
                        windows.firstOrNull { it.id == event.windowId }?.getRoot(0) ==
                            binding.reviewRoot ||
                        phase == OriginalDestinationIntent.Phase.RETURNING &&
                        windows.none { it.id == event.windowId })) return
                noteAutoEventRefusal(binding, "UNEXPECTED_WINDOW_STATE", event,
                    expectedReviewWindowId)
                clearAutoIntent("UNEXPECTED_WINDOW_STATE")
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val source = event.getSource(0)
                if (fromOriginal && source == binding.node &&
                    phase != OriginalDestinationIntent.Phase.REVIEW) return
                if (eventPackage == packageName && binding.reviewOwner != null &&
                    source?.viewIdResourceName == "$packageName:id/review_english" &&
                    expectedReviewWindowId != null && event.windowId == expectedReviewWindowId &&
                    (binding.reviewRoot == null ||
                        windows.firstOrNull { it.id == event.windowId }?.getRoot(0) ==
                            binding.reviewRoot))
                    return
                noteAutoEventRefusal(binding, "UNEXPECTED_VIEW_FOCUS", event,
                    expectedReviewWindowId, sourceWasRead = true, checkedSource = source)
                clearAutoIntent("UNEXPECTED_VIEW_FOCUS")
            }
        }
    }
    private enum class ObservationCode {
        NOT_CHECKED, LOCKED, NO_INPUT_METHOD, INPUT_NOT_STARTED, NO_EDITOR_INFO,
        NO_INPUT_CONNECTION, UNSUPPORTED_EDITOR_TYPE, NO_FOCUSED_APP_WINDOW,
        MULTIPLE_FOCUSED_APP_WINDOWS, WRONG_DISPLAY, OWN_EDITOR,
        NO_FOCUSED_NODE, NODE_EDITOR_MISMATCH, NODE_DISABLED,
        PROTECTED_OR_NON_TEXT_NODE, SURROUNDING_UNAVAILABLE, UNKNOWN_TEXT_OFFSET,
        SELECTION_NOT_COLLAPSED, NODE_CARET_MISMATCH, POSTCOMMIT_REFRESH_FAILED,
        POSTCOMMIT_TARGET_CHANGED, ACTIVE_COMPOSITION,
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
    private data class ChildWindowSnapshot(val orderedIds: List<Int>, val types: Map<Int, Int>)
    private var capturedTargetChildWindows: ChildWindowSnapshot? = null
    private var capturedHostWindowId: Int? = null
    @Volatile private var firstTargetChildDeltaCode: String? = null
    private var overlayStopEntryUptimeMs: Long? = null
    private var overlayReviewChildrenCallbackUptimeMs: Long? = null
    private var firstTargetChildEventUptimeMs: Long? = null
    private var firstTargetChildCallbackUptimeMs: Long? = null
    private var firstTargetChildEventTimeStatus = "NOT_OBSERVED"

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

    /** Event occurrence is Android uptime; only signed differences are exported. */
    private fun firstTargetChildTimingDiagnostics(): String {
        val callback = firstTargetChildCallbackUptimeMs ?: return "NOT_OBSERVED"
        val occurred = firstTargetChildEventUptimeMs
        fun delta(reference: Long?): String =
            if (occurred == null) "UNKNOWN" else relativeUptime(occurred, reference)
        val delay = if (occurred == null) "UNKNOWN"
            else (callback - occurred).toString()
        return "STATUS_${firstTargetChildEventTimeStatus}" +
            "_CAPTURE_START_${delta(captureStartUptimeMs)}" +
            "_CAPTURE_COMPLETE_${delta(captureCompleteUptimeMs)}" +
            "_STOP_ENTRY_${delta(overlayStopEntryUptimeMs)}" +
            "_REVIEW_CHILDREN_POPULATED_CALLBACK_${delta(overlayReviewChildrenCallbackUptimeMs)}" +
            "_DELIVERY_DELAY_${delay}"
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
        if (startupExperiment == StartupExperiment.OVERLAY && pendingId == id) {
            experimentPhase = "OVERLAY_INVALIDATED"
            cancelSession(id)
            status = "Overlay session refused: $code. Start again from the bubble."
            renderBubble()
        }
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
            "First child-window delta: ${firstTargetChildDeltaCode ?: "none"}\n" +
            "First child-window timing: ${firstTargetChildTimingDiagnostics()}\n" +
            "Experiment arm: ${startupExperiment.name}\n" +
            "Experiment capture count: $experimentCaptureCount\n" +
            "Experiment phase: $experimentPhase\n" +
            "Unbound recording proof count: $unboundRecordingProofCount\n" +
            "Draft phase: ${unboundDraft.current()?.phase?.name ?: "NONE"}\n" +
            "Auto insert enabled: ${BuildConfig.AUTO_INSERT}\n" +
            "Speech input enabled: ${BuildConfig.SPEECH_INPUT}\n" +
            "Translation input enabled: ${BuildConfig.TRANSLATION_INPUT}\n" +
            "Original intent phase: ${autoDestination?.policy?.phase?.name ?: autoLastPhase}\n" +
            "Original intent last reason: $autoLastReason\n" +
            "Original intent captured count: $autoCapturedCount\n" +
            "Auto insert attempt count: $autoAttemptCount\n" +
            "Auto insert fallback count: $autoFallbackCount\n" +
            "Speech Stop to review created ms: ${speechLastStopToReviewMs ?: "none"}\n" +
            "Translation requests/success/errors/cancels: $translationRequests/$translationSuccesses/$translationErrors/$translationCancels\n" +
            "Last translation request duration ms: ${translationLastDurationMs ?: "none"}\n" +
            "Last translation usage input/cached/output/reasoning tokens: " +
            (translationLastUsage?.let {
                "${it.inputTokens}/${it.cachedInputTokens}/${it.outputTokens}/${it.reasoningTokens}"
            } ?: "none") + "\n" +
            "Destination capture count: $destinationCaptureCount\n" +
            "Commit request count: $commitRequestCount\n" +
            "Commit confirmation: $commitConfirmation\n" +
            "Overlay cleanup pending: ${overlayCleanupPending != null}\n" +
            "Last idle refresh: ${lastIdleRefresh?.code() ?: "none"}\n" +
            "Last idle render: count=$idleRenderCount outcome=${idleRenderOutcome.name}\n" +
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
            idleInputStartCount++
            if (startupExperiment == StartupExperiment.RESELECT) {
                scheduleIdleBubbleRefresh(IdleRefreshReason.INPUT_STARTED)
                return
            }
            if (startupExperiment == StartupExperiment.REVIEW) {
                generation++
                composition.reset()
                trackAutoInputStarted(attribute, generation, restarting)
                abortPostCommitProbe("INPUT_STARTED")
                freshDestination?.let {
                    if (it.phase == DestinationPhase.PRECOMMIT) guard.invalidate(it.id)
                    else finishUncertain(it, "INPUT_STARTED")
                    note("FRESH_INPUT_STARTED")
                }
                scheduleIdleBubbleRefresh(IdleRefreshReason.INPUT_STARTED)
                return
            }
            generation++
            composition.reset()
            inputStarted(attribute)
        }

        override fun onFinishInput() {
            if (startupExperiment == StartupExperiment.RESELECT) {
                if (unboundRecording.current() == null) {
                    idleBubbleVisible = false
                    expanded = false
                    renderBubble()
                }
                scheduleIdleBubbleRefresh(IdleRefreshReason.INPUT_FINISHED)
                return
            }
            if (startupExperiment == StartupExperiment.REVIEW) {
                trackAutoInputFinished(generation)
                composition.reset()
                abortPostCommitProbe("INPUT_FINISHED")
                freshDestination?.let {
                    if (it.phase == DestinationPhase.PRECOMMIT) guard.invalidate(it.id)
                    else finishUncertain(it, "INPUT_FINISHED")
                    note("FRESH_INPUT_FINISHED")
                }
                // Android clears currentInputStarted after this callback returns.
                // Hide immediately, then inspect the settled state on the main loop.
                if (unboundRecording.current() == null && unboundDraft.current() == null) {
                    idleBubbleVisible = false
                    expanded = false
                    renderBubble()
                }
                scheduleIdleBubbleRefresh(IdleRefreshReason.INPUT_FINISHED)
                return
            }
            composition.reset()
            inputFinished()
            // Do not call the default implementation: it can clear composition
            // in the editor owned by SwiftKey.
        }

        override fun onUpdateSelection(
            oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
            candidatesStart: Int, candidatesEnd: Int
        ) {
            if (startupExperiment == StartupExperiment.RESELECT) return
            if (startupExperiment == StartupExperiment.REVIEW) {
                if (freshDestination == null)
                    trackAutoSelection(generation, currentInputEditorInfo,
                        newSelStart, newSelEnd, candidatesStart, candidatesEnd)
                if (freshDestination != null)
                    composition.update(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
                return
            }
            composition.update(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        }

        fun compositionAt(caret: Int): CompositionState = composition.stateAt(caret)
        fun resetComposition() = composition.reset()
    }

    private fun resetFreshComposition() {
        (inputMethod as? TrackingInputMethod)?.resetComposition()
    }

    private fun inputStarted(info: EditorInfo) {
        val id = pendingId ?: return
        val session = guard.session(id) ?: return
        note(if (info.packageName == session.target.packageName) "INPUT_STARTED_TARGET_APP"
            else if (info.packageName == packageName) "INPUT_STARTED_OWN_APP" else "INPUT_STARTED_OTHER_APP")
        if (startupExperiment == StartupExperiment.OVERLAY) {
            invalidate(id, "OVERLAY_INPUT_RESTART")
            return
        }
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
        if (startupExperiment == StartupExperiment.OVERLAY) {
            invalidate(id, "OVERLAY_INPUT_FINISHED")
            return
        }
        if (returningToEditor && !returnContinuity.finishedInput()) invalidate(id, "INPUT_FINISH_SEQUENCE")
    }

    private data class IdleWindowCheck(
        val ime: IdleBubblePolicy.Ime, val ordinaryInputReady: Boolean,
        val stage: IdleCheckStage
    )

    private fun scheduleIdleBubbleRefresh(reason: IdleRefreshReason = IdleRefreshReason.OTHER_SCHEDULED) {
        if (!unboundArm() || !serviceConnected) return
        idleRefreshReason = reason
        if (idleRefreshQueued) return
        idleRefreshQueued = true
        main.post {
            idleRefreshQueued = false
            refreshIdleBubble(reason = idleRefreshReason)
        }
    }

    private fun refreshIdleBubble(
        forceRender: Boolean = false, reason: IdleRefreshReason = IdleRefreshReason.DIRECT
    ) {
        var gate = IdleRefreshGate.NONE
        var stage = IdleCheckStage.NOT_CHECKED
        var ime = IdleBubblePolicy.Ime.UNKNOWN
        var unlocked: Boolean? = null
        var wanted: Boolean? = null
        try {
        if (!unboundArm()) { gate = IdleRefreshGate.ARM_OFF; return }
        if (!serviceConnected) { gate = IdleRefreshGate.DISCONNECTED; return }
        if (!::windowManager.isInitialized) { gate = IdleRefreshGate.NO_WINDOW_MANAGER; return }
        if (unboundRecording.current() != null) { gate = IdleRefreshGate.RECORDING; return }
        if (freshDestination != null) { gate = IdleRefreshGate.DESTINATION; return }
        val draft = unboundDraft.current()
        if (draft != null) {
            if (draft.phase == UnboundDraftState.Phase.REVIEW ||
                draft.phase == UnboundDraftState.Phase.TRANSLATING) {
                gate = IdleRefreshGate.DRAFT_REVIEW
                idleBubbleVisible = false
                if (forceRender) renderBubble()
                return
            }
            gate = IdleRefreshGate.DRAFT_RESULT
            if (!idleBubbleVisible || forceRender) {
                idleBubbleVisible = true
                expanded = true
                renderBubble()
            } else if (bubble == null) {
                renderBubble()
            } else if (!idleDragging && idleBubbleGeometry?.let { it.frame != bubbleFrame() } == true) {
                // Result placement follows keyboard/bounds metadata only. Captured
                // insertion hosts and active drag already returned above.
                renderBubble()
            }
            return
        }
        val isUnlocked = runCatching {
            (getSystemService(POWER_SERVICE) as PowerManager).isInteractive &&
                !(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        }.getOrDefault(false)
        unlocked = isUnlocked
        // Only window type, display, focus, bounds, and the focused root package
        // are read. This is a visibility heuristic, never insertion eligibility.
        val check = if (!isUnlocked) IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
                IdleCheckStage.LOCKED)
            else runCatching { inspectIdleWindows() }
            .getOrDefault(IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
                IdleCheckStage.CHECK_ERROR))
        stage = check.stage
        ime = check.ime
        val shouldShow = idleBubblePolicy.shouldShow(check.ime, check.ordinaryInputReady,
            isUnlocked, activeOwner = false)
        wanted = shouldShow
        gate = IdleRefreshGate.IDLE_CHECKED
        if (shouldShow != idleBubbleVisible || forceRender) {
            idleBubbleVisible = shouldShow
            if (!shouldShow) expanded = false
            renderBubble()
        } else if (shouldShow) {
            val holder = bubble
            if (holder == null) {
                renderBubble()
                return
            }
            val params = bubbleParams
            val geometry = idleBubbleGeometry
            if (holder.isEnabled && params != null && geometry != null &&
                holder.width > 0 && holder.height > 0) {
                if (geometry.frame != bubbleFrame()) {
                    renderBubble()
                    return
                }
                if (idleDragging) return
                val desired = BubblePlacement.resolve(savedBubbleAnchor(), holder.width,
                    holder.height, geometry.frame.area)
                moveBubbleToScreenPoint(holder, params, desired)
            }
        }
        } finally {
            idleRefreshCount++
            val holder = bubble
            lastIdleRefresh = IdleRefreshSnapshot(idleRefreshCount, reason, gate, stage, ime,
                serviceConnected, idleInputStartCount, unlocked, wanted,
                idleBubblePolicy.suppressedForDiagnostics(), idleBubbleVisible,
                holder != null, holder?.isAttachedToWindow == true, holder?.isShown == true)
        }
    }

    private fun inspectIdleWindows(): IdleWindowCheck {
        val snapshot = windows ?: return IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
            IdleCheckStage.WINDOWS_UNAVAILABLE)
        val focused = snapshot.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focused.size != 1) return IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
            IdleCheckStage.FOCUSED_APP_COUNT)
        if (focused.single().displayId != Display.DEFAULT_DISPLAY)
            return IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
                IdleCheckStage.FOCUSED_APP_DISPLAY)
        val imes = snapshot.filter {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                it.displayId == Display.DEFAULT_DISPLAY
        }
        if (imes.size > 1 || snapshot.any {
                it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                    it.displayId != Display.DEFAULT_DISPLAY
            }) return IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
                IdleCheckStage.IME_MULTIPLE_OR_WRONG_DISPLAY)
        if (imes.isEmpty()) return IdleWindowCheck(IdleBubblePolicy.Ime.ABSENT, false,
            IdleCheckStage.IME_ABSENT)
        val imeBounds = Rect()
        imes.single().getBoundsInScreen(imeBounds)
        if (imeBounds.isEmpty) return IdleWindowCheck(IdleBubblePolicy.Ime.UNKNOWN, false,
            IdleCheckStage.IME_BOUNDS_EMPTY)
        val method = inputMethod as? TrackingInputMethod
            ?: return IdleWindowCheck(IdleBubblePolicy.Ime.VISIBLE, false,
                IdleCheckStage.INPUT_METHOD_UNAVAILABLE)
        if (!method.currentInputStarted) return IdleWindowCheck(IdleBubblePolicy.Ime.VISIBLE, false,
            IdleCheckStage.INPUT_NOT_STARTED)
        val info = method.currentInputEditorInfo
            ?: return IdleWindowCheck(IdleBubblePolicy.Ime.VISIBLE, false,
                IdleCheckStage.EDITOR_INFO_UNAVAILABLE)
        if (!safeInputType(info.inputType)) return IdleWindowCheck(IdleBubblePolicy.Ime.VISIBLE, false,
            IdleCheckStage.UNSAFE_INPUT_TYPE)
        // getRoot(0) avoids the framework's default descendant prefetch.
        val focusedPackage = focused.single().getRoot(0)?.packageName?.toString()
        return IdleWindowCheck(IdleBubblePolicy.Ime.VISIBLE,
            focusedPackage != null && focusedPackage == info.packageName,
            when {
                focusedPackage == null -> IdleCheckStage.ROOT_PACKAGE_UNAVAILABLE
                focusedPackage != info.packageName -> IdleCheckStage.PACKAGE_MISMATCH
                else -> IdleCheckStage.READY
            })
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceConnected = true
        if (unboundArm()) scheduleIdleBubbleRefresh(IdleRefreshReason.SERVICE_CONNECTED)
        else renderBubble()
    }

    override fun onCreateInputMethod(): InputMethod = TrackingInputMethod(this)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (unboundArm()) {
            if (autoDestination != null) trackAutoEvent(event)
            postCommitProbe?.let { probe ->
                if ((event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                        event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) &&
                    event.windowId != probe.windowId &&
                    event.packageName?.toString() != packageName)
                    abortPostCommitProbe("FOCUS_WINDOW_CHANGED")
            }
            val destination = freshDestination
            val target = destination?.let { guard.session(it.id)?.target }
            if (destination?.phase == DestinationPhase.OBSERVING &&
                (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED &&
                        event.packageName?.toString() != packageName))) {
                finishUncertain(destination, "WINDOW_OR_FOCUS_CHANGED")
                return
            }
            if (destination?.phase == DestinationPhase.PRECOMMIT &&
                (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED &&
                        event.packageName?.toString() != packageName) ||
                    (target != null && event.packageName?.toString() == target.packageName &&
                        event.windowId == target.windowId &&
                        (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
                            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)))) {
                guard.invalidate(destination.id)
                note("FRESH_TARGET_EVENT")
                return
            }
            // Idle UI visibility is metadata only. No destination or editor is captured.
            val draftPhase = unboundDraft.current()?.phase
            if (unboundRecording.current() == null && freshDestination == null &&
                (draftPhase == null || draftPhase == UnboundDraftState.Phase.READY ||
                    draftPhase == UnboundDraftState.Phase.UNCERTAIN) &&
                (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED))
                scheduleIdleBubbleRefresh(when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> IdleRefreshReason.FOCUS_EVENT
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED -> IdleRefreshReason.WINDOW_EVENT
                    else -> IdleRefreshReason.OTHER_EVENT
                })
            return
        }
        val deliveredUptimeMs = SystemClock.uptimeMillis()
        val id = pendingId ?: return
        val session = guard.session(id) ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            onWindowsChanged(event, session, deliveredUptimeMs)
            return
        }
        if (startupExperiment == StartupExperiment.OVERLAY) {
            verifyOverlayTarget(id)
            if (pendingId != id) return
        }
        val packageName = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId) {
                val sourceKind = when (event.getSource(0)) {
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
                        note(localFixtureContentSource(event.getSource(0)))
                }
                invalidate(id, reason)
            }
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId && event.getSource(0) == targetNode) {
                note("TARGET_EDITOR_FOCUSED")
                if (!returningToEditor && modalOcclusion?.hasAcquiredFocus == true)
                    invalidate(id, "TARGET_FOCUSED_DURING_PANEL")
            }
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId && event.getSource(0) != targetNode) {
                invalidate(id, "TARGET_FOCUS_NODE")
            }
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName == session.target.packageName &&
                event.windowId == session.target.windowId) {
                val sourceKind = when (event.getSource(0)) {
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

    private fun onWindowsChanged(
        event: AccessibilityEvent, session: SessionGuard.Session, callbackEntryUptimeMs: Long
    ) {
        if (startupExperiment == StartupExperiment.OVERLAY) {
            if (event.windowId == session.target.windowId && event.windowChanges != 0) {
                if (event.windowChanges and AccessibilityEvent.WINDOWS_CHANGE_CHILDREN != 0) {
                    noteFirstTargetChildTiming(event, callbackEntryUptimeMs, session)
                    try { noteFirstTargetChildDelta(session) }
                    catch (_: Exception) { firstTargetChildDeltaCode = "CHILD_DELTA_INCOMPLETE_DIAGNOSTIC_EXCEPTION" }
                }
                note(if (overlayPanel?.hostStable() == true)
                    "OVERLAY_HOST_AT_TARGET_EVENT_SAME" else "OVERLAY_HOST_AT_TARGET_EVENT_UNVERIFIED")
                note("OVERLAY_TARGET_WINDOW_FLAGS_${event.windowChanges}")
                invalidate(session.id, "OVERLAY_TARGET_WINDOW_CHANGED")
                return
            }
            verifyOverlayTarget(session.id)
            return
        }
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
        val focusedPackage = focusedApps.singleOrNull()?.getRoot(0)?.packageName?.toString()
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
            .singleOrNull()?.takeIf { it.getRoot(0)?.packageName?.toString() == packageName }?.id

    /** Used only after explicit confirmation; both target package and window must match. */
    private fun focusedReturnWindowId(
        currentWindows: List<AccessibilityWindowInfo>, target: EditorSnapshot
    ): Int? = currentWindows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }
        .singleOrNull()?.takeIf {
            it.id == target.windowId && it.getRoot(0)?.packageName?.toString() == target.packageName
        }?.id

    /** Compare fresh identities without reading the destination's text while it is covered. */
    private fun reappearedTargetIdentity(
        targetWindow: AccessibilityWindowInfo?, original: EditorSnapshot
    ): Pair<Boolean, Boolean> = runCatching {
        val root = targetWindow?.getRoot(0) ?: return@runCatching false to false
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
            else -> when (focusedApps[0].getRoot(0)?.packageName?.toString()) {
                packageName -> "OWN"
                session.target.packageName -> "TARGET"
                null -> "NO_ROOT"
                else -> "OTHER"
            }
        }
        val targetWindow = currentWindows.firstOrNull { it.id == session.target.windowId }
        val currentRoot = targetWindow?.getRoot(0)
        val root = when {
            targetWindow == null -> "ABSENT"
            currentRoot == null -> "NO_ROOT"
            currentRoot == targetRootNode -> "SAME"
            else -> "DIFFERENT"
        }
        val source = when (event.getSource(0)) {
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

    /** Window relations only: getChild never reads the child window's root or view tree. */
    private fun childWindowSnapshot(window: AccessibilityWindowInfo?): ChildWindowSnapshot? {
        if (window == null) return null
        return try {
            val count = window.childCount
            if (count !in 0..16) return null
            val order = ArrayList<Int>(count)
            val types = HashMap<Int, Int>(count)
            for (index in 0 until count) {
                val child = window.getChild(index) ?: return null
                if (child.id < 0 || types.put(child.id, child.type) != null) return null
                order.add(child.id)
            }
            ChildWindowSnapshot(order, types)
        } catch (_: Exception) { null }
    }

    private fun ownHostWindowId(): Int? = runCatching {
        bubble?.createAccessibilityNodeInfo()?.windowId?.takeIf { it >= 0 }
    }.getOrNull()

    /** One first target-children event; record before any window metadata query. */
    private fun noteFirstTargetChildTiming(
        event: AccessibilityEvent, callbackEntryUptimeMs: Long, session: SessionGuard.Session
    ) {
        if (firstTargetChildCallbackUptimeMs != null || session.invalidated) return
        firstTargetChildCallbackUptimeMs = callbackEntryUptimeMs
        val occurred = event.eventTime
        firstTargetChildEventTimeStatus = when {
            occurred == 0L -> "UNKNOWN_ZERO"
            occurred < 0L -> "UNKNOWN_NEGATIVE"
            occurred > callbackEntryUptimeMs -> "UNKNOWN_FUTURE"
            else -> "VALID"
        }
        firstTargetChildEventUptimeMs = occurred.takeIf {
            it > 0L && it <= callbackEntryUptimeMs
        }
        note("FIRST_TARGET_CHILD_TIME_${firstTargetChildEventTimeStatus}")
    }

    /** First target child-window event only; raw IDs never leave process memory. */
    private fun noteFirstTargetChildDelta(session: SessionGuard.Session) {
        if (firstTargetChildDeltaCode != null || session.invalidated) return
        val summary = try {
            val original = capturedTargetChildWindows
            val current = childWindowSnapshot(windows.singleOrNull { it.id == session.target.windowId })
            if (original == null || current == null) {
                "CHILD_DELTA_INCOMPLETE_CAPTURE_${if (original == null) 1 else 0}" +
                    "_CURRENT_${if (current == null) 1 else 0}"
            } else {
                val added = current.types.keys - original.types.keys
                val removed = original.types.keys - current.types.keys
                val retained = original.types.keys intersect current.types.keys
                fun kind(id: Int, type: Int): String = when {
                    capturedHostWindowId != null && id == capturedHostWindowId -> "OWN_HOST"
                    type == AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "IME_TYPE"
                    type == AccessibilityWindowInfo.TYPE_APPLICATION -> "APP_TYPE"
                    type == AccessibilityWindowInfo.TYPE_SYSTEM -> "SYSTEM_TYPE"
                    type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "OVERLAY_TYPE"
                    else -> "OTHER_TYPE"
                }
                val categories = listOf("OWN_HOST", "IME_TYPE", "APP_TYPE",
                    "SYSTEM_TYPE", "OVERLAY_TYPE", "OTHER_TYPE")
                fun counts(ids: Set<Int>, types: Map<Int, Int>): String =
                    categories.joinToString("_") { category ->
                        "${category}_${ids.count { id -> kind(id, types.getValue(id)) == category }}"
                    }
                val order = when {
                    added.isNotEmpty() || removed.isNotEmpty() -> "UNKNOWN_SET_CHANGED"
                    original.orderedIds == current.orderedIds -> "SAME"
                    else -> "CHANGED"
                }
                "CHILD_DELTA_ADD_${added.size}_REMOVE_${removed.size}_RETAIN_${retained.size}" +
                    "_TYPE_CHANGED_${retained.count { original.types[it] != current.types[it] }}" +
                    "_ORDER_$order" +
                    "_HOST_ID_${if (capturedHostWindowId == null) "UNKNOWN" else "KNOWN"}" +
                    "_ADD_${counts(added, current.types)}_REMOVE_${counts(removed, original.types)}"
            }
        } catch (_: Exception) { "CHILD_DELTA_INCOMPLETE_DIAGNOSTIC_EXCEPTION" }
        firstTargetChildDeltaCode = summary
        note(summary)
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

    override fun onInterrupt() {
        speechKeyCheckGeneration++
        speechKeyCheckPending = false
        unboundRecording.current()?.id?.let { cancelUnboundRecording(it, "SERVICE_INTERRUPTED") }
        unboundDraft.current()?.id?.let { endDraft(it, "SERVICE_INTERRUPTED") }
        if (autoDestination != null) clearAutoIntent("SERVICE_INTERRUPTED")
        pendingId?.let { invalidate(it, "SERVICE_INTERRUPTED") }
    }

    override fun onDestroy() {
        serviceConnected = false
        speechKeyCheckGeneration++
        speechKeyCheckPending = false
        speechJob?.cancel()
        speechJob = null
        cancelTranslationJob()
        if (autoDestination != null) clearAutoIntent("SERVICE_DESTROYED")
        speechGate.clear()
        clearPostCommitProbe()
        unboundRecording.endActive()
        unboundDraft.current()?.id?.let(ReviewActivity::closeDraft)
        unboundDraft.endActive()
        clearDraftOwnership()
        freshDestination?.id?.let { guard.cancel(it); guard.end(it) }
        freshDestination = null
        stopOverlayOwnership()
        overlayCleanupPending?.close()
        capturedTargetChildWindows = null
        capturedHostWindowId = null
        pendingId?.let { guard.cancel(it); guard.end(it) }
        targetNode = null
        targetRootNode = null
        modalOcclusion = null
        overlayInputGeneration = null
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
        if (startupExperiment == StartupExperiment.OVERLAY) stopOverlayOwnership()
        guard.end(id)
        if (startupExperiment == StartupExperiment.OVERLAY) {
            capturedTargetChildWindows = null
            capturedHostWindowId = null
        }
        pendingId = null
        if (startupExperiment != StartupExperiment.OFF) experimentPhase = "CANCELLED"
        reviewVisible = false
        returningToEditor = false
        ownTransition = false
        targetNode = null
        targetRootNode = null
        modalOcclusion = null
        overlayInputGeneration = null
        status = if (overlayCleanupPending != null) "Overlay cleanup uncertain. Restart the service."
            else "Session cancelled."
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
        if (unboundArm()) {
            startUnboundRecording(mode)
            return
        }
        if (pendingId != null || SessionActivity.isVisible || overlayCleanupPending != null) {
            status = "Finish or cancel the current review before starting another session."
            if (startupExperiment != StartupExperiment.OVERLAY || pendingId == null)
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
        if (startupExperiment == StartupExperiment.OVERLAY) {
            capturedTargetChildWindows = target.windowChildren
            capturedHostWindowId = ownHostWindowId()
            firstTargetChildDeltaCode = null
            overlayStopEntryUptimeMs = null
            overlayReviewChildrenCallbackUptimeMs = null
            firstTargetChildEventUptimeMs = null
            firstTargetChildCallbackUptimeMs = null
            firstTargetChildEventTimeStatus = "NOT_OBSERVED"
        }
        pendingId = session.id
        invalidationCodes.clear()
        lastInvalidationCode = null
        note("MODE_SELECTED_${mode.name}")
        targetNode = target.node
        targetRootNode = target.root
        modalOcclusion = if (startupExperiment == StartupExperiment.OVERLAY) null
            else ModalOcclusionGate(session.target.windowId)
        returnContinuity.capture(target.generation)
        if (startupExperiment == StartupExperiment.OVERLAY)
            overlayInputGeneration = target.generation
        recoveryText = null
        reviewVisible = false
        returningToEditor = false
        ownTransition = true
        if (startupExperiment == StartupExperiment.HOLD) {
            experimentPhase = "EXPANDED_HOLD"
            scheduleExperimentHold(session.id)
            return
        }
        if (startupExperiment != StartupExperiment.PANEL &&
            startupExperiment != StartupExperiment.OVERLAY) {
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
        if (startupExperiment == StartupExperiment.OVERLAY) {
            ownTransition = false // No modal visibility/bounds exception belongs to this arm.
            startOverlaySession(session.id, mode)
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

    /** Debug-only destination-free session. No target APIs are called until a later explicit action. */
    private fun startUnboundRecording(mode: OutputMode) {
        if (BuildConfig.SPEECH_INPUT) {
            if (mode != OutputMode.ENGLISH && !BuildConfig.TRANSLATION_INPUT) {
                status = "Korean translation arrives in the next phase. Choose EN for speech input."
                renderBubble()
                return
            }
            if (speechKeyCheckPending) return
            if (autoDestination == null && unboundRecording.current() == null &&
                unboundDraft.current() == null) captureAutoIntent(mode)
            val clickIntent = autoDestination?.takeIf { it.recordingId == null }
            val generation = ++speechKeyCheckGeneration
            speechKeyCheckPending = true
            status = "Checking API key…"
            // Keep the clicked, already attached host until the async check returns.
            // Replacing it here can change its token before the continuity check.
            val originalHost = bubble
            val originalToken = originalHost?.windowToken
            Thread({
                val available = runCatching { ApiKeyStore(this).hasKey() }.getOrDefault(false)
                main.post {
                    if (generation != speechKeyCheckGeneration || !serviceConnected) {
                        if (autoDestination === clickIntent) clearAutoIntent("KEY_PRECHECK_STALE")
                        return@post
                    }
                    speechKeyCheckPending = false
                    if (!expanded || bubble !== originalHost || originalHost == null ||
                        !originalHost.isAttachedToWindow || originalHost.windowToken != originalToken) {
                        note("SPEECH_KEY_PRECHECK_HOST_STALE")
                        if (autoDestination === clickIntent) clearAutoIntent("KEY_PRECHECK_HOST_STALE")
                        return@post
                    }
                    if (!available) {
                        note("SPEECH_KEY_MISSING")
                        status = "API key missing. Open KoScribe > API key settings."
                        if (autoDestination === clickIntent) clearAutoIntent("KEY_UNAVAILABLE")
                        renderBubble()
                    } else startUnboundRecordingReady(mode)
                }
            }, "KoScribeSpeechKeyCheck").start()
            return
        }
        if (autoDestination == null && unboundRecording.current() == null &&
            unboundDraft.current() == null) captureAutoIntent(mode)
        startUnboundRecordingReady(mode)
    }

    private fun startUnboundRecordingReady(mode: OutputMode) {
        if (pendingId != null || unboundRecording.current() != null ||
            unboundDraft.current() != null || SessionActivity.isVisible ||
            overlayCleanupPending != null) {
            clearPendingAuto("RECORDING_START_BUSY")
            status = "Finish or cancel the current recording first."
            renderBubble()
            return
        }
        if ((getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) {
            clearPendingAuto("RECORDING_START_LOCKED")
            status = "Unlock before recording."
            renderBubble()
            return
        }
        val host = bubble?.takeIf {
            expanded && it.isAttachedToWindow && it.isShown && it.isEnabled &&
                it.width > 0 && it.height > 0
        }
        if (host == null) {
            clearPendingAuto("RECORDING_HOST_MISSING")
            status = "Recording bubble is unavailable."
            renderBubble()
            return
        }
        val draft = unboundRecording.begin(mode) ?: return
        val id = draft.id
        autoDestination?.takeIf { it.recordingId == null && it.mode == mode }?.recordingId = id
        if (BuildConfig.SPEECH_INPUT && !speechGate.arm(id)) {
            if (autoForRecording(id) != null) clearAutoIntent("SPEECH_GATE_REFUSED")
            unboundRecording.end(id)
            status = "Speech session unavailable. Try again."
            renderBubble()
            return
        }
        experimentPhase = "RESELECT_PREPARING"
        note("RESELECT_MODE_SELECTED_${mode.name}")
        lateinit var owner: OverlayRecordingPanel
        owner = OverlayRecordingPanel(this, host, mode,
            onFailure = { code -> cancelUnboundRecording(id, code) },
            onReady = {
                if (unboundRecording.owns(id)) {
                    if (!owner.hostStable()) cancelUnboundRecording(id, "RESELECT_HOST_CHANGED_AT_MIC_PROOF")
                    else if (unboundRecording.microphoneProved(id)) {
                        unboundRecordingProofCount++
                        experimentPhase = "RESELECT_RECORDING_PROVED"
                        note("RESELECT_MIC_READ_POSITIVE_UNSILENCED")
                    }
                }
            },
            onStopEntry = {
                if (unboundRecording.owns(id)) {
                    note("RESELECT_STOP_ENTRY")
                    if (BuildConfig.SPEECH_INPUT) speechStopEntryUptimeMs = SystemClock.uptimeMillis()
                }
            },
            onReview = {
                if (unboundRecording.microphoneStoppedForReview(id)) {
                    note("RESELECT_MIC_PROVED_STOPPED")
                    if (startupExperiment == StartupExperiment.REVIEW) beginDraftReview(id, mode)
                    else experimentPhase = "RESELECT_READ_ONLY_REVIEW"
                }
            },
            onCancel = { cancelUnboundRecording(id, "USER_CANCELLED") },
            sessionTimeoutMs = if (BuildConfig.SPEECH_INPUT) 65_000L else 15_000L,
            speechMode = BuildConfig.SPEECH_INPUT,
            onSpeechAudio = if (BuildConfig.SPEECH_INPUT) { wav -> startSpeechTranscription(id, wav) }
                else null
        )
        overlayPanel = owner
        if (!owner.showAndStart()) {
            cancelUnboundRecording(id, "RESELECT_HOST_UNAVAILABLE")
            return
        }
        if (!unboundRecording.owns(id)) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF)
                    cancelUnboundRecording(id, "RESELECT_SCREEN_OFF")
            }
        }
        try {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
            overlayScreenReceiver = receiver
        } catch (_: Exception) {
            cancelUnboundRecording(id, "RESELECT_SCREEN_RECEIVER_FAILED")
            return
        }
        val heartbeat = object : Runnable {
            override fun run() {
                if (!unboundRecording.owns(id) || overlayPanel !== owner) return
                if ((getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked)
                    return cancelUnboundRecording(id, "RESELECT_LOCKED")
                if (!owner.hostStable()) return cancelUnboundRecording(id, "RESELECT_HOST_CHANGED")
                owner.pollMic()
                if (unboundRecording.owns(id) && overlayPanel === owner) main.postDelayed(this, 250)
            }
        }
        overlayHeartbeat = heartbeat
        main.post(heartbeat)
    }

    private fun cancelUnboundRecording(id: String, reason: String) {
        if (!unboundRecording.end(id)) return
        if (autoForRecording(id) != null) clearAutoIntent("RECORDING_ENDED")
        speechStopEntryUptimeMs = null
        speechGate.cancel(id)
        speechJob?.cancel()
        speechJob = null
        stopOverlayOwnership()
        experimentPhase = "RESELECT_CANCELLED"
        note("RESELECT_ENDED_${reason}")
        status = if (overlayCleanupPending != null) "Microphone cleanup uncertain. Restart the service."
            else if (reason.startsWith("SPEECH_")) speechErrorMessage(reason.removePrefix("SPEECH_"))
            else "Recording cancelled."
        refreshIdleBubble(forceRender = true)
    }

    /** Settings calls this before and after key mutation; it never reads the key. */
    fun cancelSpeechForCredentialChange() {
        if (!BuildConfig.SPEECH_INPUT) return
        speechKeyCheckGeneration++
        speechKeyCheckPending = false
        if (autoDestination != null) clearAutoIntent("CREDENTIAL_CHANGED")
        unboundRecording.current()?.id?.let { cancelUnboundRecording(it, "SPEECH_KEY_CHANGED") }
        speechJob?.cancel()
        speechJob = null
        unboundDraft.current()?.id?.let { endDraft(it, "CREDENTIAL_CHANGED") }
    }

    private fun speechErrorMessage(code: String): String = when (code) {
        "KEY_UNAVAILABLE", "KEY_CHANGED" -> "API key unavailable. Open KoScribe > API key settings."
        "AUTH" -> "API key was rejected. Check it in KoScribe settings."
        "RATE_LIMIT" -> "Transcription limit reached. Try a new recording later."
        "AUDIO_TOO_LARGE" -> "Recording was too large. Try a shorter message."
        "TRANSCRIPT_TOO_LONG" -> "English transcript exceeded 512 characters. Try a shorter message."
        "EMPTY_TRANSCRIPT" -> "No English speech was recognized. Try a new recording."
        "TIMEOUT" -> "Transcription timed out. Try a new recording."
        else -> "Transcription failed ($code). Try a new recording."
    }

    private fun startSpeechTranscription(recordingId: String, wav: ByteArray) {
        if (!BuildConfig.SPEECH_INPUT || startupExperiment != StartupExperiment.REVIEW ||
            !unboundRecording.owns(recordingId) || overlayPanel?.hostStable() != true ||
            speechJob != null || !speechGate.stoppedAndReleased(recordingId)) {
            wav.fill(0)
            if (unboundRecording.owns(recordingId))
                cancelUnboundRecording(recordingId, "SPEECH_HANDOFF_REFUSED")
            return
        }
        experimentPhase = "SPEECH_TRANSCRIBING"
        val transcriber = SpeechTranscriber(this)
        lateinit var job: SpeechTranscriber.Job
        job = transcriber.newJob(wav) { result ->
            main.post {
                if (speechJob !== job || !unboundRecording.owns(recordingId) ||
                    !speechGate.accept(recordingId)) return@post
                speechJob = null
                if ((getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked ||
                    overlayPanel?.hostStable() != true) {
                    cancelUnboundRecording(recordingId, "SPEECH_CONTEXT_CHANGED")
                    return@post
                }
                when (result) {
                    is SpeechTranscriber.Result.Text -> {
                        if (unboundRecording.microphoneStoppedForReview(recordingId)) {
                            val mode = unboundRecording.current()?.mode
                            if (mode != null) beginDraftReview(recordingId, mode, result.value)
                            else cancelUnboundRecording(recordingId, "SPEECH_STATE_CHANGED")
                        }
                        else cancelUnboundRecording(recordingId, "SPEECH_STATE_CHANGED")
                    }
                    is SpeechTranscriber.Result.Error ->
                        cancelUnboundRecording(recordingId, "SPEECH_${result.code.name}")
                }
            }
        }
        speechJob = job
        job.start()
    }

    private fun beginDraftReview(recordingId: String, mode: OutputMode, transcript: String? = null) {
        if (!unboundRecording.end(recordingId)) return
        val originalIntent = autoForRecording(recordingId)
        stopOverlayOwnership()
        if (overlayCleanupPending != null) {
            if (originalIntent != null) clearAutoIntent("MIC_CLEANUP_UNCERTAIN")
            experimentPhase = "REVIEW_MIC_CLEANUP_UNCERTAIN"
            status = "Microphone cleanup uncertain. Restart the service."
            refreshIdleBubble(forceRender = true)
            return
        }
        val draft = if (BuildConfig.SPEECH_INPUT) {
            if (transcript == null || (mode != OutputMode.ENGLISH && !BuildConfig.TRANSLATION_INPUT)) null
            else unboundDraft.beginSpeech(mode, transcript)
        } else unboundDraft.begin(mode)
        if (draft == null) {
            if (originalIntent != null) clearAutoIntent("DRAFT_CREATE_FAILED")
            speechStopEntryUptimeMs = null
            status = "Could not open English review. Start a new recording."
            refreshIdleBubble(forceRender = true)
            return
        }
        speechStopEntryUptimeMs?.let {
            speechLastStopToReviewMs = (SystemClock.uptimeMillis() - it).coerceAtLeast(0L)
        }
        speechStopEntryUptimeMs = null
        draftReviewOwner = null
        idleBubbleVisible = false
        expanded = false
        experimentPhase = "REVIEW_EDITING"
        renderBubble()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) endDraft(draft.id, "SCREEN_OFF")
            }
        }
        try {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
            draftScreenReceiver = receiver
        } catch (_: Exception) {
            endDraft(draft.id, "SCREEN_RECEIVER_FAILED")
            return
        }
        scheduleDraftDeadline(draft.id, 180_000L)
        val intent = Intent(this, ReviewActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ReviewActivity.EXTRA_DRAFT_ID, draft.id)
        }
        if (originalIntent != null) {
            originalIntent.draftId = draft.id
            if (!originalIntent.policy.beginReview(draft.id, SystemClock.elapsedRealtime()))
                clearAutoIntent("REVIEW_BEGIN_REFUSED")
        }
        try { startActivity(intent) }
        catch (_: Exception) { endDraft(draft.id, "REVIEW_LAUNCH_FAILED") }
    }

    internal fun bindDraftReview(id: String, owner: Any): UnboundDraftState.Draft? {
        val draft = unboundDraft.current() ?: return null
        if (startupExperiment != StartupExperiment.REVIEW || draft.id != id ||
            draft.phase != UnboundDraftState.Phase.REVIEW || draftReviewOwner != null ||
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) return null
        draftReviewOwner = owner
        autoForDraft(id)?.reviewOwner = owner
        return draft
    }

    internal fun autoInsertOffered(id: String, owner: Any): Boolean =
        BuildConfig.AUTO_INSERT && autoForDraft(id)?.let {
            it.reviewOwner === owner && it.policy.phase in setOf(
                OriginalDestinationIntent.Phase.ENTERING_REVIEW,
                OriginalDestinationIntent.Phase.REVIEW)
        } == true

    private fun authenticatedAutoReview(binding: AutoDestinationIntent,
        requireEditorFocus: Boolean = false): Boolean {
        val id = binding.draftId ?: return false
        val owner = binding.reviewOwner ?: return false
        val windowId = binding.reviewWindowId ?: return false
        if (draftReviewOwner !== owner || !ReviewActivity.ownsFocusedReview(id, owner)) return false
        val focused = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
                it.displayId == Display.DEFAULT_DISPLAY
        }.singleOrNull() ?: return false
        if (focused.id != windowId) return false
        val root = focused.getRoot(0) ?: return false
        if (root != binding.reviewRoot || root.packageName?.toString() != packageName)
            return false
        if (!requireEditorFocus) return true
        val edit = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return edit == binding.reviewNode && edit.isFocused && edit.isEditable &&
            edit.packageName?.toString() == packageName &&
            edit.viewIdResourceName == "$packageName:id/review_english"
    }

    private fun reviewEntryInputMatches(binding: AutoDestinationIntent,
        editor: android.widget.EditText): Boolean {
        val method = inputMethod as? TrackingInputMethod ?: return false
        val generation = method.generation
        if (generation != binding.generation && generation != binding.generation + 1)
            return false
        if (!method.currentInputStarted) return true
        val info = method.currentInputEditorInfo ?: return false
        return if (generation == binding.generation)
            info.packageName == binding.original.packageName &&
                info.fieldId == binding.original.editorFieldId &&
                info.inputType == binding.original.inputType
        else info.packageName == packageName && info.fieldId == R.id.review_english &&
            info.inputType == editor.inputType
    }

    private fun scheduleReviewEntryCheck(binding: AutoDestinationIntent, wait: ReviewEntryWait) {
        if (wait.callback != null) return
        val callback = Runnable {
            if (autoDestination !== binding || binding.entryWait !== wait) return@Runnable
            wait.callback = null
            val now = SystemClock.elapsedRealtime()
            if (now < wait.startedMs || now - wait.startedMs >= 1_000L) {
                note("AUTO_REVIEW_ENTRY_TIMEOUT_C${wait.checks}")
                clearAutoIntent("REVIEW_WINDOW_READY_TIMEOUT")
                return@Runnable
            }
            wait.checks++
            autoReviewFocused(wait.draftId, wait.owner, wait.decor, wait.editor,
                fromResume = false, fromPoll = true)
        }
        wait.callback = callback
        val scheduled = if (wait.checks == 0) main.post(callback)
            else main.postDelayed(callback, 50L)
        if (!scheduled) {
            wait.callback = null
            note("AUTO_REVIEW_ENTRY_ABORT_SCHEDULE")
            clearAutoIntent("REVIEW_ENTRY_SCHEDULE_FAILED")
        }
    }

    internal fun autoReviewFocused(id: String, owner: Any, decor: View,
        editor: android.widget.EditText, fromResume: Boolean, fromPoll: Boolean = false) {
        val callbackAt = SystemClock.elapsedRealtime()
        val binding = autoForDraft(id) ?: return
        if (draftReviewOwner !== owner || binding.reviewOwner !== owner ||
            !ReviewActivity.ownsFocusedReview(id, owner) || !editor.isFocused) {
            clearAutoIntent("REVIEW_OWNER_OR_FOCUS_LOST")
            return
        }
        val ownWindowId = runCatching { decor.createAccessibilityNodeInfo().windowId }
            .getOrDefault(-1)
        val editorNode = runCatching { editor.createAccessibilityNodeInfo() }.getOrNull()
        val entryWait = binding.entryWait
        val now = SystemClock.elapsedRealtime()
        if (entryWait != null) {
            if (now < entryWait.startedMs || now - entryWait.startedMs >= 1_000L) {
                note("AUTO_REVIEW_ENTRY_TIMEOUT_C${entryWait.checks}")
                clearAutoIntent("REVIEW_WINDOW_READY_TIMEOUT")
                return
            }
            if (binding.policy.phase != OriginalDestinationIntent.Phase.ENTERING_REVIEW ||
                binding.reviewWindowId != null || binding.reviewRoot != null ||
                binding.reviewNode != null || !serviceConnected ||
                (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked ||
                unboundDraft.current()?.takeIf {
                    it.id == id && it.phase == UnboundDraftState.Phase.REVIEW
                } == null || entryWait.draftId != id || entryWait.owner !== owner ||
                entryWait.decor !== decor || entryWait.editor !== editor ||
                !decor.isAttachedToWindow || !editor.isAttachedToWindow ||
                decor.windowToken == null || decor.windowToken != entryWait.token ||
                editor.windowToken != entryWait.token || ownWindowId != entryWait.windowId ||
                editorNode?.windowId != entryWait.windowId ||
                !reviewEntryInputMatches(binding, editor)) {
                note("AUTO_REVIEW_ENTRY_ABORT_LOCAL")
                clearAutoIntent("REVIEW_ENTRY_CHANGED")
                return
            }
        }
        val currentWindows = windows
        val ownWindow = currentWindows.singleOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
                it.displayId == Display.DEFAULT_DISPLAY && it.id == ownWindowId
        }
        val root = ownWindow?.getRoot(0)
        val focused = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val fullProof = ownWindowId >= 0 && root != null &&
            root.packageName?.toString() == packageName && editorNode != null &&
            focused != null && focused == editorNode && focused.isFocused &&
            focused.isEditable &&
            focused.viewIdResourceName == "$packageName:id/review_english" &&
            currentWindows.count { it.isFocused } == 1
        if (!fullProof) {
            val noRemoteOwnWindow = currentWindows.none { it.id == ownWindowId }
            val noFocusedWindow = currentWindows.none { it.isFocused }
            val measuredAbsence = binding.policy.phase ==
                OriginalDestinationIntent.Phase.ENTERING_REVIEW &&
                binding.reviewWindowId == null && binding.reviewRoot == null &&
                binding.reviewNode == null && ownWindowId >= 0 && editorNode != null &&
                editorNode.windowId == ownWindowId && noRemoteOwnWindow && noFocusedWindow
            val localReady = serviceConnected &&
                !(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked &&
                unboundDraft.current()?.takeIf {
                    it.id == id && it.phase == UnboundDraftState.Phase.REVIEW
                } != null && decor.isAttachedToWindow && editor.isAttachedToWindow &&
                decor.windowToken != null && editor.windowToken == decor.windowToken &&
                reviewEntryInputMatches(binding, editor)
            if (measuredAbsence && localReady) {
                if (entryWait == null) {
                    val startedAt = SystemClock.elapsedRealtime()
                    if (startedAt < callbackAt || startedAt - callbackAt >= 1_000L) {
                        note("AUTO_REVIEW_ENTRY_TIMEOUT_C0")
                        clearAutoIntent("REVIEW_WINDOW_READY_TIMEOUT")
                        return
                    }
                    val wait = ReviewEntryWait(id, owner, decor, editor, decor.windowToken!!,
                        ownWindowId, callbackAt)
                    binding.entryWait = wait
                    note("AUTO_REVIEW_ENTRY_WAIT")
                    scheduleReviewEntryCheck(binding, wait)
                } else scheduleReviewEntryCheck(binding, entryWait)
                return
            }
        }
        if (!fullProof) {
            if (entryWait != null) note("AUTO_REVIEW_ENTRY_ABORT_PROOF")
            // Fixed-code, metadata-only proof of the existing refusal; no target text or IDs.
            val focusedApps = currentWindows.filter {
                it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
            }
            val focusedWindows = currentWindows.filter { it.isFocused }
            val focusedKind = when {
                focusedWindows.isEmpty() -> "NONE"
                focusedWindows.size > 1 -> "MULTIPLE"
                focusedWindows[0].type == AccessibilityWindowInfo.TYPE_APPLICATION -> "APP"
                focusedWindows[0].type == AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "IME"
                focusedWindows[0].type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY ->
                    "ACCESSIBILITY_OVERLAY"
                focusedWindows[0].type == AccessibilityWindowInfo.TYPE_SYSTEM -> "SYSTEM"
                else -> "OTHER"
            }
            val focusClass = when {
                focusedApps.isEmpty() -> "NONE"
                focusedApps.size > 1 -> "MULTIPLE"
                focusedApps[0].displayId != Display.DEFAULT_DISPLAY -> "OTHER_DISPLAY"
                focusedApps[0].id == ownWindowId -> "OWN"
                focusedApps[0].id == binding.original.windowId -> "ORIGINAL"
                else -> "OTHER"
            }
            val method = inputMethod as? TrackingInputMethod
            val generationClass = when (method?.generation) {
                binding.generation -> "ORIGINAL"
                binding.generation + 1 -> "OWN"
                else -> "OTHER"
            }
            val firstFailed = when {
                ownWindowId < 0 -> "WINDOW_ID"
                root == null -> "ROOT"
                root.packageName?.toString() != packageName -> "PACKAGE"
                editorNode == null -> "EDITOR_NODE"
                focused == null -> "FOCUSED_NODE"
                focused != editorNode -> "NODE_EQUALITY"
                !focused.isFocused -> "NODE_FOCUS"
                !focused.isEditable -> "EDITABLE"
                focused.viewIdResourceName != "$packageName:id/review_english" ->
                    "RESOURCE_ID"
                else -> "FOCUSED_WINDOW_COUNT"
            }
            fun bit(value: Boolean) = if (value) 1 else 0
            note("AUTO_REVIEW_PROOF_${if (fromPoll) "ENTRY_POLL" else if (fromResume)
                "RESUME" else "WINDOW_FOCUS"}" +
                "_GEN_${generationClass}_IMESTART_${bit(method?.currentInputStarted == true)}" +
                "_IMEOWN_${bit(method?.currentInputEditorInfo?.packageName == packageName)}" +
                "_FOCUSED_APPS_${if (focusedApps.size > 1) "MULTIPLE" else focusedApps.size}" +
                "_FOCUS_${focusClass}_FOCUSED_KIND_${focusedKind}" +
                "_WINDOW_VALID_${bit(ownWindowId >= 0)}" +
                "_WINDOW_PRESENT_${bit(currentWindows.any {
                    it.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                        it.displayId == Display.DEFAULT_DISPLAY && it.id == ownWindowId
                })}_WINDOW_MATCH_${bit(ownWindow != null)}" +
                "_ROOT_${bit(root != null)}_PACKAGE_${bit(root?.packageName?.toString() == packageName)}" +
                "_EDITOR_${bit(editorNode != null)}_FOCUSED_${bit(focused != null)}" +
                "_EQUAL_${bit(focused != null && focused == editorNode)}" +
                "_EDITOR_WINDOW_${bit(editorNode?.windowId == ownWindowId)}" +
                "_FOCUSED_WINDOW_${bit(focused?.windowId == ownWindowId)}" +
                "_NODE_FOCUS_${bit(focused?.isFocused == true)}" +
                "_EDITABLE_${bit(focused?.isEditable == true)}" +
                "_RESOURCE_${bit(focused?.viewIdResourceName == "$packageName:id/review_english")}" +
                "_FIRST_$firstFailed")
            clearAutoIntent("REVIEW_WINDOW_UNVERIFIED")
            return
        }
        if (entryWait != null) {
            val acceptedAt = SystemClock.elapsedRealtime()
            if (acceptedAt < entryWait.startedMs || acceptedAt - entryWait.startedMs >= 1_000L) {
                note("AUTO_REVIEW_ENTRY_TIMEOUT_C${entryWait.checks}")
                clearAutoIntent("REVIEW_WINDOW_READY_TIMEOUT")
                return
            }
            note("AUTO_REVIEW_ENTRY_READY_C${entryWait.checks}_MS${acceptedAt - entryWait.startedMs}")
            clearReviewEntryWait(binding)
        }
        if (binding.reviewWindowId != null) {
            if (binding.reviewWindowId != ownWindowId || binding.reviewRoot != root ||
                binding.reviewNode != focused) clearAutoIntent("REVIEW_WINDOW_CHANGED")
            return
        }
        binding.reviewWindowId = ownWindowId
        binding.reviewRoot = root
        binding.reviewNode = focused
        if (!binding.policy.reviewFocused(id, SystemClock.elapsedRealtime()))
            clearAutoIntent("REVIEW_FOCUS_REFUSED")
        else note("AUTO_REVIEW_FOCUSED")
    }

    internal fun autoReviewWindowFocus(id: String, owner: Any, hasFocus: Boolean,
        decor: View, editor: android.widget.EditText) {
        val binding = autoForDraft(id) ?: return
        if (owner !== binding.reviewOwner) return clearAutoIntent("REVIEW_OWNER_CHANGED")
        if (hasFocus) autoReviewFocused(id, owner, decor, editor, fromResume = false)
        else if (binding.policy.phase != OriginalDestinationIntent.Phase.RETURNING)
            clearAutoIntent("REVIEW_FOCUS_LOST")
    }

    internal fun beginAutoReturn(id: String, owner: Any) {
        val binding = autoForDraft(id) ?: return
        if (unboundDraft.current()?.takeIf {
                it.id == id && it.phase == UnboundDraftState.Phase.READY
            } == null || binding.reviewOwner !== owner ||
            !authenticatedAutoReview(binding)) {
            clearAutoIntent("RETURN_OWNER_UNVERIFIED")
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (!binding.policy.beginReturn(id, now, ownerStillFocused = true))
            clearAutoIntent("RETURN_BEGIN_REFUSED")
        else {
            binding.returnStartedMs = now
            note("AUTO_RETURN_REQUESTED")
        }
    }

    internal fun updateDraftEnglish(id: String, owner: Any, english: String): Boolean =
        draftReviewOwner === owner && unboundDraft.edit(id, english)

    internal fun confirmDraftReview(id: String, owner: Any): UnboundDraftState.Confirm {
        if (startupExperiment != StartupExperiment.REVIEW || draftReviewOwner !== owner ||
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked)
            return UnboundDraftState.Confirm.STALE
        autoForDraft(id)?.takeIf {
            it.entryWait != null || it.policy.phase != OriginalDestinationIntent.Phase.REVIEW
        }?.let {
            note("AUTO_REVIEW_ENTRY_EARLY_CONFIRM")
            clearAutoIntent("REVIEW_NOT_ATTESTED")
        }
        val result = unboundDraft.confirm(id)
        if (result == UnboundDraftState.Confirm.READY) experimentPhase = "REVIEW_HANDOFF"
        if (result == UnboundDraftState.Confirm.TRANSLATING) {
            val draft = unboundDraft.current() ?: return UnboundDraftState.Confirm.STALE
            if (!BuildConfig.TRANSLATION_INPUT || !startDraftTranslation(draft, owner)) {
                unboundDraft.translationFailed(id, draft.translationAttempt)
                return UnboundDraftState.Confirm.TRANSLATION_ERROR
            }
        }
        return result
    }

    private fun startDraftTranslation(draft: UnboundDraftState.Draft, owner: Any): Boolean {
        if (translationJob != null || draft.phase != UnboundDraftState.Phase.TRANSLATING ||
            draftReviewOwner !== owner || draft.mode == OutputMode.ENGLISH) return false
        val generation = ++translationGeneration
        val started = SystemClock.uptimeMillis()
        val attempt = draft.translationAttempt
        lateinit var job: KoreanTranslator.Job
        return try {
            job = KoreanTranslator(this).newJob(draft.mode, draft.english) { result ->
                main.post {
                    if (translationJob !== job || generation != translationGeneration ||
                        draftReviewOwner !== owner ||
                        unboundDraft.current()?.takeIf {
                            it.id == draft.id && it.phase == UnboundDraftState.Phase.TRANSLATING &&
                                it.translationAttempt == attempt
                        } == null) return@post
                    translationJob = null
                    translationStartedUptimeMs = null
                    translationLastDurationMs = (SystemClock.uptimeMillis() - started).coerceAtLeast(0L)
                    if ((getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) {
                        translationCancels++
                        endDraft(draft.id, "TRANSLATION_LOCKED")
                        return@post
                    }
                    when (result) {
                        is KoreanTranslator.Result.Text -> {
                            if (!unboundDraft.translationSucceeded(draft.id, attempt, result.value)) {
                                translationErrors++
                                if (unboundDraft.translationFailed(draft.id, attempt)) {
                                    if (!ReviewActivity.translationFailed(draft.id,
                                            "Translation response could not be used. Tap Translate to retry."))
                                        endDraft(draft.id, "TRANSLATION_REVIEW_LOST")
                                } else endDraft(draft.id, "TRANSLATION_RESULT_REJECTED")
                                return@post
                            }
                            translationSuccesses++
                            translationLastUsage = result.usage
                            experimentPhase = "TRANSLATION_READY"
                            if (!ReviewActivity.finishReadyDraft(draft.id))
                                endDraft(draft.id, "TRANSLATION_REVIEW_LOST")
                        }
                        is KoreanTranslator.Result.Error -> {
                            translationErrors++
                            note("TRANSLATION_${result.code.name}")
                            if (unboundDraft.translationFailed(draft.id, attempt)) {
                                if (!ReviewActivity.translationFailed(draft.id,
                                        translationErrorMessage(result.code)))
                                    endDraft(draft.id, "TRANSLATION_REVIEW_LOST")
                            } else endDraft(draft.id, "TRANSLATION_STATE_CHANGED")
                        }
                    }
                }
            }
            translationJob = job
            translationStartedUptimeMs = started
            translationRequests++
            translationLastDurationMs = null
            translationLastUsage = null
            experimentPhase = "TRANSLATING"
            job.start()
            true
        } catch (_: Exception) {
            translationErrors++
            cancelTranslationJob(countCancellation = false)
            false
        }
    }

    private fun translationErrorMessage(code: KoreanTranslator.Code): String = when (code) {
        KoreanTranslator.Code.KEY_UNAVAILABLE -> "API key unavailable. Check KoScribe settings, then tap Translate."
        KoreanTranslator.Code.AUTH ->
            "Translation access was rejected. Check API key permissions and model access."
        KoreanTranslator.Code.RATE_LIMIT -> "Translation limit reached. Try again later with Translate."
        KoreanTranslator.Code.TIMEOUT -> "Translation timed out. Tap Translate to retry."
        KoreanTranslator.Code.INVALID_INPUT -> "Edit English to 512 characters or fewer, then tap Translate."
        KoreanTranslator.Code.REFUSED -> "Translation was refused. Edit English or cancel."
        KoreanTranslator.Code.INCOMPLETE -> "Translation was incomplete. Tap Translate to retry."
        else -> "Translation failed (${code.name}). Edit English or tap Translate."
    }

    private fun cancelTranslationJob(countCancellation: Boolean = true) {
        translationGeneration++
        if (translationJob != null) {
            if (countCancellation) translationCancels++
            translationStartedUptimeMs?.let {
                translationLastDurationMs = (SystemClock.uptimeMillis() - it).coerceAtLeast(0L)
            }
        }
        translationJob?.cancel()
        translationJob = null
        translationStartedUptimeMs = null
    }

    private fun scheduleDraftDeadline(id: String, delayMs: Long) {
        draftDeadline?.let(main::removeCallbacks)
        val deadline = Runnable { endDraft(id, "DEADLINE") }
        draftDeadline = deadline
        main.postDelayed(deadline, delayMs)
    }

    internal fun cancelDraftReview(id: String, owner: Any) {
        if (draftReviewOwner === owner) endDraft(id, "USER_CANCELLED")
    }

    internal fun reviewActivityFinished(id: String, owner: Any) {
        if (draftReviewOwner !== owner) return
        val originalIntent = autoForDraft(id)
        draftReviewOwner = null
        val draft = unboundDraft.current()
        if (draft?.id != id || draft.phase != UnboundDraftState.Phase.READY) {
            endDraft(id, "REVIEW_EXIT_WITHOUT_CONFIRMATION")
            return
        }
        if (originalIntent != null &&
            originalIntent.policy.phase != OriginalDestinationIntent.Phase.RETURNING)
            clearAutoIntent("REVIEW_RETURN_NOT_ARMED")
        experimentPhase = "REVIEW_READY"
        val automaticFallback = autoFallbackDraftId == id
        status = if (automaticFallback)
            "Automatic insertion unavailable. Choose the field, then tap Insert."
        else "Tap the intended field, then Insert, or Copy & close."
        compactResultNotice = if (automaticFallback)
            "Automatic insertion unavailable. Choose the field, then tap Insert." else null
        expanded = true
        refreshIdleBubble(forceRender = true)
        if (autoForDraft(id) === originalIntent && originalIntent != null)
            pollAutoReturn(originalIntent)
    }

    /** Waits for metadata and a placed result host, without reading editor content. */
    private fun pollAutoReturn(binding: AutoDestinationIntent) {
        if (autoDestination !== binding || binding.returnAttempted) return
        val id = binding.draftId ?: return clearAutoIntent("RETURN_DRAFT_MISSING")
        val now = SystemClock.elapsedRealtime()
        val started = binding.returnStartedMs
        if (started == null || now < started || now - started > 2_000L ||
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked) {
            clearAutoIntent("RETURN_NOT_READY")
            return
        }
        if (unboundDraft.current()?.takeIf {
                it.id == id && it.phase == UnboundDraftState.Phase.READY
            } == null || draftReviewOwner != null || !serviceConnected) {
            clearAutoIntent("RETURN_OWNER_OR_DRAFT_LOST")
            return
        }
        val method = inputMethod as? TrackingInputMethod
        val info = method?.currentInputEditorInfo
        val returnedInput = method?.currentInputStarted == true &&
            method.generation == binding.generation + 2 &&
            info?.packageName == binding.original.packageName &&
            info.fieldId == binding.original.editorFieldId &&
            info.inputType == binding.original.inputType
        val focusedApps = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedApps.size > 1 || focusedApps.any { it.displayId != Display.DEFAULT_DISPLAY }) {
            clearAutoIntent("RETURN_FOCUS_AMBIGUOUS")
            return
        }
        val focused = focusedApps.singleOrNull()
        val focusedRoot = focused?.getRoot(0)
        val originalWindow = focused != null && focused.id == binding.original.windowId &&
            focusedRoot == binding.root
        val ownReviewWindow = focused != null && focused.id == binding.reviewWindowId &&
            focusedRoot == binding.reviewRoot &&
            focusedRoot?.packageName?.toString() == packageName
        if (focused != null && !originalWindow && !ownReviewWindow) {
            clearAutoIntent("RETURN_FOCUS_CHANGED")
            return
        }
        if (method?.generation == binding.generation + 2 &&
            ((info != null && (info.packageName != binding.original.packageName ||
                info.fieldId != binding.original.editorFieldId ||
                info.inputType != binding.original.inputType)) ||
                ownReviewWindow)) {
            clearAutoIntent("RETURN_TARGET_CHANGED")
            return
        }
        val host = bubble
        val hostReady = host != null && host.isAttachedToWindow && host.isShown &&
            host.isEnabled && host.windowToken != null && idleBubbleGeometry != null &&
            host.findViewById<Button>(R.id.ready_insert)?.isEnabled == true
        if (returnedInput && originalWindow && hostReady &&
            binding.policy.phase == OriginalDestinationIntent.Phase.RETURNING) {
            binding.returnAttempted = true
            insertDraft(id, binding)
            return
        }
        val poll = Runnable { pollAutoReturn(binding) }
        binding.returnPoll = poll
        main.postDelayed(poll, 80L)
    }

    private fun clearDraftOwnership() {
        draftReviewOwner = null
        draftScreenReceiver?.let { runCatching { unregisterReceiver(it) } }
        draftScreenReceiver = null
        draftDeadline?.let(main::removeCallbacks)
        draftDeadline = null
    }

    private fun endDraft(id: String, reason: String, completed: Boolean = false) {
        if (!unboundDraft.end(id)) return
        if (autoFallbackDraftId == id) autoFallbackDraftId = null
        if (autoForDraft(id) != null) clearAutoIntent(if (completed) "DRAFT_COMPLETED" else "DRAFT_ENDED")
        cancelTranslationJob()
        compactResultNotice = null
        clearPostCommitProbe()
        ReviewActivity.closeDraft(id)
        freshDestination?.takeIf { it.draftId == id }?.let { guard.cancel(it.id); guard.end(it.id) }
        freshDestination = null
        resetFreshComposition()
        clearDraftOwnership()
        experimentPhase = if (completed) "REVIEW_COMPLETED" else "REVIEW_CANCELLED"
        note("REVIEW_ENDED_$reason")
        if (completed) {
            idleBubblePolicy.hideUntilNextImeCycle()
            idleBubbleVisible = false
            expanded = false
            renderBubble()
        } else {
            expanded = false
            status = if (reason == "DEADLINE") "Review expired." else "Review cancelled."
            refreshIdleBubble(forceRender = true)
        }
    }

    private fun copyDraft(id: String) {
        if (freshDestination != null) return
        val draft = unboundDraft.current() ?: return
        if (draft.id != id || draft.phase !in setOf(UnboundDraftState.Phase.READY,
                UnboundDraftState.Phase.UNCERTAIN)) return
        if (autoForDraft(id) != null)
            clearAutoIntent("MANUAL_COPY_SELECTED", renderReady = false)
        val result = draft.result ?: return
        val clip = ClipData.newPlainText("KoScribe sample", result)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        if (runCatching {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            }.isSuccess) {
            commitConfirmation = "COPIED"
            endDraft(id, "COPIED", completed = true)
        } else {
            status = "Copy failed. Result remains available."
            compactResultNotice = "Copy failed. Result remains available."
            refreshIdleBubble(forceRender = true)
        }
    }

    /** A click owns one short transaction; no prior recording/review target is inherited. */
    private fun insertDraft(draftId: String, automatic: AutoDestinationIntent? = null) {
        if (automatic == null && autoForDraft(draftId) != null)
            clearAutoIntent("MANUAL_INSERT_SELECTED", renderReady = false)
        val draft = unboundDraft.current() ?: return
        if (startupExperiment != StartupExperiment.REVIEW || draft.id != draftId ||
            draft.phase != UnboundDraftState.Phase.READY || draftReviewOwner != null ||
            freshDestination != null) return
        if (automatic != null && (autoDestination !== automatic ||
                automatic.draftId != draftId ||
                automatic.policy.phase != OriginalDestinationIntent.Phase.RETURNING)) {
            clearAutoIntent("AUTO_NOT_ELIGIBLE")
            return
        }
        val result = draft.result ?: return
        if (result.any { it == '\n' || it == '\r' })
            return if (automatic == null) refuseFreshInsertion("RESULT_NOT_SINGLE_LINE")
                else clearAutoIntent("RESULT_NOT_SINGLE_LINE")
        val host = bubble?.takeIf { it.isAttachedToWindow && it.isShown && it.isEnabled }
        if (host == null) return if (automatic == null) refuseFreshInsertion("BUBBLE_UNAVAILABLE")
            else clearAutoIntent("AUTO_HOST_UNAVAILABLE")
        host.findViewById<Button>(R.id.ready_insert)?.isEnabled = false
        host.findViewById<Button>(R.id.ready_copy)?.isEnabled = false
        val token = host.windowToken
        val hostWindowId = runCatching { host.createAccessibilityNodeInfo().windowId }.getOrDefault(-1)
        if (token == null || hostWindowId < 0)
            return if (automatic == null) refuseFreshInsertion("BUBBLE_UNVERIFIED")
                else clearAutoIntent("AUTO_HOST_UNVERIFIED")

        resetFreshComposition()
        if (automatic == null) destinationCaptureCount++
        val first = observeEditor()
        if (automatic != null) {
            val verdict = automatic.policy.checkReturn(first?.snapshot,
                first?.generation ?: automatic.generation + 2,
                first?.let { it.node == automatic.node && it.root == automatic.root } == true,
                SystemClock.elapsedRealtime())
            if (verdict != OriginalDestinationIntent.ReturnVerdict.READY) {
                clearAutoIntent("RETURN_PROOF_REFUSED")
                return
            }
        }
        if (first == null) return refuseFreshInsertion(observationCode.name)
        val expected = CommitConfirmation.expected(first.snapshot, result)
            ?: return if (automatic == null) refuseFreshInsertion("RESULT_TOO_LONG_OR_UNSAFE")
                else clearAutoIntent("AUTO_RESULT_UNSAFE")
        val session = guard.begin(draft.mode, first.snapshot, SystemClock.elapsedRealtime())
            ?: return if (automatic == null) refuseFreshInsertion("CAPTURE_REJECTED")
                else clearAutoIntent("AUTO_CAPTURE_REFUSED")
        if (automatic != null) {
            destinationCaptureCount++
            autoAttemptCount++
        }
        if (!guard.publish(session.id, result)) {
            guard.end(session.id)
            return if (automatic == null) refuseFreshInsertion("RESULT_REJECTED")
                else clearAutoIntent("AUTO_RESULT_REJECTED")
        }
        val transaction = FreshDestination(draftId, session.id, first.node, first.root,
            first.generation, expected, host, token, hostWindowId, automatic)
        freshDestination = transaction
        // Yield once so queued editor/window events can terminally invalidate this
        // capture before the final synchronous reobservation and one commit.
        main.post { commitFreshDestination(transaction, result) }
    }

    private fun commitFreshDestination(transaction: FreshDestination, result: String) {
        if (freshDestination !== transaction || transaction.phase != DestinationPhase.PRECOMMIT ||
            unboundDraft.current()?.takeIf {
                it.id == transaction.draftId && it.phase == UnboundDraftState.Phase.READY &&
                    it.result == result
            } == null) return
        val second = observeEditor()
        val same = second != null && sameFreshTarget(transaction, second)
        val automatic = transaction.autoIntent
        if (automatic != null && (autoDestination !== automatic || !same ||
                !automatic.policy.checkPrecommit(second?.snapshot,
                    second?.generation ?: automatic.generation + 2,
                    second?.let { it.node == automatic.node && it.root == automatic.root } == true,
                    SystemClock.elapsedRealtime()))) {
            guard.end(transaction.id)
            freshDestination = null
            resetFreshComposition()
            if (autoDestination === automatic) clearAutoIntent("AUTO_PRECOMMIT_REFUSED")
            else refreshIdleBubble(forceRender = true)
            return
        }
        val decision = guard.reserveCommit(transaction.id,
            if (same) second?.snapshot else null, SystemClock.elapsedRealtime())
        if (decision != GuardDecision.Ready) {
            note("FRESH_INSERT_REFUSED")
            guard.end(transaction.id)
            freshDestination = null
            resetFreshComposition()
            if (automatic != null) clearAutoIntent("AUTO_GUARD_REFUSED")
            else refuseFreshInsertion(if (second == null) observationCode.name else
                (decision as GuardDecision.Refused).reason.name)
            return
        }
        if (automatic != null) {
            if (autoDestination !== automatic || !automatic.policy.consume(second?.snapshot,
                    second?.generation ?: automatic.generation + 2,
                    second?.let { it.node == automatic.node && it.root == automatic.root } == true,
                    SystemClock.elapsedRealtime())) {
                guard.end(transaction.id)
                freshDestination = null
                resetFreshComposition()
                if (autoDestination === automatic) clearAutoIntent("AUTO_CONSUME_REFUSED")
                else refreshIdleBubble(forceRender = true)
                return
            }
            clearAutoIntent("AUTO_CONSUMED", fallback = false)
        }
        // The guard has consumed the attempt before any editor IPC. No later tap can
        // get another Insert for this draft, even if the IPC throws or proves nothing.
        if (!unboundDraft.markAttempted(transaction.draftId)) {
            guard.end(transaction.id)
            freshDestination = null
            resetFreshComposition()
            return refuseFreshInsertion("DRAFT_CHANGED")
        }
        transaction.phase = DestinationPhase.OBSERVING
        commitConfirmation = "PENDING"
        experimentPhase = "REVIEW_COMMIT_OBSERVING"
        try {
            commitRequestCount++
            transaction.commitRequestedUptimeMs = SystemClock.uptimeMillis()
            note("FRESH_COMMIT_REQUESTED_ONCE")
            second!!.connection.commitText(result, 1, null)
        } catch (_: Exception) {
            finishUncertain(transaction, "COMMIT_EXCEPTION")
            return
        }
        // An editor may apply the void commit asynchronously. Read only; never retry.
        main.post { verifyFreshCommit(transaction, 0) }
    }

    private fun sameFreshHost(transaction: FreshDestination): Boolean =
        bubble === transaction.host && transaction.host.isAttachedToWindow &&
            transaction.host.windowToken == transaction.hostToken &&
            runCatching { transaction.host.createAccessibilityNodeInfo().windowId }
                .getOrDefault(-1) == transaction.hostWindowId

    private fun sameFreshTarget(transaction: FreshDestination, observed: Observation): Boolean =
        sameFreshHost(transaction) && observed.node == transaction.node &&
            observed.root == transaction.root && observed.generation == transaction.generation &&
            guard.session(transaction.id)?.target?.let { original ->
                val now = observed.snapshot
                now.packageName != packageName && now.packageName == original.packageName &&
                    now.windowId == original.windowId && now.uniqueNodeId == original.uniqueNodeId &&
                    now.viewId == original.viewId && now.className == original.className &&
                    now.editorFieldId == original.editorFieldId && now.inputType == original.inputType
            } == true

    /** Diagnostic equivalent of sameFreshTarget before composition permits a safe snapshot. */
    private fun diagnosticFreshIdentity(transaction: FreshDestination, node: AccessibilityNodeInfo,
        root: AccessibilityNodeInfo, method: TrackingInputMethod, info: EditorInfo,
        unique: String): Boolean =
        sameFreshHost(transaction) && node == transaction.node && root == transaction.root &&
            method.generation == transaction.generation &&
            guard.session(transaction.id)?.target?.let { original ->
                info.packageName != packageName && info.packageName == original.packageName &&
                    node.windowId == original.windowId && unique == original.uniqueNodeId &&
                    node.viewIdResourceName == original.viewId &&
                    node.className?.toString() == original.className &&
                    info.fieldId == original.editorFieldId && info.inputType == original.inputType
            } == true

    private fun verifyFreshCommit(transaction: FreshDestination, poll: Int) {
        if (freshDestination !== transaction || transaction.phase != DestinationPhase.OBSERVING)
            return
        if (!sameFreshHost(transaction)) return finishUncertain(transaction, "HOST_CHANGED")
        val observed = observeEditor(transaction.expected.beforeCount,
            transaction.expected.afterCount, postCommit = true)
        if (observed == null || !sameFreshTarget(transaction, observed))
            return finishUncertain(transaction, "TARGET_CHANGED")
        val verdict = CommitConfirmation.verdict(transaction.expected, observed.snapshot)
        if (verdict == CommitConfirmation.PostCommitVerdict.CONFIRMED) {
            guard.end(transaction.id)
            freshDestination = null
            resetFreshComposition()
            commitConfirmation = "CONFIRMED"
            endDraft(transaction.draftId, "INSERT_CONFIRMED", completed = true)
            return
        }
        if (verdict == CommitConfirmation.PostCommitVerdict.COMPOSED_UNCONFIRMED)
            return finishUncertain(transaction, "COMPOSED_POSTCONTEXT_UNCONFIRMED")
        val original = guard.session(transaction.id)?.target
            ?: return finishUncertain(transaction, "ORIGINAL_CONTEXT_LOST")
        val now = observed.snapshot
        val oldStartsAt = original.offset - now.offset
        val unchanged = oldStartsAt >= 0 &&
            oldStartsAt + original.surroundingText.length <= now.surroundingText.length &&
            now.surroundingText.regionMatches(oldStartsAt, original.surroundingText, 0,
                original.surroundingText.length) &&
            now.offset + now.selectionStart == original.offset + original.selectionStart &&
            now.selectionStart == now.selectionEnd
        if (!unchanged || poll >= 4)
            return finishUncertain(transaction, "POSTCONTEXT_UNCONFIRMED")
        main.postDelayed({ verifyFreshCommit(transaction, poll + 1) }, 120L)
    }

    private fun finishUncertain(transaction: FreshDestination, code: String) {
        if (freshDestination !== transaction) return
        val original = guard.session(transaction.id)?.target
        val probeKind = when (observationCode) {
            ObservationCode.NODE_CARET_MISMATCH -> PostCommitProbeKind.CARET
            ObservationCode.ACTIVE_COMPOSITION -> PostCommitProbeKind.COMPOSITION
            else -> null
        }
        val probe = if (BuildConfig.DEBUG && startupExperiment == StartupExperiment.REVIEW &&
            code == "TARGET_CHANGED" && probeKind != null &&
            original?.packageName == "com.koscribe.smoke") {
            val expected = transaction.expected
            PostCommitProbe(probeKind, transaction.draftId, transaction.node, transaction.root,
                original.windowId, original.editorFieldId, original.inputType, transaction.generation,
                expected.oldOffset, expected.caret, expected.text.length,
                digestContext(expected.text), transaction.commitRequestedUptimeMs)
        } else null
        guard.end(transaction.id)
        freshDestination = null
        resetFreshComposition()
        commitConfirmation = "UNCERTAIN"
        experimentPhase = "REVIEW_UNCERTAIN"
        note("FRESH_COMMIT_UNCERTAIN_$code")
        status = "Insertion could not be confirmed. Check the field before Copy or manual paste."
        compactResultNotice = "Insertion unconfirmed. Check the field before Copy or manual paste."
        refreshIdleBubble(forceRender = true)
        if (probe != null) beginPostCommitProbe(probe)
    }

    /** Diagnostic only. A terminal synthetic transaction never becomes eligible again. */
    private fun beginPostCommitProbe(probe: PostCommitProbe) {
        clearPostCommitProbe()
        postCommitProbe = probe
        probe.startedUptimeMs = SystemClock.uptimeMillis()
        readPostCommitProbe(probe, "T0")
        if (postCommitProbe !== probe) return
        schedulePostCommitProbe(probe, 1)
    }

    private fun schedulePostCommitProbe(probe: PostCommitProbe, index: Int) {
        if (postCommitProbe !== probe) return
        val targetElapsed = if (index == 1) 150L else 500L
        val delayed = Runnable {
            if (postCommitProbe !== probe) return@Runnable
            if (probe.kind == PostCommitProbeKind.COMPOSITION &&
                SystemClock.uptimeMillis() - probe.startedUptimeMs > 750L) {
                abortPostCommitProbe("DEADLINE")
                return@Runnable
            }
            if (unboundDraft.current()?.takeIf { it.id == probe.draftId &&
                    it.phase == UnboundDraftState.Phase.UNCERTAIN } == null) {
                abortPostCommitProbe("DRAFT_CHANGED")
                return@Runnable
            }
            readPostCommitProbe(probe, "T$index")
            if (postCommitProbe === probe) {
                if (probe.kind == PostCommitProbeKind.COMPOSITION && index == 1)
                    schedulePostCommitProbe(probe, 2)
                else {
                    note("${probe.kind.prefix}_DONE")
                    clearPostCommitProbe()
                }
            }
        }
        probe.delayed = delayed
        val delay = if (probe.kind == PostCommitProbeKind.CARET) 150L
            else (probe.startedUptimeMs + targetElapsed - SystemClock.uptimeMillis())
                .coerceAtLeast(0L)
        main.postDelayed(delayed, delay)
    }

    private fun clearPostCommitProbe() {
        postCommitProbe?.delayed?.let(main::removeCallbacks)
        postCommitProbe = null
    }

    private fun abortPostCommitProbe(reason: String) {
        val probe = postCommitProbe ?: return
        note("${probe.kind.prefix}_ABORT_$reason")
        clearPostCommitProbe()
    }

    private fun digestContext(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    /** Bounded numeric metadata only; never retain or print span objects or text. */
    private fun composingSpanMetrics(value: CharSequence?): ComposingSpanMetrics {
        val styled = value as? Spanned ?: return ComposingSpanMetrics(0, -1, -1,
            firstZeroLength = false, more = false, error = true)
        return runCatching {
            var count = 0
            var firstStart = -1
            var firstEnd = -1
            var more = false
            for (span in styled.getSpans(0, styled.length, Any::class.java)) {
                if (styled.getSpanFlags(span) and Spanned.SPAN_COMPOSING == 0) continue
                count++
                if (count == 1) {
                    firstStart = styled.getSpanStart(span)
                    firstEnd = styled.getSpanEnd(span)
                }
                if (count > 16) { more = true; break }
            }
            ComposingSpanMetrics(count.coerceAtMost(16), firstStart, firstEnd,
                firstStart >= 0 && firstStart == firstEnd, more, error = false)
        }.getOrElse { ComposingSpanMetrics(0, -1, -1,
            firstZeroLength = false, more = false, error = true) }
    }

    private fun readPostCommitProbe(probe: PostCommitProbe, label: String) {
        // All reads stay inside the own synthetic editor. Never traverse another app.
        if (postCommitProbe !== probe) return
        val elapsed = (SystemClock.uptimeMillis() - probe.commitRequestedUptimeMs).coerceAtLeast(0L)
        fun stop(reason: String) {
            val ended = (SystemClock.uptimeMillis() - probe.commitRequestedUptimeMs).coerceAtLeast(0L)
            note("${probe.kind.prefix}_${label}_MS_${elapsed}_ENDMS_${ended}_$reason")
            abortPostCommitProbe(reason)
        }
        val interactive = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive
        val unlocked = !(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        if (!interactive || !unlocked) {
            stop("DEVICE_NOT_READY")
            return
        }
        val method = inputMethod as? TrackingInputMethod
        val info = method?.currentInputEditorInfo
        if (method == null || info == null || !method.currentInputStarted ||
            info.packageName != "com.koscribe.smoke" || info.fieldId != probe.fieldId ||
            info.inputType != probe.inputType || !safeInputType(info.inputType)) {
            stop("INPUT_MISMATCH")
            return
        }
        val focusedWindow = windows.singleOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedWindow?.id != probe.windowId ||
            focusedWindow.displayId != Display.DEFAULT_DISPLAY) {
            stop("WINDOW_MISMATCH")
            return
        }
        val root = runCatching { focusedWindow.getRoot(0) }.getOrNull()
        if (root?.packageName?.toString() != "com.koscribe.smoke") {
            stop("ROOT_MISMATCH")
            return
        }
        val rootSame = root == probe.root
        val generationSame = method.generation == probe.generation
        if (!rootSame || !generationSame) {
            stop("IDENTITY_MISMATCH_R${rootSame.b()}_G${generationSame.b()}")
            return
        }
        val rootRefreshStart = SystemClock.uptimeMillis()
        val rootRefreshed = runCatching { root.refresh() }.getOrDefault(false)
        val rootRefreshDuration = SystemClock.uptimeMillis() - rootRefreshStart
        if (!rootRefreshed || root.packageName?.toString() != "com.koscribe.smoke" ||
            root.windowId != probe.windowId) {
            stop("ROOT_REFRESH_FAILED_RRMS_$rootRefreshDuration")
            return
        }
        val focus = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
        if (focus?.packageName?.toString() != "com.koscribe.smoke" || !focus.isFocused ||
            !focus.isEditable || !focus.isEnabled || focus.isPassword ||
            !safeInputType(focus.inputType)) {
            stop("FOCUS_MISMATCH")
            return
        }
        val nodeSame = focus == probe.node
        val editorSame = info.fieldId == probe.fieldId && info.inputType == probe.inputType &&
            focus.inputType == probe.inputType && focus.windowId == probe.windowId
        if (!nodeSame || !editorSame) {
            stop("IDENTITY_MISMATCH_N${nodeSame.b()}_E${editorSame.b()}")
            return
        }
        val connection = method.currentInputConnection
        if (connection == null) {
            stop("CONNECTION_MISSING")
            return
        }
        val surrounding = runCatching {
            connection.getSurroundingText(probe.expectedLength, probe.expectedLength,
                InputConnection.GET_TEXT_WITH_STYLES)
        }.getOrNull()
        val connectionText = surrounding?.text
        val connectionContextMatches = surrounding?.offset == probe.expectedOffset &&
            connectionText?.length == probe.expectedLength &&
            connectionText?.let { digestContext(it.toString()).contentEquals(probe.expectedDigest) } == true
        val connectionCaretMatches = surrounding?.offset == probe.expectedOffset &&
            surrounding?.selectionStart == probe.expectedCaret &&
            surrounding?.selectionEnd == probe.expectedCaret
        val beforeStart = focus.textSelectionStart
        val beforeEnd = focus.textSelectionEnd
        val beforeLength = focus.text?.length ?: -1
        val beforeContextMatches = !focus.isShowingHintText && beforeLength == probe.expectedLength &&
            focus.text?.let { digestContext(it.toString()).contentEquals(probe.expectedDigest) } == true
        val focusRefreshStart = SystemClock.uptimeMillis()
        val refreshed = runCatching { focus.refresh() }.getOrDefault(false)
        val focusRefreshDuration = SystemClock.uptimeMillis() - focusRefreshStart
        if (!refreshed || focus != probe.node || focus.packageName?.toString() != "com.koscribe.smoke" ||
            focus.windowId != probe.windowId || !focus.isFocused || !focus.isEditable ||
            !focus.isEnabled || focus.isPassword || focus.inputType != probe.inputType ||
            method.generation != probe.generation ||
            method.currentInputEditorInfo?.fieldId != probe.fieldId ||
            method.currentInputEditorInfo?.packageName != "com.koscribe.smoke") {
            stop("FOCUS_REFRESH_FAILED_RRMS_${rootRefreshDuration}_FRMS_${focusRefreshDuration}" +
                "_B${beforeStart}_${beforeEnd}_${beforeLength}_${beforeContextMatches.b()}")
            return
        }
        val afterStart = focus.textSelectionStart
        val afterEnd = focus.textSelectionEnd
        val afterLength = focus.text?.length ?: -1
        val afterContextMatches = !focus.isShowingHintText && afterLength == probe.expectedLength &&
            focus.text?.let { digestContext(it.toString()).contentEquals(probe.expectedDigest) } == true
        val ended = (SystemClock.uptimeMillis() - probe.commitRequestedUptimeMs).coerceAtLeast(0L)
        note("${probe.kind.prefix}_${label}_MS_${elapsed}_ENDMS_${ended}" +
            "_RRMS_${rootRefreshDuration}_FRMS_${focusRefreshDuration}" +
            "_R${rootSame.b()}_RR${rootRefreshed.b()}" +
            "_N${nodeSame.b()}_G${generationSame.b()}_E${editorSame.b()}" +
            "_CO${surrounding?.offset ?: -1}_CS${surrounding?.selectionStart ?: -1}" +
            "_CE${surrounding?.selectionEnd ?: -1}_CL${connectionText?.length ?: -1}" +
            "_CX${connectionContextMatches.b()}_CK${connectionCaretMatches.b()}" +
            "_B${beforeStart}_${beforeEnd}_${beforeLength}_${beforeContextMatches.b()}" +
            "_FR${refreshed.b()}_A${afterStart}_${afterEnd}_${afterLength}_${afterContextMatches.b()}" +
            if (probe.kind == PostCommitProbeKind.COMPOSITION)
                composingSpanMetrics(connectionText).code() else "")
    }

    private fun Boolean.b(): Int = if (this) 1 else 0

    private fun refuseFreshInsertion(code: String) {
        commitConfirmation = "REFUSED"
        experimentPhase = "REVIEW_READY"
        note("FRESH_INSERT_REFUSED_$code")
        status = when (code) {
            "SELECTION_NOT_COLLAPSED", "SELECTION_CHANGED" ->
                "Text is selected. Tap to place the cursor, then try Insert."
            "ACTIVE_COMPOSITION" ->
                "Keyboard still composing. Finish editing, place the cursor, then Insert or Copy."
            "RESULT_TOO_LONG_OR_UNSAFE" ->
                "This Phase 1 Insert test supports up to 512 characters. Use Copy & close."
            "RESULT_NOT_SINGLE_LINE" ->
                "This Phase 1 Insert test accepts one line. Use Copy & close for line breaks."
            else -> "Could not verify a safe destination. Tap the intended text field and try Insert, or Copy."
        }
        compactResultNotice = status
        refreshIdleBubble(forceRender = true)
    }

    private fun startOverlaySession(id: String, mode: OutputMode) {
        if (pendingId != id) return
        experimentPhase = "OVERLAY_HOST_REUSE_REQUESTED"
        val host = bubble?.takeIf { expanded && it.isAttachedToWindow && it.width > 0 && it.height > 0 }
        if (host == null) {
            invalidate(id, "OVERLAY_EXPANDED_HOST_UNAVAILABLE")
            return
        }
        lateinit var owner: OverlayRecordingPanel
        owner = OverlayRecordingPanel(this, host, mode,
            onFailure = { code -> if (pendingId == id) invalidate(id, code) },
            onReady = {
                if (pendingId == id) {
                    if (!owner.hostStable()) invalidate(id, "OVERLAY_HOST_CHANGED_AT_MIC_PROOF")
                    else {
                        experimentPhase = "OVERLAY_RECORDING_PROVED"
                        note("OVERLAY_HOST_WINDOW_SAME")
                        note("OVERLAY_MIC_READ_POSITIVE_UNSILENCED")
                    }
                }
            },
            onStopEntry = {
                if (pendingId == id && overlayStopEntryUptimeMs == null) {
                    overlayStopEntryUptimeMs = SystemClock.uptimeMillis()
                    note("OVERLAY_STOP_ENTRY_CALLBACK")
                }
            },
            onReview = {
                if (pendingId == id) {
                    if (overlayReviewChildrenCallbackUptimeMs == null) {
                        overlayReviewChildrenCallbackUptimeMs = SystemClock.uptimeMillis()
                        note("OVERLAY_REVIEW_CHILDREN_POPULATED_CALLBACK")
                    }
                    experimentPhase = "OVERLAY_READ_ONLY_REVIEW"
                    note("OVERLAY_MIC_PROVED_STOPPED")
                }
            },
            onCancel = { if (pendingId == id) cancelSession(id) }
        )
        overlayPanel = owner
        if (!owner.showAndStart()) {
            if (pendingId == id) invalidate(id, "OVERLAY_HOST_REUSE_FAILED")
            return
        }
        if (pendingId != id) return // A synchronous microphone refusal may already have ended it.
        experimentPhase = "OVERLAY_HOST_REUSED"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF && pendingId == id)
                    invalidate(id, "OVERLAY_SCREEN_OFF")
            }
        }
        try {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
            overlayScreenReceiver = receiver
        } catch (_: Exception) {
            invalidate(id, "OVERLAY_SCREEN_RECEIVER_FAILED")
            return
        }
        val heartbeat = object : Runnable {
            override fun run() {
                if (pendingId != id || overlayPanel !== owner) return
                verifyOverlayTarget(id)
                if (pendingId == id && overlayPanel === owner && !owner.hostStable())
                    invalidate(id, "OVERLAY_HOST_CHANGED")
                if (pendingId == id && overlayPanel === owner) owner.pollMic()
                if (pendingId == id && overlayPanel === owner) main.postDelayed(this, 250)
            }
        }
        overlayHeartbeat = heartbeat
        main.post(heartbeat)
    }

    /** Focus and identity only: never read target text, history, or an IME tree. */
    private fun verifyOverlayTarget(id: String) {
        if (pendingId != id || startupExperiment != StartupExperiment.OVERLAY) return
        if (overlayPanel?.hostStable() != true)
            return invalidate(id, "OVERLAY_HOST_CHANGED")
        val session = guard.session(id) ?: return invalidate(id, "OVERLAY_SESSION_MISSING")
        if (session.invalidated) return invalidate(id, "OVERLAY_ALREADY_INVALID")
        if ((getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked)
            return invalidate(id, "OVERLAY_LOCKED")
        val method = inputMethod as? TrackingInputMethod
            ?: return invalidate(id, "OVERLAY_INPUT_METHOD_MISSING")
        val editorInfo = method.currentInputEditorInfo
            ?: return invalidate(id, "OVERLAY_EDITOR_INFO_MISSING")
        if (!method.currentInputStarted || method.currentInputConnection == null ||
            method.generation != overlayInputGeneration ||
            editorInfo.packageName != session.target.packageName ||
            editorInfo.fieldId != session.target.editorFieldId)
            return invalidate(id, "OVERLAY_INPUT_BINDING_CHANGED")
        val current = windows
        val focusedApps = current.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val focused = focusedApps.singleOrNull()
            ?: return invalidate(id, "OVERLAY_FOCUS_AMBIGUOUS")
        if (focused.id != session.target.windowId)
            return invalidate(id, "OVERLAY_FOCUSED_WINDOW_CHANGED")
        val originalWindow = current.singleOrNull { it.id == session.target.windowId }
            ?: return invalidate(id, "OVERLAY_TARGET_WINDOW_MISSING")
        if (originalWindow.type != AccessibilityWindowInfo.TYPE_APPLICATION || !originalWindow.isFocused)
            return invalidate(id, "OVERLAY_TARGET_WINDOW_UNFOCUSED")
        val root = originalWindow.getRoot(0)
            ?: return invalidate(id, "OVERLAY_TARGET_ROOT_MISSING")
        if (root != targetRootNode || root.packageName?.toString() != session.target.packageName)
            return invalidate(id, "OVERLAY_TARGET_ROOT_CHANGED")
        val editor = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return invalidate(id, "OVERLAY_EDITOR_FOCUS_MISSING")
        if (editor != targetNode || !editor.isFocused || !editor.isEditable || !editor.isEnabled ||
            editor.isPassword ||
            editor.windowId != session.target.windowId ||
            editor.packageName?.toString() != session.target.packageName ||
            editor.viewIdResourceName != session.target.viewId ||
            editor.className?.toString() != session.target.className ||
            editor.inputType != session.target.inputType ||
            (editor.uniqueId?.takeIf(String::isNotBlank) ?: "source-node") !=
                session.target.uniqueNodeId)
            invalidate(id, "OVERLAY_EDITOR_CHANGED")
    }

    private fun stopOverlayOwnership() {
        overlayHeartbeat?.let(main::removeCallbacks)
        overlayHeartbeat = null
        overlayScreenReceiver?.let { runCatching { unregisterReceiver(it) } }
        overlayScreenReceiver = null
        val owner = overlayPanel
        owner?.close()
        overlayPanel = null
        if (owner?.hasPendingCleanup() == true) {
            note("OVERLAY_CLEANUP_UNCERTAIN")
            overlayCleanupPending = owner
            var retriesRemaining = 20
            main.postDelayed(object : Runnable {
                override fun run() {
                    if (overlayCleanupPending !== owner) return
                    owner.close()
                    if (!owner.hasPendingCleanup()) overlayCleanupPending = null
                    else if (--retriesRemaining > 0) main.postDelayed(this, 500)
                }
            }, 500)
        }
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
        val compositionState: CompositionState,
        val windowChildren: ChildWindowSnapshot?
    )

    private fun observeEditor(beforeCount: Int = 64, afterCount: Int = 64,
        postCommit: Boolean = false): Observation? {
        if (startupExperiment == StartupExperiment.RESELECT) destinationCaptureCount++
        observationCode = ObservationCode.NOT_CHECKED
        val locked = (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        if (locked) return refuse(ObservationCode.LOCKED)
        val method = inputMethod as? TrackingInputMethod
            ?: return refuse(ObservationCode.NO_INPUT_METHOD)
        if (!method.currentInputStarted) return refuse(ObservationCode.INPUT_NOT_STARTED)
        val info = method.currentInputEditorInfo ?: return refuse(ObservationCode.NO_EDITOR_INFO)
        if (startupExperiment == StartupExperiment.REVIEW && info.packageName == packageName)
            return refuse(ObservationCode.OWN_EDITOR)
        val connection = method.currentInputConnection
            ?: return refuse(ObservationCode.NO_INPUT_CONNECTION)
        if (!safeInputType(info.inputType)) return refuse(ObservationCode.UNSUPPORTED_EDITOR_TYPE)
        val focusedWindows = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedWindows.isEmpty()) return refuse(ObservationCode.NO_FOCUSED_APP_WINDOW)
        if (focusedWindows.size > 1) return refuse(ObservationCode.MULTIPLE_FOCUSED_APP_WINDOWS)
        if (startupExperiment == StartupExperiment.REVIEW &&
            focusedWindows.single().displayId != Display.DEFAULT_DISPLAY)
            return refuse(ObservationCode.WRONG_DISPLAY)
        val focusedWindow = focusedWindows.single()
        val root = focusedWindow.getRoot(0) ?: return refuse(ObservationCode.NO_FOCUSED_NODE)
        if (!root.refresh()) return refuse(ObservationCode.NO_FOCUSED_NODE)
        if (startupExperiment == StartupExperiment.REVIEW &&
            (root.windowId != focusedWindow.id ||
                root.packageName?.toString() != info.packageName))
            return refuse(ObservationCode.NODE_EDITOR_MISMATCH)
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return refuse(ObservationCode.NO_FOCUSED_NODE)
        if (!focus.isFocused || !focus.isEditable ||
            focus.packageName?.toString() != info.packageName)
            return refuse(ObservationCode.NODE_EDITOR_MISMATCH)
        if (startupExperiment == StartupExperiment.REVIEW &&
            (focus.windowId != focusedWindow.id || focus.inputType != info.inputType))
            return refuse(ObservationCode.NODE_EDITOR_MISMATCH)
        if (startupExperiment == StartupExperiment.REVIEW && !focus.isEnabled)
            return refuse(ObservationCode.NODE_DISABLED)
        if (focus.isPassword || !safeInputType(focus.inputType))
            return refuse(ObservationCode.PROTECTED_OR_NON_TEXT_NODE)
        val initialUnique = focus.uniqueId?.takeIf(String::isNotBlank) ?: "source-node"
        val surrounding = try {
            connection.getSurroundingText(beforeCount, afterCount,
                InputConnection.GET_TEXT_WITH_STYLES)
        } catch (_: Exception) { null } ?: return refuse(ObservationCode.SURROUNDING_UNAVAILABLE)
        if (postCommit && startupExperiment == StartupExperiment.REVIEW) {
            val transaction = freshDestination
                ?: return refuse(ObservationCode.POSTCOMMIT_TARGET_CHANGED)
            if (transaction.phase != DestinationPhase.OBSERVING ||
                root != transaction.root || focus != transaction.node ||
                method.generation != transaction.generation)
                return refuse(ObservationCode.POSTCOMMIT_TARGET_CHANGED)
            // findFocus can return a cached descendant even after root.refresh().
            // Refresh this same node only after the connection read; a failed
            // refresh is terminal uncertainty, not permission to use old metadata.
            if (!runCatching { focus.refresh() }.getOrDefault(false))
                return refuse(ObservationCode.POSTCOMMIT_REFRESH_FAILED)
            val currentInfo = method.currentInputEditorInfo
            val currentWindows = windows.filter {
                it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
            }
            val currentWindow = currentWindows.singleOrNull()
            val currentRoot = currentWindow?.getRoot(0)
            val rootStillCurrent = currentRoot != null && currentRoot == root &&
                runCatching { currentRoot.refresh() }.getOrDefault(false)
            val currentFocus = if (rootStillCurrent)
                runCatching { currentRoot?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
                else null
            val unlockedNow = !(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
            val interactiveNow = (getSystemService(POWER_SERVICE) as PowerManager).isInteractive
            if (freshDestination !== transaction || !unlockedNow || !interactiveNow ||
                !method.currentInputStarted || method.generation != transaction.generation ||
                currentInfo !== info || currentInfo?.packageName != info.packageName ||
                currentInfo?.fieldId != info.fieldId || currentInfo?.inputType != info.inputType ||
                method.currentInputConnection == null || currentWindow?.id != focusedWindow.id ||
                currentWindow?.displayId != Display.DEFAULT_DISPLAY || !rootStillCurrent ||
                currentRoot?.windowId != focusedWindow.id ||
                currentRoot?.packageName?.toString() != info.packageName ||
                currentFocus != focus || !focus.isFocused || !focus.isEditable ||
                !focus.isEnabled || focus.isPassword || !safeInputType(focus.inputType) ||
                focus.windowId != focusedWindow.id ||
                focus.packageName?.toString() != info.packageName ||
                focus.inputType != info.inputType)
                return refuse(ObservationCode.POSTCOMMIT_TARGET_CHANGED)
            note("POSTCOMMIT_FOCUS_REFRESHED")
        }
        val unique = if (postCommit && startupExperiment == StartupExperiment.REVIEW)
            focus.uniqueId?.takeIf(String::isNotBlank) ?: "source-node"
            else initialUnique
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
        val nodeCaretAgrees = EditorCaretAgreement.matches(
                focus.textSelectionStart, focus.textSelectionEnd, surrounding.offset,
                surrounding.selectionStart, surrounding.selectionEnd, text.isEmpty(), nodeEmptyOrHint
            )
        if (!nodeCaretAgrees) {
            val transaction = freshDestination
            if (postCommit && BuildConfig.DEBUG && startupExperiment == StartupExperiment.REVIEW &&
                info.packageName == "com.koscribe.smoke" &&
                transaction?.phase == DestinationPhase.OBSERVING &&
                guard.session(transaction.id)?.target?.packageName == "com.koscribe.smoke") {
                val expected = transaction.expected
                val elapsed = (SystemClock.uptimeMillis() - transaction.commitRequestedUptimeMs)
                    .coerceAtLeast(0L)
                val contextMatches = surrounding.offset == expected.oldOffset &&
                    text.toString() == expected.text
                val caretMatches = surrounding.offset == expected.oldOffset &&
                    surrounding.selectionStart == expected.caret &&
                    surrounding.selectionEnd == expected.caret
                val nodeText = focus.text
                val nodeContextMatches = !focus.isShowingHintText &&
                    nodeText?.toString() == expected.text
                note("POST_DIAG_ORIG_MS_${elapsed}_R${(root == transaction.root).b()}" +
                    "_N${(focus == transaction.node).b()}" +
                    "_G${(method.generation == transaction.generation).b()}" +
                    "_CO${surrounding.offset}_CS${surrounding.selectionStart}" +
                    "_CE${surrounding.selectionEnd}_CL${text.length}" +
                    "_CX${contextMatches.b()}_CK${caretMatches.b()}" +
                    "_B${focus.textSelectionStart}_${focus.textSelectionEnd}" +
                    "_${nodeText?.length ?: -1}_${nodeContextMatches.b()}")
            }
            note("CARET_EMPTY_PROBE_C${if (connectionEmptyAtZero) "Y" else "N"}" +
                "_N${if (nodeUnknownPair) "Y" else "N"}" +
                "_H${if (nodeEmptyOrHint) "Y" else "N"}")
            return refuse(ObservationCode.NODE_CARET_MISMATCH)
        }
        if (nodeUnknownPair) note("OBS_VERIFIED_EMPTY_CARET")
        val composition = if (postCommit) CompositionState.UNKNOWN else method.compositionAt(caret)
        if ((composition == CompositionState.COMPOSING || composing) &&
            !(postCommit && startupExperiment == StartupExperiment.REVIEW)) {
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
        val valid = if (postCommit && startupExperiment == StartupExperiment.REVIEW)
            snapshot.isStructurallyValid() else snapshot.isSafe()
        if (!valid) return refuse(ObservationCode.SNAPSHOT_INVALID)
        if (postCommit && composing && BuildConfig.DEBUG &&
            startupExperiment == StartupExperiment.REVIEW && info.packageName == "com.koscribe.smoke")
            note("POSTCOMMIT_COMPOSING_OBSERVED")
        observationCode = ObservationCode.READY
        note("OBS_READY_${composition.name}")
        return Observation(snapshot, focus, root, connection, method.generation, composition,
            if (startupExperiment == StartupExperiment.OVERLAY)
                childWindowSnapshot(focusedWindows.single()) else null)
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

    private data class IdleBubbleFrame(
        val bounds: Rect, val bars: Insets, val area: BubblePlacement.Area
    )

    private data class IdleBubbleGeometry(
        val frame: IdleBubbleFrame, val screenOffset: BubblePlacement.Point
    )

    private fun bubbleFrame(): IdleBubbleFrame {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val margin = dp(8)
        val left = bounds.left + bars.left + margin
        val top = bounds.top + bars.top + margin
        val right = bounds.right - bars.right - margin
        var bottom = bounds.bottom - bars.bottom - margin
        // Window bounds are metadata only. Never traverse the keyboard tree.
        val ime = windows?.filter {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                it.displayId == Display.DEFAULT_DISPLAY
        }?.singleOrNull()
        if (ime != null) {
            val imeRect = Rect()
            ime.getBoundsInScreen(imeRect)
            if (!imeRect.isEmpty && imeRect.left < right && imeRect.right > left)
                bottom = minOf(bottom, imeRect.top - margin)
        }
        return IdleBubbleFrame(Rect(bounds), bars,
            BubblePlacement.Area(left, top, right.coerceAtLeast(left), bottom.coerceAtLeast(top)))
    }

    private fun savedBubbleAnchor(): BubblePlacement.Anchor {
        val x = bubblePrefs.getFloat("anchor_x", 1f)
        val y = bubblePrefs.getFloat("anchor_y", 0.18f)
        return BubblePlacement.Anchor(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    }

    private fun bubbleScreenPoint(holder: View): BubblePlacement.Point {
        val location = IntArray(2)
        holder.getLocationOnScreen(location)
        return BubblePlacement.Point(location[0], location[1])
    }

    /** Called on the UI thread after an asynchronous host layout; dump reads the cached bits. */
    private fun cacheIdleHostState(holder: View) {
        if (bubble !== holder) return
        val previous = lastIdleRefresh ?: return
        lastIdleRefresh = previous.copy(hostExists = true,
            hostAttached = holder.isAttachedToWindow, hostShown = holder.isShown)
    }

    private fun moveBubbleToScreenPoint(
        holder: LinearLayout, params: WindowManager.LayoutParams,
        requested: BubblePlacement.Point
    ): BubblePlacement.Point? {
        if (!unboundArm() || !serviceConnected || freshDestination != null ||
            unboundRecording.current() != null || bubble !== holder || !holder.isAttachedToWindow)
            return null
        val geometry = idleBubbleGeometry ?: return null
        if (geometry.frame != bubbleFrame()) {
            renderBubble()
            return null
        }
        val chosen = BubblePlacement.clamp(requested, holder.width, holder.height, geometry.frame.area)
        // This offset was measured before the host's first position update.
        // Mutable params may already describe an in-flight relayout, so never
        // derive another offset from the current view location and those params.
        val desiredX = chosen.x - geometry.screenOffset.x
        val desiredY = chosen.y - geometry.screenOffset.y
        if (params.x == desiredX && params.y == desiredY) return chosen
        val previousX = params.x
        val previousY = params.y
        params.x = desiredX
        params.y = desiredY
        return try {
            windowManager.updateViewLayout(holder, params)
            chosen
        } catch (_: Exception) {
            params.x = previousX
            params.y = previousY
            null
        }
    }

    private fun positionNewBubble(holder: LinearLayout, params: WindowManager.LayoutParams,
        frame: IdleBubbleFrame) {
        if (!serviceConnected) return
        holder.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                if (holder.width <= 0 || holder.height <= 0) return
                holder.removeOnLayoutChangeListener(this)
                if (!serviceConnected || bubble !== holder || unboundRecording.current() != null) return
                if (frame != bubbleFrame()) {
                    idleRenderOutcome = IdleRenderOutcome.FRAME_CHANGED
                    cacheIdleHostState(holder)
                    holder.post {
                        if (serviceConnected && bubble === holder && unboundRecording.current() == null)
                            renderBubble()
                    }
                    return
                }
                val actual = bubbleScreenPoint(holder)
                idleBubbleGeometry = IdleBubbleGeometry(frame,
                    BubblePlacement.Point(actual.x - params.x, actual.y - params.y))
                val desired = BubblePlacement.resolve(savedBubbleAnchor(), holder.width,
                    holder.height, frame.area)
                val placed = moveBubbleToScreenPoint(holder, params, desired) != null
                if (placed)
                    holder.visibility = View.VISIBLE
                // Placement may have replaced this host after a frame change.
                if (bubble === holder) {
                    idleRenderOutcome = if (placed) IdleRenderOutcome.VISIBLE
                        else IdleRenderOutcome.PLACEMENT_FAILED
                    cacheIdleHostState(holder)
                }
            }
        })
    }

    private fun enableCollapsedDrag(pill: Button, holder: LinearLayout,
        params: WindowManager.LayoutParams) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var start: BubblePlacement.Point? = null
        var dragging = false
        pill.setOnTouchListener { view, event ->
            if (!serviceConnected || unboundRecording.current() != null ||
                bubble !== holder || expanded || !holder.isShown || !holder.isEnabled ||
                idleBubbleGeometry == null)
                return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    start = bubbleScreenPoint(holder)
                    dragging = false
                    idleDragging = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val origin = start ?: return@setOnTouchListener false
                    if (!dragging &&
                        (kotlin.math.abs(event.rawX - downX) > slop ||
                            kotlin.math.abs(event.rawY - downY) > slop)) dragging = true
                    if (dragging) moveBubbleToScreenPoint(holder, params,
                        BubblePlacement.Point(origin.x + (event.rawX - downX).toInt(),
                            origin.y + (event.rawY - downY).toInt()))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val origin = start
                        val final = if (origin == null) null else moveBubbleToScreenPoint(holder, params,
                            BubblePlacement.Point(origin.x + (event.rawX - downX).toInt(),
                                origin.y + (event.rawY - downY).toInt()))
                        val geometry = idleBubbleGeometry
                        if (final != null && geometry != null) {
                            // Remember the accepted user request immediately. A later
                            // hide/expand must not cancel persistence of this completed drag.
                            val anchor = BubblePlacement.anchor(final, holder.width, holder.height,
                                geometry.frame.area)
                            bubblePrefs.edit().putFloat("anchor_x", anchor.x)
                                .putFloat("anchor_y", anchor.y).apply()
                        }
                        idleDragging = false
                        scheduleIdleBubbleRefresh()
                    } else {
                        idleDragging = false
                        view.performClick()
                    }
                    start = null
                    dragging = false
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    start?.let { moveBubbleToScreenPoint(holder, params, it) }
                    start = null
                    dragging = false
                    idleDragging = false
                    scheduleIdleBubbleRefresh()
                    true
                }
                else -> true
            }
        }
    }

    private fun enableResultDrag(handle: Button, holder: LinearLayout,
        params: WindowManager.LayoutParams) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var start: BubblePlacement.Point? = null
        var dragging = false
        fun allowed(): Boolean {
            val phase = unboundDraft.current()?.phase
            return serviceConnected && startupExperiment == StartupExperiment.REVIEW &&
                freshDestination == null && unboundRecording.current() == null &&
                (phase == UnboundDraftState.Phase.READY ||
                    phase == UnboundDraftState.Phase.UNCERTAIN) && bubble === holder &&
                holder.isAttachedToWindow && holder.isShown && holder.isEnabled &&
                idleBubbleGeometry != null
        }
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (!allowed()) return@setOnTouchListener false
                    downX = event.rawX
                    downY = event.rawY
                    start = bubbleScreenPoint(holder)
                    dragging = false
                    idleDragging = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!allowed()) {
                        start = null
                        dragging = false
                        idleDragging = false
                        return@setOnTouchListener true
                    }
                    val origin = start ?: return@setOnTouchListener true
                    if (!dragging &&
                        (kotlin.math.abs(event.rawX - downX) > slop ||
                            kotlin.math.abs(event.rawY - downY) > slop)) dragging = true
                    if (dragging) moveBubbleToScreenPoint(holder, params,
                        BubblePlacement.Point(origin.x + (event.rawX - downX).toInt(),
                            origin.y + (event.rawY - downY).toInt()))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (allowed() && dragging) {
                        val origin = start
                        val final = if (origin == null) null else moveBubbleToScreenPoint(holder, params,
                            BubblePlacement.Point(origin.x + (event.rawX - downX).toInt(),
                                origin.y + (event.rawY - downY).toInt()))
                        val geometry = idleBubbleGeometry
                        if (final != null && geometry != null) {
                            val anchor = BubblePlacement.anchor(final, holder.width, holder.height,
                                geometry.frame.area)
                            bubblePrefs.edit().putFloat("anchor_x", anchor.x)
                                .putFloat("anchor_y", anchor.y).apply()
                        }
                    }
                    start = null
                    dragging = false
                    idleDragging = false
                    scheduleIdleBubbleRefresh()
                    true // The dedicated handle never clicks Insert, Copy, or Cancel.
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (allowed()) start?.let { moveBubbleToScreenPoint(holder, params, it) }
                    start = null
                    dragging = false
                    idleDragging = false
                    scheduleIdleBubbleRefresh()
                    true
                }
                else -> true
            }
        }
    }

    private fun renderBubble() {
        if (!serviceConnected || !::windowManager.isInitialized) return
        if (unboundArm()) idleRenderCount++
        if (unboundArm() && unboundRecording.current() != null) {
            idleRenderOutcome = IdleRenderOutcome.BLOCKED_RECORDING
            return // Keep the existing window unchanged until this recording ends.
        }
        if (freshDestination != null) {
            if (unboundArm()) idleRenderOutcome = IdleRenderOutcome.BLOCKED_DESTINATION
            return // Preserve the ready host through capture and proof.
        }
        if (startupExperiment == StartupExperiment.OVERLAY && pendingId != null &&
            overlayPanel != null) return // Keep the captured host window unchanged until terminal cleanup.
        bubble?.let {
            try { windowManager.removeView(it) }
            catch (_: Exception) {
                // Do not add a second window while the old one may still exist.
                it.visibility = View.GONE
                it.isEnabled = false
                if (unboundArm()) idleRenderOutcome = IdleRenderOutcome.REMOVE_FAILED
                return
            }
        }
        idleDragging = false
        idleBubbleGeometry = null
        bubble = null
        bubbleParams = null
        if (unboundArm() && (!idleBubbleVisible ||
                unboundDraft.current()?.phase == UnboundDraftState.Phase.REVIEW ||
                unboundDraft.current()?.phase == UnboundDraftState.Phase.TRANSLATING)) {
            idleRenderOutcome = IdleRenderOutcome.HIDDEN_BY_POLICY_OR_REVIEW
            return
        }
        val frame = if (unboundArm()) bubbleFrame() else null
        val available = frame?.area
        if (available != null && (available.right - available.left < dp(56) ||
                available.bottom - available.top < dp(48))) {
            idleRenderOutcome = IdleRenderOutcome.AREA_TOO_SMALL
            return
        }
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (startupExperiment == StartupExperiment.REVIEW) {
                isSaveEnabled = false
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
            }
            if (expanded) {
                setBackgroundColor(0xFFF7F7F7.toInt())
                setPadding(dp(8), dp(8), dp(8), dp(8))
                elevation = dp(4).toFloat()
            }
        }
        val resultDraft = if (expanded && startupExperiment == StartupExperiment.REVIEW)
            unboundDraft.current()?.takeIf {
                it.phase == UnboundDraftState.Phase.READY ||
                    it.phase == UnboundDraftState.Phase.UNCERTAIN
            }
            else null
        var resultDragHandle: Button? = null
        if (resultDraft != null) {
            val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val dragHandle = Button(this).apply {
                text = if (VisualTestMode.enabled)
                    "VISUAL TEST · ${resultDraft.mode.label} · Move"
                    else "${resultDraft.mode.label} · Move"
                contentDescription = if (VisualTestMode.enabled)
                    "VISUAL TEST. Drag KoScribe result, ${resultDraft.mode.label} mode"
                    else "Drag KoScribe result, ${resultDraft.mode.label} mode"
                isAllCaps = false
                textSize = if (VisualTestMode.enabled) 12f else 14f
                maxLines = if (VisualTestMode.enabled) 2 else 1
                minWidth = 0
                setPadding(dp(4), 0, dp(4), 0)
            }
            resultDragHandle = dragHandle
            topRow.addView(dragHandle, LinearLayout.LayoutParams(0, dp(48), 1f))
            topRow.addView(action("Cancel") { endDraft(resultDraft.id, "USER_CANCELLED") }
                .apply {
                    id = R.id.ready_cancel
                    isAllCaps = false
                    textSize = 12f
                    minWidth = 0
                    setPadding(0, 0, 0, 0)
                }, LinearLayout.LayoutParams(dp(80), dp(48)))
            holder.addView(topRow, LinearLayout.LayoutParams(-1, dp(48)))
            compactResultNotice?.takeIf(String::isNotBlank)?.let { notice ->
                holder.addView(TextView(this).apply {
                    text = notice
                    textSize = 12f
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(Color.rgb(30, 30, 30))
                    setPadding(dp(4), 0, dp(4), 0)
                }, LinearLayout.LayoutParams(-1, dp(44)))
            }
            val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (resultDraft.phase == UnboundDraftState.Phase.READY)
                actionRow.addView(action("Insert") { insertDraft(resultDraft.id) }.apply {
                    id = R.id.ready_insert
                    isAllCaps = false
                    textSize = 12f
                    minWidth = 0
                    setPadding(0, 0, 0, 0)
                }, LinearLayout.LayoutParams(0, dp(48), 1f))
            actionRow.addView(action("Copy & close") { copyDraft(resultDraft.id) }.apply {
                id = R.id.ready_copy
                isAllCaps = false
                textSize = 12f
                minWidth = 0
                setPadding(0, 0, 0, 0)
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            holder.addView(actionRow, LinearLayout.LayoutParams(-1, dp(48)))
        } else if (expanded) {
            val controls = if (unboundArm())
                LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                else holder
            controls.addView(caption(if (BuildConfig.SPEECH_INPUT) "KoScribe · speech input" else "KoScribe · sample only", true))
            controls.addView(caption(status))
            if (BuildConfig.SPEECH_INPUT && !BuildConfig.TRANSLATION_INPUT)
                controls.addView(caption("EN speech now. Korean translation arrives in the next phase."))
            val modeRow = if (unboundArm())
                LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                else null
            for (mode in if (BuildConfig.SPEECH_INPUT && !BuildConfig.TRANSLATION_INPUT)
                    listOf(OutputMode.ENGLISH) else OutputMode.entries) {
                val button = action(mode.label) { startMode(mode) }
                if (modeRow != null)
                    modeRow.addView(button, LinearLayout.LayoutParams(0, -2, 1f))
                else controls.addView(button)
            }
            if (modeRow != null) controls.addView(modeRow, LinearLayout.LayoutParams(-1, -2))
            if (startupExperiment == StartupExperiment.OFF && recoveryText != null)
                controls.addView(action("Copy result manually") { copyRecovery() })
            val footerRow = if (unboundArm())
                LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            else null
            if (footerRow != null)
                footerRow.addView(action("Hide") {
                    speechKeyCheckGeneration++
                    speechKeyCheckPending = false
                    clearPendingAuto("KEY_PRECHECK_HIDDEN")
                    idleBubblePolicy.hideUntilNextImeCycle()
                    idleBubbleVisible = false
                    expanded = false
                    renderBubble()
                }, LinearLayout.LayoutParams(0, -2, 1f))
            val close = action("Close") {
                speechKeyCheckGeneration++
                speechKeyCheckPending = false
                clearPendingAuto("KEY_PRECHECK_CLOSED")
                expanded = false
                renderBubble()
            }
            if (footerRow != null) {
                footerRow.addView(close, LinearLayout.LayoutParams(0, -2, 1f))
                controls.addView(footerRow, LinearLayout.LayoutParams(-1, -2))
            } else controls.addView(close)
            if (controls !== holder) {
                val viewport = android.widget.ScrollView(this).apply { addView(controls) }
                holder.addView(viewport, LinearLayout.LayoutParams(-1, -1))
            }
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
            if (resultDraft != null)
                minOf(dp(280), available!!.right - available.left)
            else if (unboundArm() && expanded)
                minOf(dp(260), available!!.right - available.left)
            else if (expanded) dp(260) else WindowManager.LayoutParams.WRAP_CONTENT,
            if (resultDraft != null)
                minOf(dp(if (compactResultNotice.isNullOrBlank()) 112 else 156),
                    available!!.bottom - available.top)
            else if (unboundArm() && expanded)
                minOf(dp(320), available!!.bottom - available.top)
            else WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (startupExperiment == StartupExperiment.REVIEW && !VisualTestMode.enabled)
                flags = flags or WindowManager.LayoutParams.FLAG_SECURE
            gravity = if (unboundArm())
                Gravity.TOP or Gravity.LEFT else Gravity.TOP or Gravity.END
            x = if (unboundArm()) 0 else dp(8)
            y = if (unboundArm()) 0 else dp(110)
        }
        try {
            if (unboundArm()) idleRenderOutcome = IdleRenderOutcome.AWAIT_LAYOUT
            if (unboundArm()) holder.visibility = View.INVISIBLE
            windowManager.addView(holder, params)
            bubble = holder
            if (unboundArm()) {
                bubbleParams = params
                if (!expanded) enableCollapsedDrag(holder.getChildAt(0) as Button, holder, params)
                resultDragHandle?.let { enableResultDrag(it, holder, params) }
                positionNewBubble(holder, params, frame!!)
            }
        } catch (_: Exception) {
            bubble = null
            bubbleParams = null
            if (unboundArm()) idleRenderOutcome = IdleRenderOutcome.ADD_OR_SETUP_FAILED
        }
    }
}
