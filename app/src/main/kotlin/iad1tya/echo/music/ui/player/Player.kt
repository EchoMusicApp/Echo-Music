package iad1tya.echo.music.ui.player

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import iad1tya.echo.music.LocalDownloadUtil
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.DarkModeKey
import iad1tya.echo.music.constants.UseNewPlayerDesignKey
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.constants.PlayerButtonsStyle
import iad1tya.echo.music.constants.PlayerButtonsStyleKey
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import iad1tya.echo.music.ui.theme.PlayerSliderColors
import iad1tya.echo.music.constants.PlayerHorizontalPadding
import iad1tya.echo.music.constants.QueuePeekHeight
import iad1tya.echo.music.constants.SliderStyle
import iad1tya.echo.music.constants.SliderStyleKey
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.extensions.toggleRepeatMode
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.ui.component.BottomSheet
import iad1tya.echo.music.ui.component.BottomSheetState
import iad1tya.echo.music.ui.component.AnimatedGradientBackground
import iad1tya.echo.music.ui.component.LocalBottomSheetPageState
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.PlayerSliderTrack
import iad1tya.echo.music.ui.component.ResizableIconButton
import iad1tya.echo.music.ui.component.rememberBottomSheetState
import iad1tya.echo.music.ui.menu.PlayerMenu
import iad1tya.echo.music.ui.screens.settings.DarkMode
import iad1tya.echo.music.ui.utils.ShowMediaInfo
import iad1tya.echo.music.utils.makeTimeString
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = false
    )
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = true
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    var showAudioRoutingDialog by remember { mutableStateOf(false) }
    
    val audioRoutingSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(Color(0xFF1C1B1F), Color(0xFF2B2930))
    val fallbackColor = Color(0xFF1C1B1F).toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(currentMetadata.thumbnailUrl)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(8)
                                    .resizeBitmapArea(100 * 100)
                                    .generate()
                            }
                            val extractedColors = PlayerColorExtractor.extractRichGradientColors(
                                palette = palette,
                                fallbackColor = fallbackColor
                            )
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor = Color.White

    val icBackgroundColor = Color(0xFF1C1B1F)

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedIconButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(500)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = state.expandedBound,
        collapsedBound = dismissedBound + 1.dp,
        initialAnchor = 1
    )

    val lyricsSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.GRADIENT -> 
            MaterialTheme.colorScheme.surfaceContainer
        else -> 
            if (useBlackBackground) Color.Black 
            else MaterialTheme.colorScheme.surfaceContainer
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                AnimatedGradientBackground(
                                    colors = colors,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                )
                            }
                        }
                    }
                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration,
                pureBlack = pureBlack,
            )
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Detect if thumbnail is rectangular (video) or square (audio-only)
                    var isRectangularThumbnail by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(mediaMetadata.thumbnailUrl) {
                        mediaMetadata.thumbnailUrl?.let { thumbnailUrl ->
                            try {
                                val imageLoader = coil3.ImageLoader.Builder(context).build()
                                val request = ImageRequest.Builder(context)
                                    .data(thumbnailUrl)
                                    .build()
                                
                                val result = imageLoader.execute(request)
                                if (result is coil3.request.SuccessResult) {
                                    val image = result.image
                                    val width = image.width
                                    val height = image.height
                                    
                                    // Check if aspect ratio is closer to 16:9 (1.77) than 1:1 (square)
                                    // Consider it rectangular if aspect ratio > 1.3
                                    val aspectRatio = width.toFloat() / height.toFloat()
                                    isRectangularThumbnail = aspectRatio > 1.3f
                                }
                            } catch (e: Exception) {
                                // If we can't load, default to false
                                isRectangularThumbnail = false
                            }
                        }
                    }
                    
                    // Switch to Video button - above song title (only show for rectangular thumbnails)
                    if (mediaMetadata.id.isNotEmpty() && isRectangularThumbnail) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable {
                                    // Pause the current song before switching to video
                                    playerConnection.player.pause()
                                    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                        putExtra("VIDEO_ID", mediaMetadata.id)
                                        putExtra("START_POSITION", playerConnection.player.currentPosition)
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = "Switch to Video",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Switch to Video",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Placeholder to maintain consistent spacing
                        Spacer(Modifier.height(40.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .combinedClickable(
                                    enabled = true,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (mediaMetadata.album != null) {
                                            navController.navigate("album/${mediaMetadata.album.id}")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText("Copied Title", title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast
                                            .makeText(context, "Copied Title", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            ,
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                        val annotatedString = buildAnnotatedString {
                            mediaMetadata.artists.forEachIndexed { index, artist ->
                                val tag = "artist_${artist.id.orEmpty()}"
                                pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) {
                                    append(artist.name)
                                }
                                pop()
                                if (index != mediaMetadata.artists.lastIndex) append(", ")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                .padding(end = 12.dp)
                        ) {
                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            var clickOffset by remember { mutableStateOf<Offset?>(null) }
                            Text(
                                text = annotatedString,
                                style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { layoutResult = it },
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val tapPosition = event.changes.firstOrNull()?.position
                                                if (tapPosition != null) {
                                                    clickOffset = tapPosition
                                                }
                                            }
                                        }
                                    }
                                    .combinedClickable(
                                        enabled = true,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val tapPosition = clickOffset
                                            val layout = layoutResult
                                            if (tapPosition != null && layout != null) {
                                                val offset = layout.getOffsetForPosition(tapPosition)
                                                annotatedString
                                                    .getStringAnnotations(offset, offset)
                                                    .firstOrNull()
                                                    ?.let { ann ->
                                                        val artistId = ann.item
                                                        if (artistId.isNotBlank()) {
                                                            navController.navigate("artist/$artistId")
                                                            state.collapseSoft()
                                                        }
                                                    }
                                            }
                                        },
                                        onLongClick = {
                                            val clip =
                                                ClipData.newPlainText("Copied Artist", annotatedString)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Copied Artist",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (useNewPlayerDesign) {
                    val audioRoutingShape = RoundedCornerShape(
                        topStart = 50.dp, bottomStart = 50.dp,
                        topEnd = 5.dp, bottomEnd = 5.dp
                    )

                    val favShape = RoundedCornerShape(
                        topStart = 5.dp, bottomStart = 5.dp,
                        topEnd = 50.dp, bottomEnd = 50.dp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = if (mediaMetadata.id.isNotEmpty()) 48.dp else 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(audioRoutingShape)
                                .background(textButtonColor)
                                .clickable {
                                    audioRoutingSheetState.expandSoft()
                                }
                        ) {
                            // Detect current audio device
                            val audioManager = try {
                                context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                            } catch (e: Exception) {
                                null
                            }
                            val devices = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                                } else {
                                    emptyList()
                                }
                            } catch (e: Exception) {
                                emptyList()
                            }
                            val hasBluetoothDevice = devices.any { 
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO 
                            }
                            val hasWiredHeadset = devices.any { 
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES 
                            }
                            
                            // Choose icon based on connected device
                            val audioIcon = when {
                                hasBluetoothDevice -> R.drawable.audio_bluetooth  // Bluetooth icon
                                hasWiredHeadset -> R.drawable.audio_earphone  // Wired headset icon
                                else -> R.drawable.audio_device  // Phone speaker icon
                            }
                            
                            Image(
                                painter = painterResource(audioIcon),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(favShape)
                                .background(textButtonColor)
                                .clickable {
                                    playerConnection.toggleLike()
                                }
                        ) {
                            Image(
                                painter = painterResource(
                                    if (currentSong?.song?.liked == true)
                                        R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(iconButtonColor),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier =
                        Modifier
                            .padding(top = if (mediaMetadata.id.isNotEmpty()) 48.dp else 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                audioRoutingSheetState.expandSoft()
                            },
                    ) {
                        // Detect current audio device
                        val audioManager = try {
                            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        } catch (e: Exception) {
                            null
                        }
                        val devices = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                        val hasBluetoothDevice = devices.any { 
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO 
                        }
                        val hasWiredHeadset = devices.any { 
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES 
                        }
                        
                        // Choose icon based on connected device
                        val audioIcon = when {
                            hasBluetoothDevice -> R.drawable.audio_bluetooth  // Bluetooth icon
                            hasWiredHeadset -> R.drawable.audio_earphone  // Wired headset icon
                            else -> R.drawable.audio_device  // Phone speaker icon
                        }
                        
                        Image(
                            painter = painterResource(audioIcon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.size(6.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                        Modifier
                            .padding(top = if (mediaMetadata.id.isNotEmpty()) 48.dp else 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = mediaMetadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = {
                                            mediaMetadata.id.let {
                                                bottomSheetPageState.show {
                                                    ShowMediaInfo(it)
                                                }
                                            }
                                        },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        colors = PlayerSliderColors.defaultSliderColors(textButtonColor, playerBackground, useDarkTheme),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        colors = PlayerSliderColors.squigglySliderColors(textButtonColor, playerBackground, useDarkTheme),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        squigglesSpec =
                        SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                            strokeWidth = 3.dp,
                        ),
                    )
                }

                SliderStyle.SLIM -> {
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = PlayerSliderColors.slimSliderColors(textButtonColor, playerBackground, useDarkTheme)
                            )
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            if (useNewPlayerDesign) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val maxW = maxWidth
                    val playButtonHeight = maxW / 6f
                    val playButtonWidth = playButtonHeight * 1.6f
                    val sideButtonHeight = playButtonHeight * 0.8f
                    val sideButtonWidth = sideButtonHeight * 1.3f

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        FilledTonalIconButton(
                            onClick = playerConnection::seekToPrevious,
                            enabled = canSkipPrevious,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier
                                .size(width = sideButtonWidth, height = sideButtonHeight)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledIconButton(
                            onClick = {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier
                                .size(width = playButtonWidth, height = playButtonHeight)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledTonalIconButton(
                            onClick = playerConnection::seekToNext,
                            enabled = canSkipNext,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier
                                .size(width = sideButtonWidth, height = sideButtonHeight)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = when (repeatMode) {
                                Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> throw IllegalStateException()
                            },
                            color = TextBackgroundColor,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .align(Alignment.Center)
                                .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                            onClick = {
                                playerConnection.player.toggleRepeatMode()
                            },
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = R.drawable.skip_previous,
                            enabled = canSkipPrevious,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            onClick = playerConnection::seekToPrevious,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier =
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(playPauseRoundness))
                            .background(textButtonColor)
                            .clickable {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                    ) {
                        Image(
                            painter =
                            painterResource(
                                if (playbackState ==
                                    STATE_ENDED
                                ) {
                                    R.drawable.replay
                                } else if (isPlaying) {
                                    R.drawable.pause
                                } else {
                                    R.drawable.play
                                },
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                            modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = R.drawable.skip_next,
                            enabled = canSkipNext,
                            color = TextBackgroundColor,
                            modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            onClick = playerConnection::seekToNext,
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border,
                            color = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else TextBackgroundColor,
                            modifier =
                            Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .align(Alignment.Center),
                            onClick = playerConnection::toggleLike,
                        )
                    }
                }
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Back button at top left
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Close player",
                        tint = textButtonColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                            .padding(16.dp)
                            .size(24.dp)
                            .clickable { state.collapseSoft() }
                    )
                    
                    // Echo Music title at top center
                    Text(
                        text = "Echo Music",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.zalando_sans_expanded)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = textButtonColor,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                            .padding(16.dp)
                    )
                    
                    // Share button at top right
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share",
                        tint = textButtonColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                            .padding(16.dp)
                            .size(24.dp)
                            .clickable {
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                    )
                    
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                            isPlayerExpanded = state.isExpanded,
                            onToggleLyrics = {
                                if (lyricsSheetState.isExpanded) {
                                    lyricsSheetState.collapseSoft()
                                } else {
                                    lyricsSheetState.expandSoft()
                                }
                            }
                        )
                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(30.dp))
                }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Blurred Background Layer
                    mediaMetadata?.let { metadata ->
                        AsyncImage(
                            model = coil3.request.ImageRequest.Builder(LocalContext.current)
                                .data(metadata.thumbnailUrl)
                                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(100.dp) // Strong blur for background
                        )
                        
                        // Dark overlay to ensure text readability against the blurred image
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }

                    // 2. Foreground Content (Thumbnail + Controls)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound),
                    ) {
                        // Top Bar alignment handling
                         Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .height(48.dp) // Approximate height for top bar area
                        ) {
                             // Back and Share buttons will be placed here by the Box below, 
                             // but we reserve space in the column if needed or just let them float.
                             // Actually, the Back and Share buttons are floating in the outer Box. 
                             // We just need the Column to start below them or have padding.
                        }
                        
                        // Main Thumbnail
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                isPlayerExpanded = state.isExpanded,
                                onToggleLyrics = {
                                    if (lyricsSheetState.isExpanded) {
                                        lyricsSheetState.collapseSoft()
                                    } else {
                                        lyricsSheetState.expandSoft()
                                    }
                                }
                            )
                        }

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.height(30.dp))
                    }

                    // 3. Top Controls (Back, Share) - Floating on top
                    // Back button at top left
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Close player",
                        tint = Color.White, // Always white on dark immersive bg
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                            .padding(16.dp)
                            .size(24.dp)
                            .clickable { state.collapseSoft() }
                    )
                    
                    // Echo Music title at top center
                    Text(
                        text = "Echo Music",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.zalando_sans_expanded)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = Color.White, // Always white on dark immersive bg
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                            .padding(16.dp)
                    )
                    
                    // Share button at top right
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share",
                        tint = Color.White, // Always white on dark immersive bg
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                            .padding(16.dp)
                            .size(24.dp)
                            .clickable {
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                    )
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            background =
            if (useBlackBackground) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            onBackgroundColor = onBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            onShowLyrics = { lyricsSheetState.expandSoft() },
            pureBlack = pureBlack,
        )

        mediaMetadata?.let { metadata ->
            BottomSheet(
                state = lyricsSheetState,
                background = { Box(Modifier.fillMaxSize().background(Color.Unspecified)) },
                onDismiss = { },
                collapsedContent = {
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = lyricsSheetState.progress.coerceIn(0f, 1f)
                            )
                        )
                ) {
                    LyricsScreen(
                        mediaMetadata = metadata,
                        onBackClick = { lyricsSheetState.collapseSoft() },
                        navController = navController,
                        backgroundAlpha = lyricsSheetState.progress.coerceIn(0f, 1f),
                        isVisible = lyricsSheetState.progress > 0.01f
                    )
                }
            }
        }
        
        // Audio Routing Bottom Sheet
        BottomSheet(
            state = audioRoutingSheetState,
            background = { 
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (useBlackBackground) Color.Black 
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                ) 
            },
            onDismiss = { },
            collapsedContent = {}
        ) {
            val audioManager = try {
                context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            } catch (e: Exception) {
                null
            }
            
            val bluetoothAdapter = try {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                bluetoothManager?.adapter
            } catch (e: Exception) {
                null
            }
            
            val wifiManager = try {
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            } catch (e: Exception) {
                null
            }
            
            // Get available audio devices
            val devices = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
            
            // Get currently active audio device
            val activeDevices = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { device ->
                        // Check if this device is currently being used for music playback
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                                    .any { it.id == device.id }
                            } else {
                                true
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
            
            // Determine current active device type
            val isPlayingOnBluetooth = activeDevices.any { 
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO 
            }
            val isPlayingOnWiredHeadset = activeDevices.any { 
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES 
            }
            val isPlayingOnUsb = activeDevices.any { 
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE || 
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET 
            }
            val isPlayingOnSpeaker = !isPlayingOnBluetooth && !isPlayingOnWiredHeadset && !isPlayingOnUsb
            
            val hasBluetoothDevice = try {
                devices.any { 
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO 
                }
            } catch (e: Exception) {
                false
            }
            
            val hasWiredHeadset = try {
                devices.any { 
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES 
                }
            } catch (e: Exception) {
                false
            }
            
            val hasUsbDevice = try {
                devices.any { 
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE || 
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET 
                }
            } catch (e: Exception) {
                false
            }
            
            val isBluetoothOn = try {
                bluetoothAdapter?.isEnabled == true
            } catch (e: Exception) {
                false
            }
            
            val isWifiOn = try {
                wifiManager?.isWifiEnabled == true
            } catch (e: Exception) {
                false
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (useBlackBackground) Color.Black 
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Audio Output",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FilledTonalIconButton(
                        onClick = { audioRoutingSheetState.collapseSoft() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Close",
                            tint = iconButtonColor
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // Connected Devices Section
                        if (hasBluetoothDevice || hasWiredHeadset || hasUsbDevice || (!hasBluetoothDevice && !hasWiredHeadset && !hasUsbDevice)) {
                            Text(
                                "CONNECTED DEVICES",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(textButtonColor.copy(alpha = 0.3f)),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                        
                        // Phone Speaker (only show if no external devices are connected)
                        if (!hasBluetoothDevice && !hasWiredHeadset && !hasUsbDevice) {
                            Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPlayingOnSpeaker) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable {
                                    try {
                                        playerConnection.forceAudioToSpeaker(context)
                                        Toast.makeText(context, "Switched to Phone Speaker", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to switch audio output: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    audioRoutingSheetState.collapseSoft()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.audio_device),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (isPlayingOnSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "This Device",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPlayingOnSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (isPlayingOnSpeaker) "Playing now" else "Phone Speaker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPlayingOnSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isPlayingOnSpeaker) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = "Currently playing",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        }
                        
                        // Wired Headset
                        if (hasWiredHeadset) {
                            val wiredDevice = devices.firstOrNull { 
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES 
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPlayingOnWiredHeadset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                                                // Disable speaker and bluetooth
                                                audioManager.isSpeakerphoneOn = false
                                                if (audioManager.isBluetoothScoOn) {
                                                    audioManager.stopBluetoothSco()
                                                    audioManager.isBluetoothScoOn = false
                                                }
                                                audioManager.mode = AudioManager.MODE_NORMAL
                                                Toast.makeText(context, "Switched to Wired Headset", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Playing on Wired Headset", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to switch audio output", Toast.LENGTH_SHORT).show()
                                        }
                                        audioRoutingSheetState.collapseSoft()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.audio_earphone),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isPlayingOnWiredHeadset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Wired Headset",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPlayingOnWiredHeadset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (isPlayingOnWiredHeadset) "Playing now" else "Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPlayingOnWiredHeadset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isPlayingOnWiredHeadset) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = "Currently playing",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // USB Audio Device
                        if (hasUsbDevice) {
                            val usbDevice = devices.firstOrNull { 
                                it.type == AudioDeviceInfo.TYPE_USB_DEVICE || 
                                it.type == AudioDeviceInfo.TYPE_USB_HEADSET 
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPlayingOnUsb) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
                                                // Disable speaker and bluetooth
                                                audioManager.isSpeakerphoneOn = false
                                                if (audioManager.isBluetoothScoOn) {
                                                    audioManager.stopBluetoothSco()
                                                    audioManager.isBluetoothScoOn = false
                                                }
                                                audioManager.mode = AudioManager.MODE_NORMAL
                                                Toast.makeText(context, "Switched to USB Audio", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Playing on USB Device", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to switch audio output", Toast.LENGTH_SHORT).show()
                                        }
                                        audioRoutingSheetState.collapseSoft()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.audio_earphone),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isPlayingOnUsb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "USB Audio",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPlayingOnUsb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (isPlayingOnUsb) "Playing now" else "Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPlayingOnUsb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isPlayingOnUsb) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = "Currently playing",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Bluetooth Devices
                        if (hasBluetoothDevice) {
                            val btDevice = try {
                                devices.firstOrNull { 
                                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO 
                                }
                            } catch (e: Exception) {
                                null
                            }
                            
                            val btDeviceName = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    btDevice?.productName?.toString() ?: "Bluetooth Device"
                                } else {
                                    "Bluetooth Device"
                                }
                            } catch (e: Exception) {
                                "Bluetooth Device"
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPlayingOnBluetooth) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        try {
                                            playerConnection.forceAudioToBluetooth(context)
                                            Toast.makeText(context, "Switched to $btDeviceName", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to switch to Bluetooth: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        audioRoutingSheetState.collapseSoft()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.audio_bluetooth),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isPlayingOnBluetooth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        btDeviceName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPlayingOnBluetooth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (isPlayingOnBluetooth) "Playing now" else "Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPlayingOnBluetooth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isPlayingOnBluetooth) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = "Currently playing",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else if (isBluetoothOn) {
                            // Bluetooth is ON but no devices connected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        try {
                                            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open Bluetooth settings", Toast.LENGTH_SHORT).show()
                                        }
                                        audioRoutingSheetState.collapseSoft()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.audio_bluetooth),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Bluetooth Devices",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "No device found",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            // Bluetooth is OFF
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        try {
                                            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open Bluetooth settings", Toast.LENGTH_SHORT).show()
                                        }
                                        audioRoutingSheetState.collapseSoft()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.audio_bluetooth),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Bluetooth Devices",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Bluetooth is off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } // Close Card Column
                    } // Close Card
                    } // Close if (has connected devices)
                    
                    // WiFi, Cast & DLNA Devices Section
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "WIFI, CAST & DLNA DEVICES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        
                        // WiFi Audio devices - Google Cast integration
                        val coroutineScope = rememberCoroutineScope()
                        
                        // Check for required permissions before initializing Cast
                        val hasRequiredPermissions = remember {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Android 13+: Only NEARBY_WIFI_DEVICES permission is needed
                                ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
                            } else {
                                // Android 12 and below: Location permission is required
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            }
                        }
                        
                        val castContext = if (hasRequiredPermissions) {
                            try {
                                CastContext.getSharedInstance(context)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        
                        val castSession = remember { mutableStateOf<CastSession?>(null) }
                        
                        LaunchedEffect(castContext) {
                            castContext?.sessionManager?.let { sessionManager ->
                                castSession.value = sessionManager.currentCastSession
                                val listener = object : SessionManagerListener<CastSession> {
                                    override fun onSessionStarting(session: CastSession) {}
                                    override fun onSessionStarted(session: CastSession, sessionId: String) {
                                        castSession.value = session
                                    }
                                    override fun onSessionStartFailed(session: CastSession, error: Int) {}
                                    override fun onSessionEnding(session: CastSession) {}
                                    override fun onSessionEnded(session: CastSession, error: Int) {
                                        castSession.value = null
                                    }
                                    override fun onSessionResuming(session: CastSession, sessionId: String) {}
                                    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                                        castSession.value = session
                                    }
                                    override fun onSessionResumeFailed(session: CastSession, error: Int) {}
                                    override fun onSessionSuspended(session: CastSession, reason: Int) {}
                                }
                                sessionManager.addSessionManagerListener(listener, CastSession::class.java)
                            }
                        }
                        
                        val mediaRouter = if (hasRequiredPermissions) {
                            try {
                                MediaRouter.getInstance(context)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        
                        val selector = if (hasRequiredPermissions) {
                            try {
                                MediaRouteSelector.Builder()
                                    .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                                    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                                    .addControlCategory(com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(
                                        com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                                    ))
                                    .build()
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        
                        // State to track discovered routes
                        var discoveredRoutes by remember { mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList()) }
                        var isScanning by remember { mutableStateOf(false) }
                        
                        // Add MediaRouter callback to actively scan for Cast devices - only if permissions granted
                        DisposableEffect(mediaRouter, selector, hasRequiredPermissions) {
                            if (!hasRequiredPermissions) {
                                isScanning = false
                                return@DisposableEffect onDispose { }
                            }
                            
                            isScanning = true
                            val callback = object : MediaRouter.Callback() {
                                override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                                    discoveredRoutes = router.routes.filter { r ->
                                        (selector?.let { r.matchesSelector(it) } == true && 
                                        !r.isDefaultOrBluetooth &&
                                        r.isEnabled) || 
                                        r.description?.contains("Cast", ignoreCase = true) == true ||
                                        r.name.contains("Cast", ignoreCase = true)
                                    }
                                    isScanning = false
                                }
                                
                                override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                                    discoveredRoutes = router.routes.filter { r ->
                                        (selector?.let { r.matchesSelector(it) } == true && 
                                        !r.isDefaultOrBluetooth &&
                                        r.isEnabled) || 
                                        r.description?.contains("Cast", ignoreCase = true) == true ||
                                        r.name.contains("Cast", ignoreCase = true)
                                    }
                                }
                                
                                override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                                    discoveredRoutes = router.routes.filter { r ->
                                        (selector?.let { r.matchesSelector(it) } == true && 
                                        !r.isDefaultOrBluetooth &&
                                        r.isEnabled) || 
                                        r.description?.contains("Cast", ignoreCase = true) == true ||
                                        r.name.contains("Cast", ignoreCase = true)
                                    }
                                }
                            }
                            
                            if (mediaRouter != null && selector != null) {
                                try {
                                    mediaRouter.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
                                    // Initial update
                                    discoveredRoutes = mediaRouter.routes.filter { r ->
                                        (r.matchesSelector(selector) && 
                                        !r.isDefaultOrBluetooth &&
                                        r.isEnabled) || 
                                        r.description?.contains("Cast", ignoreCase = true) == true ||
                                        r.name.contains("Cast", ignoreCase = true)
                                    }
                                    // Set scanning to false after initial load
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        isScanning = false
                                    }
                                } catch (e: Exception) {
                                    // Handle any exceptions during MediaRouter operations
                                    isScanning = false
                                }
                            }
                            
                            onDispose {
                                try {
                                    if (mediaRouter != null) {
                                        mediaRouter.removeCallback(callback)
                                    }
                                } catch (e: Exception) {
                                    // Ignore exceptions during cleanup
                                }
                                isScanning = false
                            }
                        }
                        
                        // Get all WiFi/Cast routes
                        val allWifiRoutes = discoveredRoutes
                        
                        val connectedWifiRoutes = allWifiRoutes.filter { 
                            it.connectionState == MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED 
                        }
                        val availableWifiRoutes = allWifiRoutes.filter {
                            it.connectionState != MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED
                        }
                        
                        val hasConnectedDevice = connectedWifiRoutes.isNotEmpty() || castSession.value != null
                        
                        if (hasConnectedDevice) {
                            // Show connected Cast device first
                            castSession.value?.let { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .clickable {
                                            Toast.makeText(context, "Playing on ${session.castDevice?.friendlyName}", Toast.LENGTH_SHORT).show()
                                            audioRoutingSheetState.collapseSoft()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.wifi_proxy),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            session.castDevice?.friendlyName ?: "Cast Device",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Casting now",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = "Currently playing",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Show other connected WiFi devices
                            connectedWifiRoutes.forEach { route ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .clickable {
                                            Toast.makeText(context, "Playing on ${route.name}", Toast.LENGTH_SHORT).show()
                                            audioRoutingSheetState.collapseSoft()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.audio_wifi),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            route.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Playing now",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = "Currently playing",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else if (isWifiOn) {
                            // WiFi is ON - show available devices
                            if (availableWifiRoutes.isNotEmpty()) {
                                // Show available WiFi/Cast devices
                                availableWifiRoutes.forEach { route ->
                                    val isCastDevice = route.description?.contains("Cast", ignoreCase = true) == true ||
                                                      route.name.contains("Cast", ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                try {
                                                    route.select()
                                                    Toast.makeText(context, "Connecting to ${route.name}", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                                audioRoutingSheetState.collapseSoft()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isCastDevice) R.drawable.wifi_proxy else R.drawable.audio_wifi
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                route.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                route.description ?: "Available",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Show scan button if no devices found (WiFi or Cast)
                            if (availableWifiRoutes.isEmpty() && connectedWifiRoutes.isEmpty() && 
                                castSession.value == null) {
                                // No WiFi/Cast devices found - show scanning status
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.wifi_proxy),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "WiFi, Cast & DLNA Devices",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                when {
                                                    !hasRequiredPermissions -> "Grant location permission to discover devices"
                                                    isScanning -> "Scanning..."
                                                    else -> "No devices found"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    if (!isScanning && hasRequiredPermissions) {
                                        Spacer(Modifier.height(12.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                isScanning = true
                                                try {
                                                    // Scan for both WiFi/Cast and DLNA devices
                                                    mediaRouter?.let { router ->
                                                        selector?.let { sel ->
                                                            Toast.makeText(context, "Scanning for WiFi & Cast devices...", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    
                                                    // Auto-stop scanning after 5 seconds
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(5000)
                                                        isScanning = false
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to scan: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    isScanning = false
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.sync),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Scan for Devices")
                                        }
                                    }
                                }
                            }
                        } else {
                            // WiFi is OFF
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.audio_wifi),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "WiFi Audio & Cast Devices",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "WiFi is off",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        } // Close WiFi Card Column
                    } // Close WiFi Card
                } // Close spacing Column
            } // Close main Column
        } // Close Audio Routing BottomSheet and Player BottomSheet
}
