package echo.music.iad1tya.playback

enum class CastDeviceKind {
  TV,
  SPEAKER,
  CHROMECAST,
  UNKNOWN;

  companion object {
    fun fromName(name: String, description: String?): CastDeviceKind {
      val lowerName = name.lowercase()
      val lowerDesc = description?.lowercase() ?: ""
      return when {
        lowerName.contains("tv") || lowerDesc.contains("tv") -> TV
        lowerName.contains("speaker") ||
          lowerName.contains("home") ||
          lowerName.contains("nest") ||
          lowerDesc.contains("speaker") -> SPEAKER
        lowerName.contains("chromecast") || lowerDesc.contains("chromecast") -> CHROMECAST
        else -> UNKNOWN
      }
    }
  }
}
