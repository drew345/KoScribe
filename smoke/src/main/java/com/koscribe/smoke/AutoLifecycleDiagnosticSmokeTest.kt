package com.koscribe.smoke

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
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

/** One opt-in, API-free, non-inserting observation of the owned editor/review/Cancel lifecycle. */
@RunWith(AndroidJUnit4::class)
class AutoLifecycleDiagnosticSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private lateinit var instrumentation: Instrumentation
    private var sessionStarted = false
    private var cancelClicked = false
    private var listenerInstalled = false
    private val stageCodes = ArrayList<String>()
    private var stageStartMs = 0L
    private var lastStageSignature: String? = null
    private var stageOverflow = false

    private data class Counts(
        val arm: String, val auto: Boolean, val speech: Boolean, val translation: Boolean,
        val phase: String, val cleanup: Boolean, val proofs: Int, val captures: Int,
        val commits: Int, val autoCaptures: Int, val autoAttempts: Int, val autoFallbacks: Int,
        val intentPhase: String, val intentReason: String,
    )

    private data class Original(
        val windowId: Int, val root: AccessibilityNodeInfo, val node: AccessibilityNodeInfo,
        val activity: SmokeEditorActivity, val editor: EditText, val token: IBinder,
        val resource: String?, val className: String?, val uniqueId: String?, val inputType: Int,
        val creates: Int, val attaches: Int, val detaches: Int,
    )

    private data class ReviewOwner(
        val windowId: Int, val root: AccessibilityNodeInfo, val node: AccessibilityNodeInfo,
    )

    private data class Identity(
        val originalWindowPresent: Boolean, val originalWindowFocused: Boolean,
        val rootSame: Boolean, val nodeSame: Boolean, val nodeRefreshed: Boolean,
        val resourceSame: Boolean, val classSame: Boolean, val uniqueRole: String,
        val inputTypeSame: Boolean, val nodeEmpty: Boolean, val nodeCaretRole: String,
        val activitySame: Boolean, val editorSame: Boolean, val tokenSame: Boolean,
        val editorAttached: Boolean, val ownedEmpty: Boolean, val ownedCaretZero: Boolean,
        val createsDelta: Int, val attachesDelta: Int, val detachesDelta: Int,
    ) {
        fun code(): String =
            "WIN_${bit(originalWindowPresent)}_FOCUS_${bit(originalWindowFocused)}_" +
                "ROOT_${bit(rootSame)}_NODE_${bit(nodeSame)}_REFRESH_${bit(nodeRefreshed)}_" +
                "RESOURCE_${bit(resourceSame)}_CLASS_${bit(classSame)}_UNIQUE_${uniqueRole}_" +
                "INPUT_${bit(inputTypeSame)}_NODE_EMPTY_${bit(nodeEmpty)}_" +
                "NODE_CARET_${nodeCaretRole}_ACTIVITY_${bit(activitySame)}_" +
                "EDITOR_${bit(editorSame)}_TOKEN_${bit(tokenSame)}_" +
                "ATTACHED_${bit(editorAttached)}_OWN_EMPTY_${bit(ownedEmpty)}_" +
                "OWN_CARET_0_${bit(ownedCaretZero)}_LIFE_${createsDelta}_${attachesDelta}_${detachesDelta}"

        fun stable(): Boolean = originalWindowPresent && originalWindowFocused &&
            rootSame && nodeSame && nodeRefreshed && resourceSame && classSame &&
            uniqueRole != "CHANGED" && inputTypeSame && nodeEmpty &&
            nodeCaretRole != "MISMATCH" && activitySame && editorSame && tokenSame &&
            editorAttached && ownedEmpty && ownedCaretZero &&
            createsDelta == 0 && attachesDelta == 0 && detachesDelta == 0
    }

    private class OriginalWindowEvents(private val windowId: Int, private val startMs: Long) :
        UiAutomation.OnAccessibilityEventListener {
        private val lock = Any()
        private val codes = ArrayList<String>()
        private var overflow = false
        private var removedCallbackAt: Long? = null
        private var added = 0
        private var removed = 0

        override fun onAccessibilityEvent(event: AccessibilityEvent) {
            if (event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                event.windowId != windowId) return
            val mask = event.windowChanges
            val isRemoved = mask and AccessibilityEvent.WINDOWS_CHANGE_REMOVED != 0
            val isAdded = mask and AccessibilityEvent.WINDOWS_CHANGE_ADDED != 0
            val role = when {
                isRemoved && isAdded -> "REMOVED_ADDED"
                isRemoved -> "REMOVED"
                isAdded -> "ADDED"
                else -> "OTHER"
            }
            // Listener receipt time; event occurrence time may precede this callback.
            val callbackAt = SystemClock.elapsedRealtime()
            synchronized(lock) {
                if (isRemoved) {
                    removed++
                    if (removedCallbackAt == null) removedCallbackAt = callbackAt
                }
                if (isAdded) added++
                if (codes.size < 16) codes.add("${role}_MASK_${mask.coerceIn(0, 65_535)}_" +
                    "CALLBACK_AT_${elapsed(callbackAt, startMs)}")
                else overflow = true
            }
        }

        fun code(reviewAtMs: Long): String = synchronized(lock) {
            "REMOVED_${removed.coerceAtMost(99)}_ADDED_${added.coerceAtMost(99)}_" +
                "FIRST_REMOVED_CALLBACK_${removedCallbackAt?.let { elapsed(it, startMs) } ?: -1}_" +
                "REMOVED_CALLBACK_REL_REVIEW_OBSERVED_${removedCallbackAt?.let {
                    signedDelta(it, reviewAtMs) } ?: "NONE"}_" +
                "OVERFLOW_${bit(overflow)}_EVENTS_${if (codes.isEmpty()) "NONE" else codes.joinToString("|")}"
        }
    }

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("AUTO_LIFECYCLE_REQUIRES_EXACT_OPT_IN",
            args.getString("autoLifecycleReady") == "true" &&
                args.keySet().none { it.endsWith("Ready") && it != "autoLifecycleReady" &&
                    args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        val initial = counts()
        assertTrue("WRONG_PRODUCT_FLAGS", initial.arm == "REVIEW" && initial.auto &&
            !initial.speech && !initial.translation)
        assertTrue("PREEXISTING_DRAFT_OR_CLEANUP", initial.phase == "NONE" && !initial.cleanup)
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
            Manifest.permission.RECORD_AUDIO, product) == PackageManager.PERMISSION_GRANTED)
        assertTrue("SWIFTKEY_NOT_SELECTED", selectedSwiftKey())
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        launchFixture()
        seedEmptyFixture()
    }

    @After fun removeListenerAndCancelOwnSession() {
        if (!::instrumentation.isInitialized) return
        if (listenerInstalled) runCatching { automation().setOnAccessibilityEventListener(null) }
        listenerInstalled = false
        if (sessionStarted && !cancelClicked) {
            val reviewCancelled = runCatching {
                ownReviewControl("review_cancel")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }.getOrNull() == true
            if (!reviewCancelled) runCatching {
                ownButton("Cancel")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    @Test fun originalWindowReviewCancelReturnIsObservedWithoutInsertion() {
        val before = counts()
        val startMs = SystemClock.elapsedRealtime()
        stageStartMs = startMs
        val original = captureOriginal()
        val events = OriginalWindowEvents(original.windowId, startMs)
        val baseline = observeOriginal(original)
        assertTrue("ORIGINAL_BASELINE_NOT_OWNED_AND_EMPTY", baseline.stable() &&
            focusedRole() == "FIXTURE" && imeVisible() && selectedSwiftKey())
        report("baseline", baseline.code())
        automation().setOnAccessibilityEventListener(events)
        listenerInstalled = true

        val modeAt = SystemClock.elapsedRealtime()
        sampleStage("MODE", original)
        await("BUBBLE_NOT_AVAILABLE") { ownButton("Ko") ?: ownButton("Close") }
        if (ownButton("Ko") != null) clickOwnButton("Ko")
        sessionStarted = true
        clickOwnButton("요")
        await("STOP_NOT_AVAILABLE") {
            sampleStage("RECORD", original)
            ownButton("Stop")
        }
        await("MIC_PROOF_NOT_OBSERVED") { counts().takeIf { it.proofs == before.proofs + 1 } }
        val recording = observeOriginal(original)
        report("recording", recording.code())
        assertNoWrite(before)

        val stopAt = SystemClock.elapsedRealtime()
        clickOwnButton("Stop")
        val review = await("OWN_REVIEW_NOT_AVAILABLE") {
            sampleStage("ENTRY", original)
            discoverReviewOwner()
        }
        val reviewAt = SystemClock.elapsedRealtime()
        assertTrue("OWN_REVIEW_EDITOR_NOT_FOCUSED", review.node.isFocused && review.node.isEditable)
        assertTrue("OWN_REVIEW_CANCEL_NOT_AVAILABLE", ownReviewControl("review_cancel") != null)
        val inReview = observeOriginal(original)
        report("review", inReview.code())
        report("review_owner", "FOCUSED_1_EDITOR_1_CANCEL_1_ORIGINAL_ABSENT_${bit(!inReview.originalWindowPresent)}")
        reportCounts("review_counts", before, counts())
        assertNoWrite(before)

        val preCancel = discoverReviewOwner() ?: throw AssertionError("OWN_REVIEW_OWNER_LOST_BEFORE_CANCEL")
        report("pre_cancel", "WINDOW_${bit(preCancel.windowId == review.windowId)}_" +
            "ROOT_${bit(preCancel.root == review.root)}_NODE_${bit(preCancel.node == review.node)}_" +
            observeOriginal(original).code())
        assertTrue("OWN_REVIEW_OWNER_CHANGED_BEFORE_CANCEL",
            preCancel.windowId == review.windowId && preCancel.root == review.root &&
                preCancel.node == review.node)
        val cancelAt = SystemClock.elapsedRealtime()
        clickOwnReviewCancel()
        cancelClicked = true
        val returned = awaitOrNull(8_000) {
            sampleStage("RETURN", original)
            uniqueFocusedApp(fixture)?.takeIf { it.id == original.windowId }
                ?: uniqueFocusedApp(fixture)
        }
        val returnAt = SystemClock.elapsedRealtime()
        val firstReturn = observeOriginal(original)
        report("return", firstReturn.code())
        SystemClock.sleep(300)
        sampleStage("SETTLED", original)
        val settled = observeOriginal(original)
        report("settled", settled.code())
        val end = await("CANCEL_DID_NOT_CLEAR_DRAFT") { counts().takeIf {
            it.phase == "NONE" && !it.cleanup
        } }
        sessionStarted = false
        assertNoWrite(before)
        assertTrue("SWIFTKEY_CHANGED", selectedSwiftKey())
        if (returned != null) assertTrue("SWIFTKEY_NOT_VISIBLE_AFTER_RETURN", imeVisible())
        val classification = when {
            returned == null -> "UNAVAILABLE"
            firstReturn.stable() && settled.stable() -> "STABLE"
            else -> "CHANGED"
        }
        report("outcome", "RETURN_${classification}_FOCUSED_${bit(returned != null)}_" +
            "MODE_AT_${elapsed(modeAt, startMs)}_STOP_AT_${elapsed(stopAt, startMs)}_" +
            "REVIEW_OBSERVED_AT_${elapsed(reviewAt, startMs)}_" +
            "STOP_TO_REVIEW_OBSERVED_${elapsed(reviewAt, stopAt)}_" +
            "MODE_TO_REVIEW_OBSERVED_${elapsed(reviewAt, modeAt)}_" +
            "CANCEL_TO_RETURN_${if (returned == null) -1 else elapsed(returnAt, cancelAt)}")
        report("original_events", events.code(reviewAt))
        report("stages", "OVERFLOW_${bit(stageOverflow)}_" +
            if (stageCodes.isEmpty()) "NONE" else stageCodes.joinToString("|"))
        reportCounts("final_counts", before, end)
    }

    private fun captureOriginal(): Original {
        val window = uniqueFocusedApp(fixture) ?: throw AssertionError("OWN_FIXTURE_WINDOW_NOT_FOCUSED")
        assertTrue("WRONG_FIXTURE_DISPLAY", window.displayId == Display.DEFAULT_DISPLAY)
        val root = window.getRoot(0) ?: throw AssertionError("OWN_FIXTURE_ROOT_MISSING")
        val node = knownNode(root, fixture, "local_test_editor")
            ?: throw AssertionError("OWN_FIXTURE_EDITOR_MISSING")
        assertTrue("OWN_FIXTURE_EDITOR_NOT_FOCUSED", node.isFocused && node.isEditable && node.refresh())
        var activity: SmokeEditorActivity? = null
        var editor: EditText? = null
        var token: IBinder? = null
        instrumentation.runOnMainSync {
            activity = SmokeEditorActivity.current.get()
            editor = activity?.findViewById(R.id.local_test_editor)
            token = editor?.windowToken
        }
        val ownedActivity = activity ?: throw AssertionError("OWN_FIXTURE_ACTIVITY_MISSING")
        val ownedEditor = editor ?: throw AssertionError("OWN_FIXTURE_VIEW_MISSING")
        val ownedToken = token ?: throw AssertionError("OWN_FIXTURE_WINDOW_TOKEN_MISSING")
        return Original(window.id, root, node, ownedActivity, ownedEditor, ownedToken,
            node.viewIdResourceName, node.className?.toString(), node.uniqueId, node.inputType,
            SmokeEditorActivity.createCount, SmokeEditorActivity.attachCount,
            SmokeEditorActivity.detachCount)
    }

    private fun observeOriginal(original: Original): Identity {
        val window = automation().windows.firstOrNull { it.id == original.windowId }
        val root = window?.getRoot(0)?.takeIf { it.packageName?.toString() == fixture }
        val node = root?.let { knownNode(it, fixture, "local_test_editor") }
        val refreshed = node?.refresh() == true
        var activitySame = false
        var editorSame = false
        var tokenSame = false
        var attached = false
        var empty = false
        var caretZero = false
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
            val editor = activity?.findViewById<EditText>(R.id.local_test_editor)
            activitySame = activity === original.activity
            editorSame = editor === original.editor
            tokenSame = editor?.let {
                it.windowToken != null && it.windowToken == original.token
            } == true
            attached = editor?.isAttachedToWindow == true
            empty = editor?.text?.isEmpty() == true
            caretZero = editor?.let { it.selectionStart == 0 && it.selectionEnd == 0 } == true
        }
        val uniqueRole = when {
            node == null -> "UNAVAILABLE"
            original.uniqueId == null && node.uniqueId == null -> "BOTH_MISSING"
            original.uniqueId == node.uniqueId -> "SAME"
            else -> "CHANGED"
        }
        val caretRole = when {
            node == null -> "UNAVAILABLE"
            node.textSelectionStart == 0 && node.textSelectionEnd == 0 -> "EXACT"
            node.textSelectionStart == -1 && node.textSelectionEnd == -1 &&
                editorSame && empty && caretZero -> "EMPTY_UNKNOWN"
            else -> "MISMATCH"
        }
        return Identity(window != null, window?.isFocused == true, root == original.root,
            node == original.node, refreshed, node?.viewIdResourceName == original.resource,
            node?.className?.toString() == original.className, uniqueRole,
            node?.inputType == original.inputType,
            node?.let { it.isShowingHintText || it.text.isNullOrEmpty() } == true,
            caretRole, activitySame, editorSame, tokenSame, attached, empty, caretZero,
            boundedDelta(SmokeEditorActivity.createCount, original.creates),
            boundedDelta(SmokeEditorActivity.attachCount, original.attaches),
            boundedDelta(SmokeEditorActivity.detachCount, original.detaches))
    }

    private fun discoverReviewOwner(): ReviewOwner? {
        val window = uniqueFocusedApp(product) ?: return null
        if (window.displayId != Display.DEFAULT_DISPLAY) throw AssertionError("OWN_REVIEW_WRONG_DISPLAY")
        val root = window.getRoot(0) ?: return null
        val editor = knownNode(root, product, "review_english") ?: return null
        if (!editor.isFocused || !editor.isEditable) return null
        return ReviewOwner(window.id, root, editor)
    }

    private fun knownNode(root: AccessibilityNodeInfo, owner: String, name: String): AccessibilityNodeInfo? {
        if (root.packageName?.toString() != owner) return null
        val matches = root.findAccessibilityNodeInfosByViewId("$owner:id/$name")
            .filter { it.packageName?.toString() == owner && it.viewIdResourceName == "$owner:id/$name" }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_NODE_$name")
        return matches.singleOrNull()
    }

    private fun uniqueFocusedApp(owner: String): AccessibilityWindowInfo? {
        val focused = automation().windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        if (focused.size > 1) throw AssertionError("MULTIPLE_FOCUSED_APPLICATIONS")
        val window = focused.singleOrNull() ?: return null
        val packageName = window.getRoot(0)?.packageName?.toString() ?: return null
        if (packageName != fixture && packageName != product)
            failKnownForeignFocus()
        return window.takeIf { packageName == owner }
    }

    /** Initial launch may briefly leave another app focused; inspect only its root role. */
    private fun initialFixtureWindow(): AccessibilityWindowInfo? {
        val focused = automation().windows.filter {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val window = focused.singleOrNull() ?: return null
        if (window.displayId != Display.DEFAULT_DISPLAY) return null
        return window.takeIf { it.getRoot(0)?.packageName?.toString() == fixture }
    }

    private fun focusedRole(): String {
        val focused = automation().windows.filter { it.isFocused }
        val apps = focused.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        if (apps.size > 1) throw AssertionError("MULTIPLE_FOCUSED_APPLICATIONS")
        val app = apps.singleOrNull()
        if (app != null) {
            if (app.displayId != Display.DEFAULT_DISPLAY) throw AssertionError("FOREIGN_FOCUSED_DISPLAY")
            return when (app.getRoot(0)?.packageName?.toString()) {
                fixture -> "FIXTURE"
                product -> "PRODUCT"
                null -> "ROOT_UNAVAILABLE"
                else -> failKnownForeignFocus()
            }
        }
        if (focused.any { it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY })
            throw AssertionError("FOREIGN_FOCUSED_WINDOW_TYPE")
        return "NONE"
    }

    private fun failKnownForeignFocus(): Nothing {
        report("focus_interrupt", "KNOWN_FOREIGN_APPLICATION")
        throw AssertionError("FOREIGN_FOCUSED_APPLICATION")
    }

    private fun sampleStage(stage: String, original: Original) {
        val role = focusedRole()
        val originalPresent = automation().windows.any { it.id == original.windowId }
        val signature = "${stage}_${role}_ORIGINAL_${bit(originalPresent)}"
        if (lastStageSignature == signature) return
        lastStageSignature = signature
        if (stageCodes.size < 24) stageCodes.add("${signature}_AT_${elapsed(SystemClock.elapsedRealtime(), stageStartMs)}")
        else stageOverflow = true
    }

    private fun launchFixture() {
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
            putExtra(SmokeEditorActivity.EXTRA_COMPOSED_FIXTURES, false)
        })
        await("OWN_FIXTURE_NOT_AVAILABLE") {
            val window = initialFixtureWindow() ?: return@await null
            val root = window.getRoot(0) ?: return@await null
            knownNode(root, fixture, "local_test_editor")?.takeIf {
                window.displayId == Display.DEFAULT_DISPLAY && it.isVisibleToUser &&
                    knownNode(root, fixture, "local_protected_editor") == null
            }
        }
    }

    private fun seedEmptyFixture() {
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("OWN_FIXTURE_ACTIVITY_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            editor.setText("")
            editor.setSelection(0)
            editor.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        await("OWN_EMPTY_EDITOR_NOT_READY") {
            val window = uniqueFocusedApp(fixture) ?: return@await null
            val root = window.getRoot(0) ?: return@await null
            val node = knownNode(root, fixture, "local_test_editor") ?: return@await null
            if (!node.refresh() || !node.isFocused || !node.isEditable) return@await null
            var ownedReady = false
            instrumentation.runOnMainSync {
                val activity = SmokeEditorActivity.current.get()
                val editor = activity?.findViewById<EditText>(R.id.local_test_editor)
                ownedReady = editor?.let {
                    it.isFocused && it.text.isEmpty() && it.selectionStart == 0 &&
                        it.selectionEnd == 0
                } == true
            }
            val nodeCaret = (node.textSelectionStart == 0 && node.textSelectionEnd == 0) ||
                (node.textSelectionStart == -1 && node.textSelectionEnd == -1 && ownedReady)
            node.takeIf { ownedReady && nodeCaret &&
                (node.isShowingHintText || node.text.isNullOrEmpty()) && imeVisible() && selectedSwiftKey() }
        }
        SystemClock.sleep(300)
    }

    private fun ownReviewControl(name: String): AccessibilityNodeInfo? {
        require(name == "review_cancel")
        val window = uniqueFocusedApp(product) ?: return null
        val root = window.getRoot(0) ?: return null
        return knownNode(root, product, name)?.takeIf {
            it.isVisibleToUser && it.isEnabled && it.isClickable
        }
    }

    private fun clickOwnReviewCancel() {
        val node = await("OWN_REVIEW_CANCEL_NOT_AVAILABLE") { ownReviewControl("review_cancel") }
        assertTrue("OWN_REVIEW_CANCEL_CLICK_FAILED", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in listOf("Ko", "Close", "요", "Stop", "Cancel"))
        val matches = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != product) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node.packageName?.toString() != product) continue
                if (node.className?.toString() == "android.widget.Button" &&
                    node.text?.toString()?.equals(label, ignoreCase = true) == true &&
                    node.isVisibleToUser && node.isEnabled && node.isClickable) matches.add(node)
                for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
            }
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_BUTTON_$label")
        return matches.singleOrNull()
    }

    private fun clickOwnButton(label: String) {
        val node = await("OWN_BUTTON_NOT_AVAILABLE_$label") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED_$label", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun counts(): Counts {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(150).map(String::trim).toList()
        }
        fun field(label: String): String = lines.firstOrNull { it.startsWith("$label: ") }
            ?.substringAfter(": ")?.trim()?.takeIf { Regex("[A-Za-z0-9_]+").matches(it) }
            ?: throw AssertionError("OWN_DIAGNOSTIC_UNAVAILABLE_$label")
        fun count(label: String): Int = field(label).toIntOrNull()
            ?.takeIf { it in 0..100_000 } ?: throw AssertionError("BAD_OWN_COUNT_$label")
        fun boolean(label: String): Boolean = when (field(label)) {
            "true" -> true
            "false" -> false
            else -> throw AssertionError("BAD_OWN_FLAG_$label")
        }
        return Counts(field("Experiment arm"), boolean("Auto insert enabled"),
            boolean("Speech input enabled"), boolean("Translation input enabled"),
            field("Draft phase"), boolean("Overlay cleanup pending"),
            count("Unbound recording proof count"), count("Destination capture count"),
            count("Commit request count"), count("Original intent captured count"),
            count("Auto insert attempt count"), count("Auto insert fallback count"),
            field("Original intent phase"), field("Original intent last reason"))
    }

    private fun assertNoWrite(before: Counts) {
        val current = counts()
        assertTrue("UNEXPECTED_DESTINATION_CAPTURE_OR_COMMIT",
            current.captures == before.captures && current.commits == before.commits &&
                current.autoAttempts == before.autoAttempts)
    }

    private fun reportCounts(label: String, before: Counts, now: Counts) {
        report(label, "PROOF_${boundedDelta(now.proofs, before.proofs)}_" +
            "CAPTURE_${boundedDelta(now.captures, before.captures)}_" +
            "COMMIT_${boundedDelta(now.commits, before.commits)}_" +
            "INTENT_CAPTURE_${boundedDelta(now.autoCaptures, before.autoCaptures)}_" +
            "AUTO_ATTEMPT_${boundedDelta(now.autoAttempts, before.autoAttempts)}_" +
            "AUTO_FALLBACK_${boundedDelta(now.autoFallbacks, before.autoFallbacks)}_" +
            "INTENT_PHASE_${now.intentPhase}_REASON_${now.intentReason}_" +
            "DRAFT_${now.phase}_CLEANUP_${bit(now.cleanup)}")
    }

    private fun report(label: String, code: String) {
        assertTrue("UNSAFE_LIFECYCLE_DIAGNOSTIC", code.length <= 1800 &&
            Regex("[A-Za-z0-9_|-]+").matches(code))
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_auto_lifecycle_$label", code)
        })
    }

    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun selectedSwiftKey(): Boolean = Settings.Secure.getString(
        instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey

    private fun <T : Any> await(code: String, timeoutMs: Long = 8_000, probe: () -> T?): T =
        awaitOrNull(timeoutMs, probe) ?: throw AssertionError(code)

    private fun <T : Any> awaitOrNull(timeoutMs: Long, probe: () -> T?): T? {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(50)
        }
        return null
    }

    companion object {
        private fun bit(value: Boolean): Int = if (value) 1 else 0
        private fun boundedDelta(after: Int, before: Int): Int = (after - before).coerceIn(-99, 99)
        private fun elapsed(at: Long, start: Long): Long = (at - start).coerceIn(0L, 180_000L)
        private fun signedDelta(at: Long, reference: Long): String = when {
            at < reference -> "BEFORE_${elapsed(reference, at)}"
            at > reference -> "AFTER_${elapsed(at, reference)}"
            else -> "AT_0"
        }
    }
}
