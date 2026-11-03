package echo.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
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
        title = "Echo Music (Desktop)",
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) { EchoApp(player) }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun EchoApp(player: VlcPlayer) {
    val scope = rememberCoroutineScope()
    val playerState by player.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var status by remember { mutableStateOf("Type a song or artist and press Enter.") }
    var current by remember { mutableStateOf<SongItem?>(null) }
    var streamInfo by remember { mutableStateOf("") }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        status = "Searching..."
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    YouTubeSetup.ensureInit()
                    YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                }
            result
                .onSuccess {
                    songs = it.items.filterIsInstance<SongItem>()
                    status = if (songs.isEmpty()) "No songs found." else "${songs.size} results"
                }
                .onFailure { status = "Search failed: ${it.message ?: it::class.simpleName}" }
        }
    }

    fun playSong(song: SongItem) {
        current = song
        streamInfo = "Loading stream..."
        scope.launch {
            StreamResolver.resolve(song.id)
                .onSuccess {
                    streamInfo = it.description
                    player.play(it)
                }
                .onFailure { streamInfo = it.message ?: "Could not load stream" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search songs") },
                modifier =
                    Modifier.weight(1f).onKeyEvent {
                        if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) {
                            search()
                            true
                        } else false
                    },
            )
            Button(onClick = { search() }) { Text("Search") }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(songs, key = { it.id }) { song ->
                Column(modifier = Modifier.fillMaxWidth().clickable { playSong(song) }.padding(vertical = 8.dp)) {
                    Text(song.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        song.artists.joinToString { it.name } +
                            (song.duration?.let { "  •  " + formatTime(it * 1000L) } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        // Player bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(current?.title ?: "Nothing playing", style = MaterialTheme.typography.titleMedium)
            Text(
                current?.artists?.joinToString { it.name } ?: "",
                style = MaterialTheme.typography.bodySmall,
            )
            if (streamInfo.isNotEmpty()) {
                Text(streamInfo, style = MaterialTheme.typography.labelSmall)
            }
            playerState.error?.let {
                SelectionContainer {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            val length = playerState.lengthMs.coerceAtLeast(1)
            val shown = if (dragging) dragValue else playerState.positionMs.toFloat() / length
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { player.togglePause() }, enabled = current != null) {
                    Text(if (playerState.isPlaying) "Pause" else "Play")
                }
                Text(formatTime(if (dragging) (dragValue * length).toLong() else playerState.positionMs))
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
                    modifier = Modifier.weight(1f),
                )
                Text(formatTime(playerState.lengthMs))
            }
        }
    }
}
