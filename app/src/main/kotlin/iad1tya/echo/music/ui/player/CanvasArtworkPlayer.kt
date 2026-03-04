package iad1tya.echo.music.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.echo.innertube.YouTube
import okhttp3.OkHttpClient
import java.util.Locale

@Composable
fun CanvasArtworkPlayer(
    primaryUrl: String?,
    fallbackUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primary = primaryUrl?.takeIf { it.isNotBlank() }
    val fallback = fallbackUrl?.takeIf { it.isNotBlank() }
    val initial = primary ?: fallback ?: return
    var currentUrl by remember(initial) { mutableStateOf(initial) }
    var isVideoReady by remember(initial) { mutableStateOf(false) }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()
    }

    val mediaSourceFactory = remember(okHttpClient) {
        DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                context,
                OkHttpDataSource.Factory(okHttpClient),
            ),
        )
    }

    val exoPlayer = remember(initial) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false,
                )
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = isPlaying
            }
    }

    LaunchedEffect(isPlaying) {
        if (exoPlayer.playWhenReady != isPlaying) {
            exoPlayer.playWhenReady = isPlaying
        }
    }

    DisposableEffect(exoPlayer, primary, fallback) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val next = when (currentUrl) {
                    primary -> fallback
                    else -> null
                }
                if (!next.isNullOrBlank()) {
                    currentUrl = next
                    isVideoReady = false
                }
            }

            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(currentUrl, exoPlayer) {
        val normalized = currentUrl.trim()
        val mimeType = when {
            primary != null && currentUrl == primary -> MimeTypes.APPLICATION_M3U8
            fallback != null && currentUrl == fallback -> MimeTypes.VIDEO_MP4
            normalized.lowercase(Locale.ROOT).contains("m3u8") -> MimeTypes.APPLICATION_M3U8
            normalized.lowercase(Locale.ROOT).contains("mp4") -> MimeTypes.VIDEO_MP4
            else -> MimeTypes.APPLICATION_M3U8
        }

        val mediaItem = MediaItem.Builder()
            .setUri(normalized)
            .setMimeType(mimeType)
            .build()

        exoPlayer.stop()
        isVideoReady = false
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "canvasAlpha"
    )

    AndroidView(
        factory = { viewContext ->
            AspectRatioFrameLayout(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                val textureView = TextureView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
                addView(textureView)
                exoPlayer.setVideoTextureView(textureView)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { },
        modifier = modifier.alpha(alpha),
    )
}
