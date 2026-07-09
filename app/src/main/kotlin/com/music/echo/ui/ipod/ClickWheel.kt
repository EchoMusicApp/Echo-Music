package iad1tya.echo.music.ui.ipod

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.R
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Realistic iPod click wheel with circular gesture detection.
 * ponytail: Canvas + pointerInput, no custom gesture detector class.
 *
 * @param onScrollUp called when counter-clockwise rotation detected
 * @param onScrollDown called when clockwise rotation detected
 * @param onCenter center button click
 * @param onMenu menu button (top)
 * @param onPlayPause play/pause button (bottom)
 * @param onPrevious previous button (left)
 * @param onNext next button (right)
 * @param hapticsEnabled whether to vibrate on scroll ticks
 */
@Composable
fun ClickWheel(
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onCenter: () -> Unit,
    onMenu: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = LocalIpodColors.current
    val view = LocalView.current

    // Track accumulated angle for scroll detection
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var accumulatedAngle by remember { mutableFloatStateOf(0f) }
    // ponytail: 30° per tick, feels right on real hardware
    val degreesPerTick = 30f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 32.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer wheel — touch surface for circular gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(colors.wheelBackground)
                .pointerInput(hapticsEnabled) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            lastAngle = atan2(
                                (offset.y - cy).toDouble(),
                                (offset.x - cx).toDouble()
                            ).toFloat()
                            accumulatedAngle = 0f
                        },
                        onDrag = { change, _ ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            val dist = sqrt(dx * dx + dy * dy)

                            val currentAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()

                            // Only track if finger is in the ring area (not center button)
                            val innerRadius = size.width * 0.2f
                            val outerRadius = size.width * 0.5f
                            if (dist < innerRadius || dist > outerRadius) {
                                lastAngle = currentAngle
                                accumulatedAngle = 0f
                                return@detectDragGestures
                            }

                            var delta = currentAngle - lastAngle

                            // Handle wrap-around at ±π
                            if (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
                            if (delta < -Math.PI) delta += (2 * Math.PI).toFloat()

                            accumulatedAngle += Math.toDegrees(delta.toDouble()).toFloat()
                            lastAngle = currentAngle

                            if (accumulatedAngle >= degreesPerTick) {
                                onScrollDown()
                                accumulatedAngle -= degreesPerTick
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            } else if (accumulatedAngle <= -degreesPerTick) {
                                onScrollUp()
                                accumulatedAngle += degreesPerTick
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                    )
                }
                .semantics { contentDescription = "Click Wheel. Rotate clockwise to scroll down, counter-clockwise to scroll up." },
            contentAlignment = Alignment.Center,
        ) {
            // Menu button (top)
            Text(
                text = "MENU",
                color = colors.wheelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .padding(12.dp)
                    .clickable { onMenu() }
                    .semantics { contentDescription = "Menu button" },
            )

            // Previous button (left)
            Text(
                text = "◀◀",
                color = colors.wheelText,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .padding(12.dp)
                    .clickable { onPrevious() }
                    .semantics { contentDescription = "Previous track" },
            )

            // Next button (right)
            Text(
                text = "▶▶",
                color = colors.wheelText,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .padding(12.dp)
                    .clickable { onNext() }
                    .semantics { contentDescription = "Next track" },
            )

            // Play/Pause button (bottom)
            Text(
                text = "▶❚❚",
                color = colors.wheelText,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .padding(12.dp)
                    .clickable { onPlayPause() }
                    .semantics { contentDescription = "Play or Pause" },
            )

            // Center button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.wheelButtonBackground)
                    .clickable { onCenter() }
                    .semantics { contentDescription = "Select button" },
            )
        }
    }
}
