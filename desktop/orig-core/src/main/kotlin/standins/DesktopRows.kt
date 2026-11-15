package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import kotlin.math.max
import kotlinx.coroutines.launch

/**
 * Desktop versions of LazyRow / LazyHorizontalGrid: same layout, plus
 *  - drag the row sideways with the mouse,
 *  - "<" / ">" buttons that appear while the pointer is over the row (for track pads),
 *  - a thin scroll bar along the bottom that can be dragged.
 * All colours come from MaterialTheme, so they follow whatever theme is picked in Settings.
 */
private interface RowScroller {
  val contentPx: Float   // estimated total length
  val offsetPx: Float    // how far scrolled
  val viewportPx: Float
  suspend fun scrollBy(px: Float)
}

@Composable
private fun RowChrome(s: RowScroller, modifier: Modifier, inner: @Composable (Modifier) -> Unit) {
  val scope = rememberCoroutineScope()
  val hover = remember { MutableInteractionSource() }
  val hovered by hover.collectIsHoveredAsState()
  val maxOffset = (s.contentPx - s.viewportPx).coerceAtLeast(0f)
  val scrollable = maxOffset > 4f
  Box(modifier.hoverable(hover)) {
    inner(Modifier.pointerInput(Unit) {
      detectHorizontalDragGestures { change, dx -> change.consume(); scope.launch { s.scrollBy(-dx) } }
    })
    if (scrollable) {
      val barColor = MaterialTheme.colorScheme.onSurface
      var dragging by remember { mutableStateOf(false) }
      val frac = (s.viewportPx / s.contentPx).coerceIn(0.08f, 1f)
      val pos = if (maxOffset > 0f) (s.offsetPx / maxOffset).coerceIn(0f, 1f) else 0f
      // bar
      BoxWithConstraints(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).height(10.dp)
          .pointerInput(Unit) {
            detectHorizontalDragGestures(onDragStart = { dragging = true }, onDragEnd = { dragging = false }, onDragCancel = { dragging = false }) { change, dx ->
              change.consume()
              val trackPx = size.width.toFloat()
              // dragging the thumb by dx moves the content by dx / (track free space) * maxOffset
              val free = (trackPx * (1f - frac)).coerceAtLeast(1f)
              scope.launch { s.scrollBy(dx / free * maxOffset) }
            }
          },
        contentAlignment = Alignment.CenterStart,
      ) {
        val trackW = maxWidth
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
        Box(
          Modifier.offset(x = (trackW * (1f - frac)) * pos).width(trackW * frac).height(if (hovered || dragging) 6.dp else 4.dp).clip(CircleShape)
            .background(if (dragging) MaterialTheme.colorScheme.primary else barColor.copy(alpha = if (hovered) 0.6f else 0.35f)),
        )
      }
      // buttons
      if (hovered) {
        val canBack = s.offsetPx > 2f
        val canFwd = s.offsetPx < maxOffset - 2f
        if (canBack) NavButton(R.drawable.arrow_back, Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) { scope.launch { s.scrollBy(-s.viewportPx * 0.8f) } }
        if (canFwd) NavButton(R.drawable.arrow_forward, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) { scope.launch { s.scrollBy(s.viewportPx * 0.8f) } }
      }
    }
  }
}

@Composable
private fun NavButton(icon: Int, modifier: Modifier, onClick: () -> Unit) {
  Box(
    modifier.size(40.dp).clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f))
      .pointerInput(Unit) { androidx.compose.foundation.gestures.detectTapGestures { onClick() } },
    contentAlignment = Alignment.Center,
  ) { Icon(painterResource(icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface) }
}

private class ListScroller(val st: LazyListState) : RowScroller {
  private val avg: Float get() { val v = st.layoutInfo.visibleItemsInfo; return if (v.isEmpty()) 1f else v.sumOf { it.size }.toFloat() / v.size }
  override val contentPx get() = avg * st.layoutInfo.totalItemsCount
  override val offsetPx get() = avg * st.firstVisibleItemIndex + st.firstVisibleItemScrollOffset
  override val viewportPx get() = (st.layoutInfo.viewportEndOffset - st.layoutInfo.viewportStartOffset).toFloat()
  override suspend fun scrollBy(px: Float) { st.animateScrollBy(px) }
}

private class GridScroller(val st: LazyGridState, val lines: Int) : RowScroller {
  private val avg: Float get() { val v = st.layoutInfo.visibleItemsInfo; return if (v.isEmpty()) 1f else v.sumOf { it.size.width }.toFloat() / v.size }
  override val contentPx get() = avg * ((st.layoutInfo.totalItemsCount + lines - 1) / lines)
  override val offsetPx get() = avg * (st.firstVisibleItemIndex / lines) + st.firstVisibleItemScrollOffset
  override val viewportPx get() = (st.layoutInfo.viewportEndOffset - st.layoutInfo.viewportStartOffset).toFloat()
  override suspend fun scrollBy(px: Float) { st.animateScrollBy(px) }
}

@Composable
fun DesktopLazyRow(
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  content: LazyListScope.() -> Unit,
) {
  val sc = remember(state) { ListScroller(state) }
  RowChrome(sc, modifier) { drag ->
    LazyRow(modifier = drag, state = state, contentPadding = contentPadding, horizontalArrangement = horizontalArrangement, content = content)
  }
}

@Composable
fun DesktopLazyHorizontalGrid(
  rows: GridCells,
  modifier: Modifier = Modifier,
  state: LazyGridState = rememberLazyGridState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  flingBehavior: FlingBehavior? = null,
  content: LazyGridScope.() -> Unit,
) {
  val lines = (rows as? GridCells.Fixed)?.count ?: 1
  val sc = remember(state, lines) { GridScroller(state, max(1, lines)) }
  RowChrome(sc, modifier) { drag ->
    LazyHorizontalGrid(rows = rows, modifier = drag.fillMaxSize(), state = state, contentPadding = contentPadding, flingBehavior = flingBehavior ?: ScrollableDefaults.flingBehavior(), content = content)
  }
}
