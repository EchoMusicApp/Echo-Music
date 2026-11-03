package echo.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.runBlocking

/** Stage-1 smoke test: search YouTube Music, then resolve a direct audio stream URL. */
fun main(args: Array<String>) = runBlocking {
    val query = args.joinToString(" ").ifBlank { "Daft Punk Get Lucky" }
    println("== Search: $query")

    val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrElse {
        println("SEARCH FAILED: ${it::class.simpleName}: ${it.message}")
        kotlin.system.exitProcess(1)
    }
    val songs = result.items.filterIsInstance<SongItem>()
    songs.take(5).forEachIndexed { i, s ->
        println("${i + 1}. ${s.title} - ${s.artists.joinToString { a -> a.name }} [${s.id}]")
    }
    val song = songs.firstOrNull() ?: run {
        println("NO SONGS FOUND")
        kotlin.system.exitProcess(2)
    }

    println("== Resolving stream for ${song.title} (${song.id})")
    val clients = listOf(
        "ANDROID_VR_1_43_32" to YouTubeClient.ANDROID_VR_1_43_32,
        "IOS" to YouTubeClient.IOS,
    )
    var ok = false
    for ((name, client) in clients) {
        val resp = YouTube.player(song.id, client = client).getOrElse {
            println("[$name] player call failed: ${it::class.simpleName}: ${it.message}")
            continue
        }
        val status = resp.playabilityStatus.status
        val audio = resp.streamingData?.adaptiveFormats?.filter { it.isAudio }.orEmpty()
        val direct = audio.filter { !it.url.isNullOrBlank() }.maxByOrNull { it.bitrate }
        println("[$name] status=$status audioFormats=${audio.size} directUrl=${direct != null}")
        if (direct != null) {
            println("[$name] best: ${direct.mimeType} @ ${direct.bitrate} bps")
            println("[$name] url: ${direct.url!!.take(90)}...")
            ok = true
            break
        }
    }
    println(if (ok) "RESULT: OK" else "RESULT: NO DIRECT STREAM URL")
    kotlin.system.exitProcess(if (ok) 0 else 3)
}
