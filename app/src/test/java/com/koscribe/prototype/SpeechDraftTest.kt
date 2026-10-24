package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class SpeechDraftTest {
    @Test fun realEnglishEntersEditableReviewAndConfirmUsesTheEdit() {
        val state = UnboundDraftState { "speech" }
        val review = state.beginSpeech("We should meet at six.")!!
        assertEquals(OutputMode.ENGLISH, review.mode)
        assertEquals(UnboundDraftState.Phase.REVIEW, review.phase)
        assertTrue(state.edit(review.id, "We should meet at seven."))
        assertEquals(UnboundDraftState.Confirm.READY, state.confirm(review.id))
        assertEquals("We should meet at seven.", state.current()?.result)
        assertFalse(state.edit(review.id, "stale edit"))
    }

    @Test fun unusableTranscriptNeverCreatesDraft() {
        val state = UnboundDraftState { "speech" }
        assertNull(state.beginSpeech("  "))
        assertNull(state.beginSpeech("x".repeat(513)))
        assertNull(state.current())
    }
}
