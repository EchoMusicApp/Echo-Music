package echo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    /** Mood chip on the home page ("Relax", "Party", ...); null = none. */
    var mood by mutableStateOf<String?>(null)
        private set

    fun pickMood(m: String) {
        mood = if (mood == m) null else m
        reset()
    }
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
        val moodSeeds = mood?.let { listOf(it to "$it english songs", "$it vibes" to "$it songs playlist", "Best $it songs" to "best $it songs") }.orEmpty()
        return (moodSeeds + personal + defaults + years).firstOrNull { it.second !in used }
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
private fun HeroCarousel(songs: List<SongItem>, onPlay: (List<SongItem>, Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(songs.take(10)) { i, s ->
            Box(Modifier.width(420.dp).height(280.dp).clip(RoundedCornerShape(28.dp)).clickable { onPlay(songs, i) }) {
                RemoteImage(hires(s.thumbnail), Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    Text(s.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artists.joinToString { it.name }, color = Color(0xCCFFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun FeedHome(feed: Feed, onPlay: (List<SongItem>, Int) -> Unit, onProfile: () -> Unit) {
    val state = rememberLazyListState()
    val last by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
    val total = feed.shelves.size
    // Near the bottom (or nothing loaded yet): fetch the next shelves.
    LaunchedEffect(last, total) { if (last >= total + 1) feed.loadMore() }

    Panel(Modifier.fillMaxSize()) {
        LazyColumn(state = state, contentPadding = PaddingValues(top = 16.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Echo Music", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                        if (Settings.avatarUrl.isNotBlank()) RemoteImage(Settings.avatarUrl, Modifier.fillMaxSize(), CircleShape)
                        else Text(Settings.profileName.firstOrNull()?.uppercase() ?: "E", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(listOf("Feel good", "Romance", "Relax", "Party", "Energize", "Workout", "Commute", "Focus", "Sleep")) { m ->
                        FilterChip(feed.mood == m, { feed.pickMood(m) }, { Text(m) }, shape = RoundedCornerShape(20.dp))
                    }
                }
            }
            feed.shelves.firstOrNull()?.let { hero -> item { HeroCarousel(hero.songs, onPlay) } }
            itemsIndexed(feed.shelves.drop(1)) { index, shelf ->
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(shelf.title.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        OutlinedButton(onClick = { onPlay(shelf.songs, 0) }, shape = RoundedCornerShape(20.dp)) { Text("Play all") }
                    }
                    if (index % 2 == 0) {
                        // List style, like the phone's "Forgotten favorites"
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            shelf.songs.take(5).forEachIndexed { i, song -> SongRow(song) { onPlay(shelf.songs, i) } }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            itemsIndexed(shelf.songs) { i, song -> PlayCard(song) { onPlay(shelf.songs, i) } }
                        }
                    }
                }
            }
            if (feed.loading || feed.shelves.isEmpty()) item { Text("Loading more music...") }
        }
    }
}
