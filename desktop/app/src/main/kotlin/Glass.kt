package echo.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Floating, translucent rounded bar: the "liquid glass" look of the phone app. */
@Composable
fun GlassPill(modifier: Modifier, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier, shape = RoundedCornerShape(50), color = cs.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.14f)), shadowElevation = 14.dp, content = content,
    )
}

@Composable
fun MiniPlayer(playback: Playback, state: PlayerState, player: VlcPlayer, onOpen: () -> Unit) {
    val song = playback.current ?: return
    val failed = state.error != null || playback.info.startsWith("No playable") || playback.info.contains("failed")
    GlassPill(Modifier.widthIn(max = 760.dp).fillMaxWidth().height(78.dp)) {
        Column {
            Row(Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(song.thumbnail, Modifier.size(52.dp), CircleShape)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    Text(
                        if (failed) (state.error ?: playback.info) else song.artists.joinToString { it.name },
                        maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { playback.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                FilledIconButton(onClick = { player.togglePause() }, modifier = Modifier.size(46.dp)) {
                    Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause")
                }
                IconButton(onClick = { playback.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
            }
            LinearProgressIndicator(
                progress = { (state.positionMs.toFloat() / state.lengthMs.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).padding(bottom = 6.dp).height(3.dp),
            )
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 18.dp else 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label)
        if (selected) Text(label, Modifier.padding(start = 8.dp), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun NavPill(view: View, onNav: (View) -> Unit) {
    var more by remember { mutableStateOf(false) }
    GlassPill(Modifier.height(64.dp)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavItem(Icons.Filled.Home, "Home", view == View.Home) { onNav(View.Home) }
            NavItem(Icons.Filled.Search, "Search", view == View.Search) { onNav(View.Search) }
            NavItem(Icons.Filled.LibraryMusic, "Library", view == View.Liked) { onNav(View.Liked) }
            NavItem(Icons.Filled.Groups, "Community", view == View.Community || view == View.Artist) { onNav(View.Community) }
            Box {
                NavItem(Icons.Filled.MoreHoriz, "More", view == View.History || view == View.Stats || view == View.Settings) { more = true }
                DropdownMenu(more, { more = false }) {
                    listOf("History" to View.History, "Stats" to View.Stats, "Settings" to View.Settings).forEach { (label, v) ->
                        DropdownMenuItem({ Text(label) }, { more = false; onNav(v) })
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSearchField(query: String, onQuery: (String) -> Unit, onSearch: () -> Unit) {
    TextField(
        value = query, onValueChange = onQuery, singleLine = true, shape = RoundedCornerShape(50),
        placeholder = { Text("What do you want to play?") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).onKeyEvent {
            if (it.key == Key.Enter && it.type == KeyEventType.KeyDown) { onSearch(); true } else false
        },
    )
}

/** Full-screen player page, tinted with the cover's colour like the phone app. */
@Composable
fun FullPlayer(
    playback: Playback, state: PlayerState, player: VlcPlayer, lyrics: List<LyricLine>?,
    volume: Float, onVolume: (Float) -> Unit, onClose: () -> Unit,
) {
    val song = playback.current ?: return
    val cs = MaterialTheme.colorScheme
    val cover = rememberCoverColor(hires(song.thumbnail), cs.primary)
    val brush = Brush.verticalGradient(listOf(lerp(cover, cs.background, 0.4f), cs.background))
    val openArtist = LocalOpenArtist.current
    var showLyrics by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val length = state.lengthMs.coerceAtLeast(1)
    val shown = if (dragging) dragValue else state.positionMs.toFloat() / length
    val idle = cs.onSurfaceVariant

    BoxWithConstraints(Modifier.fillMaxSize().background(brush)) {
        val wide = maxWidth > 900.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, "Close player", Modifier.size(32.dp)) }
                Text("Now Playing", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!wide) TextButton(onClick = { showLyrics = !showLyrics }) { Text(if (showLyrics) "Cover" else "Lyrics") }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                if (wide || !showLyrics) {
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        RemoteImage(hires(song.thumbnail), Modifier.fillMaxWidth(0.8f).widthIn(max = 420.dp).aspectRatio(1f), RoundedCornerShape(28.dp))
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(0.8f).widthIn(max = 420.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.artists.joinToString { it.name }, color = idle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onClose(); openArtist(song.artists.firstOrNull()?.id) },
                                )
                            }
                            val liked = playback.isLiked(song)
                            IconButton(onClick = { playback.toggleLike(song) }) {
                                Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Like", tint = if (liked) cs.primary else idle)
                            }
                        }
                    }
                }
                if (wide || showLyrics) {
                    Column(Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp)) {
                        if (wide) Text("Lyrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        LyricsView(lyrics, state.positionMs, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().align(Alignment.CenterHorizontally)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(if (dragging) (dragValue * length).toLong() else state.positionMs), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = shown.coerceIn(0f, 1f),
                        onValueChange = { dragging = true; dragValue = it },
                        onValueChangeFinished = { player.seekTo((dragValue * length).toLong()); dragging = false },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(formatTime(state.lengthMs), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playback.shuffle = !playback.shuffle }) { Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (playback.shuffle) cs.primary else idle) }
                    IconButton(onClick = { playback.previous() }, Modifier.size(56.dp)) { Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(32.dp)) }
                    FilledIconButton(onClick = { player.togglePause() }, modifier = Modifier.padding(horizontal = 12.dp).size(72.dp)) {
                        Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", Modifier.size(38.dp))
                    }
                    IconButton(onClick = { playback.next() }, Modifier.size(56.dp)) { Icon(Icons.Filled.SkipNext, "Next", Modifier.size(32.dp)) }
                    IconButton(onClick = { playback.repeat = when (playback.repeat) { Repeat.Off -> Repeat.All; Repeat.All -> Repeat.One; Repeat.One -> Repeat.Off } }) {
                        Icon(if (playback.repeat == Repeat.One) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repeat", tint = if (playback.repeat == Repeat.Off) idle else cs.primary)
                    }
                }
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, "Volume", tint = idle)
                    Slider(volume, onVolume, Modifier.width(160.dp).padding(start = 8.dp))
                }
                val info = state.error ?: playback.info
                if (info.isNotBlank()) {
                    Text(info, style = MaterialTheme.typography.bodySmall, color = if (state.error != null) cs.error else idle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
