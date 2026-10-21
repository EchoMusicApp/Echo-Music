package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Seconds since the scene entered composition, advancing every frame. */
@Composable
private fun rememberSceneSeconds(running: Boolean = true): State<Float> {
  val seconds = remember { mutableFloatStateOf(0f) }
  LaunchedEffect(running) {
    if (!running) return@LaunchedEffect
    val start = withFrameNanos { it } - (seconds.floatValue * 1_000_000_000L).toLong()
    while (true) {
      withFrameNanos { now -> seconds.floatValue = (now - start) / 1_000_000_000f }
    }
  }
  return seconds
}

// ---------------------------------------------------------------------------------------------
// Waves: slim moving wave lines, used over the default Pattukal liquid glass player background.
// ---------------------------------------------------------------------------------------------

@Composable
fun MovingWaveLines(
  modifier: Modifier = Modifier,
  tint: Color = Color.White,
  running: Boolean = true,
) {
  val time = rememberSceneSeconds(running)
  Canvas(modifier = modifier) {
    val t = time.value
    val w = size.width
    val h = size.height
    val lineCount = 9
    for (i in 0 until lineCount) {
      val f = i / (lineCount - 1f)
      val baseY = h * (0.16f + 0.74f * f)
      val amp = h * (0.018f + 0.028f * sin(f * PI.toFloat()))
      val speed = 0.35f + 0.1f * (i % 3)
      val freq = 1.4f + 0.35f * (i % 4)
      val phase = t * speed * 2f * PI.toFloat() + i * 0.9f
      val path = Path()
      val steps = 48
      for (s in 0..steps) {
        val x = w * s / steps
        val nx = s / steps.toFloat()
        val y =
          baseY +
            amp * sin(nx * freq * 2f * PI.toFloat() + phase) +
            amp * 0.45f * sin(nx * (freq + 1.7f) * 2f * PI.toFloat() - phase * 0.7f)
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      val alpha = 0.10f + 0.22f * (1f - abs(f - 0.5f) * 1.6f).coerceIn(0f, 1f)
      drawPath(
        path = path,
        brush =
          Brush.horizontalGradient(
            listOf(
              Color.Transparent,
              tint.copy(alpha = alpha),
              Color.White.copy(alpha = alpha * 0.8f),
              tint.copy(alpha = alpha),
              Color.Transparent,
            )
          ),
        style = Stroke(width = (1.2f + 0.8f * (i % 2)).dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Snowland: calm snow fall over a deep-blue night with hills.
// ---------------------------------------------------------------------------------------------

private class Flake(
  val x: Float,
  val y: Float,
  val size: Float,
  val speed: Float,
  val sway: Float,
  val phase: Float,
  val alpha: Float,
)

@Composable
fun SnowlandBackground(modifier: Modifier = Modifier, running: Boolean = true) {
  val time = rememberSceneSeconds(running)
  val flakes = remember {
    val r = Random(7)
    List(150) {
      val depth = r.nextFloat()
      Flake(
        x = r.nextFloat(),
        y = r.nextFloat(),
        size = 0.8f + depth * 3.6f,
        speed = 0.025f + depth * 0.09f,
        sway = 0.01f + r.nextFloat() * 0.03f,
        phase = r.nextFloat() * 6.28f,
        alpha = 0.35f + depth * 0.6f,
      )
    }
  }
  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val t = time.value
      val w = size.width
      val h = size.height
      drawRect(
        brush =
          Brush.verticalGradient(
            listOf(Color(0xFF050B1F), Color(0xFF0E2145), Color(0xFF244B78), Color(0xFF6D8DB5))
          )
      )
      // soft aurora ribbons
      for (k in 0 until 2) {
        val path = Path()
        val baseY = h * (0.22f + 0.1f * k)
        path.moveTo(0f, baseY)
        for (s in 0..40) {
          val nx = s / 40f
          val y = baseY + h * 0.05f * sin(nx * 5f + t * 0.25f + k * 2f)
          path.lineTo(w * nx, y)
        }
        drawPath(
          path = path,
          brush =
            Brush.horizontalGradient(
              listOf(
                Color.Transparent,
                (if (k == 0) Color(0xFF59FFC8) else Color(0xFF8E7BFF)).copy(alpha = 0.16f),
                Color.Transparent,
              )
            ),
          style = Stroke(width = h * 0.09f, cap = StrokeCap.Round),
        )
      }
      // moon glow
      drawCircle(
        brush =
          Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(w * 0.78f, h * 0.14f),
            radius = w * 0.22f,
          ),
        radius = w * 0.22f,
        center = Offset(w * 0.78f, h * 0.14f),
      )
      drawCircle(Color(0xFFF4F8FF), radius = w * 0.035f, center = Offset(w * 0.78f, h * 0.14f))

      // far hills
      val far = Path()
      far.moveTo(0f, h)
      far.lineTo(0f, h * 0.80f)
      for (s in 0..24) {
        val nx = s / 24f
        far.lineTo(w * nx, h * (0.80f - 0.035f * sin(nx * 5.2f + 0.6f)))
      }
      far.lineTo(w, h)
      far.close()
      drawPath(far, Color(0xFFBFD3EA).copy(alpha = 0.55f))

      // falling snow
      for (f in flakes) {
        val yy = ((f.y + t * f.speed) % 1f) * (h + 20f) - 10f
        val xx = (f.x + sin(t * 0.6f + f.phase) * f.sway) % 1f
        drawCircle(
          color = Color.White.copy(alpha = f.alpha),
          radius = f.size.dp.toPx() * 0.5f,
          center = Offset(xx * w, yy),
        )
      }

      // near hills
      val near = Path()
      near.moveTo(0f, h)
      near.lineTo(0f, h * 0.88f)
      for (s in 0..24) {
        val nx = s / 24f
        near.lineTo(w * nx, h * (0.9f - 0.045f * sin(nx * 3.4f + 2.4f)))
      }
      near.lineTo(w, h)
      near.close()
      drawPath(
        near,
        Brush.verticalGradient(
          listOf(Color(0xFFF3F8FF), Color(0xFFD6E4F5)),
          startY = h * 0.84f,
          endY = h,
        ),
      )
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Tokyo: a night skyline built from squares, thousands of twinkling window lights and neon.
// ---------------------------------------------------------------------------------------------

private class Tower(
  val x: Float,
  val width: Float,
  val height: Float,
  val cols: Int,
  val rows: Int,
  val seed: Int,
  val shade: Float,
)

@Composable
fun TokyoBackground(modifier: Modifier = Modifier, running: Boolean = true) {
  val time = rememberSceneSeconds(running)
  val layers = remember {
    val r = Random(21)
    listOf(0.55f, 0.8f, 1f).map { depth ->
      val towers = ArrayList<Tower>()
      var x = -0.02f
      while (x < 1.02f) {
        val width = 0.07f + r.nextFloat() * 0.09f
        towers +=
          Tower(
            x = x,
            width = width,
            height = (0.18f + r.nextFloat() * 0.34f) * (0.7f + 0.5f * depth),
            cols = 3 + r.nextInt(4),
            rows = 6 + r.nextInt(10),
            seed = r.nextInt(100000),
            shade = depth,
          )
        x += width + 0.005f
      }
      towers
    }
  }
  val neon =
    remember {
      listOf(
        Color(0xFFFF3D9A),
        Color(0xFF3DE7FF),
        Color(0xFFFFC53D),
        Color(0xFF9B5CFF),
        Color(0xFFFFFFFF),
      )
    }
  Canvas(modifier = modifier) {
    val t = time.value
    val w = size.width
    val h = size.height
    drawRect(
      brush =
        Brush.verticalGradient(
          listOf(Color(0xFF05010F), Color(0xFF1A0A33), Color(0xFF3B0F52), Color(0xFF7A1F5C))
        )
    )
    drawCircle(
      brush =
        Brush.radialGradient(
          listOf(Color(0xFFFF4FA3).copy(alpha = 0.35f), Color.Transparent),
          center = Offset(w * 0.5f, h * 0.62f),
          radius = w * 0.8f,
        ),
      radius = w * 0.8f,
      center = Offset(w * 0.5f, h * 0.62f),
    )
    layers.forEachIndexed { li, towers ->
      val depth = 0.55f + 0.225f * li
      val groundY = h * (0.80f + 0.05f * li)
      towers.forEach { tw ->
        val left = tw.x * w
        val tww = tw.width * w
        val top = groundY - tw.height * h
        drawRect(
          color = Color(0xFF0A0614).copy(alpha = 0.55f + 0.4f * tw.shade),
          topLeft = Offset(left, top),
          size = Size(tww, groundY - top + h),
        )
        // roof beacon
        if (tw.seed % 3 == 0) {
          val blink = (sin(t * 2f + tw.seed) * 0.5f + 0.5f)
          drawCircle(
            Color(0xFFFF2D55).copy(alpha = 0.3f + 0.7f * blink),
            radius = 2.5.dp.toPx(),
            center = Offset(left + tww / 2f, top - 4.dp.toPx()),
          )
        }
        // window squares
        val padX = tww * 0.12f
        val padY = (groundY - top) * 0.04f
        val cellW = (tww - padX * 2f) / tw.cols
        val cellH = ((groundY - top) - padY * 2f) / tw.rows
        for (cx in 0 until tw.cols) {
          for (ry in 0 until tw.rows) {
            val key = tw.seed + cx * 131 + ry * 977
            val on = (key % 5) != 0
            if (!on) continue
            val twinkle = 0.55f + 0.45f * sin(t * (0.6f + (key % 7) * 0.25f) + key)
            val c = neon[key % neon.size]
            val aw = cellW * 0.55f
            val ah = cellH * 0.5f
            val px = left + padX + cx * cellW + (cellW - aw) / 2f
            val py = top + padY + ry * cellH + (cellH - ah) / 2f
            drawRoundRect(
              color = c.copy(alpha = (0.35f + 0.55f * twinkle) * depth),
              topLeft = Offset(px, py),
              size = Size(aw, ah),
              cornerRadius = CornerRadius(1.5f, 1.5f),
            )
          }
        }
        // neon edge
        drawRect(
          color = neon[tw.seed % 4].copy(alpha = 0.25f * depth),
          topLeft = Offset(left, top),
          size = Size(2.dp.toPx(), groundY - top),
        )
      }
    }
    // street glow
    drawRect(
      brush =
        Brush.verticalGradient(
          listOf(Color.Transparent, Color(0xFFFF4FA3).copy(alpha = 0.25f)),
          startY = h * 0.86f,
          endY = h,
        ),
      topLeft = Offset(0f, h * 0.86f),
      size = Size(w, h * 0.14f),
    )
  }
}

// ---------------------------------------------------------------------------------------------
// Fireworks: realistic high quality bursts with trails and gravity.
// ---------------------------------------------------------------------------------------------

private class Burst(
  val startTime: Float,
  val x: Float,
  val y: Float,
  val color: Color,
  val seed: Int,
  val count: Int,
  val radius: Float,
)

@Composable
fun FireworksBackground(modifier: Modifier = Modifier, running: Boolean = true) {
  val time = rememberSceneSeconds(running)
  val palette =
    remember {
      listOf(
        Color(0xFFFFC857),
        Color(0xFFFF5E5B),
        Color(0xFF6EE7FF),
        Color(0xFFB388FF),
        Color(0xFF7CFFB2),
        Color(0xFFFF8AD8),
      )
    }
  val stars = remember {
    val r = Random(3)
    List(70) { Triple(r.nextFloat(), r.nextFloat() * 0.8f, r.nextFloat()) }
  }
  Canvas(modifier = modifier) {
    val t = time.value
    val w = size.width
    val h = size.height
    drawRect(
      brush = Brush.verticalGradient(listOf(Color(0xFF02030A), Color(0xFF0A1030), Color(0xFF1B1346)))
    )
    stars.forEach { (sx, sy, sp) ->
      val tw = 0.3f + 0.7f * (sin(t * (0.6f + sp * 1.5f) + sp * 20f) * 0.5f + 0.5f)
      drawCircle(Color.White.copy(alpha = 0.55f * tw), radius = 1.2f, center = Offset(sx * w, sy * h))
    }
    val period = 1.35f
    val currentSlot = (t / period).toInt()
    for (slot in (currentSlot - 3)..currentSlot) {
      if (slot < 0) continue
      val r = Random(slot * 7919 + 13)
      val burst =
        Burst(
          startTime = slot * period,
          x = 0.15f + r.nextFloat() * 0.7f,
          y = 0.14f + r.nextFloat() * 0.38f,
          color = palette[r.nextInt(palette.size)],
          seed = slot,
          count = 52 + r.nextInt(30),
          radius = 0.2f + r.nextFloat() * 0.14f,
        )
      drawBurst(burst, t, w, h)
    }
  }
}

private fun DrawScope.drawBurst(b: Burst, t: Float, w: Float, h: Float) {
  val age = t - b.startTime
  val launch = 0.55f
  if (age < 0f) return
  if (age < launch) {
    // rising rocket with a short tail
    val p = age / launch
    val ry = h * (1f - (1f - b.y) * (1f - (1f - p) * (1f - p)))
    val rx = w * b.x
    drawLine(
      brush =
        Brush.verticalGradient(
          listOf(b.color.copy(alpha = 0.8f), Color.Transparent),
          startY = ry,
          endY = ry + h * 0.06f,
        ),
      start = Offset(rx, ry),
      end = Offset(rx, ry + h * 0.06f),
      strokeWidth = 2.5f,
      cap = StrokeCap.Round,
    )
    drawCircle(Color.White, radius = 2.5f, center = Offset(rx, ry))
    return
  }
  val e = age - launch
  val life = 2.1f
  if (e > life) return
  val fade = (1f - e / life).coerceIn(0f, 1f)
  val cx = w * b.x
  val cy = h * b.y
  val r = Random(b.seed * 31 + 5)
  // flash
  if (e < 0.18f) {
    drawCircle(
      brush =
        Brush.radialGradient(
          listOf(Color.White.copy(alpha = 0.55f * (1f - e / 0.18f)), Color.Transparent),
          center = Offset(cx, cy),
          radius = w * 0.25f,
        ),
      radius = w * 0.25f,
      center = Offset(cx, cy),
    )
  }
  for (i in 0 until b.count) {
    val ang = (i.toFloat() / b.count) * 2f * PI.toFloat() + r.nextFloat() * 0.12f
    val speed = (0.75f + r.nextFloat() * 0.5f) * b.radius * w
    val ease = 1f - (1f - (e / life).coerceIn(0f, 1f)).let { it * it * it }
    val dist = speed * ease * 1.6f
    val gravity = h * 0.09f * e * e
    val px = cx + cos(ang) * dist
    val py = cy + sin(ang) * dist + gravity
    // trail
    val e2 = (e - 0.07f).coerceAtLeast(0f)
    val ease2 = 1f - (1f - (e2 / life).coerceIn(0f, 1f)).let { it * it * it }
    val tx = cx + cos(ang) * speed * ease2 * 1.6f
    val ty = cy + sin(ang) * speed * ease2 * 1.6f + h * 0.09f * e2 * e2
    drawLine(
      color = b.color.copy(alpha = 0.5f * fade),
      start = Offset(tx, ty),
      end = Offset(px, py),
      strokeWidth = 2f,
      cap = StrokeCap.Round,
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.9f * fade),
      radius = 1.6f + 1.2f * fade,
      center = Offset(px, py),
    )
    drawCircle(
      color = b.color.copy(alpha = 0.35f * fade),
      radius = 4.5f * fade + 1f,
      center = Offset(px, py),
    )
  }
}

// ---------------------------------------------------------------------------------------------
// Classic car: a vintage sedan silhouette turning on a lit turntable (generic, unbranded).
// ---------------------------------------------------------------------------------------------

@Composable
fun ClassicCarBackground(modifier: Modifier = Modifier, running: Boolean = true) {
  val time = rememberSceneSeconds(running)
  Canvas(modifier = modifier) {
    val t = time.value
    val w = size.width
    val h = size.height
    drawRect(
      brush = Brush.verticalGradient(listOf(Color(0xFF07090F), Color(0xFF141A28), Color(0xFF0B0E16)))
    )
    // spotlight cone
    val cone = Path().apply {
      moveTo(w * 0.42f, 0f)
      lineTo(w * 0.58f, 0f)
      lineTo(w * 0.95f, h * 0.78f)
      lineTo(w * 0.05f, h * 0.78f)
      close()
    }
    drawPath(
      cone,
      Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f)),
        startY = 0f,
        endY = h * 0.78f,
      ),
    )
    // turntable
    val cx = w * 0.5f
    val floorY = h * 0.62f
    val rx = w * 0.46f
    val ry = h * 0.065f
    drawOval(
      brush =
        Brush.radialGradient(
          listOf(Color(0xFF3A4766), Color(0xFF151B2A)),
          center = Offset(cx, floorY),
          radius = rx,
        ),
      topLeft = Offset(cx - rx, floorY - ry),
      size = Size(rx * 2f, ry * 2f),
    )
    // moving marks on turntable to show rotation
    val ang = t * 0.9f
    for (i in 0 until 12) {
      val a = ang + i * (2f * PI.toFloat() / 12f)
      val px = cx + cos(a) * rx * 0.86f
      val py = floorY + sin(a) * ry * 0.86f
      drawCircle(Color.White.copy(alpha = 0.35f), radius = 2.4f, center = Offset(px, py))
    }

    // car: side profile squashed by cos(angle) to read as a spin
    val spin = t * 0.9f
    val sx = cos(spin)
    val facing = if (sx >= 0f) 1f else -1f
    val squash = (abs(sx) * 0.86f + 0.14f)
    val carW = w * 0.74f
    val carH = carW * 0.30f
    val baseY = floorY - ry * 0.2f
    scale(scaleX = squash * facing, scaleY = 1f, pivot = Offset(cx, baseY)) {
      val left = cx - carW / 2f
      val top = baseY - carH
      val body = Path().apply {
        moveTo(left + carW * 0.02f, baseY - carH * 0.30f)
        lineTo(left + carW * 0.03f, baseY - carH * 0.52f)
        cubicTo(left + carW * 0.15f, baseY - carH * 0.58f, left + carW * 0.22f, baseY - carH * 0.60f, left + carW * 0.27f, baseY - carH * 0.64f)
        lineTo(left + carW * 0.33f, top + carH * 0.12f)
        cubicTo(left + carW * 0.40f, top + carH * 0.00f, left + carW * 0.62f, top + carH * 0.00f, left + carW * 0.70f, top + carH * 0.14f)
        lineTo(left + carW * 0.77f, baseY - carH * 0.60f)
        cubicTo(left + carW * 0.90f, baseY - carH * 0.58f, left + carW * 0.97f, baseY - carH * 0.52f, left + carW * 0.98f, baseY - carH * 0.30f)
        lineTo(left + carW * 0.97f, baseY - carH * 0.12f)
        lineTo(left + carW * 0.03f, baseY - carH * 0.12f)
        close()
      }
      drawPath(
        body,
        Brush.verticalGradient(
          listOf(Color(0xFFB9C4D8), Color(0xFF6E7C96), Color(0xFF2A3246)),
          startY = top,
          endY = baseY,
        ),
      )
      // windows
      val glass = Path().apply {
        moveTo(left + carW * 0.30f, top + carH * 0.18f)
        lineTo(left + carW * 0.35f, top + carH * 0.06f)
        cubicTo(left + carW * 0.42f, top + carH * 0.02f, left + carW * 0.60f, top + carH * 0.02f, left + carW * 0.67f, top + carH * 0.15f)
        lineTo(left + carW * 0.72f, top + carH * 0.34f)
        lineTo(left + carW * 0.30f, top + carH * 0.34f)
        close()
      }
      drawPath(glass, Color(0xFF0C1322).copy(alpha = 0.85f))
      // lights
      drawCircle(Color(0xFFFFF1B8), radius = carH * 0.055f, center = Offset(left + carW * 0.965f, baseY - carH * 0.34f))
      drawCircle(Color(0xFFFF3B3B), radius = carH * 0.05f, center = Offset(left + carW * 0.035f, baseY - carH * 0.34f))
      // wheels
      val wheelR = carH * 0.19f
      for (wx in listOf(0.22f, 0.78f)) {
        val wc = Offset(left + carW * wx, baseY - carH * 0.10f)
        drawCircle(Color(0xFF07090F), radius = wheelR, center = wc)
        drawCircle(Color(0xFFC4CCDA), radius = wheelR * 0.55f, center = wc)
        drawCircle(Color(0xFF2A3246), radius = wheelR * 0.18f, center = wc)
      }
    }
    // floor reflection glow
    drawOval(
      brush =
        Brush.radialGradient(
          listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
          center = Offset(cx, floorY + ry * 1.4f),
          radius = rx,
        ),
      topLeft = Offset(cx - rx, floorY + ry * 0.4f),
      size = Size(rx * 2f, ry * 2.4f),
    )
  }
}
