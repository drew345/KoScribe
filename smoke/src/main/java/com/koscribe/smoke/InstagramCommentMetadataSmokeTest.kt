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

/** Opt-in Instagram focused-input metadata only. It cannot distinguish comments from DMs. */
@RunWith(AndroidJUnit4::class)
class InstagramCommentMetadataSmokeTest {
    private val instagram = "com.instagram.android"
    private val fixture = "com.koscribe.smoke"
    private val optIn = "instagramCommentMetadataReady"

    @Test fun focusedInstagramCommentMetadataOnly() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("INSTAGRAM_COMMENT_METADATA_REQUIRES_SOLE_OPT_IN",
            args.getString(optIn) == "true" &&
                args.keySet().none {
                    it != optIn && it.endsWith("Ready") && args.getString(it) == "true"
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
        if (focused.size != 1) return report(instrumentation, "FOCUSED_APP_COUNT_OTHER")
        val window = focused.single()
        if (window.displayId != Display.DEFAULT_DISPLAY)
            return report(instrumentation, "WRONG_DISPLAY")
        val root = runCatching { window.getRoot(0) }.getOrNull()
            ?: return report(instrumentation, "ROOT_MISSING")
        if (root.packageName?.toString() != instagram)
            return report(instrumentation, "ROOT_PACKAGE_MISMATCH")
        if (root.windowId != window.id)
            return report(instrumentation, "ROOT_WINDOW_MISMATCH")
        if (!runCatching { root.refresh() }.getOrDefault(false))
            return report(instrumentation, "ROOT_REFRESH_FAILED")
        if (root.packageName?.toString() != instagram || root.windowId != window.id)
            return report(instrumentation, "ROOT_CHANGED")

        // findFocus is the only descendant lookup. Never traverse the Instagram tree.
        val node = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
            ?: return report(instrumentation, "FOCUSED_INPUT_MISSING")
        if (node.packageName?.toString() != instagram)
            return report(instrumentation, "NODE_PACKAGE_MISMATCH")
        if (node.windowId != window.id)
            return report(instrumentation, "NODE_WINDOW_MISMATCH")
        val before = snapshot(node)

        // Refresh only this same focused node; this does not request editor content or an action.
        val refreshed = runCatching { node.refresh() }.getOrDefault(false)
        if (!refreshed)
            return report(instrumentation, "PRE_${before}_REFRESH_0_AFTER_UNAVAILABLE")
        if (node.packageName?.toString() != instagram || node.windowId != window.id)
            return report(instrumentation, "REFRESH_TARGET_CHANGED")
        val stillFocused = ui.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (stillFocused.size != 1 || stillFocused.single().id != window.id ||
            stillFocused.single().displayId != Display.DEFAULT_DISPLAY)
            return report(instrumentation, "FOCUSED_WINDOW_CHANGED")
        val currentRoot = runCatching { stillFocused.single().getRoot(0) }.getOrNull()
        if (currentRoot == null || currentRoot != root ||
            currentRoot.packageName?.toString() != instagram ||
            currentRoot.windowId != window.id)
            return report(instrumentation, "FOCUSED_ROOT_CHANGED")
        val currentNode = runCatching {
            currentRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }.getOrNull()
        if (currentNode == null || currentNode != node ||
            currentNode.packageName?.toString() != instagram ||
            currentNode.windowId != window.id)
            return report(instrumentation, "FOCUSED_INPUT_CHANGED")
        report(instrumentation, "PRE_${before}_REFRESH_1_POST_${snapshot(node)}")
    }

    private fun snapshot(node: AccessibilityNodeInfo): String {
        val type = node.inputType
        val klass = when (type and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_NULL -> "NONE"
            InputType.TYPE_CLASS_TEXT -> "TEXT"
            InputType.TYPE_CLASS_NUMBER -> "NUMBER"
            InputType.TYPE_CLASS_PHONE -> "PHONE"
            InputType.TYPE_CLASS_DATETIME -> "DATETIME"
            else -> "OTHER"
        }
        val variation = type and InputType.TYPE_MASK_VARIATION
        val variationCategory = if (klass != "TEXT") "NOT_TEXT" else when (variation) {
            InputType.TYPE_TEXT_VARIATION_NORMAL -> "NORMAL"
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> "PASSWORD"
            else -> "OTHER"
        }
        val nodeClass = when (node.className?.toString()) {
            null -> "MISSING"
            "android.widget.EditText" -> "EDIT_TEXT"
            "android.widget.TextView" -> "TEXT_VIEW"
            "android.webkit.WebView" -> "WEB_VIEW"
            else -> "OTHER"
        }
        val safeType = klass == "TEXT" && variationCategory != "PASSWORD"
        return "FOCUSED_${bit(node.isFocused)}_EDITABLE_${bit(node.isEditable)}" +
            "_ENABLED_${bit(node.isEnabled)}_VISIBLE_${bit(node.isVisibleToUser)}" +
            "_PASSWORD_${bit(node.isPassword)}" +
            "_RESOURCE_${bit(!node.viewIdResourceName.isNullOrBlank())}" +
            "_CLASS_${nodeClass}_TYPE_${klass}_VARIATION_${variationCategory}" +
            "_SAFE_TYPE_${bit(safeType)}"
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

    private fun report(instrumentation: Instrumentation, code: String) {
        val value = "INSTAGRAM_COMMENT_META_$code"
        assertTrue("UNSAFE_INSTAGRAM_METADATA_CODE", value.length <= 700 &&
            Regex("[A-Z0-9_]+").matches(value))
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_instagram_comment_metadata", value)
        })
    }
}
