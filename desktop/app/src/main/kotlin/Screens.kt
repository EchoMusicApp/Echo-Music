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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
private fun Card(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) =
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

@Composable
fun StatsScreen(history: List<SongItem>, plays: List<Play>) {
    val seconds = history.sumOf { it.duration ?: 0 }
    val topSongs = history.groupingBy { it.id }.eachCount().entries.sortedByDescending { it.value }.take(6)
    val byId = history.associateBy { it.id }
    val artistCounts = history.flatMap { s -> s.artists.map { it.name } }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
    val top = artistCounts.take(6).map { it.key to it.value }
    val other = artistCounts.drop(6).sumOf { it.value }
    val pie = if (other > 0) top + ("Others" to other) else top

    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val dates = plays.map { java.time.Instant.ofEpochMilli(it.ms).atZone(zone) }
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val perDay = days.map { d -> dates.count { it.toLocalDate() == d } }
    val dayLabels = days.map { it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }
    val perHour = (0..23).map { h -> dates.count { it.hour == h } }
    val hourLabels = (0..23).map { if (it % 3 == 0) "$it" else "" }
    val accent = MaterialTheme.colorScheme.primary

    Panel(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your stats", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (history.isEmpty()) {
                Text("Nothing here yet. Play a few songs and your charts will fill in.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Tile("${history.size}", "songs played", Modifier.weight(1f))
                Tile("${history.distinctBy { it.id }.size}", "different songs", Modifier.weight(1f))
                Tile("%.1f h".format(seconds / 3600.0), "of music started", Modifier.weight(1f))
                Tile("${artistCounts.size}", "artists", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card("Top artists", Modifier.weight(1f)) { PieChart(pie) }
                Card("Plays, last 7 days", Modifier.weight(1f)) { BarChart(dayLabels, perDay, accent) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card("Top songs", Modifier.weight(1f)) { RankBars(topSongs.map { (byId[it.key]?.title ?: "?") to it.value }, accent) }
                Card("When you listen (hour of day)", Modifier.weight(1f)) { BarChart(hourLabels, perHour, ChartColors[1]) }
            }
            Text("Day and hour charts count plays since v8; the others use your full history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsScreen(playback: Playback, onShow: (String, List<SongItem>) -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    val accents = listOf("ED5564", "D0BCFF", "FF6B6B", "4ADE80", "60A5FA", "FBBF24", "F472B6")

    Panel(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 190.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Heading("Look")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                listOf("auto" to "Auto (follow Windows)", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                    FilterChip(Settings.themeMode == mode, { Settings.themeMode = mode; Settings.save() }, { Text(label) })
                }
            }
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

            Heading("Suggestions from Spotify")
            OutlinedTextField(
                Settings.tasteLink, { Settings.tasteLink = it; Settings.save() }, singleLine = true,
                label = { Text("Public Spotify playlist link (e.g. your Liked mix). Spotify usernames can't be read without a login.") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Home shelves use its artists the next time you press the Home button.", style = MaterialTheme.typography.bodySmall)

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
