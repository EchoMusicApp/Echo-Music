package iad1tya.echo.music.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import com.echo.innertube.YouTube
import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.models.response.PlayerResponse
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.theme.EchoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.abs

@UnstableApi
class VideoPlayerActivity : ComponentActivity() {
    private var exoPlayer: ExoPlayer? = null
    
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force landscape and fullscreen
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        val startPosition = intent.getLongExtra("START_POSITION", 0L)
        
        setContent {
            EchoTheme {
                VideoPlayerContent(
                    videoId = videoId,
                    startPosition = startPosition,
                    onBack = { finish() }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
    
    data class VideoQuality(
        val label: String,
        val url: String,
        val audioUrl: String? = null,
        val height: Int?,
        val bitrate: Int?
    )
    
    @Composable
    private fun VideoPlayerContent(
        videoId: String,
        startPosition: Long = 0L,
        onBack: () -> Unit
    ) {
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var availableQualities by remember { mutableStateOf<List<VideoQuality>>(emptyList()) }
        var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }
        var showQualityMenu by remember { mutableStateOf(false) }
        var showControls by remember { mutableStateOf(true) }
        var videoScale by remember { mutableFloatStateOf(1f) }
        var isPlaying by remember { mutableStateOf(true) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        
        exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
        }
        
        // Update playback state
        LaunchedEffect(exoPlayer) {
            while (true) {
                exoPlayer?.let { player ->
                    isPlaying = player.isPlaying
                    currentPosition = player.currentPosition
                    duration = player.duration.takeIf { it > 0 } ?: 0L
                }
                kotlinx.coroutines.delay(100)
            }
        }
        
        DisposableEffect(Unit) {
            onDispose {
                exoPlayer?.release()
            }
        }
        
        // Hide controls after 3 seconds
        LaunchedEffect(showControls) {
            if (showControls) {
                kotlinx.coroutines.delay(3000)
                showControls = false
            }
        }
        
        // Function to load/switch video quality
        fun loadVideo(videoUrl: String, audioUrl: String? = null, seekToPosition: Long? = null) {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)
            
            val mediaSource = if (audioUrl != null) {
                // High quality: merge separate video and audio streams
                val videoMediaItem = MediaItem.fromUri(videoUrl)
                val audioMediaItem = MediaItem.fromUri(audioUrl)
                
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(videoMediaItem)
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(audioMediaItem)
                
                MergingMediaSource(videoSource, audioSource)
            } else {
                // Combined format (video+audio in one stream)
                val mediaItem = MediaItem.fromUri(videoUrl)
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            
            // Save playback position (use seekToPosition if provided, otherwise use current position)
            val position = seekToPosition ?: (exoPlayer?.currentPosition ?: 0)
            val isPlaying = exoPlayer?.isPlaying ?: false
            
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            if (position > 0) {
                exoPlayer?.seekTo(position)
            }
            if (isPlaying) {
                exoPlayer?.play()
            }
        }
        
        // Load video with all available qualities
        LaunchedEffect(videoId) {
            isLoading = true
            errorMessage = null
            
            try {
                withContext(Dispatchers.IO) {
                    val priorityClients = listOf(
                        YouTubeClient.ANDROID_VR_NO_AUTH,
                        YouTubeClient.IOS,
                        YouTubeClient.WEB
                    )
                    
                    Timber.d("VideoPlayer: Starting parallel requests for videoId: $videoId")
                    
                    val deferredResults = priorityClients.map { client ->
                        async {
                            try {
                                Timber.d("VideoPlayer: Requesting from ${client.clientName}")
                                val result = YouTube.player(videoId, client = client)
                                client to result
                            } catch (e: Exception) {
                                Timber.e("VideoPlayer: Error with ${client.clientName}: ${e.message}")
                                client to Result.failure(e)
                            }
                        }
                    }
                    
                    val results = deferredResults.awaitAll()
                    val qualities = mutableListOf<VideoQuality>()
                    var lastError: String? = null
                    var bestAudioUrl: String? = null
                    
                    for ((client, result) in results) {
                        result.onSuccess { playerResponse ->
                            if (playerResponse.playabilityStatus.status == "OK") {
                                val streamingData = playerResponse.streamingData
                                if (streamingData != null) {
                                    val formats = streamingData.formats ?: emptyList()
                                    val adaptiveFormats = streamingData.adaptiveFormats ?: emptyList()
                                    
                                    // Find best audio stream for merging with video
                                    val audioFormat = adaptiveFormats
                                        .filter { it.mimeType?.contains("audio") == true && it.url != null }
                                        .maxByOrNull { it.bitrate ?: 0 }
                                    bestAudioUrl = audioFormat?.url
                                    
                                    // First, try combined formats (video + audio) - these work without merging
                                    formats.forEach { format ->
                                        if (format.mimeType?.contains("video") == true && 
                                            format.url != null && 
                                            !format.url.isNullOrEmpty()) {
                                            
                                            val url = format.url!!
                                            val qualityLabel = format.qualityLabel ?: 
                                                format.height?.let { "${it}p" } ?: 
                                                "Unknown"
                                            
                                            qualities.add(
                                                VideoQuality(
                                                    label = qualityLabel,
                                                    url = url,
                                                    audioUrl = null, // Combined format doesn't need separate audio
                                                    height = format.height,
                                                    bitrate = format.bitrate
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Add adaptive (high quality) video formats with audio merging
                                    adaptiveFormats.forEach { format ->
                                        if (format.mimeType?.contains("video") == true && 
                                            format.url != null && 
                                            !format.url.isNullOrEmpty() &&
                                            bestAudioUrl != null) {
                                            
                                            val url = format.url!!
                                            val qualityLabel = format.qualityLabel ?: 
                                                format.height?.let { "${it}p" } ?: 
                                                "Unknown"
                                            
                                            qualities.add(
                                                VideoQuality(
                                                    label = qualityLabel,
                                                    url = url,
                                                    audioUrl = bestAudioUrl, // Will merge video+audio
                                                    height = format.height,
                                                    bitrate = format.bitrate
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                lastError = playerResponse.playabilityStatus.reason
                            }
                        }.onFailure { error ->
                            lastError = error.message
                        }
                        
                        if (qualities.isNotEmpty()) break
                    }
                    
                    // Try fallback clients if needed
                    if (qualities.isEmpty()) {
                        val fallbackClients = listOf(
                            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                            YouTubeClient.ANDROID_VR_1_61_48
                        )
                        
                        for (client in fallbackClients) {
                            val result = YouTube.player(videoId, client = client)
                            result.onSuccess { playerResponse ->
                                if (playerResponse.playabilityStatus.status == "OK") {
                                    val streamingData = playerResponse.streamingData
                                    if (streamingData != null) {
                                        // Prioritize combined formats (video + audio)
                                        val formats = streamingData.formats ?: emptyList()
                                        val adaptiveFormats = streamingData.adaptiveFormats ?: emptyList()
                                        
                                        // First try combined formats
                                        formats.forEach { format ->
                                            if (format.mimeType?.contains("video") == true && format.url != null) {
                                                val url = format.url!!
                                                qualities.add(
                                                    VideoQuality(
                                                        label = format.qualityLabel ?: "Unknown",
                                                        url = url,
                                                        height = format.height,
                                                        bitrate = format.bitrate
                                                    )
                                                )
                                            }
                                        }
                                        
                                        // Only use adaptive if no combined formats
                                        if (qualities.isEmpty()) {
                                            adaptiveFormats.forEach { format ->
                                                if (format.mimeType?.contains("video") == true && format.url != null) {
                                                    val url = format.url!!
                                                    qualities.add(
                                                        VideoQuality(
                                                            label = format.qualityLabel ?: "Unknown",
                                                            url = url,
                                                            height = format.height,
                                                            bitrate = format.bitrate
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (qualities.isNotEmpty()) break
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (qualities.isNotEmpty()) {
                            // Sort by height descending and remove duplicates
                            val sortedQualities = qualities
                                .distinctBy { it.height }
                                .sortedByDescending { it.height }
                            
                            availableQualities = sortedQualities
                            
                            // Auto-select best quality (720p or highest available)
                            val autoQuality = sortedQualities.firstOrNull { it.height == 720 } 
                                ?: sortedQualities.firstOrNull()
                            
                            if (autoQuality != null) {
                                selectedQuality = autoQuality
                                loadVideo(autoQuality.url, autoQuality.audioUrl, startPosition)
                                isLoading = false
                            } else {
                                errorMessage = "No video formats available"
                                isLoading = false
                            }
                        } else {
                            errorMessage = lastError ?: "No video stream found"
                            isLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error loading video"
                isLoading = false
            }
        }
        
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Video Player - Fullscreen with proper aspect ratio
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Disable default controls, use custom overlay
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        
                        // Enable pinch-to-zoom gesture
                        val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                videoScale *= detector.scaleFactor
                                videoScale = videoScale.coerceIn(0.5f, 3f)
                                scaleX = videoScale
                                scaleY = videoScale
                                return true
                            }
                        })
                        
                        setOnTouchListener { view, event ->
                            scaleDetector.onTouchEvent(event)
                            view.performClick()
                            false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Swipe down gesture to exit
                        detectDragGestures(
                            onDragEnd = {
                                if (dragOffsetY > 200) {
                                    onBack()
                                }
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y > 0) {
                                    dragOffsetY += dragAmount.y
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // Tap to toggle controls
                        detectTapGestures {
                            showControls = !showControls
                        }
                    }
            )
            
            // Custom Control Overlay
            AnimatedVisibility(
                visible = showControls && !isLoading,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top bar with back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Quality selector button
                        if (availableQualities.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable { showQualityMenu = !showQualityMenu }
                            ) {
                                selectedQuality?.let {
                                    Text(
                                        text = it.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.settings_outlined),
                                    contentDescription = "Quality",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Center play/pause button
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer?.pause()
                            } else {
                                exoPlayer?.play()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Bottom timeline slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(48.dp)
                        )
                        
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { value ->
                                val newPosition = (value * duration).toLong()
                                exoPlayer?.seekTo(newPosition)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }
            }
            
            // Auto-hide controls after 3 seconds
            LaunchedEffect(showControls) {
                if (showControls && !isLoading) {
                    delay(3000)
                    showControls = false
                }
            }
            
            // Quality selector menu
            if (showQualityMenu && availableQualities.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showQualityMenu = false }
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 300.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1C1C1E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Video Quality",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            availableQualities.forEach { quality ->
                                val isSelected = selectedQuality == quality
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) 
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else 
                                                Color.Transparent
                                        )
                                        .clickable {
                                            selectedQuality = quality
                                            loadVideo(quality.url, quality.audioUrl)
                                            showQualityMenu = false
                                            showControls = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = quality.label,
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) 
                                            androidx.compose.ui.text.font.FontWeight.Bold 
                                        else 
                                            androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(R.drawable.check),
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}
