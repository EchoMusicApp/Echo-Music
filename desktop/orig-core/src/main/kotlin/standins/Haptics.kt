package com.music.echo.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class HapticType { CLICK, LONG_PRESS, SCROLL_EDGE, SLIDER_TICK }

/** Desktop has no vibration motor; every call does nothing. */
class HapticHelper {
  fun performHaptic(type: HapticType) {}
}

@Composable fun rememberHapticHelper(): HapticHelper = HapticHelper()
fun Modifier.hapticScrollEdge(lazyListState: LazyListState): Modifier = this
fun Modifier.hapticScrollEdge(scrollState: ScrollState): Modifier = this
fun Modifier.hapticScrollEdge(lazyGridState: LazyGridState): Modifier = this
