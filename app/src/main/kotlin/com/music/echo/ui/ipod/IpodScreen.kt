package iad1tya.echo.music.ui.ipod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.constants.IpodThemeKey
import iad1tya.echo.music.constants.IpodWheelHapticsKey
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.utils.rememberPreference

/**
 * The root iPod mode composable. Renders the device frame, screen, and click wheel.
 */
@Composable
fun IpodScreen() {
    val (themeName, setThemeName) = rememberPreference(IpodThemeKey, defaultValue = ClassicBlack.name)
    val (hapticsEnabled) = rememberPreference(IpodWheelHapticsKey, defaultValue = true)
    
    val theme = ipodThemeByName(themeName)

    IpodTheme(colorScheme = theme) {
        val navigator = rememberIpodNavigator()
        val playerConnection = LocalPlayerConnection.current

        // State for wheel scrolling
        var selectedMenuIndex by remember { mutableIntStateOf(0) }
        var selectedThemeIndex by remember { mutableIntStateOf(0) }
        var selectedBrowserIndex by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .systemBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The iPod screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.screenBackground)
                    .border(2.dp, theme.divider, RoundedCornerShape(8.dp))
            ) {
                when (val current = navigator.current) {
                    is IpodDestination.MainMenu -> {
                        IpodMenuScreen(
                            selectedIndex = selectedMenuIndex,
                            onItemSelected = { navigator.push(it.destination) }
                        )
                    }
                    is IpodDestination.NowPlaying -> {
                        IpodPlayerScreen()
                    }
                    is IpodDestination.Settings -> {
                        IpodSettingsScreen(
                            selectedIndex = selectedThemeIndex,
                            onItemSelected = { setThemeName(it.name) }
                        )
                    }
                    is IpodDestination.Songs,
                    is IpodDestination.Albums,
                    is IpodDestination.Artists,
                    is IpodDestination.Playlists,
                    is IpodDestination.Favorites -> {
                        IpodBrowserScreen(
                            title = current.javaClass.simpleName,
                            destination = current,
                            selectedIndex = selectedBrowserIndex,
                            onItemSelected = { /* Play action for V2 */ }
                        )
                    }
                    else -> {}
                }
            }

            Spacer(Modifier.weight(1f))

            // The Click Wheel
            ClickWheel(
                hapticsEnabled = hapticsEnabled,
                onScrollDown = {
                    when (navigator.current) {
                        is IpodDestination.MainMenu -> selectedMenuIndex = (selectedMenuIndex + 1).coerceAtMost(mainMenuItems.lastIndex)
                        is IpodDestination.Settings -> selectedThemeIndex = (selectedThemeIndex + 1).coerceAtMost(IpodThemes.lastIndex)
                        is IpodDestination.Songs, is IpodDestination.Albums, is IpodDestination.Artists, is IpodDestination.Playlists -> {
                            selectedBrowserIndex++ // We don't have the exact max size here easily, but the UI bounds it
                        }
                        else -> {}
                    }
                },
                onScrollUp = {
                    when (navigator.current) {
                        is IpodDestination.MainMenu -> selectedMenuIndex = (selectedMenuIndex - 1).coerceAtLeast(0)
                        is IpodDestination.Settings -> selectedThemeIndex = (selectedThemeIndex - 1).coerceAtLeast(0)
                        is IpodDestination.Songs, is IpodDestination.Albums, is IpodDestination.Artists, is IpodDestination.Playlists -> {
                            selectedBrowserIndex = (selectedBrowserIndex - 1).coerceAtLeast(0)
                        }
                        else -> {}
                    }
                },
                onCenter = {
                    when (navigator.current) {
                        is IpodDestination.MainMenu -> navigator.push(mainMenuItems[selectedMenuIndex].destination)
                        is IpodDestination.Settings -> setThemeName(IpodThemes[selectedThemeIndex].name)
                        else -> {}
                    }
                },
                onMenu = {
                    navigator.pop()
                },
                onPlayPause = {
                    playerConnection?.player?.togglePlayPause()
                },
                onPrevious = {
                    playerConnection?.player?.seekToPrevious()
                },
                onNext = {
                    playerConnection?.player?.seekToNext()
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(Modifier.weight(1f))
        }
    }
}
