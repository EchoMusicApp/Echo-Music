package echo.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Repeat { Off, All, One }

/** The play queue: which song is current, how to load it, plus shuffle, repeat and likes. */
class Playback(private val player: VlcPlayer, private val scope: CoroutineScope) {
    var queue by mutableStateOf<List<SongItem>>(emptyList())
        private set
    var index by mutableStateOf(-1)
        private set
    /** Stream details or the reason loading failed. */
    var info by mutableStateOf("")
        private set
    /** Songs played this session, newest first (shown in the left strip). */
    var recent by mutableStateOf<List<SongItem>>(emptyList())
        private set
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(Repeat.Off)
    var liked by mutableStateOf(LikedFile.load())
        private set

    /** Every play, newest first (kept to the last 3000). */
    var history by mutableStateOf(HistoryFile.load())
        private set

    var plays by mutableStateOf(PlayLog.load())
        private set

    val current: SongItem?
        get() = queue.getOrNull(index)

    fun isLiked(song: SongItem) = liked.any { it.id == song.id }

    fun toggleLike(song: SongItem) {
        liked = if (isLiked(song)) liked.filter { it.id != song.id } else listOf(song) + liked
        LikedFile.save(liked)
    }

    /** Merges songs (e.g. synced from a YouTube account) into the liked list. */
    fun like(songs: List<SongItem>) {
        liked = (songs + liked).distinctBy { it.id }
        LikedFile.save(liked)
    }

    fun play(list: List<SongItem>, start: Int) {
        queue = list
        index = start
        load()
    }

    /** [auto] is true when the previous song just ended on its own. */
    fun next(auto: Boolean = false) {
        when {
            auto && repeat == Repeat.One -> load()
            shuffle && queue.size > 1 -> {
                index = queue.indices.filter { it != index }.random()
                load()
            }
            index + 1 < queue.size -> {
                index++
                load()
            }
            repeat == Repeat.All && queue.isNotEmpty() -> {
                index = 0
                load()
            }
        }
    }

    fun previous() {
        if (index > 0) {
            index--
            load()
        }
    }

    private fun load() {
        val song = current ?: return
        recent = (listOf(song) + recent.filter { it.id != song.id }).take(12)
        info = "Loading stream..."
        scope.launch {
            val stream =
                StreamResolver.resolve(song.id).getOrElse {
                    info = it.message ?: "Could not load stream"
                    return@launch
                }
            info = "Downloading audio..."
            val file =
                runCatching { AudioCache.fetch(song.id, stream) }.getOrElse {
                    info = "Download failed: ${it.message ?: it::class.simpleName}"
                    return@launch
                }
            if (current?.id != song.id) return@launch // user picked another song meanwhile
            info = stream.description
            player.playFile(file)
            history = (listOf(song) + history).take(3000)
            val play = Play(System.currentTimeMillis(), song.duration ?: 0)
            plays = plays + play
            withContext(Dispatchers.IO) {
                HistoryFile.save(history)
                PlayLog.add(play)
                Discord.show(song)
            }
        }
    }
}
