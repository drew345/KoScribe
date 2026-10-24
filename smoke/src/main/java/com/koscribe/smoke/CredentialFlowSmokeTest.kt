package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Own-app UI only. The marker is deliberately not an API credential and is never sent. */
@RunWith(AndroidJUnit4::class)
class CredentialFlowSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fakeValue = "KOSCRIBE_FAKE_TEST_VALUE_20261024"
    private val emptyStatus = "No key is saved on this device."
    private val savedStatus = "A key is saved on this device."
    private val saveConfirmedStatus = "Key saved on this device."
    private val unusableStatus = "A saved key entry is unusable. Delete it before saving a new key."
    private lateinit var instrumentation: Instrumentation
    private lateinit var ui: UiAutomation
    private var mayHaveCreatedFake = false

    @Before fun requireSoleCredentialScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val otherFlags = listOf("autofillSaveNoReady", "externalDraftReady", "externalRecordCancelReady",
            "startupHoldReady", "startupCollapseReady", "startupPanelReady", "overlayReady",
            "reselectReady", "bubbleIdleReady", "bubbleMicReady", "bubbleRestoreReady",
            "reviewCopyReady", "reviewInsertEmptyReady", "reviewInsertMiddleReady", "reviewKoreanReady",
            "reviewSelectionReady", "reviewKoreanEditReady", "reviewUncertainReady",
            "reviewConfirmationProbeReady", "reviewComposedExactReady", "reviewComposedMismatchReady")
        assumeTrue("CREDENTIAL_TEST_REQUIRES_SOLE_EXACT_OPT_IN",
            args.getString("credentialFakeOnlyReady") == "true" &&
                otherFlags.none { args.getString(it) == "true" })
        assertTrue("WRONG_SMOKE_PACKAGE", instrumentation.context.packageName == "com.koscribe.smoke")
        ui = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
    }

    @Test fun fakeKeyCanBeSavedWithoutReadbackThenDeleted() {
        openSettings()
        val initial = await("CREDENTIAL_INITIAL_STATUS_TIMEOUT") { settledStatus() }
        // A damaged entry is also occupied. Never overwrite or delete an entry found before this test.
        assumeTrue("PREEXISTING_CREDENTIAL_ENTRY_REFUSED", initial == emptyStatus)
        assertBlankAndMasked()

        val entry = await("CREDENTIAL_ENTRY_NOT_VISIBLE") { ownId("api_key_entry") }
        val text = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fakeValue)
        }
        assertTrue("FAKE_CREDENTIAL_INPUT_REFUSED",
            entry.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text))
        mayHaveCreatedFake = true // Save may finish even if the test is interrupted afterward.
        click("api_key_save")
        await("FAKE_CREDENTIAL_SAVE_NOT_CONFIRMED") {
            (status() == saveConfirmedStatus).takeIf { it }
        }
        assertBlankAndMasked()

        click("api_key_close")
        await("CREDENTIAL_SETTINGS_DID_NOT_CLOSE") {
            (ownId("api_key_status") == null).takeIf { it }
        }
        openSettings()
        await("SAVED_CREDENTIAL_STATUS_NOT_RESTORED") { (status() == savedStatus).takeIf { it } }
        assertBlankAndMasked() // The saved credential must never be prefilled or displayed.

        click("api_key_delete")
        await("FAKE_CREDENTIAL_DELETE_NOT_CONFIRMED") {
            (status() == "Saved key deleted.").takeIf { it }
        }
        click("api_key_close")
        await("CREDENTIAL_SETTINGS_DID_NOT_CLOSE") {
            (ownId("api_key_status") == null).takeIf { it }
        }
        openSettings()
        await("DELETED_CREDENTIAL_REAPPEARED") { (status() == emptyStatus).takeIf { it } }
        assertBlankAndMasked()
        mayHaveCreatedFake = false
        click("api_key_close")
        await("CREDENTIAL_SETTINGS_DID_NOT_CLOSE") {
            (ownId("api_key_status") == null).takeIf { it }
        }
    }

    @After fun cleanUpOnlyTestCreatedEntry() {
        if (!mayHaveCreatedFake || !::ui.isInitialized) return
        // The exact empty-state precondition and our only Save action establish provenance.
        runCatching {
            if (ownId("api_key_delete") == null) openSettings()
            await("FAKE_CREDENTIAL_CLEANUP_CONTROL_TIMEOUT") {
                ownId("api_key_delete")?.takeIf { it.isEnabled }
            }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            await("FAKE_CREDENTIAL_CLEANUP_NOT_CONFIRMED") {
                (status() == "Saved key deleted.").takeIf { it }
            }
            mayHaveCreatedFake = false
        }.getOrElse { throw AssertionError("FAKE_CREDENTIAL_CLEANUP_FAILED") }
    }

    private fun openSettings() {
        if (ownId("api_key_status") != null) return
        val descriptor = ui.executeShellCommand("am start -n $product/.MainActivity")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        val landing = await("CREDENTIAL_MAIN_OR_SETTINGS_NOT_VISIBLE") {
            when {
                ownId("api_key_status") != null -> "SETTINGS"
                ownId("open_api_key_settings") != null -> "MAIN"
                else -> null
            }
        }
        if (landing == "MAIN") click("open_api_key_settings")
        await("CREDENTIAL_SETTINGS_NOT_OPEN") { ownId("api_key_status") }
    }

    private fun click(id: String) {
        val node = await("CREDENTIAL_CONTROL_NOT_FOUND_$id") { ownId(id)?.takeIf { it.isEnabled } }
        assertTrue("CREDENTIAL_CONTROL_CLICK_REFUSED_$id",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun assertBlankAndMasked() {
        val field = await("CREDENTIAL_FIELD_NOT_VISIBLE") { ownId("api_key_entry") }
        assertTrue("CREDENTIAL_ENTRY_NOT_PASSWORD", field.isPassword)
        assertTrue("CREDENTIAL_ENTRY_PREFILLED_OR_NOT_CLEARED",
            field.text.isNullOrEmpty() ||
                (field.isShowingHintText && field.text?.toString() == "API key"))
    }

    private fun settledStatus(): String? = status()?.takeIf {
        it == emptyStatus || it == savedStatus || it == unusableStatus
    }

    private fun status(): String? = ownId("api_key_status")?.text?.toString()

    private fun ownId(id: String): AccessibilityNodeInfo? = ui.windows.asSequence()
        .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        .mapNotNull { it.getRoot(0) }
        .filter { it.packageName?.toString() == product }
        .flatMap { it.findAccessibilityNodeInfosByViewId("$product:id/$id").asSequence() }
        .filter { it.packageName?.toString() == product && it.isVisibleToUser }
        .singleOrNull()

    private fun <T : Any> await(code: String, block: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            block()?.let { return it }
            SystemClock.sleep(80)
        }
        throw AssertionError(code)
    }
}
