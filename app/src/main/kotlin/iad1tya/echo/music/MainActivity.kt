package iad1tya.echo.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.datastore.preferences.core.edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavGraph
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.echo.innertube.YouTube
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import iad1tya.echo.music.constants.AppBarHeight
import iad1tya.echo.music.constants.AppLanguageKey
import iad1tya.echo.music.constants.CheckForUpdatesKey
import iad1tya.echo.music.constants.LastImportantNoticeVersionKey
import iad1tya.echo.music.constants.LastLibSongSyncKey
import iad1tya.echo.music.constants.LastLikeSongSyncKey
import iad1tya.echo.music.constants.DarkModeKey
import iad1tya.echo.music.constants.DefaultOpenTabKey
import iad1tya.echo.music.constants.DisableScreenshotKey
import iad1tya.echo.music.constants.DynamicThemeKey
import iad1tya.echo.music.constants.MaterialYouKey
import iad1tya.echo.music.constants.MiniPlayerHeight
import iad1tya.echo.music.constants.MiniPlayerBottomSpacing
import iad1tya.echo.music.constants.UpdateNotificationsEnabledKey
import iad1tya.echo.music.constants.UseNewMiniPlayerDesignKey
import iad1tya.echo.music.constants.NavigationBarAnimationSpec
import iad1tya.echo.music.constants.NavigationBarHeight
import iad1tya.echo.music.constants.PauseSearchHistoryKey
import iad1tya.echo.music.constants.PureBlackKey
import iad1tya.echo.music.constants.SYSTEM_DEFAULT
import iad1tya.echo.music.constants.SearchSource
import iad1tya.echo.music.constants.SearchSourceKey
import iad1tya.echo.music.constants.SlimNavBarHeight
import iad1tya.echo.music.constants.SlimNavBarKey
import iad1tya.echo.music.constants.StopMusicOnTaskClearKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.SearchHistory
import iad1tya.echo.music.extensions.toEnum
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.DownloadUtil
import iad1tya.echo.music.playback.MusicService
import iad1tya.echo.music.playback.MusicService.MusicBinder
import iad1tya.echo.music.playback.PlayerConnection
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.ui.component.AccountSettingsDialog
import iad1tya.echo.music.ui.component.BottomSheetMenu
import iad1tya.echo.music.ui.component.BottomSheetPage
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.ImportantNoticeDialog
import iad1tya.echo.music.ui.component.LocalBottomSheetPageState
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.TopSearch
import iad1tya.echo.music.ui.component.rememberBottomSheetState
import iad1tya.echo.music.ui.component.shimmer.ShimmerTheme
import iad1tya.echo.music.ui.menu.YouTubeSongMenu
import iad1tya.echo.music.ui.player.BottomSheetPlayer
import iad1tya.echo.music.ui.screens.Screens
import iad1tya.echo.music.ui.screens.navigationBuilder
import iad1tya.echo.music.ui.screens.search.LocalSearchScreen
import iad1tya.echo.music.ui.screens.search.OnlineSearchScreen
import iad1tya.echo.music.ui.screens.settings.DarkMode
import iad1tya.echo.music.ui.screens.settings.NavigationTab
import iad1tya.echo.music.ui.theme.ColorSaver
import iad1tya.echo.music.ui.theme.DefaultThemeColor
import iad1tya.echo.music.ui.theme.EchoTheme
import iad1tya.echo.music.ui.theme.extractThemeColor
import iad1tya.echo.music.ui.utils.appBarScrollBehavior
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.ui.utils.resetHeightOffset
import iad1tya.echo.music.utils.SyncUtils
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.utils.reportException
import iad1tya.echo.music.utils.setAppLocale
import iad1tya.echo.music.viewmodels.HomeViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.days


@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var isServiceBound = false

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }
        try {
            startService(Intent(this, MusicService::class.java))
        } catch (e: Exception) {
             if (Build.VERSION.SDK_INT >= 31 && e.javaClass.name.contains("BackgroundServiceStartNotAllowedException")) {
                Log.e("MainActivity", "BackgroundServiceStartNotAllowedException caught", e)
             } else if (e is IllegalStateException) {
                 Log.e("MainActivity", "IllegalStateException caught in startService", e)
             } else {
                 throw e
             }
        }
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        isServiceBound = true
    }

    override fun onStop() {
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                // Service might interpret as not registered
                e.printStackTrace()
            }
            isServiceBound = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            if (isServiceBound) {
                 try {
                    unbindService(serviceConnection)
                } catch (e: IllegalArgumentException) {
                     e.printStackTrace()
                }
                isServiceBound = false
            }
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        setContent {
            val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)
            
            // Request all runtime permissions at app startup
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->

            }
            
            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                
                // Notification permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                
                // Bluetooth permissions (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                    }
                }
                
                // Location permissions for Cast device discovery (all Android versions)
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                
                // Nearby WiFi devices (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                    }
                }
                
                // Request all permissions at once
                if (permissionsToRequest.isNotEmpty()) {
                    permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            LaunchedEffect(checkForUpdates) {
                if (checkForUpdates) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val url = java.net.URL("https://api.github.com/repos/iad1tya/Echo-Music/releases/latest")
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.setRequestProperty("Accept", "application/json")
                            
                            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                            connection.disconnect()
                            
                            val json = org.json.JSONObject(responseText)
                            val tagName = json.getString("tag_name")
                            if (tagName.isNotEmpty()) {
                                val version = tagName.removePrefix("v")
                                withContext(Dispatchers.Main) {
                                    latestVersionName = version
                                    // Show notification if new version is available
                                    if (version != BuildConfig.VERSION_NAME) {
                                        showUpdateNotification(this@MainActivity, version)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    latestVersionName = BuildConfig.VERSION_NAME
                }
            }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val enableMaterialYou by rememberPreference(MaterialYouKey, defaultValue = false)
            
            // Read dark mode preference
            val darkModePreference by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
            val useDarkTheme = when (darkModePreference) {
                DarkMode.ON -> true
                DarkMode.OFF -> false
                DarkMode.AUTO -> isSystemInDarkTheme()
            }

            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }

            val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
            val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme 
            }

            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }

                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    if (song?.thumbnailUrl != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                val result = imageLoader.execute(
                                    ImageRequest.Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(false)
                                        .build()
                                )
                                themeColor = result.image?.toBitmap()?.extractThemeColor()
                                    ?: DefaultThemeColor
                            } catch (e: Exception) {
                                // Fallback to default on error
                                themeColor = DefaultThemeColor
                            }
                        }
                    } else {
                        themeColor = DefaultThemeColor
                    }
                }
            }

            EchoTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
                isDynamicColor = enableMaterialYou,
            ) {
                BoxWithConstraints(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                            )
                    ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val configuration = LocalConfiguration.current
                    val cutoutInsets = WindowInsets.displayCutout
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                    val navController = rememberNavController()

                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                    val (showFindInNavbar) = rememberPreference(iad1tya.echo.music.constants.ShowFindInNavbarKey, defaultValue = true)
                    val navigationItems = remember(showFindInNavbar) {
                        if (showFindInNavbar) Screens.MainScreens
                        else Screens.MainScreens.filter { it != Screens.Find }
                    }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                    val defaultOpenTab = remember {
                        dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                    }
                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_LIBRARY -> NavigationTab.LIBRARY
                            ACTION_SEARCH -> NavigationTab.SEARCH
                            else -> null
                        }
                    }

                    val topLevelScreens = remember {
                        listOf(
                            Screens.Home.route,
                            Screens.Search.route,
                            Screens.Library.route,
                            "settings",
                        )
                    }

                    // Define hierarchy for tabs to keep selection active
                    val libraryHierarchy = remember {
                        listOf(
                            Screens.Library.route,
                            "local_playlist/{playlistId}",
                            "online_playlist/{playlistId}",
                            "top_playlist/{top}",
                            "cache_playlist/{playlist}",
                            "auto_playlist/{playlist}",
                            "artist/{artistId}",
                            "artist/{artistId}/songs",
                            "artist/{artistId}/albums",
                            "artist/{artistId}/items",
                            "album/{albumId}",
                            "browse/{browseId}",
                            "youtube_browse/{browseId}?params={params}"
                        )
                    }

                    val homeHierarchy = remember {
                        listOf(
                            Screens.Home.route,
                            "new_release",
                            "charts_screen",
                            "mood_and_genres",
                            "history",
                            "stats"
                        )
                    }

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    // Speech recognition launcher
                    val speechRecognizerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
                            if (!spokenText.isNullOrEmpty()) {
                                onQueryChange(TextFieldValue(spokenText))
                                // Automatically search after speech input
                                lifecycleScope.launch {
                                    delay(300)
                                    onActiveChange(false)
                                    navController.navigate("search/${URLEncoder.encode(spokenText, "UTF-8")}") {
                                        popUpTo(Screens.Home.route)
                                    }
                                    
                                    if (dataStore[PauseSearchHistoryKey] != true) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            database.query {
                                                insert(SearchHistory(query = spokenText))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val onSearch: (String) -> Unit = remember {
                        { searchQuery ->
                            if (searchQuery.isNotEmpty()) {
                                onActiveChange(false)
                                // Navigate to search results and pop Search tab from back stack
                                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}") {
                                    popUpTo(Screens.Home.route)
                                }

                                if (dataStore[PauseSearchHistoryKey] != true) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        database.query {
                                            insert(SearchHistory(query = searchQuery))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val inSearchScreen = remember(navBackStackEntry) {
                        navBackStackEntry?.destination?.route?.startsWith("search/") == true
                    }
                    
                    val isAmbientMode = remember(navBackStackEntry) {
                        navBackStackEntry?.destination?.route == "ambient_mode"
                    }

                    val isFindScreen = remember(navBackStackEntry) {
                        navBackStackEntry?.destination?.route == Screens.Find.route
                    }
                    
                    val isWrappedScreen = remember(navBackStackEntry) {
                        navBackStackEntry?.destination?.route == "wrapped"
                    }

                    val shouldShowSearchBar = remember(active, navBackStackEntry, isFindScreen) {
                        active ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                inSearchScreen ||
                                (active && !isFindScreen) 
                    }

                    val shouldShowNavigationBar = remember(navBackStackEntry, active, isAmbientMode, isFindScreen, isWrappedScreen) {
                        !isAmbientMode && !isFindScreen && !isWrappedScreen
                    }

                    val isLandscape = remember(configuration) {
                        configuration.screenWidthDp > configuration.screenHeightDp
                    }
                    val showRail = isLandscape && !inSearchScreen && !isAmbientMode && !isFindScreen

                    val getNavPadding: () -> Dp = remember(shouldShowNavigationBar, showRail, slimNav) {
                        {
                            if (shouldShowNavigationBar && !showRail) {
                                if (slimNav) SlimNavBarHeight else NavigationBarHeight
                            } else {
                                0.dp
                            }
                        }
                    }

                    val targetNavBarHeight = if (slimNav) SlimNavBarHeight else NavigationBarHeight

                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar && !showRail) targetNavBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset +
                                (if (!showRail && shouldShowNavigationBar) getNavPadding() else 0.dp) +
                                (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val playerAwareWindowInsets = remember(
                        bottomInset,
                        shouldShowNavigationBar,
                        playerBottomSheetState.isDismissed,
                        showRail,
                    ) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar && !showRail) {
                            bottom += NavigationBarHeight
                        }
                        if (!playerBottomSheetState.isDismissed && !isFindScreen) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    appBarScrollBehavior(
                        canScroll = {
                            !inSearchScreen &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                !inSearchScreen &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                !inSearchScreen &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    // Navigation tracking
                    LaunchedEffect(navBackStackEntry) {
                        if (inSearchScreen) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }

                        // Reset scroll behavior for main navigation items
                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            if (navigationItems.fastAny { it.route == previousTab }) {
                                searchBarScrollBehavior.state.resetHeightOffset()
                            }
                        }

                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()

                        // Track previous tab for animations
                        navController.currentBackStackEntry?.destination?.route?.let {
                            setPreviousTab(it)
                        }
                    }

                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry) {
                        shouldShowTopBar =
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }

                    LaunchedEffect(Unit) {
                        if (pendingIntent != null) {
                            handleDeepLinkIntent(pendingIntent!!, navController)
                            pendingIntent = null
                        } else {
                            handleDeepLinkIntent(intent, navController)
                        }
                    }

                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent ->
                            handleDeepLinkIntent(intent, navController)
                        }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitleRes = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }

                    var showAccountDialog by remember { mutableStateOf(false) }

                    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                    val insetBg = if (playerBottomSheetState.progress > 0f) Color.Transparent else baseBg

                    // Handle back press on search screens - navigate to home instead of search tab
                    val isOnSearchTab = navBackStackEntry?.destination?.route == Screens.Search.route
                    val isOnSearchResults = navBackStackEntry?.destination?.route?.startsWith("search/") == true
                    BackHandler(enabled = isOnSearchTab || isOnSearchResults) {
                        if (active) {
                            onActiveChange(false)
                        }
                        navController.navigate(Screens.Home.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    }

                    // Handle back press on Find screen - navigate to home instead of closing app
                    BackHandler(enabled = isFindScreen) {
                        navController.navigate(Screens.Home.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                    ) {
                        Scaffold(
                            topBar = {
                                AnimatedVisibility(
                                    visible = shouldShowTopBar,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { -it / 4 },
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearEasing)),
                                    exit = slideOutHorizontally(
                                        targetOffsetX = { -it / 4 },
                                        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = LinearEasing))
                                ) {
                                    Box {
                                        // Blurred background - always visible when navbar shows
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .zIndex(10f)
                                                .then(
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                        Modifier.graphicsLayer {
                                                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                                                25f,
                                                                25f,
                                                                android.graphics.Shader.TileMode.CLAMP
                                                            ).asComposeRenderEffect()
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            if (pureBlack) 
                                                                Color.Black.copy(alpha = 0.98f)
                                                            else if (useDarkTheme)
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                                                            else
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
                                                            if (pureBlack) 
                                                                Color.Black.copy(alpha = 0.90f)
                                                            else if (useDarkTheme)
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                                            else
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        
                                        Row(modifier = Modifier.zIndex(11f)) {
                                            TopAppBar(
                                                title = {
                                                    Text(
                                                        text = if (navBackStackEntry?.destination?.route == Screens.Home.route) {
                                                            "Echo"
                                                        } else {
                                                            currentTitleRes?.let { stringResource(it) } ?: ""
                                                        },
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = if (navBackStackEntry?.destination?.route == Screens.Home.route) 28.sp else MaterialTheme.typography.titleLarge.fontSize
                                                        ),
                                                    )
                                                },
                                                actions = {
                                                    IconButton(onClick = { navController.navigate("history") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.history),
                                                            contentDescription = stringResource(R.string.history)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate("stats") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.stats),
                                                            contentDescription = stringResource(R.string.stats),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(onClick = { showAccountDialog = true }) {
                                                        BadgedBox(
                                                            badge = {
                                                                if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                                    Badge(
                                                                        containerColor = MaterialTheme.colorScheme.error
                                                                    )
                                                                }
                                                            }
                                                        ) {
                                                            if (accountImageUrl != null) {
                                                                AsyncImage(
                                                                    model = accountImageUrl,
                                                                    contentDescription = stringResource(R.string.account),
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .clip(CircleShape)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.account),
                                                                    contentDescription = stringResource(R.string.account),
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                scrollBehavior = searchBarScrollBehavior,
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = Color.Transparent,
                                                    scrolledContainerColor = Color.Transparent,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.windowInsetsPadding(
                                                    if (showRail) {
                                                        WindowInsets(left = NavigationBarHeight)
                                                            .add(cutoutInsets.only(WindowInsetsSides.Start))
                                                    } else {
                                                        cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                                AnimatedVisibility(
                                    visible = active || inSearchScreen,
                                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(150)),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(100))
                                ) {
                                    TopSearch(
                                        query = query,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        active = active,
                                        onActiveChange = onActiveChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.string.search_library
                                                        SearchSource.ONLINE -> R.string.search_yt_music
                                                    }
                                                ),
                                            )
                                        },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    when {
                                                        active -> {
                                                            onActiveChange(false)
                                                            navController.navigate(Screens.Home.route) {
                                                                popUpTo(navController.graph.id)
                                                            }
                                                        }
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                            navController.navigateUp()
                                                        }

                                                        else -> onActiveChange(true)
                                                    }
                                                },
                                                onLongClick = {
                                                    when {
                                                        active -> {}
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                            navController.backToMain()
                                                        }
                                                        else -> {}
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(
                                                        if (active ||
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                        ) {
                                                            R.drawable.arrow_back
                                                        } else {
                                                            R.drawable.search
                                                        },
                                                    ),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Row {
                                                if (active) {
                                                    if (query.text.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                onQueryChange(
                                                                    TextFieldValue(
                                                                        ""
                                                                    )
                                                               )
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.close),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                    // Microphone button for voice search
                                                    IconButton(
                                                        onClick = {
                                                            try {
                                                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the song name")
                                                                }
                                                                speechRecognizerLauncher.launch(intent)
                                                            } catch (e: Exception) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Speech recognition not available",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.mic),
                                                            contentDescription = "Voice search",
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            searchSource =
                                                                if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.drawable.library_music
                                                                    SearchSource.ONLINE -> R.drawable.language
                                                                },
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        focusRequester = searchBarFocusRequester,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .windowInsetsPadding(
                                                if (showRail) {
                                                    WindowInsets(left = NavigationBarHeight)
                                                } else {
                                                    WindowInsets(0.dp)
                                                }
                                            ),
                                        colors = if (pureBlack && active) {
                                            SearchBarDefaults.colors(
                                                containerColor = Color.Black,
                                                dividerColor = Color.DarkGray,
                                                inputFieldColors = TextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.Gray,
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    cursorColor = Color.White,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                )
                                            )
                                        } else {
                                            SearchBarDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                            )
                                        }
                                    ) {
                                        // Disable crossfade transitions as requested
                                        Crossfade(
                                            targetState = searchSource,
                                            label = "",
                                            animationSpec = tween(150, easing = FastOutSlowInEasing),
                                            modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                                .navigationBarsPadding(),
                                        ) { searchSource ->
                                            when (searchSource) {
                                                SearchSource.LOCAL ->
                                                    LocalSearchScreen(
                                                        query = query.text,
                                                        navController = navController,
                                                        onDismiss = { onActiveChange(false) },
                                                        pureBlack = pureBlack,
                                                    )

                                                SearchSource.ONLINE ->
                                                    OnlineSearchScreen(
                                                        query = query.text,
                                                        onQueryChange = onQueryChange,
                                                        navController = navController,
                                                        onSearch = { searchQuery ->
                                                            navController.navigate(
                                                                "search/${URLEncoder.encode(searchQuery, "UTF-8")}"
                                                            ) {
                                                                popUpTo(Screens.Home.route)
                                                            }
                                                            if (dataStore[PauseSearchHistoryKey] != true) {
                                                                lifecycleScope.launch(Dispatchers.IO) {
                                                                    database.query {
                                                                        insert(SearchHistory(query = searchQuery))
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onDismiss = { onActiveChange(false) },
                                                        pureBlack = pureBlack
                                                    )
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                if (!isAmbientMode && !isWrappedScreen) {
                                    if (isFindScreen) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack
                                        )
                                    } else if (!showRail) {
                                        Box {
                                            BottomSheetPlayer(
                                                state = playerBottomSheetState,
                                                navController = navController,
                                                pureBlack = pureBlack
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .height(bottomInset + getNavPadding())
                                                    .fillMaxWidth()
                                                    .offset {
                                                        if (navigationBarHeight == 0.dp) {
                                                            IntOffset(
                                                                x = 0,
                                                                y = (bottomInset + targetNavBarHeight).roundToPx(),
                                                            )
                                                        } else {
                                                            val slideOffset =
                                                                (bottomInset + targetNavBarHeight) *
                                                                        playerBottomSheetState.progress.coerceIn(
                                                                            0f,
                                                                            1f,
                                                                        )
                                                            val hideOffset =
                                                                (bottomInset + targetNavBarHeight) * (1 - navigationBarHeight / targetNavBarHeight)
                                                            IntOffset(
                                                                x = 0,
                                                                y = (slideOffset + hideOffset).roundToPx(),
                                                            )
                                                        }
                                                    }
                                                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer)
                                            ) {
                                                NavigationBar(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .height(bottomInset + getNavPadding()),
                                                    containerColor = Color.Transparent,
                                                    contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                ) {
                                                    navigationItems.fastForEach { screen ->
                                                        val isSelected = when (screen) {
                                                            Screens.Library -> navBackStackEntry?.destination?.route?.let { route ->
                                                                libraryHierarchy.any { 
                                                                    if (it.contains("/{")) 
                                                                        route.startsWith(it.substringBefore("/{")) 
                                                                    else 
                                                                        route == it 
                                                                }
                                                            } == true
                                                            Screens.Home -> navBackStackEntry?.destination?.route?.let { route ->
                                                                homeHierarchy.any { 
                                                                    if (it.contains("/{")) 
                                                                        route.startsWith(it.substringBefore("/{")) 
                                                                    else 
                                                                        route == it 
                                                                }
                                                            } == true
                                                            else -> navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                                                        }

                                                        NavigationBarItem(
                                                            selected = isSelected,
                                                            icon = {
                                                                if (screen.route == Screens.Settings.route) {
                                                                    BadgedBox(badge = {
                                                                        if (latestVersionName != BuildConfig.VERSION_NAME) {
                                                                            Badge()
                                                                        }
                                                                    }) {
                                                                        Icon(
                                                                            painter = painterResource(
                                                                                id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                                            ),
                                                                            contentDescription = null,
                                                                        )
                                                                    }
                                                                } else {
                                                                    Icon(
                                                                        painter = painterResource(
                                                                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                                        ),
                                                                        contentDescription = null,
                                                                    )
                                                                }
                                                            },
                                                            label = {
                                                                if (!slimNav) {
                                                                    Text(
                                                                        text = stringResource(screen.titleId),
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                if (isSelected) {
                                                                    // If already on the start destination of the tab, scroll to top
                                                                    // Otherwise, pop back to the start destination
                                                                    val currentRoute = navBackStackEntry?.destination?.route
                                                                    if (currentRoute != screen.route) {
                                                                        navController.popBackStack(screen.route, false)
                                                                    } else {
                                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                                            "scrollToTop",
                                                                            true
                                                                        )
                                                                        coroutineScope.launch {
                                                                            searchBarScrollBehavior.state.resetHeightOffset()
                                                                        }
                                                                    }
                                                                } else {
                                                                    // Close search bar when navigating away from search
                                                                    if (navBackStackEntry?.destination?.route == Screens.Search.route && screen.route != Screens.Search.route) {
                                                                        onActiveChange(false)
                                                                    }
                                                                    // Navigate to the screen
                                                                    if (screen.route == Screens.Home.route) {
                                                                        navController.navigate(screen.route) {
                                                                            popUpTo(navController.graph.id) {
                                                                                inclusive = true
                                                                            }
                                                                        }
                                                                    } else {
                                                                        navController.navigate(screen.route) {
                                                                            popUpTo(navController.graph.id) {
                                                                                saveState = true
                                                                            }
                                                                            launchSingleTop = true
                                                                            restoreState = true
                                                                        }
                                                                    }
                                                                    // Open search bar when navigating to search
                                                                    if (screen.route == Screens.Search.route) {
                                                                        onActiveChange(true)
                                                                    }
                                                                }
                                                            },
                                                        )
                                                    }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(insetBg)
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .height(bottomInsetDp)
                                            )
                                        }
                                    } else {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack
                                        )

                                        Box(
                                            modifier = Modifier
                                                .background(insetBg)
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .height(bottomInsetDp)
                                        )
                                    }
                                }
                            },

                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                if (showRail) {
                                    NavigationRail(
                                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Spacer(modifier = Modifier.weight(1f))

                                        navigationItems.fastForEach { screen ->
                                            val isSelected = when (screen) {
                                                Screens.Library -> navBackStackEntry?.destination?.route?.let { route ->
                                                    libraryHierarchy.any { 
                                                        if (it.contains("/{")) 
                                                            route.startsWith(it.substringBefore("/{")) 
                                                        else 
                                                            route == it 
                                                    }
                                                } == true
                                                Screens.Home -> navBackStackEntry?.destination?.route?.let { route ->
                                                    homeHierarchy.any { 
                                                        if (it.contains("/{")) 
                                                            route.startsWith(it.substringBefore("/{")) 
                                                        else 
                                                            route == it 
                                                    }
                                                } == true
                                                else -> navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                                            }
                                            NavigationRailItem(
                                                selected = isSelected,
                                                onClick = {
                                                    if (isSelected) {
                                                                    // If already on the start destination of the tab, scroll to top
                                                                    // Otherwise, pop back to the start destination
                                                                    val currentRoute = navBackStackEntry?.destination?.route
                                                                    if (currentRoute != screen.route) {
                                                                        navController.popBackStack(screen.route, false)
                                                                    } else {
                                                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                                                            "scrollToTop",
                                                                            true
                                                                        )
                                                                        coroutineScope.launch {
                                                                            searchBarScrollBehavior.state.resetHeightOffset()
                                                                        }
                                                                    }
                                                    } else {
                                                        // Close search bar when navigating away from search
                                                        if (navBackStackEntry?.destination?.route == Screens.Search.route && screen.route != Screens.Search.route) {
                                                            onActiveChange(false)
                                                        }
                                                        // Navigate to the screen
                                                        if (screen.route == Screens.Home.route) {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(navController.graph.id) {
                                                                    inclusive = true
                                                                }
                                                            }
                                                        } else {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(navController.graph.id) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                        // Open search bar when navigating to search
                                                        if (screen.route == Screens.Search.route) {
                                                            onActiveChange(true)
                                                        }
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        painter = painterResource(
                                                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                        ),
                                                        contentDescription = null,
                                                    )
                                                },
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Box(Modifier.weight(1f)) {
                                    // NavHost with smooth animations
                                    NavHost(
                                        navController = navController,
                                        startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                            NavigationTab.HOME -> Screens.Home
                                            NavigationTab.LIBRARY -> Screens.Library
                                            NavigationTab.FIND -> Screens.Find
                                            else -> Screens.Home
                                        }.route,
                                        // Enter Transition - smoother with easing
                                        enterTransition = {
                                            if (targetState.destination.route == "ambient_mode") {
                                                fadeIn(animationSpec = tween(700, easing = LinearEasing))
                                            } else {
                                                val currentRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == targetState.destination.route
                                                }
                                                val previousRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == initialState.destination.route
                                                }

                                                if (targetState.destination.route == Screens.Find.route) {
                                                    fadeIn(animationSpec = tween(300))
                                                } else if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex)
                                                    slideInHorizontally(
                                                        initialOffsetX = { it / 4 },
                                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                    ) + fadeIn(tween(250, easing = LinearEasing))
                                                else
                                                    slideInHorizontally(
                                                        initialOffsetX = { -it / 4 },
                                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                    ) + fadeIn(tween(250, easing = LinearEasing))
                                            }
                                        },
                                        // Exit Transition - smoother
                                        exitTransition = {
                                            if (initialState.destination.route == "ambient_mode") {
                                                fadeOut(animationSpec = tween(700, easing = LinearEasing))
                                            } else {
                                                val currentRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == initialState.destination.route
                                                }
                                                val targetRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == targetState.destination.route
                                                }

                                                if (initialState.destination.route == Screens.Find.route) {
                                                    fadeOut(animationSpec = tween(300))
                                                } else if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex)
                                                    slideOutHorizontally(
                                                        targetOffsetX = { -it / 4 },
                                                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                                                    ) + fadeOut(tween(200, easing = LinearEasing))
                                                else
                                                    slideOutHorizontally(
                                                        targetOffsetX = { it / 4 },
                                                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                                                    ) + fadeOut(tween(200, easing = LinearEasing))
                                            }
                                        },
                                        // Pop Enter Transition
                                        popEnterTransition = {
                                            if (targetState.destination.route == "ambient_mode") {
                                                fadeIn(animationSpec = tween(700, easing = LinearEasing))
                                            } else {
                                                val currentRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == targetState.destination.route
                                                }
                                                val previousRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == initialState.destination.route
                                                }

                                                if (targetState.destination.route == Screens.Find.route) {
                                                    fadeIn(animationSpec = tween(300))
                                                } else if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex)
                                                    slideInHorizontally(
                                                        initialOffsetX = { it / 4 },
                                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                    ) + fadeIn(tween(250, easing = LinearEasing))
                                                else
                                                    slideInHorizontally(
                                                        initialOffsetX = { -it / 4 },
                                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                                    ) + fadeIn(tween(250, easing = LinearEasing))
                                            }
                                        },
                                        // Pop Exit Transition
                                        popExitTransition = {
                                            if (initialState.destination.route == "ambient_mode") {
                                                fadeOut(animationSpec = tween(700, easing = LinearEasing))
                                            } else {
                                                val currentRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == initialState.destination.route
                                                }
                                                val targetRouteIndex = navigationItems.indexOfFirst {
                                                    it.route == targetState.destination.route
                                                }

                                                if (initialState.destination.route == Screens.Find.route) {
                                                    fadeOut(animationSpec = tween(300))
                                                } else if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex)
                                                    slideOutHorizontally(
                                                        targetOffsetX = { -it / 4 },
                                                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                                                    ) + fadeOut(tween(200, easing = LinearEasing))
                                                else
                                                    slideOutHorizontally(
                                                        targetOffsetX = { it / 4 },
                                                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                                                    ) + fadeOut(tween(200, easing = LinearEasing))
                                            }
                                        },
                                        modifier = Modifier.nestedScroll(
                                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                                inSearchScreen
                                            ) {
                                                searchBarScrollBehavior.nestedScrollConnection
                                            } else {
                                                topAppBarScrollBehavior.nestedScrollConnection
                                            }
                                        )
                                    ) {
                                        navigationBuilder(
                                            navController,
                                            topAppBarScrollBehavior,
                                            latestVersionName,
                                            onOpenPlayer = {
                                                coroutineScope.launch {
                                                    playerBottomSheetState.expandSoft()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (!isAmbientMode) {
                            BottomSheetMenu(
                                state = LocalMenuState.current,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            BottomSheetPage(
                                state = LocalBottomSheetPageState.current,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        if (showAccountDialog) {
                            AccountSettingsDialog(
                                navController = navController,
                                onDismiss = {
                                    showAccountDialog = false
                                    homeViewModel.refresh()
                                },
                                latestVersionName = latestVersionName
                            )
                        }

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val lastNoticeVersion by rememberPreference(LastImportantNoticeVersionKey, defaultValue = "")
                    var showNoticeDialog by remember { mutableStateOf(false) }
                    var isPreferenceLoaded by remember { mutableStateOf(false) }

                    // Only check after we are sure preferences are loaded (simple heuristic: defaultValue is empty)
                    // Better approach: use a dedicated "loaded" state if rememberPreference supported it,
                    // but here we wait for the first composition where lastNoticeVersion matches expectations or we simply delay slightly.
                    // Actually, rememberPreference is a composable that returns state.
                    // The issue is likely that on some devices, the write doesn't happen fast enough or the read is stale.

                    LaunchedEffect(lastNoticeVersion) {
                        if (lastNoticeVersion != BuildConfig.VERSION_NAME && lastNoticeVersion.isNotEmpty()) {
                            // If we have a stored version that isn't null/empty but doesn't match current, show dialog.
                            // However, we also want to show it on first install if we want to force it?
                            // The defaultValue is "", so if it is "" it might be first run.
                            // Let's assume we want to show it if it doesn't match.
                             showNoticeDialog = true
                        } else if (lastNoticeVersion == "") {
                             // First run or cleared data, maybe don't show or show?
                             // Assuming we want to show it to everyone on this version.
                             showNoticeDialog = true
                        }
                    }

                    if (showNoticeDialog && lastNoticeVersion != BuildConfig.VERSION_NAME) {
                        ImportantNoticeDialog(
                            onDismiss = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    dataStore.edit {
                                        it[LastImportantNoticeVersionKey] = BuildConfig.VERSION_NAME
                                    }
                                }
                                showNoticeDialog = false
                            }
                        )
                    }



                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            searchBarFocusRequester.requestFocus()
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        if (intent.action == ACTION_FIND) {
            navController.navigate(Screens.Find.route)
            return
        }
        // Handle both VIEW and SEND actions
        val uri = when (intent.action) {
            Intent.ACTION_SEND -> {
                // Handle shared text (from YouTube share)
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                    // Extract URL from shared text
                    val urlPattern = "(https?://[^\\s]+)".toRegex()
                    urlPattern.find(sharedText)?.value?.toUri()
                }
            }
            Intent.ACTION_VIEW -> intent.data
            else -> intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri()
        } ?: return
        
        val coroutineScope = lifecycleScope

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                withContext(Dispatchers.Main) {
                                    navController.navigate("album/$browseId")
                                }
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                    else -> null
                }

                val playlistId = uri.getQueryParameter("list")

                videoId?.let {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(it), playlistId).onSuccess { queue ->
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                        queue.firstOrNull()?.toMediaMetadata()
                                    )
                                )
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    private fun showUpdateNotification(context: Context, version: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "updates",
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for app updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to open MainActivity with settings
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openSettings", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, "updates")
            .setSmallIcon(R.drawable.update)
            .setContentTitle("Echo Music Update Available")
            .setContentText("Version $version is now available")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A new version ($version) of Echo Music is available. Tap to download and install."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(1001, notification)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "iad1tya.echo.music.action.SEARCH"
        const val ACTION_LIBRARY = "iad1tya.echo.music.action.LIBRARY"
        const val ACTION_FIND = "iad1tya.echo.music.action.FIND"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
