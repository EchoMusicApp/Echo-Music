package echo.music.iad1tya.ui.screens.recognition

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R
import echo.music.iad1tya.db.entities.RecognitionHistory
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain
import com.music.shazamkit.models.RecognitionResult
import com.music.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import echo.music.iad1tya.LocalPlayerConnection
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        echo.music.iad1tya.recognition.MusicRecognitionService.reset()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            echo.music.iad1tya.recognition.MusicRecognitionService.reset()
        }
    }
    
    val recognitionStatus by echo.music.iad1tya.recognition.MusicRecognitionService.recognitionStatus.collectAsState()
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                echo.music.iad1tya.recognition.MusicRecognitionService.recognize(context)
            }
        }
    }
    
    fun startRecognition() {
        if (hasPermission) {
            coroutineScope.launch {
                echo.music.iad1tya.recognition.MusicRecognitionService.recognize(context)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    fun resetToReady() {
        echo.music.iad1tya.recognition.MusicRecognitionService.reset()
    }

    fun saveToHistory(result: RecognitionResult) {
        coroutineScope.launch(Dispatchers.IO) {
            database.query {
                insert(
                    RecognitionHistory(
                        trackId = result.trackId,
                        title = result.title,
                        artist = result.artist,
                        album = result.album,
                        coverArtUrl = result.coverArtUrl,
                        coverArtHqUrl = result.coverArtHqUrl,
                        genre = result.genre,
                        releaseDate = result.releaseDate,
                        label = result.label,
                        shazamUrl = result.shazamUrl,
                        appleMusicUrl = result.appleMusicUrl,
                        spotifyUrl = result.spotifyUrl,
                        isrc = result.isrc,
                        youtubeVideoId = result.youtubeVideoId,
                        recognizedAt = LocalDateTime.now()
                    )
                )
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = recognitionStatus,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            label = "main_content"
        ) { status ->
            if (status is RecognitionStatus.Success) {
                val playerConnection = LocalPlayerConnection.current
                SuccessState(
                    result = status.result,
                    onPlayOnApp = { result ->
                        coroutineScope.launch(Dispatchers.IO) {
                            val searchQuery = "${result.title} ${result.artist}"
                            YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG)
                                .onSuccess { searchResult ->
                                    val song = searchResult.items.firstOrNull() as? SongItem
                                    song?.let {
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            playerConnection?.playQueue(YouTubeQueue.radio(it.toMediaMetadata()))
                                        }
                                    }
                                }
                        }
                    },
                    onTryAgain = {
                        startRecognition()
                    },
                    onClose = ::resetToReady,
                    onSaveToHistory = ::saveToHistory
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.background
                                    ),
                                    radius = 1500f
                                )
                            )
                    )

                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                ),
                                title = { Text(stringResource(R.string.recognize_music)) },
                                navigationIcon = {
                                    androidx.compose.material3.IconButton(
                                        onClick = { navController.navigateUp() }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_back),
                                            contentDescription = null
                                        )
                                    }
                                },
                                actions = {
                                    androidx.compose.material3.IconButton(onClick = { navController.navigate("recognition_history") }) {
                                        Icon(
                                            painter = painterResource(R.drawable.history),
                                            contentDescription = stringResource(R.string.recognition_history)
                                        )
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (status) {
                                is RecognitionStatus.Ready -> {
                                    ReadyState(onStartRecognition = ::startRecognition)
                                }
                                is RecognitionStatus.Listening -> {
                                    ListeningState(
                                        onCancel = { echo.music.iad1tya.recognition.MusicRecognitionService.reset() }
                                    )
                                }
                                is RecognitionStatus.Processing -> {
                                    ProcessingState()
                                }
                                is RecognitionStatus.NoMatch -> {
                                    NoMatchState(
                                        message = status.message,
                                        onTryAgain = {
                                            startRecognition()
                                        }
                                    )
                                }
                                is RecognitionStatus.Error -> {
                                    ErrorState(
                                        message = status.message,
                                        onTryAgain = {
                                            startRecognition()
                                        }
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyState(
    onStartRecognition: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onStartRecognition() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Text(
            text = stringResource(R.string.tap_to_recognize),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ListeningState(
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val bars = 5
            val animatables = remember { List(bars) { Animatable(0.2f) } }

            LaunchedEffect(Unit) {
                animatables.forEach { animatable ->
                    launch {
                        while (true) {
                            animatable.animateTo(
                                targetValue = kotlin.random.Random.nextFloat() * 0.8f + 0.2f,
                                animationSpec = tween(
                                    durationMillis = kotlin.random.Random.nextInt(300, 600),
                                    easing = LinearEasing
                                )
                            )
                        }
                    }
                }
            }

            val colorPrimary = MaterialTheme.colorScheme.primary
            val colorTertiary = MaterialTheme.colorScheme.tertiary

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(160.dp)
            ) {
                animatables.forEachIndexed { index, animatable ->
                    val color = if (index % 2 == 0) colorPrimary else colorTertiary
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(animatable.value)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
        
        Text(
            text = stringResource(R.string.listening),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        OutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ProcessingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = stringResource(R.string.processing),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SuccessState(
    result: RecognitionResult,
    onPlayOnApp: (RecognitionResult) -> Unit,
    onTryAgain: () -> Unit,
    onClose: () -> Unit,
    onSaveToHistory: (RecognitionResult) -> Unit
) {
    LaunchedEffect(result) {
        onSaveToHistory(result)
    }

    val highResImageUrl = (result.coverArtHqUrl ?: result.coverArtUrl)?.replace("400x400", "1000x1000")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Blurred Background
        AsyncImage(
            model = highResImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 48.dp)
                .alpha(0.6f),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // Floating Album Art
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp), // Shift up to clear the bottom text
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = highResImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .shadow(elevation = 32.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(
                onClick = onClose,
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close)
                )
            }
            
            androidx.compose.material3.Surface(
                onClick = onTryAgain,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.re_listen),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            androidx.compose.material3.FloatingActionButton(
                onClick = { onPlayOnApp(result) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = stringResource(R.string.play_on_app),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun NoMatchState(
    message: String,
    onTryAgain: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = stringResource(R.string.no_match_found),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(
            onClick = onTryAgain,
            modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.try_again), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onTryAgain: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.error),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = stringResource(R.string.recognition_error),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(
            onClick = onTryAgain,
            modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.try_again), style = MaterialTheme.typography.titleMedium)
        }
    }
}
