package iad1tya.echo.music.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.delay

@Composable
fun Modifier.scrollToOnHighlight(
    scrollState: ScrollState,
    isHighlighted: Boolean,
    delayMs: Long = 300L
): Modifier {
    val targetY = remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(isHighlighted, targetY.value) {
        if (isHighlighted && targetY.value != null) {
            delay(delayMs) // Wait for layout/animations
            scrollState.animateScrollTo(targetY.value!! - 200) // Offset slightly so it's not at the very top
        }
    }

    return if (isHighlighted) {
        this.onGloballyPositioned { coordinates ->
            if (targetY.value == null) {
                targetY.value = coordinates.positionInParent().y.toInt()
            }
        }
    } else {
        this
    }
}
