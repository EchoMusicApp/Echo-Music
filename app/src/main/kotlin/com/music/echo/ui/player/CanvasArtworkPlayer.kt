package echo.music.iad1tya.ui.player

import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import echo.music.iad1tya.LocalPlayerConnection
import java.util.Locale
import okhttp3.OkHttpClient

@Composable
fun CanvasArtworkPlayer(
  primaryUrl: String?,
  fallbackUrl: String?,
  isPlaying: Boolean,
  clipStartMs: Long? = null,
  clipEndMs: Long? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val primary = primaryUrl?.takeIf { it.isNotBlank() }
  val fallback = fallbackUrl?.takeIf { it.isNotBlank() }
  val initial = primary ?: fallback ?: return
  var currentUrl by remember(initial) { mutableStateOf(initial) }
  var isVideoReady by remember(initial) { mutableStateOf(false) }
  var videoAspectRatio by remember(initial) { mutableStateOf(1f) }

  val okHttpClient = remember {
    OkHttpClient.Builder()
      .proxy(YouTube.proxy)
      .addInterceptor { chain ->
        val request = chain.request()
        val host = request.url.host
        val isYouTubeMediaHost =
          host.endsWith("googlevideo.com") ||
            host.endsWith("googleusercontent.com") ||
            host.endsWith("youtube.com") ||
            host.endsWith("youtube-nocookie.com") ||
            host.endsWith("ytimg.com")

        if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

        val url = request.url.toString()
        val extraHeaders =
          echo.music.iad1tya.utils.InnerTubeXResolver.headersFor(url)
            ?: echo.music.iad1tya.utils.PlayerClient.forStreamUrl(url).mediaHeaders()

        val builder = request.newBuilder()
        for ((k, v) in extraHeaders) {
          builder.header(k, v)
        }

        chain.proceed(builder.build())
      }
      .build()
  }
  val downloadCache = LocalPlayerConnection.current?.service?.downloadCache
  val mediaSourceFactory =
    remember(okHttpClient, downloadCache) {
      val upstreamFactory =
        DefaultDataSource.Factory(
          context,
          OkHttpDataSource.Factory(okHttpClient),
        )
      val dataSourceFactory =
        if (downloadCache != null) {
          CacheDataSource.Factory()
            .setCache(downloadCache as androidx.media3.datasource.cache.Cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
          upstreamFactory
        }
      DefaultMediaSourceFactory(dataSourceFactory)
    }
  val exoPlayer = remember {
    ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build().apply {
      trackSelectionParameters =
        trackSelectionParameters.buildUpon().setForceHighestSupportedBitrate(true).build()
      setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
          .build(),
        false,
      )
      videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
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
    val listener =
      object : Player.Listener {
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
          val next =
            when (currentUrl) {
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

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
          if (videoSize.width > 0 && videoSize.height > 0) {
            videoAspectRatio = videoSize.width.toFloat() / videoSize.height
          }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
            exoPlayer.play()
          }
        }
      }
    exoPlayer.addListener(listener)
    onDispose { exoPlayer.removeListener(listener) }
  }

  LaunchedEffect(currentUrl, exoPlayer, clipStartMs, clipEndMs) {
    val normalized = currentUrl.trim()
    val mimeType =
      when {
        normalized.contains("googlevideo.com", ignoreCase = true) ||
          normalized.contains("mime=video", ignoreCase = true) ||
          normalized.contains(".mp4", ignoreCase = true) ||
          normalized.contains("mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
        normalized.contains(".m3u8", ignoreCase = true) ||
          normalized.contains("m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        normalized.contains("apple.com") || normalized.contains("music.apple") -> MimeTypes.APPLICATION_M3U8
        else -> MimeTypes.VIDEO_MP4
      }

    val mediaItemBuilder = MediaItem.Builder().setUri(normalized).setMimeType(mimeType)
    if (clipStartMs != null && clipEndMs != null && clipEndMs > clipStartMs) {
      mediaItemBuilder.setClippingConfiguration(
        MediaItem.ClippingConfiguration.Builder()
          .setStartPositionMs(clipStartMs)
          .setEndPositionMs(clipEndMs)
          .setStartsAtKeyFrame(true)
          .build()
      )
    }
    val mediaItem = mediaItemBuilder.build()

    exoPlayer.stop()
    isVideoReady = false
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = isPlaying
  }

  DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

  val alpha by
    animateFloatAsState(
      targetValue = if (isVideoReady) 1f else 0f,
      animationSpec = tween(durationMillis = 300),
      label = "canvasAlpha"
    )

  AndroidView(
    factory = { viewContext ->
      AspectRatioFrameLayout(viewContext).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        val textureView =
          TextureView(viewContext).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          }
        addView(textureView)
        exoPlayer.setVideoTextureView(textureView)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
      }
    },
    update = { view -> view.setAspectRatio(videoAspectRatio) },
    modifier = modifier.alpha(alpha),
  )
}
