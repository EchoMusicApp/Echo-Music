package echo.music.iad1tya.ui.immersive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import echo.music.iad1tya.constants.LiquidBackgroundKey
import echo.music.iad1tya.constants.LiquidColorModeKey
import echo.music.iad1tya.constants.LiquidCustomColorKey
import echo.music.iad1tya.constants.LiquidIntensityKey
import echo.music.iad1tya.constants.LiquidLayoutKey
import echo.music.iad1tya.constants.LiquidMaterialKey
import echo.music.iad1tya.constants.LiquidQualityKey
import echo.music.iad1tya.constants.LiquidStyleKey
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object LiquidOptions {
  val styles = listOf("Liquid Orb", "Chrome Waves", "Liquid Glass")
  val materials = listOf("Chrome", "Liquid Glass", "Holographic", "Matte")
  val colorModes = listOf("Album Colors", "Monochrome", "Custom")
  val intensities = listOf("Low", "Medium", "High")
  val backgrounds = listOf("Black", "Album Blur", "Gradient")
  val qualities = listOf("Low", "Medium", "High")
  val layouts = listOf("Full screen", "Compact")
  val customSwatches =
    listOf(0xFF7A5CFF, 0xFF00E5FF, 0xFFFF4D8D, 0xFFFFB300, 0xFF4DFF88, 0xFFFF5A12).map { it.toInt() }
}

class LiquidConfig(
  val style: Int,
  val material: Int,
  val colorMode: Int,
  val customColor: Color,
  val intensity: Int,
  val background: Int,
  val quality: Int,
  val compact: Boolean,
)

@Composable
fun rememberLiquidConfig(forceCompact: Boolean? = null): LiquidConfig {
  val (style, _) = rememberPreference(LiquidStyleKey, 0)
  val (material, _) = rememberPreference(LiquidMaterialKey, 0)
  val (colorMode, _) = rememberPreference(LiquidColorModeKey, 0)
  val (custom, _) = rememberPreference(LiquidCustomColorKey, LiquidOptions.customSwatches[0])
  val (intensity, _) = rememberPreference(LiquidIntensityKey, 1)
  val (bg, _) = rememberPreference(LiquidBackgroundKey, 0)
  val (quality, _) = rememberPreference(LiquidQualityKey, 1)
  val (layout, _) = rememberPreference(LiquidLayoutKey, 0)
  return LiquidConfig(style, material, colorMode, Color(custom), intensity, bg, quality, forceCompact ?: (layout == 1))
}

private fun lighten(c: Color, f: Float) = Color(c.red + (1f - c.red) * f, c.green + (1f - c.green) * f, c.blue + (1f - c.blue) * f, c.alpha)
private fun darken(c: Color, f: Float) = Color(c.red * (1f - f), c.green * (1f - f), c.blue * (1f - f), c.alpha)

private fun blobPath(
  path: Path, cx: Float, cy: Float, radius: Float, n: Int, t: Float, amp: Float, seed: Float,
  bass: Float, mid: Float, treble: Float,
) {
  path.rewind()
  for (i in 0..n) {
    val a = (i % n).toFloat() / n * 2f * PI.toFloat()
    val noise =
      0.5f * sin(2f * a + t * 0.8f + seed) + 0.3f * sin(3f * a - t * 1.1f + seed * 1.7f) +
        0.2f * sin(5f * a + t * 1.6f + seed * 0.6f)
    val audio = bass * 0.22f * sin(2f * a + t * 1.4f) + mid * 0.14f * sin(4f * a - t * 1.9f) + treble * 0.08f * sin(7f * a + t * 3f)
    val r = radius * (1f + amp * noise + audio)
    val x = cx + cos(a) * r
    val y = cy + sin(a) * r
    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
  }
  path.close()
}

/** Premium liquid-metal visualiser. Pure Canvas — works on every Android version. */
@Composable
fun LiquidSoundBackground(
  config: LiquidConfig,
  reactive: Reactive,
  palette: AlbumPalette,
  thumbnailUrl: String?,
  modifier: Modifier = Modifier,
) {
  val path = remember { Path() }
  val path2 = remember { Path() }
  Box(modifier.fillMaxSize()) {
    // background
    when (config.background) {
      1 -> {
        Box(Modifier.fillMaxSize().background(Color.Black))
        if (thumbnailUrl != null) {
          AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(60.dp),
          )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))
      }
      2 ->
        Box(
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(darken(palette.a, 0.55f), darken(palette.c, 0.75f), Color.Black)
              )
            )
        )
      else -> Box(Modifier.fillMaxSize().background(Color.Black))
    }
    Canvas(Modifier.fillMaxSize()) {
      @Suppress("UNUSED_VARIABLE") val tick = reactive.frame // redraw every frame
      val colors = pickColors(config, palette)
      val k = when (config.intensity) { 0 -> 0.6f; 2 -> 1.5f; else -> 1f }
      when (config.style) {
        1 -> drawWaves(config, reactive, colors, k, path, path2)
        2 -> drawGlassForms(config, reactive, colors, k, path)
        else -> drawOrb(config, reactive, colors, k, path)
      }
    }
  }
}

private fun pickColors(config: LiquidConfig, p: AlbumPalette): Triple<Color, Color, Color> =
  when (config.colorMode) {
    1 -> Triple(Color(0xFFE8E8EE), Color(0xFF9096A3), Color(0xFF3A3D46))
    2 -> Triple(config.customColor, lighten(config.customColor, 0.45f), darken(config.customColor, 0.55f))
    else -> Triple(p.a, p.b, p.c)
  }

private fun DrawScope.shadeBlob(
  config: LiquidConfig, path: Path, cx: Float, cy: Float, r: Float, t: Float,
  c: Triple<Color, Color, Color>, alpha: Float = 1f,
) {
  val (c1, c2, c3) = c
  when (config.material) {
    1 -> { // liquid glass
      clipPath(path) {
        drawRect(
          Brush.radialGradient(
            listOf(c2.copy(alpha = 0.10f * alpha), c1.copy(alpha = 0.30f * alpha), c3.copy(alpha = 0.38f * alpha)),
            center = Offset(cx - r * 0.2f, cy - r * 0.25f), radius = r * 1.3f,
          )
        )
        drawCircle(
          Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f * alpha), Color.Transparent), center = Offset(cx - r * 0.38f, cy - r * 0.45f), radius = r * 0.45f),
          radius = r * 0.5f, center = Offset(cx - r * 0.38f, cy - r * 0.45f),
        )
      }
      drawPath(path, Brush.sweepGradient(listOf(Color.White.copy(alpha = 0.85f * alpha), c2.copy(alpha = 0.2f), Color.White.copy(alpha = 0.6f * alpha), c1.copy(alpha = 0.15f), Color.White.copy(alpha = 0.85f * alpha)), center = Offset(cx, cy)), style = Stroke(width = 2.2f * density))
    }
    2 -> { // holographic
      clipPath(path) {
        rotate(t * 24f, Offset(cx, cy)) {
          drawRect(
            Brush.sweepGradient(
              listOf(c1, Color(0xFF00E5FF), c2, Color(0xFFFF4DDE), c3, Color(0xFFFFE86B), c1),
              center = Offset(cx, cy),
            ),
            alpha = alpha,
          )
        }
        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.45f)), startY = cy - r, endY = cy + r))
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), center = Offset(cx - r * 0.35f, cy - r * 0.42f), radius = r * 0.4f), radius = r * 0.45f, center = Offset(cx - r * 0.35f, cy - r * 0.42f))
      }
      drawPath(path, Color.White.copy(alpha = 0.5f * alpha), style = Stroke(width = 1.5f * density))
    }
    3 -> { // matte
      clipPath(path) {
        drawRect(
          Brush.radialGradient(
            listOf(lighten(c1, 0.25f).copy(alpha = alpha), c1.copy(alpha = alpha), darken(c3, 0.3f).copy(alpha = alpha)),
            center = Offset(cx - r * 0.3f, cy - r * 0.35f), radius = r * 1.5f,
          )
        )
      }
    }
    else -> { // chrome
      clipPath(path) {
        drawRect(
          Brush.radialGradient(
            listOf(lighten(c1, 0.8f), lighten(c1, 0.15f), darken(c3, 0.35f), Color(0xFF05050A)),
            center = Offset(cx - r * 0.35f, cy - r * 0.45f), radius = r * 1.7f,
          ),
          alpha = alpha,
        )
        // horizon reflection band that drifts slowly
        val hy = cy + sin(t * 0.7f) * r * 0.12f
        drawRect(
          Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.62f * alpha), lighten(c2, 0.4f).copy(alpha = 0.35f * alpha), Color.Transparent),
            startY = hy - r * 0.05f, endY = hy + r * 0.45f,
          ),
          topLeft = Offset(cx - r * 1.2f, hy - r * 0.05f), size = Size(r * 2.4f, r * 0.5f),
        )
        drawRect(
          Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f * alpha), Color.Transparent), startY = hy + r * 0.42f, endY = hy + r * 0.95f),
          topLeft = Offset(cx - r * 1.2f, hy + r * 0.42f), size = Size(r * 2.4f, r * 0.55f),
        )
        rotate(-18f + sin(t * 0.5f) * 12f, Offset(cx, cy)) {
          drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.28f * alpha), Color.Transparent), startX = cx - r * 0.7f, endX = cx - r * 0.1f),
            topLeft = Offset(cx - r * 0.7f, cy - r * 1.3f), size = Size(r * 0.6f, r * 2.6f),
          )
        }
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f * alpha), Color.Transparent), center = Offset(cx - r * 0.38f, cy - r * 0.45f), radius = r * 0.36f), radius = r * 0.4f, center = Offset(cx - r * 0.38f, cy - r * 0.45f))
      }
      drawPath(path, Color.White.copy(alpha = 0.28f * alpha), style = Stroke(width = 1.2f * density))
    }
  }
}

private fun DrawScope.drawOrb(config: LiquidConfig, r: Reactive, c: Triple<Color, Color, Color>, k: Float, path: Path) {
  val w = size.width
  val h = size.height
  val cx = w / 2f
  val cy = if (config.compact) h * 0.30f else h * 0.40f
  val base = min(w, h) * (if (config.compact) 0.20f else 0.30f)
  val t = r.time
  val pts = when (config.quality) { 0 -> 48; 2 -> 128; else -> 84 }
  val radius = base * (1f + r.level * 0.10f * k + r.pulse * 0.07f * k)
  // glow
  drawCircle(
    Brush.radialGradient(listOf(c.first.copy(alpha = 0.34f * (0.5f + r.level)), Color.Transparent), center = Offset(cx, cy), radius = radius * 2.1f),
    radius = radius * 2.1f, center = Offset(cx, cy),
  )
  // floor reflection
  drawOval(
    Brush.radialGradient(listOf(c.second.copy(alpha = 0.22f), Color.Transparent), center = Offset(cx, cy + radius * 1.35f), radius = radius * 1.1f),
    topLeft = Offset(cx - radius * 1.1f, cy + radius * 1.25f), size = Size(radius * 2.2f, radius * 0.3f),
  )
  blobPath(path, cx, cy, radius, pts, t, 0.07f * k, 0f, r.bass * k, r.mid * k, r.treble * k)
  shadeBlob(config, path, cx, cy, radius, t, c)
  // satellite droplets driven by treble
  val sats = if (config.quality == 0) 2 else 4
  for (i in 0 until sats) {
    val a = t * (0.5f + i * 0.17f) + i * 1.7f
    val d = radius * (1.45f + 0.12f * sin(t + i) + r.treble * 0.25f * k)
    val sx = cx + cos(a) * d
    val sy = cy + sin(a) * d * 0.55f
    val sr = radius * (0.07f + 0.03f * (i % 2) + r.mid * 0.04f * k)
    blobPath(path, sx, sy, sr, 24, t * 1.5f, 0.12f, i * 2f, 0f, 0f, 0f)
    shadeBlob(config, path, sx, sy, sr, t, c, 0.95f)
  }
}

private fun DrawScope.drawWaves(config: LiquidConfig, r: Reactive, c: Triple<Color, Color, Color>, k: Float, p: Path, p2: Path) {
  val w = size.width
  val h = size.height
  val count = when (config.quality) { 0 -> 3; 2 -> 6; else -> 4 }
  val seg = when (config.quality) { 0 -> 36; 2 -> 110; else -> 70 }
  val t = r.time
  val area = if (config.compact) h * 0.34f else h * 0.62f
  val top = if (config.compact) h * 0.10f else h * 0.12f
  for (i in 0 until count) {
    val f = i / max(1, count - 1).toFloat()
    val baseY = top + area * (0.18f + 0.64f * f)
    val thick = area * (0.10f + 0.05f * (1f - f)) * (1f + r.level * 0.4f * k)
    val ampl = area * (0.07f + 0.05f * f) * (0.6f + (r.bass * 1.2f + r.mid * 0.6f) * k) + r.pulse * area * 0.03f * k
    p.rewind()
    for (s in 0..seg) {
      val x = w * s / seg
      val u = s.toFloat() / seg
      val phase = t * (0.9f + f * 0.5f) + i * 1.3f
      val y = baseY + sin(u * 2f * PI.toFloat() * (1.1f + f * 0.4f) + phase) * ampl +
        sin(u * 2f * PI.toFloat() * 2.6f - phase * 1.4f) * ampl * 0.35f * (0.4f + r.treble * 1.5f * k)
      if (s == 0) { p.moveTo(x, y) } else { p.lineTo(x, y) }
    }
    // build closed ribbon: top edge forward + bottom edge backward
    val ribbon = Path()
    ribbon.addPath(p)
    for (s in seg downTo 0) {
      val x = w * s / seg
      val u = s.toFloat() / seg
      val phase = t * (0.9f + f * 0.5f) + i * 1.3f
      val y = baseY + sin(u * 2f * PI.toFloat() * (1.1f + f * 0.4f) + phase) * ampl +
        sin(u * 2f * PI.toFloat() * 2.6f - phase * 1.4f) * ampl * 0.35f * (0.4f + r.treble * 1.5f * k)
      val th = thick * (0.7f + 0.3f * sin(u * 6f + phase))
      ribbon.lineTo(x, y + th)
    }
    ribbon.close()
    val (c1, c2, c3) = c
    val tint = if (i % 2 == 0) c1 else c2
    val brush =
      when (config.material) {
        1 -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), tint.copy(alpha = 0.28f), c3.copy(alpha = 0.22f)), startY = baseY - ampl, endY = baseY + thick + ampl)
        2 -> Brush.horizontalGradient(listOf(c1, Color(0xFF00E5FF), c2, Color(0xFFFF4DDE), c3).map { it.copy(alpha = 0.9f) })
        3 -> Brush.verticalGradient(listOf(lighten(tint, 0.2f), tint, darken(c3, 0.3f)), startY = baseY - ampl, endY = baseY + thick + ampl)
        else -> Brush.verticalGradient(listOf(lighten(tint, 0.85f), lighten(tint, 0.2f), Color(0xFF0B0B12), lighten(c2, 0.55f), darken(c3, 0.4f)), startY = baseY - ampl, endY = baseY + thick + ampl)
      }
    drawPath(ribbon, brush, alpha = 0.92f)
    drawPath(p, Color.White.copy(alpha = if (config.material == 3) 0.12f else 0.55f), style = Stroke(width = 1.4f * density))
  }
}

private fun DrawScope.drawGlassForms(config: LiquidConfig, r: Reactive, c: Triple<Color, Color, Color>, k: Float, path: Path) {
  val w = size.width
  val h = size.height
  val t = r.time
  val cx0 = w / 2f
  val cy0 = if (config.compact) h * 0.30f else h * 0.40f
  val base = min(w, h) * (if (config.compact) 0.17f else 0.24f)
  val pts = when (config.quality) { 0 -> 40; 2 -> 110; else -> 72 }
  val forms = listOf(Triple(c.first, -0.55f, -0.2f), Triple(c.second, 0.55f, -0.05f), Triple(c.third, 0f, 0.5f), Triple(lighten(c.first, 0.5f), 0.1f, -0.55f))
  val take = if (config.quality == 0) 3 else 4
  for (i in 0 until take) {
    val (col, ox, oy) = forms[i]
    val wob = 0.12f * sin(t * (0.6f + i * 0.2f) + i)
    val cx = cx0 + (ox + wob) * base * (1.1f + r.bass * 0.5f * k)
    val cy = cy0 + (oy + wob) * base * (1.1f + r.mid * 0.5f * k)
    val rad = base * (0.78f + 0.12f * i / 3f + r.level * 0.12f * k + r.pulse * 0.06f * k)
    blobPath(path, cx, cy, rad, pts, t + i, 0.10f * k, i * 1.9f, r.bass * k, r.mid * k, r.treble * k)
    val cfg = LiquidConfig(1, if (config.material == 0) 1 else config.material, config.colorMode, config.customColor, config.intensity, config.background, config.quality, config.compact)
    shadeBlob(cfg, path, cx, cy, rad, t, Triple(col, lighten(col, 0.4f), darken(col, 0.4f)), 0.9f)
  }
}

/** Small honest tag: tells the user whether the motion follows the real audio or is just decorative. */
fun reactiveLabel(live: Boolean): String = if (live) "Live audio" else "Ambient motion"
