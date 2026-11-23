package echo.music.iad1tya.musicflow

import androidx.datastore.preferences.core.Preferences
import echo.music.iad1tya.constants.CanvasThumbnailAnimationKey
import echo.music.iad1tya.constants.DataSaverEnabledKey
import echo.music.iad1tya.constants.MusicFlowAnimatedBackgroundKey
import echo.music.iad1tya.constants.MusicFlowAutoNextKey
import echo.music.iad1tya.constants.MusicFlowAutoplayKey
import echo.music.iad1tya.constants.MusicFlowCellularPrefetchKey
import echo.music.iad1tya.constants.MusicFlowEnabledKey
import echo.music.iad1tya.constants.MusicFlowHideRecentKey
import echo.music.iad1tya.constants.MusicFlowLyricsKey

data class MusicFlowPreferences(
  val enabled: Boolean = true,
  val autoplay: Boolean = true,
  val autoNext: Boolean = true,
  val animatedBackground: Boolean = false,
  val lyrics: Boolean = true,
  val cellularPrefetch: Boolean = true,
  val hideRecent: Boolean = true,
  val dataSaver: Boolean = false,
) {
  companion object {
    fun from(preferences: Preferences) =
      MusicFlowPreferences(
        enabled = preferences[MusicFlowEnabledKey] ?: true,
        autoplay = preferences[MusicFlowAutoplayKey] ?: true,
        autoNext = preferences[MusicFlowAutoNextKey] ?: true,
        animatedBackground =
          preferences[MusicFlowAnimatedBackgroundKey]
            ?: preferences[CanvasThumbnailAnimationKey]
            ?: false,
        lyrics = preferences[MusicFlowLyricsKey] ?: true,
        cellularPrefetch = preferences[MusicFlowCellularPrefetchKey] ?: true,
        hideRecent = preferences[MusicFlowHideRecentKey] ?: true,
        dataSaver = preferences[DataSaverEnabledKey] ?: false,
      )
  }
}
