package com.koscribe.prototype

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import java.lang.ref.WeakReference

/** Own editor for one unbound sample draft. Only its opaque ID crosses the Intent. */
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
    }

    private val owner = Any()
    private var draftId = ""
    private var handedOff = false
    private lateinit var feedback: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draftId = intent.getStringExtra(EXTRA_DRAFT_ID) ?: ""
        val draft = if (savedInstanceState == null)
            KoScribeService.active()?.bindDraftReview(draftId, owner) else null
        if (draft == null) { finish(); return }
        active = WeakReference(this)

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
        content.addView(caption("Fixed sample; no speech recognition ran. Edit English, then confirm."))
        val editor = EditText(this).apply {
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
        content.addView(action("Confirm English") {
            val service = KoScribeService.active()
            service?.updateDraftEnglish(draftId, owner, editor.text.toString())
            when (service?.confirmDraftReview(draftId, owner)) {
                UnboundDraftState.Confirm.READY -> {
                    handedOff = true
                    finish()
                }
                UnboundDraftState.Confirm.EMPTY -> feedback.text = "Enter English before confirming."
                UnboundDraftState.Confirm.EDITED_KOREAN_SAMPLE ->
                    feedback.text = "Phase 1 cannot translate edits. Restore the exact sample or choose EN in a new recording."
                else -> { feedback.text = "Review ended. Start again from the bubble."; finish() }
            }
        }.apply { id = R.id.review_confirm })
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
