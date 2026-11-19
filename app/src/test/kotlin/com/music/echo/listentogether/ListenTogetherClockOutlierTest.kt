package echo.music.iad1tya.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherClockOutlierTest {
  private val epoch = 1_800_000_000_000L

  // One accepted sample per pair; timestamps include relay millisecond truncation.
  private fun probe(clock: ListenTogetherClock, round: Int, delayNs: Long) {
    val start = round * 10_000L * NS_PER_MS
    for (half in 0..1) {
      val sent = start + half * 25L * NS_PER_MS + 350_000
      val receive = sent + 1_100_000
      val reply = receive + 600_000
      val arrived = reply + delayNs
      val id = round * 2L + 2 + half
      clock.sent(id, sent, true)
      clock.pong(id, epoch + receive / NS_PER_MS, epoch + reply / NS_PER_MS, arrived)
    }
  }

  @Test
  fun isolatedDelayedPairDoesNotHideThreeFreshLowDelayPairs() {
    val clock = ListenTogetherClock()
    repeat(3) { probe(clock, it, 1_100_000) }
    probe(clock, 3, 21_100_000)
    assertTrue(
      clock.driftCorrection(4 * NS_PER_MS, 30_100L * NS_PER_MS, true) is DriftCorrection.AdjustSpeed
    )
    // A second delayed pair restores the conservative delay-based floor.
    probe(clock, 4, 21_100_000)
    assertEquals(
      DriftCorrection.None,
      clock.driftCorrection(4 * NS_PER_MS, 40_100L * NS_PER_MS, true),
    )
  }

  @Test
  fun oldLowDelayPairsDoNotOverrideRecentDelayedPairs() {
    val clock = ListenTogetherClock()
    repeat(3) { probe(clock, it, 1_100_000) }
    probe(clock, 4, 21_100_000)
    assertEquals(
      DriftCorrection.None,
      clock.driftCorrection(4 * NS_PER_MS, 40_100L * NS_PER_MS, true),
    )
  }

  @Test
  fun disagreeingLowDelayPairsStillWidenTolerance() {
    val clock = ListenTogetherClock()
    repeat(2) { probe(clock, it, 1_100_000) }
    // The third pair has a different but still low RTT and offset.
    probe(clock, 2, 10_000_000)
    probe(clock, 3, 21_100_000)
    assertEquals(
      DriftCorrection.None,
      clock.driftCorrection(3 * NS_PER_MS, 30_100L * NS_PER_MS, true),
    )
  }
}
