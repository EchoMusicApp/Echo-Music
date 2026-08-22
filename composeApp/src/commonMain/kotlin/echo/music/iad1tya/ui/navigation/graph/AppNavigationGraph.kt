package echo.music.iad1tya.ui.navigation.graph

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import echo.music.iad1tya.ui.navigation.destination.home.AnalyticsDestination
import echo.music.iad1tya.ui.navigation.destination.home.HomeDestination
import echo.music.iad1tya.ui.theme.ForceDarkContent
import echo.music.iad1tya.ui.navigation.destination.library.LibraryDestination
import echo.music.iad1tya.ui.navigation.destination.player.FullscreenDestination
import echo.music.iad1tya.ui.navigation.destination.search.SearchDestination
import echo.music.iad1tya.ui.screen.home.HomeScreen
import echo.music.iad1tya.ui.screen.home.analytics.AnalyticsScreen
import echo.music.iad1tya.ui.screen.library.LibraryScreen
import echo.music.iad1tya.ui.screen.other.SearchScreen
import echo.music.iad1tya.ui.screen.player.FullscreenPlayer

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun AppNavigationGraph(
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: Any = HomeDestination,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn() + slideInHorizontally { -it }
        },
        exitTransition = {
            fadeOut() + slideOutHorizontally { it }
        },
        popEnterTransition = {
            fadeIn() + slideInHorizontally { -it }
        },
        popExitTransition = {
            fadeOut() + slideOutHorizontally { it }
        },
    ) {
        // Bottom bar destinations
        composable<HomeDestination> {
            HomeScreen(
                onScrolling = onScrolling,
                navController = navController,
            )
        }
        composable<SearchDestination> {
            SearchScreen(
                navController = navController,
            )
        }
        composable<LibraryDestination> {
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        // Only reachable as a tab while local tracking is enabled
        composable<AnalyticsDestination> {
            AnalyticsScreen(
                navController = navController,
                innerPadding = innerPadding,
            )
        }
        composable<FullscreenDestination> {
            ForceDarkContent {
                FullscreenPlayer(
                    navController,
                    hideNavBar = hideNavBar,
                    showNavBar = {
                        showNavBar.invoke(true)
                        showNowPlayingSheet.invoke()
                    },
                )
            }
        }
        // Home screen graph
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // Library screen graph
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // List screen graph
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // Login screen graph
        loginScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomBar = hideNavBar,
            showBottomBar = {
                showNavBar(false)
            },
        )
    }
}