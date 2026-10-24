package com.koscribe.prototype

import java.util.concurrent.atomic.AtomicReference

/** Exactly one terminal event wins, including races between network and Cancel. */
internal class SpeechJobOutcome {
    enum class State { ACTIVE, CANCELLED, TIMED_OUT, DELIVERED }
    private val state = AtomicReference(State.ACTIVE)

    fun isActive(): Boolean = state.get() == State.ACTIVE
    fun cancel(): Boolean = state.compareAndSet(State.ACTIVE, State.CANCELLED)
    fun timeOut(): Boolean = state.compareAndSet(State.ACTIVE, State.TIMED_OUT)
    fun deliver(): Boolean = state.compareAndSet(State.ACTIVE, State.DELIVERED)
}
