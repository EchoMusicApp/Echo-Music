package echo.music.iad1tya.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// shortcut: the Android version plays the canvas video through ExoPlayer; desktop draws nothing until a video backend exists.
@Composable
fun CanvasArtworkPlayer(primaryUrl: String?, fallbackUrl: String?, isPlaying: Boolean, modifier: Modifier = Modifier) {}
