package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exact opt-in, API-free automatic insertion checks in the owned synthetic editor only. */
@RunWith(AndroidJUnit4::class)
class AutomaticInsertSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val fixedEnglish = "Hello, I will arrive in ten minutes."
    private val fixedKorean = "안녕하세요. 10분 후에 도착할게요."
    private val flags = listOf("autoEmptyReady", "autoMiddleReady", "autoChangedReady",
        "autoCaretReady", "autoFieldReady", "autoUncertainReady",
        "autoSelectedReady", "autoCancelReady")
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false

    private data class State(
        val arm: String, val phase: String, val captures: Int, val commits: Int,
        val confirmation: String, val proofs: Int, val hostReviewProofs: Int,
        val entryInventory: String, val inventoryCompletions: Int,
        val cleanup: Boolean,
        val autoEnabled: Boolean, val speechEnabled: Boolean, val translationEnabled: Boolean,
        val intentPhase: String, val intentReason: String, val intentCaptures: Int,
        val autoAttempts: Int, val autoFallbacks: Int, val manualResultRenders: Int,
    )

    private data class ReviewGeometry(val window: Rect, val confirm: Rect, val cancel: Rect,
        val keyboards: List<Rect>)

    private data class SeedProbe(
        val nodePresent: Boolean, val nodeRefreshed: Boolean, val nodeFocused: Boolean,
        val nodeTextMatches: Boolean, val nodeStart: Int, val nodeEnd: Int,
        val ownedFocused: Boolean, val ownedTextMatches: Boolean, val ownedEmpty: Boolean,
        val ownedStart: Int, val ownedEnd: Int, val fixtureForeground: Boolean,
    ) {
        fun ready(empty: Boolean, caret: Int): Boolean {
            val exactNodeCaret = nodeStart == caret && nodeEnd == caret
            val knownEmptyUnknownNodeCaret = empty && caret == 0 && ownedEmpty &&
                nodeStart == -1 && nodeEnd == -1
            return nodePresent && nodeRefreshed && nodeFocused && nodeTextMatches &&
                ownedFocused && ownedTextMatches && ownedStart == caret && ownedEnd == caret &&
                fixtureForeground && (exactNodeCaret || knownEmptyUnknownNodeCaret)
        }

        fun metadata(): String =
            "NODE_${if (nodePresent) 1 else 0}_REFRESH_${if (nodeRefreshed) 1 else 0}_" +
                "NODE_FOCUS_${if (nodeFocused) 1 else 0}_NODE_TEXT_${if (nodeTextMatches) 1 else 0}_" +
                "NODE_CARET_${nodeStart}_${nodeEnd}_OWN_FOCUS_${if (ownedFocused) 1 else 0}_" +
                "OWN_TEXT_${if (ownedTextMatches) 1 else 0}_OWN_EMPTY_${if (ownedEmpty) 1 else 0}_" +
                "OWN_CARET_${ownedStart}_${ownedEnd}_FOREGROUND_${if (fixtureForeground) 1 else 0}"
    }

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactSyntheticScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("AUTO_TEST_REQUIRES_ONE_EXACT_OPT_IN",
            flags.count { args.getString(it) == "true" } == 1 &&
                args.keySet().none { it.endsWith("Ready") && it !in flags && args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val initial = state()
        assertTrue("WRONG_PRODUCT_ARM", initial.arm == "REVIEW")
        assertTrue("AUTO_INSERT_FLAG_NOT_ENABLED", initial.autoEnabled)
        assertTrue("SPEECH_FLAG_MUST_BE_DISABLED", !initial.speechEnabled)
        assertTrue("TRANSLATION_FLAG_MUST_BE_DISABLED", !initial.translationEnabled)
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
            Manifest.permission.RECORD_AUDIO, product) == PackageManager.PERMISSION_GRANTED)
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        launchFixture()
        await("FIXTURE_NOT_FOREGROUND") {
            fixtureEditor()?.takeIf { it.isVisibleToUser && focusedFixtureApp() }
        }
    }

    @After fun cancelOwnDraftOnFailure() {
        if (!::instrumentation.isInitialized || !sessionAttempted) return
        runCatching {
            val cancel = ownId("review_cancel")?.takeIf { it.isEnabled && it.isClickable }
                ?: ownId("ready_cancel")?.takeIf { it.isEnabled && it.isClickable }
                ?: ownButton("Cancel")
            cancel?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    @Test fun unchangedEmptyOriginalAutoInsertsOnceAndDismisses() {
        only("autoEmptyReady")
        seedFixture("", 0)
        val before = state()
        startReview(before)
        clickOwnId("review_confirm") // No destination tap or Insert click.
        successfulAutoInsert(before, fixedKorean)
    }

    @Test fun unchangedMiddleOriginalPreservesBothSides() {
        only("autoMiddleReady")
        val prefix = "앞🙂 "
        val suffix = " 뒤🌙"
        seedFixture(prefix + suffix, prefix.length)
        val before = state()
        startReview(before)
        clickOwnId("review_confirm") // No destination tap or Insert click.
        successfulAutoInsert(before, prefix + fixedKorean + suffix)
    }

    @Test fun changedOriginalFallsBackAndLateRestoreNeedsManualInsert() {
        only("autoChangedReady")
        val prefix = "앞🙂 "
        val suffix = " 뒤🌙"
        val original = prefix + suffix
        val changed = prefix + "변경" + suffix
        seedFixture(original, prefix.length)
        val before = state()
        startReview(before)
        assertArmedReview(before)
        // Change only our paused fixture. The return must reject this original-context mismatch.
        seedFixtureOnMain(changed, prefix.length, requestFocus = false)
        assertTrue("PAUSED_FIXTURE_MUTATION_STOLE_REVIEW_FOCUS",
            ownId("review_english")?.isFocused == true)
        clickOwnId("review_confirm")
        manualFallback(before, changed)
        val fallback = state()
        assertTrue("CHANGED_ORIGINAL_DID_NOT_DISARM_AUTO",
            fallback.intentCaptures == before.intentCaptures + 1 &&
                fallback.intentPhase == "FALLBACK" &&
                fallback.intentReason != "NONE" && fallback.intentReason != "none" &&
                fallback.autoFallbacks == before.autoFallbacks + 1 &&
                fallback.manualResultRenders > before.manualResultRenders)
        seedFixture(original, prefix.length)
        SystemClock.sleep(400) // A late exact match cannot arm an automatic write.
        assertNoWrite(before)
        assertTrue("MANUAL_INSERT_NOT_AVAILABLE_AFTER_LATE_RESTORE", ownId("ready_insert") != null)
        clickOwnId("ready_insert") // One fresh manual transaction after explicit choice.
        successfulManualInsert(before, prefix + fixedKorean + suffix)
    }

    @Test fun changedOriginalCaretFallsBackWithoutLateAutoRetry() {
        only("autoCaretReady")
        val prefix = "앞🙂 "
        val suffix = " 뒤🌙"
        val original = prefix + suffix
        seedFixture(original, prefix.length)
        val before = state()
        startReview(before)
        setPausedFixtureCaret(prefix.length + 1)
        assertTrue("CARET_MUTATION_STOLE_REVIEW_FOCUS",
            ownId("review_english")?.isFocused == true)
        clickOwnId("review_confirm")
        manualFallback(before, original)
        assertTrue("CARET_MISMATCH_DID_NOT_TERMINALLY_FALL_BACK", state().let {
            it.intentPhase == "FALLBACK" && it.autoFallbacks == before.autoFallbacks + 1 &&
                it.manualResultRenders > before.manualResultRenders
        })
        selectFixtureRange(prefix.length, prefix.length)
        SystemClock.sleep(400)
        assertNoWrite(before)
        assertTrue("LATE_CARET_RESTORE_REARMED_AUTO", ownId("ready_insert") != null)
        clickOwnId("ready_cancel")
        sessionAttempted = false
        assertNoWrite(before)
    }

    @Test fun changedOriginalFieldFallsBackWithoutWritingEitherField() {
        only("autoFieldReady")
        val original = "Synthetic original field."
        seedFixture(original, original.length)
        val before = state()
        startReview(before)
        var changedFocus = false
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val other = activity.findViewById<EditText>(R.id.local_uncertain_editor)
            changedFocus = other.requestFocus() && other.isFocused &&
                !activity.findViewById<EditText>(R.id.local_test_editor).isFocused
        }
        assertTrue("FIELD_MUTATION_NOT_APPLIED", changedFocus)
        assertTrue("FIELD_MUTATION_STOLE_REVIEW_FOCUS",
            ownId("review_english")?.isFocused == true)
        clickOwnId("review_confirm")
        manualFallback(before, original)
        assertTrue("FIELD_MISMATCH_DID_NOT_TERMINALLY_FALL_BACK", state().let {
            it.intentPhase == "FALLBACK" && it.autoFallbacks == before.autoFallbacks + 1 &&
                it.manualResultRenders > before.manualResultRenders
        })
        assertTrue("FIELD_SWITCH_LOST_ON_RETURN", fixtureEditor()?.isFocused == false)
        assertTrue("ALTERNATE_FIXTURE_FIELD_CHANGED", fixtureText(
            fixtureEditor("local_uncertain_editor") ?: throw AssertionError("ALTERNATE_FIELD_MISSING")) == "")
        clickOwnId("ready_cancel")
        sessionAttempted = false
        assertNoWrite(before)
    }

    @Test fun acceptedButUnobservedAutoCommitIsTerminalWithoutInsertRetry() {
        only("autoUncertainReady")
        val original = "Synthetic uncertain commit."
        seedUncertainFixture(original)
        val before = state()
        startReview(before, targetEditorName = "local_uncertain_editor")
        clickOwnId("review_confirm")
        await("AUTO_UNCERTAIN_COMMIT_NOT_TERMINAL") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.autoAttempts == before.autoAttempts + 1 &&
                it.autoFallbacks == before.autoFallbacks && it.phase == "UNCERTAIN" &&
                it.confirmation == "UNCERTAIN" && !it.cleanup
        } }
        assertTrue("UNCERTAIN_FIXTURE_WAS_CHANGED", fixtureEditor("local_uncertain_editor")?.let {
            fixtureText(it) == original
        } == true)
        assertTrue("UNCERTAIN_COMMIT_REGAINED_INSERT", ownId("ready_insert") == null &&
            ownId("ready_copy") != null && ownId("ready_cancel") != null)
        SystemClock.sleep(500)
        assertTrue("UNCERTAIN_COMMIT_RETRIED_OR_RENDERED_READY_INSERT", state().let {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.autoAttempts == before.autoAttempts + 1 &&
                it.manualResultRenders == before.manualResultRenders &&
                it.confirmation == "UNCERTAIN" && ownId("ready_insert") == null
        })
        clickOwnId("ready_cancel")
        sessionAttempted = false
    }

    @Test fun initialSelectedRangeRequiresManualFallbackWithoutWriting() {
        only("autoSelectedReady")
        val original = "Synthetic alpha omega."
        seedFixture(original, original.length)
        selectFixtureRange(10, 15)
        val before = state()
        startReview(before, expectArmed = false)
        clickOwnId("review_confirm")
        manualFallback(before, original)
        assertTrue("SELECTED_RANGE_CAPTURED_ORIGINAL_INTENT",
            state().intentCaptures == before.intentCaptures)
        assertTrue("SELECTED_RANGE_CHANGED", fixtureEditor()?.let {
            it.textSelectionStart == 10 && it.textSelectionEnd == 15
        } == true)
        clickOwnId("ready_cancel")
        sessionAttempted = false
        assertNoWrite(before)
    }

    @Test fun reviewCancelNeverAutoCommits() {
        only("autoCancelReady")
        val original = "Synthetic cancel target."
        seedFixture(original, original.length)
        val before = state()
        startReview(before)
        assertArmedReview(before)
        clickOwnId("review_cancel")
        await("REVIEW_CANCEL_DID_NOT_END_DRAFT") { state().takeIf {
            it.phase == "NONE" && !it.cleanup
        } }
        sessionAttempted = false
        assertFixtureText(original)
        assertNoWrite(before)
        assertTrue("CANCEL_LOST_INTENT_CAPTURE_EVIDENCE",
            state().intentCaptures == before.intentCaptures + 1)
        assertTrue("REVIEW_CANCEL_LEFT_INSERT_AVAILABLE", ownId("ready_insert") == null)
    }

    private fun startReview(before: State, expectArmed: Boolean = true,
        targetEditorName: String = "local_test_editor") {
        assertTrue("PREEXISTING_DRAFT", before.phase == "NONE")
        assertTrue("ORIGINAL_FIXTURE_OR_SWIFTKEY_NOT_READY",
            focusedFixtureApp() && fixtureEditor(targetEditorName)?.isFocused == true &&
                imeVisible() && selectedSwiftKey())
        await("BUBBLE_NOT_AVAILABLE") { ownId("collapsed_bubble") ?: ownButton("Close") }
        if (ownId("collapsed_bubble") != null) clickOwnId("collapsed_bubble")
        sessionAttempted = true
        clickOwnButton("요")
        await("STOP_NOT_AVAILABLE") { ownButton("Stop") }
        await("MIC_PROOF_NOT_OBSERVED") { state().takeIf { it.proofs == before.proofs + 1 } }
        assertNoWrite(before)
        clickOwnButton("Stop")
        await("EDITABLE_REVIEW_NOT_AVAILABLE") { ownId("review_english")?.takeIf {
            it.isEditable && it.isEnabled && it.isFocused
        } }
        assertTrue("FIXED_SAMPLE_NOT_IN_REVIEW", ownId("review_english")?.text?.toString() == fixedEnglish)
        assertTrue("MIC_CLEANUP_PENDING_AT_REVIEW", !state().cleanup)
        if (expectArmed) await("AUTO_REVIEW_HOST_PROOF_NOT_READY", 2_000) {
            val current = state()
            if (current.captures != before.captures || current.commits != before.commits ||
                current.autoAttempts != before.autoAttempts)
                throw AssertionError("WRITE_BEFORE_REVIEW_CONFIRM")
            if (current.intentPhase == "FALLBACK" || current.autoFallbacks > before.autoFallbacks)
                throw AssertionError("AUTO_REVIEW_FELL_BACK_BEFORE_CONFIRM")
            if (current.entryInventory == "FAILED")
                throw AssertionError("AUTO_ENTRY_INVENTORY_FAILED_BEFORE_CONFIRM")
            if (current.intentCaptures != before.intentCaptures + 1 ||
                current.intentPhase !in setOf("ENTERING_REVIEW", "REVIEW") ||
                current.phase != "REVIEW")
                throw AssertionError("AUTO_REVIEW_STATE_CHANGED_BEFORE_CONFIRM")
            current.takeIf { it.hostReviewProofs == before.hostReviewProofs + 1 &&
                it.entryInventory == "COMPLETE" &&
                it.inventoryCompletions == before.inventoryCompletions + 1 &&
                it.intentPhase == "REVIEW" }
        }
        assertReviewActionGeometry()
        val review = state()
        if (expectArmed) {
            assertTrue("ORIGINAL_INTENT_NOT_ARMED_IN_REVIEW",
                review.intentCaptures == before.intentCaptures + 1 &&
                    review.intentPhase == "REVIEW" &&
                    review.autoFallbacks == before.autoFallbacks)
        } else {
            assertTrue("INELIGIBLE_SELECTION_CAPTURED_ORIGINAL_INTENT",
                review.intentCaptures == before.intentCaptures &&
                    review.autoAttempts == before.autoAttempts)
        }
        assertNoWrite(before)
    }

    private fun assertArmedReview(before: State) {
        val review = state()
        assertTrue("ORIGINAL_INTENT_NOT_ARMED_BEFORE_NEGATIVE_ACTION",
            review.intentCaptures == before.intentCaptures + 1 &&
                review.intentPhase == "REVIEW" && review.autoFallbacks == before.autoFallbacks)
    }

    private fun assertReviewActionGeometry() {
        var previous: ReviewGeometry? = null
        val geometry = await("OWN_REVIEW_ACTION_BOUNDS_UNAVAILABLE_OR_UNSTABLE", 8_000) {
            val current = reviewGeometry()
            if (current != null && current == previous) current
            else { previous = current; null }
        }
        assertTrue("OWN_REVIEW_CONFIRM_OUTSIDE_WINDOW", geometry.window.contains(geometry.confirm))
        assertTrue("OWN_REVIEW_CANCEL_OUTSIDE_WINDOW", geometry.window.contains(geometry.cancel))
        assertTrue("OWN_REVIEW_CONFIRM_OBSCURED_BY_IME", geometry.keyboards.none {
            Rect.intersects(it, geometry.confirm)
        })
        assertTrue("OWN_REVIEW_CANCEL_OBSCURED_BY_IME", geometry.keyboards.none {
            Rect.intersects(it, geometry.cancel)
        })
    }

    private fun reviewGeometry(): ReviewGeometry? {
        if (!selectedSwiftKey()) return null
        val windows = automation().windows
        val ownReview = windows.filter { window ->
            window.type == AccessibilityWindowInfo.TYPE_APPLICATION && window.isFocused &&
                window.getRoot(0)?.packageName?.toString() == product
        }.singleOrNull() ?: return null
        val root = ownReview.getRoot(0) ?: return null
        fun action(name: String): AccessibilityNodeInfo? =
            root.findAccessibilityNodeInfosByViewId("$product:id/$name")
                .filter { it.packageName?.toString() == product && it.isVisibleToUser &&
                    it.isEnabled && it.isClickable }.singleOrNull()
        val confirm = action("review_confirm") ?: return null
        val cancel = action("review_cancel") ?: return null
        val keyboards = windows.filter { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            .map { Rect().also(it::getBoundsInScreen) }.filter { !it.isEmpty }
        if (keyboards.isEmpty()) return null
        val windowBounds = Rect().also(ownReview::getBoundsInScreen)
        val confirmBounds = Rect().also(confirm::getBoundsInScreen)
        val cancelBounds = Rect().also(cancel::getBoundsInScreen)
        if (windowBounds.isEmpty || confirmBounds.isEmpty || cancelBounds.isEmpty) return null
        return ReviewGeometry(windowBounds, confirmBounds, cancelBounds,
            keyboards.sortedWith(compareBy<Rect>({ it.top }, { it.left },
                { it.bottom }, { it.right })))
    }

    private fun successfulAutoInsert(before: State, expected: String) {
        await("AUTOMATIC_RESULT_NOT_VISIBLE") { fixtureEditor()?.takeIf { fixtureText(it) == expected } }
        await("AUTOMATIC_COMMIT_NOT_CONFIRMED") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.autoAttempts == before.autoAttempts + 1 &&
                it.phase == "NONE" && it.confirmation == "CONFIRMED" && !it.cleanup
        } }
        sessionAttempted = false
        assertCollapsedBubbleWithoutResult("AUTO_INSERT_RESULT_PANEL_REMAINED")
        assertTrue("SWIFTKEY_NOT_VISIBLE_AFTER_AUTO", imeVisible() && selectedSwiftKey())
        SystemClock.sleep(500)
        assertFixtureText(expected)
        val settled = state()
        assertTrue("AUTO_INSERT_RETRIED_OR_REOPENED",
            settled.captures == before.captures + 1 && settled.commits == before.commits + 1 &&
                settled.autoAttempts == before.autoAttempts + 1 &&
                settled.autoFallbacks == before.autoFallbacks &&
                settled.manualResultRenders == before.manualResultRenders &&
                settled.phase == "NONE" && settled.confirmation == "CONFIRMED" &&
                ownId("ready_insert") == null)
        assertCollapsedBubbleWithoutResult("AUTO_INSERT_REOPENED_RESULT_PANEL")
    }

    private fun manualFallback(before: State, expected: String) {
        await("MANUAL_FALLBACK_NOT_AVAILABLE") { ownId("ready_insert")?.takeIf {
            ownId("ready_copy") != null && ownId("ready_cancel") != null &&
                state().phase == "READY"
        } }
        assertFixtureText(expected)
        assertNoWrite(before)
        assertTrue("FALLBACK_LOST_SWIFTKEY", selectedSwiftKey())
    }

    private fun successfulManualInsert(before: State, expected: String) {
        await("MANUAL_RESULT_NOT_VISIBLE") { fixtureEditor()?.takeIf { fixtureText(it) == expected } }
        await("MANUAL_COMMIT_NOT_CONFIRMED") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.autoAttempts == before.autoAttempts && it.phase == "NONE" &&
                it.confirmation == "CONFIRMED" && !it.cleanup
        } }
        sessionAttempted = false
        assertCollapsedBubbleWithoutResult("MANUAL_INSERT_RESULT_PANEL_REMAINED")
        assertTrue("SWIFTKEY_NOT_VISIBLE_AFTER_MANUAL", imeVisible() && selectedSwiftKey())
        SystemClock.sleep(500)
        assertFixtureText(expected)
        assertTrue("MANUAL_INSERT_RETRIED", state().commits == before.commits + 1)
        assertCollapsedBubbleWithoutResult("MANUAL_INSERT_REOPENED_RESULT_PANEL")
    }

    private fun assertCollapsedBubbleWithoutResult(code: String) {
        await(code) { ownId("collapsed_bubble")?.takeIf {
            it.isEnabled && it.isClickable && ownId("ready_insert") == null &&
                ownId("ready_copy") == null && ownId("ready_cancel") == null
        } }
    }

    private fun assertNoWrite(before: State) {
        val current = state()
        assertTrue("DESTINATION_CAPTURED_OR_COMMITTED_BEFORE_WRITE_CHOICE",
            current.captures == before.captures && current.commits == before.commits &&
                current.autoAttempts == before.autoAttempts)
    }

    private fun launchFixture() {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
            putExtra(SmokeEditorActivity.EXTRA_COMPOSED_FIXTURES, false)
        })
        await("FIXTURE_NOT_AVAILABLE", 8_000) { fixtureEditor()?.takeIf {
            focusedFixtureApp() && it.isVisibleToUser &&
                nodes(fixture).none { node ->
                    node.viewIdResourceName == "$fixture:id/local_protected_editor"
                }
        } }
    }

    private fun seedFixture(text: String, caret: Int) {
        seedFixtureOnMain(text, caret, requestFocus = true)
        val end = SystemClock.elapsedRealtime() + 8_000L
        var last: SeedProbe? = null
        while (SystemClock.elapsedRealtime() < end) {
            val current = seedProbe(text)
            last = current
            if (current.ready(text.isEmpty(), caret)) break
            SystemClock.sleep(100)
        }
        if (last?.ready(text.isEmpty(), caret) != true)
            throw AssertionError("FIXTURE_NOT_FOCUSED_${last?.metadata() ?: "NO_PROBE"}")
        await("SWIFTKEY_NOT_VISIBLE") { imeVisible().takeIf { it } }
        assertTrue("FIXTURE_OR_SWIFTKEY_NOT_FOREGROUND",
            focusedFixtureApp() && selectedSwiftKey())
        SystemClock.sleep(300)
    }

    private fun seedProbe(expected: String): SeedProbe {
        val node = fixtureEditor()
        val refreshed = node?.refresh() == true
        var ownedFocused = false
        var ownedTextMatches = false
        var ownedEmpty = false
        var ownedStart = -2
        var ownedEnd = -2
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            ownedFocused = editor.isFocused
            ownedTextMatches = editor.text.toString() == expected
            ownedEmpty = editor.text.isEmpty()
            ownedStart = editor.selectionStart
            ownedEnd = editor.selectionEnd
        }
        return SeedProbe(node != null, refreshed, node?.isFocused == true,
            node?.let { fixtureText(it) == expected } == true,
            node?.textSelectionStart ?: -2, node?.textSelectionEnd ?: -2,
            ownedFocused, ownedTextMatches, ownedEmpty, ownedStart, ownedEnd,
            focusedFixtureApp())
    }

    private fun seedFixtureOnMain(text: String, caret: Int, requestFocus: Boolean) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            editor.setText(text)
            editor.setSelection(caret)
            if (requestFocus) {
                editor.requestFocus()
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun seedUncertainFixture(text: String) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_uncertain_editor)
            editor.setText(text)
            editor.setSelection(text.length)
            editor.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        await("UNCERTAIN_FIXTURE_NOT_READY") { fixtureEditor("local_uncertain_editor")?.takeIf {
            it.isFocused && fixtureText(it) == text &&
                it.textSelectionStart == text.length && it.textSelectionEnd == text.length &&
                focusedFixtureApp() && imeVisible()
        } }
        assertTrue("UNCERTAIN_FIXTURE_SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        SystemClock.sleep(300)
    }

    private fun selectFixtureRange(start: Int, end: Int) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            activity.findViewById<EditText>(R.id.local_test_editor).setSelection(start, end)
        }
        await("FIXTURE_RANGE_NOT_READY") { fixtureEditor()?.takeIf {
            it.isFocused && it.textSelectionStart == start && it.textSelectionEnd == end
        } }
        SystemClock.sleep(250)
    }

    private fun setPausedFixtureCaret(caret: Int) {
        var applied = false
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            editor.setSelection(caret)
            applied = editor.selectionStart == caret && editor.selectionEnd == caret
        }
        assertTrue("PAUSED_FIXTURE_CARET_MUTATION_NOT_APPLIED", applied)
    }

    private fun only(flag: String) = assumeTrue("AUTO_METHOD_REQUIRES_OWN_FLAG",
        InstrumentationRegistry.getArguments().getString(flag) == "true")

    private fun state(): State {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(150).map(String::trim).toList()
        }
        fun field(label: String): String = lines.firstOrNull { it.startsWith("$label: ") }
            ?.substringAfter(": ")?.trim()?.takeIf { Regex("[A-Za-z0-9_]+").matches(it) }
            ?: throw AssertionError("PRODUCT_DIAGNOSTIC_UNAVAILABLE_$label")
        fun count(label: String): Int = field(label).toIntOrNull()
            ?.takeIf { it in 0..100_000 } ?: throw AssertionError("BAD_PRODUCT_COUNT_$label")
        val hostCounts = lines.firstOrNull {
            it.startsWith("Auto retained host acquired/review/return: ")
        }?.substringAfter(": ")?.split('/')?.map { it.toIntOrNull() }
        val hostReviewProofs = hostCounts?.takeIf { it.size == 3 && it.all { value ->
            value != null && value in 0..100_000
        } }?.get(1) ?: throw AssertionError("BAD_PRODUCT_HOST_REVIEW_COUNT")
        fun boolean(label: String): Boolean = when (field(label)) {
            "true" -> true
            "false" -> false
            else -> throw AssertionError("BAD_PRODUCT_FLAG_$label")
        }
        return State(field("Experiment arm"), field("Draft phase"),
            count("Destination capture count"), count("Commit request count"),
            field("Commit confirmation"), count("Unbound recording proof count"),
            hostReviewProofs, field("Auto entry inventory"),
            count("Auto entry inventory completions"),
            boolean("Overlay cleanup pending"), boolean("Auto insert enabled"),
            boolean("Speech input enabled"), boolean("Translation input enabled"),
            field("Original intent phase"), field("Original intent last reason"),
            count("Original intent captured count"),
            count("Auto insert attempt count"), count("Auto insert fallback count"),
            count("Auto manual result renders"))
    }

    private fun fixtureEditor(name: String = "local_test_editor"): AccessibilityNodeInfo? {
        require(name in listOf("local_test_editor", "local_uncertain_editor"))
        val matches = nodes(fixture).filter {
            it.viewIdResourceName == "$fixture:id/$name" && it.isEditable
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_EDITOR")
        return matches.singleOrNull()
    }

    private fun ownId(name: String): AccessibilityNodeInfo? {
        require(name in listOf("review_english", "review_confirm", "review_cancel",
            "ready_insert", "ready_copy", "ready_cancel", "collapsed_bubble"))
        val matches = nodes(product).filter {
            it.viewIdResourceName == "$product:id/$name" && it.isVisibleToUser
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_CONTROL_$name")
        return matches.singleOrNull()
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in listOf("Ko", "Close", "Stop", "Cancel", "요"))
        val matches = nodes(product).filter {
            it.className?.toString() == "android.widget.Button" && it.isEnabled &&
                it.isClickable && it.isVisibleToUser &&
                matchesOwnButtonLabel(it.text, label)
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_BUTTON_$label")
        return matches.singleOrNull()
    }

    private fun clickOwnId(name: String) {
        val node = await("OWN_CONTROL_NOT_FOUND_$name") { ownId(name)?.takeIf {
            it.isEnabled && it.isClickable
        } }
        assertTrue("OWN_CONTROL_CLICK_FAILED_$name", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun clickOwnButton(label: String) {
        val node = await("OWN_BUTTON_NOT_FOUND_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun fixtureText(editor: AccessibilityNodeInfo): String =
        if (editor.isShowingHintText) "" else editor.text?.toString().orEmpty()

    private fun assertFixtureText(expected: String) {
        val editor = fixtureEditor() ?: throw AssertionError("FIXTURE_EDITOR_NOT_FOUND")
        assertTrue("SYNTHETIC_FIXTURE_TEXT_CHANGED", fixtureText(editor) == expected)
    }

    private fun nodes(requested: String): List<AccessibilityNodeInfo> {
        if (requested != product && requested != fixture) throw AssertionError("PACKAGE_NOT_ALLOWED")
        val result = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != requested) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node.packageName?.toString() != requested) continue
                result.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
            }
        }
        return result
    }

    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey

    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun focusedFixtureApp(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
            it.getRoot(0)?.packageName?.toString() == fixture
    }

    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000, probe: () -> T?): T {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError(code)
    }
}
