package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import echo.music.iad1tya.R
import echo.music.iad1tya.ai.AiPlaylistGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateAiPlaylistDialog(
    onDismiss: () -> Unit,
    onPlaylistCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var prompt by remember { mutableStateOf("") }
    var numSongs by remember { mutableFloatStateOf(15f) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationLog by remember { mutableStateOf("Initializing...") }
    var errorLog by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (!isGenerating) {
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
                    text = "Create with AI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!isGenerating && errorLog == null) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("What kind of playlist do you want?") },
                        placeholder = { Text("e.g. upbeat workout pop songs") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Number of songs: ${numSongs.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = numSongs,
                            onValueChange = { numSongs = it },
                            valueRange = 5f..50f,
                            steps = 44
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (prompt.isNotBlank()) {
                                    isGenerating = true
                                    errorLog = null
                                    coroutineScope.launch {
                                        val newPlaylistId = AiPlaylistGenerator.generatePlaylist(
                                            context = context,
                                            userPrompt = prompt,
                                            numberOfSongs = numSongs.toInt(),
                                            onLog = { log ->
                                                withContext(Dispatchers.Main) {
                                                    generationLog = log
                                                }
                                            }
                                        )
                                        if (newPlaylistId != null) {
                                            onPlaylistCreated(newPlaylistId)
                                        } else {
                                            isGenerating = false
                                            errorLog = "Failed to generate playlist. Check logs or settings."
                                        }
                                    }
                                }
                            },
                            enabled = prompt.isNotBlank()
                        ) {
                            Text("Generate")
                        }
                    }
                } else if (isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = generationLog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (errorLog != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorLog!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Last Log: $generationLog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { errorLog = null }) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                }
            }
        }
    }
}
