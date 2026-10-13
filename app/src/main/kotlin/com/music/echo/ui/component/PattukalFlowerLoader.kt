package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val CubeBlue = Color(0xFF275EFE)
private val CubeBlueLight = Color(0xFF2F71FF)

@Composable
fun PattukalFlowerLoaderOverlay(visible: Boolean) {
  if (!visible) return
  Dialog(
    onDismissRequest = {},
    properties = DialogProperties(
      dismissOnBackPress = false,
      dismissOnClickOutside = false,
      usePlatformDefaultWidth = false
    )
  ) {
    Box(
      modifier = Modifier.fillMaxSize().background(Color(0x24101018)),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        CubeBuildAnimation(Modifier.size(width = 250.dp, height = 250.dp))
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Pattukal",
          fontSize = 30.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.6.sp,
          color = Color.White
        )
        Spacer(Modifier.height(7.dp))
        Text(
          text = "Loading",
          fontSize = 14.sp,
          letterSpacing = 2.sp,
          color = Color(0xFFE5ECFF)
        )
      }
    }
  }
}

@Composable
private fun CubeBuildAnimation(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "pattukal_cube_loader")
  val progress by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "cube_progress"
  )
  val bob by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "cube_bob"
  )

  Canvas(modifier = modifier) {
    val unit = size.minDimension / 9f
    val centerX = size.width / 2f
    val centerY = size.height * 0.48f
    val cube = unit * 0.86f
    val blueGradient = Brush.linearGradient(
      colors = listOf(CubeBlue, CubeBlueLight),
      start = Offset(0f, 0f),
      end = Offset(size.width, size.height)
    )

    // Perspective platform grows in and out like the original CSS loader.
    val platformScale = if (progress in 0.65f..0.92f) 1f else 0.15f
    rotate(degrees = -13f, pivot = Offset(centerX, centerY + unit * 2.2f)) {
      drawRoundRect(
        brush = blueGradient,
        topLeft = Offset(centerX - unit * 2.4f * platformScale, centerY + unit * 1.2f),
        size = Size(unit * 4.8f * platformScale, unit * 1.35f * platformScale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.12f),
        alpha = 0.92f
      )
      drawRoundRect(
        color = CubeBlueLight.copy(alpha = 0.55f),
        topLeft = Offset(centerX - unit * 2.4f * platformScale, centerY + unit * 1.2f),
        size = Size(unit * 2.4f * platformScale, unit * 1.35f * platformScale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.12f)
      )
    }

    val starts = listOf(
      Offset(-unit * 2.6f, -unit * 1.9f),
      Offset(-unit * 3.0f, unit * 1.4f),
      Offset(unit * 1.5f, -unit * 2.5f),
      Offset(unit * 3.0f, -unit * 0.2f),
      Offset(unit * 0.8f, unit * 2.7f),
      Offset(-unit * 2.8f, -unit * 1.2f),
      Offset(-unit * 3.0f, unit * 1.3f),
      Offset(-unit * 2.5f, unit * 2.4f)
    )
    val delays = listOf(0.02f, 0.10f, 0.18f, 0.26f, 0.34f, 0.42f, 0.50f, 0.58f)

    starts.forEachIndexed { index, start ->
      val local = ((progress - delays[index]) / 0.45f).coerceIn(0f, 1f)
      val eased = local * local * (3f - 2f * local)
      val x = centerX + start.x * (1f - eased)
      val y = centerY + start.y * (1f - eased) - if (local > 0.78f) unit * 1.2f * bob else 0f
      val scale = if (local < 0.04f) local / 0.04f else 1f
      if (scale > 0f) {
        val left = x - cube * scale / 2f
        val top = y - cube * scale / 2f
        // Three bright faces mimic the CSS 3D cubes without expensive blur effects.
        drawRect(
          brush = blueGradient,
          topLeft = Offset(left, top),
          size = Size(cube * scale, cube * scale),
          alpha = 0.98f
        )
        drawRect(
          color = Color(0xFF79A4FF).copy(alpha = 0.9f),
          topLeft = Offset(left, top),
          size = Size(cube * scale * 0.36f, cube * scale),
        )
        drawRect(
          color = Color(0xFF1746C7).copy(alpha = 0.9f),
          topLeft = Offset(left, top + cube * scale * 0.76f),
          size = Size(cube * scale, cube * scale * 0.24f)
        )
        drawLine(
          color = Color.White.copy(alpha = 0.24f),
          start = Offset(left + cube * scale * 0.38f, top),
          end = Offset(left + cube * scale, top + cube * scale * 0.38f),
          strokeWidth = unit * 0.035f
        )
      }
    }
  }
}
