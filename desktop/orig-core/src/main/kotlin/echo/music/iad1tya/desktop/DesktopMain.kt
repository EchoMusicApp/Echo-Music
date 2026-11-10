package echo.music.iad1tya.desktop

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import echo.music.iad1tya.R
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
import echo.music.iad1tya.ui.player.BottomSheetPlayer
import echo.music.iad1tya.ui.component.rememberBottomSheetState
import echo.music.iad1tya.constants.MiniPlayerHeight
import echo.music.iad1tya.LocalExpandPlayer
import echo.music.iad1tya.LocalIsPlayerExpanded
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import echo.music.iad1tya.ui.screens.Screens
import echo.music.iad1tya.ui.screens.navigationBuilder
import echo.music.iad1tya.ui.theme.echomusicTheme

/** Shows any crash with its stack trace (and saves it), instead of failing silently or as "Unknown error". */
private fun installErrorDialog() {
  var shown = 0
  Thread.setDefaultUncaughtExceptionHandler { _, e ->
    val text = java.io.StringWriter().also { e.printStackTrace(java.io.PrintWriter(it)) }.toString()
    runCatching { java.io.File(System.getProperty("user.home"), ".echomusic/error.log").also { it.parentFile.mkdirs() }.appendText("${java.util.Date()}\n$text\n") }
    System.err.println(text)
    if (shown++ < 3) javax.swing.SwingUtilities.invokeLater {
      javax.swing.JOptionPane.showMessageDialog(null, text.take(1800) + "\n(also saved to ~/.echomusic/error.log)", "Echo Music error", javax.swing.JOptionPane.ERROR_MESSAGE)
    }
  }
}

fun main() = application {
  remember { installErrorDialog() }
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
          val playerState = rememberBottomSheetState(dismissedBound = 0.dp, collapsedBound = MiniPlayerHeight, expandedBound = maxHeight)
          val meta by rt.playerConnection.mediaMetadata.collectAsState()
          LaunchedEffect(meta == null) { if (meta == null) playerState.dismiss() else if (playerState.isDismissed) playerState.collapseSoft() }
          CompositionLocalProvider(LocalIsPlayerExpanded provides playerState.isExpanded, LocalExpandPlayer provides { playerState.expandSoft() }) {
          Row(Modifier.fillMaxSize().padding(bottom = if (meta != null) MiniPlayerHeight else 0.dp)) {
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
              Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                IconButton({ navController.navigate("history") }) { Icon(painterResource(R.drawable.music_history), "History") }
                IconButton({ navController.navigate("stats") }) { Icon(painterResource(R.drawable.stats), "Stats") }
                IconButton({ navController.navigate("settings") }) { Icon(painterResource(R.drawable.settings), "Settings") }
              }
              Box(Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = Screens.Home.route) {
                  navigationBuilder(navController, scrollBehavior, android.app.Activity(), snackbarHostState)
                }
              }
            }
          }
          BottomSheetPlayer(state = playerState, navController = navController, pureBlack = false, modifier = Modifier.align(Alignment.BottomCenter))
          }
          BottomSheetMenu(state = menuState, modifier = Modifier.align(Alignment.BottomCenter))
          BottomSheetPage(state = pageState, modifier = Modifier.align(Alignment.BottomCenter))
          SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
      }
    }
  }
}
