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
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
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

/** Explicitly selected synthetic fixture checks; never opens a real chat. */
@RunWith(AndroidJUnit4::class)
class BubbleLifecycleSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private lateinit var instrumentation: Instrumentation
    private var micAttempted = false
    private var preCleanupFailureSnapshot: String? = null

    @get:Rule val ownUiFailureSnapshot: TestWatcher = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            // The timeout path stores its snapshot before @After can cancel a panel.
            // Only fixed own-product labels, booleans and screen bounds are emitted.
            val state = preCleanupFailureSnapshot ?: runCatching {
                if (::instrumentation.isInitialized) ownControlSnapshot() else "NOT_INITIALIZED"
            }.getOrDefault("OWN_SNAPSHOT_UNAVAILABLE")
            System.err.println("BUBBLE_OWN_UI_FAILURE ${if (preCleanupFailureSnapshot != null) "PRE_CLEANUP" else "POST_CLEANUP"} $state")
        }
    }

    private fun automation(): UiAutomation =
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactSyntheticScope() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val idle = args.getString("bubbleIdleReady") == "true"
        val mic = args.getString("bubbleMicReady") == "true"
        val restore = args.getString("bubbleRestoreReady") == "true"
        val other = listOf("externalDraftReady", "externalRecordCancelReady", "startupHoldReady",
            "startupCollapseReady", "startupPanelReady", "overlayReady", "reselectReady",
            "reviewCopyReady", "reviewInsertEmptyReady", "reviewInsertMiddleReady",
            "reviewKoreanReady", "reviewSelectionReady", "reviewKoreanEditReady",
            "reviewUncertainReady", "reviewConfirmationProbeReady", "autofillSaveNoReady")
            .any { args.getString(it) == "true" }
        assumeTrue("BUBBLE_TEST_REQUIRES_ONE_EXACT_OPT_IN",
            listOf(idle, mic, restore).count { it } == 1 && !other)
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        assertTrue("SWIFTKEY_NOT_SELECTED", Settings.Secure.getString(
            instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey)
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
        await("FIXTURE_NOT_FOREGROUND") {
            automation().windows.firstOrNull {
                it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
                    it.getRoot(0)?.packageName?.toString() == fixture
            }
        }
    }

    @After fun ownFixtureCleanupOnly() {
        if (!::instrumentation.isInitialized) return
        if (micAttempted) runCatching {
            ownButton("Cancel")?.let { clickOwn("Cancel") }
        }
        // No global Back, other app launch, keyboard selection, or target text change.
    }

    @Test fun idleBubbleTracksImeHideCycleDragAndProtectedField() {
        assumeTrue("IDLE_METHOD_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("bubbleIdleReady") == "true")
        instrumentation.context.getSharedPreferences("bubble_smoke", Context.MODE_PRIVATE)
            .edit().remove("drag_saved").apply()
        assertTrue("WRONG_PRODUCT_ARM", serviceField("Experiment arm") == "RESELECT")
        focusFixture(protected = false, showIme = false)
        await("IME_DID_NOT_HIDE") { (!imeVisible()).takeIf { it } }
        await("BUBBLE_VISIBLE_WITHOUT_IME") { (!visibleOwnUi()).takeIf { it } }

        focusFixture(protected = false, showIme = true)
        await("IME_NOT_VISIBLE") { imeVisible().takeIf { it } }
        await("BUBBLE_NOT_VISIBLE") { ownButton("Ko") }
        clickOwn("Ko")
        clickOwn("Hide")
        await("EXPLICIT_HIDE_FAILED") { (!visibleOwnUi()).takeIf { it } }
        assertTrue("IME_CHANGED_BY_HIDE", imeVisible())
        SystemClock.sleep(400)
        assertTrue("HIDE_LATCH_RELEASED_WITH_IME_VISIBLE", !visibleOwnUi() && imeVisible())
        focusFixture(protected = false, showIme = false)
        await("IME_CYCLE_ABSENT_NOT_OBSERVED") { (!imeVisible()).takeIf { it } }
        focusFixture(protected = false, showIme = true)
        await("BUBBLE_DID_NOT_REAPPEAR") { ownButton("Ko") }

        val before = buttonRect("Ko")
        dragOwnKo(before)
        await("BUBBLE_DRAG_DID_NOT_MOVE") {
            ownButton("Ko")?.let { buttonRect("Ko") }?.takeIf {
                kotlin.math.abs(it.centerX() - before.centerX()) > 30
            }
        }
        SystemClock.sleep(500) // Let the completed drag settle before testing persistence.
        val dragged = buttonRect("Ko")
        assertTrue("DRAG_EXPANDED_BUBBLE", ownButton("Close") == null)
        focusFixture(protected = false, showIme = false)
        await("BUBBLE_NOT_HIDDEN_AFTER_IME_HIDE") { (!visibleOwnUi()).takeIf { it } }
        focusFixture(protected = false, showIme = true)
        val restored = await("BUBBLE_POSITION_NOT_RESTORED") { ownButton("Ko")?.let { buttonRect("Ko") } }
        assertTrue("SAVED_DRAG_POSITION_CHANGED",
            kotlin.math.abs(restored.centerX() - dragged.centerX()) <= 24 &&
                kotlin.math.abs(restored.centerY() - dragged.centerY()) <= 24)
        focusFixture(protected = true, showIme = true)
        await("PROTECTED_FIELD_NOT_FOCUSED") {
            fixtureNode(R.id.local_protected_editor)?.takeIf { it.isFocused }
        }
        await("BUBBLE_VISIBLE_ON_PROTECTED_FIELD") { (!visibleOwnUi()).takeIf { it } }
        assertTrue("IME_NOT_VISIBLE_ON_PROTECTED_FIELD", imeVisible())
        instrumentation.context.getSharedPreferences("bubble_smoke", Context.MODE_PRIVATE)
            .edit().putInt("drag_x", restored.centerX()).putInt("drag_y", restored.centerY())
            .putBoolean("drag_saved", true).apply()
    }

    @Test fun savedPositionRestoresAfterProductServiceRestart() {
        assumeTrue("RESTORE_METHOD_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("bubbleRestoreReady") == "true")
        assertTrue("WRONG_PRODUCT_ARM", serviceField("Experiment arm") == "RESELECT")
        val saved = instrumentation.context.getSharedPreferences("bubble_smoke", Context.MODE_PRIVATE)
        assertTrue("NO_PRIOR_COMPLETED_DRAG", saved.getBoolean("drag_saved", false))
        val oldX = saved.getInt("drag_x", Int.MIN_VALUE)
        val oldY = saved.getInt("drag_y", Int.MIN_VALUE)
        assertTrue("SAVED_POSITION_UNAVAILABLE", oldX != Int.MIN_VALUE && oldY != Int.MIN_VALUE)
        focusFixture(protected = false, showIme = false)
        await("IME_DID_NOT_HIDE") { (!imeVisible()).takeIf { it } }
        focusFixture(protected = false, showIme = true)
        await("IME_NOT_VISIBLE_FOR_RESTORE") { imeVisible().takeIf { it } }
        val restored = await("BUBBLE_NOT_RESTORED") { ownButton("Ko")?.let { buttonRect("Ko") } }
        assertTrue("POSITION_NOT_RESTORED_AFTER_SERVICE_RESTART",
            kotlin.math.abs(restored.centerX() - oldX) <= 24 &&
                kotlin.math.abs(restored.centerY() - oldY) <= 24)
        saved.edit().remove("drag_saved").apply()
    }

    @Test fun draggedHostStaysPinnedThroughSyntheticStopAndCancel() {
        assumeTrue("MIC_METHOD_REQUIRES_OWN_FLAG",
            InstrumentationRegistry.getArguments().getString("bubbleMicReady") == "true")
        assertTrue("WRONG_PRODUCT_ARM", serviceField("Experiment arm") == "RESELECT")
        assertTrue("MIC_PERMISSION_REQUIRED", instrumentation.context.packageManager.checkPermission(
            Manifest.permission.RECORD_AUDIO, product) == PackageManager.PERMISSION_GRANTED)
        focusFixture(protected = false, showIme = false)
        await("IME_DID_NOT_HIDE") { (!imeVisible()).takeIf { it } }
        focusFixture(protected = false, showIme = true)
        await("IME_NOT_VISIBLE_FOR_MIC_TEST") { imeVisible().takeIf { it } }
        await("BUBBLE_NOT_VISIBLE") { ownButton("Ko") }
        dragOwnKo(buttonRect("Ko"))
        SystemClock.sleep(500)
        clickOwn("Ko")
        val hostWindowId = await("EXPANDED_HOST_NOT_FOUND") { ownButton("Close") }.windowId
        val priorProofs = serviceField("Unbound recording proof count")?.toIntOrNull()
            ?: throw AssertionError("PROOF_COUNT_UNAVAILABLE")
        val priorCaptures = serviceField("Destination capture count")?.toIntOrNull()
            ?: throw AssertionError("CAPTURE_COUNT_UNAVAILABLE")
        micAttempted = true
        clickOwn("요")
        val stop = await("STOP_NOT_VISIBLE") { ownButton("Stop") }
        assertTrue("HOST_WINDOW_CHANGED_DURING_RECORDING", stop.windowId == hostWindowId)
        await("MIC_PROOF_NOT_OBSERVED") {
            (serviceField("Unbound recording proof count")?.toIntOrNull() == priorProofs + 1)
                .takeIf { it }
        }
        assertTrue("FIXTURE_LOST_FOCUS", fixtureNode(R.id.local_test_editor)?.isFocused == true)
        assertTrue("IME_LOST_DURING_RECORDING", imeVisible())
        assertTrue("TARGET_CAPTURED_DURING_RECORDING",
            serviceField("Destination capture count")?.toIntOrNull() == priorCaptures)
        focusFixture(protected = false, showIme = false)
        await("IME_DID_NOT_HIDE_DURING_RECORDING") { (!imeVisible()).takeIf { it } }
        assertTrue("HOST_NOT_PINNED_WHEN_IME_HIDDEN", ownButton("Stop")?.windowId == hostWindowId)
        focusFixture(protected = false, showIme = true)
        await("IME_DID_NOT_REAPPEAR_DURING_RECORDING") { imeVisible().takeIf { it } }
        assertTrue("HOST_NOT_PINNED_WHEN_IME_RETURNED", ownButton("Stop")?.windowId == hostWindowId)
        clickOwn("Stop")
        await("STOP_DID_NOT_FINISH") { (ownButton("Stop") == null).takeIf { it } }
        val cancel = await("REVIEW_CANCEL_NOT_VISIBLE") { ownButton("Cancel") }
        assertTrue("HOST_WINDOW_CHANGED_IN_REVIEW", cancel.windowId == hostWindowId)
        assertTrue("STOP_DID_NOT_REACH_READ_ONLY_REVIEW",
            serviceField("Experiment phase") == "RESELECT_READ_ONLY_REVIEW")
        assertTrue("MIC_CLEANUP_UNCERTAIN_AT_STOP",
            serviceField("Overlay cleanup pending") == "false")
        assertTrue("INSERT_OR_COPY_EXPOSED", nodes(product).none {
            it.className?.toString() == "android.widget.Button" &&
                (it.text?.toString()?.contains("Insert", ignoreCase = true) == true ||
                    it.text?.toString()?.contains("Copy", ignoreCase = true) == true)
        })
        clickOwn("Cancel")
        await("MIC_NOT_CANCELLED") { (serviceField("Experiment phase") == "RESELECT_CANCELLED").takeIf { it } }
        assertTrue("MIC_CLEANUP_UNCERTAIN", serviceField("Overlay cleanup pending") == "false")
        micAttempted = false
        assertTrue("FIXTURE_LOST_FOCUS_AFTER_CANCEL",
            fixtureNode(R.id.local_test_editor)?.isFocused == true)
        assertTrue("IME_LOST_AFTER_CANCEL", imeVisible())
    }

    private fun focusFixture(protected: Boolean, showIme: Boolean) {
        val id = if (protected) R.id.local_protected_editor else R.id.local_test_editor
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get()
                ?: throw AssertionError("FIXTURE_ACTIVITY_UNAVAILABLE")
            val editor = activity.findViewById<android.widget.EditText>(id)
            editor.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (showIme) imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
            else imm.hideSoftInputFromWindow(editor.windowToken, 0)
        }
        await("FIXTURE_FOCUS_NOT_READY") { fixtureNode(id)?.takeIf { it.isFocused } }
    }

    private fun dragOwnKo(rect: Rect) {
        val node = ownButton("Ko") ?: throw AssertionError("KO_NOT_FOUND_FOR_DRAG")
        assertTrue("KO_NOT_VISIBLE_FOR_DRAG", node.isVisibleToUser)
        val startX = rect.centerX().toFloat()
        val startY = rect.centerY().toFloat()
        val down = SystemClock.uptimeMillis()
        fun inject(action: Int, x: Float, y: Float, offset: Long) {
            val event = MotionEvent.obtain(down, down + offset, action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue("OWN_DRAG_INJECTION_FAILED", automation().injectInputEvent(event, true)) }
            finally { event.recycle() }
        }
        inject(MotionEvent.ACTION_DOWN, startX, startY, 0)
        for (step in 1..5) {
            SystemClock.sleep(18)
            inject(MotionEvent.ACTION_MOVE,
                startX - 24f * step, startY + 12f * step, step * 18L)
        }
        SystemClock.sleep(20)
        inject(MotionEvent.ACTION_UP, startX - 120f, startY + 60f, 110)
    }

    private fun clickOwn(label: String) {
        val node = await("OWN_BUTTON_NOT_FOUND") { ownButton(label) }
        assertTrue("OWN_BUTTON_CLICK_FAILED", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun buttonRect(label: String): Rect {
        val node = ownButton(label) ?: throw AssertionError("OWN_BUTTON_NOT_FOUND")
        return Rect().also(node::getBoundsInScreen)
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in listOf("Ko", "요", "반말", "EN", "Hide", "Close", "Stop", "Cancel")) {
            "UNKNOWN_OWN_CONTROL"
        }
        val matches = nodes(product).filter {
            it.className?.toString() == "android.widget.Button" &&
                it.isEnabled && it.isClickable && it.isVisibleToUser &&
                it.text?.toString()?.equals(label, ignoreCase = true) == true
        }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_OWN_BUTTON")
        return matches.singleOrNull()
    }

    private fun visibleOwnUi(): Boolean = nodes(product).any { it.isVisibleToUser }

    private fun fixtureNode(id: Int): AccessibilityNodeInfo? {
        val name = "$fixture:id/${if (id == R.id.local_test_editor) "local_test_editor" else "local_protected_editor"}"
        val matches = nodes(fixture).filter { it.viewIdResourceName == name &&
            it.className?.toString() == "android.widget.EditText" }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_FIELD")
        return matches.singleOrNull()
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

    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun serviceField(label: String): String? = runCatching {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { reader ->
            reader.lineSequence().take(80).map(String::trim)
                .firstOrNull { it.startsWith("$label: ") }
                ?.substringAfter(": ")?.trim()?.takeIf { Regex("[A-Za-z0-9_]+").matches(it) }
        }
    }.getOrNull()

    private fun ownControlSnapshot(): String {
        val own = nodes(product)
        val labels = listOf("Ko", "요", "반말", "EN", "Hide", "Close", "Stop", "Cancel")
        val buttons = own.filter { it.className?.toString() == "android.widget.Button" }
        val buttonCodes = labels.joinToString("_") { label ->
            val matches = buttons.filter {
                it.text?.toString()?.equals(label, ignoreCase = true) == true
            }
            val flags = matches.take(2).joinToString("+") { node ->
                val bounds = Rect().also(node::getBoundsInScreen)
                "M${if (node.text?.toString() == label) "EXACT" else "CASE_VARIANT"}" +
                    "V${if (node.isVisibleToUser) 1 else 0}" +
                    "E${if (node.isEnabled) 1 else 0}" +
                    "C${if (node.isClickable) 1 else 0}" +
                    "B${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
            }
            "${label}_N${matches.size.coerceAtMost(9)}_${flags.ifEmpty { "NONE" }}"
        }
        val scrolls = own.filter { it.className?.toString() == "android.widget.ScrollView" }
        val scrollCodes = scrolls.take(2).joinToString("+") { node ->
            val bounds = Rect().also(node::getBoundsInScreen)
            "V${if (node.isVisibleToUser) 1 else 0}" +
                "S${if (node.isScrollable) 1 else 0}" +
                "B${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
        }
        return "BUTTONS_${buttonCodes}_SCROLL_N${scrolls.size.coerceAtMost(9)}_${scrollCodes.ifEmpty { "NONE" }}"
    }

    private fun <T : Any> await(code: String, timeoutMs: Long = 5_000, probe: () -> T?): T {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        preCleanupFailureSnapshot = runCatching { ownControlSnapshot() }
            .getOrDefault("OWN_SNAPSHOT_UNAVAILABLE")
        throw AssertionError("$code ${preCleanupFailureSnapshot}")
    }
}
