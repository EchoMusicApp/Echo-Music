

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.constants.FloatingToolbarBottomPadding
import echo.music.iad1tya.constants.MiniPlayerBottomSpacing
import echo.music.iad1tya.constants.MiniPlayerHeight
import echo.music.iad1tya.constants.NavigationBarHeight
import echo.music.iad1tya.ui.component.DefaultDialog
import echo.music.iad1tya.ui.component.TextFieldDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign

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
    val fabHeightPlusSpacing = 128.dp
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

            val bottomPadding = with(density) { currentInsets.getBottom(density).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        end = 16.dp,
                        bottom = bottomPadding + 20.dp
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
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )

                    Box {
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(R.string.import_playlist)) },
                            icon = { Icon(painter = painterResource(R.drawable.download), contentDescription = "Import playlist") },
                            onClick = { showImportMenu = true },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )

                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_from_spotify)) },
                                onClick = {
                                    showImportMenu = false
                                    navController.navigate("settings/spotify_import")
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_from_youtube_music)) },
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
    }

    if (showYoutubeImportDialog) {
        var url by remember { mutableStateOf(TextFieldValue("")) }
        val invalidUrlMessage = stringResource(R.string.invalid_playlist_url)
        echo.music.iad1tya.ui.component.TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.link), contentDescription = null) },
            title = {
                Column {
                    Text(text = stringResource(R.string.import_youtube_music_playlist_title))
                    Text(
                        text = stringResource(R.string.import_youtube_music_playlist_desc),
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
                    android.widget.Toast.makeText(context, invalidUrlMessage, android.widget.Toast.LENGTH_SHORT).show()
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
        DefaultDialog(
            onDismiss = { showCreatePlaylistOptionsDialog = false },
            title = { Text(stringResource(R.string.create_playlist)) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Normally
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            showCreatePlaylistOptionsDialog = false
                            showCreatePlaylistDialog = true
                        }
                        .padding(vertical = 20.dp, horizontal = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.create_playlist_normally),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }

                // Create with AI
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            showCreatePlaylistOptionsDialog = false
                            showAiPlaylistDialog = true
                        }
                        .padding(vertical = 20.dp, horizontal = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.sparks),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.create_playlist_with_ai),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
