package com.koscribe.prototype

import org.junit.Assert.*
import org.junit.Test

class ModalOcclusionGateTest {
    private val targetWindow = 7
    private val panelWindow = 12

    private class Panel(val gate: ModalOcclusionGate, val owner: Any)

    private fun recording(): Panel {
        val gate = ModalOcclusionGate(targetWindow)
        val owner = Any()
        assertTrue(gate.bind(owner))
        assertTrue(gate.updatePanel(owner, PanelPhase.RECORDING, false, false, -1))
        assertTrue(gate.updatePanel(owner, PanelPhase.RECORDING, true, true, panelWindow))
        return Panel(gate, owner)
    }

    private fun removed(): Panel = recording().also {
        assertTrue(it.gate.targetRemoved(targetWindow, panelWindow, true, true))
    }

    private fun reviewing(): Panel = removed().also {
        assertTrue(it.gate.updatePanel(it.owner, PanelPhase.REVIEW, true, true, panelWindow))
    }

    private fun add(
        panel: Panel, window: Int = targetWindow, focusedWindow: Int? = panelWindow,
        exactEvent: Boolean = true, application: Boolean = true, targetFocused: Boolean = false,
        rootMatches: Boolean = true, editorMatches: Boolean = true
    ) = panel.gate.targetAdded(window, focusedWindow, exactEvent, application,
        targetFocused, rootMatches, editorMatches)

    @Test fun presentTargetAllowsIntentionalReturnOnlyOnce() {
        val panel = reviewing()
        assertTrue(add(panel))
        assertFalse(panel.gate.readyForCommit())
        assertTrue(panel.gate.beginReturn(panel.owner))
        assertTrue(panel.gate.readyForCommit())
        assertFalse(panel.gate.beginReturn(panel.owner))
        assertFalse(panel.gate.readyForCommit())
    }

    @Test fun hiddenFinalReturnRequiresOneVerifiedAdditionBeforeCommit() {
        val panel = removed()
        assertTrue(panel.gate.updatePanel(panel.owner, PanelPhase.REVIEW, true, true, panelWindow))
        assertTrue(panel.gate.beginReturn(panel.owner))
        assertFalse(panel.gate.readyForCommit())
        assertTrue(add(panel, focusedWindow = targetWindow, targetFocused = true))
        assertTrue(panel.gate.readyForCommit())
        assertFalse(add(panel, focusedWindow = targetWindow, targetFocused = true))
        assertFalse(panel.gate.readyForCommit())
    }

    @Test fun additionWithoutRemovalCannotQualify() {
        val noRemoval = recording()
        assertTrue(noRemoval.gate.updatePanel(noRemoval.owner, PanelPhase.REVIEW, true, true, panelWindow))
        assertFalse(add(noRemoval))
        assertFalse(noRemoval.gate.readyForCommit())
    }

    @Test fun alternatingCyclesAcrossRecordingAndReviewRequireUnbrokenPanelEvidence() {
        val panel = recording()
        repeat(3) {
            assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
            assertTrue(add(panel))
        }
        assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
        // Changing the UI stage does not reset a hidden interval or its ownership.
        assertTrue(panel.gate.updatePanel(panel.owner, PanelPhase.REVIEW, true, true, panelWindow))
        assertTrue(add(panel))
        repeat(3) {
            assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
            assertTrue(add(panel))
        }
        assertTrue(panel.gate.beginReturn(panel.owner))
        assertTrue(panel.gate.readyForCommit())
    }

    @Test fun removalRequiresExactWindowEventAndAbsentTarget() {
        val variants = listOf(
            Triple(targetWindow + 1, true, true),
            Triple(targetWindow, false, true), // Combined structural/focus/unknown bits.
            Triple(targetWindow, true, false)
        )
        for ((window, exactEvent, absent) in variants) {
            val panel = recording()
            assertFalse(panel.gate.targetRemoved(window, panelWindow, exactEvent, absent))
            assertFalse(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
        }
        for (wrongFocus in listOf(null, targetWindow, panelWindow + 1)) {
            val panel = recording()
            assertFalse(panel.gate.targetRemoved(targetWindow, wrongFocus, true, true))
        }
    }

    @Test fun additionRequiresFreshOriginalRootAndEditorAndNonfocusedApplication() {
        val checks: List<(Panel) -> Boolean> = listOf(
            { add(it, window = targetWindow + 1) },
            { add(it, focusedWindow = null) },
            { add(it, focusedWindow = panelWindow + 1) }, // Another KoScribe window is insufficient.
            { add(it, focusedWindow = targetWindow) },
            { add(it, exactEvent = false) },
            { add(it, application = false) },
            { add(it, targetFocused = true) },
            { add(it, rootMatches = false) }, // Includes missing/unrefreshable root.
            { add(it, editorMatches = false) } // Includes missing/unrefreshable editor.
        )
        for (check in checks) {
            val panel = reviewing()
            assertFalse(check(panel))
            assertFalse(add(panel)) // Later good evidence cannot repair the failed attempt.
        }
    }

    @Test fun finalAdditionNeedsTargetFocusAndCancellationCannotBeRepaired() {
        val confirmed = reviewing()
        assertTrue(confirmed.gate.beginReturn(confirmed.owner))
        assertFalse(add(confirmed))
        val cancelled = reviewing()
        cancelled.gate.invalidate()
        assertFalse(add(cancelled))
        assertFalse(cancelled.gate.beginReturn(cancelled.owner))
    }

    @Test fun everyFinalAdditionRequiresOriginalRootEditorWindowAndSoleTargetFocus() {
        val checks: List<(Panel) -> Boolean> = listOf(
            { add(it, window = targetWindow + 1, focusedWindow = targetWindow, targetFocused = true) },
            { add(it, focusedWindow = null, targetFocused = true) },
            { add(it, focusedWindow = panelWindow, targetFocused = true) },
            { add(it, focusedWindow = targetWindow, targetFocused = false) },
            { add(it, focusedWindow = targetWindow, targetFocused = true, exactEvent = false) },
            { add(it, focusedWindow = targetWindow, targetFocused = true, application = false) },
            { add(it, focusedWindow = targetWindow, targetFocused = true, rootMatches = false) },
            { add(it, focusedWindow = targetWindow, targetFocused = true, editorMatches = false) }
        )
        for (check in checks) {
            val panel = reviewing()
            assertTrue(panel.gate.beginReturn(panel.owner))
            assertFalse(check(panel))
            assertFalse(add(panel, focusedWindow = targetWindow, targetFocused = true))
            assertFalse(panel.gate.readyForCommit())
        }
    }

    @Test fun anyRemovalAfterConfirmationRefusesEvenWithPreviouslyValidCycles() {
        for (hiddenAtConfirmation in listOf(false, true)) {
            val panel = reviewing()
            assertTrue(add(panel))
            if (hiddenAtConfirmation)
                assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
            assertTrue(panel.gate.beginReturn(panel.owner))
            assertFalse(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
            assertFalse(panel.gate.readyForCommit())
        }
    }

    @Test fun duplicateRemovalOrAdditionPermanentlyRefuses() {
        val twiceRemoved = removed()
        assertFalse(twiceRemoved.gate.targetRemoved(targetWindow, panelWindow, true, true))
        assertFalse(twiceRemoved.gate.updatePanel(twiceRemoved.owner, PanelPhase.REVIEW, true, true, panelWindow))
        val twiceAdded = reviewing()
        assertTrue(add(twiceAdded))
        assertFalse(add(twiceAdded))
        assertFalse(twiceAdded.gate.beginReturn(twiceAdded.owner))
    }

    @Test fun changedOrMissingIdentityOnAnyLaterReappearancePermanentlyRefuses() {
        for (validCycles in 0..3) {
            for (rootChanged in listOf(false, true)) {
                val panel = recording()
                repeat(validCycles) {
                    assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
                    assertTrue(add(panel))
                }
                assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
                assertFalse(add(panel, rootMatches = !rootChanged, editorMatches = rootChanged))
                assertFalse(add(panel))
                assertFalse(panel.gate.updatePanel(panel.owner, PanelPhase.REVIEW, true, true, panelWindow))
                assertFalse(panel.gate.beginReturn(panel.owner))
                assertFalse(panel.gate.readyForCommit())
            }
        }
    }

    @Test fun resumedAndFocusedEvidenceAreBothRequired() {
        for ((resumed, focused) in listOf(false to false, false to true, true to false)) {
            val gate = ModalOcclusionGate(targetWindow)
            val owner = Any()
            assertTrue(gate.bind(owner))
            assertTrue(gate.updatePanel(owner, PanelPhase.RECORDING, resumed, focused, panelWindow))
            assertFalse(gate.targetRemoved(targetWindow, panelWindow, true, true))
        }
        val missingWindow = ModalOcclusionGate(targetWindow)
        val owner = Any()
        assertTrue(missingWindow.bind(owner))
        assertTrue(missingWindow.updatePanel(owner, PanelPhase.RECORDING, true, true, -1))
        assertFalse(missingWindow.targetRemoved(targetWindow, panelWindow, true, true))
    }

    @Test fun pauseBeforeFirstFocusCannotLaterArm() {
        val gate = ModalOcclusionGate(targetWindow)
        val owner = Any()
        assertTrue(gate.bind(owner))
        assertTrue(gate.updatePanel(owner, PanelPhase.RECORDING, true, false, panelWindow))
        assertFalse(gate.updatePanel(owner, PanelPhase.RECORDING, false, false, panelWindow, interrupted = true))
        assertFalse(gate.updatePanel(owner, PanelPhase.RECORDING, true, true, panelWindow))
        assertFalse(gate.targetRemoved(targetWindow, panelWindow, true, true))
    }

    @Test fun focusLossAndPauseDuringReviewRemainPoisonedAfterResume() {
        for (duringReview in listOf(false, true)) {
            val panel = if (duringReview) reviewing() else recording()
            val phase = if (duringReview) PanelPhase.REVIEW else PanelPhase.RECORDING
            assertFalse(panel.gate.updatePanel(panel.owner, phase, true, false, panelWindow))
            assertFalse(panel.gate.updatePanel(panel.owner, phase, true, true, panelWindow))
            assertFalse(add(panel))
        }
        val paused = reviewing()
        assertFalse(paused.gate.updatePanel(paused.owner, PanelPhase.REVIEW, false, true, panelWindow, true))
        assertFalse(paused.gate.updatePanel(paused.owner, PanelPhase.REVIEW, true, true, panelWindow))
    }

    @Test fun ownerWindowAndPhaseCannotChangeOrRebind() {
        val changes: List<(Panel) -> Boolean> = listOf(
            { it.gate.updatePanel(Any(), PanelPhase.RECORDING, true, true, panelWindow) },
            { it.gate.updatePanel(it.owner, PanelPhase.RECORDING, true, true, panelWindow + 1) },
            { it.gate.updatePanel(it.owner, PanelPhase.RECORDING, true, true, -1) },
            { it.gate.updatePanel(it.owner, PanelPhase.PREPARING, true, true, panelWindow) },
            { it.gate.updatePanel(it.owner, PanelPhase.RETURNING, true, true, panelWindow) },
            { it.gate.bind(it.owner) },
            { it.gate.bind(Any()) }
        )
        for (change in changes) {
            val panel = recording()
            assertFalse(change(panel))
            assertFalse(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
        }
    }

    @Test fun invalidationBeforeOrAfterReappearanceCannotBeClearedByLifecycleOrReturn() {
        for (afterPair in listOf(false, true)) {
            val panel = reviewing()
            if (afterPair) assertTrue(add(panel))
            panel.gate.invalidate() // Service forwards every content/selection/state/input invalidation.
            assertFalse(panel.gate.updatePanel(panel.owner, PanelPhase.REVIEW, true, true, panelWindow))
            assertFalse(add(panel))
            assertFalse(panel.gate.beginReturn(panel.owner))
        }
    }

    @Test fun visibilityCyclesDoNotExtendExpiryOrAuthorizeChangedCaretContextCancellationOrRepeatCommit() {
        val editor = EditorSnapshot("chat.app", targetWindow, "node-42", "chat.app:id/message",
            "EditText", 21, 1, "beforeafter", 0, 6, 6, false)
        val panel = reviewing()
        assertTrue(add(panel))
        val guard = SessionGuard()
        val session = guard.begin(OutputMode.ENGLISH, editor, 100)!!
        assertTrue(guard.publish(session.id, "sample"))
        repeat(4) {
            assertTrue(panel.gate.targetRemoved(targetWindow, panelWindow, true, true))
            assertTrue(add(panel))
        }
        assertTrue(panel.gate.beginReturn(panel.owner))
        assertTrue(panel.gate.readyForCommit())
        assertEquals(GuardDecision.Refused(GuardFailure.SELECTION_CHANGED),
            guard.check(session.id, editor.copy(selectionStart = 7, selectionEnd = 7), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.CONTEXT_CHANGED),
            guard.check(session.id, editor.copy(surroundingText = "BEFOREafter"), 200))
        assertEquals(GuardDecision.Refused(GuardFailure.EXPIRED), guard.check(session.id, editor, 180_101))
        assertEquals(GuardDecision.Ready, guard.reserveCommit(session.id, editor, 200))
        assertEquals(GuardDecision.Refused(GuardFailure.ALREADY_ATTEMPTED),
            guard.reserveCommit(session.id, editor, 201))
        val cancelled = guard.begin(OutputMode.ENGLISH, editor, 300)!!
        assertTrue(guard.publish(cancelled.id, "sample"))
        guard.cancel(cancelled.id)
        assertEquals(GuardDecision.Refused(GuardFailure.CANCELLED), guard.check(cancelled.id, editor, 301))
    }
}
