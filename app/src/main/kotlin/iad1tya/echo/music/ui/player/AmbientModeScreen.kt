package iad1tya.echo.music.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.Lyrics
import iad1tya.echo.music.utils.rememberEnumPreference
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.constants.PlayerBackgroundStyleKey
import iad1tya.echo.music.constants.PlayerBackgroundStyle
import iad1tya.echo.music.constants.AmbientModeDullBackgroundKey
import iad1tya.echo.music.constants.AmbientModeSongAccentKey
import iad1tya.echo.music.ui.theme.extractThemeColor
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import iad1tya.echo.music.ui.component.AnimatedGradientBackground
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import androidx.compose.ui.graphics.toArgb
import iad1tya.echo.music.db.entities.LyricsEntity
import iad1tya.echo.music.di.LyricsHelperEntryPoint
import iad1tya.echo.music.LocalDatabase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Composable
fun AmbientModeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
        
    // State for options
    var showAlbumArt by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(false) }

    val (isDullBackground, onDullBackgroundChange) = rememberPreference(AmbientModeDullBackgroundKey, false)
    val (isSongAccent, onSongAccentChange) = rememberPreference(AmbientModeSongAccentKey, false)
    
    // Extract song accent colors
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    LaunchedEffect(mediaMetadata?.thumbnailUrl, isSongAccent) {
        if (isSongAccent && mediaMetadata?.thumbnailUrl != null) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(mediaMetadata?.thumbnailUrl)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                result.image?.toBitmap()?.let { bitmap ->
                    val palette = androidx.palette.graphics.Palette.from(bitmap)
                        .maximumColorCount(32)
                        .generate()
                    
                    gradientColors = PlayerColorExtractor.extractRichGradientColors(
                        palette = palette,
                        fallbackColor = Color.Black.toArgb()
                    )
                }
            } catch (e: Exception) {
                gradientColors = emptyList()
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Auto-fetch lyrics if missing (mirrors LyricsScreen logic)
    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            withContext(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata!!)
                    
                    // Check if lyrics were added manually while we were fetching
                    if (database.lyrics(mediaMetadata!!.id).first() == null) {
                        database.query {
                            upsert(LyricsEntity(mediaMetadata!!.id, lyrics))
                        }
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    // Force Landscape and Fullscreen
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Hide system bars
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                
                val layoutParams = window.attributes
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = layoutParams
            }
        }
    }

    // Handle Brightness Change
    LaunchedEffect(isDullBackground) {
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = if (isDullBackground) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    var totalDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                    val threshold = 100f // Threshold in pixels for sensitivity

                    if (abs(totalDrag) > threshold) {
                         if (totalDrag < 0) {
                             // Swipe Left -> Next Song
                             playerConnection.player.seekToNext()
                         } else {
                             // Swipe Right -> Previous Song
                             playerConnection.player.seekToPrevious()
                         }
                         // Reset to avoid multiple triggers for the same swipe motion
                         totalDrag = 0f 
                    }
                }
            }
    ) {
        if (isSongAccent && gradientColors.isNotEmpty()) {
            AnimatedGradientBackground(
                colors = gradientColors,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f) // Slightly dimmed for ambient mode
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isDullBackground) 0.3f else 1f), // Dull background effect
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Section (Left, if enabled)
            AnimatedVisibility(visible = showAlbumArt) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    mediaMetadata?.let { metadata ->
                        AsyncImage(
                            model = metadata.thumbnailUrl,
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(300.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Lyrics Section
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                // We need to override the text color in Lyrics composable. 
                // Since Lyrics composable reads from preferences or themes, we might need a wrapper 
                // or pass a color modifier if supported. 
                // Looking at Lyrics.kt, it uses MaterialTheme.typography which uses LocalContentColor.
                // So wrapping it in a CompositionLocalProvider for LocalContentColor might work?
                // Actually, Lyrics.kt might handle its own colors.
                // Let's try CompositionLocalProvider(LocalContentColor provides lyricsColor)
                
                Lyrics(
                    sliderPositionProvider = { playerConnection.player.currentPosition },
                    isVisible = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Controls Overlay (Replaced by Floating Settings Icon)
        /*
        AnimatedVisibility(
            visible = areControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { ... }
        */
        
        // Permanent Settings Icon (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = { areControlsVisible = true }) {
                Icon(
                    painter = painterResource(R.drawable.settings_outlined),
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.5f), // Dimmed to prevent burn-in
                    modifier = Modifier.size(24.dp)
                )
            }
            
            DropdownMenu(
                expanded = areControlsVisible,
                onDismissRequest = { areControlsVisible = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                DropdownMenuItem(
                    text = { Text("Album Art") },
                    onClick = { 
                        showAlbumArt = !showAlbumArt
                        areControlsVisible = false 
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(if (showAlbumArt) R.drawable.insert_photo else R.drawable.hide_image),
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Dull Background") },
                    onClick = { 
                        onDullBackgroundChange(!isDullBackground)
                        areControlsVisible = false 
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(if (isDullBackground) R.drawable.contrast else R.drawable.contrast),
                            contentDescription = null,
                            tint = if (isDullBackground) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Song Accent") },
                    onClick = { 
                        onSongAccentChange(!isSongAccent)
                        areControlsVisible = false 
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.palette),
                            contentDescription = null,
                            tint = if (isSongAccent) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exit Ambient Mode") },
                    onClick = { 
                        areControlsVisible = false
                        navController.popBackStack() 
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
