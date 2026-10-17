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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Crack(val pts: List<Offset>, val width: Float, val phase: Float, val speed: Float)
private class Blob(val x: Float, val y: Float, val r: Float, val sx: Float, val sy: Float, val ph: Float)
private class Smoke(val x: Float, val size: Float, val speed: Float, val off: Float, val drift: Float, val ph: Float)
private class Ember(val x: Float, val speed: Float, val off: Float, val size: Float, val ph: Float)

/**
 * AMOLED Lava Black backdrop: true black "cooled crust" with glowing molten cracks that pulse,
 * slow magma blobs shifting underneath, rising embers and soft smoke drifting upward.
 */
@Composable
fun LavaBackground(modifier: Modifier = Modifier) {
  val cracks = remember {
    val r = Random(7)
    List(13) {
      var x = r.nextFloat()
      var y = r.nextFloat()
      val pts = mutableListOf(Offset(x, y))
      var a = r.nextFloat() * 2f * PI.toFloat()
      repeat(7 + r.nextInt(5)) {
        a += (r.nextFloat() - 0.5f) * 1.5f
        val step = 0.045f + r.nextFloat() * 0.07f
        x += cos(a) * step * 0.6f
        y += sin(a) * step
        pts += Offset(x, y)
      }
      Crack(pts, 1.2f + r.nextFloat() * 2.2f, r.nextFloat() * 6.28f, 0.5f + r.nextFloat() * 1.1f)
    }
  }
  val blobs = remember {
    val r = Random(11)
    List(6) { Blob(r.nextFloat(), r.nextFloat(), 0.28f + r.nextFloat() * 0.22f, 0.03f + r.nextFloat() * 0.05f, 0.03f + r.nextFloat() * 0.05f, r.nextFloat() * 6.28f) }
  }
  val smoke = remember {
    val r = Random(21)
    List(9) { Smoke(r.nextFloat(), 70f + r.nextFloat() * 70f, 0.02f + r.nextFloat() * 0.03f, r.nextFloat(), 20f + r.nextFloat() * 40f, r.nextFloat() * 6.28f) }
  }
  val embers = remember {
    val r = Random(33)
    List(26) { Ember(r.nextFloat(), 0.04f + r.nextFloat() * 0.08f, r.nextFloat(), 1f + r.nextFloat() * 2.2f, r.nextFloat() * 6.28f) }
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
    val w = size.width
    val h = size.height

    // magma glowing under the crust (stronger toward the bottom)
    blobs.forEach { b ->
      val c = Offset((b.x + sin(t * b.sx * 6f + b.ph) * 0.12f) * w, (b.y + cos(t * b.sy * 6f + b.ph) * 0.1f) * h)
      val rad = b.r * w * 1.2f
      val heat = 0.10f + 0.18f * (c.y / h)
      drawCircle(
        Brush.radialGradient(
          0f to Color(0xFFFF7A18).copy(alpha = heat * 1.4f),
          0.45f to Color(0xFFD1280A).copy(alpha = heat),
          1f to Color.Transparent,
          center = c, radius = rad
        ),
        radius = rad, center = c
      )
    }

    // molten cracks
    cracks.forEach { k ->
      val pulse = 0.5f + 0.5f * sin(t * k.speed + k.phase)
      val path = Path()
      k.pts.forEachIndexed { i, p ->
        val o = Offset(p.x * w + sin(t * 0.4f + k.phase + i) * 2.dp.toPx(), p.y * h)
        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
      }
      val wd = k.width.dp.toPx()
      drawPath(path, Color(0xFFB81E00).copy(alpha = 0.10f + 0.12f * pulse), style = Stroke(wd * 7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      drawPath(path, Color(0xFFFF4A0A).copy(alpha = 0.30f + 0.35f * pulse), style = Stroke(wd * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      drawPath(path, Color(0xFFFFC04A).copy(alpha = 0.45f + 0.45f * pulse), style = Stroke(wd, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // embers
    embers.forEach { e ->
      val p = (t * e.speed + e.off) % 1f
      val x = e.x * w + sin(t * 1.3f + e.ph) * 14.dp.toPx()
      val y = h - p * h * 0.9f
      val a = (1f - p) * (0.5f + 0.5f * sin(t * 5f + e.ph))
      drawCircle(Color(0xFFFFB347).copy(alpha = a.coerceIn(0f, 1f)), radius = e.size.dp.toPx(), center = Offset(x, y))
    }

    // smoke rising and fading
    smoke.forEach { s ->
      val p = (t * s.speed + s.off) % 1f
      val rad = (s.size * (0.7f + p * 1.1f)).dp.toPx()
      val x = s.x * w + sin(t * 0.5f + s.ph) * s.drift.dp.toPx() + p * 30.dp.toPx()
      val y = h * 1.05f - p * h * 1.15f
      val a = (if (p < 0.2f) p / 0.2f else (1f - p) / 0.8f) * 0.16f
      drawCircle(
        Brush.radialGradient(
          0f to Color(0xFFB9ACA6).copy(alpha = a.coerceIn(0f, 1f)),
          1f to Color.Transparent,
          center = Offset(x, y), radius = rad
        ),
        radius = rad, center = Offset(x, y)
      )
    }
  }
}
