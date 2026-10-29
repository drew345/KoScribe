package com.koscribe.prototype

import java.util.concurrent.atomic.AtomicReference

/** Fixed categories only; no editor content, identifiers, or numeric offsets are retained. */
internal class KarrotEmptyReadTrace {
    enum class Phase {
        QUEUED, PREFLIGHT, FIRST_YIELD, CONTINUITY, SECOND_YIELD,
        WORKER_QUEUED, WORKER_PRE_READ, CONTENT_CALL, FINAL_CHECK
    }

    enum class Call { CALL_NOT_STARTED, CALL_ATTEMPTED, CALL_RETURNED, CALL_THROWN }

    enum class Source {
        NONE, INPUT_START, INPUT_FINISH, INPUT_SELECTION,
        WINDOWS_CHANGED, WINDOW_STATE_CHANGED, VIEW_FOCUSED, VIEW_TEXT_CHANGED,
        VIEW_TEXT_SELECTION_CHANGED, WINDOW_CONTENT_CHANGED,
        SCREEN_OFF, SERVICE_INTERRUPT, SERVICE_DESTROY, TIMEOUT,
        PREFLIGHT_FAILURE, CONTINUITY_FAILURE, POST_FAILURE, WORKER_FAILURE,
        FINAL_CHECK_FAILURE
    }

    companion object {
        fun finalSource(code: String): Source = when {
            code == "FINAL_CHECK_UNAVAILABLE" -> Source.FINAL_CHECK_FAILURE
            code == "REPORTED_EMPTY_AT_ZERO CONTINUITY_STABLE_NOT_INSERT_ELIGIBILITY" ||
                KarrotEmptyReadPolicy.Result.entries.any { it.name == code } -> Source.NONE
            else -> Source.CONTINUITY_FAILURE
        }
    }

    private data class State(
        val phase: Phase = Phase.QUEUED,
        val call: Call = Call.CALL_NOT_STARTED,
        val code: String? = null,
        val source: Source? = null,
        val terminalPhase: Phase? = null
    )

    private val state = AtomicReference(State())

    fun advance(phase: Phase) {
        state.updateAndGet { current ->
            if (current.code != null || phase.ordinal <= current.phase.ordinal) current
            else current.copy(phase = phase)
        }
    }

    /** Called immediately before the one local API invocation. It does not prove a remote read. */
    fun callAttempted() = advanceCall(Call.CALL_ATTEMPTED)
    fun callReturned() = advanceCall(Call.CALL_RETURNED)
    fun callThrown() = advanceCall(Call.CALL_THROWN)

    private fun advanceCall(next: Call) {
        state.updateAndGet { current ->
            if (current.call == Call.CALL_RETURNED || current.call == Call.CALL_THROWN ||
                next.ordinal <= current.call.ordinal) current
            else current.copy(call = next)
        }
    }

    /** The first terminal event freezes cause and phase. Call telemetry can still advance. */
    fun finish(code: String, source: Source): Boolean {
        while (true) {
            val current = state.get()
            if (current.code != null) return false
            if (state.compareAndSet(current,
                    current.copy(code = code, source = source, terminalPhase = current.phase)))
                return true
        }
    }

    fun isLive(): Boolean = state.get().code == null

    fun code(): String {
        val current = state.get()
        val status = current.code ?: "RUNNING"
        val source = current.source ?: Source.NONE
        val phase = current.terminalPhase ?: current.phase
        return "$status SOURCE=${source.name} PHASE=${phase.name} CALL=${current.call.name}"
    }
}
