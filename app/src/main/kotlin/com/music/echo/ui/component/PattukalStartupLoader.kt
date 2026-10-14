package echo.music.iad1tya.ui.component

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Set to true by the home screen once its first content (or first load attempt) is done. */
object PattukalStartup {
  val homeReady = MutableStateFlow(false)
}

private const val WORD = "PATTUKAL"

// The CSS loader is authored for a 25.6px font drawn at `scale: 2`, i.e. 51.2 "effective" px.
private const val CSS_FONT_PX = 51.2f
private const val LETTER_CYCLE_MS = 4000f

// Startup timing.
private const val MIN_VISIBLE_MS = 1800L // always show the word long enough to be seen
private const val SETTLE_MS = 350L // let the first frames of the app land before revealing
private const val MAX_WAIT_MS = 5000L // never hold the user hostage if something never loads
private const val FADE_OUT_MS = 450

private val GlowEasing = CubicBezierEasing(0.6f, 0.8f, 0.5f, 1f)

private val GlowYellow = Color(0xFFFFFF00)
private val GlowRed = Color(0xFFFF0000)
private val GlowCyan = Color(0xFF00FFFF)
private val GlowGreen = Color(0xFF00FF00)
private val GlowBlue = Color(0xFF0000FF)

/** State of the startup screen. [progress] goes 1 (fully shown) to 0 (gone). */
class PattukalStartupState internal constructor(
  val progress: Animatable<Float, *>,
  private val finished: State<Boolean>,
) {
  val visible: Boolean get() = !finished.value
}

/**
 * Drives the startup screen: it stays up until the player is bound and the home screen reports its
 * first content (or [MAX_WAIT_MS] passes), shows for at least [MIN_VISIBLE_MS], waits a short
 * [SETTLE_MS] for the UI to settle, then fades out. It only ever runs once per launch.
 */
@Composable
fun rememberPattukalStartup(playerReady: Boolean): PattukalStartupState {
  var finished by rememberSaveable { mutableStateOf(false) }
  val progress = remember { Animatable(if (finished) 0f else 1f) }
  val playerReadyState = rememberUpdatedState(playerReady)

  LaunchedEffect(Unit) {
    if (finished) return@LaunchedEffect
    val start = SystemClock.elapsedRealtime()
    withTimeoutOrNull(MAX_WAIT_MS) {
      snapshotFlow { playerReadyState.value }.first { it }
      PattukalStartup.homeReady.first { it }
    }
    val spent = SystemClock.elapsedRealtime() - start
    if (spent < MIN_VISIBLE_MS) delay(MIN_VISIBLE_MS - spent)
    delay(SETTLE_MS)
    progress.animateTo(0f, tween(FADE_OUT_MS, easing = FastOutSlowInEasing))
    finished = true
  }

  val finishedState = rememberUpdatedState(finished)
  return remember(progress) { PattukalStartupState(progress, finishedState) }
}

/**
 * Blurs the app behind the startup screen (Android 12+; older versions rely on the dark scrim).
 * The blur is only applied while the startup screen is up and fades out together with it.
 */
fun Modifier.pattukalStartupBlur(progress: () -> Float): Modifier = graphicsLayer {
  val p = progress()
  if (p > 0.01f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val radius = 24.dp.toPx() * p
    renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
  }
}

/** Full-screen startup overlay: a dark frosted scrim with the PATTUKAL loader in the middle. */
@Composable
fun PattukalStartupOverlay(progress: () -> Float, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .graphicsLayer { alpha = progress() }
        // Swallow touches so nothing underneath can be tapped while loading.
        .pointerInput(Unit) {}
        .background(Color(0x8C090812)),
    contentAlignment = Alignment.Center,
  ) {
    PattukalStartupLoader()
  }
}

/**
 * Compose port of the "Uiverse dexter-st" loader: each letter flashes in with a small glow and
 * fades out, while a band of coloured light sweeps across the word and is only visible through a
 * pattern of thin vertical lines.
 *
 * Performance: everything is driven by one infinite transition that is only read inside draw and
 * graphicsLayer blocks, so nothing recomposes or re-measures while it runs. All gradient brushes
 * and the stripe pattern are built once.
 */
@Composable
fun PattukalStartupLoader(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "pattukal_startup")
  val time =
    transition.animateFloat(
      initialValue = 0f,
      targetValue = LETTER_CYCLE_MS,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = LETTER_CYCLE_MS.toInt(), easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "pattukal_startup_time",
    )

  val density = LocalDensity.current
  val fontDp = 44.dp
  val fontSize = with(density) { fontDp.toSp() }
  // One CSS "effective" pixel in device pixels.
  val unit = with(density) { fontDp.toPx() } / CSS_FONT_PX
  val boxHeight = with(density) { (240f * unit).toDp() }
  val liftPx = 4f * unit

  val stripeBrush = remember(unit) { buildStripeBrush(unit) }

  Box(
    modifier =
      modifier.height(boxHeight).semantics(mergeDescendants = true) { contentDescription = "Loading" },
    contentAlignment = Alignment.Center,
  ) {
    // Coloured light, masked by the stripes.
    Box(
      modifier =
        Modifier.matchParentSize()
          .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            alpha = glowOpacity(time.value)
          }
          .drawWithCache {
            val w = size.width
            val h = size.height
            val farthestCorner = hypot(w / 2f, h / 2f)
            val ringMask =
              Brush.radialGradient(
                0f to Color.Transparent,
                0.10f to Color.Transparent,
                0.25f to Color.Black,
                center = Offset(w / 2f, h / 2f),
                radius = farthestCorner,
              )
            // Drawn bottom to top; the first CSS background is the top-most one.
            val blobs =
              listOf(
                glowBlob(GlowBlue, 0.55f, 0.45f, 0.45f, w, h),
                glowBlob(GlowGreen, 0.45f, 0.55f, 0.45f, w, h),
                glowBlob(GlowCyan, 0.55f, 0.55f, 0.45f, w, h),
                glowBlob(GlowRed, 0.45f, 0.45f, 0.45f, w, h),
                glowBlob(GlowYellow, 0.50f, 0.50f, 0.50f, w, h),
              )
            onDrawBehind {
              translate(left = glowTranslation(time.value) * w) {
                blobs.forEach { drawRect(it) }
                drawRect(ringMask, blendMode = BlendMode.DstIn)
              }
              drawRect(stripeBrush, blendMode = BlendMode.DstIn)
            }
          }
    )

    // The letters.
    Row {
      WORD.forEachIndexed { index, letter ->
        BasicText(
          text = letter.toString(),
          style =
            TextStyle(
              color = Color.White,
              fontSize = fontSize,
              fontWeight = FontWeight.SemiBold,
              shadow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = 4f * unit),
            ),
          modifier =
            Modifier.graphicsLayer {
              val p = letterPhase(time.value, index)
              alpha = letterOpacity(p)
              val s = letterScale(p)
              scaleX = s
              scaleY = s
              translationY = -liftPx * letterLift(p)
            },
        )
      }
    }
  }
}

// --- drawing helpers -------------------------------------------------------------------------

private fun glowBlob(
  color: Color,
  cx: Float,
  cy: Float,
  stop: Float,
  w: Float,
  h: Float,
): Brush {
  // CSS `radial-gradient(circle at X Y, color 0%, transparent stop)` uses the farthest corner.
  val farthest = hypot(max(cx, 1f - cx) * w, max(cy, 1f - cy) * h)
  return Brush.radialGradient(
    colors = listOf(color, color.copy(alpha = 0f)),
    center = Offset(w * cx, h * cy),
    radius = farthest * stop,
  )
}

/** Thin vertical lines: the CSS `repeating-linear-gradient(90deg, transparent 0 6px, black 7px 8px)`. */
private fun buildStripeBrush(unit: Float): ShaderBrush {
  val pitch = max(4, (16f * unit).toInt())
  val thickness = max(1, (2f * unit).toInt()).coerceAtMost(pitch - 1)
  val tile = ImageBitmap(pitch, 1)
  val canvas = androidx.compose.ui.graphics.Canvas(tile)
  canvas.drawRect(
    Rect((pitch - thickness).toFloat(), 0f, pitch.toFloat(), 1f),
    Paint().apply { color = Color.White },
  )
  return ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

// --- animation curves (all derived from the CSS keyframes) -----------------------------------

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** `opacity-animation`: 0 -> 1 at 15%, back to 0 at 65%, 0 until 100% (4s loop). */
private fun glowOpacity(timeMs: Float): Float {
  val p = (timeMs % LETTER_CYCLE_MS) / LETTER_CYCLE_MS
  return when {
    p < 0.15f -> GlowEasing.transform(p / 0.15f)
    p < 0.65f -> 1f - GlowEasing.transform((p - 0.15f) / 0.50f)
    else -> 0f
  }
}

/** `transform-animation`: -55% -> 55% over 2s, alternating. Returns a fraction of the width. */
private fun glowTranslation(timeMs: Float): Float {
  val half = LETTER_CYCLE_MS / 2f
  val iteration = (timeMs / half).toInt()
  val t = (timeMs % half) / half
  // In reverse iterations CSS also reverses the timing function.
  val eased = if (iteration % 2 == 0) GlowEasing.transform(t) else 1f - GlowEasing.transform(1f - t)
  val position = if (iteration % 2 == 0) eased else 1f - eased
  return lerp(-0.55f, 0.55f, position)
}

private fun letterPhase(timeMs: Float, index: Int): Float {
  val delayMs = 100f + index * 105.2f
  val local = timeMs - delayMs
  return (((local % LETTER_CYCLE_MS) + LETTER_CYCLE_MS) % LETTER_CYCLE_MS) / LETTER_CYCLE_MS
}

/** `loader-letter-anim`: 0 -> 1 at 5%, 0.2 at 20%, 0 at 100%. */
private fun letterOpacity(p: Float): Float =
  when {
    p < 0.05f -> p / 0.05f
    p < 0.20f -> lerp(1f, 0.2f, (p - 0.05f) / 0.15f)
    else -> lerp(0.2f, 0f, (p - 0.20f) / 0.80f)
  }

/** 0 at rest, 1 at the 5% keyframe (scale 1.1, raised by 2px), then settles back by 100%. */
private fun letterLift(p: Float): Float =
  if (p < 0.05f) p / 0.05f else lerp(1f, 0f, (p - 0.05f) / 0.95f)

private fun letterScale(p: Float): Float = 1f + 0.1f * letterLift(p)
