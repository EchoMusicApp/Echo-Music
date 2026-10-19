package echo.music.iad1tya.ui.buddy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Liquid-glass surface used by every control of the floating companion: a clear body with a soft
 * accent glow near the bottom edge, a bright light-to-accent rim and a small specular streak.
 */
fun Modifier.buddyGlass(shape: Shape, accent: Color, tint: Color = Color.White, strength: Float = 1f): Modifier =
  this.clip(shape).drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val w = size.width
    val h = size.height
    drawOutline(outline, Brush.verticalGradient(listOf(tint.copy(alpha = 0.20f * strength), tint.copy(alpha = 0.06f * strength))))
    drawOutline(
      outline,
      Brush.radialGradient(
        listOf(accent.copy(alpha = 0.30f * strength), Color.Transparent),
        center = Offset(w * 0.5f, h * 1.08f),
        radius = max(w, h) * 0.8f,
      ),
    )
    drawOutline(
      outline,
      Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.75f * strength), Color.White.copy(alpha = 0.05f), accent.copy(alpha = 0.70f * strength)),
        start = Offset.Zero,
        end = Offset(w, h),
      ),
      style = Stroke(width = 1.2.dp.toPx()),
    )
    if (h > 26.dp.toPx()) {
      drawRoundRect(
        Color.White.copy(alpha = 0.42f * strength),
        topLeft = Offset(w * 0.18f, h * 0.07f),
        size = Size(w * 0.64f, max(2f, h * 0.06f)),
        cornerRadius = CornerRadius(h * 0.04f),
      )
    }
  }

@Composable
fun GlassCircleButton(
  icon: ImageVector,
  description: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: androidx.compose.ui.unit.Dp = 44.dp,
  active: Boolean = false,
  enabled: Boolean = true,
) {
  Box(
    modifier
      .size(size)
      .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
      .buddyGlass(CircleShape, accent, strength = if (active) 1.6f else 1f)
      .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(size * 0.5f))
  }
}

@Composable
fun GlassChip(text: String, accent: Color, onClick: () -> Unit) {
  Text(
    text,
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    modifier =
      Modifier.buddyGlass(RoundedCornerShape(50), accent, strength = 0.9f)
        .clickable(onClick = onClick)
        .padding(horizontal = 13.dp, vertical = 8.dp),
  )
}

/** Liquid-glass text box. Multi-line (up to 4 lines), Send on the keyboard. */
@Composable
fun GlassTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  accent: Color,
  onSend: () -> Unit,
  modifier: Modifier = Modifier,
  focusRequester: FocusRequester? = null,
) {
  Box(
    modifier.buddyGlass(RoundedCornerShape(26.dp), accent, strength = 0.85f).padding(horizontal = 18.dp, vertical = 12.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (value.isEmpty()) Text(placeholder, color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
      cursorBrush = SolidColor(accent),
      maxLines = 4,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
      keyboardActions = KeyboardActions(onSend = { onSend() }),
      modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
    )
  }
}

/** Three bouncing dots shown while the companion is thinking. */
@Composable
fun TypingDots(accent: Color) {
  val inf = rememberInfiniteTransition(label = "dots")
  val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "dotsT")
  Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 6.dp)) {
    for (i in 0..2) {
      val phase = ((t + (2 - i) * 0.2f) % 1f)
      val lift = kotlin.math.sin(phase * Math.PI.toFloat()).coerceAtLeast(0f)
      Box(
        Modifier.size(8.dp)
          .graphicsLayer { translationY = -6.dp.toPx() * lift; alpha = 0.45f + 0.55f * lift }
          .clip(CircleShape)
          .background(Color.White)
      )
    }
  }
}

@Composable
fun GlassBubble(text: String, fromUser: Boolean, accent: Color, isError: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
  val shape = if (fromUser) RoundedCornerShape(20.dp, 6.dp, 20.dp, 20.dp) else RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp)
  Box(
    modifier
      .buddyGlass(shape, if (isError) Color(0xFFFF6B7A) else accent, tint = if (fromUser) accent else Color.White, strength = if (fromUser) 1.3f else 0.9f)
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    if (text.isEmpty()) content()
    else Text(text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
  }
}
