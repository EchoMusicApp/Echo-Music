package echo.music.iad1tya.playback

import echo.music.iad1tya.db.entities.LyricsEntity
import echo.music.iad1tya.lyrics.LyricsEntry
import echo.music.iad1tya.lyrics.LyricsUtils
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong
import org.json.JSONObject

/**
 * Builds the `lyricInfo` JSON that ColorOS / OxygenOS (OPlus) SystemUI reads from the player's
 * MediaSession metadata to show synced lyrics on the lock screen, the lock-screen media card and
 * the always-on display.
 *
 * Format (player-owned integration, no dependency on any other app):
 * https://github.com/Andrea-lyz/ColorOS-Live-Lyrics-Bridge/blob/4.0/docs/PLAYER_INTEGRATION.md
 *
 * Other systems ignore the unknown metadata key, so publishing it is harmless elsewhere.
 */
object OplusLyricInfo {
  const val METADATA_KEY = "lyricInfo"

  /** The metadata parcel also carries the artwork; stay well below the binder limit. */
  private const val MAX_PAYLOAD_CHARS = 100_000

  /**
   * Returns the JSON payload, or null when [lyrics] has no usable synced lines (not found, plain
   * text only, or empty).
   */
  fun build(
    packageName: String,
    songId: String,
    title: String,
    artist: String,
    durationSec: Int,
    lyrics: String?,
    generation: Long,
  ): String? {
    if (lyrics.isNullOrBlank() || lyrics == LyricsEntity.LYRICS_NOT_FOUND) return null
    val parsed = runCatching { LyricsUtils.parseLyrics(lyrics) }.getOrNull() ?: return null
    if (parsed.size > 1 && parsed.none { it.time > 0L }) return null
    val lines = parsed.filter { !it.isBackground && it.text.isNotBlank() }.sorted()
    if (lines.isEmpty()) return null

    val lyric = buildString {
      lines.forEach { append('[').append(tag(it.time)).append(']').append(it.text).append('\n') }
    }
    val rawLyric =
      if (lines.all { !it.words.isNullOrEmpty() }) {
        buildString { lines.forEach { append(rawLine(it)).append('\n') } }
      } else {
        null // line-timed source: no karaoke lane
      }
    val translationLyric =
      lines
        .mapNotNull { line ->
          line.translatedTextFlow.value
            ?.takeIf { it.isNotBlank() && it.trim() != line.text.trim() }
            ?.let { "[${tag(line.time)}]${it.trim()}\n" }
        }
        .takeIf { it.isNotEmpty() }
        ?.joinToString("")

    fun json(withRaw: Boolean, withTranslation: Boolean) =
      JSONObject()
        .put("songName", title)
        .put("artist", artist)
        .put("songId", songId)
        .put("lyricType", 0)
        .put("lyric", lyric)
        .apply { if (withRaw && rawLyric != null) put("rawLyric", rawLyric) }
        .apply {
          if (withTranslation && translationLyric != null) put("translationLyric", translationLyric)
        }
        .put("provider", packageName)
        .put("source", packageName)
        .put("trackKey", "$songId|$title|$artist|$durationSec")
        .put("sessionGeneration", generation)
        .put("noLyric", false)
        .toString()

    // Drop the optional lanes before dropping the lyric itself.
    return sequenceOf(json(true, true), json(false, true), json(false, false)).firstOrNull {
      it.length <= MAX_PAYLOAD_CHARS
    }
  }

  /** `[line]<w1>word <w2>word<end>` with absolute word times that never move backward. */
  private fun rawLine(line: LyricsEntry): String {
    val words = line.words.orEmpty()
    val text = line.text
    val sb = StringBuilder().append('[').append(tag(line.time)).append(']')
    var cursor = 0
    var last = 0L
    words.forEachIndexed { i, word ->
      val start = max(last, (word.startTime * 1000).roundToLong())
      last = start
      // Words are trimmed by the parser; take the original spacing back from the line text.
      val at = text.indexOf(word.text, cursor)
      val piece =
        if (at >= 0) {
          var end = at + word.text.length
          while (end < text.length && text[end].isWhitespace()) end++
          cursor = end
          text.substring(at, end)
        } else {
          if (i < words.lastIndex) word.text + " " else word.text
        }
      sb.append('<').append(tag(start)).append('>').append(piece)
    }
    val end = max(last, (words.last().endTime * 1000).roundToLong())
    return sb.append('<').append(tag(end)).append('>').toString().trimEnd()
  }

  private fun tag(ms: Long): String {
    val t = max(0L, ms)
    return String.format(Locale.US, "%02d:%02d.%03d", t / 60_000, (t / 1000) % 60, t % 1000)
  }
}

/**
 * The `lyricInfo` payload for the current song, shared by every [SessionArtworkPlayer] the service
 * creates (the session player is replaced on crossfade).
 */
class SessionLyricInfo {
  class Payload(val mediaId: String, val json: String, val stamp: String)

  @Volatile private var payload: Payload? = null
  private var publishCount = 0L
  private var trackId: String? = null
  private var trackGeneration = 0L

  fun current(mediaId: String?): Payload? = payload?.takeIf { it.mediaId == mediaId }

  /** Monotonic per real track change, as the format asks for `sessionGeneration`. */
  @Synchronized
  fun generationFor(mediaId: String): Long {
    if (mediaId != trackId) {
      trackId = mediaId
      trackGeneration++
    }
    return trackGeneration
  }

  /** Returns true when the session has to republish its metadata. */
  @Synchronized
  fun set(mediaId: String, json: String?): Boolean {
    val old = payload
    if (json == null) {
      if (old == null) return false
      payload = null
      return true
    }
    if (old != null && old.mediaId == mediaId && old.json == json) return false
    publishCount++
    // Two alternating invisible values: see SessionArtworkPlayer.withLyricInfo.
    payload = Payload(mediaId, json, if (publishCount % 2 == 0L) "⁠⁠" else "⁠")
    return true
  }
}
