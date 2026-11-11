package echo.music.iad1tya.ui.screens

import androidx.compose.material3.carousel.CarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Mouse wheel over a carousel moves it one card sideways (a notch of the wheel = one card). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.carouselWheel(state: CarouselState, count: Int): Modifier {
  val scope = rememberCoroutineScope()
  var last by remember { mutableStateOf(0L) }
  return onPointerEvent(PointerEventType.Scroll) { e ->
    val c = e.changes.first()
    val d = c.scrollDelta
    val step = if (abs(d.x) > abs(d.y)) d.x else d.y
    val now = System.currentTimeMillis()
    // shortcut: fixed 200 ms gap so a fast wheel spin does not skip cards; no momentum
    if (step != 0f && now - last > 200 && count > 0) {
      last = now
      scope.launch { state.animateScrollToItem((state.currentItem + if (step > 0) 1 else -1).coerceIn(0, count - 1)) }
    }
    c.consume()
  }
}
