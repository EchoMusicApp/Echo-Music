@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: (() -> Unit)? = null,
  highlightKey: String? = null
) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("About Pattukal", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(
            onClick = { onBack?.invoke() ?: navController.navigateUp() },
            onLongClick = navController::backToMain,
          ) {
            Icon(
              painter = androidx.compose.ui.res.painterResource(R.drawable.arrow_back),
              contentDescription = null
            )
          }
        },
        windowInsets = TopAppBarDefaults.windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
        )
        .verticalScroll(rememberScrollState())
        .padding(
          start = 22.dp,
          end = 22.dp,
          top = innerPadding.calculateTopPadding() + 18.dp,
          bottom = 120.dp
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      PattukalTiltCard()

      Spacer(Modifier.height(6.dp))
      Text("\uD83C\uDFA7 PATTUKAL", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
      Text(
        "Premium Music. Modern Technology.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
      )
      Text(
        "Experience music with a premium UI designed by Akshay, smooth performance, and YouTube-powered playback. We\u2019re always exploring new features and innovative technologies to make your music experience better.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Text(
        "Our mission is simple: bring music and technology together and make everyone happy. \uD83D\uDC9C",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
      ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Designed by", fontWeight = FontWeight.SemiBold)
            Text("Akshay", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
          Row(
            Modifier.fillMaxWidth().clickable {
              runCatching {
                context.startActivity(
                  android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:akshaybiju602@gmail.com"))
                )
              }
            },
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Contact", fontWeight = FontWeight.SemiBold)
            Text("akshaybiju602@gmail.com", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
          Row(
            Modifier.fillMaxWidth().clickable {
              runCatching {
                context.startActivity(
                  android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:pattukaloffical@gmail.com"))
                )
              }
            },
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Contact", fontWeight = FontWeight.SemiBold)
            Text("pattukaloffical@gmail.com", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Version", fontWeight = FontWeight.SemiBold)
            Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
        }
      }

      Text(
        "PATTUKAL",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 4.sp,
        modifier = Modifier.padding(top = 8.dp)
      )
      Text(
        "Feel the Music. Explore the Future.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

private val CardMint = Color(0xFF00FFAA)
private val CardBlue = Color(0xFF00A2FF)
private val CardIndigo = Color(0xFF5C67FF)
private val CardViolet = Color(0xFFAD51FF)

/** Touch-driven 3D tilt card (port of the Uiverse "00Kubi" hover card): 5x5 grid of tilt zones. */
@Composable
private fun PattukalTiltCard() {
  var touching by remember { mutableStateOf(false) }
  var col by remember { mutableIntStateOf(2) }
  var row by remember { mutableIntStateOf(2) }
  val rotX by animateFloatAsState(if (touching) (2 - row) * 10f else 0f, tween(125), label = "rotX")
  val rotY by animateFloatAsState(if (touching) (col - 2) * 5f else 0f, tween(125), label = "rotY")
  val press by animateFloatAsState(if (touching) 0.95f else 1f, tween(200), label = "press")
  val reveal by animateFloatAsState(if (touching) 1f else 0f, tween(300), label = "reveal")

  val inf = rememberInfiniteTransition(label = "cardFx")
  val particle by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "particle")
  val scan by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "scan")
  val lineT by inf.animateFloat(0f, 3f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "lines")

  val shape = RoundedCornerShape(20.dp)
  Box(
    modifier =
      Modifier.width(250.dp)
        .height(334.dp)
        .graphicsLayer {
          rotationX = rotX
          rotationY = rotY
          scaleX = press
          scaleY = press
          cameraDistance = 14f * density
        }
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFF262626))))
        .border(2.dp, Color.White.copy(alpha = 0.1f), shape)
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val change = awaitPointerEvent().changes.firstOrNull()
              if (change != null && change.pressed) {
                touching = true
                col = ((change.position.x / size.width) * 5).toInt().coerceIn(0, 4)
                row = ((change.position.y / size.height) * 5).toInt().coerceIn(0, 4)
              } else {
                touching = false
              }
            }
          }
        },
    contentAlignment = Alignment.Center
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      // glowing blobs (visible while touched)
      val glowAlpha = reveal
      listOf(Offset(0f, 0f), Offset(w, h * 0.5f), Offset(w * 0.4f, h)).forEach {
        drawCircle(
          Brush.radialGradient(listOf(CardMint.copy(alpha = 0.3f * glowAlpha), Color.Transparent), center = it, radius = 90.dp.toPx()),
          radius = 90.dp.toPx(),
          center = it
        )
      }
      // cyber lines
      for (i in 0 until 4) {
        val phase = ((lineT + i * 0.9f) % 3f) / 3f
        val grow = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
        val y = h * (0.2f + 0.2f * i)
        val fromLeft = i % 2 == 0
        val len = w * grow
        drawLine(
          Brush.horizontalGradient(listOf(Color.Transparent, CardIndigo.copy(alpha = 0.35f * grow), Color.Transparent)),
          Offset(if (fromLeft) 0f else w - len, y),
          Offset(if (fromLeft) len else w, y),
          strokeWidth = 1.dp.toPx()
        )
      }
      // scan line
      val sy = h * (0.5f + 0.5f * scan)
      drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, CardIndigo.copy(alpha = 0.12f), Color.Transparent), startY = sy - 60.dp.toPx(), endY = sy + 60.dp.toPx()),
        topLeft = Offset(0f, sy - 60.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(w, 120.dp.toPx())
      )
      // corner brackets
      val cc = CardIndigo.copy(alpha = 0.3f + 0.5f * reveal)
      val m = 10.dp.toPx(); val L = 15.dp.toPx(); val sw = 2.dp.toPx()
      listOf(
        Triple(Offset(m, m), 1f, 1f), Triple(Offset(w - m, m), -1f, 1f),
        Triple(Offset(m, h - m), 1f, -1f), Triple(Offset(w - m, h - m), -1f, -1f)
      ).forEach { (o, dx, dy) ->
        drawLine(cc, o, Offset(o.x + dx * L, o.y), sw)
        drawLine(cc, o, Offset(o.x, o.y + dy * L), sw)
      }
      // particles
      val pts = listOf(
        floatArrayOf(0.2f, 0.4f, 1f, -1f), floatArrayOf(0.8f, 0.6f, -1f, -1f), floatArrayOf(0.4f, 0.2f, 0.5f, 1f),
        floatArrayOf(0.6f, 0.8f, -0.5f, 1f), floatArrayOf(0.6f, 0.3f, 1f, 0.5f), floatArrayOf(0.4f, 0.7f, -1f, 0.5f)
      )
      pts.forEach { p ->
        val a = sin(particle * Math.PI.toFloat()) * reveal
        drawCircle(
          CardMint.copy(alpha = a),
          1.5.dp.toPx(),
          Offset(w * p[0] + p[2] * 30.dp.toPx() * particle, h * p[1] + p[3] * 30.dp.toPx() * particle)
        )
      }
      // glare
      drawRect(
        Brush.linearGradient(
          listOf(Color.Transparent, Color.White.copy(alpha = 0.06f * reveal), Color.White.copy(alpha = 0.12f * reveal), Color.White.copy(alpha = 0.06f * reveal), Color.Transparent),
          start = Offset.Zero,
          end = Offset(w, h)
        )
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text(
        "PATTUKAL",
        style = TextStyle(
          brush = Brush.linearGradient(listOf(CardMint, CardBlue)),
          fontSize = 28.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 4.sp
        ),
        modifier = Modifier.graphicsLayer { alpha = reveal; translationY = -10f * (1f - reveal) * density }
      )
      Text(
        "TOUCH ME",
        color = Color.White.copy(alpha = 0.7f * (1f - reveal)),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = Modifier.graphicsLayer { translationY = -(24.dp.toPx()) }
      )
    }
    Column(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("FEEL THE MUSIC", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, letterSpacing = 2.sp)
      Text(
        "EXPLORE THE FUTURE",
        style = TextStyle(brush = Brush.linearGradient(listOf(CardIndigo, CardViolet)), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
      )
    }
  }
}
