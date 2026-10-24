package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exact opt-in REVIEW checks using only KoScribe controls and our synthetic fixture. */
@RunWith(AndroidJUnit4::class)
class ReviewFlowSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val fixedEnglish = "Hello, I will arrive in ten minutes."
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false

    private data class State(val arm: String, val phase: String, val captures: Int,
        val commits: Int, val confirmation: String, val proofs: Int, val cleanup: Boolean)

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactSyntheticScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val reviewFlags = listOf("reviewCopyReady", "reviewInsertEmptyReady",
            "reviewInsertMiddleReady", "reviewKoreanReady", "reviewSelectionReady",
            "reviewKoreanEditReady", "reviewUncertainReady")
        val olderFlags = listOf("externalDraftReady", "externalRecordCancelReady",
            "startupHoldReady", "startupCollapseReady", "startupPanelReady", "overlayReady",
            "reselectReady", "bubbleIdleReady", "bubbleMicReady", "bubbleRestoreReady")
        assumeTrue("REVIEW_TEST_REQUIRES_ONE_EXACT_OPT_IN",
            reviewFlags.count { args.getString(it) == "true" } == 1 &&
                olderFlags.none { args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        assertTrue("WRONG_PRODUCT_ARM", state().arm == "REVIEW")
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
            Manifest.permission.RECORD_AUDIO, product) == PackageManager.PERMISSION_GRANTED)
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        launchFixture()
        await("FIXTURE_NOT_FOREGROUND") { fixtureEditor(R.id.local_test_editor)?.takeIf { it.isVisibleToUser } }
    }

    @After fun cancelOwnDraftOnFailure() {
        if (!::instrumentation.isInitialized || !sessionAttempted) return
        runCatching {
            listOf("review_cancel", "ready_cancel").firstNotNullOfOrNull { ownId(it) }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ?: ownButton("Cancel")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // The product's own deadline and mic owner also terminate an interrupted capture.
    }

    @Test fun editedEnglishCopyClosesAndReappearsAfterImeCycle() {
        only("reviewCopyReady")
        val original = "Keep synthetic draft."
        val edited = "Edited English for synthetic copy."
        seedFixture(R.id.local_test_editor, original, original.length)
        val before = state()
        startReview("EN", before)
        editReview(edited)
        confirmReady(before)
        tapDestination(R.id.local_test_editor)
        assertTrue("DESTINATION_CAPTURED_BEFORE_INSERT", state().captures == before.captures)
        clickOwnId("ready_copy")
        sessionAttempted = false
        await("COPY_DID_NOT_DISMISS") { (!visibleOwnUi()).takeIf { it } }
        assertFixtureText(R.id.local_test_editor, original)
        assertTrue("COPY_CAPTURED_OR_COMMITTED", state().captures == before.captures &&
            state().commits == before.commits && state().phase == "NONE" &&
            state().confirmation == "COPIED")
        assertTrue("FIXTURE_NOT_FOCUSED_FOR_OWN_CLIP_CHECK", focusedFixtureApp())
        val clipboard = instrumentation.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertTrue("OWN_SYNTHETIC_CLIP_METADATA_MISSING", ownClipDescription(clipboard.primaryClipDescription))
        val clip = clipboard.primaryClip
        assertTrue("OWN_SYNTHETIC_CLIP_CHANGED_BEFORE_READ",
            clip?.itemCount == 1 && ownClipDescription(clip.description))
        // Only after the owned label, MIME, sensitivity, and foreground checks.
        assertTrue("OWN_SYNTHETIC_CLIP_MISMATCH", clip!!.getItemAt(0).text?.toString() == edited)
        assertTrue("COPY_REOPENED_IN_SAME_IME_SESSION", imeVisible() && !visibleOwnUi())
        focusFixture(R.id.local_test_editor, showIme = false)
        await("IME_HIDE_NOT_OBSERVED") { (!imeVisible()).takeIf { it } }
        focusFixture(R.id.local_test_editor, showIme = true)
        await("BUBBLE_NOT_REARMED_AFTER_IME_CYCLE") { ownButton("Ko") }
    }

    @Test fun editedEnglishInsertsIntoEmptyFixtureOnce() {
        only("reviewInsertEmptyReady")
        val edited = "Edited English for empty synthetic editor."
        seedFixture(R.id.local_test_editor, "", 0)
        val before = state()
        startReview("EN", before)
        editReview(edited)
        confirmReady(before)
        tapDestination(R.id.local_test_editor)
        clickOwnId("ready_insert")
        successfulInsert(before, R.id.local_test_editor, edited)
    }

    @Test fun longEnglishInsertsAtMiddlePreservingKoreanAndEmoji() {
        only("reviewInsertMiddleReady")
        val prefix = "앞🙂 "
        val suffix = " 뒤🌙"
        val edited = "This deliberately long English result crosses the accessibility context boundary while preserving text on both sides."
        assertTrue("TEST_RESULT_NOT_LONG", edited.length > 64)
        seedFixture(R.id.local_test_editor, prefix + suffix, prefix.length)
        val before = state()
        startReview("EN", before)
        editReview(edited)
        confirmReady(before)
        tapDestination(R.id.local_test_editor)
        assertTrue("MIDDLE_CARET_CHANGED_BEFORE_INSERT", fixtureEditor(R.id.local_test_editor)
            ?.let { it.textSelectionStart == prefix.length && it.textSelectionEnd == prefix.length } == true)
        clickOwnId("ready_insert")
        successfulInsert(before, R.id.local_test_editor, prefix + edited + suffix)
    }

    @Test fun fixedKoreanSampleInsertsIntoEmptyFixture() {
        only("reviewKoreanReady")
        seedFixture(R.id.local_test_editor, "", 0)
        val before = state()
        startReview("요", before)
        assertTrue("FIXED_ENGLISH_NOT_SHOWN", reviewEditor()?.text?.toString() == fixedEnglish)
        confirmReady(before)
        tapDestination(R.id.local_test_editor)
        clickOwnId("ready_insert")
        successfulInsert(before, R.id.local_test_editor, "안녕하세요. 10분 후에 도착할게요.")
    }

    @Test fun selectedRangeRefusesAndPreservesResult() {
        only("reviewSelectionReady")
        val original = "Synthetic alpha omega."
        seedFixture(R.id.local_test_editor, original, original.length)
        val before = state()
        startReview("EN", before)
        confirmReady(before)
        tapDestination(R.id.local_test_editor)
        selectFixtureRange(R.id.local_test_editor, 10, 15)
        clickOwnId("ready_insert")
        await("SELECTED_RANGE_NOT_REFUSED") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits &&
                it.phase == "READY" && it.confirmation == "REFUSED"
        } }
        assertFixtureText(R.id.local_test_editor, original)
        await("RESULT_NOT_AVAILABLE_AFTER_REFUSAL") { ownId("ready_insert")?.takeIf {
            ownId("ready_copy") != null
        } }
        assertTrue("SELECTION_CHANGED_AFTER_REFUSAL", fixtureEditor(R.id.local_test_editor)
            ?.let { it.textSelectionStart == 10 && it.textSelectionEnd == 15 } == true)
        clickOwnId("ready_cancel")
        sessionAttempted = false
    }

    @Test fun koreanEditRejectsThenCancelPreservesFixture() {
        only("reviewKoreanEditReady")
        val original = "Synthetic original."
        seedFixture(R.id.local_test_editor, original, original.length)
        val before = state()
        startReview("반말", before)
        editReview("Changed English sample for Korean rejection.")
        clickOwnId("review_confirm")
        await("KOREAN_EDIT_WAS_NOT_REJECTED") { reviewEditor()?.takeIf {
            it.text?.toString() == "Changed English sample for Korean rejection."
        } }
        assertTrue("KOREAN_EDIT_EXPLANATION_NOT_VISIBLE", nodes(product).any {
            it.className?.toString() == "android.widget.TextView" && it.isVisibleToUser &&
                it.text?.toString() ==
                    "Phase 1 cannot translate edits. Restore the exact sample or choose EN in a new recording."
        })
        assertTrue("KOREAN_EDIT_PREPARED_RESULT", ownId("ready_insert") == null &&
            ownId("ready_copy") == null)
        assertTrue("DESTINATION_CAPTURED_DURING_REVIEW", state().captures == before.captures)
        clickOwnId("review_cancel")
        sessionAttempted = false
        await("REVIEW_CANCEL_DID_NOT_CLOSE") { (reviewEditor() == null).takeIf { it } }
        assertFixtureText(R.id.local_test_editor, original)
        assertTrue("CANCEL_COMMITTED", state().commits == before.commits)
        assertTrue("MIC_CLEANUP_PENDING", !state().cleanup)
    }

    @Test fun acknowledgedButUnchangedCommitIsTerminallyUncertain() {
        only("reviewUncertainReady")
        seedFixture(R.id.local_uncertain_editor, "", 0)
        val before = state()
        startReview("EN", before)
        confirmReady(before)
        tapDestination(R.id.local_uncertain_editor)
        clickOwnId("ready_insert")
        await("UNCERTAIN_COMMIT_NOT_REPORTED") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.phase == "UNCERTAIN" && it.confirmation == "UNCERTAIN"
        } }
        assertFixtureText(R.id.local_uncertain_editor, "")
        await("UNCERTAIN_RECOVERY_NOT_AVAILABLE") { ownId("ready_copy")?.takeIf {
            ownId("ready_cancel") != null
        } }
        assertTrue("UNCERTAIN_EXPOSED_RETRY", ownId("ready_insert") == null)
        SystemClock.sleep(500)
        assertTrue("UNCERTAIN_RETRIED", state().commits == before.commits + 1)
        clickOwnId("ready_cancel")
        sessionAttempted = false
    }

    private fun successfulInsert(before: State, editorId: Int, expected: String) {
        await("SYNTHETIC_INSERT_RESULT_TIMEOUT") { fixtureEditor(editorId)?.takeIf { fixtureText(it) == expected } }
        await("SUCCESS_NOT_CONFIRMED") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.phase == "NONE" && it.confirmation == "CONFIRMED" && !it.cleanup
        } }
        sessionAttempted = false
        assertTrue("BUBBLE_NOT_DISMISSED_AFTER_INSERT", !visibleOwnUi())
        assertTrue("SWIFTKEY_NOT_VISIBLE_AFTER_INSERT", imeVisible() && selectedSwiftKey())
        SystemClock.sleep(500)
        assertFixtureText(editorId, expected)
        assertTrue("INSERT_RETRIED_OR_REOPENED", state().commits == before.commits + 1 &&
            ownId("ready_insert") == null && !visibleOwnUi())
    }

    private fun startReview(mode: String, before: State) {
        assertTrue("PREEXISTING_DRAFT", before.phase == "NONE" || before.phase == "CANCELLED" ||
            before.phase == "COPIED" || before.phase == "INSERTED")
        assertTrue("DESTINATION_CAPTURED_BEFORE_RECORDING", before.captures >= 0)
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        sessionAttempted = true
        clickOwnButton(mode)
        await("STOP_NOT_AVAILABLE") { ownButton("Stop") }
        await("MIC_PROOF_NOT_OBSERVED") { state().takeIf { it.proofs == before.proofs + 1 } }
        assertTrue("DESTINATION_CAPTURED_WHILE_RECORDING", state().captures == before.captures)
        assertTrue("SELECTED_MODE_NOT_VISIBLE_DURING_RECORDING", nodes(product).any {
            it.isVisibleToUser && it.text?.toString() == "Selected mode: $mode"
        })
        clickOwnButton("Stop")
        await("EDITABLE_REVIEW_NOT_AVAILABLE") { reviewEditor()?.takeIf { it.isEditable && it.isEnabled } }
        await("REVIEW_EDITOR_OR_SWIFTKEY_NOT_FOCUSED", 4_000) {
            reviewEditor()?.takeIf { it.isFocused && imeVisible() && selectedSwiftKey() }
        }
        assertTrue("MIC_NOT_RELEASED_AT_REVIEW", !state().cleanup)
        assertTrue("DESTINATION_CAPTURED_AT_REVIEW", state().captures == before.captures)
        assertTrue("FIXED_SAMPLE_NOT_IN_REVIEW", reviewEditor()?.text?.toString() == fixedEnglish)
        assertTrue("SWIFTKEY_NOT_SELECTED_AT_REVIEW", selectedSwiftKey())
    }

    private fun editReview(edited: String) {
        val editor = await("REVIEW_EDITOR_NOT_FOUND") { reviewEditor()?.takeIf { it.isVisibleToUser } }
        assertTrue("REVIEW_EDITOR_NOT_EDITABLE", editor.isEditable && editor.isEnabled)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edited)
        }
        // Only our own review EditText is changed; this does not test SwiftKey keystrokes.
        assertTrue("OWN_REVIEW_EDIT_FAILED", editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        await("OWN_REVIEW_EDIT_NOT_VISIBLE") { reviewEditor()?.takeIf { it.text?.toString() == edited } }
    }

    private fun confirmReady(before: State) {
        clickOwnId("review_confirm")
        await("READY_CONTROLS_NOT_VISIBLE") { ownId("ready_insert")?.takeIf {
            ownId("ready_copy") != null && ownId("ready_cancel") != null
        } }
        assertTrue("DESTINATION_CAPTURED_BEFORE_EXPLICIT_INSERT", state().captures == before.captures)
        assertTrue("COMMIT_REQUESTED_BEFORE_EXPLICIT_INSERT", state().commits == before.commits)
    }

    private fun launchFixture() {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
        val end = SystemClock.elapsedRealtime() + 8_000L
        while (SystemClock.elapsedRealtime() < end) {
            if (focusedFixtureApp()) return
            SystemClock.sleep(100)
        }
        // Snapshot before JUnit cleanup can destroy the fixture. Codes/counts only.
        throw AssertionError("FIXTURE_NOT_AVAILABLE_${fixtureLaunchSnapshot()}")
    }

    private fun fixtureLaunchSnapshot(): String {
        val local = "LIFE_${SmokeEditorActivity.lifecycleCode}_" +
            "CREATES_${SmokeEditorActivity.createCount}_" +
            "PUBLISHED_${if (SmokeEditorActivity.current.get() != null) 1 else 0}_" +
            "OWN_FOCUS_${if (SmokeEditorActivity.windowFocused) 1 else 0}_" +
            "ATTACH_${SmokeEditorActivity.attachCount}_" +
            "DETACH_${SmokeEditorActivity.detachCount}_" +
            "FOCUS_CALLBACKS_${SmokeEditorActivity.focusCallbackCount}_" +
            "MAIN_HEARTBEAT_${SmokeEditorActivity.postResumeHeartbeatCount}_" +
            "DISPLAY_${SmokeEditorActivity.displayCode}"
        val remote = runCatching {
            val ui = automation()
            val windows = ui.windows
            val apps = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .map { it.isFocused to it.getRoot(0)?.packageName?.toString() }
            val focused = apps.filter { it.first }
            val imeCount = windows.count { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            val overlayCount = windows.count {
                it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
            }
            val systemCount = windows.count { it.type == AccessibilityWindowInfo.TYPE_SYSTEM }
            val otherCount = windows.size - apps.size - imeCount - overlayCount - systemCount
            val fixtureRoots = apps.count { it.second == fixture }
            val fixtureFocused = focused.count { it.second == fixture }
            val focusedCode = when {
                focused.isEmpty() -> "NONE"
                focused.size > 1 -> "MULTIPLE"
                else -> when (focused.single().second) {
                    fixture -> "FIXTURE"
                    product -> "PRODUCT"
                    null -> "ROOT_UNAVAILABLE"
                    else -> "OTHER"
                }
            }
            "WINDOWS_${windows.size.coerceAtMost(9)}_" +
                "APPS_${apps.size.coerceAtMost(9)}_" +
                "FOCUSED_${focused.size.coerceAtMost(9)}_" +
                "IME_${imeCount.coerceAtMost(9)}_" +
                "OVERLAY_${overlayCount.coerceAtMost(9)}_" +
                "SYSTEM_${systemCount.coerceAtMost(9)}_" +
                "OTHER_${otherCount.coerceAtLeast(0).coerceAtMost(9)}_" +
                "FIXTURE_ROOTS_${fixtureRoots.coerceAtMost(9)}_" +
                "FIXTURE_FOCUSED_${fixtureFocused.coerceAtMost(9)}_" +
                "FOCUSED_KIND_$focusedCode"
        }.getOrDefault("WINDOWS_UNAVAILABLE")
        val automationInfo = runCatching {
            val info = automation().serviceInfo ?: return@runCatching "INFO_UNAVAILABLE"
            "RETRIEVE_${if ((info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS) != 0) 1 else 0}_" +
                "EVENTS_${if (info.eventTypes != 0) 1 else 0}"
        }.getOrDefault("INFO_UNAVAILABLE")
        return "${local}_${remote}_$automationInfo"
    }

    private fun seedFixture(id: Int, text: String, caret: Int) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(id)
            editor.setText(text)
            editor.setSelection(caret)
            editor.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        await("FIXTURE_NOT_FOCUSED") { fixtureEditor(id)?.takeIf { it.isFocused && fixtureText(it) == text } }
        await("SWIFTKEY_NOT_VISIBLE") { imeVisible().takeIf { it } }
        SystemClock.sleep(300) // Settle fixture-generated events before a new capture.
    }

    private fun focusFixture(id: Int, showIme: Boolean) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(id)
            editor.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (showIme) imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
            else imm.hideSoftInputFromWindow(editor.windowToken, 0)
        }
    }

    private fun tapDestination(id: Int) {
        launchFixture()
        val editor = await("DESTINATION_NOT_VISIBLE") { fixtureEditor(id)?.takeIf { it.isVisibleToUser } }
        assertTrue("SYNTHETIC_DESTINATION_TAP_FAILED", editor.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("DESTINATION_NOT_FOCUSED") { fixtureEditor(id)?.takeIf { it.isFocused } }
        focusFixture(id, showIme = true)
        await("SWIFTKEY_NOT_VISIBLE_FOR_DESTINATION") { imeVisible().takeIf { it } }
        assertTrue("READY_INSERT_NOT_AVAILABLE_AFTER_FIELD_TAP", ownId("ready_insert") != null)
    }

    private fun selectFixtureRange(id: Int, start: Int, end: Int) {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            activity.findViewById<EditText>(id).setSelection(start, end)
        }
        await("FIXTURE_RANGE_NOT_READY") { fixtureEditor(id)?.takeIf {
            it.isFocused && it.textSelectionStart == start && it.textSelectionEnd == end
        } }
        SystemClock.sleep(250)
    }

    private fun only(flag: String) = assumeTrue("REVIEW_METHOD_REQUIRES_OWN_FLAG",
        InstrumentationRegistry.getArguments().getString(flag) == "true")

    private fun state(): State {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(128).map(String::trim).toList()
        }
        fun field(label: String): String = lines.firstOrNull { it.startsWith("$label: ") }
            ?.substringAfter(": ")?.trim()?.takeIf { Regex("[A-Za-z0-9_]+").matches(it) }
            ?: throw AssertionError("PRODUCT_DIAGNOSTIC_UNAVAILABLE_$label")
        val result = State(field("Experiment arm"), field("Draft phase"),
            field("Destination capture count").toIntOrNull() ?: throw AssertionError("BAD_CAPTURE_COUNT"),
            field("Commit request count").toIntOrNull() ?: throw AssertionError("BAD_COMMIT_COUNT"),
            field("Commit confirmation"),
            field("Unbound recording proof count").toIntOrNull() ?: throw AssertionError("BAD_PROOF_COUNT"),
            field("Overlay cleanup pending") == "true")
        assertTrue("BAD_PRODUCT_DIAGNOSTICS", result.captures in 0..100_000 &&
            result.commits in 0..100_000 && result.proofs in 0..100_000)
        return result
    }

    private fun fixtureEditor(id: Int): AccessibilityNodeInfo? {
        val name = when (id) {
            R.id.local_test_editor -> "local_test_editor"
            R.id.local_uncertain_editor -> "local_uncertain_editor"
            else -> throw AssertionError("UNAPPROVED_FIXTURE_EDITOR")
        }
        val matches = nodes(fixture).filter {
            it.viewIdResourceName == "$fixture:id/$name" && it.isEditable
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_EDITOR")
        return matches.singleOrNull()
    }

    private fun reviewEditor(): AccessibilityNodeInfo? = ownId("review_english")

    private fun ownId(name: String): AccessibilityNodeInfo? {
        require(name in listOf("review_english", "review_confirm", "review_cancel",
            "ready_insert", "ready_copy", "ready_cancel"))
        val matches = nodes(product).filter {
            it.viewIdResourceName == "$product:id/$name" && it.isVisibleToUser
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_CONTROL_$name")
        return matches.singleOrNull()
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in listOf("Ko", "Close", "Stop", "Cancel", "EN", "요", "반말"))
        val matches = nodes(product).filter {
            it.className?.toString() == "android.widget.Button" && it.isEnabled &&
                it.isClickable && it.isVisibleToUser &&
                it.text?.toString()?.equals(label, ignoreCase = true) == true
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_BUTTON_$label")
        return matches.singleOrNull()
    }

    private fun clickOwnId(id: String) {
        val node = await("OWN_CONTROL_NOT_FOUND_$id") { ownId(id)?.takeIf { it.isEnabled && it.isClickable } }
        assertTrue("OWN_CONTROL_CLICK_FAILED_$id", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun clickOwnButton(label: String) {
        val node = await("OWN_BUTTON_NOT_FOUND_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun fixtureText(editor: AccessibilityNodeInfo): String =
        if (editor.isShowingHintText) "" else editor.text?.toString().orEmpty()

    private fun assertFixtureText(id: Int, expected: String) {
        val editor = fixtureEditor(id) ?: throw AssertionError("FIXTURE_EDITOR_NOT_FOUND")
        assertTrue("SYNTHETIC_FIXTURE_TEXT_CHANGED", fixtureText(editor) == expected)
    }

    private fun visibleOwnUi(): Boolean = nodes(product).any { it.isVisibleToUser }

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

    private fun ownClipDescription(description: ClipDescription?): Boolean =
        description != null && description.label?.toString() == "KoScribe sample" &&
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
            description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true

    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000, probe: () -> T?): T {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError(code)
    }
}
