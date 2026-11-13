package echo.music.iad1tya.ui.player

import android.graphics.Matrix
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.music.innertube.YouTube
import echo.music.iad1tya.LocalPlayerConnection
import okhttp3.OkHttpClient

private fun updateTextureTransform(
  textureView: TextureView,
  viewWidth: Int,
  viewHeight: Int,
  videoWidth: Int,
  videoHeight: Int,
) {
  if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

  val viewRatio = viewWidth.toFloat() / viewHeight
  val videoRatio = videoWidth.toFloat() / videoHeight

  var scaleX = 1f
  var scaleY = 1f

  if (viewRatio > videoRatio) {
    scaleY = viewRatio / videoRatio
  } else {
    scaleX = videoRatio / viewRatio
  }

  // Cinematic letterbox cropping:
  // Many 16:9 YouTube music videos have 2.35:1 or 2.39:1 letterbox bars.
  // 16/9 = 1.78, active 2.35:1 picture height = 16 / 2.35 = 6.8 vs 9 -> 9 / 6.8 = 1.32x.
  // Zooming by 1.32x crops those bars away completely while keeping the central subject focused.
  val letterboxCrop = if (videoRatio > 1.2f) 1.32f else 1.0f
  scaleX *= letterboxCrop
  scaleY *= letterboxCrop

  val matrix = Matrix()
  matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
  textureView.setTransform(matrix)
}

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
  var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
  var currentVideoWidth by remember { mutableIntStateOf(0) }
  var currentVideoHeight by remember { mutableIntStateOf(0) }

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
            currentVideoWidth = videoSize.width
            currentVideoHeight = videoSize.height
            textureViewRef?.let { tv ->
              updateTextureTransform(tv, tv.width, tv.height, videoSize.width, videoSize.height)
            }
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
        normalized.contains("apple.com") || normalized.contains("music.apple") ->
          MimeTypes.APPLICATION_M3U8
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
      label = "canvasAlpha",
    )

  AndroidView(
    factory = { viewContext ->
      FrameLayout(viewContext).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        clipChildren = true

        val textureView =
          TextureView(viewContext).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
              val w = right - left
              val h = bottom - top
              updateTextureTransform(this, w, h, currentVideoWidth, currentVideoHeight)
            }
          }
        addView(textureView)
        textureViewRef = textureView
        exoPlayer.setVideoTextureView(textureView)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
      }
    },
    update = { _ ->
      textureViewRef?.let { tv ->
        updateTextureTransform(tv, tv.width, tv.height, currentVideoWidth, currentVideoHeight)
      }
    },
    modifier = modifier.alpha(alpha),
  )
}
