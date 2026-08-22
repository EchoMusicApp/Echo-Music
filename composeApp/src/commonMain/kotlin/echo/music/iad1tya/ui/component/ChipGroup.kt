package echo.music.iad1tya.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.ui.icon.Done
import echo.music.iad1tya.ui.icon.echoIcons

@Composable
fun Chip(
    isAnimated: Boolean = false,
    isAi: Boolean = false,
    isSelected: Boolean = false,
    text: String,
    onClick: () -> Unit,
) {
    val aiBrush = Brush.sweepGradient(listOf(Color(0xFF4285F4), Color(0xFF9B72CB), Color(0xFFD96570), Color(0xFF4285F4)))
    InfiniteBorderAnimationView(
        isAnimated = (isAnimated && isSelected) || isAi,
        brush = if (isAi) aiBrush else Brush.sweepGradient(listOf(Color.Gray, Color.White)),
        backgroundColor = Color.Transparent,
        contentPadding = 0.dp,
        borderWidth = 1.dp,
        shape = CircleShape,
        oneCircleDurationMillis = 2500,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            ElevatedFilterChip(
                shape = CircleShape,
                colors =
                    FilterChipDefaults.elevatedFilterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                onClick = { onClick.invoke() },
                label = {
                    Text(text, maxLines = 1)
                },
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = Color.Transparent,
                        borderColor = MaterialTheme.colorScheme.outline,
                    ),
                selected = isSelected,
                leadingIcon = {
                    AnimatedContent(isSelected) {
                        if (it) {
                            Icon(
                                imageVector = echoIcons.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    }
                },
            )
        }
    }
}