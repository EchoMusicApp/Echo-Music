package echo.music.iad1tya.utils

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Desktop replacement for the Android stream resolver (which used a YouTube client library not available on the JVM).
 * It asks the yt-dlp program for the audio stream URL. yt-dlp is looked up in ECHO_YTDLP, then next to the app
 * (yt-dlp.exe / yt-dlp), then on PATH. If it is missing, extract() returns null and playback has no stream.
 * shortcut: one yt-dlp process per song (about 1-3 s), no caching beyond the URL map; no PoToken.
 */
object InnerTubeXResolver {
  class Extracted(
    val videoId: String,
    val url: String,
    val kbps: Int,
    val mimeType: String,
    val loudnessDb: Double?,
    val perceptualLoudnessDb: Double? = null,
    val clientName: String,
    val profileId: String,
    val headers: Map<String, String>,
    val itag: Int = 0,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val contentLength: Long? = null,
  )

  private val minted = ConcurrentHashMap<String, Extracted>()
  private val excluded = ConcurrentHashMap.newKeySet<String>()

  fun init(context: Context) {}
  fun headersFor(url: String): Map<String, String>? = minted[url]?.headers
  fun onRefused(url: String): String? = minted[url]?.profileId
  fun exclude(videoId: String, profileId: String) { excluded += "$videoId/$profileId" }
  fun onSessionChanged() { minted.clear(); excluded.clear(); cache.clear() }

  private fun ytDlp(): String {
    System.getenv("ECHO_YTDLP")?.takeIf { it.isNotBlank() }?.let { return it }
    echo.music.iad1tya.desktop.Bundled.dir?.let { d -> File(d, "tools/yt-dlp.exe").takeIf { it.exists() }?.let { return it.absolutePath } }
    for (n in listOf("yt-dlp.exe", "yt-dlp")) File(n).takeIf { it.exists() }?.let { return it.absolutePath }
    return "yt-dlp"
  }

  private val cache = ConcurrentHashMap<String, Pair<Long, Extracted>>()

  /** Stream lookups are remembered for 20 minutes, so a song looked up ahead of time (prefetch) starts at once. */
  suspend fun extract(videoId: String, maxKbps: Int, skipClients: Set<String> = emptySet(), requireM4a: Boolean = false): Extracted? {
    val key = "$videoId/$requireM4a"
    if (skipClients.isEmpty()) cache[key]?.takeIf { System.currentTimeMillis() - it.first < 20 * 60_000 }?.let { return it.second }
    val r = fetch(videoId, maxKbps, skipClients, requireM4a)
    if (r != null && skipClients.isEmpty()) cache[key] = System.currentTimeMillis() to r
    return r
  }

  private suspend fun fetch(videoId: String, maxKbps: Int, skipClients: Set<String>, requireM4a: Boolean): Extracted? =
    withContext(Dispatchers.IO) {
      val fmt = if (requireM4a) "bestaudio[ext=m4a]/bestaudio" else "bestaudio"
      val p = ProcessBuilder(ytDlp(), "-f", fmt, "-j", "--no-playlist", "--no-warnings", "https://music.youtube.com/watch?v=$videoId")
        .redirectErrorStream(false).start()
      val out = p.inputStream.bufferedReader().readText()
      if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) return@withContext null
      val o = Json.parseToJsonElement(out.trim().lines().last()).jsonObject
      fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull
      val url = s("url") ?: return@withContext null
      val abr = o["abr"]?.jsonPrimitive?.doubleOrNull ?: 0.0
      val ext = s("ext") ?: "webm"
      val codec = s("acodec")
      val headers = (o["http_headers"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
      val e = Extracted(
        videoId = videoId, url = url, kbps = abr.toInt(),
        mimeType = (if (ext == "m4a") "audio/mp4" else "audio/webm") + (codec?.let { "; codecs=\"$it\"" } ?: ""),
        loudnessDb = null, clientName = "yt-dlp", profileId = "yt-dlp", headers = headers,
        itag = s("format_id")?.toIntOrNull() ?: 0, bitrate = (abr * 1000).toInt(),
        sampleRate = o["asr"]?.jsonPrimitive?.intOrNull, contentLength = o["filesize"]?.jsonPrimitive?.longOrNull,
      )
      minted[url] = e
      e
    }
}
