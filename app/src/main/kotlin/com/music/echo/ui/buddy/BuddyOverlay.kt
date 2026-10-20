@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.buddy

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import echo.music.iad1tya.constants.BuddyToneKey
import echo.music.iad1tya.constants.BuddyTtsKey
import java.util.Locale
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import echo.music.iad1tya.constants.BuddyPinnedKey
import echo.music.iad1tya.constants.BuddyPosXKey
import echo.music.iad1tya.constants.BuddyPosYKey
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
import kotlinx.coroutines.Dispatchers
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

private class Heart(val birth: Float, val dx: Float, val size: Float, val tint: Color)

private const val MENU_W = 252f

private fun DrawScope.drawHeart(cx: Float, cy: Float, s: Float, color: Color, alpha: Float) {
  val path = Path()
  path.moveTo(cx, cy + s * 0.35f)
  path.cubicTo(cx - s * 1.1f, cy - s * 0.35f, cx - s * 0.55f, cy - s * 1.05f, cx, cy - s * 0.35f)
  path.cubicTo(cx + s * 0.55f, cy - s * 1.05f, cx + s * 1.1f, cy - s * 0.35f, cx, cy + s * 0.35f)
  path.close()
  drawPath(path, color.copy(alpha = alpha))
  drawCircle(Color.White.copy(alpha = alpha * 0.7f), radius = s * 0.12f, center = Offset(cx - s * 0.42f, cy - s * 0.38f))
}

private class ChatMsg(val fromUser: Boolean, text: String, isError: Boolean = false) {
  var text by mutableStateOf(text)
  var isError by mutableStateOf(isError)
}

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
        BuddyMood.SHY -> R.drawable.buddy_luna_shy
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
        BuddyMood.SHY -> R.drawable.buddy_kai_happy
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
  val selected by rememberPreference(BuddySelectedKey, "luna")
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
  var posLoaded by remember { mutableStateOf(false) }
  var pinned by remember { mutableStateOf(false) }
  var pinFx by remember { mutableFloatStateOf(0.5f) }
  var pinFy by remember { mutableFloatStateOf(0.6f) }
  var touchPulse by remember { mutableIntStateOf(0) }
  var pendingChat by remember { mutableStateOf<String?>(null) }
  var blushUntil by remember { mutableFloatStateOf(0f) }
  val hearts = remember { mutableStateListOf<Heart>() }

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
  val pinnedState = rememberUpdatedState(pinned)

  // ----- saved spot (the user's free placement survives restarts) -----
  LaunchedEffect(Unit) {
    val (p, fx, fy) =
      withContext(Dispatchers.IO) {
        Triple(
          context.dataStore.get(BuddyPinnedKey, false),
          context.dataStore.get(BuddyPosXKey, 0.5f),
          context.dataStore.get(BuddyPosYKey, 0.6f)
        )
      }
    pinned = p
    pinFx = fx
    pinFy = fy
    posLoaded = true
  }

  // The Settings switch can release Buddy while the app is running.
  val prefPinned by rememberPreference(BuddyPinnedKey, false)
  LaunchedEffect(prefPinned, posLoaded) {
    if (posLoaded && prefPinned != pinned) pinned = prefPinned
  }

  fun savePin() {
    val p = pinned
    val fx = pinFx
    val fy = pinFy
    scope.launch(Dispatchers.IO) {
      context.dataStore.edit {
        it[BuddyPinnedKey] = p
        it[BuddyPosXKey] = fx
        it[BuddyPosYKey] = fy
      }
    }
  }

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
        if (screenW > 0f && screenH > 0f && posLoaded) {
          if (pinned && !dragging) {
            // free placement: stay exactly where the user dropped it, on every screen
            xPos = pinFx * (screenW - spriteW)
            yPos = pinFy * (screenH - 92f * d)
            targetX = xPos
            walking = false
            placed = true
            scaleNow += (1f - scaleNow) * min(1f, dt * 8f)
            lookX += (lookTx - lookX) * min(1f, dt * 9f)
            lookY += (lookTy - lookY) * min(1f, dt * 9f)
            return@withFrameNanos
          }
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
      val mw = MENU_W * d
      val mcx = xPos + spriteW / 2f
      val mLeft = (mcx - mw / 2f).coerceIn(8f * d, max(8f * d, screenW - mw - 8f * d))
      val mAbove = yPos + footLine * (1f - scaleNow) - 70f * d
      val mTop = if (mAbove < 4f * d) yPos + 96f * d else mAbove
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

  // ----- blush + floating hearts when touched -----
  LaunchedEffect(touchPulse) {
    if (touchPulse == 0) return@LaunchedEffect
    blushUntil = clock + 1.8f
    moodOverride = BuddyMood.SHY
    hearts.removeAll { clock - it.birth > 1.8f }
    val palette = listOf(Color(0xFFFF4D8D), Color(0xFFFF7BAA), Color(0xFFFF2E63), Color(0xFFFFA3C4))
    repeat(5) { i ->
      hearts.add(
        Heart(
          birth = clock + i * 0.13f,
          dx = (Random.nextFloat() * 2f - 1f) * 20f * d,
          size = (4.5f + Random.nextFloat() * 3.5f) * d,
          tint = palette[i % palette.size]
        )
      )
    }
    delay(1800)
    if (bubble == null && moodOverride == BuddyMood.SHY) moodOverride = null
    delay(600)
    hearts.removeAll { clock - it.birth > 1.8f }
  }

  // ----- voice search -----
  fun looksLikeCommand(t: String): Boolean {
    val l = t.lowercase().trim()
    return listOf("play", "pause", "stop", "next", "skip", "previous", "resume", "search", "find", "continue")
      .any { l == it || l.startsWith("$it ") }
  }

  fun playByVoice(text: String) {
    scope.launch {
      say("I heard \"$text\" 🎧", BuddyMood.THINKING, 3000)
      thinking = true
      val l = text.lowercase().trim()
      val turn =
        if (looksLikeCommand(l) && !l.startsWith("play ")) {
          BuddyBrain.respond(context, buddy, text, emptyList(), null, "")
        } else {
          val q = l.removePrefix("play ").removeSuffix(" song").trim()
          BuddyTurn("Playing $q 🎶", "play", q, "excited")
        }
      val extra = BuddyBrain.execute(turn, pc, db, navController)
      thinking = false
      say(extra ?: turn.reply, BuddyMood.EXCITED, 4800)
    }
  }

  val voiceLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      val heard =
        res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
      if (res.resultCode == Activity.RESULT_OK && !heard.isNullOrBlank()) {
        if (chat.isEmpty()) chat.add(ChatMsg(false, greetingIst(buddy, false, true).first))
        menuOpen = false
        chatOpen = true
        pendingChat = heard
      } else if (!chatOpen) {
        say("I didn't catch that — tap the mic and try again 🎤", BuddyMood.SURPRISED, 3000)
      }
    }

  fun startVoice() {
    val intent =
      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ${buddy.displayName}…")
    try {
      voiceLauncher.launch(intent)
    } catch (e: ActivityNotFoundException) {
      say("Voice input isn't available on this phone 😕", BuddyMood.SURPRISED, 3500)
    }
  }

  // ----- wandering, greeting and small talk -----
  LaunchedEffect(buddy) {
    snapshotFlow { placed }.first { it }
    delay(1800)
    if (greetingOn) {
      val lastSeen = withContext(kotlinx.coroutines.Dispatchers.IO) { context.dataStore.get(BuddyLastSeenKey, 0L) }
      val back = lastSeen > 0L && System.currentTimeMillis() - lastSeen > 6 * 60 * 60 * 1000L
      val timeAware = withContext(kotlinx.coroutines.Dispatchers.IO) { context.dataStore.get(echo.music.iad1tya.constants.BuddyTimeGreetingsKey, true) }
      val (text, m) = greetingIst(buddy, back, timeAware)
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
      if (!pinnedState.value && !walking && Random.nextFloat() < 0.32f) {
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
      if (untilTalk <= 0 && bubble == null && (pinnedState.value || !laneState.value.hidden)) {
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

  val ttsOn by rememberPreference(BuddyTtsKey, false)
  val setTtsOn = rememberPreference(BuddyTtsKey, false).component2()
  val selectedBuddyKey by rememberPreference(BuddySelectedKey, "luna")
  var tts by remember { mutableStateOf<TextToSpeech?>(null) }
  DisposableEffect(ttsOn) {
    var engine: TextToSpeech? = null
    if (ttsOn) {
      engine = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) runCatching { engine?.language = Locale.getDefault() }
      }
      tts = engine
    }
    onDispose {
      runCatching { engine?.stop(); engine?.shutdown() }
      tts = null
    }
  }
  fun speak(text: String) {
    val clean = text.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "").trim()
    val engine = tts
    if (clean.isNotEmpty() && engine != null) {
      val luna = selectedBuddyKey != "kai"
      // cute voices: Luna is bright and sweet, Kai is a little higher and bouncier than a normal boy voice
      runCatching {
        engine.setPitch(if (luna) 1.6f else 1.3f)
        engine.setSpeechRate(if (luna) 1.08f else 1.06f)
        val voices = engine.voices?.filter { it.locale.language == Locale.getDefault().language && !it.isNetworkConnectionRequired }.orEmpty()
        val pick =
          if (luna) voices.firstOrNull { it.name.contains("female", true) } ?: voices.firstOrNull { it.name.contains("-x-ena") || it.name.contains("-x-iob") }
          else voices.firstOrNull { it.name.contains("male", true) && !it.name.contains("female", true) } ?: voices.firstOrNull { it.name.contains("-x-end") || it.name.contains("-x-iom") }
        if (pick != null) engine.voice = pick
      }
      engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "buddy")
    }
  }

  var chatJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
  var lastUserText by remember { mutableStateOf("") }

  suspend fun buildInfo(userText: String = ""): BuddyContextInfo {
    val md = metadata
    val nowPlaying = md?.let { m -> "${m.title} — ${m.artists.joinToString { it.name }}" }
    val upNext =
      runCatching {
          val pl = pc?.player
          if (pl != null && pl.currentMediaItemIndex + 1 < pl.mediaItemCount)
            pl.getMediaItemAt(pl.currentMediaItemIndex + 1).mediaMetadata.title?.toString()
          else null
        }
        .getOrNull()
    val lyrics =
      if (md == null || !Regex("(?i)lyric|mean|song|about|story|sing").containsMatchIn(userText)) null
      else
        runCatching {
            withTimeoutOrNull(250) { pc?.currentLyrics?.firstOrNull() }
              ?.lyrics
              ?.replace(Regex("\\[\\d+:\\d+(?:[.:]\\d+)?]"), "")
              ?.replace(Regex("\\s+"), " ")
              ?.trim()
              ?.take(700)
              ?.takeIf { it.length > 20 }
          }
          .getOrNull()
    val (mem, tone) =
      withContext(kotlinx.coroutines.Dispatchers.IO) {
        (if (BuddyMemory.enabled(context)) BuddyMemory.items(context) else emptyList()) to context.dataStore.get(BuddyToneKey, 0)
      }
    return BuddyContextInfo(nowPlaying, isPlaying, upNext, lyrics, taste ?: BuddyBrain.tasteLine(db).also { taste = it }, mem, tone)
  }

  fun send(raw: String) {
    val text = raw.trim()
    if (text.isEmpty() || thinking) return
    lastUserText = text
    chat.add(ChatMsg(true, text))
    val reply = ChatMsg(false, "")
    chat.add(reply)
    thinking = true
    chatJob =
      scope.launch {
        val history = chat.dropLast(2).filter { !it.isError && it.text.isNotBlank() }.map { it.fromUser to it.text }
        val turn =
          BuddyBrain.converse(context, buddy, text, history, buildInfo(text)) { visibleText -> reply.text = visibleText }
        reply.text = turn.reply
        reply.isError = turn.error != null
        moodOverride = moodOf(turn.mood)
        thinking = false
        latestReply = turn.reply
        val extra = if (turn.error == null) BuddyBrain.execute(turn, pc, db, navController) else null
        if (extra != null) {
          chat.add(ChatMsg(false, extra))
          latestReply = extra
        }
        if (turn.error == null && ttsOn) speak(if (extra != null) turn.reply + " " + extra else turn.reply)
        if (turn.action == "search" || turn.action == "favorites") {
          delay(900)
          chatOpen = false
        }
      }
  }

  fun newChat() {
    chatJob?.cancel()
    tts?.stop()
    thinking = false
    chat.clear()
    chat.add(ChatMsg(false, greetingIst(buddy, false, true).first))
  }

  LaunchedEffect(pendingChat) {
    val t = pendingChat ?: return@LaunchedEffect
    pendingChat = null
    send(t)
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
            onPress = {
              touchPulse++
              tryAwaitRelease()
            },
            onTap = {
              targetX = xPos
              bubble = null
              menuOpen = !menuOpen
            },
            onDoubleTap = {
              menuOpen = false
              if (pinned) {
                pinned = false
                savePin()
                say("Back to roaming! 🚶", BuddyMood.PLAYFUL, 2600)
              } else {
                pinned = true
                pinFx = (xPos / (screenW - spriteW).coerceAtLeast(1f)).coerceIn(0f, 1f)
                pinFy = (yPos / (screenH - 92f * d).coerceAtLeast(1f)).coerceIn(0f, 1f)
                savePin()
                say("I'll stay right here 📍", BuddyMood.HAPPY, 2600)
              }
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
              // wherever you drop it, it stays there (home, player, settings, everywhere)
              val first = !pinned
              pinned = true
              pinFx = (xPos / (screenW - spriteW).coerceAtLeast(1f)).coerceIn(0f, 1f)
              pinFy = (yPos / (screenH - 92f * d).coerceAtLeast(1f)).coerceIn(0f, 1f)
              savePin()
              if (first) say("I'll stay here! Double-tap me to let me roam again 📍", BuddyMood.HAPPY, 4200)
            },
            onDragCancel = {
              dragging = false
              targetX = xPos
              moodOverride = null
            },
            onDrag = { change, amount ->
              change.consume()
              xPos = (xPos + amount.x).coerceIn(0f, (screenW - spriteW).coerceAtLeast(0f))
              yPos = (yPos + amount.y).coerceIn(0f, (screenH - 92f * d).coerceAtLeast(0f))
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
        blush = { ((blushUntil - clock) / 0.5f).coerceIn(0f, 1f) },
      )
    }

    // ---------------- floating hearts ----------------
    if (hearts.isNotEmpty()) {
      Canvas(Modifier.fillMaxSize()) {
        val t = clock
        val cx = xPos + spriteW / 2f
        val top = yPos + footLine * (1f - scaleNow) + 6f * d
        hearts.forEach { h ->
          val p = (t - h.birth) / 1.6f
          if (p in 0f..1f) {
            val a = sin(p * PI.toFloat()).coerceIn(0f, 1f)
            val x = cx + h.dx + sin(p * 7f + h.dx) * 7f * d
            val y = top - p * 78f * d
            drawHeart(x, y, h.size * (0.75f + 0.5f * p), h.tint, a)
          }
        }
      }
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
          val above = yPos + footLine * (1f - scaleNow) - bh - 2f * d
          val top = if (above < 4f * d) yPos + 96f * d else above
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
          val mw = MENU_W * d
          val cx = xPos + spriteW / 2f
          val left = (cx - mw / 2f).coerceIn(8f * d, max(8f * d, screenW - mw - 8f * d))
          val above = yPos + footLine * (1f - scaleNow) - 70f * d
          val top = if (above < 4f * d) yPos + 96f * d else above
          IntOffset(left.toInt(), top.toInt())
        }
    ) {
      Row(
        Modifier.width(MENU_W.dp)
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
        MenuButton(buddy, Icons.Rounded.Mic, "Voice") {
          menuOpen = false
          say("Listening… say a song name 🎤", BuddyMood.EXCITED, 2500)
          startVoice()
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
        chat = chat,
        thinking = thinking,
        aiOn = aiOn,
        ttsOn = ttsOn,
        onToggleTts = {
          if (ttsOn) tts?.stop()
          setTtsOn(!ttsOn)
        },
        onNewChat = { newChat() },
        onRetry = { if (lastUserText.isNotBlank() && !thinking) { chat.removeAll { it.isError }; send(lastUserText) } },
        onSend = { send(it) },
        onVoice = { startVoice() },
        onConnectAi = {
          chatOpen = false
          navController.navigate("settings/ai") { launchSingleTop = true }
        },
        onClose = {
          chatOpen = false
          moodOverride = null
          tts?.stop()
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
      Modifier.size(44.dp).buddyGlass(CircleShape, if (danger) Color(0xFFFF5C7A) else buddy.accent, strength = 1.2f),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.height(3.dp))
    Text(label, color = Color.White.copy(alpha = 0.92f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
  blush: () -> Float,
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
          val b = blush()
          if (b > 0.01f) {
            // rosy cheeks
            val cheek = Color(0xFFFF5C8A).copy(alpha = 0.55f * b)
            drawOval(Brush.radialGradient(listOf(cheek, Color.Transparent), center = Offset(size.width * 0.27f, size.height * 0.64f), radius = size.width * 0.17f), topLeft = Offset(size.width * 0.08f, size.height * 0.53f), size = Size(size.width * 0.38f, size.height * 0.22f))
            drawOval(Brush.radialGradient(listOf(cheek, Color.Transparent), center = Offset(size.width * 0.73f, size.height * 0.64f), radius = size.width * 0.17f), topLeft = Offset(size.width * 0.54f, size.height * 0.53f), size = Size(size.width * 0.38f, size.height * 0.22f))
          }
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

/** Conversation panel: glass bubbles, streaming answers, typing dots, glass input and controls. */
@Composable
private fun BuddyChatPanel(
  buddy: BuddyCharacter,
  face: Int,
  chat: List<ChatMsg>,
  thinking: Boolean,
  aiOn: Boolean,
  ttsOn: Boolean,
  onToggleTts: () -> Unit,
  onNewChat: () -> Unit,
  onRetry: () -> Unit,
  onSend: (String) -> Unit,
  onVoice: () -> Unit,
  onConnectAi: () -> Unit,
  onClose: () -> Unit,
) {
  var input by remember { mutableStateOf("") }
  val focus = remember { FocusRequester() }
  val listState = rememberLazyListState()
  val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
  val lastLen = chat.lastOrNull()?.text?.length ?: 0
  LaunchedEffect(chat.size, lastLen / 30, thinking) {
    if (chat.isNotEmpty()) listState.animateScrollToItem(chat.size - 1)
  }
  LaunchedEffect(Unit) {
    delay(350)
    runCatching { focus.requestFocus() }
  }
  fun submit() {
    if (input.isNotBlank() && !thinking) {
      onSend(input)
      input = ""
    }
  }
  val inf = rememberInfiniteTransition(label = "talk")
  val pulse by inf.animateFloat(1f, 1.07f, infiniteRepeatable(tween(170, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
  val ring by inf.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ring")
  val talking = thinking

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
        .heightIn(max = (screenH * 0.74f).dp)
        .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = buddy.accent, spotColor = buddy.accent)
        .clip(RoundedCornerShape(32.dp))
        .background(Brush.verticalGradient(listOf(Color(0xE61A1440), Color(0xEB0A0818))))
        .buddyGlass(RoundedCornerShape(32.dp), buddy.accent, strength = 0.8f)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        .padding(14.dp)
    ) {
      // header
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
          Canvas(Modifier.fillMaxSize()) {
            drawCircle(
              Brush.radialGradient(listOf(buddy.accent.copy(alpha = if (talking) ring * 0.6f else 0.25f), Color.Transparent), radius = size.minDimension / 2f),
              radius = size.minDimension / 2f
            )
          }
          Box(
            Modifier.size(48.dp)
              .graphicsLayer { val sc = if (talking) pulse else 1f; scaleX = sc; scaleY = sc }
              .clip(CircleShape)
              .border(2.dp, Brush.sweepGradient(listOf(buddy.accent, buddy.accent2, buddy.accent)), CircleShape)
          ) {
            Crossfade(targetState = face, animationSpec = tween(220), label = "chatface") { f ->
              Image(painter = painterResource(f), contentDescription = buddy.displayName, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.08f; scaleY = 1.08f })
            }
          }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
          Text(buddy.displayName, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
          Text(
            if (thinking) "thinking…" else if (aiOn) "Your AI key · online" else "Free AI · online",
            color = buddy.accent2, fontSize = 11.sp
          )
        }
        GlassCircleButton(if (ttsOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, if (ttsOn) "Turn off read aloud" else "Read answers aloud", buddy.accent, onToggleTts, size = 38.dp, active = ttsOn)
        Spacer(Modifier.width(6.dp))
        GlassCircleButton(Icons.Rounded.Refresh, "New chat", buddy.accent, onNewChat, size = 38.dp)
        Spacer(Modifier.width(6.dp))
        GlassCircleButton(Icons.Rounded.Close, "Close", buddy.accent, onClose, size = 38.dp)
      }
      Spacer(Modifier.height(8.dp))

      // conversation
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(chat.size) { i ->
          val m = chat[i]
          Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
            GlassBubble(m.text, m.fromUser, buddy.accent, m.isError, Modifier.widthIn(max = 300.dp)) {
              TypingDots(buddy.accent)
            }
          }
        }
        if (chat.lastOrNull()?.isError == true) {
          item { Row { GlassChip("↻ Try again", buddy.accent, onRetry) } }
        }
      }

      Spacer(Modifier.height(10.dp))
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "🎧 Play something relaxing" to "play something relaxing",
            "📚 Study music" to "suggest music for studying",
            "🎵 Songs like this" to "find songs similar to the current track",
            "💬 What's this song about?" to "explain the meaning of this song",
            "⏸ Pause + fun fact" to "pause the music and tell me something interesting",
            "💡 Ask me anything" to "tell me something amazing I probably don't know",
          )
          .forEach { (label, cmd) -> GlassChip(label, buddy.accent) { onSend(cmd) } }
        if (!aiOn) GlassChip("🔌 Use my own AI key", buddy.accent, onConnectAi)
      }
      Spacer(Modifier.height(10.dp))
      Row(verticalAlignment = Alignment.Bottom) {
        GlassTextField(
          value = input,
          onValueChange = { input = it },
          placeholder = "Message ${buddy.displayName}…",
          accent = buddy.accent,
          onSend = { submit() },
          modifier = Modifier.weight(1f),
          focusRequester = focus,
        )
        Spacer(Modifier.width(8.dp))
        GlassCircleButton(Icons.Rounded.Mic, "Speak", buddy.accent, onVoice, size = 48.dp)
        Spacer(Modifier.width(8.dp))
        GlassCircleButton(Icons.Rounded.Send, "Send", buddy.accent, { submit() }, size = 48.dp, active = input.isNotBlank(), enabled = input.isNotBlank() && !thinking)
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
