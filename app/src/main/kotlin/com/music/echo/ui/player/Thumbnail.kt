

package iad1tya.echo.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import iad1tya.echo.music.LocalListenTogetherManager
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.CropAlbumArtKey
import iad1tya.echo.music.constants.HidePlayerThumbnailKey
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.constants.PlayerHorizontalPadding
import iad1tya.echo.music.constants.RotatingThumbnailKey
import iad1tya.echo.music.constants.SeekExtraSeconds
import iad1tya.echo.music.constants.SwipeThumbnailKey
import iad1tya.echo.music.constants.ThumbnailCornerRadiusKey
import iad1tya.echo.music.constants.ThumbnailCornerRadius
import iad1tya.echo.music.listentogether.RoomRole
import iad1tya.echo.music.ui.component.CastButton
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.constants.CanvasThumbnailAnimationKey
import iad1tya.echo.music.canvas.TidalCanvasProvider
import iad1tya.echo.music.canvas.CanvasArtwork
import iad1tya.echo.music.extensions.metadata
import iad1tya.echo.music.ui.utils.resize
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import iad1tya.echo.music.applecanvas.AppleMusicCanvasProvider
import iad1tya.echo.music.echomusiccanvas.echomusicCanvasProvider
import java.util.Locale


@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)


@Immutable
data class MediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)


@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    
    val effectiveSize = if (isLandscape) {
        minOf(containerWidth, containerHeight) - (horizontalPadding * 2)
    } else {
        containerWidth - (horizontalPadding * 2)
    }
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = cornerRadius * 2
    )
}


@Stable
private fun getMediaItems(
    player: Player,
    swipeThumbnail: Boolean
): MediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled
    
    val currentMediaItem = try {
        player.currentMediaItem
    } catch (e: Exception) { null }
    
    val previousMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(previousIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(nextIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val items = listOfNotNull(previousMediaItem, currentMediaItem, nextMediaItem)
    val currentMediaIndex = items.indexOf(currentMediaItem)
    
    return MediaItemsData(items, currentMediaIndex)
}


@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH -> Color.White
    }
}


object CanvasArtworkPlaybackCache {
    private const val defaultMaxSize = 256
    private val map = LinkedHashMap<String, CanvasArtwork>(defaultMaxSize, 0.75f, true)
    @Volatile private var maxSize = defaultMaxSize

    @Synchronized
    fun get(mediaId: String): CanvasArtwork? {
        if (maxSize <= 0) return null
        return map[mediaId]
    }

    @Synchronized
    fun put(mediaId: String, artwork: CanvasArtwork) {
        val limit = maxSize
        if (limit <= 0) return
        if (mediaId.isBlank()) return
        map[mediaId] = artwork
        while (map.size > limit) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            }
        }
    }

    @Synchronized
    fun clear() {
        map.clear()
    }

    @Synchronized
    fun setMaxSize(value: Int) {
        maxSize = value.coerceAtLeast(0)
        if (maxSize == 0) {
            map.clear()
            return
        }
        while (map.size > maxSize) {
            val it = map.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            } else {
                break
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    
    
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    val thumbnailCornerRadius by rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 3f)
    
    
    val textBackgroundColor = getTextColor(playerBackground)
    
    
    val thumbnailLazyGridState = rememberLazyGridState()
    
    
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        swipeThumbnail,
        mediaMetadata
    ) {
        derivedStateOf {
            getMediaItems(playerConnection.player, swipeThumbnail)
        }
    }
    
    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    
    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
        ThumbnailSnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize / 2f - itemSize / 2f)
            },
            velocityThreshold = 500f
        )
    }

    
    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    
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
        val index = mediaItemsData.currentIndex
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    
    LaunchedEffect(mediaItems) {
        mediaItems.forEach { item ->
            val artworkUri = item.mediaMetadata.artworkUri?.toString()?.resize(1200, 1200) ?: return@forEach
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            SingletonImageLoader.get(context).enqueue(request)
        }
    }

    
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .graphicsLayer {
                
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        
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

        
        AnimatedVisibility(
            visible = error == null && !(playerBackground == PlayerBackgroundStyle.APPLE_MUSIC && !isLandscape),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isLandscape) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                
                if (!isLandscape) {
                    ThumbnailHeader(
                        queueTitle = queueTitle,
                        albumTitle = mediaMetadata?.album?.title,
                        textColor = textBackgroundColor
                    )
                }
                
                
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    
                    val dimensions = remember(maxWidth, maxHeight, isLandscape, thumbnailCornerRadius) {
                        calculateThumbnailDimensions(
                            containerWidth = maxWidth,
                            containerHeight = maxHeight,
                            cornerRadius = thumbnailCornerRadius.dp,
                            isLandscape = isLandscape
                        )
                    }

                    
                    val onSeekCallback = remember {
                        { direction: String, showEffect: Boolean ->
                            seekDirection = direction
                            showSeekEffect = showEffect
                        }
                    }
                    
                    
                    val isScrollEnabled by remember(swipeThumbnail) {
                        derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                    }
                    
                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                        userScrollEnabled = isScrollEnabled,
                        modifier = if (isLandscape) {
                            Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        items(
                            items = mediaItems,
                            key = { item -> 
                                item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                            }
                        ) { item ->
                            ThumbnailItem(
                                item = item,
                                dimensions = dimensions,
                                hidePlayerThumbnail = hidePlayerThumbnail,
                                cropAlbumArt = cropAlbumArt,
                                textBackgroundColor = textBackgroundColor,
                                layoutDirection = layoutDirection,
                                onSeek = onSeekCallback,
                                playerConnection = playerConnection,
                                context = context,
                                isLandscape = isLandscape,
                                isListenTogetherGuest = isListenTogetherGuest,
                                currentMediaId = mediaMetadata?.id,
                                currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                playerBackground = playerBackground
                            )
                        }
                    }
                }
            }
        }

        
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
            SeekEffectOverlay(seekDirection = seekDirection)
        }
    }
}


@Composable
private fun ThumbnailHeader(
    queueTitle: String?,
    albumTitle: String?,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
        ) {
            
            if (listenTogetherRoleState?.value != RoomRole.NONE) {
                Text(
                    text = if (listenTogetherRoleState?.value == RoomRole.HOST) "Hosting Listen Together" else "Listening Together",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            } else {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
            val playingFrom = albumTitle ?: queueTitle 
            androidx.compose.animation.AnimatedContent(
                targetState = playingFrom,
                transitionSpec = { androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut() },
                label = "NowPlayingAnimation"
            ) { text ->
                if (!text.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }


    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: iad1tya.echo.music.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    currentMediaId: String? = null,
    currentMediaThumbnail: String? = null,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val rotatingThumbnail by rememberPreference(RotatingThumbnailKey, defaultValue = false)
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isCurrentItem = item.mediaId == currentMediaId
    
    val infiniteTransition = rememberInfiniteTransition(label = "ThumbnailRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying && rotatingThumbnail && isCurrentItem) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
    var skipMultiplier by remember { mutableIntStateOf(1) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val canvasThumbnailAnimation by rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)

    Box(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                } else {
                    Modifier
                        .width(dimensions.itemWidth)
                        .fillMaxSize()
                }
            )
            .padding(horizontal = PlayerHorizontalPadding)
            .graphicsLayer {
                
                
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (isListenTogetherGuest) return@detectTapGestures

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

                        val leftBound = size.width * 0.33f
                        val rightBound = size.width * 0.66f

                        val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < leftBound) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > rightBound)
                        val isRightSide = (layoutDirection == LayoutDirection.Ltr && offset.x > rightBound) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x < leftBound)

                        if (isLeftSide) {
                            playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                            onSeek(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                        } else if (isRightSide) {
                            playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(duration))
                            onSeek(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                        } else {
                            if (playerConnection.player.playWhenReady) {
                                playerConnection.player.pause()
                            } else {
                                playerConnection.player.play()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(dimensions.thumbnailSize)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .clip(
                    if (rotatingThumbnail) {
                        MaterialShapes.Clover8Leaf.toShape()
                    } else {
                        RoundedCornerShape(dimensions.cornerRadius)
                    }
                )
                .graphicsLayer {
                    rotationZ = -rotation
                }
        ) {
            if (hidePlayerThumbnail) {
                HiddenThumbnailPlaceholder(textBackgroundColor = textBackgroundColor)
            } else {
                val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                    currentMediaThumbnail
                } else {
                    item.mediaMetadata.artworkUri?.toString()
                }

                ThumbnailImage(
                    artworkUri = artworkUriToUse?.resize(1200, 1200),
                    cropArtwork = cropAlbumArt
                )
            }
            
            if (canvasThumbnailAnimation && item.mediaId == currentMediaId && !rotatingThumbnail && playerBackground != PlayerBackgroundStyle.APPLE_MUSIC) {
                var canvasArtwork by remember(item.mediaId) { mutableStateOf<CanvasArtwork?>(null) }
                var canvasFetchInFlight by remember(item.mediaId) { mutableStateOf(false) }
                val storefront = remember {
                    val country = Locale.getDefault().country
                    if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
                }

                LaunchedEffect(item.mediaId) {
                    CanvasArtworkPlaybackCache.get(item.mediaId)?.let { cached ->
                        canvasArtwork = cached
                        return@LaunchedEffect
                    }

                    if (canvasFetchInFlight) return@LaunchedEffect
                    canvasFetchInFlight = true

                    val fetched = withContext(Dispatchers.IO) {
                        val metadata = item.metadata
                        val albumName = (metadata?.album?.title ?: item.mediaMetadata.albumTitle)?.toString()
                        val duration = metadata?.duration
                        
                        val songTitleRaw = item.mediaMetadata.title?.toString() ?: ""
                        val artistNameRaw = item.mediaMetadata.artist?.toString() ?: ""
                        
                        val songTitle = normalizeCanvasSongTitle(songTitleRaw)
                        val artistName = normalizeCanvasArtistName(artistNameRaw)
                        
                        linkedSetOf(
                            songTitle to artistName,
                            songTitleRaw to artistName,
                            songTitle to artistNameRaw,
                            songTitleRaw to artistNameRaw,
                        ).filter { (s, a) -> s.isNotBlank() && a.isNotBlank() }
                            .firstNotNullOfOrNull { (s, a) ->
                                
                                
                                if (!albumName.isNullOrBlank()) {
                                    AppleMusicCanvasProvider.getByAlbumArtist(
                                        album = albumName,
                                        artist = a,
                                        storefront = storefront
                                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return@firstNotNullOfOrNull it }
                                }

                                echomusicCanvasProvider.getBySongArtist(
                                    song = s,
                                    artist = a
                                )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                                    ?: TidalCanvasProvider.getBySongArtist(
                                        song = s,
                                        artist = a,
                                        album = albumName
                                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                                    ?: AppleMusicCanvasProvider.getBySongArtist(
                                        song = s,
                                        artist = a,
                                        album = albumName,
                                        storefront = storefront
                                    )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                            }
                    }
                    
                    
                    
                    val requestedArtist = item.mediaMetadata.artist?.toString() ?: ""
                    val requestedTitle = item.mediaMetadata.title?.toString() ?: ""
                    
                    val validated = fetched?.let { artwork ->
                        val resultArtist = artwork.artist
                        val resultName = artwork.name
                        
                        
                        val artistMatches = if (resultArtist != null && requestedArtist.isNotBlank()) {
                            val normalizedResult = normalizeCanvasArtistName(resultArtist)
                            val normalizedRequested = normalizeCanvasArtistName(requestedArtist)
                            resultArtist.contains(requestedArtist, ignoreCase = true) || 
                            requestedArtist.contains(resultArtist, ignoreCase = true) ||
                            normalizedResult.contains(normalizedRequested, ignoreCase = true) ||
                            normalizedRequested.contains(normalizedResult, ignoreCase = true)
                        } else true

                        
                        
                        
                        
                        val requestedAlbum = item.mediaMetadata.albumTitle?.toString() ?: ""
                        val canvasAlbumName = artwork.albumName
                        val canvasSongName = artwork.name

                        val titleMatches = when {
                            
                            canvasAlbumName != null && requestedAlbum.isNotBlank() -> {
                                val normalizedCanvasAlbum = normalizeCanvasSongTitle(canvasAlbumName)
                                val normalizedRequestedAlbum = normalizeCanvasSongTitle(requestedAlbum)
                                canvasAlbumName.contains(requestedAlbum, ignoreCase = true) ||
                                requestedAlbum.contains(canvasAlbumName, ignoreCase = true) ||
                                normalizedCanvasAlbum.contains(normalizedRequestedAlbum, ignoreCase = true) ||
                                normalizedRequestedAlbum.contains(normalizedCanvasAlbum, ignoreCase = true)
                            }
                            
                            canvasSongName != null && requestedTitle.isNotBlank() -> {
                                val normalizedCanvasSong = normalizeCanvasSongTitle(canvasSongName)
                                val normalizedRequestedTitle = normalizeCanvasSongTitle(requestedTitle)
                                val normalizedRequestedAlbum = if (requestedAlbum.isNotBlank()) normalizeCanvasSongTitle(requestedAlbum) else ""
                                canvasSongName.contains(requestedTitle, ignoreCase = true) ||
                                requestedTitle.contains(canvasSongName, ignoreCase = true) ||
                                normalizedCanvasSong.contains(normalizedRequestedTitle, ignoreCase = true) ||
                                normalizedRequestedTitle.contains(normalizedCanvasSong, ignoreCase = true) ||
                                (requestedAlbum.isNotBlank() && (
                                    canvasSongName.contains(requestedAlbum, ignoreCase = true) ||
                                    requestedAlbum.contains(canvasSongName, ignoreCase = true) ||
                                    normalizedCanvasSong.contains(normalizedRequestedAlbum, ignoreCase = true) ||
                                    normalizedRequestedAlbum.contains(normalizedCanvasSong, ignoreCase = true)
                                ))
                            }
                            
                            else -> true
                        }

                        if (artistMatches && titleMatches) {
                            artwork
                        } else {
                            null
                        }
                    }
                    
                    canvasArtwork = validated
                    if (validated != null) {
                        CanvasArtworkPlaybackCache.put(item.mediaId, validated)
                    }
                    canvasFetchInFlight = false
                }

                if (playerBackground != PlayerBackgroundStyle.APPLE_MUSIC) {
                    canvasArtwork?.let { artwork ->
                        CanvasArtworkPlayer(
                            primaryUrl = artwork.animated,
                            fallbackUrl = artwork.videoUrl,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_nobg),
            contentDescription = stringResource(R.string.hide_player_thumbnail),
            tint = textBackgroundColor.copy(alpha = 0.7f),
            modifier = Modifier.size(120.dp)
        )
    }
}


@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        var currentUrl by remember(artworkUri) {
            mutableStateOf(artworkUri)
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = if (cropArtwork) ContentScale.Crop else ContentScale.Fit,
            error = painterResource(R.drawable.ic_launcher_nobg),
            fallback = painterResource(R.drawable.ic_launcher_nobg),
            onError = {
                val url = currentUrl
                if (url != null && url.contains("maxresdefault.jpg")) {
                    currentUrl = url.replace("maxresdefault.jpg", "hqdefault.jpg")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = seekDirection,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}

internal fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}

internal fun splitAndNormalizeArtists(raw: String): List<String> {
    return raw.split(
        Regex(
            "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
            RegexOption.IGNORE_CASE,
        )
    ).map { it.replace(Regex("\\s+"), " ").trim() }
     .filter { it.isNotEmpty() }
}
