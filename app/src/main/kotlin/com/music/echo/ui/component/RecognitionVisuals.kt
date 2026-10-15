package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Idle "tap to recognize" orb: breathing glow, expanding sonar rings and a pulsing core. */
@Composable
fun PattukalRecognizeOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val inf = rememberInfiniteTransition(label = "orb")
  val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "ring")
  val breathe by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "breathe")
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(260.dp)) {
      val c = center
      val base = size.minDimension * 0.23f
      for (i in 0 until 3) {
        val p = (ring + i / 3f) % 1f
        drawCircle(
          primary.copy(alpha = (1f - p) * 0.45f),
          radius = base + p * (size.minDimension / 2 - base),
          center = c,
          style = Stroke(width = 2.dp.toPx())
        )
      }
      drawCircle(
        Brush.radialGradient(listOf(primary.copy(alpha = 0.45f + 0.2f * breathe), Color.Transparent), center = c, radius = base * 2.1f),
        radius = base * 2.1f,
        center = c
      )
    }
    Box(
      modifier =
        Modifier.size((112 + 6 * breathe).dp)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(primary, tertiary)))
          .clickable(interactionSource = remember { MutableInteractionSource() }, indication = echo.music.iad1tya.ui.utils.PattukalIndication) { onClick() },
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.material3.Icon(
        painter = androidx.compose.ui.res.painterResource(echo.music.iad1tya.R.drawable.music_note),
        contentDescription = null,
        modifier = Modifier.size(46.dp),
        tint = Color.White
      )
    }
  }
}

/** Listening visual: a circular equalizer of 56 bars rippling around a glowing core, plus sonar rings. */
@Composable
fun PattukalListeningVisualizer(modifier: Modifier = Modifier) {
  val inf = rememberInfiniteTransition(label = "listen")
  val t by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
  val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  Canvas(modifier = modifier.size(280.dp)) {
    val c = center
    val inner = size.minDimension * 0.24f
    val maxLen = size.minDimension * 0.2f
    val bars = 56
    for (i in 0 until bars) {
      val a = (i.toFloat() / bars) * (2 * PI).toFloat()
      val e =
        abs(sin(a * 3f + t * 2f)) * 0.55f + abs(sin(a * 5f - t * 3f)) * 0.3f + abs(cos(a * 2f + t)) * 0.15f
      val len = inner * 0.12f + maxLen * e
      val s = Offset(c.x + cos(a) * inner, c.y + sin(a) * inner)
      val en = Offset(c.x + cos(a) * (inner + len), c.y + sin(a) * (inner + len))
      drawLine(
        Brush.linearGradient(listOf(primary, tertiary), start = s, end = en),
        s,
        en,
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
    for (i in 0 until 2) {
      val p = (ring + i * 0.5f) % 1f
      drawCircle(
        primary.copy(alpha = (1f - p) * 0.4f),
        radius = inner * 0.9f + p * inner * 0.8f,
        center = c,
        style = Stroke(width = 1.5.dp.toPx())
      )
    }
    drawCircle(
      Brush.radialGradient(listOf(primary.copy(alpha = 0.55f), Color.Transparent), center = c, radius = inner),
      radius = inner,
      center = c
    )
    drawCircle(Color.White.copy(alpha = 0.9f), inner * (0.22f + 0.05f * sin(t * 4f)), c)
  }
}
