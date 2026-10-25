package com.music.echo.ui.screens.wrapped

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val SLIDE_COUNT = 9
private const val SLIDE_DURATION_MS = 8500L

@Composable
fun WrappedScreen(
  navController: NavController,
  viewModel: EchoWrappedViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val selectedRange by viewModel.selectedRange.collectAsState()
  val isSavingPlaylist by viewModel.isPlaylistSaving.collectAsState()
  val playlistSaved by viewModel.playlistSaved.collectAsState()
  val playerConnection = LocalPlayerConnection.current

  when (val state = uiState) {
    is WrappedUiState.Loading -> {
      Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07090E)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(
            color = Color(0xFFA855F7),
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp)
          )
          Spacer(modifier = Modifier.height(20.dp))
          Text(
            "Synthesizing your year in music…",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.3.sp)
          )
        }
      }
    }

    is WrappedUiState.InsufficientData -> {
      InsufficientDataView(
        state = state,
        onTryAllTime = { viewModel.setTimeRange(WrappedTimeRange.ALL_TIME) },
        onBack = { navController.navigateUp() }
      )
    }

    is WrappedUiState.Ready -> {
      val data = state.data
      var currentSlide by remember { mutableIntStateOf(0) }
      var isPaused by remember { mutableStateOf(false) }
      var slideProgress by remember { mutableFloatStateOf(0f) }
      val coroutineScope = rememberCoroutineScope()

      val currentSlideUpdated by rememberUpdatedState(currentSlide)
      val isPausedUpdated by rememberUpdatedState(isPaused)

      // Handle system back gesture
      BackHandler {
        currentSlide = 0
        slideProgress = 0f
        isPaused = false
        navController.navigateUp()
      }

      val isPlaybackPlaying by (playerConnection?.isPlaying ?: MutableStateFlow(false)).collectAsState()

      // Auto-play top song in the background when going through Wrapped
      LaunchedEffect(data.topSong) {
        val topSong = data.topSong
        if (topSong != null && playerConnection != null) {
          val currentId = playerConnection.mediaMetadata.value?.id
          if (currentId != topSong.id) {
            playerConnection.playQueue(
              YouTubeQueue(
                endpoint = WatchEndpoint(topSong.id),
                preloadItem = topSong.toMediaMetadata()
              )
            )
          } else if (!playerConnection.isPlaying.value) {
            playerConnection.play()
          }
        }
      }

      // Pause playback when leaving Wrapped
      DisposableEffect(Unit) {
        onDispose {
          playerConnection?.pause()
        }
      }

      // 60fps Smooth Slide Progress with accurate elapsed timer
      LaunchedEffect(currentSlide, isPaused) {
        if (isPaused) return@LaunchedEffect
        slideProgress = 0f
        val startTime = System.currentTimeMillis()

        while (!isPaused) {
          val elapsed = System.currentTimeMillis() - startTime
          val p = (elapsed.toFloat() / SLIDE_DURATION_MS).coerceIn(0f, 1f)
          slideProgress = p
          if (p >= 1f) {
            if (currentSlide < SLIDE_COUNT - 1) {
              currentSlide++
              slideProgress = 0f
            } else {
              isPaused = true
            }
            break
          }
          delay(16)
        }
      }

      // Dynamic Aurora Palette per slide
      val targetColors =
        when (currentSlide) {
          0 -> listOf(Color(0xFF6D28D9), Color(0xFF3B0764), Color(0xFF1E1B4B)) // Intro: Deep violet / night
          1 -> listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF022C22)) // Minutes: Emerald / cyan
          2 -> listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF1C1917)) // Horizons: Sunset amber
          3 -> listOf(Color(0xFFDB2777), Color(0xFF9D174D), Color(0xFF1E1B4B)) // Top Song: Electric Magenta
          4 -> listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF0F172A)) // Top 5 Songs: Cyber Blue
          5 -> listOf(Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFF1A130F)) // Top Artist: Golden Glow
          6 -> listOf(Color(0xFFDC2626), Color(0xFF991B1B), Color(0xFF180A0A)) // Top 5 Artists: Crimson
          7 -> listOf(Color(0xFF0D9488), Color(0xFF7C3AED), Color(0xFF0F172A)) // Persona: Holographic Aurora
          else -> listOf(Color(0xFF4338CA), Color(0xFF312E81), Color(0xFF090A0F)) // Grand Finale
        }

      Box(
        modifier =
          Modifier.fillMaxSize()
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  isPaused = true
                  tryAwaitRelease()
                  isPaused = false
                },
                onTap = { offset ->
                  val halfWidth = size.width / 2f
                  if (offset.x < halfWidth * 0.7f) {
                    if (currentSlideUpdated > 0) {
                      currentSlide = currentSlideUpdated - 1
                      slideProgress = 0f
                      isPaused = false
                    }
                  } else {
                    if (currentSlideUpdated < SLIDE_COUNT - 1) {
                      currentSlide = currentSlideUpdated + 1
                      slideProgress = 0f
                      isPaused = false
                    }
                  }
                }
              )
            }
      ) {
        // Dynamic Fluid Aurora Canvas Backdrop
        WrappedDynamicAurora(colors = targetColors)

        Column(
          modifier =
            Modifier.fillMaxSize()
              .systemBarsPadding()
        ) {
          // Top Segmented Progress Bar (iOS / Spotify style)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            for (i in 0 until SLIDE_COUNT) {
              val progress =
                when {
                  i < currentSlide -> 1f
                  i == currentSlide -> slideProgress
                  else -> 0f
                }
              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxHeight()
                      .fillMaxWidth(progress)
                      .background(Color.White)
                )
              }
            }
          }

          // Top Header: Sleek Pill Badges, Audio Toggle & Close Button
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Echo Replay Pill
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Box(
                    modifier =
                      Modifier.size(5.dp)
                        .background(Color(0xFF34D399), CircleShape)
                  )
                  Text(
                    "WRAPPED",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Black,
                      letterSpacing = 1.2.sp,
                      fontSize = 10.sp
                    ),
                    color = Color.White
                  )
                }
              }

              // Year Selector Pill (Clickable)
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable {
                      val nextRange =
                        when (selectedRange) {
                          WrappedTimeRange.THIS_YEAR -> WrappedTimeRange.LAST_YEAR
                          WrappedTimeRange.LAST_YEAR -> WrappedTimeRange.ALL_TIME
                          WrappedTimeRange.ALL_TIME -> WrappedTimeRange.THIS_YEAR
                        }
                      viewModel.setTimeRange(nextRange)
                      currentSlide = 0
                      slideProgress = 0f
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  data.yearLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = Color(0xFFFFD54F)
                )
              }
            }

            // Right Actions: Audio Toggle & Close Button
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Sound / Mute Toggle
              Box(
                modifier =
                  Modifier.size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                    .clickable {
                      if (isPlaybackPlaying) {
                        playerConnection?.pause()
                      } else {
                        playerConnection?.play()
                      }
                    },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  painter = painterResource(if (isPlaybackPlaying) R.drawable.volume_up else R.drawable.volume_off),
                  contentDescription = if (isPlaybackPlaying) "Mute Background Music" else "Play Background Music",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }

              // Close Button
              Box(
                modifier =
                  Modifier.size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                    .clickable { navController.navigateUp() },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  painter = painterResource(R.drawable.close),
                  contentDescription = "Close",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          // Main Interactive Slide Stage
          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            AnimatedContent(
              targetState = currentSlide,
              transitionSpec = {
                if (targetState > initialState) {
                  (slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                  ) { it / 3 } + fadeIn(tween(350)) + scaleIn(initialScale = 0.92f))
                    .togetherWith(
                      slideOutVertically { -it / 4 } + fadeOut(tween(250)) + scaleOut(targetScale = 1.05f)
                    )
                } else {
                  (slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                  ) { -it / 3 } + fadeIn(tween(350)) + scaleIn(initialScale = 1.05f))
                    .togetherWith(
                      slideOutVertically { it / 4 } + fadeOut(tween(250)) + scaleOut(targetScale = 0.92f)
                    )
                }
              },
              label = "SlideTransition"
            ) { slideIndex ->
              when (slideIndex) {
                0 -> IntroSlideModern(data) {
                  currentSlide = 1
                  slideProgress = 0f
                }
                1 -> TotalMinutesSlideModern(data, isPlaybackPlaying)
                2 -> ExplorationSlideModern(data)
                3 ->
                  TopSongSlideModern(
                    song = data.topSong,
                    data = data,
                    isPlaybackPlaying = isPlaybackPlaying,
                    onTogglePlay = {
                      if (isPlaybackPlaying) {
                        playerConnection?.pause()
                      } else {
                        playerConnection?.play()
                      }
                    }
                  )
                4 -> Top5SongsSlideModern(data)
                5 -> TopArtistSlideModern(data)
                6 -> Top5ArtistsSlideModern(data)
                7 -> VibeSlideModern(data)
                8 ->
                  SummarySlideModern(
                    data = data,
                    isSaving = isSavingPlaylist,
                    isSaved = playlistSaved,
                    onSavePlaylist = {
                      viewModel.createWrappedPlaylist(data) { success ->
                        Toast.makeText(
                          context,
                          if (success) "Saved 'Your ${data.yearLabel} Wrapped' to library!"
                          else "Failed to create playlist",
                          Toast.LENGTH_SHORT
                        ).show()
                      }
                    },
                    onShare = {
                      coroutineScope.launch {
                        WrappedShareHelper.shareWrappedSummary(context, data, isStoryFormat = true)
                      }
                    },
                    onReplay = {
                      currentSlide = 0
                      slideProgress = 0f
                      isPaused = false
                    }
                  )
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// DYNAMIC FLUID AURORA BACKDROP (Apple Music & Spotify Fluid Style)
// -------------------------------------------------------------
@Composable
private fun WrappedDynamicAurora(colors: List<Color>) {
  val infiniteTransition = rememberInfiniteTransition(label = "AuroraDrift")
  val time by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(18000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "auroraTime"
  )

  val c1 by animateColorAsState(colors.getOrElse(0) { Color(0xFF6D28D9) }, tween(1400), label = "c1")
  val c2 by animateColorAsState(colors.getOrElse(1) { Color(0xFF3B0764) }, tween(1400), label = "c2")
  val c3 by animateColorAsState(colors.getOrElse(2) { Color(0xFF0F172A) }, tween(1400), label = "c3")

  Canvas(
    modifier = Modifier.fillMaxSize()
  ) {
    val w = size.width
    val h = size.height

    // Base background
    drawRect(color = Color(0xFF07080E))

    // Orb 1: Upper oscillating radial bloom
    val o1X = w * (0.50f + 0.32f * sin(time))
    val o1Y = h * (0.28f + 0.18f * cos(time * 0.9f))
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(c1.copy(alpha = 0.70f), c1.copy(alpha = 0.25f), Color.Transparent),
        center = Offset(o1X, o1Y),
        radius = w * 0.85f
      ),
      center = Offset(o1X, o1Y),
      radius = w * 0.85f
    )

    // Orb 2: Lower-center oscillating radial bloom
    val o2X = w * (0.50f - 0.28f * cos(time + 1.8f))
    val o2Y = h * (0.62f + 0.20f * sin(time * 1.1f))
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(c2.copy(alpha = 0.65f), c2.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(o2X, o2Y),
        radius = w * 0.80f
      ),
      center = Offset(o2X, o2Y),
      radius = w * 0.80f
    )

    // Orb 3: Deep ambient base bloom
    val o3X = w * (0.35f + 0.30f * sin(time * 1.4f))
    val o3Y = h * (0.85f - 0.15f * cos(time * 0.7f))
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(c3.copy(alpha = 0.55f), Color.Transparent),
        center = Offset(o3X, o3Y),
        radius = w * 0.75f
      ),
      center = Offset(o3X, o3Y),
      radius = w * 0.75f
    )

    // Subtle dark gradient vignette for readable content
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.Black.copy(alpha = 0.25f),
          Color.Transparent,
          Color.Black.copy(alpha = 0.40f)
        )
      )
    )
  }
}

// -------------------------------------------------------------
// SLIDE 0: INTRO (Apple Music Replay Kinetic Intro)
// -------------------------------------------------------------
@Composable
private fun IntroSlideModern(data: WrappedData, onStartClick: () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "IntroSpin")
  val spin by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
    label = "introVinyl"
  )
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "introPulse"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
  ) {
    // Top Kicker Monospace Micro-Badge
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = 0.10f))
        .border(0.5.dp, Color(0xFFA78BFA).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
        Text(
          "ECHO MUSIC // ${data.yearLabel}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            fontSize = 10.sp
          ),
          color = Color.White
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Expressive Kinetic Typography
    Text(
      text = "YOUR YEAR IN",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 3.sp,
        fontSize = 24.sp
      ),
      color = Color.White.copy(alpha = 0.9f)
    )

    Text(
      text = "REPLAY",
      style = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Black,
        fontSize = 58.sp,
        letterSpacing = 4.sp,
        lineHeight = 60.sp
      ),
      color = Color(0xFFFFD54F)
    )

    Text(
      text = "Every beat, obsession, and melody that soundtracked your 2026.",
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
      color = Color.White.copy(alpha = 0.70f),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 24.dp)
    )

    Spacer(modifier = Modifier.height(26.dp))

    // Masterpiece 3D Grooved Vinyl LP with Acoustic Pulse Aura
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(240.dp)
    ) {
      // Expanding acoustic aura
      Box(
        modifier = Modifier
          .size(255.dp * pulseScale)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              listOf(Color(0xFF8B5CF6).copy(alpha = 0.35f), Color.Transparent)
            )
          )
      )

      // Spinning Vinyl LP with realistic sound grooves
      Box(
        modifier = Modifier
          .size(230.dp)
          .rotate(spin)
          .shadow(32.dp, CircleShape)
          .clip(CircleShape)
          .background(Color(0xFF0A0C14))
          .drawBehind {
            val r = size.minDimension / 2f
            // Concentric sonic tracks
            for (ratio in listOf(0.48f, 0.58f, 0.68f, 0.78f, 0.88f, 0.96f)) {
              drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r * ratio,
                style = Stroke(width = 1.2f)
              )
            }
            // Specular reflection band across opposite diagonals
            drawRect(
              brush = Brush.sweepGradient(
                listOf(
                  Color.Transparent,
                  Color.White.copy(alpha = 0.12f),
                  Color.Transparent,
                  Color.White.copy(alpha = 0.12f),
                  Color.Transparent
                )
              )
            )
          }
          .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        // Center Album Sticker
        Box(
          modifier = Modifier
            .size(94.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E1B4B))
            .border(2.dp, Color(0xFFFFD54F), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (!data.topSong?.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
              model = data.topSong.thumbnailUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
          } else {
            Icon(
              painter = painterResource(R.drawable.music_note),
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(36.dp)
            )
          }
          // Spindle center hole
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(Color(0xFF0A0C14))
              .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Quick Stats Preview Chips
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IntroStatBadge("${data.totalMinutes} Mins", Color(0xFF34D399))
      IntroStatBadge("${data.top5Songs.size} Top Tracks", Color(0xFF60A5FA))
      IntroStatBadge(data.dominantVibe, Color(0xFFF472B6))
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Prominent Glowing Start Button
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = Color(0xFF8B5CF6),
      shadowElevation = 10.dp,
      onClick = onStartClick,
      modifier = Modifier.fillMaxWidth(0.74f).height(50.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          "START YOUR REPLAY",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp),
          color = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          painter = painterResource(R.drawable.arrow_forward),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun IntroStatBadge(text: String, dotColor: Color) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .background(Color.White.copy(alpha = 0.08f))
      .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      Box(modifier = Modifier.size(5.dp).background(dotColor, CircleShape))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

// -------------------------------------------------------------
// SLIDE 1: MINUTES (Spotify Counter + Live EQ Waves)
// -------------------------------------------------------------
@Composable
private fun TotalMinutesSlideModern(data: WrappedData, isPlaybackPlaying: Boolean) {
  val animatedMinutes by animateIntAsState(
    targetValue = data.totalMinutes.toInt(),
    animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
    label = "minutesCount"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "// TOTAL LISTENING TIME",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 12.sp
      ),
      color = Color(0xFF6EE7B7)
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Giant Kinetic Counter
    Text(
      text = "%,d".format(animatedMinutes),
      style = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Black,
        fontSize = 76.sp,
        letterSpacing = (-3).sp,
        lineHeight = 74.sp
      ),
      color = Color.White
    )

    Text(
      text = "MINUTES",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp
      ),
      color = Color(0xFF34D399)
    )

    Spacer(modifier = Modifier.height(36.dp))

    // Glass Bento Stat Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      val hours = data.totalMinutes / 60
      GlassBentoCard(
        title = "HOURS",
        value = "$hours",
        subtitle = "Equivalent days: ${(hours / 24f * 10).toInt() / 10f}",
        modifier = Modifier.weight(1f)
      )

      GlassBentoCard(
        title = "STREAM TIER",
        value = if (data.totalMinutes > 300) "Top 10%" else "Explorer",
        subtitle = "Active music lover",
        modifier = Modifier.weight(1f),
        accentColor = Color(0xFF6EE7B7)
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Animated Live Soundwave Bars
    AnimatedEqualizerBars(color = Color(0xFF34D399), isPlaying = isPlaybackPlaying)
  }
}

// -------------------------------------------------------------
// SLIDE 2: EXPLORATION (Apple Music Bento Stack)
// -------------------------------------------------------------
@Composable
private fun ExplorationSlideModern(data: WrappedData) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "// YOUR EXPEDITIONS",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 12.sp
      ),
      color = Color(0xFFFDBA74)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "You explored an expansive universe.",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      color = Color.White,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Bento Card 1: Unique Tracks
    ExplorationBentoRow(
      number = data.uniqueSongsCount.toString(),
      label = "Unique Songs",
      sublabel = "Across your library & online streams",
      icon = R.drawable.music_note,
      accentColor = Color(0xFFF97316)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Bento Card 2: Unique Artists
    ExplorationBentoRow(
      number = data.uniqueArtistsCount.toString(),
      label = "Distinct Artists",
      sublabel = "Different voices and visions heard",
      icon = R.drawable.artist,
      accentColor = Color(0xFFEC4899)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Bento Card 3: Unique Albums
    ExplorationBentoRow(
      number = data.uniqueAlbumsCount.toString(),
      label = "Albums Explored",
      sublabel = "Complete projects and records",
      icon = R.drawable.album,
      accentColor = Color(0xFF38BDF8)
    )
  }
}

@Composable
private fun ExplorationBentoRow(
  number: String,
  label: String,
  sublabel: String,
  icon: Int,
  accentColor: Color
) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(Color.White.copy(alpha = 0.08f))
        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
        .padding(horizontal = 20.dp, vertical = 18.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.20f))
              .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(painter = painterResource(icon), contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Column {
          Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
          Text(sublabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
        }
      }
      Text(
        number,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 28.sp),
        color = Color.White
      )
    }
  }
}

// -------------------------------------------------------------
// SLIDE 3: #1 SONG (Vinyl Sleeve + Soundtracking Status)
// -------------------------------------------------------------
@Composable
private fun TopSongSlideModern(
  song: Song?,
  data: WrappedData,
  isPlaybackPlaying: Boolean,
  onTogglePlay: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "TopSongSpin")
  val spin by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
    label = "vinylSongSpin"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "// YOUR #1 SOUNDTRACK",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 11.sp
      ),
      color = Color(0xFFF472B6)
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "The Song That Defined Your 2026",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
      color = Color.White,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Hero Vinyl Record Sleeve + Sliding Grooved Vinyl
    Box(
      contentAlignment = Alignment.CenterStart,
      modifier = Modifier.height(230.dp).width(290.dp)
    ) {
      // Sliding grooved black vinyl record peeking out to the right
      Box(
        modifier =
          Modifier.size(200.dp)
            .offset(x = 80.dp)
            .rotate(spin)
            .shadow(20.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF0A0C14))
            .drawBehind {
              val r = size.minDimension / 2f
              for (ratio in listOf(0.55f, 0.68f, 0.80f, 0.92f)) {
                drawCircle(
                  color = Color.White.copy(alpha = 0.08f),
                  radius = r * ratio,
                  style = Stroke(width = 1.2f)
                )
              }
              drawRect(
                brush = Brush.sweepGradient(
                  listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                  )
                )
              )
            }
            .border(1.5.dp, Color.White.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        // Vinyl center label
        Box(
          modifier = Modifier.size(70.dp).clip(CircleShape).background(Color(0xFF2E1065)),
          contentAlignment = Alignment.Center
        ) {
          if (!song?.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
              model = song.thumbnailUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
          }
          Box(
            modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF0A0C14))
          )
        }
      }

      // Front Album Jacket / Sleeve
      AsyncImage(
        model = song?.thumbnailUrl,
        contentDescription = song?.title,
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.size(210.dp)
            .shadow(28.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFFDB2777))
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
      )
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Song Title & Artist
    Text(
      text = song?.title ?: "No Track",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        fontSize = 24.sp
      ),
      color = Color.White,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = song?.artists?.joinToString(", ") { it.name } ?: "Unknown Artist",
      style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
      color = Color.White.copy(alpha = 0.75f),
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Now Soundtracking Capsule
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White.copy(alpha = 0.12f),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
      onClick = onTogglePlay
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AnimatedEqualizerBars(color = Color(0xFF34D399), height = 18.dp, isPlaying = isPlaybackPlaying)
        Text(
          if (isPlaybackPlaying) "SOUNDTRACKING WRAPPED" else "AUDIO PAUSED",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
          color = Color.White
        )
        Icon(
          painter = painterResource(if (isPlaybackPlaying) R.drawable.pause else R.drawable.play),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(14.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Bento Stat Row
    val playCount = data.topSongStats?.songCountListened ?: 1
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth(0.85f)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White.copy(alpha = 0.08f))
          .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Played $playCount times", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF34D399))
          Text("Top Obsession", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.6f))
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White.copy(alpha = 0.08f))
          .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
          .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("#1 Most Played", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFFFFD54F))
          Text("Soundtrack of 2026", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.6f))
        }
      }
    }
  }
}

// -------------------------------------------------------------
// SLIDE 4: TOP 5 TRACKS (Spotify Ranked Countdown)
// -------------------------------------------------------------
@Composable
private fun Top5SongsSlideModern(data: WrappedData) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "// THE HEAVY ROTATION",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 12.sp
      ),
      color = Color(0xFF93C5FD)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Your Top 5 Songs",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
      color = Color.White
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Frosted Glass Ranked Card
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(26.dp))
          .background(Color.White.copy(alpha = 0.08f))
          .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
          .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.top5Songs.take(5).forEachIndexed { index, song ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (index == 0) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Rank Number
            Text(
              text = "0${index + 1}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = (-0.5).sp
              ),
              color = if (index == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.50f),
              modifier = Modifier.width(36.dp)
            )

            // Artwork
            AsyncImage(
              model = song.thumbnailUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier =
                Modifier.size(46.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
              Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (index == 0) {
              Icon(
                painter = painterResource(R.drawable.star),
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// SLIDE 5: #1 ARTIST (Spotify Hero Artist Portrait)
// -------------------------------------------------------------
@Composable
private fun TopArtistSlideModern(data: WrappedData) {
  val artist = data.topArtist
  val infiniteTransition = rememberInfiniteTransition(label = "ArtistHalo")
  val haloScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "artistHalo"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "// YOUR #1 ARTIST",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 12.sp
      ),
      color = Color(0xFFFCD34D)
    )

    Spacer(modifier = Modifier.height(30.dp))

    // Hero Circular Portrait with Breathing Amber Halo
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(230.dp)
    ) {
      Box(
        modifier =
          Modifier.size(230.dp * haloScale)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(Color(0xFFF59E0B).copy(alpha = 0.35f), Color.Transparent)
              )
            )
      )

      Box(
        modifier =
          Modifier.size(210.dp)
            .shadow(32.dp, CircleShape, spotColor = Color(0xFFF59E0B))
            .clip(CircleShape)
            .border(2.5.dp, Color(0xFFFFD54F), CircleShape)
      ) {
        if (!artist?.thumbnailUrl.isNullOrBlank()) {
          AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = artist.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF2E1065)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.artist),
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(72.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = artist?.title ?: "Various Artists",
      style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
      color = Color.White,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "You soundtracked ${data.totalMinutes} minutes of your life with their sound.",
      style = MaterialTheme.typography.bodyLarge,
      color = Color.White.copy(alpha = 0.80f),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 24.dp)
    )
  }
}

// -------------------------------------------------------------
// SLIDE 6: TOP 5 ARTISTS
// -------------------------------------------------------------
@Composable
private fun Top5ArtistsSlideModern(data: WrappedData) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = "// TOP ARTISTS",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 12.sp
      ),
      color = Color(0xFFF87171)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Your Top Creators",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
      color = Color.White
    )

    Spacer(modifier = Modifier.height(24.dp))

    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(26.dp))
          .background(Color.White.copy(alpha = 0.08f))
          .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
          .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.top5Artists.take(5).forEachIndexed { index, artist ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (index == 0) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "0${index + 1}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
              color = if (index == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.50f),
              modifier = Modifier.width(36.dp)
            )

            // Artist Avatar
            if (!artist.thumbnailUrl.isNullOrBlank()) {
              AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                  Modifier.size(46.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
              )
            } else {
              Box(
                modifier =
                  Modifier.size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E1065)),
                contentAlignment = Alignment.Center
              ) {
                Icon(painter = painterResource(R.drawable.artist), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                artist.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                "${data.totalMinutes} minutes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.60f)
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// SLIDE 7: MUSICAL PERSONA (Holographic Collector's Tarot Card)
// -------------------------------------------------------------
@Composable
private fun VibeSlideModern(data: WrappedData) {
  val infiniteTransition = rememberInfiniteTransition(label = "HoloSheen")
  val borderAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
    label = "holoBorder"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
  ) {
    Text(
      text = "// YOUR MUSICAL PERSONA",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 11.sp
      ),
      color = Color(0xFF5EEAD4)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Full-height Holographic Tarot Card
    Box(
      modifier =
        Modifier.fillMaxWidth(0.94f)
          .clip(RoundedCornerShape(32.dp))
          .background(Color(0xFF0E111F).copy(alpha = 0.90f))
          .border(
            width = 2.dp,
            brush = Brush.sweepGradient(
              listOf(
                Color(0xFF5EEAD4),
                Color(0xFFA78BFA),
                Color(0xFFF472B6),
                Color(0xFFFFD54F),
                Color(0xFF5EEAD4)
              )
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .padding(horizontal = 22.dp, vertical = 26.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Monospace Stamp
        Text(
          "● SONIC IDENTITY // ARCHETYPE NO. 07",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            fontSize = 9.sp
          ),
          color = Color(0xFF5EEAD4)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Glowing Celestial Sonic Emblem
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(90.dp)
        ) {
          Box(
            modifier = Modifier.size(86.dp).clip(CircleShape)
              .background(
                Brush.radialGradient(
                  listOf(Color(0xFF0D9488).copy(alpha = 0.40f), Color.Transparent)
                )
              )
          )
          Box(
            modifier = Modifier.size(68.dp).clip(CircleShape)
              .background(Color(0xFF13192B))
              .border(1.5.dp, Color(0xFF5EEAD4), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.star),
              contentDescription = null,
              tint = Color(0xFF5EEAD4),
              modifier = Modifier.size(34.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Archetype Name
        Text(
          text = data.dominantVibe.uppercase(),
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = 1.sp
          ),
          color = Color.White,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Trait Pills Row
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PersonaTraitPill("✦ Genre Defier")
          PersonaTraitPill("✦ Late-Night Voyager")
          PersonaTraitPill("✦ No Skips")
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Narrative Quote
        val vibeExplanation =
          when (data.dominantVibe) {
            "Certified Audiophile" -> "Music is not just background noise for you. It is your daily fuel, sanctuary, and frequency."
            "Musical Nomad" -> "You cross sonic borders effortlessly. Your taste spans worlds, rhythms, and horizons."
            "Infinite Playlist Curator" -> "Your library is endless. You love uncovering diamonds before the crowd catches on."
            "Devoted Superfan" -> "When an artist clicks with your soul, you stand by their discography faithfully on loop."
            else -> "You move with the rhythm, always listening deeply to whatever moves your spirit."
          }

        Text(
          text = vibeExplanation,
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
          color = Color.White.copy(alpha = 0.80f),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sonic Fingerprint Progress Meters
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SonicRadarBar("Exploration", 0.98f, Color(0xFF5EEAD4))
          SonicRadarBar("Diversity", 0.94f, Color(0xFFA78BFA))
          SonicRadarBar("Replay Loyalty", 0.88f, Color(0xFFF472B6))
        }
      }
    }
  }
}

@Composable
private fun PersonaTraitPill(label: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color.White.copy(alpha = 0.08f))
      .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
      color = Color(0xFFE2E8F0)
    )
  }
}

@Composable
private fun SonicRadarBar(label: String, progress: Float, color: Color) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.7f))
      Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = color)
    }
    Spacer(modifier = Modifier.height(3.dp))
    Box(
      modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))
    ) {
      Box(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(color)
      )
    }
  }
}

// -------------------------------------------------------------
// SLIDE 8: SUMMARY & GRAND FINALE (Festival Poster Bento)
// -------------------------------------------------------------
@Composable
private fun SummarySlideModern(
  data: WrappedData,
  isSaving: Boolean,
  isSaved: Boolean,
  onSavePlaylist: () -> Unit,
  onShare: () -> Unit,
  onReplay: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
  ) {
    Text(
      text = "// 2026 COMPLETE RECAP",
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        fontSize = 11.sp
      ),
      color = Color(0xFFFFD54F)
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Festival Poster Bento Card
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(28.dp))
          .background(Color(0xFF0F121E).copy(alpha = 0.90f))
          .border(
            width = 1.dp,
            brush = Brush.linearGradient(
              listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color(0xFF38BDF8).copy(alpha = 0.3f), Color.White.copy(alpha = 0.15f))
            ),
            shape = RoundedCornerShape(28.dp)
          )
          .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
            Text(
              "ECHO WRAPPED",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                fontSize = 15.sp
              ),
              color = Color.White
            )
          }
          Text(
            data.yearLabel,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp
            ),
            color = Color(0xFFFFD54F)
          )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.8.dp)

        // 2. Champions Row: Top Track + Top Artist side by side
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Top Song Bento
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AsyncImage(
                model = data.topSong?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
              )
              Column(modifier = Modifier.weight(1f)) {
                Text("TOP TRACK", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFFF472B6))
                Text(
                  data.topSong?.title ?: "-",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  data.topSong?.artists?.joinToString(", ") { it.name } ?: "",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = Color.White.copy(alpha = 0.6f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Top Artist Bento
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2E1065)),
                contentAlignment = Alignment.Center
              ) {
                Icon(painter = painterResource(R.drawable.artist), contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(22.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("TOP ARTIST", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFFFFD54F))
                Text(
                  data.topArtist?.title ?: "-",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  "${data.totalMinutes} mins",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = Color.White.copy(alpha = 0.6f)
                )
              }
            }
          }
        }

        // 3. Top 5 Tracks Compact Row List
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(Color.White.copy(alpha = 0.04f))
              .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            "TOP REPLAYS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = Color(0xFFA78BFA)
          )
          data.top5Songs.take(4).forEachIndexed { i, s ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "0${i + 1}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = if (i == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.width(22.dp)
              )
              Text(
                s.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              Text(
                s.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // 4. Trio Metrics Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          SummaryStatPill("${data.totalMinutes}", "MINUTES", Color(0xFF34D399))
          SummaryStatPill("${data.uniqueSongsCount}", "SONGS", Color(0xFF60A5FA))
          SummaryStatPill("${data.uniqueArtistsCount}", "ARTISTS", Color(0xFFF472B6))
        }

        // 5. Persona Badge
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFF6366F1).copy(alpha = 0.25f), Color(0xFF0D9488).copy(alpha = 0.25f))
                )
              )
              .border(0.5.dp, Color(0xFF818CF8).copy(alpha = 0.40f), RoundedCornerShape(16.dp))
              .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Sonic Archetype", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.7f))
            Text(data.dominantVibe, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 11.sp), color = Color(0xFF5EEAD4))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Action Button 1: Share Story
    Button(
      onClick = onShare,
      modifier = Modifier.fillMaxWidth(0.92f).height(48.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
      shape = RoundedCornerShape(24.dp),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
      Icon(painter = painterResource(R.drawable.share), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Share Your Story", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Action Button 2: Save Playlist
    FilledTonalButton(
      onClick = onSavePlaylist,
      enabled = !isSaving && !isSaved,
      modifier = Modifier.fillMaxWidth(0.92f).height(46.dp),
      colors =
        ButtonDefaults.filledTonalButtonColors(
          containerColor = if (isSaved) Color(0xFF059669) else Color.White.copy(alpha = 0.12f)
        ),
      shape = RoundedCornerShape(24.dp)
    ) {
      if (isSaving) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
      } else {
        Icon(
          painter = painterResource(if (isSaved) R.drawable.check else R.drawable.playlist_add),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          if (isSaved) "Playlist Added to Library" else "Save Wrapped Playlist",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    TextButton(onClick = onReplay) {
      Icon(painter = painterResource(R.drawable.replay), contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Replay Experience", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
private fun SummaryStatPill(value: String, label: String, accentColor: Color) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(horizontal = 8.dp)
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
      color = accentColor
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
      color = Color.White.copy(alpha = 0.6f)
    )
  }
}

@Composable
private fun GlassBentoCard(
  title: String,
  value: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  accentColor: Color = Color.White
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(alpha = 0.08f))
        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
        .padding(18.dp)
  ) {
    Column {
      Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = accentColor.copy(alpha = 0.8f))
      Spacer(modifier = Modifier.height(8.dp))
      Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
      Spacer(modifier = Modifier.height(4.dp))
      Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.6f))
    }
  }
}

// -------------------------------------------------------------
// ANIMATED LIVE EQUALIZER BARS
// -------------------------------------------------------------
@Composable
private fun AnimatedEqualizerBars(
  color: Color,
  height: Dp = 28.dp,
  isPlaying: Boolean = true
) {
  val infiniteTransition = rememberInfiniteTransition(label = "EQBars")
  val bar1 by infiniteTransition.animateFloat(0.3f, 1.0f, infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "b1")
  val bar2 by infiniteTransition.animateFloat(0.6f, 0.2f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "b2")
  val bar3 by infiniteTransition.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "b3")
  val bar4 by infiniteTransition.animateFloat(0.8f, 0.4f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "b4")
  val bar5 by infiniteTransition.animateFloat(0.4f, 1.0f, infiniteRepeatable(tween(490, easing = LinearEasing), RepeatMode.Reverse), label = "b5")

  Row(
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = Modifier.height(height)
  ) {
    listOf(bar1, bar2, bar3, bar4, bar5).forEach { scale ->
      val actualScale = if (isPlaying) scale else 0.25f
      Box(
        modifier =
          Modifier.width(3.5.dp)
            .fillMaxHeight(actualScale)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
      )
    }
  }
}

@Composable
private fun InsufficientDataView(
  state: WrappedUiState.InsufficientData,
  onTryAllTime: () -> Unit,
  onBack: () -> Unit
) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF07090E))
        .systemBarsPadding()
        .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier =
          Modifier.size(80.dp)
            .clip(CircleShape)
            .background(Color(0xFF6366F1).copy(alpha = 0.20f))
            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.40f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(R.drawable.star),
          contentDescription = null,
          tint = Color(0xFFA78BFA),
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        "More Music Needed",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
        color = Color.White,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        "You've played ${state.songsPlayed} song(s) so far. Echo Wrapped requires at least ${state.minSongsRequired} songs to generate your personal musical journey.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = onTryAllTime,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
        shape = RoundedCornerShape(20.dp)
      ) {
        Text("Switch to All-Time Stats", color = Color.White, style = MaterialTheme.typography.labelLarge)
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = onBack,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
      ) {
        Text("Back to Music", color = Color.White)
      }
    }
  }
}
