package iad1tya.echo.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun AnimatedGradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (colors.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "gradientAnimation")

    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Ensure we always have enough colors by repeating or fallback
        val safeColors = if (colors.size < 2) {
            listOf(colors.firstOrNull() ?: Color.Gray, Color.Black)
        } else {
            colors
        }
        
        // 1. Base wash (e.g. bottom-right)
        val c1 = safeColors.getOrElse(0) { Color.Gray }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(c1.copy(alpha = 0.8f), Color.Transparent),
                center = Offset(width * 0.8f, height * 0.8f),
                radius = width * 1.2f
            )
        )

        // 2. Animated top-left blob
        val c2 = safeColors.getOrElse(1) { c1 }
        val offset1 = calculateAnimatedOffset(
            center = Offset(width * 0.2f, height * 0.2f),
            radiusX = width * 0.15f,
            radiusY = height * 0.1f,
            time = t,
            phase = 0f
        )
        drawRadialBlob(c2, offset1, width * 0.9f)
        
        // 3. Animated bottom-left blob
        val c3 = safeColors.getOrElse(2) { c1 }
        val offset2 = calculateAnimatedOffset(
            center = Offset(width * 0.2f, height * 0.8f),
            radiusX = width * 0.2f,
            radiusY = height * 0.15f,
            time = t,
            phase = 2f
        )
        drawRadialBlob(c3, offset2, width * 0.8f)

        // 4. Animated top-right/center blob
        val c4 = safeColors.getOrElse(3) { c2 }
        val offset3 = calculateAnimatedOffset(
            center = Offset(width * 0.9f, height * 0.3f),
            radiusX = width * 0.25f,
            radiusY = height * 0.2f,
            time = t,
            phase = 4f
        )
        drawRadialBlob(c4, offset3, width * 1.0f)
    }
}

private fun DrawScope.drawRadialBlob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.6f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun calculateAnimatedOffset(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    time: Float,
    phase: Float
): Offset {
    val x = center.x + radiusX * sin(time + phase)
    val y = center.y + radiusY * cos(time + phase * 0.5f)
    return Offset(x, y)
}
