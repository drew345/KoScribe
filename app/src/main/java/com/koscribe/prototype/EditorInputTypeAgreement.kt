package com.koscribe.prototype

import android.text.InputType

/** Compare the focused node's type with the served EditorInfo type, in that direction only. */
internal object EditorInputTypeAgreement {
    // API 37 TextView.onCreateInputConnection may add this to EditorInfo while
    // AccessibilityNodeInfo still exposes the original view type. Compile SDK 36
    // does not define the constant.
    // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-17.0.0_r1/core/java/android/widget/TextView.java#10172
    private const val TEXT_SUGGESTION_SELECTED = 0x00200000

    fun matches(nodeType: Int, editorInfoType: Int, sdkInt: Int): Boolean {
        if (nodeType == editorInfoType) return true
        if (sdkInt < 37 || !safeText(nodeType) || !safeText(editorInfoType)) return false
        return (nodeType and TEXT_SUGGESTION_SELECTED) == 0 &&
            editorInfoType == (nodeType or TEXT_SUGGESTION_SELECTED)
    }

    private fun safeText(type: Int): Boolean {
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (type and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> false
            else -> true
        }
    }
}
