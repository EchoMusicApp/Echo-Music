package iad1tya.echo.music.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import iad1tya.echo.music.R
import iad1tya.echo.music.utils.makeTimeString
import iad1tya.echo.music.viewmodels.ArtistStats
import iad1tya.echo.music.viewmodels.SongStats
import iad1tya.echo.music.viewmodels.WrappedData
import iad1tya.echo.music.viewmodels.WrappedViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedScreen(
    navController: NavController,
    viewModel: WrappedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 7 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    // Immersive Mode
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
    }
    
    // Background Music
    DisposableEffect(Unit) {
        val mediaPlayer = MediaPlayer()
        try {
            val descriptor: AssetFileDescriptor = context.assets.openFd("Echo-Wrapped.mp3")
            mediaPlayer.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            descriptor.close()
            mediaPlayer.prepare()
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LavaLampBackground()
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                when (page) {
                    0 -> DevApologySlide()
                    1 -> IntroSlide()
                    2 -> TimeListenedSlide(uiState.totalTimeMillis, uiState.isLoading)
                    3 -> TopTrackSlide(uiState.topSongs.firstOrNull())
                    4 -> TopArtistSlide(uiState.topArtists.firstOrNull())
                    5 -> SummarySlide(uiState)
                    6 -> ShareSlide(uiState)
                }
            }
        }

        // Navigation Zones
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.3f).fillMaxSize().clickable(indication=null, interactionSource=remember{MutableInteractionSource()}){
                if (pagerState.currentPage > 0) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            })
            Spacer(modifier = Modifier.weight(0.4f))
            Box(modifier = Modifier.weight(0.3f).fillMaxSize().clickable(indication=null, interactionSource=remember{MutableInteractionSource()}){
                if (pagerState.currentPage < pagerState.pageCount - 1) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            })
        }

        // Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(pagerState.pageCount) { index ->
                Box(
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (index <= pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.3f))
                )
            }
        }

        // Close Button
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 16.dp)
        ) {
            Icon(painter = painterResource(R.drawable.close), contentDescription = "Close", tint = Color.White)
        }
    }
}

// --- MATERIAL SHAPES ---

@Composable
fun MaterialBlob(modifier: Modifier = Modifier, color: Color = Color.White.copy(0.1f)) {
    val infiniteTransition = rememberInfiniteTransition(label="blob")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing))
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Canvas(modifier = modifier.rotate(angle).scale(scale)) {
        val w = size.width
        val h = size.height
        val path = Path()
        
        // Squaricle
        path.moveTo(w * 0.5f, 0f)
        path.cubicTo(w, 0f, w, h * 0.0f, w, h * 0.5f) // Top Right
        path.cubicTo(w, h, w * 1.0f, h, w * 0.5f, h) // Bottom Right
        path.cubicTo(0f, h, 0f, h * 1.0f, 0f, h * 0.5f) // Bottom Left
        path.cubicTo(0f, 0f, 0f, h * 0.0f, w * 0.5f, 0f) // Top Left
        path.close()
        
        drawPath(path, color, style = Fill)
    }
}

// --- SLIDES (Refined / Cleanup) ---

@Composable
fun DevApologySlide() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Removed extra shape
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                text = "Sorry we're late.",
                style = MaterialTheme.typography.displayMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                 textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Dev vs. Exams 😵‍💫",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(0.7f)),
                 textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun IntroSlide() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MaterialBlob(Modifier.size(350.dp).align(Alignment.Center), Color.White.copy(0.05f))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "2025",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp, 
                    color = Color.White, 
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = "REWIND",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    letterSpacing = 10.sp
                )
            )
        }
    }
}

@Composable
fun TimeListenedSlide(totalTime: Long, isLoading: Boolean) {
    val minutes = (totalTime / 1000 / 60).coerceAtLeast(0)
    val isEmpty = minutes < 1
    
    val timeAnimated = remember { Animatable(0f) }
    
    // Only animate if data is loaded and we have value
    LaunchedEffect(minutes, isLoading) {
        if (!isLoading) {
             timeAnimated.animateTo(minutes.toFloat(), tween(2000))
        }
    }
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Particles removed for cleaner look as requested
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Reserve space for the main text/number to prevent jumps
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(200.dp)
            ) {
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                     Text(
                        text = "CALCULATING...",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(0.5f),
                            letterSpacing = 2.sp
                        )
                    )
                }

                AnimatedVisibility(
                    visible = !isLoading && isEmpty,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "STARTING UP",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }

                AnimatedVisibility(
                    visible = !isLoading && !isEmpty,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "${timeAnimated.value.toLong()}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 140.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (!isLoading && isEmpty) {
                 Text(
                    text = "You're just getting into the groove.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(0.7f)
                    )
                )
            } else if (!isLoading) {
                 Text(
                    text = "MINUTES LISTENED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(0.7f),
                        letterSpacing = 4.sp
                    )
                )
            }
        }
    }
}

@Composable
fun TopTrackSlide(song: SongStats?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MaterialBlob(Modifier.size(450.dp), Color.White.copy(0.08f))
        
        if (song != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = song.song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(24.dp)), // More rounded
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(48.dp))
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.headlineLarge.copy(color=Color.White, fontWeight=FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal=32.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = song.song.artists.joinToString(", ") { it.name }.ifEmpty { "Unknown Artist" },
                    style = MaterialTheme.typography.titleLarge.copy(color=Color.White.copy(0.7f)),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Text("#1 TRACK", color=Color.White, letterSpacing=2.sp)
            }
        } else {
             // Empty State
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(250.dp).background(Color.White.copy(0.1f), RoundedCornerShape(24.dp)), contentAlignment=Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.music_note), contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(80.dp))
                }
                Spacer(Modifier.height(48.dp))
                Text("NO TOP TRACK YET", style=MaterialTheme.typography.headlineMedium.copy(color=Color.White, fontWeight=FontWeight.Bold))
                Spacer(Modifier.height(16.dp))
                Text("Keep listening to find your anthem.", style=MaterialTheme.typography.titleLarge.copy(color=Color.White.copy(0.7f)))
             }
        }
    }
}

@Composable
fun TopArtistSlide(artist: ArtistStats?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MaterialBlob(Modifier.size(400.dp), Color.White.copy(0.06f))
            
        if (artist != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = artist.artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(250.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(48.dp))
                Text(
                    text = artist.artist.title,
                    style = MaterialTheme.typography.displaySmall.copy(color=Color.White, fontWeight=FontWeight.Black),
                    textAlign = TextAlign.Center
                )
                 Spacer(Modifier.height(32.dp))
                 Text("TOP ARTIST", color=Color.White, letterSpacing=2.sp)
            }
        } else {
             // Empty State
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(250.dp).clip(CircleShape).background(Color.White.copy(0.1f)), contentAlignment=Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.mic), contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(80.dp))
                }
                Spacer(Modifier.height(48.dp))
                Text("NO TOP ARTIST YET", style=MaterialTheme.typography.headlineMedium.copy(color=Color.White, fontWeight=FontWeight.Bold))
                Spacer(Modifier.height(16.dp))
                Text("Your favorite is out there.", style=MaterialTheme.typography.titleLarge.copy(color=Color.White.copy(0.7f)))
             }
        }
    }
}

@Composable
fun SummarySlide(data: WrappedData) {
    val isEmpty = data.topSongs.isEmpty() && data.topArtists.isEmpty()
    
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEmpty) {
             Text("THE RECAP", style=MaterialTheme.typography.displayMedium.copy(color=Color.White, fontWeight=FontWeight.Black))
             Spacer(Modifier.height(32.dp))
             Text("Come back after a few jam sessions.", style=MaterialTheme.typography.titleMedium.copy(color=Color.White.copy(0.7f)))
        } else {
            Text("THE RECAP", style=MaterialTheme.typography.headlineLarge.copy(color=Color.White, fontWeight=FontWeight.Black, letterSpacing = 2.sp))
            Spacer(Modifier.height(48.dp))

            // Vertical "Receipt" List
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                data.topSongs.take(5).forEachIndexed { i, s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "#${i+1}", 
                            color = Color.White.copy(0.5f), 
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(32.dp)
                        )
                        AsyncImage(
                            model = s.song.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.song.title, color=Color.White, maxLines=1, style=MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(s.song.artists.firstOrNull()?.name ?: "Unknown", color=Color.White.copy(0.7f), maxLines=1, style=MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(48.dp))
            val mins = (data.totalTimeMillis / 1000 / 60).coerceAtLeast(0)
            Text("$mins MINUTES VIBIN'", style=MaterialTheme.typography.titleLarge.copy(color=Color.White.copy(0.9f), fontWeight=FontWeight.Bold, letterSpacing = 1.sp))
        }
    }
}

@Composable
fun ShareSlide(data: WrappedData) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
         MaterialBlob(Modifier.size(600.dp), Color.White.copy(0.04f))
         
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text("SHARE IT", style=MaterialTheme.typography.displayLarge.copy(color=Color.White, fontWeight=FontWeight.Black))
             Spacer(Modifier.height(64.dp))
             
             Button(
                 onClick = {
                     if (!isSharing) {
                         isSharing = true
                         coroutineScope.launch {
                             shareWrapped(context, data)
                             isSharing = false
                         }
                     }
                 },
                 colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                 modifier = Modifier.height(56.dp).width(200.dp)
             ) {
                 Text(if(isSharing) "..." else "SHARE", color=Color.Black, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
             }
         }
    }
}

@Composable
fun LavaLampBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "lava")
    val b1 by infiniteTransition.animateFloat(0f, 6.28f, infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart))
    val b2 by infiniteTransition.animateFloat(0f, 6.28f, infiniteRepeatable(tween(35000, easing = LinearEasing), RepeatMode.Restart))

    Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
        drawRect(Color(0xFF101010))
        drawCircle(Color(0xFF8E2DE2).copy(0.3f), size.width*0.5f, Offset(size.width/2 + cos(b1)*size.width*0.3f, size.height/2 + sin(b1)*size.height*0.2f))
        drawCircle(Color(0xFF4A00E0).copy(0.3f), size.width*0.4f, Offset(size.width/2 + sin(b2)*size.width*0.3f, size.height/2 + cos(b2)*size.height*0.3f))
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

suspend fun shareWrapped(context: Context, data: WrappedData) {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint()
    // 1. Background (Gradient)
    val gradient = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(0xFF1A1A1A.toInt(), 0xFF000000.toInt()),
        null, android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = gradient
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // 2. Abstract Shapes (Subtle)
    val shapePaint = Paint().apply { color = 0xFF8E2DE2.toInt(); alpha = 40; isAntiAlias = true }
    canvas.drawCircle(width * 0.8f, 200f, 500f, shapePaint)
    shapePaint.color = 0xFF4A00E0.toInt()
    canvas.drawCircle(width * 0.2f, height * 0.9f, 600f, shapePaint)

    // Helper for centering text
    val textPaint = Paint().apply {
         color = android.graphics.Color.WHITE
         textAlign = Paint.Align.CENTER
         isAntiAlias = true
    }

    // 3. HEADER
    textPaint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    textPaint.textSize = 80f
    textPaint.alpha = 200
    textPaint.letterSpacing = 0.15f
    canvas.drawText("2026", width / 2f, 180f, textPaint)

    textPaint.textSize = 120f
    textPaint.alpha = 255
    textPaint.letterSpacing = 0.05f
    canvas.drawText("ECHO WRAPPED", width / 2f, 300f, textPaint)

    // 4. MAIN CONTENT (Top Song)
    var currentY = 450f
    val topSong = data.topSongs.firstOrNull()
    
    if (topSong != null) {
        // Label
        textPaint.textSize = 40f
        textPaint.alpha = 150
        textPaint.letterSpacing = 0.2f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("TOP TRACK", width / 2f, currentY, textPaint)
        currentY += 60f

        try {
            val request = ImageRequest.Builder(context).data(topSong.song.thumbnailUrl).allowHardware(false).build()
            val result = context.imageLoader.execute(request)
            val drawable = result.image?.toBitmap()
            if (drawable != null) {
                val imageSize = 600
                val imageX = (width - imageSize) / 2f
                val rect = RectF(imageX, currentY, imageX + imageSize, currentY + imageSize)
                
                // Draw rounded bitmap manually
                val roundedBitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
                val roundedCanvas = Canvas(roundedBitmap)
                val clipPaint = Paint().apply { isAntiAlias = true }
                val clipRect = RectF(0f, 0f, imageSize.toFloat(), imageSize.toFloat())
                roundedCanvas.drawRoundRect(clipRect, 60f, 60f, clipPaint)
                clipPaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                roundedCanvas.drawBitmap(drawable, null, clipRect, clipPaint)
                
                canvas.drawBitmap(roundedBitmap, imageX, currentY, null)
                currentY += imageSize + 90f

                // Song Title
                textPaint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                textPaint.textSize = 80f
                textPaint.alpha = 255
                textPaint.letterSpacing = 0f
                canvas.drawText(topSong.song.title, width / 2f, currentY, textPaint)
                
                // Content Artist
                currentY += 70f
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 50f
                textPaint.alpha = 180
                val artist = topSong.song.artists.joinToString(", ") { it.name }
                canvas.drawText(artist, width / 2f, currentY, textPaint)
                
                currentY += 150f
            }
        } catch (e: Exception) { 
             currentY += 200f
        }
    } else {
         currentY += 600f
    }

    // 5. SECONDARY CONTENT (Top Artist)
    val topArtist = data.topArtists.firstOrNull()
    if (topArtist != null) {
        textPaint.textSize = 40f
        textPaint.alpha = 150
        textPaint.letterSpacing = 0.2f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("TOP ARTIST", width / 2f, currentY, textPaint)
        currentY += 80f

        textPaint.textSize = 70f
        textPaint.alpha = 255
        textPaint.letterSpacing = 0f
        textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        canvas.drawText(topArtist.artist.title, width / 2f, currentY, textPaint)
    }

    // 6. FOOTER (Minutes)
    val mins = (data.totalTimeMillis / 1000 / 60).coerceAtLeast(0)
    
    val footerY = height - 200f
    textPaint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    textPaint.textSize = 140f
    textPaint.color = 0xFFFFFFFF.toInt()
    textPaint.alpha = 255
    canvas.drawText("$mins", width / 2f, footerY, textPaint)
    
    textPaint.textSize = 40f
    textPaint.alpha = 150
    textPaint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("MINUTES LISTENED", width / 2f, footerY + 80f, textPaint)

    // Save and Share
    try {
        val cachePath = File(context.cacheDir, "images"); cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/wrapped.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.close()
        val newFile = File(cachePath, "wrapped.png")
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", newFile)
        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Wrapped"))
        }
    } catch (e: Exception) { e.printStackTrace() }
}
