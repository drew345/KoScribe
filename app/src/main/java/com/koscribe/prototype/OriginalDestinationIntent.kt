package com.koscribe.prototype

/**
 * Process-only evidence of the destination intended when a Korean mode was selected.
 * This is not an insertion transaction and never owns an InputConnection or Android node.
 * The service authenticates window, root, node, and review-owner events before calling it.
 */
internal class OriginalDestinationIntent private constructor(
    private val original: EditorSnapshot,
    private val originalGeneration: Int,
    private val capturedAtMs: Long,
) {
    enum class Phase { ORIGINAL, ENTERING_REVIEW, REVIEW, RETURNING, CONSUMED, FALLBACK }
    enum class ReturnVerdict { READY, WAIT, FALLBACK }
    enum class AdditionEvidence { MISSING, MATCH, CONTRADICTION }
    private enum class WindowPair { NONE, REMOVED, ADD_PENDING, COMPLETE }

    companion object {
        private const val LIFETIME_MS = 180_000L
        private const val RETURN_WINDOW_MS = 2_000L
        private const val OWNED_EDITOR_PACKAGE = "com.koscribe.smoke"

        /** Call synchronously at the mode click, before any asynchronous key check. */
        fun capture(
            original: EditorSnapshot,
            originalGeneration: Int,
            capturedAtMs: Long,
            mode: OutputMode,
        ): OriginalDestinationIntent? =
            if (mode != OutputMode.ENGLISH && original.packageName == OWNED_EDITOR_PACKAGE &&
                original.isSafe() && originalGeneration in 0..(Int.MAX_VALUE - 2) &&
                capturedAtMs >= 0L
            ) OriginalDestinationIntent(original, originalGeneration, capturedAtMs) else null
    }

    var phase: Phase = Phase.ORIGINAL
        private set

    var lastInvalidationReason: String? = null
        private set

    val reviewFocusAttested: Boolean get() = reviewFocusSeen

    private var draftId: String? = null
    private var reviewInputSeen = false
    private var reviewFocusSeen = false
    private var originalInputFinished = false
    private var reviewInputFinished = false
    private var originalReturnInputSeen = false
    private var returnStartedAtMs: Long? = null
    private var returnVerified = false
    private var precommitVerified = false
    private var windowPair = WindowPair.NONE

    private fun validTime(nowMs: Long): Boolean =
        nowMs >= capturedAtMs && nowMs - capturedAtMs <= LIFETIME_MS &&
            (returnStartedAtMs?.let { nowMs >= it && nowMs - it <= RETURN_WINDOW_MS } ?: true)

    private fun activeAt(nowMs: Long): Boolean {
        if (phase == Phase.FALLBACK || phase == Phase.CONSUMED) return false
        if (!validTime(nowMs)) invalidate("EXPIRED")
        return phase != Phase.FALLBACK
    }

    private fun fail(reason: String): Boolean {
        invalidate(reason)
        return false
    }

    /** The draft exists before the exact owned review Activity is launched. */
    fun beginReview(draftId: String, nowMs: Long): Boolean {
        if (!activeAt(nowMs)) return false
        if (phase != Phase.ORIGINAL || draftId.isBlank()) return fail("REVIEW_BEGIN_MISMATCH")
        this.draftId = draftId
        phase = Phase.ENTERING_REVIEW
        return true
    }

    /** One new, non-restarting own editor input session is required. */
    fun reviewInputStarted(generation: Int, restarting: Boolean, requireOriginalFinish: Boolean = false): Boolean {
        if (phase != Phase.ENTERING_REVIEW || reviewInputSeen || restarting ||
            generation != originalGeneration + 1 || (requireOriginalFinish && !originalInputFinished))
            return fail("REVIEW_INPUT_MISMATCH")
        reviewInputSeen = true
        if (reviewFocusSeen) phase = Phase.REVIEW
        return true
    }

    /** Service must prove exact ReviewActivity owner/window/root/editor focus first. */
    fun reviewFocused(draftId: String, nowMs: Long): Boolean {
        if (!activeAt(nowMs)) return false
        if (phase != Phase.ENTERING_REVIEW || reviewFocusSeen || draftId != this.draftId)
            return fail("REVIEW_FOCUS_MISMATCH")
        reviewFocusSeen = true
        if (reviewInputSeen) phase = Phase.REVIEW
        return true
    }

    /** Extra, reordered, or unauthenticated input endings permanently disarm auto. */
    fun inputFinished(generation: Int, authenticatedOwnReview: Boolean): Boolean = when {
        phase == Phase.ENTERING_REVIEW && generation == originalGeneration &&
            !originalInputFinished -> {
            originalInputFinished = true
            true
        }
        phase == Phase.REVIEW && generation == originalGeneration + 1 &&
            authenticatedOwnReview && !reviewInputFinished -> {
            reviewInputFinished = true
            true
        }
        phase == Phase.RETURNING && generation == originalGeneration + 1 &&
            !originalReturnInputSeen && !reviewInputFinished -> {
            reviewInputFinished = true
            true
        }
        else -> fail("INPUT_FINISH_MISMATCH")
    }

    /** Service verifies READY content and still-focused review owner, then calls before finish(). */
    fun beginReturn(draftId: String, nowMs: Long, ownerStillFocused: Boolean): Boolean {
        if (!activeAt(nowMs)) return false
        if (phase != Phase.REVIEW || draftId != this.draftId || !ownerStillFocused)
            return fail("RETURN_BEGIN_MISMATCH")
        returnStartedAtMs = nowMs
        phase = Phase.RETURNING
        return true
    }

    /** A single original-window removal is allowed only under fully attested own review. */
    fun originalRemoved(nowMs: Long, exactEvent: Boolean, originalAbsent: Boolean,
        ownReviewAttested: Boolean): Boolean {
        if (!activeAt(nowMs)) return false
        if (phase !in setOf(Phase.ENTERING_REVIEW, Phase.REVIEW) ||
            windowPair != WindowPair.NONE || !exactEvent || !originalAbsent ||
            !ownReviewAttested) return fail("ORIGINAL_REMOVAL_UNVERIFIED")
        windowPair = WindowPair.REMOVED
        return true
    }

    /** Missing metadata may settle only during the original, nonextending return window. */
    fun originalAdded(nowMs: Long, exactEvent: Boolean,
        evidence: AdditionEvidence): ReturnVerdict {
        if (!activeAt(nowMs)) return ReturnVerdict.FALLBACK
        if (phase != Phase.RETURNING || windowPair != WindowPair.REMOVED || !exactEvent ||
            evidence == AdditionEvidence.CONTRADICTION) {
            invalidate("ORIGINAL_ADDITION_UNVERIFIED")
            return ReturnVerdict.FALLBACK
        }
        windowPair = if (evidence == AdditionEvidence.MATCH) WindowPair.COMPLETE
            else WindowPair.ADD_PENDING
        return if (windowPair == WindowPair.COMPLETE) ReturnVerdict.READY else ReturnVerdict.WAIT
    }

    fun resolvePendingAddition(nowMs: Long, evidence: AdditionEvidence): ReturnVerdict {
        if (!activeAt(nowMs)) return ReturnVerdict.FALLBACK
        if (phase != Phase.RETURNING || windowPair != WindowPair.ADD_PENDING ||
            evidence == AdditionEvidence.CONTRADICTION) {
            invalidate("PENDING_ADDITION_UNVERIFIED")
            return ReturnVerdict.FALLBACK
        }
        if (evidence == AdditionEvidence.MATCH) windowPair = WindowPair.COMPLETE
        return if (windowPair == WindowPair.COMPLETE) ReturnVerdict.READY else ReturnVerdict.WAIT
    }

    val additionPending: Boolean get() = windowPair == WindowPair.ADD_PENDING
    val returnWindowProved: Boolean get() =
        windowPair == WindowPair.NONE || windowPair == WindowPair.COMPLETE

    /** The original app must receive exactly the next fresh input generation. */
    fun originalInputReturned(generation: Int, restarting: Boolean): Boolean {
        if (phase != Phase.RETURNING || originalReturnInputSeen || !reviewInputFinished ||
            restarting || generation != originalGeneration + 2)
            return fail("RETURN_INPUT_MISMATCH")
        originalReturnInputSeen = true
        return true
    }

    private fun exactOriginal(
        observed: EditorSnapshot?, generation: Int, sameNodeAndRoot: Boolean,
    ): Boolean = observed != null && observed.isSafe() && observed == original &&
        generation == originalGeneration + 2 && sameNodeAndRoot

    /** WAIT is only for the expected return callbacks or a temporarily unavailable host. */
    fun checkReturn(
        observedSnapshot: EditorSnapshot?, generation: Int, sameNodeAndRoot: Boolean,
        nowMs: Long,
    ): ReturnVerdict {
        if (!activeAt(nowMs)) return ReturnVerdict.FALLBACK
        if (phase != Phase.RETURNING) {
            invalidate("RETURN_PHASE_MISMATCH")
            return ReturnVerdict.FALLBACK
        }
        if (observedSnapshot != null &&
            !exactOriginal(observedSnapshot, generation, sameNodeAndRoot)) {
            invalidate("RETURN_TARGET_MISMATCH")
            return ReturnVerdict.FALLBACK
        }
        if (!returnWindowProved || !reviewInputFinished || !originalReturnInputSeen)
            return ReturnVerdict.WAIT
        if (observedSnapshot == null) {
            invalidate("RETURN_OBSERVATION_MISSING")
            return ReturnVerdict.FALLBACK
        }
        returnVerified = true
        return ReturnVerdict.READY
    }

    /** The fresh capture is still a separate guarded transaction in the service. */
    fun checkPrecommit(
        observedSnapshot: EditorSnapshot?, generation: Int, sameNodeAndRoot: Boolean,
        nowMs: Long,
    ): Boolean {
        if (!activeAt(nowMs)) return false
        if (phase != Phase.RETURNING || !returnWindowProved || !returnVerified || !originalReturnInputSeen ||
            !exactOriginal(observedSnapshot, generation, sameNodeAndRoot))
            return fail("PRECOMMIT_MISMATCH")
        precommitVerified = true
        return true
    }

    /** Consume before IPC. A later matching observation cannot revive this permit. */
    fun consume(
        observedSnapshot: EditorSnapshot?, generation: Int, sameNodeAndRoot: Boolean,
        nowMs: Long,
    ): Boolean {
        if (!activeAt(nowMs)) return false
        if (phase != Phase.RETURNING || !returnWindowProved || !precommitVerified ||
            !exactOriginal(observedSnapshot, generation, sameNodeAndRoot))
            return fail("CONSUME_MISMATCH")
        phase = Phase.CONSUMED
        return true
    }

    /** Target changes, unknown window events, lock, or ownership loss are terminal. */
    fun invalidate(reason: String) {
        if (phase == Phase.CONSUMED || phase == Phase.FALLBACK) return
        lastInvalidationReason = reason
        phase = Phase.FALLBACK
    }
}
