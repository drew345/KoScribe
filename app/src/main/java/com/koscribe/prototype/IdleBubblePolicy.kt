package com.koscribe.prototype

/** UI visibility only. This state never grants editor or insertion eligibility. */
internal class IdleBubblePolicy {
    enum class Ime { VISIBLE, ABSENT, UNKNOWN }

    private var hiddenUntilImeCycle = false

    fun hideUntilNextImeCycle() { hiddenUntilImeCycle = true }

    fun shouldShow(ime: Ime, ordinaryInputReady: Boolean, unlocked: Boolean,
        activeOwner: Boolean): Boolean {
        if (activeOwner) return true
        if (ime == Ime.ABSENT) hiddenUntilImeCycle = false
        return !hiddenUntilImeCycle && ime == Ime.VISIBLE && ordinaryInputReady && unlocked
    }
}

/** Screen coordinates; fractions are saved only after a completed user drag. */
internal object BubblePlacement {
    data class Area(val left: Int, val top: Int, val right: Int, val bottom: Int)
    data class Point(val x: Int, val y: Int)
    data class Anchor(val x: Float, val y: Float)

    fun clamp(point: Point, width: Int, height: Int, area: Area): Point {
        val maxX = (area.right - width).coerceAtLeast(area.left)
        val maxY = (area.bottom - height).coerceAtLeast(area.top)
        return Point(point.x.coerceIn(area.left, maxX), point.y.coerceIn(area.top, maxY))
    }

    fun anchor(point: Point, width: Int, height: Int, area: Area): Anchor {
        val safe = clamp(point, width, height, area)
        val spanX = (area.right - area.left - width).coerceAtLeast(0)
        val spanY = (area.bottom - area.top - height).coerceAtLeast(0)
        return Anchor(if (spanX == 0) 0f else (safe.x - area.left).toFloat() / spanX,
            if (spanY == 0) 0f else (safe.y - area.top).toFloat() / spanY)
    }

    fun resolve(saved: Anchor, width: Int, height: Int, area: Area): Point {
        val spanX = (area.right - area.left - width).coerceAtLeast(0)
        val spanY = (area.bottom - area.top - height).coerceAtLeast(0)
        val x = area.left + (saved.x.coerceIn(0f, 1f) * spanX).toInt()
        val y = area.top + (saved.y.coerceIn(0f, 1f) * spanY).toInt()
        return clamp(Point(x, y), width, height, area)
    }
}
