package iad1tya.echo.music.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import iad1tya.echo.music.utils.makeTimeString
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneTrimmerDialog(
    isVisible: Boolean,
    songId: String?,
    songTitle: String?,
    duration: Long,
    onDismiss: () -> Unit,
    onResolveStreamUrl: suspend (String) -> String?,
    onConfirm: (Long, Long) -> Unit,
) {
    if (!isVisible || songId == null) return

    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val maxRingtoneDurationSec = 30f
    val safeDurationSec = if (duration > 0) duration.toFloat() / 1000f else 180f

    var rangeSec by remember(songId) {
        val end = if (safeDurationSec > maxRingtoneDurationSec) maxRingtoneDurationSec else safeDurationSec
        mutableStateOf(0f..end)
    }

    LaunchedEffect(songId, isVisible) {
        if (isVisible) {
            isLoading = true
            val resolvedUrl = onResolveStreamUrl(songId)
            val uri = resolvedUrl?.let { android.net.Uri.parse(it) }

            if (uri != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                if (exoPlayer.currentPosition >= (rangeSec.endInclusive * 1000).toLong()) {
                    exoPlayer.pause()
                    isPlaying = false
                }
                delay(100)
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            exoPlayer.stop()
            onDismiss()
        },
        title = {
            Text(
                "Trim Ringtone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select the part of \"${songTitle ?: "Unknown"}\" to use as ringtone. (Max 30s recommended)",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.seekTo((rangeSec.start * 1000).toLong())
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString((rangeSec.start * 1000).toLong()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = makeTimeString((rangeSec.endInclusive * 1000).toLong()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                RangeSlider(
                    value = rangeSec,
                    onValueChange = { newRangeSec ->
                        rangeSec = newRangeSec
                        
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        }
                    },
                    valueRange = 0f..safeDurationSec,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                val selectedDurationSec = (rangeSec.endInclusive - rangeSec.start).toLong()
                Text(
                    text = "Selected duration: ${makeTimeString(selectedDurationSec * 1000)}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (selectedDurationSec > 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    exoPlayer.stop()
                    onConfirm((rangeSec.start * 1000).toLong(), (rangeSec.endInclusive * 1000).toLong())
                },
            ) {
                Text("Set as Ringtone")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                exoPlayer.stop()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}
