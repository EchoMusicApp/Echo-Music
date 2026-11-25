package echo.music.iad1tya.playback.audio.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothAudioTrackerTest {

  @Test
  fun mapCodecTypeHandlesAllStandardAndVendorTypes() {
    assertEquals(
      "SBC",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_SBC),
    )
    assertEquals(
      "AAC",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_AAC),
    )
    assertEquals(
      "aptX",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_APTX),
    )
    assertEquals(
      "aptX HD",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_APTX_HD),
    )
    assertEquals(
      "LDAC",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_LDAC),
    )
    assertEquals(
      "LC3",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_LC3),
    )
    assertEquals(
      "Opus",
      BluetoothAudioTracker.mapCodecType(BluetoothAudioTracker.SOURCE_CODEC_TYPE_OPUS),
    )
    assertEquals("Vendor Codec (7)", BluetoothAudioTracker.mapCodecType(7))
    assertEquals("Vendor Codec (8)", BluetoothAudioTracker.mapCodecType(8))
    assertEquals("Unknown", BluetoothAudioTracker.mapCodecType(-1))
  }

  @Test
  fun mapSampleRateParsesBitmasksAndDirectRates() {
    assertEquals(44100, BluetoothAudioTracker.mapSampleRate(1 shl 0))
    assertEquals(48000, BluetoothAudioTracker.mapSampleRate(1 shl 1))
    assertEquals(88200, BluetoothAudioTracker.mapSampleRate(1 shl 2))
    assertEquals(96000, BluetoothAudioTracker.mapSampleRate(1 shl 3))
    assertEquals(176400, BluetoothAudioTracker.mapSampleRate(1 shl 4))
    assertEquals(192000, BluetoothAudioTracker.mapSampleRate(1 shl 5))

    assertEquals(44100, BluetoothAudioTracker.mapSampleRate(44100))
    assertEquals(48000, BluetoothAudioTracker.mapSampleRate(48000))
    assertEquals(96000, BluetoothAudioTracker.mapSampleRate(96000))
    assertEquals(192000, BluetoothAudioTracker.mapSampleRate(192000))

    assertNull(BluetoothAudioTracker.mapSampleRate(0))
    assertNull(BluetoothAudioTracker.mapSampleRate(12345))
  }

  @Test
  fun mapBitDepthParsesBitmasksAndDirectDepths() {
    assertEquals(16, BluetoothAudioTracker.mapBitDepth(1 shl 0))
    assertEquals(24, BluetoothAudioTracker.mapBitDepth(1 shl 1))
    assertEquals(32, BluetoothAudioTracker.mapBitDepth(1 shl 2))

    assertEquals(16, BluetoothAudioTracker.mapBitDepth(16))
    assertEquals(24, BluetoothAudioTracker.mapBitDepth(24))
    assertEquals(32, BluetoothAudioTracker.mapBitDepth(32))

    assertNull(BluetoothAudioTracker.mapBitDepth(0))
    assertNull(BluetoothAudioTracker.mapBitDepth(8))
  }

  @Test
  fun ldacQualityModeIsTruthfullySeparatedFromRealtimeBitrate() {
    val (bitrate1000, mode1000) = BluetoothAudioTracker.parseLdacBitrate(1000L)
    assertEquals("Not exposed by Android", bitrate1000)
    assertEquals("High Quality (990 kbps nominal)", mode1000)

    val (bitrate1001, mode1001) = BluetoothAudioTracker.parseLdacBitrate(1001L)
    assertEquals("Not exposed by Android", bitrate1001)
    assertEquals("Standard (660 kbps nominal)", mode1001)

    val (bitrate1002, mode1002) = BluetoothAudioTracker.parseLdacBitrate(1002L)
    assertEquals("Not exposed by Android", bitrate1002)
    assertEquals("Connection Priority (330 kbps nominal)", mode1002)

    val (bitrate1003, mode1003) = BluetoothAudioTracker.parseLdacBitrate(1003L)
    assertEquals("Not exposed by Android", bitrate1003)
    assertEquals("Adaptive Bitrate (ABR)", mode1003)

    val (bitrateUnknown, modeUnknown) = BluetoothAudioTracker.parseLdacBitrate(0L)
    assertEquals("Not exposed by Android", bitrateUnknown)
    assertNull(modeUnknown)
  }

  @Test
  fun bluetoothTelemetryFormatting() {
    val disconnected = BluetoothTelemetry(isConnected = false)
    assertEquals("Disconnected", disconnected.formattedSummary())
    assertFalse(disconnected.isHighRes)

    val ldac =
      BluetoothTelemetry(
        isConnected = true,
        deviceName = "Sony WH-1000XM5",
        codecName = "LDAC",
        sampleRateHz = 96000,
        bitDepth = 24,
        mode = "High Quality (990 kbps nominal)",
        isAuthoritative = true,
      )
    assertTrue(ldac.isHighRes)
    assertTrue(ldac.hasNamedCodec)
    assertEquals(
      "LDAC / 24-bit / 96000 Hz (High Quality (990 kbps nominal))",
      ldac.formattedSummary(),
    )
  }
}
