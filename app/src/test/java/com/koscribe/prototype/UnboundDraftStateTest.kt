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

    @Test fun editedSpeechKeepsSelectedKoreanModeUntilExplicitTranslation() {
        val state = UnboundDraftState { "speech" }
        val draft = state.beginSpeech(OutputMode.POLITE, "Arrive at ten.")!!
        assertEquals(OutputMode.POLITE, draft.mode)
        assertEquals(UnboundDraftState.Source.SPEECH, draft.source)
        assertNull(draft.result)
        assertTrue(state.edit(draft.id, "Please arrive at eleven."))
        assertEquals(UnboundDraftState.Confirm.TRANSLATING, state.confirm(draft.id))
        assertEquals("Please arrive at eleven.", state.current()?.english)
        assertEquals(UnboundDraftState.Phase.TRANSLATING, state.current()?.phase)
        assertNull(state.current()?.result)
        assertFalse(state.translationSucceeded(draft.id, 2, "열한 시에 와 주세요."))
        assertTrue(state.translationSucceeded(draft.id, 1, "열한 시에 와 주세요."))
        assertEquals("열한 시에 와 주세요.", state.current()?.result)
    }

    @Test fun actualSpeechIdenticalToSampleNeverUsesCannedKorean() {
        val state = UnboundDraftState { "speech" }
        state.beginSpeech(OutputMode.CASUAL, OutputMode.SAMPLE_ENGLISH)
        assertEquals(UnboundDraftState.Confirm.TRANSLATING, state.confirm("speech"))
        assertNull(state.current()?.result)
        assertEquals(UnboundDraftState.Phase.TRANSLATING, state.current()?.phase)
    }

    @Test fun failedTranslationNeedsExplicitRetryAndRejectsOldResult() {
        val state = UnboundDraftState { "speech" }
        state.beginSpeech(OutputMode.CASUAL, "I cannot go.")
        assertEquals(UnboundDraftState.Confirm.TRANSLATING, state.confirm("speech"))
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirm("speech"))
        assertFalse(state.edit("speech", "Do not change while processing"))
        assertTrue(state.translationFailed("speech", 1))
        assertFalse(state.translationSucceeded("speech", 1, "못 가."))
        assertTrue(state.edit("speech", "I cannot go today."))
        assertEquals(UnboundDraftState.Confirm.TRANSLATING, state.confirm("speech"))
        assertEquals(2, state.current()?.translationAttempt)
        assertFalse(state.translationSucceeded("speech", 1, "못 가."))
        assertTrue(state.translationSucceeded("speech", 2, "오늘은 못 가."))
        assertFalse(state.translationFailed("speech", 2))
        assertTrue(state.markAttempted("speech"))
        assertFalse(state.translationSucceeded("speech", 2, "중복"))
        assertEquals(UnboundDraftState.Confirm.STALE, state.confirm("speech"))
    }

    @Test fun cancelAndBoundsCannotCreateOrRearmResult() {
        val ids = ArrayDeque(listOf("old", "new"))
        val state = UnboundDraftState { ids.removeFirst() }
        state.beginSpeech(OutputMode.POLITE, "Hello")
        assertTrue(state.edit("old", "x".repeat(513)))
        assertEquals(UnboundDraftState.Confirm.TOO_LONG, state.confirm("old"))
        assertNull(state.current()?.result)
        assertTrue(state.edit("old", "Hello"))
        assertEquals(UnboundDraftState.Confirm.TRANSLATING, state.confirm("old"))
        assertFalse(state.translationSucceeded("old", 1, "가".repeat(513)))
        assertFalse(state.translationSucceeded("old", 1, "첫째\u2028둘째"))
        assertTrue(state.end("old"))
        state.beginSpeech(OutputMode.POLITE, "Fresh")
        assertFalse(state.translationSucceeded("old", 1, "안녕하세요."))
        assertEquals(UnboundDraftState.Phase.REVIEW, state.current()?.phase)
        assertNull(state.current()?.result)
    }
}
