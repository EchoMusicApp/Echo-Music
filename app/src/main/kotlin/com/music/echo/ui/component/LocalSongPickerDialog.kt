package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R
import echo.music.iad1tya.db.entities.Playlist
import echo.music.iad1tya.db.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSongPickerDialog(
    playlist: Playlist,
    onDismiss: () -> Unit
) {
    val database = LocalDatabase.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val localSongs by database.localSongs().collectAsState(initial = emptyList())
    val selectedSongs = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add Local Songs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        if (selectedSongs.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.addSongToPlaylist(playlist, selectedSongs.toList())
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Added ${selectedSongs.size} songs", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        }
                    },
                    enabled = selectedSongs.isNotEmpty()
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
            
            if (localSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No local songs found")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(localSongs, key = { it.id }) { song ->
                        val isSelected = selectedSongs.contains(song.song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedSongs.remove(song.song.id) else selectedSongs.add(song.song.id)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedSongs.add(song.song.id) else selectedSongs.remove(song.song.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = song.song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (song.artists.isNotEmpty()) song.artists.joinToString { it.name } else "Unknown Artist",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
