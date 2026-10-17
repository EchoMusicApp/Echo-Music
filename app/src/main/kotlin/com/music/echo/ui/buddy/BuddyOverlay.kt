@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.buddy

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.BuddyAnimationsKey
import echo.music.iad1tya.constants.BuddyEnabledKey
import echo.music.iad1tya.constants.BuddyFloatingKey
import echo.music.iad1tya.constants.BuddyGreetingKey
import echo.music.iad1tya.constants.BuddyLastSeenKey
import echo.music.iad1tya.constants.BuddySelectedKey
import echo.music.iad1tya.constants.BuddySleepModeKey
import echo.music.iad1tya.ui.screens.settings.BuddyCharacter
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import echo.music.iad1tya.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the album art is on screen while the full player is open; Buddy stands on its bottom edge. */
object BuddyStageAnchor {
  var rect by mutableStateOf<Rect?>(null)
}

class TapPing(val pos: Offset, val id: Long)

/** The last place the user touched the screen, so Buddy can look at it. */
class BuddyLook {
  var ping by mutableStateOf<TapPing?>(null)
}

/** Observes touches on the way down without consuming them, so the app underneath works normally. */
fun Modifier.buddyTapTracker(look: BuddyLook): Modifier =
  this.pointerInput(Unit) {
    awaitPointerEventScope {
      while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
        if (down != null) look.ping = TapPing(down.position, System.nanoTime())
      }
    }
  }

private data class ChatMsg(val fromUser: Boolean, val text: String)

private data class Lane(
  val line: Float,
  val minX: Float,
  val maxX: Float,
  val scale: Float,
  val hidden: Boolean,
)

private fun faceFor(b: BuddyCharacter, m: BuddyMood): Int =
  when (b) {
    BuddyCharacter.LUNA ->
      when (m) {
        BuddyMood.HAPPY -> R.drawable.buddy_luna_happy
        BuddyMood.EXCITED -> R.drawable.buddy_luna_excited
        BuddyMood.SLEEPY -> R.drawable.buddy_luna_sleepy
        BuddyMood.THINKING -> R.drawable.buddy_luna_thinking
        BuddyMood.PLAYFUL -> R.drawable.buddy_luna_playful
        BuddyMood.SURPRISED -> R.drawable.buddy_luna_surprised
        BuddyMood.WINK -> R.drawable.buddy_luna_wink
      }
    BuddyCharacter.KAI ->
      when (m) {
        BuddyMood.HAPPY -> R.drawable.buddy_kai_happy
        BuddyMood.EXCITED -> R.drawable.buddy_kai_excited
        BuddyMood.SLEEPY -> R.drawable.buddy_kai_sleepy
        BuddyMood.THINKING -> R.drawable.buddy_kai_thinking
        BuddyMood.PLAYFUL -> R.drawable.buddy_kai_playful
        BuddyMood.SURPRISED -> R.drawable.buddy_kai_surprised
        BuddyMood.WINK -> R.drawable.buddy_kai_wink
      }
  }

private fun idleLines(b: BuddyCharacter, hour: Int, playing: Boolean): List<String> {
  val bro = if (b == BuddyCharacter.KAI) " bro" else ""
  val base =
    mutableListOf(
      "Tap me and we can chat ✨",
      "Want me to pick a song for you$bro? 🎧",
      "Psst… ask me to play anything 🎶",
      "I'm right here if you need me 💜",
    )
  if (playing) {
    base += listOf("This beat though! 🔥", "Vibing with you$bro 🎵", "Turn it up a little? 😄")
  } else {
    base += listOf("It's a bit quiet… shall I play something? 🎧", "Let's find your next favourite song!")
  }
  when (hour) {
    in 5..11 -> base += "Hope your morning is going great$bro ☀️"
    in 12..15 -> base += "Don't forget to drink some water$bro 💧"
    in 16..19 -> base += "Evening vibes are the best 🌆"
    in 20..22 -> base += "Nice night for music 🌙"
    else -> base += "Don't stay up too late$bro 😴"
  }
  return base
}

private fun hopOf(t: Float, dancing: Boolean, walking: Boolean, d: Density): Float =
  with(d) {
    when {
      dancing -> abs(sin(t * 6.9f)) * 7.dp.toPx()
      walking -> abs(sin(t * 7f)) * 3.dp.toPx()
      else -> (sin(t * 2f) * 0.5f + 0.5f) * 1.2.dp.toPx()
    }
  }

/**
 * Pattukal's floating companion. It walks along the bottom of the app (or along the album art in
 * the full player), looks toward wherever you tap, dances while music plays, and opens a small
 * chat with the actual AI brain.
 */
@Composable
fun BuddyOverlay(
  navController: NavController,
  playerExpanded: Boolean,
  dockBottomPx: Float,
  look: BuddyLook,
) {
  val selected by rememberPreference(BuddySelectedKey, "")
  val enabled by rememberPreference(BuddyEnabledKey, true)
  val floating by rememberPreference(BuddyFloatingKey, true)
  val animations by rememberPreference(BuddyAnimationsKey, true)
  val greetingOn by rememberPreference(BuddyGreetingKey, true)
  val sleepMode by rememberPreference(BuddySleepModeKey, true)
  val buddy = BuddyCharacter.from(selected)
  if (buddy == null || !enabled || !floating) return
  BuddyOverlayContent(
    buddy = buddy,
    animations = animations,
    greetingOn = greetingOn,
    sleepMode = sleepMode,
    navController = navController,
    playerExpanded = playerExpanded,
    dockBottomPx = dockBottomPx,
    look = look,
  )
}

@Composable
private fun BuddyOverlayContent(
  buddy: BuddyCharacter,
  animations: Boolean,
  greetingOn: Boolean,
  sleepMode: Boolean,
  navController: NavController,
  playerExpanded: Boolean,
  dockBottomPx: Float,
  look: BuddyLook,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val d = density.density
  val scope = rememberCoroutineScope()
  val pc = LocalPlayerConnection.current
  val db = LocalDatabase.current
  val isPlaying by (pc?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
  val metadata by (pc?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
  val setFloating = rememberPreference(BuddyFloatingKey, true)

  val spriteW = 64f * d
  val footLine = 86f * d

  var screenW by remember { mutableFloatStateOf(0f) }
  var screenH by remember { mutableFloatStateOf(0f) }
  var xPos by remember { mutableFloatStateOf(-2000f) }
  var yPos by remember { mutableFloatStateOf(0f) }
  var targetX by remember { mutableFloatStateOf(-2000f) }
  var scaleNow by remember { mutableFloatStateOf(1f) }
  var clock by remember { mutableFloatStateOf(0f) }
  var walking by remember { mutableStateOf(false) }
  var facing by remember { mutableFloatStateOf(1f) }
  var lookX by remember { mutableFloatStateOf(0f) }
  var lookY by remember { mutableFloatStateOf(0f) }
  var lookTx by remember { mutableFloatStateOf(0f) }
  var lookTy by remember { mutableFloatStateOf(0f) }
  var dragging by remember { mutableStateOf(false) }
  var placed by remember { mutableStateOf(false) }

  var menuOpen by remember { mutableStateOf(false) }
  var chatOpen by remember { mutableStateOf(false) }
  var leaving by remember { mutableStateOf(false) }
  var bubble by remember { mutableStateOf<String?>(null) }
  var bubbleVer by remember { mutableIntStateOf(0) }
  var bubbleMs by remember { mutableIntStateOf(3500) }
  var moodOverride by remember { mutableStateOf<BuddyMood?>(null) }
  var bubbleSize by remember { mutableStateOf(IntSize(1, 1)) }
  var hour by remember { mutableIntStateOf(indiaNow().hour) }

  val chat = remember { mutableStateListOf<ChatMsg>() }
  var latestReply by remember { mutableStateOf("") }
  var thinking by remember { mutableStateOf(false) }
  var taste by remember { mutableStateOf<String?>(null) }
  var aiOn by remember { mutableStateOf(false) }

  val sleeping = sleepMode && (hour >= 23 || hour < 5) && !isPlaying
  val baseMood =
    when {
      sleeping -> BuddyMood.SLEEPY
      isPlaying -> BuddyMood.EXCITED
      else -> BuddyMood.HAPPY
    }
  val shownMood = if (thinking) BuddyMood.THINKING else (moodOverride ?: baseMood)
  val face = faceFor(buddy, shownMood)
  val dancing = animations && isPlaying && !sleeping

  fun say(text: String, m: BuddyMood? = null, ms: Int = 3800) {
    bubble = text
    bubbleMs = ms
    moodOverride = m
    bubbleVer++
  }

  // ----- lane geometry (where the feet stand) -----
  val stageRect = BuddyStageAnchor.rect
  val stage =
    if (playerExpanded && stageRect != null && stageRect.bottom in 1f..(screenH.takeIf { it > 0f } ?: Float.MAX_VALUE) && stageRect.width > 80f) stageRect
    else null
  val lane =
    if (stage != null) {
      val mn = stage.left + 4f * d
      Lane(stage.bottom + 2f * d, mn, max(mn + 1f, stage.right - spriteW * 0.85f - 4f * d), 0.85f, false)
    } else {
      val mn = 8f * d
      Lane(
        screenH - dockBottomPx - 2f * d,
        mn,
        max(mn + 1f, screenW - spriteW - 8f * d),
        1f,
        playerExpanded
      )
    }
  val laneState = rememberUpdatedState(lane)
  val dancingState = rememberUpdatedState(dancing)
  val pausedState = rememberUpdatedState(menuOpen || chatOpen || dragging || leaving)

  // ----- one frame loop: clock, walking, smoothing -----
  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        if (last == 0L) last = now
        val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
        last = now
        clock += dt
        val l = laneState.value
        if (screenW > 0f && screenH > 0f) {
          if (!placed) {
            val fromLeft = Random.nextBoolean()
            xPos = if (fromLeft) -spriteW * 1.3f else screenW + spriteW * 0.3f
            yPos = l.line - footLine
            targetX = (if (fromLeft) 0.18f else 0.62f).let { screenW * it }.coerceIn(l.minX, l.maxX)
            placed = true
          }
          if (!dragging) {
            val wantY = l.line - footLine
            yPos += (wantY - yPos) * min(1f, dt * 9f)
          }
          scaleNow += (l.scale - scaleNow) * min(1f, dt * 8f)
          if (l.hidden && !leaving) targetX = screenW + spriteW * 0.6f
          else if (!leaving) targetX = targetX.coerceIn(l.minX, l.maxX)
          if (!dragging) {
            val dx = targetX - xPos
            val ad = abs(dx)
            if (ad > 1.5f) {
              val speed = (55f + 62f * min(1f, ad / (150f * d))) * d
              val step = min(ad, speed * dt)
              xPos += if (dx > 0f) step else -step
              facing = if (dx > 0f) 1f else -1f
              walking = true
            } else {
              walking = false
            }
          }
          lookX += (lookTx - lookX) * min(1f, dt * 9f)
          lookY += (lookTy - lookY) * min(1f, dt * 9f)
        }
      }
    }
  }

  // ----- bubble lifetime -----
  LaunchedEffect(bubbleVer) {
    if (bubble != null) {
      delay(bubbleMs.toLong())
      bubble = null
      moodOverride = null
    }
  }

  // ----- clock for greetings -----
  LaunchedEffect(Unit) {
    while (true) {
      delay(60_000)
      hour = indiaNow().hour
      withContext(kotlinx.coroutines.Dispatchers.IO) {
        context.dataStore.edit { it[BuddyLastSeenKey] = System.currentTimeMillis() }
      }
    }
  }

  // ----- look where the user taps -----
  // Phone back button: close the chat first, then the menu, before the app goes back.
  // (Composed only while open so it always sits on top of the screens' own back handlers.)
  if (chatOpen) {
    BackHandler {
      chatOpen = false
      moodOverride = null
    }
  } else if (menuOpen) {
    BackHandler {
      menuOpen = false
      moodOverride = null
    }
  }

  LaunchedEffect(look.ping?.id) {
    val p = look.ping ?: return@LaunchedEffect
    if (menuOpen) {
      val mw = 196f * d
      val mcx = xPos + spriteW / 2f
      val mLeft = (mcx - mw / 2f).coerceIn(8f * d, max(8f * d, screenW - mw - 8f * d))
      val mTop = (yPos + footLine * (1f - scaleNow) - 70f * d).coerceAtLeast(4f * d)
      val inMenu = p.pos.x in mLeft..(mLeft + mw) && p.pos.y in mTop..(mTop + 66f * d)
      val inSprite = p.pos.x in xPos..(xPos + spriteW) && p.pos.y in yPos..(yPos + 92f * d)
      if (!inMenu && !inSprite) {
        menuOpen = false
        moodOverride = null
      }
    }
    if (screenW <= 0f || screenH <= 0f) return@LaunchedEffect
    val cx = xPos + spriteW / 2f
    val cy = yPos + 30f * d
    val dx = p.pos.x - cx
    val dy = p.pos.y - cy
    lookTx = (dx / (screenW * 0.45f)).coerceIn(-1f, 1f)
    lookTy = (dy / (screenH * 0.45f)).coerceIn(-1f, 1f)
    if (abs(dx) < 70f * d && abs(dy) < 90f * d && !menuOpen && !chatOpen) {
      moodOverride = BuddyMood.SURPRISED
      delay(650)
      if (bubble == null) moodOverride = null
    }
    delay(1800)
    lookTx = 0f
    lookTy = 0f
  }

  // ----- wandering, greeting and small talk -----
  LaunchedEffect(buddy) {
    snapshotFlow { placed }.first { it }
    delay(1800)
    if (greetingOn) {
      val lastSeen = withContext(kotlinx.coroutines.Dispatchers.IO) { context.dataStore.get(BuddyLastSeenKey, 0L) }
      val back = lastSeen > 0L && System.currentTimeMillis() - lastSeen > 6 * 60 * 60 * 1000L
      val (text, m) = greetingIst(buddy, back)
      say(text, m, 5200)
    }
    withContext(kotlinx.coroutines.Dispatchers.IO) {
      context.dataStore.edit { it[BuddyLastSeenKey] = System.currentTimeMillis() }
    }
    var untilTalk = Random.nextInt(35, 70) * 1000L
    while (true) {
      delay(1200)
      untilTalk -= 1200
      if (pausedState.value || sleeping) continue
      // wander
      if (!walking && Random.nextFloat() < 0.32f) {
        val l = laneState.value
        if (!l.hidden) {
          val span = l.maxX - l.minX
          var t = l.minX + Random.nextFloat() * span
          if (abs(t - xPos) < 50f * d) t = if (xPos > l.minX + span / 2f) l.minX + span * 0.15f else l.maxX - span * 0.15f
          targetX = t
        }
      }
      // look around when nothing is going on
      if (!walking && Random.nextFloat() < 0.5f) {
        lookTx = Random.nextFloat() * 1.2f - 0.6f
        lookTy = Random.nextFloat() * 0.5f - 0.2f
      }
      if (untilTalk <= 0 && bubble == null && !laneState.value.hidden) {
        say(idleLines(buddy, indiaNow().hour, dancingState.value).random(), if (dancingState.value) BuddyMood.EXCITED else BuddyMood.WINK, 4200)
        untilTalk = Random.nextInt(40, 80) * 1000L
      }
    }
  }

  // ----- react to a new track -----
  var lastReactId by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(metadata?.id, isPlaying) {
    val md = metadata ?: return@LaunchedEffect
    if (!isPlaying || md.id == lastReactId || pausedState.value) return@LaunchedEffect
    if (lastReactId != null) {
      delay(1500)
      val bro = if (buddy == BuddyCharacter.KAI) " bro" else ""
      val line =
        listOf(
            "Ooh, \"${md.title}\"! 🔥",
            "This one's so good$bro! 🎶",
            "Great taste$bro! 😍",
            "I love this vibe 💜",
          )
          .random()
      if (bubble == null) say(line, BuddyMood.EXCITED, 3600)
    }
    lastReactId = md.id
  }

  LaunchedEffect(chatOpen) {
    if (chatOpen) {
      if (taste == null) taste = BuddyBrain.tasteLine(db)
      aiOn = BuddyBrain.aiConfigured(context)
    }
  }

  fun send(raw: String) {
    val text = raw.trim()
    if (text.isEmpty() || thinking) return
    chat.add(ChatMsg(true, text))
    thinking = true
    scope.launch {
      val nowPlaying = metadata?.let { m -> "${m.title} — ${m.artists.joinToString { it.name }}" }
      val history = chat.dropLast(1).map { it.fromUser to it.text }
      val turn = BuddyBrain.respond(context, buddy, text, history, nowPlaying, taste ?: "unknown")
      moodOverride = moodOf(turn.mood)
      thinking = false
      chat.add(ChatMsg(false, turn.reply))
      latestReply = turn.reply
      val extra = BuddyBrain.execute(turn, pc, db, navController)
      if (extra != null) {
        chat.add(ChatMsg(false, extra))
        latestReply = extra
      }
      if (turn.action == "search" || turn.action == "favorites") {
        delay(900)
        chatOpen = false
      }
    }
  }

  Box(
    Modifier.fillMaxSize().onSizeChanged {
      screenW = it.width.toFloat()
      screenH = it.height.toFloat()
    }
  ) {
    // ---------------- walking sprite ----------------
    Box(
      Modifier.size(64.dp, 92.dp)
        .graphicsLayer {
          translationX = xPos
          translationY = yPos
          scaleX = scaleNow
          scaleY = scaleNow
          transformOrigin = TransformOrigin(0.5f, 86f / 92f)
          alpha = if (chatOpen) 0f else 1f
        }
        .pointerInput(Unit) {
          detectTapGestures(
            onTap = {
              targetX = xPos
              bubble = null
              moodOverride = BuddyMood.WINK
              menuOpen = !menuOpen
            },
            onDoubleTap = {
              say("Hehe, that tickles! 😄", BuddyMood.PLAYFUL, 2400)
            },
          )
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = {
              dragging = true
              menuOpen = false
              bubble = null
              moodOverride = BuddyMood.SURPRISED
            },
            onDragEnd = {
              dragging = false
              targetX = xPos
              moodOverride = null
            },
            onDragCancel = {
              dragging = false
              targetX = xPos
              moodOverride = null
            },
            onDrag = { change, amount ->
              change.consume()
              xPos = (xPos + amount.x).coerceIn(-spriteW * 0.3f, screenW - spriteW * 0.7f)
              yPos = (yPos + amount.y).coerceIn(0f, screenH - footLine)
            },
          )
        }
    ) {
      BuddySprite(
        buddy = buddy,
        face = face,
        clock = { clock },
        walking = { walking && !dragging },
        dancing = { dancing && !walking && !dragging },
        sleeping = sleeping,
        lookX = { lookX },
        lookY = { lookY },
        facing = { facing },
      )
    }

    // ---------------- speech bubble ----------------
    AnimatedVisibility(
      visible = bubble != null && !menuOpen && !chatOpen,
      enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn(),
      exit = scaleOut(tween(160), transformOrigin = TransformOrigin(0.5f, 1f)) + fadeOut(tween(160)),
      modifier =
        Modifier.offset {
          val bw = bubbleSize.width.toFloat()
          val bh = bubbleSize.height.toFloat()
          val cx = xPos + spriteW / 2f
          val left = (cx - bw / 2f).coerceIn(8f * d, max(8f * d, screenW - bw - 8f * d))
          val top = (yPos + footLine * (1f - scaleNow) - bh - 2f * d).coerceAtLeast(4f * d)
          IntOffset(left.toInt(), top.toInt())
        }
    ) {
      val text = bubble ?: ""
      Column(
        Modifier.onSizeChanged { bubbleSize = it }.widthIn(max = 220.dp)
      ) {
        Box(
          Modifier.shadow(10.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF17122E), Color(0xFF241B4A))))
            .border(1.dp, buddy.accent.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(text, color = Color.White, fontSize = 12.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        }
        Canvas(Modifier.fillMaxWidth().height(7.dp)) {
          val cx = xPos + spriteW / 2f
          val bw = bubbleSize.width.toFloat()
          val left = (cx - bw / 2f).coerceIn(8f * d, max(8f * d, screenW - bw - 8f * d))
          val tx = (cx - left).coerceIn(16f * d, max(16f * d, bw - 16f * d))
          val p = Path()
          p.moveTo(tx - 6f * d, 0f)
          p.lineTo(tx + 6f * d, 0f)
          p.lineTo(tx, size.height)
          p.close()
          drawPath(p, Color(0xFF241B4A))
          drawLine(buddy.accent.copy(alpha = 0.75f), Offset(tx - 6f * d, 0f), Offset(tx, size.height), 1.dp.toPx())
          drawLine(buddy.accent.copy(alpha = 0.75f), Offset(tx + 6f * d, 0f), Offset(tx, size.height), 1.dp.toPx())
        }
      }
    }

    // ---------------- 3 option menu ----------------
    AnimatedVisibility(
      visible = menuOpen && !chatOpen,
      enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn(),
      exit = scaleOut(tween(140), transformOrigin = TransformOrigin(0.5f, 1f)) + fadeOut(tween(140)),
      modifier =
        Modifier.offset {
          val mw = 196f * d
          val cx = xPos + spriteW / 2f
          val left = (cx - mw / 2f).coerceIn(8f * d, max(8f * d, screenW - mw - 8f * d))
          val top = (yPos + footLine * (1f - scaleNow) - 70f * d).coerceAtLeast(4f * d)
          IntOffset(left.toInt(), top.toInt())
        }
    ) {
      Row(
        Modifier.width(196.dp)
          .shadow(12.dp, RoundedCornerShape(26.dp))
          .clip(RoundedCornerShape(26.dp))
          .background(Brush.linearGradient(listOf(Color(0xFF17122E), Color(0xFF241B4A))))
          .border(1.dp, buddy.accent.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
          .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        MenuButton(buddy, Icons.Rounded.ChatBubble, "Text") {
          menuOpen = false
          moodOverride = null
          if (chat.isEmpty()) {
            val g = greetingIst(buddy, false)
            chat.add(ChatMsg(false, g.first))
            latestReply = g.first
          }
          chatOpen = true
        }
        MenuButton(buddy, Icons.Rounded.MusicNote, "Suggest") {
          menuOpen = false
          scope.launch {
            thinking = true
            say("Let me pick something you'll love… 🎧", BuddyMood.THINKING, 2600)
            val msg = BuddyBrain.playFromTaste(pc, db)
            thinking = false
            say(msg, BuddyMood.EXCITED, 5200)
          }
        }
        MenuButton(buddy, Icons.Rounded.Close, "Remove", danger = true) {
          menuOpen = false
          scope.launch {
            leaving = true
            say("Bye for now! Bring me back from Settings → AI Companion 👋", BuddyMood.SURPRISED, 3000)
            delay(2600)
            targetX = if (xPos < screenW / 2f) -spriteW * 1.5f else screenW + spriteW * 0.5f
            delay(1400)
            setFloating.value = false
          }
        }
      }
    }

    // ---------------- chat ----------------
    AnimatedVisibility(
      visible = chatOpen,
      enter = fadeIn(tween(180)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 3 },
      exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 3 },
    ) {
      BuddyChatPanel(
        buddy = buddy,
        face = face,
        reply = latestReply,
        thinking = thinking,
        lastUser = chat.lastOrNull { it.fromUser }?.text,
        aiOn = aiOn,
        onSend = { send(it) },
        onConnectAi = {
          chatOpen = false
          navController.navigate("settings/ai") { launchSingleTop = true }
        },
        onClose = {
          chatOpen = false
          moodOverride = null
        },
      )
    }
  }
}

@Composable
private fun MenuButton(
  buddy: BuddyCharacter,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  danger: Boolean = false,
  onClick: () -> Unit,
) {
  Column(
    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      Modifier.size(42.dp)
        .shadow(6.dp, CircleShape)
        .clip(CircleShape)
        .background(
          if (danger) Brush.linearGradient(listOf(Color(0xFFFF6B7A), Color(0xFFC21E3F)))
          else Brush.linearGradient(listOf(buddy.accent2, buddy.accent))
        )
        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.height(3.dp))
    Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
  }
}

/** The little 3D-ish character: head with a tilt toward what it looks at, torso, arms and walking legs. */
@Composable
private fun BuddySprite(
  buddy: BuddyCharacter,
  face: Int,
  clock: () -> Float,
  walking: () -> Boolean,
  dancing: () -> Boolean,
  sleeping: Boolean,
  lookX: () -> Float,
  lookY: () -> Float,
  facing: () -> Float,
) {
  val dens = LocalDensity.current
  val torsoTop = if (buddy == BuddyCharacter.LUNA) Color(0xFF2E1A5C) else Color(0xFF1A3266)
  val torsoBottom = if (buddy == BuddyCharacter.LUNA) Color(0xFF150B2B) else Color(0xFF0B1630)
  Box(Modifier.size(64.dp, 92.dp)) {
    // ground shadow: soft dark core plus a coloured under-glow so it reads on black too
    Canvas(Modifier.fillMaxSize()) {
      val hop = hopOf(clock(), dancing(), walking(), dens)
      val k = 1f - (hop / 9.dp.toPx()).coerceIn(0f, 1f) * 0.45f
      val cx = size.width / 2f
      val cy = 88.dp.toPx()
      val w = 46.dp.toPx() * k
      withTransform({ scale(1f, 0.2f, pivot = Offset(cx, cy)) }) {
        drawCircle(
          Brush.radialGradient(listOf(buddy.accent.copy(alpha = 0.30f * k), Color.Transparent), center = Offset(cx, cy), radius = w * 0.85f),
          radius = w * 0.85f,
          center = Offset(cx, cy)
        )
        drawCircle(
          Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.62f * k), Color.Transparent), center = Offset(cx, cy), radius = w * 0.55f),
          radius = w * 0.55f,
          center = Offset(cx, cy)
        )
      }
    }

    Box(
      Modifier.fillMaxSize().graphicsLayer {
        translationY = -hopOf(clock(), dancing(), walking(), dens)
      }
    ) {
      // body: legs, torso, arms
      Canvas(Modifier.fillMaxSize()) {
        val t = clock()
        val cx = size.width / 2f
        val w = walking()
        val dn = dancing()
        fun p(v: Float) = v.dp.toPx()
        val legTop = p(68f)
        // legs + shoes
        for (side in intArrayOf(-1, 1)) {
          val ph = if (w) sin(t * 7f + (if (side < 0) 0f else PI.toFloat())) else 0f
          val lift = if (w) max(0f, ph) * p(3.5f) else 0f
          val lx = cx + side * p(6.5f) + ph * p(4f)
          drawRoundRect(Color(0xFF0C0E1A), Offset(lx - p(2.6f), legTop - lift), Size(p(5.2f), p(12f)), CornerRadius(p(2.6f)))
          drawOval(Color(0xFFF1F1F8), Offset(lx - p(6.5f) + ph * p(1.5f), legTop + p(9f) - lift), Size(p(13f), p(7f)))
          drawOval(buddy.accent2.copy(alpha = 0.85f), Offset(lx - p(6.5f) + ph * p(1.5f), legTop + p(13.2f) - lift), Size(p(13f), p(2.8f)))
        }
        // arms (behind torso edge)
        for (side in intArrayOf(-1, 1)) {
          val sx = cx + side * p(14.5f)
          val sy = p(54f)
          val outward =
            when {
              dn -> 128f + 26f * sin(t * 6.9f + (if (side < 0) 0f else PI.toFloat()))
              w -> 12f + 15f * sin(t * 7f + (if (side < 0) PI.toFloat() else 0f))
              else -> 9f + 2f * sin(t * 2f)
            }
          rotate(-side * outward, pivot = Offset(sx, sy)) {
            drawRoundRect(torsoTop, Offset(sx - p(2.8f), sy - p(1f)), Size(p(5.6f), p(14f)), CornerRadius(p(2.8f)))
            drawCircle(Color(0xFFFFD9C2), p(3.1f), Offset(sx, sy + p(13.5f)))
          }
        }
        // torso
        drawRoundRect(
          Brush.verticalGradient(listOf(torsoTop, torsoBottom), startY = p(48f), endY = p(72f)),
          Offset(cx - p(14f), p(48f)),
          Size(p(28f), p(24f)),
          CornerRadius(p(10f))
        )
        drawRoundRect(buddy.accent.copy(alpha = 0.55f), Offset(cx - p(14f), p(48f)), Size(p(28f), p(24f)), CornerRadius(p(10f)), style = Stroke(p(1f)))
        // little equaliser on the hoodie, moving with the music
        for (i in 0 until 4) {
          val bh = p(3f) + (if (dn || w) p(5f) * (0.5f + 0.5f * sin(t * 9f + i * 1.3f)) else p(2f))
          val bx = cx - p(6.5f) + i * p(4.3f)
          drawRoundRect(buddy.accent2, Offset(bx, p(64f) - bh), Size(p(2.2f), bh), CornerRadius(p(1.1f)))
        }
        // hood highlight
        drawRoundRect(Color.White.copy(alpha = 0.10f), Offset(cx - p(11f), p(50f)), Size(p(10f), p(4f)), CornerRadius(p(2f)))
      }

      // head
      Box(
        Modifier.align(Alignment.TopCenter)
          .size(54.dp)
          .graphicsLayer {
            val t = clock()
            val lx = lookX()
            val ly = lookY()
            val sway =
              when {
                dancing() -> sin(t * 6.9f) * 7f
                walking() -> sin(t * 7f) * 3f
                sleeping -> 9f + sin(t * 1.2f) * 2f
                else -> sin(t * 1.4f) * 1.8f
              }
            rotationY = -lx * 20f - facing() * 4f
            rotationX = ly * 14f
            rotationZ = sway
            cameraDistance = 16f * dens.density
            transformOrigin = TransformOrigin(0.5f, 0.95f)
          }
          .shadow(7.dp, CircleShape)
          .clip(CircleShape)
          .border(
            2.dp,
            Brush.sweepGradient(listOf(buddy.accent, buddy.accent2, buddy.accent)),
            CircleShape
          )
      ) {
        Crossfade(targetState = face, animationSpec = tween(220), label = "face") { f ->
          Image(
            painter = painterResource(f),
            contentDescription = buddy.displayName,
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.fillMaxSize().graphicsLayer {
                // the face slides toward whatever Buddy is looking at
                translationX = lookX() * 5.dp.toPx()
                translationY = lookY() * 4.dp.toPx()
                scaleX = 1.14f
                scaleY = 1.14f
              }
          )
        }
        // sphere shading: soft edge darkening plus a specular spot that follows the look
        Canvas(Modifier.fillMaxSize()) {
          val r = size.minDimension / 2f
          drawCircle(
            Brush.radialGradient(
              0.55f to Color.Transparent,
              1f to Color.Black.copy(alpha = 0.38f),
              center = center,
              radius = r
            ),
            radius = r
          )
          val hx = size.width * (0.32f - lookX() * 0.08f)
          val hy = size.height * (0.24f - lookY() * 0.06f)
          drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent), center = Offset(hx, hy), radius = r * 0.55f),
            radius = r * 0.55f,
            center = Offset(hx, hy)
          )
        }
      }
    }

    if (sleeping) SleepZ(clock, buddy.accent2)
    MusicNotes(clock, dancing, buddy.accent2)
  }
}

@Composable
private fun SleepZ(clock: () -> Float, color: Color) {
  Box(Modifier.fillMaxSize()) {
    for (i in 0 until 3) {
      Text(
        "z",
        color = color,
        fontSize = (11 + i * 3).sp,
        fontWeight = FontWeight.Bold,
        modifier =
          Modifier.align(Alignment.TopEnd).graphicsLayer {
            val prog = ((clock() * 0.45f + i / 3f) % 1f)
            alpha = sin(prog * PI.toFloat())
            translationX = prog * 8.dp.toPx()
            translationY = -prog * 26.dp.toPx() + 14.dp.toPx()
          }
      )
    }
  }
}

@Composable
private fun MusicNotes(clock: () -> Float, dancing: () -> Boolean, color: Color) {
  Box(Modifier.fillMaxSize()) {
    for (i in 0 until 3) {
      Text(
        if (i % 2 == 0) "♪" else "♫",
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier =
          Modifier.align(if (i == 1) Alignment.TopStart else Alignment.TopEnd).graphicsLayer {
            if (!dancing()) {
              alpha = 0f
              return@graphicsLayer
            }
            val prog = ((clock() * 0.7f + i / 3f) % 1f)
            alpha = sin(prog * PI.toFloat())
            translationX = (if (i == 1) -1f else 1f) * (prog * 10.dp.toPx() + sin(prog * 9f) * 3.dp.toPx())
            translationY = -prog * 30.dp.toPx() + 18.dp.toPx()
          }
      )
    }
  }
}

/** Chat card: a big circular avatar that talks (bounces while the reply types out) plus a speech bubble. */
@Composable
private fun BuddyChatPanel(
  buddy: BuddyCharacter,
  face: Int,
  reply: String,
  thinking: Boolean,
  lastUser: String?,
  aiOn: Boolean,
  onSend: (String) -> Unit,
  onConnectAi: () -> Unit,
  onClose: () -> Unit,
) {
  var input by remember { mutableStateOf("") }
  var shown by remember { mutableIntStateOf(0) }
  val focus = remember { FocusRequester() }
  LaunchedEffect(reply) {
    shown = 0
    while (shown < reply.length) {
      delay(18)
      shown++
    }
  }
  LaunchedEffect(Unit) {
    delay(300)
    runCatching { focus.requestFocus() }
  }
  val talking = thinking || shown < reply.length
  val inf = rememberInfiniteTransition(label = "talk")
  val pulse by inf.animateFloat(
    initialValue = 1f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(tween(170, easing = LinearEasing), RepeatMode.Reverse),
    label = "pulse"
  )
  val bob by inf.animateFloat(
    initialValue = -3f,
    targetValue = 3f,
    animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "bob"
  )
  val ring by inf.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
    label = "ring"
  )

  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier.fillMaxSize()
        .background(Color.Black.copy(alpha = 0.5f))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
    )
    Column(
      Modifier.align(Alignment.BottomCenter)
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .padding(10.dp)
        .shadow(18.dp, RoundedCornerShape(30.dp))
        .clip(RoundedCornerShape(30.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF1B1538), Color(0xFF0C0A1C))))
        .border(1.dp, Brush.linearGradient(listOf(buddy.accent.copy(alpha = 0.9f), buddy.accent2.copy(alpha = 0.5f))), RoundedCornerShape(30.dp))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        .padding(14.dp)
    ) {
      Row(verticalAlignment = Alignment.Top) {
        // talking avatar inside a glowing circle
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
          Canvas(Modifier.fillMaxSize()) {
            drawCircle(
              Brush.radialGradient(listOf(buddy.accent.copy(alpha = if (talking) ring * 0.55f else 0.25f), Color.Transparent), radius = size.minDimension / 2f),
              radius = size.minDimension / 2f
            )
          }
          Box(
            Modifier.size(80.dp)
              .graphicsLayer {
                val s = if (talking) pulse else 1f
                scaleX = s
                scaleY = s
                translationY = bob.dp.toPx()
              }
              .shadow(12.dp, CircleShape)
              .clip(CircleShape)
              .border(2.5.dp, Brush.sweepGradient(listOf(buddy.accent, buddy.accent2, buddy.accent)), CircleShape)
          ) {
            Crossfade(targetState = face, animationSpec = tween(220), label = "chatface") { f ->
              Image(
                painter = painterResource(f),
                contentDescription = buddy.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }
              )
            }
            Canvas(Modifier.fillMaxSize()) {
              val r = size.minDimension / 2f
              drawCircle(Brush.radialGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f), center = center, radius = r), radius = r)
              drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.22f), radius = r * 0.55f),
                radius = r * 0.55f,
                center = Offset(size.width * 0.3f, size.height * 0.22f)
              )
            }
          }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(buddy.displayName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
              Text(
                if (thinking) "thinking…" else if (aiOn) "AI connected • online" else "online",
                color = buddy.accent2,
                fontSize = 11.sp
              )
            }
            Box(
              Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable(onClick = onClose),
              contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp)) }
          }
          Spacer(Modifier.height(6.dp))
          Box(
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
              .background(Brush.linearGradient(listOf(buddy.accent.copy(alpha = 0.28f), Color.White.copy(alpha = 0.08f))))
              .border(1.dp, buddy.accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
              .padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Text(
              if (thinking && reply.isEmpty()) "…" else if (thinking) "…" else reply.take(shown),
              color = Color.White,
              fontSize = 14.sp,
              lineHeight = 19.sp
            )
          }
        }
      }

      if (lastUser != null) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Text(
            lastUser,
            color = Color.White,
            fontSize = 13.sp,
            modifier =
              Modifier.widthIn(max = 260.dp)
                .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .background(buddy.accent.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
          )
        }
      }

      Spacer(Modifier.height(10.dp))
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "🎧 Play something for me" to "play something for me",
            "🔍 What's playing?" to "what is playing now",
            "⏭ Next" to "next",
            "⏸ Pause" to "pause",
            "❤ Favourites" to "favourites",
          )
          .forEach { (label, cmd) -> ChatChip(label, buddy) { onSend(cmd) } }
        if (!aiOn) ChatChip("🔌 Connect AI", buddy, onConnectAi)
      }
      Spacer(Modifier.height(10.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
          value = input,
          onValueChange = { input = it },
          placeholder = { Text("Message ${buddy.displayName}…", color = Color.White.copy(alpha = 0.45f)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = { onSend(input); input = "" }),
          modifier = Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).focusRequester(focus),
          colors =
            TextFieldDefaults.colors(
              focusedContainerColor = Color.White.copy(alpha = 0.10f),
              unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              cursorColor = buddy.accent2
            )
        )
        Spacer(Modifier.width(8.dp))
        Box(
          Modifier.size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(buddy.accent2, buddy.accent)))
            .clickable(enabled = input.isNotBlank() && !thinking) {
              onSend(input)
              input = ""
            },
          contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(22.dp)) }
      }
    }
  }
}

@Composable
private fun ChatChip(text: String, buddy: BuddyCharacter, onClick: () -> Unit) {
  Text(
    text,
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(buddy.accent.copy(alpha = 0.22f))
        .border(1.dp, buddy.accent.copy(alpha = 0.55f), RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp)
  )
}
