package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class KarrotEmptyReadTraceTest {
    @Test fun finalCheckExceptionHasItsOwnCauseNotAHandlerPostFailure() {
        assertEquals(KarrotEmptyReadTrace.Source.FINAL_CHECK_FAILURE,
            KarrotEmptyReadTrace.finalSource("FINAL_CHECK_UNAVAILABLE"))
        assertEquals(KarrotEmptyReadTrace.Source.CONTINUITY_FAILURE,
            KarrotEmptyReadTrace.finalSource("ROOT_CHANGED"))
        assertEquals(KarrotEmptyReadTrace.Source.NONE,
            KarrotEmptyReadTrace.finalSource("REPORTED_EMPTY_AT_ZERO CONTINUITY_STABLE_NOT_INSERT_ELIGIBILITY"))
    }

    @Test fun interruptionBeforeWorkerKeepsCallNotStartedAndFirstCause() {
        val trace = KarrotEmptyReadTrace()
        trace.advance(KarrotEmptyReadTrace.Phase.FIRST_YIELD)
        assertTrue(trace.finish("INTERRUPTED", KarrotEmptyReadTrace.Source.INPUT_SELECTION))
        trace.advance(KarrotEmptyReadTrace.Phase.CONTENT_CALL)
        assertFalse(trace.finish("TIMEOUT", KarrotEmptyReadTrace.Source.TIMEOUT))
        assertEquals(
            "INTERRUPTED SOURCE=INPUT_SELECTION PHASE=FIRST_YIELD CALL=CALL_NOT_STARTED",
            trace.code())
    }

    @Test fun callReturnAfterInterruptionIsRetainedWithoutChangingTerminalCause() {
        val trace = KarrotEmptyReadTrace()
        trace.advance(KarrotEmptyReadTrace.Phase.CONTENT_CALL)
        trace.callAttempted()
        assertTrue(trace.finish("INTERRUPTED", KarrotEmptyReadTrace.Source.WINDOWS_CHANGED))
        trace.callReturned()
        trace.advance(KarrotEmptyReadTrace.Phase.FINAL_CHECK)
        assertEquals(
            "INTERRUPTED SOURCE=WINDOWS_CHANGED PHASE=CONTENT_CALL CALL=CALL_RETURNED",
            trace.code())
        assertFalse(trace.isLive())
    }

    @Test fun thrownCallRetainsFirstTerminalReason() {
        val trace = KarrotEmptyReadTrace()
        trace.advance(KarrotEmptyReadTrace.Phase.CONTENT_CALL)
        trace.callAttempted()
        assertTrue(trace.finish("TIMEOUT", KarrotEmptyReadTrace.Source.TIMEOUT))
        trace.callThrown()
        trace.callReturned()
        assertEquals("TIMEOUT SOURCE=TIMEOUT PHASE=CONTENT_CALL CALL=CALL_THROWN", trace.code())
    }

    @Test fun interruptedWorkerCanRecordALaterLocalAttemptWithoutRearming() {
        val trace = KarrotEmptyReadTrace()
        trace.advance(KarrotEmptyReadTrace.Phase.WORKER_PRE_READ)
        assertTrue(trace.finish("INTERRUPTED", KarrotEmptyReadTrace.Source.SCREEN_OFF))
        trace.callAttempted()
        assertEquals(
            "INTERRUPTED SOURCE=SCREEN_OFF PHASE=WORKER_PRE_READ CALL=CALL_ATTEMPTED",
            trace.code())
        assertFalse(trace.isLive())
    }

    @Test fun concurrentTerminalAndCallCompletionNeverReopenRequest() {
        val trace = KarrotEmptyReadTrace()
        trace.advance(KarrotEmptyReadTrace.Phase.CONTENT_CALL)
        trace.callAttempted()
        val gate = CountDownLatch(1)
        val done = CountDownLatch(2)
        val callbackWon = AtomicBoolean(false)
        val callback = Thread {
            gate.await()
            callbackWon.set(trace.finish("INTERRUPTED", KarrotEmptyReadTrace.Source.INPUT_FINISH))
            done.countDown()
        }
        val worker = Thread {
            gate.await()
            trace.callReturned()
            done.countDown()
        }
        callback.start()
        worker.start()
        gate.countDown()
        assertTrue(done.await(2, TimeUnit.SECONDS))
        callback.join()
        worker.join()
        assertTrue(callbackWon.get())
        assertEquals("INTERRUPTED SOURCE=INPUT_FINISH PHASE=CONTENT_CALL CALL=CALL_RETURNED",
            trace.code())
        assertFalse(trace.finish("REPORTED_EMPTY_AT_ZERO", KarrotEmptyReadTrace.Source.NONE))
    }
}
