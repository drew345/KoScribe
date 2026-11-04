package com.koscribe.prototype

import java.util.UUID

/** Process-only message content. No destination belongs to this state. */
internal class UnboundDraftState(private val newId: () -> String = { UUID.randomUUID().toString() }) {
    enum class Phase { REVIEW, TRANSLATING, READY, UNCERTAIN }
    enum class Source { SAMPLE, SPEECH }
    data class Draft(
        val id: String,
        val mode: OutputMode,
        val english: String,
        val result: String?,
        val phase: Phase,
        val source: Source,
        val translationAttempt: Int = 0
    )
    enum class Confirm { READY, TRANSLATING, TRANSLATION_ERROR, EMPTY, TOO_LONG,
        EDITED_KOREAN_SAMPLE, STALE }

    private var draft: Draft? = null

    fun begin(mode: OutputMode): Draft? {
        if (draft != null) return null
        return Draft(newId(), mode, OutputMode.SAMPLE_ENGLISH, null, Phase.REVIEW, Source.SAMPLE)
            .also { draft = it }
    }

    /** Actual transcription enters review as English, never as a fixed Korean sample. */
    fun beginSpeech(english: String): Draft? = beginSpeech(OutputMode.ENGLISH, english)

    fun beginSpeech(mode: OutputMode, english: String): Draft? {
        if (draft != null || english.isBlank() || english.length > 512) return null
        return Draft(newId(), mode, english, null, Phase.REVIEW, Source.SPEECH)
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
        return confirm(id, fixedOverlaySample = false)
    }

    /** Explicit test-sample approval; the UI discloses that the result is fixed. */
    fun confirmFixedOverlaySample(id: String): Confirm {
        val current = draft ?: return Confirm.STALE
        if (current.source != Source.SAMPLE || current.mode == OutputMode.ENGLISH) return Confirm.STALE
        if (current.english.length > 512) return Confirm.TOO_LONG
        return confirm(id, fixedOverlaySample = true)
    }

    private fun confirm(id: String, fixedOverlaySample: Boolean): Confirm {
        val old = draft ?: return Confirm.STALE
        if (old.id != id || old.phase != Phase.REVIEW) return Confirm.STALE
        if (old.english.isBlank()) return Confirm.EMPTY
        if (old.source == Source.SPEECH && old.mode != OutputMode.ENGLISH) {
            if (old.english.length > 512) return Confirm.TOO_LONG
            draft = old.copy(phase = Phase.TRANSLATING,
                translationAttempt = old.translationAttempt + 1)
            return Confirm.TRANSLATING
        }
        if (!fixedOverlaySample && old.mode != OutputMode.ENGLISH && old.english != OutputMode.SAMPLE_ENGLISH)
            return Confirm.EDITED_KOREAN_SAMPLE
        val output = if (old.mode == OutputMode.ENGLISH) old.english else old.mode.sampleOutput
        draft = old.copy(result = output, phase = Phase.READY)
        return Confirm.READY
    }

    fun translationSucceeded(id: String, attempt: Int, output: String): Boolean {
        val old = draft ?: return false
        if (old.id != id || old.phase != Phase.TRANSLATING ||
            old.translationAttempt != attempt || old.source != Source.SPEECH ||
            old.mode == OutputMode.ENGLISH || output.isBlank() || output.length > 512 ||
            output.any { it == '\n' || it == '\r' || it == '\u2028' || it == '\u2029' ||
                Character.isISOControl(it) }) return false
        draft = old.copy(result = output, phase = Phase.READY)
        return true
    }

    /** A failed request returns to editable English; a retry needs another explicit confirm. */
    fun translationFailed(id: String, attempt: Int): Boolean {
        val old = draft ?: return false
        if (old.id != id || old.phase != Phase.TRANSLATING ||
            old.translationAttempt != attempt) return false
        draft = old.copy(phase = Phase.REVIEW)
        return true
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
