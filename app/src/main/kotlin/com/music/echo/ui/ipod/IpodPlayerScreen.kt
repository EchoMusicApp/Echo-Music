package iad1tya.echo.music.ui.ipod

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.extensions.currentMetadata
import iad1tya.echo.music.extensions.metadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * iPod Now Playing screen — reads from existing PlayerConnection.
 * ponytail: no new ViewModel, just reads the existing player state.
 */
@Composable
fun IpodPlayerScreen(
    modifier: Modifier = Modifier,
) {
    val colors = LocalIpodColors.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentMediaItem = playerConnection.player.currentMediaItem
    val metadata = currentMediaItem?.metadata

    // Poll position — ExoPlayer doesn't expose position as Flow
    // ponytail: delay loop is the simplest way, add Player.Listener if this is too coarse
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, currentMediaItem) {
        while (isActive) {
            try {
                currentPosition = playerConnection.player.currentPosition
                duration = playerConnection.player.duration.coerceAtLeast(1L)
            } catch (_: Exception) {
                // ponytail: player might not be ready yet
            }
            delay(500L)
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (duration > 0) (currentPosition.toFloat() / duration) else 0f,
        label = "progress",
    )

    val title = metadata?.title?.toString() ?: "Not Playing"
    val artist = metadata?.artist?.toString() ?: "—"
    val thumbnailUrl = metadata?.artworkUri?.toString()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.selectedHighlight.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Now Playing",
                color = colors.screenBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Album art
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Album artwork for $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "🎵",
                    fontSize = 48.sp,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Song info
        Text(
            text = title,
            color = colors.screenText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = artist,
            color = colors.screenTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(6.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = colors.selectedHighlight,
            trackColor = colors.divider,
        )

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(currentPosition),
                color = colors.screenTextSecondary,
                fontSize = 10.sp,
            )
            Text(
                text = "-${formatTime((duration - currentPosition).coerceAtLeast(0))}",
                color = colors.screenTextSecondary,
                fontSize = 10.sp,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Play state indicator
        Text(
            text = if (isPlaying) "▶ Playing" else "❚❚ Paused",
            color = colors.screenTextSecondary,
            fontSize = 11.sp,
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
