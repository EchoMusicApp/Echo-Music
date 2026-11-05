package echo.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch


fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun SongItem.artistNames() = artists.joinToString { it.name }

private fun SongItem.subtitle() = artistNames() + (duration?.let { "  •  " + formatTime(it * 1000L) } ?: "")

@Composable
fun Panel(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier) { content() }
}

@Composable
fun TopBar(onHome: () -> Unit, query: String, onQuery: (String) -> Unit, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onHome, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Home, "Home") }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text("What do you want to play?") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                colors =
                    TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                modifier =
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().onKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                            onSearch()
                            true
                        } else false
                    },
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

/** Narrow strip of covers played this session; click one to play it again. */
@Composable
fun Sidebar(recent: List<SongItem>, onNav: (View) -> Unit, onPlay: (SongItem) -> Unit) {
    Panel(Modifier.width(80.dp).fillMaxHeight()) {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                FilledTonalIconButton(onClick = { onNav(View.Liked) }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Favorite, "Liked songs") }
            }
            item { FilledTonalIconButton(onClick = { onNav(View.Community) }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.Groups, "Community") } }
            item { FilledTonalIconButton(onClick = { onNav(View.History) }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.History, "History") } }
            item { FilledTonalIconButton(onClick = { onNav(View.Stats) }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.BarChart, "Stats") } }
            item { FilledTonalIconButton(onClick = { onNav(View.Settings) }, modifier = Modifier.size(52.dp)) { Icon(Icons.Filled.SettingsIcon, "Settings") } }
            item { Text("Recent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(recent) { song ->
                RemoteImage(song.thumbnail, Modifier.size(52.dp).clickable { onPlay(song) }, RoundedCornerShape(8.dp))
            }
        }
    }
}
@Composable
fun SongRow(song: SongItem, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
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
fun SearchResults(status: String, songs: List<SongItem>, artists: List<ArtistItem> = emptyList(), onPlay: (Int) -> Unit) {
    val openArtist = LocalOpenArtist.current
    Panel(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 190.dp)) {
                if (artists.isNotEmpty()) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        items(artists) { a ->
                            Column(Modifier.width(96.dp).clickable { openArtist(a.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                RemoteImage(a.thumbnail, Modifier.size(80.dp), CircleShape)
                                Text(a.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                itemsIndexed(songs) { i, song -> SongRow(song) { onPlay(i) } }
            }
        }
    }
}

@Composable
fun HomeContent(
    home: HomePage?,
    status: String,
    onPlay: (List<SongItem>, Int) -> Unit,
    onOpen: (YTItem) -> Unit,
) {
    // First six songs of the feed become the quick-pick grid at the top.
    val quick = home?.sections.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>().distinctBy { it.id }.take(6)
    Panel(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (home == null) item { Text(status, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (quick.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quick.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { song ->
                                    QuickCard(song, Modifier.weight(1f)) { onPlay(quick, quick.indexOf(song)) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            items(home?.sections.orEmpty()) { section -> SectionView(section, onPlay, onOpen) }
        }
    }
}

@Composable
private fun QuickCard(song: SongItem, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(song.thumbnail, Modifier.size(64.dp), RoundedCornerShape(8.dp))
        Text(
            song.title,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun SectionView(section: HomePage.Section, onPlay: (List<SongItem>, Int) -> Unit, onOpen: (YTItem) -> Unit) {
    val songs = section.items.filterIsInstance<SongItem>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    fun scrollBy(delta: Int) =
        scope.launch {
            listState.animateScrollToItem((listState.firstVisibleItemIndex + delta).coerceIn(0, section.items.lastIndex))
        }
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                section.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (songs.isNotEmpty()) {
                OutlinedButton(onClick = { onPlay(songs, 0) }) { Text("Play all") }
            }
            IconButton(onClick = { scrollBy(-3) }) { Icon(Icons.Filled.ChevronLeft, "Scroll left") }
            IconButton(onClick = { scrollBy(3) }) { Icon(Icons.Filled.ChevronRight, "Scroll right") }
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(section.items) { item ->
                ItemCard(item) {
                    when (item) {
                        is SongItem -> onPlay(songs, songs.indexOf(item))
                        is AlbumItem, is PlaylistItem -> onOpen(item)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: YTItem, onClick: () -> Unit) {
    Column(Modifier.width(180.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = item !is ArtistItem, onClick = onClick)) {
        RemoteImage(hires(item.thumbnail), Modifier.size(180.dp), RoundedCornerShape(12.dp))
        Text(
            item.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Right-hand panel: big cover, title, artist and stream details (or the error, selectable). */
@Composable
fun NowPlayingPanel(
    song: SongItem,
    detail: String,
    isError: Boolean,
    lyrics: List<LyricLine>?,
    positionMs: Long,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Panel(Modifier.width(320.dp).fillMaxHeight()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showLyrics) "Lyrics" else song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onToggleLyrics) { Text(if (showLyrics) "Cover" else "Lyrics") }
            }
            if (showLyrics) {
                LyricsView(lyrics, positionMs, Modifier.weight(1f).fillMaxWidth())
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(4.dp))
                    RemoteImage(hires(song.thumbnail), Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val openArtist = LocalOpenArtist.current
                    Text(song.artistNames(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { openArtist(song.artists.firstOrNull()?.id) })
                    Spacer(Modifier.height(16.dp))
                    SelectionContainer {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsView(lines: List<LyricLine>?, positionMs: Long, modifier: Modifier) {
    when {
        lines == null -> Text("Loading lyrics...", modifier = modifier)
        lines.isEmpty() -> Text("No lyrics found for this song.", modifier = modifier)
        else -> {
            val current = if (lines.first().timeMs >= 0) lines.indexOfLast { it.timeMs <= positionMs } else -1
            val listState = rememberLazyListState()
            LaunchedEffect(current) { if (current >= 0) listState.animateScrollToItem((current - 2).coerceAtLeast(0)) }
            LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(lines) { i, line ->
                    Text(
                        line.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
@Composable
fun PlayerBar(
    playback: Playback,
    state: PlayerState,
    player: VlcPlayer,
    volume: Float,
    onVolume: (Float) -> Unit,
) {
    val song = playback.current
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val length = state.lengthMs.coerceAtLeast(1)
    val shown = if (dragging) dragValue else state.positionMs.toFloat() / length
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant

    Row(Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (song != null) {
                RemoteImage(song.thumbnail, Modifier.size(56.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    val openArtist = LocalOpenArtist.current
                    Text(song.artistNames(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = idle, modifier = Modifier.clickable { openArtist(song.artists.firstOrNull()?.id) })
                }
                val liked = playback.isLiked(song)
                IconButton(onClick = { playback.toggleLike(song) }) {
                    Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Like", tint = if (liked) active else idle)
                }
            }
        }
        Column(Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playback.shuffle = !playback.shuffle }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (playback.shuffle) active else idle)
                }
                IconButton(onClick = { playback.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                FilledIconButton(
                    onClick = { player.togglePause() },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause")
                }
                IconButton(onClick = { playback.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
                IconButton(
                    onClick = {
                        playback.repeat =
                            when (playback.repeat) {
                                Repeat.Off -> Repeat.All
                                Repeat.All -> Repeat.One
                                Repeat.One -> Repeat.Off
                            }
                    }
                ) {
                    Icon(
                        if (playback.repeat == Repeat.One) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "Repeat",
                        tint = if (playback.repeat == Repeat.Off) idle else active,
                    )
                }
            }
            Row(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(if (dragging) (dragValue * length).toLong() else state.positionMs), style = MaterialTheme.typography.bodySmall)
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
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(formatTime(state.lengthMs), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, "Volume")
            Slider(value = volume, onValueChange = onVolume, modifier = Modifier.width(120.dp).padding(start = 8.dp))
        }
    }
}
