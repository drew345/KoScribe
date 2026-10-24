package com.koscribe.prototype

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitConfirmationTest {
    private fun snapshot(text: String, caret: Int, offset: Int = 10,
        end: Int = caret, composing: Boolean = false) = EditorSnapshot(
        "fixture", 1, "node", "view", "EditText", 2, 1,
        text, offset, caret, end, composing
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

    @Test fun composedPostCommitExactMiddleInsertionCanConfirmButCannotBePrepared() {
        val old = snapshot("앞🙂뒤", 3)
        val result = "한글🙂"
        val expected = CommitConfirmation.expected(old, result)!!
        val composed = snapshot("앞🙂${result}뒤", 3 + result.length, composing = true)
        assertFalse(composed.isSafe())
        assertTrue(composed.isStructurallyValid())
        assertNull(CommitConfirmation.expected(composed, "again"))
        assertTrue(CommitConfirmation.matches(expected, composed))
        assertEquals(CommitConfirmation.PostCommitVerdict.CONFIRMED,
            CommitConfirmation.verdict(expected, composed))
    }

    @Test fun composedPostCommitNeedsFullContextOffsetAndCollapsedCaret() {
        val old = snapshot("prefix🙂suffix", 8)
        val result = "insert"
        val expected = CommitConfirmation.expected(old, result)!!
        val exact = snapshot("prefix🙂${result}suffix", 8 + result.length,
            composing = true)
        assertTrue(CommitConfirmation.matches(expected, exact))
        val changed = listOf(
            exact.copy(surroundingText = "Prefix🙂${result}suffix"),
            exact.copy(surroundingText = "prefix🙂${result}Suffix"),
            exact.copy(surroundingText = "prefix🙂${result.dropLast(1)}suffix"),
            exact.copy(offset = 9),
            exact.copy(selectionStart = exact.selectionStart - 1,
                selectionEnd = exact.selectionEnd - 1),
            exact.copy(selectionEnd = exact.selectionEnd - 1),
            exact.copy(selectionStart = -1, selectionEnd = -1),
            exact.copy(selectionStart = exact.surroundingText.length + 1,
                selectionEnd = exact.surroundingText.length + 1),
            exact.copy(uniqueNodeId = ""),
            exact.copy(windowId = -1)
        )
        changed.forEach {
            assertFalse(CommitConfirmation.matches(expected, it))
            assertEquals(CommitConfirmation.PostCommitVerdict.COMPOSED_UNCONFIRMED,
                CommitConfirmation.verdict(expected, it))
        }
        assertEquals(CommitConfirmation.PostCommitVerdict.OTHER_UNCONFIRMED,
            CommitConfirmation.verdict(expected, exact.copy(composing = false,
                surroundingText = "prefix🙂wrong suffix")))
    }
}
