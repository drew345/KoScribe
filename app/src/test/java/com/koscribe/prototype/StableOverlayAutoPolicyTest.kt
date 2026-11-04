package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class StableOverlayAutoPolicyTest {
    private val field = EditorSnapshot(StableFixtureField.PACKAGE, 5, "source-node",
        StableFixtureField.ID, "EditText", 7, 1, "left right", 0, 4, 4, false)

    @Test fun bothNodeAndServedFieldMustIdentifyTheStableView() {
        assertTrue(StableFixtureField.eligible(field, StableFixtureField.ID))
        assertFalse(StableFixtureField.eligible(field.copy(viewId = "alternate"), StableFixtureField.ID))
        assertFalse(StableFixtureField.eligible(field, "alternate"))
        assertFalse(StableFixtureField.eligible(field, null))
        assertFalse(StableFixtureField.eligible(field.copy(editorFieldId = 0), StableFixtureField.ID))
        assertFalse(StableFixtureField.eligible(field.copy(packageName = "other"), StableFixtureField.ID))
    }
    @Test fun caretDuringReviewMayBeUnavailableButKnownMismatchRefuses() {
        assertFalse(StableFixtureField.caretContradicts(-1, -1, 4))
        assertFalse(StableFixtureField.caretContradicts(4, 4, 4))
        assertTrue(StableFixtureField.caretContradicts(-1, 4, 4))
        assertTrue(StableFixtureField.caretContradicts(4, 5, 4))
        assertTrue(StableFixtureField.caretContradicts(5, 5, 4))
    }
    private fun begin() = OriginalDestinationIntent.capture(field, 8, 0, OutputMode.POLITE)!!.also {
        assertTrue(it.beginReview("draft", 1))
    }
    @Test fun overlayCannotAcquireReviewWithoutOriginalFinish() {
        val intent = begin()
        assertFalse(intent.reviewInputStarted(9, false, requireOriginalFinish = true))
        assertFalse(intent.inputFinished(8, false))
        assertEquals(OriginalDestinationIntent.Phase.FALLBACK, intent.phase)
    }
    @Test fun returnedContextChangeIsTerminalEvenIfRestoredBeforeCommit() {
        val intent = begin()
        assertTrue(intent.inputFinished(8, false))
        assertTrue(intent.reviewInputStarted(9, false, requireOriginalFinish = true))
        assertTrue(intent.reviewFocused("draft", 2))
        assertTrue(intent.beginReturn("draft", 3, true))
        assertTrue(intent.inputFinished(9, false))
        assertTrue(intent.originalInputReturned(10, false))
        assertEquals(OriginalDestinationIntent.ReturnVerdict.FALLBACK,
            intent.checkReturn(field.copy(surroundingText = "left other"), 10, true, 4))
        assertFalse(intent.checkPrecommit(field, 10, true, 5))
        assertFalse(intent.consume(field, 10, true, 6))
    }
    @Test fun editedFixedSampleNeedsExplicitSampleApproval() {
        val state = UnboundDraftState { "id" }
        val draft = state.begin(OutputMode.POLITE)!!
        assertTrue(state.edit(draft.id, "Edited English"))
        assertEquals(UnboundDraftState.Confirm.EDITED_KOREAN_SAMPLE, state.confirm(draft.id))
        assertNull(state.current()?.result)
        assertEquals(UnboundDraftState.Confirm.READY, state.confirmFixedOverlaySample(draft.id))
        assertEquals(OutputMode.POLITE.sampleOutput, state.current()?.result)
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirmFixedOverlaySample(draft.id))
    }
    @Test fun fixedSampleCannotApproveSpeechOrEmptyEnglish() {
        val state = UnboundDraftState { "id" }
        val speech = state.beginSpeech(OutputMode.POLITE, "Real transcript")!!
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirmFixedOverlaySample(speech.id))
        state.endActive()
        val sample = state.begin(OutputMode.CASUAL)!!
        state.edit(sample.id, " ")
        assertEquals(UnboundDraftState.Confirm.EMPTY, state.confirmFixedOverlaySample(sample.id))
        assertNull(state.current()?.result)
    }
}
