package androidx.compose.material3

import androidx.compose.foundation.clickable
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
