package echo.music.iad1tya.desktop

import androidx.compose.runtime.Composable
import echo.music.iad1tya.constants.AppIconTypeKey
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.map
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import echo.music.iad1tya.constants.DarkModeKey
import echo.music.iad1tya.constants.DynamicThemeKey
import echo.music.iad1tya.constants.IsMonochromeThemeKey
import echo.music.iad1tya.constants.PureBlackKey
import echo.music.iad1tya.constants.SelectedThemeColorKey
import echo.music.iad1tya.ui.screens.settings.DarkMode
import echo.music.iad1tya.ui.theme.DefaultThemeColor
import echo.music.iad1tya.ui.theme.extractThemeColor
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.window.Tray
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

/** Only one Echo Music at a time: two copies would share one database and fight over the audio and the Discord status. */
private var lockHolder: Any? = null // keeps the file channel (and so the lock) alive until exit

private fun alreadyRunning(): Boolean {
  val dir = java.io.File(System.getProperty("user.home"), ".echomusic").also { it.mkdirs() }
  val channel = java.io.RandomAccessFile(java.io.File(dir, "app.lock"), "rw").channel
  val lock = runCatching { channel.tryLock() }.getOrNull()
  lockHolder = lock?.let { channel to it }
  return lock == null
}

fun main() {
  if (alreadyRunning()) {
    javax.swing.JOptionPane.showMessageDialog(null, "Echo Music is already running.")
    return
  }
  runApp()
}

private fun runApp() = application {
  remember { installErrorDialog() }
  val runtime = remember { DesktopRuntime().also { MediaKeys.install(it.playerConnection) } }
  val version = System.getProperty("echomusic.version") ?: "dev"
  // window / taskbar icon follows Settings > Appearance > App icon (stored as the AppIconType name)
  val iconName by remember { runtime.context.dataStore.data.map { it[AppIconTypeKey] } }.collectAsState(null)
  val iconRes = when (iconName) {
    "LEGACY" -> R.mipmap.legacy_icon
    "STATIC" -> R.mipmap.ic_launcher_static
    "CAT" -> R.mipmap.cat_icon
    "CRAZY_BLUE" -> R.mipmap.crazy_blue_icon
    "POOKIE" -> R.mipmap.pookie_icon
    "SKY" -> R.mipmap.sky_icon
    "ECHO_CAT" -> R.mipmap.echo_cat_icon
    "BILLIE_EILISH" -> R.mipmap.billie_eilish_icon
    "SABRINA_CARPENTER" -> R.mipmap.ic_launcher_sabrina
    "SABRINA_CARPENTER_2" -> R.mipmap.ic_launcher_sabrina2
    "CRAFT" -> R.mipmap.ic_launcher_craft
    "SADIE" -> R.mipmap.ic_launcher_sadie
    else -> R.mipmap.ic_launcher
  }
  // tray icon with the basic controls; double-click / click brings no extra window logic (close button still quits)
  Tray(
    icon = painterResource(iconRes),
    tooltip = "Echo Music",
    menu = {
      Item("Play / Pause", onClick = { runtime.playerConnection.togglePlayPause() })
      Item("Next", onClick = { runtime.playerConnection.seekToNext() })
      Item("Previous", onClick = { runtime.playerConnection.seekToPrevious() })
      Separator()
      Item("Quit", onClick = ::exitApplication)
    },
  )
  val windowState = rememberWindowState(size = DpSize(1200.dp, 800.dp))
  Window(
    icon = painterResource(iconRes),
    onCloseRequest = ::exitApplication,
    title = "Echo Music (native port $version)",
    state = windowState,
    undecorated = true, // our own top bar replaces the system one
  ) { DesktopApp(runtime, windowState, this) { exitApplication() } }
}

/** Desktop version of MainActivity's UI: theme, providers, side navigation, the original screens, mini player. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(rt: DesktopRuntime, windowState: androidx.compose.ui.window.WindowState, windowScope: androidx.compose.ui.window.WindowScope, onClose: () -> Unit) {
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
    LocalPlayerAwareWindowInsets provides WindowInsets(top = 64.dp),
    LocalMenuState provides menuState,
    LocalBottomSheetPageState provides pageState,
  ) {
    // same theme rules as MainActivity: Appearance settings + (optionally) a colour taken from the playing song's cover
    val darkPref by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val systemDark = isSystemInDarkTheme()
    val useDark = if (darkPref == DarkMode.AUTO) systemDark else darkPref == DarkMode.ON
    val pureBlackPref by rememberPreference(PureBlackKey, false)
    val mono by rememberPreference(IsMonochromeThemeKey, false)
    val dynamicTheme by rememberPreference(DynamicThemeKey, true)
    val selectedInt by rememberPreference(SelectedThemeColorKey, DefaultThemeColor.toArgb())
    val selectedColor = Color(selectedInt)
    var themeColor by remember { mutableStateOf(selectedColor) }
    LaunchedEffect(dynamicTheme, selectedColor) {
      if (!dynamicTheme) { themeColor = selectedColor; return@LaunchedEffect }
      rt.playerConnection.mediaMetadata.collectLatest { song ->
        val url = song?.thumbnailUrl
        themeColor = if (url == null) selectedColor else withContext(Dispatchers.IO) {
          runCatching { Bitmap.makeFromImage(Image.makeFromEncoded(URL(url).readBytes())).extractThemeColor() }.getOrNull() ?: selectedColor
        }
      }
    }
    echomusicTheme(darkTheme = useDark, pureBlack = pureBlackPref && useDark, isMonochrome = mono, themeColor = themeColor) {
      Surface(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
          val playerState = rememberBottomSheetState(dismissedBound = 0.dp, collapsedBound = MiniPlayerHeight, expandedBound = maxHeight)
          val meta by rt.playerConnection.mediaMetadata.collectAsState()
          LaunchedEffect(meta == null) { if (meta == null) playerState.dismiss() else if (playerState.isDismissed) playerState.collapseSoft() }
          CompositionLocalProvider(LocalIsPlayerExpanded provides playerState.isExpanded, LocalExpandPlayer provides { playerState.expandSoft() }) {
          Column(Modifier.fillMaxSize().padding(bottom = if (meta != null) MiniPlayerHeight else 0.dp)) {
          with(windowScope) { DesktopTopBar(navController, windowState, onClose) }
          Row(Modifier.weight(1f).fillMaxWidth()) {
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
              // History / Stats / Settings sit at the bottom of the rail (a top-right overlay covered the Search bar's own buttons)
              androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
              listOf("history" to (R.drawable.music_history to "History"), "stats" to (R.drawable.stats to "Stats"), "settings" to (R.drawable.settings to "Settings")).forEach { (route, p) ->
                NavigationRailItem(
                  selected = current?.destination?.route?.startsWith(route) == true,
                  onClick = { navController.navigate(route) { launchSingleTop = true } },
                  icon = { Icon(painterResource(p.first), p.second) },
                  label = { Text(p.second) },
                )
              }
            }
            Column(Modifier.weight(1f)) {
              Box(Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = Screens.Home.route) {
                  navigationBuilder(navController, scrollBehavior, android.app.Activity(), snackbarHostState)
                }
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
