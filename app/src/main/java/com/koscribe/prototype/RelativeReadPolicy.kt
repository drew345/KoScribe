package com.koscribe.prototype

/** Numeric diagnostic evidence only. No outcome of this policy grants insertion authority. */
internal object RelativeReadPolicy {
    enum class Shape { COLLAPSED, SELECTED, UNKNOWN, INVALID }
    enum class Source { NONE, INITIAL_ONLY, CALLBACK_CURRENT_GEN }
    enum class Composition { UNKNOWN, NONE_REPORTED, COMPOSING, INVALID }
    enum class Style { NOT_INSPECTED, UNSTYLED, NO_COMPOSING_REPORTED, COMPOSING }
    data class Selection(val generation: Int, val start: Int, val end: Int,
        val source: Source, val at: Long, val composition: Composition,
        val laterSignal: Boolean = false) {
        val shape: Shape get() = shape(start, end)
    }

    fun shape(start: Int, end: Int): Shape = when {
        start == -1 && end == -1 -> Shape.UNKNOWN
        start < 0 || end < 0 -> Shape.INVALID
        start == end -> Shape.COLLAPSED
        else -> Shape.SELECTED
    }

    fun composition(start: Int, end: Int): Composition = when {
        start == -1 && end == -1 -> Composition.NONE_REPORTED
        start >= 0 && end >= start -> Composition.COMPOSING
        else -> Composition.INVALID
    }

    fun permitsEmptyRead(selection: Selection?): Boolean = selection == null ||
        selection.shape == Shape.UNKNOWN ||
        (selection.shape == Shape.COLLAPSED && selection.start == 0)

    fun classify(length: Int, offset: Int, start: Int, end: Int, style: Style,
        selection: Selection?, now: Long): String {
        val payload = when { length == 0 -> "EMPTY"; length > 64 -> "OVERSIZE"; else -> "NONEMPTY" }
        val position = when { offset == -1 -> "UNKNOWN"; offset >= 0 -> "KNOWN"; else -> "INVALID" }
        val relative = if (start < 0 || end < 0 || start > length || end > length)
            Shape.INVALID else shape(start, end)
        val independent = selection?.shape ?: Shape.UNKNOWN
        val agreement = when {
            relative !in setOf(Shape.COLLAPSED, Shape.SELECTED) ||
                independent !in setOf(Shape.COLLAPSED, Shape.SELECTED) -> "UNKNOWN"
            relative == independent -> "SHAPE_AGREES"
            else -> "SHAPE_CONTRADICTS"
        }
        val age = when {
            selection == null || now < selection.at -> "UNKNOWN"
            now - selection.at <= 1000L -> "RECENT"
            else -> "OLDER"
        }
        val signal = when { selection == null -> "UNKNOWN"; selection.laterSignal -> "LATER_SIGNAL"
            else -> "NO_LATER_SIGNAL" }
        return "PAYLOAD=$payload OFFSET=$position RELATIVE=${relative.name} STYLE=${style.name}" +
            " INPUT_SELECTION=${independent.name} INPUT_SOURCE=${selection?.source ?: Source.NONE}" +
            " INPUT_AGE=$age INPUT_COMPOSITION=${selection?.composition ?: Composition.UNKNOWN}" +
            " INPUT_SIGNAL=$signal AGREEMENT=$agreement ELIGIBLE=NEVER"
    }
}

/** Main-thread metadata history. A callback was delivered in this generation, not proven remotely fresh.
 * Initial selection is never promoted to an update; later signals and age remain explicit diagnostics. */
internal class RelativeSelectionEvidence {
    private var current: RelativeReadPolicy.Selection? = null
    fun start(generation: Int, start: Int, end: Int, at: Long) {
        current = RelativeReadPolicy.Selection(generation, start, end,
            RelativeReadPolicy.Source.INITIAL_ONLY, at, RelativeReadPolicy.Composition.UNKNOWN)
    }
    fun update(generation: Int, start: Int, end: Int, candidatesStart: Int, candidatesEnd: Int, at: Long) {
        if (current?.generation != generation) return
        current = RelativeReadPolicy.Selection(generation, start, end,
            RelativeReadPolicy.Source.CALLBACK_CURRENT_GEN, at,
            RelativeReadPolicy.composition(candidatesStart, candidatesEnd))
    }
    fun signal() { current = current?.copy(laterSignal = true) }
    fun get(generation: Int): RelativeReadPolicy.Selection? = current?.takeIf { it.generation == generation }
    fun clear() { current = null }
}
