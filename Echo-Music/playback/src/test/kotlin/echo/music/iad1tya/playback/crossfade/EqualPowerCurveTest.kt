package echo.music.iad1tya.playback.crossfade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EqualPowerCurveTest {

  @Test
  fun testBoundaryValues() {
    val (out0, in0) = EqualPowerCurve.calculateGains(0.0f)
    assertEquals(1.0f, out0, 0.001f)
    assertEquals(0.0f, in0, 0.001f)

    val (out1, in1) = EqualPowerCurve.calculateGains(1.0f)
    assertEquals(0.0f, out1, 0.001f)
    assertEquals(1.0f, in1, 0.001f)
  }

  @Test
  fun testConstantAcousticPower() {
    for (i in 0..100) {
      val progress = i / 100.0f
      val (outGain, inGain) = EqualPowerCurve.calculateGains(progress)
      val totalPower = (outGain * outGain) + (inGain * inGain)
      assertTrue("Total power at $progress should be approx 1.0, but was $totalPower", abs(totalPower - 1.0f) < 0.005)
    }
  }

  @Test
  fun testMidpointEqualGains() {
    val (outMid, inMid) = EqualPowerCurve.calculateGains(0.5f)
    assertEquals(outMid, inMid, 0.001f)
    // At t=0.5, cos(pi/4) = sin(pi/4) = sqrt(2)/2 ~= 0.7071f
    assertEquals(0.7071f, outMid, 0.002f)
  }
}
