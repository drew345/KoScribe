package com.koscribe.prototype

/** Fixed-category diagnostics for one owned session. Never consulted by the insertion policy. */
internal class StableOverlayInputTrace(private val capturedAt: Long) {
    private val prefix = mutableListOf("MODE_CAPTURE_G0")
    private var launchAt: Long? = null
    private var rejectedAt: Long? = null
    private var first = "NONE"
    private var following = "NONE"
    private var truncated = false

    fun mark(event: String, phase: String, now: Long) {
        if (rejectedAt != null) return
        if (event == "LAUNCH") launchAt = now
        if (prefix.size < 16) prefix += code(event, phase, now) else truncated = true
    }

    fun refuse(reason: String, phase: String, now: Long) {
        if (rejectedAt != null) return
        first = code(reason, phase, now)
        rejectedAt = now
    }

    fun followingStart(event: String, phase: String, now: Long) {
        val failedAt = rejectedAt ?: return
        if (following == "NONE" && now - failedAt in 0L until 500L)
            following = code(event, phase, now)
    }

    fun summary() = "first=$first prefix=${prefix.joinToString(">")}" +
        " truncated=$truncated nextStart=$following"

    private fun code(event: String, phase: String, now: Long) = "${event}_${phase}" +
        "_C${age(now - capturedAt)}_L${launchAt?.let { age(now - it) } ?: "PRE"}"

    private fun age(delta: Long): String = when {
        delta < 0 -> "UNKNOWN"
        delta < 50 -> "LT50"
        delta < 150 -> "LT150"
        delta < 500 -> "LT500"
        delta < 1_000 -> "LT1000"
        delta < 2_000 -> "LT2000"
        else -> "LATER"
    }
}
