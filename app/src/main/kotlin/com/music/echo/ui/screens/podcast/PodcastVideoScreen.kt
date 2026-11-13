package echo.music.iad1tya.ui.screens.podcast

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.podcast.PodcastVideoProvider
import echo.music.iad1tya.podcast.VideoTrackOption
import echo.music.iad1tya.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient

private fun buildMediaSource(
  context: Context,
  option: VideoTrackOption,
  okHttpClient: OkHttpClient,
): MediaSource {
  val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
  val chunkedFactory =
    com.music.echo.playback.ChunkedDataSource.Factory(okHttpFactory, 1024 * 1024L)
  val upstreamFactory = DefaultDataSource.Factory(context, chunkedFactory)
  val mediaSourceFactory = DefaultMediaSourceFactory(upstreamFactory)

  val videoMime =
    when {
      option.videoUrl.contains("mp4", ignoreCase = true) ||
        option.videoUrl.contains("googlevideo", ignoreCase = true) -> MimeTypes.VIDEO_MP4
      option.videoUrl.contains("webm", ignoreCase = true) -> MimeTypes.VIDEO_WEBM
      else -> MimeTypes.APPLICATION_M3U8
    }

  val videoMediaItem = MediaItem.Builder().setUri(option.videoUrl).setMimeType(videoMime).build()
  val videoSource = mediaSourceFactory.createMediaSource(videoMediaItem)

  val audioUrl = option.audioUrl
  if (audioUrl.isNullOrBlank()) {
    return videoSource
  }

  val audioMime =
    when {
      audioUrl.contains("mp4", ignoreCase = true) || audioUrl.contains("m4a", ignoreCase = true) ->
        MimeTypes.AUDIO_AAC
      audioUrl.contains("webm", ignoreCase = true) ||
        audioUrl.contains("opus", ignoreCase = true) -> MimeTypes.AUDIO_OPUS
      else -> null
    }

  val audioItem =
    MediaItem.Builder()
      .setUri(audioUrl)
      .apply { if (audioMime != null) setMimeType(audioMime) }
      .build()
  val audioSource = mediaSourceFactory.createMediaSource(audioItem)

  return MergingMediaSource(videoSource, audioSource)
}

@Composable
fun PodcastVideoScreen(navController: NavController) {
  val context = LocalContext.current
  val playerConnection = LocalPlayerConnection.current ?: return
  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  val activity = context as? Activity

  val videoId =
    mediaMetadata?.id
      ?: run {
        navController.popBackStack()
        return
      }
  val title = mediaMetadata?.title.orEmpty()
  val author = mediaMetadata?.artists?.firstOrNull()?.name.orEmpty()
  val startPositionMs = remember { playerConnection.player.currentPosition }

  var lastKnownPosition by remember { mutableLongStateOf(startPositionMs) }
  var isVideoReady by remember { mutableStateOf(false) }

  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      playWhenReady = true
    }
  }

  var isPlaying by remember { mutableStateOf(true) }
  var isBuffering by remember { mutableStateOf(true) }
  var currentPosition by remember { mutableLongStateOf(startPositionMs) }
  var duration by remember { mutableLongStateOf(0L) }

  fun exitVideoPlayer() {
    val exitPos =
      if (exoPlayer.currentPosition > 0) exoPlayer.currentPosition else lastKnownPosition
    val wasPlaying = isPlaying
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    playerConnection.player.seekTo(exitPos)
    if (wasPlaying) {
      playerConnection.player.play()
    }
    exoPlayer.stop()
    navController.popBackStack()
  }

  // Landscape orientation & full screen immersive bars
  DisposableEffect(Unit) {
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    val window = activity?.window
    var windowInsetsController: WindowInsetsControllerCompat? = null
    if (window != null) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    onDispose {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      if (window != null) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
      }
      if (lastKnownPosition > 0) {
        playerConnection.player.seekTo(lastKnownPosition)
      }
      if (isPlaying) {
        playerConnection.player.play()
      }
    }
  }

  BackHandler {
    exitVideoPlayer()
  }

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
        YouTube.cookie?.let { cookie -> builder.header("Cookie", cookie) }
        chain.proceed(builder.build())
      }
      .build()
  }

  var tracks by remember { mutableStateOf<List<VideoTrackOption>>(emptyList()) }
  var selectedTrack by remember { mutableStateOf<VideoTrackOption?>(null) }
  var isLoadingTracks by remember { mutableStateOf(true) }

  var controlsVisible by remember { mutableStateOf(true) }
  var qualityMenuOpen by remember { mutableStateOf(false) }
  var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
  var isScrubbing by remember { mutableStateOf(false) }
  var scrubPosition by remember { mutableLongStateOf(0L) }
  var resizeZoom by remember { mutableStateOf(false) }

  // Manage ExoPlayer listener & release with seamless handover
  DisposableEffect(exoPlayer) {
    val listener =
      object : Player.Listener {
        override fun onRenderedFirstFrame() {
          isVideoReady = true
          // Seamless audio handover: pause main audio player only when video is decoded
          if (playerConnection.player.isPlaying) {
            playerConnection.player.pause()
          }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          isBuffering = playbackState == Player.STATE_BUFFERING
          if (playbackState == Player.STATE_READY) {
            val d = exoPlayer.duration
            if (d > 0) duration = d
            if (!isVideoReady) {
              isVideoReady = true
              if (playerConnection.player.isPlaying) {
                playerConnection.player.pause()
              }
            }
          }
        }

        override fun onIsPlayingChanged(playing: Boolean) {
          isPlaying = playing
        }
      }
    exoPlayer.addListener(listener)
    onDispose {
      exoPlayer.removeListener(listener)
      exoPlayer.release()
    }
  }

  // Periodic position updates
  LaunchedEffect(exoPlayer, isPlaying) {
    while (isActive) {
      if (exoPlayer.isPlaying && !isScrubbing) {
        val pos = exoPlayer.currentPosition
        if (pos > 0) {
          currentPosition = pos
          lastKnownPosition = pos
        }
        val d = exoPlayer.duration
        if (d > 0) duration = d
      }
      delay(250)
    }
  }

  // Auto-hide controls timer (3.5s)
  LaunchedEffect(controlsVisible, isPlaying, qualityMenuOpen, isScrubbing) {
    if (controlsVisible && isPlaying && !qualityMenuOpen && !isScrubbing) {
      delay(3500)
      controlsVisible = false
    }
  }

  // Load video tracks and prepare playback
  LaunchedEffect(videoId) {
    isLoadingTracks = true
    val resolved = PodcastVideoProvider.resolveVideoTracks(videoId)
    tracks = resolved
    isLoadingTracks = false

    // Default to 720p or 1080p, or highest available
    val defaultTrack =
      resolved.firstOrNull { it.height == 720 }
        ?: resolved.firstOrNull { it.height == 1080 }
        ?: resolved.firstOrNull()

    selectedTrack = defaultTrack

    if (defaultTrack != null) {
      val mediaSource = buildMediaSource(context, defaultTrack, okHttpClient)
      exoPlayer.setMediaSource(mediaSource)
      exoPlayer.prepare()

      // Sync position with current audio playback
      val currentAudioPos = playerConnection.player.currentPosition
      if (currentAudioPos > 0) {
        exoPlayer.seekTo(currentAudioPos)
      }
      exoPlayer.play()
    }
  }

  val videoAlpha by
    animateFloatAsState(
      targetValue = if (isVideoReady) 1f else 0f,
      animationSpec = tween(durationMillis = 350),
      label = "videoFadeIn",
    )

  Box(
    modifier =
      Modifier.fillMaxSize().background(Color.Black).clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
      ) {
        controlsVisible = !controlsVisible
      }
  ) {
    // 1. Smooth placeholder background using blurred podcast artwork (eliminates black screen
    // during load)
    AsyncImage(
      model = mediaMetadata?.thumbnailUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().blur(24.dp),
    )
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

    // 2. Video surface with transparent shutter and smooth crossfade
    AndroidView(
      factory = { viewContext ->
        PlayerView(viewContext).apply {
          layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          useController = false
          player = exoPlayer
          setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
          resizeMode =
            if (resizeZoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else AspectRatioFrameLayout.RESIZE_MODE_FIT
          setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
      },
      update = { playerView ->
        playerView.player = exoPlayer
        playerView.resizeMode =
          if (resizeZoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          else AspectRatioFrameLayout.RESIZE_MODE_FIT
      },
      modifier = Modifier.fillMaxSize().alpha(videoAlpha),
    )

    // 3. Center buffering indicator or unavailable message
    if (!isLoadingTracks && tracks.isEmpty()) {
      Column(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Video unavailable for this track",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
          onClick = { exitVideoPlayer() },
          shape = CircleShape,
          color = Color.White.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        ) {
          Text(
            text = "Return to Song",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    } else if (!isVideoReady || isBuffering) {
      CircularProgressIndicator(
        modifier = Modifier.size(52.dp).align(Alignment.Center),
        color = Color.White,
        strokeWidth = 3.dp,
      )
    }

    // 4. Controls overlay
    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(tween(250)),
      exit = fadeOut(tween(250)),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.70f),
                0.2f to Color.Black.copy(alpha = 0.25f),
                0.7f to Color.Black.copy(alpha = 0.25f),
                1.0f to Color.Black.copy(alpha = 0.85f),
              )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
      ) {
        // --- TOP BAR ---
        Row(
          modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
          ) {
            IconButton(
              onClick = { exitVideoPlayer() },
              modifier =
                Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
              Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
            }

            Spacer(Modifier.width(12.dp))

            Column {
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (author.isNotBlank()) {
                Text(
                  text = author,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.75f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          Spacer(Modifier.width(16.dp))

          // Song / Video Pill & Speed, Aspect Ratio, & Quality Controls
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
              modifier = Modifier.padding(end = 12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(2.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier.clip(CircleShape)
                      .clickable { exitVideoPlayer() }
                      .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = "Song",
                    color = Color.White.copy(alpha = 0.8f),
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                      ),
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier.clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.25f))
                      .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                  Icon(
                    painter = painterResource(R.drawable.ic_smart_display),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = "Video",
                    color = Color.White,
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                      ),
                  )
                }
              }
            }

            // Aspect Ratio toggle
            Surface(
              onClick = { resizeZoom = !resizeZoom },
              shape = RoundedCornerShape(16.dp),
              color = Color.White.copy(alpha = 0.18f),
              modifier = Modifier.padding(end = 8.dp),
            ) {
              Text(
                text = if (resizeZoom) "FILL" else "FIT",
                color = Color.White,
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                  ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }

            // Playback Speed button
            Surface(
              onClick = {
                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                val nextIndex = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                playbackSpeed = speeds[nextIndex]
                exoPlayer.setPlaybackSpeed(playbackSpeed)
              },
              shape = RoundedCornerShape(16.dp),
              color = Color.White.copy(alpha = 0.18f),
              modifier = Modifier.padding(end = 8.dp),
            ) {
              Text(
                text = "${playbackSpeed}x",
                color = Color.White,
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                  ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }

            // Quality selector dropdown
            Box {
              Surface(
                onClick = { qualityMenuOpen = true },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.18f),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Text(
                    text = selectedTrack?.label ?: "Quality",
                    color = Color.White,
                    style =
                      MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                      ),
                  )
                }
              }

              DropdownMenu(
                expanded = qualityMenuOpen,
                onDismissRequest = { qualityMenuOpen = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
              ) {
                tracks.forEach { track ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = track.label + if (track.isMuxed) "" else " (HD)",
                        color =
                          if (track == selectedTrack) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurface,
                        fontWeight =
                          if (track == selectedTrack) FontWeight.Bold else FontWeight.Normal,
                      )
                    },
                    onClick = {
                      selectedTrack = track
                      qualityMenuOpen = false
                      val currentPos = exoPlayer.currentPosition
                      val newSource = buildMediaSource(context, track, okHttpClient)
                      exoPlayer.setMediaSource(newSource)
                      exoPlayer.prepare()
                      if (currentPos > 0) {
                        exoPlayer.seekTo(currentPos)
                      }
                      exoPlayer.play()
                    },
                  )
                }
              }
            }
          }
        }

        // --- CENTER CONTROLS ---
        Row(
          modifier = Modifier.align(Alignment.Center),
          horizontalArrangement = Arrangement.spacedBy(40.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // -10s
          IconButton(
            onClick = {
              val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
              exoPlayer.seekTo(target)
              currentPosition = target
              lastKnownPosition = target
            },
            modifier =
              Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_replay_10),
              contentDescription = "Seek -10s",
              tint = Color.White,
              modifier = Modifier.size(30.dp),
            )
          }

          // Play / Pause
          IconButton(
            onClick = {
              if (exoPlayer.isPlaying) {
                exoPlayer.pause()
              } else {
                exoPlayer.play()
              }
            },
            modifier = Modifier.size(68.dp).background(Color.White, CircleShape),
          ) {
            Icon(
              painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = Color.Black,
              modifier = Modifier.size(36.dp),
            )
          }

          // +10s
          IconButton(
            onClick = {
              val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(duration)
              exoPlayer.seekTo(target)
              currentPosition = target
              lastKnownPosition = target
            },
            modifier =
              Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_forward_10),
              contentDescription = "Seek +10s",
              tint = Color.White,
              modifier = Modifier.size(30.dp),
            )
          }
        }

        // --- BOTTOM BAR ---
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
          // Time slider
          val displayPos = if (isScrubbing) scrubPosition else currentPosition
          val sliderValue =
            if (duration > 0) (displayPos.toFloat() / duration).coerceIn(0f, 1f) else 0f

          Slider(
            value = sliderValue,
            onValueChange = { frac ->
              isScrubbing = true
              scrubPosition = (frac * duration).toLong()
            },
            onValueChangeFinished = {
              exoPlayer.seekTo(scrubPosition)
              currentPosition = scrubPosition
              lastKnownPosition = scrubPosition
              isScrubbing = false
            },
            colors =
              SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
              ),
            modifier = Modifier.fillMaxWidth().height(24.dp),
          )

          // Timestamps
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = makeTimeString(displayPos),
              color = Color.White.copy(alpha = 0.85f),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            )

            Text(
              text = makeTimeString(duration),
              color = Color.White.copy(alpha = 0.85f),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            )
          }
        }
      }
    }
  }
}
