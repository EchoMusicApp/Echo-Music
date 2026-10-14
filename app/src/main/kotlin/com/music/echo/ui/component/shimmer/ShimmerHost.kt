package echo.music.iad1tya.ui.component.shimmer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.defaultShimmerTheme
import echo.music.iad1tya.ui.component.PattukalLoader
import com.valentinilk.shimmer.shimmer

/**
 * Loading placeholder host. Every loading skeleton in the app now shows the animated PATTUKAL
 * loader instead of grey shimmering boxes. The skeleton [content] is intentionally not composed.
 */
@Composable
fun ShimmerHost(
  modifier: Modifier = Modifier,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  content: @Composable ColumnScope.() -> Unit,
) {
  PattukalLoader(modifier = modifier.padding(vertical = 48.dp))
}

@Composable
fun getShimmerTheme(): com.valentinilk.shimmer.ShimmerTheme {
  val surfaceVariant = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
  return defaultShimmerTheme.copy(
    animationSpec =
      infiniteRepeatable(
        animation =
          tween(
            durationMillis = 800,
            easing = LinearEasing,
            delayMillis = 250,
          ),
        repeatMode = RepeatMode.Restart,
      ),
    shaderColors =
      listOf(
        surfaceVariant.copy(alpha = 0.25f),
        surfaceVariant.copy(alpha = 0.50f),
        surfaceVariant.copy(alpha = 0.25f),
      ),
  )
}
