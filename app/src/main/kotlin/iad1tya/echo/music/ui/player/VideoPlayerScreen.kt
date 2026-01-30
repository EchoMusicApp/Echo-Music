package iad1tya.echo.music.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.echo.innertube.YouTube
import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.models.response.PlayerResponse
import iad1tya.echo.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    
    // Force landscape orientation and enable fullscreen immersive mode
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        onDispose {
            // Restore original orientation
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoId) {
        isLoading = true
        errorMessage = null
        
        try {
            // Fetch video stream URL from YouTube using parallel requests for faster loading
            withContext(Dispatchers.IO) {
                // Try first 3 most reliable clients in parallel for faster response
                val priorityClients = listOf(
                    YouTubeClient.ANDROID_VR_NO_AUTH,
                    YouTubeClient.IOS,
                    YouTubeClient.WEB
                )
                
                Timber.d("VideoPlayer: Starting parallel requests for videoId: $videoId")
                
                // Launch parallel requests to all priority clients
                val deferredResults = priorityClients.map { client ->
                    async {
                        try {
                            Timber.d("VideoPlayer: Requesting from ${client.clientName}")
                            val result = YouTube.player(videoId, client = client)
                            client to result
                        } catch (e: Exception) {
                            Timber.e("VideoPlayer: Error with ${client.clientName}: ${e.message}")
                            client to Result.failure<PlayerResponse>(e)
                        }
                    }
                }
                
                // Wait for all requests to complete
                val results = deferredResults.awaitAll()
                
                var bestUrl: String? = null
                var lastError: String? = null
                
                // Process results in order of priority
                for ((client, result) in results) {
                    result.onSuccess { playerResponse ->
                        Timber.d("VideoPlayer: Got response from ${client.clientName}, status: ${playerResponse.playabilityStatus.status}")
                        
                        if (playerResponse.playabilityStatus.status != "OK") {
                            lastError = playerResponse.playabilityStatus.reason ?: "Playback not available"
                            Timber.d("VideoPlayer: Playability status not OK: $lastError")
                            return@onSuccess
                        }
                        
                        // Get the best video stream
                        val streamingData = playerResponse.streamingData
                        if (streamingData == null) {
                            Timber.d("VideoPlayer: No streaming data from ${client.clientName}")
                            lastError = "No streaming data available"
                            return@onSuccess
                        }
                        
                        val formats = streamingData.formats ?: emptyList()
                        val adaptiveFormats = streamingData.adaptiveFormats ?: emptyList()
                        
                        Timber.d("VideoPlayer: Found ${formats.size} regular formats and ${adaptiveFormats.size} adaptive formats")
                        
                        // Prefer formats with both video and audio (regular formats)
                        val bestFormat = formats.firstOrNull { format ->
                            format.mimeType?.contains("video") == true && 
                            format.url != null &&
                            !format.url.isNullOrEmpty()
                        } ?: adaptiveFormats.firstOrNull { format ->
                            format.mimeType?.contains("video") == true && 
                            format.url != null &&
                            !format.url.isNullOrEmpty()
                        }
                        
                        if (bestFormat != null && bestFormat.url != null) {
                            bestUrl = bestFormat.url
                            Timber.d("VideoPlayer: Found format with ${client.clientName}: ${bestFormat.mimeType}, quality: ${bestFormat.qualityLabel}")
                        } else {
                            Timber.d("VideoPlayer: No valid format found with ${client.clientName}")
                            lastError = "No playable format found"
                        }
                    }.onFailure { error ->
                        lastError = error.message ?: "Failed to load video"
                        Timber.e("VideoPlayer: Error with ${client.clientName}: $lastError")
                    }
                    
                    // If we found a working URL, stop trying other clients
                    if (bestUrl != null) {
                        Timber.d("VideoPlayer: Successfully got stream URL from ${client.clientName}")
                        break
                    }
                }
                
                // If parallel requests failed, try fallback clients sequentially
                if (bestUrl == null) {
                    Timber.d("VideoPlayer: Trying fallback clients")
                    val fallbackClients = listOf(
                        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                        YouTubeClient.ANDROID_VR_1_61_48
                    )
                    
                    for (client in fallbackClients) {
                        Timber.d("VideoPlayer: Trying fallback client ${client.clientName}")
                        
                        val result = YouTube.player(videoId, client = client)
                        result.onSuccess { playerResponse ->
                            if (playerResponse.playabilityStatus.status == "OK") {
                                val streamingData = playerResponse.streamingData
                                if (streamingData != null) {
                                    val formats = streamingData.formats ?: emptyList()
                                    val adaptiveFormats = streamingData.adaptiveFormats ?: emptyList()
                                    
                                    val bestFormat = formats.firstOrNull { format ->
                                        format.mimeType?.contains("video") == true && 
                                        format.url != null &&
                                        !format.url.isNullOrEmpty()
                                    } ?: adaptiveFormats.firstOrNull { format ->
                                        format.mimeType?.contains("video") == true && 
                                        format.url != null &&
                                        !format.url.isNullOrEmpty()
                                    }
                                    
                                    if (bestFormat != null && bestFormat.url != null) {
                                        bestUrl = bestFormat.url
                                        Timber.d("VideoPlayer: Found format with fallback ${client.clientName}")
                                    }
                                }
                            }
                        }
                        
                        if (bestUrl != null) break
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (bestUrl != null) {
                        videoUrl = bestUrl
                        val mediaItem = MediaItem.fromUri(bestUrl)
                        val dataSourceFactory = DefaultHttpDataSource.Factory()
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .setConnectTimeoutMs(10000)
                            .setReadTimeoutMs(10000)
                        
                        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
                        
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()
                        isLoading = false
                        Timber.d("VideoPlayer: Player prepared and ready")
                    } else {
                        errorMessage = lastError ?: "No video stream found"
                        isLoading = false
                        Timber.e("VideoPlayer: Failed to get stream URL: $errorMessage")
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error loading video"
            isLoading = false
            Timber.e(e, "VideoPlayer: Exception while loading video")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = "Loading video...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { navController.navigateUp() }
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
