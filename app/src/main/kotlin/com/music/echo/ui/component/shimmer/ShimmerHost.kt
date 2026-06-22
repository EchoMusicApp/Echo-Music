

package iad1tya.echo.music.ui.component.shimmer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.shimmer

@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        modifier =
        modifier
            .shimmer(),
        content = content,
    )
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
