package echo.music.iad1tya.listentogether

import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToLong
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherNetworkTest {
  private enum class Link {
    LAN,
    WAN,
    MOBILE
  }

  private data class Probe(
    val sequence: Long,
    val sent: Long,
    val arrived: Long,
    val replied: Long,
    val received: Long
  )

  private class Results {
    val skew = mutableListOf<Long>()
    val continuousSkew = mutableListOf<Long>()
    var seeks = 0
    var speedTrims = 0
  }

  @Test
  fun playbackAcrossNetworkConditions() {
    for (link in Link.entries) {
      val results = Results()
      repeat(32) { runSession(link, it.toLong(), results) }
      val ordered = results.skew.sorted()
      val median = ordered[ordered.size / 2]
      val p95 = ordered[ordered.size * 95 / 100]
      val continuous = results.continuousSkew.sorted()
      val continuousMedian = continuous[continuous.size / 2]
      val continuousP95 = continuous[continuous.size * 95 / 100]
      println("$link heartbeat n=${ordered.size}, median=${median}ns, p95=${p95}ns")
      println(
        "$link 125ms sampling n=${continuous.size}, median=${continuousMedian}ns, p95=${continuousP95}ns, seeks=${results.seeks}, speed trims=${results.speedTrims}"
      )
      val budgetMs =
        when (link) {
          Link.LAN -> 5
          Link.WAN -> 15
          Link.MOBILE -> 55
        }
      if (link == Link.LAN) assertTrue("LAN median $continuousMedian", continuousMedian < NS_PER_MS)
      assertTrue("$link continuous p95 $continuousP95", continuousP95 < budgetMs * NS_PER_MS)
      assertTrue("$link heartbeat p95 $p95", p95 < budgetMs * NS_PER_MS)
      assertTrue("$link seeks ${results.seeks}", results.seeks < 32 * 3)
    }
  }

  // Sample the actual modelled guest position just before each correction. The two
  // player APIs truncate to milliseconds. The model excludes speaker/output latency.
  private fun runSession(link: Link, seed: Long, results: Results) {
    val random = Random(7001L + seed)
    val clock = ListenTogetherClock()
    val epochMs = 1_800_000_000_000L
    val phaseNs = random.nextInt(NS_PER_MS.toInt())
    val driftPpm = random.nextInt(181) - 90
    fun local(ns: Long) = ns + ns * driftPpm / 1_000_000
    fun stamp(ns: Long) = epochMs + (ns + phaseNs) / NS_PER_MS
    fun path(beat: Int, down: Boolean): Long {
      val base =
        when (link) {
          Link.LAN -> if (down) 900_000L else 800_000L
          Link.WAN -> 120 * NS_PER_MS
          Link.MOBILE ->
            if ((beat / 60) % 2 == 0) 6 * NS_PER_MS else (if (down) 95 else 65) * NS_PER_MS
        }
      val jitter = if (link == Link.MOBILE) 4_000_000 else 700_000
      val queue = if (link == Link.MOBILE && random.nextInt(12) == 0) 70 * NS_PER_MS else 0
      return base + random.nextInt(jitter) + queue
    }

    var at = 0L
    var position = 0L
    var rate = 1f
    var restoreAt = 0L
    var nextSample = 10_000 * NS_PER_MS
    fun guestPosition(time: Long): Long {
      val corrected = (minOf(time, restoreAt) - at).coerceAtLeast(0)
      return position + (time - at) + (corrected * (rate.toDouble() - 1)).roundToLong()
    }
    repeat(300) { beat ->
      val start = beat * 2_000 * NS_PER_MS
      // Switch paths each two minutes on mobile. Suspend probes for 110 s and lose
      // individual probes/heartbeats independently. All WebSocket deliveries stay ordered.
      if ((beat < 3 || beat % 5 == 0) && !(link == Link.MOBILE && beat in 100..154)) {
        var previousArrival = 0L
        var previousSend = 0L
        val probes =
          (0..1).map { half ->
            val t0 = start + half * 25 * NS_PER_MS + random.nextInt(200_000)
            val t1 = maxOf(previousArrival, t0 + path(beat, false))
            val t2 = maxOf(previousSend, t1 + random.nextInt(2_000_000))
            previousArrival = t1
            previousSend = t2
            Probe((beat + 1L) * 2 + half, t0, t1, t2, t2 + path(beat, true))
          }
        for (probe in probes) clock.sent(probe.sequence, local(probe.sent), paired = true)
        var lastReply = 0L
        for (probe in probes) {
          if (link == Link.MOBILE && random.nextInt(9) == 0) continue
          lastReply = maxOf(lastReply, probe.received)
          clock.pong(probe.sequence, stamp(probe.arrived), stamp(probe.replied), local(lastReply))
        }
      }

      val captured = start + 550 * NS_PER_MS + random.nextInt(NS_PER_MS.toInt())
      val received = captured + path(beat, true)
      val appliedAtHost = received + random.nextInt(4_000_000)
      val applied = local(appliedAtHost)
      while (nextSample <= appliedAtHost) {
        results.continuousSkew += abs(guestPosition(local(nextSample)) - nextSample)
        nextSample += 125 * NS_PER_MS
      }
      position = guestPosition(applied)
      at = applied
      // Short decoder stalls and one longer stall, independent of host progress.
      if (beat % 37 == 15) position -= (6 + random.nextInt(61)) * NS_PER_MS
      if (beat == 190) position -= 325 * NS_PER_MS
      if (link == Link.MOBILE && random.nextInt(15) == 0) return@repeat
      if (beat >= 5) results.skew += abs(position - appliedAtHost)

      val target =
        clock
          .anchor(captured / NS_PER_MS, true, stamp(captured), local(received))
          .positionNs(applied, -1)
      val error = target - (position / NS_PER_MS) * NS_PER_MS
      when (val correction = clock.driftCorrection(error, applied, true)) {
        DriftCorrection.None -> rate = 1f
        DriftCorrection.Seek -> {
          position = (target / NS_PER_MS) * NS_PER_MS
          rate = 1f
          results.seeks++
        }
        is DriftCorrection.AdjustSpeed -> {
          results.speedTrims++
          rate = correction.multiplier
          restoreAt = applied + correction.durationMs * NS_PER_MS
        }
      }
    }
  }
}
