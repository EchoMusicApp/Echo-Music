package echo.music.iad1tya.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.NavigationTitle
import echo.music.iad1tya.ui.component.YouTubeGridItem
import echo.music.iad1tya.ui.component.shimmer.GridItemPlaceHolder
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.ui.menu.YouTubeAlbumMenu
import echo.music.iad1tya.ui.menu.YouTubeArtistMenu
import echo.music.iad1tya.ui.menu.YouTubePlaylistMenu
import echo.music.iad1tya.ui.menu.YouTubeSongMenu
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.HomeViewModel
import java.net.URLEncoder

private fun NavController.navigateToPlaylistItem(playlist: PlaylistItem) {
    when (val playlistId = playlist.id.removePrefix("VL")) {
        "LM" -> navigate("auto_playlist/liked")
        "SE" -> navigate("auto_playlist/downloaded")
        else -> navigate("online_playlist/$playlistId")
    }
}

// Specification 2: 3 Default Starting Capsules
enum class DefaultCapsule(val displayName: String, val brandColor: Color) {
    ALL("All", Color(0xFF00E5FF)),
    UNIVERSAL("Universal", Color(0xFFFF0033)),
    OFFLINE("Offline", Color(0xFFFF9900))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val lazylistState = rememberLazyListState()

    // Specification 1: Dual Mode State (Music <-> Video)
    var isVideoMode by rememberSaveable { mutableStateOf(false) }

    // Specification 2 & 5: Active Capsule selection & Dynamic Aura color
    var activeCapsule by rememberSaveable { mutableStateOf(DefaultCapsule.ALL) }
    val animatedAuraColor by animateColorAsState(
        targetValue = activeCapsule.brandColor,
        animationSpec = tween(durationMillis = 350),
        label = "CapsuleAura"
    )

    // Specification 4: In-Place Capsule Search State
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isInPlaceSearchActive by rememberSaveable { mutableStateOf(false) }

    // Specification 6: Profile Photo Custom Picker with persistent storage
    var customProfileUriStr by rememberPreference("savish_custom_profile_uri", "")
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customProfileUriStr = uri.toString()
        }
    }

    // Specification 7: Video Mode Inline Preview State
    var playingCardId by remember { mutableStateOf<String?>(null) }

    // Scroll-to-Stop preview interaction
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
                                // Specification 7: Inline video silent playback
                                playingCardId = if (playingCardId == item.id) null else item.id
                            } else {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                        is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                        is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                        is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            // Specification 5: Radial / Vertical Background Glow based on selected Capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedAuraColor.copy(alpha = 0.18f),
                                animatedAuraColor.copy(alpha = 0.05f),
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
                // Section 1: Header - Status, Title, Secret Gesture & Profile Action
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
                            // Secret double tap switch: Music <-> Video
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

                            // Profile Circle: Single tap Settings, Long press Gallery
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
                                if (customProfileUriStr.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(customProfileUriStr)
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

                // Section 2: Exact 3 Starting Capsules (All, Universal, Offline)
                item(key = "savish_3_capsules") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DefaultCapsule.values().forEach { capsule ->
                            val isSelected = activeCapsule == capsule
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = if (isSelected) capsule.brandColor.copy(alpha = 0.12f) else Color(0xFF161616),
                                border = if (isSelected) BorderStroke(1.5.dp, capsule.brandColor) else BorderStroke(1.dp, Color(0xFF282828)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .pointerInput(capsule) {
                                        detectTapGestures(
                                            onTap = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LightImpact)
                                                activeCapsule = capsule
                                            },
                                            onDoubleTap = {
                                                if (capsule == DefaultCapsule.UNIVERSAL) {
                                                    // Double tap on Universal opens Extension Hub
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    navController.navigate("settings/spotify_import")
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(capsule.brandColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = capsule.displayName,
                                        color = if (isSelected) Color.White else Color(0xFFAAAAAA),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: In-Place Capsule Search Bar
                item(key = "savish_inplace_search") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, if (isInPlaceSearchActive) animatedAuraColor else Color(0xFF2C2C2C), RoundedCornerShape(25.dp))
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
                                        text = "Search songs in Savish ${activeCapsule.displayName}...",
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

                // Section 4: Welcome Back Subtext
                item(key = "savish_welcome_back") {
                    Text(
                        text = "Welcome back,",
                        color = Color(0xFF9E9E9E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                    )
                }

                // Section 5: Dynamic Home Content
                homePage?.sections?.forEachIndexed { index, sectionData ->
                    item(key = "section_header_$index") {
                        NavigationTitle(
                            title = sectionData.title,
                            label = sectionData.label,
                            onClick = sectionData.endpoint?.let { endpoint ->
                                {
                                    when {
                                        endpoint.browseId == "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                        endpoint.params != null -> navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                                        else -> navController.navigate("browse/${endpoint.browseId}")
                                    }
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "section_items_$index") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.animateItem()
                        ) {
                            items(sectionData.items.distinctBy { it.id }, key = { it.id }) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                // Section 6: Mood and Genres (12 Items in 3-Column Layout)
                explorePage?.moodAndGenres?.let { moodList ->
                    item(key = "mood_genres_title") {
                        NavigationTitle(
                            title = stringResource(R.string.mood_and_genres),
                            onClick = { navController.navigate("mood_and_genres") },
                            modifier = Modifier.animateItem()
                        )
                    }

                    val displayMoods = moodList.take(12).chunked(3)
                    items(displayMoods) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
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

                // Section 7: Trending Community Playlists
                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item(key = "community_playlists_header") {
                        NavigationTitle(
                            title = stringResource(R.string.from_the_community),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "community_playlists_grid") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
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

                if (isLoading && homePage?.sections.isNullOrEmpty()) {
                    item(key = "feed_shimmer") {
                        ShimmerHost(modifier = Modifier.animateItem()) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                repeat(3) {
                                    GridItemPlaceHolder()
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacing") {
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        }
    }
}
