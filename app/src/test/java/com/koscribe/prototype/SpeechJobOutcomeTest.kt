package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SpeechJobOutcomeTest {
    @Test fun cancellationAndTimeoutBothPreventLateDelivery() {
        val cancelled = SpeechJobOutcome()
        assertTrue(cancelled.cancel())
        assertFalse(cancelled.deliver())
        assertFalse(cancelled.timeOut())
        val timedOut = SpeechJobOutcome()
        assertTrue(timedOut.timeOut())
        assertFalse(timedOut.deliver())
        assertFalse(timedOut.cancel())
    }

    @Test fun concurrentTerminalClaimsHaveExactlyOneWinner() {
        repeat(10) {
            val state = SpeechJobOutcome()
            val start = CountDownLatch(1)
            val done = CountDownLatch(3)
            val winners = AtomicInteger()
            listOf(state::cancel, state::timeOut, state::deliver).forEach { claim ->
                Thread {
                    start.await()
                    if (claim()) winners.incrementAndGet()
                    done.countDown()
                }.start()
            }
            start.countDown()
            assertTrue(done.await(2, TimeUnit.SECONDS))
            assertEquals(1, winners.get())
            assertFalse(state.isActive())
        }
    }
}
