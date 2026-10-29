package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarrotEmptyReadPolicyTest {
    @Test fun preReadSelectionAllowsOnlyUnknownPairOrCollapsedZero() {
        assertTrue(KarrotEmptyReadPolicy.permitsPreReadSelection(-1, -1))
        assertTrue(KarrotEmptyReadPolicy.permitsPreReadSelection(0, 0))
        assertFalse(KarrotEmptyReadPolicy.permitsPreReadSelection(17, 17))
        assertFalse(KarrotEmptyReadPolicy.permitsPreReadSelection(-1, 0))
        assertFalse(KarrotEmptyReadPolicy.permitsPreReadSelection(0, 1))
    }

    @Test fun emptyPayloadNeedsKnownZeroOffsetAndCollapsedZeroSelection() {
        val classify = KarrotEmptyReadPolicy::classifyEmptyResult
        assertEquals(KarrotEmptyReadPolicy.Result.REPORTED_EMPTY_AT_ZERO, classify(0, 0, 0))
        assertEquals(KarrotEmptyReadPolicy.Result.OFFSET_UNKNOWN, classify(-1, 0, 0))
        assertEquals(KarrotEmptyReadPolicy.Result.OFFSET_NOT_ZERO, classify(4, 0, 0))
        assertEquals(KarrotEmptyReadPolicy.Result.SELECTION_INVALID, classify(0, 0, 1))
        assertEquals(KarrotEmptyReadPolicy.Result.SELECTION_INVALID, classify(0, -1, -1))
        assertEquals(KarrotEmptyReadPolicy.Result.SELECTION_INVALID, classify(0, 1, 1))
    }

    @Test fun nonemptyPayloadSkipsAllFurtherInspection() {
        assertEquals(KarrotEmptyReadPolicy.Result.NONEMPTY_REJECTED,
            KarrotEmptyReadPolicy.classifyPayloadLength(1) {
                throw AssertionError("Nonempty payload must be discarded without metadata reads")
            })
        assertEquals(KarrotEmptyReadPolicy.Result.NONEMPTY_REJECTED,
            KarrotEmptyReadPolicy.classifyPayloadLength(Int.MAX_VALUE) {
                throw AssertionError("Even a large selected payload must skip metadata reads")
            })
        assertEquals(KarrotEmptyReadPolicy.Result.REPORTED_EMPTY_AT_ZERO,
            KarrotEmptyReadPolicy.classifyPayloadLength(0) {
                KarrotEmptyReadPolicy.classifyEmptyResult(0, 0, 0)
            })
    }
}
