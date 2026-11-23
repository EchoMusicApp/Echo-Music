package echo.music.usbaudio

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import echo.music.usbaudio.model.DacCapabilities
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbDacAudioSinkTest {
  @Test
  fun testSupportsPcmFormatsMatchingDacCapabilities() {
    val caps = DacCapabilities(
      uacVersion = 2,
      supportedSampleRates = listOf(44100, 48000, 88200, 96000, 176400, 192000)
    )
    val sink = UsbDacAudioSink(UsbAudioDriver(), capabilities = caps)

    val pcm96k = Format.Builder()
      .setSampleMimeType(MimeTypes.AUDIO_RAW)
      .setChannelCount(2)
      .setSampleRate(96000)
      .setPcmEncoding(androidx.media3.common.C.ENCODING_PCM_24BIT)
      .build()
    assertTrue(sink.supportsFormat(pcm96k))

    val pcmOverMax = Format.Builder()
      .setSampleMimeType(MimeTypes.AUDIO_RAW)
      .setChannelCount(2)
      .setSampleRate(384000)
      .build()
    assertFalse(sink.supportsFormat(pcmOverMax))
  }

  @Test
  fun testSinkLifecycleAndClock() {
    val driver = UsbAudioDriver()
    val sink = UsbDacAudioSink(driver)

    val format = Format.Builder()
      .setSampleMimeType(MimeTypes.AUDIO_RAW)
      .setChannelCount(2)
      .setSampleRate(48000)
      .build()
    sink.configure(format, 0, null)

    // isEnded() must be false before playback finishes
    assertFalse(sink.isEnded())

    sink.play()
    val testPcm = java.nio.ByteBuffer.allocate(480 * 4) // 480 frames stereo 16-bit
    testPcm.put(ByteArray(480 * 4))
    testPcm.flip()

    val handled = sink.handleBuffer(testPcm, 0L, 1)
    assertTrue(handled)

    // 480 frames at 48000 Hz = 10,000 microseconds (10 ms)
    val positionUs = sink.getCurrentPositionUs(false)
    org.junit.Assert.assertEquals(10_000L, positionUs)

    // Still not ended until playToEndOfStream is called
    assertFalse(sink.isEnded())

    sink.setVolume(0.5f)

    sink.flush()
    org.junit.Assert.assertEquals(androidx.media3.exoplayer.audio.AudioSink.CURRENT_POSITION_NOT_SET, sink.getCurrentPositionUs(false))
  }
}
