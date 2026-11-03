package echo.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.pages.HomePage
import echo.desktop.core.YouTubeSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    val player = remember { VlcPlayer() }
    Window(
        onCloseRequest = {
            player.release()
            exitApplication()
        },
        title = "Echo Music (Desktop) v${System.getProperty("echomusic.version") ?: "dev"}",
        state = rememberWindowState(size = DpSize(480.dp, 820.dp)),
    ) {
        MaterialTheme(colorScheme = lightColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) { EchoApp(player) }
        }
    }
}

@Composable
fun EchoApp(player: VlcPlayer) {
    val scope = rememberCoroutineScope()
    val playback = remember { Playback(player, scope) }
    val state by player.state.collectAsState()

    var tab by remember { mutableStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }

    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var searchStatus by remember { mutableStateOf("Type a song or artist and press Enter.") }

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
    // A finished track starts the next one in the queue.
    LaunchedEffect(state.ended) { if (state.ended > 0) playback.next() }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        searchStatus = "Searching..."
        scope.launch {
            withContext(Dispatchers.IO) {
                    YouTubeSetup.ensureInit()
                    YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                }
                .onSuccess {
                    songs = it.items.filterIsInstance<SongItem>()
                    searchStatus = if (songs.isEmpty()) "No songs found." else "${songs.size} results"
                }
                .onFailure { searchStatus = "Search failed: ${it.message ?: it::class.simpleName}" }
        }
    }

    if (showPlayer && playback.current != null) {
        NowPlaying(playback, state, player) { showPlayer = false }
    } else {
        Scaffold(
            bottomBar = {
                Column {
                    MiniPlayer(playback, state, player) { showPlayer = true }
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Filled.Home, null) },
                            label = { Text("Home") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Filled.Search, null) },
                            label = { Text("Search") },
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (tab == 0) {
                    HomeScreen(home, homeStatus) { list, i -> playback.play(list, i) }
                } else {
                    SearchScreen(query, { query = it }, searchStatus, songs, { search() }) { i ->
                        playback.play(songs, i)
                    }
                }
            }
        }
    }
}
