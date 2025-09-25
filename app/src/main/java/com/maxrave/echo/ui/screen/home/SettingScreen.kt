package iad1tya.echo.music.ui.screen.home

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import iad1tya.echo.kotlinytmusicscraper.extension.isTwoLetterCode
import iad1tya.echo.kotlinytmusicscraper.extension.isValidProxyHost
import iad1tya.echo.music.R
import iad1tya.echo.music.common.CHART_SUPPORTED_COUNTRY
import iad1tya.echo.music.common.LIMIT_CACHE_SIZE
import iad1tya.echo.music.common.QUALITY
import iad1tya.echo.music.common.SPONSOR_BLOCK
import iad1tya.echo.music.common.SUPPORTED_LANGUAGE
import iad1tya.echo.music.common.SUPPORTED_LOCATION
import iad1tya.echo.music.common.VIDEO_QUALITY
import iad1tya.echo.music.data.dataStore.DataStoreManager
import iad1tya.echo.music.data.dataStore.DataStoreManager.Settings.TRUE
import iad1tya.echo.music.extension.bytesToMB
import iad1tya.echo.music.ui.component.ActionButton
import iad1tya.echo.music.ui.component.CenterLoadingBox
import iad1tya.echo.music.ui.component.EndOfPageWithSettingsSpacing
import iad1tya.echo.music.ui.component.PrivacyPolicyDialog
import iad1tya.echo.music.ui.component.RippleIconButton
import iad1tya.echo.music.ui.component.SettingItem
import iad1tya.echo.music.ui.navigation.destination.home.CreditDestination
import iad1tya.echo.music.ui.navigation.destination.login.LoginDestination
import iad1tya.echo.music.ui.navigation.destination.login.SpotifyLoginDestination
import iad1tya.echo.music.ui.theme.DarkColors
import iad1tya.echo.music.ui.theme.md_theme_dark_outline
import iad1tya.echo.music.ui.theme.md_theme_dark_primary
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.utils.LocalResource
import iad1tya.echo.music.utils.VersionManager
import iad1tya.echo.music.viewModel.SettingAlertState
import iad1tya.echo.music.viewModel.SettingBasicAlertState
import iad1tya.echo.music.viewModel.SettingsViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import iad1tya.echo.music.viewModel.WelcomeViewModel
import com.mikepenz.aboutlibraries.ui.compose.ChipColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAlertDialog(
    title: String,
    message: String? = null,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDismiss: Boolean = true,
    confirmButtonColor: Color = Color(0xFF4CAF50) // Light green
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.wrapContentSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF242424),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header with title
                Text(
                    text = title,
                    style = typo.titleMedium,
                    color = Color.White
                )
                
                if (message != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = typo.bodyMedium,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (showDismiss) {
                        TextButton(
                            onClick = onDismiss,
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = Color.Gray
                            )
                        ) {
                            Text(
                                text = dismissText,
                                style = typo.labelMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = confirmButtonColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = confirmText,
                            style = typo.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class, ExperimentalHazeMaterialsApi::class)
@UnstableApi
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    // Get mini-player state to calculate proper bottom padding
    val nowPlayingData by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val isMiniPlayerActive = nowPlayingData?.mediaItem != null && nowPlayingData?.mediaItem != MediaItem.EMPTY
    
    
    // Calculate dynamic bottom padding: minimal spacing to bring content closer to mini player
    val bottomPadding = if (isMiniPlayerActive) 20.dp else 16.dp
    val context = LocalContext.current
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    var width by rememberSaveable { mutableIntStateOf(0) }
    

    // Backup and restore
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(uri)
            }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(uri)
            }
        }

    // Open equalizer
    val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    // Safe collection of StateFlow values with proper default values
    val enableTranslucentNavBar by viewModel.translucentBottomBar.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val language by viewModel.language.collectAsStateWithLifecycle(initialValue = "en")
    val location by viewModel.location.collectAsStateWithLifecycle(initialValue = "US")
    val quality by viewModel.quality.collectAsStateWithLifecycle(initialValue = "AUDIO_QUALITY_MEDIUM")
    val homeLimit by viewModel.homeLimit.collectAsStateWithLifecycle(initialValue = 10)
    val playVideo by viewModel.playVideoInsteadOfAudio.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val videoQuality by viewModel.videoQuality.collectAsStateWithLifecycle(initialValue = "VIDEO_QUALITY_MEDIUM")
    val sendData by viewModel.sendBackToGoogle.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val normalizeVolume by viewModel.normalizeVolume.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSilent by viewModel.skipSilent.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val savePlaybackState by viewModel.savedPlaybackState.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val saveLastPlayed by viewModel.saveRecentSongAndQueue.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val killServiceOnExit by viewModel.killServiceOnExit.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = true)
    val mainLyricsProvider by viewModel.mainLyricsProvider.collectAsStateWithLifecycle(initialValue = "YOUTUBE")
    val youtubeSubtitleLanguage by viewModel.youtubeSubtitleLanguage.collectAsStateWithLifecycle(initialValue = "en")
    val enableSponsorBlock by viewModel.sponsorBlockEnabled.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle(initialValue = emptyList<String>())
    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle(initialValue = 0L)
    val downloadedCache by viewModel.downloadedCacheSize.collectAsStateWithLifecycle(initialValue = 0L)
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle(initialValue = 0L)
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle(initialValue = 0L)
    val fraction by viewModel.fraction.collectAsStateWithLifecycle(initialValue = null)
    val lastCheckUpdate by viewModel.lastCheckForUpdate.collectAsStateWithLifecycle(initialValue = null)
    val usingProxy by viewModel.usingProxy.collectAsStateWithLifecycle(initialValue = false)
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle(initialValue = null)
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle(initialValue = "")
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle(initialValue = 8080)
    val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsStateWithLifecycle(initialValue = true)
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val crashReportEnabled by viewModel.crashReportEnabled.collectAsStateWithLifecycle(initialValue = true)
    val blurFullscreenLyrics by viewModel.blurFullscreenLyrics.collectAsStateWithLifecycle(initialValue = false)
    val blurPlayerBackground by viewModel.blurPlayerBackground.collectAsStateWithLifecycle(initialValue = false)
    val helpBuildLyricsDatabase by viewModel.helpBuildLyricsDatabase.collectAsStateWithLifecycle(initialValue = false)
    val contributor by viewModel.contributor.collectAsStateWithLifecycle(initialValue = null)
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle(initialValue = false)
    val chartKey by viewModel.chartKey.collectAsStateWithLifecycle(initialValue = "")
    val spotifyLogIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle(initialValue = false)
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle(initialValue = false)
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle(initialValue = false)
    val smartLyricsDefaults by viewModel.smartLyricsDefaults.collectAsStateWithLifecycle(initialValue = false)
    val showRecentlyPlayed by viewModel.showRecentlyPlayed.collectAsStateWithLifecycle(initialValue = true)
    val showPreviousTrackButton by viewModel.showPreviousTrackButton.collectAsStateWithLifecycle(initialValue = true)
    val materialYouTheme by viewModel.materialYouTheme.collectAsStateWithLifecycle(initialValue = false)
    // Removed updateChannel variable
    
    // Get user name from WelcomeViewModel
    val welcomeViewModel: WelcomeViewModel = koinViewModel()
    val userName by welcomeViewModel.userName.collectAsStateWithLifecycle()

    val isCheckingUpdate by sharedViewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val isUpToDate by sharedViewModel.isUpToDate.collectAsStateWithLifecycle()

    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    val checkForUpdateSubtitle by remember {
        derivedStateOf {
            when {
                isCheckingUpdate -> context.getString(R.string.checking)
                isUpToDate -> context.getString(R.string.app_up_to_date)
                else -> {
                    val lastCheckString = lastCheckUpdate ?: "0"
                    val lastCheckLong = lastCheckString.toLongOrNull() ?: 0L
                    
                    // Only show date if it's a valid timestamp (not 0 or epoch)
                    if (lastCheckLong > 0L) {
                        context.getString(
                            R.string.last_checked_at,
                            DateTimeFormatter
                                .ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.ofEpochMilli(lastCheckLong)),
                        )
                    } else {
                        "Never checked"
                    }
                }
            }
        }
    }
    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showSpotifyLogoutDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showMusicTransferDialog by rememberSaveable {
        mutableStateOf(false)
    }
    // Removed showThirdPartyLibraries variable

    LaunchedEffect(true) {
        viewModel.getAllGoogleAccount()
    }

    LaunchedEffect(true) {
        viewModel.getSpotifyLogIn()
    }

    // Refresh analytics and crash report settings when settings screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshAnalyticsSettings()
        viewModel.refreshCrashReportSettings()
    }

    LaunchedEffect(true) {
        try {
            // Add delay to prevent race conditions
            delay(100)
            viewModel.getData()
            viewModel.getSmartLyricsDefaults()
        } catch (e: Exception) {
            Log.e("SettingScreen", "Error initializing settings data: ${e.message}", e)
            // Show error toast to user
            try {
                Toast.makeText(context, "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (toastError: Exception) {
                Log.e("SettingScreen", "Error showing toast: ${toastError.message}", toastError)
            }
        }
    }

    LazyColumn(
        contentPadding = innerPadding,
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .hazeSource(hazeState),
    ) {
        item {
            Spacer(Modifier.height(64.dp))
        }
        item(key = "user_name") {
            Column {
                Text(text = "Profile", style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = "Name",
                    subtitle = if (userName?.isNotBlank() == true) userName!! else "Tap to set your name",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = "Edit Name",
                                message = "Enter your name below:",
                                textField = SettingAlertState.TextFieldData(
                                    label = "Name",
                                    value = userName ?: ""
                                ),
                                confirm = "Save" to { state ->
                                    val name = state.textField?.value ?: ""
                                    if (name.isNotBlank()) {
                                        welcomeViewModel.setUserName(name)
                                        Toast.makeText(context, "Name updated successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                dismiss = "Cancel"
                            )
                        )
                    },
                )
            }
        }
        item(key = "accounts") {
            Column {
                Text(text = stringResource(R.string.accounts), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.youtube_account),
                    subtitle = stringResource(R.string.manage_your_youtube_accounts),
                    onClick = {
                        viewModel.getAllGoogleAccount()
                        showYouTubeAccountDialog = true
                    },
                )
                SettingItem(
                    title = "Spotify Account",
                    subtitle = if (spotifyLogIn) "Connected" else "Connect to Spotify for enhanced features",
                    onClick = {
                        if (spotifyLogIn) {
                            // Show modern logout dialog
                            showSpotifyLogoutDialog = true
                        } else {
                            // Navigate to Spotify login
                            navController.navigate(SpotifyLoginDestination)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(R.string.music_transfer),
                    subtitle = stringResource(R.string.transfer_music_description),
                    onClick = {
                        showMusicTransferDialog = true
                    },
                )
            }
        }
            item(key = "visuals") {
                Column {
                    Text(text = "Visuals", style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                    SettingItem(
                        title = "Material You Theme",
                        subtitle = "Use dynamic colors based on your wallpaper (Android 12+)",
                        switch = (materialYouTheme to { viewModel.setMaterialYouTheme(it) }),
                    )
                    SettingItem(
                        title = "Recently Played",
                        subtitle = "Show recently played songs and playlists on home screen",
                        switch = (showRecentlyPlayed to { viewModel.setShowRecentlyPlayed(it) }),
                    )
                    SettingItem(
                        title = "Previous Track Button",
                        subtitle = "Show previous track button in mini player",
                        switch = (showPreviousTrackButton to { viewModel.setShowPreviousTrackButton(it) }),
                    )
                    // Show Spotify Canvas only when logged in
                    if (spotifyLogIn) {
                        SettingItem(
                            title = "Spotify Canvas",
                            subtitle = "Show visual content (short videos) for tracks",
                            switch = (spotifyCanvas to { viewModel.setSpotifyCanvas(it) }),
                        )
                    }
                }
            }
        item(key = "content") {
            Column {
                Text(text = stringResource(R.string.content), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.language),
                    subtitle = SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US"),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.language),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            SUPPORTED_LANGUAGE.items.map {
                                                (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                        viewModel.setBasicAlertData(
                                            SettingBasicAlertState(
                                                title = context.getString(R.string.warning),
                                                message = context.getString(R.string.change_language_warning),
                                                confirm =
                                                    context.getString(R.string.change) to {
                                                        sharedViewModel.activityRecreate()
                                                        viewModel.setBasicAlertData(null)
                                                        viewModel.changeLanguage(code)
                                                    },
                                                dismiss = context.getString(R.string.cancel),
                                            ),
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.content_country),
                    subtitle = location ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.content_country),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            SUPPORTED_LOCATION.items.map { item ->
                                                (item.toString() == location) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.changeLocation(
                                            state.selectOne?.getSelected() ?: "US",
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.chart_country),
                    subtitle = CHART_SUPPORTED_COUNTRY.itemsData.find { 
                        CHART_SUPPORTED_COUNTRY.items[CHART_SUPPORTED_COUNTRY.itemsData.indexOf(it)] == chartKey 
                    }?.toString() ?: CHART_SUPPORTED_COUNTRY.itemsData[1].toString(),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.chart_country),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            CHART_SUPPORTED_COUNTRY.itemsData.map { item ->
                                                (CHART_SUPPORTED_COUNTRY.items[CHART_SUPPORTED_COUNTRY.itemsData.indexOf(item)] == chartKey) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        val selectedIndex = CHART_SUPPORTED_COUNTRY.itemsData.indexOfFirst { it.toString() == state.selectOne?.getSelected() }
                                        if (selectedIndex >= 0) {
                                            viewModel.setChartKey(CHART_SUPPORTED_COUNTRY.items[selectedIndex])
                                        }
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.home_limit),
                    subtitle = homeLimit?.toString() ?: stringResource(R.string.unknown),
                ) {
                    Slider(
                        value = homeLimit?.toFloat() ?: 3f,
                        onValueChange = {
                            viewModel.setHomeLimit(it.toInt())
                        },
                        modifier = Modifier,
                        enabled = true,
                        valueRange = 3f..8f,
                        steps = 4,
                        onValueChangeFinished = {},
                    )
                }
                SettingItem(
                    title = stringResource(R.string.play_video_for_video_track_instead_of_audio_only),
                    subtitle = stringResource(R.string.such_as_music_video_lyrics_video_podcasts_and_more),
                    smallSubtitle = true,
                    switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.video_quality),
                    subtitle = videoQuality ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.video_quality),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            VIDEO_QUALITY.items.map { item ->
                                                (item.toString() == videoQuality) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.changeVideoQuality(state.selectOne?.getSelected() ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                // Removed send back listening data to Google setting
                SettingItem(
                    title = stringResource(R.string.proxy),
                    subtitle = stringResource(R.string.proxy_description),
                    switch = (usingProxy to { viewModel.setUsingProxy(it) }),
                )
            }
        }
        item(key = "proxy") {
            Crossfade(usingProxy) { it ->
                if (it) {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.proxy_type),
                            subtitle =
                                when (proxyType) {
                                    DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP -> stringResource(R.string.http)
                                    DataStoreManager.Settings.ProxyType.PROXY_TYPE_SOCKS -> stringResource(R.string.socks)
                                    null -> stringResource(R.string.http)
                                },
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = context.getString(R.string.proxy_type),
                                        selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                    listOf(
                                                        (proxyType == DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP) to
                                                            context.getString(
                                                                R.string.http,
                                                            ),
                                                        (proxyType == DataStoreManager.Settings.ProxyType.PROXY_TYPE_SOCKS) to
                                                            context.getString(R.string.socks),
                                                    ),
                                            ),
                                        confirm =
                                            context.getString(R.string.change) to { state ->
                                                viewModel.setProxy(
                                                    if (state.selectOne?.getSelected() == context.getString(R.string.socks)) {
                                                        DataStoreManager.Settings.ProxyType.PROXY_TYPE_SOCKS
                                                    } else {
                                                        DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP
                                                    },
                                                    proxyHost,
                                                    proxyPort,
                                                )
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            },
                        )
                        SettingItem(
                            title = stringResource(R.string.proxy_host),
                            subtitle = proxyHost,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = context.getString(R.string.proxy_host),
                                        message = context.getString(R.string.proxy_host_message),
                                        textField =
                                            SettingAlertState.TextFieldData(
                                                label = context.getString(R.string.proxy_host),
                                                value = proxyHost,
                                                verifyCodeBlock = {
                                                    isValidProxyHost(it) to context.getString(R.string.invalid_host)
                                                },
                                            ),
                                        confirm =
                                            context.getString(R.string.change) to { state ->
                                                viewModel.setProxy(
                                                    proxyType ?: DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP,
                                                    state.textField?.value ?: "",
                                                    proxyPort,
                                                )
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            },
                        )
                        SettingItem(
                            title = stringResource(R.string.proxy_port),
                            subtitle = proxyPort.toString(),
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = context.getString(R.string.proxy_port),
                                        message = context.getString(R.string.proxy_port_message),
                                        textField =
                                            SettingAlertState.TextFieldData(
                                                label = context.getString(R.string.proxy_port),
                                                value = proxyPort.toString(),
                                                verifyCodeBlock = {
                                                    (it.toIntOrNull() != null) to context.getString(R.string.invalid_port)
                                                },
                                            ),
                                        confirm =
                                            context.getString(R.string.change) to { state ->
                                                viewModel.setProxy(
                                                    proxyType ?: DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP,
                                                    proxyHost,
                                                    state.textField?.value?.toIntOrNull() ?: 0,
                                                )
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        item(key = "audio") {
            Column {
                Text(text = stringResource(R.string.audio), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.quality),
                    subtitle = quality ?: "",
                    smallSubtitle = true,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.quality),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            QUALITY.items.map { item ->
                                                (item.toString() == quality) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.changeQuality(state.selectOne?.getSelected())
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.normalize_volume),
                    subtitle = stringResource(R.string.balance_media_loudness),
                    switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.skip_silent),
                    subtitle = stringResource(R.string.skip_no_music_part),
                    switch = (skipSilent to { viewModel.setSkipSilent(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.open_system_equalizer),
                    subtitle = stringResource(R.string.use_your_system_equalizer),
                    onClick = {
                        val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, viewModel.getAudioSessionId() ?: 0)
                        eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        val packageManager = context.packageManager
                        val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
                        Log.d("EQ", resolveInfo.toString())
                        if (resolveInfo.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show()
                        } else {
                            resultLauncher.launch(eqIntent)
                        }
                    },
                )
            }
        }
        item(key = "playback") {
            Column {
                Text(text = stringResource(R.string.playback), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.save_playback_state),
                    subtitle = stringResource(R.string.save_shuffle_and_repeat_mode),
                    switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.save_last_played),
                    subtitle = stringResource(R.string.save_last_played_track_and_queue),
                    switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.kill_service_on_exit),
                    subtitle = stringResource(R.string.kill_service_on_exit_description),
                    switch = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                )
            }
        }
        item(key = "lyrics") {
            Column {
                Text(text = stringResource(R.string.lyrics), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                // Show Spotify Lyrics only when logged in
                if (spotifyLogIn) {
                    SettingItem(
                        title = "Spotify Lyrics",
                        subtitle = "Use Spotify's official lyrics with timing",
                        switch = (spotifyLyrics to { viewModel.setSpotifyLyrics(it) }),
                    )
                }
                SettingItem(
                    title = stringResource(R.string.main_lyrics_provider),
                    subtitle =
                        when (mainLyricsProvider) {
                            DataStoreManager.YOUTUBE -> stringResource(R.string.youtube_transcript)
                            DataStoreManager.LRCLIB -> stringResource(R.string.lrclib)
                            DataStoreManager.SPOTIFY -> stringResource(R.string.spotify_lyrics_provider)
                            else -> stringResource(R.string.youtube_transcript)
                        },
                    onClick = {
                        val lyricsOptions = mutableListOf<Pair<Boolean, String>>()
                        
                        // Always include YouTube and LRCLIB
                        lyricsOptions.add((mainLyricsProvider == DataStoreManager.YOUTUBE) to context.getString(R.string.youtube_transcript))
                        lyricsOptions.add((mainLyricsProvider == DataStoreManager.LRCLIB) to context.getString(R.string.lrclib))
                        
                        // Only include Spotify if logged in
                        if (spotifyLogIn) {
                            lyricsOptions.add((mainLyricsProvider == DataStoreManager.SPOTIFY) to context.getString(R.string.spotify_lyrics_provider))
                        }
                        
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.main_lyrics_provider),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect = lyricsOptions,
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setLyricsProvider(
                                            when (state.selectOne?.getSelected()) {
                                                context.getString(R.string.youtube_transcript) -> DataStoreManager.YOUTUBE
                                                context.getString(R.string.lrclib) -> DataStoreManager.LRCLIB
                                                context.getString(R.string.spotify_lyrics_provider) -> DataStoreManager.SPOTIFY
                                                else -> DataStoreManager.YOUTUBE
                                            },
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                    SettingItem(
                        title = "Smart Lyrics",
                        subtitle = "Automatically choose the best lyrics provider",
                        switch = (smartLyricsDefaults to { viewModel.setSmartLyricsDefaults(it) }),
                    )

                SettingItem(
                    title = stringResource(R.string.youtube_subtitle_language),
                    subtitle = youtubeSubtitleLanguage,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.youtube_subtitle_language),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.youtube_subtitle_language),
                                        value = youtubeSubtitleLanguage,
                                        verifyCodeBlock = {
                                            (it.length == 2 && it.isTwoLetterCode()) to context.getString(R.string.invalid_language_code)
                                        },
                                    ),
                                message = context.getString(R.string.youtube_subtitle_language_message),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setYoutubeSubtitleLanguage(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                // Removed lyrics database description text as requested
            }
        }
        item(key = "sponsor_block") {
            Column {
                Text(text = stringResource(R.string.sponsorBlock), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.enable_sponsor_block),
                    subtitle = stringResource(R.string.skip_sponsor_part_of_video),
                    switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.categories_sponsor_block),
                    subtitle = stringResource(R.string.what_segments_will_be_skipped),
                    onClick = {
                        val listName =
                            SPONSOR_BLOCK.listName.map {
                                context.getString(it)
                            }
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.categories_sponsor_block),
                                multipleSelect =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listName
                                                .mapIndexed { index, item ->
                                                    (
                                                        skipSegments?.contains(
                                                            SPONSOR_BLOCK.list.getOrNull(index),
                                                        ) == true
                                                    ) to item
                                                }.also {
                                                    Log.w("SettingScreen", "SettingAlertState: $skipSegments")
                                                    Log.w("SettingScreen", "SettingAlertState: $it")
                                                },
                                    ),
                                confirm =
                                    context.getString(R.string.save) to { state ->
                                        viewModel.setSponsorBlockCategories(
                                            state.multipleSelect
                                                ?.getListSelected()
                                                ?.map { selected ->
                                                    listName.indexOf(selected)
                                                }?.mapNotNull { s ->
                                                    SPONSOR_BLOCK.list.getOrNull(s).let {
                                                        it?.toString()
                                                    }
                                                }?.toCollection(ArrayList()) ?: arrayListOf(),
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                    isEnable = enableSponsorBlock,
                )
                val beforeUrl = stringResource(R.string.sponsor_block_intro).substringBefore("https://sponsor.ajay.app/")
                val afterUrl = stringResource(R.string.sponsor_block_intro).substringAfter("https://sponsor.ajay.app/")
                Text(
                    buildAnnotatedString {
                        append(beforeUrl)
                        withLink(
                            LinkAnnotation.Url(
                                "https://sponsor.ajay.app/",
                                TextLinkStyles(style = SpanStyle(color = md_theme_dark_primary)),
                            ),
                        ) {
                            append("https://sponsor.ajay.app/")
                        }
                        append(afterUrl)
                    },
                    style = typo.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
        item(key = "storage") {
            Column {
                Text(text = stringResource(R.string.storage), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.player_cache),
                    subtitle = "${playerCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_player_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearPlayerCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.downloaded_cache),
                    subtitle = "${downloadedCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_downloaded_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearDownloadedCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.thumbnail_cache),
                    subtitle = "${thumbnailCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_thumbnail_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearThumbnailCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.limit_player_cache),
                    subtitle = LIMIT_CACHE_SIZE.getItemFromData((limitPlayerCache ?: 0L).toInt()).toString(),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.limit_player_cache),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            LIMIT_CACHE_SIZE.items.map { item ->
                                                (item == LIMIT_CACHE_SIZE.getItemFromData((limitPlayerCache ?: 0L).toInt())) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setPlayerCacheLimit(
                                            LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected()),
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                Box(
                    Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    ),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .onGloballyPositioned { layoutCoordinates ->
                                    with(localDensity) {
                                        width =
                                            layoutCoordinates.size.width
                                                .toDp()
                                                .value
                                                .toInt()
                                    }
                                },
                    ) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.otherApp ?: 0f) * width).dp,
                                        ).background(
                                            md_theme_dark_primary,
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.downloadCache ?: 0f) * width).dp,
                                        ).background(
                                            Color(0xD540FF17),
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.playerCache ?: 0f) * width).dp,
                                        ).background(
                                            Color(0xD5FFFF00),
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.canvasCache ?: 0f) * width).dp,
                                        ).background(
                                            Color.Cyan,
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.thumbCache ?: 0f) * width).dp,
                                        ).background(
                                            Color.Magenta,
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.appDatabase ?: 0f) * width).dp,
                                        ).background(
                                            Color.White,
                                        ),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            ((fraction?.freeSpace ?: 0f) * width).dp,
                                        ).background(
                                            Color.DarkGray,
                                        ).fillMaxHeight(),
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                md_theme_dark_primary,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.other_app), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Green,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.downloaded_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Yellow,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.player_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Cyan,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Magenta,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.thumbnail_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.database), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.LightGray,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.free_space), style = typo.bodySmall)
                }
            }
        }
        item(key = "backup") {
            Column {
                Text(text = stringResource(R.string.backup), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.backup_downloaded),
                    subtitle = stringResource(R.string.backup_downloaded_description),
                    switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.backup),
                    subtitle = stringResource(R.string.save_all_your_playlist_data),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupLauncher.launch("${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.restore_your_data),
                    subtitle = stringResource(R.string.restore_your_saved_data),
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                    },
                )
            }
        }
        item(key = "privacy") {
            Column {
                Text(text = stringResource(R.string.privacy), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.analytics),
                    subtitle = stringResource(R.string.analytics_description),
                    switch = (analyticsEnabled to { viewModel.setAnalyticsEnabled(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.crash_report),
                    subtitle = stringResource(R.string.crash_report_description),
                    switch = (crashReportEnabled to { viewModel.setCrashReportEnabled(it) }),
                )
            }
        }
        item(key = "about_us") {
            Column {
                Text(text = stringResource(R.string.about_us), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.version),
                    subtitle = stringResource(R.string.version_format, VersionManager.getVersionName()),
                    onClick = {
                        navController.navigate(CreditDestination)
                    },
                )
                SettingItem(
                    title = stringResource(R.string.auto_check_for_update),
                    subtitle = stringResource(R.string.auto_check_for_update_description),
                    switch = (autoCheckUpdate to { viewModel.setAutoCheckUpdate(it) }),
                )
                // Removed update channel setting
                SettingItem(
                    title = stringResource(R.string.check_for_update),
                    subtitle = checkForUpdateSubtitle,
                    onClick = {
                        sharedViewModel.resetUpToDateState()
                        sharedViewModel.checkForUpdate()
                    },
                )
                SettingItem(
                    title = stringResource(R.string.developed_by),
                    subtitle = "iad1tya",
                    onClick = {
                        uriHandler.openUri("https://github.com/iad1tya")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.buy_me_a_coffee),
                    subtitle = stringResource(R.string.donation),
                    onClick = {
                        uriHandler.openUri("https://buymeacoffee.com/iad1tya")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.contact_me),
                    subtitle = "hello@echomusic.fun",
                    onClick = {
                        uriHandler.openUri("mailto:hello@echomusic.fun")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.privacy_policy_description),
                    onClick = {
                        uriHandler.openUri("https://echomusic.fun/p/privacy-policy.html")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.terms_and_conditions),
                    subtitle = stringResource(R.string.terms_and_conditions_description),
                    onClick = {
                        uriHandler.openUri("https://echomusic.fun/p/toc.html")
                    },
                )
                // Removed third party libraries setting
            }
        }
        item(key = "end") {
            EndOfPageWithSettingsSpacing()
        }
        // Add dynamic bottom padding to prevent content from scrolling behind bottom navigation/mini player
        item {
            Spacer(Modifier.height(bottomPadding))
        }
    }
    val basisAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()
    if (basisAlertData != null) {
        val alertBasicState = basisAlertData ?: return
        ModernAlertDialog(
            title = alertBasicState.title,
            message = alertBasicState.message,
            confirmText = alertBasicState.confirm.first,
            dismissText = alertBasicState.dismiss,
            onConfirm = {
                alertBasicState.confirm.second.invoke()
                viewModel.setBasicAlertData(null)
            },
            onDismiss = {
                viewModel.setBasicAlertData(null)
            }
        )
    }
    if (showYouTubeAccountDialog) {
        BasicAlertDialog(
            onDismissRequest = { },
            modifier = Modifier.wrapContentSize(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color(0xFF242424),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                shadowElevation = 1.dp,
            ) {
                val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                    minActiveState = Lifecycle.State.RESUMED,
                )
                LaunchedEffect(googleAccounts) {
                    Log.w(
                        "SettingScreen",
                        "LaunchedEffect: ${
                            googleAccounts.data?.map {
                                it.name to it.isUsed
                            }
                        }",
                    )
                }
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                        ) {
                            IconButton(
                                onClick = { showYouTubeAccountDialog = false },
                                colors =
                                    IconButtonDefaults.iconButtonColors().copy(
                                        contentColor = Color.White,
                                    ),
                                modifier =
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight(),
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color.White)
                            }
                            Text(
                                stringResource(R.string.youtube_account),
                                style = typo.titleMedium,
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .wrapContentWidth(),
                            )
                        }
                    }
                    if (googleAccounts is LocalResource.Success) {
                        val data = googleAccounts.data
                        if (data.isNullOrEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.no_account),
                                    style = typo.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                )
                            }
                        } else {
                            items(data) {
                                Row(
                                    modifier =
                                        Modifier
                                            .padding(vertical = 8.dp)
                                            .clickable {
                                                viewModel.setUsedAccount(it)
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(24.dp))
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalContext.current)
                                                .data(it.thumbnailUrl)
                                                .crossfade(550)
                                                .build(),
                                        placeholder = painterResource(R.drawable.baseline_people_alt_24),
                                        error = painterResource(R.drawable.baseline_people_alt_24),
                                        contentDescription = it.name,
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = typo.labelMedium)
                                        Text(it.email, style = typo.bodySmall)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    AnimatedVisibility(it.isUsed) {
                                        Text(
                                            stringResource(R.string.signed_in),
                                            style = typo.bodySmall,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.widthIn(0.dp, 64.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(24.dp))
                                }
                            }
                        }
                    } else {
                        item {
                            CenterLoadingBox(
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                            )
                        }
                    }
                    item {
                        Column {
                            // Check if user is logged in
                            val isLoggedIn = when (googleAccounts) {
                                is LocalResource.Success -> {
                                    val data = googleAccounts.data
                                    !data.isNullOrEmpty() && data.any { it.isUsed }
                                }
                                else -> false
                            }
                            
                            if (isLoggedIn) {
                                // Show only logout option when user is logged in
                                // Custom left-aligned logout button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .clickable { 
                                            viewModel.logOutAllYouTube()
                                            showYouTubeAccountDialog = false
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    ) {
                                        // Transparent icon for spacing
                                        Image(
                                            painter = painterResource(R.drawable.baseline_circle_24),
                                            contentDescription = stringResource(R.string.log_out),
                                            modifier = Modifier
                                                .wrapContentSize(Alignment.Center)
                                                .padding(12.dp),
                                            colorFilter = ColorFilter.tint(Color.Transparent),
                                        )
                                        Text(
                                            text = stringResource(R.string.log_out),
                                            style = typo.labelSmall,
                                            color = Color.Unspecified,
                                            modifier = Modifier
                                                .padding(start = 10.dp)
                                                .wrapContentHeight(Alignment.CenterVertically),
                                        )
                                    }
                                }
                            } else {
                                // Show only add account option when user is not logged in
                                ActionButton(
                                    icon = painterResource(R.drawable.baseline_playlist_add_24),
                                    text = R.string.add_an_account,
                                ) {
                                    showYouTubeAccountDialog = false
                                    navController.navigate(LoginDestination)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Spotify Logout Dialog
    if (showSpotifyLogoutDialog) {
        ModernAlertDialog(
            title = "Spotify Logout",
            message = "Are you sure you want to logout from Spotify? You'll lose access to Spotify lyrics and enhanced features.",
            confirmText = "Logout",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.setSpotifyLogIn(false)
                showSpotifyLogoutDialog = false
            },
            onDismiss = {
                showSpotifyLogoutDialog = false
            }
        )
    }

    // Music Transfer Dialog
    if (showMusicTransferDialog) {
        MusicTransferDialog(
            onDismiss = {
                showMusicTransferDialog = false
            },
            onContinue = {
                showMusicTransferDialog = false
                uriHandler.openUri("https://www.tunemymusic.com/")
            }
        )
    }


    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    if (alertData != null) {
        val alertState = alertData ?: return
        // AlertDialog
        AlertDialog(
            onDismissRequest = { viewModel.setAlertData(null) },
            title = {
                Text(
                    text = alertState.title,
                    style = typo.titleSmall,
                )
            },
            text = {
                if (alertState.message != null) {
                    Column {
                        Text(text = alertState.message)
                        if (alertState.textField != null) {
                            val verify =
                                alertState.textField.verifyCodeBlock?.invoke(
                                    alertState.textField.value,
                                ) ?: (true to null)
                            TextField(
                                value = alertState.textField.value,
                                onValueChange = {
                                    viewModel.setAlertData(
                                        alertState.copy(
                                            textField =
                                                alertState.textField.copy(
                                                    value = it,
                                                ),
                                        ),
                                    )
                                },
                                isError = !verify.first,
                                label = { Text(text = alertState.textField.label) },
                                supportingText = {
                                    if (!verify.first) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = verify.second ?: "",
                                            color = DarkColors.error,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (!verify.first) {
                                        Icons.Outlined.Error
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp,
                                        ),
                            )
                        }
                    }
                } else if (alertState.selectOne != null) {
                    LazyColumn(
                        Modifier
                            .padding(vertical = 6.dp)
                            .heightIn(0.dp, 500.dp),
                    ) {
                        items(alertState.selectOne.listSelect) { item ->
                            val onSelect = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        selectOne =
                                            alertState.selectOne.copy(
                                                listSelect =
                                                    alertState.selectOne.listSelect.toMutableList().map {
                                                        if (it == item) {
                                                            true to it.second
                                                        } else {
                                                            false to it.second
                                                        }
                                                    },
                                            ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onSelect.invoke()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = item.first,
                                    onClick = {
                                        onSelect.invoke()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo.bodyMedium, maxLines = 1)
                            }
                        }
                    }
                } else if (alertState.multipleSelect != null) {
                    LazyColumn(
                        Modifier.padding(vertical = 6.dp),
                    ) {
                        items(alertState.multipleSelect.listSelect) { item ->
                            val onCheck = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        multipleSelect =
                                            alertState.multipleSelect.copy(
                                                listSelect =
                                                    alertState.multipleSelect.listSelect.toMutableList().map {
                                                        if (it == item) {
                                                            !it.first to it.second
                                                        } else {
                                                            it
                                                        }
                                                    },
                                            ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onCheck.invoke()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.first,
                                    onCheckedChange = {
                                        onCheck.invoke()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo.bodyMedium, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertState.confirm.second.invoke(alertState)
                        viewModel.setAlertData(null)
                    },
                    enabled =
                        if (alertState.textField?.verifyCodeBlock != null) {
                            alertState.textField.verifyCodeBlock
                                .invoke(
                                    alertState.textField.value,
                                ).first
                        } else {
                            true
                        },
                ) {
                    Text(text = alertState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setAlertData(null)
                    },
                ) {
                    Text(text = alertState.dismiss)
                }
            },
        )
    }

    // Removed third party libraries dialog

    TopAppBar(
        title = {
            Text(
                text =
                    stringResource(
                        R.string.settings,
                    ),
                style = typo.titleMedium,
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    R.drawable.baseline_arrow_back_ios_new_24,
                    Modifier
                        .size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
        modifier =
            Modifier
                .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                    blurEnabled = true
                },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}

@Composable
fun MusicTransferDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.music_transfer_title),
                style = typo.titleMedium,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.music_transfer_description),
                    style = typo.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = stringResource(R.string.music_transfer_steps_title),
                    style = typo.titleSmall,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "1. ${stringResource(R.string.music_transfer_step_1)}",
                    style = typo.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "2. ${stringResource(R.string.music_transfer_step_2)}",
                    style = typo.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "3. ${stringResource(R.string.music_transfer_step_3)}",
                    style = typo.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "4. ${stringResource(R.string.music_transfer_step_4)}",
                    style = typo.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "5. ${stringResource(R.string.music_transfer_step_5)}",
                    style = typo.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "6. ${stringResource(R.string.music_transfer_step_6)}",
                    style = typo.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = stringResource(R.string.music_transfer_completion),
                    style = typo.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_continue),
                    style = typo.labelMedium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = typo.labelMedium
                )
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
