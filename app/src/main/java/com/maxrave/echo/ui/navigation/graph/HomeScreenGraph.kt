package iad1tya.echo.music.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import iad1tya.echo.music.ui.navigation.destination.home.CreditDestination
import iad1tya.echo.music.ui.navigation.destination.home.LanguageSelectionDestination
import iad1tya.echo.music.ui.navigation.destination.home.MoodDestination
import iad1tya.echo.music.ui.navigation.destination.home.NotificationDestination
import iad1tya.echo.music.ui.navigation.destination.home.RecentlySongsDestination
import iad1tya.echo.music.ui.navigation.destination.home.SettingsDestination
import iad1tya.echo.music.ui.screen.home.MoodScreen
import iad1tya.echo.music.ui.screen.home.NotificationScreen
import iad1tya.echo.music.ui.screen.home.RecentlySongsScreen
import iad1tya.echo.music.ui.screen.home.SettingScreen
import iad1tya.echo.music.ui.screen.other.CreditScreen
import com.maxrave.echo.ui.screen.settings.LanguageSelectionScreen
import com.maxrave.echo.service.LanguageDownloadManager
import org.koin.compose.koinInject
import iad1tya.echo.music.viewModel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@UnstableApi
fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<CreditDestination> {
        CreditScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<MoodDestination> { entry ->
        val params = entry.toRoute<MoodDestination>().params
        MoodScreen(
            navController = navController,
            params = params,
        )
    }
    composable<NotificationDestination> {
        NotificationScreen(
            navController = navController,
        )
    }
    composable<RecentlySongsDestination> {
        RecentlySongsScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<SettingsDestination> {
        SettingScreen(
            navController = navController,
            innerPadding = innerPadding,
        )
    }
    composable<LanguageSelectionDestination> {
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val languageDownloadManager: LanguageDownloadManager = koinInject()
        LanguageSelectionScreen(
            settingsViewModel = settingsViewModel,
            languageDownloadManager = languageDownloadManager,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}