package echo.music.iad1tya.ui.immersive

import echo.music.iad1tya.ui.component.glassSurface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.CinemaDefaultModeKey
import echo.music.iad1tya.constants.CinemaParallaxKey
import echo.music.iad1tya.ui.player.InlineLyricsView
import echo.music.iad1tya.utils.rememberPreference
import androidx.datastore.preferences.core.edit
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Global open/close state for Cinema Mode. The overlay is hosted at the app root, above the player. */
object CinemaState {
  var visible by mutableStateOf(false)
  fun open() { visible = true }
  fun close() { visible = false }
}

/** Entry point shown under the player controls. Does not touch any existing control. */
@Composable
fun CinemaEntryPill(modifier: Modifier = Modifier, color: Color) {
  Row(
    modifier =
      modifier
        .glassSurface(RoundedCornerShape(50), color)
        .clickable(onClickLabel = "Open Cinema mode") { CinemaState.open() }
        .padding(horizontal = 18.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(painterResource(R.drawable.image), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    Text("Cinema mode", color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}

private val modeNames = listOf("Artwork", "Lyrics", "Visualizer", "Minimal")

private fun fmt(ms: Long): String {
  val s = (ms / 1000).coerceAtLeast(0)
  return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun CinemaOverlay(navController: NavController) {
  AnimatedVisibility(
    visible = CinemaState.visible,
    enter = fadeIn(tween(380)) + scaleIn(initialScale = 0.94f, animationSpec = tween(420, easing = FastOutSlowInEasing)),
    exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.96f, animationSpec = tween(260)),
  ) {
    CinemaContent(navController)
  }
}

@Composable
private fun CinemaContent(navController: NavController) {
  val playerConnection = LocalPlayerConnection.current
  if (playerConnection == null) {
    CinemaState.close()
    return
  }
  BackHandler { CinemaState.close() }

  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
  val isPlaying by playerConnection.isPlaying.collectAsState()
  val currentSong by playerConnection.currentSong.collectAsState(initial = null)
  val liked = currentSong?.song?.liked == true
  val player = playerConnection.player

  val (defaultMode, _) = rememberPreference(CinemaDefaultModeKey, 0)
  val (parallaxOn, _) = rememberPreference(CinemaParallaxKey, true)
  var mode by remember { mutableIntStateOf(defaultMode.coerceIn(0, 3)) }
  var cityVisual by remember { mutableStateOf(false) }

  val motion = rememberMotionSettings()
  val reactive = rememberReactive(playing = isPlaying, motion = motion, active = true)
  val palette = rememberAlbumPalette(mediaMetadata?.thumbnailUrl)

  // playback position, polled only while Cinema mode is on screen
  var position by remember { mutableLongStateOf(player.currentPosition) }
  var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
  var scrubbing by remember { mutableStateOf(false) }
  var scrubValue by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(player) {
    while (true) {
      if (!scrubbing) {
        position = player.currentPosition
        duration = player.duration.coerceAtLeast(0L)
      }
      delay(if (isPlaying) 250 else 600)
    }
  }

  // parallax: layers slide by different amounts as the finger moves; idle drift when no touch
  var touchX by remember { mutableFloatStateOf(0f) }
  var touchY by remember { mutableFloatStateOf(0f) }
  val px by animateFloatAsState(touchX, tween(500, easing = FastOutSlowInEasing), label = "px")
  val py by animateFloatAsState(touchY, tween(500, easing = FastOutSlowInEasing), label = "py")
  val drift by
    rememberInfiniteTransition(label = "drift").animateFloat(
      initialValue = -1f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
      label = "driftV",
    )
  val useParallax = parallaxOn && !motion.reduced
  val parallaxModifier =
    if (useParallax) {
      Modifier.pointerInput(Unit) {
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
          touchX = (down.position.x / size.width - 0.5f) * 2f
          touchY = (down.position.y / size.height - 0.5f) * 2f
          do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val c = event.changes.firstOrNull() ?: break
            touchX = (c.position.x / size.width - 0.5f) * 2f
            touchY = (c.position.y / size.height - 0.5f) * 2f
          } while (event.changes.any { it.pressed })
          touchX = 0f
          touchY = 0f
        }
      }
    } else Modifier

  val thumb = mediaMetadata?.thumbnailUrl
  val white = Color.White

  Box(
    Modifier.fillMaxSize()
      .background(Color.Black)
      .then(parallaxModifier)
      // swallow touches so nothing underneath (player, Buddy) reacts
      .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } }
  ) {
    // ---- cinematic background: blurred + darkened artwork, cross-faded between tracks ----
    Crossfade(targetState = thumb, animationSpec = tween(900), label = "cinemaBg") { url ->
      Box(
        Modifier.fillMaxSize().graphicsLayer {
          val k = if (useParallax) 1f else 0f
          translationX = (px * 14f + drift * 8f * k) * density
          translationY = (py * 10f) * density
          scaleX = 1.25f
          scaleY = 1.25f
        }
      ) {
        if (url != null) {
          AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(70.dp),
          )
        } else {
          Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.a, palette.c))))
        }
      }
    }
    Box(
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.62f), Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.80f))
          )
        )
    )

    Column(
      Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // ---- top bar: obvious close + display modes ----
      Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(white.copy(alpha = 0.14f))
            .border(1.dp, white.copy(alpha = 0.25f), CircleShape)
            .clickable(onClickLabel = "Close Cinema mode") { CinemaState.close() },
          contentAlignment = Alignment.Center,
        ) {
          Icon(painterResource(R.drawable.close), contentDescription = "Close Cinema mode", tint = white, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Row(
          Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          modeNames.forEachIndexed { i, name ->
            val sel = mode == i
            Box(
              Modifier.clip(RoundedCornerShape(50))
                .background(if (sel) white.copy(alpha = 0.92f) else white.copy(alpha = 0.12f))
                .clickable { mode = i }
                .padding(horizontal = 14.dp, vertical = 9.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(name, color = if (sel) Color.Black else white, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
          }
        }
      }

      // ---- stage ----
      Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
          targetState = mode,
          transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
          label = "cinemaMode",
        ) { m ->
          when (m) {
            1 -> {
              Box(Modifier.fillMaxSize()) {
                InlineLyricsView(
                  mediaMetadata = mediaMetadata,
                  showLyrics = true,
                  positionProvider = { position },
                )
              }
            }
            2 -> {
              Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))) {
                if (cityVisual) {
                  BeatCityView(
                    config = rememberCityConfig(forceCompact = false),
                    reactive = reactive,
                    palette = palette,
                    thumbnailUrl = thumb,
                    motion = motion,
                    interactive = true,
                    onDestination = { idx ->
                      CinemaState.close()
                      navigateCityDestination(navController, idx)
                    },
                  )
                } else {
                  LiquidSoundBackground(
                    config = rememberLiquidConfig(forceCompact = false),
                    reactive = reactive,
                    palette = palette,
                    thumbnailUrl = thumb,
                  )
                }
                Row(
                  Modifier.align(Alignment.TopCenter).padding(10.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf("Liquid", "City").forEachIndexed { i, label ->
                    val sel = (i == 1) == cityVisual
                    Box(
                      Modifier.clip(RoundedCornerShape(50))
                        .background(if (sel) white.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f))
                        .clickable { cityVisual = i == 1 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                      Text(label, color = if (sel) Color.Black else white, fontSize = 12.sp)
                    }
                  }
                }
                Text(
                  text = if (reactive.live) "Live audio · reacts to the music" else "Ambient motion · not beat-synced",
                  color = white.copy(alpha = 0.75f),
                  fontSize = 11.sp,
                  modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                )
              }
            }
            3 -> {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                  mediaMetadata?.title ?: "",
                  color = white,
                  fontSize = 30.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                  mediaMetadata?.artists?.joinToString { it.name } ?: "",
                  color = white.copy(alpha = 0.7f),
                  fontSize = 16.sp,
                  textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Text(fmt(position) + "  /  " + fmt(duration), color = white.copy(alpha = 0.6f), fontSize = 13.sp)
              }
            }
            else -> {
              // artwork centrepiece; changes animate with scale + fade, and breathe with the music
              AnimatedContent(
                targetState = mediaMetadata?.id,
                transitionSpec = {
                  (fadeIn(tween(520)) + scaleIn(initialScale = 0.86f, animationSpec = tween(520, easing = FastOutSlowInEasing))) togetherWith
                    (fadeOut(tween(300)) + scaleOut(targetScale = 1.06f, animationSpec = tween(300)))
                },
                label = "cinemaArt",
              ) { _ ->
                AsyncImage(
                  model = thumb,
                  contentDescription = mediaMetadata?.title,
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier.fillMaxWidth(0.92f)
                      .aspectRatio(1f)
                      .graphicsLayer {
                        @Suppress("UNUSED_VARIABLE") val t = reactive.frame
                        val s = 1f + (if (reactive.live) reactive.bass * 0.035f else 0f)
                        scaleX = s
                        scaleY = s
                        translationX = (if (useParallax) -px * 6f else 0f) * density
                        translationY = (if (useParallax) -py * 5f else 0f) * density
                        alpha = 0.92f + 0.08f * reactive.level.coerceIn(0f, 1f)
                      }
                      .shadow(30.dp, RoundedCornerShape(26.dp), ambientColor = palette.a, spotColor = palette.a)
                      .clip(RoundedCornerShape(26.dp)),
                )
              }
            }
          }
        }
      }

      // ---- title / artist / favourite ----
      if (mode != 3) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(mediaMetadata?.title ?: "", color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              mediaMetadata?.artists?.joinToString { it.name } ?: "",
              color = white.copy(alpha = 0.7f),
              fontSize = 14.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Box(
            Modifier.size(46.dp).clip(CircleShape).background(white.copy(alpha = 0.12f)).clickable(onClickLabel = "Favorite") { playerConnection.toggleLike() },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
              contentDescription = if (liked) "Remove from favorites" else "Add to favorites",
              tint = if (liked) Color(0xFFFF5C7A) else white,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }

      // ---- progress ----
      Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        val frac = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        Slider(
          value = if (scrubbing) scrubValue else frac,
          onValueChange = {
            scrubbing = true
            scrubValue = it
          },
          onValueChangeFinished = {
            if (duration > 0) player.seekTo((scrubValue * duration).toLong())
            position = (scrubValue * duration).toLong()
            scrubbing = false
          },
          colors =
            SliderDefaults.colors(
              thumbColor = white,
              activeTrackColor = white,
              inactiveTrackColor = white.copy(alpha = 0.25f),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(fmt(if (scrubbing) (scrubValue * duration).toLong() else position), color = white.copy(alpha = 0.7f), fontSize = 12.sp)
          Text(fmt(duration), color = white.copy(alpha = 0.7f), fontSize = 12.sp)
        }
      }

      // ---- controls ----
      Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CinemaButton(R.drawable.skip_previous, "Previous", 54.dp, filled = false) { playerConnection.seekToPrevious() }
        CinemaButton(if (isPlaying) R.drawable.pause else R.drawable.play, if (isPlaying) "Pause" else "Play", 72.dp, filled = true) {
          if (isPlaying) player.pause() else player.play()
        }
        CinemaButton(R.drawable.skip_next, "Next", 54.dp, filled = false) { playerConnection.seekToNext() }
      }
    }
  }
}

@Composable
private fun CinemaButton(icon: Int, label: String, size: androidx.compose.ui.unit.Dp, filled: Boolean, onClick: () -> Unit) {
  Box(
    Modifier.size(size)
      .clip(CircleShape)
      .background(if (filled) Color.White else Color.White.copy(alpha = 0.12f))
      .clickable(onClickLabel = label, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painterResource(icon),
      contentDescription = label,
      tint = if (filled) Color.Black else Color.White,
      modifier = Modifier.size(size * 0.46f),
    )
  }
}

/** Shared by Beat City buildings and the Music Cube: opens existing destinations, keeping the back stack intact. */
fun navigateCityDestination(navController: NavController, index: Int) {
  fun library(filter: echo.music.iad1tya.constants.LibraryFilter) {
    val ctx = navController.context
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      ctx.dataStore.edit { it[echo.music.iad1tya.constants.ChipSortTypeKey] = filter.name }
    }
    navController.navigate(echo.music.iad1tya.ui.screens.Screens.Library.route) {
      popUpTo(navController.graph.startDestinationId) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }
  when (index) {
    0 -> navController.navigate("auto_playlist/liked")
    1 -> navController.navigate("history")
    2 -> library(echo.music.iad1tya.constants.LibraryFilter.PLAYLISTS)
    3 -> library(echo.music.iad1tya.constants.LibraryFilter.ALBUMS)
    4 -> library(echo.music.iad1tya.constants.LibraryFilter.ARTISTS)
    else -> navController.navigate("mood_and_genres")
  }
}
