package echo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.HomePage

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun SongItem.artistNames() = artists.joinToString { it.name }

private fun SongItem.subtitle() = artistNames() + (duration?.let { "  •  " + formatTime(it * 1000L) } ?: "")

@Composable
fun SongRow(song: SongItem, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(song.thumbnail, Modifier.size(56.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(
                song.subtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    status: String,
    songs: List<SongItem>,
    onSearch: () -> Unit,
    onPlay: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Search YouTube Music...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            modifier =
                Modifier.fillMaxWidth().padding(top = 16.dp).onKeyEvent {
                    if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                        onSearch()
                        true
                    } else false
                },
        )
        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            itemsIndexed(songs) { i, song -> SongRow(song) { onPlay(i) } }
        }
    }
}

@Composable
fun HomeScreen(home: HomePage?, status: String, onPlay: (List<SongItem>, Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                "Echo Music",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (home == null) item { Text(status, modifier = Modifier.padding(horizontal = 16.dp)) }
        items(home?.sections.orEmpty()) { section -> SectionView(section, onPlay) }
    }
}

@Composable
private fun SectionView(section: HomePage.Section, onPlay: (List<SongItem>, Int) -> Unit) {
    val songs = section.items.filterIsInstance<SongItem>()
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (songs.isNotEmpty()) OutlinedButton(onClick = { onPlay(songs, 0) }) { Text("Play all") }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items) { item ->
                ItemCard(item) { if (item is SongItem) onPlay(songs, songs.indexOf(item)) }
            }
        }
    }
}

@Composable
private fun ItemCard(item: YTItem, onClick: () -> Unit) {
    Column(
        Modifier.width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = item is SongItem, onClick = onClick)
    ) {
        RemoteImage(hires(item.thumbnail), Modifier.size(150.dp), RoundedCornerShape(16.dp))
        Text(
            item.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            when (item) {
                is SongItem -> item.artistNames()
                is AlbumItem -> "Album"
                is PlaylistItem -> "Playlist"
                is ArtistItem -> "Artist"
                else -> ""
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun MiniPlayer(playback: Playback, state: PlayerState, player: VlcPlayer, onOpen: () -> Unit) {
    val song = playback.current ?: return
    Surface(tonalElevation = 3.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(song.thumbnail, Modifier.size(48.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(
                        song.artistNames(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { player.togglePause() }) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause")
                }
            }
            LinearProgressIndicator(
                progress = { state.positionMs.toFloat() / state.lengthMs.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun NowPlaying(playback: Playback, state: PlayerState, player: VlcPlayer, onClose: () -> Unit) {
    val song = playback.current ?: return
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val length = state.lengthMs.coerceAtLeast(1)
    val shown = if (dragging) dragValue else state.positionMs.toFloat() / length

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, "Close player") }
            Text("Now Playing", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.widthIn(max = 480.dp).weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            RemoteImage(hires(song.thumbnail), Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(24.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                song.artistNames(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Slider(
                value = shown.coerceIn(0f, 1f),
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    player.seekTo((dragValue * length).toLong())
                    dragging = false
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(if (dragging) (dragValue * length).toLong() else state.positionMs), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(state.lengthMs), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { playback.previous() }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(30.dp))
                }
                FilledIconButton(onClick = { player.togglePause() }, modifier = Modifier.size(80.dp)) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", Modifier.size(42.dp))
                }
                FilledTonalIconButton(onClick = { playback.next() }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Filled.SkipNext, "Next", Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            SelectionContainer {
                Text(
                    state.error ?: playback.info,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
