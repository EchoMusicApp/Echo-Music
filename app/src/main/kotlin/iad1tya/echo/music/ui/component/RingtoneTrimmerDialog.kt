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

    val maxRingtoneDuration = 30000f
    val safeDuration = if (duration > 0) duration.toFloat() else 180000f

    var range by remember(songId) {
        val end = if (safeDuration > maxRingtoneDuration) maxRingtoneDuration else safeDuration
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
                if (exoPlayer.currentPosition >= range.endInclusive.toLong()) {
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
                                    exoPlayer.seekTo(range.start.toLong())
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
                        text = makeTimeString(range.start.toLong()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = makeTimeString(range.endInclusive.toLong()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                RangeSlider(
                    value = range,
                    onValueChange = {
                        range = it
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        }
                    },
                    valueRange = 0f..safeDuration,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                val selectedDuration = (range.endInclusive - range.start).toLong()
                Text(
                    text = "Selected duration: ${makeTimeString(selectedDuration)}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (selectedDuration > 40000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    exoPlayer.stop()
                    onConfirm(range.start.toLong(), range.endInclusive.toLong())
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
