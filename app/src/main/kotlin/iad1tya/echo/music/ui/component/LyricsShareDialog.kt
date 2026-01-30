package iad1tya.echo.music.ui.component

import android.graphics.Bitmap
import android.text.Layout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.utils.ComposeToImage
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter

@Composable
fun LyricsShareDialog(
    mediaMetadata: MediaMetadata,
    lyrics: String,
    onDismiss: () -> Unit,
    onShare: (Bitmap) -> Unit
) {
    var backgroundColor by remember { mutableStateOf(Color(0xFF121212)) }
    var useGradient by remember { mutableStateOf(false) }
    var cornerRadiusPercent by remember { mutableStateOf(50f) } // 0 to 100, maps to 0f to ~50f (Squaricle)
    var textAlign by remember { mutableStateOf(Layout.Alignment.ALIGN_CENTER) }
    var fontScale by remember { mutableStateOf(1f) }
    
    // Derived state for actual parameters
    val gradientColors = remember(backgroundColor, useGradient) {
        if (useGradient) {
            listOf(backgroundColor, backgroundColor.copy(alpha = 0.6f), backgroundColor.copy(alpha = 0.3f))
        } else {
            null
        }
    }
    
    val actualCornerRadius = remember(cornerRadiusPercent) {
        // Max radius for a card size of ~300dp is around 50-60dp for a squaricle look
        (cornerRadiusPercent / 100f) * 60f
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(R.string.share_lyrics),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Preview Area (Simplified Compose Representation)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                   LyricsImageCard(
                       lyricText = lyrics,
                       mediaMetadata = mediaMetadata,
                       backgroundColor = backgroundColor,
                       useGradient = useGradient,
                       imageCornerRadius = actualCornerRadius.dp,
                       textAlign = when(textAlign) {
                           Layout.Alignment.ALIGN_NORMAL -> TextAlign.Start
                           Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
                           Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
                           else -> TextAlign.Center
                       },
                       fontScale = fontScale
                   )
                }

                // Controls
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    // Color Picker
                    Text("Background", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colors = listOf(
                            Color(0xFF121212), // Black
                            Color(0xFFF44336), // Red
                            Color(0xFFE91E63), // Pink
                            Color(0xFF9C27B0), // Purple
                            Color(0xFF673AB7), // Deep Purple
                            Color(0xFF3F51B5), // Indigo
                            Color(0xFF2196F3), // Blue
                            Color(0xFF009688), // Teal
                        )
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (backgroundColor == color) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { backgroundColor = color }
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gradient", modifier = Modifier.weight(1f))
                        Switch(checked = useGradient, onCheckedChange = { useGradient = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shape
                    Text("Shape (Corner Radius)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = cornerRadiusPercent,
                        onValueChange = { cornerRadiusPercent = it },
                        valueRange = 0f..100f
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Alignment
                    Text("Text Align", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                         IconButton(onClick = { textAlign = Layout.Alignment.ALIGN_NORMAL }) {
                             Icon(imageVector = Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, tint = if(textAlign == Layout.Alignment.ALIGN_NORMAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                         }
                         IconButton(onClick = { textAlign = Layout.Alignment.ALIGN_CENTER }) {
                             Icon(imageVector = Icons.Default.FormatAlignCenter, contentDescription = null, tint = if(textAlign == Layout.Alignment.ALIGN_CENTER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                         }
                         IconButton(onClick = { textAlign = Layout.Alignment.ALIGN_OPPOSITE }) {
                             Icon(imageVector = Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = null, tint = if(textAlign == Layout.Alignment.ALIGN_OPPOSITE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                         }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Scale
                    Text("Font Size", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = fontScale,
                        onValueChange = { fontScale = it },
                        valueRange = 0.5f..2.0f
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
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
                            if (isGenerating) return@Button
                            isGenerating = true
                            
                            scope.launch {
                                try {
                                    val screenWidth = context.resources.displayMetrics.widthPixels
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    val size = minOf(screenWidth, screenHeight)

                                    val bitmap = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = mediaMetadata.thumbnailUrl,
                                        songTitle = mediaMetadata.title,
                                        artistName = mediaMetadata.artists.joinToString { it.name },
                                        lyrics = lyrics,
                                        width = size,
                                        height = size, // Use square size for output
                                        backgroundColor = backgroundColor.toArgb(),
                                        gradientColors = if (useGradient) listOf(backgroundColor.toArgb(), backgroundColor.copy(alpha=0.6f).toArgb()) else null,
                                        cornerRadius = actualCornerRadius * (size.toFloat() / 360f), // Scale radius to output size
                                        textAlign = textAlign,
                                        fontScale = fontScale
                                    )
                                    onShare(bitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = !isGenerating
                    ) {
                         if (isGenerating) {
                             CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                             Spacer(modifier = Modifier.width(8.dp))
                         }
                        Text(stringResource(R.string.share))
                    }
                }
            }
        }
    }
}

// Simplified version of LyricsImageCard for preview purposes
@Composable
fun LyricsImageCard(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    backgroundColor: Color,
    useGradient: Boolean,
    imageCornerRadius: androidx.compose.ui.unit.Dp,
    textAlign: TextAlign,
    fontScale: Float
) {
    val backgroundBrush = if (useGradient) {
        Brush.linearGradient(
            colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.6f))
        )
    } else {
        androidx.compose.ui.graphics.SolidColor(backgroundColor)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Square preview
            .clip(RoundedCornerShape(imageCornerRadius))
            .background(backgroundBrush)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(imageCornerRadius))
            .padding(16.dp)
    ) {
         Column(
             verticalArrangement = Arrangement.SpaceBetween,
             modifier = Modifier.fillMaxSize()
         ) {
             // Header
             Row(verticalAlignment = Alignment.CenterVertically) {
                // Simplified header
                androidx.compose.foundation.Image(
                     painter = coil3.compose.rememberAsyncImagePainter(
                         model = mediaMetadata.thumbnailUrl
                     ),
                     contentDescription = null,
                     modifier = Modifier
                         .size(40.dp)
                         .clip(RoundedCornerShape(8.dp)),
                     contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                 Column {
                     Text(mediaMetadata.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                     Text(mediaMetadata.artists.joinToString{it.name}, color = Color.White.copy(alpha=0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                 }
             }
             
             // Lyrics
             Box(
                 modifier = Modifier.weight(1f).fillMaxWidth(),
                 contentAlignment = Alignment.Center
             ) {
                Text(
                     text = lyricText,
                     color = Color.White,
                     textAlign = textAlign,
                     fontSize = (16 * fontScale).sp,
                     fontWeight = FontWeight.Bold,
                     overflow = TextOverflow.Ellipsis
                 )
             }
             
             // Footer
             Text("echomusic.fun", color = Color.White.copy(alpha=0.5f), style = MaterialTheme.typography.labelSmall)
         }
    }
}
