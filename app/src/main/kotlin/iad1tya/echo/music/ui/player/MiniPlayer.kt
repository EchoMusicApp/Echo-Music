package iad1tya.echo.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.MiniPlayerHeight
import iad1tya.echo.music.constants.SwipeSensitivityKey
import iad1tya.echo.music.constants.ThumbnailCornerRadius
import iad1tya.echo.music.constants.UseNewMiniPlayerDesignKey
import iad1tya.echo.music.db.entities.ArtistEntity
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val useNewMiniPlayerDesign by rememberPreference(UseNewMiniPlayerDesignKey, false)

    if (useNewMiniPlayerDesign) {
        NewMiniPlayer(
            position = position,
            duration = duration,
            modifier = modifier,
            pureBlack = pureBlack
        )
    } else {
        // NEW: Wrap LegacyMiniPlayer in a Box to allow alignment on tablet landscape.
        // The outer Box fills the width, providing a container for the inner player to be aligned within.
        Box(modifier = modifier.fillMaxWidth()) {
            LegacyMiniPlayer(
                position = position,
                duration = duration,
                // NEW: Align the player to the end if it's a tablet in landscape.
                // This modifier is passed to LegacyMiniPlayer and applied to its root Box.
                modifier = if (
                    LocalConfiguration.current.screenWidthDp >= 600 &&
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                ) {
                    Modifier.align(Alignment.CenterEnd)
                } else {
                    Modifier.align(Alignment.Center)
                },
                pureBlack = pureBlack
            )
        }
    }
}

@Composable
private fun NewMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    val currentView = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(iad1tya.echo.music.constants.SwipeThumbnailKey, true)

    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 600 &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    // Optimized animation spec for smoother, more responsive feel
    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.1f
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.0f else 0.4f,
        label = "overlay_alpha",
        animationSpec = animationSpec
    )

    /**
     * Calculates the auto-swipe threshold based on swipe sensitivity.
     * The formula uses a sigmoid function to determine the threshold dynamically.
     * Constants:
     * - -11.44748: Controls the steepness of the sigmoid curve.
     * - 9.04945: Adjusts the midpoint of the curve.
     * - 600: Base threshold value in pixels.
     *
     * @param swipeSensitivity The sensitivity value (typically between 0 and 1).
     * @return The calculated auto-swipe threshold in pixels.
     */
    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            // Move the swipe detection to the outer box to affect the entire box
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                    velocity > velocityThreshold
                                ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        // Main MiniPlayer box that moves with swipe
        Box(
            modifier = Modifier
                .then(
                    if (isTabletLandscape) {
                        Modifier
                            .width(500.dp)
                            .align(Alignment.CenterEnd) // Right align
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .height(60.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(50))
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                // Play/Pause button with circular progress indicator (left side)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Circular progress indicator around the play button
                    if (duration > 0) {
                        CircularProgressIndicator(
                            progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    // Play/Pause button with thumbnail background
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            }
                    ) {
                        // Thumbnail background
                        mediaMetadata?.let { metadata ->
                            AsyncImage(
                                model = metadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        // Semi-transparent overlay for better icon visibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.Black.copy(alpha = overlayAlpha),
                                    shape = CircleShape
                                )
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = playbackState == Player.STATE_ENDED || !isPlaying,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (playbackState == Player.STATE_ENDED) {
                                        R.drawable.replay
                                    } else {
                                        R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Song info - takes most space in the middle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    mediaMetadata?.let { metadata ->
                        AnimatedContent(
                            targetState = metadata.title,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { title ->
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                            )
                        }

                        if (metadata.artists.any { it.name.isNotBlank() }) {
                            AnimatedContent(
                                targetState = metadata.artists.joinToString { it.name },
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "",
                            ) { artists ->
                                Text(
                                    text = artists,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                                )
                            }
                        }

                        // Error indicator
                        androidx.compose.animation.AnimatedVisibility(
                            visible = error != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Text(
                                text = "Error playing",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Subscribe/Subscribed button
                mediaMetadata?.let { metadata ->
                    metadata.artists.firstOrNull()?.id?.let { artistId ->
                        val libraryArtist by database.artist(artistId).collectAsState(initial = null)
                        val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (isSubscribed)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (isSubscribed)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else
                                        Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    database.transaction {
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            update(artist.toggleLike())
                                        } else {
                                            metadata.artists.firstOrNull()?.let { artistInfo ->
                                                insert(
                                                    ArtistEntity(
                                                        id = artistInfo.id ?: "",
                                                        name = artistInfo.name,
                                                        channelId = null,
                                                        thumbnailUrl = null,
                                                    ).toggleLike()
                                                )
                                            }
                                        }
                                    }
                                }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                                ),
                                contentDescription = null,
                                tint = if (isSubscribed)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Favorite button (right side)
                mediaMetadata?.let { metadata ->
                    val librarySong by database.song(metadata.id).collectAsState(initial = null)
                    val isLiked = librarySong?.song?.liked == true

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = if (isLiked)
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .background(
                                color = if (isLiked)
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                else 
                                    Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                playerConnection.service.toggleLike()
                            }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (isLiked)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    val currentView = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(iad1tya.echo.music.constants.SwipeThumbnailKey, true)

    // NEW: Get screen configuration to determine if it's a tablet in landscape mode.
    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 600 &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Extract gradient colors from album art
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        mediaMetadata?.thumbnailUrl?.let { url ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                result.image?.let { image ->
                    val bitmap = image.toBitmap()
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(32)
                        .generate()
                    gradientColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = Color.Black.toArgb()
                    )
                }
            } catch (e: Exception) {
                gradientColors = emptyList()
            }
        }
    }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .then(
                if (isTabletLandscape) {
                    Modifier.width(500.dp)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(64.dp)
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (gradientColors.isNotEmpty()) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = gradientColors
                        )
                    )
                } else {
                    Modifier.background(
                        if (pureBlack)
                            Color.Black
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            )
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                    velocity > velocityThreshold
                                ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        LinearProgressIndicator(
            progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            drawStopIndicator = {},
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .padding(end = 12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    LegacyMiniMediaInfo(
                        mediaMetadata = it,
                        error = error,
                        pureBlack = pureBlack,
                        hasGradient = gradientColors.isNotEmpty(),
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            // Previous button
            IconButton(
                enabled = canSkipPrevious,
                onClick = playerConnection::seekToPrevious,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = if (gradientColors.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Play/Pause button
            IconButton(
                onClick = {
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(
                        if (playbackState == Player.STATE_ENDED) {
                            R.drawable.replay
                        } else if (isPlaying) {
                            R.drawable.pause
                        } else {
                            R.drawable.play
                        },
                    ),
                    contentDescription = null,
                    tint = if (gradientColors.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Next button
            IconButton(
                enabled = canSkipNext,
                onClick = playerConnection::seekToNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = if (gradientColors.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Visual indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LegacyMiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: PlaybackException?,
    pureBlack: Boolean,
    hasGradient: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(48.dp)
                .clip(CircleShape)
        ) {
            // Simple background instead of expensive blur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Main thumbnail
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = if (pureBlack) Color.Black else Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { title ->
                Text(
                    text = title,
                    color = if (hasGradient) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }

            if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                AnimatedContent(
                    targetState = mediaMetadata.artists.joinToString { it.name },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "",
                ) { artists ->
                    Text(
                        text = artists,
                        color = if (hasGradient) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
