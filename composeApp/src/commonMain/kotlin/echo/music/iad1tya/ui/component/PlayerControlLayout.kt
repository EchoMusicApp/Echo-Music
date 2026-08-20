package echo.music.iad1tya.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.domain.mediaservice.handler.ControlState
import echo.music.iad1tya.domain.mediaservice.handler.RepeatState
import echo.music.iad1tya.ui.icon.Pause
import echo.music.iad1tya.ui.icon.PauseCircle
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.PlayCircle
import echo.music.iad1tya.ui.icon.Repeat
import echo.music.iad1tya.ui.icon.RepeatOne
import echo.music.iad1tya.ui.icon.Shuffle
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.icon.SkipNext
import echo.music.iad1tya.ui.icon.SkipPrevious
import echo.music.iad1tya.ui.theme.seed
import echo.music.iad1tya.viewModel.UIEvent

@Composable
fun PlayerControlLayout(
    controllerState: ControlState,
    isSmallSize: Boolean = false,
    // Bare ▶ / ⏸ glyphs instead of the disc-enclosed PlayCircle/PauseCircle pair.
    // The desktop capsule asks for these; Now Playing keeps the discs.
    plainPlayPause: Boolean = false,
    // The capsule already pads its own edges; stacking this 20dp on top of that
    // read as a hole at both ends of the transport cluster.
    horizontalPadding: Dp = 20.dp,
    // Tint for the ACTIVE shuffle/repeat state. The default keeps the raw seed (#8ECAE6) every
    // existing call site had; the capsule passes a theme-aware colour because pastel seed on a
    // light glass surface is nearly invisible.
    activeColor: Color = seed,
    contentColor: Color = Color.White,
    onUIEvent: (UIEvent) -> Unit,
) {
    val height = if (isSmallSize) 48.dp else 96.dp
    val smallIcon = if (isSmallSize) 20.dp to 28.dp else 32.dp to 42.dp
    val mediumIcon = if (isSmallSize) 28.dp to 38.dp else 42.dp to 52.dp
    val bigIcon = if (isSmallSize) 38.dp to 48.dp else 72.dp to 96.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = horizontalPadding),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            onUIEvent(UIEvent.Shuffle)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.isShuffle, label = "Shuffle Button") { isShuffle ->
                    if (!isShuffle) {
                        Icon(
                            imageVector = echoIcons.Shuffle,
                            tint = contentColor,
                            contentDescription = "",
                            modifier = Modifier.size(smallIcon.first),
                        )
                    } else {
                        Icon(
                            imageVector = echoIcons.Shuffle,
                            tint = activeColor,
                            contentDescription = "",
                            modifier = Modifier.size(smallIcon.first),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            if (controllerState.isPreviousAvailable) {
                                onUIEvent(UIEvent.Previous)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = echoIcons.SkipPrevious,
                    tint = if (controllerState.isPreviousAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = "",
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(bigIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            onUIEvent(UIEvent.PlayPause)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.isPlaying) { isPlaying ->
                    if (!isPlaying) {
                        Icon(
                            imageVector = if (plainPlayPause) echoIcons.PlayArrow else echoIcons.PlayCircle,
                            tint = contentColor,
                            contentDescription = "",
                            modifier = Modifier.size(bigIcon.first),
                        )
                    } else {
                        Icon(
                            imageVector = if (plainPlayPause) echoIcons.Pause else echoIcons.PauseCircle,
                            tint = contentColor,
                            contentDescription = "",
                            modifier = Modifier.size(bigIcon.first),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = echoIcons.SkipNext,
                    tint = if (controllerState.isNextAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = "",
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            onUIEvent(UIEvent.Repeat)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.repeatState) { rs ->
                    when (rs) {
                        is RepeatState.None -> {
                            Icon(
                                imageVector = echoIcons.Repeat,
                                tint = contentColor,
                                contentDescription = "",
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }

                        RepeatState.All -> {
                            Icon(
                                imageVector = echoIcons.Repeat,
                                tint = activeColor,
                                contentDescription = "",
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }

                        RepeatState.One -> {
                            Icon(
                                imageVector = echoIcons.RepeatOne,
                                tint = activeColor,
                                contentDescription = "",
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }
                    }
                }
            }
        }
    }
}