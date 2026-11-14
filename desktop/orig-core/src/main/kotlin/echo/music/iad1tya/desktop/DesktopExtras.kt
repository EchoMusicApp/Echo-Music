package echo.music.iad1tya.desktop

import android.widget.ToastBus
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Small pill with the latest Toast message (restore results, errors...). */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
  var text by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) { ToastBus.messages.collectLatest { text = it; delay(4000); text = null } }
  AnimatedVisibility(text != null, modifier, enter = fadeIn(), exit = fadeOut()) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 6.dp) {
      Text(text ?: "", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.inverseOnSurface)
    }
  }
}

/**
 * Touch screens (and mice) deliver drags as plain mouse input on desktop, which Compose never turns into scrolling.
 * A drag that nobody else used is replayed as mouse-wheel events at the finger, so every list/menu that scrolls with the wheel scrolls by touch too.
 */
@Composable
fun Modifier.touchDragScroll(window: java.awt.Window): Modifier {
  val density = LocalDensity.current.density
  val st = remember { DragState() }
  return this
    // before the children: after a scroll-drag swallow the release so the row under the finger is not "clicked"
    .pointerInput(Unit) {
      awaitPointerEventScope {
        while (true) {
          val e = awaitPointerEvent(PointerEventPass.Initial)
          if (st.dragging && e.type == PointerEventType.Release) e.changes.forEach { it.consume() }
        }
      }
    }
    // after the children: only drags that no slider / draggable took
    .pointerInput(Unit) {
      awaitPointerEventScope {
        while (true) {
          val e = awaitPointerEvent(PointerEventPass.Final)
          val c = e.changes.firstOrNull() ?: continue
          when (e.type) {
            PointerEventType.Press -> { st.reset(); st.last = c.position; st.start = c.position }
            PointerEventType.Release -> st.reset()
            PointerEventType.Move -> if (c.pressed && e.buttons.isPrimaryPressed) {
              val total = c.position - st.start
              if (st.axis == 0 && (abs(total.x) > 10 * density || abs(total.y) > 10 * density)) {
                st.axis = if (abs(total.y) >= abs(total.x)) 1 else 2
                // sliders (horizontal) keep their drag; vertical drags are always scrolls
                st.blocked = st.axis == 2 && c.isConsumed
              }
              if (st.axis != 0 && !st.blocked) {
                st.dragging = true
                val d = c.position - st.last
                sendWheel(window, c.position, if (st.axis == 1) d.y else d.x, st.axis == 2, density)
              }
              st.last = c.position
            }
            else -> {}
          }
        }
      }
    }
}

private class DragState {
  var dragging = false; var axis = 0; var blocked = false
  var last = Offset.Zero; var start = Offset.Zero
  fun reset() { dragging = false; axis = 0; blocked = false }
}

private fun sendWheel(window: java.awt.Window, pos: Offset, deltaPx: Float, horizontal: Boolean, density: Float) {
  if (deltaPx == 0f) return
  val root = (window as? javax.swing.RootPaneContainer)?.contentPane ?: window
  val ax = (pos.x / density).toInt(); val ay = (pos.y / density).toInt()
  val comp = SwingUtilities.getDeepestComponentAt(root, ax, ay) ?: return
  val p = SwingUtilities.convertPoint(root, ax, ay, comp)
  val precise = (-deltaPx / (64f * density)).toDouble() // finger up (negative) = scroll down (positive rotation); ~64dp per wheel notch
  val rotation = if (precise > 0) kotlin.math.ceil(precise).toInt() else kotlin.math.floor(precise).toInt()
  val mods = if (horizontal) java.awt.event.InputEvent.SHIFT_DOWN_MASK else 0
  comp.dispatchEvent(
    MouseWheelEvent(comp, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), mods, p.x, p.y, ax, ay, 0, false,
      MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation, precise)
  )
}
