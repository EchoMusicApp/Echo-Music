package echo.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.desktop.core.YouTubeSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Shelf(val title: String, val songs: List<SongItem>)

/**
 * Endless home feed. Each shelf is one YouTube search. Order: shelves from what you play and like
 * (top artists, last songs) first, then English music by genre and year. Shelves are added as you scroll.
 */
class Feed(private val playback: Playback) {
    var shelves by mutableStateOf<List<Shelf>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    private val used = mutableSetOf<String>()
    private var spotify = emptyList<String>()
    private var spotifyFor = ""

    private val defaults =
        listOf("Top English songs", "English pop hits", "English rock classics", "English hip hop", "English R&B", "Chill English songs",
            "English indie", "English acoustic", "English dance and EDM", "English country", "English love songs", "English workout songs")
            .map { it to it.lowercase() }
    private val years = (2025 downTo 1980).map { "English hits of $it" to "english hits $it" }

    private fun nextSeed(): Pair<String, String>? {
        val taste = playback.history.take(100) + playback.liked.take(50)
        val artists =
            taste.flatMap { s -> s.artists.map { it.name } }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        val personal =
            spotify.take(6).map { "From your Spotify: $it" to "$it songs" } +
                playback.history.take(3).map { "Because you played ${it.title}" to "${it.title} ${it.artists.firstOrNull()?.name.orEmpty()} similar songs" } +
                artists.take(8).map { "More from $it" to "$it songs" }
        return (personal + defaults + years).firstOrNull { it.second !in used }
    }

    suspend fun loadMore(count: Int = 2) {
        if (loading) return
        loading = true
        try {
            val link = Settings.tasteLink.trim()
            if (link != spotifyFor) {
                spotifyFor = link
                spotify = if (link.isEmpty()) emptyList() else runCatching { Spotify.artists(link) }.getOrDefault(emptyList())
            }
            repeat(count) {
                val (title, query) = nextSeed() ?: return
                used += query
                val songs =
                    withContext(Dispatchers.IO) {
                            YouTubeSetup.ensureInit()
                            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        }
                        .getOrNull()?.items?.filterIsInstance<SongItem>()?.distinctBy { it.id }?.take(15).orEmpty()
                if (songs.isNotEmpty()) shelves = shelves + Shelf(title, songs)
            }
        } finally {
            loading = false
        }
    }

    /** Start over so the new feed reflects what was played since. */
    fun reset() {
        shelves = emptyList()
        used.clear()
    }
}

@Composable
fun PlayCard(song: SongItem, onClick: () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    Column(Modifier.width(160.dp).hoverable(hover).clickable(onClick = onClick)) {
        Box {
            RemoteImage(hires(song.thumbnail), Modifier.size(160.dp), RoundedCornerShape(12.dp))
            if (hovered) {
                FilledIconButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(40.dp)) {
                    Icon(Icons.Filled.PlayArrow, "Play")
                }
            }
        }
        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Text(
            song.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FeedHome(feed: Feed, onPlay: (List<SongItem>, Int) -> Unit) {
    val state = rememberLazyListState()
    val last by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
    val total = feed.shelves.size
    // Near the bottom (or nothing loaded yet): fetch the next shelves.
    LaunchedEffect(last, total) { if (last >= total - 1) feed.loadMore() }

    Panel(Modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                val hour = java.time.LocalTime.now().hour
                Text(
                    if (hour < 12) "Good morning" else if (hour < 18) "Good afternoon" else "Good evening",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                )
            }
            items(feed.shelves) { shelf ->
                Column {
                    Text(shelf.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(shelf.songs) { i, song -> PlayCard(song) { onPlay(shelf.songs, i) } }
                    }
                }
            }
            if (feed.loading || feed.shelves.isEmpty()) item { Text("Loading more music...") }
        }
    }
}
