package com.koscribe.prototype

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StablePrecommitTraceTest {
    private fun trace() = StablePrecommitTrace(50, 100, 120, 135, 140)

    @Test fun eventStampIsRelativeToReturnReadGuardAndPublicationNotToReceipt() {
        val trace = trace()
        listOf(49L, 50L, 99L, 110L, 125L, 137L, 141L).forEach {
            trace.invalidated(StablePrecommitTrace.Kind.WINDOWS,
                StablePrecommitTrace.Role.CURRENT_DEFAULT_IME, 16, it, 180)
        }
        val summary = trace.summary()
        listOf("BEFORE_RETURN", "RETURN_TICK", "BEFORE_READ", "DURING_READ",
            "BETWEEN_READ_GUARD", "BETWEEN_GUARD_ARM", "AFTER_ARM").forEach {
            assertTrue(summary, summary.contains("WINDOWS_WC16_CURRENT_DEFAULT_IME_${it}_RECEIVED_LT50"))
        }
    }

    @Test fun coincidentTicksAndUnavailableEventTimeStayExplicit() {
        val trace = StablePrecommitTrace(null, 100, 105, 105, 105)
        listOf(105L, 0L, 112L).forEach {
            trace.invalidated(StablePrecommitTrace.Kind.TARGET_CONTENT,
                StablePrecommitTrace.Role.ORIGINAL, 1, it, 110)
        }
        trace.invalidated(StablePrecommitTrace.Kind.INPUT_FINISH,
            StablePrecommitTrace.Role.UNKNOWN, null, null, 110)
        val summary = trace.summary()
        assertTrue(summary, summary.contains("TARGET_CONTENT_CT1_ORIGINAL_AT_TICK_READ_END_GUARD_START_ARM"))
        assertTrue(summary, summary.contains("UNKNOWN_STAMP_RECEIVED_LT16_DELIVERY_UNKNOWN"))
        assertTrue(summary, summary.contains("FUTURE_STAMP_RECEIVED_LT16_DELIVERY_UNKNOWN"))
        assertTrue(summary, summary.contains("INPUT_FINISH_UNKNOWN_NO_EVENT_STAMP_HANDLED_LT16_DELIVERY_UNKNOWN"))
    }

    @Test fun capsOrderedPrefixRetainsLaterFamilyAndFreezesFirstOutcome() {
        val trace = trace()
        repeat(8) {
            trace.invalidated(StablePrecommitTrace.Kind.VIEW_FOCUS,
                StablePrecommitTrace.Role.ORIGINAL, null, 140, 150)
        }
        trace.invalidated(StablePrecommitTrace.Kind.TARGET_TEXT,
            StablePrecommitTrace.Role.ORIGINAL, null, 145, 150)
        trace.finish("RESERVE_TARGET_CHANGED")
        trace.finish("ADAPTER_DISARMED_USER_CANCELLED")
        trace.invalidated(StablePrecommitTrace.Kind.INPUT_START,
            StablePrecommitTrace.Role.UNKNOWN, null, null, 151)
        val summary = trace.summary()
        assertTrue(summary, summary.contains("outcome=RESERVE_TARGET_CHANGED count=9 truncated=true"))
        assertTrue(summary, summary.contains("families=VIEW_FOCUS+TARGET_TEXT"))
        assertFalse(summary, summary.contains("INPUT_START"))
        assertFalse(summary, summary.contains("TARGET_TEXT_ORIGINAL"))
    }
}
