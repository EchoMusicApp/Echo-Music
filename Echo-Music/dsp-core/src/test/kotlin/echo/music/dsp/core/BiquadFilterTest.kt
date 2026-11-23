package echo.music.dsp.core

import echo.music.dsp.core.models.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiquadFilterTest {

  @Test
  fun testPeakingFilterZeroGainPreservesSample() {
    val filter = BiquadFilter(
      sampleRate = 44100,
      frequency = 1000.0,
      gain = 0.0,
      q = 1.41,
      filterType = FilterType.PK
    )

    val out = filter.processSample(1.0)
    assertEquals(1.0, out, 0.001)
  }

  @Test
  fun testPeakingFilterGainBoostsSample() {
    val filter = BiquadFilter(
      sampleRate = 44100,
      frequency = 1000.0,
      gain = 6.0,
      q = 1.41,
      filterType = FilterType.PK
    )

    val out = filter.processSample(1.0)
    assertTrue("Boosted sample should be greater than 1.0", out > 1.0)
    assertFalse("Output should not be NaN", out.isNaN())
    assertFalse("Output should not be infinite", out.isInfinite())
  }

  @Test
  fun testStereoProcessingAndReset() {
    val filter = BiquadFilter(
      sampleRate = 48000,
      frequency = 500.0,
      gain = -3.0,
      q = 1.0,
      filterType = FilterType.LSC
    )

    val (left, right) = filter.processStereo(0.5, 0.5)
    assertEquals(left, right, 0.0001)

    filter.reset()
    val (afterResetL, _) = filter.processStereo(0.5, 0.5)
    assertEquals(left, afterResetL, 0.0001)
  }

  @Test
  fun testNyquistFrequencyProtection() {
    // Frequency near or at Nyquist
    val filter = BiquadFilter(
      sampleRate = 44100,
      frequency = 22050.0,
      gain = 3.0,
      q = 1.0,
      filterType = FilterType.HSC
    )

    val out = filter.processSample(0.5)
    assertFalse("Output near Nyquist should not be NaN", out.isNaN())
  }
}
