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
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
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
import java.util.concurrent.atomic.AtomicBoolean

/** API-free REVIEW completion leaves a collapsed Ko available in the same owned editor. */
@RunWith(AndroidJUnit4::class)
class BubbleReuseSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private val fixedEnglish = "Hello, I will arrive in ten minutes."
    private val flags = listOf("reuseInsertReady", "reuseCopyReady")
    private lateinit var instrumentation: Instrumentation
    private var sessionAttempted = false

    private data class State(
        val arm: String, val phase: String, val captures: Int, val commits: Int,
        val confirmation: String, val proofs: Int, val cleanup: Boolean,
        val autoEnabled: Boolean, val speechEnabled: Boolean, val translationEnabled: Boolean,
    )

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactSyntheticScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("REUSE_TEST_REQUIRES_ONE_EXACT_OPT_IN",
            flags.count { args.getString(it) == "true" } == 1 &&
                args.keySet().none { it.endsWith("Ready") && it !in flags && args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val initial = state()
        assertTrue("WRONG_PRODUCT_ARM", initial.arm == "REVIEW")
        assertTrue("AUTO_INSERT_MUST_BE_DISABLED", !initial.autoEnabled)
        assertTrue("SPEECH_MUST_BE_DISABLED", !initial.speechEnabled)
        assertTrue("TRANSLATION_MUST_BE_DISABLED", !initial.translationEnabled)
        assertTrue("PREEXISTING_DRAFT", initial.phase == "NONE")
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
            val cancel = ownId("review_cancel") ?: ownId("ready_cancel") ?: ownButton("Cancel")
            cancel?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // No global Back, editor mutation, keyboard cycle, or destination retry.
    }

    @Test fun confirmedInsertKeepsCollapsedKoAvailable() {
        only("reuseInsertReady")
        seedEmptyFixture()
        val before = state()
        startFixedEnglishReview(before)
        confirmReady(before)
        tapDestinationBeforeCompletion()
        val imeRemoved = AtomicBoolean(false)
        val ui = observeCurrentImeWindowRemoval(imeRemoved)
        try {
            clickOwnId("ready_insert") // Exactly one fresh guarded destination transaction.
            await("INSERT_NOT_EXACTLY_CONFIRMED") {
                assertImeContinuous(imeRemoved)
                state().takeIf {
                    it.captures == before.captures + 1 && it.commits == before.commits + 1 &&
                        it.phase == "NONE" && it.confirmation == "CONFIRMED" && !it.cleanup
                }
            }
            sessionAttempted = false
            assertDirectEditor(fixedEnglish, fixedEnglish.length)
            assertTrue("INSERT_RESULT_NOT_VISIBLE", fixtureText(fixtureEditor()
                ?: throw AssertionError("FIXTURE_EDITOR_MISSING")) == fixedEnglish)
            verifyCollapsedKoAfterCompletion(before, fixedEnglish, fixedEnglish.length,
                expectedCaptures = before.captures + 1,
                expectedCommits = before.commits + 1, expectedConfirmation = "CONFIRMED",
                imeRemoved = imeRemoved)
        } finally {
            ui.setOnAccessibilityEventListener(null)
        }
    }

    @Test fun copyAndCloseKeepsCollapsedKoAvailable() {
        only("reuseCopyReady")
        seedEmptyFixture()
        val before = state()
        startFixedEnglishReview(before)
        confirmReady(before)
        tapDestinationBeforeCompletion()
        val imeRemoved = AtomicBoolean(false)
        val ui = observeCurrentImeWindowRemoval(imeRemoved)
        try {
            clickOwnId("ready_copy")
            await("COPY_DID_NOT_COMPLETE") {
                assertImeContinuous(imeRemoved)
                state().takeIf {
                    it.captures == before.captures && it.commits == before.commits &&
                        it.phase == "NONE" && it.confirmation == "COPIED" && !it.cleanup
                }
            }
            sessionAttempted = false
            assertDirectEditor("", 0)
            assertTrue("COPY_CHANGED_FIXTURE", fixtureText(fixtureEditor()
                ?: throw AssertionError("FIXTURE_EDITOR_MISSING")) == "")
            verifyOwnFixedSampleClipboard()
            verifyCollapsedKoAfterCompletion(before, "", 0, expectedCaptures = before.captures,
                expectedCommits = before.commits, expectedConfirmation = "COPIED",
                imeRemoved = imeRemoved)
        } finally {
            ui.setOnAccessibilityEventListener(null)
        }
    }

    private fun startFixedEnglishReview(before: State) {
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        sessionAttempted = true
        clickOwnButton("EN")
        await("STOP_NOT_AVAILABLE") { ownButton("Stop") }
        await("RECORDING_PROOF_NOT_OBSERVED") { state().takeIf {
            it.proofs == before.proofs + 1 && it.captures == before.captures &&
                it.commits == before.commits
        } }
        assertTrue("SELECTED_MODE_NOT_VISIBLE", nodes(product).any {
            it.isVisibleToUser && it.text?.toString() == "Selected mode: EN"
        })
        clickOwnButton("Stop")
        await("ENGLISH_REVIEW_NOT_AVAILABLE") { ownId("review_english")?.takeIf {
            it.isEditable && it.isEnabled && it.isFocused && imeVisible()
        } }
        assertTrue("FIXED_SAMPLE_NOT_IN_REVIEW",
            ownId("review_english")?.text?.toString() == fixedEnglish)
        assertTrue("MIC_CLEANUP_PENDING_AT_REVIEW", !state().cleanup)
    }

    private fun confirmReady(before: State) {
        clickOwnId("review_confirm")
        await("MANUAL_READY_CONTROLS_NOT_VISIBLE") { ownId("ready_insert")?.takeIf {
            ownId("ready_copy") != null && ownId("ready_cancel") != null
        } }
        assertTrue("DESTINATION_CAPTURED_BEFORE_INSERT", state().captures == before.captures)
        assertTrue("COMMIT_REQUESTED_BEFORE_INSERT", state().commits == before.commits)
        assertTrue("VISUAL_BUILD_NOT_ALLOWED", nodes(product).none {
            it.text?.toString()?.startsWith("VISUAL TEST") == true
        })
    }

    private fun tapDestinationBeforeCompletion() {
        // Review has ended. This deliberate tap is part of the manual Insert baseline.
        launchFixture()
        val editor = await("DESTINATION_NOT_VISIBLE") { fixtureEditor()?.takeIf { it.isVisibleToUser } }
        assertTrue("DESTINATION_TAP_FAILED", editor.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("DESTINATION_NOT_FOCUSED") { fixtureEditor()?.takeIf { it.isFocused } }
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            val field = activity.findViewById<EditText>(R.id.local_test_editor)
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }
        await("SWIFTKEY_NOT_VISIBLE_FOR_DESTINATION") { imeVisible().takeIf { it } }
        assertTrue("MANUAL_INSERT_NOT_AVAILABLE", ownId("ready_insert") != null)
    }

    private fun observeCurrentImeWindowRemoval(imeRemoved: AtomicBoolean): UiAutomation {
        val ui = automation()
        assertTrue("IME_NOT_VISIBLE_BEFORE_COMPLETION", imeVisible() && selectedSwiftKey())
        val imeWindowIds = ui.windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }.map { it.id }.toSet()
        assertTrue("IME_WINDOW_ID_UNAVAILABLE", imeWindowIds.isNotEmpty())
        ui.setOnAccessibilityEventListener { event ->
            if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                event.windowId in imeWindowIds &&
                event.windowChanges and AccessibilityEvent.WINDOWS_CHANGE_REMOVED != 0)
                imeRemoved.set(true)
        }
        return ui
    }

    private fun assertImeContinuous(imeRemoved: AtomicBoolean) {
        assertTrue("IME_WINDOW_REMOVED_AFTER_COMPLETION", !imeRemoved.get())
        assertTrue("IME_DISAPPEARED_AFTER_COMPLETION", imeVisible() && selectedSwiftKey())
    }

    private fun verifyCollapsedKoAfterCompletion(before: State, expected: String, caret: Int,
        expectedCaptures: Int, expectedCommits: Int, expectedConfirmation: String,
        imeRemoved: AtomicBoolean) {
        await("COLLAPSED_KO_NOT_AVAILABLE_AFTER_COMPLETION") {
            assertImeContinuous(imeRemoved)
            ownButton("Ko")
        }
        assertCollapsedKoOnly()
        awaitCollapsedAndStable(before, expected, expectedCaptures, expectedCommits,
            expectedConfirmation, imeRemoved)
        openCollapsedKoModePanel(expected, caret, expectedCaptures,
            expectedCommits, before.proofs + 1)
        assertImeContinuous(imeRemoved)
    }

    private fun awaitCollapsedAndStable(before: State, expected: String, expectedCaptures: Int,
        expectedCommits: Int, expectedConfirmation: String, imeRemoved: AtomicBoolean) {
        assertImeContinuous(imeRemoved)
        assertTrue("FIXTURE_NOT_FOCUSED_AFTER_COMPLETION", fixtureEditor()?.isFocused == true)
        // Observe the bounded settle period without changing focus or IME state.
        val settleEnd = SystemClock.elapsedRealtime() + 650
        while (SystemClock.elapsedRealtime() < settleEnd) {
            assertCollapsedKoOnly()
            assertImeContinuous(imeRemoved)
            assertTrue("FIXTURE_FOCUS_LOST_AFTER_COMPLETION", fixtureEditor()?.isFocused == true)
            SystemClock.sleep(40)
        }
        assertTrue("FIXTURE_TEXT_CHANGED_AFTER_COMPLETION", fixtureEditor()?.let(::fixtureText) == expected)
        val stable = state()
        assertTrue("COMPLETION_CHANGED_OR_RETRIED", stable.phase == "NONE" &&
            stable.confirmation == expectedConfirmation && stable.captures == expectedCaptures &&
            stable.commits == expectedCommits && stable.proofs == before.proofs + 1 && !stable.cleanup)
    }

    private fun openCollapsedKoModePanel(expected: String, caret: Int,
        expectedCaptures: Int, expectedCommits: Int, expectedProofs: Int) {
        val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
        val editorObject = activity.findViewById<EditText>(R.id.local_test_editor)
        val token = editorObject.windowToken
        assertDirectEditor(expected, caret)
        clickOwnButton("Ko")
        await("MODE_PANEL_DID_NOT_OPEN") { ownButton("EN")?.takeIf {
            ownButton("요") != null && ownButton("반말") != null && ownButton("Close") != null
        } }
        assertTrue("IME_NOT_VISIBLE_WITH_MODE_PANEL", imeVisible() && selectedSwiftKey())
        assertTrue("EDITOR_REPLACED_WHILE_OPENING_MODE_PANEL",
            SmokeEditorActivity.current.get() === activity &&
                activity.findViewById<EditText>(R.id.local_test_editor) === editorObject &&
                editorObject.windowToken === token)
        assertDirectEditor(expected, caret)
        assertTrue("REUSE_STARTED_RECORDING_OR_WROTE", state().let {
            it.phase == "NONE" && it.captures == expectedCaptures &&
                it.commits == expectedCommits && it.proofs == expectedProofs && !it.cleanup
        })
        clickOwnButton("Close") // Collapse without choosing another mode or touching the field.
        await("MODE_PANEL_DID_NOT_CLOSE") { ownButton("Ko")?.takeIf {
            ownButton("EN") == null && ownButton("Close") == null
        } }
        assertCollapsedKoOnly()
        assertDirectEditor(expected, caret)
        assertTrue("REUSE_CLOSE_WROTE", state().let {
            it.phase == "NONE" && it.captures == expectedCaptures &&
                it.commits == expectedCommits && it.proofs == expectedProofs && !it.cleanup
        })
    }

    private fun assertCollapsedKoOnly() {
        assertTrue("COLLAPSED_KO_NOT_VISIBLE", ownButton("Ko") != null)
        assertTrue("COMPLETED_RESULT_OR_ACTIVE_PANEL_REMAINS",
            ownId("ready_insert") == null && ownId("ready_copy") == null &&
                ownId("ready_cancel") == null && ownId("review_english") == null &&
                ownId("review_confirm") == null && ownId("review_cancel") == null &&
                ownButton("EN") == null && ownButton("요") == null &&
                ownButton("반말") == null && ownButton("Stop") == null &&
                ownButton("Close") == null && ownButton("Cancel") == null)
    }

    private fun verifyOwnFixedSampleClipboard() {
        assertTrue("FIXTURE_NOT_FOREGROUND_FOR_OWN_CLIP", focusedFixtureApp())
        val clipboard = instrumentation.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val description = clipboard.primaryClipDescription
        assertTrue("OWN_FIXED_CLIP_METADATA_MISSING", description != null &&
            description.label?.toString() == "KoScribe sample" &&
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
            description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true)
        val clip = clipboard.primaryClip
        assertTrue("OWN_FIXED_CLIP_CHANGED_BEFORE_READ", clip != null && clip.itemCount == 1 &&
            clip.description.label?.toString() == "KoScribe sample" &&
            clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
            clip.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true)
        assertTrue("OWN_FIXED_CLIP_MISMATCH", clip!!.getItemAt(0).text?.toString() == fixedEnglish)
    }

    private fun launchFixture() {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        await("FIXTURE_NOT_FOREGROUND") { focusedFixtureApp().takeIf { it } }
    }

    private fun seedEmptyFixture() {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            editor.setText("")
            editor.setSelection(0)
            editor.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        await("EMPTY_FIXTURE_NOT_FOCUSED") { fixtureEditor()?.takeIf {
            it.isFocused && fixtureText(it) == ""
        } }
        await("SWIFTKEY_NOT_VISIBLE") { imeVisible().takeIf { it } }
        assertDirectEditor("", 0) // Known empty node selection may be -1/-1.
        SystemClock.sleep(300)
    }

    private fun assertDirectEditor(expected: String, caret: Int) {
        var valid = false
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            valid = editor.isFocused && editor.isAttachedToWindow && editor.text.toString() == expected &&
                editor.selectionStart == caret && editor.selectionEnd == caret
        }
        assertTrue("DIRECT_OWNED_EDITOR_TEXT_FOCUS_OR_CARET_CHANGED", valid)
    }

    private fun only(flag: String) = assumeTrue("REUSE_METHOD_REQUIRES_OWN_FLAG",
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
        return State(field("Experiment arm"), field("Draft phase"),
            field("Destination capture count").toIntOrNull() ?: throw AssertionError("BAD_CAPTURE_COUNT"),
            field("Commit request count").toIntOrNull() ?: throw AssertionError("BAD_COMMIT_COUNT"),
            field("Commit confirmation"),
            field("Unbound recording proof count").toIntOrNull() ?: throw AssertionError("BAD_PROOF_COUNT"),
            field("Overlay cleanup pending") == "true",
            field("Auto insert enabled") == "true",
            field("Speech input enabled") == "true",
            field("Translation input enabled") == "true")
    }

    private fun fixtureEditor(): AccessibilityNodeInfo? {
        val matches = nodes(fixture).filter {
            it.viewIdResourceName == "$fixture:id/local_test_editor" && it.isEditable
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_EDITOR")
        return matches.singleOrNull()
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
