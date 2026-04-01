package iad1tya.echo.music.ui.menu

import android.content.Intent
import android.content.res.Configuration
import android.media.audiofx.AudioEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.echo.innertube.YouTube
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalDownloadUtil
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.LocalSyncUtils
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.CrossfadeEnabledKey
import iad1tya.echo.music.constants.ListItemHeight
import iad1tya.echo.music.constants.ListThumbnailSize
import iad1tya.echo.music.db.entities.ArtistEntity
import iad1tya.echo.music.db.entities.Event
import iad1tya.echo.music.db.entities.PlaylistSong
import iad1tya.echo.music.db.entities.Song
import iad1tya.echo.music.db.entities.SongArtistMap
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.extensions.toMediaItem
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.ExoDownloadService
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.ui.component.ListDialog
import iad1tya.echo.music.ui.component.AdvancedDownloadDialog
import iad1tya.echo.music.ui.component.ShareChooserSheet
import iad1tya.echo.music.ui.component.LocalBottomSheetPageState
import iad1tya.echo.music.ui.component.NewAction
import iad1tya.echo.music.ui.component.NewActionGrid
import iad1tya.echo.music.ui.component.NewMenuSectionHeader
import iad1tya.echo.music.ui.component.RingtoneTrimDialog
import iad1tya.echo.music.ui.component.SongListItem
import iad1tya.echo.music.ui.component.TextFieldDialog
import iad1tya.echo.music.ui.utils.ShowMediaInfo
import iad1tya.echo.music.viewmodels.CachePlaylistViewModel
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current.getDownload(originalSong.id)
        .collectAsState(initial = null)
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 450),
        label = "",
    )

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted = artistMaps.mapNotNull { map ->
                song.artists.firstOrNull { it.id == map.artistId }
            }
            value = sorted
        }
    }

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showAdvancedDownloadDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        CrossfadeEnabledKey,
        defaultValue = false,
    )

    val TextFieldValueSaver: Saver<TextFieldValue, *> = Saver(
        save = { it.text },
        restore = { text -> TextFieldValue(text, TextRange(text.length)) }
    )

    var titleField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.song.title))
    }

    var artistField by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(song.artists.firstOrNull()?.name.orEmpty()))
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null
                )
            },
            title = {
                Text(text = stringResource(R.string.edit_song))
            },
            textFields = listOf(
                stringResource(R.string.song_title) to titleField,
                stringResource(R.string.artist_name) to artistField
            ),
            onTextFieldsChange = { index, newValue ->
                if (index == 0) titleField = newValue
                else artistField = newValue
            },
            onDoneMultiple = { values ->
                val newTitle = values[0]
                val newArtist = values[1]

                coroutineScope.launch {
                    database.query {
                        update(song.song.copy(title = newTitle))
                        val artist = song.artists.firstOrNull()
                        if (artist != null) {
                            update(artist.copy(name = newArtist))
                        }
                    }

                    showEditDialog = false
                    onDismiss()
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showAdvancedDownloadDialog) {
        AdvancedDownloadDialog(
            mediaMetadata = song.toMediaMetadata(),
            onDismiss = { showAdvancedDownloadDialog = false },
        )
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(onDismiss = { showPitchTempoDialog = false })
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showShareSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(listOf(song)) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showRingtoneTrimDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showRingtoneTrimDialog) {
        RingtoneTrimDialog(
            songId        = song.id,
            title         = song.song.title,
            artist        = song.artists.firstOrNull()?.name ?: "",
            duration      = song.song.duration,
            downloadCache = downloadUtil.downloadCache,
            playerCache   = downloadUtil.playerCache,
            onDismiss     = {
                showRingtoneTrimDialog = false
                onDismiss()
            },
        )
    }

    if (showShareSheet) {
        ShareChooserSheet(
            ytmUrl = "https://music.youtube.com/watch?v=${song.id}",
            onDismiss = { showShareSheet = false },
        )
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            item {
                Text(
                    text = "Select Artist",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                )
            }
            
            items(
                items = song.artists.distinctBy { it.id },
                key = { it.id },
            ) { artist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(12.dp)
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        )
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Icon(
                            painter = painterResource(R.drawable.navigate_next),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    SongListItem(
        song = song,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    val s = song.song.toggleLike()
                    database.query {
                        update(s)
                    }
                    syncUtils.likeSong(s)
                },
            ) {
                Icon(
                    painter = painterResource(if (song.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val bottomSheetPageState = LocalBottomSheetPageState.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        userScrollEnabled = true,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewMenuSectionHeader(text = "Downloads & Queue")
        }
        item {
            NewActionGrid(
                actions = buildList {
                    if (!song.song.isLocal) {
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                text = stringResource(R.string.action_download),
                                onClick = {
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.song.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                },
                            )
                        )
                    }
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.playlist_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.add_to_playlist),
                            onClick = { showChoosePlaylistDialog = true },
                        )
                    )
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.audio_device),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = "Audio Output",
                            onClick = {
                                onDismiss()
                                if (playerConnection != null) {
                                    playerConnection.forceAudioToSpeaker(context)
                                }
                            },
                        )
                    )
                },
                columns = 3,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        item {
            NewMenuSectionHeader(text = "Playback & Tools")
        }
        item {
            NewActionGrid(
                actions = buildList {
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.start_radio),
                            onClick = {
                                onDismiss()
                                playerConnection.startRadioSeamlessly()
                            },
                        )
                    )
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.waves),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = if (crossfadeEnabled) "Disable crossfade" else "Enable crossfade",
                            onClick = {
                                onCrossfadeEnabledChange(!crossfadeEnabled)
                            },
                        )
                    )
                    if (!song.song.isLocal) {
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                text = "Local Download",
                                onClick = {
                                    showAdvancedDownloadDialog = true
                                },
                            )
                        )
                    }
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.notification),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = "Set ringtone",
                            onClick = { showRingtoneTrimDialog = true },
                        )
                    )
                },
                columns = 4,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        item {
            NewMenuSectionHeader(text = "Equalizer & Advanced")
        }
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.equalizer),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = stringResource(R.string.equalizer),
                        onClick = {
                            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playerConnection.player.audioSessionId)
                                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                            onDismiss()
                        },
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.speed),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = stringResource(R.string.advanced),
                        onClick = {
                            showPitchTempoDialog = true
                        },
                    ),
                ),
                columns = 2,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        item {
            NewMenuSectionHeader(text = "Listen Together")
        }
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.group_outlined),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = "Listen Together",
                        onClick = {
                            onDismiss()
                            navController.navigate("listen_together")
                        },
                    ),
                ),
                columns = 1,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        item {
            NewMenuSectionHeader(text = "More")
        }
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = stringResource(R.string.edit),
                        onClick = { showEditDialog = true },
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            showShareSheet = true
                        },
                    ),
                ),
                columns = 2,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        item {
            NewMenuSectionHeader(text = "Library")
        }
        item {
            NewActionGrid(
                actions = buildList {
                    if (song.song.albumId != null) {
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                text = stringResource(R.string.view_album),
                                onClick = {
                                    onDismiss()
                                    navController.navigate("album/${song.song.albumId}")
                                },
                            )
                        )
                    }
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.artist),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.view_artist),
                            onClick = {
                                if (song.artists.size == 1) {
                                    navController.navigate("artist/${song.artists[0].id}")
                                    onDismiss()
                                } else {
                                    showSelectArtistDialog = true
                                }
                            },
                        )
                    )
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.details),
                            onClick = {
                                onDismiss()
                                bottomSheetPageState.show {
                                    ShowMediaInfo(song.id)
                                }
                            },
                        )
                    )
                },
                columns = 3,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }

        if (!song.song.isLocal) {
            item {
                when (download?.state) {
                    Download.STATE_COMPLETED -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.remove_download),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.offline),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        )
                        }
                    }
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.downloading)) },
                            leadingContent = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            },
                            modifier = Modifier.clickable {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        )
                        }
                    }
                    else -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.action_download)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                val downloadRequest =
                                    DownloadRequest
                                        .Builder(song.id, song.id.toUri())
                                        .setCustomCacheKey(song.id)
                                        .setData(song.song.title.toByteArray())
                                        .build()
                                DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false,
                                )
                            }
                        )
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play_next)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
                }
            )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.addToQueue(song.toMediaItem())
                }
            )
            }
        }
        if (!song.song.isLocal) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(
                                if (song.song.inLibrary == null) R.string.add_to_library
                                else R.string.remove_from_library
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                if (song.song.inLibrary == null) R.drawable.library_add
                                else R.drawable.library_add_check
                            ),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        val currentSong = song.song
                        val isInLibrary = currentSong.inLibrary != null
                        val token = if (isInLibrary) currentSong.libraryRemoveToken else currentSong.libraryAddToken

                        token?.let { 
                            coroutineScope.launch {
                                YouTube.feedback(listOf(it))
                            }
                        }

                        database.query {
                            update(song.song.toggleLibrary())
                        }
                    }
                )
                }
            }
        }
        if (event != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.remove_from_history)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        database.query {
                            delete(event)
                        }
                    }
                )
                }
            }
        }
        if (playlistSong != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.remove_from_playlist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        database.transaction {
                            coroutineScope.launch {
                                playlistBrowseId?.let { playlistId ->
                                    if (playlistSong.map.setVideoId != null) {
                                        YouTube.removeFromPlaylist(
                                            playlistId, playlistSong.map.songId, playlistSong.map.setVideoId
                                        )
                                    }
                                }
                            }
                            move(playlistSong.map.playlistId, playlistSong.map.position, Int.MAX_VALUE)
                            delete(playlistSong.map.copy(position = Int.MAX_VALUE))
                        }
                        onDismiss()
                    }
                )
                }
            }
        }
        if (isFromCache) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.remove_from_cache)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        cacheViewModel.removeSongFromCache(song.id)
                    }
                )
                }
            }
        }
        item {
            if (!song.song.isLocal) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.refetch)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                            )
                        },
                        modifier = Modifier.clickable {
                            refetchIconDegree -= 360
                            scope.launch(Dispatchers.IO) {
                                YouTube.queue(listOf(song.id)).onSuccess {
                                    val newSong = it.firstOrNull()
                                    if (newSong != null) {
                                        database.transaction {
                                            update(song, newSong.toMediaMetadata())
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
