package com.koscribe.prototype

/** A short UI-readiness gate; it never grants or rearms a destination intent. */
internal class ReviewImeReadiness(private val launchedAtMs: Long) {
    enum class Evidence { READY, MEASURED_ENTRY_GAP, CONTRADICTION }
    enum class Verdict { READY, WAIT, FALLBACK }
    private enum class State { INITIAL, WAITING, READY, FALLBACK }
    private var state = State.INITIAL

    val pending: Boolean get() = state == State.WAITING
    val initial: Boolean get() = state == State.INITIAL

    fun expired(nowMs: Long): Boolean = nowMs < launchedAtMs ||
        (state != State.READY && nowMs - launchedAtMs >= 1_000L)

    /** Only the fully authenticated original REMOVED event may start the measured wait. */
    fun check(evidence: Evidence, nowMs: Long, allowInitialGap: Boolean = false): Verdict {
        if (state == State.FALLBACK) return Verdict.FALLBACK
        if (expired(nowMs)) return fail()
        return when (evidence) {
            Evidence.READY -> {
                state = State.READY
                Verdict.READY
            }
            Evidence.MEASURED_ENTRY_GAP -> if (state == State.WAITING ||
                (state == State.INITIAL && allowInitialGap)) {
                state = State.WAITING
                Verdict.WAIT
            } else fail()
            Evidence.CONTRADICTION -> fail()
        }
    }

    private fun fail(): Verdict {
        state = State.FALLBACK
        return Verdict.FALLBACK
    }
}
