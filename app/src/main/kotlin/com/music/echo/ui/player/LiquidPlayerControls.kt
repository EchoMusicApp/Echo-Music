package echo.music.iad1tya.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.playback.audio.LiveAudioLevels
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Liquid seek bar: a glass tube filled with flowing, theme-coloured liquid and a glowing glass bead.
 * The bead stretches while dragged and the liquid surface ripples while music plays.
 */
@Composable
fun LiquidSeekBar(
  value: Float,
  max: Float,
  onChange: (Float) -> Unit,
  onFinish: () -> Unit,
  enabled: Boolean,
  playing: Boolean,
  modifier: Modifier = Modifier,
) {
  val accent = MaterialTheme.colorScheme.primary
  val accent2 = MaterialTheme.colorScheme.tertiary
  var dragging by remember { mutableStateOf(false) }
  val grow by animateFloatAsState(if (dragging) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 380f), label = "lsGrow")
  val inf = rememberInfiniteTransition(label = "ls")
  val flow by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "lsFlow")
  val live = remember { mutableFloatStateOf(0f) }
  LaunchedEffect(playing) {
    while (playing) {
      live.floatValue += ((if (LiveAudioLevels.isLive()) LiveAudioLevels.level else 0.25f) - live.floatValue) * 0.2f
      kotlinx.coroutines.delay(33)
    }
    live.floatValue = 0f
  }
  val safeMax = if (max <= 0f) 1f else max
  val widthHolder = remember { FloatArray(1) { 1f } }

  fun toValue(x: Float) = (x / widthHolder[0]).coerceIn(0f, 1f) * safeMax

  Box(
    modifier
      .fillMaxWidth()
      .height(40.dp)
      .pointerInput(enabled, safeMax) {
        if (!enabled) return@pointerInput
        detectTapGestures(onTap = { onChange(toValue(it.x)); onFinish() })
      }
      .pointerInput(enabled, safeMax) {
        if (!enabled) return@pointerInput
        detectDragGestures(
          onDragStart = { dragging = true; onChange(toValue(it.x)) },
          onDragEnd = { dragging = false; onFinish() },
          onDragCancel = { dragging = false; onFinish() },
        ) { change, _ -> change.consume(); onChange(toValue(change.position.x)) }
      }
      .drawBehind {
        widthHolder[0] = size.width.coerceAtLeast(1f)
        val cy = size.height / 2f
        val tube = 12.dp.toPx() + 4.dp.toPx() * grow
        val r = tube / 2f
        val frac = (value / safeMax).coerceIn(0f, 1f)
        val fillX = size.width * frac
        // glass tube
        drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(0f, cy - r), Size(size.width, tube), CornerRadius(r))
        drawRoundRect(
          Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), accent.copy(alpha = 0.5f))),
          Offset(0f, cy - r), Size(size.width, tube), CornerRadius(r), style = Stroke(1.2.dp.toPx()),
        )
        // liquid
        if (fillX > 0f) {
          drawRoundRect(
            Brush.horizontalGradient(listOf(accent, accent2), endX = size.width.coerceAtLeast(1f)),
            Offset(0f, cy - r + 1.5.dp.toPx()), Size(fillX, tube - 3.dp.toPx()), CornerRadius(r),
          )
          // flowing shine travelling through the liquid
          val sx = (flow * (fillX + 60.dp.toPx())) - 30.dp.toPx()
          drawRoundRect(
            Brush.horizontalGradient(
              listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
              startX = sx - 30.dp.toPx(), endX = sx + 30.dp.toPx(),
            ),
            Offset(0f, cy - r + 1.5.dp.toPx()), Size(fillX, tube - 3.dp.toPx()), CornerRadius(r),
          )
          // music ripples on the surface
          if (playing) {
            val amp = (1.5f + 4f * live.floatValue).dp.toPx()
            var x = 0f
            while (x < fillX) {
              val y = cy - r - amp * 0.2f + sin((x / 14.dp.toPx() + flow * 2f * PI.toFloat())) * amp * 0.35f
              drawCircle(Color.White.copy(alpha = 0.35f), 1.1.dp.toPx(), Offset(x, y))
              x += 9.dp.toPx()
            }
          }
        }
        // glass bead
        val br = 11.dp.toPx() + 4.dp.toPx() * grow
        val bx = fillX.coerceIn(br, size.width - br)
        drawCircle(accent.copy(alpha = 0.30f + 0.25f * live.floatValue + 0.2f * grow), br * 2.1f, Offset(bx, cy))
        drawCircle(Color.White.copy(alpha = 0.92f), br, Offset(bx, cy))
        drawCircle(
          Brush.radialGradient(listOf(Color.White, accent.copy(alpha = 0.55f)), center = Offset(bx - br * 0.3f, cy - br * 0.3f), radius = br * 1.4f),
          br, Offset(bx, cy),
        )
        drawCircle(Color.White.copy(alpha = 0.9f), br * 0.22f, Offset(bx - br * 0.35f, cy - br * 0.38f))
      }
  )
}

/**
 * Halo for the play button: breathing glass rings that swell with the bass and little lights
 * that orbit while music plays. Static and calm when paused.
 */
@Composable
fun Modifier.liquidPlayHalo(playing: Boolean): Modifier {
  val accent = MaterialTheme.colorScheme.primary
  val accent2 = MaterialTheme.colorScheme.tertiary
  val inf = rememberInfiniteTransition(label = "halo")
  val spin by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "haloSpin")
  val energy = remember { Animatable(0f) }
  LaunchedEffect(playing) {
    while (playing) {
      val target = if (LiveAudioLevels.isLive()) maxOf(LiveAudioLevels.bass, LiveAudioLevels.level) else 0.35f
      energy.animateTo(target, tween(90))
      kotlinx.coroutines.delay(16)
    }
    energy.animateTo(0f, tween(400))
  }
  return this.drawBehind {
    val c = Offset(size.width / 2f, size.height / 2f)
    val base = size.minDimension / 2f
    val e = energy.value
    for (i in 0..2) {
      val rr = base * (1.12f + 0.12f * i + 0.35f * e * (1f - i * 0.25f))
      drawCircle(accent.copy(alpha = (0.22f - 0.06f * i) * (if (playing) 1f else 0.5f)), rr, c, style = Stroke(1.6.dp.toPx()))
    }
    drawCircle(
      Brush.radialGradient(listOf(accent.copy(alpha = 0.35f + 0.3f * e), Color.Transparent), center = c, radius = base * (1.9f + 0.4f * e)),
      base * (1.9f + 0.4f * e), c,
    )
    if (playing) {
      for (i in 0..3) {
        val a = (spin + i / 4f) * 2f * PI.toFloat()
        val rr = base * 1.32f
        drawCircle(if (i % 2 == 0) accent2 else Color.White, 2.6.dp.toPx() + 1.5.dp.toPx() * e, Offset(c.x + cos(a) * rr, c.y + sin(a) * rr))
      }
    }
  }
}
