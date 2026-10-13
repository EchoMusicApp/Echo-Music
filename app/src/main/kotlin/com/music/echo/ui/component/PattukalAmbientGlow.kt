package com.music.echo.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun PattukalAmbientGlow(
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFFFF4D6D),
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pattukal_liquid")

    val movement by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 5000,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "liquid_movement"
    )

    val reflection by transition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 3600,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "liquid_reflection"
    )

    Box(modifier = modifier) {

        // Moving ambient light
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    glowColor.copy(alpha = 0.22f),
                                    glowColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                            center =
                                androidx.compose.ui.geometry.Offset(
                                    x = movement * 1000f,
                                    y = (1f - movement) * 700f
                                ),
                            radius = 420f
                        )
                    )
        )

        // Moving liquid reflection
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = reflection
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.025f),
                                    Color.White.copy(alpha = 0.035f),
                                    Color.Transparent
                                )
                        )
                    )
        )

        // Soft glass highlight
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.07f),
                                    Color.Transparent
                                ),
                            radius = 500f
                        )
                    )
        )

        content()
    }
}
