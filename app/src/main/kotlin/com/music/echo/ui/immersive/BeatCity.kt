package echo.music.iad1tya.ui.immersive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import echo.music.iad1tya.constants.CityIntensityKey
import echo.music.iad1tya.constants.CityLayoutKey
import echo.music.iad1tya.constants.CityLightingKey
import echo.music.iad1tya.constants.CityThemeStyleKey
import echo.music.iad1tya.constants.CityTowerArtKey
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object CityOptions {
  val themes = listOf("Cyber City", "Midnight City", "Neon Rain")
  val lighting = listOf("Dim", "Normal", "Bright")
  val intensities = listOf("Low", "Medium", "High")
  val layouts = listOf("Full screen", "Compact")
}

/** Buildings that open real destinations when tapped. */
object CityDestinations {
  val names = listOf("Favorites", "Recent", "Playlists", "Albums", "Artists", "Discover")
}

class CityConfig(val theme: Int, val lighting: Int, val intensity: Int, val compact: Boolean, val towerArt: Boolean)

@Composable
fun rememberCityConfig(forceCompact: Boolean? = null): CityConfig {
  val (theme, _) = rememberPreference(CityThemeStyleKey, 0)
  val (lighting, _) = rememberPreference(CityLightingKey, 1)
  val (intensity, _) = rememberPreference(CityIntensityKey, 1)
  val (layout, _) = rememberPreference(CityLayoutKey, 0)
  val (art, _) = rememberPreference(CityTowerArtKey, true)
  return CityConfig(theme, lighting, intensity, forceCompact ?: (layout == 1), art)
}

private class Bld(
  val x: Float, val w: Float, val h: Float, val layer: Int, val seed: Int, val cols: Int, val rows: Int, val special: Int,
)

private fun hash01(a: Int, b: Int, c: Int): Float {
  var h = a * 374761393 + b * 668265263 + c * 2147483647
  h = (h xor (h ushr 13)) * 1274126177
  h = h xor (h ushr 16)
  return (h and 0xFFFF) / 65535f
}

private class CityTheme(
  val skyTop: Color, val skyMid: Color, val skyBottom: Color,
  val accentA: Color, val accentB: Color, val warm: Color,
)

private fun themeOf(i: Int) =
  when (i) {
    1 -> CityTheme(Color(0xFF02030A), Color(0xFF060B22), Color(0xFF0D1B3E), Color(0xFF6EA8FF), Color(0xFF9B8CFF), Color(0xFFFFD27A))
    2 -> CityTheme(Color(0xFF050008), Color(0xFF12001E), Color(0xFF260036), Color(0xFFFF3D81), Color(0xFF39FF88), Color(0xFFFFB3D1))
    else -> CityTheme(Color(0xFF0A0220), Color(0xFF1A0640), Color(0xFF2B0A5E), Color(0xFF00F0FF), Color(0xFFFF2BD6), Color(0xFFFFE86B))
  }

private fun mix(a: Color, b: Color, t: Float) =
  Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

/**
 * Miniature night city drawn in 2D with parallax layers (no 3D engine, one Canvas).
 * [interactive]: swipe to rotate, pinch to zoom, tap special buildings.
 */
@Composable
fun BeatCityView(
  config: CityConfig,
  reactive: Reactive,
  palette: AlbumPalette,
  thumbnailUrl: String?,
  motion: MotionSettings,
  interactive: Boolean,
  modifier: Modifier = Modifier,
  onDestination: (Int) -> Unit = {},
) {
  val buildings =
    remember {
      val r = Random(902)
      val list = ArrayList<Bld>()
      val counts = intArrayOf(16, 13, 9)
      for (layer in 0..2) {
        val n = counts[layer]
        for (i in 0 until n) {
          val special = if (layer == 1 && i % 2 == 0 && i / 2 < 6) i / 2 else -1
          val baseW = when (layer) { 0 -> 0.075f; 1 -> 0.10f; else -> 0.125f }
          val w = baseW * (0.8f + r.nextFloat() * 0.45f) * (if (special >= 0) 1.25f else 1f)
          val hh = when (layer) { 0 -> 0.12f + r.nextFloat() * 0.16f; 1 -> 0.15f + r.nextFloat() * 0.18f; else -> 0.12f + r.nextFloat() * 0.17f } * (if (special >= 0) 1.25f else 1f)
          val cols = when (layer) { 0 -> 3; 1 -> 4; else -> 5 }
          val rows = when (layer) { 0 -> 6; 1 -> 8; else -> 9 }
          list += Bld((i + 0.15f + r.nextFloat() * 0.7f) / n, w, hh, layer, r.nextInt(100000), cols, rows, special)
        }
      }
      list
    }
  val theme = themeOf(config.theme)
  val accentA = mix(theme.accentA, palette.a, 0.40f)
  val accentB = mix(theme.accentB, palette.b, 0.40f)
  val lightMul = when (config.lighting) { 0 -> 0.65f; 2 -> 1.35f; else -> 1f }
  val k = when (config.intensity) { 0 -> 0.6f; 2 -> 1.5f; else -> 1f }

  var scroll by remember { mutableFloatStateOf(0f) }
  var zoom by remember { mutableFloatStateOf(1f) }
  val hitRects = remember { arrayOfNulls<Rect>(6) }
  val measurer = rememberTextMeasurer()
  val painter = rememberAsyncImagePainter(model = thumbnailUrl)
  val onDest by rememberUpdatedState(onDestination)

  var gestureMod: Modifier = Modifier
  if (interactive) {
    gestureMod =
      Modifier.pointerInput(Unit) {
          detectTransformGestures { _, pan, z, _ ->
            zoom = (zoom * z).coerceIn(1f, 2.4f)
            scroll -= pan.x / (size.width * 1.8f) / zoom
          }
        }
        .pointerInput(Unit) {
          detectTapGestures { p ->
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val groundY = h * (if (config.compact) 0.80f else 0.78f)
            for (i in hitRects.indices.reversed()) {
              val rc = hitRects[i] ?: continue
              // rects are stored in pre-zoom coordinates
              val sx = w / 2f + (rc.left - w / 2f) * zoom
              val ex = w / 2f + (rc.right - w / 2f) * zoom
              val sy = groundY + (rc.top - groundY) * zoom
              val ey = groundY + (rc.bottom - groundY) * zoom
              if (p.x in sx..ex && p.y in (sy - 36f)..ey) {
                onDest(i)
                return@detectTapGestures
              }
            }
          }
        }
  }

  Canvas(modifier.fillMaxSize().then(gestureMod)) {
    @Suppress("UNUSED_VARIABLE") val tick = reactive.frame
    val w = size.width
    val h = size.height
    val t = reactive.time
    val scrollNow = scroll + (if (interactive) 0f else 0f) + t * 0.006f * k
    val groundY = h * (if (config.compact) 0.80f else 0.78f)
    val heightScale = if (config.compact) 0.62f else 1f

    // sky
    drawRect(Brush.verticalGradient(listOf(theme.skyTop, theme.skyMid, theme.skyBottom), startY = 0f, endY = groundY))
    drawSky(config.theme, w, h, groundY, t, accentA, accentB, reactive, scrollNow, lightMul)

    hitRects.fill(null)
    scale(zoom, zoom, pivot = Offset(w / 2f, groundY)) {
      for (layer in 0..1) drawLayer(layer, buildings, w, h, groundY, heightScale, scrollNow, t, reactive, accentA, accentB, theme, lightMul, k, hitRects, measurer, config.theme)
      drawTower(w, h, groundY, heightScale, t, reactive, accentA, accentB, lightMul, config.towerArt, painter, measurer)
      drawLayer(2, buildings, w, h, groundY, heightScale, scrollNow, t, reactive, accentA, accentB, theme, lightMul, k, hitRects, measurer, config.theme)
    }
    drawRoad(w, h, groundY, t, reactive, accentA, accentB, theme, lightMul, k, config.theme)
    drawParticles(config.theme, w, h, t, reactive, accentA, accentB, k, motion)
  }
}

private fun DrawScope.drawSky(
  themeIdx: Int, w: Float, h: Float, groundY: Float, t: Float, a: Color, b: Color, r: Reactive, scroll: Float, lightMul: Float,
) {
  when (themeIdx) {
    1 -> { // midnight: stars + moon
      for (i in 0 until 46) {
        val x = hash01(i, 1, 3) * w
        val y = hash01(i, 2, 3) * groundY * 0.7f
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (0.8f + hash01(i, 3, 3)) + i))
        drawCircle(Color.White.copy(alpha = 0.55f * tw), radius = 1.1f + hash01(i, 4, 3) * 1.4f, center = Offset(x, y))
      }
      val mx = w * 0.78f - (scroll * 40f) % (w * 0.2f)
      drawCircle(Brush.radialGradient(listOf(Color(0x66BFD6FF), Color.Transparent), center = Offset(mx, groundY * 0.22f), radius = w * 0.2f), radius = w * 0.2f, center = Offset(mx, groundY * 0.22f))
      drawCircle(Color(0xFFEAF1FF), radius = w * 0.045f, center = Offset(mx, groundY * 0.22f))
    }
    2 -> { /* neon rain: haze handled by glow below */
      drawRect(Brush.verticalGradient(listOf(Color.Transparent, b.copy(alpha = 0.18f * lightMul)), startY = groundY * 0.4f, endY = groundY), size = Size(w, groundY))
    }
    else -> { // cyber sun
      val cx = w * 0.5f
      val cy = groundY * 0.55f
      val rad = w * 0.26f
      drawCircle(Brush.verticalGradient(listOf(b.copy(alpha = 0.75f * lightMul), a.copy(alpha = 0.35f * lightMul)), startY = cy - rad, endY = cy + rad), radius = rad, center = Offset(cx, cy))
      // horizontal slits
      for (i in 0 until 6) {
        val y = cy + rad * (0.05f + i * 0.16f)
        drawRect(theme(themeIdx).skyBottom, topLeft = Offset(cx - rad, y), size = Size(rad * 2f, rad * (0.012f + i * 0.012f)))
      }
    }
  }
}

private fun theme(i: Int) = themeOf(i)

private fun DrawScope.drawLayer(
  layer: Int, all: List<Bld>, w: Float, h: Float, groundY: Float, hs: Float, scroll: Float, t: Float, r: Reactive,
  a: Color, b: Color, th: CityTheme, lightMul: Float, k: Float, hit: Array<Rect?>,
  measurer: androidx.compose.ui.text.TextMeasurer, themeIdx: Int,
) {
  val worldW = w * 1.8f
  val parallax = when (layer) { 0 -> 0.35f; 1 -> 0.7f; else -> 1.15f }
  val baseLift = when (layer) { 0 -> h * 0.085f; 1 -> h * 0.04f; else -> 0f }
  val band = when (layer) { 0 -> r.treble; 1 -> r.mid; else -> r.bass }
  val bodyTint = when (layer) { 0 -> 0.35f; 1 -> 0.2f; else -> 0.08f }
  for (b0 in all) {
    if (b0.layer != layer) continue
    val x0 = (((b0.x - scroll * parallax) % 1f) + 1f) % 1f * worldW - (worldW - w) / 2f
    val bw = b0.w * w
    if (x0 + bw < -w * 0.1f || x0 > w * 1.1f) continue
    val bh = b0.h * h * hs * (1f + (if (b0.special >= 0) r.bass * 0.05f * k else 0f))
    val baseY = groundY - baseLift
    val top = baseY - bh
    val body = mix(mix(th.skyBottom, Color.Black, 0.55f), a, bodyTint * 0.25f)
    drawRect(Brush.verticalGradient(listOf(mix(body, a, 0.10f), body), startY = top, endY = baseY), topLeft = Offset(x0, top), size = Size(bw, bh))
    drawRect(a.copy(alpha = 0.30f * lightMul), topLeft = Offset(x0, top), size = Size(max(1.2f, bw * 0.03f), bh))
    drawRect(a.copy(alpha = 0.55f * lightMul), topLeft = Offset(x0, top), size = Size(bw, max(1.2f, bw * 0.02f)))
    // windows
    val padX = bw * 0.12f
    val padY = bh * 0.06f
    val cw = (bw - padX * 2f) / b0.cols
    val rh = (bh - padY * 2f) / b0.rows
    val ww = cw * 0.55f
    val wh = rh * 0.5f
    val thresh = (0.20f * lightMul + band * 0.8f * k).coerceIn(0.06f, 0.95f)
    for (c in 0 until b0.cols) {
      for (rr in 0 until b0.rows) {
        val hv = hash01(b0.seed, c, rr)
        var lit = hv < thresh
        if (hv in 0.9f..0.96f) lit = sin(t * 2.5f + hv * 40f) > 0f
        if (!lit) continue
        val col = when ((hv * 10f).toInt() % 3) { 0 -> a; 1 -> b; else -> th.warm }
        drawRect(col.copy(alpha = (0.55f + 0.4f * band) * lightMul.coerceAtMost(1.2f)), topLeft = Offset(x0 + padX + c * cw + (cw - ww) / 2f, top + padY + rr * rh + (rh - wh) / 2f), size = Size(ww, wh))
      }
    }
    // rooftop light
    val beat = r.pulse
    drawCircle(
      (if (b0.seed % 2 == 0) a else b).copy(alpha = (0.5f + 0.5f * sin(t * 2f + b0.seed)).coerceIn(0.2f, 1f) * (0.5f + beat) * 0.9f * lightMul.coerceAtMost(1.2f)),
      radius = 2.2f + beat * 2.5f * k, center = Offset(x0 + bw / 2f, top - 2f),
    )
    if (b0.special >= 0) {
      val signW = bw * 0.9f
      val signH = bw * 0.28f
      val sy = top + bh * 0.08f
      val sc = if (b0.special % 2 == 0) a else b
      drawRoundRect(sc.copy(alpha = 0.22f + 0.2f * r.pulse), topLeft = Offset(x0 + (bw - signW) / 2f, sy), size = Size(signW, signH), cornerRadius = CornerRadius(4f, 4f))
      drawRoundRect(sc.copy(alpha = 0.9f), topLeft = Offset(x0 + (bw - signW) / 2f, sy), size = Size(signW, signH), cornerRadius = CornerRadius(4f, 4f), style = Stroke(1.6f))
      val label = CityDestinations.names[b0.special]
      val tl = measurer.measure(label, TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold))
      drawText(tl, topLeft = Offset(x0 + (bw - tl.size.width) / 2f, sy + (signH - tl.size.height) / 2f))
      hit[b0.special] = Rect(x0, top, x0 + bw, baseY)
    }
  }
}

private fun DrawScope.drawTower(
  w: Float, h: Float, groundY: Float, hs: Float, t: Float, r: Reactive, a: Color, b: Color, lightMul: Float,
  showArt: Boolean, painter: androidx.compose.ui.graphics.painter.Painter, measurer: androidx.compose.ui.text.TextMeasurer,
) {
  val cx = w / 2f
  val tw = w * 0.17f
  val th = h * 0.46f * hs
  val top = groundY - th
  // glow
  drawCircle(Brush.radialGradient(listOf(a.copy(alpha = (0.30f + 0.4f * r.level + 0.3f * r.pulse) * lightMul.coerceAtMost(1.2f)), Color.Transparent), center = Offset(cx, top + th * 0.45f), radius = th * 0.85f), radius = th * 0.85f, center = Offset(cx, top + th * 0.45f))
  val body = Path().apply {
    moveTo(cx - tw * 0.5f, groundY)
    lineTo(cx - tw * 0.36f, top + th * 0.12f)
    lineTo(cx - tw * 0.2f, top + th * 0.12f)
    lineTo(cx - tw * 0.08f, top)
    lineTo(cx + tw * 0.08f, top)
    lineTo(cx + tw * 0.2f, top + th * 0.12f)
    lineTo(cx + tw * 0.36f, top + th * 0.12f)
    lineTo(cx + tw * 0.5f, groundY)
    close()
  }
  drawPath(body, Brush.verticalGradient(listOf(Color(0xFF1B1730), Color(0xFF0A0814)), startY = top, endY = groundY))
  drawPath(body, a.copy(alpha = 0.85f), style = Stroke(2f))
  // glass rings pulsing with the beat
  for (i in 0 until 5) {
    val y = top + th * (0.2f + i * 0.16f)
    val half = tw * (0.24f + i * 0.045f)
    drawLine(b.copy(alpha = 0.45f + 0.5f * r.pulse), Offset(cx - half, y), Offset(cx + half, y), strokeWidth = 2f)
  }
  // artwork / Pattukal mark panel
  val s = tw * 0.62f
  val px = cx - s / 2f
  val py = top + th * 0.30f
  val panel = Path().apply { addRoundRect(RoundRect(px, py, px + s, py + s, CornerRadius(s * 0.14f, s * 0.14f))) }
  drawPath(panel, Color(0xFF05040C))
  drawPattukalMark(cx, py + s / 2f, s * 0.42f, a)
  if (showArt) {
    clipPath(panel) {
      translate(px, py) { with(painter) { draw(size = Size(s, s), alpha = 0.95f) } }
    }
  }
  drawPath(panel, Color.White.copy(alpha = 0.55f), style = Stroke(1.6f))
  // beacon
  val beacon = 0.5f + 0.5f * sin(t * 3f)
  drawCircle(Color(0xFFFF4D4D).copy(alpha = 0.4f + 0.6f * max(beacon, r.pulse)), radius = 3.5f + r.pulse * 6f, center = Offset(cx, top - 3f))
  drawCircle(Color(0xFFFF4D4D).copy(alpha = 0.18f + 0.25f * r.pulse), radius = 12f + r.pulse * 22f, center = Offset(cx, top - 3f))
}

/** The Pattukal circle-and-note mark, drawn in vectors. */
private fun DrawScope.drawPattukalMark(cx: Float, cy: Float, r: Float, c: Color) {
  drawCircle(c.copy(alpha = 0.95f), radius = r, center = Offset(cx, cy), style = Stroke(r * 0.16f))
  drawCircle(c.copy(alpha = 0.8f), radius = r * 0.66f, center = Offset(cx, cy), style = Stroke(r * 0.10f))
  drawOval(Color.White, topLeft = Offset(cx - r * 0.34f, cy + r * 0.02f), size = Size(r * 0.34f, r * 0.26f))
  drawRect(Color.White, topLeft = Offset(cx - r * 0.04f, cy - r * 0.34f), size = Size(r * 0.07f, r * 0.46f))
  drawLine(Color.White, Offset(cx - r * 0.01f, cy - r * 0.34f), Offset(cx + r * 0.22f, cy - r * 0.18f), strokeWidth = r * 0.08f)
}

private fun DrawScope.drawRoad(
  w: Float, h: Float, groundY: Float, t: Float, r: Reactive, a: Color, b: Color, th: CityTheme, lightMul: Float, k: Float, themeIdx: Int,
) {
  drawRect(Brush.verticalGradient(listOf(Color(0xFF07060F), Color(0xFF020208)), startY = groundY, endY = h), topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
  drawLine(a.copy(alpha = 0.8f * lightMul.coerceAtMost(1.2f)), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 2f)
  val cx = w / 2f
  for (lane in -3..3) {
    val bx = cx + lane * w * 0.42f
    drawLine((if (lane % 2 == 0) a else b).copy(alpha = 0.28f * lightMul.coerceAtMost(1.2f)), Offset(cx, groundY), Offset(bx, h), strokeWidth = 1.4f)
  }
  val speed = 0.18f + r.level * 0.5f * k
  for (lane in listOf(-1.5f, -0.5f, 0.5f, 1.5f)) {
    val bx = cx + lane * w * 0.42f
    for (d in 0 until 8) {
      val f = ((d / 8f + t * speed) % 1f)
      val f2 = f * f
      val f3 = min(1f, (f + 0.06f)).let { it * it }
      val x1 = cx + (bx - cx) * f2
      val y1 = groundY + (h - groundY) * f2
      val x2 = cx + (bx - cx) * f3
      val y2 = groundY + (h - groundY) * f3
      drawLine((if (lane < 0) a else b).copy(alpha = (0.25f + 0.75f * f) * (0.5f + r.level) * lightMul.coerceAtMost(1.2f)), Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.2f + 4f * f)
    }
  }
  // cars: tiny headlights/taillights
  for (c in 0 until 6) {
    val dir = if (c % 2 == 0) 1f else -1f
    val f = ((hash01(c, 7, 7) + t * (0.08f + hash01(c, 8, 7) * 0.1f) * (0.6f + r.level * k)) % 1f).let { if (dir > 0) it else 1f - it }
    val f2 = f * f
    val lane = if (c % 2 == 0) 0.8f else -0.8f
    val x = cx + (cx + lane * w * 0.42f - cx) * f2
    val y = groundY + (h - groundY) * f2 + 2f
    val col = if (dir > 0) Color(0xFFFFFFFF) else Color(0xFFFF3B3B)
    drawCircle(col.copy(alpha = 0.9f), radius = 1.5f + 3.2f * f, center = Offset(x, y))
    drawCircle(col.copy(alpha = 0.22f), radius = 5f + 11f * f, center = Offset(x, y))
  }
  if (themeIdx == 2) { // wet road reflection hint
    drawRect(Brush.verticalGradient(listOf(b.copy(alpha = 0.16f * lightMul), Color.Transparent), startY = groundY, endY = groundY + (h - groundY) * 0.5f), topLeft = Offset(0f, groundY), size = Size(w, (h - groundY) * 0.5f))
  }
}

private fun DrawScope.drawParticles(
  themeIdx: Int, w: Float, h: Float, t: Float, r: Reactive, a: Color, b: Color, k: Float, motion: MotionSettings,
) {
  if (themeIdx == 2) {
    if (motion.reduced) return
    for (i in 0 until 70) {
      val sp = 0.5f + hash01(i, 1, 9) * 0.7f
      val y = ((hash01(i, 2, 9) + t * 0.55f * sp) % 1f) * h
      val x = ((hash01(i, 3, 9) * w) - y * 0.12f + w) % w
      drawLine((if (i % 3 == 0) b else a).copy(alpha = 0.30f), Offset(x, y), Offset(x - 5f, y + 20f * sp), strokeWidth = 1.2f)
    }
  } else {
    for (i in 0 until 34) {
      val sp = 0.02f + hash01(i, 1, 5) * 0.04f
      val y = h * (1f - ((hash01(i, 2, 5) + t * sp * (1f + r.level * k)) % 1f))
      val x = hash01(i, 3, 5) * w + sin(t * 0.7f + i) * 14f
      drawCircle((if (i % 2 == 0) a else b).copy(alpha = 0.22f + 0.3f * r.treble), radius = 1.2f + hash01(i, 4, 5) * 1.8f, center = Offset(x, y))
    }
  }
}
