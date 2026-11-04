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
import org.junit.Test
import org.junit.runner.RunWith

/** Exact opt-in, owned-fixture-only write gate for the separate stable overlay arm. */
@RunWith(AndroidJUnit4::class)
class StableOverlayAutoSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val fixedKorean = "안녕하세요. 10분 후에 도착할게요."
    private val prefix = "앞🙂 "
    private val suffix = " 뒤🌙"
    private val flags = listOf("stableAlternateReady", "stableMiddleReady", "stableEmptyReady",
        "stableCancelReady", "stableCaretReady", "stableContextReady")
    private lateinit var instrumentation: Instrumentation
    private var sessionStarted = false
    private var cleanupBaseline: State? = null

    private enum class Case { ALTERNATE, MIDDLE, EMPTY, CANCEL, CARET, CONTEXT }
    private data class State(
        val arm: String, val enabled: Boolean, val auto: Boolean, val speech: Boolean,
        val translation: Boolean, val phase: String, val reason: String,
        val draft: String, val cleanup: Boolean, val confirmation: String,
        val originalCaptures: Int, val autoAttempts: Int, val autoFallbacks: Int,
        val destinationCaptures: Int, val commits: Int, val manualRenders: Int,
        val recordingProofs: Int
    )
    private data class Fixture(val activity: SmokeEditorActivity, val original: EditText,
        val alternate: EditText, val draftToken: Any, val text: String, val caret: Int)
    private data class ReadyControls(
        val insert: AccessibilityNodeInfo?, val copy: AccessibilityNodeInfo?,
        val cancel: AccessibilityNodeInfo?, val code: String
    ) {
        val allReady: Boolean get() = insert != null && copy != null && cancel != null
    }

    @Test fun changedIdenticalFieldFallsBackBeforeWrite() =
        runCase(Case.ALTERNATE, "stableAlternateReady")
    @Test fun unchangedMiddleCaretInsertsOnce() = runCase(Case.MIDDLE, "stableMiddleReady")
    @Test fun unchangedEmptyInsertsOnce() = runCase(Case.EMPTY, "stableEmptyReady")
    @Test fun reviewCancelNeverWrites() = runCase(Case.CANCEL, "stableCancelReady")
    @Test fun changedCaretFallsBackBeforeWrite() = runCase(Case.CARET, "stableCaretReady")
    @Test fun changedContextFallsBackBeforeWrite() = runCase(Case.CONTEXT, "stableContextReady")

    @After fun cancelOwnSessionOnFailure() {
        if (!::instrumentation.isInitialized || !sessionStarted) return
        val baseline = cleanupBaseline ?: return
        runCatching {
            val end = SystemClock.elapsedRealtime() + 2_000L
            while (SystemClock.elapsedRealtime() < end) {
                val current = state()
                if (current.draft == "NONE" ||
                    current.originalCaptures != baseline.originalCaptures + 1) return@runCatching
                val cancel = when (current.draft) {
                    "REVIEW" -> ownId("auto_overlay_cancel")?.takeIf {
                        it.isVisibleToUser && it.isEnabled && it.isClickable
                    }
                    "READY", "UNCERTAIN" -> readyControls().cancel
                    else -> null
                }
                if (cancel != null) {
                    cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return@runCatching // One dispatch even if the framework returns false.
                }
                SystemClock.sleep(100)
            }
        }
    }

    private fun runCase(case: Case, flag: String) {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("STABLE_AUTO_REQUIRES_ONE_EXACT_READY_FLAG",
            args.getString(flag) == "true" && flags.count { args.getString(it) == "true" } == 1 &&
                args.keySet().none { it.endsWith("Ready") && it !in flags &&
                    args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        val initial = state()
        assertTrue("WRONG_STABLE_OVERLAY_BUILD", initial.arm == "REVIEW" && initial.enabled &&
            !initial.auto && !initial.speech && !initial.translation &&
            initial.phase in setOf("IDLE", "CONSUMED", "FALLBACK", "CANCELLED", "REFUSED") &&
            initial.draft == "NONE" && !initial.cleanup && selectedSwiftKey())
        val fixture = launchAndSeed(case)
        val before = state()
        cleanupBaseline = before
        assertNoWrite(initial) // Seeding an owned field cannot trigger a stale product write.
        assertTrue("ORIGINAL_INTENT_CAPTURED_DURING_FIXTURE_SEED",
            before.originalCaptures == initial.originalCaptures)
        sessionStarted = true
        openReview()
        val armed = await("STABLE_REVIEW_NOT_ARMED", 8_000) {
            val current = state()
            if (current.phase in setOf("FALLBACK", "REFUSED", "CANCELLED"))
                throw AssertionError("STABLE_AUTO_DISARMED_BEFORE_REVIEW_${current.reason}")
            val editor = ownId("auto_overlay_english")
            current.takeIf { it.phase == "REVIEW" &&
                it.originalCaptures == before.originalCaptures + 1 &&
                it.autoFallbacks == before.autoFallbacks &&
                it.autoAttempts == before.autoAttempts &&
                it.destinationCaptures == before.destinationCaptures &&
                it.commits == before.commits && editor?.isFocused == true &&
                editor.isEditable && editor.isVisibleToUser && imeVisible() && selectedSwiftKey() }
        }
        report("review", "CASE_${case.name}_PHASE_${armed.phase}_FOCUS_1_IME_1")
        assertFixtureStable(fixture)
        assertNoWrite(before)

        when (case) {
            Case.CANCEL -> {
                clickOwnId("auto_overlay_cancel")
                val ended = await("STABLE_CANCEL_NOT_COMPLETE") { state().takeIf {
                    it.phase == "CANCELLED" && it.draft == "NONE" && !it.cleanup
                } }
                sessionStarted = false
                assertNoWrite(before)
                assertFixtureFields(fixture.text, fixture.text, fixture.caret, fixture.caret)
                assertOwnReviewGone()
                assertNoResultControls()
                report("outcome", "CANCELLED_REASON_${ended.reason}_NO_WRITE_1")
            }
            Case.MIDDLE, Case.EMPTY -> {
                clickOwnId("auto_overlay_confirm") // The sole approval; no Insert action.
                val expected = fixture.text.substring(0, fixture.caret) + fixedKorean +
                    fixture.text.substring(fixture.caret)
                val expectedCaret = fixture.caret + fixedKorean.length
                val done = await("STABLE_AUTO_COMMIT_NOT_CONFIRMED") { state().takeIf {
                    it.phase == "CONSUMED" && it.draft == "NONE" && !it.cleanup &&
                        it.confirmation == "CONFIRMED" &&
                        it.originalCaptures == before.originalCaptures + 1 &&
                        it.autoAttempts == before.autoAttempts + 1 &&
                        it.destinationCaptures == before.destinationCaptures + 1 &&
                        it.commits == before.commits + 1 &&
                        it.manualRenders == before.manualRenders &&
                        it.recordingProofs == before.recordingProofs
                } }
                sessionStarted = false
                assertFixtureFields(expected, fixture.text, expectedCaret, fixture.caret)
                assertFixtureStable(fixture)
                assertOwnReviewGone()
                assertNoResultControls()
                await("COLLAPSED_BUBBLE_OR_TARGET_NOT_RESTORED", 2_000) {
                    ownButton("Ko")?.takeIf { fixtureNode("stable_auto_editor")?.isFocused == true &&
                        imeVisible() && selectedSwiftKey() }
                }
                SystemClock.sleep(400)
                assertTrue("STABLE_AUTO_RETRIED_OR_RENDERED_MANUAL_RESULT", state().let {
                    it.autoAttempts == before.autoAttempts + 1 &&
                        it.destinationCaptures == before.destinationCaptures + 1 &&
                        it.commits == before.commits + 1 &&
                        it.manualRenders == before.manualRenders
                })
                report("outcome", "CASE_${case.name}_CONFIRMED_1_COMMIT_1_" +
                    "PRESERVED_PREFIX_SUFFIX_1_CARET_EXACT_1_PHASE_${done.phase}")
            }
            Case.ALTERNATE, Case.CARET, Case.CONTEXT -> {
                mutateFixture(case, fixture)
                val beforeConfirm = state()
                assertTrue("EXPLICIT_REVIEW_LOST_BEFORE_NEGATIVE_CONFIRM",
                    beforeConfirm.draft == "REVIEW" &&
                        beforeConfirm.phase in setOf("REVIEW", "FALLBACK") &&
                        ownId("auto_overlay_english")?.isFocused == true &&
                        ownId("ready_insert") == null &&
                        beforeConfirm.manualRenders == before.manualRenders)
                assertNoWrite(before)
                if (beforeConfirm.phase == "FALLBACK")
                    assertTrue("UNRELATED_PRECONFIRM_DISARM_${beforeConfirm.reason}",
                        targetSpecificReason(case, beforeConfirm.reason))
                val refusalTiming = if (beforeConfirm.phase == "FALLBACK")
                    "BEFORE_CONFIRM" else "AFTER_CONFIRM"
                clickOwnId("auto_overlay_confirm") // Still the only English approval.
                val fallback = await("CHANGED_TARGET_DID_NOT_FALL_BACK") { state().takeIf {
                    it.phase == "FALLBACK" && it.draft == "READY" &&
                        it.originalCaptures == before.originalCaptures + 1 &&
                        it.autoFallbacks == before.autoFallbacks + 1 &&
                        it.autoAttempts == before.autoAttempts &&
                        it.destinationCaptures == before.destinationCaptures &&
                        it.commits == before.commits &&
                        it.manualRenders > before.manualRenders && !it.cleanup
                } }
                assertNegativeFixture(case, fixture)
                assertNoWrite(before)
                assertOwnReviewGone()
                val targetSpecific = targetSpecificReason(case, fallback.reason)
                report("outcome", "CASE_${case.name}_FALLBACK_REASON_${fallback.reason}_" +
                    "TIMING_${refusalTiming}_TARGET_SPECIFIC_${if (targetSpecific) 1 else 0}_" +
                    "NO_CAPTURE_COMMIT_ATTEMPT_1_MANUAL_READY_1")
                assertTrue("FALLBACK_CAUSE_NOT_TARGET_SPECIFIC_${fallback.reason}", targetSpecific)
                awaitManualControls(before)
                // A later return to the old text/caret cannot silently rearm this intent.
                instrumentation.runOnMainSync {
                    fixture.original.setText(fixture.text)
                    fixture.original.setSelection(fixture.caret)
                    fixture.original.requestFocus()
                }
                SystemClock.sleep(400)
                assertNoWrite(before)
                assertTrue("LATE_RESTORE_REARMED_AUTO", state().phase == "FALLBACK")
                val cancel = awaitManualControls(before).cancel
                    ?: throw AssertionError("MANUAL_CANCEL_NOT_VISIBLE_AFTER_LATE_RESTORE")
                assertTrue("MANUAL_CANCEL_CLICK_FAILED",
                    cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                await("FALLBACK_DRAFT_NOT_CLEANED") { state().takeIf {
                    it.draft == "NONE" && !it.cleanup
                } }
                sessionStarted = false
            }
        }
        assertTrue("SWIFTKEY_CHANGED", selectedSwiftKey())
        assertTrue("FIXTURE_LOGICAL_DRAFT_CHANGED", fixture.activity.stableAutoDraftToken() ===
            fixture.draftToken && fixture.activity.stableAutoEditor() === fixture.original)
    }

    private fun launchAndSeed(case: Case): Fixture {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_STABLE_AUTO_FIXTURE, true)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        await("STABLE_FIXTURE_ACTIVITY_NOT_AVAILABLE") { SmokeEditorActivity.current.get() }
        val initialText = if (case == Case.EMPTY) "" else prefix + suffix
        val initialCaret = if (case == Case.EMPTY) 0 else prefix.length
        lateinit var fixture: Fixture
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            val original = activity.stableAutoEditor() ?: throw AssertionError("STABLE_FIELD_MISSING")
            val alternate = activity.stableAutoAlternateEditor()
                ?: throw AssertionError("STABLE_ALTERNATE_MISSING")
            val token = activity.stableAutoDraftToken() ?: throw AssertionError("DRAFT_TOKEN_MISSING")
            original.setText(initialText); original.setSelection(initialCaret)
            alternate.setText(initialText); alternate.setSelection(initialCaret)
            original.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(original, InputMethodManager.SHOW_IMPLICIT)
            fixture = Fixture(activity, original, alternate, token, initialText, initialCaret)
        }
        await("STABLE_FIELD_OR_SWIFTKEY_NOT_READY") {
            val node = fixtureNode("stable_auto_editor")
            node?.takeIf { it.isFocused && it.isEditable && it.isVisibleToUser &&
                (it.textSelectionStart == initialCaret && it.textSelectionEnd == initialCaret ||
                    initialText.isEmpty() && it.textSelectionStart == -1 &&
                        it.textSelectionEnd == -1) && imeVisible() && selectedSwiftKey() }
        }
        return fixture
    }

    private fun mutateFixture(case: Case, fixture: Fixture) {
        var applied = false
        instrumentation.runOnMainSync {
            if (SmokeEditorActivity.current.get() !== fixture.activity) return@runOnMainSync
            applied = when (case) {
                Case.ALTERNATE -> fixture.activity.stableAutoSelectIdenticalAlternate()
                Case.CARET -> {
                    fixture.original.setSelection((fixture.caret + 1).coerceAtMost(fixture.text.length))
                    fixture.original.selectionStart != fixture.caret
                }
                Case.CONTEXT -> {
                    fixture.original.setText(fixture.text + "변경")
                    fixture.original.setSelection(fixture.caret)
                    fixture.original.text.toString() != fixture.text
                }
                else -> false
            }
        }
        assertTrue("OWN_NEGATIVE_MUTATION_NOT_APPLIED", applied)
    }

    private fun assertNegativeFixture(case: Case, fixture: Fixture) {
        instrumentation.runOnMainSync {
            when (case) {
                Case.ALTERNATE -> assertTrue("IDENTICAL_ALTERNATE_NOT_SELECTED_OR_TEXT_CHANGED",
                    fixture.alternate.isFocused && !fixture.original.isFocused &&
                        fixture.original.text.toString() == fixture.text &&
                        fixture.alternate.text.toString() == fixture.text &&
                        fixture.alternate.selectionStart == fixture.caret)
                Case.CARET -> assertTrue("CARET_MUTATION_LOST_OR_TEXT_CHANGED",
                    fixture.original.text.toString() == fixture.text &&
                        fixture.original.selectionStart != fixture.caret &&
                        fixture.alternate.text.toString() == fixture.text)
                Case.CONTEXT -> assertTrue("CONTEXT_MUTATION_LOST_OR_ALTERNATE_CHANGED",
                    fixture.original.text.toString() == fixture.text + "변경" &&
                        fixture.alternate.text.toString() == fixture.text)
                else -> Unit
            }
        }
    }

    private fun targetSpecificReason(case: Case, reason: String): Boolean = when (case) {
        Case.ALTERNATE -> reason in setOf("RETURN_FIELD_CHANGED", "RETURN_TARGET_MISMATCH")
        Case.CARET -> reason in setOf("TARGET_CARET_CHANGED", "TARGET_SELECTION_CHANGED",
            "RETURN_TARGET_MISMATCH")
        Case.CONTEXT -> reason in setOf("TARGET_TEXT_CHANGED", "RETURN_TARGET_MISMATCH")
        else -> false
    }

    private fun assertFixtureStable(fixture: Fixture) {
        assertTrue("STABLE_FIXTURE_VIEW_OR_DRAFT_REPLACED",
            SmokeEditorActivity.current.get() === fixture.activity &&
                fixture.activity.stableAutoEditor() === fixture.original &&
                fixture.activity.stableAutoDraftToken() === fixture.draftToken)
    }

    private fun assertFixtureFields(original: String, alternate: String,
        originalCaret: Int, alternateCaret: Int) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            val stable = activity.stableAutoEditor() ?: throw AssertionError("STABLE_FIELD_MISSING")
            val other = activity.stableAutoAlternateEditor() ?: throw AssertionError("ALTERNATE_MISSING")
            assertTrue("STABLE_OR_ALTERNATE_TEXT_OR_CARET_CHANGED", stable.text.toString() == original &&
                other.text.toString() == alternate && stable.selectionStart == originalCaret &&
                stable.selectionEnd == originalCaret && other.selectionStart == alternateCaret &&
                other.selectionEnd == alternateCaret)
        }
    }

    private fun openReview() {
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        clickOwnButton("요")
    }

    private fun ownId(name: String): AccessibilityNodeInfo? {
        require(name in setOf("auto_overlay_english", "auto_overlay_confirm",
            "auto_overlay_cancel", "ready_insert", "ready_copy", "ready_cancel"))
        val matches = ownedNodes().filter { it.viewIdResourceName == "$product:id/$name" &&
            it.isVisibleToUser }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_ID_$name")
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

    private fun clickOwnId(name: String) {
        val node = await("OWN_CONTROL_NOT_AVAILABLE_$name") { ownId(name)?.takeIf {
            it.isEnabled && it.isClickable
        } }
        assertTrue("OWN_CONTROL_CLICK_FAILED_$name",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun clickOwnButton(label: String) {
        val node = await("OWN_BUTTON_NOT_AVAILABLE_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun fixtureNode(name: String): AccessibilityNodeInfo? {
        require(name in setOf("stable_auto_editor", "stable_auto_alternate_editor"))
        val matches = automation().windows.asSequence().mapNotNull { it.getRoot(0) }
            .filter { it.packageName?.toString() == fixture }
            .flatMap { it.findAccessibilityNodeInfosByViewId("$fixture:id/$name").asSequence() }
            .toList()
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_FIELD_$name")
        return matches.singleOrNull()
    }

    private fun assertNoResultControls() {
        await("RESULT_PANEL_REMAINED", 2_000) {
            true.takeIf { ownId("ready_insert") == null && ownId("ready_copy") == null &&
                ownId("ready_cancel") == null }
        }
    }

    private fun assertOwnReviewGone() {
        await("OWN_AUTO_REVIEW_STILL_VISIBLE", 2_000) {
            val ids = ownedNodes().mapNotNull { it.viewIdResourceName }.toSet()
            true.takeIf { listOf("auto_overlay_english", "auto_overlay_confirm",
                "auto_overlay_cancel").none { "$product:id/$it" in ids } }
        }
    }

    /** One framework window/tree per sample; never combine controls from successive layouts. */
    private fun readyControls(): ReadyControls {
        val candidates = ArrayList<ReadyControls>()
        var unavailableRoots = 0
        for (window in automation().windows) {
            if (window.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY ||
                window.displayId != Display.DEFAULT_DISPLAY) continue
            val root = window.getRoot(0)
            if (root == null) { unavailableRoots++; continue }
            if (root.packageName?.toString() != product) continue
            fun exact(name: String): Pair<String, AccessibilityNodeInfo?> {
                val found = root.findAccessibilityNodeInfosByViewId("$product:id/$name")
                    .filter { it.packageName?.toString() == product &&
                        it.viewIdResourceName == "$product:id/$name" &&
                        it.windowId == window.id }
                val node = found.singleOrNull()
                val present = found.size == 1
                val visible = node?.isVisibleToUser == true
                val usable = node != null && visible && node.isEnabled && node.isClickable
                return "P${if (present) 1 else 0}V${if (visible) 1 else 0}" +
                    "U${if (usable) 1 else 0}" to node?.takeIf { usable }
            }
            val (iCode, insert) = exact("ready_insert")
            val (cCode, copy) = exact("ready_copy")
            val (xCode, cancel) = exact("ready_cancel")
            if (iCode != "P0V0U0" || cCode != "P0V0U0" || xCode != "P0V0U0")
                candidates.add(ReadyControls(insert, copy, cancel,
                    "I${iCode}_C${cCode}_X${xCode}"))
        }
        return when (candidates.size) {
            1 -> candidates[0]
            0 -> ReadyControls(null, null, null,
                "NO_RESULT_WINDOW_ROOT_MISSING_${if (unavailableRoots > 0) 1 else 0}")
            else -> ReadyControls(null, null, null, "MULTIPLE_RESULT_WINDOWS")
        }
    }

    private fun awaitManualControls(before: State): ReadyControls {
        val end = SystemClock.elapsedRealtime() + 2_500L
        var last = "NOT_SAMPLED"
        while (SystemClock.elapsedRealtime() < end) {
            val current = state()
            assertNoWrite(before)
            assertTrue("MANUAL_FALLBACK_STATE_CHANGED_DURING_UI_WAIT",
                current.phase == "FALLBACK" && current.draft == "READY" && !current.cleanup)
            val sample = readyControls()
            last = sample.code
            if (sample.allReady) return sample
            SystemClock.sleep(100)
        }
        throw AssertionError("MANUAL_FALLBACK_NOT_AVAILABLE_$last")
    }

    private fun assertNoWrite(before: State) {
        val now = state()
        assertTrue("UNEXPECTED_STABLE_AUTO_WRITE", now.destinationCaptures == before.destinationCaptures &&
            now.commits == before.commits && now.autoAttempts == before.autoAttempts &&
            now.recordingProofs == before.recordingProofs)
    }

    private fun state(): State {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(180).map(String::trim).toList()
        }
        fun field(label: String): String {
            val line = lines.firstOrNull { it.startsWith("$label: ") }
                ?: throw AssertionError("PRODUCT_DIAGNOSTIC_MISSING_$label")
            return line.substringAfter(": ").trim()
        }
        fun count(label: String): Int = field(label).toIntOrNull()
            ?.takeIf { it in 0..100_000 } ?: throw AssertionError("BAD_PRODUCT_COUNT_$label")
        fun bool(label: String): Boolean = when (field(label)) {
            "true" -> true
            "false" -> false
            else -> throw AssertionError("BAD_PRODUCT_FLAG_$label")
        }
        return State(field("Experiment arm"), bool("Stable overlay auto enabled"),
            bool("Auto insert enabled"), bool("Speech input enabled"),
            bool("Translation input enabled"), field("Stable overlay auto phase"),
            field("Stable overlay auto reason"), field("Draft phase"),
            bool("Overlay cleanup pending"), field("Commit confirmation"),
            count("Original intent captured count"), count("Auto insert attempt count"),
            count("Auto insert fallback count"), count("Destination capture count"),
            count("Commit request count"), count("Auto manual result renders"),
            count("Unbound recording proof count"))
    }

    private fun automation(): UiAutomation = instrumentation.getUiAutomation(
        UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey
    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }
    private fun report(label: String, code: String) {
        assertTrue("UNSAFE_STABLE_AUTO_REPORT", code.length <= 400 &&
            Regex("[A-Za-z0-9_]+").matches(code))
        instrumentation.sendStatus(0, Bundle().apply { putString("koscribe_stable_auto_$label", code) })
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
