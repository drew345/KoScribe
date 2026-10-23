package com.koscribe.smoke

import android.app.UiAutomation
import androidx.test.runner.AndroidJUnitRunner

/** Preserve KoScribe's accessibility service even if a runner helper uses the no-arg API. */
class KoScribeTestRunner : AndroidJUnitRunner() {
    override fun getUiAutomation(): UiAutomation? =
        super.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    override fun getUiAutomation(flags: Int): UiAutomation? {
        require(flags == 0 || flags == UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES) {
            "UNSUPPORTED_UI_AUTOMATION_FLAGS"
        }
        return super.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    }
}
