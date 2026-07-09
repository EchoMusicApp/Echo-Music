package iad1tya.echo.music.ui.ipod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * iPod-style push/pop navigation. No Jetpack Navigation — YAGNI for a flat menu.
 * ponytail: mutableStateListOf is the nav stack.
 */
sealed class IpodDestination {
    data object MainMenu : IpodDestination()
    data object NowPlaying : IpodDestination()
    data object Songs : IpodDestination()
    data object Albums : IpodDestination()
    data object Artists : IpodDestination()
    data object Playlists : IpodDestination()
    data object Favorites : IpodDestination()
    data object Settings : IpodDestination()
    data class AlbumDetail(val albumId: String, val albumTitle: String) : IpodDestination()
    data class ArtistDetail(val artistId: String, val artistName: String) : IpodDestination()
}

class IpodNavigator {
    val stack = mutableStateListOf<IpodDestination>(IpodDestination.MainMenu)

    val current: IpodDestination
        get() = stack.last()

    fun push(destination: IpodDestination) {
        stack.add(destination)
    }

    fun pop(): Boolean {
        if (stack.size > 1) {
            stack.removeLast()
            return true
        }
        return false
    }

    fun goToNowPlaying() {
        push(IpodDestination.NowPlaying)
    }

    val canGoBack: Boolean
        get() = stack.size > 1
}

@Composable
fun rememberIpodNavigator(): IpodNavigator {
    return remember { IpodNavigator() }
}
