package iad1tya.echo.music.ui.ipod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import iad1tya.echo.music.viewmodels.LibraryViewModels

/**
 * Generic iPod list browser for Songs, Albums, Artists.
 * ponytail: reads from existing LibraryViewModels, renders a flat list.
 */
@Composable
fun IpodBrowserScreen(
    title: String,
    destination: IpodDestination,
    selectedIndex: Int,
    onItemSelected: (index: Int) -> Unit,
    viewModels: LibraryViewModels = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val colors = LocalIpodColors.current
    val listState = rememberLazyListState()

    // Read data from existing viewmodels based on destination
    val items = when (destination) {
        is IpodDestination.Songs -> {
            val songs by viewModels.songs.collectAsState()
            songs.map { it.title }
        }
        is IpodDestination.Albums -> {
            val albums by viewModels.albums.collectAsState()
            albums.map { it.album.title }
        }
        is IpodDestination.Artists -> {
            val artists by viewModels.artists.collectAsState()
            artists.map { it.artist.name }
        }
        is IpodDestination.Playlists -> {
            val playlists by viewModels.playlists.collectAsState()
            playlists.map { it.playlist.name }
        }
        else -> emptyList()
    }

    LaunchedEffect(selectedIndex, items.size) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.selectedHighlight.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = title,
                color = colors.screenBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No items",
                    color = colors.screenTextSecondary,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(items) { index, item ->
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
                            text = item,
                            color = if (isSelected) colors.screenBackground else colors.screenText,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < items.lastIndex) {
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
}
