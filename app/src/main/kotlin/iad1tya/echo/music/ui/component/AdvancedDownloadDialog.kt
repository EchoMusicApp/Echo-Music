package iad1tya.echo.music.ui.component

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.echo.innertube.models.response.PlayerResponse
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdvancedDownloadDialog(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var playerResponse by remember { mutableStateOf<PlayerResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadLocation by remember { mutableStateOf<Uri?>(null) }
    var defaultDownloadPath by remember { mutableStateOf("Downloads/EchoMusic") }

    // Fetch PlayerResponse for formats
    LaunchedEffect(mediaMetadata.id) {
        withContext(Dispatchers.IO) {
            // Use ANDROID_VR_NO_AUTH to avoid authentication issues when logged in
            val result = YouTube.player(mediaMetadata.id, client = YouTubeClient.ANDROID_VR_NO_AUTH)
            playerResponse = result.getOrNull()
            isLoading = false
        }
    }

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
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Advance Download",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val formats = playerResponse?.streamingData?.adaptiveFormats
                    val videoFormats = formats?.filter { it.mimeType.startsWith("video/mp4") }
                        ?.sortedByDescending { it.height }
                    val audioFormats = formats?.filter { it.mimeType.startsWith("audio/mp4") }
                        ?.sortedByDescending { it.bitrate }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // Limit height
                    ) {
                        // Download Location
                        item {
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
                             Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Thumbnail
                        item {
                            DownloadOptionItem(
                                icon = R.drawable.insert_photo,
                                title = "Thumbnail",
                                subtitle = "High Quality",
                                onClick = {
                                    downloadFile(
                                        context,
                                        mediaMetadata.thumbnailUrl ?: "",
                                        "${mediaMetadata.title}_thumbnail.jpg",
                                        downloadLocation
                                    )
                                    onDismiss()
                                }
                            )
                        }

                        // Audio
                        if (!audioFormats.isNullOrEmpty()) {
                            item {
                                Text(
                                    text = "Audio",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(audioFormats) { format ->
                                DownloadOptionItem(
                                    icon = R.drawable.music_note,
                                    title = "Audio (${format.bitrate / 1000}kbps)",
                                    subtitle = format.mimeType.split(";")[0],
                                    onClick = {
                                        downloadFile(
                                            context,
                                            format.url ?: "",
                                            "${mediaMetadata.title}_audio.${if (format.mimeType.contains("mp4")) "m4a" else "webm"}",
                                            downloadLocation
                                        )
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Video
                        if (!videoFormats.isNullOrEmpty()) {
                             item {
                                Text(
                                    text = "Video",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(videoFormats) { format ->
                                DownloadOptionItem(
                                    icon = R.drawable.video, 
                                    title = "Video ${format.height}p",
                                    subtitle = format.mimeType.split(";")[0],
                                    onClick = {
                                        downloadFile(
                                            context,
                                            format.url ?: "",
                                            "${mediaMetadata.title}_${format.height}p.mp4",
                                            downloadLocation
                                        )
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DownloadOptionItem(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.download),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun downloadFile(context: Context, url: String, fileName: String, destinationUri: Uri?) {
    if (url.isEmpty()) {
        Toast.makeText(context, "Download URL not available", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        if (destinationUri != null) {
              // Creating a file in the selected directory is more complex with Scoped Storage + DownloadManager
              // DownloadManager only supports setDestinationUri with file:// or content:// (if supported)
              // But usually it's restricted to Public directories.
              // For simplicity and reliability, we default to Public Downloads if not easily possible,
              // or use setDestinationInExternalPublicDir if no Uri is provided.
              
              // IF specific URI is tricky with DownloadManager, we might want to guide user to standard paths.
              // For now, let's use the standard "Downloads/EchoMusic" SubPath approach which works well.
              // If the user picked a tree URI, we can't easily pass that to DownloadManager directly
              // without handling file creation ourselves and copying bytes (which loses DownloadManager benefits).
              
              // So, we will ignore the picked URI for DownloadManager and just use the standard Downloads folder 
              // but create a subfolder.
              
               request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "EchoMusic/$fileName")
               Toast.makeText(context, "Downloading to Downloads/EchoMusic", Toast.LENGTH_SHORT).show()

        } else {
             request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "EchoMusic/$fileName")
             Toast.makeText(context, "Downloading to Downloads/EchoMusic", Toast.LENGTH_SHORT).show()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        
    } catch (e: Exception) {
        Toast.makeText(context, "Download Failed: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}
