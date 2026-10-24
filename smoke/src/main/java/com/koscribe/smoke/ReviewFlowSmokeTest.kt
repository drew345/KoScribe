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
            "reviewKoreanEditReady", "reviewUncertainReady", "reviewConfirmationProbeReady",
            "reviewComposedExactReady", "reviewComposedMismatchReady")
        val olderFlags = listOf("autofillSaveNoReady", "externalDraftReady", "externalRecordCancelReady",
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
            if (ownReviewCancelCandidate() != null) {
                revealOwnReviewCancel(1_500)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                ownId("ready_cancel")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    ?: ownButton("Cancel")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
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

    @Test fun editedEnglishEmptyInsertReportsNaturalConfirmationOrTerminalProbe() {
        only("reviewConfirmationProbeReady")
        val edited = "Edited English for empty synthetic editor."
        seedFixture(R.id.local_test_editor, "", 0)
        val before = state()
        startReview("EN", before)
        editReview(edited)
        confirmReady(before)
        tapDestination(R.id.local_test_editor)
        clickOwnId("ready_insert") // One synthetic commit attempt; no recovery Insert.
        await("SYNTHETIC_INSERT_RESULT_TIMEOUT") {
            fixtureEditor(R.id.local_test_editor)?.takeIf { fixtureText(it) == edited }
        }
        val terminal = await("POST_COMMIT_TERMINAL_OUTCOME_NOT_REPORTED") {
            val current = state()
            if (current.captures > before.captures + 1 || current.commits > before.commits + 1)
                throw AssertionError("INSERT_RETRIED_OR_RECAPTURED")
            if (current.captures == before.captures + 1 && current.commits == before.commits + 1 &&
                ((current.phase == "NONE" && current.confirmation == "CONFIRMED") ||
                    (current.phase == "UNCERTAIN" && current.confirmation == "UNCERTAIN"))) current
            else null
        }
        assertTrue("MIC_CLEANUP_PENDING_AFTER_INSERT", !terminal.cleanup)
        assertTrue("SWIFTKEY_NOT_VISIBLE_AFTER_PROBE_INSERT", imeVisible() && selectedSwiftKey())
        when (terminal.confirmation) {
            "CONFIRMED" -> {
                sessionAttempted = false
                assertTrue("CONFIRMED_INSERT_DID_NOT_DISMISS", !visibleOwnUi())
                assertTrue("CONFIRMED_INSERT_EXPOSED_RETRY", ownId("ready_insert") == null)
                SystemClock.sleep(500)
                assertFixtureText(R.id.local_test_editor, edited)
                val stable = state()
                assertTrue("CONFIRMED_INSERT_RETRIED_OR_CHANGED", stable.captures == before.captures + 1 &&
                    stable.commits == before.commits + 1 && stable.phase == "NONE" &&
                    stable.confirmation == "CONFIRMED" && !stable.cleanup)
                reportConfirmationProbe("NATURAL_CONFIRMED")
            }
            "UNCERTAIN" -> {
                val codes = await("POST_DIAG_COMPLETION_NOT_REPORTED") {
                    postCommitProbeCodes()?.takeIf { events ->
                        events.any { it == "POST_DIAG_DONE" || it.startsWith("POST_DIAG_ABORT_") ||
                            it == "POST_COMPOSE_DONE" || it.startsWith("POST_COMPOSE_ABORT_") }
                    }
                }
                reportPostCommitProbeCodes(codes)
                val diagnosticOutcome = assessPostCommitProbe(codes)
                await("UNCERTAIN_RECOVERY_NOT_AVAILABLE") { ownId("ready_copy")?.takeIf {
                    ownId("ready_cancel") != null
                } }
                assertTrue("UNCERTAIN_EXPOSED_RETRY_OR_EDIT", ownId("ready_insert") == null &&
                    reviewEditor() == null)
                SystemClock.sleep(500)
                assertFixtureText(R.id.local_test_editor, edited)
                val stable = state()
                assertTrue("UNCERTAIN_RETRIED_OR_RELABELED", stable.captures == before.captures + 1 &&
                    stable.commits == before.commits + 1 && stable.phase == "UNCERTAIN" &&
                    stable.confirmation == "UNCERTAIN" && !stable.cleanup)
                assertTrue("UNCERTAIN_REEXPOSED_INSERT_OR_EDIT", ownId("ready_insert") == null &&
                    reviewEditor() == null && ownId("ready_copy") != null &&
                    ownId("ready_cancel") != null)
                clickOwnId("ready_cancel")
                await("CANCEL_DID_NOT_END_DRAFT") { state().takeIf {
                    it.phase == "NONE" && it.confirmation == "UNCERTAIN" && !it.cleanup &&
                        it.captures == before.captures + 1 && it.commits == before.commits + 1
                } }
                sessionAttempted = false
                assertFixtureText(R.id.local_test_editor, edited)
                reportConfirmationProbe(diagnosticOutcome)
            }
            else -> throw AssertionError("UNEXPECTED_CONFIRMATION_OUTCOME")
        }
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

    @Test fun exactComposedCommitConfirmsAndDismissesOnce() {
        only("reviewComposedExactReady")
        val editorId = R.id.local_composed_match_editor
        val edited = "Exact synthetic composed result."
        await("COMPOSED_MATCH_EDITOR_NOT_AVAILABLE") { fixtureEditor(editorId) }
        seedFixture(editorId, "", 0)
        val fixtureCommits = composedFixtureCommitCount(editorId)
        val before = state()
        startReview("EN", before)
        editReview(edited)
        confirmReady(before)
        tapDestination(editorId)
        clickOwnId("ready_insert")
        successfulInsert(before, editorId, edited)
        assertTrue("COMPOSED_EXACT_CARET_MISMATCH", fixtureEditor(editorId)?.let {
            it.textSelectionStart == edited.length && it.textSelectionEnd == edited.length
        } == true)
        assertTrue("COMPOSED_FIXTURE_COMMITTED_MORE_THAN_ONCE",
            composedFixtureCommitCount(editorId) == fixtureCommits + 1)
        assertProductObservedPostcommitComposition()
    }

    @Test fun wrongComposedCommitStaysTerminallyUncertain() {
        only("reviewComposedMismatchReady")
        val editorId = R.id.local_composed_mismatch_editor
        val edited = "Expected synthetic composed result."
        await("COMPOSED_MISMATCH_EDITOR_NOT_AVAILABLE") { fixtureEditor(editorId) }
        seedFixture(editorId, "", 0)
        val fixtureCommits = composedFixtureCommitCount(editorId)
        val before = state()
        startReview("EN", before)
        editReview(edited)
        confirmReady(before)
        tapDestination(editorId)
        clickOwnId("ready_insert")
        await("WRONG_COMPOSED_FIXTURE_RESULT_NOT_VISIBLE") { fixtureEditor(editorId)?.takeIf {
            fixtureText(it) == SmokeEditorActivity.COMPOSED_WRONG_RESULT
        } }
        assertTrue("WRONG_COMPOSED_CARET_NOT_COLLAPSED", fixtureEditor(editorId)?.let {
            val end = SmokeEditorActivity.COMPOSED_WRONG_RESULT.length
            it.textSelectionStart == end && it.textSelectionEnd == end
        } == true)
        await("WRONG_COMPOSED_COMMIT_NOT_UNCERTAIN") { state().takeIf {
            it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                it.phase == "UNCERTAIN" && it.confirmation == "UNCERTAIN" && !it.cleanup
        } }
        assertTrue("COMPOSED_FIXTURE_COMMITTED_MORE_THAN_ONCE",
            composedFixtureCommitCount(editorId) == fixtureCommits + 1)
        val codes = assertProductObservedPostcommitComposition()
        assertTrue("WRONG_COMPOSED_RESULT_WAS_NOT_THE_TERMINAL_CAUSE",
            codes.contains("FRESH_COMMIT_UNCERTAIN_COMPOSED_POSTCONTEXT_UNCONFIRMED"))
        await("WRONG_COMPOSED_RECOVERY_NOT_AVAILABLE") { ownId("ready_copy")?.takeIf {
            ownId("ready_cancel") != null
        } }
        assertTrue("WRONG_COMPOSED_EXPOSED_RETRY_OR_EDIT", ownId("ready_insert") == null &&
            reviewEditor() == null)
        SystemClock.sleep(500)
        assertFixtureText(editorId, SmokeEditorActivity.COMPOSED_WRONG_RESULT)
        val stable = state()
        assertTrue("WRONG_COMPOSED_RETRIED_OR_RELABELED", stable.captures == before.captures + 1 &&
            stable.commits == before.commits + 1 && stable.phase == "UNCERTAIN" &&
            stable.confirmation == "UNCERTAIN" && !stable.cleanup &&
            ownId("ready_insert") == null && reviewEditor() == null &&
            ownId("ready_copy") != null && ownId("ready_cancel") != null)
        clickOwnId("ready_cancel")
        await("WRONG_COMPOSED_CANCEL_DID_NOT_CLEAR_DRAFT") { state().takeIf {
            it.phase == "NONE" && it.confirmation == "UNCERTAIN" && !it.cleanup &&
                it.captures == before.captures + 1 && it.commits == before.commits + 1
        } }
        sessionAttempted = false
        assertFixtureText(editorId, SmokeEditorActivity.COMPOSED_WRONG_RESULT)
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
        clickOwnReviewCancel()
        await("REVIEW_CANCEL_DID_NOT_CLOSE") { state().takeIf {
            it.phase == "NONE" && it.captures == before.captures &&
                it.commits == before.commits && !ownReviewControlsRemain()
        } }
        sessionAttempted = false
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
        val args = InstrumentationRegistry.getArguments()
        val composed = args.getString("reviewComposedExactReady") == "true" ||
            args.getString("reviewComposedMismatchReady") == "true"
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
            putExtra(SmokeEditorActivity.EXTRA_COMPOSED_FIXTURES, composed)
        })
        val end = SystemClock.elapsedRealtime() + 8_000L
        while (SystemClock.elapsedRealtime() < end) {
            if (focusedFixtureApp()) {
                val fixtureNodes = nodes(fixture)
                val protectedAbsent = fixtureNodes.none {
                    it.viewIdResourceName == "$fixture:id/local_protected_editor"
                }
                val composedCount = fixtureNodes.count {
                    it.viewIdResourceName in listOf("$fixture:id/local_composed_match_editor",
                        "$fixture:id/local_composed_mismatch_editor")
                }
                if (protectedAbsent && composedCount == (if (composed) 2 else 0)) return
            }
            SystemClock.sleep(100)
        }
        if (focusedFixtureApp()) throw AssertionError("REVIEW_PROTECTED_FIELD_NOT_OMITTED")
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

    private fun postCommitProbeCodes(): List<String>? {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(128).map(String::trim).toList()
        }
        val heading = lines.indexOf("Recent guard events (codes only):")
        if (heading < 0) throw AssertionError("PRODUCT_GUARD_CODES_UNAVAILABLE")
        val codes = lines.drop(heading + 1).mapNotNull { line ->
            Regex("^[0-9]{1,9} ([A-Z0-9_-]+)$").matchEntire(line)?.groupValues?.get(1)
        }
        val request = codes.indexOfLast { it == "FRESH_COMMIT_REQUESTED_ONCE" }
        if (request < 0) return null
        return codes.drop(request + 1)
    }

    private fun assessPostCommitProbe(codes: List<String>): String {
        val caretOriginal = codes.indexOfFirst { it.startsWith("POST_DIAG_ORIG_MS_") }
        val composeOriginal = codes.indexOfFirst { it.startsWith("POST_COMPOSE_ORIG_MS_") }
        assertTrue("POST_PROBE_ORIGINAL_KIND_UNKNOWN", (caretOriginal >= 0) != (composeOriginal >= 0))
        val composition = composeOriginal >= 0
        val prefix = if (composition) "POST_COMPOSE" else "POST_DIAG"
        val original = if (composition) composeOriginal else caretOriginal
        val terminal = codes.indexOfFirst { it == "FRESH_COMMIT_UNCERTAIN_TARGET_CHANGED" }
        assertTrue("POST_PROBE_TERMINAL_SEQUENCE_INVALID", terminal > original)
        val originalConjunction = if (composition) {
            val match = Regex("""POST_COMPOSE_ORIG_MS_\d+_I([01])_CX([01])_CK([01])_NK([01])_NA([01])_SP[01]_SC(?:[0-9]|1[0-6])_SS-?\d+_SE-?\d+_SZ[01]_SM[01]_SX[01]""")
                .matchEntire(codes[original])
            val active = codes.indexOfFirst { it == "OBS_ACTIVE_COMPOSITION" }
            assertTrue("POST_COMPOSE_ORIGINAL_FORMAT_OR_SEQUENCE_INVALID",
                match != null && active > original && active < terminal)
            (1..5).all { match!!.groupValues[it] == "1" }
        } else {
            assertTrue("POST_DIAG_ORIGINAL_FORMAT_INVALID",
                Regex("""POST_DIAG_ORIG_MS_\d+_R[01]_N[01]_G[01]_CO-?\d+_CS-?\d+_CE-?\d+_CL-?\d+_CX[01]_CK[01]_B-?\d+_-?\d+_-?\d+_[01]""")
                    .matches(codes[original]))
            false
        }
        val done = codes.indexOfFirst { it == "${prefix}_DONE" }
        val abort = codes.indexOfFirst { it.startsWith("${prefix}_ABORT_") }
        assertTrue("POST_PROBE_OUTCOME_AMBIGUOUS", (done >= 0) != (abort >= 0))
        val end = if (done >= 0) done else abort
        assertTrue("POST_PROBE_ENDED_BEFORE_TERMINAL", end > terminal)
        val labels = if (composition) listOf("T0", "T1", "T2") else listOf("T0", "T1")
        val samples = labels.map { label ->
            codes.indexOfFirst { it.startsWith("${prefix}_${label}_MS_") }
        }
        assertTrue("POST_PROBE_MISSING_T0", samples.first() > terminal && samples.first() < end)
        if (done >= 0) {
            assertTrue("POST_PROBE_SAMPLES_INCOMPLETE", samples.all { it in (terminal + 1) until done } &&
                samples.zipWithNext().all { (a, b) -> b > a })
            assertTrue("POST_PROBE_SAMPLE_FORMAT_UNKNOWN", samples.indices.all { index ->
                knownPostDiagnosticSample(codes[samples[index]], prefix, labels[index])
            })
            return when {
                !composition -> "CARET_DIAGNOSTIC_COMPLETE"
                originalConjunction -> "COMPOSE_DIAGNOSTIC_COMPLETE_CONJUNCTIVE_ORIG"
                else -> "COMPOSE_DIAGNOSTIC_COMPLETE_NONPROVING"
            }
        }
        assertTrue("POST_PROBE_ABORT_REASON_UNKNOWN", knownPostDiagnosticAbort(codes[abort], prefix))
        val observedSamples = samples.filter { it in (terminal + 1) until abort }
        assertTrue("POST_PROBE_ABORT_SAMPLE_ORDER_INVALID",
            observedSamples.zipWithNext().all { (a, b) -> b > a } &&
                observedSamples.all { index -> labels.any { label ->
                    knownPostDiagnosticSample(codes[index], prefix, label) ||
                        knownPostDiagnosticRefusal(codes[index], prefix, label)
                } })
        return if (composition) "COMPOSE_DIAGNOSTIC_INCOMPLETE_ABORT"
            else "CARET_DIAGNOSTIC_INCOMPLETE_ABORT"
    }

    private fun knownPostDiagnosticSample(code: String, prefix: String, label: String): Boolean {
        require(prefix == "POST_DIAG" || prefix == "POST_COMPOSE")
        require(label in listOf("T0", "T1", "T2"))
        val base = """${prefix}_${label}_MS_\d+_ENDMS_\d+_RRMS_\d+_FRMS_\d+_R[01]_RR[01]_N[01]_G[01]_E[01]_CO-?\d+_CS-?\d+_CE-?\d+_CL-?\d+_CX[01]_CK[01]_B-?\d+_-?\d+_-?\d+_[01]_FR[01]_A-?\d+_-?\d+_-?\d+_[01]"""
        val span = if (prefix == "POST_COMPOSE")
            """_SP[01]_SC(?:[0-9]|1[0-6])_SS-?\d+_SE-?\d+_SZ[01]_SM[01]_SX[01]"""
        else ""
        return Regex(base + span).matches(code)
    }

    private fun knownPostDiagnosticRefusal(code: String, prefix: String, label: String): Boolean {
        val reason = """(?:DEVICE_NOT_READY|INPUT_MISMATCH|WINDOW_MISMATCH|ROOT_MISMATCH|IDENTITY_MISMATCH_R[01]_G[01]|ROOT_REFRESH_FAILED_RRMS_\d+|FOCUS_MISMATCH|IDENTITY_MISMATCH_N[01]_E[01]|CONNECTION_MISSING|FOCUS_REFRESH_FAILED_RRMS_\d+_FRMS_\d+_B-?\d+_-?\d+_-?\d+_[01])"""
        return Regex("""${prefix}_${label}_MS_\d+_ENDMS_\d+_$reason""").matches(code)
    }

    private fun knownPostDiagnosticAbort(code: String, prefix: String): Boolean {
        val lifecycle = """(?:INPUT_STARTED|INPUT_FINISHED|FOCUS_WINDOW_CHANGED|DEADLINE|DRAFT_CHANGED)"""
        val refusal = """(?:DEVICE_NOT_READY|INPUT_MISMATCH|WINDOW_MISMATCH|ROOT_MISMATCH|IDENTITY_MISMATCH_R[01]_G[01]|ROOT_REFRESH_FAILED_RRMS_\d+|FOCUS_MISMATCH|IDENTITY_MISMATCH_N[01]_E[01]|CONNECTION_MISSING|FOCUS_REFRESH_FAILED_RRMS_\d+_FRMS_\d+_B-?\d+_-?\d+_-?\d+_[01])"""
        return Regex("""${prefix}_ABORT_(?:$lifecycle|$refusal)""").matches(code)
    }

    private fun reportPostCommitProbeCodes(codes: List<String>) {
        val diagnostic = codes.filter {
            it.startsWith("POST_DIAG_") || it.startsWith("POST_COMPOSE_") ||
                it == "FRESH_COMMIT_UNCERTAIN_TARGET_CHANGED"
        }
        assertTrue("POST_DIAG_CODE_OUTPUT_INVALID", diagnostic.size in 2..10 && diagnostic.all {
            it.length <= 384 && Regex("[A-Z0-9_-]+").matches(it)
        })
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_confirmation_probe_codes", diagnostic.joinToString("|"))
        })
    }

    private fun reportConfirmationProbe(code: String) {
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_confirmation_probe", code)
        })
    }

    private fun fixtureEditor(id: Int): AccessibilityNodeInfo? {
        val name = when (id) {
            R.id.local_test_editor -> "local_test_editor"
            R.id.local_uncertain_editor -> "local_uncertain_editor"
            R.id.local_composed_match_editor -> "local_composed_match_editor"
            R.id.local_composed_mismatch_editor -> "local_composed_mismatch_editor"
            else -> throw AssertionError("UNAPPROVED_FIXTURE_EDITOR")
        }
        val matches = nodes(fixture).filter {
            it.viewIdResourceName == "$fixture:id/$name" && it.isEditable
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_EDITOR")
        return matches.singleOrNull()
    }

    private fun composedFixtureCommitCount(id: Int): Int {
        var count = -1
        instrumentation.runOnMainSync {
            count = SmokeEditorActivity.current.get()?.composedCommitCount(id)
                ?: throw AssertionError("COMPOSED_FIXTURE_ACTIVITY_MISSING")
        }
        return count
    }

    private fun assertProductObservedPostcommitComposition(): List<String> {
        val codes = postCommitProbeCodes()
            ?: throw AssertionError("COMPOSED_PRODUCT_COMMIT_TRACE_MISSING")
        assertTrue("COMPOSING_NOT_OBSERVED_AFTER_CURRENT_COMMIT",
            codes.count { it == "POSTCOMMIT_COMPOSING_OBSERVED" } == 1)
        return codes
    }

    private fun reviewEditor(): AccessibilityNodeInfo? = ownId("review_english")

    private fun ownReviewCancelCandidate(): AccessibilityNodeInfo? {
        val reviewRoots = automation().windows.asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }
            .mapNotNull { it.getRoot(0) }
            .filter { it.packageName?.toString() == product }
            .filter { root ->
                root.findAccessibilityNodeInfosByViewId("$product:id/review_english")
                    .singleOrNull()?.let { it.packageName?.toString() == product && it.isEditable } == true &&
                    root.findAccessibilityNodeInfosByViewId("$product:id/review_confirm")
                        .singleOrNull()?.packageName?.toString() == product
            }.toList()
        if (reviewRoots.size > 1) throw AssertionError("AMBIGUOUS_OWN_REVIEW_WINDOW")
        val matches = reviewRoots.singleOrNull()
            ?.findAccessibilityNodeInfosByViewId("$product:id/review_cancel").orEmpty()
            .filter { it.packageName?.toString() == product &&
                it.viewIdResourceName == "$product:id/review_cancel" &&
                it.className?.toString() == "android.widget.Button" &&
                it.text?.toString()?.equals("Cancel", ignoreCase = true) == true }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_REVIEW_CANCEL")
        return matches.singleOrNull()
    }

    private fun revealOwnReviewCancel(timeoutMs: Long): AccessibilityNodeInfo? {
        val original = ownReviewCancelCandidate() ?: return null
        if (!original.isEnabled || !original.isClickable) return null
        if (original.isVisibleToUser) return original
        original.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            val current = ownReviewCancelCandidate() ?: return null
            if (current != original || current.windowId != original.windowId)
                throw AssertionError("OWN_REVIEW_CANCEL_CHANGED_DURING_REVEAL")
            if (current.isVisibleToUser && current.isEnabled && current.isClickable) return current
            SystemClock.sleep(100)
        }
        return null
    }

    private fun clickOwnReviewCancel() {
        val cancel = revealOwnReviewCancel(4_000)
            ?: throw AssertionError("OWN_REVIEW_CANCEL_NOT_REVEALED")
        assertTrue("OWN_REVIEW_CANCEL_CLICK_FAILED",
            cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun ownReviewControlsRemain(): Boolean = automation().windows.asSequence()
        .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        .mapNotNull { it.getRoot(0) }
        .filter { it.packageName?.toString() == product }
        .any { root ->
            root.findAccessibilityNodeInfosByViewId("$product:id/review_english").isNotEmpty() ||
                root.findAccessibilityNodeInfosByViewId("$product:id/review_cancel").isNotEmpty()
        }

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
