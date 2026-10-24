package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnboundDraftStateTest {
    @Test fun englishEditsBecomeTheResultOnlyAfterConfirmation() {
        val state = UnboundDraftState { "draft" }
        assertEquals("draft", state.begin(OutputMode.ENGLISH)?.id)
        assertNull(state.begin(OutputMode.POLITE))
        assertTrue(state.edit("draft", "Edited English"))
        assertEquals(UnboundDraftState.Confirm.READY, state.confirm("draft"))
        assertEquals("Edited English", state.current()?.result)
        assertFalse(state.edit("draft", "later edit"))
        assertTrue(state.markAttempted("draft"))
        assertFalse(state.markAttempted("draft"))
        assertEquals(UnboundDraftState.Phase.UNCERTAIN, state.current()?.phase)
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirm("draft"))
    }

    @Test fun fixedKoreanSampleRejectsEditsAndStaleCallbacks() {
        val state = UnboundDraftState { "draft" }
        state.begin(OutputMode.POLITE)
        assertTrue(state.edit("draft", "Changed"))
        assertEquals(UnboundDraftState.Confirm.EDITED_KOREAN_SAMPLE, state.confirm("draft"))
        assertNull(state.current()?.result)
        assertFalse(state.edit("stale", OutputMode.SAMPLE_ENGLISH))
        assertTrue(state.edit("draft", OutputMode.SAMPLE_ENGLISH))
        assertEquals(UnboundDraftState.Confirm.READY, state.confirm("draft"))
        assertEquals(OutputMode.POLITE.sampleOutput, state.current()?.result)
        assertTrue(state.end("draft"))
        assertFalse(state.end("draft"))
    }

    @Test fun consumedOrOlderDraftCannotBeEditedOrConfirmed() {
        val ids = ArrayDeque(listOf("old", "new"))
        val state = UnboundDraftState { ids.removeFirst() }
        state.begin(OutputMode.ENGLISH)
        assertTrue(state.end("old"))
        state.begin(OutputMode.ENGLISH)
        assertFalse(state.edit("old", "stale"))
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirm("old"))
        assertEquals(UnboundDraftState.Confirm.READY, state.confirm("new"))
        assertTrue(state.markAttempted("new"))
        assertFalse(state.edit("new", "retry"))
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirm("new"))
    }
}
