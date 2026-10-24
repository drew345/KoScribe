package com.koscribe.prototype

/** One-way speech handoff. An ended or superseded upload can never publish a draft. */
internal class SpeechTransferGate {
    private enum class Phase { RECORDING, UPLOADING }
    private var owner: String? = null
    private var phase: Phase? = null

    fun arm(id: String): Boolean {
        if (owner != null) return false
        owner = id
        phase = Phase.RECORDING
        return true
    }

    fun stoppedAndReleased(id: String): Boolean {
        if (owner != id || phase != Phase.RECORDING) return false
        phase = Phase.UPLOADING
        return true
    }

    fun accept(id: String): Boolean {
        if (owner != id || phase != Phase.UPLOADING) return false
        clear()
        return true
    }

    fun cancel(id: String) { if (owner == id) clear() }
    fun clear() { owner = null; phase = null }
}
