package iad1tya.echo.music.ui.screen.library

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.media3.common.MediaItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.ui.theme.typo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.EndOfPage
import iad1tya.echo.music.ui.component.LibraryItem
import iad1tya.echo.music.ui.component.LibraryItemState
import iad1tya.echo.music.ui.component.LibraryItemType
import iad1tya.echo.music.ui.component.LibraryTilingBox
import iad1tya.echo.music.ui.component.LibrarySectionHeader
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.utils.LocalResource
import iad1tya.echo.music.viewModel.LibraryViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@UnstableApi
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    // Get mini-player state to calculate proper bottom padding
    val nowPlayingData by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val isMiniPlayerActive = nowPlayingData?.mediaItem != null && nowPlayingData?.mediaItem != MediaItem.EMPTY
    
    // Calculate dynamic bottom padding: 56dp for bottom nav + 60dp for mini-player when active
    val bottomPadding = if (isMiniPlayerActive) 116.dp else 56.dp
    
    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val listCanvasSong by viewModel.listCanvasSong.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    LaunchedEffect(true) {
        Log.w("LibraryScreen", "Check youtubePlaylist: ${youTubePlaylist.data}")
        if (youTubePlaylist.data.isNullOrEmpty()) {
            viewModel.getYouTubePlaylist()
        }
        viewModel.getCanvasSong()
        viewModel.getLocalPlaylist()
        viewModel.getPlaylistFavorite()
        viewModel.getDownloadedPlaylist()
        viewModel.getDownloadedSongs()
        viewModel.getFavoritePodcasts()
    }
    LaunchedEffect(nowPlaying) {
        Log.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
    }

    LazyColumn(
        contentPadding = innerPadding,
        modifier = Modifier.hazeSource(hazeState),
    ) {
        item {
            Spacer(Modifier.height(64.dp))
        }
        
        // Quick Access Section
        item {
            LibrarySectionHeader(
                title = stringResource(R.string.quick_access),
                subtitle = ""
            )
        }
        item {
            LibraryTilingBox(navController)
        }
        
        // Create Playlist Button
        item {
            CreatePlaylistButton(
                onClick = {
                    // Show dialog for playlist creation
                    playlistName = ""
                    showCreatePlaylistDialog = true
                }
            )
        }
        
        // Recently Played Section
        item {
            AnimatedVisibility(!listCanvasSong.data.isNullOrEmpty()) {
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
        
        // YouTube Playlists Section - Only show if user is logged in and has YouTube playlists
        val hasYouTubePlaylists = loggedIn && !youTubePlaylist.data.isNullOrEmpty()
        if (hasYouTubePlaylists) {
            item {
                LibraryItem(
                    state =
                        LibraryItemState(
                            type =
                                LibraryItemType.YouTubePlaylist(loggedIn) {
                                    viewModel.getYouTubePlaylist()
                                },
                            data = youTubePlaylist.data ?: emptyList(),
                            isLoading = youTubePlaylist is LocalResource.Loading,
                        ),
                    navController = navController,
                )
            }
        }
        
        // Local Playlists Section - Only show if user has local playlists
        val hasLocalPlaylists = !yourLocalPlaylist.data.isNullOrEmpty()
        if (hasLocalPlaylists) {
            item {
                LibraryItem(
                    state =
                        LibraryItemState(
                            type =
                                LibraryItemType.LocalPlaylist { newTitle ->
                                    viewModel.createPlaylist(newTitle)
                                },
                            data = yourLocalPlaylist.data ?: emptyList(),
                            isLoading = yourLocalPlaylist is LocalResource.Loading,
                        ),
                    navController = navController,
                )
            }
        }
        
        // Favorite Playlists Section - Only show if user has favorite playlists
        val hasFavoritePlaylists = !favoritePlaylist.data.isNullOrEmpty()
        if (hasFavoritePlaylists) {
            item {
                LibraryItem(
                    state =
                        LibraryItemState(
                            type = LibraryItemType.FavoritePlaylist,
                            data = favoritePlaylist.data ?: emptyList(),
                            isLoading = favoritePlaylist is LocalResource.Loading,
                        ),
                    navController = navController,
                )
            }
        }
        
        // Downloads Section - Only show if user has downloaded content
        val hasDownloadedSongs = !downloadedSongs.data.isNullOrEmpty()
        val hasDownloadedPlaylists = !downloadedPlaylist.data.isNullOrEmpty()
        val hasAnyDownloads = hasDownloadedSongs || hasDownloadedPlaylists
        
        if (hasAnyDownloads) {
            item {
                LibraryItem(
                    state =
                        LibraryItemState(
                            type = LibraryItemType.DownloadedSongs(nowPlaying),
                            data = downloadedSongs.data ?: emptyList(),
                            isLoading = downloadedSongs is LocalResource.Loading,
                        ),
                    navController = navController,
                )
            }
        }
        
        
        item {
            EndOfPage()
        }
        // Add dynamic bottom padding to prevent content from scrolling behind bottom navigation/mini player
        item {
            Spacer(Modifier.height(bottomPadding))
        }
    }
    
    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                Column {
                    Text("Enter a name for your new playlist:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName)
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.library),
                style = typo.titleLarge,
            )
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
}

@Composable
fun CreatePlaylistButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White, // White background
            contentColor = Color.Black // Black text and icon
        ),
        shape = RoundedCornerShape(8.dp) // More boxy with less rounded corners
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = "Create Playlist",
            style = typo.labelMedium
        )
    }
}
