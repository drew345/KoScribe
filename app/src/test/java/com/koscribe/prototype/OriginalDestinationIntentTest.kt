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
}
