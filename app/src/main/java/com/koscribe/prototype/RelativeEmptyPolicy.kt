package com.koscribe.prototype

import java.util.UUID

/** Separate from absolute snapshots and from the diagnostic classifiers. Memory only; never log text. */
internal object RelativeEmptyPolicy {
    const val PRECOMMIT_MS = 3000L
    const val POSTCOMMIT_MS = 3000L
    enum class Source { INITIAL_ONLY, CALLBACK_CURRENT_GEN }
    enum class Composition { UNKNOWN, NONE_REPORTED, COMPOSING, INVALID }
    enum class Verdict { CONFIRMED, PENDING, UNCERTAIN }
    enum class PostRead { EXACT, UNCHANGED_EMPTY, OTHER, UNAVAILABLE }
    enum class EventBinding { MATCH, NO_SOURCE, OTHER_SOURCE, TARGET_CHANGED }
    enum class EventWitness { NONE, EXPECTED, NO_SOURCE, OTHER_SOURCE, STALE, SAME_TICK,
        FUTURE, EXPIRED, CONTRADICTORY, TEXT_AFTER_WITNESS, IDENTITY_CHANGED }
    enum class ConfirmationSource { NONE, CALLBACK, SELECTION_EVENT }
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

    /** Diagnostic classification only. A null-selected fallback can look EXACT while hiding a range. */
    fun classifyPostRead(result: String, sample: Sample?): PostRead = when {
        sample == null -> PostRead.UNAVAILABLE
        sample.offset != -1 || sample.start < 0 || sample.start != sample.end ||
            sample.end > sample.text.length -> PostRead.OTHER
        sample.text == result && sample.start == result.length -> PostRead.EXACT
        empty(sample) -> PostRead.UNCHANGED_EMPTY
        else -> PostRead.OTHER
    }

    class Attempt(val selection: Selection, val result: String, val started: Long) {
        val id: String = UUID.randomUUID().toString()
        private var ended = false
        private var firstVerified = false
        private var attemptedAt: Long? = null
        private var baselineUpdates: Long? = null
        private var dispatchedAt: Long? = null
        private var witnessSequence = 0L
        private var acceptedWitness: Long? = null
        @Volatile var eventWitness = EventWitness.NONE
            private set
        @Volatile var confirmationSource = ConfirmationSource.NONE
            private set
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
        fun commitDispatched(now: Long): Boolean {
            if (!commitAuthorized(now) || dispatchedAt != null) { ended = true; return false }
            dispatchedAt = now
            return true
        }
        /** Binding is established by the Android observer, never by package/range alone. */
        fun selectionEvent(binding: EventBinding, generation: Int, eventTime: Long, receipt: Long,
            now: Long, start: Int, end: Int, itemCount: Int): Boolean {
            if (ended) return false
            if (!postcommitLive(now)) return endEvent(EventWitness.EXPIRED)
            if (generation != selection.generation || binding == EventBinding.TARGET_CHANGED)
                return endEvent(EventWitness.IDENTITY_CHANGED)
            if (binding != EventBinding.MATCH) {
                acceptedWitness = null
                eventWitness = if (binding == EventBinding.NO_SOURCE) EventWitness.NO_SOURCE else EventWitness.OTHER_SOURCE
                return true
            }
            val dispatch = dispatchedAt
            val rejected = when {
                dispatch == null || eventTime < dispatch -> EventWitness.STALE
                eventTime == dispatch -> EventWitness.SAME_TICK
                eventTime > receipt || receipt > now -> EventWitness.FUTURE
                else -> null
            }
            if (rejected != null) {
                acceptedWitness = null
                eventWitness = rejected
                return true
            }
            if (start != result.length || end != result.length || itemCount != result.length)
                return endEvent(EventWitness.CONTRADICTORY)
            if (witnessSequence == Long.MAX_VALUE) return endEvent(EventWitness.CONTRADICTORY)
            acceptedWitness = ++witnessSequence
            eventWitness = EventWitness.EXPECTED
            return true
        }
        /** A subsequent relevant text notification cannot be excused as our own mutation. */
        fun textEvent(binding: EventBinding): Boolean {
            if (ended) return false
            if (binding == EventBinding.TARGET_CHANGED) return endEvent(EventWitness.IDENTITY_CHANGED)
            if (acceptedWitness != null && binding == EventBinding.MATCH)
                return endEvent(EventWitness.TEXT_AFTER_WITNESS)
            if (binding == EventBinding.NO_SOURCE) {
                acceptedWitness = null
                eventWitness = EventWitness.NO_SOURCE
            }
            return true
        }
        /** Take before the content call. A prior exact sample can never acquire a later witness. */
        fun postReadWitness(now: Long): Long? = if (postcommitLive(now)) acceptedWitness else null
        private fun endEvent(reason: EventWitness): Boolean {
            eventWitness = reason
            acceptedWitness = null
            ended = true
            return false
        }
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
        fun verdict(latest: Selection?, sample: Sample?, now: Long, readWitness: Long? = null): Verdict {
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
            val witnessed = readWitness != null && readWitness == acceptedWitness
            if (exact && updated) {
                confirmationSource = ConfirmationSource.CALLBACK
                return terminal(Verdict.CONFIRMED)
            }
            if (exact && witnessed) {
                confirmationSource = ConfirmationSource.SELECTION_EVENT
                return terminal(Verdict.CONFIRMED)
            }
            // Old initial/callback metadata is pending, not a new contradictory caret report.
            if (exact || (empty(sample) && !updated && !witnessed)) return Verdict.PENDING
            return terminal(Verdict.UNCERTAIN)
        }
        private fun terminal(verdict: Verdict): Verdict { ended = true; return verdict }
    }
}
