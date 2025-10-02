package iad1tya.echo.music.ui.screen

import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import iad1tya.echo.music.R
import iad1tya.echo.music.data.db.entities.SongEntity
import iad1tya.echo.music.extension.connectArtists
import iad1tya.echo.music.extension.getColorFromPalette
import iad1tya.echo.music.ui.component.ExplicitBadge
import iad1tya.echo.music.ui.component.HeartCheckBox
import iad1tya.echo.music.ui.component.PlayPauseButton
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.viewModel.SharedViewModel
import iad1tya.echo.music.viewModel.UIEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
@UnstableApi
fun MiniPlayer(
    modifier: Modifier,
    sharedViewModel: SharedViewModel = koinInject(),
    onClose: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit = onClick, // Add swipe up callback, defaults to onClick
    onDragProgress: (Float) -> Unit = {}, // Real-time drag progress callback
    onDragEnd: (Float) -> Unit = { if (it >= 0.3f) onSwipeUp() }, // Drag end callback with final progress
    showPreviousTrackButton: Boolean = true,
) {
    val (songEntity, setSongEntity) =
        remember {
            mutableStateOf<SongEntity?>(null)
        }
    val (liked, setLiked) =
        remember {
            mutableStateOf(false)
        }
    val (isPlaying, setIsPlaying) =
        remember {
            mutableStateOf(false)
        }
    val (progress, setProgress) =
        remember {
            mutableFloatStateOf(0f)
        }
    val (isCrossfading, setIsCrossfading) =
        remember {
            mutableStateOf(false)
        }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "",
    )

    // Palette state
    val paletteState = rememberPaletteState()
    val background =
        remember {
            Animatable(Color.DarkGray)
        }

    val offsetX = remember { Animatable(initialValue = 0f) }
    val offsetY = remember { Animatable(0f) }
    
    // Visual feedback for swipe gestures
    val swipeAlpha by animateFloatAsState(
        targetValue = if (kotlin.math.abs(offsetX.value) > 10f) 0.3f else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "swipe_alpha",
    )

    var loading by rememberSaveable {
        mutableStateOf(true)
    }

    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    
    // Swipe cooldown to prevent rapid successive swipes
    var lastSwipeTime by remember { mutableStateOf(0L) }
    val swipeCooldownMs = 200L // 200ms cooldown between swipes for better responsiveness

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null) {
            paletteState.generate(bm)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                background.animateTo(it.getColorFromPalette())
            }
    }

    LaunchedEffect(key1 = true) {
        val job1 =
            launch {
                sharedViewModel.nowPlayingState.collect { item ->
                    if (item != null) {
                        setSongEntity(item.songEntity)
                    }
                }
            }
        val job2 =
            launch {
                sharedViewModel.controllerState.collectLatest { state ->
                    setLiked(state.isLiked)
                    setIsPlaying(state.isPlaying)
                    setIsCrossfading(state.isCrossfading)
                }
            }
        val job4 =
            launch {
                sharedViewModel.timeline.collect { timeline ->
                    loading = timeline.loading
                    val prog =
                        if (timeline.total > 0L && timeline.current >= 0L) {
                            timeline.current.toFloat() / timeline.total
                        } else {
                            0f
                        }
                    setProgress(prog)
                }
            }
        job1.join()
        job2.join()
        job4.join()
    }

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(10.dp),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = background.value,
            ),
        modifier =
            modifier
                .clipToBounds()
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .clickable(
                    onClick = onClick,
                )
                .pointerInput(Unit) {
                    // Vertical drag for closing mini player (down) and opening full screen (up)
                    val maxDragDistance = 200f // Maximum drag distance for full transition
                    
                    detectVerticalDragGestures(
                        onDragStart = {
                            Log.d("MiniPlayer", "Drag started")
                        },
                        onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                            coroutineScope.launch {
                                change.consume()
                                val newOffsetY = offsetY.value + 2 * dragAmount
                                offsetY.animateTo(newOffsetY)
                                
                                // Calculate drag progress for upward swipes (0f to 1f)
                                val progress = if (newOffsetY < 0) {
                                    (-newOffsetY / maxDragDistance).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                
                                // Provide real-time progress updates
                                onDragProgress(progress)
                                
                                Log.d("MiniPlayer", "Dragged ${offsetY.value}, progress: $progress")
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetY.animateTo(0f)
                                onDragProgress(0f)
                            }
                        },
                        onDragEnd = {
                            Log.d("MiniPlayer", "Drag ended at ${offsetY.value}")
                            coroutineScope.launch {
                                val finalProgress = if (offsetY.value < 0) {
                                    (-offsetY.value / maxDragDistance).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                
                                when {
                                    // Swipe up to open full screen (negative offset)
                                    offsetY.value < -70 -> {
                                        Log.d("MiniPlayer", "Swipe up detected - progress: $finalProgress")
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                        } catch (e: Exception) {
                                            Log.e("MiniPlayer", "Error triggering haptic feedback: ${e.message}")
                                        }
                                        offsetY.animateTo(0f)
                                        onDragEnd(finalProgress)
                                    }
                                    // Swipe down to close (positive offset)
                                    offsetY.value > 70 -> {
                                        Log.d("MiniPlayer", "Swipe down detected - closing mini player")
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                        } catch (e: Exception) {
                                            Log.e("MiniPlayer", "Error triggering haptic feedback: ${e.message}")
                                        }
                                        onClose()
                                    }
                                    // Return to original position if swipe wasn't far enough
                                    else -> {
                                        offsetY.animateTo(0f)
                                        onDragEnd(finalProgress)
                                    }
                                }
                            }
                        },
                    )
                }.pointerInput(Unit) {
                    // Horizontal drag for track navigation
                    detectHorizontalDragGestures(
                        onDragStart = {
                            Log.d("MiniPlayer", "Horizontal drag started")
                        },
                        onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                            coroutineScope.launch {
                                change.consume()
                                offsetX.animateTo(offsetX.value + dragAmount * 0.8f) // Increase sensitivity
                                Log.d("MiniPlayer", "Horizontal drag: ${offsetX.value}")
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                            }
                        },
                        onDragEnd = {
                            Log.d("MiniPlayer", "Horizontal drag ended")
                            coroutineScope.launch {
                                val currentTime = System.currentTimeMillis()
                                val threshold = 25f // More sensitive swipe distance
                                
                                // Check cooldown
                                if (currentTime - lastSwipeTime < swipeCooldownMs) {
                                    Log.d("MiniPlayer", "Swipe ignored due to cooldown")
                                    offsetX.animateTo(0f)
                                    return@launch
                                }
                                
                                when {
                                    offsetX.value > threshold -> {
                                        // Swipe right - Previous track
                                        Log.d("MiniPlayer", "Swipe right - Previous track")
                                        lastSwipeTime = currentTime
                                        
                                        // Add haptic feedback
                                        try {
                                            (context as? android.app.Activity)?.window?.decorView?.performHapticFeedback(
                                                HapticFeedbackConstants.VIRTUAL_KEY
                                            )
                                        } catch (e: Exception) {
                                            Log.e("MiniPlayer", "Error providing haptic feedback: ${e.message}")
                                        }
                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                    }
                                    offsetX.value < -threshold -> {
                                        // Swipe left - Next track
                                        Log.d("MiniPlayer", "Swipe left - Next track")
                                        lastSwipeTime = currentTime
                                        
                                        // Add haptic feedback
                                        try {
                                            (context as? android.app.Activity)?.window?.decorView?.performHapticFeedback(
                                                HapticFeedbackConstants.VIRTUAL_KEY
                                            )
                                        } catch (e: Exception) {
                                            Log.e("MiniPlayer", "Error providing haptic feedback: ${e.message}")
                                        }
                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                    }
                                }
                                offsetX.animateTo(0f)
                            }
                        },
                    )
                },
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            // Swipe direction indicators
            if (swipeAlpha > 0f) {
                // Previous track indicator (right side)
                if (offsetX.value > 10f) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = swipeAlpha),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(24.dp)
                    )
                }
                // Next track indicator (left side)
                if (offsetX.value < -10f) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_skip_next_24),
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = swipeAlpha),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(24.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.size(12.dp))
                Box(modifier = Modifier.weight(1F)) {
                    Row(
                        modifier =
                            Modifier
                                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(songEntity?.thumbnails)
                                    .crossfade(550)
                                    .build(),
                            placeholder = painterResource(R.drawable.echo_logo),
                            error = painterResource(R.drawable.echo_logo),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            onSuccess = {
                                bitmap =
                                    it.result.image
                                        .toBitmap()
                                        .asImageBitmap()
                            },
                            modifier =
                                Modifier
                                    .size(52.dp)
                                    .align(Alignment.CenterVertically)
                                    .clip(
                                        RoundedCornerShape(8.dp),
                                    ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedContent(
                            targetState = songEntity,
                            modifier = Modifier.weight(1F).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                            transitionSpec = {
                                // Compare the incoming number with the previous number.
                                if (targetState != initialState) {
                                    // If the target number is larger, it slides up and fades in
                                    // while the initial (smaller) number slides up and fades out.
                                    (
                                        slideInHorizontally { width ->
                                            width
                                        } + fadeIn()
                                    ).togetherWith(
                                        slideOutHorizontally { width -> +width } + fadeOut(),
                                    )
                                } else {
                                    // If the target number is smaller, it slides down and fades in
                                    // while the initial number slides down and fades out.
                                    (
                                        slideInHorizontally { width ->
                                            +width
                                        } + fadeIn()
                                    ).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut(),
                                    )
                                }.using(
                                    // Disable clipping since the faded slide-in/out should
                                    // be displayed out of bounds.
                                    SizeTransform(clip = false),
                                )
                            },
                        ) { target ->
                            if (target != null) {
                                Column(
                                    Modifier
                                        .wrapContentHeight()
                                        .align(Alignment.CenterVertically),
                                ) {
                                    Text(
                                        text = (songEntity?.title ?: "").toString(),
                                        style = typo.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(
                                                    align = Alignment.CenterVertically,
                                                ).basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                ).focusable(),
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.animation.AnimatedVisibility(visible = songEntity?.isExplicit == true) {
                                            ExplicitBadge(
                                                modifier =
                                                    Modifier
                                                        .size(20.dp)
                                                        .padding(end = 4.dp)
                                                        .weight(1f),
                                            )
                                        }
                                        Text(
                                            text = (songEntity?.artistName?.connectArtists() ?: "").toString(),
                                            style = typo.bodySmall,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                                    .wrapContentHeight(
                                                        align = Alignment.CenterVertically,
                                                    ).basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                    ).focusable(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Previous track button
                if (showPreviousTrackButton) {
                    FilledTonalIconButton(
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = Color.Transparent,
                        ),
                        modifier = Modifier.size(56.dp),
                        onClick = {
                            sharedViewModel.onUIEvent(UIEvent.Previous)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            tint = Color.White,
                            contentDescription = "Previous Track",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Crossfade(targetState = loading, label = "") {
                    if (it) {
                        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.LightGray,
                                strokeWidth = 3.dp,
                            )
                        }
                    } else {
                        PlayPauseButton(isPlaying = isPlaying, modifier = Modifier.size(64.dp)) {
                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalIconButton(
                    colors = IconButtonDefaults.iconButtonColors().copy(
                        containerColor = Color.Transparent,
                    ),
                    modifier = Modifier.size(56.dp),
                    onClick = {
                        sharedViewModel.onUIEvent(UIEvent.Next)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        tint = Color.White,
                        contentDescription = "Next Track",
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(
                modifier =
                    Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(
                            horizontal = 10.dp,
                        ).align(Alignment.BottomCenter),
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(4.dp),
                            ),
                    color = MaterialTheme.colorScheme.primary, // Use Material You primary color
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // Use Material You surface variant
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
        }
    }
}