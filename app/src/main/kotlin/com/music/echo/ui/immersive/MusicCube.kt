package echo.music.iad1tya.ui.immersive

import android.content.Context
import android.graphics.Matrix as AMatrix
import android.graphics.Paint
import android.graphics.Typeface
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.MusicCubeEnabledKey
import echo.music.iad1tya.constants.MusicCubeGridKey
import echo.music.iad1tya.constants.MusicCubeReducedMotionKey
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private class CubeFace(
  val name: String,
  val nx: Float, val ny: Float, val nz: Float,
  val ux: Float, val uy: Float, val uz: Float,
  val vx: Float, val vy: Float, val vz: Float,
  /** Orientation (pitch, yaw in degrees) that brings this face to the front. */
  val rx: Float, val ry: Float,
)

private val cubeFaces =
  listOf(
    CubeFace("Favorites", 0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f),
    CubeFace("Recently Played", 1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f, 0f, -90f),
    CubeFace("Playlists", 0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f, 0f, 180f),
    CubeFace("Albums", -1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 90f),
    CubeFace("Artists", 0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, -1f, 90f, 0f),
    CubeFace("Discover", 0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, -90f, 0f),
  )

private val rad = (PI / 180.0).toFloat()

private class P3(val x: Float, val y: Float, val z: Float)

/** Rotate by yaw (about Y) then pitch (about X). */
private fun rot(x: Float, y: Float, z: Float, rxDeg: Float, ryDeg: Float): P3 {
  val cy = cos(ryDeg * rad); val sy = sin(ryDeg * rad)
  val x1 = x * cy + z * sy
  val z1 = -x * sy + z * cy
  val cx = cos(rxDeg * rad); val sx = sin(rxDeg * rad)
  val y2 = y * cx - z1 * sx
  val z2 = y * sx + z1 * cx
  return P3(x1, y2, z2)
}

private fun frontFace(rx: Float, ry: Float): Int {
  var best = 0
  var bz = -2f
  cubeFaces.forEachIndexed { i, f ->
    val z = rot(f.nx, f.ny, f.nz, rx, ry).z
    if (z > bz) { bz = z; best = i }
  }
  return best
}

private fun unwrap(current: Float, target: Float): Float = target + 360f * ((current - target) / 360f).roundToInt()

@Composable
private fun rememberCubeFacePainters(): List<Painter> =
  listOf(
    painterResource(R.drawable.favorite),
    painterResource(R.drawable.history),
    painterResource(R.drawable.playlist_play),
    painterResource(R.drawable.album),
    painterResource(R.drawable.artist),
    painterResource(R.drawable.explore_outlined),
  )

/** Small rotatable cube shown with Quick Picks. Swipe to turn, tap a face to open it, snaps to a face. */
@Composable
fun MusicCubeHomeCard(navController: NavController, modifier: Modifier = Modifier) {
  val (enabled, _) = rememberPreference(MusicCubeEnabledKey, true)
  if (!enabled) return
  val (reducedPref, _) = rememberPreference(MusicCubeReducedMotionKey, false)
  val (gridPref, _) = rememberPreference(MusicCubeGridKey, false)
  val context = LocalContext.current
  val screenReader =
    remember {
      runCatching {
          (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isTouchExplorationEnabled
        }
        .getOrDefault(false)
    }
  val systemOff =
    remember {
      runCatching {
          android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }
        .getOrDefault(false)
    }
  val reduced = reducedPref || systemOff
  val useGrid = gridPref || screenReader

  Column(
    modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(26.dp))
      .background(Brush.linearGradient(listOf(Color(0xFF0B0D12), Color(0xFF151A26))))
      .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
      .padding(12.dp)
  ) {
    if (useGrid) CubeGrid(navController) else CubeBody(navController, reduced)
  }
}

@Composable
private fun CubeGrid(navController: NavController) {
  Text("Music Cube", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
  val painters = rememberCubeFacePainters()
  for (row in 0..1) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      for (col in 0..2) {
        val i = row * 3 + col
        Row(
          Modifier.weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(onClickLabel = "Open ${cubeFaces[i].name}") { navigateCityDestination(navController, i) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(painters[i], contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Text(cubeFaces[i].name, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun CubeBody(navController: NavController, reduced: Boolean) {
  val scope = rememberCoroutineScope()
  val rx = remember { Animatable(0f) }
  val ry = remember { Animatable(0f) }
  var shown by remember { mutableIntStateOf(0) }
  val painters = rememberCubeFacePainters()
  var lastDx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
  var lastDy by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

  fun snapToFace(i: Int) {
    val f = cubeFaces[i]
    shown = i
    val targetRy = unwrap(ry.value, f.ry)
    val targetRx = unwrap(rx.value, f.rx)
    scope.launch {
      if (reduced) {
        rx.snapTo(targetRx); ry.snapTo(targetRy)
      } else {
        launch { rx.animateTo(targetRx, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
        launch { ry.animateTo(targetRy, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
      }
    }
  }

  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier.size(118.dp)
        .semantics { contentDescription = "Music cube, showing ${cubeFaces[shown].name}. Swipe to turn, double tap actions below." }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { lastDx = 0f; lastDy = 0f },
            onDragEnd = {
              val px = rx.value + lastDy * 5f
              val py = ry.value + lastDx * 5f
              snapToFace(frontFace(px, py))
            },
            onDragCancel = { snapToFace(frontFace(rx.value, ry.value)) },
          ) { change, drag ->
            change.consume()
            lastDx = drag.x
            lastDy = drag.y
            scope.launch {
              ry.snapTo(ry.value + drag.x * 0.9f)
              rx.snapTo((rx.value + drag.y * 0.9f))
            }
          }
        }
        .pointerInput(Unit) {
          detectTapGestures {
            val f = frontFace(rx.value, ry.value)
            navigateCityDestination(navController, f)
          }
        }
    ) {
      Canvas(Modifier.size(118.dp)) {
        drawCube(rx.value, ry.value, painters)
      }
    }

    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text("MUSIC CUBE", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(2.dp))
      Text(cubeFaces[frontFace(rx.value, ry.value)].name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
      Spacer(Modifier.height(2.dp))
      Text("Swipe to turn · tap to open", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          Modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClickLabel = "Open ${cubeFaces[frontFace(rx.value, ry.value)].name}") {
              navigateCityDestination(navController, frontFace(rx.value, ry.value))
            }
            .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text("Open", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Box(
          Modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClickLabel = "Reset cube") { snapToFace(0) }
            .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text("Reset", color = Color.White, fontSize = 12.sp) }
      }
    }
  }
}

private val labelPaint =
  Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    textSize = 24f
  }

private fun DrawScope.drawCube(rxDeg: Float, ryDeg: Float, painters: List<Painter>) {
  val cx = size.width / 2f
  val cy = size.height / 2f
  val s = size.minDimension * 0.30f
  val cam = 4.6f
  // soft contact shadow under the cube: restrained, one cheap oval
  drawOval(
    Color.Black.copy(alpha = 0.35f),
    topLeft = Offset(cx - s * 0.9f, cy + s * 1.38f),
    size = Size(s * 1.8f, s * 0.22f),
  )
  val light = floatArrayOf(-0.35f, 0.55f, 0.76f)
  fun proj(p: P3): Offset {
    val k = cam / (cam - p.z)
    return Offset(cx + p.x * k * s, cy - p.y * k * s)
  }
  cubeFaces.forEachIndexed { i, f ->
    val n = rot(f.nx, f.ny, f.nz, rxDeg, ryDeg)
    if (n.z <= 0.02f) return@forEachIndexed // back-face culled
    fun corner(su: Float, sv: Float): Offset =
      proj(
        rot(
          f.nx + su * f.ux + sv * f.vx,
          f.ny + su * f.uy + sv * f.vy,
          f.nz + su * f.uz + sv * f.vz,
          rxDeg, ryDeg,
        )
      )
    val tl = corner(-1f, 1f); val tr = corner(1f, 1f); val br = corner(1f, -1f); val bl = corner(-1f, -1f)
    val lit = (0.50f + 0.50f * max(0f, n.x * light[0] + n.y * light[1] + n.z * light[2])).coerceIn(0.35f, 1f)
    val m = AMatrix()
    val src = floatArrayOf(0f, 0f, 200f, 0f, 200f, 200f, 0f, 200f)
    val dst = floatArrayOf(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
    m.setPolyToPoly(src, 0, dst, 0, 4)
    drawIntoCanvas { c ->
      val nc = c.nativeCanvas
      nc.save()
      nc.concat(m)
    }
    // dark glass face with a restrained highlight
    drawRoundRect(
      brush = Brush.linearGradient(
        listOf(Color(0xFF1B2130).copy(alpha = 1f).scaleRgb(lit), Color(0xFF0B0E16).scaleRgb(lit)),
        start = Offset(0f, 0f), end = Offset(200f, 200f),
      ),
      size = Size(200f, 200f),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
    )
    drawRoundRect(
      color = Color.White.copy(alpha = 0.18f * lit),
      size = Size(200f, 200f),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
      style = Stroke(width = 3f),
    )
    translate(left = 70f, top = 38f) {
      with(painters[i]) { draw(Size(60f, 60f), alpha = 0.95f * lit, colorFilter = ColorFilter.tint(Color.White)) }
    }
    drawIntoCanvas { c ->
      labelPaint.alpha = (255 * (0.55f + 0.45f * lit)).toInt().coerceIn(0, 255)
      val label = f.name
      labelPaint.textSize = if (label.length > 10) 20f else 25f
      c.nativeCanvas.drawText(label, 100f, 152f, labelPaint)
      c.nativeCanvas.restore()
    }
  }
}

private fun Color.scaleRgb(f: Float) = Color(red * f, green * f, blue * f, alpha)
