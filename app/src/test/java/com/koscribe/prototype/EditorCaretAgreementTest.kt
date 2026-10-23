package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorCaretAgreementTest {
    private fun matches(
        nodeStart: Int = -1,
        nodeEnd: Int = -1,
        offset: Int = 0,
        start: Int = 0,
        end: Int = 0,
        textEmpty: Boolean = true,
        nodeEmptyOrHint: Boolean = true
    ) = EditorCaretAgreement.matches(
        nodeStart, nodeEnd, offset, start, end, textEmpty, nodeEmptyOrHint
    )

    @Test fun acceptsOnlyVerifiedEmptyUnknownNodeOrOrdinaryMatchingCaret() {
        assertTrue(matches()) // Empty node with no displayed text or with a hint.
        assertTrue(matches(nodeStart = 13, nodeEnd = 13, offset = 10, start = 3,
            end = 3, textEmpty = false, nodeEmptyOrHint = false))
    }

    @Test fun unknownNodeCaretNeedsEveryEmptyEditorFact() {
        assertFalse(matches(nodeStart = -1, nodeEnd = 0))
        assertFalse(matches(nodeStart = 0, nodeEnd = -1))
        assertFalse(matches(textEmpty = false))
        assertFalse(matches(offset = 1))
        assertFalse(matches(start = 1, end = 1))
        assertFalse(matches(nodeEmptyOrHint = false))
        assertFalse(matches(start = 0, end = 1))
        assertFalse(matches(offset = -1))
        assertFalse(matches(offset = Int.MAX_VALUE, start = 1, end = 1,
            textEmpty = false, nodeEmptyOrHint = false))
    }

    @Test fun emptyCaptureStillRejectsChangedTargetAndCancelledSession() {
        val empty = EditorSnapshot("chat.app", 7, "node", "chat.app:id/editor",
            "EditText", 21, 1, "", 0, 0, 0, false)
        val guard = SessionGuard()
        val session = guard.begin(OutputMode.ENGLISH, empty, 100)!!
        assertTrue(guard.publish(session.id, "sample"))
        assertEquals(GuardDecision.Ready, guard.check(session.id, empty, 200))
        assertEquals(GuardDecision.Refused(GuardFailure.CONTEXT_CHANGED),
            guard.check(session.id, empty.copy(surroundingText = "draft"), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.TARGET_CHANGED),
            guard.check(session.id, empty.copy(windowId = 8), 200))
        guard.cancel(session.id)
        assertEquals(GuardDecision.Refused(GuardFailure.CANCELLED),
            guard.reserveCommit(session.id, empty, 201))
    }
}
