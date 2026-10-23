package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnboundRecordingStateTest {
    @Test fun stopRequiresProvedRecordingAndCannotRepeat() {
        val owner = UnboundRecordingState { "one" }
        assertEquals("one", owner.begin(OutputMode.POLITE)?.id)
        assertNull(owner.begin(OutputMode.ENGLISH))
        assertFalse(owner.microphoneStoppedForReview("one"))
        assertTrue(owner.microphoneProved("one"))
        assertFalse(owner.microphoneProved("one"))
        assertTrue(owner.microphoneStoppedForReview("one"))
        assertFalse(owner.microphoneStoppedForReview("one"))
        assertEquals(UnboundRecordingState.Phase.READ_ONLY_REVIEW, owner.current()?.phase)
    }

    @Test fun staleCallbacksCannotTakeOverOrCancelLaterRecording() {
        val ids = ArrayDeque(listOf("old", "new"))
        val owner = UnboundRecordingState { ids.removeFirst() }
        owner.begin(OutputMode.ENGLISH)
        assertTrue(owner.end("old"))
        owner.begin(OutputMode.CASUAL)
        assertFalse(owner.microphoneProved("old"))
        assertFalse(owner.microphoneStoppedForReview("old"))
        assertFalse(owner.end("old"))
        assertEquals("new", owner.current()?.id)
        assertTrue(owner.microphoneProved("new"))
        owner.endActive()
        assertNull(owner.current())
        assertFalse(owner.microphoneStoppedForReview("new"))
    }
}
