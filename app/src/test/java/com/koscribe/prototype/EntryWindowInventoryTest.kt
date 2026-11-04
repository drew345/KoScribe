package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryWindowInventoryTest {
    private val original = 10
    private val ime = 20
    private val system = 30
    private val host = 40
    private val review = 50
    private val localIdentity = "local-review-objects"
    private val screen = "display-shape"
    private fun window(id: Int, type: Int, layer: Int, focused: Boolean = false) =
        EntryWindowInventory.Window(id, type, 0, layer, "root-$id", "package-$id", "class-$id",
            "bounds-$id", "region-$id", null, emptyList(), false, focused, focused, false)
    private val baseline get() = listOf(window(original, 1, 0, true), window(ime, 2, 1),
        window(system, 3, 2), window(host, 4, 3))
    private val down get() = listOf(window(review, 1, 0, true), window(system, 3, 1),
        window(host, 4, 2).copy(bounds = "tight-host", region = "tight-region"))
    private val restored get() = listOf(window(review, 1, 0, true), window(ime, 2, 1),
        window(system, 3, 2), window(host, 4, 3).copy(bounds = "tight-host", region = "tight-region"))
    private val recording get() = baseline.map { if (it.id == host)
        it.copy(bounds = "recording-host", region = "recording-region") else it }
    private val prepared get() = baseline.map { if (it.id == host)
        it.copy(bounds = "tight-host", region = "tight-region") else it }
    private fun captured() = EntryWindowInventory.capture(baseline, original, host, screen).proof!!
    private fun preparing(): EntryWindowInventory = captured().also {
        assertTrue(it.beginPreparation(recording, screen, 480L, 480L, true).accepted)
    }
    private fun prepFrame(now: Long = 490L, localTarget: Boolean = false,
        remoteTarget: Boolean = false, current: List<EntryWindowInventory.Window> =
            if (remoteTarget) prepared else recording) = EntryWindowInventory.PreparationFrame(
        current, screen, now, true, !localTarget, localTarget, remoteTarget)
    private fun proof(): EntryWindowInventory = preparing().also {
        assertTrue(it.observePreparation(prepFrame(495L, true, true)).accepted)
        assertTrue(it.start(1_000L, 500L, 480L))
    }
    private fun frame(windows: List<EntryWindowInventory.Window> = down, delta: Long = 100L,
        anchored: Boolean = true) = EntryWindowInventory.Frame(windows, null, review, "root-$review",
        anchored, true, true, true, screen, 1_000L + delta, 500L + delta,
            localReviewIdentity = localIdentity)
    private fun lossFrame(delta: Long = 10L) = frame(prepared.map {
        if (it.id == original) it.copy(focused = false, active = false) else it
    }, delta, anchored = false).copy(reviewProved = false, hostProved = false,
        reviewRoot = null, preAnchorProved = true)
    private fun lossEvent(time: Long = 505L, bits: Int = 3) = EntryWindowInventory.Event(
        original, EntryWindowInventory.Kind.ORIGINAL_FOCUS, time, focusBits = bits)
    private fun event(id: Int, kind: EntryWindowInventory.Kind, time: Long) =
        EntryWindowInventory.Event(id, kind, time)
    private fun acknowledge(proof: EntryWindowInventory, id: Int, kind: EntryWindowInventory.Kind,
        time: Long, frame: EntryWindowInventory.Frame = frame()) {
        val result = proof.observe(event(id, kind, time), frame)
        assertTrue(result.reason, result.accepted)
    }
    private fun finishDown(proof: EntryWindowInventory) {
        acknowledge(proof, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
        acknowledge(proof, original, EntryWindowInventory.Kind.REMOVED, 511L)
        acknowledge(proof, host, EntryWindowInventory.Kind.LAYER, 512L)
        acknowledge(proof, system, EntryWindowInventory.Kind.LAYER, 513L)
        acknowledge(proof, review, EntryWindowInventory.Kind.ADDED, 514L)
        assertEquals(EntryWindowInventory.State.DOWN_COMPLETE, proof.state)
    }
    private fun finishUp(proof: EntryWindowInventory) {
        val current = frame(restored, 200L)
        acknowledge(proof, host, EntryWindowInventory.Kind.LAYER, 600L, current)
        acknowledge(proof, system, EntryWindowInventory.Kind.LAYER, 601L, current)
        acknowledge(proof, ime, EntryWindowInventory.Kind.ADDED, 602L, current)
    }

    @Test fun orderedRestorationLayersPrecedeImeAddedAndOnlyThenComplete() {
        val proof = proof()
        finishDown(proof)
        val current = frame(restored, 200L)
        acknowledge(proof, host, EntryWindowInventory.Kind.LAYER, 600L, current)
        assertEquals(EntryWindowInventory.State.UP_PENDING, proof.state)
        assertFalse(proof.complete)
        acknowledge(proof, system, EntryWindowInventory.Kind.LAYER, 601L, current)
        assertFalse(proof.complete)
        acknowledge(proof, ime, EntryWindowInventory.Kind.ADDED, 602L, current)
        assertTrue(proof.complete)
        assertTrue(proof.verifyComplete(current.copy(elapsedMs = 20_000L, uptimeMs = 19_500L)))
    }

    @Test fun replacementWithoutImeCycleNeedsNoInventedImeNotifications() {
        val proof = proof()
        val current = frame(restored)
        acknowledge(proof, original, EntryWindowInventory.Kind.REMOVED, 510L, current)
        acknowledge(proof, review, EntryWindowInventory.Kind.ADDED, 511L, current)
        assertTrue(proof.complete)
        assertTrue(proof.verifyComplete(current))
    }

    @Test fun unknownAbsentRemovalAndUnknownCurrentWindowAreTerminal() {
        val unknownEvent = proof()
        assertEquals("UNKNOWN_EVENT_ID", unknownEvent.observe(
            event(999, EntryWindowInventory.Kind.REMOVED, 510L), frame()).reason)
        val unknownCurrent = proof()
        val changed = down.map { if (it.id == system) it.copy(id = 999) else it }
        assertEquals("UNKNOWN_WINDOW_ID", unknownCurrent.observe(
            event(ime, EntryWindowInventory.Kind.REMOVED, 510L), frame(changed)).reason)
    }

    @Test fun droppedReorderedDuplicateAndMixedLayerNotificationsRefuse() {
        for (wrong in listOf(event(system, EntryWindowInventory.Kind.LAYER, 512L),
                event(host, EntryWindowInventory.Kind.OTHER, 512L))) {
            val proof = proof()
            acknowledge(proof, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
            acknowledge(proof, original, EntryWindowInventory.Kind.REMOVED, 511L)
            assertEquals("EVENT_ORDER_OR_MASK", proof.observe(wrong, frame()).reason)
        }
        val duplicate = proof()
        acknowledge(duplicate, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
        acknowledge(duplicate, original, EntryWindowInventory.Kind.REMOVED, 511L)
        acknowledge(duplicate, host, EntryWindowInventory.Kind.LAYER, 512L)
        assertFalse(duplicate.observe(event(host, EntryWindowInventory.Kind.LAYER, 513L), frame()).accepted)
    }

    @Test fun restorationCannotSkipOutstandingDownAcknowledgments() {
        val proof = proof()
        acknowledge(proof, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
        acknowledge(proof, original, EntryWindowInventory.Kind.REMOVED, 511L)
        acknowledge(proof, host, EntryWindowInventory.Kind.LAYER, 512L)
        assertEquals("UP_BEFORE_DOWN_COMPLETE", proof.observe(
            event(host, EntryWindowInventory.Kind.LAYER, 600L), frame(restored, 200L)).reason)
    }

    @Test fun staleDownEventCannotBeReinterpretedAsUp() {
        val proof = proof()
        finishDown(proof)
        assertEquals("OBSERVED_EVENT_TIME_REGRESSED", proof.observe(
            event(host, EntryWindowInventory.Kind.LAYER, 512L), frame(restored, 200L)).reason)
        val equalTime = proof()
        finishDown(equalTime)
        assertEquals("STALE_UP_EVENT", equalTime.observe(
            event(host, EntryWindowInventory.Kind.LAYER, 514L), frame(restored, 200L)).reason)
    }

    @Test fun imeReplacementRegionChangesAndSurvivorOrderChangesRefuse() {
        for ((changed, expected) in listOf(
                restored.map { if (it.id == ime) it.copy(root = "replacement") else it } to "IME_METADATA_CHANGED",
                restored.map { if (it.id == system) it.copy(region = "changed-same-bounds") else it } to "SURVIVOR_METADATA_CHANGED",
                restored.map { when (it.id) { host -> it.copy(layer = 2); system -> it.copy(layer = 3); else -> it } }
                    to "MEMBERSHIP_OR_RELATIVE_ORDER")) {
            val proof = proof()
            finishDown(proof)
            assertEquals(expected, proof.observe(event(host, EntryWindowInventory.Kind.LAYER, 600L),
                frame(changed, 200L)).reason)
        }
    }

    @Test fun incompleteDuplicateAndGappedBaselinesRefuse() {
        assertEquals("BASELINE_MISSING_METADATA", EntryWindowInventory.capture(null, original, host, screen).reason)
        assertEquals("BASELINE_WINDOW_IDS", EntryWindowInventory.capture(baseline + baseline.last(),
            original, host, screen).reason)
        assertEquals("BASELINE_ORDINAL_LAYERS", EntryWindowInventory.capture(
            baseline.map { if (it.id == host) it.copy(layer = 4) else it }, original, host, screen).reason)
        assertEquals("BASELINE_ROOT_METADATA", EntryWindowInventory.capture(
            baseline.map { if (it.id == system) it.copy(rootClass = "") else it }, original, host, screen).reason)
    }

    @Test fun deadlineStillAppliesAfterGeometryIsReadyAndLateEventsNeverRevive() {
        val proof = proof()
        finishDown(proof)
        acknowledge(proof, host, EntryWindowInventory.Kind.LAYER, 600L, frame(restored, 200L))
        acknowledge(proof, system, EntryWindowInventory.Kind.LAYER, 601L, frame(restored, 200L))
        assertEquals("ENTRY_DEADLINE", proof.observe(event(ime, EntryWindowInventory.Kind.ADDED, 1_500L),
            frame(restored, 1_000L)).reason)
        assertFalse(proof.observe(event(ime, EntryWindowInventory.Kind.ADDED, 602L),
            frame(restored, 200L)).accepted)
    }

    @Test fun earlyConfirmationOwnerLossAndSecondCycleAreTerminal() {
        val early = proof()
        finishDown(early)
        assertFalse(early.verifyComplete(frame()))
        assertEquals("LEDGER_INCOMPLETE", early.reason)
        val ownerLost = proof()
        acknowledge(ownerLost, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
        assertEquals("CURRENT_OWNED_PROOF", ownerLost.observe(
            event(original, EntryWindowInventory.Kind.REMOVED, 511L), frame().copy(hostProved = false)).reason)
        val secondCycle = proof()
        finishDown(secondCycle)
        finishUp(secondCycle)
        assertEquals("SECOND_IME_CYCLE", secondCycle.observe(
            event(ime, EntryWindowInventory.Kind.REMOVED, 710L), frame(delta = 250L)).reason)
    }

    @Test fun ownedResizeIsBoundedAndNoOriginalRemovalIsExplicitlyUnsupported() {
        val resize = proof()
        assertEquals("HOST_RESIZE_EVENT", resize.observe(
            event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 479L), frame()).reason)
        val noRemoval = proof()
        assertEquals("NO_ORIGINAL_REMOVAL_UNSUPPORTED", noRemoval.observe(
            event(review, EntryWindowInventory.Kind.ADDED, 510L), frame(anchored = false)).reason)
    }

    @Test fun provisionalAndLateOwnedNoticesCannotHideObservedSurvivorDrift() {
        val provisional = proof()
        val drifted = down.map { if (it.id == system) it.copy(region = "changed") else it }
        assertEquals("SURVIVOR_METADATA_CHANGED", provisional.observe(
            event(ime, EntryWindowInventory.Kind.REMOVED, 510L), frame(drifted, anchored = false)).reason)
        assertFalse(provisional.observe(event(original, EntryWindowInventory.Kind.REMOVED, 511L),
            frame()).accepted)
        val late = proof()
        finishDown(late)
        finishUp(late)
        val laterDrift = restored.map { if (it.id == system) it.copy(root = "different") else it }
        assertEquals("SURVIVOR_METADATA_CHANGED", late.observe(
            event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 490L), frame(laterDrift, 250L)).reason)
        assertFalse(late.verifyComplete(frame(restored, 300L)))
        assertEquals("SURVIVOR_METADATA_CHANGED", late.reason)
    }

    @Test fun completedProofStillRequiresCurrentOwnerAndMonotonicObservation() {
        val lostOwner = proof()
        finishDown(lostOwner)
        finishUp(lostOwner)
        assertFalse(lostOwner.verifyComplete(frame(restored, 300L).copy(entryActive = false)))
        assertEquals("COMPLETION_OWNER_OR_PHASE", lostOwner.reason)
        val clock = proof()
        finishDown(clock)
        finishUp(clock)
        assertFalse(clock.verifyComplete(frame(restored, 199L)))
        assertEquals("COMPLETION_CLOCK_REGRESSED", clock.reason)
    }

    @Test fun preparationAndOwnedControlsCannotRepeatOrHideMetadataChanges() {
        val capture = preparing()
        assertTrue(capture.observePreparation(prepFrame(), 489L).accepted)
        assertEquals("PREPARATION_OLD_BOUNDS_DUPLICATE", capture.observePreparation(
            prepFrame(496L), 491L).reason)
        val controls = proof()
        finishDown(controls)
        finishUp(controls)
        val changed = EntryWindowInventory.Event(review, EntryWindowInventory.Kind.REVIEW_CONTROL,
            800L, controlBits = 2)
        assertTrue(controls.observe(changed, frame(restored, 400L)).accepted)
        assertEquals("REVIEW_CONTROL_REPEATED", controls.observe(changed.copy(uptimeMs = 801L),
            frame(restored, 401L)).reason)
    }

    @Test fun cyclicAndNonreciprocalWindowTopologyCannotBeCaptured() {
        val cyclic = baseline.map { when (it.id) {
            system -> it.copy(parent = host, children = listOf(host))
            host -> it.copy(parent = system, children = listOf(system))
            else -> it
        } }
        assertEquals("BASELINE_TOPOLOGY_CYCLE", EntryWindowInventory.capture(cyclic,
            original, host, screen).reason)
        val nonreciprocal = baseline.map { if (it.id == host) it.copy(parent = system) else it }
        assertEquals("BASELINE_TOPOLOGY_RECIPROCAL", EntryWindowInventory.capture(nonreciprocal,
            original, host, screen).reason)
    }

    @Test fun ownedNoticesCannotHideSkippedOrRepeatedImeEpochs() {
        val complete = proof()
        finishDown(complete)
        finishUp(complete)
        assertEquals("SECOND_IME_CYCLE", complete.observe(
            event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 490L), frame(delta = 250L)).reason)
        val pending = proof()
        acknowledge(pending, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
        acknowledge(pending, original, EntryWindowInventory.Kind.REMOVED, 511L)
        assertEquals("UP_BEFORE_DOWN_COMPLETE", pending.observe(
            event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 490L), frame(restored, 200L)).reason)
        val provisional = proof()
        acknowledge(provisional, ime, EntryWindowInventory.Kind.REMOVED, 510L, frame(anchored = false))
        assertEquals("IME_EPOCH_SKIPPED", provisional.observe(
            EntryWindowInventory.Event(review, EntryWindowInventory.Kind.REVIEW_CONTROL, 511L,
                controlBits = 1), frame(restored, anchored = false)).reason)
    }

    @Test fun ownedControlAlsoOrdersLaterEpochNotifications() {
        val proof = proof()
        finishDown(proof)
        assertTrue(proof.observe(EntryWindowInventory.Event(review,
            EntryWindowInventory.Kind.REVIEW_CONTROL, 800L, controlBits = 2),
            frame(delta = 400L)).accepted)
        assertEquals("OBSERVED_EVENT_TIME_REGRESSED", proof.observe(
            event(host, EntryWindowInventory.Kind.LAYER, 600L), frame(restored, 500L)).reason)
    }

    @Test fun anyObservedPreAnchorAbsenceCannotBecomeAnUnchangedImePath() {
        val proof = proof()
        assertTrue(proof.observe(event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 490L),
            frame(anchored = false)).accepted)
        assertEquals("IME_EPOCH_SKIPPED", proof.observe(
            event(original, EntryWindowInventory.Kind.REMOVED, 511L), frame(restored)).reason)
    }

    @Test fun unusedOwnedControlsExpireAndUnexpectedPreparationCannotRearm() {
        val proof = proof()
        finishDown(proof)
        finishUp(proof)
        assertEquals("ENTRY_DEADLINE", proof.observe(EntryWindowInventory.Event(review,
            EntryWindowInventory.Kind.REVIEW_CONTROL, 1_500L, controlBits = 8),
            frame(restored, 1_000L)).reason)
        val prep = EntryWindowInventory.capture(baseline, original, host, screen).proof!!
        assertFalse(prep.unexpectedPreparationEvent().accepted)
        assertFalse(prep.start(1_000L, 500L, 480L))
    }

    @Test fun preparationClocksAndIdentityFailuresRemainTerminal() {
        val launched = proof()
        assertEquals("PREPARATION_ALREADY_LAUNCHED", launched.observePreparation(prepFrame()).reason)
        val cases = listOf(479L to "PREPARATION_EVENT_BEFORE_LEASE",
            491L to "PREPARATION_EVENT_FUTURE")
        for ((eventTime, reason) in cases) {
            val candidate = preparing()
            val result = candidate.observePreparation(prepFrame(), eventTime)
            assertFalse(result.accepted)
            assertEquals(reason, result.reason)
            assertEquals(result.reason, candidate.observePreparation(prepFrame(495L, true, true)).reason)
            assertFalse(candidate.start(1_000L, 500L, 480L))
        }
        assertEquals("PREPARATION_HOST_IDENTITY", preparing().observePreparation(
            prepFrame().copy(hostIdentityProved = false)).reason)
        assertEquals("PREPARATION_OLD_HOST_UNPROVED", captured().beginPreparation(
            recording, screen, 480L, 480L, false).reason)
    }

    @Test fun measuredLocalTargetRemoteOldWaitsUntilBothEndpointsAreProved() {
        val candidate = preparing()
        assertTrue(candidate.observePreparation(prepFrame(localTarget = true), 485L).accepted)
        assertFalse(candidate.preparationReady)
        assertTrue(candidate.observePreparation(prepFrame(495L, true, true), 492L).accepted)
        assertTrue(candidate.preparationReady)
        assertTrue(candidate.start(1_000L, 500L, 480L))
        finishDown(candidate)
        finishUp(candidate)
        assertTrue(candidate.complete)
    }

    @Test fun endpointNoticesAreUniqueAndStrictlyOrdered() {
        val targetTwice = preparing()
        assertTrue(targetTwice.observePreparation(prepFrame(490L, true, true), 485L).accepted)
        assertEquals("PREPARATION_TARGET_BOUNDS_DUPLICATE", targetTwice.observePreparation(
            prepFrame(495L, true, true), 492L).reason)
        for (targetTime in listOf(484L, 485L)) {
            val order = preparing()
            assertTrue(order.observePreparation(prepFrame(), 485L).accepted)
            assertEquals("PREPARATION_TARGET_NOTICE_ORDER", order.observePreparation(
                prepFrame(495L, true, true), targetTime).reason)
        }
    }

    @Test fun eitherEndpointRegressionAndThirdShapesRefuseOnPollWithoutNotices() {
        val local = preparing()
        assertTrue(local.observePreparation(prepFrame(localTarget = true)).accepted)
        assertEquals("PREPARATION_LOCAL_REGRESSION", local.observePreparation(prepFrame(495L)).reason)
        val remote = preparing()
        assertTrue(remote.observePreparation(prepFrame(490L, false, true)).accepted)
        assertFalse(remote.preparationReady)
        assertEquals("PREPARATION_REMOTE_REGRESSION", remote.observePreparation(
            prepFrame(495L)).reason)
        val third = recording.map { if (it.id == host) it.copy(bounds = "intermediate") else it }
        assertEquals("PREPARATION_REMOTE_THIRD_SHAPE", preparing().observePreparation(
            prepFrame(current = third)).reason)
        assertEquals("PREPARATION_LOCAL_THIRD_SHAPE", preparing().observePreparation(
            prepFrame().copy(localAtOld = false)).reason)
    }

    @Test fun targetRegionAndEverySurvivorRemainImmutableDuringPreparation() {
        val target = preparing()
        assertTrue(target.observePreparation(prepFrame(490L, true, true)).accepted)
        val changedRegion = prepared.map { if (it.id == host) it.copy(region = "other-tight-region") else it }
        assertEquals("PREPARATION_TARGET_SHAPE_CHANGED", target.observePreparation(
            prepFrame(495L, true, true, changedRegion)).reason)
        val drift = recording.map { if (it.id == system) it.copy(root = "new-root") else it }
        assertEquals("PREPARATION_METADATA_CHANGED", preparing().observePreparation(
            prepFrame(current = drift)).reason)
        assertEquals("PREPARATION_METADATA_CHANGED", captured().beginPreparation(
            drift, screen, 480L, 480L, true).reason)
        val missing = recording.filter { it.id != ime }.mapIndexed { index, window -> window.copy(layer = index) }
        assertEquals("PREPARATION_INVENTORY", preparing().observePreparation(
            prepFrame(current = missing)).reason)
    }

    @Test fun preparationDeadlineIncludesInitialCaptureFinalReadAndLaunch() {
        assertEquals("PREPARATION_DEADLINE", captured().beginPreparation(
            recording, screen, 480L, 980L, true).reason)
        assertEquals("PREPARATION_DEADLINE", preparing().observePreparation(
            prepFrame(980L, true, true)).reason)
        val launch = preparing()
        assertTrue(launch.observePreparation(prepFrame(979L, true, true)).accepted)
        assertFalse(launch.start(1_000L, 980L, 480L))
        assertEquals("PREPARATION_NOT_READY_AT_LAUNCH", launch.reason)
        val clock = preparing()
        assertTrue(clock.observePreparation(prepFrame(490L)).accepted)
        assertEquals("PREPARATION_CLOCK_REGRESSED", clock.observePreparation(prepFrame(489L)).reason)
        val early = preparing()
        assertTrue(early.observePreparation(prepFrame(localTarget = true)).accepted)
        assertFalse(early.start(1_000L, 500L, 480L))
    }

    @Test fun noOpNeedsNoInventedNoticeAndLateTargetNoticeCannotBeReused() {
        val noOp = captured()
        assertTrue(noOp.beginPreparation(prepared, screen, 480L, 480L, true).accepted)
        assertTrue(noOp.observePreparation(prepFrame(490L, true, true).copy(localAtOld = true)).accepted)
        assertTrue(noOp.start(1_000L, 500L, 480L))
        val delayed = preparing()
        assertTrue(delayed.observePreparation(prepFrame(), 485L).accepted)
        assertTrue(delayed.observePreparation(prepFrame(495L, true, true)).accepted)
        assertTrue(delayed.start(1_000L, 500L, 480L))
        assertTrue(delayed.observe(event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 490L),
            frame(anchored = false)).accepted)
        assertEquals("PREPARATION_TARGET_BOUNDS_DUPLICATE", delayed.observe(
            event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 491L), frame(anchored = false)).reason)
        val tooLate = proof()
        assertEquals("HOST_RESIZE_EVENT", tooLate.observe(
            event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 501L), frame(anchored = false)).reason)
    }

    @Test fun finalHostRegionCannotDriftAfterLaunchEvenWithTightAdapterProof() {
        val candidate = proof()
        val changed = down.map { if (it.id == host) it.copy(region = "different-tight-region") else it }
        assertEquals("HOST_TARGET_SHAPE_CHANGED", candidate.observe(
            event(ime, EntryWindowInventory.Kind.REMOVED, 510L),
            frame(changed, anchored = false)).reason)
        assertFalse(candidate.observe(event(original, EntryWindowInventory.Kind.REMOVED, 511L), frame()).accepted)
    }

    @Test fun exactOriginalLossGrantsNoAnchorButCanPrecedeTheExistingFullLedger() {
        val candidate = proof()
        assertEquals("ORIGINAL_FOCUS_LOSS_PENDING", candidate.observe(lossEvent(), lossFrame()).reason)
        assertTrue(candidate.originalFocusLossObserved)
        assertTrue(candidate.awaitingRemovalAfterFocusLoss)
        assertEquals(EntryWindowInventory.State.BASELINE, candidate.state)
        assertFalse(candidate.complete)
        assertTrue(candidate.observePreAnchorFrame(lossFrame(20L)).accepted)
        finishDown(candidate)
        assertFalse(candidate.awaitingRemovalAfterFocusLoss)
        finishUp(candidate)
        assertTrue(candidate.verifyComplete(frame(restored, 250L)))
    }

    @Test fun originalLossRequiresBothRealLossBitsAndCannotRepeat() {
        for (bits in listOf(1, 2, 7)) assertEquals("ORIGINAL_LOSS_MASK",
            proof().observe(lossEvent(bits = bits), lossFrame()).reason)
        val duplicate = proof()
        assertTrue(duplicate.observe(lossEvent(), lossFrame()).accepted)
        assertEquals("ORIGINAL_LOSS_DUPLICATE", duplicate.observe(lossEvent(506L), lossFrame(20L)).reason)
        val inactive = baseline.map { if (it.id == original) it.copy(active = false) else it }
        val candidate = EntryWindowInventory.capture(inactive, original, host, screen).proof!!
        val target = prepared.map { if (it.id == original) it.copy(active = false) else it }
        assertTrue(candidate.beginPreparation(inactive, screen, 480L, 480L, true).accepted)
        assertTrue(candidate.observePreparation(prepFrame(495L, true, true, target)).accepted)
        assertTrue(candidate.start(1_000L, 500L, 480L))
        assertEquals("ORIGINAL_LOSS_BASELINE", candidate.observe(lossEvent(), lossFrame()).reason)
    }

    @Test fun provisionalOriginalProjectionRequiresExactSurvivorsAndNoSuccessor() {
        val originalLoss = lossFrame()
        for (changed in listOf(
            originalLoss.windows!!.map { if (it.id == ime) it.copy(root = "replacement-ime") else it },
            originalLoss.windows!!.map { if (it.id == system) it.copy(region = "changed-region") else it },
            originalLoss.windows!!.map { if (it.id == host) it.copy(region = "other-tight-region") else it },
            originalLoss.windows!!.map { if (it.id == original) it.copy(root = "replacement-root") else it }
        )) assertEquals("ORIGINAL_LOSS_METADATA_CHANGED", proof().observe(lossEvent(),
            originalLoss.copy(windows = changed)).reason)
        val missingIme = originalLoss.windows!!.filter { it.id != ime }
            .mapIndexed { index, window -> window.copy(layer = index) }
        assertEquals("ORIGINAL_LOSS_INVENTORY", proof().observe(lossEvent(),
            originalLoss.copy(windows = missingIme)).reason)
        val reviewAdded = originalLoss.windows!! + window(review, 1, 4)
        assertEquals("ORIGINAL_LOSS_INVENTORY", proof().observe(lossEvent(),
            originalLoss.copy(windows = reviewAdded)).reason)
        assertEquals("ORIGINAL_LOSS_OWNED_PROOF", proof().observe(lossEvent(),
            originalLoss.copy(preAnchorProved = false)).reason)
    }

    @Test fun originalLossCannotHideRegainIdentityChangeOrPollDrift() {
        for (focused in listOf(true, false)) {
            val candidate = proof()
            assertTrue(candidate.observe(lossEvent(), lossFrame()).accepted)
            val gained = lossFrame(20L).windows!!.map {
                if (it.id == original) it.copy(focused = focused, active = !focused) else it
            }
            assertEquals("ORIGINAL_LOSS_FOCUS_OR_ACTIVE", candidate.observePreAnchorFrame(
                lossFrame(20L).copy(windows = gained)).reason)
            assertFalse(candidate.observePreAnchorFrame(lossFrame(30L)).accepted)
        }
        val identity = proof()
        assertTrue(identity.observe(lossEvent(), lossFrame()).accepted)
        assertEquals("ORIGINAL_LOSS_LOCAL_IDENTITY", identity.observePreAnchorFrame(
            lossFrame(20L).copy(localReviewIdentity = "new-objects")).reason)
        val window = proof()
        assertTrue(window.observe(lossEvent(), lossFrame()).accepted)
        assertEquals("ORIGINAL_LOSS_REVIEW_ID", window.observePreAnchorFrame(
            lossFrame(20L).copy(reviewId = review + 1)).reason)
        val drift = proof()
        assertTrue(drift.observe(lossEvent(), lossFrame()).accepted)
        assertEquals("ORIGINAL_LOSS_METADATA_CHANGED", drift.observePreAnchorFrame(
            lossFrame(20L).copy(windows = lossFrame().windows!!.map {
                if (it.id == system) it.copy(root = "changed") else it
            })).reason)
    }

    @Test fun provisionalMembershipAndImeHistoryCannotReopenAfterReviewAppears() {
        val reappeared = proof()
        assertTrue(reappeared.observe(lossEvent(), lossFrame()).accepted)
        assertTrue(reappeared.observePreAnchorFrame(frame(delta = 20L, anchored = false)).accepted)
        assertEquals("ORIGINAL_LOSS_STAGE_CLOSED", reappeared.observePreAnchorFrame(lossFrame(30L)).reason)
        val imeRestored = proof()
        assertTrue(imeRestored.observe(lossEvent(), lossFrame()).accepted)
        assertTrue(imeRestored.observePreAnchorFrame(frame(delta = 20L, anchored = false)).accepted)
        assertEquals("IME_EPOCH_SKIPPED", imeRestored.observePreAnchorFrame(
            frame(restored, 30L, anchored = false)).reason)
    }

    @Test fun pendingOriginalCannotAuthorizeOtherActorsAndDeadlineNeverRenews() {
        for (event in listOf(event(host, EntryWindowInventory.Kind.HOST_BOUNDS, 490L),
            event(ime, EntryWindowInventory.Kind.REMOVED, 506L),
            EntryWindowInventory.Event(review, EntryWindowInventory.Kind.REVIEW_CONTROL, 506L, controlBits = 4))) {
            val candidate = proof()
            assertTrue(candidate.observe(lossEvent(), lossFrame()).accepted)
            assertFalse(candidate.observe(event, lossFrame(20L)).accepted)
        }
        val expired = proof()
        assertTrue(expired.observe(lossEvent(), lossFrame()).accepted)
        assertEquals("ENTRY_DEADLINE", expired.observePreAnchorFrame(lossFrame(1_000L)).reason)
        assertFalse(expired.observePreAnchorFrame(lossFrame(20L)).accepted)
        val stale = proof()
        assertEquals("STALE_EVENT", stale.observe(lossEvent(499L), lossFrame()).reason)
    }

    @Test fun previouslyUnknownLocalWindowIdIsLearnedOnceWithoutFocusAuthority() {
        val candidate = proof()
        assertTrue(candidate.observe(lossEvent(), lossFrame().copy(reviewId = null)).accepted)
        assertTrue(candidate.observePreAnchorFrame(lossFrame(20L)).accepted)
        assertEquals("ORIGINAL_LOSS_REVIEW_ID", candidate.observePreAnchorFrame(
            lossFrame(30L).copy(reviewId = null)).reason)
        val legacy = proof()
        assertTrue(legacy.observe(lossEvent(), frame(anchored = false)).accepted)
        assertEquals("ORIGINAL_FOCUS_EVENT", legacy.observe(lossEvent(506L), frame(anchored = false)).reason)
    }
}
