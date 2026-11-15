package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Home's top cards as a "wheel": the card in the middle is full size, the ones beside it shrink and fade,
 * and a swipe, drag or mouse wheel snaps the next card into the middle. Replaces the Material hero carousel,
 * which on a wide desktop window drew every card the same size.
 * shortcut: scaling only (no corner-shape morphing); upgrade if the Material carousel gets a proper desktop layout.
 */
class WheelState(val list: LazyListState, val count: () -> Int) {
  /** The card closest to the middle of the row. */
  val currentItem: Int
    get() {
      val info = list.layoutInfo
      val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
      return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - mid) }?.index ?: 0
    }
  val isScrollInProgress get() = list.isScrollInProgress
  suspend fun scrollToItem(index: Int) = list.scrollToItem(index)
  suspend fun animateScrollToItem(index: Int) = list.animateScrollToItem(index)
}

@Composable
fun rememberWheelState(initialItem: Int = 0, itemCount: () -> Int): WheelState {
  val list = rememberLazyListState(initialFirstVisibleItemIndex = initialItem)
  return remember { WheelState(list, itemCount) }
}

@Composable
fun WheelCarousel(
  state: WheelState,
  maxItemWidth: Dp,
  modifier: Modifier = Modifier,
  itemSpacing: Dp = 0.dp,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  realCount: Int = 0,   // number of distinct cards when the list loops; > 1 adds the bar and < > buttons
  content: @Composable (Int) -> Unit,
) {
  val hover = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  val hovered by hover.collectIsHoveredAsState()
  val scope = rememberCoroutineScope()
  val hasChrome = realCount > 1
  BoxWithConstraints(modifier.hoverable(hover)) {
    val itemWidth = minOf(maxItemWidth, maxWidth)
    val side = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp) // lets the first and last card reach the middle
    LazyRow(
      modifier = if (hasChrome) Modifier.padding(bottom = 22.dp) else Modifier,
      state = state.list,
      contentPadding = PaddingValues(horizontal = side),
      horizontalArrangement = Arrangement.spacedBy(itemSpacing),
      flingBehavior = rememberSnapFlingBehavior(state.list),
    ) {
      items(state.count()) { i ->
        Box(
          Modifier.width(itemWidth).fillMaxHeight().graphicsLayer {
            val info = state.list.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == i }
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val away = if (item == null) 1f else (abs(item.offset + item.size / 2f - mid) / (item.size + itemSpacing.toPx())).coerceIn(0f, 1f)
            val scale = 1f - 0.22f * away
            scaleX = scale; scaleY = scale; alpha = 1f - 0.4f * away
          },
        ) { content(i) }
      }
    }
    if (hasChrome) {
      val cur = state.currentItem
      val idx = cur % realCount
      val frac = 1f / realCount
      var dragging by remember { mutableStateOf(false) }
      fun goTo(target: Int) { scope.launch { state.list.scrollToItem(cur - idx + target.coerceIn(0, realCount - 1)) } }
      BoxWithConstraints(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).height(18.dp)
          .pointerInput(realCount) {
            detectTapGestures { o -> goTo((o.x / size.width * realCount).toInt()) }
          }
          .pointerInput(realCount) {
            detectHorizontalDragGestures(onDragStart = { dragging = true }, onDragEnd = { dragging = false }, onDragCancel = { dragging = false }) { change, _ ->
              change.consume(); goTo((change.position.x / size.width * realCount).toInt())
            }
          },
        contentAlignment = Alignment.CenterStart,
      ) {
        val trackW = maxWidth
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
        Box(
          Modifier.offset(x = trackW * frac * idx).width(trackW * frac).height(if (hovered || dragging) 6.dp else 4.dp).clip(CircleShape)
            .background(if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = if (hovered) 0.6f else 0.35f)),
        )
      }
      if (hovered) {
        HeroNavButton(echo.music.iad1tya.R.drawable.arrow_back, Modifier.align(Alignment.CenterStart).padding(start = 6.dp, bottom = 22.dp)) { scope.launch { state.list.animateScrollToItem(cur - 1) } }
        HeroNavButton(echo.music.iad1tya.R.drawable.arrow_forward, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp, bottom = 22.dp)) { scope.launch { state.list.animateScrollToItem(cur + 1) } }
      }
    }
  }
}

@Composable
private fun HeroNavButton(icon: Int, modifier: Modifier, onClick: () -> Unit) {
  Box(
    modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)).clickable { onClick() },
    contentAlignment = Alignment.Center,
  ) { androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface) }
}
