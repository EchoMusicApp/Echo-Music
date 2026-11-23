package echo.music.usbaudio

import echo.music.usbaudio.model.VolumeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeControllerTest {
  @Test
  fun testLogarithmicVolumeCurveToMillibelConversion() {
    val controller = VolumeController(hasHardwareVolume = true, minDb = -60.0f, maxDb = 0.0f)
    // 100% volume -> 0 dB
    assertEquals(0.0f, controller.calculateDbForSlider(1.0f), 0.01f)
    // 0% volume -> minDb (-60.0 dB)
    assertEquals(-60.0f, controller.calculateDbForSlider(0.0f), 0.01f)
    // 50% volume on log curve -> ~ -18 dB
    val midDb = controller.calculateDbForSlider(0.5f)
    assertTrue(midDb in -22.0f..-15.0f)
  }

  @Test
  fun testBitPerfectModeLocksGainAtUnity() {
    val controller = VolumeController(hasHardwareVolume = false, minDb = 0f, maxDb = 0f)
    controller.volumeMode = VolumeMode.PURE_BIT_PERFECT
    assertEquals(1.0, controller.getEffectiveSoftwareMultiplier(), 0.0001)
  }
}
