package com.koscribe.prototype

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import java.lang.ref.WeakReference

/** Own editor for one unbound draft. Only its opaque ID crosses the Intent. */
class ReviewActivity : Activity() {
    companion object {
        const val EXTRA_DRAFT_ID = "draft_id"
        private var active = WeakReference<ReviewActivity>(null)

        fun closeDraft(id: String) {
            active.get()?.takeIf { it.draftId == id && !it.isFinishing }?.let {
                it.handedOff = true
                it.finish()
            }
        }

        fun finishReadyDraft(id: String): Boolean {
            val activity = active.get()?.takeIf { it.draftId == id && !it.isFinishing }
                ?: return false
            activity.handedOff = true
            activity.finish()
            return true
        }

        fun translationFailed(id: String, message: String): Boolean {
            val activity = active.get()?.takeIf { it.draftId == id && !it.isFinishing }
                ?: return false
            activity.setProcessing(false)
            activity.confirmButton.text = "Translate"
            activity.showFeedback(message)
            return true
        }
    }

    private val owner = Any()
    private var draftId = ""
    private var handedOff = false
    private lateinit var feedback: TextView
    private lateinit var editor: EditText
    private lateinit var instructions: TextView
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draftId = intent.getStringExtra(EXTRA_DRAFT_ID) ?: ""
        val draft = if (savedInstanceState == null)
            KoScribeService.active()?.bindDraftReview(draftId, owner) else null
        if (draft == null) { finish(); return }
        active = WeakReference(this)
        val translationReview = BuildConfig.TRANSLATION_INPUT &&
            draft.source == UnboundDraftState.Source.SPEECH && draft.mode != OutputMode.ENGLISH

        if (!VisualTestMode.enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        installInsets(root)
        root.addView(caption((if (VisualTestMode.enabled)
            "VISUAL TEST · KoScribe" else "KoScribe") +
            " · English review · ${draft.mode.label}", true).apply { textSize = 17f })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        confirmButton = action(if (translationReview)
            "Confirm & translate" else "Confirm English") {
            val service = KoScribeService.active()
            service?.updateDraftEnglish(draftId, owner, editor.text.toString())
            when (service?.confirmDraftReview(draftId, owner)) {
                UnboundDraftState.Confirm.READY -> {
                    handedOff = true
                    finish()
                }
                UnboundDraftState.Confirm.TRANSLATING -> setProcessing(true)
                UnboundDraftState.Confirm.TRANSLATION_ERROR -> {
                    confirmButton.text = "Translate"
                    showFeedback("Translation could not start. Edit English or tap Translate.")
                }
                UnboundDraftState.Confirm.EMPTY -> showFeedback("Enter English before confirming.")
                UnboundDraftState.Confirm.TOO_LONG ->
                    showFeedback("English exceeds 512 characters. Shorten it before translating.")
                UnboundDraftState.Confirm.EDITED_KOREAN_SAMPLE ->
                    showFeedback("Phase 1 cannot translate edits. Restore the exact sample or choose EN in a new recording.")
                else -> { showFeedback("Review ended. Start again from the bubble."); finish() }
            }
        }.apply { id = R.id.review_confirm; isAllCaps = false; textSize = 13f }
        actions.addView(confirmButton, LinearLayout.LayoutParams(0, dp(52), 2f))
        actions.addView(action("Cancel") { cancel() }.apply {
            id = R.id.review_cancel
            isAllCaps = false
            textSize = 13f
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(actions, LinearLayout.LayoutParams(-1, dp(56)))
        feedback = caption("").apply { visibility = View.GONE; textSize = 13f }
        root.addView(feedback)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        instructions = caption(when {
            translationReview ->
                "Edit English, then confirm. Only confirmed English is sent to OpenAI for translation."
            BuildConfig.SPEECH_INPUT && !BuildConfig.TRANSLATION_INPUT ->
                "Edit the English transcript, then confirm. Korean translation arrives in the next phase."
            BuildConfig.SPEECH_INPUT ->
                "Edit the English transcript, then confirm. EN remains English."
            else -> "Fixed sample; no speech recognition ran. Edit English, then confirm."
        })
        content.addView(instructions)
        editor = EditText(this).apply {
            id = R.id.review_english
            setText(draft.english)
            minLines = 2
            maxLines = 6
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        content.addView(editor, LinearLayout.LayoutParams(-1, -2))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
            addView(content)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        // Dialog themes default to wrap-content; weight needs a bounded viewport.
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT)
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                KoScribeService.active()?.updateDraftEnglish(draftId, owner, s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        editor.requestFocus()
        editor.post {
            if (!isFinishing && !isDestroyed)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setProcessing(processing: Boolean) {
        editor.isEnabled = !processing
        editor.visibility = if (processing) View.GONE else View.VISIBLE
        instructions.visibility = if (processing) View.GONE else View.VISIBLE
        confirmButton.isEnabled = !processing
        confirmButton.visibility = if (processing) View.GONE else View.VISIBLE
        if (processing) showFeedback("Translating to the selected mode… Cancel remains available.")
    }

    private fun showFeedback(message: String) {
        feedback.text = message
        feedback.visibility = if (message.isBlank()) View.GONE else View.VISIBLE
    }

    /** Apply only the portion of bars/keyboard that actually overlaps the activity root. */
    private fun installInsets(root: View) {
        val side = dp(16)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bounds = windowManager.currentWindowMetrics.bounds
            val position = IntArray(2)
            view.getLocationOnScreen(position)
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or
                WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            val top = (bounds.top + bars.top - position[1]).coerceAtLeast(0)
            val bottomEdge = bounds.bottom - maxOf(bars.bottom, ime.bottom)
            val bottom = (position[1] + view.height - bottomEdge).coerceAtLeast(0)
            val left = side + (bounds.left + bars.left - position[0]).coerceAtLeast(0)
            val right = side + (position[0] + view.width - (bounds.right - bars.right))
                .coerceAtLeast(0)
            if (view.paddingLeft != left || view.paddingTop != top ||
                view.paddingRight != right || view.paddingBottom != bottom)
                view.setPadding(left, top, right, bottom)
            insets
        }
        root.addOnLayoutChangeListener { view, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom != oldBottom) view.requestApplyInsets()
        }
    }

    private fun cancel() {
        KoScribeService.active()?.cancelDraftReview(draftId, owner)
        handedOff = true
        finish()
    }

    override fun onPause() {
        if (!handedOff) cancel()
        super.onPause()
    }

    override fun onStop() {
        if (handedOff) KoScribeService.active()?.reviewActivityFinished(draftId, owner)
        super.onStop()
    }

    override fun onDestroy() {
        if (!handedOff) KoScribeService.active()?.cancelDraftReview(draftId, owner)
        if (active.get() === this) active = WeakReference<ReviewActivity>(null)
        super.onDestroy()
    }
}
