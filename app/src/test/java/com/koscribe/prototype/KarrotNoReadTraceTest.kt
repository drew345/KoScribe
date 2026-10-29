package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarrotNoReadTraceTest {
    @Test fun firstInterruptionPreservesItsPhaseAndCause() {
        val trace = KarrotNoReadTrace()
        trace.advance(KarrotNoReadTrace.Phase.FIRST_YIELD)
        assertTrue(trace.finish(KarrotNoReadTrace.Source.INPUT_SELECTION))
        trace.advance(KarrotNoReadTrace.Phase.SECOND_YIELD_WOULD_READ)
        assertFalse(trace.finish(KarrotNoReadTrace.Source.TIMEOUT))
        assertEquals("INTERRUPTED SOURCE=INPUT_SELECTION PHASE=FIRST_YIELD", trace.code())
    }

    @Test fun successfulDryRunReportsTheWouldReadBoundary() {
        val trace = KarrotNoReadTrace()
        trace.advance(KarrotNoReadTrace.Phase.PREFLIGHT)
        trace.advance(KarrotNoReadTrace.Phase.FIRST_YIELD)
        trace.advance(KarrotNoReadTrace.Phase.CONTINUITY)
        trace.advance(KarrotNoReadTrace.Phase.SECOND_YIELD_WOULD_READ)
        assertTrue(trace.finish(KarrotNoReadTrace.Source.NONE))
        assertEquals("WOULD_READ SOURCE=NONE PHASE=SECOND_YIELD_WOULD_READ", trace.code())
    }

    @Test fun preflightFailureIsCategorical() {
        val trace = KarrotNoReadTrace()
        trace.advance(KarrotNoReadTrace.Phase.PREFLIGHT)
        assertTrue(trace.finish(KarrotNoReadTrace.Source.PREFLIGHT_FAILURE))
        assertEquals("REFUSED SOURCE=PREFLIGHT_FAILURE PHASE=PREFLIGHT", trace.code())
    }
}
