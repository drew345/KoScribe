package com.koscribe.prototype

/** Diagnostic state only. No permit, draft, result, or insertion eligibility exists here. */
internal class ReviewSurfaceProbePolicy(private val openedMs: Long) {
    enum class State { OPENING, EDITING, RETURNING, COMPLETE, CANCELLED, REFUSED }
    var state = State.OPENING
        private set
    var edits = 0
        private set
    private var returnMs: Long? = null
    private var lastMs = openedMs
    val active: Boolean get() = state in setOf(State.OPENING, State.EDITING, State.RETURNING)

    fun tick(now: Long): Boolean {
        if (!active) return false
        if (now < lastMs) { state = State.REFUSED; return false }
        if (now - (returnMs ?: openedMs) >= (if (returnMs == null) 120_000L else 2_000L)) {
            state = if (returnMs == null) State.CANCELLED else State.COMPLETE
            return false
        }
        lastMs = now
        return true
    }

    fun focused(now: Long) { if (tick(now) && state == State.OPENING) state = State.EDITING }
    fun edited(now: Long) { if (tick(now) && state == State.EDITING) edits++ }
    fun dismiss(now: Long): Boolean {
        if (!tick(now) || state !in setOf(State.OPENING, State.EDITING)) return false
        returnMs = now
        state = State.RETURNING
        return true
    }
    fun cancel(refused: Boolean = false) {
        if (active) state = if (refused) State.REFUSED else State.CANCELLED
    }

    data class Comparison(
        val identity: String = "UNKNOWN", val caret: String = "UNKNOWN",
        val context: String = "UNKNOWN", val generation: String = "UNKNOWN"
    ) {
        // Even exact node/root equality cannot expose logical conversation rebinding.
        fun code(input: String) = "identity=$identity caret=$caret context=$context" +
            " generation=$generation input=$input semantic=UNKNOWN"
    }

    companion object {
        fun compare(original: EditorSnapshot, observed: EditorSnapshot?,
            sameNode: Boolean, sameRoot: Boolean, originalGeneration: Int,
            observedGeneration: Int?): Comparison {
            if (observed == null || observedGeneration == null) return Comparison()
            fun match(value: Boolean) = if (value) "MATCH" else "MISMATCH"
            return Comparison(
                match(sameNode && sameRoot && original.windowId == observed.windowId &&
                    original.packageName == observed.packageName &&
                    original.uniqueNodeId == observed.uniqueNodeId &&
                    original.viewId == observed.viewId && original.className == observed.className &&
                    original.editorFieldId == observed.editorFieldId &&
                    original.inputType == observed.inputType),
                match(original.offset + original.selectionStart ==
                    observed.offset + observed.selectionStart &&
                    original.offset + original.selectionEnd == observed.offset + observed.selectionEnd),
                match(original.offset == observed.offset &&
                    original.surroundingText == observed.surroundingText),
                if (observedGeneration > originalGeneration) "FRESH"
                else if (observedGeneration == originalGeneration) "SAME" else "UNKNOWN"
            )
        }
    }
}
