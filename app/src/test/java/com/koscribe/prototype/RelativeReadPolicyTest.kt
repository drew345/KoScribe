package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class RelativeReadPolicyTest {
    @Test fun unknownOffsetDoesNotConcealRelativeInvalidityOrComposition() {
        val code = RelativeReadPolicy.classify(0, -1, 1, 1,
            RelativeReadPolicy.Style.COMPOSING, null, 10)
        assertTrue(code.contains("OFFSET=UNKNOWN RELATIVE=INVALID STYLE=COMPOSING"))
        assertTrue(code.contains("ELIGIBLE=NEVER"))
    }
    @Test fun missingSelectedTextFallbackContradictsIndependentRealSelection() {
        val evidence = RelativeSelectionEvidence().apply { start(1, 0, 4, 1) }
        val code = RelativeReadPolicy.classify(0, -1, 0, 0,
            RelativeReadPolicy.Style.UNSTYLED, evidence.get(1), 2)
        assertTrue(code.contains("RELATIVE=COLLAPSED"))
        assertTrue(code.contains("INPUT_SELECTION=SELECTED INPUT_SOURCE=INITIAL_ONLY"))
        assertTrue(code.contains("AGREEMENT=SHAPE_CONTRADICTS ELIGIBLE=NEVER"))
        assertFalse(RelativeReadPolicy.permitsEmptyRead(evidence.get(1)))
    }
    @Test fun relativeCollapsedWithoutIndependentEvidenceRemainsUnknown() {
        val code = RelativeReadPolicy.classify(2, -1, 1, 1,
            RelativeReadPolicy.Style.UNSTYLED, null, 1)
        assertTrue(code.contains("INPUT_SELECTION=UNKNOWN INPUT_SOURCE=NONE"))
        assertTrue(code.contains("AGREEMENT=UNKNOWN ELIGIBLE=NEVER"))
    }
    @Test fun reversedSelectionIsStillSelectedAndCannotPassEmptyScope() {
        val evidence = RelativeSelectionEvidence().apply { start(1, 4, 0, 1) }
        val code = RelativeReadPolicy.classify(4, -1, 4, 0,
            RelativeReadPolicy.Style.NO_COMPOSING_REPORTED, evidence.get(1), 2)
        assertTrue(code.contains("RELATIVE=SELECTED"))
        assertFalse(RelativeReadPolicy.permitsEmptyRead(evidence.get(1)))
    }
    @Test fun updateSourceGenerationAndKnownCompositionArePreserved() {
        val evidence = RelativeSelectionEvidence()
        evidence.start(2, 0, 0, 10)
        assertEquals(RelativeReadPolicy.Composition.UNKNOWN, evidence.get(2)?.composition)
        evidence.update(1, 9, 9, -1, -1, 11)
        assertEquals(RelativeReadPolicy.Source.INITIAL_ONLY, evidence.get(2)?.source)
        evidence.update(2, 0, 0, 0, 2, 12)
        assertEquals(RelativeReadPolicy.Composition.COMPOSING, evidence.get(2)?.composition)
        assertEquals(RelativeReadPolicy.Source.CALLBACK_CURRENT_GEN, evidence.get(2)?.source)
        evidence.signal()
        assertTrue(evidence.get(2)!!.laterSignal)
        evidence.clear()
        assertNull(evidence.get(2))
        evidence.update(2, 0, 0, -1, -1, 13)
        assertNull(evidence.get(2))
    }
    @Test fun emptyReadScopeRejectsNonzeroPartialAndKnownSelectedEvidence() {
        val evidence = RelativeSelectionEvidence()
        for (pair in listOf(1 to 1, -1 to 0, 0 to 2)) {
            evidence.start(1, pair.first, pair.second, 1)
            assertFalse(RelativeReadPolicy.permitsEmptyRead(evidence.get(1)))
        }
        evidence.start(1, -1, -1, 1)
        assertTrue(RelativeReadPolicy.permitsEmptyRead(evidence.get(1)))
    }
    @Test fun callbackAgeAndSignalsAreDiagnosticNotFreshnessAuthorization() {
        val evidence = RelativeSelectionEvidence().apply {
            start(1, 0, 0, 1); update(1, 0, 0, -1, -1, 10); signal()
        }
        val code = RelativeReadPolicy.classify(0, -1, 0, 0,
            RelativeReadPolicy.Style.UNSTYLED, evidence.get(1), 2011)
        assertTrue(code.contains("INPUT_AGE=OLDER INPUT_COMPOSITION=NONE_REPORTED INPUT_SIGNAL=LATER_SIGNAL"))
        assertTrue(code.contains("AGREEMENT=SHAPE_AGREES ELIGIBLE=NEVER"))
    }
    @Test fun latestUpdateSupersedesHistoricalStartButNewInputResetsIt() {
        val evidence = RelativeSelectionEvidence().apply { start(1, 6, 6, 1) }
        assertFalse(RelativeReadPolicy.permitsEmptyRead(evidence.get(1)))
        evidence.update(1, 0, 0, -1, -1, 2)
        assertTrue(RelativeReadPolicy.permitsEmptyRead(evidence.get(1)))
        evidence.start(2, -1, -1, 3)
        assertNull(evidence.get(1))
        assertEquals(RelativeReadPolicy.Source.INITIAL_ONLY, evidence.get(2)?.source)
        assertEquals(RelativeReadPolicy.Composition.UNKNOWN, evidence.get(2)?.composition)
    }
    @Test fun malformedOffsetAndSelectionAreIndependentlyReported() {
        val code = RelativeReadPolicy.classify(2, -2, -1, 0,
            RelativeReadPolicy.Style.NOT_INSPECTED, null, 1)
        assertTrue(code.contains("OFFSET=INVALID RELATIVE=INVALID STYLE=NOT_INSPECTED"))
        assertEquals(RelativeReadPolicy.Composition.INVALID, RelativeReadPolicy.composition(-1, 0))
    }
}
