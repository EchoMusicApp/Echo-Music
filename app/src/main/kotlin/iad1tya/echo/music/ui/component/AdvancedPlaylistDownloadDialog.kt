package iad1tya.echo.music.ui.component

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import com.echo.innertube.YouTube
import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.utils.completed
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdvancedPlaylistDownloadDialog(
    songs: List<MediaMetadata>,
    playlistId: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var songsToDownload by remember { mutableStateOf(songs) }
    var isFetchingSongs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (songsToDownload.isEmpty() && playlistId != null) {
            isFetchingSongs = true
            withContext(Dispatchers.IO) {
                try {
                    val result = YouTube.playlist(playlistId).completed().getOrNull()
                    val fetchedSongs = result?.songs?.map { it.toMediaMetadata() }
                    withContext(Dispatchers.Main) {
                        if (fetchedSongs != null) {
                            songsToDownload = fetchedSongs
                        } else {
                             Toast.makeText(context, "Failed to fetch playlist songs", Toast.LENGTH_SHORT).show()
                             onDismiss()
                        }
                        isFetchingSongs = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error fetching playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            }
        }
    }
    
    var downloadLocation by remember { mutableStateOf<Uri?>(null) }
    var defaultDownloadPath by remember { mutableStateOf("Downloads/EchoMusic") }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(DownloadType.Audio) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            downloadLocation = uri
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            defaultDownloadPath = documentFile?.name ?: "Selected Folder"
        }
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = {
            if (!isDownloading && !isFetchingSongs) onDismiss()
        },
        title = {
            Text(
                text = "Advance Playlist Download",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isFetchingSongs) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                         Text("Fetching playlist info...")
                    }
                } else {
                    Text(
                        text = "${songsToDownload.size} songs selected",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .padding(vertical = 8.dp),
                        )
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Download Location
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { folderPickerLauncher.launch(null) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.storage),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Save to",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = defaultDownloadPath,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Format",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFormat == DownloadType.Audio,
                                onClick = { selectedFormat = DownloadType.Audio },
                                label = "Audio (m4a/webm)",
                                icon = R.drawable.music_note
                            )
                            FilterChip(
                                selected = selectedFormat == DownloadType.Video,
                                onClick = { selectedFormat = DownloadType.Video },
                                label = "Video (High)",
                                icon = R.drawable.video
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isDownloading && !isFetchingSongs && songsToDownload.isNotEmpty()) {
                TextButton(
                    onClick = {
                        isDownloading = true
                        coroutineScope.launch(Dispatchers.IO) {
                            var successCount = 0
                            songsToDownload.forEachIndexed { index, song ->
                                progress = (index.toFloat() / songsToDownload.size.toFloat())
                                withContext(Dispatchers.Main) {
                                    progressText = "Fetching ${index + 1}/${songsToDownload.size}: ${song.title}"
                                }

                                try {
                                    // Use ANDROID_VR_NO_AUTH to avoid authentication issues when logged in
                                    val result = YouTube.player(song.id, client = YouTubeClient.ANDROID_VR_NO_AUTH)
                                    val playerResponse = result.getOrNull()
                                    val formats = playerResponse?.streamingData?.adaptiveFormats

                                    val url = if (selectedFormat == DownloadType.Audio) {
                                        formats?.filter { it.mimeType.startsWith("audio/mp4") }
                                            ?.maxByOrNull { it.bitrate ?: 0 }?.url
                                    } else {
                                        formats?.filter { it.mimeType.startsWith("video/mp4") }
                                            ?.maxByOrNull { it.height ?: 0 }?.url
                                    }

                                    if (!url.isNullOrEmpty()) {
                                        val extension = if (selectedFormat == DownloadType.Audio) "m4a" else "mp4"
                                        val fileName = "${song.title.replace("/", "_")}.$extension"
                                        
                                        withContext(Dispatchers.Main) {
                                            downloadFile(context, url, fileName, downloadLocation)
                                        }
                                        successCount++
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            withContext(Dispatchers.Main) {
                                progress = 1f
                                progressText = "Done! queued $successCount downloads."
                                Toast.makeText(context, "Queued $successCount downloads", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text("Start Download")
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class DownloadType {
    Audio,
    Video
}
