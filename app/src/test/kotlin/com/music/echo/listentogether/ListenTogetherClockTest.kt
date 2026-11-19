package echo.music.iad1tya.listentogether

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class ListenTogetherClockTest {
  private val epoch = 1_800_000_000_000L
  private val clock = ListenTogetherClock()

  @Test
  fun roundTripExcludesServerQueueAndKeepsFractionalMilliseconds() {
    // 1.250001 ms each way and 37 ms waiting in the server's outgoing queue.
    val t0 = 9_000_000_000_000_000L
    val t3 = t0 + 39_500_002
    clock.sent(2, t0, paired = false)
    assertEquals(2_500_002L, clock.pong(2, epoch, epoch + 37, t3))
    val anchor = clock.anchor(45, true, epoch + 37, t3)
    assertEquals(46_250_001L, anchor.positionNs(t3, -1))
    assertEquals(46_250_002L, anchor.positionNs(t3 + 1, -1))
  }

  @Test
  fun pairsMayInterleaveAndReturnOutOfOrder() {
    for ((id, ms) in listOf(2L to 0, 3L to 25, 4L to 40, 5L to 65)) {
      clock.sent(id, ms * NS_PER_MS, paired = true)
    }
    assertNull(clock.pong(3, epoch + 35, epoch + 35, 80 * NS_PER_MS))
    assertNull(clock.pong(4, epoch + 50, epoch + 50, 85 * NS_PER_MS))
    assertEquals(40 * NS_PER_MS, clock.pong(5, epoch + 75, epoch + 75, 105 * NS_PER_MS))
    assertEquals(55 * NS_PER_MS, clock.pong(2, epoch + 10, epoch + 10, 110 * NS_PER_MS))
    assertNull(clock.pong(2, epoch, epoch, 111 * NS_PER_MS))
  }

  @Test
  fun queuedOrMissingHalfCannotContaminateEstimate() {
    clock.sent(2, 0, false)
    clock.pong(2, epoch + 10, epoch + 10, 20 * NS_PER_MS)
    clock.sent(4, 100 * NS_PER_MS, true)
    clock.sent(5, 125 * NS_PER_MS, true)
    clock.pong(4, epoch + 110, epoch + 110, 120 * NS_PER_MS)
    assertNull(clock.pong(5, epoch + 155, epoch + 155, 165 * NS_PER_MS))
    clock.sent(6, 200 * NS_PER_MS, true)
    clock.pong(6, epoch + 204, epoch + 204, 210 * NS_PER_MS)
    clock.sent(9, 240 * NS_PER_MS, true)
    assertNull(clock.pong(9, epoch + 249, epoch + 249, 260 * NS_PER_MS))
    assertEquals(epoch + 270, clock.serverNowMs(270 * NS_PER_MS))
    assertNull(clock.serverNowMs(100_000 * NS_PER_MS))
  }

  @Test
  fun barePongsProvideTransitOnlyAndExpire() {
    clock.sent(2, 100, false)
    assertEquals(29_000_002L, clock.pong(null, null, null, 29_000_102))
    assertTrue(clock.legacyPongs)
    assertNull(clock.serverNowMs(30_000_000))
    assertEquals(14_500_001L, clock.anchor(0, true, epoch, 30_000_000).transitNs)
    assertEquals(0L, clock.oneWayNs(100_000 * NS_PER_MS))
  }

  @Test
  fun legacySnapshotUsesLastLiveServerStampAndMeasuredTransit() {
    clock.sent(2, 0, false)
    clock.pong(null, null, null, 20 * NS_PER_MS)
    clock.anchor(0, true, epoch, 40 * NS_PER_MS)
    val later = 8_040 * NS_PER_MS
    val snapshot = clock.anchor(50, true, epoch + 2_000, later, snapshot = true)
    assertEquals(6_060 * NS_PER_MS, snapshot.positionNs(later, -1))
    assertNull(clock.serverNowMs(later))
  }

  @Test
  fun unsupportedDiscoveryCanRetryInTheNegotiatedFormat() {
    assertTrue(clock.sent(2, 0, false))
    assertFalse(clock.sent(4, NS_PER_MS, false))
    clock.discoveryRejected(payloadUnsupported = false)
    assertTrue(clock.sent(4, 2 * NS_PER_MS, false))
    assertEquals(2 * NS_PER_MS, clock.pong(4, epoch + 3, epoch + 3, 4 * NS_PER_MS))
  }

  @Test
  fun lostDiscoveryRetriesAfterExpiryWithoutWaitingForTheLegacyKeepalive() {
    var elapsedMs = 0L
    val sentAt = mutableListOf<Long>()
    for (sequence in 2L..12L step 2) {
      if (clock.sent(sequence, elapsedMs * NS_PER_MS, false)) sentAt += elapsedMs
      elapsedMs += clock.nextProbeIntervalMs(paired = false)
    }
    assertEquals(listOf(0L, 3_000L), sentAt)
  }

  @Test
  fun delayedBarePongLeavesTimestampedPairIntact() {
    clock.sent(2, 0, true)
    assertNull(clock.pong(null, null, null, 1 * NS_PER_MS))
    assertNull(clock.pong(2, epoch + 1, epoch + 1, 2 * NS_PER_MS))
    clock.sent(3, 25 * NS_PER_MS, true)
    assertEquals(2 * NS_PER_MS, clock.pong(3, epoch + 26, epoch + 26, 27 * NS_PER_MS))
    assertEquals(epoch + 28, clock.serverNowMs(28 * NS_PER_MS))
  }

  @Test
  fun onlyFreshConsistentLanSamplesEnableFineCorrection() {
    fun probe(id: Long, startMs: Long, outboundMs: Long, inboundMs: Long) {
      clock.sent(id, startMs * NS_PER_MS, false)
      val server = epoch + startMs + outboundMs
      clock.pong(id, server, server, (startMs + outboundMs + inboundMs) * NS_PER_MS)
    }
    fun correct(errorNs: Long, timeMs: Long) =
      clock.driftCorrection(errorNs, timeMs * NS_PER_MS, true)
    probe(2, 0, 1, 1)
    probe(4, 750, 1, 1)
    assertEquals(DriftCorrection.None, correct(4 * NS_PER_MS, 800))
    probe(6, 1_500, 1, 1)
    assertEquals(DriftCorrection.None, correct(900_000, 1_600))
    assertEquals(DriftCorrection.AdjustSpeed(1.008f, 500), correct(4 * NS_PER_MS, 1_600))
    assertEquals(DriftCorrection.None, correct(4 * NS_PER_MS, 40_000))
    probe(8, 40_100, 60, 30)
    assertEquals(DriftCorrection.None, correct(4 * NS_PER_MS, 40_200))
  }

  @Test
  fun bufferingConsumesTheLatestCommandAndAcknowledgesReadinessOnce() {
    val buffer = BufferedPlayback("song", PlaybackAnchor(0, true, 0), needsBarrier = true)
    val acks = mutableListOf<String>()
    assertNull(buffer.onPlayerReady("old song", acks::add))
    assertNull(buffer.onPlayerReady("song", acks::add))
    buffer.anchor = PlaybackAnchor(30_000, true, NS_PER_MS)
    buffer.anchor = PlaybackAnchor(30_010, false, 11 * NS_PER_MS)
    buffer.roomReady("old song")
    assertNull(buffer.onPlayerReady("song", acks::add))
    buffer.roomReady("song")
    val result = buffer.onPlayerReady("song", acks::add)!!
    assertEquals(listOf("song"), acks)
    assertEquals(30_010 * NS_PER_MS, result.positionNs(5_000 * NS_PER_MS, -1))
    val early = BufferedPlayback("next", PlaybackAnchor(500, true, 0), needsBarrier = true)
    early.roomReady("next")
    assertNull(early.onPlayerReady("song", acks::add))
    assertEquals(
      1_000 * NS_PER_MS,
      early.onPlayerReady("next", acks::add)!!.positionNs(500 * NS_PER_MS, -1),
    )
  }

  @Test
  fun invalidOrUnmatchedRepliesDoNotCalibrate() {
    clock.sent(2, 0, false)
    assertNull(clock.pong(3, epoch, epoch, NS_PER_MS))
    assertNull(clock.pong(2, epoch, epoch + 9, NS_PER_MS))
    clock.sent(4, 100, false)
    assertNull(clock.pong(4, Long.MAX_VALUE, Long.MAX_VALUE, 1_000))
    clock.sent(6, 100, false)
    assertNull(clock.pong(6, epoch, epoch, 1))
    assertNull(clock.serverNowMs(2_000))
  }

  @Test
  fun freshLowDelaySamplesEstimateClockFrequency() {
    // The server gains 100 ppm on this receiver. Queueing is symmetric.
    clock.sent(2, 0, false)
    clock.pong(2, epoch + 5, epoch + 5, 10 * NS_PER_MS)
    clock.sent(4, 40_000 * NS_PER_MS, false)
    clock.pong(4, epoch + 40_009, epoch + 40_009, 40_010 * NS_PER_MS)
    assertEquals(epoch + 70_017, clock.serverNowMs(70_010 * NS_PER_MS))
  }

  @Test
  fun bufferedPlayAdvancesToReadyTimeAndPauseRemainsFixed() {
    clock.sent(2, 0, false)
    clock.pong(2, epoch + 10, epoch + 10, 20 * NS_PER_MS)
    val received = 50 * NS_PER_MS
    val ready = received + 1_200 * NS_PER_MS + 321
    val play = clock.anchor(10_000, true, epoch + 40, received)
    assertEquals(11_210 * NS_PER_MS + 321, play.positionNs(ready, -1))
    val pause = clock.anchor(530, false, epoch + 45, received)
    assertEquals(530 * NS_PER_MS, pause.positionNs(ready, -1))
    assertEquals(11_000 * NS_PER_MS, play.positionNs(ready, 11_000))
  }

  @Test
  fun historicalSnapshotAndStaleLiveActionAreDistinct() {
    clock.sent(2, 0, false)
    clock.pong(2, epoch, epoch, 0)
    val received = 45_000 * NS_PER_MS
    assertEquals(
      received,
      clock.anchor(0, true, epoch, received, snapshot = true).positionNs(received, -1),
    )
    assertEquals(0L, clock.anchor(0, true, epoch, received).positionNs(received, -1))
    assertEquals(0L, clock.anchor(-1, true, Long.MAX_VALUE, received).positionNs(received, -1))
    assertEquals(
      Long.MAX_VALUE,
      clock.anchor(Long.MAX_VALUE, true, null, received).positionNs(received + 10, -1),
    )
  }

  @Test
  fun driftCorrectionHasADeadbandAndAdjustsInBothDirections() {
    assertEquals(
      DriftCorrection.None,
      DriftCorrection.forError(9 * NS_PER_MS, 12 * NS_PER_MS, true),
    )
    assertEquals(
      DriftCorrection.None,
      DriftCorrection.forError(45 * NS_PER_MS, 80 * NS_PER_MS, true),
    )
    assertEquals(
      DriftCorrection.AdjustSpeed(1.025f),
      DriftCorrection.forError(150 * NS_PER_MS, 0, true),
    )
    assertEquals(
      DriftCorrection.AdjustSpeed(.975f),
      DriftCorrection.forError(-150 * NS_PER_MS, 0, true),
    )
    assertEquals(DriftCorrection.Seek, DriftCorrection.forError(180 * NS_PER_MS, 0, true))
    assertEquals(DriftCorrection.Seek, DriftCorrection.forError(25 * NS_PER_MS, 0, false))
  }

  @Test
  fun asymmetricQueueSimulationBoundsPlayheadProjectionError() {
    val baseline = mutableListOf<Long>()
    val calibrated = mutableListOf<Long>()
    // Four minutes, one host heartbeat every two seconds, one probe pair every ten.
    // The guest wall clock is 120 ms fast and its monotonic oscillator gains 40 ppm.
    val random = java.util.Random(8173L)
    fun local(serverElapsedNs: Long) = serverElapsedNs + serverElapsedNs / 25_000
    for (beat in 0 until 120) {
      val start = beat * 2_000 * NS_PER_MS
      if (beat % 5 == 0) {
        for (half in 0..1) {
          val sequence = (beat + 1L) * 2 + half
          val t0 = start + half * 25 * NS_PER_MS + random.nextInt(650_000)
          val up = (if (beat % 7 == 3) 37 else 9) * NS_PER_MS + random.nextInt(1_800_000)
          val down = (if (beat % 8 == 2) 74 else 12) * NS_PER_MS + random.nextInt(2_200_000)
          val t1 = t0 + up + if (beat % 9 == 4 && half == 1) 13 * NS_PER_MS else 0
          val t2 = t1 + 3 * NS_PER_MS + random.nextInt(750_000)
          clock.sent(sequence, local(t0), true)
          clock.pong(sequence, epoch + t1 / NS_PER_MS, epoch + t2 / NS_PER_MS, local(t2 + down))
        }
      }
      val captured = start + 220 * NS_PER_MS + random.nextInt(1_000_000)
      val received =
        captured + (if (beat % 8 == 2) 74 else 12) * NS_PER_MS + random.nextInt(2_200_000)
      val applied = received + 4_325_112
      val capturedMs = captured / NS_PER_MS
      val corrected = clock.anchor(capturedMs, true, epoch + capturedMs, local(received))
      calibrated += abs(corrected.positionNs(local(applied), -1) - applied)
      val oldWallAtApplyMs = epoch + 120 + local(applied) / NS_PER_MS
      val oldTargetMs = capturedMs + (oldWallAtApplyMs - (epoch + capturedMs))
      baseline += abs(oldTargetMs * NS_PER_MS - applied)
    }
    val oldP95 = baseline.sorted()[113]
    val newP95 = calibrated.sorted()[113]
    println(
      "120 simulated heartbeats, p95 projected position error: old=${oldP95}ns new=${newP95}ns"
    )
    assertTrue(oldP95 > 119 * NS_PER_MS)
    assertTrue("p95 error: $newP95 ns", newP95 < 5 * NS_PER_MS)
  }
}
