package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class ReviewSurfaceProbePolicyTest {
    private val original = EditorSnapshot("com.koscribe.smoke", 7, "node", "editor",
        "EditText", 12, 1, "prefix suffix", 0, 7, 7, false)
    private fun compare(next: EditorSnapshot? = original, sameNode: Boolean = true,
        sameRoot: Boolean = true, generation: Int? = 3) =
        ReviewSurfaceProbePolicy.compare(original, next, sameNode, sameRoot, 1, generation)

    @Test fun identicalFieldContentDoesNotConferIdentity() {
        val result = compare(sameNode = false)
        assertEquals("MISMATCH", result.identity)
        assertEquals("MATCH", result.context)
        assertEquals("MATCH", result.caret)
    }
    @Test fun exactSameViewCannotProveLogicalConversation() {
        assertEquals("MATCH", compare().identity)
        assertTrue(compare().code("TARGET").endsWith("semantic=UNKNOWN"))
    }
    @Test fun unavailableOrIncoherentSampleStaysUnknown() {
        assertEquals(ReviewSurfaceProbePolicy.Comparison(), compare(next = null))
        assertEquals(ReviewSurfaceProbePolicy.Comparison(), compare(generation = null))
    }
    @Test fun changedRootAndAvailableMetadataAreNotIdentityMatches() {
        assertEquals("MISMATCH", compare(sameRoot = false).identity)
        assertEquals("MISMATCH", compare(original.copy(viewId = "other")).identity)
        assertEquals("MISMATCH", compare(original.copy(uniqueNodeId = "other")).identity)
        assertEquals("MISMATCH", compare(original.copy(editorFieldId = 13)).identity)
    }
    @Test fun contextCaretAndFreshSessionAreSeparateEvidence() {
        assertEquals("FRESH", compare().generation)
        assertEquals("SAME", compare(generation = 1).generation)
        assertEquals("UNKNOWN", compare(generation = 0).generation)
        assertEquals("MISMATCH", compare(original.copy(selectionStart = 8, selectionEnd = 8)).caret)
        assertEquals("MISMATCH", compare(original.copy(surroundingText = "other suffix")).context)
    }
    @Test fun editsAndFocusDoNotRenewReviewDeadline() {
        val p = ReviewSurfaceProbePolicy(10)
        p.focused(20); p.edited(119_999)
        assertFalse(p.tick(120_010))
        assertEquals(ReviewSurfaceProbePolicy.State.CANCELLED, p.state)
        p.focused(120_011); p.edited(120_012)
        assertEquals(1, p.edits)
        assertFalse(p.dismiss(120_013))
    }
    @Test fun returnDeadlineIsFixedAndCannotRearmOrEdit() {
        val p = ReviewSurfaceProbePolicy(0)
        p.focused(1); assertTrue(p.dismiss(2))
        assertFalse(p.dismiss(100))
        p.edited(200)
        assertEquals(0, p.edits)
        assertFalse(p.tick(2002))
        assertEquals(ReviewSurfaceProbePolicy.State.COMPLETE, p.state)
        assertFalse(p.tick(2003))
    }
    @Test fun regressionDuringReturnIsRefusedNotCompleted() {
        val p = ReviewSurfaceProbePolicy(0)
        p.focused(10); p.dismiss(20)
        assertFalse(p.tick(19))
        assertEquals(ReviewSurfaceProbePolicy.State.REFUSED, p.state)
    }
    @Test fun staleCallbacksCannotRestoreCancelledSession() {
        val p = ReviewSurfaceProbePolicy(0)
        p.cancel()
        p.focused(1); p.edited(2)
        assertFalse(p.dismiss(3))
        assertEquals(ReviewSurfaceProbePolicy.State.CANCELLED, p.state)
        assertEquals(0, p.edits)
    }
}
