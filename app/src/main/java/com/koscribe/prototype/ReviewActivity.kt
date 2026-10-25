package com.koscribe.prototype

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
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
            activity.feedback.text = message
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
        val content = column().apply {
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        content.addView(caption(if (VisualTestMode.enabled)
            "VISUAL TEST · KoScribe English review" else "KoScribe · English review", true))
        content.addView(caption("Selected mode: ${draft.mode.label}"))
        if (translationReview)
            content.addView(action("Cancel") { cancel() }.apply { id = R.id.review_cancel })
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
            minLines = 3
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        content.addView(editor, LinearLayout.LayoutParams(-1, dp(150)))
        feedback = caption("")
        content.addView(feedback)
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
                    feedback.text = "Translation could not start. Edit English or tap Translate."
                }
                UnboundDraftState.Confirm.EMPTY -> feedback.text = "Enter English before confirming."
                UnboundDraftState.Confirm.TOO_LONG ->
                    feedback.text = "English exceeds 512 characters. Shorten it before translating."
                UnboundDraftState.Confirm.EDITED_KOREAN_SAMPLE ->
                    feedback.text = "Phase 1 cannot translate edits. Restore the exact sample or choose EN in a new recording."
                else -> { feedback.text = "Review ended. Start again from the bubble."; finish() }
            }
        }.apply { id = R.id.review_confirm }
        content.addView(confirmButton)
        if (!translationReview)
            content.addView(action("Cancel") { cancel() }.apply { id = R.id.review_cancel })
        setContentView(insetScroll(content))
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
        if (processing) feedback.text = "Translating to the selected mode… Cancel is available above."
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
