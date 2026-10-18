package echo.music.iad1tya.ui.immersive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * A tiny liquid-glass "pitch" strip for the mini player. Only composed while music plays.
 * Bars follow the real bass / mid / treble levels when live audio analysis is available,
 * otherwise a calm decorative ripple (no fake beat pulses).
 */
@Composable
fun MiniPlayerPitch(modifier: Modifier = Modifier) {
  val motion = rememberMotionSettings()
  val reactive = rememberReactive(playing = true, motion = motion, active = true)
  Canvas(modifier.width(112.dp).height(14.dp)) {
    @Suppress("UNUSED_VARIABLE") val tick = reactive.frame
    val bars = 22
    val gap = size.width / bars
    val bw = gap * 0.56f
    val t = reactive.time
    for (i in 0 until bars) {
      val f = i / (bars - 1f)
      // low bars follow bass, centre follows mids, right follows treble
      val band =
        when {
          f < 0.34f -> reactive.bass
          f < 0.67f -> reactive.mid
          else -> reactive.treble
        }
      val wob = 0.5f + 0.5f * sin(t * (2.2f + f * 3.1f) + i * 0.9f)
      val level = (0.16f + band * 0.95f * (0.55f + 0.45f * wob)).coerceIn(0.10f, 1f)
      val h = size.height * level
      val x = i * gap + (gap - bw) / 2f
      val y = size.height - h
      drawRoundRect(
        brush =
          Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.22f)),
            startY = y,
            endY = size.height,
          ),
        topLeft = Offset(x, y),
        size = Size(bw, h),
        cornerRadius = CornerRadius(bw / 2f, bw / 2f),
      )
    }
  }
}
