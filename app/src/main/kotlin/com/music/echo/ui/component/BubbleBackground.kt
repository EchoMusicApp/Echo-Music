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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

private class Bubble(
  val x: Float, val size: Float, val speed: Float, val offset: Float,
  val wobble: Float, val wobbleSpeed: Float, val phase: Float, val tint: Color
)

/**
 * AMOLED backdrop: true black with glossy soap bubbles drifting upward. Each bubble has a clear
 * body, an iridescent rim, a bright highlight and a faint reflected crescent so it reads as 3D.
 */
@Composable
fun BubbleBackground(modifier: Modifier = Modifier) {
  val bubbles = remember {
    val r = Random(42)
    val tints = listOf(Color(0xFF7DE3FF), Color(0xFFD08BFF), Color(0xFFFFA6E0), Color(0xFF9CFFD6))
    List(16) {
      Bubble(
        r.nextFloat(), 14f + r.nextFloat() * 46f, 0.018f + r.nextFloat() * 0.035f, r.nextFloat(),
        6f + r.nextFloat() * 14f, 0.4f + r.nextFloat() * 0.8f, r.nextFloat() * 6.28f, tints[r.nextInt(tints.size)]
      )
    }
  }
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
  Canvas(modifier.fillMaxSize()) {
    drawRect(Color.Black)
    val t = timeMs / 1000f
    bubbles.forEach { b ->
      val rad = b.size.dp.toPx()
      val travel = size.height + rad * 2
      val y = size.height + rad - ((t * b.speed + b.offset) % 1f) * travel
      val x = b.x * size.width + sin(t * b.wobbleSpeed + b.phase) * b.wobble.dp.toPx()
      val c = Offset(x, y)
      // body: clear in the middle, tinted toward the edge
      drawCircle(
        Brush.radialGradient(
          0.0f to Color.Transparent,
          0.7f to b.tint.copy(alpha = 0.06f),
          1.0f to b.tint.copy(alpha = 0.28f),
          center = c, radius = rad
        ),
        radius = rad, center = c
      )
      // iridescent rim
      drawCircle(
        Brush.sweepGradient(
          listOf(b.tint.copy(alpha = 0.7f), Color(0xFFFFE08A).copy(alpha = 0.35f), Color(0xFFFF7BD5).copy(alpha = 0.55f), b.tint.copy(alpha = 0.7f)),
          center = c
        ),
        radius = rad, center = c, style = Stroke(width = 1.4.dp.toPx())
      )
      // main highlight (top-left) and small glint
      drawCircle(Color.White.copy(alpha = 0.75f), rad * 0.16f, Offset(c.x - rad * 0.42f, c.y - rad * 0.46f))
      drawCircle(Color.White.copy(alpha = 0.5f), rad * 0.07f, Offset(c.x - rad * 0.22f, c.y - rad * 0.62f))
      // reflected crescent (bottom-right)
      drawCircle(
        Brush.radialGradient(listOf(b.tint.copy(alpha = 0.22f), Color.Transparent), center = Offset(c.x + rad * 0.45f, c.y + rad * 0.5f), radius = rad * 0.5f),
        radius = rad * 0.5f, center = Offset(c.x + rad * 0.45f, c.y + rad * 0.5f)
      )
    }
  }
}
