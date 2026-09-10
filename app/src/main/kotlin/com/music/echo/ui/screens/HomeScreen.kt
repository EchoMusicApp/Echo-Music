package echo.music.iad1tya.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.utils.completed
import com.music.innertube.utils.parseCookieString
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.GridItemSize
import echo.music.iad1tya.constants.GridItemsSizeKey
import echo.music.iad1tya.constants.GridThumbnailHeight
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.ListItemHeight
import echo.music.iad1tya.constants.RandomizeHomeOrderKey
import echo.music.iad1tya.constants.ShowSpeedDialKey
import echo.music.iad1tya.constants.SmallGridThumbnailHeight
import echo.music.iad1tya.constants.SongSortType
import echo.music.iad1tya.db.entities.Album
import echo.music.iad1tya.db.entities.Artist
import echo.music.iad1tya.db.entities.LocalItem
import echo.music.iad1tya.db.entities.Playlist
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.extension.DynamicExtensionManager
import echo.music.iad1tya.extension.PlatformDataBridge
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.AlbumGridItem
import echo.music.iad1tya.ui.component.ArtistGridItem
import echo.music.iad1tya.ui.component.LocalBottomSheetPageState
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.RandomizeGridItem
import echo.music.iad1tya.ui.component.SongGridItem
import echo.music.iad1tya.ui.component.SongListItem
import echo.music.iad1tya.ui.component.SpeedDialGridItem
import echo.music.iad1tya.ui.component.YouTubeGridItem
import echo.music.iad1tya.ui.component.YouTubeListItem
import echo.music.iad1tya.ui.component.shimmer.GridItemPlaceHolder
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.ui.component.shimmer.TextPlaceholder
import echo.music.iad1tya.ui.menu.AlbumMenu
import echo.music.iad1tya.ui.menu.ArtistMenu
import echo.music.iad1tya.ui.menu.SongMenu
import echo.music.iad1tya.ui.menu.YouTubeAlbumMenu
import echo.music.iad1tya.ui.menu.YouTubeArtistMenu
import echo.music.iad1tya.ui.menu.YouTubePlaylistMenu
import echo.music.iad1tya.ui.menu.YouTubeSongMenu
import echo.music.iad1tya.ui.utils.SnapLayoutInfoProvider
import echo.music.iad1tya.ui.utils.resize
import echo.music.iad1tya.utils.listItemShape
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.CommunityPlaylistItem
import echo.music.iad1tya.viewmodels.DailyDiscoverItem
import echo.music.iad1tya.viewmodels.HomeViewModel
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun NavController.navigateToPlaylistItem(playlist: PlaylistItem) {
    when (val playlistId = playlist.id.removePrefix("VL")) {
        "LM" -> navigate("auto_playlist/liked")
        "SE" -> navigate("auto_playlist/downloaded")
        else -> navigate("online_playlist/$playlistId")
    }
}

sealed class HomeSection(val id: String, val baseWeight: Int) {
    data object SpeedDial : HomeSection("speed_dial", 100)
    data object AiRecommendations : HomeSection("ai_recommendations", 95)
    data object QuickPicks : HomeSection("quick_picks", 90)
    data object DailyDiscover : HomeSection("daily_discover", 80)
    data object KeepListening : HomeSection("keep_listening", 50)
    data object AccountPlaylists : HomeSection("account_playlists", 40)
    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)
    data object FromTheCommunity : HomeSection("from_the_community", 20)
    data class SimilarRecommendation(val index: Int) : HomeSection("similar_recommendation_$index", 10)
    data class HomePageSection(val index: Int) : HomeSection("home_page_section_$index", 10)
    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val dbPlaylist by database.playlistByBrowseId(item.playlist.id).collectAsState(initial = null)
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    Card(
        modifier = modifier
            .width(320.dp)
            .height(420.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(0)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(1)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(2)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(3)?.thumbnail?.resize(544, 544),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.playlist.author?.name ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                item.songs.take(3).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(onClick = { onSongClick(song) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = song.thumbnail.resize(544, 544),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                IconButton(
                    onClick = {
                        item.playlist.playEndpoint?.let {
                            playerConnection?.playQueue(YouTubeQueue(it))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_widget_play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        item.playlist.radioEndpoint?.let {
                            playerConnection?.playQueue(YouTubeQueue(it))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (dbPlaylist?.playlist == null) {
                                database.transaction {
                                    val playlistEntity = PlaylistEntity(
                                        name = item.playlist.title,
                                        browseId = item.playlist.id,
                                        thumbnailUrl = item.playlist.thumbnail,
                                        remoteSongCount = item.playlist.songCountText?.split(" ")?.firstOrNull()?.toIntOrNull(),
                                        playEndpointParams = item.playlist.playEndpoint?.params,
                                        shuffleEndpointParams = item.playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = item.playlist.radioEndpoint?.params
                                    ).toggleLike()
                                    insert(playlistEntity)
                                    scope.launch(Dispatchers.IO) {
                                        item.songs.ifEmpty {
                                            YouTube.playlist(item.playlist.id).completed()
                                                .getOrNull()?.songs.orEmpty()
                                        }.map { it.toMediaMetadata() }
                                            .onEach(::insert)
                                            .mapIndexed { index, song ->
                                                PlaylistSongMap(
                                                    songId = song.id,
                                                    playlistId = playlistEntity.id,
                                                    position = index,
                                                    setVideoId = song.setVideoId
                                                )
                                            }
                                            .forEach(::insert)
                                    }
                                }
                            } else {
                                database.transaction {
                                    val currentPlaylist = dbPlaylist!!.playlist
                                    update(currentPlaylist.toggleLike())
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(if (isBookmarked) R.drawable.library_add_check else R.drawable.library_add),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: DailyDiscoverItem,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val database = LocalDatabase.current
    val playCount by database.getLifetimePlayCount(dailyDiscover.recommendation.id).collectAsState(initial = 0)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val song = dailyDiscover.recommendation as? SongItem
    val playsString = stringResource(R.string.plays)

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (song != null) {
                        menuState.show {
                            YouTubeSongMenu(
                                song = song,
                                navController = navController,
                                onDismiss = { menuState.dismiss() }
                            )
                        }
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(28.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dailyDiscover.recommendation.thumbnail?.resize(1200, 1200))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = dailyDiscover.recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = buildString {
                                append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
                                if (playCount > 0) {
                                    append(" • $playCount $playsString")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    val messages = listOf(
                        R.string.daily_discover_sounds_like,
                        R.string.daily_discover_because_you_listen_to,
                        R.string.daily_discover_similar_to,
                        R.string.daily_discover_based_on,
                        R.string.daily_discover_for_fans_of
                    )
                    val messageRes = remember(dailyDiscover.seed.id) {
                        messages[kotlin.math.abs(dailyDiscover.seed.id.hashCode()) % messages.size]
                    }

                    Text(
                        text = stringResource(messageRes, "${dailyDiscover.seed.title} • ${dailyDiscover.seed.artists.joinToString(", ") { it.name }}"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val dynamicExtensionManager = remember { DynamicExtensionManager(context) }
    val availableExtensions by dynamicExtensionManager.availableExtensions.collectAsState()

    LaunchedEffect(Unit) {
        dynamicExtensionManager.fetchRepository()
    }

    val installedExtensions = remember(availableExtensions) {
        availableExtensions.filter { it.isInstalled }
    }

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val aiRecommendedPlaylist by viewModel.aiRecommendedPlaylist.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val dailyDiscover by viewModel.dailyDiscover.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState()

    val allLocalItems by database.songs(SongSortType.CREATE_DATE, true).collectAsState(initial = emptyList())
    val speedDialItems by viewModel.speedDialItems.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isRandomizing by viewModel.isRandomizing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, true)
    val (showSpeedDial) = rememberPreference(ShowSpeedDialKey, true)

    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val url = if (isLoggedIn) accountImageUrl else null

    var isVideoMode by rememberSaveable { mutableStateOf(false) }

    var activeCapsuleId by rememberSaveable { mutableStateOf("all") }
    var activeCapsuleName by rememberSaveable { mutableStateOf("All") }
    var activeCapsuleColor by remember { mutableStateOf(Color(0xFF00E5FF)) }

    val animatedAuraColor by animateColorAsState(
        targetValue = activeCapsuleColor,
        animationSpec = tween(durationMillis = 350),
        label = "CapsuleAura"
    )

    var platformFeedItems by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    var isPlatformLoading by remember { mutableStateOf(false) }

    LaunchedEffect(activeCapsuleId, installedExtensions) {
        if (activeCapsuleId == "universal") {
            isPlatformLoading = true
            val combinedItems = mutableListOf<YTItem>()
            if (installedExtensions.isNotEmpty()) {
                for (ext in installedExtensions) {
                    try {
                        val feed = PlatformDataBridge.fetchPlatformFeed(ext.id)
                        if (feed.isNotEmpty()) {
                            combinedItems.addAll(feed)
                        } else {
                            val ytFallback = YouTube.search(ext.name, com.music.innertube.models.filter.Const.MusicFilter.SONG).getOrNull()?.items.orEmpty()
                            combinedItems.addAll(ytFallback)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            val ytFallback = YouTube.search(ext.name, com.music.innertube.models.filter.Const.MusicFilter.SONG).getOrNull()?.items.orEmpty()
                            combinedItems.addAll(ytFallback)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
            platformFeedItems = combinedItems.distinctBy { it.id }
            isPlatformLoading = false
        } else if (activeCapsuleId != "all" && activeCapsuleId != "offline") {
            isPlatformLoading = true
            try {
                val feed = PlatformDataBridge.fetchPlatformFeed(activeCapsuleId)
                if (feed.isNotEmpty()) {
                    platformFeedItems = feed
                } else {
                    val extItem = installedExtensions.find { it.id == activeCapsuleId }
                    val queryName = extItem?.name ?: activeCapsuleId
                    platformFeedItems = YouTube.search(queryName, com.music.innertube.models.filter.Const.MusicFilter.SONG).getOrNull()?.items.orEmpty()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val extItem = installedExtensions.find { it.id == activeCapsuleId }
                    val queryName = extItem?.name ?: activeCapsuleId
                    platformFeedItems = YouTube.search(queryName, com.music.innertube.models.filter.Const.MusicFilter.SONG).getOrNull()?.items.orEmpty()
                } catch (ex: Exception) {
                    platformFeedItems = emptyList()
                }
            }
            isPlatformLoading = false
        }
    }

    var showExtensionHubDialog by rememberSaveable { mutableStateOf(false) }
    var showExtensionLoginDialog by rememberSaveable { mutableStateOf(false) }
    var selectedExtensionForLogin by rememberSaveable { mutableStateOf<String?>(null) }
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isInPlaceSearchActive by rememberSaveable { mutableStateOf(false) }

    var searchResults by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    var isSearchingInCapsule by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, activeCapsuleId) {
        if (searchQuery.isNotBlank()) {
            isSearchingInCapsule = true
            try {
                val queryPrefix = if (activeCapsuleId == "all" || activeCapsuleId == "universal" || activeCapsuleId == "offline") {
                    searchQuery
                } else {
                    "$searchQuery $activeCapsuleName"
                }
                val result = YouTube.search(queryPrefix, com.music.innertube.models.filter.Const.MusicFilter.SONG).getOrNull()?.items.orEmpty()
                searchResults = result
            } catch (e: Exception) {
                e.printStackTrace()
                searchResults = emptyList()
            }
            isSearchingInCapsule = false
        } else {
            searchResults = emptyList()
        }
    }

    val prefs = remember { context.getSharedPreferences("savish_app_prefs", Context.MODE_PRIVATE) }
    var customProfilePath by remember { mutableStateOf(prefs.getString("profile_image_path", "")) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val avatarFile = File(context.filesDir, "savish_profile_avatar.png")
                    val outputStream = FileOutputStream(avatarFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    prefs.edit().putString("profile_image_path", avatarFile.absolutePath).apply()
                    withContext(Dispatchers.Main) {
                        customProfilePath = avatarFile.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    var playingCardId by remember { mutableStateOf<String?>(null) }
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(lazylistState.isScrollInProgress) {
        if (lazylistState.isScrollInProgress && playingCardId != null) {
            playingCardId = null
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(YouTubeQueue.radio(it.toMediaMetadata()))
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { navController.navigate("album/${it.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { navController.navigate("artist/${it.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .pointerInput(item.id, isVideoMode) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isVideoMode) {
                                playingCardId = if (playingCardId == item.id) null else item.id
                            } else {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = menuState::dismiss
                                        )
                                        is PlaylistItem -> YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        },
                        onTap = {
                            when (item) {
                                is SongItem -> playerConnection.playQueue(
                                    YouTubeQueue(
                                        item.endpoint ?: WatchEndpoint(videoId = item.id),
                                        item.toMediaMetadata()
                                    )
                                )
                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigateToPlaylistItem(item)
                            }
                        }
                    )
                }
        )
    }

    val homeSections = remember(
        randomizeHomeOrder,
        randomSeed,
        speedDialItems,
        quickPicks,
        dailyDiscover,
        keepListening,
        accountPlaylists,
        forgottenFavorites,
        communityPlaylists,
        similarRecommendations,
        homePage?.sections,
        explorePage?.moodAndGenres,
        aiRecommendedPlaylist
    ) {
        val list = mutableListOf<HomeSection>()

        if (showSpeedDial && speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
        if (aiRecommendedPlaylist != null && aiRecommendedPlaylist!!.second.isNotEmpty()) list.add(HomeSection.AiRecommendations)
        if (quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
        if (dailyDiscover?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
        if (keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        similarRecommendations?.indices?.forEach { i ->
            list.add(HomeSection.SimilarRecommendation(i))
        }

        homePage?.sections?.indices?.forEach { i ->
            list.add(HomeSection.HomePageSection(i))
        }

        if (explorePage?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)

        if (randomizeHomeOrder) {
            list.sortedByDescending { section ->
                val sectionRandom = Random(randomSeed + section.id.hashCode())
                val base = when (section) {
                    HomeSection.QuickPicks -> 10000
                    HomeSection.SpeedDial, HomeSection.DailyDiscover -> 500
                    HomeSection.KeepListening, HomeSection.AccountPlaylists,
                    HomeSection.ForgottenFavorites, HomeSection.FromTheCommunity -> 300
                    else -> 100
                }
                val modifier = when (section) {
                    HomeSection.QuickPicks -> 0
                    HomeSection.SpeedDial, HomeSection.DailyDiscover -> sectionRandom.nextInt(-200, 400)
                    HomeSection.KeepListening, HomeSection.AccountPlaylists,
                    HomeSection.ForgottenFavorites, HomeSection.FromTheCommunity -> sectionRandom.nextInt(-100, 400)
                    else -> sectionRandom.nextInt(-50, 50)
                }
                base + modifier
            }
        } else {
            val defaultOrder = mapOf(
                HomeSection.QuickPicks to 1000,
                HomeSection.SpeedDial to 100,
                HomeSection.FromTheCommunity to 80,
                HomeSection.DailyDiscover to 70,
                HomeSection.KeepListening to 60,
                HomeSection.AccountPlaylists to 50,
                HomeSection.ForgottenFavorites to 40,
                HomeSection.MoodAndGenres to 10
            )

            list.sortedByDescending { section ->
                when (section) {
                    is HomeSection.SimilarRecommendation -> 30 - section.index
                    is HomeSection.HomePageSection -> 20 - section.index
                    else -> defaultOrder[section] ?: 0
                }
            }
        }
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.TopStart
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = quickPicksLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedAuraColor.copy(alpha = 0.22f),
                                animatedAuraColor.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "savish_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Text(
                                text = "GLOBAL SYNC ACTIVE",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isVideoMode = !isVideoMode
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    text = if (isVideoMode) "Savish Video" else "Savish Music",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Text(
                                    text = if (isVideoMode) "Video Mode Active" else "Audio Mode Active",
                                    color = animatedAuraColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF141414))
                                    .border(1.5.dp, animatedAuraColor, CircleShape)
                                    .combinedClickable(
                                        onClick = { navController.navigate("settings") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            photoPickerLauncher.launch("image/*")
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val effectiveProfilePath = if (!customProfilePath.isNullOrEmpty()) customProfilePath else url
                                if (!effectiveProfilePath.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(File(effectiveProfilePath).takeIf { it.exists() } ?: effectiveProfilePath)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        tint = animatedAuraColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "savish_dynamic_capsules") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isAllSelected = activeCapsuleId == "all"
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isAllSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF161616),
                            border = if (isAllSelected) BorderStroke(1.5.dp, Color(0xFF00E5FF)) else BorderStroke(1.dp, Color(0xFF282828)),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activeCapsuleId = "all"
                                    activeCapsuleName = "All"
                                    activeCapsuleColor = Color(0xFF00E5FF)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E5FF)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("All", color = if (isAllSelected) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val isUniversalSelected = activeCapsuleId == "universal"
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isUniversalSelected) Color(0xFFFF0033).copy(alpha = 0.15f) else Color(0xFF161616),
                            border = if (isUniversalSelected) BorderStroke(1.5.dp, Color(0xFFFF0033)) else BorderStroke(1.dp, Color(0xFF282828)),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            activeCapsuleId = "universal"
                                            activeCapsuleName = "Universal"
                                            activeCapsuleColor = Color(0xFFFF0033)
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showExtensionHubDialog = true
                                        }
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF0033)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Universal", color = if (isUniversalSelected) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val isOfflineSelected = activeCapsuleId == "offline"
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isOfflineSelected) Color(0xFFFF9900).copy(alpha = 0.15f) else Color(0xFF161616),
                            border = if (isOfflineSelected) BorderStroke(1.5.dp, Color(0xFFFF9900)) else BorderStroke(1.dp, Color(0xFF282828)),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activeCapsuleId = "offline"
                                    activeCapsuleName = "Offline"
                                    activeCapsuleColor = Color(0xFFFF9900)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9900)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Offline", color = if (isOfflineSelected) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        installedExtensions.forEach { ext ->
                            val isExtSelected = activeCapsuleId == ext.id
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = if (isExtSelected) ext.brandColor.copy(alpha = 0.15f) else Color(0xFF161616),
                                border = if (isExtSelected) BorderStroke(1.5.dp, ext.brandColor) else BorderStroke(1.dp, Color(0xFF282828)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeCapsuleId = ext.id
                                        activeCapsuleName = ext.name
                                        activeCapsuleColor = ext.brandColor
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ext.brandColor))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(ext.name, color = if (isExtSelected) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item(key = "savish_inplace_search") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.5.dp, animatedAuraColor, RoundedCornerShape(25.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = null,
                                tint = animatedAuraColor,
                                modifier = Modifier.size(20.dp)
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search songs in Savish $activeCapsuleName...",
                                        color = Color(0xFF757575),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        isInPlaceSearchActive = it.isNotEmpty()
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(animatedAuraColor),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        isInPlaceSearchActive = false
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.close),
                                        contentDescription = null,
                                        tint = Color(0xFFAAAAAA)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isInPlaceSearchActive && searchResults.isNotEmpty()) {
                    item(key = "search_results_title") {
                        Text(
                            text = "Results in $activeCapsuleName",
                            color = animatedAuraColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(searchResults.distinctBy { it.id }, key = { "search_${it.id}" }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
                            ytGridItem(item)
                        }
                    }
                } else if (isLoading && homePage?.chips.isNullOrEmpty()) {
                    item(key = "chips_shimmer") {
                        ShimmerHost {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(5) {
                                    TextPlaceholder(
                                        height = 30.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.width(72.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeCapsuleId == "offline") {
                    item(key = "offline_title") {
                        Text(
                            text = "Downloaded & Local Songs (${allLocalItems.size})",
                            color = animatedAuraColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    if (allLocalItems.isEmpty()) {
                        item(key = "offline_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No offline or downloaded songs found in database.",
                                    color = Color(0xFF888888),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(items = allLocalItems.distinctBy { it.id }, key = { it.id }) { localItem ->
                            localGridItem(localItem)
                        }
                    }
                }
                else if (activeCapsuleId == "universal") {
                    item(key = "universal_feed_title") {
                        Text(
                            text = if (installedExtensions.isEmpty()) "No Extensions Installed" else "Universal Aggregated Feed (${installedExtensions.size} Connected)",
                            color = animatedAuraColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (installedExtensions.isEmpty()) {
                        item(key = "universal_empty_hint") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Long-press the Universal capsule to open Extension Hub and install platforms.",
                                    color = Color(0xFF888888),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else if (isPlatformLoading) {
                        item(key = "universal_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    } else {
                        item(key = "universal_feed_grid") {
                            val distinctItems = platformFeedItems.distinctBy { it.id }
                            val rows = if (distinctItems.size > 4) 2 else 1

                            LazyHorizontalGrid(
                                state = rememberLazyGridState(),
                                rows = GridCells.Fixed(rows),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((currentGridHeight + 60.dp) * rows)
                                    .animateItem()
                            ) {
                                items(distinctItems, key = { it.id }) { item ->
                                    Box(modifier = Modifier.width(160.dp)) {
                                        ytGridItem(item)
                                    }
                                }
                            }
                        }
                    }
                }
                else if (activeCapsuleId != "all") {
                    item(key = "platform_feed_title") {
                        Text(
                            text = "$activeCapsuleName Hits & Featured",
                            color = animatedAuraColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (isPlatformLoading) {
                        item(key = "platform_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    } else {
                        item(key = "platform_feed_grid") {
                            val distinctItems = platformFeedItems.distinctBy { it.id }
                            val rows = if (distinctItems.size > 4) 2 else 1

                            LazyHorizontalGrid(
                                state = rememberLazyGridState(),
                                rows = GridCells.Fixed(rows),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((currentGridHeight + 60.dp) * rows)
                                    .animateItem()
                            ) {
                                items(distinctItems, key = { it.id }) { item ->
                                    Box(modifier = Modifier.width(160.dp)) {
                                        ytGridItem(item)
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    homeSections.forEach { section ->
                        when (section) {
                            HomeSection.SpeedDial -> {
                                speedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                                    item(key = "speed_dial_title") {
                                        Text(
                                            text = stringResource(R.string.speed_dial),
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }

                                    item(key = "speed_dial_list") {
                                        val targetItemSize = 160.dp
                                        val availableWidth = maxWidth - 32.dp
                                        val columns = (availableWidth / targetItemSize).toInt().coerceAtLeast(3)
                                        val rows = if (columns >= 6) 1 else if (columns >= 4) 2 else 3
                                        val itemsPerPage = columns * rows
                                        val itemWidth = availableWidth / columns

                                        val pagerState = rememberPagerState(pageCount = { (items.size + itemsPerPage - 1) / itemsPerPage })

                                        Column(
                                            modifier = Modifier.fillMaxWidth().animateItem(),
                                        ) {
                                            HorizontalPager(
                                                state = pagerState,
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                pageSpacing = 16.dp,
                                                modifier = Modifier.fillMaxWidth().height(itemWidth * rows),
                                            ) { page ->
                                                val pageStartIndex = page * itemsPerPage
                                                val pageItems = items.drop(pageStartIndex).take(itemsPerPage)

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    for (row in 0 until rows) {
                                                        Row(modifier = Modifier.fillMaxWidth()) {
                                                            for (col in 0 until columns) {
                                                                val itemIndex = row * columns + col
                                                                val isRandomizeSlot = (page == 0 && itemIndex == itemsPerPage - 1)

                                                                if (isRandomizeSlot) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .width(itemWidth)
                                                                            .height(itemWidth)
                                                                            .padding(4.dp)
                                                                    ) {
                                                                        RandomizeGridItem(
                                                                            isLoading = isRandomizing,
                                                                            onClick = {
                                                                                if (isRandomizing) {
                                                                                    randomizeJob?.cancel()
                                                                                } else {
                                                                                    randomizeJob = scope.launch {
                                                                                        val randomItem = viewModel.getRandomItem()
                                                                                        if (randomItem != null) {
                                                                                            when (randomItem) {
                                                                                                is SongItem -> playerConnection.playQueue(
                                                                                                    YouTubeQueue(
                                                                                                        randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id),
                                                                                                        randomItem.toMediaMetadata()
                                                                                                    )
                                                                                                )
                                                                                                is AlbumItem -> navController.navigate("album/${randomItem.id}")
                                                                                                is ArtistItem -> navController.navigate("artist/${randomItem.id}")
                                                                                                is PlaylistItem -> navController.navigateToPlaylistItem(randomItem)
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        )
                                                                    }
                                                                } else if (itemIndex < pageItems.size) {
                                                                    val item = pageItems[itemIndex]
                                                                    val isPinned by database.speedDialDao.isPinned(item.id).collectAsState(initial = false)

                                                                    Box(
                                                                        modifier = Modifier
                                                                            .width(itemWidth)
                                                                            .height(itemWidth)
                                                                            .padding(4.dp)
                                                                    ) {
                                                                        SpeedDialGridItem(
                                                                            item = item,
                                                                            isPinned = isPinned,
                                                                            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                                                            isPlaying = isPlaying,
                                                                            modifier = Modifier
                                                                                .fillMaxSize()
                                                                                .combinedClickable(
                                                                                    onClick = {
                                                                                        when (item) {
                                                                                            is SongItem -> playerConnection.playQueue(
                                                                                                YouTubeQueue(
                                                                                                    item.endpoint ?: WatchEndpoint(videoId = item.id),
                                                                                                    item.toMediaMetadata()
                                                                                                )
                                                                                            )
                                                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                                                            is PlaylistItem -> navController.navigateToPlaylistItem(item)
                                                                                        }
                                                                                    },
                                                                                    onLongClick = {
                                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                                        menuState.show {
                                                                                            when (item) {
                                                                                                is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                                                                                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                                                                                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                                                                                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                )
                                                                        )
                                                                    }
                                                                } else {
                                                                    Spacer(modifier = Modifier.width(itemWidth))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (pagerState.pageCount > 1) {
                                                Row(
                                                    modifier = Modifier
                                                        .height(24.dp)
                                                        .fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    repeat(pagerState.pageCount) { iteration ->
                                                        val color = if (pagerState.currentPage == iteration)
                                                            animatedAuraColor
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(4.dp)
                                                                .clip(CircleShape)
                                                                .background(color)
                                                                .size(8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.AiRecommendations -> {
                                aiRecommendedPlaylist?.let { pair ->
                                    val (playlist, songs) = pair
                                    item(key = "ai_recommendation_title") {
                                        Text(
                                            text = playlist.title,
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .clickable { navController.navigate("local_playlist/${playlist.id}") }
                                        )
                                    }
                                    item(key = "ai_recommendation_list") {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(items = songs.distinctBy { it.id }, key = { it.id }) { songObj ->
                                                localGridItem(songObj)
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.QuickPicks -> {
                                quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicksList ->
                                    item(key = "quick_picks_list") {
                                        val distinctQuickPicks = quickPicksList.distinctBy { it.id }
                                        HorizontalCenteredHeroCarousel(
                                            state = rememberCarouselState { distinctQuickPicks.size },
                                            maxItemWidth = 250.dp,
                                            itemSpacing = 8.dp,
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(290.dp)
                                                .animateItem()
                                        ) { index ->
                                            val originalSong = distinctQuickPicks[index]
                                            val song by database.song(originalSong.id).collectAsState(initial = originalSong)
                                            val isActive = song!!.id == mediaMetadata?.id

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .maskClip(MaterialTheme.shapes.extraLarge)
                                                    .maskBorder(
                                                        BorderStroke(1.dp, animatedAuraColor.copy(alpha = 0.5f)),
                                                        MaterialTheme.shapes.extraLarge
                                                    )
                                                    .focusable()
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (isActive) {
                                                                playerConnection.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                                            }
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                SongMenu(
                                                                    originalSong = song!!,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss
                                                                )
                                                            }
                                                        }
                                                    )
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(song!!.thumbnailUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    Color.Transparent,
                                                                    Color.Transparent,
                                                                    Color.Black.copy(alpha = 0.7f)
                                                                )
                                                            )
                                                        )
                                                )

                                                if (isActive && isPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(12.dp)
                                                            .size(32.dp)
                                                            .background(animatedAuraColor, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.volume_up),
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(16.dp)
                                                ) {
                                                    Text(
                                                        text = song!!.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = song!!.artists.joinToString { it.name },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.FromTheCommunity -> {
                                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                                    item(key = "community_playlists_title") {
                                        Text(
                                            text = stringResource(R.string.from_the_community),
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }

                                    item(key = "community_playlists_content") {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(playlists.distinctBy { it.playlist.id }, key = { it.playlist.id }) { item ->
                                                CommunityPlaylistCard(
                                                    item = item,
                                                    onClick = { navController.navigateToPlaylistItem(item.playlist) },
                                                    onSongClick = { song ->
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                                song.toMediaMetadata()
                                                            )
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.DailyDiscover -> {
                                dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
                                    item(key = "daily_discover_title") {
                                        Text(
                                            text = stringResource(R.string.your_daily_discover),
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                    item(key = "daily_discover_content") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(340.dp)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val carouselState = rememberCarouselState { discoverList.size }
                                            HorizontalMultiBrowseCarousel(
                                                state = carouselState,
                                                preferredItemWidth = 320.dp,
                                                itemSpacing = 16.dp,
                                                modifier = Modifier.fillMaxWidth().height(320.dp)
                                            ) { i ->
                                                val item = discoverList[i]
                                                DailyDiscoverCard(
                                                    dailyDiscover = item,
                                                    onClick = {
                                                        val song = item.recommendation as? SongItem
                                                        val meta = song?.toMediaMetadata()
                                                        if (meta != null) {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), meta)
                                                            )
                                                        }
                                                    },
                                                    navController = navController,
                                                    modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.KeepListening -> {
                                keepListening?.takeIf { it.isNotEmpty() }?.let { keepList ->
                                    item(key = "keep_listening_title") {
                                        Text(
                                            text = stringResource(R.string.keep_listening),
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }

                                    item(key = "keep_listening_list") {
                                        val rows = if (keepList.size > 6) 2 else 1
                                        LazyHorizontalGrid(
                                            state = rememberLazyGridState(),
                                            rows = GridCells.Fixed(rows),
                                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((currentGridHeight + with(LocalDensity.current) {
                                                    MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                                            MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                                                }) * rows)
                                                .animateItem()
                                        ) {
                                            items(keepList.distinctBy { it.id }, key = { it.id }) {
                                                localGridItem(it)
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.AccountPlaylists -> {
                                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylistsList ->
                                    item(key = "account_playlists_title") {
                                        Text(
                                            text = accountName,
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                                .clickable { navController.navigate("account") }
                                        )
                                    }

                                    item(key = "account_playlists_list") {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(items = accountPlaylistsList.distinctBy { it.id }, key = { it.id }) { item ->
                                                ytGridItem(item)
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.ForgottenFavorites -> {
                                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavoritesList ->
                                    item(key = "forgotten_favorites_title") {
                                        Text(
                                            text = stringResource(R.string.forgotten_favorites),
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }

                                    item(key = "forgotten_favorites_list") {
                                        val rows = min(4, forgottenFavoritesList.size)
                                        LazyHorizontalGrid(
                                            state = forgottenFavoritesLazyGridState,
                                            rows = GridCells.Fixed(rows),
                                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                            flingBehavior = rememberSnapFlingBehavior(forgottenFavoritesSnapLayoutInfoProvider),
                                            modifier = Modifier.fillMaxWidth().height(ListItemHeight * rows).animateItem()
                                        ) {
                                            itemsIndexed(
                                                items = forgottenFavoritesList.distinctBy { it.id },
                                                key = { _, it -> it.id }
                                            ) { index, originalSong ->
                                                val song by database.song(originalSong.id).collectAsState(initial = originalSong)
                                                SongListItem(
                                                    song = song!!,
                                                    showInLibraryIcon = true,
                                                    isActive = song!!.id == mediaMetadata?.id,
                                                    isPlaying = isPlaying,
                                                    isSwipeable = false,
                                                    shape = listItemShape(index = index % rows, count = rows),
                                                    trailingContent = {
                                                        IconButton(
                                                            onClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = song!!,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss
                                                                    )
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.more_vert),
                                                                contentDescription = null
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .width(horizontalLazyGridItemWidth)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (song!!.id == mediaMetadata?.id) {
                                                                    playerConnection.togglePlayPause()
                                                                } else {
                                                                    playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                                                }
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = song!!,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss
                                                                    )
                                                                }
                                                            }
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is HomeSection.SimilarRecommendation -> {
                                val recommendation = similarRecommendations?.getOrNull(section.index)
                                recommendation?.let {
                                    item(key = "similar_to_title_${section.index}") {
                                        Text(
                                            text = recommendation.title.title,
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }

                                    item(key = "similar_to_list_${section.index}") {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(recommendation.items.distinctBy { it.id }, key = { it.id }) { item ->
                                                ytGridItem(item)
                                            }
                                        }
                                    }
                                }
                            }
                            is HomeSection.HomePageSection -> {
                                val sectionData = homePage?.sections?.getOrNull(section.index)
                                sectionData?.let {
                                    val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
                                    val isSongsOnlySection = sectionData.items.isNotEmpty() && sectionData.items.all { it is SongItem }

                                    item(key = "home_section_title_${section.index}") {
                                        Text(
                                            text = sectionData.title,
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                                .clickable {
                                                    sectionData.endpoint?.let { endpoint ->
                                                        when {
                                                            endpoint.browseId == "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                                            endpoint.params != null -> navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                                                            else -> navController.navigate("browse/${endpoint.browseId}")
                                                        }
                                                    }
                                                }
                                        )
                                    }

                                    if (isSongsOnlySection) {
                                        item(key = "home_section_list_${section.index}") {
                                            LazyHorizontalGrid(
                                                state = rememberLazyGridState(),
                                                rows = GridCells.Fixed(4),
                                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                                modifier = Modifier.fillMaxWidth().height(ListItemHeight * 4).animateItem()
                                            ) {
                                                itemsIndexed(
                                                    items = sectionSongs.distinctBy { it.id },
                                                    key = { _, it -> it.id }
                                                ) { index, song ->
                                                    YouTubeListItem(
                                                        item = song,
                                                        isActive = song.id == mediaMetadata?.id,
                                                        isPlaying = isPlaying,
                                                        isSwipeable = false,
                                                        shape = listItemShape(index = index % 4, count = 4),
                                                        trailingContent = {
                                                            IconButton(
                                                                onClick = {
                                                                    menuState.show {
                                                                       YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .width(horizontalLazyGridItemWidth)
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (song.id == mediaMetadata?.id) {
                                                                        playerConnection.togglePlayPause()
                                                                    } else {
                                                                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    menuState.show {
                                                                        YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                                                    }
                                                                }
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        item(key = "home_section_list_${section.index}") {
                                            LazyRow(
                                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                                modifier = Modifier.animateItem()
                                            ) {
                                                items(sectionData.items.distinctBy { it.id }, key = { it.id }) { item ->
                                                    ytGridItem(item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HomeSection.MoodAndGenres -> {
                                explorePage?.moodAndGenres?.let { moodList ->
                                    item(key = "mood_genres_title") {
                                        Text(
                                            text = stringResource(R.string.mood_and_genres),
                                            color = animatedAuraColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                                .clickable { navController.navigate("mood_and_genres") }
                                        )
                                    }

                                    val displayMoods = moodList.take(12).chunked(3)
                                    items(displayMoods) { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { mood ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF1E2621))
                                                        .clickable {
                                                            navController.navigate("youtube_browse/${mood.endpoint.browseId}?params=${mood.endpoint.params}")
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = mood.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 6.dp)
                                                    )
                                                }
                                            }
                                            repeat(3 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                    item(key = "loading_shimmer") {
                        ShimmerHost(modifier = Modifier.animateItem()) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues())
                            ) {
                                repeat(3) {
                                    Spacer(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 12.dp)
                                            .width(250.dp)
                                            .height(290.dp)
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }

                            TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(200.dp))
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                                repeat(2) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        repeat(3) {
                                            GridItemPlaceHolder(modifier = Modifier.weight(1f), fillMaxWidth = true)
                                        }
                                    }
                                }
                            }

                            TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(250.dp))
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues())
                            ) {
                                repeat(4) {
                                    GridItemPlaceHolder()
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (showExtensionHubDialog) {
        val availableExtensions by dynamicExtensionManager.availableExtensions.collectAsState()

        AlertDialog(
            onDismissRequest = { showExtensionHubDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF0033)))
                    Text(
                        text = "Savish Extension Hub",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Download platforms from the remote repository to add them to your Home capsules:",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    if (availableExtensions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    } else {
                        availableExtensions.forEach { ext ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1B1B1B),
                                border = BorderStroke(1.dp, if (ext.isInstalled) ext.brandColor else Color(0xFF2E2E2E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(ext.brandColor)
                                        )
                                        Column {
                                            Text(
                                                text = ext.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = ext.description.ifEmpty { ext.version },
                                                color = Color(0xFF888888),
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (ext.requiresLogin && ext.isInstalled) {
                                            TextButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedExtensionForLogin = ext.name
                                                    showExtensionLoginDialog = true
                                                },
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Login", fontSize = 11.sp, color = Color(0xFFFF9900))
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (ext.isInstalled) {
                                                    dynamicExtensionManager.uninstallExtension(ext)
                                                } else {
                                                    scope.launch {
                                                        dynamicExtensionManager.downloadAndInstall(ext) { _ -> }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = if (ext.isInstalled) "Installed" else "Install",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionHubDialog = false }) {
                    Text("Close Hub", color = Color(0xFFFF0033), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141414),
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showExtensionLoginDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionLoginDialog = false },
            title = {
                Text(
                    text = "${selectedExtensionForLogin ?: "Platform"} Real Login",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Connect your account via secure browser OAuth flow to sync playlists and stream music.",
                        fontSize = 13.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val platformName = selectedExtensionForLogin?.lowercase() ?: "spotify"
                            val loginUrl = when {
                                platformName.contains("spotify") -> "https://accounts.spotify.com/en/login"
                                platformName.contains("gaana") -> "https://gaana.com/login"
                                platformName.contains("jiosaavn") -> "https://www.jiosaavn.com"
                                platformName.contains("apple") -> "https://music.apple.com"
                                else -> "https://accounts.spotify.com/en/login"
                            }
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(loginUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            showExtensionLoginDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect Account (Open Login)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExtensionLoginDialog = false }) {
                    Text("Cancel", color = Color(0xFFFF0033), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141414),
            shape = RoundedCornerShape(24.dp)
        )
    }
}
