package iad1tya.echo.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.constants.LocalDownloadDirectoryKey
import iad1tya.echo.music.utils.LocalFileDownloader
import iad1tya.echo.music.utils.LocalYouTubeDownloader
import iad1tya.echo.music.utils.qobuz.QobuzApiClient
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadableFormat(
    val title: String,
    val subtitle: String,
    val url: String,
    val mimeType: String,
    val fileExtension: String,
    val youtubeFormatId: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDownloadBottomSheet(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var formats by remember { mutableStateOf<List<DownloadableFormat>>(emptyList()) }
    
    val (localDownloadDirectory) = rememberPreference(
        key = LocalDownloadDirectoryKey,
        defaultValue = ""
    )

    LaunchedEffect(mediaMetadata.id) {
        val videoId = mediaMetadata.id
        isLoading = true
        
        withContext(Dispatchers.IO) {
            val availableFormats = mutableListOf<DownloadableFormat>()
            
            // 2. Try Qobuz Lossless
            try {
                val qobuzClient = QobuzApiClient()
                val title = mediaMetadata.title
                val artist = mediaMetadata.artists.firstOrNull()?.name?.replace(" - Topic", "")
                
                if (title != null && artist != null) {
                    val searchResult = qobuzClient.search("$artist $title").tracks?.items?.firstOrNull()
                    if (searchResult != null) {
                        val trackId = searchResult.id
                        if (trackId != null) {
                            val url = qobuzClient.getFileUrl(trackId).url
                            if (url != null) {
                                availableFormats.add(
                                    DownloadableFormat(
                                        title = "FLAC Lossless",
                                        subtitle = "Qobuz",
                                        url = url,
                                        mimeType = "audio/flac",
                                        fileExtension = "flac"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore Qobuz failure
            }

            
            formats =
                availableFormats.sortedWith(
                    compareByDescending<DownloadableFormat> { it.title.contains("FLAC") }
                        .thenBy { it.youtubeFormatId == null }
                )
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Download Format",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (formats.isEmpty()) {
                Text("No downloadable formats found.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(formats) { format ->
                        ListItem(
                            headlineContent = { Text(format.title) },
                            supportingContent = { Text(format.subtitle) },
                            modifier = Modifier.clickable {
                                if (localDownloadDirectory.isEmpty()) {
                                    Toast.makeText(context, "Please configure Download Destination in Settings first.", Toast.LENGTH_LONG).show()
                                } else {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val cleanTitle = mediaMetadata.title.replace(Regex("[\\\\/:*?\"<>|]"), "")
                                        val cleanArtist = mediaMetadata.artists.firstOrNull()?.name?.replace(Regex("[\\\\/:*?\"<>|]"), "") ?: "Unknown"
                                        val fileName = "$cleanTitle - $cleanArtist.${format.fileExtension}"

                                        LocalFileDownloader.download(
                                            context = context,
                                            url = format.url,
                                            destinationDirUriString = localDownloadDirectory,
                                            fileName = fileName,
                                            mimeType = format.mimeType,
                                        )
                                    }
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
