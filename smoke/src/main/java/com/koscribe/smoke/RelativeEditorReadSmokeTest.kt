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
import android.text.InputType
import android.view.Display
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

/** One scoped diagnostic read per method. No review, writer, clipboard, or Send action. */
@RunWith(AndroidJUnit4::class)
class RelativeEditorReadSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val serviceCommand = "dumpsys activity service $product/.KoScribeService"
    private val flags = listOf("relativeEmptyReady", "relativeMiddleReady", "relativeSelectedReady")
    private lateinit var instrumentation: Instrumentation
    private var launchedFixture = false

    private data class Case(val flag: String, val text: String, val start: Int, val end: Int,
        val payload: String, val inputSelection: String, val agreement: String)
    private data class Counters(val destination: Int, val commit: Int, val original: Int,
        val autoAttempt: Int, val recording: Int, val translation: Int)

    @Before fun requireExactFixtureArm() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("RELATIVE_READ_REQUIRES_ONE_EXACT_READY_FLAG",
            flags.count { args.getString(it) == "true" } == 1 &&
                args.keySet().none { it.endsWith("Ready") && it !in flags &&
                    args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val lines = diagnostics()
        fun line(label: String) = field(lines, label)
        assertTrue("WRONG_RELATIVE_READ_ARM", line("Relative read probe arm") == "FIXTURE")
        assertTrue("WRONG_PRODUCT_ARM_OR_ACTIVE_DRAFT", line("Experiment arm") == "REVIEW" &&
            line("Draft phase") == "NONE" && line("Overlay cleanup pending") == "false")
        assertTrue("OTHER_EXPERIMENT_ENABLED", listOf("Auto insert enabled",
            "Stable overlay auto enabled", "Speech input enabled", "Translation input enabled",
            "Karrot service probe enabled", "Karrot empty connection probe enabled",
            "Karrot no-read probe enabled").all { line(it) == "false" } &&
            line("Review surface probe").startsWith("arm=OFF state=IDLE "))
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
    }

    @After fun finishOnlyOwnedFixture() {
        if (!::instrumentation.isInitialized || !launchedFixture) return
        instrumentation.runOnMainSync {
            SmokeEditorActivity.current.get()?.takeIf {
                it.intent.getBooleanExtra(SmokeEditorActivity.EXTRA_RELATIVE_READ_FIXTURE, false)
            }?.finish()
        }
    }

    @Test fun emptyCollapsedHasUnknownAbsoluteOffset() = runCase(Case(
        "relativeEmptyReady", "", 0, 0, "EMPTY", "COLLAPSED", "SHAPE_AGREES"))

    @Test fun middleCollapsedHasUnknownAbsoluteOffset() = runCase(Case(
        "relativeMiddleReady", "A🙂B", 3, 3, "NONEMPTY", "COLLAPSED", "SHAPE_AGREES"))

    @Test fun selectedEmojiLooksCollapsedWhenSelectedTextIsUnavailable() = runCase(Case(
        "relativeSelectedReady", "A🙂B", 1, 3, "NONEMPTY", "SELECTED", "SHAPE_CONTRADICTS"))

    private fun runCase(case: Case) {
        assumeTrue("WRONG_RELATIVE_READ_CASE",
            InstrumentationRegistry.getArguments().getString(case.flag) == "true")
        val editor = launchAndSeed(case)
        await("MASKED_SERVED_EDITOR_AND_SWIFTKEY_NOT_READY") {
            true.takeIf { maskedRemoteNodeReady() && defaultDisplayImeVisible() &&
                selectedSwiftKey() && directFixtureMatches(editor, case) }
        }
        await("PROBE_INPUT_NOT_STARTED_FOR_FIXTURE") {
            val status = status()
            true.takeIf { status["CURRENT_INPUT_STARTED"] == "YES" &&
                status["CURRENT_INPUT_PACKAGE"] == "FIXTURE" }
        }
        // The fixture launch and SwiftKey focus can publish late window events. Settle those
        // before spending the sole diagnostic read; product interruption rules remain strict.
        try {
            automation().waitForIdle(500L, 3_000L)
        } catch (_: java.util.concurrent.TimeoutException) {
            throw AssertionError("FIXTURE_WINDOW_EVENTS_NOT_IDLE_BEFORE_READ")
        }
        assertTrue("FIXTURE_PROOF_CHANGED_AFTER_IDLE", maskedRemoteNodeReady() &&
            defaultDisplayImeVisible() && selectedSwiftKey() && directFixtureMatches(editor, case))
        val ready = status()
        assertTrue("SERVED_INPUT_CHANGED_AFTER_IDLE", ready["CURRENT_INPUT_STARTED"] == "YES" &&
            ready["CURRENT_INPUT_PACKAGE"] == "FIXTURE")
        val before = counters()
        val started = command("--relative-editor-read-once")
        val terminal = setOf("OBSERVED", "REFUSED", "INTERRUPTED", "TIMEOUT", "UNAVAILABLE")
        assertTrue("RELATIVE_READ_DID_NOT_START_ONCE",
            started["STATE"] == "RUNNING" || started["STATE"] in terminal)
        val result = if (started["STATE"] in terminal) started else
            await("RELATIVE_READ_DID_NOT_FINISH", 5_000) {
                status().takeIf { it["STATE"] in terminal }
            }
        val outcome = "STATE_${result["STATE"] ?: "MISSING"}" +
            "_REASON_${result["REASON"] ?: "MISSING"}_CALL_${result["CALL"] ?: "MISSING"}"
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_READ_TERMINAL_$outcome\n")
        })
        assertTrue("WRITER_OR_RECORDING_EFFECT_$outcome", counters() == before)
        assertTrue("RELATIVE_READ_NOT_OBSERVED_$outcome", result["STATE"] == "OBSERVED")
        assertTrue("RELATIVE_READ_PAYLOAD_SHAPE_CHANGED", result["PAYLOAD"] == case.payload &&
            result["OFFSET"] == "UNKNOWN" && result["RELATIVE"] == "COLLAPSED")
        assertTrue("INDEPENDENT_SELECTION_DISAGREES", result["INPUT_SELECTION"] ==
            case.inputSelection && result["AGREEMENT"] == case.agreement &&
            result["INPUT_SOURCE"] in setOf("INITIAL_ONLY", "CALLBACK_CURRENT_GEN"))
        assertTrue("RELATIVE_READ_NOT_HARD_NO_WRITE", result["ELIGIBLE"] == "NEVER" &&
            result["CALL"] == "RETURNED")
        assertTrue("FIXTURE_CHANGED_DURING_READ", directFixtureMatches(editor, case))
        assertTrue("MASKED_NODE_CHANGED_DURING_READ", maskedRemoteNodeReady())
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_FIXTURE_${case.flag}_OBSERVED_NO_WRITE\n")
        })
    }

    private fun launchAndSeed(case: Case): EditText {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_RELATIVE_READ_FIXTURE, true)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        launchedFixture = true
        val activity = await("RELATIVE_FIXTURE_NOT_AVAILABLE") {
            SmokeEditorActivity.current.get()?.takeIf { it.relativeReadEditor() != null }
        }
        lateinit var editor: EditText
        instrumentation.runOnMainSync {
            editor = activity.relativeReadEditor() ?: throw AssertionError("RELATIVE_EDITOR_MISSING")
            editor.setText(case.text)
            editor.setSelection(case.start, case.end)
            editor.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        return editor
    }

    private fun directFixtureMatches(editor: EditText, case: Case): Boolean {
        var matches = false
        instrumentation.runOnMainSync {
            matches = editor.isAttachedToWindow && editor.isFocused &&
                editor.text.toString() == case.text && editor.selectionStart == case.start &&
                editor.selectionEnd == case.end &&
                (editor.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        }
        return matches
    }

    private fun maskedRemoteNodeReady(): Boolean {
        val focused = automation().windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
                it.displayId == Display.DEFAULT_DISPLAY
        }
        if (focused.size != 1) return false
        val window = focused.single()
        val root = window.getRoot(0) ?: return false
        if (root.packageName?.toString() != fixture || root.windowId != window.id) return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return node.packageName?.toString() == fixture && node.windowId == window.id &&
            !node.isFocused && !node.isEditable && node.isEnabled && node.isVisibleToUser &&
            !node.isPassword && node.inputType == InputType.TYPE_NULL &&
            node.viewIdResourceName.isNullOrBlank() && node.textSelectionStart == -1 &&
            node.textSelectionEnd == -1
    }

    private fun defaultDisplayImeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
            it.displayId == Display.DEFAULT_DISPLAY
    }

    private fun counters(): Counters {
        val lines = diagnostics()
        fun count(label: String): Int = field(lines, label).toIntOrNull()?.takeIf {
            it in 0..100_000
        } ?: throw AssertionError("BAD_PRODUCT_COUNT_$label")
        val translation = field(lines, "Translation requests/success/errors/cancels")
            .substringBefore('/').toIntOrNull() ?: throw AssertionError("BAD_TRANSLATION_COUNT")
        assertTrue("DRAFT_OR_CLEANUP_CHANGED", field(lines, "Draft phase") == "NONE" &&
            field(lines, "Overlay cleanup pending") == "false")
        return Counters(count("Destination capture count"), count("Commit request count"),
            count("Original intent captured count"), count("Auto insert attempt count"),
            count("Unbound recording proof count"), translation)
    }

    private fun diagnostics(): List<String> {
        val descriptor = automation().executeShellCommand(serviceCommand)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(190).map(String::trim).toList()
        }
    }

    private fun field(lines: List<String>, label: String): String = lines.firstOrNull {
        it.startsWith("$label: ")
    }?.substringAfter(": ")?.trim() ?: throw AssertionError("PRODUCT_DIAGNOSTIC_MISSING_$label")

    private fun status(): Map<String, String> = command("--relative-editor-read-status")

    private fun command(arg: String): Map<String, String> {
        val descriptor = automation().executeShellCommand("$serviceCommand $arg")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(5).map(String::trim).toList()
        }
        val line = lines.singleOrNull { it.startsWith("RELATIVE_READ_V1 ") }
            ?: throw AssertionError("RELATIVE_READ_STATUS_MISSING")
        val entries = line.removePrefix("RELATIVE_READ_V1 ").split(' ').map {
            val parts = it.split('=', limit = 2)
            if (parts.size != 2 || !Regex("[A-Z_]+").matches(parts[0]) ||
                !Regex("[A-Z0-9_]+").matches(parts[1]))
                throw AssertionError("RELATIVE_READ_STATUS_UNSAFE")
            parts[0] to parts[1]
        }
        assertTrue("DUPLICATE_RELATIVE_STATUS_FIELD", entries.map { it.first }.distinct().size ==
            entries.size)
        return entries.toMap()
    }

    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey

    private fun automation(): UiAutomation = instrumentation.getUiAutomation(
        UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000,
        probe: () -> T?): T {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(80)
        }
        throw AssertionError(code)
    }
}
