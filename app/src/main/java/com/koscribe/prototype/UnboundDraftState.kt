package com.koscribe.prototype

import java.util.UUID

/** Process-only message content. No destination belongs to this state. */
internal class UnboundDraftState(private val newId: () -> String = { UUID.randomUUID().toString() }) {
    enum class Phase { REVIEW, READY, UNCERTAIN }
    data class Draft(
        val id: String,
        val mode: OutputMode,
        val english: String,
        val result: String?,
        val phase: Phase
    )
    enum class Confirm { READY, EMPTY, EDITED_KOREAN_SAMPLE, STALE }

    private var draft: Draft? = null

    fun begin(mode: OutputMode): Draft? {
        if (draft != null) return null
        return Draft(newId(), mode, OutputMode.SAMPLE_ENGLISH, null, Phase.REVIEW)
            .also { draft = it }
    }

    /** Actual transcription enters review as English, never as a fixed Korean sample. */
    fun beginSpeech(english: String): Draft? {
        if (draft != null || english.isBlank() || english.length > 512) return null
        return Draft(newId(), OutputMode.ENGLISH, english, null, Phase.REVIEW)
            .also { draft = it }
    }

    fun current(): Draft? = draft

    fun edit(id: String, english: String): Boolean {
        val old = draft ?: return false
        if (old.id != id || old.phase != Phase.REVIEW) return false
        draft = old.copy(english = english)
        return true
    }

    fun confirm(id: String): Confirm {
        val old = draft ?: return Confirm.STALE
        if (old.id != id || old.phase != Phase.REVIEW) return Confirm.STALE
        if (old.english.isBlank()) return Confirm.EMPTY
        if (old.mode != OutputMode.ENGLISH && old.english != OutputMode.SAMPLE_ENGLISH)
            return Confirm.EDITED_KOREAN_SAMPLE
        val output = if (old.mode == OutputMode.ENGLISH) old.english else old.mode.sampleOutput
        draft = old.copy(result = output, phase = Phase.READY)
        return Confirm.READY
    }

    fun markAttempted(id: String): Boolean {
        val old = draft ?: return false
        if (old.id != id || old.phase != Phase.READY || old.result.isNullOrBlank()) return false
        draft = old.copy(phase = Phase.UNCERTAIN)
        return true
    }

    fun end(id: String): Boolean {
        if (draft?.id != id) return false
        draft = null
        return true
    }

    fun endActive() { draft = null }
}
