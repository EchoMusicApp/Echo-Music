package iad1tya.echo.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.constants.FloatingCompactMaxWidth
import iad1tya.echo.music.constants.FloatingCompactWidthFraction
import iad1tya.echo.music.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(FloatingCompactWidthFraction)
            .widthIn(max = FloatingCompactMaxWidth),
        shape = RoundedCornerShape(28.dp),
        color =
            if (pureBlack) {
                Color(0xFF0B0B0B)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            },
        border = BorderStroke(
            width = 1.dp,
            color =
                if (pureBlack) {
                    Color.White.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                },
        ),
        tonalElevation = 10.dp,
        shadowElevation = 18.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { screen ->
                val selected = isSelected(screen)
                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    slim = slim,
                    pureBlack = pureBlack,
                    onClick = { onItemClick(screen, selected) },
                )
            }
        }
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    slim: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val containerColor by animateColorAsState(
        targetValue =
            when {
                selected && pureBlack -> Color.White.copy(alpha = 0.22f)
                selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                pureBlack -> Color.White.copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
            },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                selected && pureBlack -> Color.White
                selected -> MaterialTheme.colorScheme.onPrimaryContainer
                pureBlack -> Color.White.copy(alpha = 0.9f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            },
        label = "",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )
    val showLabel = selected && !slim && screen.route != Screens.Search.route

    Row(
        modifier =
            Modifier
                .scale(scale)
                .animateContentSize()
                .clip(shape)
                .background(color = containerColor, shape = shape)
                .border(
                    width = if (selected) 1.dp else 0.5.dp,
                    color =
                        if (selected) {
                            if (pureBlack) Color.White.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        } else {
                            if (pureBlack) Color.White.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                        },
                    shape = shape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .widthIn(min = 48.dp)
                .padding(
                    horizontal = if (showLabel) 14.dp else 10.dp,
                    vertical = 12.dp,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
        )

        if (showLabel) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
