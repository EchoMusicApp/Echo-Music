

package echo.music.iad1tya.ui.screens.library

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateListOf
import echo.music.iad1tya.ui.menu.SelectionSongMenu
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.CONTENT_TYPE_HEADER
import echo.music.iad1tya.constants.CONTENT_TYPE_SONG
import echo.music.iad1tya.constants.LocalSongsExcludedFoldersKey
import echo.music.iad1tya.constants.LocalSongsMinDurationSecondsKey
import echo.music.iad1tya.constants.LocalSongsSortDescendingKey
import echo.music.iad1tya.constants.LocalSongsSortTypeKey
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.extensions.togglePlayPause
import echo.music.iad1tya.localmedia.LocalSongScanConfig
import echo.music.iad1tya.localmedia.SupportedLocalAudio
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.SongListItem
import echo.music.iad1tya.ui.component.SortHeader
import echo.music.iad1tya.ui.menu.SongMenu
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.LocalSongsScanState
import echo.music.iad1tya.viewmodels.LocalSongsViewModel
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun LocalSongScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.navigateUp() },
    isEmbedded: Boolean = false,
    viewModel: LocalSongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val listState = rememberLazyListState()
    val scanSheetState = rememberModalBottomSheetState()
    var showScanSheet by rememberSaveable { mutableStateOf(false) }
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<String>() }
    val onExitSelectionMode = { 
        inSelectMode = false
        selection.clear()
    }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var query by rememberSaveable { mutableStateOf("") }
    val (sortDescending, onSortDescendingChange) = rememberPreference(LocalSongsSortDescendingKey, true)
    val (sortTypeName, onSortTypeNameChange) = rememberPreference(LocalSongsSortTypeKey, LocalSongSortType.MODIFIED.name)
    val (minimumDurationSeconds, onMinimumDurationSecondsChange) = rememberPreference(
        LocalSongsMinDurationSecondsKey,
        0,
    )
    val (excludedFolders, onExcludedFoldersChange) = rememberPreference(
        LocalSongsExcludedFoldersKey,
        emptySet<String>(),
    )
    val sortType = remember(sortTypeName) { LocalSongSortType.valueOf(sortTypeName) }
    val scanConfig = remember(minimumDurationSeconds, excludedFolders) {
        LocalSongScanConfig(
            minimumDurationSeconds = minimumDurationSeconds,
            excludedFolders = excludedFolders,
        )
    }

    val storagePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var hasStoragePermission by remember(storagePermission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasStoragePermission = granted
    }

    val excludedFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val normalizedFolder = uri?.toExcludedFolderEntry() ?: return@rememberLauncherForActivityResult
        onExcludedFoldersChange(
            LocalSongScanConfig.deduplicateFolderEntries(excludedFolders + normalizedFolder),
        )
    }

    val collator = remember {
        Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
    }
    val bottomContentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 20.dp

    val visibleSongs by remember(songs, query, sortType, sortDescending, collator) {
        derivedStateOf {
            val normalizedQuery = query.trim()
            val supportedSongs = songs.filter { song ->
                SupportedLocalAudio.isSupportedMimeType(song.format?.mimeType)
            }
            val filteredSongs = if (normalizedQuery.isBlank()) {
                supportedSongs
            } else {
                supportedSongs.filter { song ->
                    song.song.title.contains(normalizedQuery, ignoreCase = true) ||
                        song.song.albumName.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        song.artists.any { artist -> artist.name.contains(normalizedQuery, ignoreCase = true) }
                }
            }

            val sortedSongs = when (sortType) {
                LocalSongSortType.MODIFIED -> filteredSongs.sortedBy { song ->
                    song.song.dateModified ?: LocalDateTime.MIN
                }

                LocalSongSortType.NAME -> filteredSongs.sortedWith(compareBy(collator) { song -> song.song.title })
                LocalSongSortType.ARTIST -> filteredSongs.sortedWith(
                    compareBy(collator) { song ->
                        song.artists.joinToString(separator = "") { artist -> artist.name }
                    },
                )

                LocalSongSortType.ALBUM -> filteredSongs.sortedWith(
                    compareBy(collator) { song -> song.song.albumName.orEmpty() },
                )
            }

            if (sortDescending) sortedSongs.asReversed() else sortedSongs
        }
    }

    val queueItems = remember(visibleSongs) { visibleSongs.map { it.toMediaItem() } }

    if (showScanSheet) {
        LocalSongScanSheet(
            hasStoragePermission = hasStoragePermission,
            scanState = scanState,
            minimumDurationSeconds = minimumDurationSeconds,
            onMinimumDurationSecondsChange = onMinimumDurationSecondsChange,
            excludedFolders = excludedFolders,
            onExcludedFoldersChange = onExcludedFoldersChange,
            onAddExcludedFolder = { excludedFolderPickerLauncher.launch(null) },
            sheetState = scanSheetState,
            onDismiss = { showScanSheet = false },
            onPrimaryAction = {
                if (hasStoragePermission) {
                    viewModel.scanDevice(scanConfig)
                } else {
                    permissionLauncher.launch(storagePermission)
                }
            },
        )
    }

    val topPadding = if (isEmbedded) {
        LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
                        AnimatedContent(
                targetState = inSelectMode to isSearchActive,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "localSongTopBar",
            ) { (isSelecting, searching) ->
                if (isSelecting) {
                    TopAppBar(
                        title = { Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size)) },
                        navigationIcon = {
                            IconButton(onClick = onExitSelectionMode) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                )
                            }
                        },
                        actions = {
                            Checkbox(
                                checked = selection.size == visibleSongs.size && selection.isNotEmpty(),
                                onCheckedChange = {
                                    if (selection.size == visibleSongs.size) {
                                        selection.clear()
                                    } else {
                                        selection.clear()
                                        selection.addAll(visibleSongs.map { it.id })
                                    }
                                }
                            )
                            IconButton(
                                enabled = selection.isNotEmpty(),
                                onClick = {
                                    menuState.show {
                                        SelectionSongMenu(
                                            songSelection = selection.mapNotNull { id ->
                                                songs.find { it.id == id }
                                            },
                                            onDismiss = menuState::dismiss,
                                            clearAction = onExitSelectionMode
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
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        ),
                        windowInsets = if (isEmbedded) WindowInsets(0.dp) else TopAppBarDefaults.windowInsets
                    )
                } else if (searching) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = { isSearchActive = false },
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = {
                                    Text(text = stringResource(R.string.search_library))
                                },
                                leadingIcon = {
                                    IconButton(
                                        onClick = {
                                            query = ""
                                            isSearchActive = false
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_back),
                                            contentDescription = stringResource(R.string.back_button_desc),
                                        )
                                    }
                                },
                                trailingIcon = if (query.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.close),
                                            )
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 4.dp),
                        windowInsets = if (isEmbedded) WindowInsets(0.dp) else SearchBarDefaults.windowInsets,
                    ) {}
                } else {
                    LargeTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.local_history),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_back),
                                    contentDescription = null,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = stringResource(R.string.search),
                                )
                            }
                            IconButton(onClick = { showScanSheet = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.settings),
                                    contentDescription = stringResource(R.string.settings),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        ),
                        scrollBehavior = scrollBehavior,
                        windowInsets = if (isEmbedded) WindowInsets(0.dp) else TopAppBarDefaults.windowInsets,
                    )
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = bottomContentPadding,
            ),
        ) {
            item(
                key = "controls",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                LocalSongControlsCard(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    visibleSongCount = visibleSongs.size,
                    onSortTypeChange = { onSortTypeNameChange(it.name) },
                    onSortDescendingChange = onSortDescendingChange,
                )
            }

            if (visibleSongs.isEmpty()) {
                item(
                    key = "empty",
                    contentType = CONTENT_TYPE_HEADER,
                ) {
                    LocalSongEmptyState(query = query)
                }
            } else {

                itemsIndexed(
                    items = visibleSongs,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> CONTENT_TYPE_SONG },
                ) { index, song ->
                                        val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }
                    SongListItem(
                        song = song,
                        showInLibraryIcon = false,
                        showDownloadIcon = false,
                        shape = echo.music.iad1tya.utils.listItemShape(index, visibleSongs.size),
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = selection.contains(song.id),
                                    onCheckedChange = onCheckedChange
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
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
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (inSelectMode) {
                                        onCheckedChange(!selection.contains(song.id))
                                    } else if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = if (query.isBlank()) {
                                                    context.getString(R.string.local_history)
                                                } else {
                                                    context.getString(R.string.queue_searched_songs)
                                                },
                                                items = queueItems,
                                                startIndex = index,
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!inSelectMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inSelectMode = true
                                        onCheckedChange(true)
                                    } else {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                },
                            )
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalSongBadge(
    iconRes: Int,
    text: String,
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(20.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LocalSongControlsCard(
    sortType: LocalSongSortType,
    sortDescending: Boolean,
    visibleSongCount: Int,
    onSortTypeChange: (LocalSongSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp),
    ) {
        SortHeader(
            sortType = sortType,
            sortDescending = sortDescending,
            onSortTypeChange = onSortTypeChange,
            onSortDescendingChange = onSortDescendingChange,
            sortTypeText = { selectedSort ->
                when (selectedSort) {
                    LocalSongSortType.MODIFIED -> R.string.sort_by_last_updated
                    LocalSongSortType.NAME -> R.string.sort_by_name
                    LocalSongSortType.ARTIST -> R.string.sort_by_artist
                    LocalSongSortType.ALBUM -> R.string.sort_by_album
                }
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = pluralStringResource(R.plurals.n_song, visibleSongCount, visibleSongCount),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun LocalSongEmptyState(
    query: String,
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(28.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Icon(
                painter = painterResource(if (query.isBlank()) R.drawable.music_note else R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = if (query.isBlank()) {
                    stringResource(R.string.local_songs_empty_title)
                } else {
                    stringResource(R.string.local_songs_no_matches_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (query.isBlank()) {
                    stringResource(R.string.local_songs_empty_desc)
                } else {
                    stringResource(R.string.local_songs_no_matches_desc)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LocalSongScanSheet(
    hasStoragePermission: Boolean,
    scanState: LocalSongsScanState,
    minimumDurationSeconds: Int,
    onMinimumDurationSecondsChange: (Int) -> Unit,
    excludedFolders: Set<String>,
    onExcludedFoldersChange: (Set<String>) -> Unit,
    onAddExcludedFolder: () -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    val lastSummary = scanState.lastSummary
    val hasError = scanState.errorMessage != null
    val hasSummary = lastSummary != null
    val sanitizedExcludedFolders = remember(excludedFolders) {
        LocalSongScanConfig.deduplicateFolderEntries(excludedFolders)
            .toList()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val durationLabel = if (minimumDurationSeconds <= 0) {
        stringResource(R.string.dark_theme_off)
    } else {
        pluralStringResource(R.plurals.seconds, minimumDurationSeconds, minimumDurationSeconds)
    }

    val heroIcon = when {
        scanState.isScanning -> R.drawable.sync
        hasError -> R.drawable.error
        !hasStoragePermission -> R.drawable.security
        hasSummary -> R.drawable.done
        else -> R.drawable.library_music
    }

    val heroTint = when {
        hasError -> MaterialTheme.colorScheme.error
        scanState.isScanning -> MaterialTheme.colorScheme.primary
        !hasStoragePermission -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val heroContainerColor = when {
        hasError -> MaterialTheme.colorScheme.errorContainer
        scanState.isScanning -> MaterialTheme.colorScheme.primaryContainer
        !hasStoragePermission -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val statusText = when {
        scanState.isScanning -> stringResource(R.string.scanning_device)
        hasError -> stringResource(R.string.local_songs_scan_failed)
        !hasStoragePermission -> stringResource(R.string.local_songs_permission_body)
        hasSummary -> stringResource(
            R.string.local_songs_scan_summary,
            lastSummary!!.scannedSongs,
            lastSummary.removedSongs,
        )
        else -> stringResource(R.string.local_songs_ready_desc)
    }

    val primaryButtonText = if (hasStoragePermission) {
        stringResource(R.string.scan_device)
    } else {
        stringResource(R.string.allow)
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (scanState.isScanning) 0.6f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentAlpha",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusTL = 36.dp,
            cornerRadiusTR = 36.dp,
            cornerRadiusBL = 0.dp,
            cornerRadiusBR = 0.dp,
            smoothnessAsPercentTL = 60,
            smoothnessAsPercentTR = 60,
            smoothnessAsPercentBL = 0,
            smoothnessAsPercentBR = 0,
        ),
        tonalElevation = 2.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {

            AnimatedVisibility(
                visible = scanState.isScanning,
                enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut(),
            ) {
                Surface(
                    shape = AbsoluteSmoothCornerShape(24.dp, 60),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = stringResource(R.string.scanning_device),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Surface(
                shape = AbsoluteSmoothCornerShape(28.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    ScanSheetInfoRow(
                        iconRes = R.drawable.storage,
                        title = stringResource(R.string.permission_storage_title),
                        description = stringResource(R.string.permission_storage_desc),
                        trailing = {
                            Surface(
                                shape = AbsoluteSmoothCornerShape(16.dp, 60),
                                color = if (hasStoragePermission) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (hasStoragePermission) R.drawable.done else R.drawable.close,
                                        ),
                                        contentDescription = null,
                                        tint = if (hasStoragePermission) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = if (hasStoragePermission) {
                                            stringResource(R.string.permission_status_allowed)
                                        } else {
                                            stringResource(R.string.not_allowed)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasStoragePermission) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                    )
                                }
                            }
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    ScanSheetInfoRow(
                        iconRes = R.drawable.info,
                        title = stringResource(R.string.local_songs_latest_scan),
                        description = statusText,
                        trailing = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = AbsoluteSmoothCornerShape(28.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.local_songs_scan_filters_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.local_songs_scan_filters_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    LocalSongScanSettingCard(
                        iconRes = R.drawable.timer,
                        title = stringResource(R.string.local_songs_scan_duration_title),
                        description = stringResource(R.string.local_songs_scan_duration_desc),
                    ) {
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Slider(
                            value = minimumDurationSeconds.toFloat(),
                            onValueChange = { onMinimumDurationSecondsChange(it.roundToInt()) },
                            valueRange = 0f..180f,
                            steps = 11,
                            enabled = !scanState.isScanning,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    LocalSongScanSettingCard(
                        iconRes = R.drawable.snippet_folder,
                        title = stringResource(R.string.local_songs_scan_folders_title),
                        description = stringResource(R.string.local_songs_scan_folders_desc),
                    ) {
                        Surface(
                            shape = AbsoluteSmoothCornerShape(18.dp, 60),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .combinedClickable(onClick = {
                                        if (!scanState.isScanning) {
                                            onAddExcludedFolder()
                                        }
                                    }),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(R.string.local_songs_scan_folders_add),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        if (sanitizedExcludedFolders.isEmpty()) {
                            Text(
                                text = stringResource(R.string.local_songs_scan_folders_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                sanitizedExcludedFolders.forEach { folderPath ->
                                    LocalSongFolderChip(
                                        folderPath = folderPath,
                                        enabled = !scanState.isScanning,
                                        onRemove = {
                                            onExcludedFoldersChange(
                                                excludedFolders.filterNot {
                                                    LocalSongScanConfig.normalizeFolderEntry(it)
                                                        .equals(folderPath, ignoreCase = true)
                                                }.toSet(),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPrimaryAction,
                enabled = !scanState.isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasStoragePermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    contentColor = if (hasStoragePermission) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onTertiary
                    },
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                shape = AbsoluteSmoothCornerShape(28.dp, 60),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = scanState.isScanning,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                    },
                    label = "buttonContent",
                ) { isScanning ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isScanning) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (hasStoragePermission) R.drawable.sync else R.drawable.security,
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isScanning) {
                                stringResource(R.string.scanning_device)
                            } else {
                                primaryButtonText
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = hasError,
                enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow)) + fadeOut(),
            ) {
                Surface(
                    shape = AbsoluteSmoothCornerShape(20.dp, 60),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = scanState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalSongScanSettingCard(
    iconRes: Int,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = AbsoluteSmoothCornerShape(18.dp, 60),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                }
            }

            content()
        }
}

@Composable
private fun LocalSongFolderChip(
    folderPath: String,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(20.dp, 60),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.snippet_folder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = folderPath,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Surface(
                shape = AbsoluteSmoothCornerShape(12.dp, 60),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .combinedClickable(enabled = enabled, onClick = onRemove),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanSheetInfoRow(
    iconRes: Int,
    title: String,
    description: String,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Surface(
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

private fun Uri.toExcludedFolderEntry(): String? {
    if (!DocumentsContract.isTreeUri(this)) return null
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(this) }
        .getOrNull()
        .orEmpty()
    val relativeFolder = treeDocumentId.substringAfter(':', missingDelimiterValue = treeDocumentId)
    return LocalSongScanConfig.normalizeFolderEntry(relativeFolder).takeIf(String::isNotEmpty)
}

private enum class LocalSongSortType {
    MODIFIED,
    NAME,
    ARTIST,
    ALBUM,
}
