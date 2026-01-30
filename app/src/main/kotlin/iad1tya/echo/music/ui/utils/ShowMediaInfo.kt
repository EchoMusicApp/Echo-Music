package iad1tya.echo.music.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.echo.innertube.YouTube
import com.echo.innertube.models.MediaInfo
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.db.entities.FormatEntity
import iad1tya.echo.music.db.entities.Song
import kotlinx.coroutines.flow.collect

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank()) return

    val context = LocalContext.current
    val windowInsets = WindowInsets.systemBars
    var info by remember { mutableStateOf<MediaInfo?>(null) }
    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }
    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }
    val playerConnection = LocalPlayerConnection.current

    LaunchedEffect(videoId) {
        YouTube.getMediaInfo(videoId).onSuccess { info = it }
    }
    LaunchedEffect(videoId) {
        database.song(videoId).collect { song = it }
    }
    LaunchedEffect(videoId) {
        database.format(videoId).collect { currentFormat = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred Background
        if (!song?.thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = song?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            // Gradient Overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        } else {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(windowInsets.asPaddingValues())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER: Album Art & Title ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.size(160.dp)
                    ) {
                        AsyncImage(
                            model = song?.thumbnailUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = song?.title ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song?.artists?.joinToString { it.name } ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- SECTION 1: TECHNICAL INFO (Format) ---
            if (currentFormat != null) {
                item {
                    InfoCard(title = stringResource(R.string.details)) {
                        InfoRow(
                            painter = painterResource(R.drawable.music_note),
                            label = stringResource(R.string.mime_type),
                            value = currentFormat?.mimeType
                        )
                        InfoRow(
                            painter = painterResource(R.drawable.integration),
                            label = stringResource(R.string.codecs),
                            value = currentFormat?.codecs
                        )
                        InfoRow(
                            painter = painterResource(R.drawable.speed),
                            label = stringResource(R.string.bitrate),
                            value = currentFormat?.bitrate?.let { "${it / 1000} Kbps" }
                        )
                        InfoRow(
                            painter = painterResource(R.drawable.graphic_eq),
                            label = stringResource(R.string.sample_rate),
                            value = currentFormat?.sampleRate?.let { "$it Hz" }
                        )
                        InfoRow(
                            painter = painterResource(R.drawable.volume_up),
                            label = stringResource(R.string.loudness),
                            value = currentFormat?.loudnessDb?.let { "$it dB" }
                        )
                        InfoRow(
                            painter = painterResource(R.drawable.storage),
                            label = stringResource(R.string.file_size),
                            value = currentFormat?.contentLength?.let {
                                Formatter.formatShortFileSize(context, it)
                            }
                        )
                    }
                }
            }

            // --- SECTION 2: STATISTICS (Online) ---
             if (info != null) {
                item {
                    InfoCard(title = stringResource(R.string.numbers)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                painter = painterResource(R.drawable.stats),
                                label = stringResource(R.string.views),
                                value = info?.viewCount?.toInt()?.let { compactNumberFormatter(it) }
                            )
                            StatItem(
                                painter = painterResource(R.drawable.favorite_border),
                                label = stringResource(R.string.likes),
                                value = info?.like?.toInt()?.let { compactNumberFormatter(it) }
                            )
                        }
                    }
                }

                // --- SECTION 3: DESCRIPTION ---
                if (!info?.description.isNullOrBlank()) {
                    item {
                         InfoCard(title = stringResource(R.string.description)) {
                             Text(
                                 text = info?.description ?: "",
                                 style = MaterialTheme.typography.bodyMedium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 modifier = Modifier.padding(8.dp)
                             )
                         }
                    }
                }
            }

            // --- SECTION 4: METADATA ---
            item {
                InfoCard(title = "Metadata") {
                     InfoRow(
                        painter = painterResource(R.drawable.key),
                        label = stringResource(R.string.media_id),
                        value = song?.id
                    )
                     if (currentFormat?.itag != null) {
                         InfoRow(
                             painter = painterResource(R.drawable.bookmark),
                             label = "Itag",
                             value = currentFormat?.itag.toString()
                         )
                     }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(
    painter: Painter,
    label: String,
    value: String?
) {
    val context = LocalContext.current
    if (value.isNullOrBlank()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatItem(
    painter: Painter,
    label: String,
    value: String?
) {
    if (value == null) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun compactNumberFormatter(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> String.format("%.1fK", count / 1000.0)
        else -> String.format("%.1fM", count / 1000000.0)
    }
}
