package iad1tya.echo.music.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iad1tya.echo.music.R
import iad1tya.echo.music.ai.AiPlaylistModifier
import iad1tya.echo.music.db.entities.PlaylistSong
import kotlinx.coroutines.launch

@Composable
fun AiModifyPlaylistDialog(
    playlistId: String,
    currentSongs: List<PlaylistSong>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var prompt by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.modify_with_ai),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!isProcessing) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text(stringResource(R.string.modify_with_ai_desc)) },
                        placeholder = { Text(stringResource(R.string.ai_modify_prompt_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = true,
                        minLines = 3
                    )
                    
                    if (logs.isNotEmpty()) {
                        Text(text = "Logs:", style = MaterialTheme.typography.bodyMedium)
                        val scrollState = rememberScrollState()
                        Text(
                            text = logs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(scrollState),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (prompt.isNotBlank()) {
                                    isProcessing = true
                                    logs = "Starting modification...\n"
                                    coroutineScope.launch {
                                        AiPlaylistModifier.modifyPlaylist(
                                            context = context,
                                            playlistId = playlistId,
                                            currentSongs = currentSongs,
                                            userPrompt = prompt,
                                            onLog = { newLog ->
                                                logs = newLog + "\n" + logs
                                            }
                                        )
                                        isProcessing = false
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = prompt.isNotBlank()
                        ) {
                            Text("Start")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        val latestLog = logs.split("\n").firstOrNull { it.isNotBlank() } ?: "Processing..."
                        Text(
                            text = latestLog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
