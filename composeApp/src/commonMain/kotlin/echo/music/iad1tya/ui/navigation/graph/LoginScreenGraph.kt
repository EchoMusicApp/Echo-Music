package echo.music.iad1tya.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import echo.music.iad1tya.ui.navigation.destination.login.DiscordLoginDestination
import echo.music.iad1tya.ui.navigation.destination.login.LastfmLoginDestination
import echo.music.iad1tya.ui.navigation.destination.login.LoginDestination
import echo.music.iad1tya.ui.navigation.destination.login.SpotifyLoginDestination
import echo.music.iad1tya.ui.screen.login.DiscordLoginScreen
import echo.music.iad1tya.ui.screen.login.LastfmLoginScreen
import echo.music.iad1tya.ui.screen.login.LoginScreen
import echo.music.iad1tya.ui.screen.login.SpotifyLoginScreen

fun NavGraphBuilder.loginScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomBar: () -> Unit,
    showBottomBar: () -> Unit,
) {
    composable<LoginDestination> {
        LoginScreen(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomNavigation = hideBottomBar,
            showBottomNavigation = showBottomBar,
        )
    }

    composable<SpotifyLoginDestination> {
        SpotifyLoginScreen(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomNavigation = hideBottomBar,
            showBottomNavigation = showBottomBar,
        )
    }

    composable<DiscordLoginDestination> {
        DiscordLoginScreen(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomNavigation = hideBottomBar,
            showBottomNavigation = showBottomBar,
        )
    }

    composable<LastfmLoginDestination> {
        LastfmLoginScreen(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomNavigation = hideBottomBar,
            showBottomNavigation = showBottomBar,
        )
    }
}