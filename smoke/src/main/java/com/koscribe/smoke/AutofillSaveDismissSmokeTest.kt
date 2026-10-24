package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** One exact, user-authorized dismissal of the Android autofill save prompt. */
@RunWith(AndroidJUnit4::class)
class AutofillSaveDismissSmokeTest {
    private val negativeId = "android:id/autofill_save_no"

    @Test fun dismissVisibleAndroidAutofillSaveNoOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val otherFlags = listOf("externalDraftReady", "externalRecordCancelReady",
            "startupHoldReady", "startupCollapseReady", "startupPanelReady", "overlayReady",
            "reselectReady", "bubbleIdleReady", "bubbleMicReady", "bubbleRestoreReady",
            "reviewCopyReady", "reviewInsertEmptyReady", "reviewInsertMiddleReady",
            "reviewKoreanReady", "reviewSelectionReady", "reviewKoreanEditReady",
            "reviewUncertainReady", "reviewConfirmationProbeReady")
        assumeTrue("AUTOFILL_DISMISS_REQUIRES_SOLE_EXACT_OPT_IN",
            args.getString("autofillSaveNoReady") == "true" &&
                otherFlags.none { args.getString(it) == "true" })
        val ui = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info

        val initial = exactNegativeCandidates(ui)
        if (initial.size != 1 || !eligible(initial.single())) {
            report(instrumentation, "NOT_FOUND")
            return
        }
        val button = initial.single()
        if (!button.refresh() || !eligible(button)) {
            report(instrumentation, "NOT_FOUND")
            return
        }
        val fresh = exactNegativeCandidates(ui)
        if (fresh.size != 1 || fresh.single() != button || !eligible(fresh.single())) {
            report(instrumentation, "NOT_FOUND")
            return
        }
        val accepted = fresh.single().performAction(AccessibilityNodeInfo.ACTION_CLICK)
        report(instrumentation, if (accepted) "CLICK_ACCEPTED" else "CLICK_NOT_ACCEPTED")
        assertTrue("AUTOFILL_NEGATIVE_CLICK_NOT_ACCEPTED", accepted)
    }

    private fun exactNegativeCandidates(ui: UiAutomation): List<AccessibilityNodeInfo> =
        ui.windows.asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_SYSTEM }
            .mapNotNull { it.getRoot(0) }
            .filter { it.packageName?.toString() == "android" }
            .flatMap { it.findAccessibilityNodeInfosByViewId(negativeId).asSequence() }
            .filter { it.packageName?.toString() == "android" &&
                it.viewIdResourceName == negativeId }
            .toList()

    private fun eligible(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser || !node.isEnabled || !node.isClickable) return false
        val labels = listOfNotNull(node.text, node.contentDescription).map {
            it.toString().lowercase(Locale.ROOT).replace(Regex("[^a-z]"), "")
        }
        return labels.none { it == "never" } && labels.any {
            it == "nothanks" || it == "nothankyou" || it == "notnow"
        }
    }

    private fun report(instrumentation: Instrumentation, code: String) {
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_autofill_negative", code)
        })
    }
}
