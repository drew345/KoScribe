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
import android.view.Display
import android.view.accessibility.AccessibilityEvent
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

/** API-free manual-result checks in the owned fixture. Never clicks Copy or Send. */
@RunWith(AndroidJUnit4::class)
class ManualResultEditSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val fixedEnglish = "Hello, I will arrive in ten minutes."
    private val fixedKorean = "안녕하세요. 10분 후에 도착할게요."
    private val editedKorean = "수정했어요🙂 지금 출발해요."
    private val longKorean = ("긴 한국어 문장을 확인해요🙂 ").repeat(20) + "끝까지 보여요."
    private val sourceText = "Synthetic source stays unchanged."
    private val prefix = "왼🙂 "
    private val suffix = " 오른🌙"
    private val flags = listOf("manualResultEditedReady", "manualResultUneditedReady",
        "manualResultDiscardReady", "manualResultLongReady")
    private lateinit var instrumentation: Instrumentation
    private var sessionStarted = false

    private data class State(val arm: String, val phase: String, val experimentPhase: String,
        val captures: Int,
        val commits: Int, val confirmation: String, val proofs: Int, val cleanup: Boolean,
        val auto: Boolean, val speech: Boolean, val translation: Boolean,
        val stableAuto: Boolean, val surfaceProbeOff: Boolean, val karrotService: Boolean,
        val karrotEmpty: Boolean, val karrotNoRead: Boolean)
    private data class Fixture(val activity: SmokeEditorActivity, val source: EditText,
        val target: EditText, val initialTarget: String, val caret: Int)

    @Before fun requireExactOwnedScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("MANUAL_RESULT_REQUIRES_ONE_EXACT_READY_FLAG",
            flags.count { args.getString(it) == "true" } == 1 &&
                args.keySet().none { it.endsWith("Ready") && it !in flags &&
                    args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val initial = state()
        assertTrue("WRONG_PRODUCT_FLAGS", initial.arm == "REVIEW" && !initial.auto &&
            !initial.speech && !initial.translation && !initial.stableAuto &&
            initial.surfaceProbeOff && !initial.karrotService && !initial.karrotEmpty &&
            !initial.karrotNoRead && initial.phase == "NONE" && !initial.cleanup &&
            selectedSwiftKey())
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
            Manifest.permission.RECORD_AUDIO, product) == PackageManager.PERMISSION_GRANTED)
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
    }

    @After fun cancelOwnSessionOnFailure() {
        if (!::instrumentation.isInitialized || !sessionStarted) return
        val end = SystemClock.elapsedRealtime() + 2_500L
        var discarded = false
        var cancelled = false
        while (SystemClock.elapsedRealtime() < end) {
            val edit = ownId("ready_result_discard")?.takeIf {
                it.isVisibleToUser && it.isEnabled && it.isClickable
            }
            if (edit != null && !discarded) {
                discarded = true
                edit.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                SystemClock.sleep(100)
                continue
            }
            val cancel = ownId("review_cancel")?.takeIf {
                it.isVisibleToUser && it.isEnabled && it.isClickable
            } ?: ownId("ready_cancel")?.takeIf {
                it.isVisibleToUser && it.isEnabled && it.isClickable
            } ?: ownButton("Cancel") ?: ownButton("Close")
            if (cancel != null) {
                cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                cancelled = true
                break // Never dispatch a second Cancel after an ambiguous return.
            }
            if (ownSessionControlsGone()) {
                sessionStarted = false
                return
            }
            SystemClock.sleep(100)
        }
        assertTrue("OWN_CLEANUP_CONTROL_NOT_AVAILABLE", cancelled)
        await("OWN_SESSION_NOT_CLEAN_AFTER_FAILURE", 3_000) {
            true.takeIf { ownSessionControlsGone() }
        }
        sessionStarted = false
    }

    private fun ownSessionControlsGone(): Boolean =
        state().let { it.phase == "NONE" && !it.cleanup && it.experimentPhase !in setOf(
            "RESELECT_PREPARING", "RESELECT_RECORDING_PROVED", "RESELECT_READ_ONLY_REVIEW",
            "REVIEW_HANDOFF", "REVIEW_EDITING", "TRANSLATING") } &&
            ownButton("Stop") == null && ownButton("Cancel") == null &&
            ownButton("Close") == null && ownId("review_cancel") == null &&
            ownId("ready_result_discard") == null && ownId("ready_cancel") == null

    @Test fun editedKoreanInsertsOnlyFinalResultIntoChosenField() {
        only("manualResultEditedReady")
        val fixture = launchAndSeed()
        val before = state()
        startReviewAndPrepare(before)
        assertPreview(fixedKorean, before, naturalKeyboard = false)
        clickOwnId("ready_edit")
        editOwnResult(editedKorean)
        clickOwnId("ready_result_done")
        assertPreview(editedKorean, before, naturalKeyboard = false)
        assertTrue("UNEDITED_COPY_CONTROL_NOT_REACHABLE", reachableOwnId("ready_copy") != null)
        chooseTargetAfterDone(fixture, editedKorean)
        assertNoProductWrite(before)
        clickOwnId("ready_insert") // One explicit manual transaction only.
        awaitConfirmed(before, fixture, editedKorean)
    }

    @Test fun uneditedPreparedKoreanIsVisibleAndInsertsOnce() {
        only("manualResultUneditedReady")
        val fixture = launchAndSeed()
        val before = state()
        startReviewAndPrepare(before)
        assertPreview(fixedKorean, before, naturalKeyboard = false)
        chooseTargetAfterDone(fixture, fixedKorean)
        assertNoProductWrite(before)
        clickOwnId("ready_insert")
        awaitConfirmed(before, fixture, fixedKorean)
    }

    @Test fun discardKeepsPreparedKoreanWithoutWriting() {
        only("manualResultDiscardReady")
        val fixture = launchAndSeed()
        val before = state()
        startReviewAndPrepare(before)
        assertPreview(fixedKorean, before)
        clickOwnId("ready_edit")
        editOwnResult(editedKorean)
        clickOwnId("ready_result_discard")
        assertPreview(fixedKorean, before)
        assertFixtureUnchanged(fixture)
        clickOwnId("ready_cancel")
        await("RESULT_CANCEL_DID_NOT_CLEAN") { state().takeIf {
            it.phase == "NONE" && !it.cleanup && it.captures == before.captures &&
                it.commits == before.commits
        } }
        sessionStarted = false
    }

    @Test fun longKoreanPreviewCanBeReadAndControlsReached() {
        only("manualResultLongReady")
        val fixture = launchAndSeed()
        val before = state()
        startReviewAndPrepare(before)
        clickOwnId("ready_edit")
        editOwnResult(longKorean)
        clickOwnId("ready_result_done")
        await("LONG_KOREAN_PREVIEW_NOT_VISIBLE") { ownId("ready_result_text")?.takeIf {
            it.isVisibleToUser && it.text?.toString() == longKorean
        } }
        assertNoProductWrite(before)
        assertLongPreviewWithKeyboard(fixture, before)
        assertLongPreviewScrolls()
        assertTrue("LONG_RESULT_INSERT_NOT_REACHABLE", reachableOwnId("ready_insert") != null)
        assertTrue("LONG_RESULT_COPY_NOT_REACHABLE", reachableOwnId("ready_copy") != null)
        assertFixtureUnchanged(fixture)
        clickOwnId("ready_cancel")
        await("LONG_RESULT_CANCEL_DID_NOT_CLEAN") { state().takeIf {
            it.phase == "NONE" && !it.cleanup && it.captures == before.captures &&
                it.commits == before.commits
        } }
        sessionStarted = false
    }

    private fun only(flag: String) {
        assumeTrue("WRONG_MANUAL_RESULT_CASE",
            InstrumentationRegistry.getArguments().getString(flag) == "true")
    }

    private fun launchAndSeed(): Fixture {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_MANUAL_RESULT_FIXTURE, true)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        await("OWN_FIXTURE_NOT_AVAILABLE") { SmokeEditorActivity.current.get() }
        lateinit var result: Fixture
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            val source = activity.findViewById<EditText>(R.id.local_test_editor)
            val target = activity.manualResultTargetEditor() ?: throw AssertionError("TARGET_MISSING")
            val targetText = prefix + suffix
            source.setText(sourceText); source.setSelection(sourceText.length)
            target.setText(targetText); target.setSelection(prefix.length)
            source.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(source, InputMethodManager.SHOW_IMPLICIT)
            result = Fixture(activity, source, target, targetText, prefix.length)
        }
        await("SOURCE_FIELD_OR_SWIFTKEY_NOT_READY") {
            fixtureNode("local_test_editor")?.takeIf { it.isFocused && it.isEditable &&
                it.isVisibleToUser && imeVisible() && selectedSwiftKey() }
        }
        return result
    }

    private fun startReviewAndPrepare(before: State) {
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        sessionStarted = true
        clickOwnButton("요")
        await("STOP_NOT_AVAILABLE") { ownButton("Stop") }
        await("SAMPLE_RECORDING_PROOF_NOT_OBSERVED") { state().takeIf {
            it.proofs == before.proofs + 1
        } }
        assertNoProductWrite(before)
        clickOwnButton("Stop")
        await("EDITABLE_ENGLISH_REVIEW_NOT_FOCUSED") { ownId("review_english")?.takeIf {
            it.isEditable && it.isFocused && it.isVisibleToUser && imeVisible() && selectedSwiftKey()
        } }
        assertTrue("FIXED_ENGLISH_NOT_PRESENT", ownId("review_english")?.text?.toString() == fixedEnglish)
        clickOwnId("review_confirm")
        await("PREPARED_RESULT_NOT_READY") { state().takeIf { it.phase == "READY" && !it.cleanup } }
        assertNoProductWrite(before)
    }

    private fun assertPreview(expected: String, before: State,
        naturalKeyboard: Boolean = true) {
        await("FINAL_KOREAN_PREVIEW_NOT_VISIBLE") { ownId("ready_result_text")?.takeIf {
            it.isVisibleToUser && it.text?.toString() == expected
        } }
        assertTrue("SWIFTKEY_NOT_SELECTED_FOR_RESULT", selectedSwiftKey())
        if (naturalKeyboard) await("SWIFTKEY_NOT_OPEN_WHILE_PREVIEW_VISIBLE", 2_000) {
            true.takeIf { previewWithSwiftKey(expected) }
        }
        assertTrue("RESULT_EDIT_NOT_REACHABLE", reachableOwnId("ready_edit") != null)
        assertTrue("RESULT_INSERT_OR_COPY_NOT_REACHABLE",
            reachableOwnId("ready_insert") != null && reachableOwnId("ready_copy") != null)
        assertNoProductWrite(before)
        if (naturalKeyboard) await("SWIFTKEY_OR_PREVIEW_LOST_AFTER_CONTROLS", 2_000) {
            true.takeIf { previewWithSwiftKey(expected) }
        }
    }

    private fun assertLongPreviewWithKeyboard(fixture: Fixture, before: State) {
        if (waitForPreviewWithSwiftKey(longKorean, 2_000)) {
            keyboardPath("NATURAL")
            return
        }
        val now = state()
        assertTrue("LONG_RESULT_CHANGED_BEFORE_REFOCUS", now.phase == "READY" &&
            !now.cleanup && now.captures == before.captures && now.commits == before.commits &&
            ownId("ready_result_text")?.let {
                it.isVisibleToUser && it.text?.toString() == longKorean
            } == true && selectedSwiftKey() && !defaultDisplayImeVisible())
        instrumentation.runOnMainSync {
            assertTrue("LONG_RESULT_SOURCE_CHANGED_BEFORE_REFOCUS",
                fixture.source.text.toString() == sourceText &&
                    fixture.source.selectionStart == sourceText.length &&
                    fixture.source.selectionEnd == sourceText.length)
            fixture.source.requestFocus()
            (fixture.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(fixture.source, InputMethodManager.SHOW_IMPLICIT)
        }
        keyboardPath("SYNTHETIC_SOURCE_REFOCUS")
        await("LONG_RESULT_KEYBOARD_NOT_OPEN_AFTER_OWN_REFOCUS", 3_000) {
            true.takeIf { previewWithSwiftKey(longKorean) &&
                fixtureNode("local_test_editor")?.let {
                    it.isFocused && it.textSelectionStart == sourceText.length &&
                        it.textSelectionEnd == sourceText.length
                } == true }
        }
        assertNoProductWrite(before)
    }

    private fun waitForPreviewWithSwiftKey(expected: String, timeoutMs: Long): Boolean {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            if (previewWithSwiftKey(expected)) return true
            SystemClock.sleep(80)
        }
        return previewWithSwiftKey(expected)
    }

    private fun keyboardPath(code: String) {
        instrumentation.sendStatus(2, Bundle().apply {
            putString("stream", "LONG_PREVIEW_KEYBOARD_PATH=$code\n")
        })
    }

    private fun defaultDisplayImeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
            it.displayId == Display.DEFAULT_DISPLAY
    }

    private fun previewWithSwiftKey(expected: String, targetCaret: Int? = null): Boolean {
        if (!selectedSwiftKey()) return false
        val windows = automation().windows
        if (windows.none { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                it.displayId == Display.DEFAULT_DISPLAY }) return false
        val previews = windows.asSequence().filter {
            it.displayId == Display.DEFAULT_DISPLAY &&
                it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
        }
            .mapNotNull { it.getRoot(0) }.filter { it.packageName?.toString() == product }
            .flatMap { it.findAccessibilityNodeInfosByViewId(
                "$product:id/ready_result_text").asSequence() }.toList()
        if (previews.size != 1 || !previews.single().isVisibleToUser ||
            previews.single().text?.toString() != expected) return false
        if (targetCaret == null) return true
        val targets = windows.asSequence().filter {
            it.displayId == Display.DEFAULT_DISPLAY &&
                it.type == AccessibilityWindowInfo.TYPE_APPLICATION
        }
            .mapNotNull { it.getRoot(0) }.filter { it.packageName?.toString() == fixture }
            .flatMap { it.findAccessibilityNodeInfosByViewId(
                "$fixture:id/local_result_target_editor").asSequence() }.toList()
        return targets.size == 1 && targets.single().isFocused && targets.single().isEditable &&
            targets.single().textSelectionStart == targetCaret &&
            targets.single().textSelectionEnd == targetCaret
    }

    private fun editOwnResult(value: String) {
        val editor = await("OWN_RESULT_EDITOR_NOT_FOCUSED") { ownId("ready_result_editor")?.takeIf {
            it.isEditable && it.isEnabled && it.isFocused && it.isVisibleToUser &&
                imeVisible() && selectedSwiftKey()
        } }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        // Own KoScribe editor only. This synthetic action is not SwiftKey typing or a target write.
        assertTrue("OWN_RESULT_SET_TEXT_FAILED",
            editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        await("OWN_RESULT_EDIT_NOT_OBSERVED") { ownId("ready_result_editor")?.takeIf {
            it.text?.toString() == value
        } }
    }

    private fun chooseTargetAfterDone(fixture: Fixture, expected: String) {
        instrumentation.runOnMainSync {
            fixture.target.requestFocus()
            fixture.target.setSelection(fixture.caret)
            (fixture.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(fixture.target, InputMethodManager.SHOW_IMPLICIT)
        }
        val target = await("CHOSEN_TARGET_NOT_AVAILABLE") { fixtureNode("local_result_target_editor") }
        assertTrue("OWN_TARGET_TAP_FAILED", target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.runOnMainSync { fixture.target.setSelection(fixture.caret) }
        await("EXPLICIT_DESTINATION_OR_CARET_NOT_READY") {
            fixtureNode("local_result_target_editor")?.takeIf {
                it.isFocused && it.isEditable && it.textSelectionStart == fixture.caret &&
                    it.textSelectionEnd == fixture.caret && imeVisible() && selectedSwiftKey()
            }
        }
        await("PREINSERT_PREVIEW_TARGET_AND_SWIFTKEY_NOT_TOGETHER", 2_000) {
            true.takeIf { previewWithSwiftKey(expected, fixture.caret) }
        }
    }

    private fun awaitConfirmed(before: State, fixture: Fixture, result: String) {
        val expected = prefix + result + suffix
        await("EDITED_MANUAL_INSERT_NOT_CONFIRMED", 8_000) { state().takeIf {
            it.phase == "NONE" && it.confirmation == "CONFIRMED" && !it.cleanup &&
                it.captures == before.captures + 1 && it.commits == before.commits + 1
        } }
        sessionStarted = false
        instrumentation.runOnMainSync {
            assertTrue("SOURCE_FIELD_CHANGED", fixture.source.text.toString() == sourceText)
            assertTrue("TARGET_TEXT_OR_CARET_CHANGED", fixture.target.text.toString() == expected &&
                fixture.target.selectionStart == prefix.length + result.length &&
                fixture.target.selectionEnd == prefix.length + result.length)
        }
        assertTrue("RESULT_PANEL_REMAINED", ownId("ready_insert") == null &&
            ownId("ready_copy") == null)
        SystemClock.sleep(400)
        assertTrue("MANUAL_INSERT_RETRIED", state().commits == before.commits + 1)
        assertTrue("SWIFTKEY_NOT_SELECTED_AFTER_INSERT", selectedSwiftKey())
    }

    private fun assertFixtureUnchanged(fixture: Fixture) {
        instrumentation.runOnMainSync {
            assertTrue("RESULT_ACTION_CHANGED_FIXTURE", fixture.source.text.toString() == sourceText &&
                fixture.target.text.toString() == fixture.initialTarget &&
                fixture.target.selectionStart == fixture.caret)
        }
    }

    private fun assertNoProductWrite(before: State) {
        val now = state()
        assertTrue("DESTINATION_CAPTURED_OR_COMMITTED_BEFORE_MANUAL_INSERT",
            now.captures == before.captures && now.commits == before.commits)
    }

    private fun ownId(name: String): AccessibilityNodeInfo? {
        require(name in setOf("review_english", "review_confirm", "review_cancel",
            "ready_result_text", "ready_edit", "ready_result_editor", "ready_result_done",
            "ready_result_discard", "ready_insert", "ready_copy", "ready_cancel"))
        val matches = ownedNodes().filter { it.viewIdResourceName == "$product:id/$name" }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_ID_$name")
        return matches.singleOrNull()
    }

    private fun reachableOwnId(name: String): AccessibilityNodeInfo? {
        val end = SystemClock.elapsedRealtime() + 2_000L
        repeat(4) {
            ownId(name)?.takeIf { it.isVisibleToUser && it.isEnabled && it.isClickable }
                ?.let { return it }
            val scrolls = ownedNodes().filter {
                it.className?.toString() == "android.widget.ScrollView" &&
                    it.isVisibleToUser && it.isScrollable
            }
            if (scrolls.size != 1 || SystemClock.elapsedRealtime() >= end) return null
            scrolls.single().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(120)
        }
        return ownId(name)?.takeIf { it.isVisibleToUser && it.isEnabled && it.isClickable }
    }

    private fun assertLongPreviewScrolls() {
        val scroll = await("LONG_RESULT_SCROLL_VIEW_NOT_AVAILABLE", 2_000) {
            var ancestor = ownId("ready_result_text")?.parent
            repeat(8) {
                if (ancestor?.className?.toString() == "android.widget.ScrollView" &&
                    ancestor?.isVisibleToUser == true && ancestor?.isScrollable == true)
                    return@await ancestor
                ancestor = ancestor?.parent
            }
            null
        }
        val windowId = scroll.windowId
        var reachedEnd = false
        for (attempt in 0 until 8) {
            val event = automation().executeAndWaitForEvent({
                assertTrue("LONG_RESULT_SCROLL_FAILED",
                    scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
            }, { candidate ->
                candidate.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
                    candidate.packageName?.toString() == product &&
                    candidate.windowId == windowId &&
                    candidate.source == scroll &&
                    candidate.className?.toString() == "android.widget.ScrollView"
            }, 1_500L)
            assertTrue("LONG_RESULT_SCROLL_RANGE_UNKNOWN", event.maxScrollY > 0)
            if (event.scrollY >= event.maxScrollY) {
                reachedEnd = true
                break
            }
        }
        assertTrue("LONG_RESULT_BOTTOM_NOT_EXPOSED", reachedEnd)
        assertTrue("LONG_RESULT_LOST_AFTER_SCROLL",
            ownId("ready_result_text")?.text?.toString() == longKorean)
    }

    private fun clickOwnId(name: String) {
        val node = await("OWN_CONTROL_NOT_REACHABLE_$name") { reachableOwnId(name) }
        assertTrue("OWN_CONTROL_CLICK_FAILED_$name",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in setOf("Ko", "Close", "요", "Stop", "Cancel"))
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
        val node = await("OWN_BUTTON_NOT_AVAILABLE_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label",
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
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

    private fun fixtureNode(name: String): AccessibilityNodeInfo? {
        require(name in setOf("local_test_editor", "local_result_target_editor"))
        val matches = automation().windows.asSequence().mapNotNull { it.getRoot(0) }
            .filter { it.packageName?.toString() == fixture }
            .flatMap { it.findAccessibilityNodeInfosByViewId("$fixture:id/$name").asSequence() }
            .toList()
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_EDITOR_$name")
        return matches.singleOrNull()
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
        return State(field("Experiment arm"), field("Draft phase"),
            field("Experiment phase"),
            count("Destination capture count"), count("Commit request count"),
            field("Commit confirmation"), count("Unbound recording proof count"),
            bool("Overlay cleanup pending"), bool("Auto insert enabled"),
            bool("Speech input enabled"), bool("Translation input enabled"),
            bool("Stable overlay auto enabled"),
            field("Review surface probe").startsWith("arm=OFF state=IDLE "),
            bool("Karrot service probe enabled"),
            bool("Karrot empty connection probe enabled"),
            bool("Karrot no-read probe enabled"))
    }

    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey
    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }
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
