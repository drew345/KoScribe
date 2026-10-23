package com.koscribe.prototype

import java.util.UUID

/** Recording ownership only. No editor or destination state exists until a later explicit action. */
internal class UnboundRecordingState(private val newId: () -> String = { UUID.randomUUID().toString() }) {
    enum class Phase { PREPARING, RECORDING, READ_ONLY_REVIEW }
    data class Active(val id: String, val mode: OutputMode, val phase: Phase)

    private var active: Active? = null

    fun begin(mode: OutputMode): Active? {
        if (active != null) return null
        return Active(newId(), mode, Phase.PREPARING).also { active = it }
    }

    fun current(): Active? = active
    fun owns(id: String): Boolean = active?.id == id

    fun microphoneProved(id: String): Boolean = advance(id, Phase.PREPARING, Phase.RECORDING)
    fun microphoneStoppedForReview(id: String): Boolean =
        advance(id, Phase.RECORDING, Phase.READ_ONLY_REVIEW)

    fun end(id: String): Boolean {
        if (!owns(id)) return false
        active = null
        return true
    }

    fun endActive() { active = null }

    private fun advance(id: String, from: Phase, to: Phase): Boolean {
        val old = active ?: return false
        if (old.id != id || old.phase != from) return false
        active = old.copy(phase = to)
        return true
    }
}
