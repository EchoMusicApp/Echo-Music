package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin

private val RichBlack = Color(0xFF040309)

/**
 * Rich-black backdrop with a field of violet dots that roll like ocean waves. Dots are drawn in a
 * handful of batched `drawPoints` calls (one per brightness band) and the canvas redraws at ~30 fps,
 * so it stays light behind scrolling content.
 */
@Composable
fun DotWaveBackground(accent: Color, modifier: Modifier = Modifier) {
  var timeMs by remember { mutableLongStateOf(0L) }
  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { nanos ->
        val ms = nanos / 1_000_000L
        if (ms - last >= 33) {
          last = ms
          timeMs = ms
        }
      }
    }
  }
  val bands = 4
  Canvas(modifier = modifier.fillMaxSize()) {
    val t = timeMs / 1000f
    drawRect(Brush.verticalGradient(listOf(RichBlack, Color(0xFF07050F), RichBlack)))
    // top glow
    drawRect(
      Brush.radialGradient(
        listOf(accent.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(size.width * 0.5f, 0f),
        radius = size.maxDimension * 0.6f
      )
    )
    val gap = 22.dp.toPx()
    val cols = (size.width / gap).toInt() + 2
    val rows = (size.height / gap).toInt() + 2
    val buckets = Array(bands) { ArrayList<Offset>(cols * rows / bands + 8) }
    for (r in 0 until rows) {
      for (c in 0 until cols) {
        val x = c * gap
        val yBase = r * gap
        // two crossing travelling waves
        val w1 = sin(c * 0.33f + r * 0.18f - t * 1.4f)
        val w2 = sin(c * 0.16f - r * 0.27f + t * 0.9f)
        val w = (w1 + w2) * 0.25f + 0.5f // 0..1
        val y = yBase + (w1 * 5.dp.toPx())
        val fade = 1f - (yBase / size.height) * 0.55f
        val v = (w * fade).coerceIn(0f, 0.999f)
        buckets[(v * bands).toInt()].add(Offset(x, y))
      }
    }
    for (b in 0 until bands) {
      val k = (b + 1f) / bands
      drawPoints(
        buckets[b],
        PointMode.Points,
        accent.copy(alpha = 0.10f + 0.55f * k),
        strokeWidth = (1.6f + 2.6f * k).dp.toPx(),
        cap = StrokeCap.Round
      )
    }
    // bottom fade keeps the nav bar readable
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, RichBlack.copy(alpha = 0.7f)), startY = size.height * 0.6f, endY = size.height))
  }
}
