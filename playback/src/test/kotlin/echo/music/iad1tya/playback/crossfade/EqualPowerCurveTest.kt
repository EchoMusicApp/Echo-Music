package echo.music.iad1tya.playback.crossfade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EqualPowerCurveTest {
  @Test
  fun gainsStartAndEndAtExpectedValues() {
    val start = EqualPowerCurve.calculateGains(0f)
    assertEquals(1f, start.outgoingGain, 0.001f)
    assertEquals(0f, start.incomingGain, 0.001f)

    val end = EqualPowerCurve.calculateGains(1f)
    assertEquals(0f, end.outgoingGain, 0.001f)
    assertEquals(1f, end.incomingGain, 0.001f)
  }

  @Test
  fun gainsMaintainApproximatelyConstantPowerAcrossTransition() {
    for (i in 0..100) {
      val progress = i / 100f
      val gains = EqualPowerCurve.calculateGains(progress)
      val totalPower =
        gains.outgoingGain * gains.outgoingGain +
          gains.incomingGain * gains.incomingGain
      assertTrue("Unexpected power $totalPower at $progress", abs(totalPower - 1f) < 0.005f)
    }
  }

  @Test
  fun invalidAndOutOfRangeProgressAreHandledSafely() {
    val below = EqualPowerCurve.calculateGains(-1f)
    assertEquals(1f, below.outgoingGain, 0.001f)
    assertEquals(0f, below.incomingGain, 0.001f)

    val above = EqualPowerCurve.calculateGains(2f)
    assertEquals(0f, above.outgoingGain, 0.001f)
    assertEquals(1f, above.incomingGain, 0.001f)

    val invalid = EqualPowerCurve.calculateGains(Float.NaN)
    assertEquals(1f, invalid.outgoingGain, 0.001f)
    assertEquals(0f, invalid.incomingGain, 0.001f)
  }
}
