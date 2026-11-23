package echo.music.usbaudio

import org.junit.Assert.assertEquals
import org.junit.Test

class UsbStreamEngineTest {
  @Test
  fun testSamplePacingPerMicroframeAt96kHz() {
    val driver = UsbAudioDriver()
    // 96000 samples/sec / 8000 microframes/sec = 12 samples/microframe
    val samplesPerPacket = driver.calculateNominalPacketSamples(sampleRate = 96000)
    assertEquals(12, samplesPerPacket)
    // 44100 samples/sec / 8000 microframes/sec = 5.5125 samples/microframe (nominal floor is 5)
    val fractionalSamples = driver.calculateNominalPacketSamples(sampleRate = 44100)
    assertEquals(5, fractionalSamples)
  }
}
