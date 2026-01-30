package iad1tya.echo.music.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import iad1tya.echo.music.R
import iad1tya.echo.music.recognition.MusicRecognitionViewModel
import iad1tya.echo.music.recognition.RecognitionState
import iad1tya.echo.music.ui.theme.extractThemeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.ui.component.AnimatedGradientBackground
import iad1tya.echo.music.ui.theme.PlayerColorExtractor
import iad1tya.echo.music.utils.rememberPreference

import android.provider.Settings
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindSongScreen(
    navController: NavController,
    viewModel: MusicRecognitionViewModel = hiltViewModel(),
    onOpenPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    var hasStartedListening by remember { mutableStateOf(false) }
    var isPermissionDenied by remember { mutableStateOf(false) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Re-check permission on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isPermissionDenied) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    isPermissionDenied = false
                    if (!hasStartedListening) {
                         viewModel.startListening()
                         hasStartedListening = true
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isPermissionDenied = false
            if (!hasStartedListening) {
                viewModel.startListening()
                hasStartedListening = true
            }
        } else {
            isPermissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStartedListening) {
            delay(300) // Wait for transition animation to finish
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startListening()
                hasStartedListening = true
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(Unit) {
        val wasPlaying = playerConnection.isPlaying.value
        if (wasPlaying) {
            playerConnection.player.pause()
        }
        onDispose {
            if (wasPlaying) {
                playerConnection.player.play()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Echo Find",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = if (state is RecognitionState.Success) Color.White else MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate(Screens.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = if (state is RecognitionState.Success) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    val currentState = state
                    if (currentState is RecognitionState.Success) {
                        val scope = rememberCoroutineScope()
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val url = viewModel.findSongOnYouTube(currentState.track)
                                    if (url != null) {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, url)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    iad1tya.echo.music.LocalPlayerAwareWindowInsets.current
                        .only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPermissionDenied) {
                PermissionDeniedView(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            } else {
                when (val currentState = state) {
                    is RecognitionState.Idle, is RecognitionState.Listening -> {
                        ListeningView(isListening = currentState is RecognitionState.Listening)
                    }
                    is RecognitionState.Success -> {
                        SuccessView(
                            track = currentState.track,
                            navController = navController,
                            onPlay = {
                                viewModel.playSong(currentState.track, playerConnection)
                                onOpenPlayer()
                            }
                        )
                    }
                    is RecognitionState.Error -> {
                        ErrorView(
                            message = currentState.message,
                            onRetry = { viewModel.startListening() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedView(onOpenSettings: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Echo needs access to your microphone to identify songs playing around you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Open Settings")
        }
    }
}

@Composable
private fun ListeningView(isListening: Boolean) {
    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isListening) {
                // Outer Pulse
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                )
                // Inner Pulse
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale * 0.9f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha * 1.5f))
                )
            }
            
            // Icon Background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = if (isListening) "Listening..." else "Tap to start",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Make sure your device can hear the music",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun SuccessView(
    track: iad1tya.echo.music.recognition.Track,
    navController: NavController,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    var paletteColorList by remember { mutableStateOf(emptyList<Color>()) }

    // Load image and extract color
    LaunchedEffect(track) {
        val imageUrl = track.images?.coverarthq ?: track.images?.coverart
        if (imageUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Required for palette
                .build()
            
            val result = coil3.ImageLoader(context).execute(request)
            result.image?.toBitmap()?.let { bitmap ->
                 // Use PlayerColorExtractor to get rich colors
                 val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                 paletteColorList = PlayerColorExtractor.extractRichGradientColors(
                     palette = palette,
                     fallbackColor = Color.Black.toArgb()
                 )
            }
        }
    }

    // Main Container with Animated Background
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedGradientBackground(
            colors = paletteColorList,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Album Art
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.images?.coverarthq ?: track.images?.coverart)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title & Artist
            Text(
                text = track.title ?: "Unknown Title",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = track.subtitle ?: "Unknown Artist",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Metadata
            val songSection = track.sections?.find { it.type == "SONG" }
            val label = songSection?.metadata?.find { it.title == "Label" }?.text
            val released = songSection?.metadata?.find { it.title == "Released" }?.text
            val genre = track.genres?.primary

            if (!genre.isNullOrEmpty() || !released.isNullOrEmpty()) {
                Text(
                    text = listOfNotNull(genre, released).joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (!label.isNullOrEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val query = "${track.title} ${track.subtitle}"
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

                OutlinedButton(
                    onClick = {
                        navController.navigate("search/${encodedQuery}?autoplay=false")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search")
                }
                
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(50), 
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Could not identify song",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Try Again")
        }
    }
}
