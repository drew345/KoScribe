package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** One missing-key refusal check using only KoScribe controls and its own empty fixture. */
@RunWith(AndroidJUnit4::class)
class SpeechSetupSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val noKey = "No key is saved on this device."
    private val savedKey = "A key is saved on this device."
    private val unusableKey = "A saved key entry is unusable. Delete it before saving a new key."
    private val missingNotice = "API key missing. Open KoScribe > API key settings."
    private lateinit var instrumentation: Instrumentation
    private lateinit var ui: UiAutomation

    private data class State(val arm: String, val proof: Int, val captures: Int,
        val commits: Int, val draft: String, val cleanup: Boolean)

    @Before fun requireSoleMissingKeyScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val otherFlags = listOf("credentialFakeOnlyReady", "autofillSaveNoReady",
            "externalDraftReady", "externalRecordCancelReady", "startupHoldReady",
            "startupCollapseReady", "startupPanelReady", "overlayReady", "reselectReady",
            "bubbleIdleReady", "bubbleMicReady", "bubbleRestoreReady", "reviewCopyReady",
            "reviewInsertEmptyReady", "reviewInsertMiddleReady", "reviewKoreanReady",
            "reviewSelectionReady", "reviewKoreanEditReady", "reviewUncertainReady",
            "reviewConfirmationProbeReady", "reviewComposedExactReady", "reviewComposedMismatchReady")
        assumeTrue("MISSING_KEY_TEST_REQUIRES_SOLE_EXACT_OPT_IN",
            args.getString("speechMissingKeyReady") == "true" &&
                otherFlags.none { args.getString(it) == "true" })
        assertTrue("WRONG_SMOKE_PACKAGE", instrumentation.context.packageName == fixture)
        assertTrue("SWIFTKEY_NOT_SELECTED", Settings.Secure.getString(
            instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey)
        ui = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
    }

    @Test fun missingKeyRefusesBeforeRecording() {
        openSettings()
        val initial = await("MISSING_KEY_INITIAL_STATUS_TIMEOUT") { settledKeyStatus() }
        // Never modify an existing usable, unusable, or otherwise ambiguous entry.
        assumeTrue("EXISTING_CREDENTIAL_ENTRY_REFUSED", initial == noKey)
        clickOwnId("api_key_close")
        await("SETTINGS_DID_NOT_CLOSE") { (ownId("api_key_status") == null).takeIf { it } }

        instrumentation.context.startActivity(Intent(instrumentation.context,
            DebugFixtureLauncherActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        await("MANUAL_FIXTURE_NOT_OPEN") {
            fixtureEditor()?.takeIf { it.isVisibleToUser && !it.isPassword }
        }
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
                ?: throw AssertionError("OWN_FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            editor.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        await("FIXTURE_NOT_FOCUSED") { fixtureEditor()?.takeIf { it.isFocused } }
        await("SWIFTKEY_NOT_VISIBLE") { ui.windows.any {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }.takeIf { it } }
        await("IDLE_BUBBLE_NOT_VISIBLE") { ownButton("Ko") }

        val before = state()
        assertTrue("WRONG_SPEECH_TEST_ARM", before.arm == "REVIEW")
        assertTrue("PREEXISTING_DRAFT_OR_CLEANUP", before.draft == "NONE" && !before.cleanup)
        clickOwnButton("Ko")
        await("SPEECH_MODE_NOT_VISIBLE") { ownButton("EN") }
        clickOwnButton("EN")
        await("MISSING_KEY_NOTICE_NOT_VISIBLE") { ownText(missingNotice) }
        SystemClock.sleep(300) // Let an incorrectly started recorder expose its controls/state.

        val after = state()
        assertTrue("MISSING_KEY_STARTED_RECORDING_OR_DESTINATION",
            after == before && ownButton("Stop") == null && ownId("review_english") == null &&
                ownId("review_confirm") == null)
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_speech_setup", "MISSING_KEY_REFUSED_BEFORE_RECORDING")
        })
        clickOwnClose()
    }

    @After fun closeOnlyOwnControls() {
        if (!::ui.isInitialized) return
        runCatching {
            ownId("api_key_close")?.takeIf { it.isEnabled && it.isClickable }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            // If a regression started recording, use its explicit own Cancel control.
            ownButton("Cancel")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (ownOverlayScroll() != null) clickOwnClose()
        }
    }

    private fun openSettings() {
        if (ownId("api_key_status") != null) return
        val descriptor = ui.executeShellCommand("am start -n $product/.MainActivity")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        val landing = await("MISSING_KEY_MAIN_OR_SETTINGS_NOT_VISIBLE") {
            when {
                ownId("api_key_status") != null -> "SETTINGS"
                ownId("open_api_key_settings") != null -> "MAIN"
                else -> null
            }
        }
        if (landing == "MAIN") clickOwnId("open_api_key_settings")
        await("API_KEY_SETTINGS_NOT_OPEN") { ownId("api_key_status") }
    }

    private fun settledKeyStatus(): String? = ownId("api_key_status")?.text?.toString()?.takeIf {
        it == noKey || it == savedKey || it == unusableKey
    }

    private fun fixtureEditor(): AccessibilityNodeInfo? = nodes(fixture).filter {
        it.viewIdResourceName == "$fixture:id/local_test_editor" && it.isEditable
    }.singleOrNull()

    private fun ownId(id: String): AccessibilityNodeInfo? = nodes(product).filter {
        it.viewIdResourceName == "$product:id/$id" && it.isVisibleToUser
    }.singleOrNull()

    private fun ownButton(label: String): AccessibilityNodeInfo? = nodes(product).filter {
        it.className?.toString() == "android.widget.Button" && it.isVisibleToUser &&
            it.isEnabled && it.isClickable && matchesOwnButtonLabel(it.text, label, ignoreCase = false)
    }.singleOrNull()

    private fun ownCloseCandidate(): AccessibilityNodeInfo? {
        val matches = nodes(product).filter {
            it.className?.toString() == "android.widget.Button" && it.isEnabled &&
                it.isClickable && matchesOwnButtonLabel(it.text, "Close", ignoreCase = false)
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_CLOSE")
        return matches.singleOrNull()
    }

    /** Only the current KoScribe accessibility overlay may be scrolled for cleanup. */
    private fun ownOverlayScroll(): AccessibilityNodeInfo? {
        val roots = ui.windows.asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .mapNotNull { it.getRoot(0) }
            .filter { it.packageName?.toString() == product }.toList()
        if (roots.size > 1) throw AssertionError("AMBIGUOUS_OWN_OVERLAY_WINDOW")
        val root = roots.singleOrNull() ?: return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        val matches = ArrayList<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.packageName?.toString() != product) continue
            if (node.className?.toString() == "android.widget.ScrollView" &&
                node.isVisibleToUser && node.isEnabled) matches.add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_OVERLAY_SCROLL")
        return matches.singleOrNull()
    }

    private fun ownText(exact: String): AccessibilityNodeInfo? = nodes(product).filter {
        it.className?.toString() == "android.widget.TextView" && it.isVisibleToUser &&
            it.text?.toString() == exact
    }.singleOrNull()

    private fun clickOwnId(id: String) {
        val node = await("OWN_CONTROL_NOT_FOUND_$id") {
            ownId(id)?.takeIf { it.isEnabled && it.isClickable }
        }
        assertTrue("OWN_CONTROL_CLICK_REFUSED_$id", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun clickOwnButton(label: String) {
        val node = await("OWN_BUTTON_NOT_FOUND_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_REFUSED_$label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun clickOwnClose() {
        val originalScroll = ownOverlayScroll() ?: throw AssertionError("OWN_OVERLAY_SCROLL_NOT_FOUND")
        val windowId = originalScroll.windowId
        repeat(4) { scrollCount ->
            val candidate = ownCloseCandidate()
            if (candidate != null && candidate.windowId != windowId)
                throw AssertionError("OWN_CLOSE_WRONG_WINDOW")
            if (candidate != null && !candidate.isVisibleToUser)
                candidate.performAction(
                    AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
            val visible = ownCloseCandidate()?.takeIf { it.windowId == windowId &&
                it.isVisibleToUser && it.isEnabled && it.isClickable }
            if (visible != null) {
                assertTrue("OWN_CLOSE_CLICK_REFUSED",
                    visible.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                return
            }
            if (scrollCount == 3) throw AssertionError("OWN_CLOSE_NOT_VISIBLE_AFTER_SCROLL")
            val scroll = ownOverlayScroll()?.takeIf { it.windowId == windowId }
                ?: throw AssertionError("OWN_OVERLAY_CHANGED_DURING_SCROLL")
            assertTrue("OWN_OVERLAY_SCROLL_REFUSED", scroll.isScrollable &&
                scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            SystemClock.sleep(180)
        }
    }

    private fun state(): State {
        val descriptor = ui.executeShellCommand("dumpsys activity service $product/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(128).map(String::trim).toList()
        }
        fun field(name: String): String = lines.firstOrNull { it.startsWith("$name: ") }
            ?.substringAfter(": ")?.trim()?.takeIf { Regex("[A-Za-z0-9_]+").matches(it) }
            ?: throw AssertionError("OWN_SERVICE_STATE_UNAVAILABLE_$name")
        fun count(name: String): Int = field(name).toIntOrNull()?.takeIf { it in 0..100_000 }
            ?: throw AssertionError("OWN_SERVICE_COUNT_INVALID_$name")
        return State(field("Experiment arm"), count("Unbound recording proof count"),
            count("Destination capture count"), count("Commit request count"),
            field("Draft phase"), field("Overlay cleanup pending") == "true")
    }

    private fun nodes(requested: String): List<AccessibilityNodeInfo> {
        require(requested == product || requested == fixture)
        val result = ArrayList<AccessibilityNodeInfo>()
        for (window in ui.windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != requested) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node.packageName?.toString() != requested) continue
                result.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
            }
        }
        return result
    }

    private fun <T : Any> await(code: String, probe: () -> T?): T {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (SystemClock.elapsedRealtime() < deadline) {
            probe()?.let { return it }
            SystemClock.sleep(80)
        }
        throw AssertionError(code)
    }
}
