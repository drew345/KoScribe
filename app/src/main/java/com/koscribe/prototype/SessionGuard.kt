package com.koscribe.prototype

import java.util.UUID

enum class OutputMode(val label: String, val sampleOutput: String) {
    POLITE("요", "안녕하세요. 10분 후에 도착할게요."),
    CASUAL("반말", "안녕. 10분 후에 도착할게."),
    ENGLISH("EN", "Hello, I will arrive in ten minutes.");

    companion object { const val SAMPLE_ENGLISH = "Hello, I will arrive in ten minutes." }
}

/** Values are held only in process memory for one session. Never serialize or log this object. */
data class EditorSnapshot(
    val packageName: String,
    val windowId: Int,
    val uniqueNodeId: String,
    val viewId: String?,
    val className: String,
    val editorFieldId: Int,
    val inputType: Int,
    val surroundingText: String,
    val offset: Int,
    val selectionStart: Int,
    val selectionEnd: Int,
    val composing: Boolean
) {
    fun isSafe(): Boolean = packageName.isNotBlank() && windowId >= 0 &&
        uniqueNodeId.isNotBlank() && className.isNotBlank() && offset >= 0 &&
        selectionStart >= 0 && selectionStart == selectionEnd &&
        selectionEnd <= surroundingText.length && !composing
}

enum class GuardFailure {
    NO_SESSION, EXPIRED, CANCELLED, SUPERSEDED, TARGET_CHANGED, SELECTION_CHANGED,
    CONTEXT_CHANGED, COMPOSING, ALREADY_ATTEMPTED, NO_RESULT
}

sealed interface GuardDecision {
    data object Ready : GuardDecision
    data class Refused(val reason: GuardFailure) : GuardDecision
}

/** Serial, single-threaded state machine. reserveCommit consumes before IPC is called. */
class SessionGuard(private val lifetimeMs: Long = 180_000L) {
    data class Session(
        val id: String,
        val mode: OutputMode,
        val target: EditorSnapshot,
        val startedMs: Long,
        var result: String? = null,
        var cancelled: Boolean = false,
        var invalidated: Boolean = false,
        var attempted: Boolean = false
    )

    private var current: Session? = null

    fun begin(mode: OutputMode, target: EditorSnapshot, nowMs: Long): Session? {
        current?.cancelled = true
        if (!target.isSafe()) {
            current = null
            return null
        }
        return Session(UUID.randomUUID().toString(), mode, target, nowMs).also { current = it }
    }

    fun session(id: String): Session? = current?.takeIf { it.id == id && !it.cancelled }

    fun publish(id: String, result: String): Boolean {
        val session = session(id) ?: return false
        if (session.attempted || session.invalidated || result.isBlank()) return false
        session.result = result
        return true
    }

    fun cancel(id: String) {
        session(id)?.cancelled = true
    }

    fun invalidate(id: String) {
        session(id)?.invalidated = true
    }

    fun end(id: String) {
        if (current?.id == id) current = null
    }

    fun check(id: String, observed: EditorSnapshot?, nowMs: Long): GuardDecision {
        val session = current ?: return GuardDecision.Refused(GuardFailure.NO_SESSION)
        if (session.id != id) return GuardDecision.Refused(GuardFailure.SUPERSEDED)
        if (session.cancelled) return GuardDecision.Refused(GuardFailure.CANCELLED)
        if (session.attempted) return GuardDecision.Refused(GuardFailure.ALREADY_ATTEMPTED)
        if (session.invalidated) return GuardDecision.Refused(GuardFailure.TARGET_CHANGED)
        if (nowMs < session.startedMs || nowMs - session.startedMs > lifetimeMs)
            return GuardDecision.Refused(GuardFailure.EXPIRED)
        if (session.result.isNullOrBlank()) return GuardDecision.Refused(GuardFailure.NO_RESULT)
        if (observed?.composing == true) return GuardDecision.Refused(GuardFailure.COMPOSING)
        if (observed == null) return GuardDecision.Refused(GuardFailure.TARGET_CHANGED)
        val original = session.target
        if (original.selectionStart != observed.selectionStart ||
            original.selectionEnd != observed.selectionEnd || original.offset != observed.offset ||
            observed.selectionStart != observed.selectionEnd)
            return GuardDecision.Refused(GuardFailure.SELECTION_CHANGED)
        if (!observed.isSafe()) return GuardDecision.Refused(GuardFailure.TARGET_CHANGED)
        if (original.packageName != observed.packageName || original.windowId != observed.windowId ||
            original.uniqueNodeId != observed.uniqueNodeId || original.viewId != observed.viewId ||
            original.className != observed.className || original.editorFieldId != observed.editorFieldId ||
            original.inputType != observed.inputType)
            return GuardDecision.Refused(GuardFailure.TARGET_CHANGED)
        if (original.surroundingText != observed.surroundingText)
            return GuardDecision.Refused(GuardFailure.CONTEXT_CHANGED)
        return GuardDecision.Ready
    }

    fun reserveCommit(id: String, observed: EditorSnapshot?, nowMs: Long): GuardDecision {
        val decision = check(id, observed, nowMs)
        if (decision == GuardDecision.Ready) current!!.attempted = true
        return decision
    }
}
