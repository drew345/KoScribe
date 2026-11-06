package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class RelativeEmptyPolicyTest {
    private val empty = RelativeEmptyPolicy.Sample("", -1, 0, 0, false)
    private val saved = "  고친 문장🙂  "
    private fun evidence() = RelativeEmptyPolicy.Evidence().apply { start(1, 0, 0) }
    private fun attempt(e: RelativeEmptyPolicy.Evidence, first: RelativeEmptyPolicy.Sample = empty): RelativeEmptyPolicy.Attempt =
        RelativeEmptyPolicy.Attempt(e.current()!!, saved, 100).also { assertTrue(it.captureFirst(first, 101)) }
    private fun exact(composing: Boolean = false) = RelativeEmptyPolicy.Sample(saved, -1, saved.length, saved.length, composing)
    private fun delivered(e: RelativeEmptyPolicy.Evidence, start: Int = saved.length, end: Int = start,
        candidateStart: Int = -1, candidateEnd: Int = -1) = e.update(1, start, end, candidateStart, candidateEnd)

    @Test fun untouchedFreshInitialEmptyNeedsTwoReadsThenANewerCallbackAndExactSavedPayload() {
        val e = evidence()
        val initial = e.current()!!
        assertEquals(RelativeEmptyPolicy.Source.INITIAL_ONLY, initial.source)
        assertEquals(RelativeEmptyPolicy.Composition.UNKNOWN, initial.composition)
        val proof = attempt(e)
        assertTrue(proof.reserve(e.current(), empty, 102))
        assertEquals(RelativeEmptyPolicy.Source.INITIAL_ONLY, proof.admissionSource)
        assertEquals(RelativeEmptyPolicy.Verdict.PENDING, proof.verdict(e.current(), exact(), 103))
        delivered(e)
        assertTrue(proof.selectionChanged(e.current()))
        assertEquals(RelativeEmptyPolicy.Verdict.CONFIRMED, proof.verdict(e.current(), exact(), 104))
        assertEquals(saved, proof.result)
        assertFalse(proof.reserve(e.current(), empty, 105))
    }

    @Test fun callbackClearedEmptyAndRedundantMatchingCallbacksAreAllowed() {
        val e = evidence()
        e.update(1, 4, 4, 0, 4)
        e.update(1, 0, 4, -1, -1)
        e.update(1, 0, 0, -1, -1)
        val proof = attempt(e)
        e.update(1, 0, 0, -1, -1)
        assertTrue(proof.selectionChanged(e.current()))
        assertTrue(proof.reserve(e.current(), empty, 102))
        assertEquals(RelativeEmptyPolicy.Source.CALLBACK_CURRENT_GEN, proof.admissionSource)
        assertEquals(RelativeEmptyPolicy.Verdict.PENDING, proof.verdict(e.current(), empty, 103))
        delivered(e)
        assertEquals(RelativeEmptyPolicy.Verdict.CONFIRMED, proof.verdict(e.current(), exact(), 104))
    }

    @Test fun initialCanBeCorroboratedByMatchingCallbackWithoutReusingItsOldUpdateCounter() {
        val e = evidence()
        val proof = attempt(e)
        e.update(1, 0, 0, -1, -1)
        assertTrue(proof.selectionChanged(e.current()))
        assertTrue(proof.reserve(e.current(), empty, 102))
        assertEquals(RelativeEmptyPolicy.Source.CALLBACK_CURRENT_GEN, proof.admissionSource)
        assertEquals(RelativeEmptyPolicy.Verdict.PENDING, proof.verdict(e.current(), exact(), 103))
    }

    @Test fun selectedNullFallbackUnknownPartialAndNonzeroInitialCannotAdmit() {
        for ((start, end) in listOf(0 to 4, 4 to 0, -1 to -1, -1 to 0, 4 to 4)) {
            val e = RelativeEmptyPolicy.Evidence().apply { start(1, start, end) }
            assertFalse(RelativeEmptyPolicy.admits(e.current()))
            val proof = RelativeEmptyPolicy.Attempt(e.current()!!, saved, 100)
            assertFalse(proof.captureFirst(empty, 101)) // Default null-selected fallback still says empty0/0.
            assertFalse(proof.reserve(e.current(), empty, 102))
        }
    }

    @Test fun firstAndSecondReadAreBothRequiredAndNoNonemptyOrMalformedContextIsAccepted() {
        val e = evidence()
        val noFirst = RelativeEmptyPolicy.Attempt(e.current()!!, saved, 100)
        assertFalse(noFirst.reserve(e.current(), empty, 101))
        for (bad in listOf(empty.copy(text = "existing"), empty.copy(offset = 0), empty.copy(offset = -2),
            empty.copy(start = -1, end = -1), empty.copy(end = 1), empty.copy(composing = true))) {
            val firstBad = RelativeEmptyPolicy.Attempt(e.current()!!, saved, 100)
            assertFalse(firstBad.captureFirst(bad, 101))
            val secondBad = attempt(e)
            assertFalse(secondBad.reserve(e.current(), bad, 102))
            assertFalse(secondBad.reserve(e.current(), empty, 103))
        }
    }

    @Test fun knownCompositionRejectsEvenEmptyAndUnknownInitialNeverBecomesClear() {
        for ((start, end) in listOf(0 to 0, 0 to 4, -1 to 0)) {
            val e = evidence()
            e.update(1, 0, 0, start, end)
            assertFalse(RelativeEmptyPolicy.admits(e.current()))
        }
        val e = evidence()
        val proof = attempt(e)
        assertEquals(RelativeEmptyPolicy.Composition.UNKNOWN, proof.selection.composition)
        assertTrue(proof.reserve(e.current(), empty, 102))
        assertEquals(RelativeEmptyPolicy.Composition.UNKNOWN, proof.selection.composition)
    }

    @Test fun staleInitialAfterRelevantEventRefusesButNewExplicitCaptureCanUseLaterValidEvidence() {
        val e = evidence()
        e.selectionEvent(0, 0) // Matching metadata is not a contradiction.
        assertTrue(RelativeEmptyPolicy.admits(e.current()))
        e.textChanged()
        assertFalse(RelativeEmptyPolicy.admits(e.current()))
        e.update(1, 0, 0, -1, -1)
        assertTrue(RelativeEmptyPolicy.admits(e.current()))
        e.selectionEvent(0, 4)
        assertFalse(RelativeEmptyPolicy.admits(e.current()))
        e.update(1, 0, 0, -1, -1)
        assertTrue(attempt(e).reserve(e.current(), empty, 102))
    }

    @Test fun invalidatedCaptureNeverRearmsAfterContextAndSelectionReturnToEmpty() {
        val e = evidence()
        val proof = attempt(e)
        proof.end() // Real service text/window/input event latch.
        e.update(1, 0, 0, -1, -1)
        assertFalse(proof.selectionChanged(e.current()))
        assertFalse(proof.reserve(e.current(), empty, 102))
        assertFalse(proof.precommitLive(103))
    }

    @Test fun precommitContradictionIsStickyEvenWhenALaterCallbackLooksSafe() {
        val e = evidence()
        val proof = attempt(e)
        e.update(1, 0, 4, -1, -1)
        assertFalse(proof.selectionChanged(e.current()))
        e.update(1, 0, 0, -1, -1)
        assertFalse(proof.selectionChanged(e.current()))
        assertFalse(proof.reserve(e.current(), empty, 102))
    }

    @Test fun changedGenerationAndClearedEvidenceCannotReuseOldContext() {
        val e = evidence()
        val proof = attempt(e)
        e.start(2, 0, 0)
        e.update(1, 0, 0, -1, -1)
        assertEquals(RelativeEmptyPolicy.Source.INITIAL_ONLY, e.current()!!.source)
        assertFalse(proof.reserve(e.current(), empty, 102))
        e.clear()
        e.update(2, 0, 0, -1, -1)
        assertNull(e.current())
    }

    @Test fun wrongPostcommitCallbackCannotBeErasedBeforeTheNextRead() {
        val e = evidence()
        val proof = attempt(e)
        assertTrue(proof.reserve(e.current(), empty, 102))
        delivered(e, 0, saved.length)
        assertFalse(proof.selectionChanged(e.current()))
        delivered(e)
        assertFalse(proof.selectionChanged(e.current()))
        assertEquals(RelativeEmptyPolicy.Verdict.UNCERTAIN, proof.verdict(e.current(), exact(), 103))
    }

    @Test fun exactComposingResultCanConfirmButAlteredTextCaretOrMissingReadsAreTerminal() {
        val e = evidence()
        val exactProof = attempt(e)
        assertTrue(exactProof.reserve(e.current(), empty, 102))
        delivered(e, candidateStart = 0, candidateEnd = saved.length)
        assertEquals(RelativeEmptyPolicy.Verdict.CONFIRMED, exactProof.verdict(e.current(), exact(true), 103))
        for (bad in listOf(null, exact().copy(text = saved + "extra"), exact().copy(start = 0, end = 0),
            exact().copy(text = "changed", start = 7, end = 7, composing = true))) {
            val nextEvidence = evidence()
            val proof = attempt(nextEvidence)
            assertTrue(proof.reserve(nextEvidence.current(), empty, 102))
            delivered(nextEvidence)
            assertEquals(RelativeEmptyPolicy.Verdict.UNCERTAIN, proof.verdict(nextEvidence.current(), bad, 103))
            assertEquals(RelativeEmptyPolicy.Verdict.UNCERTAIN, proof.verdict(nextEvidence.current(), exact(), 104))
        }
    }

    @Test fun fixedDeadlinesAndAttemptConsumptionNeverExtendForFurtherReadsOrCallbacks() {
        val e = evidence()
        val late = attempt(e)
        assertFalse(late.reserve(e.current(), empty, 3100))
        val proof = attempt(e)
        assertTrue(proof.reserve(e.current(), empty, 102))
        assertTrue(proof.commitAuthorized(3099))
        assertFalse(proof.commitAuthorized(3100))
        assertTrue(proof.postcommitLive(3101))
        assertFalse(proof.postcommitLive(3102))
        assertEquals(RelativeEmptyPolicy.Verdict.UNCERTAIN, proof.verdict(e.current(), empty, 3102))
        delivered(e)
        assertEquals(RelativeEmptyPolicy.Verdict.UNCERTAIN, proof.verdict(e.current(), exact(), 3103))
        assertFalse(proof.reserve(e.current(), empty, 3104))
    }
}
