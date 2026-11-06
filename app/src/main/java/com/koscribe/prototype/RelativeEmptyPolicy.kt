package com.koscribe.prototype

import java.util.UUID

/** Separate from absolute snapshots and from the diagnostic classifiers. Memory only; never log text. */
internal object RelativeEmptyPolicy {
    const val PRECOMMIT_MS = 3000L
    const val POSTCOMMIT_MS = 3000L
    enum class Source { INITIAL_ONLY, CALLBACK_CURRENT_GEN }
    enum class Composition { UNKNOWN, NONE_REPORTED, COMPOSING, INVALID }
    enum class Verdict { CONFIRMED, PENDING, UNCERTAIN }
    data class Selection(val generation: Int, val start: Int, val end: Int,
        val source: Source, val composition: Composition, val updates: Long,
        val unresolvedMutation: Boolean = false)
    data class Sample(val text: String, val offset: Int, val start: Int, val end: Int,
        val composing: Boolean)

    /** Current-generation numeric metadata, independent of transaction-local composition resets. */
    class Evidence {
        @Volatile private var current: Selection? = null
        fun current(): Selection? = current
        fun start(generation: Int, start: Int, end: Int) {
            current = Selection(generation, start, end, Source.INITIAL_ONLY, Composition.UNKNOWN, 0)
        }
        fun clear() { current = null }
        fun update(generation: Int, start: Int, end: Int, candidatesStart: Int, candidatesEnd: Int) {
            val old = current?.takeIf { it.generation == generation } ?: return
            val composition = when {
                candidatesStart == -1 && candidatesEnd == -1 -> Composition.NONE_REPORTED
                candidatesStart >= 0 && candidatesEnd >= candidatesStart -> Composition.COMPOSING
                else -> Composition.INVALID
            }
            current = Selection(generation, start, end, Source.CALLBACK_CURRENT_GEN, composition,
                if (old.updates == Long.MAX_VALUE) Long.MAX_VALUE else old.updates + 1,
                unresolvedMutation = old.updates == Long.MAX_VALUE)
        }
        fun textChanged() { current = current?.copy(unresolvedMutation = true) }
        fun selectionEvent(start: Int, end: Int) {
            val old = current ?: return
            if (start != old.start || end != old.end || start < 0 || end < 0)
                current = old.copy(unresolvedMutation = true)
        }
    }

    fun admits(selection: Selection?): Boolean = selection != null && selection.generation >= 0 &&
        selection.start == 0 && selection.end == 0 && !selection.unresolvedMutation &&
        when (selection.source) {
            Source.INITIAL_ONLY -> selection.composition == Composition.UNKNOWN
            Source.CALLBACK_CURRENT_GEN -> selection.composition == Composition.NONE_REPORTED
        }

    fun empty(sample: Sample?): Boolean = sample != null && sample.offset == -1 &&
        sample.text.isEmpty() && sample.start == 0 && sample.end == 0 && !sample.composing

    fun resultAllowed(result: String): Boolean = result.isNotBlank() && result.length <= 512 &&
        result.none { it == '\n' || it == '\r' }

    class Attempt(val selection: Selection, val result: String, val started: Long) {
        val id: String = UUID.randomUUID().toString()
        private var ended = false
        private var firstVerified = false
        private var attemptedAt: Long? = null
        private var baselineUpdates: Long? = null
        var admissionSource: Source = selection.source
            private set
        fun end() { ended = true }
        fun precommitLive(now: Long): Boolean = !ended && attemptedAt == null && now >= started &&
            now - started < PRECOMMIT_MS
        fun captureFirst(first: Sample?, now: Long): Boolean {
            if (!precommitLive(now) || firstVerified || !admits(selection) || !empty(first) || !resultAllowed(result)) {
                ended = true; return false
            }
            firstVerified = true
            return true
        }
        fun postcommitLive(now: Long): Boolean = !ended && attemptedAt?.let {
            now >= it && now - it < POSTCOMMIT_MS
        } == true
        fun commitAuthorized(now: Long): Boolean = !ended && attemptedAt != null &&
            now >= started && now - started < PRECOMMIT_MS
        /** Observe every delivered callback, so a later matching pair cannot erase a contradiction. */
        fun selectionChanged(latest: Selection?): Boolean {
            if (ended) return false
            val valid = if (attemptedAt == null) latest?.generation == selection.generation && admits(latest)
                else latest != null && latest.generation == selection.generation &&
                    (!(latest.source == Source.CALLBACK_CURRENT_GEN && latest.updates > (baselineUpdates ?: Long.MAX_VALUE)) ||
                        (latest.start == result.length && latest.end == result.length && latest.composition != Composition.INVALID))
            if (!valid) ended = true
            return valid
        }
        fun reserve(latest: Selection?, second: Sample?, now: Long): Boolean {
            if (!precommitLive(now) || !firstVerified || latest?.generation != selection.generation || !admits(latest) || !empty(second) ||
                !resultAllowed(result)) { ended = true; return false }
            baselineUpdates = latest!!.updates
            admissionSource = latest.source
            attemptedAt = now // Consume before editor IPC.
            return true
        }
        fun verdict(latest: Selection?, sample: Sample?, now: Long): Verdict {
            val attempted = attemptedAt ?: return Verdict.UNCERTAIN
            if (ended || now < attempted || now - attempted >= POSTCOMMIT_MS || latest == null ||
                latest.generation != selection.generation || sample == null || sample.offset != -1 ||
                sample.start < 0 || sample.start != sample.end || sample.end > sample.text.length)
                return terminal(Verdict.UNCERTAIN)
            val updated = latest.source == Source.CALLBACK_CURRENT_GEN &&
                latest.updates > (baselineUpdates ?: Long.MAX_VALUE)
            val expectedCaret = latest.start == result.length && latest.end == result.length
            if (updated && (!expectedCaret || latest.composition == Composition.INVALID))
                return terminal(Verdict.UNCERTAIN)
            val exact = sample.text == result && sample.start == result.length
            if (exact && updated) return terminal(Verdict.CONFIRMED)
            // Old initial/callback metadata is pending, not a new contradictory caret report.
            if (exact || (empty(sample) && !updated)) return Verdict.PENDING
            return terminal(Verdict.UNCERTAIN)
        }
        private fun terminal(verdict: Verdict): Verdict { ended = true; return verdict }
    }
}
