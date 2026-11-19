package echo.music.iad1tya.listentogether

import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal const val NS_PER_MS = 1_000_000L

/**
 * One clock per WebSocket, inspired by Freeman Jiang's BeatSync
 * (github.com/freeman-jiang/beatsync). Local timestamps use elapsedRealtimeNanos, including time
 * asleep.
 */
internal class ListenTogetherClock {
  private data class Probe(val sentNs: Long, val paired: Boolean)

  private data class Sample(
    val sentNs: Long,
    val receivedNs: Long,
    val serverReceiveNs: Long,
    val serverSendNs: Long,
    val rttNs: Long,
  ) {
    // Express the clock mapping at a nearby anchor, preserving integer nanoseconds.
    fun serverAt(localNs: Long): Long = serverSendNs + rttNs / 2 + (localNs - receivedNs)
  }

  private val probes = mutableMapOf<Long, Probe>()
  private val halves = mutableMapOf<Long, Sample>()
  private val samples = ArrayDeque<Sample>()
  private var warmupRounds = 0
  private var lastLiveServerNs: Long? = null
  private var lastLiveReceiptNs = 0L
  private var legacyRttNs: Long? = null
  private var legacyReceivedNs = 0L

  @Volatile
  var legacyPongs = false
    private set

  @Synchronized
  fun sent(sequence: Long, sentNs: Long, paired: Boolean): Boolean {
    probes.entries.removeAll { sentNs - it.value.sentNs >= MAX_RTT_NS }
    halves.entries.removeAll { sentNs - it.value.receivedNs > MAX_RTT_NS }
    if (!paired && probes.isNotEmpty()) return false
    probes[sequence] = Probe(sentNs, paired)
    if (!paired || sequence % 2 == 0L) warmupRounds = (warmupRounds + 1).coerceAtMost(4)
    return true
  }

  @Synchronized
  fun nextProbeIntervalMs(paired: Boolean): Long =
    when {
      warmupRounds < 4 -> 750L
      paired -> 10_000L
      else -> 25_000L
    }

  @Synchronized
  fun discoveryRejected(payloadUnsupported: Boolean) {
    probes.clear()
    legacyPongs = payloadUnsupported
  }

  /** A bare legacy pong can measure a single round trip but cannot establish a clock offset. */
  @Synchronized
  fun pong(sequence: Long?, serverReceiveMs: Long?, serverSendMs: Long?, receivedNs: Long): Long? {
    val echoedId = sequence?.takeIf { it > 0 }
    val id = echoedId ?: probes.keys.singleOrNull() ?: return null
    val probe = probes[id] ?: return null
    if (echoedId == null && probe.paired) return null
    probes.remove(id)
    val elapsed = receivedNs - probe.sentNs
    if (elapsed !in 0..MAX_RTT_NS) return null
    if (echoedId == null) {
      legacyPongs = true
      legacyRttNs = elapsed
      legacyReceivedNs = receivedNs
      return elapsed
    }

    val t1 = epochNs(serverReceiveMs) ?: return null
    val t2 = epochNs(serverSendMs) ?: return null
    val residence = t2 - t1
    // Unix millisecond truncation at the two server boundaries can add up to 1 ms.
    if (residence !in 0..(elapsed + NS_PER_MS)) return null
    val sample = Sample(probe.sentNs, receivedNs, t1, t2, (elapsed - residence).coerceAtLeast(0))
    legacyPongs = false
    val accepted =
      if (probe.paired) {
        // Even and odd sequence numbers form a pair. Replies may arrive in either order.
        val group = id / 2
        val other = halves.remove(group)
        if (other == null) {
          halves[group] = sample
          return null
        }
        val gapError =
          (sample.serverReceiveNs - other.serverReceiveNs) - (sample.sentNs - other.sentNs)
        if (abs(gapError) > 5 * NS_PER_MS) return null
        if (sample.rttNs <= other.rttNs) sample else other
      } else sample

    samples.addLast(accepted)
    if (samples.size > 12) samples.removeFirst()
    return accepted.rttNs
  }

  private fun best(nowNs: Long): Sample? {
    var best: Sample? = null
    for (sample in samples) {
      if (
        nowNs - sample.receivedNs in 0..MAX_AGE_NS &&
          sample.rttNs <= (best?.rttNs ?: Long.MAX_VALUE)
      ) {
        best = sample
      }
    }
    return best
  }

  private fun serverAt(nowNs: Long): Long? {
    val anchor = best(nowNs) ?: return null
    val nominal = anchor.serverAt(nowNs)
    // Fit frequency only between well-separated, similarly clean measurements. A
    // millisecond of server quantization over a short baseline looks like clock drift.
    val other =
      samples
        .filter {
          nowNs - it.receivedNs in 0..MAX_AGE_NS &&
            abs(it.receivedNs - anchor.receivedNs) >= 20_000 * NS_PER_MS &&
            it.rttNs <= anchor.rttNs + 2 * NS_PER_MS
        }
        .minByOrNull { it.rttNs } ?: return nominal
    val span = other.receivedNs - anchor.receivedNs
    val offsetChange = other.serverAt(other.receivedNs) - anchor.serverAt(other.receivedNs)
    if (abs(offsetChange) > abs(span) / 4_000) return nominal // 250 ppm
    val ratePpb = offsetChange * 1_000_000_000L / span
    return nominal + (nowNs - anchor.receivedNs) * ratePpb / 1_000_000_000L
  }

  @Synchronized fun serverNowMs(nowNs: Long): Long? = serverAt(nowNs)?.div(NS_PER_MS)

  @Synchronized
  fun oneWayNs(nowNs: Long): Long {
    val rtt =
      best(nowNs)?.rttNs ?: legacyRttNs?.takeIf { nowNs - legacyReceivedNs in 0..MAX_AGE_NS }
    return (rtt ?: 0L) / 2
  }

  @Synchronized
  fun driftCorrection(errorNs: Long, nowNs: Long, canAdjustSpeed: Boolean): DriftCorrection {
    val fallbackNs = (12 * NS_PER_MS + oneWayNs(nowNs) / 2).coerceAtMost(80 * NS_PER_MS)
    return DriftCorrection.forError(
      errorNs,
      correctionToleranceNs(nowNs, fallbackNs),
      canAdjustSpeed,
    )
  }

  private fun correctionToleranceNs(nowNs: Long, fallbackNs: Long): Long {
    // Round-trip time includes propagation. Three recent offset estimates show whether
    // successive playback targets are repeatable enough to correct a smaller difference.
    if (samples.size < 3) return fallbackNs
    // Wi-Fi retries can delay one probe while the other three still agree.
    val recent = samples.takeLast(4)
    val lowDelay = recent.filter { it.rttNs <= LOW_DELAY_RTT_NS }
    val window =
      if (lowDelay.size == 3 && recent.all { nowNs - it.receivedNs in 0..PRECISION_MAX_AGE_NS }) {
        lowDelay
      } else {
        recent.takeLast(3)
      }
    var minServer = Long.MAX_VALUE
    var maxServer = Long.MIN_VALUE
    var maxRtt = 0L
    for (sample in window) {
      if (nowNs - sample.receivedNs !in 0..PRECISION_MAX_AGE_NS) return fallbackNs
      val estimate = sample.serverAt(nowNs)
      minServer = minOf(minServer, estimate)
      maxServer = maxOf(maxServer, estimate)
      maxRtt = maxOf(maxRtt, sample.rttNs)
    }
    val playerFloor = if (maxRtt <= LOW_DELAY_RTT_NS) NS_PER_MS else 10 * NS_PER_MS
    return minOf(fallbackNs, playerFloor + (maxServer - minServer) / 2)
  }

  @Synchronized
  fun anchor(
    positionMs: Long,
    playing: Boolean,
    serverTimeMs: Long?,
    receivedNs: Long,
    snapshot: Boolean = false,
  ): PlaybackAnchor {
    val serverNs = epochNs(serverTimeMs)
    val oneWay = oneWayNs(receivedNs)
    val serverNow =
      serverAt(receivedNs)
        ?: lastLiveServerNs?.let {
          val elapsed = receivedNs - lastLiveReceiptNs
          if (elapsed in 0..MAX_AGE_NS) it + elapsed + oneWay else null
        }
    if (!snapshot && serverNs != null) {
      lastLiveServerNs = serverNs
      lastLiveReceiptNs = receivedNs
    }
    // A room snapshot can describe a position captured before this client joined. A
    // live action with an implausibly old timestamp should only get transit compensation.
    val maxAge = if (snapshot) TimeUnit.DAYS.toNanos(1) else MAX_RTT_NS
    val transit =
      if (serverNow != null && serverNs != null) {
        (serverNow - serverNs).takeIf { it in 0..maxAge } ?: oneWay
      } else oneWay
    return PlaybackAnchor(positionMs, playing, receivedNs, transit)
  }

  private fun epochNs(ms: Long?): Long? =
    ms?.takeIf { it in 1..(Long.MAX_VALUE / NS_PER_MS - 120_000) }?.times(NS_PER_MS)

  private companion object {
    const val MAX_RTT_NS = 3_000 * NS_PER_MS
    const val MAX_AGE_NS = 90_000 * NS_PER_MS
    const val PRECISION_MAX_AGE_NS = 35_000 * NS_PER_MS
    const val LOW_DELAY_RTT_NS = 12 * NS_PER_MS
  }
}

internal data class PlaybackAnchor(
  val positionMs: Long,
  val playing: Boolean,
  val receivedNs: Long,
  val transitNs: Long = 0,
) {
  fun positionNs(nowNs: Long, durationMs: Long): Long {
    val limit = if (durationMs > 0) TimeUnit.MILLISECONDS.toNanos(durationMs) else Long.MAX_VALUE
    val base = TimeUnit.MILLISECONDS.toNanos(positionMs.coerceAtLeast(0)).coerceAtMost(limit)
    if (!playing) return base
    val transit = transitNs.coerceIn(0, limit - base)
    return base + transit + (nowNs - receivedNs).coerceIn(0, limit - base - transit)
  }
}

/** The latest timeline wins while either the local player or the room is buffering. */
internal class BufferedPlayback(
  val trackId: String,
  var anchor: PlaybackAnchor,
  private val needsBarrier: Boolean,
) {
  private var complete = !needsBarrier
  private var readySent = false

  fun roomReady(track: String) {
    if (track == trackId) complete = true
  }

  fun onPlayerReady(track: String?, reportReady: (String) -> Unit): PlaybackAnchor? {
    if (track != trackId) return null
    if (needsBarrier && !readySent) {
      readySent = true
      reportReady(trackId)
    }
    return anchor.takeIf { complete }
  }
}

internal sealed interface DriftCorrection {
  data object None : DriftCorrection

  data object Seek : DriftCorrection

  data class AdjustSpeed(val multiplier: Float, val durationMs: Long = ADJUSTMENT_MS) :
    DriftCorrection

  companion object {
    const val ADJUSTMENT_MS = 2_000L

    fun forError(errorNs: Long, toleranceNs: Long, canAdjustSpeed: Boolean): DriftCorrection {
      if (errorNs in -toleranceNs..toleranceNs) return None
      if (!canAdjustSpeed || abs(errorNs) >= 180 * NS_PER_MS) return Seek
      val durationMs =
        if (toleranceNs < 4 * NS_PER_MS && abs(errorNs) <= 12 * NS_PER_MS) 500L else ADJUSTMENT_MS
      val fraction = (errorNs.toDouble() / (durationMs * NS_PER_MS)).coerceIn(-0.025, 0.025)
      return AdjustSpeed((1.0 + fraction).toFloat(), durationMs)
    }
  }
}
