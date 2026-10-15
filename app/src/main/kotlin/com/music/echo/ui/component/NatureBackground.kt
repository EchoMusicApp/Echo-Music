package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

val NatureDeep = Color(0xFF04110A)
val NatureForest = Color(0xFF0B2416)
val NatureMoss = Color(0xFF123524)
val NatureLeaf = Color(0xFF3FAE6A)

private class Leaf(val x: Float, val y: Float, val size: Float, val angle: Float, val tone: Int, val phase: Float)
private class RainDrop(val x: Float, val speed: Float, val length: Float, val offset: Float, val alpha: Float)
private class Slider(val x: Float, val speed: Float, val radius: Float, val offset: Float)

/**
 * Dark green forest backdrop: leaf silhouettes, falling rain and droplets sliding down the glass.
 * [light] keeps it cheap enough to sit behind scrolling lists (fewer rain streaks, 30 fps).
 */
@Composable
fun NatureBackground(modifier: Modifier = Modifier, light: Boolean = false, rain: Boolean = true) {
  val leaves = remember {
    val r = Random(7)
    List(if (light) 9 else 14) {
      Leaf(r.nextFloat(), r.nextFloat(), 0.16f + r.nextFloat() * 0.2f, r.nextFloat() * 360f, r.nextInt(3), r.nextFloat() * 6.28f)
    }
  }
  val rainDrops = remember {
    val r = Random(11)
    List(if (light) 28 else 90) {
      RainDrop(r.nextFloat(), 0.7f + r.nextFloat() * 0.9f, 0.025f + r.nextFloat() * 0.05f, r.nextFloat(), 0.12f + r.nextFloat() * 0.25f)
    }
  }
  val sliders = remember {
    val r = Random(21)
    List(if (light) 5 else 9) { Slider(0.08f + r.nextFloat() * 0.84f, 0.02f + r.nextFloat() * 0.035f, 2.5f + r.nextFloat() * 3f, r.nextFloat()) }
  }
  var timeMs by remember { mutableLongStateOf(0L) }
  LaunchedEffect(light) {
    var last = 0L
    while (true) {
      withFrameNanos { nanos ->
        val ms = nanos / 1_000_000L
        if (!light || ms - last >= 33) {
          last = ms
          timeMs = ms
        }
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val t = timeMs / 1000f
      drawRect(Brush.verticalGradient(listOf(NatureDeep, NatureForest, Color(0xFF06150D))))
      // soft green light from the top
      drawRect(
        Brush.radialGradient(
          listOf(NatureLeaf.copy(alpha = 0.18f), Color.Transparent),
          center = Offset(size.width * 0.3f, 0f),
          radius = size.maxDimension * 0.7f
        )
      )
      leaves.forEach { drawLeaf(it, t, light) }
      if (rain) {
        rainDrops.forEach { d ->
          val travel = size.height + d.length * size.height
          val y = ((t * d.speed * 0.9f + d.offset) % 1f) * travel - d.length * size.height
          val x = d.x * size.width + y * 0.12f
          drawLine(
            Color(0xFFBFF5D2).copy(alpha = d.alpha),
            Offset(x, y),
            Offset(x + d.length * size.height * 0.12f, y + d.length * size.height),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
          )
        }
        sliders.forEach { s ->
          val p = (t * s.speed + s.offset) % 1f
          val cx = s.x * size.width + sin(t * 0.6f + s.offset * 9f) * 6f
          val cy = p * size.height
          val rad = s.radius * density
          // wet trail
          drawLine(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color(0xFFBFF5D2).copy(alpha = 0.18f)),
              startY = cy - 90f * density,
              endY = cy
            ),
            Offset(cx, cy - 90f * density),
            Offset(cx, cy),
            strokeWidth = rad * 0.55f,
            cap = StrokeCap.Round
          )
          drawCircle(Color(0xFFBFF5D2).copy(alpha = 0.22f), rad, Offset(cx, cy))
          drawCircle(Color.White.copy(alpha = 0.55f), rad * 0.3f, Offset(cx - rad * 0.3f, cy - rad * 0.3f))
        }
      }
      // vignette so text stays readable
      drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.45f))))
    }
  }
}

private fun DrawScope.drawLeaf(leaf: Leaf, t: Float, light: Boolean) {
  val len = size.minDimension * leaf.size * 1.6f
  val sway = if (light) 0f else sin(t * 0.5f + leaf.phase) * 4f
  val tone =
    when (leaf.tone) {
      0 -> Color(0xFF1F6B3F)
      1 -> Color(0xFF2E8B57)
      else -> Color(0xFF14502E)
    }
  withTransform({
    translate(leaf.x * size.width, leaf.y * size.height)
    rotate(leaf.angle + sway, Offset.Zero)
  }) {
    val path = Path().apply {
      moveTo(0f, -len / 2)
      quadraticTo(len * 0.55f, 0f, 0f, len / 2)
      quadraticTo(-len * 0.55f, 0f, 0f, -len / 2)
      close()
    }
    drawPath(path, tone.copy(alpha = 0.22f))
    drawLine(Color(0xFF8FE3A8).copy(alpha = 0.12f), Offset(0f, -len / 2), Offset(0f, len / 2), strokeWidth = 1.5f)
    for (i in -2..2) {
      if (i == 0) continue
      val y = i * len * 0.16f
      drawLine(Color(0xFF8FE3A8).copy(alpha = 0.07f), Offset(0f, y), Offset(len * 0.28f * (if (i % 2 == 0) 1 else -1), y - len * 0.08f), strokeWidth = 1f)
    }
  }
}
