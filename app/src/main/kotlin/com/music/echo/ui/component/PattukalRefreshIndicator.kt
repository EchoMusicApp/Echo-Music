@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Pattukal pull-to-refresh: while pulling, a glowing ring draws itself and orbiting beads fan out;
 * while refreshing, the beads orbit around a breathing core. Smooth, no layout work, canvas only.
 */
@Composable
fun BoxScope.PattukalRefreshIndicator(
  state: PullToRefreshState,
  isRefreshing: Boolean,
  modifier: Modifier = Modifier
) {
  val inf = rememberInfiniteTransition(label = "refresh")
  val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
  val breathe by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "breathe")
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary

  Canvas(
    modifier =
      modifier
        .align(Alignment.TopCenter)
        .size(56.dp)
        .graphicsLayer {
          val f = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1.3f)
          translationY = f * 64.dp.toPx() - 44.dp.toPx()
          alpha = min(1f, f * 1.6f)
          val sc = 0.5f + 0.5f * min(1f, f)
          scaleX = sc
          scaleY = sc
        }
  ) {
    val f = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1f)
    val c = Offset(size.width / 2, size.height / 2)
    val r = size.minDimension * 0.32f
    // soft aura
    drawCircle(
      Brush.radialGradient(listOf(primary.copy(alpha = 0.35f * f), Color.Transparent), center = c, radius = size.minDimension / 2),
      radius = size.minDimension / 2,
      center = c
    )
    // progress ring
    val sweep = if (isRefreshing) 270f else 360f * f
    val start = if (isRefreshing) spin else -90f + f * 120f
    drawArc(
      brush = Brush.sweepGradient(listOf(primary, tertiary, primary), center = c),
      startAngle = start,
      sweepAngle = sweep,
      useCenter = false,
      topLeft = Offset(c.x - r, c.y - r),
      size = Size(r * 2, r * 2),
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
    // orbiting beads
    val beads = 3
    for (i in 0 until beads) {
      val ang = Math.toRadians((spin * (if (isRefreshing) 1.6f else 0.4f) + i * 360.0 / beads)).toFloat()
      val rr = r + 6.dp.toPx()
      val bead = Offset(c.x + cos(ang) * rr, c.y + sin(ang) * rr)
      drawCircle(tertiary.copy(alpha = 0.35f + 0.65f * f), 2.4.dp.toPx() * (0.6f + 0.4f * f), bead)
    }
    // core
    val core = r * (0.28f + 0.16f * (if (isRefreshing) breathe else f))
    drawCircle(primary, core, c)
    drawCircle(Color.White.copy(alpha = 0.7f), core * 0.35f, Offset(c.x - core * 0.25f, c.y - core * 0.25f))
  }
}
