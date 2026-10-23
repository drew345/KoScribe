package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
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

/** Opt-in capture-only probe; it cannot confirm, copy, insert, or send a draft. */
@RunWith(AndroidJUnit4::class)
class KakaoOverlayRecordingSmokeTest {
    private val productPackage = "com.koscribe.prototype"
    private val kakaoPackage = "com.kakao.talk"
    private val swiftKeyPackage = "com.touchtype.swiftkey"
    private val swiftKeyComponent = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val fixedEnglish = "Hello, I will arrive in ten minutes."
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false

    private data class Target(
        val windowId: Int, val root: AccessibilityNodeInfo, val node: AccessibilityNodeInfo,
        val resourceId: String?, val className: String?, val uniqueId: String?, val inputType: Int
    )

    private data class ProbeState(val arm: String, val count: Int, val phase: String,
        val firstInvalidation: String, val cleanupPending: Boolean)

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactOptIn() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val older = listOf("externalDraftReady", "externalRecordCancelReady",
            "startupHoldReady", "startupCollapseReady", "startupPanelReady", "reselectReady")
            .any { args.getString(it) == "true" }
        assumeTrue("OVERLAY_TEST_REQUIRES_SOLE_OPT_IN", args.getString("overlayReady") == "true" && !older)
        assertTrue("WRONG_INSTRUMENTATION_PACKAGE",
            instrumentation.context.packageName == "com.koscribe.smoke")
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
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
        runCatching { if (uniqueOwnButton("Cancel") != null) clickOwnButton("Cancel") }
        // The product arm also owns an eight-second deadline and releases its mic.
    }

    @Test fun nonfocusableOverlayRecordsStopsAndCancelsWithoutDraftMutation() {
        assumeTrue("OVERLAY_METHOD_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("overlayReady") == "true")
        val before = probeState()
        assertTrue("WRONG_PRODUCT_EXPERIMENT_ARM", before.arm == "OVERLAY")
        val original = captureEmptyFocusedKakaoEditor()
        assertTrue("OTHER_PRODUCT_SESSION_OPEN",
            ownButtons("Stop").isEmpty() && ownButtons("Cancel").isEmpty())
        await("BUBBLE_NOT_AVAILABLE") {
            (ownButtons("Ko").size + ownButtons("Close").size == 1).takeIf { it }
        }
        if (ownButtons("Close").size == 1) clickOwnButton("Close")
        clickOwnButton("Ko")
        assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
        sessionAttempted = true
        var stopped = false
        try {
            clickOwnButton("요")
            await("OVERLAY_MIC_PROOF_OR_STOP_NOT_AVAILABLE", 4_000) {
                uniqueOwnButton("Stop", visibleOnly = true)
            }
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor(requireVisible = false),
                requireVisible = false)
            assertTrue("SWIFTKEY_NOT_VISIBLE_DURING_RECORDING", swiftKeySelectedAndVisible())
            assertTrue("EXPERIMENT_EXPOSED_CONFIRMATION", noInsertOrCopyButtons())
            SystemClock.sleep(2_000)
            clickOwnButton("Stop")
            stopped = true
            await("READ_ONLY_REVIEW_NOT_AVAILABLE") {
                (ownButtons("Stop").isEmpty() && uniqueOwnButton("Cancel") != null &&
                    ownExactText(fixedEnglish).size == 1).takeIf { it }
            }
            assertTrue("EXPERIMENT_EXPOSED_CONFIRMATION", noInsertOrCopyButtons())
            clickOwnButton("Cancel")
            await("OVERLAY_DID_NOT_CANCEL", 10_000) {
                probeState().takeIf { it.arm == "OVERLAY" &&
                    it.count == before.count + 1 && it.phase == "CANCELLED" }
            }
        } finally {
            runCatching { if (uniqueOwnButton("Cancel") != null) clickOwnButton("Cancel") }
            await("KAKAO_DID_NOT_REMAIN_FOCUSED") { focusedKakaoEditor() }
            await("SWIFTKEY_NOT_VISIBLE_ON_RETURN", 4_000) {
                swiftKeySelectedAndVisible().takeIf { it }
            }
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
            SystemClock.sleep(1_200)
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
            sessionAttempted = false
        }
        val after = probeState()
        assertTrue("FRESH_OVERLAY_CAPTURE_NOT_OBSERVED", after.count == before.count + 1)
        assertTrue("OVERLAY_RECORDING_DID_NOT_STOP", stopped)
        assertTrue("OVERLAY_GUARD_INVALIDATED", after.firstInvalidation == "none")
        assertTrue("OVERLAY_MIC_CLEANUP_UNCERTAIN", !after.cleanupPending)
    }

    @Test fun nonfocusableOverlayCancelDuringRecordingReleasesOwnerAndPreservesDraft() {
        assumeTrue("OVERLAY_METHOD_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("overlayReady") == "true")
        val before = probeState()
        assertTrue("WRONG_PRODUCT_EXPERIMENT_ARM", before.arm == "OVERLAY")
        val original = captureEmptyFocusedKakaoEditor()
        assertTrue("OTHER_PRODUCT_SESSION_OPEN",
            ownButtons("Stop").isEmpty() && ownButtons("Cancel").isEmpty())
        await("BUBBLE_NOT_AVAILABLE") {
            (ownButtons("Ko").size + ownButtons("Close").size == 1).takeIf { it }
        }
        if (ownButtons("Close").size == 1) clickOwnButton("Close")
        clickOwnButton("Ko")
        assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
        sessionAttempted = true
        try {
            clickOwnButton("요")
            await("OVERLAY_MIC_PROOF_OR_STOP_NOT_AVAILABLE", 4_000) {
                uniqueOwnButton("Stop", visibleOnly = true)
            }
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor(requireVisible = false),
                requireVisible = false)
            assertTrue("SWIFTKEY_NOT_VISIBLE_DURING_RECORDING", swiftKeySelectedAndVisible())
            assertTrue("EXPERIMENT_EXPOSED_CONFIRMATION", noInsertOrCopyButtons())
            SystemClock.sleep(1_000)
            clickOwnButton("Cancel")
            await("OVERLAY_DID_NOT_CANCEL", 10_000) {
                probeState().takeIf { it.arm == "OVERLAY" &&
                    it.count == before.count + 1 && it.phase == "CANCELLED" }
            }
            assertTrue("OVERLAY_STOP_STILL_VISIBLE", ownButtons("Stop").isEmpty())
        } finally {
            runCatching { if (uniqueOwnButton("Cancel") != null) clickOwnButton("Cancel") }
            await("KAKAO_DID_NOT_REMAIN_FOCUSED") { focusedKakaoEditor() }
            await("SWIFTKEY_NOT_VISIBLE_ON_RETURN", 4_000) {
                swiftKeySelectedAndVisible().takeIf { it }
            }
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
            SystemClock.sleep(1_200)
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
            sessionAttempted = false
        }
        val after = probeState()
        assertTrue("FRESH_OVERLAY_CAPTURE_NOT_OBSERVED", after.count == before.count + 1)
        assertTrue("OVERLAY_GUARD_INVALIDATED", after.firstInvalidation == "none")
        assertTrue("OVERLAY_MIC_CLEANUP_UNCERTAIN", !after.cleanupPending)
    }

    private fun noInsertOrCopyButtons(): Boolean =
        ownButtons("Translate & insert sample").isEmpty() &&
            ownButtons("Insert English sample").isEmpty() &&
            ownButtons("Copy result manually").isEmpty()

    private fun probeState(): ProbeState {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(128).toList()
        }
        fun field(name: String): String = lines.firstOrNull { it.trim().startsWith("$name: ") }
            ?.trim()?.substringAfter("$name: ")?.trim()
            ?: throw AssertionError("PRODUCT_DIAGNOSTICS_UNAVAILABLE")
        val arm = field("Experiment arm")
        val count = field("Experiment capture count").toIntOrNull()
            ?: throw AssertionError("PRODUCT_CAPTURE_COUNT_UNAVAILABLE")
        val phase = field("Experiment phase")
        val invalidation = field("First invalidation")
        assertTrue("INVALID_PRODUCT_PROBE_DIAGNOSTICS",
            arm in setOf("OFF", "HOLD", "COLLAPSE", "PANEL", "OVERLAY") &&
                count in 0..100_000 && phase.matches(Regex("[A-Z][A-Z0-9_]{0,80}")) &&
                invalidation.matches(Regex("[A-Z][A-Z0-9_]{0,80}|none")))
        val cleanup = field("Overlay cleanup pending")
        assertTrue("INVALID_OVERLAY_CLEANUP_DIAGNOSTIC", cleanup == "true" || cleanup == "false")
        return ProbeState(arm, count, phase, invalidation, cleanup == "true")
    }

    private fun captureEmptyFocusedKakaoEditor(requireVisible: Boolean = true): Target {
        val target = focusedKakaoEditor() ?: throw AssertionError("KAKAO_EDITOR_NOT_FOCUSED")
        assertTrue("KAKAO_EDITOR_NOT_EDITABLE", target.node.isEditable && target.node.isEnabled &&
            target.node.isFocused && (!requireVisible || target.node.isVisibleToUser) &&
            !target.className.isNullOrBlank())
        val variation = target.inputType and InputType.TYPE_MASK_VARIATION
        assertTrue("KAKAO_EDITOR_PROTECTED_OR_NON_TEXT", !target.node.isPassword &&
            target.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
            variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        assertTrue("KAKAO_COMPOSER_NOT_EMPTY", target.node.isShowingHintText || target.node.text.isNullOrEmpty())
        val start = target.node.textSelectionStart
        val end = target.node.textSelectionEnd
        assertTrue("KAKAO_SELECTION_NOT_SAFE_EMPTY",
            (start == 0 && end == 0) || (start == -1 && end == -1))
        assertTrue("SWIFTKEY_NOT_VISIBLE", swiftKeySelectedAndVisible())
        return target
    }

    /** Only root metadata and the focused input node are read in KakaoTalk. */
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

    private fun assertSameEmptyIdentity(original: Target, current: Target, requireVisible: Boolean = true) {
        assertTrue("KAKAO_TARGET_IDENTITY_CHANGED",
            current.windowId == original.windowId && current.root == original.root &&
                current.node == original.node && current.resourceId == original.resourceId &&
                current.className == original.className && current.uniqueId == original.uniqueId &&
                current.inputType == original.inputType)
        assertTrue("KAKAO_DRAFT_CHANGED_OR_UNSAFE", current.node.isFocused &&
            current.node.isEditable && current.node.isEnabled &&
            (!requireVisible || current.node.isVisibleToUser) && !current.node.isPassword &&
            (current.node.isShowingHintText || current.node.text.isNullOrEmpty()))
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
        await("PRODUCT_BUTTON_NOT_FOUND") { uniqueOwnButton(label, visibleOnly = true) }
        val fresh = uniqueOwnButton(label, visibleOnly = true)
            ?: throw AssertionError("PRODUCT_BUTTON_DISAPPEARED")
        assertTrue("PRODUCT_BUTTON_CLICK_FAILED", fresh.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun uniqueOwnButton(label: String, visibleOnly: Boolean = false): AccessibilityNodeInfo? {
        val matches = ownButtons(label, visibleOnly)
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_PRODUCT_BUTTON")
        return matches.singleOrNull()
    }

    private fun ownButtons(label: String, visibleOnly: Boolean = false): List<AccessibilityNodeInfo> =
        ownNodes("android.widget.Button", label, visibleOnly)

    private fun ownExactText(value: String): List<AccessibilityNodeInfo> =
        ownNodes("android.widget.TextView", value, visibleOnly = true)

    /** Traversal starts only after confirming each root is KoScribe's. */
    private fun ownNodes(className: String, exactText: String, visibleOnly: Boolean): List<AccessibilityNodeInfo> {
        val matches = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != productPackage) continue
            val pending = ArrayDeque<AccessibilityNodeInfo>()
            pending.add(root)
            while (pending.isNotEmpty()) {
                val node = pending.removeFirst()
                if (node.packageName?.toString() != productPackage) continue
                if (node.className?.toString() == className && node.isEnabled &&
                    (!visibleOnly || node.isVisibleToUser) &&
                    node.text?.toString()?.equals(exactText, ignoreCase = true) == true)
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
