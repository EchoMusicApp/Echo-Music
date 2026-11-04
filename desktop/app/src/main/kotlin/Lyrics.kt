package echo.desktop

import com.music.innertube.models.SongItem
import com.music.lrclib.LrcLib

/** One lyric line; [timeMs] is -1 when the lyrics are plain (not time-synced). */
data class LyricLine(val timeMs: Long, val text: String)

object Lyrics {
    private val stamp = Regex("""\[(\d+):(\d+)(?:\.(\d+))?]\s*(.*)""")

    suspend fun fetch(song: SongItem): Result<List<LyricLine>> = runCatching {
        val text = LrcLib.getLyrics(song.title, song.artists.firstOrNull()?.name.orEmpty(), song.duration ?: -1).getOrThrow()
        parse(text)
    }

    fun parse(text: String): List<LyricLine> {
        val synced =
            text.lines().mapNotNull { raw ->
                stamp.matchEntire(raw.trim())?.let { m ->
                    val (mm, ss, frac, line) = m.destructured
                    val ms = mm.toLong() * 60_000 + ss.toLong() * 1000 + (frac.padEnd(3, '0').take(3).toLongOrNull() ?: 0)
                    LyricLine(ms, line)
                }
            }
        return synced.ifEmpty { text.lines().map { LyricLine(-1, it) } }
    }
}
