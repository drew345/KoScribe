package com.koscribe.prototype

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
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
import android.text.InputFilter
import java.lang.ref.WeakReference

/** Own editor for one unbound draft. Only its opaque ID crosses the Intent. */
class ReviewActivity : Activity() {
    companion object {
        data class OwnGeometry(
            val slot: Rect, val card: Rect, val editor: Rect, val actions: Rect,
            val imeTop: Int?, val imeVisible: Boolean, val imeBottom: Int
        )
        /** Read-only, code-only breakdown for a refused automatic review transition. */
        data class OwnGeometryProbe(
            val code: String, val slot: Rect?, val card: Rect?, val editor: Rect?,
            val actions: Rect?, val imeTop: Int?
        )
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
            KoScribeService.active()?.beginAutoReturn(id, activity.owner)
            activity.handedOff = true
            activity.finish()
            return true
        }

        fun ownsFocusedReview(id: String, owner: Any): Boolean = active.get()?.let {
            it.draftId == id && it.owner === owner && !it.isFinishing &&
                !it.isDestroyed && it.window.decorView.hasWindowFocus()
        } == true

        /** Only the currently bound review window may justify an own-window transition. */
        fun expectedWindowId(id: String, owner: Any): Int? {
            val activity = active.get()?.takeIf {
                it.draftId == id && it.owner === owner && !it.isFinishing && !it.isDestroyed
            } ?: return null
            val decor = activity.window.decorView.takeIf { it.isAttachedToWindow } ?: return null
            return runCatching { decor.createAccessibilityNodeInfo().windowId }
                .getOrNull()?.takeIf { it >= 0 }
        }

        /** Local references only; the service performs the remote accessibility attestation. */
        fun focusedReviewViews(id: String, owner: Any): Pair<View, EditText>? {
            val activity = active.get()?.takeIf {
                it.draftId == id && it.owner === owner && !it.isFinishing && !it.isDestroyed &&
                    it::editor.isInitialized
            } ?: return null
            val decor = activity.window.decorView
            val edit = activity.editor
            return if (decor.hasWindowFocus() && decor.isAttachedToWindow && edit.isAttachedToWindow &&
                edit.isFocused && decor.windowToken != null && edit.windowToken == decor.windowToken)
                decor to edit else null
        }

        /** Only owned view rectangles and keyboard inset; no editor contents. */
        fun ownGeometry(id: String, owner: Any): OwnGeometry? {
            val activity = active.get()?.takeIf {
                it.draftId == id && it.owner === owner && !it.isFinishing && !it.isDestroyed &&
                    it::editor.isInitialized && it::hostSlot.isInitialized
            } ?: return null
            val decorOrigin = IntArray(2).also(activity.window.decorView::getLocationOnScreen)
            fun bounds(view: View): Rect? = Rect().takeIf {
                view.isAttachedToWindow && view.isShown && view.getGlobalVisibleRect(it) && !it.isEmpty
            }?.apply { offset(decorOrigin[0], decorOrigin[1]) }
            val slot = bounds(activity.hostSlot) ?: return null
            val card = bounds(activity.contentCard) ?: return null
            val editor = bounds(activity.editor) ?: return null
            val actions = bounds(activity.actionsDock) ?: return null
            val insets = activity.window.decorView.rootWindowInsets ?: return null
            val imeVisible = insets.isVisible(WindowInsets.Type.ime())
            val imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom
            val screen = activity.windowManager.currentWindowMetrics.bounds
            val imeTop = if (imeVisible && imeBottom > 0) screen.bottom - imeBottom else null
            return OwnGeometry(slot, card, editor, actions, imeTop, imeVisible, imeBottom)
        }

        fun ownGeometryProbe(id: String, owner: Any): OwnGeometryProbe {
            val activity = active.get()?.takeIf {
                it.draftId == id && it.owner === owner && !it.isFinishing && !it.isDestroyed
            } ?: return OwnGeometryProbe("ACTIVITY_OR_OWNER", null, null, null, null, null)
            val decor = activity.window.decorView
            val origin = IntArray(2).also(decor::getLocationOnScreen)
            fun inspect(view: View?): Pair<String, Rect?> {
                if (view == null) return "000" to null
                val attached = view.isAttachedToWindow
                val shown = view.isShown
                val visible = Rect()
                val nonempty = attached && shown && view.getGlobalVisibleRect(visible) &&
                    !visible.isEmpty
                if (nonempty) visible.offset(origin[0], origin[1])
                return "${if (attached) 1 else 0}${if (shown) 1 else 0}" +
                    "${if (nonempty) 1 else 0}" to visible.takeIf { nonempty }
            }
            val (slotBits, slot) = inspect(if (activity::hostSlot.isInitialized)
                activity.hostSlot else null)
            val (cardBits, card) = inspect(if (activity::contentCard.isInitialized)
                activity.contentCard else null)
            val (editorBits, editor) = inspect(if (activity::editor.isInitialized)
                activity.editor else null)
            val (actionsBits, actions) = inspect(if (activity::actionsDock.isInitialized)
                activity.actionsDock else null)
            val insets = decor.rootWindowInsets
            val imeVisible = insets?.isVisible(WindowInsets.Type.ime()) == true
            val imeBottom = insets?.getInsets(WindowInsets.Type.ime())?.bottom ?: 0
            val imeTop = if (imeVisible && imeBottom > 0)
                activity.windowManager.currentWindowMetrics.bounds.bottom - imeBottom else null
            val stage = when {
                slot == null -> "SLOT_NOT_READY"
                card == null -> "CARD_NOT_READY"
                editor == null -> "EDITOR_NOT_READY"
                actions == null -> "ACTIONS_NOT_READY"
                insets == null -> "INSETS_MISSING"
                !imeVisible -> "IME_HIDDEN"
                imeBottom <= 0 -> "IME_ZERO"
                else -> "READY"
            }
            return OwnGeometryProbe("${stage}_S${slotBits}_C${cardBits}_E${editorBits}" +
                "_A${actionsBits}_INSETS_${if (insets != null) 1 else 0}" +
                "_IME_VISIBLE_${if (imeVisible) 1 else 0}_POSITIVE_${if (imeBottom > 0) 1 else 0}",
                slot, card, editor, actions, imeTop)
        }

        fun translationFailed(id: String, message: String): Boolean {
            val activity = active.get()?.takeIf { it.draftId == id && !it.isFinishing }
                ?: return false
            activity.setProcessing(false)
            activity.confirmButton.text = activity.confirmLabel()
            activity.showFeedback(message)
            return true
        }

        fun autoUnavailable(id: String) {
            active.get()?.takeIf { it.draftId == id && !it.isFinishing }?.let {
                it.autoRequested = false
                it.updateAutoLabel()
            }
        }
    }

    private val owner = Any()
    private var draftId = ""
    private var handedOff = false
    private lateinit var feedback: TextView
    private lateinit var editor: EditText
    private lateinit var instructions: TextView
    private lateinit var confirmButton: Button
    private lateinit var hostSlot: View
    private lateinit var contentCard: View
    private lateinit var actionsDock: View
    private var autoRequested = false
    private var translationReview = false
    private var koreanSampleReview = false
    private val processingInputBlock = InputFilter { _, _, _, dest, start, end ->
        dest.subSequence(start, end)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draftId = intent.getStringExtra(EXTRA_DRAFT_ID) ?: ""
        val draft = if (savedInstanceState == null)
            KoScribeService.active()?.bindDraftReview(draftId, owner) else null
        if (draft == null) { finish(); return }
        active = WeakReference(this)
        translationReview = BuildConfig.TRANSLATION_INPUT &&
            draft.source == UnboundDraftState.Source.SPEECH && draft.mode != OutputMode.ENGLISH
        koreanSampleReview = draft.source == UnboundDraftState.Source.SAMPLE &&
            draft.mode != OutputMode.ENGLISH
        autoRequested = KoScribeService.active()?.autoInsertOffered(draftId, owner) == true

        if (!VisualTestMode.enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        installInsets(root)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(Color.WHITE, 16)
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        contentCard = content
        content.addView(TextView(this).apply {
            text = if (VisualTestMode.enabled) "VISUAL TEST · ENGLISH REVIEW"
                else "ENGLISH REVIEW"
            textSize = 11f
            letterSpacing = 0.1f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF2E7569.toInt())
            contentDescription = "English review · ${draft.mode.label}"
        })
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(Color.WHITE, 12)
        }
        actionsDock = actions
        confirmButton = action(confirmLabel()) {
            val service = KoScribeService.active()
            service?.updateDraftEnglish(draftId, owner, editor.text.toString())
            when (service?.confirmDraftReview(draftId, owner)) {
                UnboundDraftState.Confirm.READY -> {
                    service?.beginAutoReturn(draftId, owner)
                    handedOff = true
                    finish()
                }
                UnboundDraftState.Confirm.TRANSLATING -> setProcessing(true)
                UnboundDraftState.Confirm.TRANSLATION_ERROR -> {
                    confirmButton.text = confirmLabel()
                    showFeedback("Translation could not start. Edit English or tap Translate.")
                }
                UnboundDraftState.Confirm.EMPTY -> showFeedback("Enter English before confirming.")
                UnboundDraftState.Confirm.TOO_LONG ->
                    showFeedback("English exceeds 512 characters. Shorten it before translating.")
                UnboundDraftState.Confirm.EDITED_KOREAN_SAMPLE ->
                    showFeedback("This fixed sample cannot translate edits. Restore the sample or choose EN in a new recording.")
                else -> { showFeedback("Review ended. Start again from the bubble."); finish() }
            }
        }.apply {
            id = R.id.review_confirm
            styleAction(true)
        }
        actions.addView(confirmButton, LinearLayout.LayoutParams(0, dp(48), 1.8f).apply {
            marginEnd = dp(8)
        })
        actions.addView(action("Cancel") { cancel() }.apply {
            id = R.id.review_cancel
            styleAction(false)
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        instructions = caption(when {
            translationReview && autoRequested ->
                "Edit English, then translate. Only confirmed English is sent to OpenAI. KoScribe inserts only if the original field and cursor verify; otherwise choose the field and tap Insert."
            translationReview ->
                "Edit the transcript, then confirm it for translation."
            koreanSampleReview && autoRequested ->
                "Fixed sample; no speech recognition ran. Confirm to insert only if the original field and cursor verify. Otherwise choose the field and tap Insert."
            BuildConfig.SPEECH_INPUT && !BuildConfig.TRANSLATION_INPUT ->
                "Edit the transcript, then confirm the English output."
            BuildConfig.SPEECH_INPUT ->
                "Edit the transcript, then confirm the English output."
            else -> "Fixed sample; no speech recognition ran. Edit English, then confirm."
        }).apply {
            textSize = 13f
            setTextColor(0xFF536A5F.toInt())
            setPadding(0, dp(8), 0, dp(9))
        }
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
            textSize = 16f
            setTextColor(0xFF203B36.toInt())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(0xFFFBFDFB.toInt(), 10).apply {
                setStroke(dp(1), 0xFFD8E4DC.toInt())
            }
        }
        content.addView(editor, LinearLayout.LayoutParams(-1, -2))
        feedback = caption("").apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(0xFF9B2635.toInt())
        }
        content.addView(feedback)
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(0, dp(12), 0, dp(8))
            addView(content, LinearLayout.LayoutParams(-1, -2))
        }
        val scroll = ScrollView(this).apply {
            // Fill only this transparent wrapper. The review card stays wrap-content
            // just above the fixed action dock and can still scroll when it grows.
            isFillViewport = true
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
            addView(scrollContent)
        }
        if (autoRequested) {
            hostSlot = View(this).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                isClickable = false
            }
            root.addView(hostSlot, LinearLayout.LayoutParams(-1, dp(56)))
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(actions, LinearLayout.LayoutParams(-1, dp(64)).apply {
            bottomMargin = dp(8)
        })
        setContentView(root)
        if (autoRequested) root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            root.post {
                if (!isFinishing && !isDestroyed)
                    KoScribeService.active()?.autoReviewGeometryChanged(draftId, owner)
            }
        }
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
        // Keep the exact focused review editor/input session alive for the optional
        // automatic return. The confirmed English is immutable while translation runs.
        editor.filters = if (processing && autoRequested) arrayOf(processingInputBlock)
            else emptyArray()
        editor.isEnabled = !processing || autoRequested
        editor.visibility = if (processing && !autoRequested) View.GONE else View.VISIBLE
        editor.isCursorVisible = !processing
        instructions.visibility = if (processing) View.GONE else View.VISIBLE
        confirmButton.isEnabled = !processing
        confirmButton.visibility = if (processing) View.GONE else View.VISIBLE
        if (processing) showFeedback("Translating to the selected mode… Cancel remains available.")
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun Button.styleAction(primary: Boolean) {
        isAllCaps = false
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (primary) Color.WHITE else 0xFF536A5F.toInt())
        background = rounded(if (primary) 0xFF2E7569.toInt() else Color.WHITE, 10).apply {
            if (!primary) setStroke(dp(1), 0xFFDCE6DF.toInt())
        }
    }

    private fun confirmLabel(): String = when {
        autoRequested && translationReview -> "Translate & insert"
        autoRequested && koreanSampleReview -> "Confirm & insert"
        translationReview -> "Confirm & translate"
        else -> "Confirm English"
    }

    private fun updateAutoLabel() {
        if (::confirmButton.isInitialized) confirmButton.text = confirmLabel()
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

    override fun onResume() {
        super.onResume()
        if (BuildConfig.AUTO_INSERT && ::editor.isInitialized && window.decorView.hasWindowFocus()) {
            KoScribeService.active()?.autoReviewFocused(draftId, owner, window.decorView, editor,
                fromResume = true)
            editor.post { KoScribeService.active()?.autoReviewGeometryChanged(draftId, owner) }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (BuildConfig.AUTO_INSERT && ::editor.isInitialized)
            KoScribeService.active()?.autoReviewWindowFocus(draftId, owner, hasFocus,
                window.decorView, editor)
        if (hasFocus && autoRequested && ::editor.isInitialized)
            editor.post { KoScribeService.active()?.autoReviewGeometryChanged(draftId, owner) }
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
