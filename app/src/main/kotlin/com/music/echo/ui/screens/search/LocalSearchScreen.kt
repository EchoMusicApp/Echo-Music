package echo.music.iad1tya.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.db.entities.Album
import echo.music.iad1tya.db.entities.Artist
import echo.music.iad1tya.db.entities.Playlist
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.AlbumListItem
import echo.music.iad1tya.ui.component.ArtistListItem
import echo.music.iad1tya.ui.component.EmptyPlaceholder
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.NavigationTitle
import echo.music.iad1tya.ui.component.PlaylistListItem
import echo.music.iad1tya.ui.component.SongListItem
import echo.music.iad1tya.ui.menu.SongMenu
import echo.music.iad1tya.viewmodels.LocalSearchViewModel

@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val searchResult by viewModel.result.collectAsState()

    viewModel.query.value = query

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        searchResult.map.forEach { (category, items) ->
            item(key = category) {
                NavigationTitle(
                    title = stringResource(category.titleRes),
                    modifier = Modifier.animateItem()
                )
            }

            items(
                items = items,
                key = { it.id }
            ) { item ->
                when (item) {
                    is Song -> SongListItem(
                        song = item,
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
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
                            .combinedClickable(
                                onClick = {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(item.toMediaMetadata())
                                        )
                                        onDismiss()
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                            .animateItem(),
                    )

                    is Album -> AlbumListItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .clickable {
                                onDismiss()
                                navController.navigate("album/${item.id}")
                            }
                            .animateItem(),
                    )

                    is Artist -> ArtistListItem(
                        artist = item,
                        modifier = Modifier
                            .clickable {
                                onDismiss()
                                navController.navigate("artist/${item.id}")
                            }
                            .animateItem(),
                    )

                    is Playlist -> PlaylistListItem(
                        playlist = item,
                        modifier = Modifier
                            .clickable {
                                onDismiss()
                                navController.navigate("local_playlist/${item.id}")
                            }
                            .animateItem(),
                    )
                }
            }
        }

        if (searchResult.query.isNotEmpty() && searchResult.map.isEmpty()) {
            item(key = "no_result") {
                EmptyPlaceholder(
                    icon = R.drawable.search,
                    text = stringResource(R.string.no_results_found),
                )
            }
        }
    }
}
