package echo.music.iad1tya.ui.navigation.graph

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import echo.music.iad1tya.ui.navigation.destination.list.AlbumDestination
import echo.music.iad1tya.ui.navigation.destination.list.ArtistDestination
import echo.music.iad1tya.ui.navigation.destination.list.LocalPlaylistDestination
import echo.music.iad1tya.ui.navigation.destination.list.MoreAlbumsDestination
import echo.music.iad1tya.ui.navigation.destination.list.PlaylistDestination
import echo.music.iad1tya.ui.navigation.destination.list.PodcastDestination
import echo.music.iad1tya.ui.screen.library.LocalPlaylistScreen
import echo.music.iad1tya.ui.screen.other.AlbumScreen
import echo.music.iad1tya.ui.screen.other.ArtistScreen
import echo.music.iad1tya.ui.screen.other.MoreAlbumsScreen
import echo.music.iad1tya.ui.screen.other.PlaylistScreen
import echo.music.iad1tya.ui.screen.other.PodcastScreen
import echo.music.iad1tya.ui.theme.ForceDarkContent

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun NavGraphBuilder.listScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<AlbumDestination> { entry ->
        val data = entry.toRoute<AlbumDestination>()
        ForceDarkContent {
            AlbumScreen(
                browseId = data.browseId,
                navController = navController,
            )
        }
    }
    composable<ArtistDestination> { entry ->
        val data = entry.toRoute<ArtistDestination>()
        ForceDarkContent {
            ArtistScreen(
                channelId = data.channelId,
                navController = navController,
            )
        }
    }
    composable<LocalPlaylistDestination> { entry ->
        val data = entry.toRoute<LocalPlaylistDestination>()
        ForceDarkContent {
            LocalPlaylistScreen(
                id = data.id,
                navController = navController,
            )
        }
    }
    composable<MoreAlbumsDestination> { entry ->
        val data = entry.toRoute<MoreAlbumsDestination>()
        MoreAlbumsScreen(
            innerPadding = innerPadding,
            navController = navController,
            type = data.type,
            id = data.id,
        )
    }
    composable<PlaylistDestination> { entry ->
        val data = entry.toRoute<PlaylistDestination>()
        ForceDarkContent {
            PlaylistScreen(
                playlistId = data.playlistId,
                isYourYouTubePlaylist = data.isYourYouTubePlaylist,
                navController = navController,
            )
        }
    }
    composable<PodcastDestination> { entry ->
        val data = entry.toRoute<PodcastDestination>()
        ForceDarkContent {
            PodcastScreen(
                podcastId = data.podcastId,
                navController = navController,
            )
        }
    }
}