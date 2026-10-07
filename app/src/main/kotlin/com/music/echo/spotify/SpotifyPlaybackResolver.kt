package com.music.echo.spotify

import com.music.innertube.YouTube
import echo.music.iad1tya.spotify.models.SpotifyTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton
import com.music.innertube.models.SongItem

@Singleton
class SpotifyPlaybackResolver @Inject constructor() {
    private val trackIdCache = object : LinkedHashMap<String, String>(1000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 1000
    }
    private val resolutionSemaphore = Semaphore(3)

    suspend fun resolveAndFilter(
        tracks: List<SpotifyTrack>,
        hideExplicit: Boolean
    ): List<SongItem> = coroutineScope {
        tracks
            .filter { !hideExplicit || !it.explicit }
            .map { track ->
                async {
                    try {
                        resolutionSemaphore.withPermit {
                            val cachedId = synchronized(trackIdCache) { trackIdCache[track.id] }
                            if (cachedId != null) {
                                val songInfo = YouTube.song(cachedId).getOrNull()
                                return@withPermit songInfo
                            }

                            val query = "${track.name} ${track.artists.firstOrNull()?.name.orEmpty()}"
                            val searchResult = YouTube.search(query, "songs").getOrNull()
                            val firstSong = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull {
                                !hideExplicit || !it.explicit
                            }

                            if (firstSong != null) {
                                synchronized(trackIdCache) { trackIdCache[track.id] = firstSong.id }
                            }
                            firstSong
                        }
                    } catch (e: Exception) { null }
                }
            }.awaitAll().filterNotNull()
    }
}
