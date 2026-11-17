package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only focused-node comparison on the owned fixture, independent of the product APK. */
@RunWith(AndroidJUnit4::class)
class OwnedFocusedMetadataSmokeTest {
    private val fixture = "com.koscribe.smoke"
    private val optIn = "ownedFocusedMetadataReady"
    private val soleTest =
        "com.koscribe.smoke.OwnedFocusedMetadataSmokeTest#focusedFixtureNodeBeforeAndAfterRefresh"

    @Test fun focusedFixtureNodeBeforeAndAfterRefresh() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("OWNED_METADATA_REQUIRES_SOLE_OPT_IN",
            args.getString(optIn) == "true" && args.getString("class") == soleTest &&
                args.keySet().none {
                it != optIn && it.endsWith("Ready") && args.getString(it) == "true"
            })
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertTrue("WRONG_INSTRUMENTATION_PACKAGE",
            instrumentation.context.packageName == fixture)
        val ui = instrumentation.getUiAutomation(
            UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
        val serviceInfo = ui.serviceInfo
        val savedFlags = serviceInfo.flags
        serviceInfo.flags = savedFlags or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = serviceInfo
        var activity: SmokeEditorActivity? = null
        try {
        val initialCreateCount = SmokeEditorActivity.createCount
        val previousActivity = SmokeEditorActivity.current.get()
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SmokeEditorActivity.EXTRA_MANUAL_FIXTURE, true)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        val deadline = SystemClock.elapsedRealtime() + 4_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val candidate = SmokeEditorActivity.current.get()?.takeIf {
                SmokeEditorActivity.createCount > initialCreateCount &&
                    it !== previousActivity && !it.isFinishing
            }
            if (candidate != null) activity = candidate
            if (candidate?.hasWindowFocus() == true) break
            SystemClock.sleep(80)
        }
        val ownActivity = activity?.takeIf { it.hasWindowFocus() }
            ?: return report(instrumentation, "FIXTURE_FOCUS_MISSING")
        var ownWindowId = -1
        instrumentation.runOnMainSync {
            val editor = ownActivity.findViewById<EditText>(R.id.local_test_editor)
            editor.requestFocus()
            (ownActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
            if (editor.isAttachedToWindow && ownActivity.hasWindowFocus())
                ownWindowId = editor.createAccessibilityNodeInfo().windowId
        }
        if (ownWindowId < 0) return report(instrumentation, "OWN_WINDOW_ID_MISSING")

        val focused = ui.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focused.size != 1) return report(instrumentation, "FOCUSED_APP_COUNT_OTHER")
        val focusedWindow = focused.single()
        if (focusedWindow.id != ownWindowId || !ownsWindow(instrumentation, ownActivity, ownWindowId))
            return report(instrumentation, "FOCUSED_WINDOW_NOT_OWNED")
        if (focusedWindow.displayId != Display.DEFAULT_DISPLAY)
            return report(instrumentation, "WRONG_DISPLAY")
        val root = runCatching { focusedWindow.getRoot(0) }.getOrNull()
            ?: return report(instrumentation, "ROOT_MISSING")
        if (root.packageName?.toString() != fixture || root.windowId != focusedWindow.id)
            return report(instrumentation, "ROOT_MISMATCH")
        if (!runCatching { root.refresh() }.getOrDefault(false))
            return report(instrumentation, "ROOT_REFRESH_FAILED")
        val node = runCatching { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }.getOrNull()
            ?: return report(instrumentation, "FOCUSED_INPUT_MISSING")
        val before = snapshot(node, focusedWindow)
        val refreshed = runCatching { node.refresh() }.getOrDefault(false)
        if (!refreshed) return report(instrumentation, "PRE_${before}_REFRESH_0")
        val current = ui.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }.singleOrNull()
        val sameWindow = current?.id == ownWindowId &&
            current?.displayId == Display.DEFAULT_DISPLAY &&
            ownsWindow(instrumentation, ownActivity, ownWindowId)
        val currentRoot = if (sameWindow) current?.getRoot(0) else null
        val sameRoot = currentRoot != null && currentRoot == root &&
            currentRoot.packageName?.toString() == fixture &&
            currentRoot.windowId == focusedWindow.id
        val sameNode = sameRoot && runCatching {
            currentRoot?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) == node
        }.getOrDefault(false)
        report(instrumentation, "PRE_${before}_REFRESH_1_WINDOW_${bit(sameWindow)}" +
            "_ROOT_${bit(sameRoot)}_NODE_${bit(sameNode)}_POST_${snapshot(node, focusedWindow)}")
        } finally {
            try {
                serviceInfo.flags = savedFlags
                ui.serviceInfo = serviceInfo
            } finally {
                activity?.let { own ->
                    instrumentation.runOnMainSync { if (!own.isFinishing) own.finish() }
                    val cleanupDeadline = SystemClock.elapsedRealtime() + 2_000L
                    while (!own.isDestroyed && SystemClock.elapsedRealtime() < cleanupDeadline)
                        SystemClock.sleep(50)
                    assertTrue("OWN_FIXTURE_CLEANUP_TIMEOUT", own.isDestroyed)
                }
            }
        }
    }

    private fun ownsWindow(instrumentation: Instrumentation, activity: SmokeEditorActivity,
                           expectedId: Int): Boolean {
        var owned = false
        instrumentation.runOnMainSync {
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            owned = SmokeEditorActivity.current.get() === activity &&
                !activity.isFinishing && activity.hasWindowFocus() &&
                editor.isAttachedToWindow && editor.createAccessibilityNodeInfo().windowId == expectedId
        }
        return owned
    }

    private fun snapshot(node: AccessibilityNodeInfo, window: AccessibilityWindowInfo): String {
        val type = node.inputType
        val kind = when (type and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> "TEXT"
            InputType.TYPE_CLASS_NUMBER -> "NUMBER"
            InputType.TYPE_CLASS_PHONE -> "PHONE"
            InputType.TYPE_CLASS_DATETIME -> "DATETIME"
            else -> "NONE"
        }
        val variation = when (type and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_NORMAL -> "NORMAL"
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> "PASSWORD"
            else -> "OTHER"
        }
        return "P${bit(node.packageName?.toString() == fixture)}" +
            "_W${bit(node.windowId == window.id)}" +
            "_F${bit(node.isFocused)}_E${bit(node.isEditable)}_EN${bit(node.isEnabled)}" +
            "_PW${bit(node.isPassword)}" +
            "_FIELD_${bit(node.viewIdResourceName == "$fixture:id/local_test_editor")}" +
            "_TYPE_${kind}_VAR_${variation}" +
            "_EXPECTED_${bit(type == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE))}"
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

    private fun report(instrumentation: Instrumentation, code: String) {
        val value = "OWNED_FOCUSED_META_$code"
        assertTrue("UNSAFE_OWNED_METADATA_CODE", value.length <= 500 &&
            Regex("[A-Z0-9_]+").matches(value))
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_owned_focused_metadata", value)
        })
    }
}
