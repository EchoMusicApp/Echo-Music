package iad1tya.echo.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import android.app.Activity
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import androidx.compose.material3.Icon
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.constants.PlayerHorizontalPadding
import iad1tya.echo.music.constants.SeekExtraSeconds
import iad1tya.echo.music.constants.ShowLyricsKey
import iad1tya.echo.music.constants.SwipeThumbnailKey
import iad1tya.echo.music.constants.TapAlbumArtForLyricsKey
import iad1tya.echo.music.constants.ThumbnailCornerRadius
import iad1tya.echo.music.constants.ThumbnailCornerRadiusKey
import iad1tya.echo.music.constants.CropAlbumArtKey
import iad1tya.echo.music.constants.ArchiveTuneCanvasKey
import iad1tya.echo.music.constants.HidePlayerThumbnailKey
import iad1tya.echo.music.constants.MaxCanvasCacheSizeKey
import iad1tya.echo.music.constants.PlayerDesignStyle
import iad1tya.echo.music.constants.PlayerDesignStyleKey
import iad1tya.echo.music.constants.DoubleTapToLikeKey
import iad1tya.echo.music.constants.GestureDoubleTapSeekKey
import iad1tya.echo.music.constants.GestureVerticalControlsKey
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: Boolean = true, // Add parameter to control swipe based on player state
    onToggleLyrics: () -> Unit = {}, // Callback to toggle lyrics
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val currentView = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember(context) { context.getSystemService(AUDIO_SERVICE) as? AudioManager }

    // States
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val tapAlbumArtForLyrics by rememberPreference(TapAlbumArtForLyricsKey, false)
    val doubleTapToLike by rememberPreference(DoubleTapToLikeKey, false)
    val doubleTapSeekEnabled by rememberPreference(GestureDoubleTapSeekKey, true)
    val verticalGesturesEnabled by rememberPreference(GestureVerticalControlsKey, false)
    val thumbnailCornerRadius by rememberPreference(ThumbnailCornerRadiusKey, 3f)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val canvasThumbnailAnimation by rememberPreference(ArchiveTuneCanvasKey, false)
    val playerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V4,
    )
    val (maxCanvasCacheSize, _) = rememberPreference(
        key = MaxCanvasCacheSizeKey,
        defaultValue = 256,
    )
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    
    // Player background style for consistent theming
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BLUR
    )
    
    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.GRADIENT -> Color.White
        PlayerBackgroundStyle.CUSTOM -> Color.White
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.COLORING -> Color.White
        PlayerBackgroundStyle.BLUR_GRADIENT -> Color.White
        PlayerBackgroundStyle.GLOW -> Color.White
        PlayerBackgroundStyle.GLOW_ANIMATED -> Color.White
    }

    LaunchedEffect(maxCanvasCacheSize) {
        iad1tya.echo.music.canvas.CanvasArtworkPlaybackCache.setMaxSize(maxCanvasCacheSize)
    }
    
    // Grid state
    val thumbnailLazyGridState = rememberLazyGridState()
    
    // Create a playlist using correct shuffle-aware logic
    val timeline = playerConnection.player.currentTimeline
    val currentIndex = playerConnection.player.currentMediaItemIndex
    val shuffleModeEnabled = playerConnection.player.shuffleModeEnabled
    val previousMediaMetadata = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try {
                playerConnection.player.getMediaItemAt(previousIndex)
            } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaMetadata = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try {
                playerConnection.player.getMediaItemAt(nextIndex)
            } catch (e: Exception) { null }
        } else null
    } else null

    val currentMediaItem = try {
        playerConnection.player.currentMediaItem
    } catch (e: Exception) { null }

    val mediaItems = listOfNotNull(previousMediaMetadata, currentMediaItem, nextMediaMetadata)
    val currentMediaIndex = mediaItems.indexOf(currentMediaItem)

    // OuterTune Snap behavior
    val horizontalLazyGridItemWidthFactor = 1f
    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        SnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    // Current item tracking
    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    // Handle swipe to change song
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    // Update position when song changes
    LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex) {
        val index = mediaItems.indexOf(currentMediaItem)
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    // Seek on double tap
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }
    val layoutDirection = LocalLayoutDirection.current

    Box(modifier = modifier) {
        // Error view
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

        // Main thumbnail view
        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Echo header removed by user request
                // Column(
                //     horizontalAlignment = Alignment.CenterHorizontally,
                //     modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                // ) {
                //     Text(
                //         text = "Echo Music",
                //         style = MaterialTheme.typography.titleLarge.copy(
                //             fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                //             fontWeight = FontWeight.Bold,
                //             fontSize = 20.sp
                //         ),
                //         color = textBackgroundColor,
                //     )
                // }
                
                // Thumbnail content
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp)
                ) {
                    val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
                    val containerMaxWidth = maxWidth

                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                        userScrollEnabled = swipeThumbnail && isPlayerExpanded, // Only allow swipe when player is expanded
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = mediaItems,
                            key = { item -> 
                                // Use mediaId with stable fallback to avoid recomposition issues
                                item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                            }
                        ) { item ->
                            val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
                            var skipMultiplier by remember { mutableIntStateOf(1) }
                            var lastTapTime by remember { mutableLongStateOf(0L) }

            val isCurrentItem = item.mediaId == (currentMediaItem?.mediaId ?: "")
            val shouldAnimateCanvas =
                canvasThumbnailAnimation &&
                    playerDesignStyle != PlayerDesignStyle.V7 &&
                    isCurrentItem
            var canvasArtwork by remember(item.mediaId) { mutableStateOf<iad1tya.echo.music.canvas.CanvasArtwork?>(null) }
            val canvasFetchInFlight = remember(item.mediaId) { mutableStateOf(false) }
            val storefront = remember {
                val country = java.util.Locale.getDefault().country
                if (country.length == 2) country.lowercase(java.util.Locale.ROOT) else "us"
            }

            LaunchedEffect(shouldAnimateCanvas) {
                if (!shouldAnimateCanvas) {
                    canvasArtwork = null
                    canvasFetchInFlight.value = false
                }
            }

            if (shouldAnimateCanvas) {
                LaunchedEffect(item.mediaId, item.mediaMetadata.title, item.mediaMetadata.artist, item.mediaMetadata.subtitle) {
                    iad1tya.echo.music.canvas.CanvasArtworkPlaybackCache.get(item.mediaId)?.let { cached ->
                        canvasArtwork = cached
                        return@LaunchedEffect
                    }
                    if (canvasFetchInFlight.value) return@LaunchedEffect
                    canvasFetchInFlight.value = true
                    val fetched = withContext(Dispatchers.IO) {
                        val songTitleRaw = item.mediaMetadata.title?.toString() ?: ""
                        val artistNameRaw =
                            item.mediaMetadata.artist?.toString()
                                ?.takeIf { it.isNotBlank() }
                                ?: item.mediaMetadata.subtitle?.toString().orEmpty()
                        val songTitle = normalizeCanvasSongTitle(songTitleRaw)
                        val artistName = normalizeCanvasArtistName(artistNameRaw)
                        linkedSetOf(
                            songTitle to artistName,
                            songTitleRaw to artistName,
                            songTitle to artistNameRaw,
                            songTitleRaw to artistNameRaw,
                        ).filter { (s, a) -> s.isNotBlank() && a.isNotBlank() }
                            .firstNotNullOfOrNull { (s, a) ->
                                iad1tya.echo.music.canvas.ArchiveTuneCanvas.getBySongArtist(
                                    song = s,
                                    artist = a,
                                    storefront = storefront
                                )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                            }
                    }
                    canvasArtwork = fetched
                    if (fetched != null) {
                        iad1tya.echo.music.canvas.CanvasArtworkPlaybackCache.put(item.mediaId, fetched)
                    }
                    canvasFetchInFlight.value = false
                }
            }
                            Box(
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .fillMaxSize()
                                    .padding(horizontal = PlayerHorizontalPadding)
                                    .pointerInput(doubleTapToLike, doubleTapSeekEnabled, tapAlbumArtForLyrics) {
                                        detectTapGestures(
                                            onTap = {
                                                if (tapAlbumArtForLyrics) {
                                                    onToggleLyrics()
                                                }
                                            },
                                            onDoubleTap = { offset ->
                                                if (doubleTapToLike) {
                                                    playerConnection.toggleLike()
                                                    return@detectTapGestures
                                                }
                                                if (!doubleTapSeekEnabled) return@detectTapGestures

                                                val currentPosition = playerConnection.player.currentPosition
                                                val duration = playerConnection.player.duration

                                                val now = System.currentTimeMillis()
                                                if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                                                    skipMultiplier++
                                                } else {
                                                    skipMultiplier = 1
                                                }
                                                lastTapTime = now

                                                val skipAmount = 5000 * skipMultiplier

                                                if ((layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                                    (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)
                                                ) {
                                                    playerConnection.player.seekTo(
                                                        (currentPosition - skipAmount).coerceAtLeast(0)
                                                    )
                                                    seekDirection =
                                                        context.getString(R.string.seek_backward_dynamic, skipAmount / 1000)
                                                } else {
                                                    playerConnection.player.seekTo(
                                                        (currentPosition + skipAmount).coerceAtMost(duration)
                                                    )
                                                    seekDirection = context.getString(R.string.seek_forward_dynamic, skipAmount / 1000)
                                                }

                                                showSeekEffect = true
                                            }
                                        )
                                    }
                                    .pointerInput(verticalGesturesEnabled) {
                                        if (!verticalGesturesEnabled) return@pointerInput
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val isLeftSide = change.position.x < size.width / 2f
                                            if (isLeftSide) {
                                                val activity = context as? Activity
                                                val window = activity?.window ?: return@detectVerticalDragGestures
                                                val currentBrightness = if (window.attributes.screenBrightness >= 0f) {
                                                    window.attributes.screenBrightness
                                                } else {
                                                    0.5f
                                                }
                                                val updated = (currentBrightness - dragAmount / 1400f).coerceIn(0.05f, 1f)
                                                window.attributes = window.attributes.apply {
                                                    screenBrightness = updated
                                                }
                                            } else {
                                                val manager = audioManager ?: return@detectVerticalDragGestures
                                                val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                                val currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val delta = (-dragAmount / 80f).toInt()
                                                val target = (currentVolume + delta).coerceIn(0, maxVolume)
                                                manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!hidePlayerThumbnail) {
                                Box(
                                    modifier = Modifier
                                        .size(containerMaxWidth - (PlayerHorizontalPadding * 2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Outer box: scalloped shape rotates via graphicsLayer clip
                                    // Inner content: counter-rotates so the image/video stays upright
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                                    ) {
                                        // Content
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            AsyncImage(
                                                model = coil3.request.ImageRequest.Builder(LocalContext.current)
                                                    .data(item.mediaMetadata.artworkUri?.toString())
                                                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                                    .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = if (canvasThumbnailAnimation || cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                                error = painterResource(R.drawable.echo_logo),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (shouldAnimateCanvas) {
                                                canvasArtwork?.let { artwork ->
                                                    val isPlayingCanvas by playerConnection.isPlaying.collectAsState()
                                                    CanvasArtworkPlayer(
                                                        primaryUrl = artwork.animated,
                                                        fallbackUrl = artwork.videoUrl,
                                                        isPlaying = isPlayingCanvas,
                                                        modifier = Modifier.fillMaxSize()
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
            }
        }

        // Seek effect
        LaunchedEffect(showSeekEffect) {
            if (showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}

/*
 * Copyright (C) OuterTune Project
 * Custom SnapLayoutInfoProvider idea belongs to OuterTune
 */

// SnapLayoutInfoProvider
@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float = { layoutSize, itemSize ->
        (layoutSize / 2f - itemSize / 2f)
    },
    velocityThreshold: Float = 1000f,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {
    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f
    override fun calculateSnapOffset(velocity: Float): Float {
        val bounds = calculateSnappingOffsetBounds()

        // Only snap when velocity exceeds threshold
        if (abs(velocity) < velocityThreshold) {
            if (abs(bounds.start) < abs(bounds.endInclusive))
                return bounds.start

            return bounds.endInclusive
        }

        return when {
            velocity < 0 -> bounds.start
            velocity > 0 -> bounds.endInclusive
            else -> 0f
        }
    }

    fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset = calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return lowerBoundOffset.rangeTo(upperBoundOffset)
    }
}

fun calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float,
): Float {
    val containerSize =
        layoutInfo.singleAxisViewportSize - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding

    val desiredDistance = positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
    val itemCurrentPosition = item.offset.x.toFloat()

    return itemCurrentPosition - desiredDistance
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

private fun normalizeCanvasSongTitle(raw: String): String {
    val stripped = raw
        .replace(Regex("\\s*\\[[^]]*]"), "")
        .replace(Regex("\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return stripped.trim('-').replace(Regex("\\s+"), " ").trim()
}

private fun normalizeCanvasArtistName(raw: String): String {
    val first = raw.split(
        Regex("(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)", RegexOption.IGNORE_CASE),
        limit = 2
    ).firstOrNull().orEmpty()
    return first.replace(Regex("\\s+"), " ").trim()
}
