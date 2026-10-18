package echo.music.iad1tya.ui.immersive

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import echo.music.iad1tya.constants.ImmersiveBatterySaverKey
import echo.music.iad1tya.constants.ImmersiveReducedMotionKey
import echo.music.iad1tya.playback.audio.LiveAudioLevels
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Per-frame audio-reactive values. Everything is read inside draw lambdas, via [frame] to invalidate. */
@Stable
class Reactive {
  var level = 0f
  var bass = 0f
  var mid = 0f
  var treble = 0f
  /** 1 on a detected beat, decays to 0. Always 0 when no real audio analysis is available. */
  var pulse = 0f
  /** Seconds, slowed down by reduced-motion. */
  var time = 0f
  /** True when the numbers come from the real audio being played; false = decorative motion only. */
  var live = false
  var frame by mutableLongStateOf(0L)
}

@Stable
class MotionSettings(val reduced: Boolean, val battery: Boolean) {
  /** 1 = full speed. */
  val speed: Float get() = if (reduced) 0.25f else 1f
  val frameGapNanos: Long get() = if (battery || reduced) 33_000_000L else 0L
}

@Composable
fun rememberMotionSettings(): MotionSettings {
  val context = LocalContext.current
  val (reduced, _) = rememberPreference(ImmersiveReducedMotionKey, defaultValue = false)
  val (battery, _) = rememberPreference(ImmersiveBatterySaverKey, defaultValue = false)
  val systemOff =
    remember {
      runCatching {
          Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
        .getOrDefault(false)
    }
  return remember(reduced, battery, systemOff) { MotionSettings(reduced || systemOff, battery) }
}

/**
 * Drives a [Reactive]. Stops entirely while the app is in the background or [active] is false,
 * so hidden visualisations cost nothing.
 */
@Composable
fun rememberReactive(playing: Boolean, motion: MotionSettings, active: Boolean = true): Reactive {
  val r = remember { Reactive() }
  val owner = LocalLifecycleOwner.current
  val playingNow by rememberUpdatedState(playing)
  LaunchedEffect(active, motion, owner) {
    if (!active) return@LaunchedEffect
    owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      var last = 0L
      var lastDraw = 0L
      var lastBeat = LiveAudioLevels.beatCount
      while (true) {
        withFrameNanos { now ->
          if (last == 0L) last = now
          if (motion.frameGapNanos > 0 && now - lastDraw < motion.frameGapNanos) return@withFrameNanos
          lastDraw = now
          val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
          last = now
          r.time += dt * motion.speed
          val live = playingNow && LiveAudioLevels.isLive()
          r.live = live
          val k = 1f - 0.0001f.pow(dt) // smooth toward target, frame-rate independent
          if (live) {
            r.level += (LiveAudioLevels.level - r.level) * k
            r.bass += (LiveAudioLevels.bass - r.bass) * k
            r.mid += (LiveAudioLevels.mid - r.mid) * k
            r.treble += (LiveAudioLevels.treble - r.treble) * k
            val bc = LiveAudioLevels.beatCount
            if (bc != lastBeat) {
              lastBeat = bc
              if (!motion.reduced) r.pulse = 1f
            }
          } else {
            // Decorative breathing only. No beat pulse is ever invented here.
            val a = if (playingNow) 0.26f else 0.06f
            val t = r.time
            r.level += (a + 0.06f * sin(t * 1.3f) - r.level) * k
            r.bass += (a + 0.10f * sin(t * 0.9f) - r.bass) * k
            r.mid += (a * 0.8f + 0.07f * sin(t * 1.7f + 1f) - r.mid) * k
            r.treble += (a * 0.6f + 0.05f * sin(t * 2.3f + 2f) - r.treble) * k
            lastBeat = LiveAudioLevels.beatCount
          }
          r.pulse = max(0f, r.pulse - dt * 3.2f)
          r.frame = now
        }
      }
    }
  }
  return r
}

class AlbumPalette(val a: Color, val b: Color, val c: Color)

private val DefaultPalette = AlbumPalette(Color(0xFF7A5CFF), Color(0xFFFF5C9E), Color(0xFF3CC8FF))

/** Dominant colours of the current artwork, cross-faded when the track changes. */
@Composable
fun rememberAlbumPalette(thumbnailUrl: String?): AlbumPalette {
  val context = LocalContext.current
  var target by remember { mutableStateOf(DefaultPalette) }
  LaunchedEffect(thumbnailUrl) {
    if (thumbnailUrl == null) {
      target = DefaultPalette
      return@LaunchedEffect
    }
    val result =
      withContext(Dispatchers.IO) {
        runCatching {
            val request =
              ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(96, 96)
                .allowHardware(false)
                .build()
            val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@runCatching null
            val p = Palette.from(bitmap).maximumColorCount(10).generate()
            val fb = DefaultPalette.a.toArgb()
            AlbumPalette(
              Color(p.getVibrantColor(p.getDominantColor(fb))),
              Color(p.getLightVibrantColor(p.getMutedColor(DefaultPalette.b.toArgb()))),
              Color(p.getDarkVibrantColor(p.getDarkMutedColor(DefaultPalette.c.toArgb()))),
            )
          }
          .getOrNull()
      }
    if (result != null) target = result
  }
  val a by animateColorAsState(target.a, tween(900), label = "palA")
  val b by animateColorAsState(target.b, tween(900), label = "palB")
  val c by animateColorAsState(target.c, tween(900), label = "palC")
  return AlbumPalette(a, b, c)
}
