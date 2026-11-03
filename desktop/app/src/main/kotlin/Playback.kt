package echo.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The play queue: which song is current, and how to load it into the player. */
class Playback(private val player: VlcPlayer, private val scope: CoroutineScope) {
    var queue by mutableStateOf<List<SongItem>>(emptyList())
        private set
    var index by mutableStateOf(-1)
        private set
    /** Stream details or the reason loading failed. */
    var info by mutableStateOf("")
        private set

    val current: SongItem?
        get() = queue.getOrNull(index)

    fun play(list: List<SongItem>, start: Int) {
        queue = list
        index = start
        load()
    }

    fun next() {
        if (index + 1 < queue.size) {
            index++
            load()
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
        info = "Loading stream..."
        scope.launch {
            StreamResolver.resolve(song.id)
                .onSuccess {
                    info = it.description
                    player.play(it)
                }
                .onFailure { info = it.message ?: "Could not load stream" }
        }
    }
}
