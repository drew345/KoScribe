package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class OriginalDestinationIntentTest {
    private val original = EditorSnapshot(
        "com.koscribe.smoke", 7, "node-42", "com.koscribe.smoke:id/test_editor",
        "EditText", 21, 1, "beforeafter", 0, 6, 6, false
    )

    private fun captured(at: Long = 100L): OriginalDestinationIntent =
        OriginalDestinationIntent.capture(original, 8, at, OutputMode.POLITE)!!

    private fun inReview(
        originalFinishFirst: Boolean = true,
        focusFirst: Boolean = false,
    ): OriginalDestinationIntent = captured().also { intent ->
        assertTrue(intent.beginReview("draft-1", 200))
        if (originalFinishFirst) assertTrue(intent.inputFinished(8, false))
        if (focusFirst) assertTrue(intent.reviewFocused("draft-1", 201))
        assertTrue(intent.reviewInputStarted(9, false))
        if (!focusFirst) assertTrue(intent.reviewFocused("draft-1", 202))
        assertEquals(OriginalDestinationIntent.Phase.REVIEW, intent.phase)
    }

    private fun returning(): OriginalDestinationIntent = inReview().also { intent ->
        assertTrue(intent.beginReturn("draft-1", 300, ownerStillFocused = true))
        assertTrue(intent.inputFinished(9, false))
        assertTrue(intent.originalInputReturned(10, false))
    }

    @Test fun onlySafeOwnedKoreanClickCanCaptureAndUnavailableIntentDoesNotBlockManualDraft() {
        assertNull(OriginalDestinationIntent.capture(
            original.copy(packageName = "other.app"), 8, 100, OutputMode.POLITE))
        assertNull(OriginalDestinationIntent.capture(original, 8, 100, OutputMode.ENGLISH))
        assertNull(OriginalDestinationIntent.capture(
            original.copy(selectionEnd = 7), 8, 100, OutputMode.CASUAL))
        assertNull(OriginalDestinationIntent.capture(
            original.copy(composing = true), 8, 100, OutputMode.POLITE))
        assertNull(OriginalDestinationIntent.capture(original, -1, 100, OutputMode.POLITE))
        assertNotNull(OriginalDestinationIntent.capture(original, 8, 100, OutputMode.CASUAL))
    }

    @Test fun authenticReviewCanArriveBeforeOrAfterItsInputStart() {
        assertEquals(OriginalDestinationIntent.Phase.REVIEW,
            inReview(focusFirst = false).phase)
        assertEquals(OriginalDestinationIntent.Phase.REVIEW,
            inReview(focusFirst = true).phase)
    }

    @Test fun earlyConfirmBeforeFullReviewProofFallsBackAndLateProofCannotRearm() {
        val awaitingOwnInput = captured()
        assertTrue(awaitingOwnInput.beginReview("draft-1", 200))
        assertTrue(awaitingOwnInput.reviewFocused("draft-1", 201))
        assertEquals(OriginalDestinationIntent.Phase.ENTERING_REVIEW, awaitingOwnInput.phase)
        assertFalse(awaitingOwnInput.beginReturn("draft-1", 202, ownerStillFocused = true))
        assertFalse(awaitingOwnInput.reviewInputStarted(9, false))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, awaitingOwnInput.phase)

        val awaitingWindowProof = captured()
        assertTrue(awaitingWindowProof.beginReview("draft-2", 200))
        assertTrue(awaitingWindowProof.reviewInputStarted(9, false))
        assertEquals(OriginalDestinationIntent.Phase.ENTERING_REVIEW, awaitingWindowProof.phase)
        assertFalse(awaitingWindowProof.beginReturn("draft-2", 202, ownerStillFocused = true))
        assertFalse(awaitingWindowProof.reviewFocused("draft-2", 203))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, awaitingWindowProof.phase)
    }

    @Test fun originalFinishMayPrecedeOwnStartAndOwnFinishMayPrecedeReturnRequest() {
        val intent = inReview()
        assertTrue(intent.inputFinished(9, authenticatedOwnReview = true))
        assertTrue(intent.beginReturn("draft-1", 300, ownerStillFocused = true))
        assertTrue(intent.originalInputReturned(10, false))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            intent.checkReturn(original, 10, true, 350))
    }

    @Test fun twoExactFreshObservationsGrantOnlyOnePermit() {
        val intent = returning()
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            intent.checkReturn(original, 10, true, 350))
        assertTrue(intent.checkPrecommit(original, 10, true, 351))
        assertTrue(intent.consume(original, 10, true, 351))
        assertEquals(OriginalDestinationIntent.Phase.CONSUMED, intent.phase)
        assertFalse(intent.consume(original, 10, true, 352))
        intent.invalidate("LATE_EVENT")
        assertEquals(OriginalDestinationIntent.Phase.CONSUMED, intent.phase)
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            intent.checkReturn(original, 10, true, 353))
    }

    @Test fun anExplicitReturnAndTwoProofsAreRequiredBeforeConsume() {
        val intent = returning()
        assertFalse(intent.consume(original, 10, true, 350))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
        assertFalse(intent.beginReview("draft-2", 351))
        assertFalse(intent.consume(original, 10, true, 352))
    }

    @Test fun mismatchedDraftOwnerOrInputGenerationFallsBackWithoutRearming() {
        val wrongDraft = captured()
        assertTrue(wrongDraft.beginReview("draft-1", 200))
        assertFalse(wrongDraft.reviewFocused("draft-2", 201))
        assertFalse(wrongDraft.beginReview("draft-1", 202))

        val wrongOwnInput = captured()
        assertTrue(wrongOwnInput.beginReview("draft-1", 200))
        assertFalse(wrongOwnInput.reviewInputStarted(10, false))
        assertFalse(wrongOwnInput.reviewInputStarted(9, false))

        val wrongReturn = inReview()
        assertFalse(wrongReturn.beginReturn("draft-2", 300, ownerStillFocused = true))
        assertFalse(wrongReturn.beginReturn("draft-1", 301, ownerStillFocused = true))
    }

    @Test fun restartExtraAndOutOfOrderInputCallbacksAreTerminal() {
        val restartedOwn = captured()
        assertTrue(restartedOwn.beginReview("draft-1", 200))
        assertFalse(restartedOwn.reviewInputStarted(9, true))

        val extraOwn = inReview()
        assertFalse(extraOwn.reviewInputStarted(9, false))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, extraOwn.phase)

        val earlyReturn = inReview()
        assertTrue(earlyReturn.beginReturn("draft-1", 300, true))
        assertFalse(earlyReturn.originalInputReturned(10, false))

        val restartedReturn = inReview()
        assertTrue(restartedReturn.beginReturn("draft-1", 300, true))
        assertTrue(restartedReturn.inputFinished(9, false))
        assertFalse(restartedReturn.originalInputReturned(10, true))

        val extraFinish = returning()
        assertFalse(extraFinish.inputFinished(9, false))
    }

    @Test fun ownInputFinishRequiresReviewAuthenticationUnlessReturnHasBegun() {
        val intent = inReview()
        assertFalse(intent.inputFinished(9, authenticatedOwnReview = false))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
        val afterReturn = returning()
        assertEquals(OriginalDestinationIntent.Phase.RETURNING, afterReturn.phase)
    }

    @Test fun exactSnapshotRequiresIdentityContextOffsetCaretAndClearComposition() {
        val variants = listOf(
            original.copy(windowId = 9),
            original.copy(uniqueNodeId = "reused-looking-node"),
            original.copy(viewId = "other"),
            original.copy(editorFieldId = 22),
            original.copy(surroundingText = "changedafter"),
            original.copy(offset = 1),
            original.copy(selectionStart = 5, selectionEnd = 5),
            original.copy(selectionEnd = 7),
            original.copy(composing = true)
        )
        variants.forEach { changed ->
            val intent = returning()
            assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
                intent.checkReturn(changed, 10, true, 350))
            assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
        }
        val rootChanged = returning()
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            rootChanged.checkReturn(original, 10, false, 350))
    }

    @Test fun missingReturnProofWaitsOnlyForExpectedCallbacksAndCannotWaitAfterReturnedInput() {
        val intent = inReview()
        assertTrue(intent.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.WAIT,
            intent.checkReturn(null, 9, false, 301))
        assertTrue(intent.inputFinished(9, false))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.WAIT,
            intent.checkReturn(null, 9, false, 302))
        assertTrue(intent.originalInputReturned(10, false))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            intent.checkReturn(null, 10, false, 303))
    }

    @Test fun returnedTargetMutationOrLaterPrecommitMismatchIsTerminal() {
        val intent = returning()
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            intent.checkReturn(original, 10, true, 350))
        assertFalse(intent.checkPrecommit(original.copy(surroundingText = "edited"), 10, true, 351))
        assertFalse(intent.checkPrecommit(original, 10, true, 352))
        assertFalse(intent.consume(original, 10, true, 353))
    }

    @Test fun targetMutationEvenIfRestoredCannotRearmOriginalIntent() {
        val intent = inReview()
        intent.invalidate("TARGET_TEXT_CHANGED")
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
        assertFalse(intent.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            intent.checkReturn(original, 10, true, 350))
    }

    @Test fun lockOwnerLossAndTimeLimitsRejectWithoutChangingManualDraftState() {
        val lostOwner = inReview()
        assertFalse(lostOwner.beginReturn("draft-1", 300, false))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, lostOwner.phase)

        val expired = captured()
        assertFalse(expired.beginReview("draft-1", 180_101))
        assertEquals("EXPIRED", expired.lastInvalidationReason)

        val slowReturn = inReview()
        assertTrue(slowReturn.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            slowReturn.checkReturn(null, 9, false, 2_301))
        assertEquals("EXPIRED", slowReturn.lastInvalidationReason)
    }

    @Test fun unchangedNoRemovalPathStillUsesFreshReturnChecks() {
        val intent = returning()
        assertTrue(intent.returnWindowProved)
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            intent.checkReturn(original, 10, true, 350))
        assertTrue(intent.checkPrecommit(original, 10, true, 351))
        assertTrue(intent.consume(original, 10, true, 352))
    }

    @Test fun exactPairMayAttestOwnReviewBeforeInputCallbackButNeedsThatCallback() {
        val intent = captured()
        assertTrue(intent.beginReview("draft-1", 200))
        assertTrue(intent.originalRemoved(201, true, true, true))
        assertTrue(intent.reviewFocused("draft-1", 202))
        assertFalse(intent.beginReturn("draft-1", 203, true))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)

        val complete = captured()
        assertTrue(complete.beginReview("draft-1", 200))
        assertTrue(complete.originalRemoved(201, true, true, true))
        assertTrue(complete.reviewFocused("draft-1", 202))
        assertTrue(complete.inputFinished(8, false))
        assertTrue(complete.reviewInputStarted(9, false))
        assertTrue(complete.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            complete.originalAdded(301, true, OriginalDestinationIntent.AdditionEvidence.MATCH))
        assertTrue(complete.inputFinished(9, false))
        assertTrue(complete.originalInputReturned(10, false))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            complete.checkReturn(original, 10, true, 350))
        assertTrue(complete.checkPrecommit(original, 10, true, 351))
        assertTrue(complete.consume(original, 10, true, 352))
    }

    @Test fun previouslyAttestedFocusCanBeRecheckedAtRemovalBeforeInputCallback() {
        val intent = captured()
        assertTrue(intent.beginReview("draft-1", 200))
        assertTrue(intent.reviewFocused("draft-1", 201))
        assertTrue(intent.reviewFocusAttested)
        // The service rechecks the live owner/window/root/editor here and must not
        // submit a second policy focus edge for this same review Activity.
        assertTrue(intent.originalRemoved(202, true, true, true))
        assertTrue(intent.reviewInputStarted(9, false))
        assertEquals(OriginalDestinationIntent.Phase.REVIEW, intent.phase)
        assertTrue(intent.beginReturn("draft-1", 300, true))
    }

    @Test fun incompletePairCannotAuthorizeReturnOrPrecommit() {
        val intent = inReview()
        assertTrue(intent.originalRemoved(220, true, true, true))
        assertTrue(intent.beginReturn("draft-1", 300, true))
        assertTrue(intent.inputFinished(9, false))
        assertTrue(intent.originalInputReturned(10, false))
        assertFalse(intent.returnWindowProved)
        assertEquals(OriginalDestinationIntent.ReturnVerdict.WAIT,
            intent.checkReturn(original, 10, true, 350))
        assertFalse(intent.checkPrecommit(original, 10, true, 351))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
    }

    @Test fun duplicateMixedWrongPhaseAndUnattestedRemovalAreTerminal() {
        fun entering() = captured().also { assertTrue(it.beginReview("draft-1", 200)) }
        val cases = listOf<(OriginalDestinationIntent) -> Boolean>(
            { it.originalRemoved(201, false, true, true) },
            { it.originalRemoved(201, true, false, true) },
            { it.originalRemoved(201, true, true, false) }
        )
        cases.forEach { reject ->
            val intent = entering()
            assertFalse(reject(intent))
            assertFalse(intent.originalRemoved(202, true, true, true))
            assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
        }
        val duplicate = entering()
        assertTrue(duplicate.originalRemoved(201, true, true, true))
        assertFalse(duplicate.originalRemoved(202, true, true, true))
        val tooEarly = captured()
        assertFalse(tooEarly.originalRemoved(101, true, true, true))
        val tooLate = returning()
        assertFalse(tooLate.originalRemoved(350, true, true, true))
    }

    @Test fun additionRequiresPriorRemovalExplicitReturnAndOneExactEvent() {
        val noRemoval = returning()
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            noRemoval.originalAdded(350, true, OriginalDestinationIntent.AdditionEvidence.MATCH))
        val beforeReturn = inReview()
        assertTrue(beforeReturn.originalRemoved(220, true, true, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            beforeReturn.originalAdded(221, true, OriginalDestinationIntent.AdditionEvidence.MATCH))
        val mixed = inReview()
        assertTrue(mixed.originalRemoved(220, true, true, true))
        assertTrue(mixed.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            mixed.originalAdded(301, false, OriginalDestinationIntent.AdditionEvidence.MATCH))
        val duplicate = inReview()
        assertTrue(duplicate.originalRemoved(220, true, true, true))
        assertTrue(duplicate.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            duplicate.originalAdded(301, true, OriginalDestinationIntent.AdditionEvidence.MATCH))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            duplicate.originalAdded(302, true, OriginalDestinationIntent.AdditionEvidence.MATCH))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, duplicate.phase)
    }

    @Test fun pendingAdditionWaitsWithinOriginalDeadlineAndNeverTreatsMismatchAsAbsence() {
        val intent = inReview()
        assertTrue(intent.originalRemoved(220, true, true, true))
        assertTrue(intent.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.WAIT,
            intent.originalAdded(301, true, OriginalDestinationIntent.AdditionEvidence.MISSING))
        assertFalse(intent.returnWindowProved)
        assertEquals(OriginalDestinationIntent.ReturnVerdict.WAIT,
            intent.resolvePendingAddition(350, OriginalDestinationIntent.AdditionEvidence.MISSING))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.READY,
            intent.resolvePendingAddition(2_299, OriginalDestinationIntent.AdditionEvidence.MATCH))
        assertTrue(intent.returnWindowProved)

        val timedOut = inReview()
        assertTrue(timedOut.originalRemoved(220, true, true, true))
        assertTrue(timedOut.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.WAIT,
            timedOut.originalAdded(301, true, OriginalDestinationIntent.AdditionEvidence.MISSING))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            timedOut.resolvePendingAddition(2_301, OriginalDestinationIntent.AdditionEvidence.MATCH))
        assertFalse(timedOut.returnWindowProved)

        val mismatch = inReview()
        assertTrue(mismatch.originalRemoved(220, true, true, true))
        assertTrue(mismatch.beginReturn("draft-1", 300, true))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            mismatch.originalAdded(301, true, OriginalDestinationIntent.AdditionEvidence.CONTRADICTION))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            mismatch.resolvePendingAddition(302, OriginalDestinationIntent.AdditionEvidence.MATCH))
    }

}
