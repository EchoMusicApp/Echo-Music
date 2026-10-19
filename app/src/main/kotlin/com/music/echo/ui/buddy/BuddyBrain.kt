package echo.music.iad1tya.ui.buddy

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
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
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** What a Buddy turn produced: text to say, an optional music action, and a mood for the face. */
data class BuddyTurn(
  val reply: String,
  val action: String = "none",
  val query: String = "",
  val mood: String = "happy",
  /** Set when the AI service could not be reached or rejected the request. */
  val error: String? = null,
)

enum class BuddyMood { HAPPY, EXCITED, SLEEPY, THINKING, PLAYFUL, SURPRISED, WINK, SHY }

fun indiaNow(): ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))

fun moodOf(name: String): BuddyMood =
  when (name.lowercase()) {
    "excited" -> BuddyMood.EXCITED
    "sleepy" -> BuddyMood.SLEEPY
    "thinking" -> BuddyMood.THINKING
    "playful" -> BuddyMood.PLAYFUL
    "surprised" -> BuddyMood.SURPRISED
    "wink" -> BuddyMood.WINK
    "shy" -> BuddyMood.SHY
    else -> BuddyMood.HAPPY
  }

/** Time-of-day greeting in Indian Standard Time. */
fun greetingIst(buddy: BuddyCharacter, welcomeBack: Boolean, timeAware: Boolean = true): Pair<String, BuddyMood> {
  val h = indiaNow().hour
  val bro = if (buddy == BuddyCharacter.KAI) " bro" else ""
  if (welcomeBack) return "Glad you're back$bro! 💜" to BuddyMood.EXCITED
  if (!timeAware) return "Hey$bro! 🎧 What shall we listen to?" to BuddyMood.HAPPY
  return when (h) {
    in 5..11 -> "Good morning$bro! ☀️ Ready for some music?" to BuddyMood.HAPPY
    in 12..15 -> "Good afternoon$bro! 🎧 What shall we play?" to BuddyMood.WINK
    in 16..19 -> "Good evening$bro! 🌆 Time to unwind with a song." to BuddyMood.HAPPY
    in 20..22 -> "Hey$bro, nice night for music 🌙" to BuddyMood.PLAYFUL
    else -> "It's so late$bro… 😴 want something calm?" to BuddyMood.SLEEPY
  }
}

/** Everything the AI is told about the current moment. Only what the app really knows. */
class BuddyContextInfo(
  val nowPlaying: String? = null,
  val playing: Boolean = false,
  val upNext: String? = null,
  val lyrics: String? = null,
  val taste: String = "unknown",
  val memory: List<String> = emptyList(),
  val tone: Int = 0,
)

private class AiProvider(val url: String, val model: String, val key: String?, val label: String)

private class AiFailure(val userMessage: String, val tryNext: Boolean) : Exception(userMessage)

/** Optional long-term memory: only things the user explicitly asked Buddy to remember, fully visible in settings. */
object BuddyMemory {
  private val ask = Regex("^(?:please\\s+)?(?:remember|don'?t forget|note)(?:\\s+that)?[:,]?\\s+(.{3,200})$", RegexOption.IGNORE_CASE)
  private val forget = Regex("^(?:please\\s+)?(?:forget everything|clear (?:my )?memory|forget (?:it )?all)\\W*$", RegexOption.IGNORE_CASE)

  suspend fun enabled(context: Context): Boolean = context.dataStore.get(echo.music.iad1tya.constants.BuddyMemoryOnKey, false)

  suspend fun items(context: Context): List<String> =
    context.dataStore.get(echo.music.iad1tya.constants.BuddyMemoryKey, "").split('\n').map { it.trim() }.filter { it.isNotEmpty() }

  suspend fun save(context: Context, list: List<String>) {
    context.dataStore.edit { it[echo.music.iad1tya.constants.BuddyMemoryKey] = list.take(40).joinToString("\n") }
  }

  /** Returns a confirmation if the message was a memory command (handled locally, never sent to the AI), else null. */
  suspend fun handle(context: Context, text: String): String? {
    val t = text.trim()
    val wantsForget = forget.matches(t)
    val m = ask.find(t)
    if (!wantsForget && m == null) return null
    if (!enabled(context))
      return "Memory is off right now. You can turn it on in Settings → AI Companion, and I'll only remember what you tell me to."
    if (wantsForget) {
      save(context, emptyList())
      return "Done — I've forgotten everything you asked me to remember."
    }
    val fact = m!!.groupValues[1].trim().trimEnd('.')
    save(context, (items(context) + fact).distinct())
    return "Okay, I'll remember: \"$fact\". You can see or delete it any time in Settings → AI Companion."
  }
}

object BuddyBrain {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .callTimeout(120, TimeUnit.SECONDS)
      .build()

  /** True when the user added their own AI key (Settings → AI). Otherwise the free keyless service is tried. */
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

  private suspend fun providers(context: Context): List<AiProvider> {
    val key = context.dataStore.get(OpenRouterApiKey, "")
    return if (key.isNotBlank()) {
      listOf(
        AiProvider(
          context.dataStore.get(OpenRouterBaseUrlKey, "https://openrouter.ai/api/v1/chat/completions"),
          context.dataStore.get(OpenRouterModelKey, "openrouter/free"),
          key,
          "your AI key",
        )
      )
    } else {
      // Best-effort free service that needs no key. Availability is outside the app's control.
      listOf(
        AiProvider("https://text.pollinations.ai/openai", "openai", null, "free AI"),
        AiProvider("https://text.pollinations.ai/openai", "mistral", null, "free AI"),
      )
    }
  }

  private fun toneLine(tone: Int) =
    when (tone) {
      1 -> "Playful, witty and energetic."
      2 -> "Calm, gentle and soothing."
      3 -> "Concise and to the point: shortest helpful answer."
      else -> "Warm, friendly and natural."
    }

  private fun systemPrompt(buddy: BuddyCharacter, c: BuddyContextInfo): String {
    val now = indiaNow()
    val persona =
      if (buddy == BuddyCharacter.KAI) "Kai, a cool, upbeat boy companion who says 'bro' occasionally."
      else "Luna, a sweet, warm, cheerful girl companion."
    val mem = if (c.memory.isEmpty()) "nothing" else c.memory.joinToString("; ")
    return """
You are ${buddy.displayName}, the AI companion who lives inside the Pattukal music app. You are $persona Tone: ${toneLine(c.tone)}
You are also a capable general-purpose assistant: answer questions about science, technology, programming, education, creativity and everyday life accurately and helpfully, like a knowledgeable friend. Understand spelling mistakes and casual phrasing, and reply in the user's language (English, Hinglish, Malayalam, Tamil, Hindi...). Explain complex topics simply when asked. If you are unsure or don't know, say so plainly; never invent facts, links, quotes or personal memories. You only know about the user what is listed under Context or what they tell you in this chat.
Style: conversational plain text. Short by default (1-4 sentences); give fuller step-by-step or code answers when the question needs it. No markdown headings. Emoji only when it fits.
Controlling music: when (and only when) the user wants playback or navigation, end your message with ONE tag on the final line:
[[ACTION {"action":"...","query":"...","mood":"..."}]]
actions: play (query = a real song title and artist matching the request), taste (play from the user's favourites), similar (radio based on the current song), pause, resume, next, previous, search (query), favorites. mood: happy|excited|thinking|sleepy|playful|surprised|wink|shy.
Examples: "play something relaxing" -> short friendly line + [[ACTION {"action":"play","query":"Weightless Marconi Union","mood":"happy"}]]. "pause the music and tell me something interesting" -> a genuinely true fun fact + [[ACTION {"action":"pause"}]]. "find songs like this" -> [[ACTION {"action":"similar"}]]. "what does this song mean?" -> explain from the title, artist and lyrics excerpt if provided, saying so if unsure; no action. If the user only says a song, movie or artist name, play it. Never mention or explain the tag.
Context: India time ${now.toLocalTime().withSecond(0).withNano(0)} (${now.dayOfWeek}). Now playing: ${c.nowPlaying ?: "nothing"}${if (c.nowPlaying != null) if (c.playing) " (playing)" else " (paused)" else ""}. Up next: ${c.upNext ?: "unknown"}. Lyrics excerpt: ${c.lyrics ?: "not available"}. The user's most played songs: ${c.taste}. Things the user asked you to remember: $mem.
"""
      .trim()
  }

  /** Text safe to show while streaming: hides the control tag, even a half-typed one. */
  private fun visible(full: String): String {
    val cut = full.indexOf("[[")
    var v = if (cut >= 0) full.substring(0, cut) else full
    if (cut < 0 && v.endsWith("[")) v = v.dropLast(1)
    return v.trimEnd()
  }

  private val actionTag = Regex("\\[\\[\\s*ACTION\\s*(\\{.*?\\})\\s*\\]\\]", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

  private fun parseTurn(full: String): BuddyTurn {
    val text = visible(full)
    val m = actionTag.find(full)
    if (m != null) {
      runCatching {
        val j = JSONObject(m.groupValues[1])
        return BuddyTurn(
          text.ifBlank { "On it! 🎶" },
          j.optString("action", "none").lowercase(),
          j.optString("query"),
          j.optString("mood", "happy"),
        )
      }
    }
    return BuddyTurn(text.ifBlank { "…" })
  }

  /** One streaming chat-completions call. Throws [AiFailure] with a friendly message. */
  private fun streamOnce(
    p: AiProvider,
    system: String,
    history: List<Pair<Boolean, String>>,
    userText: String,
    onText: (String) -> Unit,
  ): String {
    val messages = JSONArray()
    messages.put(JSONObject().put("role", "system").put("content", system))
    history.takeLast(14).forEach { (fromUser, text) ->
      if (text.isNotBlank()) messages.put(JSONObject().put("role", if (fromUser) "user" else "assistant").put("content", text))
    }
    messages.put(JSONObject().put("role", "user").put("content", userText))
    val body =
      JSONObject().put("model", p.model).put("messages", messages).put("stream", true).put("temperature", 0.7).put("max_tokens", 700).toString()
    val req =
      Request.Builder()
        .url(p.url)
        .apply { if (p.key != null) addHeader("Authorization", "Bearer ${p.key}") }
        .addHeader("Accept", "text/event-stream, application/json")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()
    try {
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) {
          val msg =
            when (resp.code) {
              401, 403 -> "The AI service rejected the key. Check it in Settings → AI."
              402 -> "The AI service says this account is out of credit."
              429 -> "The AI is busy right now. Try again in a moment."
              in 500..599 -> "The AI service is having a hiccup. Try again shortly."
              else -> "The AI service returned an error (${resp.code})."
            }
          throw AiFailure(msg, tryNext = resp.code != 401 && resp.code != 403)
        }
        val source = resp.body?.source() ?: throw AiFailure("The AI sent an empty answer.", true)
        val full = StringBuilder()
        val contentType = resp.header("Content-Type").orEmpty()
        if (contentType.contains("event-stream")) {
          while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            val delta =
              runCatching {
                  JSONObject(data).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content", "")
                }
                .getOrNull()
                .orEmpty()
            if (delta.isNotEmpty() && delta != "null") {
              full.append(delta)
              onText(visible(full.toString()))
            }
          }
        } else {
          val raw = source.readUtf8()
          val content =
            runCatching { JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content") }
              .getOrNull() ?: raw
          full.append(content)
          onText(visible(full.toString()))
        }
        if (full.isBlank()) throw AiFailure("The AI sent an empty answer.", true)
        return full.toString()
      }
    } catch (e: AiFailure) {
      throw e
    } catch (e: java.net.SocketTimeoutException) {
      throw AiFailure("The AI took too long to answer. Check your connection and try again.", true)
    } catch (e: IOException) {
      throw AiFailure("I can't reach the internet right now.", false)
    }
  }

  /**
   * One conversation turn: streams the answer through [onText] (only the human-readable part), then returns
   * the final reply with any music action. Never throws; failures come back as a turn with [BuddyTurn.error].
   */
  suspend fun converse(
    context: Context,
    buddy: BuddyCharacter,
    userText: String,
    history: List<Pair<Boolean, String>>,
    info: BuddyContextInfo,
    onText: (String) -> Unit = {},
  ): BuddyTurn =
    withContext(Dispatchers.IO) {
      val text = userText.trim()
      if (text.isEmpty()) return@withContext BuddyTurn("Say something and I'll answer 😊")
      BuddyMemory.handle(context, text)?.let { return@withContext BuddyTurn(it, mood = "happy") }
      // plain playback commands never need the network
      simpleCommand(text)?.let { return@withContext it }
      val system = systemPrompt(buddy, info)
      var failure: String? = null
      for (p in providers(context)) {
        try {
          val full = streamOnce(p, system, history, text, onText)
          return@withContext parseTurn(full)
        } catch (e: AiFailure) {
          failure = e.userMessage
          if (!e.tryNext) break
        }
      }
      // AI unreachable: still honour what can be done offline
      val offline = localTurn(buddy, text)
      if (offline != null) return@withContext offline.copy(error = null)
      val hint =
        if (context.dataStore.get(OpenRouterApiKey, "").isBlank())
          " For reliable answers add a free AI key in Settings → AI (Google Gemini, Groq or OpenRouter all have free tiers)."
        else ""
      BuddyTurn("${failure ?: "I couldn't get an answer."}$hint", mood = "thinking", error = failure ?: "offline")
    }

  /** Kept for callers that just want a finished turn (voice search). */
  suspend fun respond(
    context: Context,
    buddy: BuddyCharacter,
    userText: String,
    history: List<Pair<Boolean, String>>,
    nowPlaying: String?,
    taste: String,
  ): BuddyTurn = converse(context, buddy, userText, history, BuddyContextInfo(nowPlaying = nowPlaying, taste = taste))

  /** Exact playback commands that need no AI. */
  private fun simpleCommand(raw: String): BuddyTurn? {
    val l = raw.trim().lowercase().trimEnd('.', '!', '?')
    return when (l) {
      "pause", "stop", "pause music", "pause the music" -> BuddyTurn("Paused ⏸️", "pause")
      "play", "resume", "continue", "resume music" -> BuddyTurn("Playing ▶️", "resume", mood = "excited")
      "next", "skip", "next song", "skip song" -> BuddyTurn("Next one! ⏭️", "next", mood = "playful")
      "previous", "go back", "previous song", "last song" -> BuddyTurn("Going back ⏮️", "previous")
      "favourites", "favorites", "my favourites", "my favorites", "liked songs" -> BuddyTurn("Opening your favourites ❤️", "favorites", mood = "wink")
      else -> null
    }
  }

  /** Offline fallback for when the AI can't be reached: a few obvious intents. */
  private fun localTurn(buddy: BuddyCharacter, raw: String): BuddyTurn? {
    val q = raw.trim()
    val l = q.lowercase()
    return when {
      l.startsWith("search ") || l.startsWith("find ") ->
        BuddyTurn("Searching for \"${q.substringAfter(' ').trim()}\" 🔍", "search", q.substringAfter(' ').trim(), "thinking")
      l.startsWith("play ") && q.substringAfter(' ').trim().lowercase() !in setOf("music", "song", "songs", "something", "anything") &&
        !l.contains("my taste") && !l.contains("surprise") && !l.contains("something") ->
        BuddyTurn("On it! Finding \"${q.substringAfter(' ').trim()}\" 🎶", "play", q.substringAfter(' ').trim(), "excited")
      l.startsWith("play ") || l.contains("suggest") || l.contains("recommend") || l.contains("surprise") || l.contains("my taste") ->
        BuddyTurn("Let me pick something you'll love… 🎧", "taste", mood = "excited")
      l.contains("next") || l.contains("skip") -> BuddyTurn("Next one! ⏭️", "next", mood = "playful")
      l.contains("who are you") || l.contains("your name") ->
        BuddyTurn("I'm ${buddy.displayName}, your music buddy living in Pattukal 💜", mood = "wink")
      l.startsWith("hi") || l.startsWith("hello") || l.startsWith("hey") -> BuddyTurn(greetingIst(buddy, false).first, mood = "happy")
      else -> null
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
      "similar" -> {
        val md = player?.mediaMetadata?.value
        if (player == null || md == null) "Play a song first and I'll find similar ones 🎵"
        else {
          withContext(Dispatchers.Main) { player.playQueue(YouTubeQueue.radio(md)) }
          "Finding songs similar to ${md.title} 🎶"
        }
      }
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
