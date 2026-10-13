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

private val LoaderRose = Color(0xFFFF477E)
private val LoaderGold = Color(0xFFFFD36A)

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
      modifier = Modifier.fillMaxSize().background(Color(0xFF181011)),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        PattukalFlowerMark(Modifier.size(190.dp))
        Spacer(Modifier.height(18.dp))
        Text(
          text = "Pattukal",
          fontSize = 37.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.4.sp,
          color = Color(0xFFFFC0D5)
        )
        Spacer(Modifier.height(12.dp))
        LoadingDots()
      }
    }
  }
}

@Composable
private fun PattukalFlowerMark(modifier: Modifier = Modifier) {
  val motion = rememberInfiniteTransition(label = "pattukal_flower")
  val rotation by motion.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(9000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "flower_rotation"
  )
  val pulse by motion.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(1700, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "flower_pulse"
  )
  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val petalWidth = size.width * 0.22f * pulse
    val petalHeight = size.height * 0.43f * pulse
    val petalBrush = Brush.verticalGradient(
      colors = listOf(Color(0xFFFFD6E1), LoaderRose, Color(0xFFB92967)),
      startY = center.y - petalHeight,
      endY = center.y + petalHeight
    )
    for (ring in 0..2) {
      val count = 10 + ring * 2
      val ringRotation = rotation * (if (ring % 2 == 0) 1f else -0.72f) + ring * 13f
      val width = petalWidth * (1f - ring * 0.13f)
      val height = petalHeight * (1f - ring * 0.12f)
      for (index in 0 until count) {
        rotate(degrees = index * 360f / count + ringRotation, pivot = center) {
          drawOval(
            brush = petalBrush,
            topLeft = Offset(center.x - width / 2f, center.y - height + size.height * 0.025f * ring),
            size = Size(width, height)
          )
        }
      }
    }
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFFF0B2), LoaderGold, Color(0xFFDB8A31)),
        center = center,
        radius = size.minDimension * 0.105f
      ),
      radius = size.minDimension * 0.105f,
      center = center
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.78f),
      radius = size.minDimension * 0.018f,
      center = Offset(center.x - size.width * 0.025f, center.y - size.height * 0.025f)
    )
  }
}

@Composable
private fun LoadingDots() {
  val motion = rememberInfiniteTransition(label = "pattukal_loading_dots")
  val phase by motion.animateFloat(
    initialValue = 0f,
    targetValue = 3f,
    animationSpec = infiniteRepeatable(tween(1050), repeatMode = RepeatMode.Restart),
    label = "loading_dot_phase"
  )
  Text(
    text = "Loading" + ".".repeat(phase.toInt().coerceIn(0, 3)),
    fontSize = 15.sp,
    letterSpacing = 2.sp,
    color = Color(0xFFFFB7C8)
  )
}
