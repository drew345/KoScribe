package com.koscribe.smoke

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            setPadding(margin, margin, margin, margin)
        }
        fixtureRoot = root
        root.addView(TextView(this).apply {
            text = "KoScribe synthetic editor test"
            textSize = 18f
        })
        root.addView(EditText(this).apply {
            id = R.id.local_test_editor
            hint = "Synthetic text only"
            minLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }, LinearLayout.LayoutParams(-1, (160 * density + 0.5f).toInt()))
        if (!intent.getBooleanExtra(EXTRA_OMIT_PROTECTED_EDITOR, false))
            root.addView(protectedEditor(), LinearLayout.LayoutParams(-1, -2))
        root.addView(UncertainCommitEditor(this).apply {
            id = R.id.local_uncertain_editor
            hint = "Synthetic uncertain commit field"
            inputType = InputType.TYPE_CLASS_TEXT
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }, LinearLayout.LayoutParams(-1, -2))
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
        // Publish only after PhoneWindow has installed its decor/content container.
        current = WeakReference(this)
        lifecycleCode = "CONTENT_READY"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::fixtureRoot.isInitialized) syncProtectedEditor()
    }

    private fun syncProtectedEditor() {
        val existing = fixtureRoot.findViewById<EditText>(R.id.local_protected_editor)
        if (intent.getBooleanExtra(EXTRA_OMIT_PROTECTED_EDITOR, false)) {
            if (existing != null) fixtureRoot.removeView(existing)
        } else if (existing == null) {
            fixtureRoot.addView(protectedEditor(), 2, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun protectedEditor(): EditText = EditText(this).apply {
        id = R.id.local_protected_editor
        hint = "Synthetic protected field"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
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
