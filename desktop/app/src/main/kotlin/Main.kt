package echo.desktop

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.HomePage
import echo.desktop.core.YouTubeSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        background = PanelColor,
        surface = PanelColor,
        surfaceVariant = Color(0xFF2C2C2C),
        surfaceContainerHigh = Color(0xFF242424),
        onSurfaceVariant = Color(0xFFB3B3B3),
    )

enum class View { Home, Search, Liked, Detail }

fun main() = application {
    val player = remember { VlcPlayer() }
    Window(
        onCloseRequest = {
            player.release()
            exitApplication()
        },
        title = "Echo Music (Desktop) v${System.getProperty("echomusic.version") ?: "dev"}",
        state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
    ) {
        MaterialTheme(colorScheme = DarkColors) {
            Surface(modifier = Modifier.fillMaxSize(), color = Backdrop) { EchoApp(player) }
        }
    }
}

@Composable
fun EchoApp(player: VlcPlayer) {
    val scope = rememberCoroutineScope()
    val playback = remember { Playback(player, scope) }
    val state by player.state.collectAsState()

    var view by remember { mutableStateOf(View.Home) }
    var volume by remember { mutableStateOf(0.8f) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<List<LyricLine>?>(null) }

    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var searchStatus by remember { mutableStateOf("") }

    var detailStatus by remember { mutableStateOf("") }
    var detailSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    var home by remember { mutableStateOf<HomePage?>(null) }
    var homeStatus by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
                YouTubeSetup.ensureInit()
                YouTube.home()
            }
            .onSuccess { home = it }
            .onFailure { homeStatus = "Could not load home: ${it.message ?: it::class.simpleName}" }
    }
    // A finished track starts the next one (respecting shuffle and repeat).
    LaunchedEffect(state.ended) { if (state.ended > 0) playback.next(auto = true) }
    LaunchedEffect(volume) { player.setVolume((volume * 100).toInt()) }
    LaunchedEffect(playback.current?.id) {
        lyrics = null
        val song = playback.current ?: return@LaunchedEffect
        lyrics = withContext(Dispatchers.IO) { Lyrics.fetch(song) }.getOrDefault(emptyList())
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        view = View.Search
        searchStatus = "Searching..."
        scope.launch {
            withContext(Dispatchers.IO) {
                    YouTubeSetup.ensureInit()
                    YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                }
                .onSuccess {
                    songs = it.items.filterIsInstance<SongItem>()
                    searchStatus = if (songs.isEmpty()) "No songs found." else "Results for \"$q\""
                }
                .onFailure { searchStatus = "Search failed: ${it.message ?: it::class.simpleName}" }
        }
    }

    fun open(item: YTItem) {
        view = View.Detail
        detailStatus = "Loading ${item.title}..."
        detailSongs = emptyList()
        scope.launch {
            withContext(Dispatchers.IO) {
                    YouTubeSetup.ensureInit()
                    when (item) {
                        is AlbumItem -> YouTube.album(item.browseId).map { it.songs }
                        is PlaylistItem -> YouTube.playlist(item.id).map { it.songs }
                        else -> Result.success(emptyList<SongItem>())
                    }
                }
                .onSuccess {
                    detailSongs = it
                    detailStatus = item.title
                }
                .onFailure { detailStatus = "Could not open: ${it.message ?: it::class.simpleName}" }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
        val wide = maxWidth > 1000.dp
        Column(Modifier.fillMaxSize()) {
            TopBar(onHome = { view = View.Home }, query = query, onQuery = { query = it }, onSearch = { search() })
            Row(Modifier.weight(1f)) {
                Sidebar(playback.recent, onLiked = { view = View.Liked }) { playback.play(listOf(it), 0) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    when (view) {
                        View.Home -> HomeContent(home, homeStatus, { list, i -> playback.play(list, i) }, { open(it) })
                        View.Search -> SearchResults(searchStatus, songs) { playback.play(songs, it) }
                        View.Liked ->
                            SearchResults("Liked songs (${playback.liked.size})", playback.liked) {
                                playback.play(playback.liked, it)
                            }
                        View.Detail -> SearchResults(detailStatus, detailSongs) { playback.play(detailSongs, it) }
                    }
                }
                val song = playback.current
                if (wide && song != null) {
                    Spacer(Modifier.width(8.dp))
                    NowPlayingPanel(
                        song,
                        state.error ?: playback.info,
                        state.error != null,
                        lyrics,
                        state.positionMs,
                        showLyrics,
                    ) {
                        showLyrics = !showLyrics
                    }
                }
            }
            PlayerBar(playback, state, player, volume) { volume = it }
        }
    }
}
