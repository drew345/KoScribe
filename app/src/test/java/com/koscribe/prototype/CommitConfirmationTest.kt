package com.koscribe.prototype

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitConfirmationTest {
    private fun snapshot(text: String, caret: Int, offset: Int = 10) = EditorSnapshot(
        "fixture", 1, "node", "view", "EditText", 2, 1,
        text, offset, caret, caret, false
    )

    @Test fun exactContextAndCaretAreRequiredForSuccess() {
        val expected = CommitConfirmation.expected(snapshot("abCD", 2), "한글")
        assertNotNull(expected)
        assertTrue(CommitConfirmation.matches(expected!!, snapshot("ab한글CD", 4)))
        assertFalse(CommitConfirmation.matches(expected, snapshot("abCD", 2)))
        assertFalse(CommitConfirmation.matches(expected, snapshot("ab한글CD", 3)))
        assertFalse(CommitConfirmation.matches(expected, snapshot("ab한글CD", 4, offset = 9)))
        assertFalse(CommitConfirmation.matches(expected, snapshot("ab한글CE", 4)))
    }

    @Test fun utf16AndLongResultNeedTheWholeWidenedContext() {
        val result = "🙂" + "가".repeat(80)
        val old = snapshot("left🙂right", 6)
        val expected = CommitConfirmation.expected(old, result)!!
        assertEquals(6 + result.length, expected.beforeCount)
        assertEquals(5, expected.afterCount)
        assertTrue(CommitConfirmation.matches(expected,
            snapshot("left🙂${result}right", 6 + result.length)))
        assertFalse(CommitConfirmation.matches(expected,
            snapshot("left🙂${result.dropLast(1)}right", 6 + result.length - 1)))
        assertNull(CommitConfirmation.expected(old, "x".repeat(513)))
        assertNull(CommitConfirmation.expected(old, " "))
        assertNull(CommitConfirmation.expected(old, "first\nsecond"))
    }
}
