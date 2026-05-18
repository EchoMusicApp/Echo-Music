

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package iad1tya.echo.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.db.entities.PlaylistEntity
import iad1tya.echo.music.db.entities.PlaylistSongMap
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.SpotifyImportHelper
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SpotifyImportScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var spotifyUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var importedSongs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var playlistName by remember { mutableStateOf("") }
    var importProgress by remember { mutableIntStateOf(0) }
    var totalTracks by remember { mutableIntStateOf(0) }
    var isImporting by remember { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(false) }

    fun fetchPlaylist() {
        if (spotifyUrl.isBlank()) {
            Toast.makeText(context, "Please enter a Spotify playlist URL", Toast.LENGTH_SHORT).show()
            return
        }
        keyboard?.hide()
        scope.launch {
            isLoading = true
            statusText = "Fetching playlist…"
            try {
                val (name, songs) = SpotifyImportHelper.getPlaylistSongs(spotifyUrl)
                playlistName = name
                importedSongs = songs
                totalTracks = songs.size
                statusText = if (songs.isEmpty()) {
                    "No songs found. Check the URL and try again."
                } else {
                    "Found $totalTracks tracks in \"$name\""
                }
            } catch (e: Exception) {
                statusText = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = "Spotify Import",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // URL input card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_import),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import from Spotify",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Paste a Spotify playlist link",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = spotifyUrl,
                            onValueChange = { spotifyUrl = it },
                            label = { Text("Spotify Playlist URL") },
                            placeholder = { Text("https://open.spotify.com/playlist/…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { fetchPlaylist() }),
                        )

                        Button(
                            onClick = { fetchPlaylist() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && !isImporting,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Fetch Playlist")
                        }
                    }
                }
            }

            // Status card
            if (statusText.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            // Import button + progress
            if (importedSongs.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isImporting = true
                                    importProgress = 0
                                    statusText = "Importing to Echo Music…"

                                    val foundIds = mutableListOf<String>()
                                    val failed = mutableListOf<String>()

                                    for ((index, pair) in importedSongs.withIndex()) {
                                        val (title, artist) = pair
                                        importProgress = index + 1
                                        statusText = "Searching: $title ($importProgress/$totalTracks)"

                                        val videoId = SpotifyImportHelper.searchYouTubeForSong(title, artist)
                                        if (videoId != null) foundIds.add(videoId)
                                        else failed.add("$title - $artist")
                                    }

                                    if (foundIds.isNotEmpty()) {
                                        withContext(Dispatchers.IO) {
                                            val songMetadataList = foundIds.mapNotNull { songId ->
                                                try {
                                                    iad1tya.echo.music.innertube.YouTube.queue(listOf(songId))
                                                        .getOrNull()?.firstOrNull()?.let { ytSong ->
                                                            songId to ytSong.toMediaMetadata()
                                                        }
                                                } catch (_: Exception) { null }
                                            }
                                            database.query {
                                                val playlist = PlaylistEntity(
                                                    name = playlistName,
                                                    browseId = null,
                                                    bookmarkedAt = LocalDateTime.now(),
                                                    isEditable = true,
                                                )
                                                insert(playlist)
                                                songMetadataList.forEachIndexed { idx, (songId, metadata) ->
                                                    insert(metadata)
                                                    insert(
                                                        PlaylistSongMap(
                                                            songId = songId,
                                                            playlistId = playlist.id,
                                                            position = idx,
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    statusText = "Done! Imported ${foundIds.size}/$totalTracks songs" +
                                            if (failed.isNotEmpty()) ". ${failed.size} not found." else ""
                                    isImporting = false

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Playlist \"$playlistName\" created with ${foundIds.size} songs",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isImporting && !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                            ),
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Import to Echo Music")
                        }

                        if (isImporting) {
                            LinearProgressIndicator(
                                progress = { importProgress.toFloat() / totalTracks.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape),
                            )
                        }
                    }
                }
            }

            // Track list preview
            if (importedSongs.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tracks ($totalTracks)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                importedSongs.forEachIndexed { index, (title, artist) ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 78.dp, end = 20.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        )
                                    }
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = artist,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        },
                                        leadingContent = {
                                            Surface(
                                                modifier = Modifier.size(36.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Help guide card
            item {
                SpotifyHelpCard(showGuide = showGuide, onToggle = { showGuide = !showGuide })
            }
        }
    }
}

@Composable
private fun SpotifyHelpCard(showGuide: Boolean, onToggle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Import Help",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "If fetch fails",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Use a playlist transfer service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onToggle,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(if (showGuide) "Hide" else "Show")
                    }
                }

                AnimatedVisibility(
                    visible = showGuide,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                        Text(
                            text = "⚠ Currently supports up to 100 songs per playlist.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        SpotifyGuideSection(
                            title = "Transfer services",
                            items = listOf(
                                "TuneMyMusic — tunemymusic.com",
                                "Soundiiz — soundiiz.com",
                                "FreeYourMusic — freeyourmusic.com",
                                "MusConv — musconv.com",
                            ),
                        )
                        SpotifyGuideSection(
                            title = "Supported sources",
                            items = listOf(
                                "Spotify", "Apple Music", "Amazon Music",
                                "Deezer", "TIDAL", "Pandora", "SoundCloud", "YouTube Music",
                            ),
                        )
                        Text(
                            text = "Destination must be YouTube Music.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SpotifyGuideSection(
                            title = "Step-by-step",
                            items = listOf(
                                "Choose a transfer service and open it.",
                                "Sign in to your source platform.",
                                "Choose YouTube Music as destination.",
                                "Use the same account as Echo Music.",
                                "Select playlists and start transfer.",
                                "Open Echo Music — playlists will appear.",
                            ),
                            numbered = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyGuideSection(
    title: String,
    items: List<String>,
    numbered: Boolean = false,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            items.forEachIndexed { index, item ->
                Text(
                    text = if (numbered) "${index + 1}. $item" else "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
