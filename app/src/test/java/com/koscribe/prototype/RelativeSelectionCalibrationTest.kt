package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class RelativeSelectionCalibrationTest {
    private val fixture = RelativeSelectionCalibration.Package.FIXTURE

    @Test fun selectionArmsNeverEnterTheClosureThatAllocatesOrReadsAnEditor() {
        var entered = 0
        for (arm in listOf("OFF", "FIXTURE_SELECTION", "KARROT_SELECTION", "UNKNOWN")) {
            assertEquals("blocked", RelativeProbeAccess.requestContent(true, arm, "blocked") {
                entered++; error("Editor access must be unreachable")
            })
        }
        for (arm in listOf("FIXTURE", "KARROT", "FIXTURE_SELECTION", "KARROT_SELECTION")) {
            assertEquals("blocked", RelativeProbeAccess.requestContent(false, arm, "blocked") {
                entered++; error("Release access must be unreachable")
            })
        }
        assertEquals(0, entered)
        for (arm in listOf("FIXTURE", "KARROT"))
            assertEquals("read", RelativeProbeAccess.requestContent(true, arm, "blocked") { entered++; "read" })
        assertEquals(2, entered)
    }

    @Test fun initialPairAndActualCallbacksRemainDistinctAcrossTheFixedSampleStages() {
        val cache = RelativeSelectionCalibration()
        cache.start(2, fixture, false, 0, 0, 10)
        val initial = cache.snapshot()
        assertEquals(RelativeReadPolicy.Source.INITIAL_ONLY, initial.source)
        assertEquals(RelativeReadPolicy.Composition.UNKNOWN, initial.composition)
        assertEquals(0, initial.callbacksGen)
        assertEquals(RelativeSelectionCalibration.SampleRange.ZERO, initial.sampleRange)
        cache.update(2, 4, 4, -1, -1, 20)
        assertEquals(RelativeSelectionCalibration.SampleRange.CARET4, cache.snapshot().sampleRange)
        assertEquals(RelativeSelectionCalibration.Position.NONZERO, cache.snapshot().position)
        cache.update(2, 0, 4, -1, -1, 30)
        assertEquals(RelativeReadPolicy.Shape.SELECTED, cache.snapshot().shape)
        assertEquals(RelativeSelectionCalibration.SampleRange.RANGE_0_4, cache.snapshot().sampleRange)
        cache.update(2, 0, 0, -1, -1, 40)
        val final = cache.snapshot()
        assertEquals(RelativeReadPolicy.Source.CALLBACK_CURRENT_GEN, final.source)
        assertEquals(RelativeReadPolicy.Composition.NONE_REPORTED, final.composition)
        assertEquals(RelativeSelectionCalibration.Position.ORIGIN, final.position)
        assertEquals(3, final.callbacksGen)
        assertEquals(3, final.callbacksTotal)
        assertEquals(1, final.starts)
        assertEquals(2, final.generation)
        assertEquals(RelativeReadPolicy.Source.INITIAL_ONLY, initial.source) // Published object never mutates.
        assertEquals(0, initial.callbacksTotal)
    }

    @Test fun restartDoesNotPromoteInitialMetadataAndOldGenerationUpdatesAreIgnored() {
        val cache = RelativeSelectionCalibration()
        cache.start(1, fixture, false, 0, 0, 1)
        cache.update(1, 4, 4, 0, 4, 2)
        cache.signal(RelativeSelectionCalibration.Signal.TEXT)
        cache.start(2, fixture, true, 4, 4, 3)
        val restarted = cache.snapshot()
        assertEquals(2, restarted.starts)
        assertEquals(1, restarted.restarts)
        assertEquals(RelativeSelectionCalibration.Start.RESTART, restarted.lastStart)
        assertEquals(0, restarted.callbacksGen)
        assertEquals(1, restarted.callbacksTotal)
        assertEquals(0, restarted.signalsGen)
        assertEquals(RelativeReadPolicy.Composition.UNKNOWN, restarted.composition)
        assertEquals(RelativeReadPolicy.Source.INITIAL_ONLY, restarted.source)
        cache.update(1, 0, 4, -1, -1, 4)
        assertEquals(restarted, cache.snapshot())
        cache.update(2, 4, 4, -1, -1, 5) // An identical delivered pair is still a callback.
        assertEquals(1, cache.snapshot().callbacksGen)
        assertEquals(2, cache.snapshot().callbacksTotal)
    }

    @Test fun foreignOrUnsafeInputClearsTheSampleAndCannotReceiveTargetUpdates() {
        for (outside in listOf(RelativeSelectionCalibration.Package.OTHER, fixture)) {
            val cache = RelativeSelectionCalibration()
            cache.start(1, fixture, false, 4, 4, 1)
            cache.update(1, 0, 4, -1, -1, 2)
            cache.outOfScope(outside) // Includes a same-package input whose type is unsafe.
            val cleared = cache.snapshot()
            assertEquals(RelativeSelectionCalibration.State.OUT_OF_SCOPE, cleared.state)
            assertEquals(-1, cleared.generation)
            assertEquals(RelativeReadPolicy.Source.NONE, cleared.source)
            assertEquals(RelativeSelectionCalibration.SampleRange.UNKNOWN, cleared.sampleRange)
            assertNull(cleared.sampleAt)
            cache.update(1, 4, 4, -1, -1, 3)
            cache.signal(RelativeSelectionCalibration.Signal.CONTENT)
            assertEquals(cleared, cache.snapshot())
            assertEquals(1, cleared.callbacksTotal)
        }
    }

    @Test fun finishClearsCurrentGenerationAndRepeatedCleanupDoesNotInventFinishes() {
        val cache = RelativeSelectionCalibration()
        cache.start(7, fixture, false, 0, 0, 1)
        cache.update(7, 0, 0, -1, -1, 2)
        cache.finish()
        val ended = cache.snapshot()
        assertEquals(RelativeSelectionCalibration.State.NO_INPUT, ended.state)
        assertEquals(RelativeSelectionCalibration.Package.NONE, ended.packageKind)
        assertEquals(1, ended.finishes)
        assertEquals(1, ended.callbacksTotal)
        assertEquals(0, ended.callbacksGen)
        cache.finish()
        cache.update(7, 4, 4, -1, -1, 3)
        assertEquals(ended, cache.snapshot())
    }

    @Test fun laterSignalsAndElapsedAgeCannotMasqueradeAsFreshSelection() {
        val cache = RelativeSelectionCalibration()
        cache.start(1, fixture, false, 0, 0, 10)
        cache.signal(RelativeSelectionCalibration.Signal.CONTENT)
        val signaled = cache.snapshot()
        assertEquals(RelativeReadPolicy.Source.INITIAL_ONLY, signaled.source)
        assertEquals(RelativeSelectionCalibration.Order.AFTER_SAMPLE, signaled.signalOrder)
        assertTrue(signaled.code(1011).contains("SAMPLE_AGE=OLDER SIGNAL_ORDER=AFTER_SAMPLE"))
        cache.update(1, 4, 4, -1, -1, 1020)
        assertEquals(RelativeSelectionCalibration.Order.BEFORE_SAMPLE, cache.snapshot().signalOrder)
        cache.signal(RelativeSelectionCalibration.Signal.SELECTION)
        assertEquals(RelativeSelectionCalibration.Order.AFTER_SAMPLE, cache.snapshot().signalOrder)
        assertEquals(2, cache.snapshot().signalsGen)
        assertEquals(1, cache.snapshot().callbacksGen)
        assertTrue(cache.snapshot().code(1000).contains("SAMPLE_AGE=UNKNOWN"))
        assertTrue(cache.snapshot().code(1021).endsWith("CONTENT_READ=DISABLED ELIGIBLE=NEVER"))
    }

    @Test fun invalidUnknownReversedAndOtherPairsHaveBoundedLabelsOnly() {
        val cache = RelativeSelectionCalibration()
        cache.start(1, fixture, false, -1, -1, 1)
        assertEquals(RelativeSelectionCalibration.SampleRange.UNKNOWN, cache.snapshot().sampleRange)
        cache.update(1, -1, 0, -1, 0, 2)
        assertEquals(RelativeSelectionCalibration.SampleRange.INVALID, cache.snapshot().sampleRange)
        assertEquals(RelativeReadPolicy.Composition.INVALID, cache.snapshot().composition)
        cache.update(1, 4, 0, 0, 0, 3)
        assertEquals(RelativeSelectionCalibration.SampleRange.RANGE_0_4, cache.snapshot().sampleRange)
        assertEquals(RelativeReadPolicy.Composition.COMPOSING, cache.snapshot().composition)
        cache.update(1, 9876, 9876, -1, -1, 4)
        assertEquals(RelativeSelectionCalibration.SampleRange.OTHER, cache.snapshot().sampleRange)
        assertFalse(cache.snapshot().code(5).contains("9876"))
    }

    @Test fun countersSaturateExplicitlyInsteadOfWrappingOrImplyingNewOrdering() {
        val cache = RelativeSelectionCalibration()
        cache.start(1, fixture, false, 0, 0, 1)
        repeat(RelativeSelectionCalibration.COUNTER_LIMIT + 1) { cache.update(1, 0, 0, -1, -1, 2) }
        val saturated = cache.snapshot()
        assertEquals(RelativeSelectionCalibration.COUNTER_LIMIT, saturated.callbacksGen)
        assertEquals(RelativeSelectionCalibration.COUNTER_LIMIT, saturated.callbacksTotal)
        assertTrue(saturated.code(3).contains("COUNTERS=SATURATED"))
        cache.start(2, fixture, true, 0, 0, 4)
        assertEquals(0, cache.snapshot().callbacksGen)
        assertTrue(cache.snapshot().saturated)
    }
}
