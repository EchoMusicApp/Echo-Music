@file:OptIn(ExperimentalMaterial3Api::class)

package androidx.compose.material3

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.getValue
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Desktop versions of the Material 3 "expressive" components. Compose Desktop's material3 does not expose them yet,
// so these are plain look-alikes built from Surface/Button, in the same package so the original imports keep working.
// shortcut: no press/checked shape morphing and no loading-indicator morph animation; replace with the real ones once
// Compose Multiplatform ships them.

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalMaterial3ExpressiveApi

class ToggleButtonShapes(val shape: Shape)

class ToggleButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object ButtonGroupDefaults {
    val ConnectedSpaceBetween = 2.dp
    private val outer = 20.dp
    private val inner = 4.dp
    fun connectedLeadingButtonShapes() = ToggleButtonShapes(RoundedCornerShape(outer, inner, inner, outer))
    fun connectedMiddleButtonShapes() = ToggleButtonShapes(RoundedCornerShape(inner))
    fun connectedTrailingButtonShapes() = ToggleButtonShapes(RoundedCornerShape(inner, outer, outer, inner))
}

object ToggleButtonDefaults {
    val IconSpacing = 8.dp
    @Composable
    fun toggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ) = ToggleButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)
}

@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 48.dp).clickable(enabled = enabled, role = Role.Button) { onCheckedChange(!checked) },
        shape = shapes.shape,
        color = if (enabled) colors.containerColor else colors.disabledContainerColor,
        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun SplitButtonLayout(
    leadingButton: @Composable () -> Unit,
    trailingButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        leadingButton()
        trailingButton()
    }
}

object SplitButtonDefaults {
    val TrailingIconSize = 20.dp
    private val leadingShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
    private val trailingShape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp)

    @Composable
    fun LeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        content: @Composable RowScope.() -> Unit,
    ) = Button(onClick, modifier, enabled, leadingShape, colors, content = content)

    @Composable
    fun TrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        content: @Composable () -> Unit,
    ) = Surface(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 40.dp).clickable(enabled = enabled, role = Role.Button) { onCheckedChange(!checked) },
        shape = trailingShape,
        color = if (enabled) colors.containerColor else colors.disabledContainerColor,
        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
    ) {
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    CircularProgressIndicator(modifier = modifier, color = if (color == Color.Unspecified) LocalContentColor.current else color)
}

// ---- floating toolbar ----
interface FloatingToolbarScrollBehavior

class FloatingToolbarColors(val toolbarContainerColor: Color, val toolbarContentColor: Color)

object FloatingToolbarDefaults {
    @Composable
    fun standardFloatingToolbarColors(
        toolbarContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        toolbarContentColor: Color = MaterialTheme.colorScheme.onSurface,
    ) = FloatingToolbarColors(toolbarContainerColor, toolbarContentColor)
}

@Composable
fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(modifier, shape = RoundedCornerShape(percent = 50), color = colors.toolbarContainerColor, contentColor = colors.toolbarContentColor) {
        Row(Modifier.padding(contentPadding).defaultMinSize(minHeight = 64.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

// ---- wavy progress ----
object WavyProgressIndicatorDefaults {
    val LinearTrackStopIndicatorSize = 4.dp
    val LinearDeterminateWavelength = 40.dp
    val LinearContainerHeight = 16.dp
}

// shortcut: a sine path drawn on a Canvas with a moving phase, not the exact Material shape (no amplitude easing at the ends).
@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
    stroke: Stroke,
    trackStroke: Stroke,
    gapSize: Dp,
    stopSize: Dp,
    amplitude: (Float) -> Float,
    wavelength: Dp,
    waveSpeed: Dp,
) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    Canvas(modifier.fillMaxWidth().height(WavyProgressIndicatorDefaults.LinearContainerHeight)) {
        val p = progress().coerceIn(0f, 1f)
        val cy = size.height / 2
        val gap = gapSize.toPx()
        val endX = (size.width * p - gap).coerceAtLeast(0f)
        val startInactive = (size.width * p + gap).coerceAtMost(size.width)
        if (startInactive < size.width) drawLine(trackColor, Offset(startInactive, cy), Offset(size.width, cy), trackStroke.width, trackStroke.cap)
        if (endX > 0f) {
            val amp = amplitude(p) * (size.height / 2 - stroke.width / 2).coerceAtLeast(0f)
            val wl = wavelength.toPx()
            val path = Path().apply {
                moveTo(0f, cy)
                var x = 0f
                while (x <= endX) { lineTo(x, cy + amp * sin(2 * PI * (x / wl - phase)).toFloat()); x += 2f }
            }
            drawPath(path, color, style = stroke)
        }
    }
}

// ---- slider track: used in place of SliderDefaults.Track(...) because the library's overload with trackCornerSize is internal ----
@Composable
fun DesktopSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
    thumbTrackGapSize: Dp = 6.dp,
    trackCornerSize: Dp = 12.dp,
    drawStopIndicator: (DrawScope.(Offset) -> Unit)? = null,
) {
    Canvas(modifier.fillMaxWidth()) {
        val range = sliderState.valueRange
        val fraction = ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        val gap = thumbTrackGapSize.toPx()
        val r = CornerRadius(trackCornerSize.toPx())
        val activeEnd = (size.width * fraction - gap).coerceAtLeast(0f)
        val inactiveStart = (size.width * fraction + gap).coerceAtMost(size.width)
        val active = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
        val inactive = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor
        if (activeEnd > 0f) drawRoundRect(active, Offset.Zero, Size(activeEnd, size.height), r)
        if (inactiveStart < size.width) drawRoundRect(inactive, Offset(inactiveStart, 0f), Size(size.width - inactiveStart, size.height), r)
        drawStopIndicator?.invoke(this, Offset(size.width - gap, size.height / 2))
    }
}

@Composable
fun CircularWavyProgressIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    CircularProgressIndicator(modifier = modifier, color = if (color == Color.Unspecified) LocalContentColor.current else color)
}

@Composable
fun ContainedLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

