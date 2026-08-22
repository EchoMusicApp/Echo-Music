

package echo.music.iad1tya.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ChipSortTypeKey
import echo.music.iad1tya.constants.LibraryFilter
import echo.music.iad1tya.ui.component.ChipsRow
import echo.music.iad1tya.utils.rememberEnumPreference
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.constants.FloatingToolbarBottomPadding
import echo.music.iad1tya.constants.MiniPlayerBottomSpacing
import echo.music.iad1tya.constants.MiniPlayerHeight
import echo.music.iad1tya.constants.NavigationBarHeight
import echo.music.iad1tya.ui.component.TextFieldDialog

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    var showImportMenu by remember { mutableStateOf(false) }
    var showYoutubeImportDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showAiPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler(enabled = filterType != LibraryFilter.LIBRARY) {
        filterType = LibraryFilter.LIBRARY
    }

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips =
                listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    LibraryFilter.LOCAL to stringResource(R.string.filter_local),
                ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    val currentInsets = LocalPlayerAwareWindowInsets.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fabHeightPlusSpacing = 140.dp
    val newInsets = remember(currentInsets, density, layoutDirection) {
        object : WindowInsets {
            override fun getLeft(density: Density, layoutDirection: LayoutDirection) = currentInsets.getLeft(density, layoutDirection)
            override fun getTop(density: Density) = currentInsets.getTop(density)
            override fun getRight(density: Density, layoutDirection: LayoutDirection) = currentInsets.getRight(density, layoutDirection)
            override fun getBottom(density: Density) = currentInsets.getBottom(density) + with(density) { fabHeightPlusSpacing.roundToPx() }
        }
    }

    CompositionLocalProvider(LocalPlayerAwareWindowInsets provides newInsets) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY })

            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY })

            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY })

            LibraryFilter.LOCAL -> LocalSongScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
                isEmbedded = true
            )
        }
        }
    }

    if (filterType == LibraryFilter.PLAYLISTS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    end = 16.dp,
                    bottom = FloatingToolbarBottomPadding + NavigationBarHeight + MiniPlayerBottomSpacing + MiniPlayerHeight + 16.dp
                )
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.create_playlist)) },
                    icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = "Create playlist") },
                    onClick = { showCreatePlaylistOptionsDialog = true },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
                
                Box {
                    ExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.import_playlist)) },
                        icon = { Icon(painter = painterResource(R.drawable.download), contentDescription = "Import playlist") },
                        onClick = { showImportMenu = true },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    
                    DropdownMenu(
                        expanded = showImportMenu,
                        onDismissRequest = { showImportMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Import from Spotify") },
                            onClick = {
                                showImportMenu = false
                                navController.navigate("settings/spotify_import")
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Import from YouTube Music") },
                            onClick = {
                                showImportMenu = false
                                showYoutubeImportDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showYoutubeImportDialog) {
        var url by remember { mutableStateOf(TextFieldValue("")) }
        echo.music.iad1tya.ui.component.TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.link), contentDescription = null) },
            title = {
                Column {
                    Text(text = "Import playlist from YouTube Music")
                    Text(
                        text = "Paste the public YouTube Music playlist link below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            initialTextFieldValue = url,
            autoFocus = true,
            onDismiss = { showYoutubeImportDialog = false },
            onDone = { finalUrl ->
                val listId = Regex("[?&]list=([a-zA-Z0-9_-]+)").find(finalUrl)?.groupValues?.get(1)
                if (listId != null) {
                    navController.navigate("online_playlist/$listId")
                } else {
                    android.widget.Toast.makeText(context, "Invalid playlist URL", android.widget.Toast.LENGTH_SHORT).show()
                }
                showYoutubeImportDialog = false
            }
        )
    }

    if (showCreatePlaylistDialog) {
        echo.music.iad1tya.ui.component.CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = null,
            allowSyncing = true,
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }

    if (showCreatePlaylistOptionsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreatePlaylistOptionsDialog = false },
            title = { Text(stringResource(R.string.create_playlist)) },
            text = { Text("How would you like to create your playlist?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCreatePlaylistOptionsDialog = false
                    showAiPlaylistDialog = true
                }) {
                    Text("Create with AI")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCreatePlaylistOptionsDialog = false
                    showCreatePlaylistDialog = true
                }) {
                    Text("Normally")
                }
            }
        )
    }

    if (showAiPlaylistDialog) {
        echo.music.iad1tya.ui.component.CreateAiPlaylistDialog(
            onDismiss = { showAiPlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showAiPlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }
}
