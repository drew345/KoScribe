package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.os.Bundle
import android.text.InputType
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Separate opt-in comparison of cached and refreshed metadata on the same focused node.
 * Run only this method with karrotRefreshReady=true while Karrot's intended editor is focused.
 * No editor content, UI action, input connection, or credential is accessed.
 */
@RunWith(AndroidJUnit4::class)
class KarrotFocusedNodeRefreshSmokeTest {
    private val karrot = "com.towneers.www"
    private val fixture = "com.koscribe.smoke"
    private val soleTest =
        "com.koscribe.smoke.KarrotFocusedNodeRefreshSmokeTest#compareSameFocusedNodeBeforeAfterRefresh"

    @Test fun compareSameFocusedNodeBeforeAfterRefresh() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("KARROT_REFRESH_REQUIRES_SOLE_OPT_IN",
            args.getString("karrotRefreshReady") == "true" &&
                args.getString("class") == soleTest &&
                args.keySet().none {
                    it != "karrotRefreshReady" && it.endsWith("Ready") &&
                        args.getString(it) == "true"
                })
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue("WRONG_INSTRUMENTATION_PACKAGE",
            instrumentation.context.packageName == fixture)
        val ui = instrumentation.getUiAutomation(
            UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info

        val focused = ui.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focused.size != 1) {
            report(instrumentation, "FOCUSED_APP_${if (focused.isEmpty()) "NONE" else "MULTIPLE"}")
            return
        }
        val window = focused.single()
        if (window.displayId != Display.DEFAULT_DISPLAY) {
            report(instrumentation, "FOCUSED_APP_WRONG_DISPLAY")
            return
        }
        val root = runCatching { window.getRoot(0) }.getOrNull()
        if (root == null) {
            report(instrumentation, "ROOT_MISSING")
            return
        }
        // Check ownership before refreshing. Product capture likewise refreshes this root
        // before asking it for the focused input descendant.
        val rootBefore = ownership(root, window.id)
        if (rootBefore != "PKG_MATCH_WIN_MATCH") {
            report(instrumentation, "ROOT_BEFORE_$rootBefore")
            return
        }
        if (!runCatching { root.refresh() }.getOrDefault(false)) {
            report(instrumentation, "ROOT_REFRESH_FAILED")
            return
        }
        val rootAfter = ownership(root, window.id)
        if (rootAfter != "PKG_MATCH_WIN_MATCH") {
            report(instrumentation, "ROOT_AFTER_$rootAfter")
            return
        }
        val node = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
        if (node == null) {
            report(instrumentation, "FOCUSED_INPUT_MISSING")
            return
        }
        val before = metadata(node, window.id)
        if (!before.startsWith("PKG_MATCH_WIN_MATCH_")) {
            report(instrumentation, "NODE_BEFORE_WRONG_TARGET", before)
            return
        }
        // Keep this exact node object; do not find a replacement descendant.
        if (!runCatching { node.refresh() }.getOrDefault(false)) {
            report(instrumentation, "NODE_REFRESH_FAILED", before)
            return
        }
        // A refresh can leave an old node object valid after focus has moved. Reacquire
        // only the focused app/root/input identity before reading its refreshed fields.
        val currentWindows = ui.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (currentWindows.size != 1) {
            report(instrumentation,
                "FOCUSED_APP_AFTER_${if (currentWindows.isEmpty()) "NONE" else "MULTIPLE"}",
                before)
            return
        }
        val currentWindow = currentWindows.single()
        if (currentWindow.id != window.id || currentWindow.displayId != Display.DEFAULT_DISPLAY) {
            report(instrumentation, "FOCUSED_APP_AFTER_CHANGED", before)
            return
        }
        val currentRoot = runCatching { currentWindow.getRoot(0) }.getOrNull()
        if (currentRoot == null) {
            report(instrumentation, "ROOT_AFTER_MISSING", before)
            return
        }
        if (currentRoot != root) {
            report(instrumentation, "ROOT_AFTER_CHANGED", before)
            return
        }
        if (!runCatching { currentRoot.refresh() }.getOrDefault(false)) {
            report(instrumentation, "ROOT_AFTER_REFRESH_FAILED", before)
            return
        }
        if (ownership(currentRoot, currentWindow.id) != "PKG_MATCH_WIN_MATCH") {
            report(instrumentation, "ROOT_AFTER_WRONG_TARGET", before)
            return
        }
        val currentNode = runCatching {
            currentRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }.getOrNull()
        if (currentNode == null) {
            report(instrumentation, "NODE_AFTER_MISSING", before)
            return
        }
        if (currentNode != node || ownership(currentNode, currentWindow.id) !=
            "PKG_MATCH_WIN_MATCH") {
            report(instrumentation, "NODE_AFTER_CHANGED", before)
            return
        }
        val after = metadata(node, window.id)
        report(instrumentation,
            if (after.startsWith("PKG_MATCH_WIN_MATCH_")) "NODE_REFRESHED_SAME_TARGET"
            else "NODE_REFRESHED_WRONG_TARGET",
            before, after)
    }

    private fun ownership(node: AccessibilityNodeInfo, windowId: Int): String {
        val pkg = when (node.packageName?.toString()) {
            null -> "MISSING"
            karrot -> "MATCH"
            else -> "OTHER"
        }
        return "PKG_${pkg}_WIN_${if (node.windowId == windowId) "MATCH" else "OTHER"}"
    }

    private fun metadata(node: AccessibilityNodeInfo, windowId: Int): String {
        val owner = ownership(node, windowId)
        if (owner != "PKG_MATCH_WIN_MATCH") return "${owner}_DETAILS_SKIPPED"
        return "${owner}_FOCUSED_${bit(node.isFocused)}_EDITABLE_${bit(node.isEditable)}" +
            "_ENABLED_${bit(node.isEnabled)}_VISIBLE_${bit(node.isVisibleToUser)}" +
            "_PASSWORD_${bit(node.isPassword)}" +
            "_RESOURCE_${bit(!node.viewIdResourceName.isNullOrBlank())}" +
            "_TYPE_${typeCategory(node.inputType)}"
    }

    private fun typeCategory(type: Int): String {
        if (type == 0) return "ZERO"
        return when (type and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> when (type and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_TEXT_VARIATION_NORMAL -> "TEXT_NORMAL"
                InputType.TYPE_TEXT_VARIATION_PASSWORD -> "TEXT_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> "TEXT_VISIBLE_PASSWORD"
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> "TEXT_WEB_PASSWORD"
                else -> "TEXT_OTHER"
            }
            InputType.TYPE_CLASS_NUMBER ->
                if ((type and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                    "NUMBER_PASSWORD" else "NUMBER_OTHER"
            InputType.TYPE_CLASS_PHONE -> "PHONE"
            InputType.TYPE_CLASS_DATETIME -> "DATETIME"
            else -> "OTHER"
        }
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

    private fun report(instrumentation: Instrumentation, result: String,
                       before: String = "UNAVAILABLE", after: String = "UNAVAILABLE") {
        val codes = listOf(result, before, after)
        assertTrue("UNSAFE_KARROT_REFRESH_CODE", codes.all {
            it.length <= 220 && Regex("[A-Z0-9_]+").matches(it)
        })
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_karrot_refresh_result", result)
            putString("koscribe_karrot_refresh_before", before)
            putString("koscribe_karrot_refresh_after", after)
        })
    }
}
