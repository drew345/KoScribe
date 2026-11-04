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
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Hard no-write, fixture-only comparison; every invocation selects one arm and case. */
@RunWith(AndroidJUnit4::class)
class ReviewSurfaceComparisonSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val sample = "Synthetic alpha omega."
    private val caret = "Synthetic alpha ".length
    private val flags = listOf("activityMiddleReady", "overlayMiddleReady",
        "activityAlternateReady", "overlayAlternateReady",
        "activityRebindReady", "overlayRebindReady")
    private lateinit var instrumentation: Instrumentation
    private var probeStarted = false

    private enum class Arm { ACTIVITY, OVERLAY }
    private enum class Case { MIDDLE, ALTERNATE, REBIND }
    private data class Counters(val captures: Int, val commits: Int, val attempts: Int,
        val recordingProofs: Int)
    private data class Probe(val arm: String, val state: String, val samples: Int,
        val edits: Int, val blocked: Int, val target: String?, val returned: String?,
        val outcome: String?, val firstNull: String?, val effects: String?,
        val reviewSamples: Int?, val reviewRoots: Int?, val reviewNodes: Int?,
        val inputCounts: String?, val returnMismatchSeen: String?)
    private data class FixtureBaseline(val activity: SmokeEditorActivity,
        val original: EditText, val epoch: Int)

    @Test fun activityMiddleCaret() = runCase(Arm.ACTIVITY, Case.MIDDLE, "activityMiddleReady")
    @Test fun overlayMiddleCaret() = runCase(Arm.OVERLAY, Case.MIDDLE, "overlayMiddleReady")
    @Test fun activityIdenticalAlternate() =
        runCase(Arm.ACTIVITY, Case.ALTERNATE, "activityAlternateReady")
    @Test fun overlayIdenticalAlternate() =
        runCase(Arm.OVERLAY, Case.ALTERNATE, "overlayAlternateReady")
    @Test fun activitySameViewRebind() =
        runCase(Arm.ACTIVITY, Case.REBIND, "activityRebindReady")
    @Test fun overlaySameViewRebind() =
        runCase(Arm.OVERLAY, Case.REBIND, "overlayRebindReady")

    @After fun dismissOwnProbeOnFailure() {
        if (::instrumentation.isInitialized && probeStarted) runCatching {
            ownControl("Dismiss probe")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun runCase(arm: Arm, case: Case, readyFlag: String) {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("REQUIRES_ONE_EXACT_READY_FLAG",
            args.getString(readyFlag) == "true" &&
                flags.count { args.getString(it) == "true" } == 1 &&
                args.keySet().none { it.endsWith("Ready") && it !in flags &&
                    args.getString(it) == "true" })
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        val initial = probe()
        assertTrue("WRONG_PROBE_ARM_OR_STATE", initial.arm == arm.name &&
            initial.state in setOf("IDLE", "COMPLETE", "CANCELLED", "REFUSED") &&
            ownControl("Dismiss probe") == null && serviceField("Draft phase") == "NONE" &&
            serviceField("Overlay cleanup pending") == "false" &&
            (initial.state == "IDLE" || initial.effects ==
                "microphone=NEVER_STARTED authority=NONE cleanup=CLEAN"))
        val before = counters()
        assertTrue("WRITER_OR_RECORDING_COUNTERS_NOT_ZERO_AT_START",
            before.captures == 0 && before.commits == 0 && before.attempts == 0 &&
                before.recordingProofs == 0)
        assertTrue("WRONG_PROBE_PRODUCT_FLAGS", serviceField("Experiment arm") == "REVIEW" &&
            serviceField("Auto insert enabled") == "false" &&
            serviceField("Speech input enabled") == "false" &&
            serviceField("Translation input enabled") == "false")
        val baseline = launchAndSeedFixture()
        assertNoWrite(before)
        openProbe()
        probeStarted = true
        val editing = await("OWN_EDITABLE_PROBE_NOT_READY", 8_000) {
            val state = probe()
            val editor = ownControl("Review surface probe English")
            state.takeIf { it.arm == arm.name && it.state == "EDITING" &&
                editor?.isEditable == true && editor.isFocused && editor.isVisibleToUser &&
                imeVisible() && selectedSwiftKey() }
        }
        report("review", "ARM_${arm.name}_CASE_${case.name}_STATE_${editing.state}_" +
            "OWN_EDITABLE_1_IME_1")
        assertNoWrite(before)

        if (case == Case.MIDDLE) {
            val initialText = ownControl("Review surface probe English")?.text?.toString()
                ?: throw AssertionError("OWN_ENGLISH_TEXT_UNAVAILABLE")
            report("action", "WAITING_FOR_USER_SWIFTKEY_SINGLE_EDIT_${arm.name}")
            await("REAL_SWIFTKEY_EDIT_NOT_OBSERVED", 90_000) {
                val current = probe()
                val editor = ownControl("Review surface probe English")
                assertNoWrite(before)
                current.takeIf { it.state == "EDITING" && it.edits > editing.edits &&
                    editor?.text?.toString() != initialText && editor?.isFocused == true &&
                    imeVisible() && selectedSwiftKey() }
            }
            report("edit", "OWN_TEXT_CHANGED_1_WATCHER_EDIT_1_SWIFTKEY_SELECTED_1_" +
                "WAITING_FOR_USER_DISMISS")
        } else {
            mutateOwnedFixture(case, baseline)
            assertTrue("OWN_REVIEW_FOCUS_LOST_AFTER_FIXTURE_MUTATION",
                ownControl("Review surface probe English")?.isFocused == true)
            report("action", "FIXTURE_ONLY_${case.name}_NO_REVIEW_TYPING")
        }
        assertNoWrite(before)
        if (case != Case.MIDDLE) {
            val dismiss = await("PROBE_DISMISS_NOT_AVAILABLE") {
                ownControl("Dismiss probe")?.takeIf { it.isEnabled && it.isClickable }
            }
            assertTrue("PROBE_DISMISS_FAILED",
                dismiss.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        val terminal = await("PROBE_RETURN_NOT_OBSERVED",
            if (case == Case.MIDDLE) 30_000 else 5_000) {
            probe().takeIf { it.state in setOf("COMPLETE", "REFUSED", "CANCELLED") }
        }
        probeStarted = false
        await("PROBE_OWN_SURFACE_STILL_VISIBLE", 2_000) {
            true.takeIf { ownControl("Dismiss probe") == null &&
                ownControl("Review surface probe English") == null }
        }
        assertNoWrite(before)
        assertTrue("DRAFT_OR_MIC_CLEANUP_NOT_COMPLETE", serviceField("Draft phase") == "NONE" &&
            serviceField("Overlay cleanup pending") == "false")
        assertTrue("PROBE_EFFECT_AUTHORITY_NOT_NONE",
            terminal.effects == "microphone=NEVER_STARTED authority=NONE cleanup=CLEAN")
        assertTrue("SWIFTKEY_SELECTION_CHANGED", selectedSwiftKey())
        val oracle = fixtureOracle(case, baseline)
        val observed = Regex("identity=(MATCH|MISMATCH|UNKNOWN)")
            .find(terminal.returned ?: "")?.groupValues?.get(1) ?: "UNKNOWN"
        val returnCode = terminal.returned ?: ""
        fun category(name: String, choices: String): String =
            Regex("$name=($choices)").find(returnCode)?.groupValues?.get(1) ?: "UNKNOWN"
        val caretCode = category("caret", "MATCH|MISMATCH|UNKNOWN")
        val contextCode = category("context", "MATCH|MISMATCH|UNKNOWN")
        val generationCode = category("generation", "FRESH|SAME|UNKNOWN")
        val inputCode = category("input", "TARGET|OWN|OTHER|NONE")
        val comparison = when (oracle) {
            "UNCHANGED" -> when {
                observed == "UNKNOWN" || caretCode == "UNKNOWN" ||
                    contextCode == "UNKNOWN" || generationCode == "UNKNOWN" -> "UNKNOWN"
                observed == "MATCH" && caretCode == "MATCH" &&
                    contextCode == "MATCH" && generationCode == "FRESH" &&
                    inputCode == "TARGET" -> "MATCH"
                else -> "MISMATCH"
            }
            "CHANGED" -> when (observed) {
                "MISMATCH" -> "MATCH"
                "MATCH" -> "MISMATCH"
                else -> "UNKNOWN"
            }
            else -> "UNKNOWN"
        }
        val limitation = if (case == Case.REBIND && oracle == "CHANGED" && observed == "MATCH")
            "SAME_VIEW_INDISTINGUISHABLE" else "NONE"
        report("result", "ARM_${arm.name}_CASE_${case.name}_ORACLE_${oracle}_" +
            "OBSERVED_${observed}_CARET_${caretCode}_CONTEXT_${contextCode}_" +
            "GENERATION_${generationCode}_INPUT_${inputCode}_COMPARISON_${comparison}_" +
            "LIMITATION_${limitation}_" +
            "STATE_${terminal.state}_SAMPLES_${terminal.samples.coerceAtMost(999)}_" +
            "EDITS_${terminal.edits.coerceAtMost(999)}_BLOCKED_${terminal.blocked.coerceAtMost(999)}_" +
            "REVIEW_SAMPLES_${terminal.reviewSamples?.coerceAtMost(999) ?: "UNKNOWN"}_" +
            "ROOT_${terminal.reviewRoots?.coerceAtMost(999) ?: "UNKNOWN"}_" +
            "NODE_${terminal.reviewNodes?.coerceAtMost(999) ?: "UNKNOWN"}_" +
            "FIRST_NULL_${terminal.firstNull ?: "UNKNOWN"}")
        terminal.target?.let { report("target", it) }
        terminal.returned?.let { report("return", it) }
        terminal.outcome?.let { report("outcome", it) }
        terminal.inputCounts?.let { report("input_counts", it) }
        terminal.returnMismatchSeen?.let { report("return_mismatch_seen", it) }
    }

    private fun launchAndSeedFixture(): FixtureBaseline {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_REVIEW_SURFACE_PROBE, true)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        await("OWN_FIXTURE_NOT_AVAILABLE") { SmokeEditorActivity.current.get() }
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            val original = activity.probeOriginalEditor()
            val alternate = activity.probeAlternateEditor()
                ?: throw AssertionError("ALTERNATE_EDITOR_MISSING")
            original.setText(sample)
            original.setSelection(caret)
            alternate.setText(sample)
            alternate.setSelection(caret)
            original.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(original, InputMethodManager.SHOW_IMPLICIT)
        }
        await("OWN_MIDDLE_CARET_OR_SWIFTKEY_NOT_READY") {
            val editor = fixtureEditor()
            editor?.takeIf { it.isFocused && it.isEditable &&
                it.textSelectionStart == caret && it.textSelectionEnd == caret &&
                imeVisible() && selectedSwiftKey() }
        }
        lateinit var baseline: FixtureBaseline
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            baseline = FixtureBaseline(activity, activity.probeOriginalEditor(),
                activity.probeLogicalEpoch())
        }
        return baseline
    }

    private fun mutateOwnedFixture(case: Case, baseline: FixtureBaseline) {
        var changed = false
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
            if (activity === baseline.activity) changed = when (case) {
                Case.ALTERNATE -> activity.probeSelectIdenticalAlternate()
                Case.REBIND -> activity.probeRebindSameView()
                Case.MIDDLE -> false
            }
        }
        assertTrue("OWN_FIXTURE_MUTATION_NOT_APPLIED", changed)
    }

    private fun fixtureOracle(case: Case, baseline: FixtureBaseline): String {
        var result = "UNKNOWN"
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
            if (activity !== baseline.activity || activity.probeOriginalEditor() !== baseline.original)
                return@runOnMainSync
            val original = baseline.original
            val alternate = activity.probeAlternateEditor()
            val sameTextCaret = original.text.toString() == sample &&
                original.selectionStart == caret && original.selectionEnd == caret
            result = when (case) {
                Case.MIDDLE -> if (sameTextCaret && original.isFocused &&
                    activity.probeLogicalEpoch() == baseline.epoch) "UNCHANGED" else "UNKNOWN"
                Case.ALTERNATE -> if (sameTextCaret && alternate?.isFocused == true &&
                    alternate.text.toString() == sample && alternate.selectionStart == caret &&
                    alternate.selectionEnd == caret && !original.isFocused) "CHANGED" else "UNKNOWN"
                Case.REBIND -> if (sameTextCaret && original.isFocused &&
                    activity.probeLogicalEpoch() == baseline.epoch + 1)
                    "CHANGED" else "UNKNOWN"
            }
        }
        return result
    }

    private fun openProbe() {
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        clickOwnButton("요")
    }

    private fun ownControl(description: String): AccessibilityNodeInfo? {
        require(description in setOf("Review surface probe English", "Dismiss probe"))
        val matches = ownedNodes().filter {
            it.contentDescription?.toString() == description && it.isVisibleToUser
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_PROBE_CONTROL")
        return matches.singleOrNull()
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in setOf("Ko", "Close", "요"))
        val matches = ownedNodes().filter {
            it.className?.toString() == "android.widget.Button" &&
                (matchesOwnButtonLabel(it.text, label) ||
                    (label == "Ko" && it.viewIdResourceName == "$product:id/collapsed_bubble")) &&
                it.isVisibleToUser && it.isEnabled && it.isClickable
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_BUTTON_$label")
        return matches.singleOrNull()
    }

    private fun clickOwnButton(label: String) {
        val button = await("OWN_BUTTON_NOT_AVAILABLE_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label",
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun ownedNodes(): List<AccessibilityNodeInfo> {
        val result = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != product) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node.packageName?.toString() != product) continue
                result.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
            }
        }
        return result
    }

    private fun fixtureEditor(): AccessibilityNodeInfo? = automation().windows
        .asSequence().mapNotNull { it.getRoot(0) }
        .filter { it.packageName?.toString() == fixture }
        .flatMap { it.findAccessibilityNodeInfosByViewId("$fixture:id/local_test_editor").asSequence() }
        .singleOrNull()

    private fun automation(): UiAutomation = instrumentation.getUiAutomation(
        UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey

    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun serviceLines(): List<String> {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(180).map(String::trim).toList()
        }
    }

    private fun serviceField(label: String): String {
        val line = serviceLines().firstOrNull { it.startsWith("$label: ") }
            ?: throw AssertionError("PRODUCT_DIAGNOSTIC_MISSING_$label")
        return line.substringAfter(": ").trim()
    }

    private fun counters(): Counters {
        val lines = serviceLines()
        fun count(label: String): Int {
            val line = lines.firstOrNull { it.startsWith("$label: ") }
                ?: throw AssertionError("BAD_PRODUCT_COUNT_$label")
            return line.substringAfter(": ").trim().toIntOrNull()
                ?.takeIf { it in 0..100_000 }
                ?: throw AssertionError("BAD_PRODUCT_COUNT_$label")
        }
        return Counters(count("Destination capture count"), count("Commit request count"),
            count("Auto insert attempt count"), count("Unbound recording proof count"))
    }

    private fun assertNoWrite(before: Counters) {
        val now = counters()
        assertTrue("PROBE_REACHED_DESTINATION_OR_RECORDING_PATH",
            now == before)
    }

    private fun probe(): Probe {
        val lines = serviceLines()
        val header = lines.firstOrNull { it.startsWith("Review surface probe: ") }
            ?: throw AssertionError("PROBE_DIAGNOSTIC_MISSING")
        val match = Regex("Review surface probe: arm=(ACTIVITY|OVERLAY) " +
            "state=(IDLE|OPENING|EDITING|RETURNING|COMPLETE|CANCELLED|REFUSED) " +
            "samples=([0-9]+) edits=([0-9]+) blocked=([0-9]+)").matchEntire(header)
            ?: throw AssertionError("BAD_PROBE_HEADER")
        fun safe(label: String): String? = lines.firstOrNull { it.startsWith("$label: ") }
            ?.substringAfter(": ")?.trim()?.takeIf {
                it.length <= 400 && Regex("[A-Za-z0-9_= |]+").matches(it)
            }
        val firstNull = Regex("firstNull=(ORIGINAL|REVIEW|HOST|IME|OTHER|NONE|UNKNOWN)")
            .find(safe("Probe inventory") ?: "")?.groupValues?.get(1)
        val reviewCounts = lines.firstOrNull {
            it.startsWith("Probe review samples/root/node: ")
        }?.substringAfter(": ")?.split('/')?.map { it.toIntOrNull() }
            ?.takeIf { it.size == 3 && it.all { count -> count != null && count in 0..999 } }
        val inputCountValues = lines.firstOrNull {
            it.startsWith("Probe input starts/finishes/own/target/restarting: ")
        }?.substringAfter(": ")?.trim()?.takeIf {
            Regex("[0-9]{1,3}(/[0-9]{1,3}){4}").matches(it)
        }?.split('/')
        val inputCounts = inputCountValues?.let {
            "START_${it[0]}_FINISH_${it[1]}_OWN_${it[2]}_TARGET_${it[3]}_RESTART_${it[4]}"
        }
        val mismatchSeen = lines.firstOrNull {
            it.startsWith("Probe return mismatch observed: ")
        }?.substringAfter(": ")?.trim()?.takeIf { it == "true" || it == "false" }
        return Probe(match.groupValues[1], match.groupValues[2],
            match.groupValues[3].toInt(), match.groupValues[4].toInt(),
            match.groupValues[5].toInt(), safe("Probe target"), safe("Probe return"),
            safe("Probe outcome"), firstNull, safe("Probe effects"),
            reviewCounts?.get(0), reviewCounts?.get(1), reviewCounts?.get(2),
            inputCounts, mismatchSeen)
    }

    private fun report(label: String, code: String) {
        assertTrue("UNSAFE_PROBE_REPORT", code.length <= 600 &&
            Regex("[A-Za-z0-9_= |]+").matches(code))
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_review_surface_$label", code)
        })
    }

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
