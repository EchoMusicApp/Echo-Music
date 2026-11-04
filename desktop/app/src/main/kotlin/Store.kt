package echo.desktop

import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import java.io.File

val AppDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "EchoMusic")

/** Songs saved as one tab-separated line each in %APPDATA%\EchoMusic\<name>. */
class SongFile(name: String) {
    private val file = File(AppDir, name)
    private val ARTIST_SEP = " ·· "

    fun load(): List<SongItem> =
        runCatching {
                file.readLines().mapNotNull { line ->
                    val p = line.split('\t')
                    if (p.size < 5) null
                    else
                        SongItem(
                            id = p[0],
                            title = p[1],
                            artists = p[2].split(ARTIST_SEP).filter { it.isNotBlank() }.map { Artist(it, null) },
                            duration = p[3].toIntOrNull(),
                            thumbnail = p[4],
                        )
                }
            }
            .getOrDefault(emptyList())

    fun save(songs: List<SongItem>) {
        runCatching {
            file.parentFile.mkdirs()
            file.writeText(
                songs.joinToString("\n") { s ->
                    listOf(
                            s.id,
                            s.title.replace('\t', ' '),
                            s.artists.joinToString(ARTIST_SEP) { it.name.replace('\t', ' ') },
                            s.duration?.toString() ?: "",
                            s.thumbnail,
                        )
                        .joinToString("\t")
                }
            )
        }
    }
}

val LikedFile = SongFile("liked.tsv")
val HistoryFile = SongFile("history.tsv")
