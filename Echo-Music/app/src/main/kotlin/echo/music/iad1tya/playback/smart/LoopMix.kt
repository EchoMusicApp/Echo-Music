package echo.music.iad1tya.playback.smart

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The loop mix: DJ beat-matched loop mixing between outgoing and incoming tracks.
 */
object LoopMix {
    const val LOOP_BARS = 4
    private val LOOP_LENGTHS = intArrayOf(4, 2, 1)

    const val ENTER_BARS = 4
    const val EXIT_BARS = 2
    const val SETTLE_BARS = 2

    const val MAX_BPM_PER_BAR = 2.5
    const val MIN_RAMP_BARS = 2
    const val MAX_RAMP_BARS = 24

    const val MAX_SUNG_SHARE = 0.03
    const val MAX_SUNG_RUN_SEC = 0.2
    const val MIN_MASK_COVERAGE = 0.8
    const val LYRIC_GUARD_SEC = 0.3
    const val LYRIC_LEAD_SEC = 0.15
    const val LYRIC_TAIL_SEC = 0.35
    const val LYRIC_OPEN_LINE_SEC = 10.0
    const val OPEN_LINE_CERTAIN_SEC = 2.0

    const val MIN_LOUDNESS = 0.6
    const val SILENT_RMS = 0.01

    const val OUT_SEARCH_SEC = 60.0
    const val IN_SEARCH_SEC = 45.0
    const val HANDOVER_SEC = 0.025

    const val BASS_CUT_DB = -18.0
    const val HIGH_PASS_FROM_HZ = 20.0
    const val HIGH_PASS_TO_HZ = 1500.0

    enum class Check { LYRICS, MASK, LYRICS_AND_MASK }

    data class Span(val start: Double, val end: Double, val certainUntil: Double = end)
    data class Line(val timeMs: Long, val endMs: Long?, val blank: Boolean)

    data class Pick(
        val loopStart: Double,
        val loopEnd: Double,
        val bars: Int,
        val beatSec: Double,
        val bpm: Double,
        val vocal: Double,
        val checkedBy: Check,
    ) {
        val beats: Int get() = bars * 4
    }

    data class Plan(
        val out: Pick,
        val inn: Pick,
        val fold: Double,
        val enterBars: Int,
        val rampBars: Int,
        val exitBars: Int,
        val totalBars: Int,
    ) {
        val targetBpm: Double get() = inn.bpm / fold
    }

    fun plan(
        out: TrackAnalysis,
        outDuration: Double,
        inn: TrackAnalysis,
        inDuration: Double,
        earliestOutSec: Double,
        sungOut: List<Span>? = null,
        sungIn: List<Span>? = null,
        outVideo: Boolean = false,
        inVideo: Boolean = false,
        why: StringBuilder? = null,
    ): Plan? {
        if (outVideo || inVideo) return null.also { why?.append("a music video") }
        val outGrid = constantGrid(out) ?: return null.also { why?.append("no grid on the outgoing song") }
        val inGrid = constantGrid(inn) ?: return null.also { why?.append("no grid on the incoming song") }
        val outCount = Rejections()
        val inCount = Rejections()

        val outEnd = outDuration - 0.5
        val outFirst = max(earliestOutSec, outDuration - OUT_SEARCH_SEC)
        val outPick = firstClean(
            candidates = { bars -> loopsBetween(out.tailBeats, outGrid, outFirst, outEnd, bars).asReversed() },
            analysis = out,
            sung = sungOut,
            beatSec = outGrid.beatSec,
            count = outCount,
        ) ?: return null.also { why?.append("outgoing: ")?.append(outCount) }

        val inEnd = min(IN_SEARCH_SEC + LOOP_BARS * inGrid.barSec, inDuration - 20)
        val inPick = firstClean(
            candidates = { bars -> loopsBetween(inn.headBeats, inGrid, 0.0, inEnd, bars) },
            analysis = inn,
            sung = sungIn,
            beatSec = inGrid.beatSec,
            count = inCount,
        ) ?: return null.also { why?.append("incoming: ")?.append(inCount) }

        val outBeat = (outPick.end - outPick.start) / (outPick.bars * 4)
        val inBeat = (inPick.end - inPick.start) / (inPick.bars * 4)
        val outBpm = 60 / outBeat
        val inBpm = 60 / inBeat

        val fold = doubleArrayOf(1.0, 2.0, 0.5).minBy { abs(ln(inBpm / it / outBpm)) }
        val target = inBpm / fold
        val rampBars = ceil(abs(target - outBpm) / MAX_BPM_PER_BAR).toInt().coerceIn(MIN_RAMP_BARS, MAX_RAMP_BARS)
        val inLoopOutBars = inPick.bars / fold
        val totalBars = (ceil((ENTER_BARS + rampBars + EXIT_BARS) / inLoopOutBars) * inLoopOutBars).roundToInt()

        fun pick(clean: Clean, beatSec: Double, bpm: Double) = Pick(
            loopStart = clean.start,
            loopEnd = clean.end,
            bars = clean.bars,
            beatSec = beatSec,
            bpm = bpm,
            vocal = clean.vocal,
            checkedBy = clean.checkedBy,
        )
        return Plan(
            out = pick(outPick, outBeat, outBpm),
            inn = pick(inPick, inBeat, inBpm),
            fold = fold,
            enterBars = ENTER_BARS,
            rampBars = rampBars,
            exitBars = totalBars - ENTER_BARS - rampBars,
            totalBars = totalBars,
        )
    }

    private class Grid(val beatSec: Double, val anchor: Double) {
        val barSec: Double get() = beatSec * 4
    }

    private fun constantGrid(analysis: TrackAnalysis): Grid? {
        val beat = analysis.beatInterval.takeIf { it.isFinite() && it > 0 }
            ?: analysis.bpm.takeIf { it.isFinite() && it > 0 }?.let { 60 / it }
            ?: return null
        val anchor = analysis.downbeats.firstOrNull { it.isFinite() } ?: analysis.firstBeat
        if (!anchor.isFinite()) return null
        return Grid(beat, anchor)
    }

    private open class Loop(val start: Double, val end: Double)

    private class Clean(start: Double, end: Double, val bars: Int, val vocal: Double, val checkedBy: Check) :
        Loop(start, end)

    private fun loopsBetween(side: BeatSide?, grid: Grid, from: Double, to: Double, bars: Int): List<Loop> {
        val loops = ArrayList<Loop>()
        if (side != null && side.bpm > 0 && side.downbeats.size >= bars + 1) {
            val beat = 60 / side.bpm
            val downs = side.downbeats
            for (start in downs) {
                if (start < from) continue
                val target = start + bars * 4 * beat
                val end = nearest(downs, target)
                if (abs(end - target) > beat / 4 || end > to) continue
                loops += Loop(start, end)
            }
            if (loops.isNotEmpty()) return loops
        }
        if (to < from) return loops
        val bar = grid.barSec
        val length = bars * bar
        var t = grid.anchor + ceil((from - grid.anchor) / bar) * bar
        while (t + length <= to) {
            loops += Loop(t, t + length)
            t += bar
        }
        return loops
    }

    private fun nearest(sorted: List<Double>, target: Double): Double {
        var low = 0
        var high = sorted.size - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (sorted[mid] < target) low = mid + 1 else high = mid
        }
        val after = sorted[low]
        val before = sorted.getOrNull(low - 1) ?: return after
        return if (abs(before - target) <= abs(after - target)) before else after
    }

    private class Rejections {
        var loops = 0
        var quiet = 0
        var lyrics = 0
        var unmasked = 0
        var sung = 0

        override fun toString() = if (loops == 0) {
            "no bar lines in the search"
        } else {
            "$loops loops: $quiet quiet, $lyrics sung (lyrics), $sung sung (mask), $unmasked unmeasured"
        }
    }

    private fun firstClean(
        candidates: (Int) -> List<Loop>,
        analysis: TrackAnalysis,
        sung: List<Span>?,
        beatSec: Double,
        count: Rejections,
    ): Clean? {
        for (bars in LOOP_LENGTHS) {
            val loops = candidates(bars)
            if (loops.isEmpty()) continue
            count.loops += loops.size
            val louds = DoubleArray(loops.size) { quietestBar(analysis.levelWindows, loops[it], bars) }
            val known = louds.filter { !it.isNaN() }.sorted()
            val typical = if (known.isEmpty()) Double.NaN else known[min(known.size - 1, floor(known.size * 0.6).toInt())]
            for ((index, loop) in loops.withIndex()) {
                val loud = louds[index]
                if (!loud.isNaN() && (loud < typical * MIN_LOUDNESS || loud <= SILENT_RMS)) {
                    count.quiet++
                    continue
                }
                if (sung != null) {
                    val from = loop.start - LYRIC_GUARD_SEC
                    val touching = sung.filter { it.start < loop.end && it.end > from }
                    if (touching.any { it.start < loop.end && it.certainUntil > from }) {
                        count.lyrics++
                        continue
                    }
                    if (touching.isEmpty()) return Clean(loop.start, loop.end, bars, 0.0, Check.LYRICS)
                    val mask = maskStats(analysis.vocalWindows, loop.start - beatSec, loop.end)
                    if (mask == null) {
                        count.unmasked++
                        continue
                    }
                    if (mask.share > MAX_SUNG_SHARE || mask.longestRunSec > MAX_SUNG_RUN_SEC) {
                        count.sung++
                        continue
                    }
                    return Clean(loop.start, loop.end, bars, mask.share, Check.LYRICS_AND_MASK)
                }
                val mask = maskStats(analysis.vocalWindows, loop.start - beatSec, loop.end)
                if (mask == null) {
                    count.unmasked++
                    continue
                }
                if (mask.share > MAX_SUNG_SHARE || mask.longestRunSec > MAX_SUNG_RUN_SEC) {
                    count.sung++
                    continue
                }
                return Clean(loop.start, loop.end, bars, mask.share, Check.MASK)
            }
        }
        return null
    }

    private fun quietestBar(windows: List<CurveWindow>, loop: Loop, bars: Int): Double {
        if (windows.isEmpty()) return Double.NaN
        var quietest = Double.POSITIVE_INFINITY
        val bar = (loop.end - loop.start) / bars
        for (b in 0 until bars) {
            val rms = meanRms(windows, loop.start + b * bar, loop.start + (b + 1) * bar)
            if (rms.isNaN()) return Double.NaN
            quietest = min(quietest, rms)
        }
        return quietest
    }

    private fun meanRms(windows: List<CurveWindow>, from: Double, to: Double): Double {
        var sumSquares = 0.0
        var count = 0
        for (window in windows) {
            val a = max(0, ceil((from - window.startTime) * window.frameRate).toInt())
            val b = min(window.values.size, floor((to - window.startTime) * window.frameRate).toInt())
            for (i in a until b) {
                val rms = rmsFromLevel(window.values[i].toDouble())
                sumSquares += rms * rms
                count++
            }
        }
        return if (count == 0) Double.NaN else sqrt(sumSquares / count)
    }

    data class MaskStats(val share: Double, val longestRunSec: Double)

    fun maskStats(windows: List<CurveWindow>, from: Double, to: Double): MaskStats? {
        var sung = 0
        var n = 0
        var covered = 0.0
        var run = 0
        var longest = 0
        var fps = 1.0
        for (window in windows) {
            val a = max(0, ceil((from - window.startTime) * window.frameRate).toInt())
            val b = min(window.values.size, floor((to - window.startTime) * window.frameRate).toInt())
            for (i in a until b) {
                n++
                if (window.values[i] > VOCAL_ACTIVE_THRESHOLD) {
                    sung++
                    run++
                    longest = max(longest, run)
                } else {
                    run = 0
                }
            }
            if (b > a) {
                covered += (b - a) / window.frameRate
                fps = window.frameRate
            }
        }
        return if (n > 0 && covered >= (to - from) * MIN_MASK_COVERAGE) {
            MaskStats(sung.toDouble() / n, longest / fps)
        } else {
            null
        }
    }

    fun sungSpans(lines: List<Line>): List<Span> {
        val sorted = lines.sortedBy { it.timeMs }
        val spans = ArrayList<Span>()
        for ((index, line) in sorted.withIndex()) {
            if (line.blank) continue
            val start = line.timeMs / 1000.0
            val next = sorted.subList(index + 1, sorted.size).firstOrNull { it.timeMs > line.timeMs }?.timeMs
            val stated = line.endMs != null && line.endMs > line.timeMs
            val end = if (stated) {
                line.endMs!! / 1000.0
            } else {
                min(start + LYRIC_OPEN_LINE_SEC, (next ?: (line.timeMs + 10_000)) / 1000.0)
            }
            val certain = if (stated) end else min(end, start + OPEN_LINE_CERTAIN_SEC)
            spans += Span(start - LYRIC_LEAD_SEC, end + LYRIC_TAIL_SEC, certain + LYRIC_TAIL_SEC)
        }
        return spans
    }

    class Clock(val plan: Plan) {
        val beats: Int = plan.totalBars * 4
        val settleBeats: Int = (SETTLE_BARS * 4 / plan.fold).roundToInt()
        val rampFrom: Int = plan.enterBars * 4
        val rampTo: Int = (plan.enterBars + plan.rampBars) * 4

        fun tempo(k: Int): Double = when {
            k < rampFrom -> plan.out.bpm
            k >= rampTo -> plan.targetBpm
            else -> plan.out.bpm + (plan.targetBpm - plan.out.bpm) * (k - rampFrom + 0.5) / (rampTo - rampFrom)
        }

        val beatTimes: DoubleArray = DoubleArray(beats + settleBeats + 1).also { times ->
            for (k in 0 until beats + settleBeats) times[k + 1] = times[k] + 60 / tempo(k)
        }

        val enterEnd: Double get() = beatTimes[rampFrom]
        val rampEnd: Double get() = beatTimes[rampTo]
        val handoffAt: Double get() = beatTimes[(rampFrom + rampTo) / 2]
        val swapBar: Double get() = 4 * 60 / tempo((rampFrom + rampTo) / 2)
        val releaseAt: Double get() = beatTimes[beats]
        val elementAt: Double get() = beatTimes[beats + settleBeats]
        val elementTrackTime: Double get() = plan.inn.loopEnd + (elementAt - releaseAt)

        fun outPositionAt(k: Int): Double = plan.out.loopStart + k * plan.out.beatSec
        fun inPositionAt(k: Int): Double = plan.inn.loopStart + k * plan.fold * plan.inn.beatSec
        val inReleasePosition: Double get() = inPositionAt(beats)

        fun beatAt(t: Double): Pair<Int, Double> {
            if (t < 0) return -1 to 0.0
            var low = 0
            var high = beatTimes.size - 1
            while (low < high) {
                val mid = (low + high + 1) ushr 1
                if (beatTimes[mid] <= t) low = mid else high = mid - 1
            }
            val k = low
            if (k + 1 >= beatTimes.size) return k to 0.0
            return k to min(1.0, (t - beatTimes[k]) / (beatTimes[k + 1] - beatTimes[k]))
        }
    }

    enum class Side { OUT, IN }

    fun positionAt(clock: Clock, side: Side, t: Double): Double? {
        val plan = clock.plan
        if (t < 0) return null
        if (side == Side.IN && t >= clock.releaseAt) return plan.inn.loopEnd + (t - clock.releaseAt)
        if (side == Side.OUT && t >= clock.releaseAt) return null
        val (k, frac) = clock.beatAt(t)
        if (k >= clock.beats) return null
        return if (side == Side.OUT) {
            plan.out.loopStart + ((k % plan.out.beats) + frac) * plan.out.beatSec
        } else {
            plan.inn.loopStart + (((k * plan.fold) % plan.inn.beats) + frac * plan.fold) * plan.inn.beatSec
        }
    }

    fun beatPhaseAt(clock: Clock, t: Double): Double? {
        if (t < 0 || t >= clock.beatTimes.last()) return null
        return clock.beatAt(t).second
    }
}
