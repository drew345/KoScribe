package com.koscribe.prototype

/** The only accepted unknown node caret is an independently verified empty editor at zero. */
internal object EditorCaretAgreement {
    fun matches(
        nodeStart: Int,
        nodeEnd: Int,
        surroundingOffset: Int,
        surroundingStart: Int,
        surroundingEnd: Int,
        surroundingTextEmpty: Boolean,
        nodeEmptyOrShowingHint: Boolean
    ): Boolean {
        if (surroundingOffset < 0 || surroundingStart < 0 || surroundingStart != surroundingEnd)
            return false
        val caret = surroundingOffset.toLong() + surroundingStart
        if (caret > Int.MAX_VALUE) return false
        if (nodeStart.toLong() == caret && nodeEnd.toLong() == caret) return true
        return nodeStart == -1 && nodeEnd == -1 && surroundingOffset == 0 &&
            surroundingStart == 0 && surroundingEnd == 0 && surroundingTextEmpty &&
            nodeEmptyOrShowingHint
    }
}
