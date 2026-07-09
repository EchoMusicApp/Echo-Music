package iad1tya.echo.music.ui.ipod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iPod specific settings (theme picker).
 */
@Composable
fun IpodSettingsScreen(
    selectedIndex: Int,
    onItemSelected: (IpodColorScheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalIpodColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex.coerceIn(0, IpodThemes.lastIndex))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
    ) {
        IpodHeaderBar(title = "Themes")

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(IpodThemes) { index, theme ->
                val isSelected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) colors.selectedHighlight
                            else colors.screenBackground
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = theme.name,
                        color = if (isSelected) colors.screenBackground else colors.screenText,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (colors.name == theme.name) {
                        Text(
                            text = "✓",
                            color = if (isSelected) colors.screenBackground else colors.screenTextSecondary,
                            fontSize = 16.sp,
                        )
                    }
                }
                if (index < IpodThemes.lastIndex) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
