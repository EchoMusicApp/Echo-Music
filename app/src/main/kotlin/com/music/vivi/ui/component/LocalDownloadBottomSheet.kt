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
import iad1tya.echo.music.utils.YTPlayerUtils
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
    val fileExtension: String
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
            
            val playerResponse = com.music.innertube.YouTube.player(videoId, client = com.music.innertube.models.YouTubeClient.IOS).getOrNull()
            val newPipeUrls = runCatching { com.music.innertube.YouTube.getNewPipeStreamUrls(videoId) }.getOrDefault(emptyList())
            
            if (playerResponse != null) {
                val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats?.filter { it.mimeType.startsWith("audio/") } ?: emptyList()
                for (format in adaptiveFormats) {
                    val resolvedUrl = newPipeUrls.find { it.first == format.itag }?.second ?: format.url ?: YTPlayerUtils.findUrlOrNull(format, videoId, playerResponse)
                    if (resolvedUrl != null) {
                        val isOpus = format.mimeType.contains("opus", ignoreCase = true)
                        val isMp4 = format.mimeType.contains("mp4", ignoreCase = true)
                        
                        val name = if (isOpus) "Opus" else if (isMp4) "M4A" else "Audio"
                        val ext = if (isOpus) "opus" else if (isMp4) "m4a" else "webm"
                        val bitrate = format.bitrate / 1000
                        
                        availableFormats.add(
                            DownloadableFormat(
                                title = "$name (${bitrate}kbps)",
                                subtitle = "YouTube Music",
                                url = resolvedUrl,
                                mimeType = format.mimeType.substringBefore(";"),
                                fileExtension = ext
                            )
                        )
                    }
                }
            }

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
            
            formats = availableFormats.sortedByDescending { 
                if (it.title.contains("FLAC")) 1000000 else it.title.filter { it.isDigit() }.toIntOrNull() ?: 0 
            }
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
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        var downloadUrl = format.url
                                        var finalUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        val videoId = mediaMetadata.id
                                        
                                        if ("youtube" in format.url) {
                                            try {
                                                val mainClient = com.music.innertube.models.YouTubeClient.ANDROID_VR_1_43_32
                                                val mainResponse = com.music.innertube.YouTube.player(videoId, client = mainClient).getOrNull()
                                                val itag = android.net.Uri.parse(format.url).getQueryParameter("itag")?.toIntOrNull()
                                                val targetFormat = if (itag != null) {
                                                    mainResponse?.streamingData?.adaptiveFormats?.find { it.itag == itag } ?: mainResponse?.streamingData?.formats?.find { it.itag == itag }
                                                } else null
                                                
                                                if (targetFormat != null && mainResponse != null) {
                                                    val foundUrl = iad1tya.echo.music.utils.YTPlayerUtils.findUrlOrNull(targetFormat, videoId, mainResponse)
                                                    if (foundUrl != null) {
                                                        var transformed = foundUrl
                                                        try {
                                                            transformed = iad1tya.echo.music.utils.sabr.EjsNTransformSolver.transformNParamInUrl(foundUrl)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        
                                                        val isLoggedIn = com.music.innertube.YouTube.cookie != null
                                                        val sessionId = if (isLoggedIn) com.music.innertube.YouTube.dataSyncId else com.music.innertube.YouTube.visitorData
                                                        if (sessionId != null) {
                                                            val pot = iad1tya.echo.music.utils.potoken.PoTokenGenerator().getWebClientPoToken(videoId, sessionId)?.streamingDataPoToken
                                                            if (pot != null) {
                                                                val sep = if ("?" in transformed) "&" else "?"
                                                                transformed = "${transformed}${sep}pot=$pot"
                                                            }
                                                        }
                                                        downloadUrl = transformed
                                                        finalUserAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }

                                        val cleanTitle = mediaMetadata.title.replace(Regex("[\\\\/:*?\"<>|]"), "")
                                        val cleanArtist = mediaMetadata.artists.firstOrNull()?.name?.replace(Regex("[\\\\/:*?\"<>|]"), "") ?: "Unknown"
                                        val fileName = "$cleanTitle - $cleanArtist.${format.fileExtension}"
                                        
                                        LocalFileDownloader.download(
                                            context = context,
                                            url = downloadUrl,
                                            destinationDirUriString = localDownloadDirectory,
                                            fileName = fileName,
                                            mimeType = format.mimeType,
                                            userAgent = finalUserAgent
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
