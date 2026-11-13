package echo.music.iad1tya.export

internal data class ExportAudioFormat(val mimeType: String, val extension: String)

internal object ExportAudioFormats {
  fun fromHeader(header: ByteArray, declaredMime: String): ExportAudioFormat? =
    when {
      header.startsWith(0x1A, 0x45, 0xDF, 0xA3) -> ExportAudioFormat("audio/webm", "webm")
      header.startsWith("OggS") ->
        if (declaredMime.contains("opus", ignoreCase = true)) {
          ExportAudioFormat("audio/ogg", "opus")
        } else {
          ExportAudioFormat("audio/ogg", "ogg")
        }
      header.startsWith("fLaC") -> ExportAudioFormat("audio/flac", "flac")
      header.startsWith("RIFF") && header.startsWith("WAVE", 8) -> ExportAudioFormat("audio/wav", "wav")
      header.startsWith("ftyp", 4) -> ExportAudioFormat("audio/mp4", "m4a")
      isMp3Header(header) -> ExportAudioFormat("audio/mpeg", "mp3")
      declaredMime.startsWith("audio/") -> fromMime(declaredMime)
      else -> null
    }

  fun fromMime(mimeType: String): ExportAudioFormat? =
    when {
      mimeType.contains("flac", true) -> ExportAudioFormat("audio/flac", "flac")
      mimeType.contains("opus", true) -> ExportAudioFormat("audio/ogg", "opus")
      mimeType.contains("ogg", true) -> ExportAudioFormat("audio/ogg", "ogg")
      mimeType.contains("wav", true) -> ExportAudioFormat("audio/wav", "wav")
      mimeType.contains("webm", true) -> ExportAudioFormat("audio/webm", "webm")
      mimeType.contains("mp3", true) || mimeType.contains("mpeg", true) -> ExportAudioFormat("audio/mpeg", "mp3")
      mimeType.contains("aac", true) || mimeType.contains("mp4", true) || mimeType.contains("m4a", true) ->
        ExportAudioFormat("audio/mp4", "m4a")
      else -> null
    }

  fun hasContainerSignature(header: ByteArray): Boolean =
    header.startsWith(0x1A, 0x45, 0xDF, 0xA3) ||
      header.startsWith("OggS") ||
      header.startsWith("fLaC") ||
      (header.startsWith("RIFF") && header.startsWith("WAVE", 8)) ||
      header.startsWith("ftyp", 4) ||
      isMp3Header(header)

  fun isMp3Header(header: ByteArray): Boolean =
    header.size >= 3 &&
      ((header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) ||
        ((header[0].toInt() and 0xFF) == 0xFF && (header[1].toInt() and 0xE0) == 0xE0))

  private fun ByteArray.startsWith(vararg bytes: Int): Boolean =
    size >= bytes.size && bytes.indices.all { (this[it].toInt() and 0xFF) == bytes[it] }

  private fun ByteArray.startsWith(text: String, offset: Int = 0): Boolean =
    size >= offset + text.length && text.indices.all { this[offset + it] == text[it].code.toByte() }
}
