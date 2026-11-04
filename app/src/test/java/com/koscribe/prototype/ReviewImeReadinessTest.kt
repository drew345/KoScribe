package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewImeReadinessTest {
    private val ready = ReviewImeReadiness.Evidence.READY
    private val gap = ReviewImeReadiness.Evidence.MEASURED_ENTRY_GAP
    private val changed = ReviewImeReadiness.Evidence.CONTRADICTION

    @Test fun onlyAuthenticatedEntryMayStartWaiting() {
        val ordinary = ReviewImeReadiness(100L)
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK, ordinary.check(gap, 150L))
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK, ordinary.check(ready, 160L))

        val entry = ReviewImeReadiness(100L)
        assertEquals(ReviewImeReadiness.Verdict.WAIT, entry.check(gap, 150L, true))
        assertTrue(entry.pending)
        assertEquals(ReviewImeReadiness.Verdict.READY, entry.check(ready, 250L))
        assertFalse(entry.pending)
    }

    @Test fun repeatedCallbacksCannotExtendLaunchDeadlineOrAcceptLateReadiness() {
        val gate = ReviewImeReadiness(100L)
        assertEquals(ReviewImeReadiness.Verdict.WAIT, gate.check(gap, 900L, true))
        assertEquals(ReviewImeReadiness.Verdict.WAIT, gate.check(gap, 1_099L))
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK, gate.check(ready, 1_100L))
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK, gate.check(ready, 1_101L))
    }

    @Test fun contradictionAndRegressedReadinessAreTerminal() {
        val changedWhileWaiting = ReviewImeReadiness(100L)
        changedWhileWaiting.check(gap, 150L, true)
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK,
            changedWhileWaiting.check(changed, 160L))
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK,
            changedWhileWaiting.check(ready, 170L))

        val regressed = ReviewImeReadiness(100L)
        assertEquals(ReviewImeReadiness.Verdict.READY, regressed.check(ready, 150L))
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK, regressed.check(gap, 160L, true))
    }

    @Test fun completedReadinessSurvivesTheStartupDeadlineButNotClockRegression() {
        val gate = ReviewImeReadiness(100L)
        assertEquals(ReviewImeReadiness.Verdict.READY, gate.check(ready, 150L))
        assertEquals(ReviewImeReadiness.Verdict.READY, gate.check(ready, 5_000L))
        assertEquals(ReviewImeReadiness.Verdict.FALLBACK, gate.check(ready, 99L))
    }
}
