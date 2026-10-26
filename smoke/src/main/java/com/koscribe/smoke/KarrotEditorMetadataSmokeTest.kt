package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.os.Bundle
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in, read-only metadata check. It never opens an app or touches editor content. */
@RunWith(AndroidJUnit4::class)
class KarrotEditorMetadataSmokeTest {
    private val karrot = "com.towneers.www"
    private val fixture = "com.koscribe.smoke"

    @Test fun focusedKarrotEditorMetadataOnly() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("KARROT_METADATA_REQUIRES_SOLE_OPT_IN",
            args.getString("karrotMetadataReady") == "true" &&
                args.keySet().none {
                    it != "karrotMetadataReady" && it.endsWith("Ready") &&
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
            report(instrumentation, "KARROT_META_FOCUSED_APP_COUNT_${focused.size.coerceAtMost(9)}")
            return
        }
        val window = focused.single()
        val root = runCatching { window.getRoot(0) }.getOrNull()
        if (root == null) {
            report(instrumentation, "KARROT_META_ROOT_MISSING")
            return
        }
        val rootPackage = root.packageName?.toString()
        if (rootPackage != karrot) {
            report(instrumentation, "KARROT_META_ROOT_PACKAGE_${
                if (rootPackage == null) "MISSING" else "OTHER"
            }_WINDOW_MATCH_${bit(root.windowId == window.id)}")
            return
        }
        if (root.windowId != window.id) {
            report(instrumentation, "KARROT_META_ROOT_WINDOW_MISMATCH")
            return
        }
        val node = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
        if (node == null) {
            report(instrumentation, "KARROT_META_ROOT_MATCH_1_FOCUSED_INPUT_MISSING")
            return
        }
        val nodePackage = node.packageName?.toString()
        if (nodePackage != karrot) {
            report(instrumentation, "KARROT_META_NODE_PACKAGE_${
                if (nodePackage == null) "MISSING" else "OTHER"
            }_WINDOW_MATCH_${bit(node.windowId == window.id)}")
            return
        }
        val type = node.inputType
        val klass = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        val safeType = klass == InputType.TYPE_CLASS_TEXT &&
            variation != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        val nodeClass = when (node.className?.toString()) {
            null -> "MISSING"
            "android.widget.EditText" -> "EDIT_TEXT"
            "android.widget.TextView" -> "TEXT_VIEW"
            "android.webkit.WebView" -> "WEB_VIEW"
            else -> "OTHER"
        }
        report(instrumentation,
            "KARROT_META_ROOT_MATCH_1_NODE_PACKAGE_MATCH_1" +
                "_NODE_WINDOW_${bit(node.windowId == window.id)}" +
                "_FOCUSED_${bit(node.isFocused)}_EDITABLE_${bit(node.isEditable)}" +
                "_ENABLED_${bit(node.isEnabled)}_VISIBLE_${bit(node.isVisibleToUser)}" +
                "_PASSWORD_${bit(node.isPassword)}_RESOURCE_${bit(!node.viewIdResourceName.isNullOrBlank())}" +
                "_NODE_CLASS_${nodeClass}_TYPE_${type}_CLASS_${klass}_VARIATION_${variation}" +
                "_SAFE_TYPE_${bit(safeType)}")
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

    private fun report(instrumentation: Instrumentation, code: String) {
        assertTrue("UNSAFE_KARROT_METADATA_CODE", code.length <= 400 &&
            Regex("[A-Z0-9_]+").matches(code))
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_karrot_metadata", code)
        })
    }
}
