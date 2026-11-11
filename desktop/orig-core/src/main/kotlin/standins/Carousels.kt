package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Home's big-card carousels as plain scrolling rows. The Material carousel did not draw its cards on desktop,
 * so HomeScreen is patched to use these (same names, so the call sites stay as they are).
 * shortcut: no centre-zoom / edge-shrink effect, cards are a fixed width; upgrade if the Material carousel starts working on desktop.
 */
class CarouselState(val list: LazyListState, val count: () -> Int) {
  val currentItem get() = list.firstVisibleItemIndex
  val isScrollInProgress get() = list.isScrollInProgress
  suspend fun scrollToItem(index: Int) = list.scrollToItem(index)
  suspend fun animateScrollToItem(index: Int) = list.animateScrollToItem(index)
}

@Composable
fun rememberCarouselState(itemCount: () -> Int): CarouselState = remember { CarouselState(LazyListState(), itemCount) }

@Composable
fun HorizontalCenteredHeroCarousel(
  state: CarouselState,
  maxItemWidth: Dp,
  modifier: Modifier = Modifier,
  itemSpacing: Dp = 0.dp,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  content: @Composable (Int) -> Unit,
) = Cards(state, minOf(maxItemWidth, 560.dp), modifier, itemSpacing, contentPadding, content)

@Composable
fun HorizontalMultiBrowseCarousel(
  state: CarouselState,
  preferredItemWidth: Dp,
  modifier: Modifier = Modifier,
  itemSpacing: Dp = 0.dp,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  content: @Composable (Int) -> Unit,
) = Cards(state, preferredItemWidth, modifier, itemSpacing, contentPadding, content)

@Composable
private fun Cards(state: CarouselState, width: Dp, modifier: Modifier, spacing: Dp, padding: PaddingValues, content: @Composable (Int) -> Unit) {
  LazyRow(modifier, state.list, padding, horizontalArrangement = Arrangement.spacedBy(spacing)) {
    items((0 until state.count()).toList()) { i -> Box(Modifier.width(width).fillMaxHeight()) { content(i) } }
  }
}
