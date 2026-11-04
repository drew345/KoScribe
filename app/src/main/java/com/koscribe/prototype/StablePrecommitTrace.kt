package com.koscribe.prototype

/** Fixed codes from one owned-fixture transaction. No editor payload or write authority. */
internal class StablePrecommitTrace(
    private val returnStarted: Long?,
    private val readStarted: Long,
    private val readFinished: Long,
    private val guardStarted: Long,
    private val armed: Long
) {
    enum class Kind { WINDOWS, WINDOW_STATE, VIEW_FOCUS, TARGET_TEXT, TARGET_SELECTION, TARGET_CONTENT, INPUT_START, INPUT_FINISH }
    enum class Role { ORIGINAL, REVIEW, HOST, CURRENT_DEFAULT_IME, CURRENT_SYSTEM, FIXTURE_PACKAGE, OWN_PACKAGE, UNKNOWN }

    private val prefix = mutableListOf<String>()
    private val families = mutableSetOf<Kind>()
    private var count = 0
    private var outcome = "PENDING"

    @Synchronized
    fun invalidated(kind: Kind, role: Role, changes: Int?, eventTime: Long?, receipt: Long) {
        if (outcome != "PENDING") return
        count = (count + 1).coerceAtMost(999)
        families += kind
        if (prefix.size >= 8) return
        val mask = when (kind) {
            Kind.WINDOWS -> "_WC${changes}"
            Kind.WINDOW_STATE, Kind.TARGET_CONTENT -> "_CT${changes}"
            else -> ""
        }
        val timestamp = when {
            eventTime == null -> "NO_EVENT_STAMP"
            eventTime <= 0 -> "UNKNOWN_STAMP"
            eventTime > receipt -> "FUTURE_STAMP"
            returnStarted != null && eventTime < returnStarted -> "BEFORE_RETURN"
            eventTime == returnStarted -> "RETURN_TICK"
            eventTime < readStarted -> "BEFORE_READ"
            eventTime == readStarted || eventTime == readFinished ||
                eventTime == guardStarted || eventTime == armed -> buildString {
                append("AT_TICK")
                if (eventTime == readStarted) append("_READ_START")
                if (eventTime == readFinished) append("_READ_END")
                if (eventTime == guardStarted) append("_GUARD_START")
                if (eventTime == armed) append("_ARM")
            }
            eventTime < readFinished -> "DURING_READ"
            eventTime < guardStarted -> "BETWEEN_READ_GUARD"
            eventTime < armed -> "BETWEEN_GUARD_ARM"
            else -> "AFTER_ARM"
        }
        val deliveryAge = if (eventTime != null && eventTime in 1L..receipt)
            age(receipt - eventTime) else "UNKNOWN"
        val clock = if (eventTime == null) "HANDLED" else "RECEIVED"
        prefix += "${kind.name}${mask}_${role.name}_${timestamp}_${clock}_${age(receipt - armed)}_DELIVERY_${deliveryAge}"
    }

    @Synchronized
    fun finish(code: String) {
        if (outcome == "PENDING") outcome = code
    }

    @Synchronized
    fun summary(): String = "outcome=${outcome} count=${count} truncated=${count > prefix.size}" +
        " families=${families.joinToString("+")} prefix=${prefix.joinToString(">")}"

    private fun age(delta: Long): String = when {
        delta < 0 -> "UNKNOWN"
        delta < 16 -> "LT16"
        delta < 50 -> "LT50"
        delta < 150 -> "LT150"
        delta < 500 -> "LT500"
        else -> "LATER"
    }
}
