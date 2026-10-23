package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class InputContinuityTest {
    @Test fun unknownAndComposingRemainUnsafeAcrossEditorRestart() {
        val evidence = CompositionEvidence()
        assertEquals(CompositionState.UNKNOWN, evidence.stateAt(4))
        evidence.update(4, 4, 2, 4)
        assertEquals(CompositionState.COMPOSING, evidence.stateAt(4))
        evidence.update(4, 4, -1, -1)
        assertEquals(CompositionState.CLEAR, evidence.stateAt(4))
        evidence.reset() // Review editor took input focus.
        assertEquals(CompositionState.UNKNOWN, evidence.stateAt(4))
        evidence.update(4, 4, -1, -1)
        assertEquals(CompositionState.SELECTION_MISMATCH, evidence.stateAt(5))
        assertEquals(CompositionState.CLEAR, evidence.stateAt(4))
    }

    @Test fun returnRequiresOriginalGenerationOrExactlyOneFreshInput() {
        val continuity = ReturnContinuity()
        continuity.capture(3)
        continuity.beginReturn()
        assertTrue(continuity.allows(3))
        assertFalse(continuity.allows(4)) // Review editor generation is not the target.
        continuity.returnedInput()
        assertTrue(continuity.allows(5))
        continuity.returnedInput()
        assertFalse(continuity.allows(5)) // Second input start may be navigation.
    }

    @Test fun repeatedWindowReturnOrFinishedInputFailsClosed() {
        val continuity = ReturnContinuity()
        continuity.capture(1)
        continuity.beginReturn()
        continuity.returnedInput()
        continuity.returnedStateWindow()
        assertTrue(continuity.allows(3))
        continuity.returnedStateWindow()
        assertFalse(continuity.allows(3))
        continuity.capture(3)
        continuity.beginReturn()
        continuity.returnedInput()
        continuity.finishedInput()
        assertFalse(continuity.allows(4))
    }
}
