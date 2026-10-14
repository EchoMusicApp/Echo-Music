package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp

private const val LOADER_CYCLE_MS = 2000
private const val LETTER_STAGGER_MS = 50f

/**
 * "PATTUKAL" letter-wave loader.
 *
 * Every letter flies in from the left while spinning upright (180° -> 0°), drifts slowly through
 * its slot so the word is readable for a moment, then flies out to the right spinning on (0° ->
 * -180°) while fading away. Letters are staggered, and the whole cycle repeats every 2 seconds.
 *
 * Performance: a single infinite transition drives one float. It is only read inside
 * [graphicsLayer] blocks, so each frame only the layer properties change. Nothing is recomposed
 * and nothing is re-measured while the loader is on screen.
 */
@Composable
fun PattukalLoader(
  modifier: Modifier = Modifier,
  text: String = "PATTUKAL",
  color: Color = MaterialTheme.colorScheme.primary,
  fontSize: TextUnit = 32.sp,
) {
  val transition = rememberInfiniteTransition(label = "pattukal_loader")
  val time =
    transition.animateFloat(
      initialValue = 0f,
      targetValue = LOADER_CYCLE_MS.toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = LOADER_CYCLE_MS, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pattukal_loader_time",
    )

  val density = LocalDensity.current
  val travelPx = with(density) { 120.dp.toPx() }
  val driftPx = with(density) { 6.dp.toPx() }

  Box(
    modifier =
      modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Loading" },
    contentAlignment = Alignment.Center,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      text.forEachIndexed { index, letter ->
        Text(
          text = letter.toString(),
          color = color,
          fontSize = fontSize,
          fontWeight = FontWeight.Bold,
          modifier =
            Modifier.graphicsLayer {
              val cycle = LOADER_CYCLE_MS.toFloat()
              val p = (((time.value - index * LETTER_STAGGER_MS) % cycle) + cycle) % cycle / cycle

              when {
                // Fly in from the left, spinning upright.
                p < 0.30f -> {
                  val u = p / 0.30f
                  val eased = 1f - (1f - u) * (1f - u)
                  translationX = lerp(-travelPx, -driftPx, eased)
                  alpha = u
                  rotationZ = 180f * (1f - eased)
                }
                // Hold: drift gently through the slot so the word can be read.
                p < 0.70f -> {
                  val u = (p - 0.30f) / 0.40f
                  translationX = lerp(-driftPx, driftPx, u)
                  alpha = 1f
                  rotationZ = 0f
                }
                // Fly out to the right, spinning on and fading away.
                else -> {
                  val u = (p - 0.70f) / 0.30f
                  val eased = u * u
                  translationX = lerp(driftPx, travelPx, eased)
                  alpha = 1f - u
                  rotationZ = -180f * eased
                }
              }
            },
        )
      }
    }
  }
}
