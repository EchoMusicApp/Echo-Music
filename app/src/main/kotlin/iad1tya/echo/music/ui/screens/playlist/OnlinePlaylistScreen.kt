package iad1tya.echo.music.ui.screens.playlist

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.echo.innertube.models.SongItem
import com.echo.innertube.models.WatchEndpoint
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.AlbumThumbnailSize
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.ThumbnailCornerRadius
import iad1tya.echo.music.db.entities.PlaylistEntity
import iad1tya.echo.music.db.entities.PlaylistSongMap
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.extensions.toMediaItem
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.queues.ListQueue
import iad1tya.echo.music.ui.component.AutoResizeText
import iad1tya.echo.music.ui.component.DraggableScrollbar
import iad1tya.echo.music.ui.component.FontSizeRange
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.YouTubeListItem
import iad1tya.echo.music.ui.component.shimmer.ButtonPlaceholder
import iad1tya.echo.music.ui.component.shimmer.ListItemPlaceHolder
import iad1tya.echo.music.ui.component.shimmer.ShimmerHost
import iad1tya.echo.music.ui.component.shimmer.TextPlaceholder
import iad1tya.echo.music.ui.menu.SelectionMediaMetadataMenu
import iad1tya.echo.music.ui.menu.YouTubePlaylistMenu
import iad1tya.echo.music.ui.menu.YouTubeSongMenu
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.ui.utils.ItemWrapper
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    var selection by remember {
        mutableStateOf(false)
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.fastAny {
                                    it.name.contains(
                                        query.text,
                                        ignoreCase = true
                                    )
                                }
                    }
            }
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }
    }.toMutableStateList()

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (songs.size >= 5 && lastVisibleIndex != null && lastVisibleIndex >= songs.size - 5) {
                    viewModel.loadMoreSongs()
                }
            }
    }

    var paletteColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.background.toArgb()

    LaunchedEffect(playlist?.thumbnail) {
        val thumbnail = playlist?.thumbnail ?: run {
            paletteColors = emptyList()
            return@LaunchedEffect
        }

        runCatching {
            val request = ImageRequest.Builder(context)
                .data(thumbnail)
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
    val playlistContentColor = Color.White
    val playlistButtonContainerColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White
    }
    val playlistButtonContentColor = if (isDarkMode) {
        playlistContentColor
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
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                .asPaddingValues(),
        ) {
            playlist.let { playlist ->
                if (isLoading) {
                    item(key = "loading_shimmer") {
                        ShimmerHost {
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
                } else if (playlist != null) {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .animateItem(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(playlist.thumbnail)
                                        .build(),
                                    contentDescription = null,
                                    placeholder = painterResource(R.drawable.queue_music),
                                    error = painterResource(R.drawable.queue_music),
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize + 28.dp)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                )

                                Spacer(Modifier.height(16.dp))

                                AutoResizeText(
                                    text = playlist.title,
                                    color = playlistContentColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSizeRange = FontSizeRange(18.sp, 26.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                playlist.author?.let { artist ->
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Normal,
                                                    color = playlistContentColor,
                                                ).toSpanStyle(),
                                            ) {
                                                if (artist.id != null) {
                                                    val link = LinkAnnotation.Clickable(artist.id!!) {
                                                        navController.navigate("artist/${artist.id!!}")
                                                    }
                                                    withLink(link) {
                                                        append(artist.name)
                                                    }
                                                } else {
                                                    append(artist.name)
                                                }
                                            }
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                val playlistMeta = playlist.songCountText
                                    ?: pluralStringResource(R.plurals.n_song, songs.size, songs.size)

                                Text(
                                    text = playlistMeta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (playlist.id != "LM") {
                                        IconButton(
                                            onClick = {
                                                if (dbPlaylist?.playlist == null) {
                                                    database.transaction {
                                                        val playlistEntity = PlaylistEntity(
                                                            name = playlist.title,
                                                            browseId = playlist.id,
                                                            thumbnailUrl = playlist.thumbnail,
                                                            isEditable = playlist.isEditable,
                                                            playEndpointParams = playlist.playEndpoint?.params,
                                                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                            radioEndpointParams = playlist.radioEndpoint?.params,
                                                        ).toggleLike()
                                                        insert(playlistEntity)
                                                        songs.map(SongItem::toMediaMetadata)
                                                            .onEach(::insert)
                                                            .mapIndexed { index, song ->
                                                                PlaylistSongMap(
                                                                    songId = song.id,
                                                                    playlistId = playlistEntity.id,
                                                                    position = index,
                                                                )
                                                            }
                                                            .forEach(::insert)
                                                    }
                                                } else {
                                                    database.transaction {
                                                        val currentPlaylist = dbPlaylist!!.playlist
                                                        update(currentPlaylist, playlist)
                                                        update(currentPlaylist.toggleLike())
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                        R.drawable.favorite
                                                    } else {
                                                        R.drawable.favorite_border
                                                    },
                                                ),
                                                contentDescription = null,
                                                tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    playlistContentColor
                                                },
                                            )
                                        }
                                    }

                                    playlist.shuffleEndpoint?.let {
                                        Button(
                                            onClick = {
                                                val shuffledSongs = songs.map { it.toMediaItem() }.shuffled()
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist.title,
                                                        items = shuffledSongs,
                                                    ),
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = playlistButtonContainerColor,
                                                contentColor = playlistButtonContentColor,
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
                                    }

                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubePlaylistMenu(
                                                    playlist = playlist,
                                                    songs = songs,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                    selectAction = { selection = true },
                                                    canSelect = true,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                            tint = playlistContentColor,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                if (songs.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist.title,
                                                    items = songs.map { it.toMediaItem() },
                                                ),
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = playlistButtonContainerColor,
                                            contentColor = playlistButtonContentColor,
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
                                        Text(stringResource(R.string.play))
                                    }
                                }
                            }
                        }
                    }

                    if (songs.isEmpty() && !isLoading && error == null) {
                        // Show empty playlist message when playlist is loaded but has no songs
                        item(key = "empty_playlist") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_playlist),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_playlist_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = wrappedSongs,
                    ) { index, song ->
                        CompositionLocalProvider(LocalContentColor provides playlistContentColor) {
                            YouTubeListItem(
                                item = song.item.second,
                                subtitleColor = Color.White,
                                isActive = mediaMetadata?.id == song.item.second.id,
                                isPlaying = isPlaying,
                                isSelected = song.isSelected && selection,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song.item.second,
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
                                modifier =
                                Modifier
                                    .background(screenAccentColor)
                                    .combinedClickable(
                                        enabled = !hideExplicit || !song.item.second.explicit,
                                        onClick = {
                                            if (!selection) {
                                                if (song.item.second.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = playlist.title,
                                                            items = filteredSongs.map { it.second.toMediaItem() },
                                                            startIndex = index
                                                        )
                                                    )
                                                }
                                            } else {
                                                song.isSelected = !song.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) {
                                                selection = true
                                            }
                                            wrappedSongs.forEach { it.isSelected = false }
                                            song.isSelected = true
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (viewModel.continuation != null && songs.isNotEmpty() && isLoadingMore) {
                        item(key = "loading_more") {
                            ShimmerHost {
                                repeat(2) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }

                } else {
                    // Show error state when playlist is null and there's an error
                    item(key = "error_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (error != null) {
                                    stringResource(R.string.error_unknown)
                                } else {
                                    stringResource(R.string.playlist_not_found)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (error != null) {
                                    error!!
                                } else {
                                    stringResource(R.string.playlist_not_found_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { 
                                        viewModel.retry()
                                    }
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 1
        )

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = screenAccentColor,
                scrolledContainerColor = screenAccentColor,
                titleContentColor = playlistContentColor,
                navigationIconContentColor = playlistContentColor,
                actionIconContentColor = playlistContentColor,
            ),
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = playlistContentColor,
                            unfocusedTextColor = playlistContentColor,
                            cursorColor = playlistContentColor,
                            focusedPlaceholderColor = playlistContentColor.copy(alpha = 0.75f),
                            unfocusedPlaceholderColor = playlistContentColor.copy(alpha = 0.75f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else if (showTopBarTitle) {
                    Text(playlist?.title.orEmpty())
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
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
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.second.toMediaItem().metadata!! },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else {
                    if (!isSearching) {
                        IconButton(
                            onClick = { isSearching = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}
