package com.music.echo.spotify

import com.music.innertube.YouTube
import echo.music.iad1tya.spotify.models.SpotifyTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton
import com.music.innertube.models.SongItem

@Singleton
class SpotifyPlaybackResolver @Inject constructor() {
    private val trackCache = object : LinkedHashMap<String, SongItem>(1000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongItem>?): Boolean = size > 1000
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
                            val cachedSong = synchronized(trackCache) { trackCache[track.id] }
                            if (cachedSong != null) {
                                return@withPermit cachedSong
                            }

                            val query = "${track.name} ${track.artists.firstOrNull()?.name.orEmpty()}"
                            val searchResult = YouTube.search(query, com.music.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            val firstSong = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull {
                                !hideExplicit || !it.explicit
                            }

                            if (firstSong != null) {
                                synchronized(trackCache) { trackCache[track.id] = firstSong }
                            }
                            firstSong
                        }
                    } catch (e: Exception) { null }
                }
            }.awaitAll().filterNotNull()
    }
}
