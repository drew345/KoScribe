package com.koscribe.smoke

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.Display
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
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
