package echo.music.iad1tya.playback

interface IWidgetManager {
  suspend fun updateWidgets(
    title: String,
    artist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long = 0,
    currentPosition: Long = 0
  )
}
