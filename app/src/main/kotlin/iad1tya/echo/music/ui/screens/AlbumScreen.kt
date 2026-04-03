package iad1tya.echo.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.AlbumThumbnailSize
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.ThumbnailCornerRadius
import iad1tya.echo.music.db.entities.Album
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.playback.queues.LocalAlbumRadio
import iad1tya.echo.music.ui.component.AutoResizeText
import iad1tya.echo.music.ui.component.FontSizeRange
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.NavigationTitle
import iad1tya.echo.music.ui.component.SongListItem
import iad1tya.echo.music.ui.component.YouTubeGridItem
import iad1tya.echo.music.ui.component.shimmer.ButtonPlaceholder
import iad1tya.echo.music.ui.component.shimmer.ListItemPlaceHolder
import iad1tya.echo.music.ui.component.shimmer.ShimmerHost
import iad1tya.echo.music.ui.component.shimmer.TextPlaceholder
import iad1tya.echo.music.ui.menu.AlbumMenu
import iad1tya.echo.music.ui.menu.SelectionSongMenu
import iad1tya.echo.music.ui.menu.SongMenu
import iad1tya.echo.music.ui.menu.YouTubeAlbumMenu
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.ui.utils.ItemWrapper
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }
    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    var paletteColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.background.toArgb()

    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        val thumbnailUrl = albumWithSongs?.album?.thumbnailUrl ?: run {
            paletteColors = emptyList()
            return@LaunchedEffect
        }

        runCatching {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            result.image?.toBitmap()?.let { bitmap ->
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).maximumColorCount(24).generate()
                }
                PlayerColorExtractor.extractRichGradientColors(
                    palette = palette,
                    fallbackColor = fallbackColor,
                )
            } ?: emptyList()
        }.onSuccess { colors ->
            paletteColors = colors
        }.onFailure {
            paletteColors = emptyList()
        }
    }

    val isDarkMode = isSystemInDarkTheme()
    val baseAccentColor = paletteColors.firstOrNull() ?: MaterialTheme.colorScheme.surfaceVariant
    val screenAccentColor = if (isDarkMode) {
        lerp(baseAccentColor, Color.Black, 0.28f)
    } else {
        lerp(baseAccentColor, Color.White, 0.16f)
    }
    val albumContentColor = Color.White
    val albumButtonContainerColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White
    }
    val albumButtonContentColor = if (isDarkMode) {
        albumContentColor
    } else {
        Color.Black
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenAccentColor),
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(screenAccentColor),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item(key = "album_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = albumWithSongs.album.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(AlbumThumbnailSize + 28.dp)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    )

                    Spacer(Modifier.height(16.dp))

                    AutoResizeText(
                        text = albumWithSongs.album.title,
                        color = albumContentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSizeRange = FontSizeRange(18.sp, 26.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = albumContentColor,
                                ).toSpanStyle(),
                            ) {
                                albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                    val link = LinkAnnotation.Clickable(artist.id) {
                                        navController.navigate("artist/${artist.id}")
                                    }
                                    withLink(link) {
                                        append(artist.name)
                                    }
                                    if (index != albumWithSongs.artists.lastIndex) {
                                        append(", ")
                                    }
                                }
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val albumMeta = listOfNotNull(
                        albumWithSongs.album.year?.toString(),
                        pluralStringResource(
                            R.plurals.n_song,
                            albumWithSongs.songs.size,
                            albumWithSongs.songs.size,
                        ),
                    ).joinToString(" • ")

                    Text(
                        text = albumMeta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(
                            onClick = {
                                database.query {
                                    update(albumWithSongs.album.toggleLike())
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (albumWithSongs.album.bookmarkedAt != null) {
                                        R.drawable.favorite
                                    } else {
                                        R.drawable.favorite_border
                                    },
                                ),
                                contentDescription = null,
                                tint = if (albumWithSongs.album.bookmarkedAt != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    albumContentColor
                                },
                            )
                        }

                        Button(
                            onClick = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = albumButtonContainerColor,
                                contentColor = albumButtonContentColor,
                            ),
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.shuffle))
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = Album(
                                            albumWithSongs.album,
                                            albumWithSongs.artists,
                                        ),
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = albumContentColor,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(
                                LocalAlbumRadio(albumWithSongs),
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = albumButtonContainerColor,
                            contentColor = albumButtonContentColor,
                        ),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(R.string.play),
                        )
                    }
                }
            }

            if (!wrappedSongs.isNullOrEmpty()) {
                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, song -> song.item.id },
                ) { index, songWrapper ->
                    CompositionLocalProvider(LocalContentColor provides albumContentColor) {
                        SongListItem(
                            song = songWrapper.item,
                            albumIndex = index + 1,
                            subtitleColor = Color.White,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,

                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            isSelected = songWrapper.isSelected && selection,
                            modifier =
                            Modifier
                                .background(screenAccentColor)
                                .fillMaxWidth()
                                .animateItem()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(albumWithSongs, startIndex = index),
                                                )
                                            }
                                        } else {
                                            songWrapper.isSelected = !songWrapper.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSongs.forEach {
                                            it.isSelected = false
                                        } // Clear previous selections
                                        songWrapper.isSelected = true // Select the current item
                                    },
                                ),
                        )
                    }
                }
            }

            if (otherVersions.isNotEmpty()) {
                item(key = "other_versions_title") {
                    NavigationTitle(
                        title = stringResource(R.string.other_versions),
                        modifier = Modifier.animateItem()
                    )
                }
                item(key = "other_versions_list") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    ) {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${item.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "loading_shimmer") {
                ShimmerHost(
                    modifier = Modifier.animateItem()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier =
                                Modifier
                                    .size(AlbumThumbnailSize)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                    .background(MaterialTheme.colorScheme.onSurface),
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                TextPlaceholder()
                                TextPlaceholder()
                                TextPlaceholder()
                            }
                        }

                        Spacer(Modifier.padding(8.dp))

                        Row {
                            ButtonPlaceholder(Modifier.weight(1f))

                            Spacer(Modifier.width(12.dp))

                            ButtonPlaceholder(Modifier.weight(1f))
                        }
                    }

                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = screenAccentColor,
            scrolledContainerColor = screenAccentColor,
            titleContentColor = albumContentColor,
            navigationIconContentColor = albumContentColor,
            actionIconContentColor = albumContentColor,
        ),
        title = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (selection) {
                        selection = false
                    } else {
                        navController.navigateUp()
                    }
                },
                onLongClick = {
                    if (!selection) {
                        navController.backToMain()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (selection) R.drawable.close else R.drawable.arrow_back
                    ),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                IconButton(
                    onClick = {
                        if (count == wrappedSongs?.size) {
                            wrappedSongs.forEach { it.isSelected = false }
                        } else {
                            wrappedSongs?.forEach { it.isSelected = true }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == wrappedSongs?.size) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs?.filter { it.isSelected }!!
                                    .map { it.item },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false }
                            )
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            }
        }
    )
}
