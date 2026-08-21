package echo.music.iad1tya.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kyant.backdrop.highlight.Highlight
import com.kmpalette.rememberPaletteState
import echo.music.iad1tya.common.LOCAL_PLAYLIST_ID
import echo.music.iad1tya.domain.data.entities.DownloadState
import echo.music.iad1tya.domain.data.entities.LocalPlaylistEntity
import echo.music.iad1tya.domain.data.entities.PairSongLocalPlaylist
import echo.music.iad1tya.domain.data.entities.SongEntity
import echo.music.iad1tya.domain.utils.FilterState
import echo.music.iad1tya.domain.utils.toTrack
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.Platform
import echo.music.iad1tya.expect.ui.layerBackdrop
import echo.music.iad1tya.expect.ui.rememberBackdrop
import echo.music.iad1tya.expect.ui.toImageBitmap
import echo.music.iad1tya.extension.artworkScrimBrush
import echo.music.iad1tya.extension.displayNameRes
import echo.music.iad1tya.extension.getColorFromPalette
import echo.music.iad1tya.extension.getScreenSizeInfo
import echo.music.iad1tya.extension.toImmersiveBackground
import echo.music.iad1tya.getPlatform
import echo.music.iad1tya.ui.component.CenterLoadingBox
import echo.music.iad1tya.ui.component.rememberSurfaceDarkColors
import echo.music.iad1tya.ui.component.DraggableItem
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.LiquidGlassIconButton
import echo.music.iad1tya.ui.component.LoadingDialog
import echo.music.iad1tya.ui.component.LocalPlaylistBottomSheet
import echo.music.iad1tya.ui.component.NowPlayingBottomSheet
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.component.SongFullWidthItems
import echo.music.iad1tya.ui.component.SortPlaylistBottomSheet
import echo.music.iad1tya.ui.component.SuggestItems
import echo.music.iad1tya.ui.component.liquidGlass
import echo.music.iad1tya.ui.component.painterPlaylistThumbnail
import echo.music.iad1tya.ui.component.playlistTitleGradient
import echo.music.iad1tya.ui.component.rememberDragDropState
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.DownloadForOffline
import echo.music.iad1tya.ui.icon.MoreVert
import echo.music.iad1tya.ui.icon.Pause
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.Shuffle
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.icon.Sort
import echo.music.iad1tya.ui.icon.TipsAndUpdates
import echo.music.iad1tya.ui.theme.LocalIsDarkTheme
import echo.music.iad1tya.ui.theme.seed
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.LocalPlaylistUIEvent
import echo.music.iad1tya.viewModel.LocalPlaylistViewModel
import echo.music.iad1tya.viewModel.SharedViewModel
import echo.music.iad1tya.viewModel.UIEvent
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.album_length
import echomusic.composeapp.generated.resources.baseline_downloaded
import echomusic.composeapp.generated.resources.cancel
import echomusic.composeapp.generated.resources.created_at
import echomusic.composeapp.generated.resources.downloaded
import echomusic.composeapp.generated.resources.downloading
import echomusic.composeapp.generated.resources.reload
import echomusic.composeapp.generated.resources.sort_by
import echomusic.composeapp.generated.resources.suggest
import echomusic.composeapp.generated.resources.sync_playlist_warning
import echomusic.composeapp.generated.resources.synced_playlist_cannot_change_order
import echomusic.composeapp.generated.resources.unsync_playlist_warning
import echomusic.composeapp.generated.resources.warning
import echomusic.composeapp.generated.resources.yes
import echomusic.composeapp.generated.resources.your_playlist

private const val TAG = "LocalPlaylistScreen"

@ExperimentalFoundationApi
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoroutinesApi::class,
    ExperimentalHazeMaterialsApi::class,
)
@Composable
fun LocalPlaylistScreen(
    id: Long,
    sharedViewModel: SharedViewModel = koinInject(),
    viewModel: LocalPlaylistViewModel = koinViewModel(),
    navController: NavController,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/downloading_animation.json").decodeToString(),
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val aiPainter = rememberVectorPainter(echoIcons.TipsAndUpdates)
    val limit = 1.5f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -limit,
        targetValue = limit,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val downloadState by viewModel.uiState.map { it.downloadState }.collectAsState(
        initial = DownloadState.STATE_NOT_DOWNLOADED,
    )
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val suggestedTracks by viewModel.uiState.map { it.suggestions?.songs ?: emptyList() }.collectAsState(initial = emptyList())
    val suggestionsLoading by viewModel.loading.collectAsStateWithLifecycle()
    var showSyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var firstTimeGetLocalPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var sortBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var changingOrder by remember {
        mutableStateOf(false)
    }

    val trackPagingItems: LazyPagingItems<Pair<SongEntity, PairSongLocalPlaylist>> = viewModel.tracksPagingState.collectAsLazyPagingItems()
    LaunchedEffect(Unit) {
        snapshotFlow {
            trackPagingItems.loadState
        }.collectLatest {
            Logger.d("PlaylistScreen", "loadState: ${trackPagingItems.loadState}")
            viewModel.setLazyTrackPagingItems(trackPagingItems)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            firstTimeGetLocalPlaylist = true
        }
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            LocalPlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = trackPagingItems.itemSnapshotList.findLast { it?.first?.videoId == videoId }?.first
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = shouldShowSuggestions) {
        if (suggestedTracks.isEmpty() && uiState.syncState != LocalPlaylistEntity.YouTubeSyncState.NotSynced) {
            viewModel.getSuggestions(uiState.id)
        }
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.id) {
            Logger.w("PlaylistScreen", "new id: $id")
            viewModel.setOffset(0)
            viewModel.removeListSuggestion()
            viewModel.updatePlaylistState(id, true)
            delay(100)
            firstTimeGetLocalPlaylist = true
        }
    }
    LaunchedEffect(key1 = uiState) {
        shouldShowSuggestButton =
            !uiState.ytPlaylistId.isNullOrEmpty() &&
            uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val paletteState = rememberPaletteState()
    val hazeState = rememberHazeState(blurEnabled = true)
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    // Track which thumbnail we've already extracted a palette from.
    // Prevents palette flash when LazyColumn recycles the header item on scroll —
    // AsyncImage re-mount fires onSuccess again, but we skip the regenerate.
    var paletteGeneratedFor by remember {
        mutableStateOf<String?>(null)
    }
    val currentThumbnail = uiState.thumbnail

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && paletteGeneratedFor != currentThumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentThumbnail
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), Color.Black))
            }
    }

    // Apple Music-inspired immersive treatment. Which header is used depends on the window's
    // aspect ratio alone, not on the platform: a portrait window (a phone held upright, or a
    // narrow desktop window) gets the edge-to-edge artwork header, a landscape one gets the
    // side-by-side header. Everything else on the page — the palette background, the row
    // dividers, the blurred top bar — is shared by both.
    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP
    // Apple Music-style page background. When the playlist has real artwork we pick a vivid
    // swatch from its palette (getColorFromPalette). Many local playlists have NO artwork —
    // AsyncImage shows the title placeholder and onSuccess never fires, so the palette stays
    // null; in that case we fall back to the SAME deterministic title gradient the placeholder
    // draws, so the background still matches the thumbnail. Darkened slightly for readability.
    val mutedPaletteBg =
        run {
            val palette = paletteState.palette
            if (palette != null) {
                palette.toImmersiveBackground()
            } else {
                val titleColors = playlistTitleGradient(uiState.title)
                val base =
                    if (titleColors.size >= 2) {
                        lerp(titleColors[0], titleColors[1], 0.5f)
                    } else {
                        titleColors.firstOrNull() ?: Color.Black
                    }
                lerp(base, Color.Black, 0.3f)
            }
        }

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val dragDropState =
        rememberDragDropState(lazyState) { from, to ->
            coroutineScope.launch {
                Logger.d(TAG, "onMove from $from to $to")
                viewModel.changeLocalPlaylistItemPosition(from - 1, to - 1)
                trackPagingItems.refresh()
            }
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
//    Box {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(mutedPaletteBg)
                .hazeSource(hazeState)
                .pointerInput(changingOrder) {
                    if (!changingOrder) return@pointerInput
                    val onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, offset: Offset) -> Unit =
                        { change, offset ->
                            Logger.d(TAG, "onDrag $offset")
                            change.consume()
                            dragDropState.onDrag(offset = offset)

                            if (overscrollJob?.isActive != true) {
                                dragDropState
                                    .checkForOverScroll()
                                    .takeIf { it != 0f }
                                    ?.let {
                                        overscrollJob =
                                            coroutineScope.launch {
                                                dragDropState.state.animateScrollBy(
                                                    it * 1.3f,
                                                    tween(easing = FastOutLinearInEasing),
                                                )
                                            }
                                    }
                                    ?: run { overscrollJob?.cancel() }
                            }
                        }
                    val onDragStart: (Offset) -> Unit = { offset ->
                        Logger.d(TAG, "onDragStart $offset")
                        dragDropState.onDragStart(offset)
                    }
                    val onDragEnd: () -> Unit = {
                        Logger.d(TAG, "onDragEnd")
                        dragDropState.onDragInterrupted(true)
                        overscrollJob?.cancel()
                    }
                    val onDragCancel: () -> Unit = {
                        Logger.d(TAG, "onDragCancel")
                        dragDropState.onDragInterrupted()
                        overscrollJob?.cancel()
                    }
                    if (getPlatform() == Platform.Desktop) {
                        // Desktop: normal drag (click-and-drag), no long press needed
                        detectDragGestures(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    } else {
                        // Android: long press then drag (touch-friendly)
                        detectDragGesturesAfterLongPress(
                            onDrag = onDrag,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                },
        state = lazyState,
    ) {
        item(contentType = "header") {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Transparent),
            ) {
                Column(
                    Modifier
                        .background(Color.Transparent),
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        if (isPortrait) {
                            // Apple Music-style: edge-to-edge artwork + liquid glass buttons.
                            // Glass buttons MUST be siblings of the backdrop source (not children)
                            // to avoid render feedback loop / RuntimeShader crash.
                            val artworkBackdrop = rememberBackdrop(Color.Black)
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height((screenInfo.hDP / 2).dp),
                            ) {
                                // Inner Box — backdrop SOURCE (artwork + overlays only, NO glass)
                                Box(modifier = Modifier.fillMaxSize().layerBackdrop(artworkBackdrop)) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(uiState.thumbnail)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(uiState.thumbnail)
                                                .memoryCacheKey(uiState.thumbnail)
                                                .crossfade(false)
                                                .build(),
                                        placeholder = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                                        error = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                                        fallback = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        onSuccess = {
                                            bitmap = it.result.image.toImageBitmap()
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    // Scrim spans 70% of the artwork (not a fixed 200dp): the shorter the
                                    // ramp, the steeper the alpha, and a steep ramp is what makes the fade
                                    // read as an edge. See artworkScrimBrush for the curve itself.
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height((screenInfo.hDP * 0.35f).dp)
                                                .align(Alignment.BottomCenter)
                                                .background(artworkScrimBrush(mutedPaletteBg)),
                                    )
                                    Column(
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .padding(bottom = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = uiState.title,
                                            style = typo().titleLarge,
                                            color = Color.White,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(Res.string.your_playlist),
                                            style = typo().titleSmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text =
                                                stringResource(
                                                    Res.string.created_at,
                                                    uiState.inLibrary?.format(
                                                        LocalDateTime.Format {
                                                            hour()
                                                            char(':')
                                                            minute()
                                                            chars(" - ")
                                                            day()
                                                            char(' ')
                                                            monthName(
                                                                MonthNames.ENGLISH_FULL,
                                                            )
                                                            char(' ')
                                                            year()
                                                        },
                                                    ) ?: "",
                                                ),
                                            style = typo().bodyMedium,
                                            color = Color(0xC4FFFFFF),
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                // Back button + right pill (More only) overlays — liquid glass, siblings of source
                                Row(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .windowInsetsPadding(WindowInsets.statusBars),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LiquidGlassIconButton(
                                        backdrop = artworkBackdrop,
                                        imageVector = echoIcons.ArrowBackIosNew,
                                        modifier =
                                            Modifier
                                                .size(48.dp),
                                    ) {
                                        navController.navigateUp()
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Row(
                                        modifier =
                                            Modifier
                                                .height(48.dp)
                                                .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // AI Suggest — only when synced with YouTube
                                        if (shouldShowSuggestButton) {
                                            IconButton(
                                                onClick = {
                                                    shouldShowSuggestions = !shouldShowSuggestions
                                                },
                                            ) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(24.dp)
                                                            .graphicsLayer {
                                                                compositingStrategy = CompositingStrategy.Offscreen
                                                            }.drawWithContent {
                                                                val width = size.width
                                                                val height = size.height
                                                                val offsetDraw = width * progressAnimated
                                                                val brush =
                                                                    Brush.linearGradient(
                                                                        colors =
                                                                            listOf(
                                                                                Color(0xFF4C82EF),
                                                                                Color(0xFFD96570),
                                                                            ),
                                                                        start = Offset(offsetDraw, 0f),
                                                                        end =
                                                                            Offset(
                                                                                offsetDraw + width,
                                                                                height,
                                                                            ),
                                                                    )
                                                                with(aiPainter) {
                                                                    draw(size = Size(width, height))
                                                                }
                                                                drawRect(
                                                                    brush = brush,
                                                                    blendMode = BlendMode.SrcIn,
                                                                )
                                                            },
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = onPlaylistMoreClick,
                                        ) {
                                            Icon(
                                                imageVector = echoIcons.MoreVert,
                                                contentDescription = "More",
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Apple Music desktop header: back and the overlay actions on their own
                            // top row, then a square artwork with the text column and the action
                            // cluster laid out beside it rather than under it.
                            // Built exactly like the portrait branch, which renders correctly on both
                            // platforms: the backdrop SOURCE is the content column — artwork included,
                            // so the recorded layer holds something to refract — and the glass buttons
                            // are SIBLINGS placed with align(), never children of the source (that
                            // nesting is the render-feedback loop that kills the RuntimeShader).
                            val headerBackdrop = rememberBackdrop(mutedPaletteBg)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .layerBackdrop(headerBackdrop)
                                            .windowInsetsPadding(WindowInsets.statusBars)
                                            .padding(horizontal = 32.dp, vertical = 16.dp),
                                ) {
                                    // Reserves the strip the sibling glass buttons are drawn over.
                                    Spacer(modifier = Modifier.height(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        AsyncImage(
                                            model =
                                                ImageRequest
                                                    .Builder(LocalPlatformContext.current)
                                                    .data(uiState.thumbnail)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(uiState.thumbnail)
                                                    .memoryCacheKey(uiState.thumbnail)
                                                    .crossfade(false)
                                                    .build(),
                                            placeholder = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                                            error = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                                            fallback = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            onSuccess = {
                                                bitmap = it.result.image.toImageBitmap()
                                            },
                                            modifier =
                                                Modifier
                                                    .size(280.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = uiState.title,
                                                style = typo().headlineSmall,
                                                color = Color.White,
                                                maxLines = 2,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(Res.string.your_playlist),
                                                style = typo().titleMedium,
                                                // The app accent, standing in for the brand red Apple uses here.
                                                color = seed,
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.created_at,
                                                        uiState.inLibrary?.format(
                                                            LocalDateTime.Format {
                                                                hour()
                                                                char(':')
                                                                minute()
                                                                chars(" - ")
                                                                day()
                                                                char(' ')
                                                                monthName(
                                                                    MonthNames.ENGLISH_FULL,
                                                                )
                                                                char(' ')
                                                                year()
                                                            },
                                                        ) ?: "",
                                                    ),
                                                style = typo().bodyMedium,
                                                color = Color(0xC4FFFFFF),
                                                textAlign = TextAlign.Center,
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            // Apple Music-style action row: [Shuffle][Play pill][Download][Suggest]
                                            // centered cluster. Suggest (AI) only shows when synced with YouTube.
                                            val isThisPlaying =
                                                isPlaying && playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(
                                                        12.dp,
                                                        Alignment.Start,
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.12f))
                                                            .clickable {
                                                                viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick)
                                                            },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = echoIcons.Shuffle,
                                                        contentDescription = "Shuffle",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .height(48.dp)
                                                            .widthIn(min = 110.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                            .clickable {
                                                                if (isThisPlaying) {
                                                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                                } else {
                                                                    viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                                                                }
                                                            }.padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector =
                                                                if (isThisPlaying) {
                                                                    echoIcons.Pause
                                                                } else {
                                                                    echoIcons.PlayArrow
                                                                },
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(22.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isThisPlaying) "Pause" else "Play",
                                                            color = Color.Black,
                                                            style = typo().labelLarge,
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Crossfade(targetState = downloadState) { state ->
                                                        when (state) {
                                                            DownloadState.STATE_DOWNLOADED -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxSize()
                                                                            .clickable {
                                                                                viewModel.makeToast(
                                                                                    runBlocking { getString(Res.string.downloaded) },
                                                                                )
                                                                            },
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(Res.drawable.baseline_downloaded),
                                                                        tint = Color(0xFF00A0CB),
                                                                        contentDescription = "",
                                                                        modifier = Modifier.size(22.dp),
                                                                    )
                                                                }
                                                            }

                                                            DownloadState.STATE_DOWNLOADING -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxSize()
                                                                            .clickable {
                                                                                viewModel.makeToast(
                                                                                    runBlocking { getString(Res.string.downloading) },
                                                                                )
                                                                            },
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Image(
                                                                        painter =
                                                                            rememberLottiePainter(
                                                                                composition = composition,
                                                                                iterations = Compottie.IterateForever,
                                                                            ),
                                                                        contentDescription = "Lottie animation",
                                                                        modifier = Modifier.size(28.dp),
                                                                    )
                                                                }
                                                            }

                                                            else -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxSize()
                                                                            .clickable {
                                                                                Logger.w("PlaylistScreen", "downloadState: $downloadState")
                                                                                viewModel.downloadFullPlaylist()
                                                                            },
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Icon(
                                                                        imageVector = echoIcons.DownloadForOffline,
                                                                        tint = Color.White,
                                                                        contentDescription = "Download",
                                                                        modifier = Modifier.size(22.dp),
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Glass overlays — siblings of the source, over the strip the column reserved.
                                Box(Modifier.padding(start = 12.dp)) {
                                    LiquidGlassIconButton(
                                        backdrop = headerBackdrop,
                                        imageVector = echoIcons.ArrowBackIosNew,
                                        shape = RoundedCornerShape(24.dp),
                                        // Same directional style as the like/⋯ pill, a touch thicker. The default
                                        // width of 0.5.dp becomes a ~2px stroke (HighlightModifier: ceil(width.toPx()) * 2),
                                        // which reads along the pill's long edge but vanishes around a 48dp circle. 1.dp
                                        // is the smallest step up that stays visible without looking like a border.
                                        highlight = Highlight(width = 1.dp),
                                        modifier =
                                            Modifier
                                                .align(Alignment.TopStart)
                                                .padding(12.dp)
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .size(48.dp),
                                    ) {
                                        navController.navigateUp()
                                    }
                                }
                                Row(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .windowInsetsPadding(WindowInsets.statusBars)
                                            .padding(end = 32.dp, top = 16.dp)
                                            .height(48.dp)
                                            .liquidGlass(headerBackdrop, RoundedCornerShape(24.dp)),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // AI Suggest — only when synced with YouTube
                                    if (shouldShowSuggestButton) {
                                        IconButton(
                                            onClick = {
                                                shouldShowSuggestions = !shouldShowSuggestions
                                            },
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(24.dp)
                                                        .graphicsLayer {
                                                            compositingStrategy = CompositingStrategy.Offscreen
                                                        }.drawWithContent {
                                                            val width = size.width
                                                            val height = size.height
                                                            val offsetDraw = width * progressAnimated
                                                            val brush =
                                                                Brush.linearGradient(
                                                                    colors =
                                                                        listOf(
                                                                            Color(0xFF4C82EF),
                                                                            Color(0xFFD96570),
                                                                        ),
                                                                    start = Offset(offsetDraw, 0f),
                                                                    end =
                                                                        Offset(
                                                                            offsetDraw + width,
                                                                            height,
                                                                        ),
                                                                )
                                                            with(aiPainter) {
                                                                draw(size = Size(width, height))
                                                            }
                                                            drawRect(
                                                                brush = brush,
                                                                blendMode = BlendMode.SrcIn,
                                                            )
                                                        },
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = onPlaylistMoreClick,
                                    ) {
                                        Icon(
                                            imageVector = echoIcons.MoreVert,
                                            contentDescription = "More",
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                        ) {
                            Column(Modifier.padding(horizontal = 32.dp)) {
                                if (isPortrait) {
                                    // Apple Music-style action row: [Shuffle][Play pill][Download][Suggest]
                                    // centered cluster. Suggest (AI) only shows when synced with YouTube.
                                    val isThisPlaying =
                                        isPlaying && playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(
                                                12.dp,
                                                Alignment.CenterHorizontally,
                                            ),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .clickable {
                                                        viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick)
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = echoIcons.Shuffle,
                                                contentDescription = "Shuffle",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .height(48.dp)
                                                    .widthIn(min = 110.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .clickable {
                                                        if (isThisPlaying) {
                                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                        } else {
                                                            viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                                                        }
                                                    }.padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector =
                                                        if (isThisPlaying) {
                                                            echoIcons.Pause
                                                        } else {
                                                            echoIcons.PlayArrow
                                                        },
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isThisPlaying) "Pause" else "Play",
                                                    color = Color.Black,
                                                    style = typo().labelLarge,
                                                )
                                            }
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Crossfade(targetState = downloadState) { state ->
                                                when (state) {
                                                    DownloadState.STATE_DOWNLOADED -> {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable {
                                                                        viewModel.makeToast(
                                                                            runBlocking { getString(Res.string.downloaded) },
                                                                        )
                                                                    },
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(Res.drawable.baseline_downloaded),
                                                                tint = Color(0xFF00A0CB),
                                                                contentDescription = "",
                                                                modifier = Modifier.size(22.dp),
                                                            )
                                                        }
                                                    }

                                                    DownloadState.STATE_DOWNLOADING -> {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable {
                                                                        viewModel.makeToast(
                                                                            runBlocking { getString(Res.string.downloading) },
                                                                        )
                                                                    },
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Image(
                                                                painter =
                                                                    rememberLottiePainter(
                                                                        composition = composition,
                                                                        iterations = Compottie.IterateForever,
                                                                    ),
                                                                contentDescription = "Lottie animation",
                                                                modifier = Modifier.size(28.dp),
                                                            )
                                                        }
                                                    }

                                                    else -> {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable {
                                                                        Logger.w("PlaylistScreen", "downloadState: $downloadState")
                                                                        viewModel.downloadFullPlaylist()
                                                                    },
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(
                                                                imageVector = echoIcons.DownloadForOffline,
                                                                tint = Color.White,
                                                                contentDescription = "Download",
                                                                modifier = Modifier.size(22.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.album_length,
                                            (uiState.trackCount).toString(),
                                            "",
                                        ),
                                    color = Color.White,
                                    style = typo().bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                AnimatedVisibility(visible = shouldShowSuggestions) {
                                    Column(
                                        modifier = Modifier.animateContentSize(),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.suggest,
                                                    ),
                                                color = Color.White,
                                                modifier =
                                                    Modifier
                                                        .padding(vertical = 8.dp)
                                                        .weight(1f),
                                            )
                                            TextButton(
                                                onClick = { viewModel.reloadSuggestion() },
                                                modifier =
                                                    Modifier
                                                        .padding(horizontal = 8.dp),
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.reload),
                                                    color = Color.White,
                                                    modifier =
                                                        Modifier.align(
                                                            Alignment.CenterVertically,
                                                        ),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Crossfade(targetState = suggestionsLoading) {
                                            if (it) {
                                                CenterLoadingBox(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .align(Alignment.CenterHorizontally),
                                                )
                                            } else {
                                                Column {
                                                    suggestedTracks.forEachIndexed { index, track ->
                                                        SuggestItems(
                                                            forceDark = true,
                                                            track = track,
                                                            isPlaying = playingTrack?.videoId == track.videoId,
                                                            onAddClickListener = {
                                                                viewModel.addSuggestTrackToListTrack(
                                                                    track,
                                                                )
                                                            },
                                                            onClickListener = {
                                                                viewModel.onUIEvent(LocalPlaylistUIEvent.SuggestionsItemClick(track.videoId))
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(12.dp))
                                        HorizontalDivider(
                                            color = Color.Gray,
                                            thickness = 0.5.dp,
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }
                                Column {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                        // Match the Apple Music action buttons: flat white@12% pill, white content.
                                        Row(
                                            modifier =
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.12f))
                                                    .clickable {
                                                        sortBottomSheetShow = true
                                                    }.padding(vertical = 8.dp, horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = echoIcons.Sort,
                                                contentDescription = "Sort playlist",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(
                                                text =
                                                    stringResource(Res.string.sort_by) + ": " +
                                                        stringResource(
                                                            uiState.filterState.displayNameRes(),
                                                        ),
                                                style = typo().bodySmall,
                                                color = Color.White,
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        AnimatedVisibility(
                                            visible =
                                                uiState.filterState == FilterState.CustomOrder,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    changingOrder = !changingOrder
                                                },
                                            ) {
                                                Text(
                                                    text =
                                                        if (changingOrder) {
                                                            "Done"
                                                        } else {
                                                            "Change order"
                                                        },
                                                    style = typo().bodySmall,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        items(count = trackPagingItems.itemCount, key = { index ->
            val item = trackPagingItems[index]
            (item?.first?.videoId ?: "") + "item_$index" + item?.second?.inPlaylist + item?.second?.position
        }) { index ->
            val item = trackPagingItems[index]?.first
            if (item != null) {
                val content = @Composable { mod: Modifier ->
                    if (playingTrack?.videoId == item.videoId && isPlaying) {
                        SongFullWidthItems(
                            forceDark = true,
                            isPlaying = true,
                            shouldShowDragHandle = changingOrder,
                            songEntity = item,
                            onMoreClickListener = { onItemMoreClick(it) },
                            onClickListener = {
                                Logger.w("PlaylistScreen", "index: $index")
                                onPlaylistItemClick(it)
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(item.toTrack()),
                                )
                            },
                            modifier = mod,
                        )
                    } else {
                        SongFullWidthItems(
                            forceDark = true,
                            isPlaying = false,
                            shouldShowDragHandle = changingOrder,
                            songEntity = item,
                            onMoreClickListener = { onItemMoreClick(it) },
                            onClickListener = {
                                Logger.w("PlaylistScreen", "index: $index")
                                onPlaylistItemClick(it)
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(item.toTrack()),
                                )
                            },
                            modifier = mod,
                        )
                    }
                }
                if (changingOrder) {
                    DraggableItem(
                        dragDropState,
                        index + 1,
                        Modifier.animateItem(),
                    ) {
                        content(Modifier)
                    }
                } else {
                    Column(modifier = Modifier.animateItem()) {
                        content(Modifier)
                        if (index < trackPagingItems.itemCount - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = Color.White.copy(alpha = 0.12f),
                            )
                        }
                    }
                }
            }
        }
        trackPagingItems.apply {
            item {
                AnimatedVisibility(loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(15.dp))
                        CenterLoadingBox(modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(15.dp))
                    }
                }
            }
        }
        item {
            EndOfPage()
        }
    }
    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return
        NowPlayingBottomSheet(
            onDelete = { viewModel.deleteItem(uiState.id, track) },
            onDismiss = {
                itemBottomSheetShow = false
                currentItem = null
            },
            navController = navController,
            song = track,
        )
    }
    if (playlistBottomSheetShow) {
        Logger.w("PlaylistScreen", "PlaylistBottomSheet")
        LocalPlaylistBottomSheet(
            isBottomSheetVisible = playlistBottomSheetShow,
            onDismiss = { playlistBottomSheetShow = false },
            title = uiState.title,
            ytPlaylistId = uiState.ytPlaylistId,
            onEditTitle =
                { newTitle ->
                    viewModel.updatePlaylistTitle(newTitle, uiState.id)
                },
            onEditThumbnail =
                { thumbUri ->
                    viewModel.updatePlaylistThumbnail(thumbUri, uiState.id)
                },
            onAddToQueue = {
                viewModel.addAllToQueue()
            },
            onSync = {
                if (uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
                    showUnsyncAlertDialog = true
                } else {
                    showSyncAlertDialog = true
                }
            },
            onUpdatePlaylist = {
                viewModel.updateListTrackSynced(uiState.id)
            },
            onDelete = {
                viewModel.deletePlaylist(uiState.id)
                navController.navigateUp()
            },
        )
    }
    if (showSyncAlertDialog) {
        AlertDialog(
            containerColor = rememberSurfaceDarkColors().container,
            titleContentColor = rememberSurfaceDarkColors().content,
            textContentColor = rememberSurfaceDarkColors().content,
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.sync_playlist_warning)) },
            onDismissRequest = { showSyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.syncPlaylistWithYouTubePlaylist(uiState.id)
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
    if (showUnsyncAlertDialog) {
        AlertDialog(
            containerColor = rememberSurfaceDarkColors().container,
            titleContentColor = rememberSurfaceDarkColors().content,
            textContentColor = rememberSurfaceDarkColors().content,
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.unsync_playlist_warning)) },
            onDismissRequest = { showUnsyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unsyncPlaylistWithYouTubePlaylist(uiState.id)
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
    if (sortBottomSheetShow) {
        SortPlaylistBottomSheet(
            selectedState = uiState.filterState,
            onDismiss = { sortBottomSheetShow = false },
            onSortChanged = {
                viewModel.onUIEvent(LocalPlaylistUIEvent.ChangeFilter(it))
                sortBottomSheetShow = false
            },
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        TopAppBar(
            windowInsets =
                TopAppBarDefaults.windowInsets.exclude(
                    TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                ),
            title = {
                Text(
                    text = uiState.title,
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        echoIcons.ArrowBackIosNew,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            modifier =
                Modifier.hazeEffect(hazeState) {
                    blurEnabled = true
                    blurRadius = 24.dp
                    backgroundColor = mutedPaletteBg
                    tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                },
        )
    }
}