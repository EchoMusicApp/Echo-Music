package echo.music.dsp.core

import echo.music.dsp.core.models.FilterType
import echo.music.dsp.core.parser.ParametricEQParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricEQParserTest {

  @Test
  fun testParseAutoEqTextBlock() {
    val autoEqText = """
      Preamp: -6.5 dB
      Filter 1: ON PK Fc 31 Hz Gain 5.2 dB Q 1.41
      Filter 2: ON LSC Fc 105 Hz Gain 3.0 dB Q 0.71
      Filter 3: ON HSC Fc 10000 Hz Gain -2.0 dB Q 0.71
    """.trimIndent()

    val eq = ParametricEQParser.parseText(autoEqText)
    assertEquals(-6.5, eq.preamp, 0.001)
    assertEquals(3, eq.bands.size)

    assertEquals(FilterType.PK, eq.bands[0].filterType)
    assertEquals(31.0, eq.bands[0].frequency, 0.001)
    assertEquals(5.2, eq.bands[0].gain, 0.001)
    assertEquals(1.41, eq.bands[0].q, 0.001)

    assertEquals(FilterType.LSC, eq.bands[1].filterType)
    assertEquals(FilterType.HSC, eq.bands[2].filterType)

    val validationErrors = ParametricEQParser.validate(eq)
    assertTrue("Parsed valid EQ should have zero validation errors", validationErrors.isEmpty())
  }

  @Test
  fun testIgnoresDisabledBands() {
    val autoEqText = """
      Preamp: -1.0 dB
      Filter 1: OFF PK Fc 100 Hz Gain 2.0 dB Q 1.0
      Filter 2: ON PK Fc 200 Hz Gain 3.0 dB Q 1.0
    """.trimIndent()

    val eq = ParametricEQParser.parseText(autoEqText)
    assertEquals(1, eq.bands.size)
    assertEquals(200.0, eq.bands[0].frequency, 0.001)
  }
}
