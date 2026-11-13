package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
fun rememberWheelState(itemCount: () -> Int): WheelState {
  val list = rememberLazyListState()
  return remember { WheelState(list, itemCount) }
}

@Composable
fun WheelCarousel(
  state: WheelState,
  maxItemWidth: Dp,
  modifier: Modifier = Modifier,
  itemSpacing: Dp = 0.dp,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  content: @Composable (Int) -> Unit,
) {
  BoxWithConstraints(modifier) {
    val itemWidth = minOf(maxItemWidth, maxWidth)
    val side = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp) // lets the first and last card reach the middle
    LazyRow(
      state = state.list,
      contentPadding = PaddingValues(horizontal = side),
      horizontalArrangement = Arrangement.spacedBy(itemSpacing),
      flingBehavior = rememberSnapFlingBehavior(state.list),
    ) {
      items((0 until state.count()).toList()) { i ->
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
  }
}
