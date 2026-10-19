@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.settings

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.BuddyAnimationsKey
import echo.music.iad1tya.constants.BuddyEnabledKey
import echo.music.iad1tya.constants.BuddyFloatingKey
import echo.music.iad1tya.constants.BuddyPinnedKey
import echo.music.iad1tya.constants.BuddyGreetingKey
import echo.music.iad1tya.constants.BuddyLastSeenKey
import echo.music.iad1tya.constants.BuddySelectedKey
import echo.music.iad1tya.constants.BuddySleepModeKey
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.sin

/** The two companions. Artwork is cropped from the supplied Luna & Kai reference sheet. */
enum class BuddyCharacter(
  val key: String,
  val displayName: String,
  val role: String,
  val tagline: String,
  val accent: Color,
  val accent2: Color,
  val body: Int,
  val happy: Int,
  val excited: Int,
  val sleepy: Int,
  val thinking: Int,
  val playful: Int,
) {
  LUNA(
    "luna", "Luna", "Girl Companion", "Warm, cute and friendly. Good vibes only!",
    Color(0xFFB45CFF), Color(0xFFFF7BD5),
    R.drawable.buddy_luna, R.drawable.buddy_luna_happy, R.drawable.buddy_luna_excited,
    R.drawable.buddy_luna_sleepy, R.drawable.buddy_luna_thinking, R.drawable.buddy_luna_playful
  ),
  KAI(
    "kai", "Kai", "Boy Companion", "Cool, playful and energetic. Let's find your next favourite!",
    Color(0xFF3D8BFF), Color(0xFF29E0FF),
    R.drawable.buddy_kai, R.drawable.buddy_kai_happy, R.drawable.buddy_kai_excited,
    R.drawable.buddy_kai_sleepy, R.drawable.buddy_kai_thinking, R.drawable.buddy_kai_playful
  );

  companion object {
    fun from(key: String): BuddyCharacter? = entries.firstOrNull { it.key == key }
  }
}

private val BuddyBg = Color(0xFF060A13) // matches the artwork's own backdrop so it blends in

private fun indiaHour(): Int = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).hour

private fun greetingFor(hour: Int, name: String): String =
  when (hour) {
    in 5..11 -> "Good morning! Ready for some music? ☀️"
    in 12..16 -> "Good afternoon! What shall we play? 🎧"
    in 17..20 -> "Good evening! Time to unwind with a song 🌆"
    else -> "It's late… want something calm? 🌙"
  }

private data class ChatLine(val fromUser: Boolean, val text: String)

@Composable
fun AICompanionScreen(navController: NavController) {
  val (selectedKey, onSelectedChange) = rememberPreference(BuddySelectedKey, defaultValue = "luna")
  val (enabled, onEnabledChange) = rememberPreference(BuddyEnabledKey, defaultValue = true)
  val (animations, onAnimationsChange) = rememberPreference(BuddyAnimationsKey, defaultValue = true)
  val (floating, onFloatingChange) = rememberPreference(BuddyFloatingKey, defaultValue = true)
  val (pinned, onPinnedChange) = rememberPreference(BuddyPinnedKey, defaultValue = false)
  val (greeting, onGreetingChange) = rememberPreference(BuddyGreetingKey, defaultValue = true)
  val (sleepMode, onSleepModeChange) = rememberPreference(BuddySleepModeKey, defaultValue = true)
  val (lastSeen, onLastSeenChange) = rememberPreference(BuddyLastSeenKey, defaultValue = 0L)

  val buddy = BuddyCharacter.from(selectedKey)

  Box(Modifier.fillMaxSize().background(BuddyBg)) {
    if (buddy == null) {
      BuddyOnboarding(
        onBack = { navController.navigateUp() },
        onChoose = { chosen ->
          onSelectedChange(chosen.key)
          onEnabledChange(true)
        }
      )
    } else {
      BuddyMain(
        buddy = buddy,
        enabled = enabled,
        animations = animations,
        floating = floating,
        onFloatingChange = onFloatingChange,
        pinned = pinned,
        onPinnedChange = onPinnedChange,
        greeting = greeting,
        sleepMode = sleepMode,
        lastSeen = lastSeen,
        onBack = { navController.navigateUp() },
        navController = navController,
        onEnabledChange = onEnabledChange,
        onAnimationsChange = onAnimationsChange,
        onGreetingChange = onGreetingChange,
        onSleepModeChange = onSleepModeChange,
        onChangeBuddy = { onSelectedChange("") },
        onSeen = { onLastSeenChange(System.currentTimeMillis()) }
      )
    }
  }
}

@Composable
private fun BuddyOnboarding(onBack: () -> Unit, onChoose: (BuddyCharacter) -> Unit) {
  var picked by remember { mutableStateOf<BuddyCharacter?>(null) }
  Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    TopAppBar(
      title = {},
      navigationIcon = {
        IconButton(onClick = onBack) { Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White) }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
    Text(
      "Pattukal Buddy AI",
      fontSize = 30.sp,
      fontWeight = FontWeight.ExtraBold,
      style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(listOf(Color(0xFFB45CFF), Color(0xFF29E0FF)))),
    )
    Spacer(Modifier.height(6.dp))
    Text("Meet your little music companion.", color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      BuddyCharacter.entries.forEach { c ->
        val on = picked == c
        Column(
          Modifier.weight(1f)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(c.accent.copy(alpha = if (on) 0.30f else 0.14f), Color.White.copy(alpha = 0.03f))))
            .border(BorderStroke(if (on) 2.dp else 1.dp, (if (on) c.accent2 else c.accent).copy(alpha = if (on) 1f else 0.4f)), RoundedCornerShape(26.dp))
            .clickable { picked = c }
            .padding(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Image(
            painter = painterResource(c.body),
            contentDescription = c.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(260.dp)
          )
          Spacer(Modifier.height(8.dp))
          Text("${c.displayName} — ${c.role}", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp)
          Text(c.tagline, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, fontSize = 11.sp)
        }
      }
    }
    Spacer(Modifier.height(22.dp))
    Button(
      onClick = { picked?.let(onChoose) },
      enabled = picked != null,
      modifier = Modifier.fillMaxWidth().height(54.dp),
      shape = RoundedCornerShape(27.dp),
      colors = ButtonDefaults.buttonColors(containerColor = picked?.accent ?: Color(0xFF6E5BFF))
    ) {
      Text("Choose My Buddy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    }
    Spacer(Modifier.height(10.dp))
    Text("You can change your buddy any time in the companion settings.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun BuddyMain(
  buddy: BuddyCharacter,
  enabled: Boolean,
  animations: Boolean,
  floating: Boolean,
  onFloatingChange: (Boolean) -> Unit,
  pinned: Boolean,
  onPinnedChange: (Boolean) -> Unit,
  greeting: Boolean,
  sleepMode: Boolean,
  lastSeen: Long,
  onBack: () -> Unit,
  navController: NavController,
  onEnabledChange: (Boolean) -> Unit,
  onAnimationsChange: (Boolean) -> Unit,
  onGreetingChange: (Boolean) -> Unit,
  onSleepModeChange: (Boolean) -> Unit,
  onChangeBuddy: () -> Unit,
  onSeen: () -> Unit,
) {
  val playerConnection = LocalPlayerConnection.current
  val isPlaying by (playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
  val metadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })
  var showSettings by remember { mutableStateOf(false) }
  val chat = remember { mutableStateListOf<ChatLine>() }
  var input by remember { mutableStateOf("") }
  var thinking by remember { mutableStateOf(false) }

  val welcomeBack = remember { lastSeen > 0L && System.currentTimeMillis() - lastSeen > 6 * 60 * 60 * 1000L }
  LaunchedEffect(Unit) { onSeen() }

  val hour = indiaHour()
  val night = hour !in 5..20
  val sleepy = sleepMode && night && !isPlaying && metadata == null

  val bubbleText =
    when {
      metadata != null && isPlaying -> "Vibing to ${metadata!!.title}${metadata!!.artists.firstOrNull()?.name?.let { " — $it" } ?: ""} 🎶"
      sleepy -> "Zzz… it's quiet. Wake me with a song? 😴"
      greeting && welcomeBack -> "Welcome back! I missed you 💜"
      greeting -> greetingFor(hour, buddy.displayName)
      else -> "Hi! I'm ${buddy.displayName}."
    }
  val face =
    when {
      thinking -> buddy.thinking
      sleepy -> buddy.sleepy
      isPlaying -> buddy.excited
      else -> buddy.happy
    }

  fun reply(text: String) {
    chat.add(ChatLine(false, text))
    thinking = false
  }

  val appContext = androidx.compose.ui.platform.LocalContext.current
  val database = echo.music.iad1tya.LocalDatabase.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()

  fun handle(raw: String) {
    val q = raw.trim()
    if (q.isEmpty() || thinking) return
    chat.add(ChatLine(true, q))
    thinking = true
    scope.launch {
      val nowPlaying = metadata?.let { m -> "${m.title} — ${m.artists.joinToString { it.name }}" }
      val history = chat.dropLast(1).map { it.fromUser to it.text }
      val taste = echo.music.iad1tya.ui.buddy.BuddyBrain.tasteLine(database)
      val turn = echo.music.iad1tya.ui.buddy.BuddyBrain.respond(appContext, buddy, q, history, nowPlaying, taste)
      reply(turn.reply)
      echo.music.iad1tya.ui.buddy.BuddyBrain.execute(turn, playerConnection, database, navController)?.let { reply(it) }
    }
  }

  Column(Modifier.fillMaxSize().imePadding()) {
    TopAppBar(
      title = { Text("${buddy.displayName} • Buddy AI", color = Color.White, fontWeight = FontWeight.Bold) },
      navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White) } },
      actions = { TextButton(onClick = { showSettings = true }) { Text("Settings", color = buddy.accent2) } },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )

    if (!enabled) {
      Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("${buddy.displayName} is taking a break.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("The companion is turned off.", color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onEnabledChange(true) }, colors = ButtonDefaults.buttonColors(containerColor = buddy.accent)) { Text("Turn on ${buddy.displayName}", color = Color.White) }
      }
    } else {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Speech bubble with mood avatar
        Row(
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(buddy.accent.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))))
            .border(1.dp, buddy.accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AnimatedContent(targetState = face, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) }, label = "face") { f ->
            Image(painterResource(f), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
          }
          Spacer(Modifier.width(12.dp))
          AnimatedContent(targetState = bubbleText, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "bubble") { t ->
            Text(t, color = Color.White, fontSize = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
          }
        }

        Spacer(Modifier.height(10.dp))
        BuddyStage(buddy = buddy, playing = isPlaying, animations = animations, sleepy = sleepy)

        metadata?.let { md ->
          Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.07f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(model = md.thumbnailUrl, contentDescription = null, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
              Text(md.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(md.artists.joinToString { it.name }, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (isPlaying) "NOW PLAYING" else "PAUSED", color = buddy.accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
          }
          Spacer(Modifier.height(10.dp))
        }

        // Quick actions
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          BuddyChip("🔍 Find a song", buddy) { navController.navigate("search_input") }
          BuddyChip(if (isPlaying) "⏸ Pause" else "▶ Play", buddy) {
            val pc = playerConnection
            if (pc == null || pc.mediaMetadata.value == null) navController.navigate("auto_playlist/liked")
            else pc.togglePlayPause()
          }
          BuddyChip("⏭ Next", buddy) { playerConnection?.seekToNext() }
          BuddyChip("❤ Favourites", buddy) { navController.navigate("auto_playlist/liked") }
        }

        Spacer(Modifier.height(10.dp))
        chat.takeLast(6).forEach { line ->
          Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start) {
            Text(
              line.text,
              color = Color.White,
              fontSize = 14.sp,
              modifier =
                Modifier.clip(RoundedCornerShape(18.dp))
                  .background(if (line.fromUser) buddy.accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f))
                  .padding(horizontal = 14.dp, vertical = 8.dp)
            )
          }
        }
        Spacer(Modifier.height(8.dp))
      }

      Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
          value = input,
          onValueChange = { input = it },
          placeholder = { Text("Ask ${buddy.displayName} to search, play, pause…", color = Color.White.copy(alpha = 0.45f)) },
          singleLine = true,
          modifier = Modifier.weight(1f).clip(RoundedCornerShape(28.dp)),
          colors =
            TextFieldDefaults.colors(
              focusedContainerColor = Color.White.copy(alpha = 0.09f),
              unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              cursorColor = buddy.accent2
            )
        )
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = { handle(input); input = "" },
          enabled = input.isNotBlank(),
          shape = CircleShape,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
          modifier = Modifier.size(50.dp),
          colors = ButtonDefaults.buttonColors(containerColor = buddy.accent)
        ) { Text("➤", color = Color.White) }
      }
    }
  }

  if (showSettings) {
    ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = Color(0xFF0B0F1C), contentColor = Color.White) {
      Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Companion settings", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        BuddySwitchRow("Show companion", "Turn ${buddy.displayName} on or off", enabled, onEnabledChange, buddy)
        BuddySwitchRow("Floating companion", "${buddy.displayName} walks around the app and the player, and reacts to your taps", floating, onFloatingChange, buddy)
        BuddySwitchRow("Stay where I place it", "Drag the companion anywhere and it stays there on every screen. Turn off to let it walk around again (or double-tap it)", pinned, onPinnedChange, buddy)
        BuddySwitchRow("Character animation", "Gentle floating and music-vibe motion", animations, onAnimationsChange, buddy)
        BuddySwitchRow("Greetings", "Time-of-day (India time) and welcome-back messages", greeting, onGreetingChange, buddy)
        BuddySwitchRow("Sleep mode", "Sleepy face at night when nothing is playing", sleepMode, onSleepModeChange, buddy)
        BuddyBrainSettings(buddy)
        Spacer(Modifier.height(6.dp))
        Button(
          onClick = { showSettings = false; onChangeBuddy() },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = buddy.accent)
        ) { Text("Change my buddy", color = Color.White) }
      }
    }
  }
}

@Composable
private fun BuddyChip(text: String, buddy: BuddyCharacter, onClick: () -> Unit) {
  Text(
    text,
    color = Color.White,
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(Brush.verticalGradient(listOf(buddy.accent.copy(alpha = 0.35f), buddy.accent.copy(alpha = 0.12f))))
        .border(1.dp, buddy.accent2.copy(alpha = 0.5f), RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 9.dp)
  )
}

@Composable
private fun BuddySwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit, buddy: BuddyCharacter) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.SemiBold)
      Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
    Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = buddy.accent, checkedThumbColor = Color.White))
  }
}

/** Full-body artwork (never stretched) over a soft glow; floats gently and shows music bars while playing. */
@Composable
private fun BuddyStage(buddy: BuddyCharacter, playing: Boolean, animations: Boolean, sleepy: Boolean) {
  val inf = rememberInfiniteTransition(label = "buddy")
  val phase by inf.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "float")
  val glow by inf.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow")
  val barPhase by inf.animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "bars")
  Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.BottomCenter) {
    Canvas(Modifier.fillMaxSize()) {
      val g = if (animations && playing) glow else 0.6f
      drawCircle(
        Brush.radialGradient(listOf(buddy.accent.copy(alpha = 0.40f * g), Color.Transparent), center = Offset(size.width / 2, size.height * 0.55f), radius = size.width * 0.62f),
        radius = size.width * 0.62f,
        center = Offset(size.width / 2, size.height * 0.55f)
      )
      if (animations && playing) {
        val bars = 9
        val base = size.height - 6.dp.toPx()
        for (i in 0 until bars) {
          val h = (10 + 34 * (0.5f + 0.5f * sin(barPhase * 1.0f + i * 0.9f))).dp.toPx()
          val x = size.width / 2 - (bars - 1) * 7.dp.toPx() + i * 14.dp.toPx()
          drawLine(Brush.verticalGradient(listOf(buddy.accent2, buddy.accent), startY = base - h, endY = base), Offset(x, base), Offset(x, base - h), 5.dp.toPx(), StrokeCap.Round)
        }
      }
    }
    Image(
      painter = painterResource(buddy.body),
      contentDescription = buddy.displayName,
      contentScale = ContentScale.Fit,
      alpha = if (sleepy) 0.8f else 1f,
      modifier =
        Modifier.fillMaxSize().padding(bottom = 22.dp).graphicsLayer {
          if (animations) translationY = sin(phase) * 7.dp.toPx() * (if (playing) 1.4f else 1f)
        }
    )
  }
}


/** AI brain, personality, read-aloud and optional memory: everything the user can control about how the companion thinks. */
@Composable
private fun BuddyBrainSettings(buddy: BuddyCharacter) {
  val (hasKey, _) = rememberPreference(echo.music.iad1tya.constants.OpenRouterApiKey, defaultValue = "")
  val (tone, setTone) = rememberPreference(echo.music.iad1tya.constants.BuddyToneKey, defaultValue = 0)
  val (timeGreet, setTimeGreet) = rememberPreference(echo.music.iad1tya.constants.BuddyTimeGreetingsKey, defaultValue = true)
  val (tts, setTts) = rememberPreference(echo.music.iad1tya.constants.BuddyTtsKey, defaultValue = false)
  val (memOn, setMemOn) = rememberPreference(echo.music.iad1tya.constants.BuddyMemoryOnKey, defaultValue = false)
  val (memRaw, setMemRaw) = rememberPreference(echo.music.iad1tya.constants.BuddyMemoryKey, defaultValue = "")
  val items = memRaw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

  Spacer(Modifier.height(10.dp))
  Text("Brain & personality", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = buddy.accent2)
  Text(
    if (hasKey.isNotBlank()) "Answering with your own AI key (Settings → AI)."
    else "Answering with a free AI service that needs no key (best effort). For the most reliable answers add a free key in Settings → AI.",
    color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp),
  )
  Text("Personality", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("Friendly", "Playful", "Calm", "Concise").forEachIndexed { i, name ->
      Text(
        name, color = Color.White, fontSize = 13.sp, fontWeight = if (i == tone) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(50))
          .background(if (i == tone) buddy.accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f))
          .border(1.dp, buddy.accent.copy(alpha = if (i == tone) 0.9f else 0.35f), RoundedCornerShape(50))
          .clickable { setTone(i) }
          .padding(horizontal = 14.dp, vertical = 8.dp)
      )
    }
  }
  BuddySwitchRow("Good morning / good night", "Time-aware greetings (India time). Off = a simple hello", timeGreet, setTimeGreet, buddy)
  BuddySwitchRow("Read answers aloud", "Speaks replies with your phone's voice. The microphone is only used when you tap the mic button", tts, setTts, buddy)
  BuddySwitchRow("Remember things I ask", "Off by default. Only saves what you explicitly tell ${buddy.displayName} to remember", memOn, setMemOn, buddy)
  if (memOn) {
    if (items.isEmpty()) {
      Text("Nothing remembered yet. Say \"remember that …\" in the chat.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    } else {
      items.forEach { fact ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("• $fact", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
          Text(
            "Delete", color = Color(0xFFFF8C9A), fontSize = 12.sp,
            modifier = Modifier.clickable { setMemRaw(items.filter { it != fact }.joinToString("\n")) }.padding(8.dp)
          )
        }
      }
      Text(
        "Forget everything", color = Color(0xFFFF8C9A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable { setMemRaw("") }.padding(vertical = 8.dp)
      )
    }
  }
}
