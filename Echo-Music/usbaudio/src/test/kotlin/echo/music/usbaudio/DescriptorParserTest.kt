package echo.music.usbaudio

import echo.music.usbaudio.model.DacCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptorParserTest {
  @Test
  fun testParseUac2DescriptorExtractsHighResFormatsAndClockSource() {
    val driver = UsbAudioDriver()
    val sampleUac2Descriptor = byteArrayOf(
      // Standard Configuration Descriptor (9 bytes)
      0x09, 0x02, 0x64, 0x00, 0x02, 0x01, 0x00, 0xC0.toByte(), 0x32,
      // Standard AudioControl Interface (UAC2: Class 0x01, Subclass 0x01, Protocol 0x20)
      0x09, 0x04, 0x00, 0x00, 0x00, 0x01, 0x01, 0x20, 0x00,
      // AudioControl Header UAC2 (9 bytes, bcdADC = 0x0200)
      0x09, 0x24, 0x01, 0x00, 0x02, 0x01, 0x30, 0x00, 0x00,
      // Clock Source Descriptor (Unit 0x01, internal clock, 0x07 controls)
      0x08, 0x24, 0x0A, 0x01, 0x03, 0x07, 0x00, 0x00,
      // AudioStreaming Interface Alt 1 (24-bit 96k/192k)
      0x09, 0x04, 0x01, 0x01, 0x02, 0x01, 0x02, 0x20, 0x00
    )
    val capabilities = driver.parseDescriptors(sampleUac2Descriptor)
    assertEquals(2, capabilities.uacVersion)
    assertEquals(1, capabilities.clockSourceId)
    assertTrue(capabilities.supportedSampleRates.contains(48000))
  }

  @Test
  fun testParseShortDescriptorsDoesNotThrowOrReadOutOfBounds() {
    val driver = UsbAudioDriver()
    // Descriptors with truncated length (e.g., CS_INTERFACE with length 2)
    val malformedDescriptors = byteArrayOf(
      0x02, 0x24, // Length 2 CS_INTERFACE (subtype missing)
      0x03, 0x24, 0x02, // Length 3 CS_INTERFACE (subtype 0x02, format missing)
      0x00 // Zero length terminator
    )
    val capabilities = driver.parseDescriptors(malformedDescriptors)
    assertEquals(1, capabilities.uacVersion)
  }
}
