package androidx.core.graphics

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ColorUtils {
    @JvmStatic fun colorToHSL(color: Int, hsl: FloatArray) {
        val r = ((color shr 16) and 255) / 255f; val g = ((color shr 8) and 255) / 255f; val b = (color and 255) / 255f
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn; val l = (mx + mn) / 2
        if (d == 0f) { hsl[0] = 0f; hsl[1] = 0f } else {
            hsl[1] = d / (1 - abs(2 * l - 1))
            val h = when (mx) { r -> ((g - b) / d) % 6; g -> (b - r) / d + 2; else -> (r - g) / d + 4 } * 60
            hsl[0] = (h + 360) % 360
        }
        hsl[2] = l
    }
    @JvmStatic fun HSLToColor(hsl: FloatArray): Int {
        val h = hsl[0]; val s = hsl[1]; val l = hsl[2]
        val c = (1 - abs(2 * l - 1)) * s; val m = l - c / 2
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val (r, g, b) = when ((h / 60).toInt() % 6) { 0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x); 3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x) }
        fun ch(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
