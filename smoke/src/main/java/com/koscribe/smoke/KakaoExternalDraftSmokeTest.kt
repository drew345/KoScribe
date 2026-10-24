package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
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

/** One opt-in, unsent draft check in the user-prepared KakaoTalk self-chat. */
@RunWith(AndroidJUnit4::class)
class KakaoExternalDraftSmokeTest {
    private val productPackage = "com.koscribe.prototype"
    private val kakaoPackage = "com.kakao.talk"
    private val swiftKeyPackage = "com.touchtype.swiftkey"
    private val swiftKeyComponent = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val politeSample = "안녕하세요. 10분 후에 도착할게요."
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false

    private data class Target(
        val windowId: Int,
        val root: AccessibilityNodeInfo,
        val node: AccessibilityNodeInfo,
        val resourceId: String?,
        val className: String?,
        val uniqueId: String?,
        val inputType: Int
    )

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExplicitReadyState() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val draftReady = InstrumentationRegistry.getArguments().getString("externalDraftReady") == "true"
        val recordCancelReady =
            InstrumentationRegistry.getArguments().getString("externalRecordCancelReady") == "true"
        val startupReady = listOf("startupHoldReady", "startupCollapseReady", "startupPanelReady",
            "overlayReady", "reselectReady", "bubbleIdleReady", "bubbleMicReady",
            "bubbleRestoreReady", "reviewCopyReady", "reviewInsertEmptyReady",
            "reviewInsertMiddleReady", "reviewKoreanReady", "reviewSelectionReady",
            "reviewKoreanEditReady", "reviewUncertainReady", "reviewConfirmationProbeReady",
            "autofillSaveNoReady")
            .any { InstrumentationRegistry.getArguments().getString(it) == "true" }
        assumeTrue("EXTERNAL_TEST_REQUIRES_ONE_EXPLICIT_OPT_IN",
            draftReady.xor(recordCancelReady) && !startupReady)
        assertTrue("WRONG_INSTRUMENTATION_PACKAGE",
            instrumentation.context.packageName == "com.koscribe.smoke")
        assertTrue("MIC_PERMISSION_REQUIRED",
            instrumentation.context.packageManager.checkPermission(
                Manifest.permission.RECORD_AUDIO, productPackage) == PackageManager.PERMISSION_GRANTED)
        assertTrue("SWIFTKEY_NOT_SELECTED", swiftKeySelected())
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
    }

    @After fun cancelOnlyOwnPanelOnFailure() {
        if (!::instrumentation.isInitialized || !sessionAttempted) return
        // Never navigate, tap, select, or type in KakaoTalk during cleanup.
        runCatching { if (uniqueOwnButton("Cancel") != null) clickOwnButton("Cancel") }
    }

    @Test fun kakaoEmptyComposerPoliteFixedSampleOnce() {
        assumeTrue("DRAFT_TEST_REQUIRES_DRAFT_OPT_IN",
            InstrumentationRegistry.getArguments().getString("externalDraftReady") == "true")
        val original = captureEmptyFocusedKakaoEditor()
        assertTrue("OTHER_PRODUCT_SESSION_OPEN",
            ownButtons("Stop").isEmpty() && ownButtons("Cancel").isEmpty())
        await("BUBBLE_NOT_AVAILABLE") {
            (ownButtons("Ko").size + ownButtons("Close").size == 1).takeIf { it }
        }
        if (ownButtons("Close").size == 1) clickOwnButton("Close")
        clickOwnButton("Ko")
        assertSameIdentity(original, captureEmptyFocusedKakaoEditor())
        sessionAttempted = true
        clickOwnButton("요")
        await("FRESH_RECORDING_CONTROL_NOT_VISIBLE") { uniqueOwnButton("Stop", visibleOnly = true) }
        SystemClock.sleep(2_000)
        clickOwnButton("Stop")
        awaitOwnButton("Translate & insert sample")
        SystemClock.sleep(750)
        clickOwnButton("Translate & insert sample")
        await("REVIEW_DID_NOT_FINISH") {
            (ownButtons("Translate & insert sample").isEmpty() &&
                ownButtons("Cancel").isEmpty()).takeIf { it }
        }
        await("KAKAO_DID_NOT_RETURN") { focusedKakaoEditor() }
        await("KAKAO_RESULT_NOT_EXACT") {
            val returned = focusedKakaoEditor() ?: return@await null
            assertSameIdentity(original, returned)
            if (!returned.node.isShowingHintText && returned.node.text?.toString() == politeSample) {
                assertTrue("KAKAO_RESULT_SELECTION_UNKNOWN", knownCollapsedSelection(returned.node))
                true
            } else {
                assertTrue("KAKAO_UNEXPECTED_DRAFT_CONTENT", emptyOrHint(returned.node))
                assertTrue("KAKAO_EMPTY_WAIT_CARET_CHANGED", initialEmptyNodeCaret(returned.node))
                null
            }
        }
        await("SWIFTKEY_NOT_VISIBLE_ON_RETURN", 4_000) {
            swiftKeySelectedAndVisible().takeIf { it }
        }
        SystemClock.sleep(500)
        val finalEditor = focusedKakaoEditor() ?: throw AssertionError("KAKAO_TARGET_LOST")
        assertSameIdentity(original, finalEditor)
        assertTrue("SWIFTKEY_CHANGED_ON_RETURN", swiftKeySelectedAndVisible())
        assertTrue("KAKAO_RESULT_CHANGED_OR_DUPLICATED",
            !finalEditor.node.isShowingHintText && finalEditor.node.text?.toString() == politeSample)
        assertTrue("KAKAO_RESULT_SELECTION_UNKNOWN", knownCollapsedSelection(finalEditor.node))
        sessionAttempted = false
    }

    @Test fun kakaoEmptyComposerPoliteRecordCancelPreservesEmptyDraft() {
        assumeTrue("RECORD_CANCEL_TEST_REQUIRES_OWN_OPT_IN",
            InstrumentationRegistry.getArguments().getString("externalRecordCancelReady") == "true")
        val original = captureEmptyFocusedKakaoEditor()
        assertTrue("OTHER_PRODUCT_SESSION_OPEN",
            ownButtons("Stop").isEmpty() && ownButtons("Cancel").isEmpty())
        await("BUBBLE_NOT_AVAILABLE") {
            (ownButtons("Ko").size + ownButtons("Close").size == 1).takeIf { it }
        }
        if (ownButtons("Close").size == 1) clickOwnButton("Close")
        clickOwnButton("Ko")
        assertSameIdentity(original, captureEmptyFocusedKakaoEditor())
        sessionAttempted = true
        clickOwnButton("요")
        await("FRESH_RECORDING_CONTROL_NOT_VISIBLE") { uniqueOwnButton("Stop", visibleOnly = true) }
        SystemClock.sleep(2_000)
        clickOwnButton("Stop")
        awaitOwnButton("Translate & insert sample")
        clickOwnButton("Cancel")
        await("REVIEW_DID_NOT_CANCEL") {
            (ownButtons("Cancel").isEmpty() &&
                ownButtons("Translate & insert sample").isEmpty()).takeIf { it }
        }
        await("KAKAO_DID_NOT_RETURN") { focusedKakaoEditor() }
        await("SWIFTKEY_NOT_VISIBLE_ON_RETURN", 4_000) {
            swiftKeySelectedAndVisible().takeIf { it }
        }
        val returned = captureEmptyFocusedKakaoEditor()
        assertSameIdentity(original, returned)
        SystemClock.sleep(1_200)
        val delayed = captureEmptyFocusedKakaoEditor()
        assertSameIdentity(original, delayed)
        sessionAttempted = false
    }

    private fun captureEmptyFocusedKakaoEditor(): Target {
        val target = focusedKakaoEditor() ?: throw AssertionError("KAKAO_EDITOR_NOT_FOCUSED")
        assertTrue("KAKAO_EDITOR_NOT_EDITABLE", target.node.isEditable && target.node.isEnabled &&
            target.node.isFocused && target.node.isVisibleToUser &&
            !target.className.isNullOrBlank())
        val variation = target.inputType and InputType.TYPE_MASK_VARIATION
        assertTrue("KAKAO_EDITOR_PROTECTED_OR_NON_TEXT", !target.node.isPassword &&
            (target.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        assertTrue("KAKAO_COMPOSER_NOT_EMPTY", emptyOrHint(target.node))
        assertTrue("KAKAO_SELECTION_NOT_SAFE_EMPTY", initialEmptyNodeCaret(target.node))
        assertTrue("SWIFTKEY_NOT_VISIBLE", swiftKeySelectedAndVisible())
        return target
    }

    /** Read only root metadata and root.findFocus; never traverse KakaoTalk's tree. */
    private fun focusedKakaoEditor(): Target? {
        val focusedApps = automation().windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedApps.size > 1) throw AssertionError("MULTIPLE_FOCUSED_APP_WINDOWS")
        val window = focusedApps.singleOrNull() ?: return null
        val root = window.getRoot(0) ?: return null
        if (root.packageName?.toString() != kakaoPackage) return null
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (node.packageName?.toString() != kakaoPackage || node.windowId != window.id)
            throw AssertionError("FOCUSED_NODE_NOT_KAKAO_COMPOSER")
        return Target(window.id, root, node, node.viewIdResourceName,
            node.className?.toString(), node.uniqueId, node.inputType)
    }

    private fun assertSameIdentity(original: Target, current: Target) {
        assertTrue("KAKAO_TARGET_IDENTITY_CHANGED",
            current.windowId == original.windowId && current.root == original.root &&
                current.node == original.node && current.resourceId == original.resourceId &&
                current.className == original.className && current.uniqueId == original.uniqueId &&
                current.inputType == original.inputType && current.node.isFocused &&
                current.node.isEditable && current.node.isEnabled && current.node.isVisibleToUser &&
                !current.node.isPassword)
    }

    private fun emptyOrHint(node: AccessibilityNodeInfo): Boolean =
        node.isShowingHintText || node.text.isNullOrEmpty()

    /** The absent node caret is only a candidate for production's independent empty-connection proof. */
    private fun initialEmptyNodeCaret(node: AccessibilityNodeInfo): Boolean {
        if (!emptyOrHint(node)) return false
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        return (start == 0 && end == 0) ||
            (start == -1 && end == -1)
    }

    private fun knownCollapsedSelection(node: AccessibilityNodeInfo): Boolean {
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val visibleTextLength = if (node.isShowingHintText) 0 else node.text?.length ?: 0
        return start >= 0 && start == end && start <= visibleTextLength
    }

    private fun swiftKeySelected(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
    ) == swiftKeyComponent

    private fun swiftKeySelectedAndVisible(): Boolean {
        if (!swiftKeySelected()) return false
        val imeWindows = automation().windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }
        return imeWindows.size == 1 &&
            imeWindows.single().getRoot(0)?.packageName?.toString() == swiftKeyPackage
    }

    private fun clickOwnButton(label: String) {
        awaitOwnButton(label)
        repeat(12) {
            val visible = uniqueOwnButton(label, visibleOnly = true)
            if (visible != null) {
                assertTrue("PRODUCT_BUTTON_CLICK_FAILED",
                    visible.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                return
            }
            val candidate = uniqueOwnButton(label) ?: throw AssertionError("PRODUCT_BUTTON_DISAPPEARED")
            candidate.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
            ownScrollableParent(candidate)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(250)
        }
        throw AssertionError("PRODUCT_BUTTON_NOT_REVEALED")
    }

    private fun ownScrollableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        repeat(12) {
            if (parent == null || parent?.packageName?.toString() != productPackage) return null
            if (parent?.className?.toString() == "android.widget.ScrollView") return parent
            parent = parent?.parent
        }
        return null
    }

    private fun awaitOwnButton(label: String): AccessibilityNodeInfo =
        await("PRODUCT_BUTTON_NOT_FOUND") { uniqueOwnButton(label) }

    private fun uniqueOwnButton(label: String, visibleOnly: Boolean = false): AccessibilityNodeInfo? {
        val matches = ownButtons(label, visibleOnly)
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_PRODUCT_BUTTON")
        return matches.singleOrNull()
    }

    /** Traverse only KoScribe roots for exact product-control labels. */
    private fun ownButtons(label: String, visibleOnly: Boolean = false): List<AccessibilityNodeInfo> {
        val matches = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != productPackage) continue
            val pending = ArrayDeque<AccessibilityNodeInfo>()
            pending.add(root)
            while (pending.isNotEmpty()) {
                val node = pending.removeFirst()
                if (node.packageName?.toString() != productPackage) continue
                if (node.className?.toString() == "android.widget.Button" && node.isClickable &&
                    node.isEnabled && (!visibleOnly || node.isVisibleToUser) &&
                    node.text?.toString()?.equals(label, ignoreCase = true) == true)
                    matches.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(pending::addLast)
            }
        }
        return matches
    }

    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000, probe: () -> T?): T {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError(code)
    }
}
