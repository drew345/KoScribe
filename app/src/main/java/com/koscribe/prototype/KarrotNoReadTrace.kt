package com.koscribe.prototype

import java.util.concurrent.atomic.AtomicReference

/** Categorical state only. This diagnostic never stores editor content or identifiers. */
internal class KarrotNoReadTrace {
    enum class Phase { QUEUED, PREFLIGHT, FIRST_YIELD, CONTINUITY, SECOND_YIELD_WOULD_READ }
    enum class Source {
        NONE, INPUT_START, INPUT_FINISH, INPUT_SELECTION,
        WINDOWS_CHANGED, WINDOW_STATE_CHANGED, VIEW_FOCUSED, VIEW_TEXT_CHANGED,
        VIEW_TEXT_SELECTION_CHANGED, WINDOW_CONTENT_CHANGED,
        SCREEN_OFF, SERVICE_INTERRUPT, SERVICE_DESTROY, TIMEOUT,
        PREFLIGHT_FAILURE, CONTINUITY_FAILURE, POST_FAILURE
    }

    data class Result(val source: Source, val phase: Phase) {
        val outcome: String get() = when (source) {
            Source.NONE -> "WOULD_READ"
            Source.PREFLIGHT_FAILURE, Source.CONTINUITY_FAILURE, Source.POST_FAILURE -> "REFUSED"
            else -> "INTERRUPTED"
        }
        fun code(): String = "$outcome SOURCE=${source.name} PHASE=${phase.name}"
    }

    private val phase = AtomicReference(Phase.QUEUED)
    private val first = AtomicReference<Result?>(null)

    fun advance(next: Phase) {
        if (first.get() == null) phase.set(next)
    }

    /** Returns true only for the first terminal cause. */
    fun finish(source: Source): Boolean = first.compareAndSet(null, Result(source, phase.get()))

    fun isLive(): Boolean = first.get() == null

    fun code(): String = first.get()?.code() ?: "RUNNING PHASE=${phase.get().name}"
}
