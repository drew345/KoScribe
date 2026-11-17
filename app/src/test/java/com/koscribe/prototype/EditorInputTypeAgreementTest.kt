package com.koscribe.prototype

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInputTypeAgreementTest {
    private val a11yFlag = 0x00200000
    private val text = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE

    @Test fun acceptsExactTypesAndOnlyTheDirectionalApi37TextViewAddition() {
        assertTrue(EditorInputTypeAgreement.matches(text, text, 36))
        assertTrue(EditorInputTypeAgreement.matches(text, text, 37))
        assertTrue(EditorInputTypeAgreement.matches(text, text or a11yFlag, 37))
        val email = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        assertTrue(EditorInputTypeAgreement.matches(email, email or a11yFlag, 37))
    }

    @Test fun refusesEarlierApisReverseDirectionAndEveryOtherDifference() {
        assertFalse(EditorInputTypeAgreement.matches(text, text or a11yFlag, 36))
        assertFalse(EditorInputTypeAgreement.matches(text or a11yFlag, text, 37))
        assertFalse(EditorInputTypeAgreement.matches(text,
            text or a11yFlag or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 37))
        assertFalse(EditorInputTypeAgreement.matches(text,
            (text xor InputType.TYPE_TEXT_FLAG_MULTI_LINE) or a11yFlag, 37))
        assertFalse(EditorInputTypeAgreement.matches(text,
            InputType.TYPE_CLASS_NUMBER or a11yFlag, 37))
        assertFalse(EditorInputTypeAgreement.matches(text,
            text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or a11yFlag, 37))
    }

    @Test fun refusesProtectedAndNonTextTypesEvenWithTheSingleFlagDifference() {
        for (variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) {
            val protected = InputType.TYPE_CLASS_TEXT or variation
            assertFalse(EditorInputTypeAgreement.matches(protected, protected or a11yFlag, 37))
        }
        assertFalse(EditorInputTypeAgreement.matches(InputType.TYPE_NULL, a11yFlag, 37))
        assertFalse(EditorInputTypeAgreement.matches(InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_NUMBER or a11yFlag, 37))
    }
}
