package com.koscribe.smoke

/** The REVIEW menu uses descriptive labels; earlier experimental arms use short labels. */
internal fun matchesOwnButtonLabel(actual: CharSequence?, expected: String, ignoreCase: Boolean = true): Boolean {
    val text = actual?.toString() ?: return false
    return text.equals(expected, ignoreCase) || when (expected) {
        "Close" -> text.equals("Close panel", ignoreCase)
        "Hide" -> text.equals("Hide bubble", ignoreCase)
        else -> false
    }
}

internal fun matchesRecordingModeLabel(actual: CharSequence?, mode: String): Boolean {
    val text = actual?.toString() ?: return false
    val badge = when (mode) {
        "요" -> "요 · Polite"
        "반말" -> "반말 · Informal"
        "EN" -> "EN · English"
        else -> return false
    }
    return text == "Selected mode: $mode" || text == badge
}
