package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class SpeechTransferGateTest {
    @Test fun cancelledUploadNeverPublishesOrRearmsTheOldSession() {
        val gate = SpeechTransferGate()
        assertTrue(gate.arm("first"))
        assertTrue(gate.stoppedAndReleased("first"))
        gate.cancel("first")
        assertFalse(gate.accept("first"))
        assertTrue(gate.arm("second"))
        assertFalse(gate.accept("first"))
        assertFalse(gate.stoppedAndReleased("first"))
        assertTrue(gate.stoppedAndReleased("second"))
        assertTrue(gate.accept("second"))
        assertFalse(gate.accept("second"))
    }

    @Test fun uploadRequiresExplicitStopAndReleaseTransition() {
        val gate = SpeechTransferGate()
        assertTrue(gate.arm("a"))
        assertFalse(gate.accept("a"))
        assertFalse(gate.stoppedAndReleased("b"))
        assertTrue(gate.stoppedAndReleased("a"))
        assertFalse(gate.stoppedAndReleased("a"))
        assertTrue(gate.accept("a"))
    }
}
