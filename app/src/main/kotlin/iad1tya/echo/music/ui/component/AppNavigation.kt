package iad1tya.echo.music.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import iad1tya.echo.music.ui.screens.Screens

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    slimNav: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        navigationItems.forEach { screen ->
            val selected = isSelected(screen)
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(screen, selected) },
                icon = {
                    Icon(
                        painter = painterResource(id = if (selected) screen.iconIdActive else screen.iconIdInactive),
                        contentDescription = stringResource(screen.titleId),
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
