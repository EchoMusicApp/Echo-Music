package echo.music.iad1tya.listentogether

import java.util.Locale
import java.util.PriorityQueue
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToLong
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherWlanTest {
  @Test fun quietWlan() = checkSkew(bursty = false, p95BudgetMs = 7.0)

  @Test fun burstyWlan() = checkSkew(bursty = true, p95BudgetMs = 9.5)

  private fun checkSkew(bursty: Boolean, p95BudgetMs: Double) {
    val results = Results()
    repeat(SESSIONS) { seed -> Session(7103L + seed, bursty, results).run() }
    val all = results.skew.sorted()
    val steady = results.steadySkew.sorted()
    val stalls = results.stalledSkew.sorted()
    val transit = results.heartbeatTransit.sorted()
    val name = if (bursty) "Bursty WLAN" else "Quiet WLAN"
    val summary =
      "$name, ${all.size} samples at ${SAMPLE_INTERVAL_MS} ms, ${quantiles(all)}, " +
        "p99=${ms(percentile(all, 99))} ms, max=${ms(all.last())} ms"
    println(summary)
    println(
      "$name, ${steady.size} samples outside player stalls and 4 s recovery windows, ${quantiles(steady)}"
    )
    println(
      "$name, ${stalls.size} samples during simulated player stalls, " +
        "p95=${ms(percentile(stalls, 95))} ms, max=${ms(stalls.last())} ms, " +
        "${results.seeks} playback seeks after startup, ${results.trims} speed trims"
    )
    println(
      "$name, ${transit.size} heartbeat deliveries, ${quantiles(transit)}, max=${ms(transit.last())} ms, " +
        "${results.untimestampedHeartbeats} sent without a capture timestamp"
    )
    assertTrue("Expected to observe the severe stalls", stalls.last() > 200 * NS_PER_MS)
    assertTrue(
      summary,
      percentile(all, 50) <= NS_PER_MS && percentile(all, 95) <= p95BudgetMs * NS_PER_MS,
    )
    if (bursty) {
      val contended = results.contendedSkew.sorted()
      val contentionSummary =
        "$name, ${contended.size} samples during contention, ${quantiles(contended)}, " +
          "MAC retries=${results.retries}, power-save deferrals=${results.powerSaveDeferrals}"
      println(contentionSummary)
      assertTrue(
        "Expected repeated retries and power-save deferrals",
        results.retries > 0 && results.powerSaveDeferrals > 0,
      )
      assertTrue("Expected substantial time spent in contention", contended.size > all.size / 5)
      assertTrue(contentionSummary, percentile(contended, 95) <= 6_500_000)
    }
  }

  private class Results {
    val skew = mutableListOf<Long>()
    val contendedSkew = mutableListOf<Long>()
    val steadySkew = mutableListOf<Long>()
    val stalledSkew = mutableListOf<Long>()
    val heartbeatTransit = mutableListOf<Long>()
    var untimestampedHeartbeats = 0
    var retries = 0
    var powerSaveDeferrals = 0
    var seeks = 0
    var trims = 0
  }

  // Both stations use one half-duplex AP. The Metroserver is connected to it by LAN.
  // The virtual network and relay let each run replay the same traffic. The measured
  // positions come from virtual media clocks before the devices' audio output pipelines.
  private class Session(seed: Long, bursty: Boolean, private val results: Results) {
    private val timeline = Timeline()
    private val random = Random(seed)
    private val host = Station(timeline, random)
    private val guest = Station(timeline, random)
    private val serverPhase = random.nextInt(NS_PER_MS.toInt())
    private val ap = AccessPoint(timeline, seed, random, bursty, results)
    private val hostUp = Stream(ap, HOST_UP)
    private val hostDown = Stream(ap, HOST_DOWN)
    private val guestUp = Stream(ap, GUEST_UP)
    private val guestDown = Stream(ap, GUEST_DOWN)
    private val mainThread = Random(seed + 42)
    private var recoveryUntil = 0L

    fun run() {
      startProbes(host, hostUp, hostDown)
      timeline.at(173 * NS_PER_MS) { startProbes(guest, guestUp, guestDown) }
      val start = 4_000 * NS_PER_MS + random.nextInt(NS_PER_MS.toInt())
      timeline.at(start) {
        host.player.playing = true
        heartbeat()
      }
      // A slow refill stops the playhead for its whole duration. Keep measuring through
      // these stalls and while the guest waits for its next actionable heartbeat.
      for ((afterMs, lengthMs) in
        listOf(28_000L to 35L, 73_000L to 62L, 132_000L to 265L, 191_000L to 87L)) {
        val stalledAt = start + (afterMs + random.nextInt(5_000)) * NS_PER_MS
        timeline.at(stalledAt) {
          guest.player.stalled = true
          recoveryUntil = stalledAt + (lengthMs + 4_000) * NS_PER_MS
        }
        timeline.at(stalledAt + lengthMs * NS_PER_MS) { guest.player.stalled = false }
      }
      val end = start + 240_000 * NS_PER_MS
      for (at in start until end step SAMPLE_INTERVAL_MS * NS_PER_MS) {
        timeline.at(at) {
          val error = abs(host.player.positionNs() - guest.player.positionNs())
          results.skew += error
          if (ap.contention(at)) results.contendedSkew += error
          if (guest.player.stalled) results.stalledSkew += error
          if (at >= recoveryUntil) results.steadySkew += error
        }
      }
      timeline.runUntil(end)
    }

    private fun startProbes(station: Station, up: Stream, down: Stream, sequence: Long = 2) {
      val paired = station.clock.serverNowMs(station.localNow()) != null
      fun sendProbe(id: Long) {
        if (!station.clock.sent(id, station.localNow(), paired)) return
        up.send(id) {
          val arrivedMs = stamp()
          timeline.at(timeline.now + relayDispatch(id)) {
            // The server stamps its pong when starting the socket write.
            val sentMs = stamp()
            down.send(id) { station.clock.pong(id, arrivedMs, sentMs, station.localNow()) }
          }
        }
      }
      fun nextRound() {
        val delay = station.clock.nextProbeIntervalMs(paired) * NS_PER_MS
        timeline.at(timeline.now + station.elapsed(delay)) {
          startProbes(station, up, down, sequence + 2)
        }
      }
      sendProbe(sequence)
      if (paired) {
        timeline.at(timeline.now + station.elapsed(25 * NS_PER_MS)) {
          sendProbe(sequence + 1)
          nextRound()
        }
      } else {
        nextRound()
      }
    }

    private fun heartbeat(number: Long = 0) {
      val captured = timeline.now
      val positionMs = host.player.positionNs() / NS_PER_MS
      val captureMs = host.clock.serverNowMs(host.localNow())
      if (captureMs == null) results.untimestampedHeartbeats++
      val id = 10_000L + number // Distinguish airtime randomness for probes and heartbeats.
      hostUp.send(id) {
        // Metroserver advances PLAY from the capture timestamp and broadcasts the
        // adjusted position with its own room timestamp. It clears the capture field.
        val serverMs = stamp()
        val transitMs =
          if (captureMs != null) serverMs - captureMs
          else host.clock.oneWayNs(host.localNow()) / NS_PER_MS
        val relayedMs = positionMs + if (transitMs in 1..30_000) transitMs else 0
        timeline.at(timeline.now + relayDispatch(id)) {
          guestDown.send(id) {
            results.heartbeatTransit += timeline.now - captured
            val anchor = guest.clock.anchor(relayedMs, true, serverMs, guest.localNow())
            timeline.at(timeline.now + mainThread.nextInt(3_000_000)) { alignGuest(anchor) }
          }
        }
      }
      timeline.at(timeline.now + host.elapsed(2_000 * NS_PER_MS)) { heartbeat(number + 1) }
    }

    private fun alignGuest(anchor: PlaybackAnchor) {
      val player = guest.player
      val now = guest.localNow()
      val target = anchor.positionNs(now, -1)
      val error = target - player.positionNs() / NS_PER_MS * NS_PER_MS
      val correction =
        if (!player.playing && abs(error) >= NS_PER_MS) {
          DriftCorrection.Seek
        } else {
          guest.clock.driftCorrection(error, now, player.playing && !player.stalled)
        }
      player.trim(1f)
      when (correction) {
        DriftCorrection.None -> Unit
        DriftCorrection.Seek -> {
          if (player.playing) results.seeks++
          player.seek(target / NS_PER_MS)
        }
        is DriftCorrection.AdjustSpeed -> {
          results.trims++
          val revision = player.trim(correction.multiplier)
          timeline.at(timeline.now + guest.elapsed(correction.durationMs * NS_PER_MS)) {
            if (player.trimRevision == revision) player.trim(1f)
          }
        }
      }
      player.playing = true
    }

    private fun stamp() = 1_800_000_000_000L + (timeline.now + serverPhase) / NS_PER_MS

    private fun relayDispatch(id: Long) = 100_000L + (id * 205_019 + serverPhase) % 850_000
  }

  private class Station(timeline: Timeline, random: Random) {
    val clock = ListenTogetherClock()
    val player =
      Player(timeline, random.nextInt(121) - 60, random.nextInt(NS_PER_MS.toInt()).toLong())
    private val timeline = timeline
    private val offset =
      (30_000L + random.nextInt(10_000_000)) * NS_PER_MS + random.nextInt(NS_PER_MS.toInt())
    private val ppm = random.nextInt(161) - 80

    fun localNow() = offset + timeline.now + timeline.now * ppm / 1_000_000

    fun elapsed(localNs: Long) = localNs * 1_000_000 / (1_000_000 + ppm)
  }

  private class Player(
    private val timeline: Timeline,
    private val ppm: Int,
    initialPosition: Long,
  ) {
    private var position = initialPosition
    private var at = 0L
    private var speed = 1f
    var trimRevision = 0
      private set

    var playing = false
      set(value) {
        savePosition()
        field = value
      }

    var stalled = false
      set(value) {
        savePosition()
        field = value
      }

    fun positionNs(): Long {
      if (!playing || stalled) return position
      return position + ((timeline.now - at) * (1 + ppm / 1_000_000.0) * speed).roundToLong()
    }

    fun seek(positionMs: Long) {
      at = timeline.now
      position = positionMs * NS_PER_MS
    }

    fun trim(multiplier: Float): Int {
      savePosition()
      speed = multiplier
      return ++trimRevision
    }

    private fun savePosition() {
      position = positionNs()
      at = timeline.now
    }
  }

  private class Timeline {
    private class Event(val at: Long, val order: Long, val act: () -> Unit)

    private val pending = PriorityQueue(compareBy<Event> { it.at }.thenBy { it.order })
    private var order = 0L
    var now = 0L
      private set

    fun at(time: Long, act: () -> Unit) {
      pending += Event(time, order++, act)
    }

    fun runUntil(end: Long) {
      while (true) {
        val event = pending.peek() ?: return
        if (event.at > end) return
        pending.remove()
        now = event.at
        event.act()
      }
    }
  }

  // Serialize each WebSocket direction through retry/deferral, as TCP delivers in order.
  private class Stream(private val ap: AccessPoint, private val direction: Int) {
    private data class Packet(val id: Long, val deliver: () -> Unit)

    private val queue = ArrayDeque<Packet>()

    fun send(id: Long, deliver: () -> Unit) {
      queue.addLast(Packet(id, deliver))
      if (queue.size == 1) sendNext()
    }

    private fun sendNext() {
      val packet = queue.first()
      ap.transmit(direction, packet.id) {
        packet.deliver()
        queue.removeFirst()
        if (queue.isNotEmpty()) sendNext()
      }
    }
  }

  private class AccessPoint(
    private val timeline: Timeline,
    private val seed: Long,
    random: Random,
    private val bursty: Boolean,
    private val results: Results,
  ) {
    private val phase = random.nextInt(17_000) * NS_PER_MS
    private val beaconPhases = LongArray(2) { random.nextInt(102_400_000).toLong() }
    private val directionDelay = LongArray(4) { 160_000L + random.nextInt(400_000) }
    private var sharedRadioFree = 0L

    // Run the background stream for five seconds per 17-second cycle. Its short slots
    // approximate queued airtime from other stations on the same channel.
    fun contention(now: Long) = bursty && (now + phase) % (17_000 * NS_PER_MS) < 5_000 * NS_PER_MS

    fun transmit(direction: Int, id: Long, deliver: () -> Unit) {
      val packetRandom = Random(seed * 216_091 + id * 9_973 + direction * 22_537)
      val now = timeline.now
      val station = direction / 2
      val cycle = (now + phase) / (17_000 * NS_PER_MS)
      val beaconPhase = (now + beaconPhases[station]) % 102_400_000
      val waitForBeacon =
        if (
          contention(now) &&
            direction % 2 == 1 &&
            cycle % 4 == station.toLong() &&
            beaconPhase >= 32_000_000
        )
          102_400_000 - beaconPhase
        else 0
      if (waitForBeacon > 0) results.powerSaveDeferrals++

      fun attempt(ready: Long, retry: Int) {
        timeline.at(ready) {
          val allowed = maxOf(ready, sharedRadioFree)
          val airtimeSlot = (allowed + phase) % 3_500_000
          val waitForTraffic =
            if (contention(allowed)) (2_650_000 - airtimeSlot).coerceAtLeast(0) else 0
          val airtimeStart = allowed + waitForTraffic
          if (airtimeStart > ready) {
            attempt(airtimeStart, retry)
          } else {
            sharedRadioFree = ready + 160_000 + packetRandom.nextInt(170_000)
            val retryChance = if (contention(ready)) 36 else 1
            if (retry < 4 && packetRandom.nextInt(100) < retryChance) {
              results.retries++
              val backoff = packetRandom.nextInt(800_000 shl retry)
              attempt(sharedRadioFree + backoff, retry + 1)
            } else {
              timeline.at(sharedRadioFree + 80_000 + packetRandom.nextInt(180_000), deliver)
            }
          }
        }
      }
      attempt(now + waitForBeacon + directionDelay[direction] + packetRandom.nextInt(420_000), 0)
    }
  }

  private companion object {
    const val SESSIONS = 24
    const val SAMPLE_INTERVAL_MS = 20L
    const val HOST_UP = 0
    const val HOST_DOWN = 1
    const val GUEST_UP = 2
    const val GUEST_DOWN = 3

    fun percentile(sorted: List<Long>, percent: Int) =
      sorted[(sorted.size * percent + 99) / 100 - 1]

    fun quantiles(sorted: List<Long>) =
      "median=${ms(percentile(sorted, 50))} ms, p95=${ms(percentile(sorted, 95))} ms"

    fun ms(ns: Long) = String.format(Locale.ROOT, "%.3f", ns.toDouble() / NS_PER_MS)
  }
}
