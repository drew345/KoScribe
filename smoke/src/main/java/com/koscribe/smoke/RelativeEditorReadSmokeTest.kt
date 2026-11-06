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
    private var showRequestAccepted: Boolean? = null

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
        awaitInitialReadiness(editor, case)
        try {
            await("PROBE_INPUT_NOT_STARTED_FOR_FIXTURE") {
                val status = status()
                true.takeIf { status["CURRENT_INPUT_STARTED"] == "YES" &&
                    status["CURRENT_INPUT_PACKAGE"] == "FIXTURE" }
            }
        } catch (_: AssertionError) {
            failReadiness("PROBE_INPUT_NOT_STARTED_FOR_FIXTURE", editor, case)
        }
        // The fixture launch and SwiftKey focus can publish late window events. Settle those
        // before spending the sole diagnostic read; product interruption rules remain strict.
        try {
            automation().waitForIdle(500L, 3_000L)
        } catch (_: java.util.concurrent.TimeoutException) {
            throw AssertionError("FIXTURE_WINDOW_EVENTS_NOT_IDLE_BEFORE_READ")
        }
        val settledWindows = automation().windows
        assertTrue("FIXTURE_PROOF_CHANGED_AFTER_IDLE", maskedRemoteNodeReady(settledWindows) &&
            defaultDisplayImeVisible(settledWindows) && selectedSwiftKey() &&
            directFixtureMatches(editor, case))
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
        try {
            await("OWN_FIXTURE_WINDOW_NOT_FOCUSED") {
                true.takeIf { ownedWindowReady(activity) }
            }
        } catch (_: AssertionError) {
            failReadiness("OWN_FIXTURE_WINDOW_NOT_FOCUSED", activity.relativeReadEditor(), case)
        }
        lateinit var editor: EditText
        instrumentation.runOnMainSync {
            assertTrue("OWN_FIXTURE_CHANGED_BEFORE_IME_REQUEST",
                SmokeEditorActivity.current.get() === activity &&
                    activity.window.decorView.hasWindowFocus())
            editor = activity.relativeReadEditor() ?: throw AssertionError("RELATIVE_EDITOR_MISSING")
            editor.setText(case.text)
            editor.setSelection(case.start, case.end)
            assertTrue("OWN_FIELD_FOCUS_FAILED_BEFORE_IME_REQUEST",
                editor.requestFocus() && editor.isFocused && editor.isAttachedToWindow &&
                    editor.windowToken != null && activity.window.decorView.hasWindowFocus() &&
                    SmokeEditorActivity.current.get() === activity)
            showRequestAccepted =
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        return editor
    }

    private fun ownedWindowReady(activity: SmokeEditorActivity): Boolean {
        var ready = false
        instrumentation.runOnMainSync {
            val decor = activity.window.decorView
            val candidate = activity.relativeReadEditor()
            ready = SmokeEditorActivity.current.get() === activity && candidate != null &&
                decor.isAttachedToWindow && decor.hasWindowFocus() &&
                decor.display?.displayId == Display.DEFAULT_DISPLAY &&
                candidate.isAttachedToWindow && candidate.windowToken != null &&
                candidate.width > 0 && candidate.height > 0
        }
        return ready
    }

    private fun awaitInitialReadiness(editor: EditText, case: Case) {
        val end = SystemClock.elapsedRealtime() + 8_000L
        while (SystemClock.elapsedRealtime() < end) {
            val currentWindows = automation().windows
            if (maskedRemoteNodeReady(currentWindows) &&
                defaultDisplayImeVisible(currentWindows) && selectedSwiftKey() &&
                directFixtureMatches(editor, case)) return
            SystemClock.sleep(80)
        }
        failReadiness("MASKED_SERVED_EDITOR_AND_SWIFTKEY_NOT_READY", editor, case)
    }

    private fun failReadiness(code: String, editor: EditText?, case: Case): Nothing {
        val evidence = readinessEvidence(editor, case)
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_READ_PRETRIGGER_${code}_$evidence\n")
        })
        throw AssertionError("${code}_$evidence")
    }

    private fun readinessEvidence(editor: EditText?, case: Case): String {
        var local = "LOCAL_UNAVAILABLE"
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
            val decor = activity?.window?.decorView
            val candidate = activity?.relativeReadEditor()
            local = "LOCAL_OWNER_${bit(activity != null && candidate != null && candidate === editor)}" +
                "_DECOR_ATTACHED_${bit(decor?.isAttachedToWindow == true)}" +
                "_WINDOW_FOCUS_${bit(decor?.hasWindowFocus() == true)}" +
                "_FIELD_ATTACHED_${bit(candidate?.isAttachedToWindow == true)}" +
                "_FIELD_SHOWN_${bit(candidate?.isShown == true)}" +
                "_FIELD_FOCUS_${bit(candidate?.isFocused == true)}" +
                "_TOKEN_${bit(candidate?.windowToken != null)}" +
                "_SIZE_${bit(candidate != null && candidate.width > 0 && candidate.height > 0)}" +
                "_TEXT_MATCH_${bit(candidate?.text?.toString() == case.text)}" +
                "_SELECTION_MATCH_${bit(candidate?.selectionStart == case.start &&
                    candidate?.selectionEnd == case.end)}" +
                "_TYPE_TEXT_${bit(candidate != null &&
                    (candidate.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT)}"
        }
        val windows = automation().windows
        val focusedApps = windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val imeCount = windows.count {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                it.displayId == Display.DEFAULT_DISPLAY
        }
        val remote = when {
            focusedApps.isEmpty() -> "APP_NONE"
            focusedApps.size > 1 -> "APP_MULTIPLE"
            focusedApps.single().displayId != Display.DEFAULT_DISPLAY -> "APP_OTHER_DISPLAY"
            else -> {
                val window = focusedApps.single()
                val root = window.getRoot(0)
                when {
                    root == null -> "ROOT_MISSING"
                    root.packageName?.toString() != fixture -> "ROOT_OTHER_PACKAGE"
                    root.windowId != window.id -> "ROOT_WINDOW_MISMATCH"
                    else -> {
                        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                        if (node == null) "NODE_MISSING" else
                            "NODE_PRESENT_PKG_${bit(node.packageName?.toString() == fixture)}" +
                                "_WINDOW_${bit(node.windowId == window.id)}" +
                                "_FOCUS_${bit(node.isFocused)}" +
                                "_EDITABLE_${bit(node.isEditable)}" +
                                "_ENABLED_${bit(node.isEnabled)}" +
                                "_VISIBLE_${bit(node.isVisibleToUser)}" +
                                "_PASSWORD_${bit(node.isPassword)}" +
                                "_TYPE_ZERO_${bit(node.inputType == InputType.TYPE_NULL)}" +
                                "_RESOURCE_ABSENT_${bit(node.viewIdResourceName.isNullOrBlank())}" +
                                "_SELECTION_UNKNOWN_${bit(node.textSelectionStart == -1 &&
                                    node.textSelectionEnd == -1)}"
                    }
                }
            }
        }
        val served = runCatching { status() }.getOrNull()
        val started = when (served?.get("CURRENT_INPUT_STARTED")) {
            "YES" -> "YES"; "NO" -> "NO"; else -> "UNKNOWN"
        }
        val packageCode = when (served?.get("CURRENT_INPUT_PACKAGE")) {
            "FIXTURE" -> "FIXTURE"; "KARROT" -> "KARROT"; "OTHER" -> "OTHER"
            "NONE" -> "NONE"; else -> "UNKNOWN"
        }
        val show = when (showRequestAccepted) {
            true -> "ACCEPTED"; false -> "DECLINED"; null -> "NOT_REQUESTED"
        }
        return "${local}_FOCUSED_APPS_${focusedApps.size.coerceAtMost(9)}" +
            "_REMOTE_${remote}_IME_COUNT_${imeCount.coerceAtMost(9)}" +
            "_SWIFTKEY_${bit(selectedSwiftKey())}_SHOW_$show" +
            "_INPUT_STARTED_${started}_INPUT_PACKAGE_$packageCode"
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

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

    private fun maskedRemoteNodeReady(
        currentWindows: List<AccessibilityWindowInfo> = automation().windows): Boolean {
        val focused = currentWindows.filter {
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

    private fun defaultDisplayImeVisible(
        currentWindows: List<AccessibilityWindowInfo> = automation().windows): Boolean = currentWindows.any {
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
