package echo.music.usbaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAudioDriverSanityTest {
  @Test
  fun testNativeDriverLoadsAndReportsVersion() {
    val driver = UsbAudioDriver()
    assertTrue(driver.isLoaded())
    assertEquals("1.0.0-usbaudio", driver.getNativeDriverVersion())
  }
}
