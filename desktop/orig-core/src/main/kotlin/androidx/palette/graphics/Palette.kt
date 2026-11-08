package androidx.palette.graphics

import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Desktop look-alike of androidx.palette.Palette: counts pixel colours (4 bits per channel) and picks the same seven
 * named swatches by closeness to Android's target saturation/lightness.
 * shortcut: histogram instead of Android's median-cut, so picked colours can differ slightly; swap in a port of the real algorithm if the gradients look off.
 */
class Palette private constructor(val swatches: List<Swatch>) {
    class Swatch(val rgb: Int, val population: Int) {
        val hsl: FloatArray by lazy { toHsl(rgb) }
        val titleTextColor: Int get() = if (hsl[2] > 0.5f) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        val bodyTextColor: Int get() = titleTextColor
    }

    val dominantSwatch: Swatch? = swatches.maxByOrNull { it.population }
    val vibrantSwatch = pick(0.5f, 1f, 0.35f)
    val lightVibrantSwatch = pick(0.74f, 1f, 0.35f)
    val darkVibrantSwatch = pick(0.26f, 1f, 0.35f)
    val mutedSwatch = pick(0.5f, 0.3f, 0f, maxSat = 0.4f)
    val lightMutedSwatch = pick(0.74f, 0.3f, 0f, maxSat = 0.4f)
    val darkMutedSwatch = pick(0.26f, 0.3f, 0f, maxSat = 0.4f)

    fun getDominantColor(default: Int) = dominantSwatch?.rgb ?: default
    fun getVibrantColor(default: Int) = vibrantSwatch?.rgb ?: default
    fun getMutedColor(default: Int) = mutedSwatch?.rgb ?: default
    fun getLightVibrantColor(default: Int) = lightVibrantSwatch?.rgb ?: default
    fun getDarkVibrantColor(default: Int) = darkVibrantSwatch?.rgb ?: default
    fun getLightMutedColor(default: Int) = lightMutedSwatch?.rgb ?: default
    fun getDarkMutedColor(default: Int) = darkMutedSwatch?.rgb ?: default

    private fun pick(targetL: Float, targetS: Float, minSat: Float, maxSat: Float = 1f): Swatch? {
        val total = max(1, swatches.sumOf { it.population }).toFloat()
        return swatches.filter { it.hsl[1] in minSat..maxSat && abs(it.hsl[2] - targetL) < 0.26f }
            .maxByOrNull { 3f * (1 - abs(it.hsl[1] - targetS)) + 6f * (1 - abs(it.hsl[2] - targetL)) + 1f * it.population / total }
    }

    class Builder(private val bitmap: Bitmap) {
        private var maxColors = 16
        private var maxArea = 112 * 112
        fun maximumColorCount(n: Int) = apply { maxColors = n }
        fun resizeBitmapArea(area: Int) = apply { maxArea = area }
        fun generate(): Palette {
            val w = bitmap.width; val h = bitmap.height
            if (w == 0 || h == 0) return Palette(emptyList())
            val px = IntArray(w * h)
            bitmap.asComposeImageBitmap().readPixels(px)
            val step = max(1, kotlin.math.sqrt((w * h).toDouble() / maxArea).toInt())
            val counts = HashMap<Int, IntArray>() // bucket -> [population, r, g, b sums]
            for (y in 0 until h step step) for (x in 0 until w step step) {
                val c = px[y * w + x]
                if ((c ushr 24) < 128) continue
                val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
                val e = counts.getOrPut(((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)) { IntArray(4) }
                e[0]++; e[1] += r; e[2] += g; e[3] += b
            }
            val list = counts.values.sortedByDescending { it[0] }.take(maxColors).map {
                Swatch((0xFF shl 24) or ((it[1] / it[0]) shl 16) or ((it[2] / it[0]) shl 8) or (it[3] / it[0]), it[0])
            }
            return Palette(list)
        }
    }

    companion object {
        @JvmStatic fun from(bitmap: Bitmap) = Builder(bitmap)

        private fun toHsl(rgb: Int): FloatArray {
            val r = ((rgb shr 16) and 255) / 255f; val g = ((rgb shr 8) and 255) / 255f; val b = (rgb and 255) / 255f
            val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn
            val l = (mx + mn) / 2
            if (d == 0f) return floatArrayOf(0f, 0f, l)
            val s = d / (1 - abs(2 * l - 1))
            val h = when (mx) { r -> ((g - b) / d) % 6; g -> (b - r) / d + 2; else -> (r - g) / d + 4 } * 60
            return floatArrayOf((h + 360) % 360, s, l)
        }
    }
}
