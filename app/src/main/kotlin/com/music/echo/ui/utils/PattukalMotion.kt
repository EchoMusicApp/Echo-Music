package echo.music.iad1tya.ui.utils

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

/** Shared motion language for Pattukal: used for every screen transition (except the startup). */
val PattukalEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

fun pattukalEnter(forward: Boolean): EnterTransition =
  fadeIn(tween(360, delayMillis = 70, easing = PattukalEase)) +
    scaleIn(tween(460, easing = PattukalEase), initialScale = 0.93f) +
    slideInHorizontally(tween(460, easing = PattukalEase)) { if (forward) it / 7 else -it / 7 }

fun pattukalExit(forward: Boolean): ExitTransition =
  fadeOut(tween(200, easing = PattukalEase)) +
    scaleOut(tween(320, easing = PattukalEase), targetScale = 0.96f) +
    slideOutHorizontally(tween(360, easing = PattukalEase)) { if (forward) -it / 9 else it / 9 }

/**
 * Press feedback for every button, card and row in the app: a springy squeeze with a soft light
 * bloom that follows the touch. Installed once through [androidx.compose.foundation.LocalIndication].
 */
object PattukalIndication : IndicationNodeFactory {
  override fun create(interactionSource: InteractionSource): DelegatableNode =
    PattukalIndicationNode(interactionSource)

  override fun equals(other: Any?): Boolean = other === PattukalIndication

  override fun hashCode(): Int = 7919
}

private class PattukalIndicationNode(private val interactionSource: InteractionSource) :
  Modifier.Node(), DrawModifierNode {
  private val press = Animatable(0f)

  override fun onAttach() {
    coroutineScope.launch {
      interactionSource.interactions.collect { interaction ->
        when (interaction) {
          is PressInteraction.Press ->
            launch { press.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
          is PressInteraction.Release,
          is PressInteraction.Cancel ->
            launch { press.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
          else -> Unit
        }
      }
    }
  }

  override fun ContentDrawScope.draw() {
    val p = press.value
    val big = max(size.width, size.height) > 600f
    val s = 1f - p * (if (big) 0.012f else 0.05f)
    scale(s, s, center) { this@draw.drawContent() }
    if (p > 0.01f) {
      val r = min(size.width, size.height) * 0.6f
      drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f * p), Color.Transparent), center = center, radius = r),
        radius = r,
        center = center
      )
    }
  }
}

@Suppress("unused") private val keepIndicationImport: Indication? = null
