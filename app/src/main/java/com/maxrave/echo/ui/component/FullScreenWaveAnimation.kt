package iad1tya.echo.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun FullScreenWaveAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    animationDuration: Int = 3000,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fullScreenWave")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scaleAnimation"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alphaAnimation"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val maxRadius = sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

            // Draw multiple expanding waves
            for (i in 0..8) {
                val waveScale = (scale - i * 0.1f).coerceAtLeast(0f)
                if (waveScale > 0) {
                    val currentRadius = maxRadius * waveScale
                    val currentAlpha = alpha * (1 - waveScale * 0.8f)
                    
                    // Draw multiple circles for wave effect
                    for (j in 0..3) {
                        val radius = currentRadius + (j * 30.dp.toPx())
                        if (radius <= maxRadius) {
                            drawCircle(
                                color = color.copy(alpha = currentAlpha * (1 - j * 0.25f) * 0.3f),
                                radius = radius,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
