package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** Everything a glass surface needs to draw itself; shared by buttons and pills. */
private fun DrawScope.drawGlass(
  shape: Shape,
  base: Color,
  accent: Color,
  solid: Boolean,
  ripple: Float,
  ringDeg: Float?,
) {
  val outline = shape.createOutline(size, layoutDirection, this)
  val w = size.width
  val h = size.height
  if (solid) {
    drawOutline(outline, color = base)
    drawOutline(
      outline,
      Brush.verticalGradient(0f to Color.White.copy(alpha = 0.42f), 0.5f to Color.Transparent, 1f to accent.copy(alpha = 0.18f)),
    )
  } else {
    drawOutline(outline, Brush.verticalGradient(listOf(base.copy(alpha = 0.22f), base.copy(alpha = 0.07f))))
    // light focusing at the bottom edge, like water in a glass
    drawOutline(
      outline,
      Brush.radialGradient(
        listOf(accent.copy(alpha = 0.34f), Color.Transparent),
        center = Offset(w * 0.5f, h * 1.05f),
        radius = max(w, h) * 0.75f,
      ),
    )
  }
  // bright rim: light from the top-left, accent from the bottom-right
  drawOutline(
    outline,
    Brush.linearGradient(
      listOf(Color.White.copy(alpha = if (solid) 0.9f else 0.7f), Color.White.copy(alpha = 0.06f), accent.copy(alpha = 0.65f)),
      start = Offset.Zero,
      end = Offset(w, h),
    ),
    style = Stroke(width = 1.3.dp.toPx()),
  )
  // specular streak
  drawRoundRect(
    Color.White.copy(alpha = if (solid) 0.40f else 0.50f),
    topLeft = Offset(w * 0.2f, h * 0.08f),
    size = Size(w * 0.6f, max(2f, h * 0.09f)),
    cornerRadius = CornerRadius(h * 0.05f),
  )
  if (ringDeg != null) {
    rotate(ringDeg) {
      drawOutline(
        outline,
        Brush.sweepGradient(
          listOf(Color.Transparent, accent.copy(alpha = 0.95f), Color.White, accent.copy(alpha = 0.95f), Color.Transparent),
        ),
        style = Stroke(width = 2.6.dp.toPx()),
      )
    }
  }
  if (ripple > 0f && ripple < 1f) {
    val md = max(w, h)
    drawCircle(
      Color.White.copy(alpha = (1f - ripple) * 0.55f),
      radius = md * (0.15f + 1.0f * ripple),
      center = Offset(w / 2f, h / 2f),
      style = Stroke(width = (1f + 3f * (1f - ripple)).dp.toPx()),
    )
    val r2 = ((ripple - 0.2f) / 0.8f).coerceIn(0f, 1f)
    if (r2 > 0f) {
      drawCircle(
        accent.copy(alpha = (1f - r2) * 0.5f),
        radius = md * (0.1f + 0.9f * r2),
        center = Offset(w / 2f, h / 2f),
        style = Stroke(width = 2.dp.toPx()),
      )
    }
  }
}

/**
 * Liquid-glass button used across the player. Same call shape as FilledIconButton so existing buttons
 * adopt it directly. Press = springy squeeze + a water ripple ring. [active] spins a light ring around it.
 * The accent follows the current Pattukal theme.
 */
@Composable
fun GlassIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = CircleShape,
  colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
  interactionSource: MutableInteractionSource? = null,
  emphasis: Boolean = false,
  active: Boolean = false,
  content: @Composable () -> Unit,
) {
  val source = interactionSource ?: remember { MutableInteractionSource() }
  val pressed by source.collectIsPressedAsState()
  val accent = MaterialTheme.colorScheme.primary
  val container = colors.containerColor
  val contentColor = colors.contentColor
  val base = if (container.alpha >= 0.6f) container else contentColor
  val ripple = remember { Animatable(0f) }
  LaunchedEffect(pressed) {
    if (pressed) {
      ripple.snapTo(0f)
      ripple.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
    }
  }
  val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.5f, stiffness = 520f), label = "glassScale")
  val ring =
    if (active) {
      rememberInfiniteTransition(label = "glassRing")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(4800, easing = LinearEasing), RepeatMode.Restart), label = "ringDeg")
    } else null

  Box(
    modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = if (enabled) 1f else 0.4f
      }
      .clip(shape)
      .drawBehind { drawGlass(shape, base, accent, emphasis, ripple.value, ring?.value) }
      .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    CompositionLocalProvider(LocalContentColor provides (if (emphasis) contentColor else base)) { content() }
  }
}

/** Glass fill + rim for pills and chips (used by the player's bottom row). */
@Composable
fun Modifier.glassSurface(shape: Shape, base: Color, solid: Boolean = false): Modifier {
  val accent = MaterialTheme.colorScheme.primary
  return this.clip(shape).drawBehind { drawGlass(shape, base, accent, solid, 0f, null) }
}
