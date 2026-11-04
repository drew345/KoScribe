package com.koscribe.prototype

/**
 * A finite accessibility inventory proof for the owned review's entry only.
 * Opaque identity/shape values are immutable copies supplied by the Android adapter.
 * Notifications acknowledge a frozen snapshot delta; they never supply a layer value.
 */
internal class EntryWindowInventory private constructor(
    private val baseline: List<Window>,
    private val originalId: Int,
    private val hostId: Int,
    private val imeId: Int,
    private val screen: Any,
) {
    data class Window(
        val id: Int, val type: Int, val display: Int, val layer: Int,
        val root: Any, val rootPackage: String, val rootClass: String,
        val bounds: Any, val region: Any, val parent: Int?, val children: List<Int>,
        val pictureInPicture: Boolean, val focused: Boolean, val active: Boolean,
        val accessibilityFocused: Boolean,
    ) {
        fun stable(): Window = copy(layer = 0)
        fun stableWithoutShape(): Window = copy(layer = 0, bounds = Unit, region = Unit)
    }

    enum class State { BASELINE, DOWN_PENDING, DOWN_COMPLETE, UP_PENDING, COMPLETE, FAILED }
    enum class Kind { ADDED, REMOVED, LAYER, HOST_BOUNDS, REVIEW_CONTROL, ORIGINAL_FOCUS, OTHER }
    data class Event(val id: Int, val kind: Kind, val uptimeMs: Long,
        val focusBits: Int = 0, val controlBits: Int = 0)
    data class Frame(
        val windows: List<Window>?, val missingCode: String?,
        val reviewId: Int?, val reviewRoot: Any?,
        val originalRemovalAccepted: Boolean, val reviewProved: Boolean, val hostProved: Boolean,
        val entryActive: Boolean, val screen: Any,
        val elapsedMs: Long, val uptimeMs: Long,
        val preAnchorProved: Boolean = false, val localReviewIdentity: Any? = null,
    )
    data class PreparationFrame(
        val windows: List<Window>?, val screen: Any, val uptimeMs: Long,
        val hostIdentityProved: Boolean, val localAtOld: Boolean,
        val localAtTarget: Boolean, val remoteAtTarget: Boolean,
    )
    data class Decision(val accepted: Boolean, val layerAcknowledged: Boolean, val reason: String)
    data class Capture(val proof: EntryWindowInventory?, val reason: String)
    private data class Expected(val id: Int, val kind: Kind)

    companion object {
        const val MAX_WINDOWS = 16
        private const val ENTRY_MS = 1_000L
        private const val PREPARATION_MS = 500L
        private const val APPLICATION = 1
        private const val INPUT_METHOD = 2
        private const val ACCESSIBILITY_OVERLAY = 4

        fun capture(windows: List<Window>?, originalId: Int, hostId: Int,
            screen: Any, missingCode: String? = null): Capture {
            val invalid = inventoryError(windows)
            if (invalid != null) return Capture(null, missingCode ?: "BASELINE_$invalid")
            val complete = windows!!
            val original = complete.singleOrNull { it.id == originalId }
                ?: return Capture(null, "BASELINE_ORIGINAL")
            val host = complete.singleOrNull { it.id == hostId }
                ?: return Capture(null, "BASELINE_HOST")
            val ime = complete.singleOrNull { it.type == INPUT_METHOD }
                ?: return Capture(null, "BASELINE_IME_COUNT")
            if (setOf(originalId, hostId, ime.id).size != 3 ||
                original.type != APPLICATION || host.type != ACCESSIBILITY_OVERLAY ||
                !original.focused || complete.count { it.focused } != 1 ||
                host.focused || ime.focused)
                return Capture(null, "BASELINE_ROLES")
            return Capture(EntryWindowInventory(complete.toList(), originalId, hostId,
                ime.id, screen), "OK")
        }

        private fun inventoryError(windows: List<Window>?): String? {
            if (windows == null) return "MISSING_METADATA"
            if (windows.isEmpty() || windows.size > MAX_WINDOWS) return "WINDOW_COUNT"
            val ids = windows.map { it.id }
            if (ids.any { it < 0 } || ids.distinct().size != ids.size) return "WINDOW_IDS"
            if (windows.any { it.display != 0 || it.type <= 0 }) return "DISPLAY_OR_TYPE"
            if (windows.map { it.layer }.sorted() != windows.indices.toList()) return "ORDINAL_LAYERS"
            if (windows.any { it.rootPackage.isBlank() || it.rootClass.isBlank() })
                return "ROOT_METADATA"
            val known = ids.toSet()
            if (windows.any { window ->
                    window.parent == window.id || window.parent?.let { it !in known } == true ||
                        window.children.size > MAX_WINDOWS ||
                        window.children.distinct().size != window.children.size ||
                        window.children.any { it == window.id || it !in known }
                }) return "TOPOLOGY"
            val indexed = windows.associateBy { it.id }
            if (windows.any { window ->
                    window.parent?.let { window.id !in indexed.getValue(it).children } == true ||
                        window.children.any { indexed.getValue(it).parent != window.id }
                }) return "TOPOLOGY_RECIPROCAL"
            for (window in windows) {
                val visited = mutableSetOf<Int>()
                var cursor: Int? = window.id
                while (cursor != null) {
                    if (!visited.add(cursor)) return "TOPOLOGY_CYCLE"
                    cursor = indexed.getValue(cursor).parent
                }
            }
            return null
        }
    }

    var state = State.BASELINE
        private set
    var reason = "NONE"
        private set
    val complete: Boolean get() = state == State.COMPLETE
    private var launchedElapsed: Long? = null
    private var launchedUptime: Long? = null
    private var resizeStartedUptime: Long? = null
    private var preparationOldHost: Window? = null
    private var preparationTargetHost: Window? = null
    private var preparationLocalTargetSeen = false
    private var preparationLastUptime: Long? = null
    private var oldBoundsNoticeUptime: Long? = null
    private var targetBoundsNoticeUptime: Long? = null
    var preparationReady = false
        private set
    private var originalFocusBits = 0
    private var focusLossIdentity: Any? = null
    private var focusLossReviewId: Int? = null
    val originalFocusLossObserved: Boolean get() = focusLossIdentity != null
    val awaitingRemovalAfterFocusLoss: Boolean get() = originalFocusLossObserved && !originalAnchored
    private var reviewControlBits = 0
    private var reviewBoundsDown = false
    private var reviewBoundsUp = false
    private var originalAnchored = false
    private var provisionalImeRemoval: Event? = null
    private var observedImeAbsenceBeforeAnchor = false
    private var reviewIdentity: Window? = null
    private var downWindows: List<Window>? = null
    private var hadImeGap = false
    private var expected = emptyList<Expected>()
    private var next = 0
    private var lastEventUptime: Long? = null
    private var downLastUptime: Long? = null
    private var lastFrameElapsed: Long? = null
    private var lastFrameUptime: Long? = null
    private var lastObservedEventUptime: Long? = null

    fun start(elapsedMs: Long, uptimeMs: Long, hostResizeStartedUptimeMs: Long): Boolean {
        if (state == State.FAILED) return false
        if (launchedElapsed != null || elapsedMs < 0 || uptimeMs < 0 ||
            hostResizeStartedUptimeMs !in 0..uptimeMs ||
            resizeStartedUptime?.let { it != hostResizeStartedUptimeMs } == true)
            return fail("LAUNCH_CLOCK").accepted
        if (!preparationReady || preparationTargetHost == null ||
            uptimeMs < (preparationLastUptime ?: Long.MAX_VALUE) ||
            uptimeMs - hostResizeStartedUptimeMs >= PREPARATION_MS)
            return fail("PREPARATION_NOT_READY_AT_LAUNCH").accepted
        launchedElapsed = elapsedMs
        launchedUptime = uptimeMs
        resizeStartedUptime = hostResizeStartedUptimeMs
        return true
    }

    /** Freeze the settled recording host before the service mutates its children or layout. */
    fun beginPreparation(current: List<Window>?, currentScreen: Any, startedUptimeMs: Long,
        nowUptimeMs: Long, hostBeforeProved: Boolean): Decision {
        if (state == State.FAILED) return Decision(false, false, reason)
        if (launchedElapsed != null) return fail("PREPARATION_ALREADY_LAUNCHED")
        if (preparationOldHost != null) return fail("PREPARATION_RESTART")
        if (startedUptimeMs < 0) return fail("PREPARATION_LEASE_CLOCK")
        if (nowUptimeMs < startedUptimeMs) return fail("PREPARATION_CLOCK_REGRESSED")
        if (nowUptimeMs - startedUptimeMs >= PREPARATION_MS) return fail("PREPARATION_DEADLINE")
        if (!hostBeforeProved) return fail("PREPARATION_OLD_HOST_UNPROVED")
        preparationInventoryError(current, currentScreen)?.let { return fail(it) }
        preparationOldHost = current!!.single { it.id == hostId }
        resizeStartedUptime = startedUptimeMs
        preparationLastUptime = nowUptimeMs
        return accepted()
    }

    /** Endpoint observations grant no launch readiness until local and remote target agree. */
    fun observePreparation(frame: PreparationFrame, eventUptimeMs: Long? = null): Decision {
        if (state == State.FAILED) return Decision(false, false, reason)
        if (launchedElapsed != null) return fail("PREPARATION_ALREADY_LAUNCHED")
        val started = resizeStartedUptime ?: return fail("PREPARATION_NOT_STARTED")
        val old = preparationOldHost ?: return fail("PREPARATION_NOT_STARTED")
        if (frame.uptimeMs < (preparationLastUptime ?: started))
            return fail("PREPARATION_CLOCK_REGRESSED")
        if (frame.uptimeMs - started >= PREPARATION_MS) return fail("PREPARATION_DEADLINE")
        if (eventUptimeMs != null) {
            if (eventUptimeMs < started) return fail("PREPARATION_EVENT_BEFORE_LEASE")
            if (eventUptimeMs > frame.uptimeMs) return fail("PREPARATION_EVENT_FUTURE")
        }
        preparationInventoryError(frame.windows, frame.screen)?.let { return fail(it) }
        if (!frame.hostIdentityProved) return fail("PREPARATION_HOST_IDENTITY")
        if (!frame.localAtOld && !frame.localAtTarget) return fail("PREPARATION_LOCAL_THIRD_SHAPE")
        if (preparationLocalTargetSeen && !frame.localAtTarget)
            return fail("PREPARATION_LOCAL_REGRESSION")
        if (frame.localAtTarget) preparationLocalTargetSeen = true
        val current = frame.windows!!.single { it.id == hostId }
        val target = preparationTargetHost
        if (frame.remoteAtTarget) {
            if (target != null && !sameShape(current, target))
                return fail("PREPARATION_TARGET_SHAPE_CHANGED")
            if (target == null) preparationTargetHost = current
        } else {
            if (target != null) return fail("PREPARATION_REMOTE_REGRESSION")
            if (!sameShape(current, old)) return fail("PREPARATION_REMOTE_THIRD_SHAPE")
        }
        preparationLastUptime = frame.uptimeMs
        preparationReady = frame.localAtTarget && frame.remoteAtTarget
        if (eventUptimeMs != null) {
            val invalid = acknowledgeHostBounds(eventUptimeMs, frame.remoteAtTarget)
            if (invalid != null) return fail(invalid)
        }
        return accepted()
    }

    private fun preparationInventoryError(current: List<Window>?, currentScreen: Any): String? {
        inventoryError(current)?.let { return "PREPARATION_$it" }
        if (currentScreen != screen || current!!.sortedBy { it.layer }.map { it.id } !=
            baseline.sortedBy { it.layer }.map { it.id }) return "PREPARATION_INVENTORY"
        if (baseline.any { before ->
                val now = current.single { it.id == before.id }
                if (before.id == hostId) before.stableWithoutShape() != now.stableWithoutShape()
                    else before != now
            }) return "PREPARATION_METADATA_CHANGED"
        return null
    }

    private fun sameShape(first: Window, second: Window): Boolean =
        first.bounds == second.bounds && first.region == second.region

    private fun acknowledgeHostBounds(eventUptimeMs: Long, atTarget: Boolean): String? {
        if (atTarget) {
            if (targetBoundsNoticeUptime != null) return "PREPARATION_TARGET_BOUNDS_DUPLICATE"
            if (oldBoundsNoticeUptime?.let { eventUptimeMs <= it } == true)
                return "PREPARATION_TARGET_NOTICE_ORDER"
            targetBoundsNoticeUptime = eventUptimeMs
        } else {
            if (targetBoundsNoticeUptime != null) return "PREPARATION_OLD_NOTICE_AFTER_TARGET"
            if (oldBoundsNoticeUptime != null) return "PREPARATION_OLD_BOUNDS_DUPLICATE"
            oldBoundsNoticeUptime = eventUptimeMs
        }
        return null
    }

    fun observe(event: Event, frame: Frame): Decision {
        if (state == State.FAILED) return Decision(false, false, reason)
        val clock = clockError(event, frame)
        if (clock != null) return fail(clock)
        lastFrameElapsed = frame.elapsedMs
        lastFrameUptime = frame.uptimeMs
        if (event.kind != Kind.HOST_BOUNDS) lastObservedEventUptime = event.uptimeMs
        if (!frame.entryActive) return fail("ENTRY_OWNER_OR_PHASE")
        val current = frame.windows ?: return fail(frame.missingCode ?: "CURRENT_MISSING_METADATA")
        inventoryError(current)?.let { return fail("CURRENT_$it") }
        if (frame.screen != screen) return fail("DISPLAY_GEOMETRY")
        val known = baseline.mapTo(mutableSetOf()) { it.id }
        frame.reviewId?.let(known::add)
        if (event.id !in known) return fail("UNKNOWN_EVENT_ID")
        if (current.any { it.id !in known }) return fail("UNKNOWN_WINDOW_ID")
        if (!originalAnchored && event.id == originalId && event.kind == Kind.ORIGINAL_FOCUS &&
            current.any { it.id == originalId }) {
            if (originalFocusLossObserved || originalFocusBits != 0) return fail("ORIGINAL_LOSS_DUPLICATE")
            if (event.focusBits != 3) return fail("ORIGINAL_LOSS_MASK")
            val before = baseline.single { it.id == originalId }
            if (!before.focused || !before.active) return fail("ORIGINAL_LOSS_BASELINE")
            pendingOriginalError(frame)?.let { return fail(it) }
            focusLossIdentity = frame.localReviewIdentity
            focusLossReviewId = frame.reviewId
            originalFocusBits = 3
            return Decision(true, false, "ORIGINAL_FOCUS_LOSS_PENDING")
        }
        // Provisional notices confer no anchor authority, but may never hide observed drift.
        projectionError(frame, requireOriginalAnchor = originalAnchored)?.let { return fail(it) }
        if (originalAnchored) {
            advanceEpoch(current)?.let { return fail(it) }
        } else {
            val imeAbsent = current.none { it.id == imeId }
            if (observedImeAbsenceBeforeAnchor && !imeAbsent) return fail("IME_EPOCH_SKIPPED")
            if (imeAbsent) observedImeAbsenceBeforeAnchor = true
        }

        if (event.kind == Kind.HOST_BOUNDS && event.id == hostId) {
            if (!frame.hostProved ||
                event.uptimeMs < (resizeStartedUptime ?: Long.MAX_VALUE) ||
                event.uptimeMs > (launchedUptime ?: Long.MIN_VALUE))
                return fail("HOST_RESIZE_EVENT")
            acknowledgeHostBounds(event.uptimeMs, atTarget = true)?.let { return fail(it) }
            return accepted()
        }
        if (!originalAnchored && event.id == originalId && event.kind == Kind.ORIGINAL_FOCUS) {
            if (event.focusBits !in 1..3 || originalFocusBits and event.focusBits != 0)
                return fail("ORIGINAL_FOCUS_EVENT")
            originalFocusBits = originalFocusBits or event.focusBits
            return accepted()
        }
        if (!originalAnchored && event.id == frame.reviewId && event.kind == Kind.REVIEW_CONTROL)
            return reviewControl(event)
        if (!originalAnchored && event.id == imeId && event.kind == Kind.REMOVED) {
            if (provisionalImeRemoval != null || current.any { it.id == imeId })
                return fail("IME_REMOVAL_MARKER")
            provisionalImeRemoval = event
            return accepted()
        }
        if (!originalAnchored) {
            if (event.id != originalId || event.kind != Kind.REMOVED)
                return fail(if (event.id == frame.reviewId && event.kind == Kind.ADDED)
                    "NO_ORIGINAL_REMOVAL_UNSUPPORTED" else "ORIGINAL_ANCHOR_MISSING")
            if (!frame.originalRemovalAccepted) return fail("ORIGINAL_ANCHOR_UNPROVED")
            val projection = projectionError(frame)
            if (projection != null) return fail(projection)
            originalAnchored = true
            hadImeGap = current.none { it.id == imeId }
            if (!hadImeGap && provisionalImeRemoval != null) return fail("IME_EPOCH_SKIPPED")
            downWindows = current.toList()
            expected = delta(baseline, current)
            next = 0
            state = State.DOWN_PENDING
            provisionalImeRemoval?.let {
                val prefix = consume(it, frame)
                if (!prefix.accepted) return prefix
            }
            return consume(event, frame)
        }

        if (event.id == frame.reviewId && event.kind == Kind.REVIEW_CONTROL) return reviewControl(event)
        if (state == State.COMPLETE) return fail("EXTRA_EVENT_AFTER_COMPLETE")
        if (state == State.DOWN_COMPLETE) return fail("UNPREDICTED_DOWN_EVENT")
        return consume(event, frame)
    }

    /** Polls preserve observed continuity without consuming any window notification or anchor. */
    fun observePreAnchorFrame(frame: Frame): Decision {
        if (state == State.FAILED) return Decision(false, false, reason)
        if (!awaitingRemovalAfterFocusLoss) return accepted()
        val launched = launchedElapsed ?: return fail("NOT_LAUNCHED")
        if (frame.elapsedMs < launched || frame.elapsedMs - launched >= ENTRY_MS)
            return fail("ENTRY_DEADLINE")
        if (frame.elapsedMs < (lastFrameElapsed ?: launched) ||
            frame.uptimeMs < (lastFrameUptime ?: launchedUptime ?: Long.MAX_VALUE))
            return fail("SNAPSHOT_CLOCK_REGRESSED")
        if (!frame.entryActive) return fail("ENTRY_OWNER_OR_PHASE")
        inventoryError(frame.windows)?.let { return fail(frame.missingCode ?: "CURRENT_$it") }
        val current = frame.windows!!
        if (current.any { it.id == originalId }) {
            pendingOriginalError(frame)?.let { return fail(it) }
        } else {
            projectionError(frame, requireOriginalAnchor = false)?.let { return fail(it) }
            val imeAbsent = current.none { it.id == imeId }
            if (observedImeAbsenceBeforeAnchor && !imeAbsent) return fail("IME_EPOCH_SKIPPED")
            if (imeAbsent) observedImeAbsenceBeforeAnchor = true
        }
        lastFrameElapsed = frame.elapsedMs
        lastFrameUptime = frame.uptimeMs
        return accepted()
    }

    private fun focusLossIdentityError(frame: Frame): String? {
        if (!originalFocusLossObserved) return null
        if (frame.localReviewIdentity != focusLossIdentity) return "ORIGINAL_LOSS_LOCAL_IDENTITY"
        if (focusLossReviewId != null && frame.reviewId != focusLossReviewId)
            return "ORIGINAL_LOSS_REVIEW_ID"
        if (focusLossReviewId == null) focusLossReviewId = frame.reviewId
        return null
    }

    private fun pendingOriginalError(frame: Frame): String? {
        if (state != State.BASELINE || reviewIdentity != null || originalAnchored ||
            frame.originalRemovalAccepted || observedImeAbsenceBeforeAnchor)
            return "ORIGINAL_LOSS_STAGE_CLOSED"
        if (!frame.preAnchorProved || frame.localReviewIdentity == null) return "ORIGINAL_LOSS_OWNED_PROOF"
        focusLossIdentityError(frame)?.let { return it }
        inventoryError(frame.windows)?.let { return frame.missingCode ?: "CURRENT_$it" }
        val current = frame.windows!!
        if (frame.screen != screen || current.sortedBy { it.layer }.map { it.id } !=
            baseline.sortedBy { it.layer }.map { it.id }) return "ORIGINAL_LOSS_INVENTORY"
        if (frame.reviewId != null && current.any { it.id == frame.reviewId }) return "ORIGINAL_LOSS_REVIEW_PRESENT"
        if (current.any { it.focused || it.active }) return "ORIGINAL_LOSS_FOCUS_OR_ACTIVE"
        for (before in baseline) {
            val now = current.single { it.id == before.id }
            val expected = when (before.id) {
                originalId -> before.copy(focused = false, active = false)
                hostId -> preparationTargetHost ?: return "ORIGINAL_LOSS_HOST_TARGET"
                else -> before
            }
            if (now != expected) return "ORIGINAL_LOSS_METADATA_CHANGED"
        }
        return null
    }

    /** Read-only current proof before Confirm/return. Completion cannot be reconstructed later. */
    fun verifyComplete(frame: Frame): Boolean {
        if (state == State.FAILED) return false
        if (!complete) { fail("LEDGER_INCOMPLETE"); return false }
        if (!frame.entryActive) { fail("COMPLETION_OWNER_OR_PHASE"); return false }
        if (frame.elapsedMs < (lastFrameElapsed ?: Long.MAX_VALUE) ||
            frame.uptimeMs < (lastFrameUptime ?: Long.MAX_VALUE)) {
            fail("COMPLETION_CLOCK_REGRESSED"); return false
        }
        val invalid = projectionError(frame)
        if (invalid != null) { fail(invalid); return false }
        if (frame.windows!!.none { it.id == imeId }) { fail("IME_NOT_RESTORED"); return false }
        return true
    }

    fun expireIncomplete(): Decision = when (state) {
        State.COMPLETE -> accepted()
        State.FAILED -> Decision(false, false, reason)
        else -> fail("ENTRY_DEADLINE")
    }

    fun unexpectedPreparationEvent(): Decision = if (state == State.FAILED)
        Decision(false, false, reason) else fail("PREPARATION_UNEXPECTED_WINDOW_EVENT")

    private fun projectionError(frame: Frame, requireOriginalAnchor: Boolean = true): String? {
        inventoryError(frame.windows)?.let { return frame.missingCode ?: "CURRENT_$it" }
        focusLossIdentityError(frame)?.let { return it }
        if ((requireOriginalAnchor && !frame.originalRemovalAccepted) ||
            !frame.reviewProved || !frame.hostProved)
            return "CURRENT_OWNED_PROOF"
        if (frame.screen != screen) return "DISPLAY_GEOMETRY"
        val current = frame.windows!!
        val reviewId = frame.reviewId ?: return "REVIEW_ID"
        if (reviewId in baseline.map { it.id }) return "REVIEW_ID_COLLISION"
        if (current.any { it.id == originalId }) return "NO_ORIGINAL_REMOVAL_UNSUPPORTED"
        val review = current.singleOrNull { it.id == reviewId } ?: return "REVIEW_WINDOW"
        if (review.type != APPLICATION || review.root != frame.reviewRoot ||
            !review.focused || current.count { it.focused } != 1 || review.pictureInPicture)
            return "REVIEW_IDENTITY"
        val previousReview = reviewIdentity
        if (previousReview != null && previousReview.stableWithoutShape() != review.stableWithoutShape())
            return "REVIEW_METADATA_CHANGED"
        val imePresent = current.any { it.id == imeId }
        val expectedOrder = baseline.sortedBy { it.layer }.filter { imePresent || it.id != imeId }
            .map { if (it.id == originalId) reviewId else it.id }
        if (current.sortedBy { it.layer }.map { it.id } != expectedOrder)
            return "MEMBERSHIP_OR_RELATIVE_ORDER"
        for (before in baseline) {
            if (before.id == originalId || (!imePresent && before.id == imeId)) continue
            val now = current.single { it.id == before.id }
            val same = if (before.id == hostId)
                before.stableWithoutShape() == now.stableWithoutShape()
                else before.stable() == now.stable()
            if (!same) return when (before.id) {
                hostId -> "HOST_METADATA_CHANGED"
                imeId -> "IME_METADATA_CHANGED"
                else -> "SURVIVOR_METADATA_CHANGED"
            }
            if (before.id == hostId && preparationTargetHost?.let { sameShape(now, it) } != true)
                return "HOST_TARGET_SHAPE_CHANGED"
        }
        if (previousReview == null) reviewIdentity = review
        return null
    }

    private fun delta(before: List<Window>, after: List<Window>): List<Expected> {
        val prior = before.associateBy { it.id }
        val current = after.associateBy { it.id }
        val removals = before.sortedByDescending { it.layer }.filter { it.id !in current }
            .map { Expected(it.id, Kind.REMOVED) }
        val changes = after.sortedByDescending { it.layer }.mapNotNull { window ->
            when {
                window.id !in prior -> Expected(window.id, Kind.ADDED)
                prior.getValue(window.id).layer != window.layer -> Expected(window.id, Kind.LAYER)
                else -> null
            }
        }
        return removals + changes
    }

    private fun advanceEpoch(current: List<Window>): String? {
        val imePresent = current.any { it.id == imeId }
        when (state) {
            State.DOWN_PENDING -> if (hadImeGap && imePresent) return "UP_BEFORE_DOWN_COMPLETE"
            State.DOWN_COMPLETE -> if (imePresent) {
                expected = delta(downWindows!!, current)
                next = 0
                state = State.UP_PENDING
            }
            State.UP_PENDING, State.COMPLETE -> if (!imePresent) return "SECOND_IME_CYCLE"
            else -> return "EPOCH_STATE"
        }
        return null
    }

    private fun consume(event: Event, frame: Frame): Decision {
        val wanted = expected.getOrNull(next) ?: return fail("EXTRA_OR_DUPLICATE_EVENT")
        if (event.id != wanted.id || event.kind != wanted.kind) return fail("EVENT_ORDER_OR_MASK")
        if (event.uptimeMs < (lastEventUptime ?: Long.MIN_VALUE)) return fail("EVENT_TIME_REGRESSED")
        if (state == State.UP_PENDING && event.uptimeMs <= (downLastUptime ?: Long.MAX_VALUE))
            return fail("STALE_UP_EVENT")
        if (event.uptimeMs > frame.uptimeMs) return fail("EVENT_TIME_FUTURE")
        lastEventUptime = event.uptimeMs
        next++
        if (next == expected.size) {
            if (state == State.DOWN_PENDING && hadImeGap) {
                downLastUptime = event.uptimeMs
                state = State.DOWN_COMPLETE
            } else state = State.COMPLETE
        }
        return accepted(event.kind == Kind.LAYER)
    }

    private fun clockError(event: Event, frame: Frame): String? {
        val elapsed = launchedElapsed ?: return "NOT_LAUNCHED"
        val uptime = launchedUptime ?: return "NOT_LAUNCHED"
        if (frame.elapsedMs < elapsed || frame.elapsedMs - elapsed >= ENTRY_MS) return "ENTRY_DEADLINE"
        if (frame.elapsedMs < (lastFrameElapsed ?: elapsed) ||
            frame.uptimeMs < (lastFrameUptime ?: uptime)) return "SNAPSHOT_CLOCK_REGRESSED"
        if (event.uptimeMs > frame.uptimeMs || event.uptimeMs < 0) return "EVENT_CLOCK"
        if (event.kind != Kind.HOST_BOUNDS && event.uptimeMs < uptime) return "STALE_EVENT"
        if (event.kind != Kind.HOST_BOUNDS &&
            event.uptimeMs < (lastObservedEventUptime ?: uptime)) return "OBSERVED_EVENT_TIME_REGRESSED"
        return null
    }

    /** Bounds may change once per declared IME epoch; focus/active/title bits only once. */
    private fun reviewControl(event: Event): Decision {
        val bits = event.controlBits
        if (bits !in 1..15) return fail("REVIEW_CONTROL_MASK")
        val bounds = bits and 1 != 0
        val other = bits and 14
        val up = state == State.UP_PENDING || (state == State.COMPLETE && hadImeGap)
        if (reviewControlBits and other != 0 || bounds && (if (up) reviewBoundsUp else reviewBoundsDown))
            return fail("REVIEW_CONTROL_REPEATED")
        reviewControlBits = reviewControlBits or other
        if (bounds) { if (up) reviewBoundsUp = true else reviewBoundsDown = true }
        return accepted()
    }

    private fun accepted(layer: Boolean = false) = Decision(true, layer, "OK")
    private fun fail(code: String): Decision {
        state = State.FAILED
        reason = code
        return Decision(false, false, code)
    }
}
