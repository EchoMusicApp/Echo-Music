package iad1tya.echo.music.ui.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import iad1tya.echo.kotlinytmusicscraper.config.Constants
import iad1tya.echo.music.R
import iad1tya.echo.music.common.CHART_SUPPORTED_COUNTRY
import iad1tya.echo.music.common.Config
import iad1tya.echo.music.data.dataStore.DataStoreManager
import iad1tya.echo.music.data.model.browse.album.Track
import iad1tya.echo.music.data.model.explore.mood.Mood
import iad1tya.echo.music.data.model.home.HomeItem
import iad1tya.echo.music.data.model.home.chart.Chart
import iad1tya.echo.music.extension.isScrollingUp
import iad1tya.echo.music.extension.toMediaItem
import iad1tya.echo.music.extension.toTrack
import iad1tya.echo.music.service.PlaylistType
import iad1tya.echo.music.service.QueueData
import iad1tya.echo.music.ui.component.CenterLoadingBox
import iad1tya.echo.music.ui.component.Chip
import iad1tya.echo.music.ui.component.DropdownButton
import iad1tya.echo.music.ui.component.EndOfPage
import iad1tya.echo.music.ui.component.GetDataSyncIdBottomSheet
import iad1tya.echo.music.ui.component.HomeItem
import iad1tya.echo.music.ui.component.HomeItemContentPlaylist
import iad1tya.echo.music.ui.component.HomeShimmer
import iad1tya.echo.music.ui.component.NetworkAwareContent
import iad1tya.echo.music.ui.component.NetworkConnectivityManager
import iad1tya.echo.music.ui.navigation.destination.library.LibraryDestination
import iad1tya.echo.music.ui.component.ItemArtistChart
import iad1tya.echo.music.ui.component.MoodMomentAndGenreHomeItem
import iad1tya.echo.music.ui.component.QuickPicksItem
import iad1tya.echo.music.ui.component.RecentlyPlayedSection
import iad1tya.echo.music.ui.component.RippleIconButton
import iad1tya.echo.music.ui.navigation.destination.home.HomeDestination
import iad1tya.echo.music.ui.navigation.destination.home.MoodDestination
import iad1tya.echo.music.ui.navigation.destination.home.NotificationDestination
import iad1tya.echo.music.ui.navigation.destination.home.RecentlySongsDestination
import iad1tya.echo.music.ui.navigation.destination.home.SettingsDestination
import iad1tya.echo.music.ui.navigation.destination.list.ArtistDestination
import iad1tya.echo.music.ui.navigation.destination.list.PlaylistDestination
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.viewModel.HomeViewModel
import iad1tya.echo.music.viewModel.SettingsViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import iad1tya.echo.music.viewModel.WelcomeViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun HomeScreen(
    viewModel: HomeViewModel =
        koinViewModel(),
    sharedViewModel: SharedViewModel =
        koinInject(),
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = 0
    )
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val recentlyPlayed by sharedViewModel.recentlyPlayed.collectAsStateWithLifecycle()
    
    // Get settings ViewModel for show recently played setting
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val showRecentlyPlayed by settingsViewModel.showRecentlyPlayed.collectAsStateWithLifecycle()
    
    var accountShow by rememberSaveable {
        mutableStateOf(false)
    }
    val chartKey by viewModel.chartKey.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    
    // Get mini-player state to calculate proper bottom padding
    val nowPlayingData by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val isMiniPlayerActive = nowPlayingData?.mediaItem != null && nowPlayingData?.mediaItem != MediaItem.EMPTY
    
    // Calculate dynamic bottom padding: 56dp for bottom nav + extra space for better scrolling
    // Without mini player: 56dp (nav) + 52dp (1-2cm extra) = 108dp
    // With mini player: 56dp (nav) + 80dp (mini player) + 72dp (2-3cm extra) = 208dp
    val bottomPadding = if (isMiniPlayerActive) 208.dp else 108.dp
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()


    val dataSyncId by viewModel.dataSyncId.collectAsStateWithLifecycle()
    val youTubeCookie by viewModel.youTubeCookie.collectAsStateWithLifecycle()
    var shouldShowGetDataSyncIdBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var topAppBarHeightPx by rememberSaveable {
        mutableIntStateOf(0)
    }

    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    LaunchedEffect(dataSyncId, youTubeCookie) {
        try {
            Log.d("HomeScreen", "dataSyncId: $dataSyncId, youTubeCookie: $youTubeCookie")
            if (dataSyncId.isEmpty() && youTubeCookie.isNotEmpty()) {
                shouldShowGetDataSyncIdBottomSheet = true
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error in LaunchedEffect: ${e.message}", e)
        }
    }

    val onRefresh: () -> Unit = {
        try {
            isRefreshing = true
            viewModel.getHomeItemList(params)
            sharedViewModel.getRecentlyPlayed()
            Log.w("HomeScreen", "onRefresh - Full refresh triggered")
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error in onRefresh: ${e.message}", e)
            isRefreshing = false
        }
    }
    LaunchedEffect(key1 = reloadDestination) {
        try {
            if (reloadDestination == HomeDestination::class) {
                if (scrollState.firstVisibleItemIndex > 1) {
                    Log.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                    scrollState.animateScrollToItem(0)
                    sharedViewModel.reloadDestinationDone()
                } else {
                    Log.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                    onRefresh.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error in reloadDestination LaunchedEffect: ${e.message}", e)
        }
    }
    LaunchedEffect(key1 = loading) {
        try {
            if (!loading) {
                isRefreshing = false
                sharedViewModel.reloadDestinationDone()
                coroutineScope.launch {
                    pullToRefreshState.animateToHidden()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error in loading LaunchedEffect: ${e.message}", e)
        }
    }
    // Initial data load when screen is first composed
    LaunchedEffect(Unit) {
        try {
            // Load data with retry mechanism
            viewModel.getHomeItemList()
            sharedViewModel.getRecentlyPlayed()
            
            // Add a small delay to ensure data is processed
            kotlinx.coroutines.delay(100)
            
            // Retry if no data loaded after initial attempt
            if (homeData.isEmpty()) {
                kotlinx.coroutines.delay(500)
                viewModel.getHomeItemList()
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error in initial data load: ${e.message}", e)
            // Retry on error
            kotlinx.coroutines.delay(1000)
            try {
                viewModel.getHomeItemList()
                sharedViewModel.getRecentlyPlayed()
            } catch (retryException: Exception) {
                Log.e("HomeScreen", "Error in retry data load: ${retryException.message}", retryException)
            }
        }
    }
    
    // Force show content after timeout to prevent blank screen
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000) // 3 second timeout
        if (homeData.isEmpty()) {
            Log.w("HomeScreen", "Timeout reached - forcing content display")
            // Force load data one more time
            viewModel.getHomeItemList()
        }
    }
    
    LaunchedEffect(key1 = homeData) {
        try {
            accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error in homeData LaunchedEffect: ${e.message}", e)
        }
    }

    if (shouldShowGetDataSyncIdBottomSheet) {
        GetDataSyncIdBottomSheet(
            cookie = youTubeCookie,
            onDismissRequest = {
                shouldShowGetDataSyncIdBottomSheet = false
            },
        )
    }




    NetworkConnectivityManager(
        onRefresh = onRefresh
    ) { isOnline, onRetry, onSeeDownloads, showNoInternetMessage ->
        Box {
            NetworkAwareContent(
                isOnline = isOnline,
                onRetry = onRetry,
                onSeeDownloads = {
                    navController.navigate(LibraryDestination)
                },
                showNoInternetMessage = showNoInternetMessage
            ) {
                PullToRefreshBox(
            modifier =
                Modifier
                    .hazeSource(hazeState),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top =
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                            ),
                    containerColor = PullToRefreshDefaults.containerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    threshold = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading || homeData.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        state = scrollState,
                        contentPadding = PaddingValues(bottom = bottomPadding),
                        // Performance optimizations
                        userScrollEnabled = true,
                        reverseLayout = false,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        item {
                            Spacer(
                                Modifier.height(
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                                ),
                            )
                        }
                        // AccountLayout removed - no space taken when no account info
                        item {
                            AnimatedVisibility(
                                visible = showRecentlyPlayed && recentlyPlayed.isNotEmpty(),
                            ) {
                                RecentlyPlayedSection(
                                    recentlyPlayed = recentlyPlayed,
                                    navController = navController,
                                    onSongClick = { songEntity ->
                                        sharedViewModel.loadMediaItem(songEntity, Config.SONG_CLICK)
                                    },
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                )
                            }
                        }
                        item {
                            AnimatedVisibility(
                                visible =
                                    homeData.find {
                                        it.title ==
                                            context.getString(
                                                R.string.quick_picks,
                                            )
                                    } != null,
                            ) {
                                QuickPicks(
                                    homeItem =
                                        (
                                            homeData.find {
                                                it.title ==
                                                    context.getString(
                                                        R.string.quick_picks,
                                                    )
                                            } ?: return@AnimatedVisibility
                                        ).let { content ->
                                            content.copy(
                                                contents =
                                                    content.contents.mapNotNull { ct ->
                                                        ct?.copy(
                                                            artists =
                                                                ct.artists?.let { art ->
                                                                    if (art.size > 1) {
                                                                        art.dropLast(1)
                                                                    } else {
                                                                        art
                                                                    }
                                                                },
                                                        )
                                                    },
                                            )
                                        },
                                    viewModel = viewModel,
                                )
                            }
                        }
                        items(homeData, key = { it.hashCode() }) {
                            if (it.title != context.getString(R.string.quick_picks)) {
                                HomeItem(
                                    navController = navController,
                                    data = it,
                                )
                            }
                        }
                        items(newRelease, key = { it.hashCode() }) {
                            AnimatedVisibility(
                                visible = newRelease.isNotEmpty(),
                            ) {
                                HomeItem(
                                    navController = navController,
                                    data = it,
                                )
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ChartTitle()
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(
                                    targetState = chartLoading,
                                    label = "Chart",
                                ) { loading ->
                                    if (!loading) {
                                        chart?.let {
                                            ChartData(
                                                chart = it,
                                                viewModel = viewModel,
                                                navController = navController,
                                                context = context,
                                            )
                                        }
                                    } else {
                                        CenterLoadingBox(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp),
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            EndOfPage()
                        }
                    }
                } else {
                    Column {
                        Spacer(
                            Modifier.height(
                                with(LocalDensity.current) {
                                    topAppBarHeightPx.toDp()
                                },
                            ),
                        )
                        HomeShimmer()
                    }
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    }.onGloballyPositioned { coordinates ->
                        topAppBarHeightPx = coordinates.size.height
                    },
        ) {
            AnimatedVisibility(
                visible = scrollState.isScrollingUp(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                HomeTopAppBar(navController)
            }
            AnimatedVisibility(
                visible = !scrollState.isScrollingUp(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.statusBars,
                            ),
                )
            }
            Row(
                modifier =
                    Modifier
                        .horizontalScroll(chipRowState)
                        .padding(vertical = 8.dp, horizontal = 15.dp)
                        .background(Color.Transparent),
            ) {
                Config.listOfHomeChip.forEach { id ->
                    val isSelected =
                        when (params) {
                            Constants.HOME_PARAMS_RELAX -> id == R.string.relax
                            Constants.HOME_PARAMS_SLEEP -> id == R.string.sleep
                            Constants.HOME_PARAMS_ENERGIZE -> id == R.string.energize
                            Constants.HOME_PARAMS_SAD -> id == R.string.sad
                            Constants.HOME_PARAMS_ROMANCE -> id == R.string.romance
                            Constants.HOME_PARAMS_FEEL_GOOD -> id == R.string.feel_good
                            Constants.HOME_PARAMS_WORKOUT -> id == R.string.workout
                            Constants.HOME_PARAMS_PARTY -> id == R.string.party
                            Constants.HOME_PARAMS_COMMUTE -> id == R.string.commute
                            Constants.HOME_PARAMS_FOCUS -> id == R.string.focus
                            else -> id == R.string.all
                        }
                    Spacer(modifier = Modifier.width(4.dp))
                    Chip(
                        isAnimated = loading,
                        isSelected = isSelected,
                        text = stringResource(id = id),
                    ) {
                        when (id) {
                            R.string.all -> viewModel.setParams(null)
                            R.string.relax -> viewModel.setParams(Constants.HOME_PARAMS_RELAX)
                            R.string.sleep -> viewModel.setParams(Constants.HOME_PARAMS_SLEEP)
                            R.string.energize -> viewModel.setParams(Constants.HOME_PARAMS_ENERGIZE)
                            R.string.sad -> viewModel.setParams(Constants.HOME_PARAMS_SAD)
                            R.string.romance -> viewModel.setParams(Constants.HOME_PARAMS_ROMANCE)
                            R.string.feel_good -> viewModel.setParams(Constants.HOME_PARAMS_FEEL_GOOD)
                            R.string.workout -> viewModel.setParams(Constants.HOME_PARAMS_WORKOUT)
                            R.string.party -> viewModel.setParams(Constants.HOME_PARAMS_PARTY)
                            R.string.commute -> viewModel.setParams(Constants.HOME_PARAMS_COMMUTE)
                            R.string.focus -> viewModel.setParams(Constants.HOME_PARAMS_FOCUS)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController) {
    val welcomeViewModel: WelcomeViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val userName by welcomeViewModel.userName.collectAsStateWithLifecycle()
    val loggedIn by settingsViewModel.loggedIn.collectAsStateWithLifecycle()
    
    // Get account thumbnail URL from SettingsViewModel
    val accountThumbUrl by settingsViewModel.accountThumbUrl.collectAsStateWithLifecycle()
    
    // Also get Google accounts to ensure we have the latest data
    val googleAccounts by settingsViewModel.googleAccounts.collectAsStateWithLifecycle()
    
    // Initialize settings data when HomeTopAppBar is first displayed
    LaunchedEffect(Unit) {
        try {
            Log.d("HomeTopAppBar", "Initializing settings data...")
            settingsViewModel.getLoggedIn()
            settingsViewModel.getAccountThumbUrl()
            // Also try to refresh account data
            settingsViewModel.getAllGoogleAccount()
        } catch (e: Exception) {
            Log.e("HomeTopAppBar", "Error initializing settings: ${e.message}", e)
        }
    }
    
    // Get the active account's thumbnail URL as a fallback
    val activeAccountThumbUrl = googleAccounts.data?.find { it.isUsed }?.thumbnailUrl
    
    TopAppBar(
        title = {
            Text(
                text = if (userName?.isNotBlank() == true) "Hi, $userName" else stringResource(id = R.string.app_name),
                style = typo.titleMedium.copy(fontSize = 26.sp),
                color = Color.White,
            )
        },
        actions = {
            // Use the primary thumbnail URL or fallback to active account thumbnail
            val finalThumbUrl = accountThumbUrl?.takeIf { it.isNotEmpty() } ?: activeAccountThumbUrl
            
            // Debug logging
            Log.d("HomeTopAppBar", "loggedIn: $loggedIn, accountThumbUrl: '$accountThumbUrl', activeAccountThumbUrl: '$activeAccountThumbUrl', finalThumbUrl: '$finalThumbUrl'")
            
            // Show profile picture if logged in and has thumbnail URL, otherwise show settings icon
            if (loggedIn == DataStoreManager.TRUE && !finalThumbUrl.isNullOrEmpty()) {
                Log.d("HomeTopAppBar", "Showing profile picture with URL: $finalThumbUrl")
                ProfilePictureButton(
                    thumbnailUrl = finalThumbUrl,
                    onClick = { navController.navigate(SettingsDestination) }
                )
            } else {
                Log.d("HomeTopAppBar", "Showing settings icon - loggedIn: $loggedIn, finalThumbUrl empty: ${finalThumbUrl.isNullOrEmpty()}")
                RippleIconButton(resId = R.drawable.baseline_settings_24) {
                    navController.navigate(SettingsDestination)
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}

@Composable
fun ProfilePictureButton(
    thumbnailUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.baseline_people_alt_24),
            error = painterResource(R.drawable.baseline_people_alt_24)
        )
    }
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
) {
    Column {
        // Welcome back text and app logo removed
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            // App logo removed - only show account name
            Text(
                text = accountName,
                style = typo.headlineMedium,
                color = Color.White,
            )
        }
    }
}

@UnstableApi
@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val density = LocalDensity.current
    var widthDp by remember {
        mutableStateOf(0.dp)
    }
    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Text(
            text = stringResource(id = R.string.let_s_start_with_a_radio),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.quick_picks),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier.height(280.dp),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
            // Performance optimizations
            userScrollEnabled = true,
            reverseLayout = false,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(homeItem.contents, key = { it.hashCode() }) {
                if (it != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = it.toTrack()
                            viewModel.setQueueData(
                                QueueData(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${it.videoId}",
                                    playlistName = "\"${it.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        data = it,
                        widthDp = widthDp,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {
    val lazyListState1 = rememberLazyGridState()
    val snapperFlingBehavior1 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState1))

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.let_s_pick_a_playlist_for_you),
            style = typo.bodyMedium.copy(fontSize = 12.sp),
        )
        Text(
            text = stringResource(id = R.string.moods_amp_moment),
            style = typo.headlineMedium.copy(fontSize = 20.sp),
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.height(200.dp),
            state = lazyListState1,
            flingBehavior = snapperFlingBehavior1,
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(mood.moodsMoments, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigate(
                        MoodDestination(
                            it.params,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.genre),
            style = typo.headlineMedium.copy(fontSize = 20.sp),
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(300.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(mood.genres, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigate(
                        MoodDestination(
                            it.params,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(id = R.string.what_is_best_choice_today),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.chart),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
    }
}

@UnstableApi
@Composable
fun ChartData(
    chart: Chart,
    viewModel: HomeViewModel,
    navController: NavController,
    context: Context,
) {
    var gridWidthDp by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo.headlineMedium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(
                flingBehavior = snapperFlingBehavior,
                // Performance optimizations
                userScrollEnabled = true,
                reverseLayout = false,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(id = R.string.top_artists),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(onClick = {
                    navController.navigate(
                        ArtistDestination(
                            channelId = data.browseId,
                        ),
                    )
                }, data = data, context = context, widthDp = gridWidthDp)
            }
        }
    }
}