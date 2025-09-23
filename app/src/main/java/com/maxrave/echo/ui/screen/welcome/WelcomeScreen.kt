package iad1tya.echo.music.ui.screen.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.theme.typo

@Composable
fun WelcomeScreen(
    onAnimationComplete: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Show content and start wave animation together
        delay(500) // Reduced delay for faster appearance
        showContent = true
        
        // Animate title and subtitle together
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        
        // Animate subtitle with minimal delay
        delay(100)
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Custom wave animation positioned behind the logo
        LogoWaveAnimation(
            modifier = Modifier.fillMaxSize(),
            color = Color.White.copy(alpha = 0.1f)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            if (showContent) {
                AsyncImage(
                    model = R.mipmap.ic_launcher_round,
                    contentDescription = "Echo Music Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(titleAlpha.value)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Echo Music",
                    style = typo.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(titleAlpha.value)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your music, always with you.",
                    style = typo.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(subtitleAlpha.value)
                )
            }
        }
        
        // Auto-advance after animation
        LaunchedEffect(showContent) {
            if (showContent) {
                delay(4000) // Show for 4 seconds to appreciate the full wave effect
                onAnimationComplete()
            }
        }
    }
}

@Composable
fun LogoWaveAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    animationDuration: Int = 3000,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoWave")

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

    Canvas(
        modifier = modifier
    ) {
        // Position the wave center exactly at the center of the icon
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2 - 80.dp.toPx() // Align with icon center
        // Calculate max radius to go all around the screen
        val maxRadius = sqrt((this.size.width * this.size.width + this.size.height * this.size.height).toDouble()).toFloat() / 2

        // Draw multiple expanding waves that go all around the screen
        for (i in 0..8) {
            val waveScale = (scale - i * 0.12f).coerceAtLeast(0f)
            if (waveScale > 0) {
                val currentRadius = maxRadius * waveScale
                val currentAlpha = alpha * (1 - waveScale * 0.7f)
                
                // Draw multiple circles for wave effect
                for (j in 0..3) {
                    val radius = currentRadius + (j * 25.dp.toPx())
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
