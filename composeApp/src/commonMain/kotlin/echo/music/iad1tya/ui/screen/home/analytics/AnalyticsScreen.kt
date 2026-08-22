package echo.music.iad1tya.ui.screen.home.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import echo.music.iad1tya.common.Config
import echo.music.iad1tya.domain.data.entities.SongEntity
import echo.music.iad1tya.domain.mediaservice.handler.PlaylistType
import echo.music.iad1tya.domain.mediaservice.handler.QueueData
import echo.music.iad1tya.domain.utils.LocalResource
import echo.music.iad1tya.domain.utils.connectArtists
import echo.music.iad1tya.domain.utils.toArrayListTrack
import echo.music.iad1tya.domain.utils.toTrack
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.extension.getScreenSizeInfo
import echo.music.iad1tya.extension.getStringBlocking
import echo.music.iad1tya.extension.smoothScrimBrush
import echo.music.iad1tya.ui.component.CenterLoadingBox
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.FiveImagesComponent
import echo.music.iad1tya.ui.component.ImageData
import echo.music.iad1tya.ui.component.NowPlayingBottomSheet
import echo.music.iad1tya.ui.component.SongFullWidthItems
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.CalendarToday
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.navigation.destination.home.RecentlySongsDestination
import echo.music.iad1tya.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import echo.music.iad1tya.ui.navigation.destination.list.AlbumDestination
import echo.music.iad1tya.ui.navigation.destination.list.ArtistDestination
import echo.music.iad1tya.ui.screen.library.LibraryDynamicPlaylistType
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.AnalyticsUiState
import echo.music.iad1tya.viewModel.AnalyticsViewModel
import echo.music.iad1tya.viewModel.SharedViewModel
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.artists
import echomusic.composeapp.generated.resources.date_range
import echomusic.composeapp.generated.resources.last_30_days
import echomusic.composeapp.generated.resources.last_7_days
import echomusic.composeapp.generated.resources.last_90_days
import echomusic.composeapp.generated.resources.lower_plays
import echomusic.composeapp.generated.resources.more
import echomusic.composeapp.generated.resources.no_data_analytics
import echomusic.composeapp.generated.resources.seconds
import echomusic.composeapp.generated.resources.songs_played
import echomusic.composeapp.generated.resources.this_year
import echomusic.composeapp.generated.resources.top_song
import echomusic.composeapp.generated.resources.total_listened_time
import echomusic.composeapp.generated.resources.your_recently_played
import echomusic.composeapp.generated.resources.your_top_albums
import echomusic.composeapp.generated.resources.your_top_artists
import echomusic.composeapp.generated.resources.your_top_tracks

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun AnalyticsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    analyticsViewModel: AnalyticsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val density = LocalDensity.current
    val screenSizeInfo = getScreenSizeInfo()
    val uiState by analyticsViewModel.analyticsUIState.collectAsStateWithLifecycle()
    val playingTrack by sharedViewModel.nowPlayingState.map { it?.track?.videoId }.collectAsState(null)

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }
    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }

    val onItemMoreClick: (song: SongEntity) -> Unit = {
        currentItem = it
        itemBottomSheetShow = true
    }

    val onArtistClick: (channelId: String) -> Unit = {
        navController.navigate(
            ArtistDestination(
                channelId = it,
            ),
        )
    }

    val onAlbumClick: (browseId: String) -> Unit = {
        navController.navigate(
            AlbumDestination(
                browseId = it,
            ),
        )
    }

    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return
        NowPlayingBottomSheet(
            onDismiss = {
                itemBottomSheetShow = false
                currentItem = null
            },
            navController = navController,
            song = track,
        )
    }

    LaunchedEffect(uiState) {
        Logger.d(
            "AnalyticsScreen",
            "UI State updated: ${uiState.scrobblesCount.data}, ${uiState.artistCount.data}, ${uiState.totalListenTimeInSeconds.data}",
        )
        Logger.d("AnalyticsScreen", "Top Tracks: ${uiState.topTracks.data?.joinToString { it.second.title }}")
        Logger.d("AnalyticsScreen", "Top Artists: ${uiState.topArtists.data?.joinToString { it.second.name }}")
        Logger.d("AnalyticsScreen", "Top Albums: ${uiState.topAlbums.data?.joinToString { it.second.title }}")
        Logger.d("AnalyticsScreen", "Recently Played: ${uiState.recentlyRecord.data?.joinToString { it.second.title }}")
        Logger.d("AnalyticsScreen", "Scrobbles line chart data: ${uiState.scrobblesLineChart.data}")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            // Header item
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((screenSizeInfo.hDP / 2.5).dp),
                ) {
                    when (val topTracks = uiState.topTracks) {
                        is LocalResource.Success if (!topTracks.data.isNullOrEmpty()) -> {
                            val topTrack = topTracks.data?.firstOrNull() ?: return@Box
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(topTrack.second.thumbnails)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(topTrack.second.thumbnails)
                                        .crossfade(550)
                                        .build(),
                                contentDescription = "",
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .fillMaxSize(),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush =
                                            smoothScrimBrush(
                                                from = MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                                to = MaterialTheme.colorScheme.background,
                                                // The header is hDP/2.5 tall, so starting at 3/4 of
                                                // that left only a quarter of it to fade in — short
                                                // enough that the edge still read as a hard cut.
                                                // Half the header gives the ramp room to disappear.
                                                startY = (screenSizeInfo.hPX / 2.5f) / 2,
                                            ),
                                    ),
                            )
                            Column(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.top_song),
                                    style = typo().titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(
                                        // Takes the leftover width and stops there. Without a
                                        // weight the column sizes to the song title, so a long
                                        // name echoly grew across the "Listened time" column.
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            topTrack.second.title,
                                            style = typo().labelMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            topTrack.second.artistName?.connectArtists() ?: "",
                                            style = typo().bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.wrapContentWidth(),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            "Listened time",
                                            style = typo().bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                        )
                                        Text(
                                            "${topTrack.first.totalListeningTime} seconds",
                                            style = typo().bodyLarge,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }

                        is LocalResource.Loading -> {
                            CenterLoadingBox(
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        else -> {
                            Box(Modifier.fillMaxSize()) {
                                Text(
                                    stringResource(Res.string.no_data_analytics),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = typo().bodyLarge,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (val scrobblesCount = uiState.scrobblesCount) {
                        is LocalResource.Success -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    stringResource(Res.string.songs_played),
                                    style = typo().bodyMedium,
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    "${scrobblesCount.data ?: 0}",
                                    style = typo().bodyLarge,
                                    maxLines = 1,
                                )
                            }
                        }

                        else -> {}
                    }

                    when (val artistCount = uiState.artistCount) {
                        is LocalResource.Success -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    stringResource(Res.string.artists),
                                    style = typo().bodyMedium,
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    "${artistCount.data ?: 0}",
                                    style = typo().bodyLarge,
                                    maxLines = 1,
                                )
                            }
                        }

                        else -> {}
                    }

                    when (val totalPlayedTime = uiState.totalListenTimeInSeconds) {
                        is LocalResource.Success -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    stringResource(Res.string.total_listened_time),
                                    style = typo().bodyMedium,
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                                Text(
                                    "${totalPlayedTime.data ?: 0} ${stringResource(Res.string.seconds)}",
                                    style = typo().bodyLarge,
                                    maxLines = 1,
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }

            item {
                when (val recentlyRecord = uiState.recentlyRecord) {
                    is LocalResource.Success if (!recentlyRecord.data.isNullOrEmpty()) -> {
                        val records = recentlyRecord.data ?: return@item
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .padding(horizontal = 24.dp)
                                        .padding(top = 12.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.your_recently_played),
                                    style = typo().labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        navController.navigate(RecentlySongsDestination)
                                    },
                                    colors =
                                        ButtonDefaults
                                            .textButtonColors()
                                            .copy(
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                ) {
                                    Text(stringResource(Res.string.more), style = typo().bodySmall)
                                }
                            }

                            records.forEach { pair ->
                                val song = pair.second.toTrack()
                                SongFullWidthItems(
                                    track = song,
                                    isPlaying = song.videoId == playingTrack,
                                    modifier = Modifier.fillMaxWidth(),
                                    onMoreClickListener = {
                                        onItemMoreClick(pair.second)
                                    },
                                    onClickListener = {
                                        val targetList = records.map { it.second }
                                        val playTrack = pair.second
                                        with(sharedViewModel) {
                                            setQueueData(
                                                QueueData.Data(
                                                    listTracks = targetList.toArrayListTrack(),
                                                    firstPlayedTrack = playTrack.toTrack(),
                                                    playlistId = null,
                                                    playlistName = getStringBlocking(Res.string.your_recently_played),
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            loadMediaItem(
                                                playTrack.toTrack(),
                                                Config.PLAYLIST_CLICK,
                                                targetList.indexOf(playTrack).coerceAtLeast(0),
                                            )
                                        }
                                    },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(
                                            arrayListOf(song),
                                        )
                                    },
                                    rightView = {
                                        Text(
                                            text =
                                                pair.first.timestamp.format(
                                                    LocalDateTime.Format {
                                                        hour()
                                                        char(':')
                                                        minute()
                                                        chars(" - ")
                                                        day()
                                                        char(' ')
                                                        monthName(
                                                            MonthNames.ENGLISH_FULL,
                                                        )
                                                        char(' ')
                                                        year()
                                                    },
                                                ),
                                            style = typo().bodySmall,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }

            item {
                // Top artists
                when (uiState.topArtists) {
                    is LocalResource.Success if (!uiState.topArtists.data.isNullOrEmpty()) -> {
                        val artists =
                            uiState.topArtists.data?.let {
                                if (it.size > 5) it.subList(0, 5) else it
                            } ?: return@item
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .padding(horizontal = 24.dp)
                                        .padding(top = 12.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.your_top_artists),
                                    style = typo().labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        navController.navigate(
                                            LibraryDynamicPlaylistDestination(
                                                type = LibraryDynamicPlaylistType.TopArtists.toStringParams(),
                                            ),
                                        )
                                    },
                                    colors =
                                        ButtonDefaults
                                            .textButtonColors()
                                            .copy(
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                ) {
                                    Text(stringResource(Res.string.more), style = typo().bodySmall)
                                }
                            }
                            FiveImagesComponent(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                images =
                                    artists.map {
                                        ImageData(
                                            imageUrl = it.second.thumbnails ?: "",
                                            title = it.second.name,
                                            subtitle = "${it.first.playCount} ${stringResource(Res.string.lower_plays)}",
                                            onClick = {
                                                onArtistClick(it.second.channelId)
                                            },
                                        )
                                    },
                            )
                        }
                    }

                    else -> {}
                }
            }

            item {
                // Top albums
                when (uiState.topAlbums) {
                    is LocalResource.Success if (!uiState.topAlbums.data.isNullOrEmpty()) -> {
                        val albums =
                            uiState.topAlbums.data?.let {
                                if (it.size > 5) it.subList(0, 5) else it
                            } ?: return@item
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .padding(horizontal = 24.dp)
                                        .padding(top = 12.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.your_top_albums),
                                    style = typo().labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        navController.navigate(
                                            LibraryDynamicPlaylistDestination(
                                                type = LibraryDynamicPlaylistType.TopAlbums.toStringParams(),
                                            ),
                                        )
                                    },
                                    colors =
                                        ButtonDefaults
                                            .textButtonColors()
                                            .copy(
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                ) {
                                    Text(stringResource(Res.string.more), style = typo().bodySmall)
                                }
                            }
                            FiveImagesComponent(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                images =
                                    albums.map {
                                        ImageData(
                                            imageUrl = it.second.thumbnails ?: "",
                                            title = it.second.title,
                                            subtitle = it.second.artistName?.connectArtists() ?: "",
                                            thirdTitle = "${it.first.playCount} ${stringResource(Res.string.lower_plays)}",
                                            onClick = {
                                                onAlbumClick(it.second.browseId)
                                            },
                                        )
                                    },
                            )
                        }
                    }

                    else -> {}
                }
            }

            item {
                // Top tracks
                when (val topTracks = uiState.topTracks) {
                    is LocalResource.Success if (!topTracks.data.isNullOrEmpty()) -> {
                        val tracks =
                            topTracks.data?.let {
                                if (it.size > 5) it.subList(0, 5) else it
                            } ?: return@item
                        val maxPlays = tracks.maxOf { it.first.playCount }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .padding(horizontal = 24.dp)
                                        .padding(top = 12.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.your_top_tracks),
                                    style = typo().labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        navController.navigate(
                                            LibraryDynamicPlaylistDestination(
                                                type = LibraryDynamicPlaylistType.TopTracks.toStringParams(),
                                            ),
                                        )
                                    },
                                    colors =
                                        ButtonDefaults
                                            .textButtonColors()
                                            .copy(
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                            ),
                                ) {
                                    Text(stringResource(Res.string.more), style = typo().bodySmall)
                                }
                            }

                            tracks.forEach { pair ->
                                val song = pair.second.toTrack()
                                SongFullWidthItems(
                                    track = song,
                                    isPlaying = song.videoId == playingTrack,
                                    modifier = Modifier.fillMaxWidth(),
                                    onMoreClickListener = {
                                        onItemMoreClick(pair.second)
                                    },
                                    onClickListener = {
                                        val targetList = tracks.map { it.second }
                                        val playTrack = pair.second
                                        with(sharedViewModel) {
                                            setQueueData(
                                                QueueData.Data(
                                                    listTracks = targetList.toArrayListTrack(),
                                                    firstPlayedTrack = playTrack.toTrack(),
                                                    playlistId = null,
                                                    playlistName = getStringBlocking(Res.string.your_top_tracks),
                                                    playlistType = PlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            loadMediaItem(
                                                playTrack.toTrack(),
                                                Config.PLAYLIST_CLICK,
                                                targetList.indexOf(playTrack).coerceAtLeast(0),
                                            )
                                        }
                                    },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(
                                            arrayListOf(song),
                                        )
                                    },
                                    rightView = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(0.4f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = "${pair.first.totalListeningTime} ${stringResource(Res.string.seconds)}",
                                                style = typo().bodySmall,
                                            )
                                            Box(Modifier.fillMaxWidth()) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .wrapContentHeight()
                                                            .fillMaxWidth(pair.first.playCount.toFloat() / maxPlays)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.onSurfaceVariant),
                                                ) {
                                                    Text(
                                                        text = "",
                                                        style = typo().bodySmall,
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.CenterStart)
                                                                .padding(horizontal = 4.dp),
                                                    )
                                                }
                                                Text(
                                                    text = "${pair.first.playCount} ${stringResource(Res.string.lower_plays)}",
                                                    style = typo().bodySmall,
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.CenterStart)
                                                            .padding(horizontal = 4.dp),
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }

            item {
                when (uiState.scrobblesLineChart) {
                    is LocalResource.Success if (!uiState.scrobblesLineChart.data.isNullOrEmpty()) -> {
                        val data = uiState.scrobblesLineChart.data ?: return@item
                        val maxPlays = data.maxOf { it.second }
                        Column(
                            modifier =
                                Modifier
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 12.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.date_range),
                                style = typo().labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    data.map { it.first }.forEach { chartType ->
                                        Box(
                                            modifier = Modifier.height(32.dp),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            when (chartType) {
                                                is AnalyticsUiState.ChartType.Day -> {
                                                    Text(
                                                        text =
                                                            chartType.day.format(
                                                                LocalDate.Format {
                                                                    day()
                                                                    char(' ')
                                                                    monthName(MonthNames.ENGLISH_ABBREVIATED)
                                                                    char(' ')
                                                                    year()
                                                                },
                                                            ),
                                                        style = typo().bodyMedium,
                                                        modifier = Modifier.padding(horizontal = 8.dp),
                                                    )
                                                }

                                                is AnalyticsUiState.ChartType.Month -> {
                                                    Text(
                                                        text = "${chartType.month} - ${chartType.year}",
                                                        style = typo().bodySmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    data.map { it.second }.forEach { playCount ->
                                        Box(
                                            modifier = Modifier.height(32.dp),
                                        ) {
                                            Box(Modifier.fillMaxWidth()) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(playCount.toFloat() / maxPlays)
                                                            .padding(vertical = 4.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.onSurfaceVariant),
                                                ) {
                                                    Text(
                                                        text = "",
                                                        style = typo().bodySmall,
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.CenterStart)
                                                                .padding(horizontal = 4.dp),
                                                    )
                                                }
                                                Text(
                                                    text = "$playCount ${stringResource(Res.string.lower_plays)}",
                                                    style = typo().bodySmall,
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.CenterStart)
                                                            .padding(horizontal = 8.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }

            item {
                EndOfPage()
            }
        }

        var dayRangeMenuExpanded by rememberSaveable { mutableStateOf(false) }
        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter),
            title = {},
            navigationIcon = {
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .wrapContentSize()
                            .align(Alignment.TopStart)
                            .padding(
                                12.dp,
                            ),
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        colors =
                            IconButtonDefaults.iconButtonColors().copy(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                        alpha = 0.8f,
                                    ),
                                contentColor =
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f,
                                    ),
                            ),
                    ) {
                        Icon(echoIcons.ArrowBackIosNew, "Back")
                    }
                }
            },
            actions = {
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .wrapContentSize()
                            .padding(
                                12.dp,
                            ),
                ) {
                    IconButton(
                        onClick = {
                            dayRangeMenuExpanded = !dayRangeMenuExpanded
                        },
                        colors =
                            IconButtonDefaults.iconButtonColors().copy(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                        alpha = 0.8f,
                                    ),
                                contentColor =
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f,
                                    ),
                            ),
                    ) {
                        Box {
                            Icon(echoIcons.CalendarToday, "Analytics", tint = MaterialTheme.colorScheme.onSurface)
                            Text(
                                when (uiState.dayRange) {
                                    AnalyticsUiState.DayRange.LAST_7_DAYS -> "7d"
                                    AnalyticsUiState.DayRange.LAST_30_DAYS -> "30d"
                                    AnalyticsUiState.DayRange.LAST_90_DAYS -> "90d"
                                    AnalyticsUiState.DayRange.THIS_YEAR -> "1y"
                                },
                                style = typo().bodySmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = dayRangeMenuExpanded,
                        onDismissRequest = { dayRangeMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.last_7_days), style = typo().labelSmall) },
                            onClick = {
                                analyticsViewModel.setDayRange(AnalyticsUiState.DayRange.LAST_7_DAYS)
                                dayRangeMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.last_30_days), style = typo().labelSmall) },
                            onClick = {
                                analyticsViewModel.setDayRange(AnalyticsUiState.DayRange.LAST_30_DAYS)
                                dayRangeMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.last_90_days), style = typo().labelSmall) },
                            onClick = {
                                analyticsViewModel.setDayRange(AnalyticsUiState.DayRange.LAST_90_DAYS)
                                dayRangeMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.this_year), style = typo().labelSmall) },
                            onClick = {
                                analyticsViewModel.setDayRange(AnalyticsUiState.DayRange.THIS_YEAR)
                                dayRangeMenuExpanded = false
                            },
                        )
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}