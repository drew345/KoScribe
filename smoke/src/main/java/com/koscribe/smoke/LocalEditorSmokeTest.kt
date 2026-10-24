package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Controlled two-package tests: the smoke fixture and KoScribe's own controls. */
@RunWith(AndroidJUnit4::class)
class LocalEditorSmokeTest {
    private val productPackage = "com.koscribe.prototype"
    private val fixturePackage = "com.koscribe.smoke"
    private val editorResource = "$fixturePackage:id/local_test_editor"
    private val sample = "Hello, I will arrive in ten minutes."
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false

    @get:Rule val diagnosticsOnFailure = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            // Shell output is filtered to the service's fixed diagnostic codes.
            System.out.println("KOSCRIBE_SMOKE_DIAGNOSTICS ${serviceDiagnosticCodes()}")
        }
    }

    /** Every acquisition explicitly keeps installed accessibility services running. */
    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun prepareOwnEditor() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val reviewFlags = listOf("reviewCopyReady", "reviewInsertEmptyReady",
            "reviewInsertMiddleReady", "reviewKoreanReady", "reviewSelectionReady",
            "reviewKoreanEditReady", "reviewUncertainReady")
        assumeTrue("LEGACY_LOCAL_TEST_EXCLUDES_REVIEW_OPT_IN", reviewFlags.none {
            InstrumentationRegistry.getArguments().getString(it) == "true"
        })
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info

        val context = instrumentation.context
        assertTrue("WRONG_INSTRUMENTATION_PACKAGE", context.packageName == fixturePackage)
        assertTrue("MIC_PERMISSION_REQUIRED",
            context.packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, productPackage) ==
                PackageManager.PERMISSION_GRANTED)
        launchFixtureEditor()
        awaitFixtureEditor()
        await("BUBBLE_NOT_AVAILABLE") {
            if (ownButtons("Ko").size + ownButtons("Close").size == 1) true else null
        }
    }

    @After fun finishOwnSessionOnAnyFailure() {
        if (!::instrumentation.isInitialized || !sessionAttempted) return
        // Cancel only KoScribe's own panel. If it is not reachable, bringing
        // the smoke fixture forward invokes the panel's onPause mic cleanup.
        val cancelled = runCatching {
            if (uniqueOwnButton("Cancel") != null) {
                clickOwnButton("Cancel")
                true
            } else false
        }.getOrDefault(false)
        if (!cancelled) launchFixtureEditor()
        await("SESSION_CLEANUP_TIMEOUT", 4_000) {
            if (ownButtons("Cancel").isEmpty()) true else null
        }
    }

    @Test fun emptyEditorEnglishInsertsExactlyOnce() {
        seedFixture("", 0)
        runEnglishSample()
        awaitFieldText(sample)
        SystemClock.sleep(350)
        assertFieldText(sample)
    }

    /** Diagnostic only: isolates insertion from a keyboard-driven target relayout. */
    @Test fun emptyEditorKeyboardHiddenDiagnostic() {
        seedFixture("", 0)
        val focused = awaitFixtureEditor()
        assertTrue("DIAGNOSTIC_EDITOR_NOT_FOCUSED", focused.isFocused)
        assertTrue("DIAGNOSTIC_WINDOW_NOT_FOCUSED", automation().windows.any {
            it.id == focused.windowId && it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        })
        await("DIAGNOSTIC_IME_NOT_VISIBLE", 4_000) {
            if (automation().windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }) true else null
        }
        val beforeBack = automation().windows
        assertTrue("DIAGNOSTIC_PREBACK_STATE_CHANGED",
            fixtureEditor()?.isFocused == true &&
                beforeBack.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } &&
                beforeBack.any { it.id == focused.windowId &&
                    it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused })
        assertTrue("DIAGNOSTIC_IME_BACK_FAILED",
            automation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
        await("DIAGNOSTIC_IME_STILL_VISIBLE", 4_000) {
            if (automation().windows.none { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }) true else null
        }
        await("DIAGNOSTIC_EDITOR_LOST_FOCUS") { fixtureEditor()?.takeIf { it.isFocused } }
        SystemClock.sleep(300)
        assertFieldText("")
        runEnglishSample(requireVisibleIme = false)
        awaitFieldText(sample)
        SystemClock.sleep(350)
        assertFieldText(sample)
    }

    @Test fun cancelAfterStopPreservesExistingDraft() {
        val draft = "Keep this synthetic draft. "
        seedFixture(draft, draft.length)
        openEnglishRecording()
        clickOwnButton("Stop")
        awaitOwnButton("Insert English sample")
        SystemClock.sleep(750) // Let the review/IME transition settle before cancelling.
        clickOwnButton("Cancel")
        await("SESSION_DID_NOT_CANCEL") {
            if (ownButtons("Cancel").isEmpty()) true else null
        }
        sessionAttempted = false
        SystemClock.sleep(900)
        assertFieldText(draft)
    }

    @Test fun middleCaretPreservesTextOnBothSides() {
        val before = "Alpha "
        val after = " Omega"
        seedFixture(before + after, before.length)
        runEnglishSample()
        awaitFieldText(before + sample + after)
        SystemClock.sleep(350)
        assertFieldText(before + sample + after)
    }

    @Test fun politeSamplePreservesKoreanEmojiDraftAroundCaret() {
        val before = "한글🙂 "
        val after = " 마침🌙"
        val politeSample = "안녕하세요. 10분 후에 도착할게요."
        seedFixture(before + after, before.length)
        runKoreanSample("요")
        awaitFieldText(before + politeSample + after)
        SystemClock.sleep(350)
        assertFieldText(before + politeSample + after)
    }

    @Test fun casualSampleInsertsIntoEmptyEditor() {
        val casualSample = "안녕. 10분 후에 도착할게."
        seedFixture("", 0)
        runKoreanSample("반말")
        awaitFieldText(casualSample)
        SystemClock.sleep(350)
        assertFieldText(casualSample)
    }

    @Test fun editedEnglishReviewPreservesMiddleFixtureText() {
        val before = "Alpha "
        val after = " Omega"
        val edited = "Edited English sample for this test."
        seedFixture(before + after, before.length)
        openEnglishRecording()
        clickOwnButton("Stop")
        awaitOwnButton("Insert English sample")
        SystemClock.sleep(750)
        clickOwnButton("Edit English")
        await("REVIEW_EDITOR_OR_IME_NOT_READY", 4_000) {
            reviewEditor()?.takeIf { it.isFocused &&
                automation().windows.any { window ->
                    window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
            }
        }
        val editor = reviewEditor() ?: throw AssertionError("REVIEW_EDITOR_NOT_FOUND")
        assertTrue("REVIEW_EDITOR_NOT_FOCUSED", editor.isFocused)
        assertTrue("REVIEW_IME_NOT_VISIBLE", automation().windows.any {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        })
        assertTrue("REVIEW_SAMPLE_NOT_PRESENT", editor.text?.toString() == sample)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edited)
        }
        // Only KoScribe's verified review field receives this synthetic edit.
        assertTrue("REVIEW_SET_TEXT_FAILED",
            editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        await("REVIEW_EDIT_NOT_APPLIED") {
            reviewEditor()?.takeIf { it.text?.toString() == edited }
        }
        SystemClock.sleep(750)
        clickOwnButton("Insert English sample")
        await("SESSION_DID_NOT_FINISH") {
            if (ownButtons("Insert English sample").isEmpty() && ownButtons("Cancel").isEmpty())
                true else null
        }
        sessionAttempted = false
        awaitFieldText(before + edited + after)
        SystemClock.sleep(350)
        assertFieldText(before + edited + after)
    }

    @Test fun selectedRangeRefusesRecordingAndPreservesDraft() {
        val draft = "Synthetic alpha omega."
        val selectionStart = 10
        val selectionEnd = 15
        seedFixture(draft, draft.length)
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionEnd)
        }
        assertTrue("FIXTURE_SET_RANGE_FAILED",
            awaitFixtureEditor().performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args))
        await("FIXTURE_RANGE_NOT_READY") {
            fixtureEditor()?.takeIf {
                it.isFocused && it.textSelectionStart == selectionStart &&
                    it.textSelectionEnd == selectionEnd
            }
        }
        if (ownButtons("Close").size == 1) clickOwnButton("Close")
        requireOwnEditorWithVisibleIme()
        clickOwnButton("Ko")
        assertFixtureRange(selectionStart, selectionEnd)
        sessionAttempted = true
        clickOwnButton("EN")
        await("RANGE_REFUSAL_NOT_SHOWN") {
            ownStatus("Editor refused: SELECTION_NOT_COLLAPSED. Open KoScribe for diagnostic codes.")
        }
        SystemClock.sleep(500)
        assertTrue("RANGE_STARTED_RECORDING", ownButtons("Stop").isEmpty())
        assertFieldText(draft)
        sessionAttempted = false
    }

    @Test fun leavingReviewCancelsWithoutInsertion() {
        val draft = "Keep this synthetic draft. "
        seedFixture(draft, draft.length)
        openEnglishRecording()
        clickOwnButton("Stop")
        awaitOwnButton("Insert English sample")
        SystemClock.sleep(750)
        // Reuse only the synthetic fixture Activity; do not approve insertion.
        launchFixtureEditor()
        await("FIXTURE_DID_NOT_RETURN") {
            val editor = fixtureEditor()
            if (editor?.isFocused == true && automation().windows.any {
                it.id == editor.windowId && it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
            }) true else null
        }
        await("REVIEW_DID_NOT_CLOSE") {
            if (ownButtons("Cancel").isEmpty() && ownButtons("Insert English sample").isEmpty())
                true else null
        }
        if (ownButtons("Ko").size == 1) clickOwnButton("Ko")
        await("REVIEW_DID_NOT_CANCEL") { ownStatus("Session cancelled.") }
        assertFieldText(draft)
        SystemClock.sleep(1_200)
        assertFieldText(draft)
        sessionAttempted = false
    }

    private fun runEnglishSample(requireVisibleIme: Boolean = true) {
        openEnglishRecording(requireVisibleIme)
        clickOwnButton("Stop")
        awaitOwnButton("Insert English sample")
        SystemClock.sleep(750) // Confirm after review controls and the keyboard settle.
        clickOwnButton("Insert English sample")
        await("SESSION_DID_NOT_FINISH") {
            if (ownButtons("Insert English sample").isEmpty() && ownButtons("Cancel").isEmpty()) true else null
        }
        sessionAttempted = false
    }

    private fun runKoreanSample(modeLabel: String) {
        assertTrue("UNEXPECTED_KOREAN_MODE", modeLabel == "요" || modeLabel == "반말")
        openModeRecording(modeLabel)
        clickOwnButton("Stop")
        awaitOwnButton("Translate & insert sample")
        SystemClock.sleep(750)
        clickOwnButton("Translate & insert sample")
        await("SESSION_DID_NOT_FINISH") {
            if (ownButtons("Translate & insert sample").isEmpty() && ownButtons("Cancel").isEmpty())
                true else null
        }
        sessionAttempted = false
    }

    private fun openEnglishRecording(requireVisibleIme: Boolean = true) =
        openModeRecording(requireVisibleIme = requireVisibleIme)

    private fun openModeRecording(modeLabel: String = "EN", requireVisibleIme: Boolean = true) {
        await("BUBBLE_NOT_AVAILABLE") {
            if (ownButtons("Ko").size + ownButtons("Close").size == 1) true else null
        }
        if (ownButtons("Close").size == 1) clickOwnButton("Close")
        if (requireVisibleIme) requireOwnEditorWithVisibleIme()
        clickOwnButton("Ko")
        sessionAttempted = true
        clickOwnButton(modeLabel)
        awaitOwnButton("Stop")
        SystemClock.sleep(2_000) // Simulate a short utterance without retaining audio.
    }

    private fun launchFixtureEditor() {
        val intent = Intent(instrumentation.context, SmokeEditorActivity::class.java).apply {
            // Preserve the existing fixture Activity and its synthetic draft
            // when returning from KoScribe's review task.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        instrumentation.context.startActivity(intent)
    }

    private fun assertFixtureRange(start: Int, end: Int) {
        val editor = fixtureEditor() ?: throw AssertionError("LOCAL_EDITOR_NOT_FOUND")
        assertTrue("FIXTURE_RANGE_CHANGED",
            editor.isFocused && editor.textSelectionStart == start && editor.textSelectionEnd == end)
    }

    private fun ownStatus(expected: String): Boolean? =
        nodesForPackage(productPackage).any {
            it.className?.toString() == "android.widget.TextView" &&
                it.text?.toString() == expected
        }.takeIf { it }

    private fun requireOwnEditorWithVisibleIme() {
        await("NORMAL_IME_NOT_VISIBLE", 4_000) {
            val editor = fixtureEditor()
            val windows = automation().windows
            if (editor?.isFocused == true &&
                windows.any { it.id == editor.windowId &&
                    it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused } &&
                windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }) true else null
        }
    }

    private fun seedFixture(value: String, caret: Int) {
        assertTrue("FIXTURE_CARET_OUT_OF_RANGE", caret in 0..value.length)
        val editorForFocus = awaitFixtureEditor()
        assertTrue("FIXTURE_FOCUS_FAILED",
            editorForFocus.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("FIXTURE_NOT_FOCUSED") { fixtureEditor()?.takeIf { it.isFocused } }
        val editorForText = awaitFixtureEditor()
        val textArgs = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        // Test fixture only: production insertion never uses ACTION_SET_TEXT.
        assertTrue("FIXTURE_SET_TEXT_FAILED",
            editorForText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, textArgs))
        awaitFieldText(value)
        if (value.isNotEmpty()) {
            val editorForCaret = awaitFixtureEditor()
            if (editorForCaret.textSelectionStart != caret || editorForCaret.textSelectionEnd != caret) {
                val selectionArgs = Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, caret)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, caret)
                }
                assertTrue("FIXTURE_SET_CARET_FAILED",
                    editorForCaret.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs))
            }
            await("FIXTURE_CARET_NOT_READY") {
                fixtureEditor()?.takeIf { it.textSelectionStart == caret && it.textSelectionEnd == caret }
            }
        }
        await("FIXTURE_LOST_FOCUS") { fixtureEditor()?.takeIf { it.isFocused } }
        SystemClock.sleep(300) // Let fixture-generated accessibility events settle before capture.
        assertFieldText(value)
    }

    private fun clickOwnButton(label: String) {
        awaitOwnButton(label)
        repeat(12) {
            val visible = uniqueOwnButton(label, visibleOnly = true)
            if (visible != null) {
                assertTrue("BUTTON_CLICK_FAILED", visible.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                return
            }
            val candidate = uniqueOwnButton(label) ?: throw AssertionError("BUTTON_DISAPPEARED")
            // A freshly added review button can be temporarily outside the
            // dialog viewport while SwiftKey/resize animations settle.
            candidate.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
            ownScrollableParent(candidate)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(250)
        }
        throw AssertionError("BUTTON_NOT_REVEALED")
    }

    private fun ownScrollableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var parent = node.parent
        repeat(12) {
            if (parent == null || parent?.packageName?.toString() != productPackage) return null
            if (parent?.className?.toString() == "android.widget.ScrollView") return parent
            parent = parent?.parent
        }
        return null
    }

    private fun awaitOwnButton(label: String): AccessibilityNodeInfo =
        await("BUTTON_NOT_FOUND") { uniqueOwnButton(label) }

    private fun uniqueOwnButton(label: String, visibleOnly: Boolean = false): AccessibilityNodeInfo? {
        val matches = ownButtons(label, visibleOnly)
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_BUTTON")
        return matches.singleOrNull()
    }

    private fun ownButtons(label: String, visibleOnly: Boolean = false): List<AccessibilityNodeInfo> =
        nodesForPackage(productPackage).filter {
        it.className?.toString() == "android.widget.Button" && it.isClickable &&
            it.isEnabled && (!visibleOnly || it.isVisibleToUser) &&
            it.text?.toString()?.equals(label, ignoreCase = true) == true
    }

    private fun awaitFixtureEditor(): AccessibilityNodeInfo {
        val candidate = await("LOCAL_EDITOR_NOT_FOUND") { fixtureEditor() }
        if (candidate.isVisibleToUser) return candidate
        assertTrue("LOCAL_EDITOR_REVEAL_FAILED",
            candidate.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id))
        return await("LOCAL_EDITOR_NOT_REVEALED") { fixtureEditor(visibleOnly = true) }
    }

    private fun fixtureEditor(visibleOnly: Boolean = false): AccessibilityNodeInfo? {
        val matches = nodesForPackage(fixturePackage).filter {
            it.viewIdResourceName == editorResource &&
                it.className?.toString() == "android.widget.EditText" &&
                it.isEditable && (!visibleOnly || it.isVisibleToUser)
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_LOCAL_EDITOR")
        return matches.singleOrNull()
    }

    private fun reviewEditor(): AccessibilityNodeInfo? {
        val focusedApps = automation().windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focusedApps.size > 1) throw AssertionError("AMBIGUOUS_FOCUSED_APP_WINDOW")
        val root = focusedApps.singleOrNull()?.root ?: return null
        if (root.packageName?.toString() != productPackage) return null
        val nodes = ArrayList<AccessibilityNodeInfo>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node.packageName?.toString() != productPackage) continue
            nodes.add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(pending::addLast)
        }
        fun hasUniqueReviewButton(label: String): Boolean = nodes.count {
            it.className?.toString() == "android.widget.Button" && it.isEnabled && it.isClickable &&
                it.text?.toString()?.equals(label, ignoreCase = true) == true
        } == 1
        if (!hasUniqueReviewButton("Edit English") ||
            !hasUniqueReviewButton("Insert English sample") ||
            !hasUniqueReviewButton("Cancel")) return null
        val editors = nodes.filter {
            it.className?.toString() == "android.widget.EditText" &&
                it.isEditable && it.isEnabled && it.isVisibleToUser
        }
        if (editors.size > 1) throw AssertionError("AMBIGUOUS_REVIEW_EDITOR")
        return editors.singleOrNull()
    }

    private fun nodesForPackage(requestedPackage: String): List<AccessibilityNodeInfo> {
        if (requestedPackage != productPackage && requestedPackage != fixturePackage)
            throw AssertionError("TREE_PACKAGE_NOT_ALLOWED")
        val nodes = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() != requestedPackage) continue
            val pending = ArrayDeque<AccessibilityNodeInfo>()
            pending.add(root)
            while (pending.isNotEmpty()) {
                val node = pending.removeFirst()
                if (node.packageName?.toString() != requestedPackage) continue
                nodes.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(pending::addLast)
            }
        }
        return nodes
    }

    private fun awaitFieldText(expected: String) {
        await("FIELD_RESULT_TIMEOUT", 8_000) {
            fixtureEditor()?.takeIf { fixtureText(it) == expected }
        }
    }

    private fun assertFieldText(expected: String) {
        val editor = fixtureEditor() ?: throw AssertionError("LOCAL_EDITOR_NOT_FOUND")
        assertTrue("FIELD_RESULT_CHANGED", fixtureText(editor) == expected)
    }

    private fun fixtureText(editor: AccessibilityNodeInfo): String =
        if (editor.isShowingHintText) "" else editor.text?.toString().orEmpty()

    private fun serviceDiagnosticCodes(): String = runCatching {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(128).toList()
        }
        val code = Regex("[A-Z][A-Z0-9_]{0,191}")
        val observation = lines.firstOrNull { it.trim().startsWith("Last observation: ") }
            ?.substringAfter("Last observation: ")?.trim()?.takeIf { code.matches(it) } ?: "UNKNOWN"
        val invalidation = lines.firstOrNull { it.trim().startsWith("First invalidation: ") }
            ?.substringAfter("First invalidation: ")?.trim()?.takeIf { code.matches(it) } ?: "UNKNOWN"
        val events = lines.map { it.trim() }
            .mapNotNull { it.substringAfter(' ', "").takeIf { item -> code.matches(item) &&
                it.substringBefore(' ').all(Char::isDigit) } }
            .takeLast(20)
        if (events.isEmpty()) "TRACE_UNAVAILABLE" else
            "OBS=$observation FIRST=$invalidation EVENTS=${events.joinToString(",")}"
    }.getOrDefault("TRACE_UNAVAILABLE")

    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000, probe: () -> T?): T {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError(code)
    }
}
