package com.koscribe.smoke

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.Spanned
import android.text.TextUtils
import android.text.style.UnderlineSpan
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowInsets
import android.view.WindowManager
import android.view.Display
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.SurroundingText
import android.view.inputmethod.TextAttribute
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.ref.WeakReference

/** One standard editor containing only synthetic fixture text; no send action. */
class SmokeEditorActivity : Activity() {
    companion object {
        const val EXTRA_OMIT_PROTECTED_EDITOR = "com.koscribe.smoke.OMIT_PROTECTED_EDITOR"
        const val EXTRA_MANUAL_FIXTURE = "com.koscribe.smoke.MANUAL_FIXTURE"
        const val EXTRA_COMPOSED_FIXTURES = "com.koscribe.smoke.COMPOSED_FIXTURES"
        const val EXTRA_REVIEW_SURFACE_PROBE = "com.koscribe.smoke.REVIEW_SURFACE_PROBE"
        const val EXTRA_STABLE_AUTO_FIXTURE = "com.koscribe.smoke.STABLE_AUTO_FIXTURE"
        const val EXTRA_MANUAL_RESULT_FIXTURE = "com.koscribe.smoke.MANUAL_RESULT_FIXTURE"
        const val EXTRA_RELATIVE_READ_FIXTURE = "com.koscribe.smoke.RELATIVE_READ_FIXTURE"
        const val COMPOSED_WRONG_RESULT = "Incorrect synthetic composed result."
        @Volatile
        var current = WeakReference<SmokeEditorActivity>(null)
            private set
        /** Fixed, content-free lifecycle codes for pre-cleanup fixture diagnostics. */
        @Volatile var lifecycleCode = "NOT_CREATED"
            private set
        @Volatile var windowFocused = false
            private set
        @Volatile var createCount = 0
            private set
        @Volatile var attachCount = 0
            private set
        @Volatile var detachCount = 0
            private set
        @Volatile var focusCallbackCount = 0
            private set
        @Volatile var postResumeHeartbeatCount = 0
            private set
        @Volatile var displayCode = "UNKNOWN"
            private set
    }

    private lateinit var fixtureRoot: LinearLayout
    private lateinit var heading: TextView
    private lateinit var messageEditor: EditText
    private var probeLogicalEpoch = 0

    /** Fixture-only oracle; the product cannot read this logical target marker. */
    fun probeLogicalEpoch(): Int = probeLogicalEpoch
    fun probeOriginalEditor(): EditText = messageEditor
    fun probeAlternateEditor(): EditText? = findViewById(R.id.local_probe_alternate_editor)
    fun stableAutoEditor(): EditText? = findViewById(R.id.stable_auto_editor)
    fun stableAutoAlternateEditor(): EditText? = findViewById(R.id.stable_auto_alternate_editor)
    fun manualResultTargetEditor(): EditText? = findViewById(R.id.local_result_target_editor)
    fun relativeReadEditor(): EditText? = findViewById(R.id.local_relative_read_editor)
    /** Immutable fixture-only draft ownership for this view's entire lifetime. */
    fun stableAutoDraftToken(): Any? =
        (stableAutoEditor() as? StableAutoEditor)?.logicalDraft

    fun stableAutoSelectIdenticalAlternate(): Boolean {
        if (!intent.getBooleanExtra(EXTRA_STABLE_AUTO_FIXTURE, false)) return false
        val original = stableAutoEditor() ?: return false
        val alternate = stableAutoAlternateEditor() ?: return false
        if (alternate.text.toString() != original.text.toString() ||
            alternate.selectionStart != original.selectionStart ||
            alternate.selectionEnd != original.selectionEnd) return false
        original.clearFocus()
        return alternate.requestFocus() && alternate.isFocused
    }

    fun probeSelectIdenticalAlternate(): Boolean {
        if (!intent.getBooleanExtra(EXTRA_REVIEW_SURFACE_PROBE, false)) return false
        val alternate = probeAlternateEditor() ?: return false
        if (alternate.text.toString() != messageEditor.text.toString() ||
            alternate.selectionStart != messageEditor.selectionStart ||
            alternate.selectionEnd != messageEditor.selectionEnd) return false
        messageEditor.clearFocus()
        return alternate.requestFocus() && alternate.isFocused
    }

    fun probeRebindSameView(): Boolean {
        if (!intent.getBooleanExtra(EXTRA_REVIEW_SURFACE_PROBE, false) ||
            !messageEditor.isAttachedToWindow) return false
        // A new fixture-only logical message reuses the exact view, text and caret.
        // No public editor restart is injected: indistinguishability is the point.
        probeLogicalEpoch++
        return true
    }

    private fun manualFixture(): Boolean = intent.getBooleanExtra(EXTRA_MANUAL_FIXTURE, false)
    private fun composedFixtures(): Boolean = !manualFixture() &&
        intent.getBooleanExtra(EXTRA_COMPOSED_FIXTURES, false)

    fun composedCommitCount(id: Int): Int =
        (findViewById<EditText>(id) as? ComposedCommitEditor)?.delegatedCommitCount ?: -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createCount++
        lifecycleCode = "CREATING"
        windowFocused = false
        displayCode = "UNKNOWN"
        // This window keeps the device awake only while the synthetic fixture is visible.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val density = resources.displayMetrics.density
        val margin = (20 * density + 0.5f).toInt()
        val manual = manualFixture()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            isFocusableInTouchMode = manual
            setPadding(margin, margin, margin, margin)
        }
        fixtureRoot = root
        heading = TextView(this).apply {
            text = if (manual) "Test message" else "KoScribe synthetic editor test"
            textSize = 18f
        }
        root.addView(heading)
        messageEditor = EditText(this).apply {
            id = R.id.local_test_editor
            hint = if (manual) "Tap here to choose this empty field" else "Synthetic text only"
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        root.addView(messageEditor, LinearLayout.LayoutParams(-1, (160 * density + 0.5f).toInt()))
        if (intent.getBooleanExtra(EXTRA_REVIEW_SURFACE_PROBE, false))
            root.addView(probeAlternateField(), LinearLayout.LayoutParams(-1, -2))
        if (intent.getBooleanExtra(EXTRA_STABLE_AUTO_FIXTURE, false)) {
            root.addView(stableAutoField(), LinearLayout.LayoutParams(-1, -2))
            root.addView(stableAutoAlternateField(), LinearLayout.LayoutParams(-1, -2))
        }
        if (intent.getBooleanExtra(EXTRA_MANUAL_RESULT_FIXTURE, false))
            root.addView(manualResultTargetField(), LinearLayout.LayoutParams(-1, -2))
        if (intent.getBooleanExtra(EXTRA_RELATIVE_READ_FIXTURE, false))
            root.addView(relativeReadField(), LinearLayout.LayoutParams(-1, -2))
        if (composedFixtures()) {
            root.addView(composedEditor(R.id.local_composed_match_editor, false),
                LinearLayout.LayoutParams(-1, -2))
            root.addView(composedEditor(R.id.local_composed_mismatch_editor, true),
                LinearLayout.LayoutParams(-1, -2))
        }
        if (!manual && !intent.getBooleanExtra(EXTRA_OMIT_PROTECTED_EDITOR, false))
            root.addView(protectedEditor(), LinearLayout.LayoutParams(-1, -2))
        if (!manual) root.addView(uncertainEditor(), LinearLayout.LayoutParams(-1, -2))
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val left = margin + bars.left
            val top = margin + bars.top
            val right = margin + bars.right
            val bottom = margin + bars.bottom
            if (view.paddingLeft != left || view.paddingTop != top ||
                view.paddingRight != right || view.paddingBottom != bottom)
                view.setPadding(left, top, right, bottom)
            insets
        }
        setContentView(root)
        if (manual) root.requestFocus()
        // Publish only after PhoneWindow has installed its decor/content container.
        current = WeakReference(this)
        lifecycleCode = "CONTENT_READY"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::fixtureRoot.isInitialized) syncFixtureMode()
    }

    private fun syncFixtureMode() {
        val manual = manualFixture()
        heading.text = if (manual) "Test message" else "KoScribe synthetic editor test"
        messageEditor.hint = if (manual) "Tap here to choose this empty field" else "Synthetic text only"
        fixtureRoot.isFocusableInTouchMode = manual
        val alternate = probeAlternateEditor()
        if (intent.getBooleanExtra(EXTRA_REVIEW_SURFACE_PROBE, false)) {
            if (alternate == null) fixtureRoot.addView(probeAlternateField(), 2,
                LinearLayout.LayoutParams(-1, -2))
        } else if (alternate != null) fixtureRoot.removeView(alternate)
        val stable = stableAutoEditor()
        val stableAlternate = stableAutoAlternateEditor()
        if (intent.getBooleanExtra(EXTRA_STABLE_AUTO_FIXTURE, false)) {
            if (stable == null) fixtureRoot.addView(stableAutoField(),
                LinearLayout.LayoutParams(-1, -2))
            if (stableAlternate == null) fixtureRoot.addView(stableAutoAlternateField(),
                LinearLayout.LayoutParams(-1, -2))
        } else {
            if (stableAlternate != null) fixtureRoot.removeView(stableAlternate)
            if (stable != null) fixtureRoot.removeView(stable)
        }
        val manualTarget = manualResultTargetEditor()
        if (intent.getBooleanExtra(EXTRA_MANUAL_RESULT_FIXTURE, false)) {
            if (manualTarget == null) fixtureRoot.addView(manualResultTargetField(),
                LinearLayout.LayoutParams(-1, -2))
        } else if (manualTarget != null) fixtureRoot.removeView(manualTarget)
        val relativeRead = relativeReadEditor()
        if (intent.getBooleanExtra(EXTRA_RELATIVE_READ_FIXTURE, false)) {
            if (relativeRead == null) fixtureRoot.addView(relativeReadField(),
                LinearLayout.LayoutParams(-1, -2))
        } else if (relativeRead != null) fixtureRoot.removeView(relativeRead)
        val protectedField = fixtureRoot.findViewById<EditText>(R.id.local_protected_editor)
        if (manual || intent.getBooleanExtra(EXTRA_OMIT_PROTECTED_EDITOR, false)) {
            if (protectedField != null) fixtureRoot.removeView(protectedField)
        } else if (protectedField == null) {
            fixtureRoot.addView(protectedEditor(), 2, LinearLayout.LayoutParams(-1, -2))
        }
        val uncertain = fixtureRoot.findViewById<EditText>(R.id.local_uncertain_editor)
        if (manual) {
            if (uncertain != null) fixtureRoot.removeView(uncertain)
            messageEditor.clearFocus()
            fixtureRoot.requestFocus()
        } else if (uncertain == null) {
            fixtureRoot.addView(uncertainEditor(), LinearLayout.LayoutParams(-1, -2))
        }
        for ((id, mismatch) in listOf(R.id.local_composed_match_editor to false,
            R.id.local_composed_mismatch_editor to true)) {
            val existing = fixtureRoot.findViewById<EditText>(id)
            if (!composedFixtures()) {
                if (existing != null) fixtureRoot.removeView(existing)
            } else if (existing == null) {
                fixtureRoot.addView(composedEditor(id, mismatch),
                    if (mismatch) 3 else 2, LinearLayout.LayoutParams(-1, -2))
            }
        }
    }

    private fun protectedEditor(): EditText = EditText(this).apply {
        id = R.id.local_protected_editor
        hint = "Synthetic protected field"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }

    private fun probeAlternateField(): EditText = EditText(this).apply {
        id = R.id.local_probe_alternate_editor
        hint = "Synthetic alternate editor"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }

    private fun stableAutoField(): EditText = StableAutoEditor(this).apply {
        id = R.id.stable_auto_editor
        hint = "Stable synthetic destination"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }

    private fun stableAutoAlternateField(): EditText = EditText(this).apply {
        id = R.id.stable_auto_alternate_editor
        hint = "Identical synthetic alternate"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }

    private fun manualResultTargetField(): EditText = EditText(this).apply {
        id = R.id.local_result_target_editor
        hint = "Synthetic manually chosen destination"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }

    private fun relativeReadField(): EditText = RelativeReadEditor(this).apply {
        id = R.id.local_relative_read_editor
        hint = "Synthetic relative read field"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
    }

    private class StableAutoEditor(context: Context) : EditText(context) {
        val logicalDraft = Any()
    }

    private fun uncertainEditor(): EditText = UncertainCommitEditor(this).apply {
        id = R.id.local_uncertain_editor
        hint = "Synthetic uncertain commit field"
        inputType = InputType.TYPE_CLASS_TEXT
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }

    private fun composedEditor(id: Int, mismatch: Boolean): EditText =
        ComposedCommitEditor(this, mismatch).apply {
            this.id = id
            hint = "Synthetic composed test field"
            inputType = InputType.TYPE_CLASS_TEXT
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }

    override fun onResume() {
        super.onResume()
        if (current.get() === this) {
            lifecycleCode = "RESUMED"
            displayCode = when (display?.displayId) {
                Display.DEFAULT_DISPLAY -> "DEFAULT"
                null -> "UNKNOWN"
                else -> "OTHER"
            }
            Handler(Looper.getMainLooper()).post {
                if (current.get() === this) postResumeHeartbeatCount++
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (current.get() === this) {
            attachCount++
            displayCode = when (display?.displayId) {
                Display.DEFAULT_DISPLAY -> "DEFAULT"
                null -> "UNKNOWN"
                else -> "OTHER"
            }
        }
    }

    override fun onDetachedFromWindow() {
        if (current.get() === this) detachCount++
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (current.get() === this) {
            focusCallbackCount++
            windowFocused = hasFocus
            lifecycleCode = if (hasFocus) "FOCUSED" else "UNFOCUSED"
        }
    }

    override fun onPause() {
        if (current.get() === this) {
            lifecycleCode = "PAUSED"
            windowFocused = false
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (current.get() === this) {
            lifecycleCode = "DESTROYED"
            windowFocused = false
            current = WeakReference(null)
        }
        super.onDestroy()
    }

}

/** Test-only served editor with deliberately incomplete public destination metadata. */
private class RelativeReadEditor(context: Context) : EditText(context) {
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        // Model the measured Karrot node flags while the actual view stays input focused.
        info.isFocused = false
        info.isEditable = false
        info.inputType = InputType.TYPE_NULL
        info.setTextSelection(-1, -1)
        info.viewIdResourceName = null
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val delegate = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(delegate, true) {
            // An unsupported selected-text read is possible even with a real selected range.
            override fun getSelectedText(flags: Int): CharSequence? = null

            override fun getSurroundingText(beforeLength: Int, afterLength: Int,
                flags: Int): SurroundingText? {
                // Mirror InputConnection's default fallback. InputConnectionWrapper delegates
                // this method, so overriding getSelectedText alone would not exercise it.
                val before = getTextBeforeCursor(beforeLength, flags) ?: return null
                val after = getTextAfterCursor(afterLength, flags) ?: return null
                val selected = getSelectedText(flags) ?: ""
                return SurroundingText(TextUtils.concat(before, selected, after), before.length,
                    before.length + selected.length, -1)
            }
        }
    }
}

/** Test-only editor: acknowledges a commit without changing its visible text. */
private class UncertainCommitEditor(context: Context) : EditText(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val delegate = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(delegate, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = true
            override fun commitText(text: CharSequence, newCursorPosition: Int,
                textAttribute: TextAttribute?): Boolean = true
        }
    }
}

/** One ordinary commit, followed by a transferable composing span on its inserted range. */
private class ComposedCommitEditor(context: Context, private val mismatch: Boolean) : EditText(context) {
    @Volatile var delegatedCommitCount = 0
        private set

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val delegate = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(delegate, true) {
            private var inCommit = false

            private fun commitOnce(requested: CharSequence?, cursor: Int,
                attribute: TextAttribute?, attributed: Boolean): Boolean {
                if (inCommit) return false
                inCommit = true
                try {
                    val inserted = if (mismatch) SmokeEditorActivity.COMPOSED_WRONG_RESULT else requested
                    val applied = if (attributed)
                        delegate.commitText(inserted ?: "", cursor, attribute)
                    else delegate.commitText(inserted, cursor)
                    if (!applied) return false
                    delegatedCommitCount++
                    val editable = this@ComposedCommitEditor.text
                    val end = this@ComposedCommitEditor.selectionStart
                    val start = end - (inserted?.length ?: 0)
                    if (start >= 0 && end <= editable.length && end > start &&
                        editable.subSequence(start, end).toString() == inserted.toString()) {
                        editable.setSpan(UnderlineSpan(), start, end,
                            Spanned.SPAN_COMPOSING or Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    return true
                } finally {
                    inCommit = false
                }
            }

            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
                commitOnce(text, newCursorPosition, null, false)

            override fun commitText(text: CharSequence, newCursorPosition: Int,
                textAttribute: TextAttribute?): Boolean =
                commitOnce(text, newCursorPosition, textAttribute, true)
        }
    }
}
