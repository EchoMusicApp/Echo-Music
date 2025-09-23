package iad1tya.echo.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SonicBoomAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 200.dp,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonic_boom")
    
    // Animation for the expanding circles
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    // Animation for the pulsing center
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Animation for the rotating lines
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val maxRadius = minOf(centerX, centerY)
            
            // Draw expanding circles
            for (i in 0..3) {
                val circleScale = (scale - i * 0.2f).coerceAtLeast(0f)
                val radius = maxRadius * circleScale
                val alpha = (1f - circleScale).coerceAtLeast(0f)
                
                if (alpha > 0f) {
                    drawCircle(
                        color = color.copy(alpha = alpha * 0.3f),
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            
            // Draw rotating lines
            val lineCount = 8
            val lineLength = maxRadius * 0.8f
            
            for (i in 0 until lineCount) {
                val angle = (rotation + i * (360f / lineCount)) * (Math.PI / 180f)
                val startX = centerX + cos(angle).toFloat() * (maxRadius * 0.3f)
                val startY = centerY + sin(angle).toFloat() * (maxRadius * 0.3f)
                val endX = centerX + cos(angle).toFloat() * lineLength
                val endY = centerY + sin(angle).toFloat() * lineLength
                
                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            // Draw pulsing center circle
            val centerRadius = maxRadius * 0.1f * pulse
            drawCircle(
                color = color,
                radius = centerRadius,
                center = Offset(centerX, centerY)
            )
        }
    }
}

@Composable
fun SonicBoomEntranceAnimation(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.5f) }
    
    LaunchedEffect(Unit) {
        // Start the entrance animation
        isVisible = true
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        
        // Wait a bit then complete
        delay(1500)
        onAnimationComplete()
    }
    
    if (isVisible) {
        SonicBoomAnimation(
            modifier = modifier
                .alpha(alpha.value),
            color = Color.White
        )
    }
}
