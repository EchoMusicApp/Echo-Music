package iad1tya.echo.music.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.LocalDatabase
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()

    var spotifyUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var importedSongs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var playlistName by remember { mutableStateOf("") }
    var importProgress by remember { mutableIntStateOf(0) }
    var totalTracks by remember { mutableIntStateOf(0) }
    var isImporting by remember { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Spotify Import",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // URL input
            OutlinedTextField(
                value = spotifyUrl,
                onValueChange = { spotifyUrl = it },
                label = { Text("Spotify Playlist URL") },
                placeholder = { Text("https://open.spotify.com/playlist/...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Fetch button
            Button(
                onClick = {
                    if (spotifyUrl.isBlank()) {
                        Toast.makeText(context, "Please enter a Spotify playlist URL", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        statusText = "Fetching playlist..."
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
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isImporting,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Fetch Playlist")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Import help",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "If fetch fails, use a playlist transfer service",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = { showGuide = !showGuide }) {
                            Text(if (showGuide) "Hide" else "Show")
                        }
                    }

                    if (showGuide) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                            Text(
                                text = "Important: Currently supports 100 songs per playlist.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                            )

                            GuideSection(
                                title = "Supported transfer services",
                                items = spotifyTransferServices,
                            )

                            GuideSection(
                                title = "Supported source platforms",
                                items = spotifySourcePlatforms,
                            )

                            Text(
                                text = "Destination must be YouTube Music for Echo Music compatibility.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )

                            GuideSection(
                                title = "Step-by-step",
                                items = spotifyStepByStep,
                                numbered = true,
                            )

                            GuideSection(
                                title = "Important notes",
                                items = spotifyImportantNotes,
                            )

                            Text(
                                text = "Done! Your playlists from Spotify, Apple Music, and other platforms are now available in Echo Music.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Import button
            if (importedSongs.isNotEmpty()) {
                Button(
                    onClick = {
                        scope.launch {
                            isImporting = true
                            importProgress = 0
                            statusText = "Importing to Echo Music..."

                            val foundIds = mutableListOf<String>()
                            val failed = mutableListOf<String>()

                            for ((index, pair) in importedSongs.withIndex()) {
                                val (title, artist) = pair
                                importProgress = index + 1
                                statusText = "Searching: $title ($importProgress/$totalTracks)"

                                val videoId = SpotifyImportHelper.searchYouTubeForSong(title, artist)
                                if (videoId != null) {
                                    foundIds.add(videoId)
                                } else {
                                    failed.add("$title - $artist")
                                }
                            }

                            // Create playlist with found songs
                            if (foundIds.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    // First fetch all song metadata from YouTube
                                    val songMetadataList = foundIds.mapNotNull { songId ->
                                        try {
                                            com.echo.innertube.YouTube.queue(listOf(songId))
                                                .getOrNull()?.firstOrNull()?.let { ytSong ->
                                                    songId to ytSong.toMediaMetadata()
                                                }
                                        } catch (_: Exception) { null }
                                    }
                                    
                                    // Then insert into database in a single transaction
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
                                                    position = idx
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            statusText = "Done! Imported ${foundIds.size}/$totalTracks songs" +
                                    if (failed.isNotEmpty()) ". ${failed.size} tracks not found." else ""
                            isImporting = false

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Playlist \"$playlistName\" created with ${foundIds.size} songs",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Import to Echo Music")
                }

                // Progress
                if (isImporting) {
                    LinearProgressIndicator(
                        progress = { importProgress.toFloat() / totalTracks.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Status
            if (statusText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Song list preview
            if (importedSongs.isNotEmpty()) {
                Text(
                    text = "Tracks ($totalTracks)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(importedSongs) { index, (title, artist) ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSection(
    title: String,
    items: List<String>,
    numbered: Boolean = false,
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
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

private val spotifyTransferServices = listOf(
    "TuneMyMusic - https://www.tunemymusic.com/",
    "Soundiiz - https://soundiiz.com/",
    "FreeYourMusic - https://freeyourmusic.com/",
    "MusConv - https://musconv.com/",
)

private val spotifySourcePlatforms = listOf(
    "Spotify",
    "Apple Music",
    "Amazon Music",
    "Deezer",
    "TIDAL",
    "Pandora",
    "SoundCloud",
    "Napster",
    "YouTube",
    "YouTube Music",
)

private val spotifyStepByStep = listOf(
    "Choose a transfer service and open it.",
    "Sign in to your source platform and allow playlist access.",
    "Choose YouTube Music as destination and sign in with Google.",
    "Use the same YouTube Music account that you use in Echo Music.",
    "Select playlists/albums and review before transfer.",
    "Start transfer and wait for completion.",
    "Open Echo Music and your transferred playlists should appear.",
)

private val spotifyImportantNotes = listOf(
    "Free plans may have transfer limits.",
    "Premium plans usually support larger and faster transfers.",
    "Some songs may be skipped if unavailable on YouTube Music.",
    "Playlist names and order are usually preserved.",
)
