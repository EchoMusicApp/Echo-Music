package echo.music.iad1tya.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val echoIcons.AccountCircle: ImageVector
    get() {
        if (_accountCircle != null) {
            return _accountCircle!!
        }
        _accountCircle =
            ImageVector.Builder(
                name = "AccountCircle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12f, 2f)
                    curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                    curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
                    curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
                    curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                    close()
                    moveTo(12f, 6f)
                    curveToRelative(1.93f, 0f, 3.5f, 1.57f, 3.5f, 3.5f)
                    curveToRelative(0f, 1.93f, -1.57f, 3.5f, -3.5f, 3.5f)
                    curveToRelative(-1.93f, 0f, -3.5f, -1.57f, -3.5f, -3.5f)
                    curveTo(8.5f, 7.57f, 10.07f, 6f, 12f, 6f)
                    close()
                    moveTo(12f, 20f)
                    curveToRelative(-2.5f, 0f, -4.71f, -1.28f, -6f, -3.22f)
                    curveToRelative(0.03f, -1.99f, 4f, -3.08f, 6f, -3.08f)
                    curveToRelative(1.99f, 0f, 5.97f, 1.09f, 6f, 3.08f)
                    curveTo(16.71f, 18.72f, 14.5f, 20f, 12f, 20f)
                    close()
                }
            }.build()
        return _accountCircle!!
    }

private var _accountCircle: ImageVector? = null
