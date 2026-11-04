package echo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import echo.desktop.core.YouTubeSetup
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun Heading(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))

@Composable
fun StatsScreen(history: List<SongItem>) {
    val minutes = history.sumOf { it.duration ?: 0 } / 60
    val topSongs = history.groupingBy { it.id }.eachCount().entries.sortedByDescending { it.value }.take(5)
    val byId = history.associateBy { it.id }
    val topArtists = history.flatMap { s -> s.artists.map { it.name } }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
    Panel(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Your stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${history.size} plays  •  ${history.distinctBy { it.id }.size} different songs  •  about $minutes minutes (full length of each song started)")
            Heading("Top songs")
            if (topSongs.isEmpty()) Text("Play something first.")
            topSongs.forEach { (id, n) -> Text("$n×  ${byId[id]?.title}") }
            Heading("Top artists")
            topArtists.forEach { (name, n) -> Text("$n×  $name") }
        }
    }
}

@Composable
fun SettingsScreen(playback: Playback, onShow: (String, List<SongItem>) -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    val accents = listOf("D0BCFF", "FF6B6B", "4ADE80", "60A5FA", "FBBF24", "F472B6")

    Panel(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Heading("Look")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(Settings.glass, { Settings.glass = it; Settings.save() })
                Text("  Glass theme")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accents.forEach { hex ->
                    Spacer(
                        Modifier.size(32.dp).clip(CircleShape).background(Color(("FF$hex").toLong(16)))
                            .clickable { Settings.accent = hex; Settings.save() }
                    )
                }
            }
            Text("Accent colour", style = MaterialTheme.typography.bodySmall)

            Heading("Audio quality")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(Settings.lowQuality, { Settings.lowQuality = it; Settings.save() })
                Text("  Low quality (about 50 kbit/s, less data). Off = best available. Applies to the next song.")
            }

            Heading("Discord status")
            OutlinedTextField(
                Settings.discordId, { Settings.discordId = it; Settings.save() }, singleLine = true,
                label = { Text("Discord application ID (discord.com/developers → New Application)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Heading("YouTube Music account")
            OutlinedTextField(
                Settings.cookie, { Settings.cookie = it; Settings.save() }, singleLine = true,
                label = { Text("Paste your music.youtube.com cookie") }, modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    status = "Syncing liked songs..."
                    scope.launch {
                        YouTube.cookie = Settings.cookie
                        withContext(Dispatchers.IO) { YouTubeSetup.ensureInit(); YouTube.playlist("LM").map { it.songs } }
                            .onSuccess { playback.like(it); status = "Synced ${it.size} liked songs." }
                            .onFailure { status = "Sync failed: ${it.message ?: it::class.simpleName}" }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Sync liked songs") }

            Heading("Import Spotify playlist")
            OutlinedTextField(
                link, { link = it }, singleLine = true,
                label = { Text("Public Spotify playlist link") }, modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    status = "Reading playlist..."
                    scope.launch {
                        runCatching {
                            val names = Spotify.tracks(link)
                            val songs = Spotify.toYouTube(names) { status = "Matching $it of ${names.size} on YouTube..." }
                            onShow("Imported from Spotify (${songs.size} of ${names.size} matched)", songs)
                            status = ""
                        }.onFailure { status = "Import failed: ${it.message ?: it::class.simpleName}" }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Import") }

            Text(status, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.primary)
        }
    }
}
