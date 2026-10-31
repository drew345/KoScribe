package com.koscribe.smoke

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** Icon-only, non-recording check in the owned empty fixture. */
@RunWith(AndroidJUnit4::class)
class FloatingIconSmokeTest {
    private val product = "com.koscribe.prototype"
    private val fixture = "com.koscribe.smoke"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"
    private lateinit var instrumentation: Instrumentation

    private data class State(val phase: String, val proofs: Int, val captures: Int,
        val commits: Int, val auto: Boolean)

    private fun automation(): UiAutomation = instrumentation.getUiAutomation(
        UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        ?: throw AssertionError("UI_AUTOMATION_UNAVAILABLE")

    @Before fun requireExactOptIn() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("ICON_TEST_REQUIRES_EXACT_OPT_IN", args.getString("iconReady") == "true" &&
            args.keySet().none { it != "iconReady" && it.endsWith("Ready") && args.getString(it) == "true" })
        assertTrue("WRONG_FIXTURE_PACKAGE", instrumentation.context.packageName == fixture)
        assertTrue("SWIFTKEY_NOT_SELECTED", Settings.Secure.getString(
            instrumentation.context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) == swiftKey)
        val before = state()
        assertTrue("AUTO_INSERT_MUST_BE_DISABLED", !before.auto)
        assertTrue("PREEXISTING_DRAFT", before.phase == "NONE")
        val ui = automation()
        val info = ui.serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        ui.serviceInfo = info
        instrumentation.context.startActivity(Intent(instrumentation.context,
            SmokeEditorActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(SmokeEditorActivity.EXTRA_OMIT_PROTECTED_EDITOR, true)
        })
        await("FIXTURE_NOT_FOREGROUND") { focusedFixture().takeIf { it } }
        instrumentation.runOnMainSync {
            val activity = SmokeEditorActivity.current.get() ?: throw AssertionError("FIXTURE_MISSING")
            val editor = activity.findViewById<EditText>(R.id.local_test_editor)
            editor.setText("")
            editor.setSelection(0)
            editor.requestFocus()
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
        await("EMPTY_FIXTURE_OR_SWIFTKEY_NOT_READY") {
            fixtureEditor()?.takeIf { it.isFocused && it.isVisibleToUser &&
                (it.isShowingHintText || it.text.isNullOrEmpty()) && imeVisible() }
        }
    }

    @Test fun floatingIconOpensClosesAndDragsWithoutRecording() {
        val before = state()
        val original = await("ICON_NOT_VISIBLE") { bubble() }
        val originalRect = bounds(original)
        assertHitbox(original, originalRect)
        assertTrue("ICON_LABEL_NOT_EMPTY", original.text.isNullOrEmpty())
        val resource = renderProductDrawable()
        assertOwnedReady()
        assertTrue("ICON_CLICK_FAILED", original.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("MODE_CHOICES_NOT_VISIBLE") { ownButton("Close")?.takeIf {
            ownButton("요") != null && ownButton("반말") != null && ownButton("EN") != null
        } }
        assertUnchanged(before)
        assertTrue("MODE_CLOSE_FAILED", ownButton("Close")!!
            .performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("ICON_NOT_RECOLLAPSED") { bubble()?.takeIf { ownButton("Close") == null } }
        assertUnchanged(before)

        val start = bounds(bubble() ?: throw AssertionError("ICON_MISSING_BEFORE_DRAG"))
        val density = instrumentation.context.resources.displayMetrics.density
        val width = instrumentation.context.resources.displayMetrics.widthPixels
        val delta = (48 * density).roundToInt() * if (start.centerX() > width / 2) -1 else 1
        assertOwnedReady()
        drag(start.centerX(), start.centerY(), start.centerX() + delta, start.centerY())
        val moved = await("ICON_DID_NOT_MOVE") { bubble()?.let(::bounds)?.takeIf {
            abs(it.centerX() - start.centerX()) >= (12 * density).roundToInt()
        } }
        assertHitbox(bubble()!!, moved)
        assertUnchanged(before)
        SystemClock.sleep(300)
        val settled = bounds(bubble() ?: throw AssertionError("ICON_DISAPPEARED_AFTER_DRAG"))
        assertTrue("ICON_DRAG_DID_NOT_SETTLE", abs(settled.centerX() - start.centerX()) >=
            (12 * density).roundToInt())
        assertOwnedReady()
        drag(settled.centerX(), settled.centerY(), start.centerX(), start.centerY())
        val restored = await("ICON_POSITION_NOT_RESTORED") { bubble()?.let(::bounds)?.takeIf {
            abs(it.centerX() - start.centerX()) <= (10 * density).roundToInt() &&
                abs(it.centerY() - start.centerY()) <= (10 * density).roundToInt()
        } }
        assertHitbox(bubble()!!, restored)
        assertUnchanged(before)
        instrumentation.sendStatus(0, Bundle().apply {
            putString("koscribe_floating_icon_resource", resource.absolutePath)
            putString("koscribe_floating_icon_bounds_dp", "56x48")
            putString("koscribe_floating_icon_result", "PASS_OPEN_CLOSE_DRAG_RESTORE_NO_WORK")
        })
    }

    private fun assertHitbox(node: AccessibilityNodeInfo, rect: Rect) {
        val density = instrumentation.context.resources.displayMetrics.density
        assertTrue("ICON_NOT_BUTTON", node.className?.toString() == "android.widget.Button")
        assertTrue("ICON_NOT_ACTIONABLE", node.isVisibleToUser && node.isEnabled && node.isClickable)
        assertTrue("ICON_DESCRIPTION_CHANGED", node.contentDescription?.toString() == "Open KoScribe")
        assertTrue("ICON_HITBOX_TOO_SMALL", rect.width() >= (48 * density).roundToInt() &&
            rect.height() >= (48 * density).roundToInt())
    }

    private fun renderProductDrawable(): File {
        val context = instrumentation.context.createPackageContext(product, 0)
        val id = context.resources.getIdentifier("collapsed_bubble", "drawable", product)
        assertTrue("PRODUCT_ICON_RESOURCE_MISSING", id != 0)
        val drawable = context.getDrawable(id) ?: throw AssertionError("PRODUCT_ICON_DRAWABLE_MISSING")
        val density = context.resources.displayMetrics.density
        val w = (56 * density).roundToInt()
        val h = (48 * density).roundToInt()
        val bitmap = Bitmap.createBitmap(w * 4, h * 4, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(4f, 4f)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            val file = File(instrumentation.context.filesDir, "floating_icon_resource.png")
            file.outputStream().use { assertTrue("RESOURCE_PNG_WRITE_FAILED",
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            return file
        } finally { bitmap.recycle() }
    }

    private fun assertOwnedReady() {
        assertTrue("FIXTURE_FOCUS_OR_IME_LOST", focusedFixture() && imeVisible() &&
            fixtureEditor()?.isFocused == true)
    }

    private fun assertUnchanged(before: State) {
        assertOwnedReady()
        val now = state()
        assertTrue("ICON_ACTION_STARTED_WORK", now.phase == "NONE" && now.proofs == before.proofs &&
            now.captures == before.captures && now.commits == before.commits && !now.auto)
        assertTrue("OWN_EDITOR_CHANGED", fixtureEditor()?.let {
            it.isShowingHintText || it.text.isNullOrEmpty()
        } == true)
    }

    private fun state(): State {
        val descriptor = automation().executeShellCommand(
            "dumpsys activity service com.koscribe.prototype/.KoScribeService")
        val lines = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
            it.lineSequence().take(128).map(String::trim).toList()
        }
        fun field(label: String): String = lines.firstOrNull { it.startsWith("$label: ") }
            ?.substringAfter(": ")?.trim()?.takeIf { Regex("[A-Za-z0-9_]+").matches(it) }
            ?: throw AssertionError("PRODUCT_DIAGNOSTIC_UNAVAILABLE_$label")
        return State(field("Draft phase"), field("Unbound recording proof count").toInt(),
            field("Destination capture count").toInt(), field("Commit request count").toInt(),
            field("Auto insert enabled") == "true")
    }

    private fun bubble(): AccessibilityNodeInfo? {
        val matches = nodes(product).filter { it.viewIdResourceName == "$product:id/collapsed_bubble" &&
            it.className?.toString() == "android.widget.Button" && it.isVisibleToUser }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_PRODUCT_BUBBLE")
        return matches.singleOrNull()
    }

    private fun ownButton(label: String): AccessibilityNodeInfo? {
        require(label in listOf("Close", "요", "반말", "EN"))
        val matches = nodes(product).filter { it.className?.toString() == "android.widget.Button" &&
            it.isVisibleToUser && it.isEnabled && it.isClickable &&
            matchesOwnButtonLabel(it.text, label) }
        if (matches.size > 1) throw AssertionError("AMBIGUOUS_PRODUCT_CONTROL_$label")
        return matches.singleOrNull()
    }

    private fun fixtureEditor(): AccessibilityNodeInfo? = nodes(fixture).filter {
        it.viewIdResourceName == "$fixture:id/local_test_editor" && it.isEditable
    }.let { if (it.size > 1) throw AssertionError("AMBIGUOUS_FIXTURE_EDITOR") else it.singleOrNull() }

    private fun nodes(pkg: String): List<AccessibilityNodeInfo> {
        require(pkg == product || pkg == fixture)
        val result = ArrayList<AccessibilityNodeInfo>()
        for (window in automation().windows) {
            val root = window.getRoot(0) ?: continue
            if (root.packageName?.toString() != pkg) continue
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                if (node.packageName?.toString() != pkg) continue
                result.add(node)
                for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
            }
        }
        return result
    }

    private fun focusedFixture(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused &&
            it.getRoot(0)?.packageName?.toString() == fixture
    }

    private fun imeVisible(): Boolean = automation().windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun bounds(node: AccessibilityNodeInfo): Rect = Rect().also(node::getBoundsInScreen)

    private fun drag(x0: Int, y0: Int, x1: Int, y1: Int) {
        val down = SystemClock.uptimeMillis()
        fun inject(action: Int, x: Float, y: Float, offset: Long) {
            val event = MotionEvent.obtain(down, down + offset, action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            try { assertTrue("OWN_ICON_DRAG_INJECTION_FAILED", automation().injectInputEvent(event, true)) }
            finally { event.recycle() }
        }
        inject(MotionEvent.ACTION_DOWN, x0.toFloat(), y0.toFloat(), 0)
        for (step in 1..5) {
            SystemClock.sleep(18)
            inject(MotionEvent.ACTION_MOVE, x0 + (x1 - x0) * step / 5f,
                y0 + (y1 - y0) * step / 5f, step * 18L)
        }
        SystemClock.sleep(20)
        inject(MotionEvent.ACTION_UP, x1.toFloat(), y1.toFloat(), 110)
    }

    private fun <T : Any> await(code: String, probe: () -> T?): T {
        val end = SystemClock.elapsedRealtime() + 8_000
        while (SystemClock.elapsedRealtime() < end) {
            probe()?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError(code)
    }
}
