package echo.music.iad1tya

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.listentogether.ListenTogetherManager
import echo.music.iad1tya.playback.PlayerConnection

// Desktop copies of the app-wide composition locals that the Android MainActivity defines.
val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<ListenTogetherManager?> { null }
