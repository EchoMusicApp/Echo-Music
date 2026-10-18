package echo.music.iad1tya.ui.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private class Drop(
  var x: Float, var y: Float, var r: Float,
  var vx: Float = 0f, var vy: Float = 0f,
  var sliding: Boolean = false,
  var wobble: Float = 0f,
) {
  val tx = FloatArray(TRAIL)
  val ty = FloatArray(TRAIL)
  var tn = 0
  var lastTx = x
  var lastTy = y
  companion object { const val TRAIL = 14 }
}

@Stable
private class DropWorld {
  val rnd = Random(77)
  val drops = ArrayList<Drop>()
  var w = 0f
  var h = 0f
  var gx = 0f // smoothed gravity direction, -1..1, screen space (x right, y down)
  var gy = 0.12f
  var tiltX = 0f
  var tiltY = 0f
  var spawnClock = 0f
  var frame by mutableLongStateOf(0L)
}

/** Reads the accelerometer only while the screen is visible. Output is a gravity direction in screen space. */
private class TiltListener(private val world: DropWorld) : SensorEventListener {
  override fun onSensorChanged(e: SensorEvent) {
    val ax = e.values[0] / 9.81f
    val ay = e.values[1] / 9.81f
    // up-vector reading → direction a drop would roll on the glass
    val tx = (-ax).coerceIn(-1f, 1f)
    val ty = (ay).coerceIn(-1f, 1f)
    world.gx += (tx - world.gx) * 0.18f
    world.gy += (ty - world.gy) * 0.18f
  }
  override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
}

private fun spawn(world: DropWorld, small: Boolean, atTop: Boolean = false): Drop {
  val r = if (small) 3.5f + world.rnd.nextFloat() * 5.5f else 9f + world.rnd.nextFloat() * 12f
  val x = world.rnd.nextFloat() * world.w
  val y = if (atTop) -r * 2f else world.rnd.nextFloat() * world.h
  return Drop(x, y, r * 2.2f)
}

/**
 * Rain-on-glass: realistic refractive water drops. Drops cling to the glass, and when you tilt the phone
 * the bigger ones start sliding toward the low side, leaving wet trails and merging with smaller drops.
 * Sensor + frame loop run only while the screen is visible.
 */
@Composable
fun WaterDropsBackground(tint: Color, modifier: Modifier = Modifier, dark: Boolean = true) {
  val world = remember { DropWorld() }
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current

  LaunchedEffect(owner) {
    owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      val listener = TiltListener(world)
      if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
      try {
        var last = 0L
        var lastDraw = 0L
        while (true) {
          withFrameNanos { now ->
            if (last == 0L) { last = now; lastDraw = now }
            if (now - lastDraw < 22_000_000L) return@withFrameNanos // ~45 fps
            val dt = ((now - lastDraw) / 1_000_000_000f).coerceIn(0.001f, 0.06f)
            lastDraw = now
            step(world, dt)
            world.frame = now
          }
        }
      } finally {
        sm?.unregisterListener(listener)
      }
    }
  }

  Canvas(modifier.fillMaxSize()) {
    @Suppress("UNUSED_VARIABLE") val tick = world.frame
    if (world.w != size.width || world.h != size.height) {
      world.w = size.width
      world.h = size.height
      if (world.drops.isEmpty()) {
        val d = size.density
        repeat(14) { world.drops += spawn(world, small = false).also { it.r *= d / 2.6f } }
        repeat(22) { world.drops += spawn(world, small = true).also { it.r *= d / 2.6f } }
      }
    }
    // faint glass sheen that slides with the tilt
    val sheenX = size.width * (0.5f + world.gx * 0.35f)
    drawRect(
      Brush.linearGradient(
        listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.05f else 0.18f), Color.Transparent),
        start = Offset(sheenX - size.width * 0.5f, 0f),
        end = Offset(sheenX + size.width * 0.2f, size.height),
      )
    )
    val px = world.gx * 6f
    val py = world.gy * 4f
    for (d in world.drops) {
      drawTrail(d, dark)
    }
    for (d in world.drops) drawDrop(d, tint, dark, px, py)
  }
}

private fun step(w: DropWorld, dt: Float) {
  if (w.w <= 0f) return
  val g = hypot(w.gx, w.gy)
  val dens = 1f
  // new tiny drops now and then, like light rain
  w.spawnClock += dt
  if (w.spawnClock > 0.9f) {
    w.spawnClock = 0f
    if (w.drops.size < 46) w.drops += spawn(w, small = true).also { it.r *= 0.9f + dens * 0.4f }
  }
  val maxR = 22f * 2.2f * 1.6f
  val born = ArrayList<Drop>()
  val remove = HashSet<Drop>()
  for (d in w.drops) {
    val big = (d.r / maxR).coerceIn(0f, 1f)
    val stick = 0.10f + 0.55f * (1f - big).pow(1.5f)
    if (!d.sliding && g > stick && d.r > 14f) d.sliding = true
    if (d.sliding) {
      val pull = (g - stick * 0.45f).coerceAtLeast(0f)
      val dirx = if (g > 0.001f) w.gx / g else 0f
      val diry = if (g > 0.001f) w.gy / g else 1f
      val acc = pull * (500f + big * 1200f)
      d.wobble += dt * (4f + big * 6f)
      val side = kotlin.math.sin(d.wobble * 3.1f) * 40f * (1f - big * 0.4f)
      d.vx += (dirx * acc - diry * side) * dt
      d.vy += (diry * acc + dirx * side) * dt
      val damp = 0.90f.pow(dt * 60f)
      d.vx *= damp; d.vy *= damp
      d.x += d.vx * dt
      d.y += d.vy * dt
      // shrinks a little as it leaves a trail
      d.r = max(9f, d.r - dt * 0.5f)
      val moved = hypot(d.x - d.lastTx, d.y - d.lastTy)
      if (moved > 10f) {
        if (d.tn < Drop.TRAIL) d.tn++
        for (i in d.tn - 1 downTo 1) { d.tx[i] = d.tx[i - 1]; d.ty[i] = d.ty[i - 1] }
        d.tx[0] = d.x; d.ty[0] = d.y
        d.lastTx = d.x; d.lastTy = d.y
      }
      if (hypot(d.vx, d.vy) < 14f && g < stick * 0.6f) { d.sliding = false; d.vx = 0f; d.vy = 0f; d.tn = 0 }
      // merge with smaller drops it touches
      for (o in w.drops) {
        if (o === d || o in remove || o.sliding && o.r >= d.r) continue
        if (hypot(o.x - d.x, o.y - d.y) < (d.r + o.r) * 0.62f && o.r < d.r) {
          d.r = min(maxR, sqrt(d.r * d.r + o.r * o.r * 0.75f))
          remove += o
        }
      }
      if (d.x < -d.r * 3 || d.x > w.w + d.r * 3 || d.y < -d.r * 3 || d.y > w.h + d.r * 3) remove += d
    }
  }
  for (d in remove) {
    w.drops.remove(d)
    born += spawn(w, small = true, atTop = w.gy >= 0f).also { nd -> nd.r *= 1.2f }
  }
  w.drops.addAll(born)
}

private fun DrawScope.drawTrail(d: Drop, dark: Boolean) {
  if (d.tn < 2) return
  val c = if (dark) Color.White else Color(0xFF1B2B3A)
  var px = d.x; var py = d.y
  for (i in 0 until d.tn) {
    val a = (1f - i / d.tn.toFloat()) * (if (dark) 0.10f else 0.12f)
    drawLine(
      c.copy(alpha = a), Offset(px, py), Offset(d.tx[i], d.ty[i]),
      strokeWidth = max(2f, d.r * 0.34f * (1f - i / d.tn.toFloat())), cap = StrokeCap.Round,
    )
    px = d.tx[i]; py = d.ty[i]
  }
}

private fun DrawScope.drawDrop(d: Drop, tint: Color, dark: Boolean, parX: Float, parY: Float) {
  val r = d.r
  val speed = hypot(d.vx, d.vy)
  val stretch = 1f + min(0.55f, speed / 520f)
  val ang = if (speed > 6f) (atan2(d.vy, d.vx) * 180f / PI.toFloat()) - 90f else 0f
  val cx = d.x + parX * (r / 30f)
  val cy = d.y + parY * (r / 30f)
  withTransform({
    translate(cx, cy)
    rotate(ang, Offset.Zero)
    scale(1f, stretch, Offset.Zero)
  }) {
    // contact shadow on the glass, offset down-right
    drawCircle(Color.Black.copy(alpha = if (dark) 0.30f else 0.16f), radius = r * 1.06f, center = Offset(r * 0.10f, r * 0.16f))
    // refractive body: clear centre, tinted lower edge where light gathers
    drawCircle(
      Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.02f),
        0.62f to tint.copy(alpha = 0.10f),
        0.88f to Color.White.copy(alpha = if (dark) 0.20f else 0.28f),
        1f to Color.White.copy(alpha = if (dark) 0.34f else 0.40f),
        center = Offset(-r * 0.12f, -r * 0.18f),
        radius = r * 1.12f,
      ),
      radius = r,
    )
    // inverted-image darkening at the top (drops flip the scene)
    drawArc(
      Color.Black.copy(alpha = if (dark) 0.34f else 0.20f),
      startAngle = 200f, sweepAngle = 140f, useCenter = false,
      topLeft = Offset(-r * 0.86f, -r * 0.86f), size = Size(r * 1.72f, r * 1.72f),
      style = Stroke(width = r * 0.22f, cap = StrokeCap.Round),
    )
    // bright caustic crescent at the bottom, where the light is focused
    drawArc(
      Color.White.copy(alpha = 0.62f),
      startAngle = 25f, sweepAngle = 85f, useCenter = false,
      topLeft = Offset(-r * 0.74f, -r * 0.74f), size = Size(r * 1.48f, r * 1.48f),
      style = Stroke(width = r * 0.16f, cap = StrokeCap.Round),
    )
    // specular window highlight
    rotate(-28f, Offset(-r * 0.36f, -r * 0.42f)) {
      drawOval(
        Color.White.copy(alpha = 0.92f),
        topLeft = Offset(-r * 0.36f - r * 0.22f, -r * 0.42f - r * 0.12f),
        size = Size(r * 0.44f, r * 0.24f),
      )
    }
    drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.09f, center = Offset(r * 0.34f, r * 0.46f))
    // fine rim
    drawCircle(Color.White.copy(alpha = if (dark) 0.28f else 0.45f), radius = r, style = Stroke(width = 1.2f))
  }
}
