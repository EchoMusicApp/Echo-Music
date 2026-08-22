package echo.music.iad1tya.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import echo.music.iad1tya.ui.navigation.destination.home.CreditDestination
import echo.music.iad1tya.ui.navigation.destination.home.MoodDestination
import echo.music.iad1tya.ui.navigation.destination.home.RecentlySongsDestination
import echo.music.iad1tya.ui.navigation.destination.home.SettingsDestination
import echo.music.iad1tya.ui.screen.home.MoodScreen
import echo.music.iad1tya.ui.screen.home.RecentlySongsScreen
import echo.music.iad1tya.ui.screen.home.SettingScreen
import echo.music.iad1tya.ui.screen.other.CreditScreen

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
}