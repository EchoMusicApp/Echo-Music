package echo.music.iad1tya.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportAudioFormatsTest {
  @Test
  fun detectsWebmFromContainerHeaderInsteadOfDeclaredM4aMime() {
    val format =
      ExportAudioFormats.fromHeader(
        byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()),
        "audio/mp4",
      )

    assertEquals(ExportAudioFormat("audio/webm", "webm"), format)
  }

  @Test
  fun detectsContainerAndExtensionForSupportedFormats() {
    assertEquals(
      ExportAudioFormat("audio/ogg", "opus"),
      ExportAudioFormats.fromHeader("OggS".toByteArray(), "audio/opus"),
    )
    assertEquals(
      ExportAudioFormat("audio/flac", "flac"),
      ExportAudioFormats.fromHeader("fLaC".toByteArray(), ""),
    )
    assertEquals(
      ExportAudioFormat("audio/wav", "wav"),
      ExportAudioFormats.fromHeader("RIFF0000WAVE".toByteArray(), ""),
    )
    assertEquals(
      ExportAudioFormat("audio/mp4", "m4a"),
      ExportAudioFormats.fromHeader("0000ftyp".toByteArray(), ""),
    )
    assertEquals(
      ExportAudioFormat("audio/mpeg", "mp3"),
      ExportAudioFormats.fromHeader("ID3".toByteArray(), ""),
    )
  }

  @Test
  fun rejectsUnknownHeadersAndUnsupportedMimeTypes() {
    assertNull(ExportAudioFormats.fromHeader("not audio".toByteArray(), "application/octet-stream"))
    assertNull(ExportAudioFormats.fromMime("audio/unknown"))
    assertFalse(ExportAudioFormats.hasContainerSignature("not audio".toByteArray()))
    assertFalse(ExportAudioFormats.isMp3Header("not".toByteArray()))
    assertTrue(
      ExportAudioFormats.isMp3Header(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte()))
    )
  }
}
