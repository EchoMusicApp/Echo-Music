package iad1tya.echo.music.ui.component

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import iad1tya.echo.music.R
import iad1tya.echo.music.common.Config
import iad1tya.echo.music.data.db.entities.AlbumEntity
import iad1tya.echo.music.data.db.entities.ArtistEntity
import iad1tya.echo.music.data.db.entities.LocalPlaylistEntity
import iad1tya.echo.music.data.db.entities.PlaylistEntity
import iad1tya.echo.music.data.db.entities.PodcastsEntity
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.data.model.browse.album.Track
import iad1tya.echo.music.data.model.searchResult.playlists.PlaylistsResult
import iad1tya.echo.music.data.type.LibraryType
import iad1tya.echo.music.data.type.PlaylistType
import iad1tya.echo.music.data.type.RecentlyType
import iad1tya.echo.music.extension.connectArtists
import iad1tya.echo.music.extension.toTrack
import iad1tya.echo.music.service.QueueData
import iad1tya.echo.music.ui.navigation.destination.list.AlbumDestination
import iad1tya.echo.music.ui.navigation.destination.list.ArtistDestination
import iad1tya.echo.music.ui.navigation.destination.list.LocalPlaylistDestination
import iad1tya.echo.music.ui.navigation.destination.list.PlaylistDestination
import iad1tya.echo.music.ui.navigation.destination.list.PodcastDestination
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.viewModel.LibraryViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
fun LibraryItem(
    state: LibraryItemState,
    viewModel: LibraryViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
) {
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var songEntity by remember { mutableStateOf<SongEntity?>(null) }
    
    // Playlist long press options
    var showPlaylistOptionsSheet by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistType?>(null) }
    val title =
        when (state.type) {
            is LibraryItemType.YouTubePlaylist -> stringResource(R.string.your_youtube_playlists)
            is LibraryItemType.LocalPlaylist -> stringResource(R.string.your_playlists)
            is LibraryItemType.FavoritePlaylist -> stringResource(R.string.favorite_playlists)
            is LibraryItemType.DownloadedPlaylist -> stringResource(R.string.downloaded_playlists)
            is LibraryItemType.RecentlyAdded -> stringResource(R.string.recently_added)
            is LibraryItemType.CanvasSong -> stringResource(R.string.most_played)
            is LibraryItemType.FavoritePodcasts -> stringResource(R.string.favorite_podcasts)
            is LibraryItemType.DownloadedSongs -> stringResource(R.string.downloaded_songs)
        }
    val noPlaylistTitle =
        when (state.type) {
            is LibraryItemType.YouTubePlaylist -> stringResource(R.string.no_YouTube_playlists)
            is LibraryItemType.LocalPlaylist -> stringResource(R.string.no_playlists_added)
            LibraryItemType.DownloadedPlaylist -> stringResource(R.string.no_playlists_downloaded)
            LibraryItemType.FavoritePlaylist -> stringResource(R.string.no_favorite_playlists)
            is LibraryItemType.RecentlyAdded -> stringResource(R.string.recently_added)
            is LibraryItemType.CanvasSong -> stringResource(R.string.most_played)
            is LibraryItemType.FavoritePodcasts -> stringResource(R.string.no_favorite_podcasts)
            is LibraryItemType.DownloadedSongs -> stringResource(R.string.no_songs_downloaded)
        }
    Box {
        if (showBottomSheet) {
            NowPlayingBottomSheet(
                onDismiss = {
                    showBottomSheet = false
                    songEntity = null
                },
                navController = navController,
                song = songEntity ?: return,
                onLibraryDelete = {
                    songEntity?.videoId?.let { viewModel.deleteSong(it) }
                },
            )
        }
        
        // Playlist Options Bottom Sheet
        if (showPlaylistOptionsSheet && selectedPlaylist != null) {
            val isLocalPlaylist = selectedPlaylist is LocalPlaylistEntity
            val isYouTubePlaylist = selectedPlaylist is PlaylistsResult || selectedPlaylist is PlaylistEntity
            val isSynced = if (isLocalPlaylist) {
                (selectedPlaylist as LocalPlaylistEntity).youtubePlaylistId != null
            } else false
            
            PlaylistOptionsBottomSheet(
                playlist = selectedPlaylist!!,
                onDismiss = {
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                onPlay = {
                    // Play the playlist directly
                    when (selectedPlaylist) {
                        is LocalPlaylistEntity -> {
                            val playlist = selectedPlaylist as LocalPlaylistEntity
                            viewModel.playLocalPlaylist(playlist.id, playlist.title)
                        }
                        is PlaylistsResult -> {
                            val playlist = selectedPlaylist as PlaylistsResult
                            viewModel.playYouTubePlaylist(playlist.browseId, playlist.title)
                        }
                        is PlaylistEntity -> {
                            val playlist = selectedPlaylist as PlaylistEntity
                            viewModel.playYouTubePlaylist(playlist.id, playlist.title)
                        }
                    }
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                onShuffle = {
                    // Shuffle play playlist
                    Toast.makeText(context, "Shuffling playlist...", Toast.LENGTH_SHORT).show()
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                onAddToQueue = {
                    // Add playlist to queue functionality
                    Toast.makeText(context, "Adding to queue...", Toast.LENGTH_SHORT).show()
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                onDownload = {
                    // Download playlist
                    Toast.makeText(context, "Downloading playlist...", Toast.LENGTH_SHORT).show()
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                onShare = {
                    // Share playlist
                    when (selectedPlaylist) {
                        is LocalPlaylistEntity -> {
                            val ytPlaylistId = (selectedPlaylist as LocalPlaylistEntity).youtubePlaylistId
                            if (ytPlaylistId != null) {
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${ytPlaylistId.replaceFirst("VL", "")}")
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Playlist"))
                            } else {
                                Toast.makeText(context, "Sync playlist to YouTube first to share", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is PlaylistsResult -> {
                            val playlistId = (selectedPlaylist as PlaylistsResult).browseId
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${playlistId.replaceFirst("VL", "")}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Playlist"))
                        }
                        is PlaylistEntity -> {
                            val playlistId = (selectedPlaylist as PlaylistEntity).id
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${playlistId.replaceFirst("VL", "")}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Playlist"))
                        }
                    }
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                onRename = {
                    if (selectedPlaylist is LocalPlaylistEntity) {
                        Toast.makeText(context, "Open playlist to rename", Toast.LENGTH_SHORT).show()
                        showPlaylistOptionsSheet = false
                    }
                },
                onEditThumbnail = {
                    if (selectedPlaylist is LocalPlaylistEntity) {
                        Toast.makeText(context, "Open playlist to edit thumbnail", Toast.LENGTH_SHORT).show()
                        showPlaylistOptionsSheet = false
                    }
                },
                onSync = {
                    if (selectedPlaylist is LocalPlaylistEntity) {
                        Toast.makeText(context, "Open playlist to sync with YouTube", Toast.LENGTH_SHORT).show()
                        showPlaylistOptionsSheet = false
                    }
                },
                onUpdate = {
                    if (selectedPlaylist is LocalPlaylistEntity) {
                        Toast.makeText(context, "Open playlist to update from YouTube", Toast.LENGTH_SHORT).show()
                        showPlaylistOptionsSheet = false
                    }
                },
                onSaveToLocal = {
                    if (selectedPlaylist is PlaylistsResult || selectedPlaylist is PlaylistEntity) {
                        Toast.makeText(context, "Open playlist to save to local", Toast.LENGTH_SHORT).show()
                        showPlaylistOptionsSheet = false
                    }
                },
                onDelete = {
                    if (selectedPlaylist is LocalPlaylistEntity) {
                        val playlist = selectedPlaylist as LocalPlaylistEntity
                        // Delete directly with confirmation
                        viewModel.deleteLocalPlaylist(
                            playlistId = playlist.id,
                            playlistTitle = playlist.title,
                            onSuccess = {
                                // Playlist deleted successfully
                            }
                        )
                    }
                    showPlaylistOptionsSheet = false
                    selectedPlaylist = null
                },
                isLocalPlaylist = isLocalPlaylist,
                isYouTubePlaylist = isYouTubePlaylist,
                isSynced = isSynced,
            )
        }
        
        Column {
            Row(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = typo.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                if (state.type is LibraryItemType.YouTubePlaylist) {
                    TextButton(
                        modifier =
                            Modifier
                                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                        onClick = {
                            (state.type as LibraryItemType.YouTubePlaylist).onReload.invoke()
                        },
                    ) {
                        Text(stringResource(R.string.reload))
                    }
                }
            }
            Crossfade(targetState = state.type is LibraryItemType.YouTubePlaylist && !state.type.isLoggedIn) { notLoggedIn ->
                if (notLoggedIn) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.log_in_to_get_YouTube_playlist), style = typo.bodyMedium)
                    }
                } else {
                    Crossfade(targetState = state.isLoading, label = "Loading") { isLoading ->
                        if (!isLoading) {
                            if (state.type is LibraryItemType.RecentlyAdded) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    state.data.filterIsInstance<RecentlyType>().forEach { item ->
                                        when (item.objectType()) {
                                            RecentlyType.Type.SONG -> {
                                                SongFullWidthItems(
                                                    songEntity = item as SongEntity,
                                                    isPlaying = item.videoId == state.type.playingVideoId,
                                                    modifier = Modifier,
                                                    onMoreClickListener = {
                                                        songEntity = item
                                                        showBottomSheet = true
                                                    },
                                                    onClickListener = {
                                                        viewModel.setQueueData(
                                                            QueueData(
                                                                listTracks = arrayListOf(item.toTrack()),
                                                                firstPlayedTrack = item.toTrack(),
                                                                playlistId = "RDAMVM${item.videoId}",
                                                                playlistName = item.title,
                                                                playlistType = iad1tya.echo.music.service.PlaylistType.RADIO,
                                                                continuation = null,
                                                            ),
                                                        )
                                                        viewModel.loadMediaItem(
                                                            item,
                                                            type = Config.SONG_CLICK,
                                                            index = 0,
                                                        )
                                                    },
                                                    onAddToQueue = { videoId ->
                                                        sharedViewModel.addListToQueue(
                                                            arrayListOf(item.toTrack()),
                                                        )
                                                    },
                                                )
                                            }
                                            RecentlyType.Type.ARTIST -> {
                                                ArtistFullWidthItems(
                                                    data = item as? ArtistEntity ?: return@forEach,
                                                    onClickListener = {
                                                        navController.navigate(
                                                            ArtistDestination(
                                                                channelId = item.channelId,
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                            else -> {
                                                if (item is PlaylistType) {
                                                    PlaylistFullWidthItems(
                                                        data = item,
                                                        onClickListener = {
                                                            when (item) {
                                                                is AlbumEntity -> {
                                                                    navController.navigate(
                                                                        AlbumDestination(
                                                                            item.browseId,
                                                                        ),
                                                                    )
                                                                }
                                                                is PlaylistEntity -> {
                                                                    navController.navigate(
                                                                        PlaylistDestination(
                                                                            item.id,
                                                                        ),
                                                                    )
                                                                }
                                                                is PodcastsEntity -> {
                                                                    navController.navigate(
                                                                        PodcastDestination(
                                                                            podcastId = item.podcastId,
                                                                        ),
                                                                    )
                                                                }
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (state.type is LibraryItemType.CanvasSong) {
                                LazyRow(
                                    Modifier.padding(
                                        top = 10.dp,
                                    ),
                                ) {
                                    items(state.data) { item ->
                                        val song = item as? SongEntity ?: return@items
                                        Box(
                                            Modifier
                                                .padding(horizontal = 10.dp)
                                                .height(300.dp)
                                                .width(170.dp)
                                                .clickable {
                                                    val firstQueue: Track = song.toTrack()
                                                    viewModel.setQueueData(
                                                        QueueData(
                                                            listTracks = arrayListOf(firstQueue),
                                                            firstPlayedTrack = firstQueue,
                                                            playlistId = "RDAMVM${firstQueue.videoId}",
                                                            playlistName = "\"${song.title}\" ${context.getString(R.string.radio)}",
                                                            playlistType = iad1tya.echo.music.service.PlaylistType.RADIO,
                                                            continuation = null,
                                                        ),
                                                    )
                                                    viewModel.loadMediaItem(
                                                        firstQueue,
                                                        type = Config.SONG_CLICK,
                                                    )
                                                },
                                        ) {
                                            AsyncImage(
                                                model =
                                                    ImageRequest
                                                        .Builder(LocalContext.current)
                                                        .data(item.canvasThumbUrl)
                                                        .diskCachePolicy(CachePolicy.ENABLED)
                                                        .diskCacheKey(item.canvasThumbUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                placeholder = painterResource(R.drawable.echo_nobg),
                                                error = painterResource(R.drawable.echo_nobg),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .clip(
                                                            RoundedCornerShape(8.dp),
                                                        ),
                                            )
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp)
                                                    .align(Alignment.BottomStart),
                                            ) {
                                                Text(
                                                    text = song.title,
                                                    style = typo.labelSmall,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .wrapContentHeight(
                                                                align = Alignment.CenterVertically,
                                                            ).basicMarquee(
                                                                iterations = Int.MAX_VALUE,
                                                                animationMode = MarqueeAnimationMode.Immediately,
                                                            ).focusable(),
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.animation.AnimatedVisibility(visible = song.isExplicit) {
                                                        ExplicitBadge(
                                                            modifier =
                                                                Modifier
                                                                    .size(20.dp)
                                                                    .padding(end = 4.dp)
                                                                    .weight(1f),
                                                        )
                                                    }
                                                    Text(
                                                        text = (song.artistName?.connectArtists() ?: ""),
                                                        style = typo.bodySmall,
                                                        maxLines = 1,
                                                        modifier =
                                                            Modifier
                                                                .weight(1f)
                                                                .wrapContentHeight(
                                                                    align = Alignment.CenterVertically,
                                                                ).basicMarquee(
                                                                    iterations = Int.MAX_VALUE,
                                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                                ).focusable(),
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            } else if (state.type is LibraryItemType.DownloadedSongs) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    state.data.filterIsInstance<SongEntity>().forEach { item ->
                                        SongFullWidthItems(
                                            songEntity = item,
                                            isPlaying = item.videoId == state.type.playingVideoId,
                                            modifier = Modifier,
                                            onMoreClickListener = {
                                                songEntity = item
                                                showBottomSheet = true
                                            },
                                            onClickListener = {
                                                viewModel.setQueueData(
                                                    QueueData(
                                                        listTracks = arrayListOf(item.toTrack()),
                                                        firstPlayedTrack = item.toTrack(),
                                                        playlistId = "RDAMVM${item.videoId}",
                                                        playlistName = item.title,
                                                        playlistType = iad1tya.echo.music.service.PlaylistType.RADIO,
                                                        continuation = null,
                                                    ),
                                                )
                                                viewModel.loadMediaItem(
                                                    item,
                                                    type = Config.SONG_CLICK,
                                                    index = 0,
                                                )
                                            },
                                            onAddToQueue = { videoId ->
                                                sharedViewModel.addListToQueue(
                                                    arrayListOf(item.toTrack()),
                                                )
                                            },
                                        )
                                    }
                                }
                            } else {
                                if (state.data.isNotEmpty()) {
                                    LazyRow {
                                        items(items = state.data) { item ->
                                            Box(modifier = Modifier.animateItem()) {
                                                HomeItemContentPlaylist(
                                                    onClick = {
                                                        when (item) {
                                                            is LocalPlaylistEntity -> {
                                                                navController.navigate(
                                                                    LocalPlaylistDestination(
                                                                        item.id,
                                                                    ),
                                                                )
                                                            }
                                                            is PlaylistsResult -> {
                                                                navController.navigate(
                                                                    PlaylistDestination(
                                                                        item.browseId,
                                                                        isYourYouTubePlaylist = true,
                                                                    ),
                                                                )
                                                            }
                                                            is AlbumEntity -> {
                                                                navController.navigate(
                                                                    AlbumDestination(
                                                                        item.browseId,
                                                                    ),
                                                                )
                                                            }
                                                            is PlaylistEntity -> {
                                                                navController.navigate(
                                                                    PlaylistDestination(
                                                                        item.id,
                                                                    ),
                                                                )
                                                            }
                                                            is PodcastsEntity -> {
                                                                navController.navigate(
                                                                    PodcastDestination(
                                                                        podcastId = item.podcastId,
                                                                    ),
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onLongClick = {
                                                        selectedPlaylist = item as? PlaylistType
                                                        showPlaylistOptionsSheet = true
                                                    },
                                                    data = item as? PlaylistType ?: return@items,
                                                    thumbSize = 125.dp,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(130.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(noPlaylistTitle, style = typo.bodyMedium)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf(title) }
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
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
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
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(id = R.string.playlist_name))
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
                                Toast.makeText(context, context.getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                            } else {
                                (state.type as? LibraryItemType.LocalPlaylist)?.onAddClick?.invoke(newTitle)
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(id = R.string.create))
                    }
                }
            }
        }
    }
}

sealed class LibraryItemType {
    data object CanvasSong : LibraryItemType()

    data class YouTubePlaylist(
        val isLoggedIn: Boolean,
        val onReload: () -> Unit = {},
    ) : LibraryItemType()

    data class LocalPlaylist(
        // Create new local playlist
        val onAddClick: (String) -> Unit,
    ) : LibraryItemType()

    data object FavoritePlaylist : LibraryItemType()

    data object DownloadedPlaylist : LibraryItemType()

    data object FavoritePodcasts : LibraryItemType()

    data class RecentlyAdded(
        val playingVideoId: String,
    ) : LibraryItemType()

    data class DownloadedSongs(
        val playingVideoId: String,
    ) : LibraryItemType()
}

data class LibraryItemState(
    val type: LibraryItemType,
    val data: List<LibraryType>,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistOptionsBottomSheet(
    playlist: PlaylistType,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onEditThumbnail: () -> Unit,
    onSync: () -> Unit,
    onUpdate: () -> Unit,
    onSaveToLocal: () -> Unit,
    onDelete: () -> Unit,
    isLocalPlaylist: Boolean,
    isYouTubePlaylist: Boolean,
    isSynced: Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Playlist Title
                Text(
                    text = when (playlist) {
                        is LocalPlaylistEntity -> playlist.title
                        is PlaylistsResult -> playlist.title
                        is PlaylistEntity -> playlist.title
                        is AlbumEntity -> playlist.title
                        is PodcastsEntity -> playlist.title
                        else -> "Playlist"
                    },
                    style = typo.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Play Option
                PlaylistOptionItem(
                    icon = Icons.Filled.PlayCircle,
                    text = "Play",
                    onClick = onPlay
                )
                
                // Shuffle Option
                PlaylistOptionItem(
                    icon = Icons.Filled.Shuffle,
                    text = stringResource(R.string.shuffle),
                    onClick = onShuffle
                )
                
                // Add to Queue Option
                PlaylistOptionItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = stringResource(R.string.add_to_queue),
                    onClick = onAddToQueue
                )
                
                // Download Option
                PlaylistOptionItem(
                    icon = Icons.Filled.DownloadForOffline,
                    text = stringResource(R.string.download),
                    onClick = onDownload
                )
                
                // Share Option
                PlaylistOptionItem(
                    icon = Icons.Filled.Share,
                    text = stringResource(R.string.share),
                    onClick = onShare
                )
                
                // Save to Local Playlist (only for YouTube playlists)
                if (isYouTubePlaylist) {
                    PlaylistOptionItem(
                        icon = Icons.Filled.Save,
                        text = stringResource(R.string.save_to_local_playlist),
                        onClick = onSaveToLocal
                    )
                }
                
                // Rename Option (only for local playlists)
                if (isLocalPlaylist) {
                    PlaylistOptionItem(
                        icon = Icons.Filled.Edit,
                        text = "Rename",
                        onClick = onRename
                    )
                }
                
                // Edit Thumbnail Option (only for local playlists)
                if (isLocalPlaylist) {
                    PlaylistOptionItem(
                        icon = Icons.Filled.Image,
                        text = stringResource(R.string.edit_thumbnail),
                        onClick = onEditThumbnail
                    )
                }
                
                // Sync Option (only for local playlists)
                if (isLocalPlaylist) {
                    PlaylistOptionItem(
                        icon = Icons.Filled.Sync,
                        text = if (isSynced) stringResource(R.string.synced) else stringResource(R.string.sync),
                        onClick = onSync
                    )
                }
                
                // Update Playlist Option (only for synced local playlists)
                if (isLocalPlaylist && isSynced) {
                    PlaylistOptionItem(
                        icon = Icons.Filled.Update,
                        text = stringResource(R.string.update_playlist),
                        onClick = onUpdate
                    )
                }
                
                // Delete Option (only for local playlists)
                if (isLocalPlaylist) {
                    PlaylistOptionItem(
                        icon = Icons.Filled.Delete,
                        text = stringResource(R.string.delete),
                        onClick = onDelete,
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isDestructive) Color.Red else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = typo.bodyLarge,
            color = if (isDestructive) Color.Red else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
    }
}