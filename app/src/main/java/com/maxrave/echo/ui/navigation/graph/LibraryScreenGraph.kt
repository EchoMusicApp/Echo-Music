package iad1tya.echo.music.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import iad1tya.echo.music.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import iad1tya.echo.music.ui.screen.library.LibraryDynamicPlaylistScreen

@UnstableApi
@ExperimentalMaterial3Api
fun NavGraphBuilder.libraryScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<LibraryDynamicPlaylistDestination> { entry ->
        val data = entry.toRoute<LibraryDynamicPlaylistDestination>()
        LibraryDynamicPlaylistScreen(
            innerPadding = innerPadding,
            navController = navController,
            type = data.type,
        )
    }
    
}