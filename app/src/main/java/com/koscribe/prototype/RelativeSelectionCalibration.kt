package com.koscribe.prototype

import java.util.concurrent.atomic.AtomicReference

/** The content request closure includes all token allocation and editor access. */
internal object RelativeProbeAccess {
    fun selectionOnly(arm: String): Boolean = arm == "FIXTURE_SELECTION" || arm == "KARROT_SELECTION"

    fun requestContent(debug: Boolean, arm: String, blocked: String, request: () -> String): String =
        if (debug && (arm == "FIXTURE" || arm == "KARROT")) request() else blocked
}

/** Main-thread callback writes, immutable atomic snapshots for dump-thread reads.
 * No Android/editor objects, text, arbitrary coordinates, or event history are retained.
 * Delivered callbacks are observations, never proof of remote freshness or write eligibility.
 */
internal class RelativeSelectionCalibration {
    enum class State { NO_INPUT, OUT_OF_SCOPE, TRACKING }
    enum class Package { NONE, OTHER, FIXTURE, KARROT }
    enum class Position { ORIGIN, NONZERO, RANGE, UNKNOWN, INVALID }
    enum class SampleRange { ZERO, CARET4, RANGE_0_4, OTHER, UNKNOWN, INVALID }
    enum class Start { NONE, FRESH, RESTART }
    enum class Signal { NONE, TEXT, SELECTION, CONTENT }
    enum class Order { NONE, BEFORE_SAMPLE, AFTER_SAMPLE }

    data class Snapshot(
        val state: State = State.NO_INPUT,
        val packageKind: Package = Package.NONE,
        val generation: Int = -1,
        val source: RelativeReadPolicy.Source = RelativeReadPolicy.Source.NONE,
        val shape: RelativeReadPolicy.Shape = RelativeReadPolicy.Shape.UNKNOWN,
        val position: Position = Position.UNKNOWN,
        val sampleRange: SampleRange = SampleRange.UNKNOWN,
        val composition: RelativeReadPolicy.Composition = RelativeReadPolicy.Composition.UNKNOWN,
        val sampleAt: Long? = null,
        val callbacksGen: Int = 0,
        val callbacksTotal: Int = 0,
        val starts: Int = 0,
        val finishes: Int = 0,
        val restarts: Int = 0,
        val lastStart: Start = Start.NONE,
        val signalsGen: Int = 0,
        val lastSignal: Signal = Signal.NONE,
        val signalOrder: Order = Order.NONE,
        val saturated: Boolean = false
    ) {
        fun code(now: Long): String {
            val age = when {
                sampleAt == null || now < sampleAt -> "UNKNOWN"
                now - sampleAt <= 1000L -> "RECENT"
                else -> "OLDER"
            }
            return "RELATIVE_SELECTION_V1 STATE=$state PACKAGE=$packageKind GENERATION=$generation" +
                " SOURCE=$source SHAPE=$shape POSITION=$position SAMPLE_RANGE=$sampleRange" +
                " COMPOSITION=$composition CALLBACKS_GEN=$callbacksGen CALLBACKS_TOTAL=$callbacksTotal" +
                " STARTS=$starts FINISHES=$finishes RESTARTS=$restarts LAST_START=$lastStart" +
                " SAMPLE_AGE=$age SIGNAL_ORDER=$signalOrder LAST_SIGNAL=$lastSignal SIGNALS_GEN=$signalsGen" +
                " COUNTERS=${if (saturated) "SATURATED" else "OK"} CONTENT_READ=DISABLED ELIGIBLE=NEVER"
        }
    }

    companion object { const val COUNTER_LIMIT = 10_000 }
    private val current = AtomicReference(Snapshot())
    fun snapshot(): Snapshot = current.get()

    private fun next(value: Int): Int = (value + 1).coerceAtMost(COUNTER_LIMIT)
    private fun publish(value: Snapshot) {
        current.set(value.copy(saturated = value.saturated || listOf(value.callbacksGen,
            value.callbacksTotal, value.starts, value.finishes, value.restarts, value.signalsGen)
            .any { it >= COUNTER_LIMIT }))
    }

    private fun cleared(old: Snapshot, state: State, packageKind: Package) = Snapshot(
        state = state, packageKind = packageKind, callbacksTotal = old.callbacksTotal,
        starts = old.starts, finishes = old.finishes, restarts = old.restarts, saturated = old.saturated)

    /** Call only for the configured target and a safe type, using supplied onStartInput metadata. */
    fun start(generation: Int, packageKind: Package, restarting: Boolean, start: Int, end: Int, now: Long) {
        require(packageKind == Package.FIXTURE || packageKind == Package.KARROT)
        val old = current.get()
        publish(sample(cleared(old, State.TRACKING, packageKind).copy(
            generation = generation, starts = next(old.starts),
            restarts = if (restarting) next(old.restarts) else old.restarts,
            lastStart = if (restarting) Start.RESTART else Start.FRESH), start, end, now).copy(
            source = RelativeReadPolicy.Source.INITIAL_ONLY))
    }

    fun outOfScope(packageKind: Package) {
        publish(cleared(current.get(), State.OUT_OF_SCOPE, packageKind))
    }

    fun finish() {
        val old = current.get()
        publish(cleared(old, State.NO_INPUT, Package.NONE).copy(
            finishes = if (old.state == State.TRACKING) next(old.finishes) else old.finishes))
    }

    fun update(generation: Int, start: Int, end: Int, candidatesStart: Int, candidatesEnd: Int, now: Long) {
        val old = current.get()
        if (old.state != State.TRACKING || old.generation != generation) return
        publish(sample(old, start, end, now).copy(
            source = RelativeReadPolicy.Source.CALLBACK_CURRENT_GEN,
            composition = RelativeReadPolicy.composition(candidatesStart, candidatesEnd),
            callbacksGen = next(old.callbacksGen), callbacksTotal = next(old.callbacksTotal),
            signalOrder = if (old.signalsGen == 0) Order.NONE else Order.BEFORE_SAMPLE))
    }

    fun signal(signal: Signal) {
        val old = current.get()
        if (old.state != State.TRACKING || signal == Signal.NONE) return
        publish(old.copy(signalsGen = next(old.signalsGen), lastSignal = signal,
            signalOrder = Order.AFTER_SAMPLE))
    }

    private fun sample(old: Snapshot, start: Int, end: Int, now: Long): Snapshot {
        val shape = RelativeReadPolicy.shape(start, end)
        val position = when (shape) {
            RelativeReadPolicy.Shape.UNKNOWN -> Position.UNKNOWN
            RelativeReadPolicy.Shape.INVALID -> Position.INVALID
            RelativeReadPolicy.Shape.SELECTED -> Position.RANGE
            RelativeReadPolicy.Shape.COLLAPSED -> if (start == 0) Position.ORIGIN else Position.NONZERO
        }
        val range = when {
            shape == RelativeReadPolicy.Shape.UNKNOWN -> SampleRange.UNKNOWN
            shape == RelativeReadPolicy.Shape.INVALID -> SampleRange.INVALID
            start == 0 && end == 0 -> SampleRange.ZERO
            start == 4 && end == 4 -> SampleRange.CARET4
            (start == 0 && end == 4) || (start == 4 && end == 0) -> SampleRange.RANGE_0_4
            else -> SampleRange.OTHER
        }
        return old.copy(shape = shape, position = position, sampleRange = range, sampleAt = now)
    }
}
