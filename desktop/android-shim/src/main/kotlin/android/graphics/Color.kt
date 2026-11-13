package android.graphics

import java.awt.Color as AwtColor

object Color {
    @JvmStatic fun colorToHSV(color: Int, hsv: FloatArray) {
        val r = AwtColor.RGBtoHSB((color shr 16) and 255, (color shr 8) and 255, color and 255, null)
        hsv[0] = r[0] * 360f; hsv[1] = r[1]; hsv[2] = r[2]
    }
    @JvmStatic fun HSVToColor(hsv: FloatArray): Int = AwtColor.HSBtoRGB(hsv[0] / 360f, hsv[1], hsv[2]) or (0xFF shl 24)
    @JvmStatic fun HSVToColor(alpha: Int, hsv: FloatArray): Int = (HSVToColor(hsv) and 0xFFFFFF) or (alpha shl 24)
}
