package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class SessionGuardTest {
    private val editor = EditorSnapshot(
        "chat.app", 7, "node-42", "chat.app:id/message", "EditText", 21, 1,
        "beforeafter", 0, 6, 6, false
    )

    @Test fun oneCommitOnlyEvenIfOutcomeUnknown() {
        val guard = SessionGuard()
        val session = guard.begin(OutputMode.ENGLISH, editor, 100)!!
        assertTrue(guard.publish(session.id, "hello"))
        assertEquals(GuardDecision.Ready, guard.reserveCommit(session.id, editor, 200))
        assertEquals(GuardDecision.Refused(GuardFailure.ALREADY_ATTEMPTED), guard.reserveCommit(session.id, editor, 201))
    }

    @Test fun cancelAndSupersedeRejectOldResults() {
        val guard = SessionGuard()
        val first = guard.begin(OutputMode.POLITE, editor, 100)!!
        guard.cancel(first.id)
        assertFalse(guard.publish(first.id, "old"))
        val second = guard.begin(OutputMode.CASUAL, editor, 200)!!
        guard.cancel(first.id) // Late cleanup from the old review must not cancel the new one.
        assertFalse(guard.publish(first.id, "late"))
        assertEquals(GuardDecision.Refused(GuardFailure.SUPERSEDED), guard.check(first.id, editor, 201))
        assertTrue(guard.publish(second.id, "new"))
        assertEquals(GuardDecision.Ready, guard.check(second.id, editor, 201))
    }

    @Test fun targetSelectionContextAndNavigationChangesRefuse() {
        val guard = SessionGuard()
        val session = guard.begin(OutputMode.ENGLISH, editor, 100)!!
        guard.publish(session.id, "hello")
        assertEquals(GuardDecision.Refused(GuardFailure.TARGET_CHANGED), guard.check(session.id, editor.copy(uniqueNodeId = "other"), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.TARGET_CHANGED), guard.check(session.id, editor.copy(windowId = 8), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.SELECTION_CHANGED), guard.check(session.id, editor.copy(selectionEnd = 7), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.CONTEXT_CHANGED), guard.check(session.id, editor.copy(surroundingText = "BEFOREafter"), 200))
    }

    @Test fun missingIdentityCompositionInvalidationAndExpiryRefuse() {
        val guard = SessionGuard()
        assertNull(guard.begin(OutputMode.ENGLISH, editor.copy(uniqueNodeId = ""), 100))
        assertNull(guard.begin(OutputMode.ENGLISH, editor.copy(composing = true), 100))
        val session = guard.begin(OutputMode.ENGLISH, editor, 100)!!
        guard.publish(session.id, "hello")
        assertEquals(GuardDecision.Refused(GuardFailure.COMPOSING), guard.check(session.id, editor.copy(composing = true), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.EXPIRED), guard.check(session.id, editor, 180_101))
        guard.invalidate(session.id)
        assertEquals(GuardDecision.Refused(GuardFailure.TARGET_CHANGED), guard.check(session.id, editor, 201))
    }
}
