package com.koscribe.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleBubblePolicyTest {
    @Test fun hideSurvivesAmbiguityAndActiveOwnerUntilVerifiedImeCycle() {
        val policy = IdleBubblePolicy()
        assertTrue(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, true, true, false))
        policy.hideUntilNextImeCycle()
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, true, true, false))
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.UNKNOWN, false, true, false))
        assertTrue(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, false, false, true))
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, true, true, false))
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.ABSENT, false, true, false))
        assertTrue(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, true, true, false))
    }

    @Test fun ordinaryUnlockedInputAndVisibleImeAreAllRequired() {
        val policy = IdleBubblePolicy()
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, false, true, false))
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.VISIBLE, true, false, false))
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.ABSENT, true, true, false))
        assertFalse(policy.shouldShow(IdleBubblePolicy.Ime.UNKNOWN, true, true, false))
    }

    @Test fun dragAnchorClampsAcrossResizesWithoutMovingSavedValue() {
        val area = BubblePlacement.Area(10, 20, 410, 700)
        val dragged = BubblePlacement.Point(390, 690)
        val anchor = BubblePlacement.anchor(dragged, 56, 48, area)
        assertEquals(BubblePlacement.Point(354, 652), BubblePlacement.resolve(anchor, 56, 48, area))
        val keyboardArea = BubblePlacement.Area(10, 20, 410, 380)
        assertEquals(BubblePlacement.Point(150, 332),
            BubblePlacement.resolve(anchor, 260, 48, keyboardArea))
        assertEquals(BubblePlacement.Point(354, 652), BubblePlacement.resolve(anchor, 56, 48, area))
    }
}
