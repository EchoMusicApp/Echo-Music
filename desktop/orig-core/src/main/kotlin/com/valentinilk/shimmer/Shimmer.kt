package com.valentinilk.shimmer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// Own version of the "compose-shimmer" library (same names the app uses). The real library was built against
// Google's Compose desktop jars and crashes next to JetBrains' ones (see the LinearGradientShader error).
// shortcut: a plain left-to-right sweep; no rotation or custom shimmer bounds.
data class ShimmerTheme(
  val animationSpec: AnimationSpec<Float>,
  val blendMode: BlendMode,
  val rotation: Float,
  val shaderColors: List<Color>,
  val shaderColorStops: List<Float>?,
  val shimmerWidth: androidx.compose.ui.unit.Dp,
)

val defaultShimmerTheme = ShimmerTheme(
  animationSpec = infiniteRepeatable(tween(800, 200, LinearEasing), RepeatMode.Restart),
  blendMode = BlendMode.DstIn,
  rotation = 15f,
  shaderColors = listOf(Color.Unspecified.copy(alpha = 0.25f), Color.Unspecified.copy(alpha = 1f), Color.Unspecified.copy(alpha = 0.25f)),
  shaderColorStops = listOf(0f, 0.5f, 1f),
  shimmerWidth = androidx.compose.ui.unit.Dp(400f),
)

val LocalShimmerTheme = compositionLocalOf { defaultShimmerTheme }

fun Modifier.shimmer(): Modifier = composed {
  val theme = LocalShimmerTheme.current
  val t = rememberInfiniteTransition(label = "shimmer")
  val phase = t.animateFloat(-1f, 2f, theme.animationSpec, label = "phase")
  val base = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
  this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
    drawContent()
    val w = size.width.coerceAtLeast(1f)
    val x = phase.value * w
    val colors = theme.shaderColors.map { if (it == Color.Unspecified) base else it }
    drawRect(
      brush = Brush.linearGradient(colors.map { it.copy(alpha = it.alpha * 0.6f) }, Offset(x - w * 0.3f, 0f), Offset(x + w * 0.3f, 0f)),
      blendMode = BlendMode.SrcAtop,
    )
  }
}
