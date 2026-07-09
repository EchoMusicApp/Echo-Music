package iad1tya.echo.music.ui.ipod

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iPod Classic menu list.
 * ponytail: LazyColumn with selected index, wheel scrolls the index.
 */
data class IpodMenuItem(
    val title: String,
    val icon: String, // ponytail: emoji icon, no drawable resources needed
    val destination: IpodDestination,
)

val mainMenuItems = listOf(
    IpodMenuItem("Artists", "🎤", IpodDestination.Artists),
    IpodMenuItem("Albums", "💿", IpodDestination.Albums),
    IpodMenuItem("Songs", "🎶", IpodDestination.Songs),
    IpodMenuItem("Playlists", "📋", IpodDestination.Playlists),
    IpodMenuItem("Favorites", "❤️", IpodDestination.Favorites),
    IpodMenuItem("Settings", "⚙️", IpodDestination.Settings),
    IpodMenuItem("Now Playing", "▶️", IpodDestination.NowPlaying),
)

@Composable
fun IpodMenuScreen(
    selectedIndex: Int,
    onItemSelected: (IpodMenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalIpodColors.current
    val listState = rememberLazyListState()

    // Auto-scroll to keep selected item visible
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex.coerceIn(0, mainMenuItems.lastIndex))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
    ) {
        IpodHeaderBar(title = "iPod")

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(mainMenuItems) { index, item ->
                val isSelected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) colors.selectedHighlight
                            else colors.screenBackground
                        )
                        .clickable { onItemSelected(item) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.icon,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item.title,
                        color = if (isSelected) colors.screenBackground else colors.screenText,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "›",
                        color = if (isSelected) colors.screenBackground else colors.screenTextSecondary,
                        fontSize = 18.sp,
                    )
                }
                if (index < mainMenuItems.lastIndex) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 44.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun IpodHeaderBar(title: String, modifier: Modifier = Modifier, centered: Boolean = false) {
    val colors = LocalIpodColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.selectedHighlight.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            color = colors.screenBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (centered) Modifier.fillMaxWidth() else Modifier.align(Alignment.CenterStart),
            textAlign = if (centered) TextAlign.Center else null,
        )
    }
}
