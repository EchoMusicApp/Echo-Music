package echo.music.iad1tya

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import echo.music.iad1tya.db.MusicDatabase

// Desktop copies of the app-wide composition locals that the Android MainActivity defines.
// The player-related ones (LocalPlayerConnection etc.) come with the player port.
val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
