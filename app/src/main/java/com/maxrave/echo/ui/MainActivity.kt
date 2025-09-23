package iad1tya.echo.music.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import iad1tya.echo.music.R
import iad1tya.echo.music.common.FIRST_TIME_MIGRATION
import iad1tya.echo.music.common.SELECTED_LANGUAGE
import iad1tya.echo.music.common.STATUS_DONE
import iad1tya.echo.music.common.SUPPORTED_LANGUAGE
import iad1tya.echo.music.common.SUPPORTED_LOCATION
import iad1tya.echo.music.data.dataStore.DataStoreManager
import iad1tya.echo.music.di.viewModelModule
import iad1tya.echo.music.service.SimpleMediaService
import iad1tya.echo.music.ui.component.AppBottomNavigationBar
import iad1tya.echo.music.ui.navigation.destination.home.HomeDestination
import iad1tya.echo.music.ui.navigation.destination.home.NotificationDestination
import iad1tya.echo.music.ui.navigation.destination.list.AlbumDestination
import iad1tya.echo.music.ui.navigation.destination.list.ArtistDestination
import iad1tya.echo.music.ui.navigation.destination.list.PlaylistDestination
import iad1tya.echo.music.ui.navigation.destination.welcome.WelcomeDestination
import iad1tya.echo.music.ui.navigation.graph.AppNavigationGraph
import iad1tya.echo.music.ui.screen.MiniPlayer
import iad1tya.echo.music.ui.screen.player.NowPlayingScreen
import iad1tya.echo.music.ui.theme.AppTheme
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.utils.VersionManager
import iad1tya.echo.music.utils.AnalyticsHelper
import iad1tya.echo.music.viewModel.SettingsViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import iad1tya.echo.music.viewModel.WelcomeViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.Locale

@UnstableApi
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    val viewModel: SharedViewModel by inject()
    val welcomeViewModel: WelcomeViewModel by inject()
    val settingsViewModel: SettingsViewModel by inject()

    private var mBound = false
    private var shouldUnbind = false
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                try {
                    if (service is SimpleMediaService.MusicBinder) {
                        Log.w("MainActivity", "onServiceConnected: ")
                        mBound = true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error connecting to service: ${e.message}", e)
                    mBound = false
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                try {
                    Log.w("MainActivity", "onServiceDisconnected: ")
                    mBound = false
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error disconnecting from service: ${e.message}", e)
                }
            }
        }

    override fun onStart() {
        try {
            super.onStart()
            startMusicService()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onStart: ${e.message}", e)
        }
    }

    override fun onStop() {
        try {
            super.onStop()
            if (shouldUnbind) {
                unbindService(serviceConnection)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onStop: ${e.message}", e)
        }
    }

    override fun onPause() {
        try {
            super.onPause()
            AnalyticsHelper.logAppBackgrounded()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onPause: ${e.message}", e)
        }
    }

    override fun onResume() {
        try {
            super.onResume()
            AnalyticsHelper.logScreenViewed("MainActivity")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume: ${e.message}", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        try {
            super.onNewIntent(intent)
            Log.d("MainActivity", "onNewIntent: $intent")
            viewModel.setIntent(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onNewIntent: ${e.message}", e)
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray,
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
//    }

    @UnstableApi
    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            // Recreate view model to fix the issue of view model not getting data from the service
            unloadKoinModules(viewModelModule)
            loadKoinModules(viewModelModule)
            VersionManager.initialize(applicationContext)
            AnalyticsHelper.logAppOpened()
            checkForUpdate()
            if (viewModel.recreateActivity.value || viewModel.isServiceRunning) {
                viewModel.activityRecreateDone()
            } else {
                startMusicService()
            }
            Log.d("MainActivity", "onCreate: ")
            val data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
            if (data != null) {
                viewModel.setIntent(intent)
            }
            Log.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            // Show error toast
            try {
                Toast.makeText(this, "App initialization error. Please restart.", Toast.LENGTH_LONG).show()
            } catch (toastError: Exception) {
                Log.e("MainActivity", "Error showing toast: ${toastError.message}", toastError)
            }
        }

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Log.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Log.d(
                    "Contains",
                    "onCreate: ${
                        SUPPORTED_LANGUAGE.codes.contains(
                            Locale.getDefault().toLanguageTag(),
                        )
                    }",
                )
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    putString("location", Locale.getDefault().country)
                } else {
                    putString("location", "US")
                }
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
            }
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                Log.d("Locale Key", "getString: $it")
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            getString(
                SELECTED_LANGUAGE,
            )
        ) {
            Log.d(
                "Locale Key",
                "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}",
            )
            putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }

        enableEdgeToEdge(
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
        )
        viewModel.checkIsRestoring()
        viewModel.runWorker()

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.this_app_needs_to_access_your_notification),
                    1,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }
            viewModel.getLocation()

        setContent {
            val navController = rememberNavController()

            val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
            val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
            val updateData by viewModel.updateResponse.collectAsStateWithLifecycle()
            val intent by viewModel.intent.collectAsStateWithLifecycle()
            
                // Check if this is the first launch
                val isFirstLaunch by welcomeViewModel.isFirstLaunch.collectAsStateWithLifecycle()
                
                

            val isTranslucentBottomBar by viewModel.getTranslucentBottomBar().collectAsStateWithLifecycle(DataStoreManager.FALSE)
            // MiniPlayer visibility logic
            var isShowMiniPlayer by rememberSaveable {
                mutableStateOf(true)
            }

            // Now playing screen
            var isShowNowPlaylistScreen by rememberSaveable {
                mutableStateOf(false)
            }

            var isNavBarVisible by rememberSaveable {
                mutableStateOf(true)
            }

            var shouldShowUpdateDialog by rememberSaveable {
                mutableStateOf(false)
            }

            LaunchedEffect(nowPlayingData) {
                try {
                    if (nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == MediaItem.EMPTY) {
                        isShowMiniPlayer = false
                    } else {
                        isShowMiniPlayer = true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling now playing data: ${e.message}", e)
                    isShowMiniPlayer = false // Default to hiding mini player on error
                }
            }

            // Track current navigation destination
            val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = currentNavBackStackEntry?.destination?.route
            
            // Hide navbar on welcome screen and name input screen
            LaunchedEffect(currentDestination) {
                try {
                    val isWelcomeScreen = currentDestination?.contains("welcome.WelcomeDestination") == true
                    val isNameInputScreen = currentDestination?.contains("welcome.UserNameDestination") == true
                    val shouldHideNavBar = isWelcomeScreen || isNameInputScreen
                    isNavBarVisible = !shouldHideNavBar
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling navigation destination: ${e.message}", e)
                    isNavBarVisible = true // Default to showing navbar on error
                }
            }

            LaunchedEffect(intent) {
                try {
                    val intent = intent ?: return@LaunchedEffect
                    val data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                    Log.d("MainActivity", "onCreate: $data")
                    if (data != null) {
                        if (data == "echo://notification".toUri()) {
                            viewModel.setIntent(null)
                            navController.navigate(
                                NotificationDestination,
                            )
                        } else {
                        Log.d("MainActivity", "onCreate: $data")
                        when (val path = data.pathSegments.firstOrNull()) {
                            "playlist" ->
                                data
                                    .getQueryParameter("list")
                                    ?.let { playlistId ->
                                        viewModel.setIntent(null)
                                        if (playlistId.startsWith("OLAK5uy_")) {
                                            navController.navigate(
                                                AlbumDestination(
                                                    browseId = playlistId,
                                                ),
                                            )
                                        } else if (playlistId.startsWith("VL")) {
                                            navController.navigate(
                                                PlaylistDestination(
                                                    playlistId = playlistId,
                                                ),
                                            )
                                        } else {
                                            navController.navigate(
                                                PlaylistDestination(
                                                    playlistId = "VL$playlistId",
                                                ),
                                            )
                                        }
                                    }

                            "channel", "c" ->
                                data.lastPathSegment?.let { artistId ->
                                    if (artistId.startsWith("UC")) {
                                        viewModel.setIntent(null)
                                        navController.navigate(
                                            ArtistDestination(
                                                channelId = artistId,
                                            ),
                                        )
                                    } else {
                                        Toast
                                            .makeText(
                                                this@MainActivity,
                                                getString(
                                                    R.string.this_link_is_not_supported,
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }

                            else ->
                                when {
                                    path == "watch" -> data.getQueryParameter("v")
                                    data.host == "youtu.be" -> path
                                    else -> null
                                }?.let { videoId ->
                                    viewModel.loadSharedMediaItem(videoId)
                                }
                        }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling intent: ${e.message}", e)
                }
            }

            LaunchedEffect(updateData) {
                try {
                    val response = updateData ?: return@LaunchedEffect
                    if (!this@MainActivity.isInPictureInPictureMode &&
                        viewModel.showedUpdateDialog &&
                        response.tagName != getString(R.string.version_format, VersionManager.getVersionName())
                    ) {
                        shouldShowUpdateDialog = true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling update data: ${e.message}", e)
                }
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            LaunchedEffect(navBackStackEntry) {
                try {
                    Log.d("MainActivity", "Current destination: ${navBackStackEntry?.destination?.route}")
                    if (navBackStackEntry?.destination?.route?.contains("FullscreenDestination") == true) {
                        isShowNowPlaylistScreen = false
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling navigation back stack: ${e.message}", e)
                }
            }

            AppTheme {
                Scaffold(
                    bottomBar = {
                        AnimatedVisibility(
                            isNavBarVisible,
                            enter = fadeIn() + slideInHorizontally(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                AnimatedVisibility(
                                    isShowMiniPlayer,
                                    enter = fadeIn() + slideInHorizontally(),
                                    exit = fadeOut(),
                                ) {
                                    MiniPlayer(
                                        Modifier
                                            .height(72.dp)
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                            ).padding(
                                                bottom = 4.dp,
                                            ),
                                        onClick = {
                                            isShowNowPlaylistScreen = true
                                        },
                                        onClose = {
                                            viewModel.stopPlayer()
                                            viewModel.isServiceRunning = false
                                        },
                                    )
                                }
                                AppBottomNavigationBar(
                                    navController = navController,
                                    isTranslucentBackground = isTranslucentBottomBar == DataStoreManager.TRUE,
                                ) { klass ->
                                    viewModel.reloadDestination(klass)
                                }
                            }
                        }
                    },
                    content = { innerPadding ->
                        AppNavigationGraph(
                            innerPadding = innerPadding,
                            navController = navController,
                            startDestination = if (isFirstLaunch) WelcomeDestination else HomeDestination,
                            hideNavBar = {
                                isNavBarVisible = false
                            },
                            showNavBar = {
                                isNavBarVisible = true
                            },
                            showNowPlayingSheet = {
                                isShowNowPlaylistScreen = true
                            },
                        )

                        if (isShowNowPlaylistScreen) {
                            NowPlayingScreen(
                                navController = navController,
                            ) {
                                isShowNowPlaylistScreen = false
                            }
                        }

                        if (sleepTimerState.isDone) {
                            Log.w("MainActivity", "Sleep Timer Done: $sleepTimerState")
                            AlertDialog(
                                properties =
                                    DialogProperties(
                                        dismissOnBackPress = false,
                                        dismissOnClickOutside = false,
                                    ),
                                onDismissRequest = {
                                    viewModel.stopSleepTimer()
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.stopSleepTimer()
                                    }) {
                                        Text(
                                            stringResource(R.string.yes),
                                            style = typo.bodySmall,
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.sleep_timer_off),
                                        style = typo.labelSmall,
                                    )
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.good_night),
                                        style = typo.bodySmall,
                                    )
                                },
                            )
                        }

                        if (shouldShowUpdateDialog) {
                            val response = updateData ?: return@Scaffold
                            AlertDialog(
                                properties =
                                    DialogProperties(
                                        dismissOnBackPress = false,
                                        dismissOnClickOutside = false,
                                    ),
                                onDismissRequest = {
                                    shouldShowUpdateDialog = false
                                    viewModel.showedUpdateDialog = false
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            shouldShowUpdateDialog = false
                                            viewModel.showedUpdateDialog = false
                                            val browserIntent =
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    "https://echomusic.fun".toUri(),
                                                )
                                            startActivity(browserIntent)
                                        },
                                        modifier = Modifier.fillMaxWidth(0.5f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.download),
                                            style = typo.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = androidx.compose.ui.graphics.Color.Black
                                        )
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = {
                                            shouldShowUpdateDialog = false
                                            viewModel.showedUpdateDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(0.5f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cancel),
                                            style = typo.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_update_24),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.update_available),
                                            style = typo.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                text = {
                                    val inputFormat =
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                    val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    val formatted =
                                        response.releaseTime?.let { input ->
                                            inputFormat
                                                .parse(input)
                                                ?.let { outputFormat.format(it) }
                                        } ?: stringResource(R.string.unknown)
                                    
                                    Column(
                                        modifier = Modifier
                                            .heightIn(max = 450.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        // Version info card
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "New Version Available",
                                                    style = typo.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Version: ",
                                                        style = typo.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = response.tagName ?: "Unknown",
                                                        style = typo.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Released: ",
                                                        style = typo.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = formatted,
                                                        style = typo.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Release notes section
                                        if (response.body?.isNotEmpty() == true) {
                                            Text(
                                                text = "What's New",
                                                style = typo.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Markdown(
                                                response.body,
                                                typography = markdownTypography(
                                                    h1 = typo.titleSmall,
                                                    h2 = typo.titleSmall,
                                                    h3 = typo.titleSmall,
                                                    text = typo.bodyMedium,
                                                    bullet = typo.bodyMedium,
                                                    paragraph = typo.bodyMedium,
                                                    link = typo.bodyMedium.copy(
                                                        textDecoration = TextDecoration.Underline,
                                                        color = MaterialTheme.colorScheme.primary
                                                    ),
                                                ),
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        
                                        // Update reminder
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.baseline_info_24),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Keep your app updated for the best experience and latest features!",
                                                    style = typo.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        try {
            val shouldStopMusicService = viewModel.shouldStopMusicService()
            Log.w("MainActivity", "onDestroy: Should stop service $shouldStopMusicService")

            // Always unbind service if it was bound to prevent MusicBinder leak
            if (shouldStopMusicService && shouldUnbind && isFinishing) {
                viewModel.isServiceRunning = false
            }
            unloadKoinModules(viewModelModule)
            super.onDestroy()
            Log.d("MainActivity", "onDestroy: ")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onDestroy: ${e.message}", e)
            super.onDestroy()
        }
    }

    override fun onRestart() {
        try {
            super.onRestart()
            viewModel.activityRecreate()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onRestart: ${e.message}", e)
        }
    }

    private fun startMusicService() {
        try {
            val intent = Intent(this, SimpleMediaService::class.java)
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            viewModel.isServiceRunning = true
            shouldUnbind = true
            Log.d("Service", "Service started")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting music service: ${e.message}", e)
        }
    }

    private fun checkForUpdate() {
        try {
            if (viewModel.shouldCheckForUpdate()) {
                viewModel.checkForUpdate()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking for update: ${e.message}", e)
        }
    }

    private fun putString(
        key: String,
        value: String,
    ) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? = viewModel.getString(key)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.activityRecreate()
    }
}