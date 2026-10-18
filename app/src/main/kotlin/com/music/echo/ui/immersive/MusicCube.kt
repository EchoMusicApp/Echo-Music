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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
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
private fun rememberSongPainters(songs: List<Song>): List<Painter> {
  val ctx = LocalContext.current
  return List(6) { i ->
    val url = songs.getOrNull(i % maxOf(1, songs.size))?.thumbnailUrl
    rememberAsyncImagePainter(
      ImageRequest.Builder(ctx).data(url).size(256, 256).scale(Scale.FILL).precision(Precision.EXACT).build()
    )
  }
}

private fun playCubeSong(connection: echo.music.iad1tya.playback.PlayerConnection?, song: Song, currentId: String?) {
  connection ?: return
  if (currentId == song.id) connection.togglePlayPause()
  else connection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
}

/**
 * Small transparent liquid-glass cube for Home. Each of the six faces shows a song;
 * swipe to turn it, tap the front face to play that song.
 */
@Composable
fun MusicCubeHomeCard(navController: NavController, songs: List<Song>, modifier: Modifier = Modifier) {
  val (enabled, _) = rememberPreference(MusicCubeEnabledKey, true)
  if (!enabled || songs.isEmpty()) return
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
  val shape = RoundedCornerShape(28.dp)
  Column(
    modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(shape)
      .background(
        Brush.linearGradient(
          listOf(Color.White.copy(alpha = 0.16f), Color(0xFF9FE8FF).copy(alpha = 0.07f), Color.White.copy(alpha = 0.10f))
        )
      )
      .border(
        1.dp,
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.30f))),
        shape,
      )
      .padding(12.dp)
  ) {
    if (useGrid) CubeGrid(songs) else CubeBody(songs, reduced)
  }
}

@Composable
private fun CubeGrid(songs: List<Song>) {
  val connection = LocalPlayerConnection.current
  val current by (connection?.mediaMetadata ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
  Text("Music Cube", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
  for (row in 0..1) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      for (col in 0..2) {
        val song = songs.getOrNull((row * 3 + col) % songs.size) ?: continue
        Column(
          Modifier.weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClickLabel = "Play ${song.title}") { playCubeSong(connection, song, current?.id) }
            .padding(6.dp)
        ) {
          AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
          )
          Text(song.title, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
      }
    }
  }
}

@Composable
private fun CubeBody(songs: List<Song>, reduced: Boolean) {
  val scope = rememberCoroutineScope()
  val rx = remember { Animatable(0f) }
  val ry = remember { Animatable(0f) }
  var shown by remember { mutableIntStateOf(0) }
  val painters = rememberSongPainters(songs)
  val connection = LocalPlayerConnection.current
  val current by (connection?.mediaMetadata ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
  var lastDx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
  var lastDy by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
  val titles = remember(songs) { List(6) { songs[it % songs.size].title } }

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
  val front = frontFace(rx.value, ry.value)
  val frontSong = songs[front % songs.size]

  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier.size(126.dp)
        .semantics { contentDescription = "Music cube. Front song: ${frontSong.title}. Swipe to turn, tap to play." }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { lastDx = 0f; lastDy = 0f },
            onDragEnd = { snapToFace(frontFace(rx.value + lastDy * 5f, ry.value + lastDx * 5f)) },
            onDragCancel = { snapToFace(frontFace(rx.value, ry.value)) },
          ) { change, drag ->
            change.consume()
            lastDx = drag.x
            lastDy = drag.y
            scope.launch {
              ry.snapTo(ry.value + drag.x * 0.9f)
              rx.snapTo(rx.value + drag.y * 0.9f)
            }
          }
        }
        .pointerInput(Unit) {
          detectTapGestures {
            val f = frontFace(rx.value, ry.value)
            playCubeSong(connection, songs[f % songs.size], current?.id)
          }
        }
    ) {
      Canvas(Modifier.size(126.dp)) { drawGlassCube(rx.value, ry.value, painters, titles) }
    }

    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text("MUSIC CUBE", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(2.dp))
      Text(frontSong.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Text(
        frontSong.artists.joinToString { it.name },
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          Modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClickLabel = "Play ${frontSong.title}") { playCubeSong(connection, frontSong, current?.id) }
            .padding(horizontal = 16.dp, vertical = 7.dp)
        ) { Text("Play", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Box(
          Modifier.clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
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
    textAlign = Paint.Align.LEFT
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    textSize = 19f
    setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
  }

private class FaceDraw(val index: Int, val z: Float, val m: AMatrix, val lit: Float)

/** Draws all six faces back-to-front as clear glass: the back faces show through, mirrored, like real water-clear glass. */
private fun DrawScope.drawGlassCube(rxDeg: Float, ryDeg: Float, painters: List<Painter>, titles: List<String>) {
  val cx = size.width / 2f
  val cy = size.height / 2f
  val s = size.minDimension * 0.30f
  val cam = 4.6f
  // soft aqua glow under the cube instead of a dark shadow
  drawOval(
    Brush.radialGradient(listOf(Color(0xFF8FE9FF).copy(alpha = 0.35f), Color.Transparent), center = Offset(cx, cy + s * 1.45f), radius = s * 1.2f),
    topLeft = Offset(cx - s * 1.2f, cy + s * 1.3f),
    size = Size(s * 2.4f, s * 0.34f),
  )
  val light = floatArrayOf(-0.35f, 0.55f, 0.76f)
  fun proj(p: P3): Offset {
    val k = cam / (cam - p.z)
    return Offset(cx + p.x * k * s, cy - p.y * k * s)
  }
  val faces = ArrayList<FaceDraw>(6)
  cubeFaces.forEachIndexed { i, f ->
    val n = rot(f.nx, f.ny, f.nz, rxDeg, ryDeg)
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
    val m = AMatrix()
    m.setPolyToPoly(floatArrayOf(0f, 0f, 200f, 0f, 200f, 200f, 0f, 200f), 0, floatArrayOf(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y), 0, 4)
    val lit = (0.55f + 0.45f * max(0f, n.x * light[0] + n.y * light[1] + n.z * light[2])).coerceIn(0.4f, 1f)
    faces += FaceDraw(i, n.z, m, lit)
  }
  faces.sortBy { it.z }
  val corner = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
  for (fd in faces) {
    val front = fd.z > 0.02f
    val a = if (front) 1f else 0.30f
    drawIntoCanvas { c -> c.nativeCanvas.save(); c.nativeCanvas.concat(fd.m) }
    // artwork seen through glass
    with(painters[fd.index]) { draw(Size(200f, 200f), alpha = (if (front) 0.92f else 0.5f) * fd.lit * a.coerceAtLeast(0.6f)) }
    // clear, slightly aqua body
    drawRoundRect(
      brush = Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.26f * a), Color(0xFFA6EBFF).copy(alpha = 0.06f * a), Color.White.copy(alpha = 0.14f * a)),
        start = Offset(0f, 0f), end = Offset(200f, 200f),
      ),
      size = Size(200f, 200f),
      cornerRadius = corner,
    )
    if (front) {
      // title scrim + text
      drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 120f, endY = 200f),
        topLeft = Offset(0f, 120f),
        size = Size(200f, 80f),
      )
      drawIntoCanvas { c ->
        val t = titles[fd.index]
        val n = labelPaint.breakText(t, true, 176f, null)
        val text = if (n < t.length) t.take(max(1, n - 1)) + "…" else t
        c.nativeCanvas.drawText(text, 12f, 184f, labelPaint)
      }
      // little play badge
      drawCircle(Color.White.copy(alpha = 0.30f), radius = 17f, center = Offset(166f, 34f))
      val tri = androidx.compose.ui.graphics.Path().apply {
        moveTo(160f, 25f); lineTo(160f, 43f); lineTo(176f, 34f); close()
      }
      drawPath(tri, Color.White)
      // water specular highlight
      drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f * fd.lit), Color.Transparent), center = Offset(48f, 40f), radius = 96f),
        radius = 96f, center = Offset(48f, 40f),
      )
      // bright rim
      drawRoundRect(color = Color.White.copy(alpha = 0.75f * fd.lit), size = Size(200f, 200f), cornerRadius = corner, style = Stroke(width = 3.5f))
      drawRoundRect(color = Color(0xFFB8F1FF).copy(alpha = 0.35f), topLeft = Offset(7f, 7f), size = Size(186f, 186f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f), style = Stroke(width = 1.5f))
    } else {
      drawRoundRect(color = Color.White.copy(alpha = 0.28f), size = Size(200f, 200f), cornerRadius = corner, style = Stroke(width = 2.5f))
    }
    drawIntoCanvas { c -> c.nativeCanvas.restore() }
  }
}
