package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

/** One explicit Insert per owned case. Korean editing uses a synthetic action on our own UI. */
@RunWith(AndroidJUnit4::class)
class RelativeEmptyInsertSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixturePackage = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val service = "dumpsys activity service $product/.KoScribeService"
    private val english = "Hello, I will arrive in ten minutes."
    private val prepared = "안녕하세요. 10분 후에 도착할게요."
    private val saved = "수정했어요🙂 지금 출발해요."
    private val sourceText = "Synthetic source stays unchanged."
    private val flags = setOf("relativeInsertFreshReady", "relativeInsertClearedReady",
        "relativeInsertSelectedReady", "relativeInsertChangedReadReady",
        "relativeInsertUncertainReady")
    private lateinit var instrumentation: Instrumentation
    private var sessionStarted = false
    private var fixtureLaunched = false
    private var firstInsertFixture: Fixture? = null
    private var firstInsertBaseline: SmokeEditorActivity.RelativeInsertDiagnostic? = null
    private var firstInsertRelative: Relative? = null

    private data class Product(val phase: String, val experimentPhase: String,
        val confirmation: String, val proofs: Int,
        val captures: Int, val commits: Int, val cleanup: Boolean)
    private data class Relative(val fields: Map<String, String>) {
        fun word(key: String): String = fields[key] ?: "MISSING"
        fun number(key: String): Int = fields[key]?.toIntOrNull()?.takeIf { it in 0..100_000 }
            ?: throw AssertionError("BAD_RELATIVE_NUMBER_$key")
    }
    private data class Fixture(val activity: SmokeEditorActivity, val source: EditText,
        val target: EditText)

    @Before fun requireExactOwnedArm() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("RELATIVE_INSERT_REQUIRES_ONE_CASE", flags.count {
            args.getString(it) == "true"
        } == 1 && args.keySet().none {
            it.endsWith("Ready") && it !in flags && args.getString(it) == "true"
        })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixturePackage)
        val lines = diagnostics()
        assertTrue("WRONG_RELATIVE_INSERT_ARM", field(lines, "Experiment arm") == "REVIEW" &&
            field(lines, "Relative empty insertion") == "true" &&
            field(lines, "Relative read probe arm") == "OFF")
        assertTrue("OTHER_EXPERIMENT_ENABLED", listOf("Auto insert enabled",
            "Stable overlay auto enabled", "Speech input enabled", "Translation input enabled",
            "Karrot service probe enabled", "Karrot empty connection probe enabled",
            "Karrot no-read probe enabled").all { field(lines, it) == "false" } &&
            field(lines, "Review surface probe").startsWith("arm=OFF state=IDLE "))
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
            Manifest.permission.RECORD_AUDIO, product) == PackageManager.PERMISSION_GRANTED)
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        assertTrue("UNCLEAN_INITIAL_SESSION", ownSessionClean())
        assertTrue("RELATIVE_STATUS_SCOPE_CHANGED", relative().let {
            it.word("SCOPE") == "SIGNED_FIXTURE_ONLY" && it.word("STATE") in setOf(
                "IDLE", "REFUSED", "CONFIRMED", "UNCERTAIN")
        })
    }

    @After fun cancelOnlyOwnSessionAndFixture() {
        if (!::instrumentation.isInitialized) return
        if (sessionStarted) {
            val end = SystemClock.elapsedRealtime() + 2_500L
            var clicked = false
            while (SystemClock.elapsedRealtime() < end && !clicked) {
                val discard = ownId("ready_result_discard")?.takeIf { usable(it) }
                if (discard != null) {
                    discard.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    SystemClock.sleep(100)
                    continue
                }
                val cancel = listOf("review_cancel", "ready_cancel").firstNotNullOfOrNull {
                    ownId(it)?.takeIf(::usable)
                } ?: ownButton("Cancel")
                if (cancel != null) {
                    cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    clicked = true // An ambiguous return must never dispatch a second Cancel.
                } else if (ownSessionClean()) break
                else SystemClock.sleep(100)
            }
            if (!ownSessionClean()) assertTrue("OWN_CANCEL_UNAVAILABLE", clicked)
            await("OWN_SESSION_NOT_CLEAN", 3_000) { true.takeIf {
                ownSessionClean()
            } }
            sessionStarted = false
        }
        if (fixtureLaunched) instrumentation.runOnMainSync {
            SmokeEditorActivity.current.get()?.takeIf {
                it.intent.getBooleanExtra(SmokeEditorActivity.EXTRA_RELATIVE_INSERT_FIXTURE, false)
            }?.finish()
        }
    }

    @Test fun freshUntouchedEmptyAfterEditedKoreanInsertsOnce() {
        only("relativeInsertFreshReady")
        val fixture = launchFixture(false)
        val before = productState()
        prepareEditedResult(before)
        chooseMaskedTarget(fixture)
        val prior = relative()
        assertTrue("FRESH_INITIAL_SELECTION_NOT_PRESENT", prior.word("CURRENT_SOURCE") ==
            "INITIAL_ONLY" && prior.word("CURRENT_SHAPE") == "COLLAPSED" &&
            prior.word("CURRENT_POSITION") == "ORIGIN" && prior.word("UPDATES") == "0" &&
            prior.word("UNRESOLVED") == "NO")
        assertNoWrites(before, fixture)
        instrumentation.runOnMainSync {
            firstInsertFixture = fixture
            firstInsertBaseline = fixture.activity.relativeInsertDiagnostic()
        }
        firstInsertRelative = prior
        clickOwnId("ready_insert")
        awaitConfirmed(before, fixture, prior, "INITIAL_ONLY")
        report("FRESH_INITIAL_CONFIRMED")
    }

    @Test fun callbackClearedEmptyInsertsOnce() {
        only("relativeInsertClearedReady")
        val fixture = launchFixture(false)
        val before = productState()
        prepareEditedResult(before)
        chooseMaskedTarget(fixture)
        val initial = relative()
        instrumentation.runOnMainSync {
            fixture.target.text.replace(0, fixture.target.text.length, "ABCD")
            fixture.target.setSelection(4)
        }
        val typed = await("TYPED_CURRENT_GEN_SELECTION_NOT_READY", 3_000) { relative().takeIf {
            it.word("CURRENT_SOURCE") == "CALLBACK_CURRENT_GEN" &&
                it.word("CURRENT_SHAPE") == "COLLAPSED" &&
                it.word("CURRENT_POSITION") == "NONZERO" &&
                it.word("GENERATION") == initial.word("GENERATION") &&
                it.number("UPDATES") > initial.number("UPDATES")
        } }
        instrumentation.runOnMainSync {
            fixture.target.setSelection(0, 4)
        }
        val selected = await("SELECTED_CURRENT_GEN_SELECTION_NOT_READY", 3_000) {
            relative().takeIf { it.word("CURRENT_SOURCE") == "CALLBACK_CURRENT_GEN" &&
                it.word("CURRENT_SHAPE") == "SELECTED" &&
                it.word("CURRENT_POSITION") == "RANGE" &&
                it.word("GENERATION") == initial.word("GENERATION") &&
                it.number("UPDATES") > typed.number("UPDATES") }
        }
        instrumentation.runOnMainSync {
            fixture.target.text.clear()
            fixture.target.setSelection(0)
        }
        await("CLEARED_CURRENT_GEN_SELECTION_NOT_READY", 3_000) { relative().takeIf {
            it.word("CURRENT_SOURCE") == "CALLBACK_CURRENT_GEN" &&
                it.word("CURRENT_SHAPE") == "COLLAPSED" &&
                it.word("CURRENT_POSITION") == "ORIGIN" &&
                it.word("CURRENT_COMPOSITION") == "NONE_REPORTED" &&
                it.word("UNRESOLVED") == "NO" &&
                it.word("GENERATION") == initial.word("GENERATION") &&
                it.number("UPDATES") > selected.number("UPDATES")
        } }
        assertTarget(fixture, "", 0, 0)
        assertNoWrites(before, fixture)
        clickOwnId("ready_insert")
        awaitConfirmed(before, fixture, initial, "CALLBACK_CURRENT_GEN")
        report("CLEARED_CALLBACK_CONFIRMED")
    }

    @Test fun selectedRangeWithNullSelectedTextRefusesBeforeCommit() {
        only("relativeInsertSelectedReady")
        val fixture = launchFixture(false)
        val before = productState()
        prepareEditedResult(before)
        chooseMaskedTarget(fixture)
        val prior = relative()
        instrumentation.runOnMainSync {
            fixture.target.text.replace(0, fixture.target.text.length, "ABCD")
            fixture.target.setSelection(0, 4)
        }
        await("SELECTED_CALLBACK_NOT_READY", 3_000) { relative().takeIf {
            it.word("CURRENT_SOURCE") == "CALLBACK_CURRENT_GEN" &&
                it.word("CURRENT_SHAPE") == "SELECTED" &&
                it.word("CURRENT_POSITION") == "RANGE" &&
                it.word("GENERATION") == prior.word("GENERATION")
        } }
        assertTarget(fixture, "ABCD", 0, 4)
        assertNoWrites(before, fixture)
        clickOwnId("ready_insert")
        awaitRefusedNoCommit(before, fixture, "ABCD", 0, 4,
            setOf("SELECTION_EVIDENCE"), reads = prior.number("READS"))
        report("SELECTED_REFUSED")
    }

    @Test fun firstReadOrdinaryTargetChangeRefusesBeforeCommit() {
        only("relativeInsertChangedReadReady")
        val fixture = launchFixture(false)
        val before = productState()
        prepareEditedResult(before)
        chooseMaskedTarget(fixture)
        val prior = relative()
        var armed = false
        instrumentation.runOnMainSync {
            armed = fixture.activity.armRelativeInsertFirstReadMutation()
        }
        assertTrue("FIRST_READ_HOOK_NOT_ARMED", armed)
        assertNoWrites(before, fixture)
        clickOwnId("ready_insert")
        await("FIRST_READ_MUTATION_NOT_APPLIED", 3_000) { true.takeIf {
            fixture.activity.relativeInsertFirstReadMutationState() == "APPLIED"
        } }
        assertTargetText(fixture, "X")
        awaitRefusedNoCommit(before, fixture, "X", null, null,
            setOf("PRECOMMIT"), reads = null)
        val outcome = relative()
        assertTrue("FIRST_READ_CAPTURE_BOUNDARY_NOT_REACHED", outcome.number("READS") >=
            prior.number("READS") + 1 && outcome.word("ORIGIN") != "NONE" &&
            outcome.word("REASON") != "FIRST_READ")
        report("FIRST_READ_CHANGED_REFUSED_${outcome.word("REASON")}")
    }

    @Test fun acknowledgedWithoutVisibleChangeIsTerminalOneAttempt() {
        only("relativeInsertUncertainReady")
        val fixture = launchFixture(true)
        val before = productState()
        prepareEditedResult(before)
        chooseMaskedTarget(fixture)
        val prior = relative()
        assertNoWrites(before, fixture)
        clickOwnId("ready_insert")
        await("ACKNOWLEDGED_COMMIT_NOT_ATTEMPTED", 5_000) { relative().takeIf {
            it.word("STATE") in setOf("ATTEMPTED", "UNCERTAIN") &&
                productState().commits == before.commits + 1
        } }
        await("UNOBSERVED_COMMIT_NOT_TERMINAL", 6_000) { productState().takeIf {
            it.phase == "UNCERTAIN" && it.confirmation == "UNCERTAIN" &&
                it.commits == before.commits + 1 &&
                relative().word("STATE") == "UNCERTAIN"
        } }
        assertTrue("UNCERTAIN_NOT_ONE_ACKNOWLEDGMENT", fixture.activity.relativeInsertCommitCounts() ==
            (1 to 0) && relative().number("READS") > prior.number("READS"))
        assertTarget(fixture, "", 0, 0)
        assertTrue("UNCERTAIN_RESULT_NOT_RETAINED", ownId("ready_result_text")?.text?.toString() ==
            saved && ownId("ready_insert") == null && ownId("ready_edit") == null &&
            ownId("ready_cancel")?.let(::usable) == true)
        SystemClock.sleep(400)
        assertTrue("UNCERTAIN_REARMED_OR_RETRIED", productState().commits ==
            before.commits + 1 && fixture.activity.relativeInsertCommitCounts() == (1 to 0) &&
            ownId("ready_insert") == null && ownId("ready_edit") == null)
        report("ACKNOWLEDGED_UNOBSERVED_TERMINAL")
    }

    private fun launchFixture(acknowledgeWithoutChange: Boolean): Fixture {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_RELATIVE_INSERT_FIXTURE, true)
            putExtra(SmokeEditorActivity.EXTRA_RELATIVE_ACK_WITHOUT_CHANGE,
                acknowledgeWithoutChange)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        fixtureLaunched = true
        val activity = await("OWN_RELATIVE_FIXTURE_NOT_AVAILABLE") {
            SmokeEditorActivity.current.get()?.takeIf { it.relativeReadEditor() != null }
        }
        await("OWN_FIXTURE_WINDOW_NOT_FOCUSED") {
            var ready = false
            instrumentation.runOnMainSync {
                val decor = activity.window.decorView
                val target = activity.relativeReadEditor()
                ready = SmokeEditorActivity.current.get() === activity &&
                    decor.isAttachedToWindow && decor.hasWindowFocus() &&
                    decor.display?.displayId == Display.DEFAULT_DISPLAY && target != null &&
                    target.isAttachedToWindow && target.windowToken != null &&
                    target.width > 0 && target.height > 0
            }
            true.takeIf { ready }
        }
        lateinit var result: Fixture
        instrumentation.runOnMainSync {
            val source = activity.probeOriginalEditor()
            val target = activity.relativeReadEditor() ?: throw AssertionError("TARGET_MISSING")
            source.setText(sourceText)
            source.setSelection(sourceText.length)
            target.setText("")
            target.setSelection(0)
            assertTrue("SOURCE_FOCUS_FAILED", source.requestFocus() && source.isFocused &&
                activity.window.decorView.hasWindowFocus())
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(source, InputMethodManager.SHOW_IMPLICIT)
            result = Fixture(activity, source, target)
        }
        await("SOURCE_FIELD_OR_SWIFTKEY_NOT_READY") { true.takeIf {
            sourceFocused(result) && defaultImeVisible() && selectedSwiftKey()
        } }
        return result
    }

    private fun prepareEditedResult(before: Product) {
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        sessionStarted = true
        clickOwnButton("요")
        await("STOP_NOT_AVAILABLE") { ownButton("Stop") }
        await("SAMPLE_RECORDING_PROOF_NOT_OBSERVED") { productState().takeIf {
            it.proofs == before.proofs + 1
        } }
        clickOwnButton("Stop")
        await("ENGLISH_REVIEW_NOT_FOCUSED") { ownId("review_english")?.takeIf {
            it.isEditable && it.isFocused && it.isVisibleToUser && defaultImeVisible() &&
                selectedSwiftKey()
        } }
        assertTrue("FIXED_ENGLISH_NOT_PRESENT", ownId("review_english")?.text?.toString() == english)
        clickOwnId("review_confirm")
        await("PREPARED_RESULT_NOT_READY") { productState().takeIf { it.phase == "READY" &&
            !it.cleanup } }
        await("PREPARED_KOREAN_NOT_VISIBLE") { ownId("ready_result_text")?.takeIf {
            it.isVisibleToUser && it.text?.toString() == prepared
        } }
        clickOwnId("ready_edit")
        val editor = await("OWN_RESULT_EDITOR_NOT_FOCUSED") { ownId("ready_result_editor")?.takeIf {
            it.isEditable && it.isFocused && it.isVisibleToUser && selectedSwiftKey() &&
                defaultImeVisible()
        } }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, saved)
        }
        assertTrue("SYNTHETIC_OWN_RESULT_EDIT_FAILED",
            editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        await("SAVED_KOREAN_NOT_OBSERVED") { ownId("ready_result_editor")?.takeIf {
            it.text?.toString() == saved
        } }
        clickOwnId("ready_result_done")
        await("SAVED_PREVIEW_NOT_VISIBLE") { ownId("ready_result_text")?.takeIf {
            it.isVisibleToUser && it.text?.toString() == saved &&
                productState().phase == "READY"
        } }
        assertTrue("WRITER_RAN_BEFORE_INSERT", productState().captures == before.captures &&
            productState().commits == before.commits)
    }

    private fun chooseMaskedTarget(fixture: Fixture) {
        instrumentation.runOnMainSync {
            assertTrue("TARGET_NOT_EMPTY_BEFORE_FOCUS", fixture.target.text.isEmpty() &&
                fixture.target.selectionStart == 0 && fixture.target.selectionEnd == 0)
            assertTrue("TARGET_FOCUS_FAILED", fixture.target.requestFocus() &&
                fixture.target.isFocused && fixture.activity.window.decorView.hasWindowFocus())
            (fixture.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(fixture.target, InputMethodManager.SHOW_IMPLICIT)
        }
        await("MASKED_SERVED_TARGET_NOT_READY") { relative().takeIf {
            it.word("GENERATION").toIntOrNull()?.let { number -> number >= 0 } == true &&
                it.word("CURRENT_SOURCE") in setOf("INITIAL_ONLY", "CALLBACK_CURRENT_GEN") &&
                it.word("CURRENT_SHAPE") == "COLLAPSED" &&
                it.word("CURRENT_POSITION") == "ORIGIN" && it.word("UNRESOLVED") == "NO" &&
                maskedTargetReady(fixture) && defaultImeVisible() && selectedSwiftKey()
        } }
        automation().waitForIdle(500L, 3_000L)
        assertTrue("MASKED_TARGET_CHANGED_AFTER_IDLE", maskedTargetReady(fixture) &&
            defaultImeVisible() && selectedSwiftKey())
        assertTrue("SAVED_PREVIEW_OR_CONTROLS_LOST", ownId("ready_result_text")?.text?.toString() ==
            saved && ownId("ready_insert")?.let(::usable) == true)
    }

    private fun awaitConfirmed(before: Product, fixture: Fixture, prior: Relative,
        requiredOrigin: String) {
        await("RELATIVE_INSERT_NOT_CONFIRMED", 8_000) { productState().takeIf {
            it.phase == "NONE" && it.confirmation == "CONFIRMED" && !it.cleanup &&
                it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                relative().let { status -> status.word("STATE") == "CONFIRMED" &&
                    status.word("REASON") == "EXACT_RELATIVE_AND_CALLBACK" &&
                    status.word("ORIGIN") == requiredOrigin &&
                    status.number("READS") >= prior.number("READS") + 3 }
        } }
        sessionStarted = false
        assertTarget(fixture, saved, saved.length, saved.length)
        assertTrue("RELATIVE_FIXTURE_NOT_ONE_APPLIED_COMMIT",
            fixture.activity.relativeInsertCommitCounts() == (1 to 1))
        assertTrue("RESULT_CONTROLS_REMAINED", ownId("ready_insert") == null &&
            ownId("ready_edit") == null && ownId("ready_copy") == null)
        await("COLLAPSED_BUBBLE_OR_KEYBOARD_NOT_RESTORED", 2_000) { true.takeIf {
            ownButton("Ko") != null && maskedTargetReady(fixture) && defaultImeVisible() &&
                selectedSwiftKey()
        } }
        SystemClock.sleep(400)
        assertTrue("RELATIVE_INSERT_RETRIED", productState().commits == before.commits + 1 &&
            fixture.activity.relativeInsertCommitCounts() == (1 to 1) && selectedSwiftKey())
    }

    private fun awaitRefusedNoCommit(before: Product, fixture: Fixture, text: String,
        start: Int?, end: Int?, reasons: Set<String>, reads: Int?) {
        await("RELATIVE_INSERT_NOT_REFUSED", 5_000) { productState().takeIf {
            it.phase == "READY" && it.captures == before.captures + 1 &&
                it.commits == before.commits && relative().let { status ->
                    status.word("STATE") == "REFUSED" && status.word("REASON") in reasons &&
                        (reads == null || status.number("READS") == reads)
                }
        } }
        if (start == null || end == null) assertTargetText(fixture, text)
        else assertTarget(fixture, text, start, end)
        assertTrue("REFUSED_FIXTURE_RECEIVED_COMMIT", fixture.activity.relativeInsertCommitCounts() ==
            (0 to 0))
        assertTrue("REFUSED_RESULT_NOT_RETAINED", ownId("ready_result_text")?.text?.toString() ==
            saved && ownId("ready_cancel")?.let(::usable) == true)
    }

    private fun assertNoWrites(before: Product, fixture: Fixture) {
        assertTrue("WRITER_RAN_BEFORE_EXPLICIT_INSERT", productState().let {
            it.captures == before.captures && it.commits == before.commits
        } && fixture.activity.relativeInsertCommitCounts() == (0 to 0))
    }

    private fun ownSessionClean(): Boolean = productState().let { state ->
        state.phase == "NONE" && !state.cleanup && state.experimentPhase !in setOf(
            "RESELECT_PREPARING", "RESELECT_RECORDING_PROVED", "RESELECT_READ_ONLY_REVIEW",
            "REVIEW_HANDOFF", "REVIEW_EDITING", "TRANSLATING")
    } && ownButton("Stop") == null && ownButton("Cancel") == null &&
        listOf("review_english", "review_confirm", "review_cancel", "ready_result_editor",
            "ready_result_discard", "ready_insert", "ready_copy", "ready_cancel").all {
            ownId(it) == null
        }

    private fun sourceFocused(fixture: Fixture): Boolean {
        var local = false
        instrumentation.runOnMainSync { local = fixture.source.isAttachedToWindow &&
            fixture.source.isFocused && fixture.source.windowToken != null }
        val focused = automation().windows.filter { it.type ==
            AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
            it.displayId == Display.DEFAULT_DISPLAY }
        if (focused.size != 1) return false
        val window = focused.single()
        val root = window.getRoot(0) ?: return false
        if (root.packageName?.toString() != fixturePackage) return false
        return local && root.findAccessibilityNodeInfosByViewId(
            "$fixturePackage:id/local_test_editor").singleOrNull()?.isFocused == true
    }

    private fun maskedTargetReady(fixture: Fixture): Boolean {
        var local = false
        instrumentation.runOnMainSync {
            local = SmokeEditorActivity.current.get() === fixture.activity &&
                fixture.target.isAttachedToWindow && fixture.target.isFocused &&
                fixture.target.windowToken != null &&
                (fixture.target.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        }
        val focused = automation().windows.filter { it.type ==
            AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
            it.displayId == Display.DEFAULT_DISPLAY }
        if (focused.size != 1) return false
        val window = focused.single()
        val root = window.getRoot(0) ?: return false
        if (root.packageName?.toString() != fixturePackage || root.windowId != window.id)
            return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return local && node.packageName?.toString() == fixturePackage &&
            node.windowId == window.id && !node.isFocused && !node.isEditable &&
            node.isEnabled && node.isVisibleToUser && !node.isPassword &&
            node.inputType == InputType.TYPE_NULL && node.viewIdResourceName.isNullOrBlank() &&
            node.textSelectionStart == -1 && node.textSelectionEnd == -1
    }

    private fun assertTarget(fixture: Fixture, text: String, start: Int, end: Int) {
        instrumentation.runOnMainSync { assertTrue("OWN_RELATIVE_TARGET_MISMATCH",
            fixture.target.text.toString() == text && fixture.target.selectionStart == start &&
                fixture.target.selectionEnd == end && fixture.source.text.toString() == sourceText) }
    }
    private fun assertTargetText(fixture: Fixture, text: String) {
        instrumentation.runOnMainSync { assertTrue("OWN_RELATIVE_TARGET_TEXT_MISMATCH",
            fixture.target.text.toString() == text && fixture.source.text.toString() == sourceText) }
    }

    private fun productState(): Product {
        val lines = diagnostics()
        fun count(name: String): Int = field(lines, name).toIntOrNull()?.takeIf {
            it in 0..100_000
        } ?: throw AssertionError("BAD_PRODUCT_COUNT_$name")
        return Product(field(lines, "Draft phase"), field(lines, "Experiment phase"),
            field(lines, "Commit confirmation"),
            count("Unbound recording proof count"), count("Destination capture count"),
            count("Commit request count"), field(lines, "Overlay cleanup pending") == "true")
    }

    private fun relative(): Relative {
        val descriptor = automation().executeShellCommand("$service --relative-empty-insert-status")
        val line = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(5).map(String::trim).singleOrNull { value ->
                value.startsWith("RELATIVE_EMPTY_V1 ")
            }
        } ?: throw AssertionError("RELATIVE_EMPTY_STATUS_MISSING")
        val entries = line.removePrefix("RELATIVE_EMPTY_V1 ").split(' ').map {
            val parts = it.split('=', limit = 2)
            if (parts.size != 2 || !Regex("[A-Z_]+").matches(parts[0]) ||
                !Regex("[A-Z0-9_-]+").matches(parts[1]))
                throw AssertionError("RELATIVE_EMPTY_STATUS_UNSAFE")
            parts[0] to parts[1]
        }
        assertTrue("RELATIVE_EMPTY_STATUS_DUPLICATE", entries.map { it.first }.distinct().size ==
            entries.size)
        return Relative(entries.toMap())
    }

    private fun diagnostics(): List<String> {
        val descriptor = automation().executeShellCommand(service)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(230).map(String::trim).toList()
        }
    }
    private fun field(lines: List<String>, name: String): String = lines.firstOrNull {
        it.startsWith("$name: ")
    }?.substringAfter(": ")?.trim() ?: throw AssertionError("PRODUCT_DIAGNOSTIC_MISSING_$name")

    private fun ownId(name: String): AccessibilityNodeInfo? {
        require(name in setOf("review_english", "review_confirm", "review_cancel",
            "ready_result_text", "ready_edit", "ready_result_editor", "ready_result_done",
            "ready_result_discard", "ready_insert", "ready_copy", "ready_cancel"))
        val matches = ownNodes().filter { it.viewIdResourceName == "$product:id/$name" }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_ID_$name")
        return matches.singleOrNull()
    }
    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in setOf("Ko", "Close", "요", "Stop", "Cancel"))
        val matches = ownNodes().filter { it.className?.toString() == "android.widget.Button" &&
            (matchesOwnButtonLabel(it.text, label) ||
                (label == "Ko" && it.viewIdResourceName == "$product:id/collapsed_bubble")) &&
            usable(it)
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_BUTTON_$label")
        return matches.singleOrNull()
    }
    private fun ownNodes(): List<AccessibilityNodeInfo> {
        val found = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != product) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node.packageName?.toString() != product) continue
                found.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
            }
        }
        return found
    }
    private fun clickOwnId(name: String) {
        val node = await("OWN_CONTROL_NOT_READY_$name") { ownId(name)?.takeIf(::usable) }
        assertTrue("OWN_CONTROL_CLICK_FAILED_$name",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun clickOwnButton(label: String) {
        val node = await("OWN_BUTTON_NOT_READY_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
    private fun usable(node: AccessibilityNodeInfo): Boolean =
        node.isVisibleToUser && node.isEnabled && node.isClickable
    private fun report(code: String) {
        val status = relative()
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_EMPTY_GATE_$code" +
                "_STATE_${status.word("STATE")}_REASON_${status.word("REASON")}" +
                "_ORIGIN_${status.word("ORIGIN")}_READS_${status.word("READS")}" +
                "_CURRENT_${status.word("CURRENT_SOURCE")}_${status.word("CURRENT_SHAPE")}" +
                "_${status.word("CURRENT_POSITION")}_${status.word("CURRENT_COMPOSITION")}" +
                "_UNRESOLVED_${status.word("UNRESOLVED")}\n")
        })
    }
    private fun only(flag: String) = assumeTrue("WRONG_RELATIVE_INSERT_CASE",
        InstrumentationRegistry.getArguments().getString(flag) == "true")
    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey
    private fun defaultImeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
            it.displayId == Display.DEFAULT_DISPLAY
    }
    private fun automation(): UiAutomation = instrumentation.getUiAutomation(
        UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")
    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000,
        probe: () -> T?): T {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            probe()?.let { return it }
            SystemClock.sleep(80)
        }
        fail(code)
    }

    private fun fail(code: String): Nothing {
        val status = runCatching { relative() }.getOrNull()
        val product = runCatching { productState() }.getOrNull()
        var local = "OWNER_0_SOURCE_FOCUS_0_TARGET_FOCUS_0_TARGET_EMPTY_0"
        var hook = "UNAVAILABLE"
        var calls = "UNKNOWN"
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
            if (activity != null) {
                val source = activity.probeOriginalEditor()
                val target = activity.relativeReadEditor()
                local = "OWNER_1_SOURCE_FOCUS_${bit(source.isFocused)}" +
                    "_TARGET_FOCUS_${bit(target?.isFocused == true)}" +
                    "_TARGET_EMPTY_${bit(target?.text?.isEmpty() == true)}"
                hook = activity.relativeInsertFirstReadMutationState()
                calls = activity.relativeInsertCommitCounts()?.let { "${it.first}_${it.second}" }
                    ?: "UNKNOWN"
            }
        }
        val evidence = "${code}_RELATIVE_${status?.word("STATE") ?: "UNAVAILABLE"}" +
            "_${status?.word("REASON") ?: "UNAVAILABLE"}" +
            "_ORIGIN_${status?.word("ORIGIN") ?: "UNAVAILABLE"}" +
            "_READS_${status?.word("READS") ?: "UNAVAILABLE"}" +
            "_CURRENT_${status?.word("CURRENT_SOURCE") ?: "UNAVAILABLE"}" +
            "_${status?.word("CURRENT_SHAPE") ?: "UNAVAILABLE"}" +
            "_${status?.word("CURRENT_POSITION") ?: "UNAVAILABLE"}" +
            "_${status?.word("CURRENT_COMPOSITION") ?: "UNAVAILABLE"}" +
            "_UNRESOLVED_${status?.word("UNRESOLVED") ?: "UNAVAILABLE"}" +
            "_POSTREAD_${status?.word("LAST_POSTREAD") ?: "UNAVAILABLE"}" +
            "_GEN_SAME_${bit(status != null && firstInsertRelative != null &&
                status.word("GENERATION") == firstInsertRelative?.word("GENERATION"))}" +
            "_UPDATES_DELTA_${boundedDelta(status?.word("UPDATES")?.toIntOrNull(),
                firstInsertRelative?.word("UPDATES")?.toIntOrNull())}" +
            "_DRAFT_${product?.phase ?: "UNAVAILABLE"}" +
            "_CAPTURES_${product?.captures ?: -1}_COMMITS_${product?.commits ?: -1}" +
            "_${local}_HOOK_${hook}_FIXTURE_CALLS_${calls}" +
            "_${runCatching { firstInsertOracle() }.getOrDefault("FIRST_ORACLE_ERROR")}" +
            "_IME_${bit(runCatching { defaultImeVisible() }.getOrDefault(false))}" +
            "_SWIFTKEY_${bit(selectedSwiftKey())}"
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "RELATIVE_EMPTY_GATE_FAILURE_$evidence\n")
        })
        throw AssertionError(evidence)
    }

    private fun bit(value: Boolean): Int = if (value) 1 else 0

    private fun boundedDelta(current: Int?, before: Int?): String = when {
        current == null || before == null -> "UNKNOWN"
        current < before -> "NEGATIVE"
        current == before -> "ZERO"
        current == before + 1 -> "ONE"
        else -> "TWO_PLUS"
    }

    private fun firstInsertOracle(): String {
        val fixture = firstInsertFixture ?: return "FIRST_ORACLE_NOT_ARMED"
        val before = firstInsertBaseline ?: return "FIRST_ORACLE_BASELINE_MISSING"
        var outcome = "FIRST_ORACLE_UNAVAILABLE"
        instrumentation.runOnMainSync {
            val target = fixture.target
            val current = fixture.activity.relativeInsertDiagnostic()
            val exact = target.text.toString() == saved
            val caret = target.selectionStart == saved.length &&
                target.selectionEnd == saved.length
            val sourceUnchanged = fixture.source.text.toString() == sourceText
            val targetFocused = target.isFocused
            val counters = if (current == null) "_DIAGNOSTIC_UNAVAILABLE" else
                "_CONNECTION_DELTA_${boundedDelta(current.connections, before.connections)}" +
                    "_SELECTION_DELTA_${boundedDelta(current.selectionCallbacks,
                        before.selectionCallbacks)}" +
                    "_SELECTION_CATEGORY_${current.selectionCategory}" +
                    "_BATCH_BEGIN_DELTA_${boundedDelta(current.totalBegins,
                        before.totalBegins)}" +
                    "_BATCH_END_DELTA_${boundedDelta(current.totalEnds, before.totalEnds)}" +
                    "_LATEST_BEGIN_DELTA_${boundedDelta(current.latestBegins,
                        before.latestBegins)}" +
                    "_LATEST_BEGIN_TRUE_DELTA_${boundedDelta(current.latestBeginReturnedTrue,
                        before.latestBeginReturnedTrue)}" +
                    "_LATEST_END_DELTA_${boundedDelta(current.latestEnds, before.latestEnds)}" +
                    "_LATEST_END_TRUE_DELTA_${boundedDelta(current.latestEndReturnedTrue,
                        before.latestEndReturnedTrue)}" +
                    "_LATEST_CALL_DEPTH_${when (current.latestCallDepth) { 0 -> "ZERO"; 1 -> "ONE";
                        else -> "TWO_PLUS" }}" +
                    "_BEGIN_RESULT_${current.latestBeginResult}" +
                    "_END_RESULT_${current.latestEndResult}"
            outcome = "FIRST_EXACT_${bit(exact)}_CARET_${bit(caret)}" +
                "_SOURCE_${bit(sourceUnchanged)}_TARGET_FOCUS_${bit(targetFocused)}" +
                counters
        }
        return outcome
    }
}
