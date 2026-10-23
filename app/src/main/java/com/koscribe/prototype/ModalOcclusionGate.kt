package com.koscribe.prototype

enum class PanelPhase { PREPARING, RECORDING, REVIEW, RETURNING }

/**
 * One session's alternating target visibility during its owned modal panel.
 * This never establishes editor continuity by itself: all ordinary event and
 * fresh precommit checks remain decisive. Losing evidence permanently poisons
 * this instance; create a new instance only for a newly captured session.
 */
class ModalOcclusionGate(private val targetWindowId: Int) {
    private var owner: Any? = null
    private var phase = PanelPhase.PREPARING
    private var resumed = false
    private var focused = false
    private var invalid = false
    private var targetHidden = false
    private var firstRejection: String? = null
    var panelWindowId: Int? = null
        private set
    var hasAcquiredFocus = false
        private set

    fun bind(candidate: Any): Boolean {
        if (owner != null || invalid) return reject("OWNER_ALREADY_BOUND")
        owner = candidate
        return true
    }

    fun owns(candidate: Any): Boolean = owner === candidate

    fun updatePanel(
        candidate: Any, nextPhase: PanelPhase, isResumed: Boolean,
        hasWindowFocus: Boolean, windowId: Int, interrupted: Boolean = false
    ): Boolean {
        if (invalid || !owns(candidate)) return reject("OWNER_OR_INVALID_SESSION")
        if (phase == PanelPhase.RETURNING) return reject("ALREADY_RETURNING")
        val legalPhase = nextPhase == phase ||
            (phase == PanelPhase.PREPARING && nextPhase == PanelPhase.RECORDING) ||
            (phase == PanelPhase.RECORDING && nextPhase == PanelPhase.REVIEW)
        if (!legalPhase) return reject("PHASE_SEQUENCE")
        if (interrupted || (resumed && !isResumed)) return reject("PANEL_PAUSED")
        if (hasAcquiredFocus && !hasWindowFocus) return reject("PANEL_FOCUS_LOST")
        if (windowId >= 0) {
            if (windowId == targetWindowId ||
                (panelWindowId != null && panelWindowId != windowId)) return reject("PANEL_WINDOW_CHANGED")
            panelWindowId = windowId
        } else if (panelWindowId != null) return reject("PANEL_WINDOW_UNKNOWN")
        phase = nextPhase
        resumed = isResumed
        focused = hasWindowFocus
        hasAcquiredFocus = hasAcquiredFocus || hasWindowFocus
        return true
    }

    /** The caller resolves a unique, rooted application window, not a package alone. */
    fun focusedPanelMatches(focusedApplicationWindowId: Int?): Boolean =
        !invalid && resumed && focused && panelWindowId != null &&
            focusedApplicationWindowId == panelWindowId

    fun targetRemoved(
        windowId: Int, focusedApplicationWindowId: Int?, exactRemovalEvent: Boolean,
        targetAbsent: Boolean
    ): Boolean {
        if (invalid || targetHidden) return reject("REMOVAL_SEQUENCE")
        if (phase != PanelPhase.RECORDING && phase != PanelPhase.REVIEW) return reject("REMOVAL_PHASE")
        if (windowId != targetWindowId || !exactRemovalEvent || !targetAbsent)
            return reject("REMOVAL_WINDOW_EVENT")
        if (!focusedPanelMatches(focusedApplicationWindowId)) return reject("REMOVAL_PANEL_EVIDENCE")
        targetHidden = true
        return true
    }

    fun targetAdded(
        windowId: Int, focusedApplicationWindowId: Int?, exactAdditionEvent: Boolean,
        targetIsApplication: Boolean, targetIsFocused: Boolean,
        originalRootMatches: Boolean, originalEditorMatches: Boolean
    ): Boolean {
        if (invalid || !targetHidden) return reject("ADDITION_SEQUENCE")
        if (windowId != targetWindowId || !exactAdditionEvent || !targetIsApplication)
            return reject("ADDITION_WINDOW_EVENT")
        if (!originalRootMatches) return reject("ADDITION_ROOT_IDENTITY")
        if (!originalEditorMatches) return reject("ADDITION_EDITOR_IDENTITY")
        if (phase == PanelPhase.RETURNING) {
            if (!targetIsFocused || focusedApplicationWindowId != targetWindowId)
                return reject("RETURN_TARGET_FOCUS")
        } else {
            if (phase != PanelPhase.RECORDING && phase != PanelPhase.REVIEW) return reject("ADDITION_PHASE")
            if (targetIsFocused || !focusedPanelMatches(focusedApplicationWindowId))
                return reject("ADDITION_PANEL_EVIDENCE")
        }
        targetHidden = false
        return true
    }

    /** Called only by confirmation, before the Activity deliberately gives up focus. */
    fun beginReturn(candidate: Any): Boolean {
        if (invalid || !owns(candidate) || phase != PanelPhase.REVIEW || !resumed || !focused ||
            panelWindowId == null)
            return reject("CONFIRMATION_EVIDENCE")
        phase = PanelPhase.RETURNING
        return true
    }

    /** A missing final ADD is incomplete evidence even if another query finds the target. */
    fun readyForCommit(): Boolean = !invalid && phase == PanelPhase.RETURNING && !targetHidden

    /** Fixed categories only: no window IDs, editor identities, or content. */
    fun diagnosticCode(): String = "${phase.name}_R${if (resumed) 1 else 0}" +
        "_F${if (focused) 1 else 0}_W${if (panelWindowId != null) 1 else 0}" +
        "_${if (targetHidden) "HIDDEN" else "PRESENT"}" +
        "_${firstRejection ?: "ELIGIBLE"}"

    fun invalidate() {
        if (firstRejection == null) firstRejection = "SESSION_INVALIDATED"
        invalid = true
    }

    private fun reject(reason: String): Boolean {
        if (firstRejection == null) firstRejection = reason
        invalidate()
        return false
    }
}
