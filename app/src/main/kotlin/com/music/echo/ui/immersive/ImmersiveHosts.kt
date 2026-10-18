package echo.music.iad1tya.ui.immersive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Liquid Sound as the player background. Rendering pauses when [visible] is false or the app is in the background. */
@Composable
fun LiquidSoundPlayerBackground(
  modifier: Modifier = Modifier,
  playing: Boolean,
  thumbnailUrl: String?,
  visible: Boolean,
) {
  val motion = rememberMotionSettings()
  val reactive = rememberReactive(playing = playing, motion = motion, active = visible)
  val palette = rememberAlbumPalette(thumbnailUrl)
  val config = rememberLiquidConfig()
  Box(modifier) {
    LiquidSoundBackground(config = config, reactive = reactive, palette = palette, thumbnailUrl = thumbnailUrl)
  }
}

/** Beat City as the player background. Not interactive here, so it never steals touches from the player. */
@Composable
fun BeatCityPlayerBackground(
  modifier: Modifier = Modifier,
  playing: Boolean,
  thumbnailUrl: String?,
  visible: Boolean,
) {
  val motion = rememberMotionSettings()
  val reactive = rememberReactive(playing = playing, motion = motion, active = visible)
  val palette = rememberAlbumPalette(thumbnailUrl)
  val config = rememberCityConfig()
  Box(modifier) {
    BeatCityView(
      config = config,
      reactive = reactive,
      palette = palette,
      thumbnailUrl = thumbnailUrl,
      motion = motion,
      interactive = false,
    )
  }
}
