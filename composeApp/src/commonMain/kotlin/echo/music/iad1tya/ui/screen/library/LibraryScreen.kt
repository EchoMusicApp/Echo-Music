package echo.music.iad1tya.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import echo.music.iad1tya.common.LibraryChipType
import echo.music.iad1tya.domain.utils.LocalResource
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.extension.copy
import echo.music.iad1tya.extension.isScrollingUp
import echo.music.iad1tya.ui.component.Chip
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.GridLibraryPlaylist
import echo.music.iad1tya.ui.component.LibraryItem
import echo.music.iad1tya.ui.component.LibraryItemState
import echo.music.iad1tya.ui.component.LibraryItemType
import echo.music.iad1tya.ui.component.LibraryTilingBox
import echo.music.iad1tya.ui.icon.PeopleAlt
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.LibraryViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.chart
import echomusic.composeapp.generated.resources.create
import echomusic.composeapp.generated.resources.downloaded_playlists
import echomusic.composeapp.generated.resources.favorite_playlists
import echomusic.composeapp.generated.resources.favorite_podcasts
import echomusic.composeapp.generated.resources.library
import echomusic.composeapp.generated.resources.mix_for_you
import echomusic.composeapp.generated.resources.no_YouTube_playlists
import echomusic.composeapp.generated.resources.no_charts_found
import echomusic.composeapp.generated.resources.no_favorite_playlists
import echomusic.composeapp.generated.resources.no_favorite_podcasts
import echomusic.composeapp.generated.resources.no_mixes_found
import echomusic.composeapp.generated.resources.no_playlists_added
import echomusic.composeapp.generated.resources.no_playlists_downloaded
import echomusic.composeapp.generated.resources.playlist_name
import echomusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import echomusic.composeapp.generated.resources.echomusic_charts
import echomusic.composeapp.generated.resources.your_library
import echomusic.composeapp.generated.resources.your_playlists
import echomusic.composeapp.generated.resources.your_youtube_playlists

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val youTubeMixForYou by viewModel.youTubeMixForYou.collectAsStateWithLifecycle()
    val listCanvasSong by viewModel.listCanvasSong.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val chartPlaylists by viewModel.chartPlaylists.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    var topAppBarHeight by remember {
        mutableStateOf(0.dp)
    }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(nowPlaying) {
        Logger.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    val chipRowState = rememberScrollState()
    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()

    LaunchedEffect(currentFilter) {
        when (currentFilter) {
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                if (youTubePlaylist.data.isNullOrEmpty()) {
                    viewModel.getYouTubePlaylist()
                }
            }



            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getCanvasSong()
                viewModel.getRecentlyAdded()
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                viewModel.getLocalPlaylist()
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }
            else -> {}


        }
    }

    Crossfade(
        modifier = Modifier.hazeSource(hazeState),
        targetState = currentFilter,
    ) { filter ->
        when (filter) {
            LibraryChipType.YOUR_LIBRARY -> {
                val state = rememberLazyListState()
                val isScrollingUp by state.isScrollingUp()
                LaunchedEffect(state) {
                    snapshotFlow { state.firstVisibleItemIndex }
                        .collect {
                            if (it <= 1) {
                                onScrolling.invoke(true)
                            } else {
                                onScrolling.invoke(isScrollingUp)
                            }
                        }
                }
                LazyColumn(
                    contentPadding =
                        innerPadding.copy(
                            top = topAppBarHeight,
                        ),
                    state = state,
                ) {
                    item {
                        LibraryTilingBox(navController)
                    }

                    if (!listCanvasSong.data.isNullOrEmpty()) {
                        item {
                            LibraryItem(
                                state =
                                    LibraryItemState(
                                        type = LibraryItemType.CanvasSong,
                                        data = listCanvasSong.data ?: emptyList(),
                                        isLoading = listCanvasSong is LocalResource.Loading,
                                    ),
                                navController = navController,
                            )
                        }
                    }

                    item {
                        LibraryItem(
                            state =
                                LibraryItemState(
                                    type =
                                        LibraryItemType.RecentlyAdded(
                                            playingVideoId = nowPlaying,
                                        ),
                                    data = recentlyAdded.data ?: emptyList(),
                                    isLoading = recentlyAdded is LocalResource.Loading,
                                ),
                            navController = navController,
                        )
                    }
                    item(contentType = "ad_banner") {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                        }
                    }
                    item {
                        EndOfPage()
                    }
                }
            }

            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    youTubePlaylist,
                    emptyText = Res.string.no_YouTube_playlists,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getYouTubePlaylist()
                }
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    yourLocalPlaylist,
                    onScrolling = onScrolling,
                    emptyText = Res.string.no_playlists_added,
                    createNewPlaylist = {
                        showAddSheet = true
                    },
                ) {
                    viewModel.getLocalPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePlaylist,
                    emptyText = Res.string.no_favorite_playlists,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getPlaylistFavorite()
                }
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    downloadedPlaylist,
                    emptyText = Res.string.no_playlists_downloaded,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getDownloadedPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePodcasts,
                    emptyText = Res.string.no_favorite_podcasts,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getFavoritePodcasts()
                }
            }
            else -> {}


        }
    }
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf("") }
        val showAddSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showAddSheetState.hide()
                    showAddSheet = false
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = MaterialTheme.colorScheme.outline,
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.playlist_name))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                viewModel.makeToast(runBlocking { getString(Res.string.playlist_name_cannot_be_empty) })
                            } else {
                                viewModel.createPlaylist(newTitle)
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.create))
                    }
                }
            }
        }
    }
    Column(
        Modifier
            .background(Color.Transparent)
            .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            }.onGloballyPositioned { coordinates ->
                topAppBarHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.library),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            navigationIcon = {
                AnimatedVisibility(
                    !accountThumbnail.isNullOrEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(accountThumbnail)
                                .crossfade(550)
                                .build(),
                        placeholder = rememberVectorPainter(echoIcons.PeopleAlt),
                        error = rememberVectorPainter(echoIcons.PeopleAlt),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape),
                    )
                }
            },
        )
        Row(
            modifier =
                Modifier
                    .horizontalScroll(chipRowState)
                    .padding(horizontal = 15.dp)
                    .padding(bottom = 8.dp)
                    .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LibraryChipType.entries.forEach { type ->
                if (type == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST && !loggedIn) {
                    return@forEach
                }
                Chip(
                    isAnimated = false,
                    isSelected = type == currentFilter,
                    text =
                        when (type) {
                            LibraryChipType.YOUR_LIBRARY -> stringResource(Res.string.your_library)
                            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> stringResource(Res.string.your_youtube_playlists)

                            LibraryChipType.LOCAL_PLAYLIST -> stringResource(Res.string.your_playlists)
                            LibraryChipType.FAVORITE_PLAYLIST -> stringResource(Res.string.favorite_playlists)
                            LibraryChipType.DOWNLOADED_PLAYLIST -> stringResource(Res.string.downloaded_playlists)
                            LibraryChipType.FAVORITE_PODCAST -> stringResource(Res.string.favorite_podcasts)
else -> ""

                        },
                ) {
                    viewModel.setCurrentScreen(type)
                }
            }
        }
    }
}