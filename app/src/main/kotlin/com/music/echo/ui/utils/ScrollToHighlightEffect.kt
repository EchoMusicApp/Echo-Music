package echo.music.iad1tya.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Modifier.scrollToOnHighlight(
    scrollState: ScrollState,
    isHighlighted: Boolean,
    delayMs: Long = 300L
): Modifier {
    val targetScroll = remember { mutableStateOf<Int?>(null) }
    
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    
    // We want the item to be positioned roughly in the center of the screen
    val targetScreenY = screenHeightPx / 2f

    LaunchedEffect(isHighlighted, targetScroll.value) {
        if (isHighlighted && targetScroll.value != null) {
            delay(delayMs) // Wait for layout/animations
            scrollState.animateScrollTo(targetScroll.value!!)
        }
    }

    return if (isHighlighted) {
        this.onGloballyPositioned { coordinates ->
            if (targetScroll.value == null) {
                // The current absolute screen position of the item
                val currentScreenY = coordinates.positionInWindow().y
                
                // How much we need to shift the scroll to make currentScreenY == targetScreenY
                val scrollDelta = currentScreenY - targetScreenY
                
                // Target scroll value is current scroll + delta
                var newScroll = scrollState.value + scrollDelta.toInt()
                if (newScroll < 0) newScroll = 0
                
                targetScroll.value = newScroll
            }
        }
    } else {
        this
    }
}
