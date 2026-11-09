package echo.music.iad1tya.desktop

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalDownloadUtil
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.LocalRingtoneViewModel
import echo.music.iad1tya.LocalSyncUtils
import androidx.compose.ui.platform.LocalContext
import echo.music.iad1tya.ui.component.BottomSheetMenu
import echo.music.iad1tya.ui.component.BottomSheetPage
import echo.music.iad1tya.ui.component.BottomSheetPageState
import echo.music.iad1tya.ui.component.LocalBottomSheetPageState
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.MenuState
import echo.music.iad1tya.ui.player.MiniPlayer
import echo.music.iad1tya.ui.screens.Screens
import echo.music.iad1tya.ui.screens.navigationBuilder
import echo.music.iad1tya.ui.theme.echomusicTheme
import kotlinx.coroutines.delay

fun main() = application {
  val runtime = remember { DesktopRuntime() }
  val version = System.getProperty("echomusic.version") ?: "dev"
  Window(
    onCloseRequest = ::exitApplication,
    title = "Echo Music (native port $version)",
    state = rememberWindowState(size = DpSize(1200.dp, 800.dp)),
  ) { DesktopApp(runtime) }
}

/** Desktop version of MainActivity's UI: theme, providers, side navigation, the original screens, mini player. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(rt: DesktopRuntime) {
  val navController = rememberNavController()
  val menuState = remember { MenuState() }
  val pageState = remember { BottomSheetPageState() }
  val snackbarHostState = remember { SnackbarHostState() }
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  val position = remember { mutableLongStateOf(0L) }
  val duration = remember { mutableLongStateOf(0L) }
  LaunchedEffect(Unit) {
    while (true) {
      position.longValue = rt.player.currentPosition.coerceAtLeast(0L)
      duration.longValue = rt.player.duration.coerceAtLeast(0L)
      delay(250)
    }
  }
  CompositionLocalProvider(
    LocalContext provides rt.context,
    LocalDatabase provides rt.database,
    LocalPlayerConnection provides rt.playerConnection,
    LocalDownloadUtil provides rt.downloadUtil,
    LocalSyncUtils provides rt.syncUtils,
    LocalRingtoneViewModel provides rt.ringtoneViewModel,
    LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0),
    LocalMenuState provides menuState,
    LocalBottomSheetPageState provides pageState,
  ) {
    echomusicTheme(darkTheme = isSystemInDarkTheme()) {
      Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
          Row(Modifier.fillMaxSize()) {
            val current by navController.currentBackStackEntryAsState()
            NavigationRail {
              Screens.MainScreens.forEach { screen ->
                val selected = current?.destination?.route == screen.route
                NavigationRailItem(
                  selected = selected,
                  onClick = { navController.navigate(screen.route) { launchSingleTop = true } },
                  icon = { Icon(painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive), null) },
                  label = { Text(stringResource(screen.titleId)) },
                )
              }
            }
            Column(Modifier.weight(1f)) {
              Box(Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = Screens.Home.route) {
                  navigationBuilder(navController, scrollBehavior, android.app.Activity(), snackbarHostState)
                }
              }
              val meta by rt.playerConnection.mediaMetadata.collectAsState()
              if (meta != null) MiniPlayer(position, duration)
            }
          }
          BottomSheetMenu(state = menuState, modifier = Modifier.align(Alignment.BottomCenter))
          BottomSheetPage(state = pageState, modifier = Modifier.align(Alignment.BottomCenter))
          SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
      }
    }
  }
}
