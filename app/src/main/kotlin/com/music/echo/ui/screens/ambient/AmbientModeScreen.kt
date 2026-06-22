package iad1tya.echo.music.ui.screens.ambient

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.extensions.togglePlayPause
import iad1tya.echo.music.ui.player.InlineLyricsView
import kotlin.math.abs

@Composable
fun AmbientModeScreen(navController: NavController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        var windowInsetsController: WindowInsetsControllerCompat? = null
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    var swipeThresholdX by remember { mutableStateOf(0f) }
    var swipeThresholdY by remember { mutableStateOf(0f) }
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Horizontal swipe logic (Skip Next/Prev)
                        if (abs(swipeThresholdX) > 150f && abs(swipeThresholdX) > abs(swipeThresholdY)) {
                            if (swipeThresholdX > 0) {
                                playerConnection.player.seekToPreviousMediaItem()
                            } else {
                                playerConnection.player.seekToNext()
                            }
                        }
                        swipeThresholdX = 0f
                        swipeThresholdY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        swipeThresholdX += dragAmount.x
                        swipeThresholdY += dragAmount.y

                        // Vertical swipe logic (Volume Control)
                        if (abs(dragAmount.y) > 10f && abs(dragAmount.y) > abs(dragAmount.x)) {
                            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            if (dragAmount.y < 0 && currentVolume < maxVolume) {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                            } else if (dragAmount.y > 0 && currentVolume > 0) {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                            }
                            swipeThresholdY = 0f // Reset to require another swipe chunk to keep raising
                        }
                    }
                )
            }
    ) {
        AmbientGlowBackground(
            mediaMetadata = mediaMetadata,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Album Art
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    playerConnection.togglePlayPause()
                                }
                            )
                        }
                )
            }

            // Right Side: Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 32.dp, top = 32.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                InlineLyricsView(
                    mediaMetadata = mediaMetadata,
                    showLyrics = true,
                    positionProvider = { playerConnection.player.currentPosition }
                )
            }
        }
    }
}
