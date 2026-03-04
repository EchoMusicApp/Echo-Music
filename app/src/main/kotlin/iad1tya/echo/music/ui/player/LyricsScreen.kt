package iad1tya.echo.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.constants.PlayerHorizontalPadding
import iad1tya.echo.music.constants.SliderStyle
import iad1tya.echo.music.constants.SliderStyleKey
import iad1tya.echo.music.db.entities.LyricsEntity
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.extensions.toggleRepeatMode
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.ui.component.Lyrics
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.PlayerSliderTrack
import iad1tya.echo.music.ui.component.BigSeekBar
import androidx.navigation.NavController
import me.saket.squiggles.SquigglySlider
import iad1tya.echo.music.ui.menu.LyricsMenu
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import iad1tya.echo.music.ui.theme.PlayerSliderColors
import iad1tya.echo.music.utils.rememberEnumPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import iad1tya.echo.music.utils.makeTimeString
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    onBackClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 1f, // Add this parameter
    isVisible: Boolean = true // Add this parameter
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)


    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            withContext(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        iad1tya.echo.music.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                    
                    // Check if lyrics were added manually while we were fetching
                    if (database.lyrics(mediaMetadata.id).first() == null) {
                        database.query {
                            upsert(LyricsEntity(mediaMetadata.id, lyrics))
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = isSystemInDarkTheme

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata.id, playerBackground) {
        if ((playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) && mediaMetadata.thumbnailUrl != null) {
            val cacheKey = if (playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) "glow_${mediaMetadata.id}" else mediaMetadata.id
            val cachedColors = gradientColorsCache[cacheKey]
            if (cachedColors != null) {
                gradientColors = cachedColors
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(mediaMetadata.thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .memoryCacheKey("gradient_${mediaMetadata.id}")
                    .build()
                val result = runCatching { context.imageLoader.execute(request).image }.getOrNull()
                if (result != null) {
                    val bitmap = result.toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(8)
                            .resizeBitmapArea(100 * 100)
                            .generate()
                    }
                    val extractedColors = if (playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
                        listOfNotNull(
                            palette.getVibrantColor(fallbackColor).let { Color(it) },
                            palette.getLightVibrantColor(fallbackColor).let { Color(it) },
                            palette.getDarkVibrantColor(fallbackColor).let { Color(it) },
                            palette.getMutedColor(fallbackColor).let { Color(it) },
                            palette.getLightMutedColor(fallbackColor).let { Color(it) },
                            palette.getDarkMutedColor(fallbackColor).let { Color(it) }
                        ).distinct()
                    } else {
                        PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                    }
                    gradientColorsCache[cacheKey] = extractedColors
                    withContext(Dispatchers.Main) { gradientColors = extractedColors }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.GRADIENT -> Color.White
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GLOW_ANIMATED -> Color.White
    }

    val iconButtonColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
        PlayerBackgroundStyle.GRADIENT -> Color.Black
        PlayerBackgroundStyle.BLUR -> Color.Black
        PlayerBackgroundStyle.GLOW_ANIMATED -> Color.Black
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(500)
                position = player.currentPosition
                duration = player.duration
            }
        }
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize().alpha(backgroundAlpha)) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    AnimatedContent(
                        targetState = mediaMetadata.thumbnailUrl,
                        transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                        label = "blurBackground"
                    ) { thumbnailUrl ->
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbnailUrl)
                                    .size(100, 100)
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(150.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                        }
                    }
                }
                PlayerBackgroundStyle.GRADIENT -> {
                    AnimatedContent(
                        targetState = gradientColors,
                        transitionSpec = {
                            fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                        },
                        label = "gradientBackground"
                    ) { colors ->
                        if (colors.isNotEmpty()) {
                            val gradientColorStops = if (colors.size >= 3) {
                                arrayOf(
                                    0.0f to colors[0],
                                    0.5f to colors[1],
                                    1.0f to colors[2]
                                )
                            } else {
                                arrayOf(
                                    0.0f to colors[0],
                                    0.6f to colors[0].copy(alpha = 0.7f),
                                    1.0f to Color.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
                PlayerBackgroundStyle.GLOW_ANIMATED -> {
                    AnimatedContent(
                        targetState = gradientColors,
                        transitionSpec = { fadeIn(tween(1200)) togetherWith fadeOut(tween(1200)) },
                        label = "GlowAnimatedContent"
                    ) { colors ->
                        if (colors.isNotEmpty()) {
                            val infiniteTransition = rememberInfiniteTransition(label = "GlowAnimation")
                            val glowProgress by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(20000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "glowProgress"
                            )
                            fun rotatedColorAt(index: Int): Color {
                                val size = colors.size
                                val idx = index.toFloat() + glowProgress * size
                                val a = kotlin.math.floor(idx).toInt() % size
                                val b = (a + 1) % size
                                val frac = idx - kotlin.math.floor(idx)
                                return lerp(
                                    colors.getOrElse(a) { Color.DarkGray },
                                    colors.getOrElse(b) { Color.DarkGray },
                                    frac
                                )
                            }
                            fun oscillate(min: Float, max: Float, phase: Float, speed: Float = 1f): Float {
                                val v = kotlin.math.sin(2.0 * kotlin.math.PI * (glowProgress * speed + phase)).toFloat()
                                return min + (max - min) * ((v + 1f) * 0.5f)
                            }
                            val color1 = rotatedColorAt(0)
                            val color2 = rotatedColorAt(1)
                            val color3 = rotatedColorAt(2)
                            val color4 = rotatedColorAt(3)
                            val color5 = rotatedColorAt(4)
                            val color6 = rotatedColorAt(5)
                            val o1x = oscillate(0.0f, 1.0f, 0.00f); val o1y = oscillate(0.0f, 0.5f, 0.07f); val r1 = oscillate(0.8f, 1.6f, 0.12f)
                            val o2x = oscillate(1.0f, 0.0f, 0.2f);  val o2y = oscillate(0.5f, 1.0f, 0.25f); val r2 = oscillate(0.7f, 1.5f, 0.18f)
                            val o3x = oscillate(0.2f, 0.8f, 0.33f); val o3y = oscillate(0.8f, 0.2f, 0.36f); val r3 = oscillate(0.6f, 1.4f, 0.29f)
                            val o4x = oscillate(0.3f, 0.7f, 0.44f); val o4y = oscillate(0.2f, 0.8f, 0.41f); val r4 = oscillate(0.9f, 1.7f, 0.47f)
                            val o5x = oscillate(0.4f, 0.6f, 0.55f); val o5y = oscillate(0.0f, 1.0f, 0.51f); val r5 = oscillate(0.7f, 1.5f, 0.58f)
                            val o6x = oscillate(0.0f, 1.0f, 0.66f); val o6y = oscillate(0.5f, 0.7f, 0.62f); val r6 = oscillate(0.8f, 1.8f, 0.69f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawWithCache {
                                        val w = size.width
                                        val h = size.height
                                        val base = Color(0xFF050505)
                                        val b1 = Brush.radialGradient(listOf(color1.copy(0.85f), color1.copy(0.5f), Color.Transparent), Offset(w*o1x, h*o1y), w*r1)
                                        val b2 = Brush.radialGradient(listOf(color2.copy(0.8f), color2.copy(0.45f), Color.Transparent), Offset(w*o2x, h*o2y), w*r2)
                                        val b3 = Brush.radialGradient(listOf(color3.copy(0.75f), color3.copy(0.4f), Color.Transparent), Offset(w*o3x, h*o3y), w*r3)
                                        val b4 = Brush.radialGradient(listOf(color4.copy(0.7f), color4.copy(0.35f), Color.Transparent), Offset(w*o4x, h*o4y), w*r4)
                                        val b5 = Brush.radialGradient(listOf(color5.copy(0.65f), color5.copy(0.3f), Color.Transparent), Offset(w*o5x, h*o5y), w*r5)
                                        val b6 = Brush.radialGradient(listOf(color6.copy(0.6f), color6.copy(0.25f), Color.Transparent), Offset(w*o6x, h*o6y), w*r6)
                                        onDrawBehind {
                                            drawRect(color = base)
                                            drawRect(brush = b1)
                                            drawRect(brush = b2)
                                            drawRect(brush = b3)
                                            drawRect(brush = b4)
                                            drawRect(brush = b5)
                                            drawRect(brush = b6)
                                        }
                                    }
                            )
                        }
                    }
                }
                else -> {
                    // DEFAULT background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }

            if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .zIndex(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 16.dp)
                                ) { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = stringResource(R.string.close),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = textBackgroundColor
                            )
                            Text(
                                text = mediaMetadata.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 16.dp)
                                ) {
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { currentLyrics },
                                            songProvider = { currentSong?.song },
                                            mediaMetadataProvider = { mediaMetadata },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Lyrics(
                            sliderPositionProvider = { sliderPosition },
                            isVisible = isVisible,
                            palette = gradientColors
                        )
                    }
                    // Slider + controls — landscape
                    Spacer(modifier = Modifier.height(12.dp))
                    when (sliderStyle) {
                        SliderStyle.DEFAULT -> Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { player.seekTo(it); position = it }; sliderPosition = null },
                            colors = PlayerSliderColors.defaultSliderColors(textBackgroundColor, playerBackground, useDarkTheme),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                        )
                        SliderStyle.SQUIGGLY -> SquigglySlider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { player.seekTo(it); position = it }; sliderPosition = null },
                            colors = PlayerSliderColors.squigglySliderColors(textBackgroundColor, playerBackground, useDarkTheme),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                            squigglesSpec = SquigglySlider.SquigglesSpec(amplitude = if (isPlaying) 2.dp else 0.dp, strokeWidth = 3.dp)
                        )
                        SliderStyle.SLIM -> Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { player.seekTo(it); position = it }; sliderPosition = null },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { s -> PlayerSliderTrack(sliderState = s, colors = PlayerSliderColors.slimSliderColors(textBackgroundColor, playerBackground, useDarkTheme)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)
                    ) {
                        Text(text = makeTimeString(sliderPosition ?: position), style = MaterialTheme.typography.labelMedium, color = textBackgroundColor, maxLines = 1)
                        Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = textBackgroundColor, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { playerConnection.player.toggleRepeatMode() }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                painter = painterResource(when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }),
                                contentDescription = "Repeat",
                                tint = if (repeatMode == Player.REPEAT_MODE_OFF) textBackgroundColor.copy(alpha = 0.4f) else textBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = "Shuffle",
                                tint = if (shuffleModeEnabled) textBackgroundColor else textBackgroundColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val maxW = maxWidth
                        val playButtonHeight = maxW / 6f
                        val playButtonWidth = playButtonHeight * 2.2f
                        val sideButtonHeight = playButtonHeight * 0.8f
                        val sideButtonWidth = sideButtonHeight * 1.3f
                        val playIS = remember { MutableInteractionSource() }
                        val playPressed by playIS.collectIsPressedAsState()
                        val playScale by animateFloatAsState(
                            targetValue = if (playPressed) 0.90f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "playScale"
                        )
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalIconButton(
                                onClick = { player.seekToPrevious() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = textBackgroundColor, contentColor = iconButtonColor),
                                modifier = Modifier.size(width = sideButtonWidth, height = sideButtonHeight).clip(RoundedCornerShape(32.dp))
                            ) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp)) }
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledIconButton(
                                onClick = {
                                    if (playbackState == Player.STATE_ENDED) { player.seekTo(0, 0); player.playWhenReady = true }
                                    else player.togglePlayPause()
                                },
                                interactionSource = playIS,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = textBackgroundColor, contentColor = iconButtonColor),
                                modifier = Modifier.size(width = playButtonWidth, height = playButtonHeight).clip(RoundedCornerShape(32.dp)).graphicsLayer(scaleX = playScale, scaleY = playScale)
                            ) {
                                Icon(
                                    painter = painterResource(when { playbackState == Player.STATE_ENDED -> R.drawable.replay; isPlaying -> R.drawable.pause; else -> R.drawable.play }),
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledTonalIconButton(
                                onClick = { player.seekToNext() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = textBackgroundColor, contentColor = iconButtonColor),
                                modifier = Modifier.size(width = sideButtonWidth, height = sideButtonHeight).clip(RoundedCornerShape(32.dp))
                            ) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp)) }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }   // end landscape Column
            }       // end ORIENTATION_LANDSCAPE
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // ── Top bar ───────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PlayerHorizontalPadding, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 20.dp)
                                ) { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = stringResource(R.string.close),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.labelMedium,
                                color = textBackgroundColor.copy(alpha = 0.6f)
                            )
                            Text(
                                text = mediaMetadata.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = textBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 20.dp)
                                ) {
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { currentLyrics },
                                            songProvider = { currentSong?.song },
                                            mediaMetadataProvider = { mediaMetadata },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    // ── Lyrics ───────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Lyrics(
                            sliderPositionProvider = { sliderPosition },
                            isVisible = isVisible,
                            palette = gradientColors
                        )
                    }
                    // ── Slider + time ────────────────────────────────────────
                    Spacer(modifier = Modifier.height(12.dp))
                    when (sliderStyle) {
                        SliderStyle.DEFAULT -> Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { player.seekTo(it); position = it }; sliderPosition = null },
                            colors = PlayerSliderColors.defaultSliderColors(textBackgroundColor, playerBackground, useDarkTheme),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                        )
                        SliderStyle.SQUIGGLY -> SquigglySlider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { player.seekTo(it); position = it }; sliderPosition = null },
                            colors = PlayerSliderColors.squigglySliderColors(textBackgroundColor, playerBackground, useDarkTheme),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                            squigglesSpec = SquigglySlider.SquigglesSpec(amplitude = if (isPlaying) 2.dp else 0.dp, strokeWidth = 3.dp)
                        )
                        SliderStyle.SLIM -> Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = { sliderPosition?.let { player.seekTo(it); position = it }; sliderPosition = null },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { s -> PlayerSliderTrack(sliderState = s, colors = PlayerSliderColors.slimSliderColors(textBackgroundColor, playerBackground, useDarkTheme)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)
                    ) {
                        Text(text = makeTimeString(sliderPosition ?: position), style = MaterialTheme.typography.labelMedium, color = textBackgroundColor, maxLines = 1)
                        Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = textBackgroundColor, maxLines = 1)
                    }
                    // ── Repeat / Shuffle ─────────────────────────────────────
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { playerConnection.player.toggleRepeatMode() }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                painter = painterResource(when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }),
                                contentDescription = "Repeat",
                                tint = if (repeatMode == Player.REPEAT_MODE_OFF) textBackgroundColor.copy(alpha = 0.4f) else textBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = "Shuffle",
                                tint = if (shuffleModeEnabled) textBackgroundColor else textBackgroundColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    // ── Playback buttons ─────────────────────────────────────
                    Spacer(modifier = Modifier.height(8.dp))
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val maxW = maxWidth
                        val playButtonHeight = maxW / 6f
                        val playButtonWidth = playButtonHeight * 2.2f
                        val sideButtonHeight = playButtonHeight * 0.8f
                        val sideButtonWidth = sideButtonHeight * 1.3f
                        val playIS = remember { MutableInteractionSource() }
                        val playPressed by playIS.collectIsPressedAsState()
                        val playScale by animateFloatAsState(
                            targetValue = if (playPressed) 0.90f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "playScale"
                        )
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalIconButton(
                                onClick = { player.seekToPrevious() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = textBackgroundColor, contentColor = iconButtonColor),
                                modifier = Modifier.size(width = sideButtonWidth, height = sideButtonHeight).clip(RoundedCornerShape(32.dp))
                            ) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp)) }
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledIconButton(
                                onClick = {
                                    if (playbackState == Player.STATE_ENDED) { player.seekTo(0, 0); player.playWhenReady = true }
                                    else player.togglePlayPause()
                                },
                                interactionSource = playIS,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = textBackgroundColor, contentColor = iconButtonColor),
                                modifier = Modifier.size(width = playButtonWidth, height = playButtonHeight).clip(RoundedCornerShape(32.dp)).graphicsLayer(scaleX = playScale, scaleY = playScale)
                            ) {
                                Icon(
                                    painter = painterResource(when { playbackState == Player.STATE_ENDED -> R.drawable.replay; isPlaying -> R.drawable.pause; else -> R.drawable.play }),
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            FilledTonalIconButton(
                                onClick = { player.seekToNext() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = textBackgroundColor, contentColor = iconButtonColor),
                                modifier = Modifier.size(width = sideButtonWidth, height = sideButtonHeight).clip(RoundedCornerShape(32.dp))
                            ) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp)) }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
