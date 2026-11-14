package echo.music.iad1tya.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import echo.music.iad1tya.R

/**
 * Volume button for the player's bottom row, drawn like its neighbours (outlined, filled when muted).
 * Hovering it slides out a small horizontal volume bar to the right.
 */
@Composable
fun PlayerVolumeButton(
  player: Player,
  buttonSize: Dp,
  shape: RoundedCornerShape,
  textButtonColor: Color,
  iconButtonColor: Color,
  iconSize: Dp,
) {
  var volume by remember { mutableStateOf(player.volume) }
  var lastLoud by remember { mutableStateOf(if (player.volume > 0f) player.volume else 1f) }
  val hover = remember { MutableInteractionSource() }
  val hovered by hover.collectIsHoveredAsState()
  var dragging by remember { mutableStateOf(false) }
  val muted = volume <= 0f
  fun set(v: Float) { volume = v; player.volume = v; if (v > 0f) lastLoud = v }

  // one hover area around button + bar, so moving onto the bar doesn't hide it
  Row(Modifier.hoverable(hover), verticalAlignment = Alignment.CenterVertically) {
    val buttonShape = if (hovered || dragging) RoundedCornerShape(3.dp) else shape
    val base = Modifier.size(buttonSize).clip(buttonShape).clickable { set(if (muted) lastLoud else 0f) }
    Box(
      if (muted) base.background(textButtonColor) else base.border(1.dp, textButtonColor.copy(alpha = 0.3f), buttonShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(
          when { muted -> R.drawable.volume_off; volume < 0.5f -> R.drawable.volume_down; else -> R.drawable.volume_up }
        ),
        contentDescription = null,
        modifier = Modifier.size(iconSize),
        tint = if (muted) iconButtonColor else textButtonColor,
      )
    }
    AnimatedVisibility(
      visible = hovered || dragging,
      enter = expandHorizontally() + fadeIn(),
      exit = shrinkHorizontally() + fadeOut(),
    ) {
      val barShape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 50.dp, bottomEnd = 50.dp)
      Box(
        Modifier.padding(start = 3.dp).size(width = 130.dp, height = buttonSize).clip(barShape)
          .border(1.dp, textButtonColor.copy(alpha = 0.3f), barShape),
        contentAlignment = Alignment.Center,
      ) {
        Slider(
          value = volume,
          onValueChange = { dragging = true; set(it) },
          onValueChangeFinished = { dragging = false },
          valueRange = 0f..1f,
          modifier = Modifier.padding(horizontal = 14.dp).width(100.dp),
          colors = SliderDefaults.colors(
            thumbColor = textButtonColor,
            activeTrackColor = textButtonColor,
            inactiveTrackColor = textButtonColor.copy(alpha = 0.25f),
          ),
        )
      }
    }
  }
}

/** Volume entry for the left rail (icon + label like History/Stats/Settings). Hovering it slides out a horizontal bar to its right. */
@Composable
fun RailVolumeItem(player: Player) {
  var volume by remember { mutableStateOf(player.volume) }
  var lastLoud by remember { mutableStateOf(if (player.volume > 0f) player.volume else 1f) }
  val itemHover = remember { MutableInteractionSource() }
  val barHover = remember { MutableInteractionSource() }
  val onItem by itemHover.collectIsHoveredAsState()
  val onBar by barHover.collectIsHoveredAsState()
  var dragging by remember { mutableStateOf(false) }
  val wanted = onItem || onBar || dragging
  // small delay before hiding so the pointer can travel from the item to the bar (separate layers)
  var show by remember { mutableStateOf(false) }
  LaunchedEffect(wanted) { if (wanted) show = true else { kotlinx.coroutines.delay(250); show = false } }
  val muted = volume <= 0f
  fun set(v: Float) { volume = v; player.volume = v; if (v > 0f) lastLoud = v }

  Box(Modifier.hoverable(itemHover)) {
    androidx.compose.material3.NavigationRailItem(
      selected = false,
      onClick = { set(if (muted) lastLoud else 0f) },
      icon = {
        Icon(painterResource(when { muted -> R.drawable.volume_off; volume < 0.5f -> R.drawable.volume_down; else -> R.drawable.volume_up }), null)
      },
      label = { androidx.compose.material3.Text("Volume") },
    )
    if (show) {
      androidx.compose.ui.window.Popup(
        popupPositionProvider = object : androidx.compose.ui.window.PopupPositionProvider {
          override fun calculatePosition(
            anchorBounds: androidx.compose.ui.unit.IntRect, windowSize: androidx.compose.ui.unit.IntSize,
            layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize,
          ) = androidx.compose.ui.unit.IntOffset(anchorBounds.right, anchorBounds.center.y - popupContentSize.height / 2)
        },
      ) {
        val pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
        Box(
          Modifier.hoverable(barHover).size(width = 170.dp, height = 48.dp).clip(pill)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          Slider(
            value = volume,
            onValueChange = { dragging = true; set(it) },
            onValueChangeFinished = { dragging = false },
            valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 18.dp).width(134.dp),
          )
        }
      }
    }
  }
}
