package echo.music.iad1tya.ui.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import echo.music.iad1tya.expect.ui.PlatformBackdrop
import echo.music.iad1tya.ui.icon.AutoGraph
import echo.music.iad1tya.ui.icon.Home
import echo.music.iad1tya.ui.icon.LibraryMusic
import echo.music.iad1tya.ui.icon.Search
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.navigation.destination.home.AnalyticsDestination
import echo.music.iad1tya.ui.navigation.destination.home.HomeDestination
import echo.music.iad1tya.ui.navigation.destination.library.LibraryDestination
import echo.music.iad1tya.ui.navigation.destination.search.SearchDestination
import echo.music.iad1tya.viewModel.SharedViewModel
import org.jetbrains.compose.resources.StringResource
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.analytics
import echomusic.composeapp.generated.resources.home
import echomusic.composeapp.generated.resources.library
import echomusic.composeapp.generated.resources.search
import kotlin.reflect.KClass

@Composable
expect fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any = HomeDestination,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean = false,
    showAnalyticsTab: Boolean = false,
    onOpenNowPlaying: () -> Unit = {},
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = { _ -> },
)

sealed class BottomNavScreen(
    val ordinal: Int,
    val destination: Any,
    val title: StringResource,
    val icon: @Composable () -> Unit,
) {
    data object Home : BottomNavScreen(
        ordinal = 0,
        destination = HomeDestination,
        title = Res.string.home,
        icon = {
            Icon(
                echoIcons.Home,
                contentDescription = null,
            )
        },
    )

    data object Search : BottomNavScreen(
        ordinal = 1,
        destination = SearchDestination,
        title = Res.string.search,
        icon = {
            Icon(
                echoIcons.Search,
                contentDescription = null,
            )
        },
    )

    data object Library : BottomNavScreen(
        ordinal = 2,
        destination = LibraryDestination,
        title = Res.string.library,
        icon = {
            Icon(
                imageVector = echoIcons.LibraryMusic,
                contentDescription = null,
            )
        },
    )

    // Only shown when local tracking is enabled, always as the last tab.
    data object Analytics : BottomNavScreen(
        ordinal = 3,
        destination = AnalyticsDestination,
        title = Res.string.analytics,
        icon = {
            Icon(
                imageVector = echoIcons.AutoGraph,
                contentDescription = null,
            )
        },
    )
}