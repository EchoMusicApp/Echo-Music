




package iad1tya.echo.music.ui.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.DarkModeKey
import iad1tya.echo.music.constants.PureBlackKey
import iad1tya.echo.music.ui.component.BottomSheet
import iad1tya.echo.music.ui.component.BottomSheetMenu
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.rememberBottomSheetState
import iad1tya.echo.music.ui.screens.BrowseScreen
import iad1tya.echo.music.ui.screens.artist.ArtistAlbumsScreen
import iad1tya.echo.music.ui.screens.artist.ArtistItemsScreen
import iad1tya.echo.music.ui.screens.artist.ArtistScreen
import iad1tya.echo.music.ui.screens.artist.ArtistSongsScreen
import iad1tya.echo.music.ui.screens.library.LocalSongScreen
import iad1tya.echo.music.ui.screens.library.LibraryScreen
import iad1tya.echo.music.ui.screens.playlist.AutoPlaylistScreen
import iad1tya.echo.music.ui.screens.playlist.LocalPlaylistScreen
import iad1tya.echo.music.ui.screens.playlist.OnlinePlaylistScreen
import iad1tya.echo.music.ui.screens.playlist.TopPlaylistScreen
import iad1tya.echo.music.ui.screens.playlist.CachePlaylistScreen
import iad1tya.echo.music.ui.screens.search.OnlineSearchResult
import iad1tya.echo.music.ui.screens.SpotifyImportScreen
import iad1tya.echo.music.ui.screens.settings.AboutScreen
import iad1tya.echo.music.ui.screens.settings.AccountSettings
import iad1tya.echo.music.ui.screens.settings.AppearanceSettings
import iad1tya.echo.music.ui.screens.settings.CustomizeBackground
import iad1tya.echo.music.ui.screens.settings.BackupAndRestore
import iad1tya.echo.music.ui.screens.settings.ChangelogScreen
import iad1tya.echo.music.ui.screens.settings.ContentSettings
import iad1tya.echo.music.ui.screens.settings.DarkMode
import iad1tya.echo.music.ui.screens.settings.DiscordLoginScreen
import iad1tya.echo.music.ui.screens.settings.DiscordSettings
import iad1tya.echo.music.ui.screens.settings.DebugSettings
import iad1tya.echo.music.ui.screens.settings.IntegrationScreen
import iad1tya.echo.music.ui.screens.settings.LastFMSettings
import iad1tya.echo.music.ui.screens.settings.MusicTogetherScreen
import iad1tya.echo.music.ui.screens.settings.PalettePickerScreen
import iad1tya.echo.music.ui.screens.settings.PlayerSettings
import iad1tya.echo.music.ui.screens.settings.PoTokenScreen
import iad1tya.echo.music.ui.screens.settings.PrivacySettings
import iad1tya.echo.music.ui.screens.settings.InternetSettings
import iad1tya.echo.music.ui.screens.settings.SettingsScreen
import iad1tya.echo.music.ui.screens.settings.StorageSettings
import iad1tya.echo.music.ui.screens.settings.ThemeCreatorScreen
import iad1tya.echo.music.ui.screens.settings.UpdateScreen
import iad1tya.echo.music.musicrecognition.MusicRecognitionRoute
import iad1tya.echo.music.ui.screens.musicrecognition.MusicRecognitionScreen
import iad1tya.echo.music.ui.utils.ShowMediaInfo
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.ui.screens.podcast.OnlinePodcastScreen

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    disableAnimations: Boolean = false,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    composable("local_songs") {
        LocalSongScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("year_in_music") {
        YearInMusicScreen(navController)
    }
    composable(MusicRecognitionRoute) {
        MusicRecognitionScreen(navController)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("charts_screen") {
       ChartsScreen(navController)
    }
    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }
    composable(
        route = "search/{query}",
        arguments =
        listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else {
                fadeIn(tween(250))
            }
        },
        exitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else {
                fadeOut(tween(200))
            }
        },
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
        listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "podcast/{podcastId}",
        arguments =
        listOf(
            navArgument("podcastId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
        listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
        listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
        listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        SettingsScreen(navController, scrollBehavior, latestVersionName)
    }
    composable("settings/account") {
        AccountSettings(navController, scrollBehavior, latestVersionName)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    composable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/internet") {
        InternetSettings(navController)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("spotify_import") {
        SpotifyImportScreen(navController)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/integration") {
        IntegrationScreen(navController, scrollBehavior)
    }
    composable("settings/music_together") {
        MusicTogetherScreen(navController, scrollBehavior)
    }
    composable("settings/lastfm") {
        LastFMSettings(navController, scrollBehavior)
    }
    composable("settings/discord/experimental") {
        iad1tya.echo.music.ui.screens.settings.DiscordExperimental(navController)
    }
    composable("settings/misc") {
        DebugSettings(navController)
    }
    composable("settings/update") {
        UpdateScreen(navController, scrollBehavior)
    }
    composable("settings/changelog") {
        ChangelogScreen(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("settings/about") {
        AboutScreen(navController)
    }
    composable("settings/po_token") {
        PoTokenScreen(navController, scrollBehavior)
    }
    composable("customize_background") {
        CustomizeBackground(navController)
    }
    composable(
        route = "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT={$LOGIN_URL_ARGUMENT}",
        arguments = listOf(
            navArgument(LOGIN_URL_ARGUMENT) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        LoginScreen(
            navController,
            startUrl = backStackEntry.arguments?.getString(LOGIN_URL_ARGUMENT)?.let(Uri::decode)
        )
    }
}
