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

/** Explicitly gated, no-insertion startup probes in the prepared empty self-chat. */
@RunWith(AndroidJUnit4::class)
class KakaoStartupIsolationSmokeTest {
    private val productPackage = "com.koscribe.prototype"
    private val kakaoPackage = "com.kakao.talk"
    private val swiftKeyPackage = "com.touchtype.swiftkey"
    private val swiftKeyComponent = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false
    private var initialCaptureCount = -1

    private data class Target(
        val windowId: Int, val root: AccessibilityNodeInfo, val node: AccessibilityNodeInfo,
        val resourceId: String?, val className: String?, val uniqueId: String?, val inputType: Int
    )

    private data class ProbeState(
        val arm: String, val count: Int, val phase: String, val holdComplete: Boolean,
        val collapseAddReturned: Boolean, val panelLaunchRequested: Boolean
    )

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireOneExplicitProbeFlag() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        val selected = listOf("startupHoldReady", "startupCollapseReady", "startupPanelReady")
            .count { arguments.getString(it) == "true" }
        val olderExternalFlag = arguments.getString("externalDraftReady") == "true" ||
            arguments.getString("externalRecordCancelReady") == "true" ||
            arguments.getString("overlayReady") == "true" ||
            arguments.getString("reselectReady") == "true" ||
            arguments.getString("bubbleIdleReady") == "true" ||
            arguments.getString("bubbleMicReady") == "true" ||
            arguments.getString("bubbleRestoreReady") == "true" ||
            listOf("reviewCopyReady", "reviewInsertEmptyReady", "reviewInsertMiddleReady",
                "reviewKoreanReady", "reviewSelectionReady", "reviewKoreanEditReady",
                "reviewUncertainReady").any { arguments.getString(it) == "true" }
        assumeTrue("STARTUP_PROBE_REQUIRES_ONE_EXPLICIT_FLAG",
            selected == 1 && !olderExternalFlag)
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
        // A failed probe cannot navigate, type, or tap in KakaoTalk.
        runCatching { if (uniqueOwnButton("Cancel") != null) clickOwnButton("Cancel") }
        runCatching {
            if (probeState().count > initialCaptureCount)
                await("PROBE_CLEANUP_TIMEOUT", 12_000) {
                    probeState().takeIf { it.phase == "CANCELLED" }
                }
        }
    }

    @Test fun captureOnlyHoldsExpandedBubbleWithoutPanel() {
        assumeTrue("HOLD_PROBE_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("startupHoldReady") == "true")
        runProbe("HOLD", panelExpected = false)
    }

    @Test fun collapseOnlyHoldsWithoutPanel() {
        assumeTrue("COLLAPSE_PROBE_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("startupCollapseReady") == "true")
        runProbe("COLLAPSE", panelExpected = false)
    }

    @Test fun panelOnlyStartsWithoutCollapsingBubble() {
        assumeTrue("PANEL_PROBE_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("startupPanelReady") == "true")
        runProbe("PANEL", panelExpected = true)
    }

    private fun runProbe(expectedArm: String, panelExpected: Boolean) {
        // Confirm the exact installed product build before any product action.
        val before = probeState()
        assertTrue("WRONG_PRODUCT_EXPERIMENT_ARM", before.arm == expectedArm)
        initialCaptureCount = before.count
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
        var panelStarted = false
        var panelStopCancelCompleted = false
        var actionFailed = false
        try {
            clickOwnButton("요")
            if (panelExpected) {
                panelStarted = awaitOptionalOwnButton("Stop", 4_000) != null
                if (panelStarted) {
                    panelStopCancelCompleted = runCatching {
                        SystemClock.sleep(2_000)
                        clickOwnButton("Stop")
                        await("STOP_DID_NOT_TRANSITION") {
                            (ownButtons("Stop").isEmpty() &&
                                uniqueOwnButton("Cancel") != null).takeIf { it }
                        }
                        assertTrue("EXPERIMENT_EXPOSED_CONFIRMATION",
                            ownButtons("Translate & insert sample").isEmpty() &&
                                ownButtons("Insert English sample").isEmpty() &&
                                ownButtons("Copy result manually").isEmpty())
                        clickOwnButton("Cancel")
                    }.isSuccess
                }
            } else {
                // Keep dumpsys/UI polling out of the 1.5-second observation interval.
                SystemClock.sleep(2_000)
                assertTrue("UNEXPECTED_RECORDING_PANEL", ownButtons("Stop").isEmpty())
            }
        } catch (_: AssertionError) {
            actionFailed = true
        } finally {
            // Only KoScribe's Cancel can be used for cleanup; the Activity and
            // service also have bounded independent cancellation deadlines.
            runCatching { if (uniqueOwnButton("Cancel") != null) clickOwnButton("Cancel") }
            val afterAction = probeState()
            if (afterAction.count == before.count + 1) {
                await("PROBE_DID_NOT_CANCEL", 12_000) {
                    probeState().takeIf { it.arm == expectedArm &&
                        it.count == before.count + 1 && it.phase == "CANCELLED" }
                }
            }
            await("KAKAO_DID_NOT_REMAIN_FOCUSED") { focusedKakaoEditor() }
            await("SWIFTKEY_NOT_VISIBLE_ON_RETURN", 4_000) {
                swiftKeySelectedAndVisible().takeIf { it }
            }
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
            SystemClock.sleep(1_200)
            assertSameEmptyIdentity(original, captureEmptyFocusedKakaoEditor())
            sessionAttempted = false
        }
        val finalProbe = probeState()
        assertTrue("FRESH_PROBE_CAPTURE_NOT_OBSERVED",
            finalProbe.arm == expectedArm && finalProbe.count == before.count + 1)
        if (!panelExpected) assertTrue("HOLD_DID_NOT_COMPLETE", finalProbe.holdComplete)
        when (expectedArm) {
            "HOLD" -> assertTrue("HOLD_STEPS_UNEXPECTED",
                !finalProbe.collapseAddReturned && !finalProbe.panelLaunchRequested)
            "COLLAPSE" -> assertTrue("COLLAPSE_STEP_UNVERIFIED",
                finalProbe.collapseAddReturned && !finalProbe.panelLaunchRequested)
            "PANEL" -> assertTrue("PANEL_STEP_UNVERIFIED",
                finalProbe.panelLaunchRequested && !finalProbe.collapseAddReturned)
        }
        if (panelExpected) {
            assertTrue("PANEL_STOP_NOT_AVAILABLE", panelStarted)
            assertTrue("PANEL_STOP_CANCEL_NOT_COMPLETED", panelStopCancelCompleted)
        }
        assertTrue("PROBE_ACTION_FAILED", !actionFailed)
    }

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
        val phase = field("Experiment phase")
        val count = field("Experiment capture count").toIntOrNull()
            ?: throw AssertionError("PRODUCT_CAPTURE_COUNT_UNAVAILABLE")
        val holdComplete = lines.any {
            it.trim().startsWith("EXPERIMENT_HOLD_COMPLETE_CAPTURE_")
        }
        val collapseAddReturned = lines.any {
            it.trim().startsWith("COLLAPSED_BUBBLE_ADD_RETURNED_CAPTURE_")
        }
        val panelLaunchRequested = lines.any {
            it.trim().startsWith("PANEL_LAUNCH_REQUESTED_CAPTURE_")
        }
        assertTrue("INVALID_PRODUCT_PROBE_DIAGNOSTICS",
            arm in setOf("OFF", "HOLD", "COLLAPSE", "PANEL") &&
                phase.matches(Regex("[A-Z][A-Z0-9_]{0,80}")) && count in 0..100_000)
        return ProbeState(arm, count, phase, holdComplete, collapseAddReturned, panelLaunchRequested)
    }

    private fun captureEmptyFocusedKakaoEditor(): Target {
        val target = focusedKakaoEditor() ?: throw AssertionError("KAKAO_EDITOR_NOT_FOCUSED")
        assertTrue("KAKAO_EDITOR_NOT_EDITABLE", target.node.isEditable && target.node.isEnabled &&
            target.node.isFocused && target.node.isVisibleToUser && !target.className.isNullOrBlank())
        val variation = target.inputType and InputType.TYPE_MASK_VARIATION
        assertTrue("KAKAO_EDITOR_PROTECTED_OR_NON_TEXT", !target.node.isPassword &&
            target.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
            variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        assertTrue("KAKAO_COMPOSER_NOT_EMPTY", emptyOrHint(target.node))
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

    private fun assertSameEmptyIdentity(original: Target, current: Target) {
        assertTrue("KAKAO_TARGET_IDENTITY_CHANGED",
            current.windowId == original.windowId && current.root == original.root &&
                current.node == original.node && current.resourceId == original.resourceId &&
                current.className == original.className && current.uniqueId == original.uniqueId &&
                current.inputType == original.inputType)
        assertTrue("KAKAO_DRAFT_CHANGED_OR_UNSAFE", current.node.isFocused &&
            current.node.isEditable && current.node.isEnabled && current.node.isVisibleToUser &&
            !current.node.isPassword && emptyOrHint(current.node))
    }

    private fun emptyOrHint(node: AccessibilityNodeInfo): Boolean =
        node.isShowingHintText || node.text.isNullOrEmpty()

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

    private fun awaitOptionalOwnButton(label: String, timeoutMs: Long): AccessibilityNodeInfo? {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            uniqueOwnButton(label, visibleOnly = true)?.let { return it }
            SystemClock.sleep(100)
        }
        return null
    }

    private fun uniqueOwnButton(label: String, visibleOnly: Boolean = false): AccessibilityNodeInfo? {
        val matches = ownButtons(label, visibleOnly)
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_PRODUCT_BUTTON")
        return matches.singleOrNull()
    }

    /** Traverse only confirmed KoScribe roots for exact own control labels. */
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
