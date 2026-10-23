package com.koscribe.prototype

enum class CompositionState { UNKNOWN, CLEAR, COMPOSING, SELECTION_MISMATCH }

/** Evidence from accessibility input callbacks. UNKNOWN is not claimed clear. */
class CompositionEvidence {
    private var known = false
    private var start = -1
    private var end = -1
    private var composingStart = -1
    private var composingEnd = -1

    fun reset() { known = false }

    fun update(newStart: Int, newEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        start = newStart
        end = newEnd
        composingStart = candidatesStart
        composingEnd = candidatesEnd
        known = newStart >= 0 && newEnd >= 0 &&
            (candidatesStart == -1 && candidatesEnd == -1 ||
                candidatesStart >= 0 && candidatesEnd >= candidatesStart)
    }

    fun stateAt(caret: Int): CompositionState = when {
        !known -> CompositionState.UNKNOWN
        start != caret || end != caret -> CompositionState.SELECTION_MISMATCH
        composingStart >= 0 -> CompositionState.COMPOSING
        composingStart == -1 && composingEnd == -1 -> CompositionState.CLEAR
        else -> CompositionState.UNKNOWN
    }
}

/** Limits the deliberate review->original editor transition to one return. */
class ReturnContinuity {
    private var capturedGeneration = -1
    private var inputSeen = false
    private var stateSeen = false
    private var invalid = false

    fun capture(generation: Int) {
        capturedGeneration = generation
        inputSeen = false
        stateSeen = false
        invalid = false
    }

    fun beginReturn() {
        inputSeen = false
        stateSeen = false
    }

    fun returnedInput(): Boolean { if (inputSeen) invalid = true else inputSeen = true; return !invalid }
    fun returnedStateWindow(): Boolean { if (stateSeen) invalid = true else stateSeen = true; return !invalid }
    fun finishedInput(): Boolean { if (inputSeen) invalid = true; return !invalid }
    fun invalidate() { invalid = true }
    fun allows(generation: Int): Boolean = !invalid && capturedGeneration >= 0 &&
        (generation == capturedGeneration || inputSeen)
}
