package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeartBurstIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    likedColor: Color = Color(0xFFFF4081), // Pink color
    unlikedColor: Color = LocalContentColor.current
) {
    val burstAnim = remember { Animatable(0f) }
    var wasLiked by remember { mutableStateOf(isLiked) }
    
    LaunchedEffect(isLiked) {
        if (isLiked && !wasLiked) {
            burstAnim.snapTo(0f)
            burstAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
        }
        wasLiked = isLiked
    }

    val actualScale = if (burstAnim.isRunning) {
        if (burstAnim.value < 0.3f) 1f + burstAnim.value else 1.3f - (burstAnim.value - 0.3f) * 0.4f
    } else {
        1f
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Draw the bursting particles
        if (burstAnim.value > 0f && burstAnim.value < 1f) {
            val progress = burstAnim.value
            val alpha = (1f - progress).coerceIn(0f, 1f)
            
            val particleCount = 6
            val heartPainter = painterResource(id = R.drawable.favorite)
            
            Canvas(modifier = Modifier.size(iconSize * 2.5f)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2.2f
                
                for (i in 0 until particleCount) {
                    val angle = (i * (360f / particleCount)) * (Math.PI / 180f)
                    // Move outward
                    val currentRadius = maxRadius * progress
                    val x = center.x + cos(angle).toFloat() * currentRadius
                    val y = center.y + sin(angle).toFloat() * currentRadius
                    
                    val particleSize = (size.width * 0.25f) * (1f - progress * 0.3f)
                    
                    translate(left = x - particleSize/2, top = y - particleSize/2) {
                        with(heartPainter) {
                            draw(
                                size = Size(particleSize, particleSize),
                                alpha = alpha,
                                colorFilter = ColorFilter.tint(likedColor)
                            )
                        }
                    }
                }
            }
        }

        // Main Icon
        Icon(
            painter = painterResource(id = if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = null,
            tint = if (isLiked) likedColor else unlikedColor,
            modifier = Modifier.size(iconSize * actualScale)
        )
    }
}
