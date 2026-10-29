package com.koscribe.prototype

/** Numeric checks for an explicit diagnostic read. These never authorize insertion. */
internal object KarrotEmptyReadPolicy {
    enum class Result {
        REPORTED_EMPTY_AT_ZERO, NONEMPTY_REJECTED, UNAVAILABLE,
        OFFSET_UNKNOWN, OFFSET_NOT_ZERO, SELECTION_INVALID
    }

    fun permitsPreReadSelection(start: Int, end: Int): Boolean =
        (start == -1 && end == -1) || (start == 0 && end == 0)

    fun classifyPayloadLength(length: Int, inspectEmptyMetadata: () -> Result): Result =
        if (length != 0) Result.NONEMPTY_REJECTED else inspectEmptyMetadata()

    fun classifyEmptyResult(offset: Int, selectionStart: Int, selectionEnd: Int): Result = when {
        offset < 0 -> Result.OFFSET_UNKNOWN
        selectionStart < 0 || selectionEnd < 0 || selectionStart != selectionEnd ->
            Result.SELECTION_INVALID
        offset != 0 -> Result.OFFSET_NOT_ZERO
        selectionStart != 0 -> Result.SELECTION_INVALID
        else -> Result.REPORTED_EMPTY_AT_ZERO
    }
}
