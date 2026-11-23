package echo.music.usbaudio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SpscRingBufferTest {
  @Test
  fun testRingBufferWriteAndReadMaintainsDataIntegrity() {
    val driver = UsbAudioDriver()
    val testPayload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
    val written = driver.testRingBufferWrite(testPayload)
    assertEquals(testPayload.size, written)
    val readBack = driver.testRingBufferRead(testPayload.size)
    assertArrayEquals(testPayload, readBack)
  }
}
