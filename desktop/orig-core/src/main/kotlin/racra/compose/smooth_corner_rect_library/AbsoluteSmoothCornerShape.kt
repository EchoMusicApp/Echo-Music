package racra.compose.smooth_corner_rect_library

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import kotlin.math.min

// Desktop replacement for the Android-only "smooth corner rect" library: a rectangle whose corners ease in gradually
// (squircle-like) instead of a plain circular arc. Same constructor shapes the app uses.
// shortcut: corners are one cubic curve each, stretched by the smoothness percent, so the curve is close to but not
// identical to the original library's; tune the 0.4477 control factor if a corner looks off next to the Android app.
class AbsoluteSmoothCornerShape(
    private val cornerRadiusTL: Dp = 0.dp,
    private val cornerRadiusTR: Dp = 0.dp,
    private val cornerRadiusBL: Dp = 0.dp,
    private val cornerRadiusBR: Dp = 0.dp,
    private val smoothnessAsPercentTL: Int = 60,
    private val smoothnessAsPercentTR: Int = 60,
    private val smoothnessAsPercentBL: Int = 60,
    private val smoothnessAsPercentBR: Int = 60,
) : Shape {
    constructor(cornerRadius: Dp, smoothnessAsPercent: Int) : this(
        cornerRadius, cornerRadius, cornerRadius, cornerRadius,
        smoothnessAsPercent, smoothnessAsPercent, smoothnessAsPercent, smoothnessAsPercent,
    )

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val limit = min(w, h) / 2f
        // distance along each edge where the curve starts: the radius stretched by the smoothness, never past half the short side
        fun reach(r: Dp, s: Int) = min(with(density) { r.toPx() } * (1f + s.coerceIn(0, 100) / 100f), limit)
        val tl = reach(cornerRadiusTL, smoothnessAsPercentTL)
        val tr = reach(cornerRadiusTR, smoothnessAsPercentTR)
        val bl = reach(cornerRadiusBL, smoothnessAsPercentBL)
        val br = reach(cornerRadiusBR, smoothnessAsPercentBR)
        val k = 0.4477f // distance of the control points from the corner, as a share of the reach (0.4477 = circle)
        val path = Path().apply {
            moveTo(tl, 0f)
            lineTo(w - tr, 0f)
            cubicTo(w - tr * k, 0f, w, tr * k, w, tr)
            lineTo(w, h - br)
            cubicTo(w, h - br * k, w - br * k, h, w - br, h)
            lineTo(bl, h)
            cubicTo(bl * k, h, 0f, h - bl * k, 0f, h - bl)
            lineTo(0f, tl)
            cubicTo(0f, tl * k, tl * k, 0f, tl, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
