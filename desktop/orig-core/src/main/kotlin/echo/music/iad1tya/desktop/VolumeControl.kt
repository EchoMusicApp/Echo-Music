package echo.music.iad1tya.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import java.io.File

private val volumeFile = File(System.getProperty("user.home"), ".echomusic/volume.txt")

/** Last volume the person chose (0..1); full volume the first time. */
fun savedVolume(): Float = runCatching { volumeFile.readText().trim().toFloat().coerceIn(0f, 1f) }.getOrDefault(1f)

/**
 * A speaker button; while the mouse is over it (or the bar that opens above it) a vertical volume bar is shown.
 * Click the bar to jump, drag to slide; click the button to mute / unmute.
 */
@Composable
fun VolumeControl(player: Player, modifier: Modifier = Modifier) {
  val hover = remember { MutableInteractionSource() }
  val hovered by hover.collectIsHoveredAsState()
  var volume by remember { mutableStateOf(player.volume) }
  var beforeMute by remember { mutableStateOf(if (player.volume > 0f) player.volume else 1f) }
  fun set(v: Float) {
    val nv = v.coerceIn(0f, 1f)
    volume = nv; player.volume = nv
    if (nv > 0f) beforeMute = nv
    runCatching { volumeFile.parentFile.mkdirs(); volumeFile.writeText(nv.toString()) }
  }
  Column(modifier.hoverable(hover), horizontalAlignment = Alignment.CenterHorizontally) {
    if (hovered) {
      Box(
        Modifier.width(40.dp).height(160.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
          .pointerInput(Unit) {
            val pad = 14.dp.toPx()
            fun at(y: Float) = 1f - ((y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)
            detectTapGestures { set(at(it.y)) }
          }
          .pointerInput(Unit) {
            val pad = 14.dp.toPx()
            detectVerticalDragGestures { change, _ -> set(1f - ((change.position.y - pad) / (size.height - 2 * pad)).coerceIn(0f, 1f)) }
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(Modifier.width(6.dp).fillMaxHeight(0.8f).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)), contentAlignment = Alignment.BottomCenter) {
          Box(Modifier.fillMaxWidth().fillMaxHeight(volume).background(MaterialTheme.colorScheme.primary))
        }
      }
    }
    Box(
      Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { set(if (volume > 0f) 0f else beforeMute) },
      contentAlignment = Alignment.Center,
    ) {
      Text(if (volume == 0f) "🔇" else if (volume < 0.34f) "🔈" else if (volume < 0.67f) "🔉" else "🔊", fontSize = 18.sp)
    }
  }
}
