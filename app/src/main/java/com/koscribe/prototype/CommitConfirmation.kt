package com.koscribe.prototype

/** Exact local-context proof of one insertion; uncertainty is never success. */
internal object CommitConfirmation {
    enum class PostCommitVerdict { CONFIRMED, COMPOSED_UNCONFIRMED, OTHER_UNCONFIRMED }

    data class Expected(
        val oldOffset: Int,
        val oldText: String,
        val oldCaret: Int,
        val result: String
    ) {
        val beforeCount: Int get() = oldCaret + result.length
        val afterCount: Int get() = oldText.length - oldCaret
        val text: String get() = oldText.substring(0, oldCaret) + result +
            oldText.substring(oldCaret)
        val caret: Int get() = oldCaret + result.length
    }

    fun expected(snapshot: EditorSnapshot, result: String): Expected? {
        if (!snapshot.isSafe() || result.isBlank() || result.length > 512 ||
            result.any { it == '\n' || it == '\r' }) return null
        return Expected(snapshot.offset, snapshot.surroundingText,
            snapshot.selectionStart, result)
    }

    fun matches(expected: Expected, observed: EditorSnapshot?): Boolean =
        observed != null && observed.isStructurallyValid() &&
            observed.offset == expected.oldOffset &&
            observed.surroundingText == expected.text &&
            observed.selectionStart == expected.caret &&
            observed.selectionEnd == expected.caret

    fun verdict(expected: Expected, observed: EditorSnapshot): PostCommitVerdict = when {
        matches(expected, observed) -> PostCommitVerdict.CONFIRMED
        observed.composing -> PostCommitVerdict.COMPOSED_UNCONFIRMED
        else -> PostCommitVerdict.OTHER_UNCONFIRMED
    }
}
