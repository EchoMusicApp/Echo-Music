package echo.music.usbaudio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbDeviceDetectorTest {
  @Test
  fun testDetectsAudioClassUsbDevice() {
    val detector = UsbDeviceDetector()
    val isAudio = detector.isAudioClassDevice(hasAudioInterface = true)
    assertTrue(isAudio)
    val isStorage = detector.isAudioClassDevice(hasAudioInterface = false)
    assertFalse(isStorage)
  }
}
