package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class StableOverlayInputTraceTest {
    @Test fun preservesDecidingPhaseAndRoleAfterCleanup() {
        val trace = StableOverlayInputTrace(100)
        trace.mark("LAUNCH", "ENTERING_REVIEW", 200)
        trace.mark("FINISH_TARGET_EXACT_G0_ACCEPTED", "ENTERING_REVIEW", 220)
        trace.mark("START_OWN_EXACT_G1_NEW", "ENTERING_REVIEW", 221)
        trace.refuse("INPUT_UNEXPECTED_FINISH_OWN_EXACT_G1", "ENTERING_REVIEW", 250)
        trace.refuse("USER_CANCELLED", "FALLBACK", 300)
        trace.mark("WINDOW_FOCUS_LOSS", "FALLBACK", 320)
        val summary = trace.summary()
        assertTrue(summary.contains("first=INPUT_UNEXPECTED_FINISH_OWN_EXACT_G1_ENTERING_REVIEW_CLT500_LLT150"))
        assertFalse(summary.contains("USER_CANCELLED")); assertFalse(summary.contains("WINDOW_FOCUS_LOSS"))
    }

    @Test fun onlyFirstSubsequentStartWithinFixedHalfSecondIsRecorded() {
        val trace = StableOverlayInputTrace(0)
        trace.refuse("INPUT_UNEXPECTED_FINISH", "REVIEW", 100)
        trace.followingStart("TOO_EARLY", "FALLBACK", 99)
        trace.followingStart("START_OWN_EXACT_G2_NEW", "FALLBACK", 599)
        trace.followingStart("SECOND", "FALLBACK", 599)
        assertTrue(trace.summary().contains("nextStart=START_OWN_EXACT_G2_NEW"))
        assertFalse(trace.summary().contains("SECOND"))
        val late = StableOverlayInputTrace(0)
        late.refuse("INPUT_UNEXPECTED_FINISH", "REVIEW", 100)
        late.followingStart("TOO_LATE", "FALLBACK", 600)
        assertTrue(late.summary().endsWith("nextStart=NONE"))
    }

    @Test fun fullPrefixCannotEvictFirstRefusal() {
        val trace = StableOverlayInputTrace(0)
        repeat(30) { trace.mark("WINDOW_FOCUS_GAIN", "ENTERING_REVIEW", it.toLong()) }
        trace.refuse("INPUT_UNEXPECTED_FINISH_OWN_EXACT_G1", "REVIEW", 80)
        assertTrue(trace.summary().contains("truncated=true"))
        assertTrue(trace.summary().contains("first=INPUT_UNEXPECTED_FINISH_OWN_EXACT_G1_REVIEW"))
        assertEquals(15, Regex("WINDOW_FOCUS_GAIN").findAll(trace.summary()).count())
    }
}
