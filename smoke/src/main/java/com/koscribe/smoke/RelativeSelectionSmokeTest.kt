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

/** Owned, metadata-only input-selection calibration. Never invokes a content read or writer. */
@RunWith(AndroidJUnit4::class)
class RelativeSelectionSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val serviceCommand = "dumpsys activity service $product/.KoScribeService"
    private val soleFlag = "relativeSelectionReady"
    private lateinit var instrumentation: Instrumentation
    private var launchedFixture = false
    private var showRequestAccepted: Boolean? = null

    private data class Counters(val destination: Int, val commit: Int, val original: Int,
        val autoAttempt: Int, val recording: Int, val translation: Int)
    private data class Snapshot(val tokens: Map<String, String>) {
        fun word(name: String): String = tokens[name] ?: "MISSING"
        fun count(name: String): Int = tokens[name]?.toIntOrNull()?.takeIf { it in 0..100_000 }
            ?: throw AssertionError("BAD_SELECTION_COUNT_$name")
        fun generation(): Int = tokens["GENERATION"]?.toIntOrNull()?.takeIf { it in -1..100_000 }
            ?: throw AssertionError("BAD_SELECTION_GENERATION")
    }

    @Before fun requireSelectionOnlyArm() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("RELATIVE_SELECTION_REQUIRES_SOLE_OPT_IN",
            args.getString(soleFlag) == "true" && args.keySet().none {
                it != soleFlag && it.endsWith("Ready") && args.getString(it) == "true"
            })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        val lines = diagnostics()
        assertTrue("WRONG_SELECTION_ARM", field(lines, "Relative read probe arm") ==
            "FIXTURE_SELECTION" && field(lines, "Experiment arm") == "REVIEW")
        assertTrue("OTHER_EXPERIMENT_ENABLED", listOf("Auto insert enabled",
            "Stable overlay auto enabled", "Speech input enabled", "Translation input enabled",
            "Karrot service probe enabled", "Karrot empty connection probe enabled",
            "Karrot no-read probe enabled").all { field(lines, it) == "false" } &&
            field(lines, "Review surface probe").startsWith("arm=OFF state=IDLE "))
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        assertTrue("DRAFT_OR_CLEANUP_PRESENT", field(lines, "Draft phase") == "NONE" &&
            field(lines, "Overlay cleanup pending") == "false")
    }

    @After fun finishOnlyOwnedFixture() {
        if (!::instrumentation.isInitialized || !launchedFixture) return
        instrumentation.runOnMainSync {
            SmokeEditorActivity.current.get()?.takeIf {
                it.intent.getBooleanExtra(SmokeEditorActivity.EXTRA_RELATIVE_READ_FIXTURE, false)
            }?.finish()
        }
    }

    @Test fun currentGenerationSelectionCallbacksTrackOwnedChangesWithoutContentRead() {
        val editor = launchAndFocusEmpty()
        val before = counters()
        val initial = awaitStage("INITIAL_SELECTION_NOT_TRACKED", editor, "", 0, 0) {
            it.word("STATE") == "TRACKING" && it.word("PACKAGE") == "FIXTURE" &&
                it.word("SHAPE") == "COLLAPSED" && it.word("POSITION") == "ORIGIN" &&
                it.word("SAMPLE_RANGE") == "ZERO" &&
                it.word("SOURCE") in setOf("INITIAL_ONLY", "CALLBACK_CURRENT_GEN")
        }
        val generation = initial.generation()
        assertTrue("INITIAL_GENERATION_UNAVAILABLE", generation >= 0)
        val starts = initial.count("STARTS")
        val finishes = initial.count("FINISHES")
        val restarts = initial.count("RESTARTS")
        assertTrue("INITIAL_ONLY_PROMOTED_WITHOUT_CALLBACK",
            initial.word("SOURCE") != "CALLBACK_CURRENT_GEN" || initial.count("CALLBACKS_GEN") > 0)
        report("INITIAL_${initial.word("SOURCE")}", initial)

        mutate(editor, "ABCD", 4, 4)
        val end = awaitCallbackStage("CARET4", editor, "ABCD", 4, 4, initial,
            generation, starts, finishes, restarts, "COLLAPSED", "NONZERO")
        mutate(editor, "ABCD", 0, 4)
        val selected = awaitCallbackStage("RANGE_0_4", editor, "ABCD", 0, 4, end,
            generation, starts, finishes, restarts, "SELECTED", "RANGE")
        mutate(editor, "", 0, 0)
        awaitCallbackStage("ZERO", editor, "", 0, 0, selected,
            generation, starts, finishes, restarts, "COLLAPSED", "ORIGIN")
        assertTrue("SELECTION_CALIBRATION_USED_WRITER_OR_RECORDING", counters() == before)
    }

    private fun awaitCallbackStage(label: String, editor: EditText, text: String,
        start: Int, end: Int, previous: Snapshot, generation: Int, starts: Int,
        finishes: Int, restarts: Int, shape: String, position: String): Snapshot {
        val priorGen = previous.count("CALLBACKS_GEN")
        val priorTotal = previous.count("CALLBACKS_TOTAL")
        val outcome = awaitStage("${label}_CURRENT_GEN_CALLBACK_NOT_OBSERVED",
            editor, text, start, end) { now ->
            requireSameSession(now, generation, starts, finishes, restarts, label,
                editor, text, start, end)
            now.word("SOURCE") == "CALLBACK_CURRENT_GEN" &&
                now.count("CALLBACKS_GEN") > priorGen &&
                now.count("CALLBACKS_TOTAL") > priorTotal &&
                now.word("SHAPE") == shape && now.word("POSITION") == position &&
                now.word("SAMPLE_RANGE") == label
        }
        report(label, outcome)
        return outcome
    }

    private fun requireSameSession(now: Snapshot, generation: Int, starts: Int,
        finishes: Int, restarts: Int, label: String, editor: EditText, text: String,
        start: Int, end: Int) {
        if (now.word("STATE") != "TRACKING" || now.word("PACKAGE") != "FIXTURE" ||
            now.generation() != generation || now.count("STARTS") != starts ||
            now.count("FINISHES") != finishes || now.count("RESTARTS") != restarts)
            fail("${label}_INPUT_SESSION_CHANGED", now, editor, text, start, end)
    }

    private fun awaitStage(code: String, editor: EditText, text: String,
        start: Int, end: Int, condition: (Snapshot) -> Boolean): Snapshot {
        val deadline = SystemClock.elapsedRealtime() + 3_000L
        var last: Snapshot? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            val now = status()
            last = now
            if (now.word("CONTENT_READ") != "DISABLED" || now.word("ELIGIBLE") != "NEVER" ||
                now.word("COUNTERS") != "OK") fail("${code}_SCOPE_CHANGED", now,
                editor, text, start, end)
            if (condition(now) && directMatches(editor, text, start, end) &&
                maskedRemoteNodeReady() && defaultDisplayImeVisible() && selectedSwiftKey())
                return now
            SystemClock.sleep(80)
        }
        fail(code, last, editor, text, start, end)
    }

    private fun launchAndFocusEmpty(): EditText {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_RELATIVE_READ_FIXTURE, true)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        launchedFixture = true
        val activity = try {
            await("RELATIVE_SELECTION_FIXTURE_NOT_AVAILABLE") {
                SmokeEditorActivity.current.get()?.takeIf { it.relativeReadEditor() != null }
            }
        } catch (_: AssertionError) {
            fail("RELATIVE_SELECTION_FIXTURE_NOT_AVAILABLE", runCatching { status() }.getOrNull(),
                null, "", 0, 0)
        }
        try {
            await("OWN_SELECTION_WINDOW_NOT_FOCUSED") {
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
                true.takeIf { ready }
            }
        } catch (_: AssertionError) {
            fail("OWN_SELECTION_WINDOW_NOT_FOCUSED", runCatching { status() }.getOrNull(),
                activity.relativeReadEditor(), "", 0, 0)
        }
        lateinit var editor: EditText
        instrumentation.runOnMainSync {
            editor = activity.relativeReadEditor() ?: throw AssertionError("SELECTION_EDITOR_MISSING")
            editor.setText("")
            editor.setSelection(0)
            assertTrue("SELECTION_FIELD_FOCUS_FAILED",
                editor.requestFocus() && editor.isFocused && editor.isAttachedToWindow &&
                    editor.windowToken != null && activity.window.decorView.hasWindowFocus() &&
                    SmokeEditorActivity.current.get() === activity)
            showRequestAccepted =
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        try {
            await("SELECTION_FIXTURE_IME_NOT_READY") {
                true.takeIf { directMatches(editor, "", 0, 0) && maskedRemoteNodeReady() &&
                    defaultDisplayImeVisible() && selectedSwiftKey() }
            }
        } catch (_: AssertionError) {
            fail("SELECTION_FIXTURE_IME_NOT_READY", runCatching { status() }.getOrNull(),
                editor, "", 0, 0)
        }
        try {
            automation().waitForIdle(500L, 3_000L)
        } catch (_: java.util.concurrent.TimeoutException) {
            fail("SELECTION_FIXTURE_EVENTS_NOT_IDLE", runCatching { status() }.getOrNull(),
                editor, "", 0, 0)
        }
        if (!directMatches(editor, "", 0, 0) || !maskedRemoteNodeReady() ||
            !defaultDisplayImeVisible() || !selectedSwiftKey())
            fail("SELECTION_FIXTURE_CHANGED_AFTER_IDLE", runCatching { status() }.getOrNull(),
                editor, "", 0, 0)
        return editor
    }

    private fun mutate(editor: EditText, text: String, start: Int, end: Int) {
        instrumentation.runOnMainSync {
            assertTrue("SELECTION_EDITOR_LOST_BEFORE_SYNTHETIC_CHANGE",
                editor.isAttachedToWindow && editor.isFocused && editor.windowToken != null)
            // Ordinary owned Editable change; never an IME command or product callback injection.
            if (editor.text.toString() != text)
                editor.text.replace(0, editor.text.length, text)
            editor.setSelection(start, end)
        }
        assertTrue("OWN_SELECTION_MUTATION_NOT_APPLIED", directMatches(editor, text, start, end))
    }

    private fun directMatches(editor: EditText, text: String, start: Int, end: Int): Boolean {
        var matches = false
        instrumentation.runOnMainSync {
            matches = editor.isAttachedToWindow && editor.isFocused &&
                editor.text.toString() == text && editor.selectionStart == start &&
                editor.selectionEnd == end &&
                (editor.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        }
        return matches
    }

    private fun maskedRemoteNodeReady(): Boolean {
        val windows = automation().windows
        val focused = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
            it.isFocused && it.displayId == Display.DEFAULT_DISPLAY }
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

    private fun fail(code: String, now: Snapshot?, editor: EditText?, text: String?,
        start: Int?, end: Int?): Nothing {
        val statusCode = if (now == null) "STATUS_UNAVAILABLE" else
            "STATE_${now.word("STATE")}_PACKAGE_${now.word("PACKAGE")}" +
                "_GEN_${now.word("GENERATION")}_SOURCE_${now.word("SOURCE")}" +
                "_SHAPE_${now.word("SHAPE")}_POSITION_${now.word("POSITION")}" +
                "_RANGE_${now.word("SAMPLE_RANGE")}_COMPOSITION_${now.word("COMPOSITION")}" +
                "_CBGEN_${now.word("CALLBACKS_GEN")}" +
                "_CBTOTAL_${now.word("CALLBACKS_TOTAL")}_STARTS_${now.word("STARTS")}" +
                "_FINISHES_${now.word("FINISHES")}_RESTARTS_${now.word("RESTARTS")}" +
                "_SIGNAL_${now.word("SIGNAL_ORDER")}_${now.word("LAST_SIGNAL")}" +
                "_READ_${now.word("CONTENT_READ")}_ELIGIBLE_${now.word("ELIGIBLE")}"
        val direct = if (editor == null || text == null || start == null || end == null) "UNKNOWN"
            else if (directMatches(editor, text, start, end)) "MATCH" else "DIFFERENT"
        val evidence = "${code}_${statusCode}_DIRECT_$direct" +
            "_MASK_${bit(runCatching { maskedRemoteNodeReady() }.getOrDefault(false))}" +
            "_IME_${bit(defaultDisplayImeVisible())}_SWIFTKEY_${bit(selectedSwiftKey())}" +
            "_SHOW_${when (showRequestAccepted) { true -> "ACCEPTED"; false -> "DECLINED";
                null -> "NOT_REQUESTED" }}_${readinessEvidence(editor, text, start, end)}"
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_SELECTION_FAILURE_$evidence\n")
        })
        throw AssertionError(evidence)
    }

    private fun report(label: String, now: Snapshot) {
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_SELECTION_$label" +
                "_GEN_${now.word("GENERATION")}_SOURCE_${now.word("SOURCE")}" +
                "_SHAPE_${now.word("SHAPE")}_RANGE_${now.word("SAMPLE_RANGE")}" +
                "_COMPOSITION_${now.word("COMPOSITION")}" +
                "_CBGEN_${now.word("CALLBACKS_GEN")}_CBTOTAL_${now.word("CALLBACKS_TOTAL")}" +
                "_STARTS_${now.word("STARTS")}_FINISHES_${now.word("FINISHES")}" +
                "_RESTARTS_${now.word("RESTARTS")}_ORDER_${now.word("SIGNAL_ORDER")}" +
                "_LAST_SIGNAL_${now.word("LAST_SIGNAL")}_SIGNALS_GEN_${now.word("SIGNALS_GEN")}" +
                "_CONTENT_READ_${now.word("CONTENT_READ")}_ELIGIBLE_${now.word("ELIGIBLE")}\n")
        })
    }

    private fun readinessEvidence(editor: EditText?, text: String?, start: Int?, end: Int?): String {
        var local = "LOCAL_UNAVAILABLE"
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
            val decor = activity?.window?.decorView
            val candidate = activity?.relativeReadEditor()
            local = "LOCAL_OWNER_${bit(activity != null && candidate != null &&
                candidate === editor)}" +
                "_DECOR_ATTACHED_${bit(decor?.isAttachedToWindow == true)}" +
                "_WINDOW_FOCUS_${bit(decor?.hasWindowFocus() == true)}" +
                "_FIELD_ATTACHED_${bit(candidate?.isAttachedToWindow == true)}" +
                "_FIELD_SHOWN_${bit(candidate?.isShown == true)}" +
                "_FIELD_FOCUS_${bit(candidate?.isFocused == true)}" +
                "_TOKEN_${bit(candidate?.windowToken != null)}" +
                "_SIZE_${bit(candidate != null && candidate.width > 0 && candidate.height > 0)}" +
                "_TEXT_MATCH_${if (text == null) "UNKNOWN" else bit(candidate?.text?.toString() == text)}" +
                "_SELECTION_MATCH_${if (start == null || end == null) "UNKNOWN" else
                    bit(candidate?.selectionStart == start && candidate?.selectionEnd == end)}" +
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
        return "${local}_FOCUSED_APPS_${focusedApps.size.coerceAtMost(9)}" +
            "_REMOTE_${remote}_IME_COUNT_${imeCount.coerceAtMost(9)}"
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey

    private fun status(): Snapshot {
        val descriptor = automation().executeShellCommand(
            "$serviceCommand --relative-editor-selection-status")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(5).map(String::trim).toList()
        }
        val line = lines.singleOrNull { it.startsWith("RELATIVE_SELECTION_V1 ") }
            ?: throw AssertionError("RELATIVE_SELECTION_STATUS_MISSING")
        val entries = line.removePrefix("RELATIVE_SELECTION_V1 ").split(' ').map {
            val parts = it.split('=', limit = 2)
            if (parts.size != 2 || !Regex("[A-Z_]+").matches(parts[0]) ||
                !Regex("[A-Z0-9_-]+").matches(parts[1]))
                throw AssertionError("RELATIVE_SELECTION_STATUS_UNSAFE")
            parts[0] to parts[1]
        }
        assertTrue("DUPLICATE_SELECTION_STATUS_FIELD", entries.map { it.first }.distinct().size ==
            entries.size)
        return Snapshot(entries.toMap())
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
