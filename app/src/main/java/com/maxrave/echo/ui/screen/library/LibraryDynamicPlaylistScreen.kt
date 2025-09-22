package iad1tya.echo.music.ui.screen.library

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.data.db.entities.ArtistEntity
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.extension.toTrack
import iad1tya.echo.music.ui.component.ArtistFullWidthItems
import iad1tya.echo.music.ui.component.EndOfPage
import iad1tya.echo.music.ui.component.NowPlayingBottomSheet
import iad1tya.echo.music.ui.component.RippleIconButton
import iad1tya.echo.music.ui.component.SongFullWidthItems
import iad1tya.echo.music.ui.navigation.destination.list.ArtistDestination
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.viewModel.LibraryDynamicPlaylistViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
@UnstableApi
@ExperimentalMaterial3Api
fun LibraryDynamicPlaylistScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: String,
    viewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    var chosenSong: SongEntity? by remember { mutableStateOf(null) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val favorite by viewModel.listFavoriteSong.collectAsStateWithLifecycle()
    var tempFavorite by rememberSaveable { mutableStateOf(emptyList<SongEntity>()) }
    val followed by viewModel.listFollowedArtist.collectAsStateWithLifecycle()
    var tempFollowed by rememberSaveable { mutableStateOf(emptyList<ArtistEntity>()) }
    val mostPlayed by viewModel.listMostPlayedSong.collectAsStateWithLifecycle()
    var tempMostPlayed by rememberSaveable { mutableStateOf(emptyList<SongEntity>()) }
    val downloaded by viewModel.listDownloadedSong.collectAsStateWithLifecycle()
    var tempDownloaded by rememberSaveable { mutableStateOf(emptyList<SongEntity>()) }
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    LaunchedEffect(query) {
        Log.w("LibraryDynamicPlaylistScreen", "Check query: $query")
        tempFavorite = favorite.filter { it.title.contains(query, ignoreCase = true) }
        Log.w("LibraryDynamicPlaylistScreen", "Check tempFavorite: $tempFavorite")
        tempFollowed = followed.filter { it.name.contains(query, ignoreCase = true) }
        Log.w("LibraryDynamicPlaylistScreen", "Check tempFollowed: $tempFollowed")
        tempMostPlayed = mostPlayed.filter { it.title.contains(query, ignoreCase = true) }
        Log.w("LibraryDynamicPlaylistScreen", "Check tempMostPlayed: $tempMostPlayed")
        tempDownloaded = downloaded.filter { it.title.contains(query, ignoreCase = true) }
        Log.w("LibraryDynamicPlaylistScreen", "Check tempDownloaded: $tempDownloaded")
    }

    val type = LibraryDynamicPlaylistType.toType(type)
    val followedList = if (type == LibraryDynamicPlaylistType.Followed) {
        if (query.isNotEmpty() && showSearchBar) {
            tempFollowed
        } else {
            followed
        }
    } else emptyList()
    
    val songList = if (type != LibraryDynamicPlaylistType.Followed) {
        when (type) {
            LibraryDynamicPlaylistType.Downloaded ->
                if (query.isNotEmpty() && showSearchBar) {
                    tempDownloaded
                } else {
                    downloaded
                }
            LibraryDynamicPlaylistType.Favorite ->
                if (query.isNotEmpty() && showSearchBar) {
                    tempFavorite
                } else {
                    favorite
                }
            LibraryDynamicPlaylistType.MostPlayed ->
                if (query.isNotEmpty() && showSearchBar) {
                    tempMostPlayed
                } else {
                    mostPlayed
                }
            else -> emptyList()
        }
    } else emptyList()
    
    val isEmpty = if (type == LibraryDynamicPlaylistType.Followed) {
        followedList.isEmpty()
    } else {
        songList.isEmpty()
    }
    
    if (isEmpty) {
        // Show empty state centered on screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) {
            when (type) {
                LibraryDynamicPlaylistType.Followed -> {
                    EmptyStateMessage(
                        title = "No Followed Artists",
                        subtitle = "Start following artists to see them here",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                LibraryDynamicPlaylistType.Downloaded -> {
                    EmptyStateMessage(
                        title = "No Downloaded Songs",
                        subtitle = "Download songs to listen offline",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                LibraryDynamicPlaylistType.Favorite -> {
                    EmptyStateMessage(
                        title = "No Favorite Songs",
                        subtitle = "Add songs to favorites to see them here",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                LibraryDynamicPlaylistType.MostPlayed -> {
                    EmptyStateMessage(
                        title = "No Most Played Songs",
                        subtitle = "Play songs to see your most played tracks",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    } else {
        // Show content in LazyColumn
        LazyColumn(
            modifier = Modifier.hazeSource(hazeState),
            contentPadding = innerPadding,
        ) {
            item {
                Spacer(Modifier.height(64.dp))
            }
            item {
                AnimatedVisibility(showSearchBar) {
                    Spacer(Modifier.height(55.dp))
                }
            }
            
            if (type == LibraryDynamicPlaylistType.Followed) {
                items(
                    followedList,
                    key = { it.channelId },
                ) { artist ->
                    ArtistFullWidthItems(
                        artist,
                        onClickListener = {
                            navController.navigate(
                                ArtistDestination(
                                    channelId = artist.channelId,
                                ),
                            )
                        },
                    )
                }
            } else {
                items(
                    songList,
                    key = { it.hashCode() },
                ) { song ->
                    SongFullWidthItems(
                        songEntity = song,
                        isPlaying = song.videoId == nowPlayingVideoId,
                        modifier = Modifier.fillMaxWidth(),
                        onMoreClickListener = {
                            chosenSong = song
                            showBottomSheet = true
                        },
                        onClickListener = { videoId ->
                            viewModel.playSong(videoId, type = type)
                        },
                        onAddToQueue = {
                            sharedViewModel.addListToQueue(
                                arrayListOf(song.toTrack()),
                            )
                        },
                    )
                }
            }
            item {
                EndOfPage()
            }
        }
    }
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                chosenSong = null
            },
            navController = navController,
            song = chosenSong ?: return,
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            title = {
                Text(
                    text =
                        stringResource(
                            type.name(),
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
            actions = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        if (showSearchBar) R.drawable.baseline_close_24 else R.drawable.baseline_search_24,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        showSearchBar = !showSearchBar
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
        androidx.compose.animation.AnimatedVisibility(visible = showSearchBar) {
            SearchBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(horizontal = 12.dp),
                inputField = {
                    CompositionLocalProvider(LocalTextStyle provides typo.bodySmall) {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { showSearchBar = false },
                            expanded = showSearchBar,
                            onExpandedChange = { showSearchBar = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.search),
                                    style = typo.bodySmall,
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        )
                    }
                },
                expanded = false,
                onExpandedChange = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
            }
        }
    }
}

sealed class LibraryDynamicPlaylistType {
    data object Favorite : LibraryDynamicPlaylistType()

    data object Followed : LibraryDynamicPlaylistType()

    data object MostPlayed : LibraryDynamicPlaylistType()

    data object Downloaded : LibraryDynamicPlaylistType()

    @StringRes fun name(): Int =
        when (this) {
            Favorite -> R.string.favorite
            Followed -> R.string.followed
            MostPlayed -> R.string.most_played
            Downloaded -> R.string.downloads
        }

    // For serialization and navigation
    fun toStringParams(): String =
        when (this) {
            Favorite -> "favorite"
            Followed -> "followed"
            MostPlayed -> "most_played"
            Downloaded -> "downloaded"
        }

    companion object {
        fun toType(input: String): LibraryDynamicPlaylistType =
            when (input) {
                "favorite" -> Favorite
                "followed" -> Followed
                "most_played" -> MostPlayed
                "downloaded" -> Downloaded
                else -> throw IllegalArgumentException("Unknown type: $this")
            }
    }
}

@Composable
fun EmptyStateMessage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = typo.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = typo.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}