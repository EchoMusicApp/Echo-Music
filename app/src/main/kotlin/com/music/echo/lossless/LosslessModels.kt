package com.music.echo.lossless

enum class LosslessProvider(val displayName: String, val badge: String, val description: String) {
  DEEZER("Deezer Hi-Fi", "FLAC 1411 kbps", "CD-Quality 16-bit / 44.1kHz FLAC and 1000x1000 artwork"),
  TIDAL("Tidal HiFi", "Hi-Res FLAC", "Bit-exact 24-bit / 96kHz MQA & HiFi FLAC audio"),
  QOBUZ("Qobuz Studio", "Hi-Res 192kHz", "Studio Master up to 24-bit / 192kHz lossless audio"),
  APPLE_MUSIC("Apple Music", "Apple Lossless", "ALAC bit-exact lossless streaming")
}

data class DeezerCredentials(
  val arl: String = "",
  val userName: String? = null,
  val isConnected: Boolean = false
) {
  val isValid: Boolean get() = arl.isNotBlank() && isConnected
}

data class TidalCredentials(
  val sessionOrToken: String = "",
  val userName: String? = null,
  val isConnected: Boolean = false
) {
  val isValid: Boolean get() = sessionOrToken.isNotBlank() && isConnected
}

data class QobuzCredentials(
  val appId: String = "",
  val appSecret: String = "",
  val userToken: String = "",
  val userName: String? = null,
  val isConnected: Boolean = false
) {
  val isValid: Boolean get() = appId.isNotBlank() && userToken.isNotBlank() && isConnected
}

data class AppleMusicCredentials(
  val musicUserToken: String = "",
  val developerToken: String? = null,
  val isConnected: Boolean = false
) {
  val isValid: Boolean get() = musicUserToken.isNotBlank() && isConnected
}

data class LosslessSettingsState(
  val preferLossless: Boolean = true,
  val deezer: DeezerCredentials = DeezerCredentials(),
  val tidal: TidalCredentials = TidalCredentials(),
  val qobuz: QobuzCredentials = QobuzCredentials(),
  val appleMusic: AppleMusicCredentials = AppleMusicCredentials(),
  val priority: List<LosslessProvider> = listOf(
    LosslessProvider.DEEZER,
    LosslessProvider.TIDAL,
    LosslessProvider.QOBUZ,
    LosslessProvider.APPLE_MUSIC
  )
) {
  val connectedCount: Int
    get() = (if (deezer.isValid) 1 else 0) +
      (if (tidal.isValid) 1 else 0) +
      (if (qobuz.isValid) 1 else 0) +
      (if (appleMusic.isValid) 1 else 0)
}

data class LosslessStreamResult(
  val streamUrl: String,
  val provider: LosslessProvider,
  val codec: String = "flac",
  val bitrate: Long = 1411200L,
  val sampleRate: Int = 44100,
  val bitDepth: Int = 16,
  val headers: Map<String, String> = emptyMap()
)

data class DeezerArtistSearchResponse(
  val data: List<DeezerArtistItem> = emptyList(),
  val total: Int = 0
)

data class DeezerArtistItem(
  val id: Long = 0L,
  val name: String = "",
  val picture: String? = null,
  val picture_small: String? = null,
  val picture_medium: String? = null,
  val picture_big: String? = null,
  val picture_xl: String? = null,
  val nb_album: Int = 0,
  val nb_fan: Int = 0
)

data class DeezerTrackSearchResponse(
  val data: List<DeezerTrackItem> = emptyList(),
  val total: Int = 0
)

data class DeezerTrackItem(
  val id: Long = 0L,
  val title: String = "",
  val duration: Int = 0,
  val preview: String? = null,
  val artist: DeezerArtistItem? = null
)
