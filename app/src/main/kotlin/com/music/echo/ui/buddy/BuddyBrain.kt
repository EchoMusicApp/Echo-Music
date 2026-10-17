package echo.music.iad1tya.ui.buddy

import android.content.Context
import android.net.Uri
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.constants.AiProviderKey
import echo.music.iad1tya.constants.OpenRouterApiKey
import echo.music.iad1tya.constants.OpenRouterBaseUrlKey
import echo.music.iad1tya.constants.OpenRouterModelKey
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.PlayerConnection
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.screens.settings.BuddyCharacter
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** What a Buddy turn produced: text to say, an optional music action, and a mood for the face. */
data class BuddyTurn(val reply: String, val action: String = "none", val query: String = "", val mood: String = "happy")

enum class BuddyMood { HAPPY, EXCITED, SLEEPY, THINKING, PLAYFUL, SURPRISED, WINK }

fun indiaNow(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))

fun moodOf(name: String): BuddyMood =
  when (name.lowercase()) {
    "excited" -> BuddyMood.EXCITED
    "sleepy" -> BuddyMood.SLEEPY
    "thinking" -> BuddyMood.THINKING
    "playful" -> BuddyMood.PLAYFUL
    "surprised" -> BuddyMood.SURPRISED
    "wink" -> BuddyMood.WINK
    else -> BuddyMood.HAPPY
  }

/** Time-of-day greeting in Indian Standard Time. */
fun greetingIst(buddy: BuddyCharacter, welcomeBack: Boolean): Pair<String, BuddyMood> {
  val h = indiaNow().hour
  val bro = if (buddy == BuddyCharacter.KAI) " bro" else ""
  if (welcomeBack) return "Glad you're back$bro! I missed you 💜" to BuddyMood.EXCITED
  return when (h) {
    in 5..11 -> "Good morning$bro! ☀️ Ready for some music?" to BuddyMood.HAPPY
    in 12..15 -> "Good afternoon$bro! 🎧 What shall we play?" to BuddyMood.WINK
    in 16..19 -> "Good evening$bro! 🌆 Time to unwind with a song." to BuddyMood.HAPPY
    in 20..22 -> "Hey$bro, nice night for music 🌙" to BuddyMood.PLAYFUL
    else -> "It's so late$bro… 😴 want something calm?" to BuddyMood.SLEEPY
  }
}

object BuddyBrain {
  private val client =
    OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

  /** True when the app already has an AI key configured (Settings → AI). */
  suspend fun aiConfigured(context: Context): Boolean =
    withContext(Dispatchers.IO) { context.dataStore.get(OpenRouterApiKey, "").isNotBlank() }

  /** A short plain-text description of what the listener likes, from their own history. */
  suspend fun tasteLine(database: MusicDatabase): String =
    withContext(Dispatchers.IO) {
      val songs = runCatching { database.topSongs(12).firstOrNull() }.getOrNull().orEmpty()
      if (songs.isEmpty()) "no listening history yet"
      else
        songs.take(8).joinToString("; ") {
          "${it.song.title} - ${it.artists.joinToString { a -> a.name }}"
        }
    }

  suspend fun respond(
    context: Context,
    buddy: BuddyCharacter,
    userText: String,
    history: List<Pair<Boolean, String>>,
    nowPlaying: String?,
    taste: String,
  ): BuddyTurn =
    withContext(Dispatchers.IO) {
      val key = context.dataStore.get(OpenRouterApiKey, "")
      if (key.isNotBlank()) {
        runCatching { askModel(context, key, buddy, userText, history, nowPlaying, taste) }
          .getOrNull()
          ?.let { return@withContext it }
      }
      localTurn(buddy, userText, key.isBlank())
    }

  private fun askModel(
    context: Context,
    key: String,
    buddy: BuddyCharacter,
    userText: String,
    history: List<Pair<Boolean, String>>,
    nowPlaying: String?,
    taste: String,
  ): BuddyTurn? {
    val baseUrl = context.dataStore.get(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions")
    val model = context.dataStore.get(OpenRouterModelKey, "openrouter/free")
    val now = indiaNow()
    val persona =
      if (buddy == BuddyCharacter.KAI) "Kai: a cool, playful, energetic boy companion who says 'bro' sometimes."
      else "Luna: a sweet, warm, cheerful girl companion."
    val system =
      """
      You are ${buddy.displayName}, the tiny cartoon companion living inside the Pattukal music app. Personality — $persona
      Talk like a friendly human friend: short (max 2 sentences), warm, a little funny, with 1-2 emojis. Match the user's language (English, Hinglish or Malayalam-English).
      Context: Indian Standard Time is ${now.toLocalTime().withSecond(0).withNano(0)} on ${now.dayOfWeek}. Now playing: ${nowPlaying ?: "nothing"}. The user's favourite songs: $taste.
      You can control the app. Reply with ONLY one JSON object, no markdown:
      {"reply":"what you say","action":"none|play|taste|pause|resume|next|previous|search|favorites","query":"song title + artist when action is play or search","mood":"happy|excited|thinking|sleepy|playful|surprised|wink"}
      Rules: use action "play" with a specific real song when the user names a song or asks for a mood/artist/movie (pick a real matching song). Use "taste" when they ask you to pick something for them, surprise them, or play something they like. Never invent features. Keep the reply consistent with the action you choose.
      """
        .trimIndent()
    val messages = JSONArray()
    messages.put(JSONObject().put("role", "system").put("content", system))
    history.takeLast(6).forEach { (fromUser, text) ->
      messages.put(JSONObject().put("role", if (fromUser) "user" else "assistant").put("content", text))
    }
    messages.put(JSONObject().put("role", "user").put("content", userText))
    val body = JSONObject().put("model", model).put("messages", messages).toString()
    val request =
      Request.Builder()
        .url(baseUrl)
        .addHeader("Authorization", "Bearer $key")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()
    client.newCall(request).execute().use { resp ->
      if (!resp.isSuccessful) return null
      val content =
        JSONObject(resp.body?.string() ?: return null)
          .optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
          ?: return null
      val start = content.indexOf('{')
      val end = content.lastIndexOf('}')
      if (start < 0 || end <= start) return BuddyTurn(content.trim().take(220))
      val j = JSONObject(content.substring(start, end + 1))
      val reply = j.optString("reply").trim()
      if (reply.isEmpty()) return null
      return BuddyTurn(reply, j.optString("action", "none").lowercase(), j.optString("query"), j.optString("mood", "happy"))
    }
  }

  /** Offline fallback: simple intent matching so Buddy still works without an AI key. */
  private fun localTurn(buddy: BuddyCharacter, raw: String, noKey: Boolean): BuddyTurn {
    val q = raw.trim()
    val l = q.lowercase()
    val vague = setOf("music", "song", "songs", "something", "anything", "a song", "some music", "some songs", "me something")
    return when {
      l.startsWith("search ") || l.startsWith("find ") ->
        BuddyTurn("Searching for \"${q.substringAfter(' ').trim()}\" 🔍", "search", q.substringAfter(' ').trim(), "thinking")
      l == "pause" || l == "stop" || l.startsWith("pause ") -> BuddyTurn("Paused ⏸️", "pause")
      l == "play" || l.contains("resume") || l.contains("continue") -> BuddyTurn("Playing ▶️", "resume", mood = "excited")
      l.startsWith("play ") && q.substringAfter(' ').trim().lowercase() !in vague &&
        !l.contains("my taste") && !l.contains("surprise") ->
        BuddyTurn("On it! Finding \"${q.substringAfter(' ').trim()}\" 🎶", "play", q.substringAfter(' ').trim(), "excited")
      l.startsWith("play ") || l.contains("suggest") || l.contains("recommend") || l.contains("surprise") ||
        l.contains("my taste") || l.contains("something for me") || l.contains("any song") ->
        BuddyTurn("Let me pick something you'll love… 🎧", "taste", mood = "excited")
      l.contains("next") || l.contains("skip") -> BuddyTurn("Next one! ⏭️", "next", mood = "playful")
      l.contains("previous") || l.contains("go back") || l.contains("last song") -> BuddyTurn("Going back ⏮️", "previous")
      l.contains("favourite") || l.contains("favorite") || l.contains("liked") -> BuddyTurn("Opening your favourites ❤️", "favorites", mood = "wink")
      l.contains("how are you") || l.contains("how r u") -> BuddyTurn("I'm great, and even better with you here! What shall we listen to? 😊", mood = "happy")
      l.contains("who are you") || l.contains("your name") ->
        BuddyTurn("I'm ${buddy.displayName}, your little music buddy living in Pattukal 💜", mood = "wink")
      l.contains("thank") -> BuddyTurn("Anytime! 😊", mood = "happy")
      l.startsWith("hi") || l.startsWith("hello") || l.startsWith("hey") || l.contains("good morning") || l.contains("good night") ->
        BuddyTurn(greetingIst(buddy, false).first, mood = "happy")
      else ->
        BuddyTurn(
          if (noKey) "I can play songs for you — try \"play <song name>\" or \"play something\". Add an AI key in Settings → AI and I can chat about anything too! ✨"
          else "Hmm, my brain lagged a bit 😅 try again or say \"play <song name>\".",
          mood = "thinking"
        )
    }
  }

  /** Performs the music action. Returns extra text to show (e.g. which song started), or null. */
  suspend fun execute(
    turn: BuddyTurn,
    player: PlayerConnection?,
    database: MusicDatabase,
    navController: NavController,
  ): String? =
    when (turn.action) {
      "play" -> {
        val song =
          withContext(Dispatchers.IO) {
            YouTube.search(turn.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.firstOrNull() as? SongItem
          }
        if (song == null || player == null) "I couldn't find that one 😕"
        else {
          withContext(Dispatchers.Main) { player.playQueue(YouTubeQueue.radio(song.toMediaMetadata())) }
          "Now playing ${song.title}${song.artists.firstOrNull()?.name?.let { " — $it" } ?: ""} 🎶"
        }
      }
      "taste" -> playFromTaste(player, database)
      "pause" -> { withContext(Dispatchers.Main) { player?.player?.pause() }; null }
      "resume" -> {
        withContext(Dispatchers.Main) {
          if (player?.mediaMetadata?.value != null) player.play() else navController.navigate("auto_playlist/liked")
        }
        null
      }
      "next" -> { withContext(Dispatchers.Main) { player?.seekToNext() }; null }
      "previous" -> { withContext(Dispatchers.Main) { player?.seekToPrevious() }; null }
      "search" -> { withContext(Dispatchers.Main) { navController.navigate("search/${Uri.encode(turn.query)}") }; null }
      "favorites" -> { withContext(Dispatchers.Main) { navController.navigate("auto_playlist/liked") }; null }
      else -> null
    }

  /** Starts a radio from one of the listener's own most-played songs. */
  suspend fun playFromTaste(player: PlayerConnection?, database: MusicDatabase): String {
    val songs = withContext(Dispatchers.IO) { runCatching { database.topSongs(15).firstOrNull() }.getOrNull().orEmpty() }
    val pick = songs.randomOrNull()
    if (pick == null || player == null) return "I don't know your taste yet — play a few songs and I'll learn! 🎵"
    withContext(Dispatchers.Main) { player.playQueue(YouTubeQueue.radio(pick.toMediaMetadata())) }
    return "Here's something in your style: ${pick.song.title} 🎶"
  }
}
