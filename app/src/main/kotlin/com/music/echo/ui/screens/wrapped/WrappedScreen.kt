package com.music.echo.ui.screens.wrapped

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import echo.music.iad1tya.db.entities.Artist
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val SLIDE_COUNT = 9
private const val SLIDE_DURATION_MS = 8500L

private val Song?.artistsText: String
  get() = this?.artists?.joinToString(", ") { it.name } ?: "Various Artists"

@Immutable
data class SpotifySlideTheme(
  val background: Color,
  val headline: Color,
  val label: Color,
  val bodyText: Color,
  val logoTint: Color,
  val cardSurface: Color,
  val progressColor: Color
)

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
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0E)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator(
            color = Color(0xFF00F5D4),
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp)
          )
          Spacer(modifier = Modifier.height(20.dp))
          Text(
            "Synthesizing your year in music…",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
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
      var selectedPosterTheme by remember { mutableStateOf(WrappedPosterTheme.VIOLET_MINT) }
      val coroutineScope = rememberCoroutineScope()

      val currentSlideUpdated by rememberUpdatedState(currentSlide)

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

      // Authentic Spotify Wrapped Distinct Color Themes per slide
      val currentTheme = when (currentSlide) {
        0 -> SpotifySlideTheme(
          background = Color(0xFF5925DC), // Electric Violet (John Mayer vibe)
          headline = Color(0xFFFFFFFF),
          label = Color(0xFF00F5D4),      // Neon Mint
          bodyText = Color(0xFFE9D5FF),
          logoTint = Color(0xFFFFFFFF),
          cardSurface = Color(0xFF4318B0),
          progressColor = Color(0xFF00F5D4)
        )
        1 -> SpotifySlideTheme(
          background = Color(0xFFFFC700), // Punchy Sunny Yellow (Anne-Marie)
          headline = Color(0xFF0A0A0A),   // Deep Black
          label = Color(0xFF1E293B),
          bodyText = Color(0xFF1E293B),
          logoTint = Color(0xFF0A0A0A),
          cardSurface = Color(0xFFE5B300),
          progressColor = Color(0xFF0A0A0A)
        )
        2 -> SpotifySlideTheme(
          background = Color(0xFF025940), // Forest Emerald Green
          headline = Color(0xFF34D399),   // Electric Mint
          label = Color(0xFFA7F3D0),
          bodyText = Color(0xFFE6FFFA),
          logoTint = Color(0xFFFFFFFF),
          cardSurface = Color(0xFF034431),
          progressColor = Color(0xFF34D399)
        )
        3 -> SpotifySlideTheme(
          background = Color(0xFFE02424), // Fire Red / Crimson (Charlie Puth)
          headline = Color(0xFFFFDF00),   // Canary Yellow
          label = Color(0xFFFFDF00),
          bodyText = Color(0xFFFFFFFF),
          logoTint = Color(0xFFFFDF00),
          cardSurface = Color(0xFFB91C1C),
          progressColor = Color(0xFFFFDF00)
        )
        4 -> SpotifySlideTheme(
          background = Color(0xFF1E40AF), // Royal Cobalt Blue
          headline = Color(0xFFFFFFFF),
          label = Color(0xFF38BDF8),      // Sky Blue
          bodyText = Color(0xFFDBEAFE),
          logoTint = Color(0xFFFFFFFF),
          cardSurface = Color(0xFF1E3A8A),
          progressColor = Color(0xFF38BDF8)
        )
        5 -> SpotifySlideTheme(
          background = Color(0xFF4C1D95), // Deep Violet / Indigo (John Mayer)
          headline = Color(0xFF00F5D4),   // Neon Mint / Cyan
          label = Color(0xFF00F5D4),
          bodyText = Color(0xFFF3E8FF),
          logoTint = Color(0xFF00F5D4),
          cardSurface = Color(0xFF3B0764),
          progressColor = Color(0xFF00F5D4)
        )
        6 -> SpotifySlideTheme(
          background = Color(0xFFEA580C), // Warm Sunset Tangerine (Kehlani)
          headline = Color(0xFFFFFBEB),   // Warm Cream
          label = Color(0xFFFEF3C7),
          bodyText = Color(0xFFFFEDD5),
          logoTint = Color(0xFFFFFBEB),
          cardSurface = Color(0xFFC2410C),
          progressColor = Color(0xFFFFFBEB)
        )
        7 -> SpotifySlideTheme(
          background = Color(0xFF701A75), // Deep Plum Magenta
          headline = Color(0xFFA3E635),   // Acid Lime
          label = Color(0xFFA3E635),
          bodyText = Color(0xFFFDF4FF),
          logoTint = Color(0xFFA3E635),
          cardSurface = Color(0xFF4A044E),
          progressColor = Color(0xFFA3E635)
        )
        else -> SpotifySlideTheme(
          background = selectedPosterTheme.composeBg,
          headline = selectedPosterTheme.composeHeadline,
          label = selectedPosterTheme.composeAccent,
          bodyText = selectedPosterTheme.composeSecondary,
          logoTint = selectedPosterTheme.composeHeadline,
          cardSurface = selectedPosterTheme.composeBg,
          progressColor = selectedPosterTheme.composeAccent
        )
      }

      val animatedBgColor by animateColorAsState(
        targetValue = currentTheme.background,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "SpotifyBgAnim"
      )

      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(animatedBgColor)
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  isPaused = true
                  tryAwaitRelease()
                  isPaused = false
                },
                onTap = { offset ->
                  val screenWidth = size.width
                  if (offset.x < screenWidth * 0.35f) {
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
        Column(
          modifier =
            Modifier.fillMaxSize()
              .systemBarsPadding()
        ) {
          // Top Segmented Progress Bar (Spotify Story style)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                    .background(currentTheme.headline.copy(alpha = 0.25f))
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxHeight()
                      .fillMaxWidth(progress)
                      .background(currentTheme.progressColor)
                )
              }
            }
          }

          // Top Header: Echo Music Logo + Wordmark + Year Selector + Controls
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Brand Logo & Wordmark (Authentic Spotify placement)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Image(
                painter = painterResource(R.drawable.ic_launcher_nobg),
                contentDescription = "Echo Music Logo",
                colorFilter = ColorFilter.tint(currentTheme.logoTint),
                modifier = Modifier.size(24.dp)
              )
              Text(
                "Echo Music",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = (-0.5).sp,
                  fontSize = 17.sp
                ),
                color = currentTheme.logoTint
              )

              // Clickable Year Selector Pill
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(12.dp))
                    .background(currentTheme.headline.copy(alpha = 0.16f))
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
                    .padding(horizontal = 9.dp, vertical = 3.dp)
              ) {
                Text(
                  data.yearLabel,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                  ),
                  color = currentTheme.headline
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
                    .background(currentTheme.headline.copy(alpha = 0.16f))
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
                  tint = currentTheme.headline,
                  modifier = Modifier.size(16.dp)
                )
              }

              // Close Button
              Box(
                modifier =
                  Modifier.size(34.dp)
                    .clip(CircleShape)
                    .background(currentTheme.headline.copy(alpha = 0.16f))
                    .clickable { navController.navigateUp() },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  painter = painterResource(R.drawable.close),
                  contentDescription = "Close",
                  tint = currentTheme.headline,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            AnimatedContent(
              targetState = currentSlide,
              transitionSpec = {
                if (targetState > initialState) {
                  (slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                  ) { it / 3 } + fadeIn(tween(350)))
                    .togetherWith(
                      slideOutVertically { -it / 4 } + fadeOut(tween(250))
                    )
                } else {
                  (slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                  ) { -it / 3 } + fadeIn(tween(350)))
                    .togetherWith(
                      slideOutVertically { it / 4 } + fadeOut(tween(250))
                    )
                }
              },
              label = "SpotifySlideTransition"
            ) { slideIndex ->
              when (slideIndex) {
                0 -> IntroSlideSpotify(data, currentTheme) {
                  currentSlide = 1
                  slideProgress = 0f
                }
                1 -> MinutesSlideSpotify(data, currentTheme)
                2 -> ExplorationSlideSpotify(data, currentTheme)
                3 ->
                  TopSongSlideSpotify(
                    song = data.topSong,
                    data = data,
                    theme = currentTheme,
                    isPlaybackPlaying = isPlaybackPlaying,
                    onTogglePlay = {
                      if (isPlaybackPlaying) {
                        playerConnection?.pause()
                      } else {
                        playerConnection?.play()
                      }
                    }
                  )
                4 -> Top5SongsSlideSpotify(data, currentTheme)
                5 -> TopArtistSlideSpotify(data, currentTheme)
                6 -> Top5ArtistsSlideSpotify(data, currentTheme)
                7 -> MusicalPersonaSlideSpotify(data, currentTheme)
                8 ->
                  RecapSlideSpotify(
                    data = data,
                    theme = currentTheme,
                    selectedPosterTheme = selectedPosterTheme,
                    onSelectPosterTheme = { selectedPosterTheme = it },
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
                        WrappedShareHelper.shareWrappedSummary(context, data, posterTheme = selectedPosterTheme)
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
// SLIDE 0: INTRO POSTER (Electric Violet & Neon Mint)
// -------------------------------------------------------------
@Composable
private fun IntroSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme,
  onStartClick: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "YOUR ${data.yearLabel.uppercase()} WRAPPED",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 13.sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "THE MUSIC\nTHAT DEFINED\nYOUR YEAR.",
        style = MaterialTheme.typography.displayMedium.copy(
          fontWeight = FontWeight.Black,
          fontSize = 46.sp,
          lineHeight = 48.sp,
          letterSpacing = (-2).sp
        ),
        color = theme.headline
      )
    }

    // Hero Geometric Album Art Stack
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      contentAlignment = Alignment.Center
    ) {
      // Background Accent Offset Card
      Box(
        modifier = Modifier
          .size(240.dp)
          .offset(x = 12.dp, y = 12.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(theme.label)
      )

      // Main Artwork Card
      Box(
        modifier = Modifier
          .size(240.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(theme.cardSurface)
          .shadow(16.dp, RoundedCornerShape(16.dp))
      ) {
        if (!data.topSong?.thumbnailUrl.isNullOrBlank()) {
          AsyncImage(
            model = data.topSong.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize().background(theme.cardSurface),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(R.drawable.ic_launcher_nobg),
              contentDescription = null,
              colorFilter = ColorFilter.tint(theme.headline),
              modifier = Modifier.size(72.dp)
            )
          }
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text(
        text = "Every beat, replay, and late-night obsession that soundtracked your life on Echo Music.",
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 22.sp
        ),
        color = theme.bodyText
      )

      // Stat Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SpotifyStatPill("${data.totalMinutes} MINS", theme)
        SpotifyStatPill("${data.uniqueSongsCount} SONGS", theme)
        SpotifyStatPill("${data.uniqueArtistsCount} ARTISTS", theme)
      }

      // Punchy Primary Button
      Button(
        onClick = onStartClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = theme.label,
          contentColor = theme.background
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp)
      ) {
        Text(
          "START YOUR REPLAY",
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            fontSize = 13.sp
          )
        )
      }
    }
  }
}

// -------------------------------------------------------------
// SLIDE 1: TOTAL MINUTES (Anne-Marie Sunny Yellow Aesthetic)
// -------------------------------------------------------------
@Composable
private fun MinutesSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "TOTAL LISTENING TIME",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 13.sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "${data.totalMinutes}",
        style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.Black,
          fontSize = 92.sp,
          lineHeight = 92.sp,
          letterSpacing = (-4).sp
        ),
        color = theme.headline
      )

      Text(
        text = "MINUTES",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Black,
          fontSize = 32.sp,
          letterSpacing = 3.sp
        ),
        color = theme.headline
      )
    }

    // Editorial Wording
    Text(
      text = "You spent ${data.totalMinutes} minutes listening to music this year. That's ${(data.totalMinutes / 60.0).let { String.format("%.1f", it) }} hours of soundtracking your everyday life.",
      style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp
      ),
      color = theme.bodyText
    )

    // Contrast Bento Blocks
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(theme.headline.copy(alpha = 0.08f))
          .padding(16.dp)
      ) {
        Column {
          Text(
            "${data.totalMinutes / 60} HOURS",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = theme.headline
          )
          Text(
            "Total Airtime",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = theme.bodyText
          )
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(theme.headline.copy(alpha = 0.08f))
          .padding(16.dp)
      ) {
        Column {
          Text(
            if (data.totalMinutes > 3000) "Top 5%" else "Explorer",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = theme.headline
          )
          Text(
            "Listener Tier",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = theme.bodyText
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// SLIDE 2: EXPLORATION (Forest Emerald Aesthetic)
// -------------------------------------------------------------
@Composable
private fun ExplorationSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "HORIZONS EXPLORED",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 13.sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "You couldn't\nbe fenced in.",
        style = MaterialTheme.typography.displayMedium.copy(
          fontWeight = FontWeight.Black,
          fontSize = 46.sp,
          lineHeight = 48.sp,
          letterSpacing = (-2).sp
        ),
        color = theme.headline
      )
    }

    // 2 Giant Stat Blocks
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(theme.cardSurface)
          .padding(20.dp)
      ) {
        Column {
          Text(
            text = "${data.uniqueSongsCount}",
            style = MaterialTheme.typography.displaySmall.copy(
              fontWeight = FontWeight.Black,
              fontSize = 58.sp,
              lineHeight = 58.sp,
              letterSpacing = (-2).sp
            ),
            color = theme.headline
          )
          Text(
            text = "DIFFERENT SONGS PLAYED",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            ),
            color = theme.label
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(theme.cardSurface)
          .padding(20.dp)
      ) {
        Column {
          Text(
            text = "${data.uniqueArtistsCount}",
            style = MaterialTheme.typography.displaySmall.copy(
              fontWeight = FontWeight.Black,
              fontSize = 58.sp,
              lineHeight = 58.sp,
              letterSpacing = (-2).sp
            ),
            color = theme.headline
          )
          Text(
            text = "DISTINCT ARTISTS DISCOVERED",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            ),
            color = theme.label
          )
        }
      }
    }

    Text(
      text = "From familiar favorites to fresh discoveries, your library had zero boundaries.",
      style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
      ),
      color = theme.bodyText
    )
  }
}

// -------------------------------------------------------------
// SLIDE 3: TOP SONG (Charlie Puth Fire Red & Yellow Aesthetic)
// -------------------------------------------------------------
@Composable
private fun TopSongSlideSpotify(
  song: Song?,
  data: WrappedData,
  theme: SpotifySlideTheme,
  isPlaybackPlaying: Boolean,
  onTogglePlay: () -> Unit
) {
  val title = song?.title ?: "No Song Found"
  val artists = song?.artistsText ?: "Various Artists"
  val playCount = data.topSongStats?.songCountListened ?: 1

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "Top Track",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
          letterSpacing = (-0.5).sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.displaySmall.copy(
          fontWeight = FontWeight.Black,
          fontSize = 44.sp,
          lineHeight = 46.sp,
          letterSpacing = (-1.5).sp
        ),
        color = theme.headline,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = artists,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 18.sp
        ),
        color = theme.bodyText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Hero Offset Artwork (Charlie Puth style)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(240.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(theme.cardSurface)
          .shadow(16.dp, RoundedCornerShape(14.dp))
      ) {
        if (!song?.thumbnailUrl.isNullOrBlank()) {
          AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize().background(theme.cardSurface),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.music_note),
              contentDescription = null,
              tint = theme.headline,
              modifier = Modifier.size(64.dp)
            )
          }
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Authentic Spotify Editorial Text
      Text(
        text = "You spent ${playCount * 4} minutes with your favorite track, playing it $playCount times, and the pleasure is all theirs.",
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 17.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 24.sp
        ),
        color = theme.bodyText
      )

      // Live Soundtrack Capsule
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = theme.headline,
        modifier = Modifier.clickable(onClick = onTogglePlay)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AnimatedEqualizerBars(
            color = theme.background,
            height = 16.dp,
            isPlaying = isPlaybackPlaying
          )
          Text(
            "SOUNDTRACKING WRAPPED",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
              fontSize = 10.sp
            ),
            color = theme.background
          )
          Icon(
            painter = painterResource(if (isPlaybackPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = null,
            tint = theme.background,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// SLIDE 4: TOP 5 SONGS (Royal Cobalt Blue Countdown)
// -------------------------------------------------------------
@Composable
private fun Top5SongsSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "HEAVY ROTATION",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 13.sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Your Top 5 Songs",
        style = MaterialTheme.typography.displaySmall.copy(
          fontWeight = FontWeight.Black,
          fontSize = 40.sp,
          lineHeight = 42.sp,
          letterSpacing = (-1.5).sp
        ),
        color = theme.headline
      )
    }

    // Top 5 Editorial Numbered Rows
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      data.top5Songs.take(5).forEachIndexed { index, song ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (index == 0) theme.cardSurface else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "0${index + 1}",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              fontSize = 26.sp,
              letterSpacing = (-1).sp
            ),
            color = if (index == 0) theme.headline else theme.label
          )

          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(theme.cardSurface)
          ) {
            if (!song.thumbnailUrl.isNullOrBlank()) {
              AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = song.title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              color = theme.headline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = song.artistsText ?: "Various",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp
              ),
              color = theme.bodyText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    Text(
      text = "Out of everything in your rotation, these five never missed.",
      style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
      ),
      color = theme.bodyText
    )
  }
}

// -------------------------------------------------------------
// SLIDE 5: TOP ARTIST (John Mayer Deep Violet & Cyan Aesthetic)
// -------------------------------------------------------------
@Composable
private fun TopArtistSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme
) {
  val artist = data.topArtist
  val artistName = artist?.title ?: "No Artist Found"
  val timeMinutes = artist?.timeListened?.let { it / 60000L } ?: data.totalMinutes

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "Top Artist",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
          letterSpacing = (-0.5).sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = artistName,
        style = MaterialTheme.typography.displaySmall.copy(
          fontWeight = FontWeight.Black,
          fontSize = 50.sp,
          lineHeight = 52.sp,
          letterSpacing = (-2).sp
        ),
        color = theme.headline,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Hero Offset Portrait (John Mayer style)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(240.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(theme.cardSurface)
          .shadow(16.dp, RoundedCornerShape(14.dp))
      ) {
        if (!artist?.thumbnailUrl.isNullOrBlank()) {
          AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = artistName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize().background(theme.cardSurface),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(R.drawable.person),
              contentDescription = null,
              tint = theme.headline,
              modifier = Modifier.size(64.dp)
            )
          }
        }
      }
    }

    // Exact Spotify Reference Text
    Text(
      text = "You spent $timeMinutes minutes with your favorite artist $artistName, and the pleasure is all theirs.",
      style = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 26.sp
      ),
      color = theme.bodyText
    )
  }
}

// -------------------------------------------------------------
// SLIDE 6: TOP 5 ARTISTS (Kehlani Sunset Tangerine Aesthetic)
// -------------------------------------------------------------
@Composable
private fun Top5ArtistsSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "CREATOR CIRCLE",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 13.sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Your Top Artists",
        style = MaterialTheme.typography.displaySmall.copy(
          fontWeight = FontWeight.Black,
          fontSize = 40.sp,
          lineHeight = 42.sp,
          letterSpacing = (-1.5).sp
        ),
        color = theme.headline
      )
    }

    // Top 5 Artists List
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      data.top5Artists.take(5).forEachIndexed { index, artist ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (index == 0) theme.cardSurface else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "0${index + 1}",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              fontSize = 26.sp,
              letterSpacing = (-1).sp
            ),
            color = if (index == 0) theme.headline else theme.label
          )

          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(theme.cardSurface)
          ) {
            if (!artist.thumbnailUrl.isNullOrBlank()) {
              AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = artist.title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              color = theme.headline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${artist.songCount} songs in rotation",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp
              ),
              color = theme.bodyText
            )
          }
        }
      }
    }

    Text(
      text = "These creators shaped the sound of your year.",
      style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
      ),
      color = theme.bodyText
    )
  }
}

// -------------------------------------------------------------
// SLIDE 7: MUSICAL PERSONA (Plum & Acid Lime Aesthetic)
// -------------------------------------------------------------
@Composable
private fun MusicalPersonaSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme
) {
  val vibeExplanation =
    when (data.dominantVibe) {
      "Certified Audiophile" -> "Music is your fuel, sanctuary, and frequency. You don't just listen—you live inside the sound."
      "Musical Nomad" -> "You cross sonic borders effortlessly. Your taste spans worlds, moods, and eras without missing a beat."
      "Infinite Playlist Curator" -> "Your queue is bottomless. You unearth diamonds before the rest of the world catches on."
      "Devoted Superfan" -> "When an artist clicks with your soul, you stand by their discography faithfully on repeat."
      else -> "You move with the rhythm, always listening deeply to whatever moves your spirit."
    }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "LISTENING PERSONA",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 13.sp
        ),
        color = theme.label
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = data.dominantVibe.uppercase(),
        style = MaterialTheme.typography.displaySmall.copy(
          fontWeight = FontWeight.Black,
          fontSize = 44.sp,
          lineHeight = 46.sp,
          letterSpacing = (-1.5).sp
        ),
        color = theme.headline
      )
    }

    // Persona Trait Tags & Radar
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpotifyTraitTag("✦ Genre Defier", theme)
        SpotifyTraitTag("✦ Deep Cuts", theme)
        SpotifyTraitTag("✦ High Replay", theme)
      }

      Text(
        text = vibeExplanation,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 26.sp
        ),
        color = theme.bodyText
      )

      // Sonic Fingerprint Bars
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SpotifyRadarMeter("Exploration", 0.98f, theme)
        SpotifyRadarMeter("Diversity", 0.92f, theme)
        SpotifyRadarMeter("Loyalty", 0.85f, theme)
      }
    }

    Text(
      text = "Your listening fingerprint was completely unique.",
      style = MaterialTheme.typography.bodySmall.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
      ),
      color = theme.bodyText
    )
  }
}

// -------------------------------------------------------------
// SLIDE 8: THE COMPLETE RECAP (9:16 Spotify Wrapped Poster)
// -------------------------------------------------------------
@Composable
private fun RecapSlideSpotify(
  data: WrappedData,
  theme: SpotifySlideTheme,
  selectedPosterTheme: WrappedPosterTheme,
  onSelectPosterTheme: (WrappedPosterTheme) -> Unit,
  isSaving: Boolean,
  isSaved: Boolean,
  onSavePlaylist: () -> Unit,
  onShare: () -> Unit,
  onReplay: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Content Block (Authentic Spotify Poster, Full Bleed, Zero Bento Boxes)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Editorial Section Headline
      Column {
        Text(
          text = "YOUR YEAR IN SOUND",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 11.sp
          ),
          color = theme.label
        )
        Text(
          text = "My Top Songs & Artists",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            lineHeight = 28.sp,
            letterSpacing = (-1).sp
          ),
          color = theme.headline
        )
      }

      // 2. Dual Hero Highlights Row (Top Track & Top Artist - Clean Editorial Presentation)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Top Track
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            if (!data.topSong?.thumbnailUrl.isNullOrBlank()) {
              AsyncImage(
                model = data.topSong.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = theme.headline,
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "TOP TRACK",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              ),
              color = theme.label
            )
            Text(
              data.topSong?.title ?: "Various",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 13.5.sp
              ),
              color = theme.headline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              data.topSong?.artistsText ?: "",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
              color = theme.bodyText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Top Artist
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            if (!data.topArtist?.thumbnailUrl.isNullOrBlank()) {
              AsyncImage(
                model = data.topArtist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              Icon(
                painter = painterResource(R.drawable.person),
                contentDescription = null,
                tint = theme.headline,
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "TOP ARTIST",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              ),
              color = theme.label
            )
            Text(
              data.topArtist?.title ?: "Various",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 13.5.sp
              ),
              color = theme.headline,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              "#1 In Rotation",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
              color = theme.bodyText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)

      // 3. Two-Column Countdown: MY TOP SONGS (Left) & MY TOP ARTISTS (Right)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Left Column: Top 5 Songs
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            "MY TOP SONGS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.2.sp
            ),
            color = theme.label
          )
          data.top5Songs.take(5).forEachIndexed { index, song ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "0${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black
                ),
                color = if (index == 0) theme.label else Color.White.copy(alpha = 0.6f)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  song.title,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                  ),
                  color = theme.headline,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  song.artistsText ?: "",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                  color = theme.bodyText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        // Right Column: Top 5 Artists
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            "MY TOP ARTISTS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.2.sp
            ),
            color = theme.label
          )
          data.top5Artists.take(5).forEachIndexed { index, artist ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "0${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black
                ),
                color = if (index == 0) theme.label else Color.White.copy(alpha = 0.6f)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  artist.title,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                  ),
                  color = theme.headline,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  if (index == 0) "Top Rotation Artist" else "Featured Creator",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                  color = theme.bodyText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)

      // 4. Typographic Stats Dual Section (Minutes & Sonic Persona)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            "TOTAL MINUTES",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.2.sp
            ),
            color = theme.label
          )
          Text(
            "${data.totalMinutes}",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Black,
              fontSize = 32.sp,
              letterSpacing = (-1).sp
            ),
            color = theme.headline
          )
          Text(
            "${(data.totalMinutes / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }} hours of soundtrack",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = theme.bodyText
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            "SONIC PERSONA",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.2.sp
            ),
            color = theme.label
          )
          Text(
            data.dominantVibe.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Black,
              fontSize = 16.sp
            ),
            color = theme.headline
          )
          Text(
            "${data.uniqueSongsCount} tracks • ${data.uniqueArtistsCount} artists",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = theme.bodyText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Bottom Controls & Colorway Picker (Just like Spotify Wrapped)
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Colorway Swatches
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "POSTER THEME",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Black,
            fontSize = 7.5.sp,
            letterSpacing = 1.sp
          ),
          color = Color.White.copy(alpha = 0.5f),
          modifier = Modifier.padding(end = 6.dp)
        )
        WrappedPosterTheme.entries.forEach { pTheme ->
          val isSelected = pTheme == selectedPosterTheme
          Box(
            modifier = Modifier
              .padding(horizontal = 3.dp)
              .size(22.dp)
              .clip(CircleShape)
              .background(pTheme.composeBg)
              .then(
                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
              )
              .clickable { onSelectPosterTheme(pTheme) },
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(pTheme.composeAccent)
            )
          }
        }
      }

      // Share Your Story Button
      Button(
        onClick = onShare,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = theme.label,
          contentColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth().height(44.dp)
      ) {
        Icon(
          painter = painterResource(R.drawable.share),
          contentDescription = null,
          modifier = Modifier.size(15.dp),
          tint = Color.Black
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          "Share Your Story",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
          color = Color.Black
        )
      }

      // Save Wrapped Playlist Button
      OutlinedButton(
        onClick = onSavePlaylist,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(42.dp)
      ) {
        Icon(
          painter = painterResource(if (isSaved) R.drawable.check else R.drawable.playlist_add),
          contentDescription = null,
          modifier = Modifier.size(15.dp),
          tint = if (isSaved) theme.label else Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          if (isSaving) "Saving Playlist…"
          else if (isSaved) "Saved to Library"
          else "Save Wrapped Playlist",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
      }

      // Replay Button
      TextButton(
        onClick = onReplay,
        modifier = Modifier.fillMaxWidth().height(28.dp),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          painter = painterResource(R.drawable.replay),
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.7f),
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          "Replay Experience",
          color = Color.White.copy(alpha = 0.7f),
          style = MaterialTheme.typography.labelMedium
        )
      }
    }
  }
}

// -------------------------------------------------------------
// REUSABLE POSTER COMPONENTS
// -------------------------------------------------------------
@Composable
private fun SpotifyStatPill(text: String, theme: SpotifySlideTheme) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(theme.headline.copy(alpha = 0.12f))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = theme.headline
    )
  }
}

@Composable
private fun SpotifyTraitTag(text: String, theme: SpotifySlideTheme) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(theme.headline.copy(alpha = 0.15f))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
      color = theme.headline
    )
  }
}

@Composable
private fun SpotifyRadarMeter(name: String, ratio: Float, theme: SpotifySlideTheme) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = theme.headline
      )
      Text(
        text = "${(ratio * 100).toInt()}%",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
        color = theme.headline
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(theme.headline.copy(alpha = 0.15f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(ratio)
          .clip(RoundedCornerShape(4.dp))
          .background(theme.headline)
      )
    }
  }
}

@Composable
private fun SpotifyPosterStat(value: String, label: String, accentColor: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
      color = Color.White
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
      color = accentColor
    )
  }
}

@Composable
private fun AnimatedEqualizerBars(
  color: Color,
  height: Dp = 16.dp,
  isPlaying: Boolean = true
) {
  val infiniteTransition = rememberInfiniteTransition(label = "EQBars")
  val bar1 by infiniteTransition.animateFloat(0.3f, 1.0f, infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "b1")
  val bar2 by infiniteTransition.animateFloat(0.6f, 0.2f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "b2")
  val bar3 by infiniteTransition.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "b3")
  val bar4 by infiniteTransition.animateFloat(0.8f, 0.4f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "b4")

  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = Modifier.height(height)
  ) {
    listOf(bar1, bar2, bar3, bar4).forEach { scale ->
      val actualScale = if (isPlaying) scale else 0.25f
      Box(
        modifier =
          Modifier.width(3.dp)
            .fillMaxHeight(actualScale)
            .clip(RoundedCornerShape(1.5.dp))
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
        .background(Color(0xFF0A0A0E))
        .systemBarsPadding()
        .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Image(
        painter = painterResource(R.drawable.ic_launcher_nobg),
        contentDescription = "Echo Music",
        colorFilter = ColorFilter.tint(Color(0xFF00F5D4)),
        modifier = Modifier.size(64.dp)
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "MORE MUSIC NEEDED",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 14.sp
        ),
        color = Color(0xFF00F5D4)
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Keep Listening",
        style = MaterialTheme.typography.headlineLarge.copy(
          fontWeight = FontWeight.Black,
          fontSize = 32.sp
        ),
        color = Color.White
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "You've played ${state.songsPlayed} songs so far. Listen to at least ${state.minSongsRequired} tracks to unlock your personalized Echo Wrapped story.",
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = Color.White.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = onTryAllTime,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF00F5D4),
          contentColor = Color(0xFF0A0A0E)
        ),
        modifier = Modifier.fillMaxWidth().height(48.dp)
      ) {
        Text("View All-Time Stats", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
      }

      Spacer(modifier = Modifier.height(12.dp))

      TextButton(onClick = onBack) {
        Text("Go Back", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}
