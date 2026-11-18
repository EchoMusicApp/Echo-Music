package com.music.echo.spotify

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.spotify.models.SpotifyTrack
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyPlaybackResolver @Inject constructor() {
    private val trackCache = object : LinkedHashMap<String, SongItem>(1000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongItem>?): Boolean = size > 1000
    }
    private val resolutionSemaphore = Semaphore(3)

    fun clearCache() {
        synchronized(trackCache) {
            trackCache.clear()
        }
    }

    suspend fun resolveTrack(
        track: SpotifyTrack,
        hideExplicit: Boolean = false
    ): SongItem? = try {
        resolutionSemaphore.withPermit {
            if (track.id.isNotBlank()) {
                val cachedSong = synchronized(trackCache) { trackCache[track.id] }
                if (cachedSong != null) {
                    return@withPermit cachedSong
                }
            }

            val resolved = findBestMatch(track, hideExplicit)
            if (resolved != null && track.id.isNotBlank()) {
                synchronized(trackCache) { trackCache[track.id] = resolved }
            }
            resolved
        }
    } catch (e: Exception) {
        Timber.tag("SpotifyResolver").w(e, "Error resolving track: ${track.name}")
        null
    }

    private suspend fun findBestMatch(
        track: SpotifyTrack,
        hideExplicit: Boolean
    ): SongItem? {
        val artistNames = track.artists.joinToString(" ") { it.name }.trim()
        val firstArtist = track.artists.firstOrNull()?.name.orEmpty().trim()

        // Search queries in order of preference:
        // 1. "Artist Title" (e.g. "Sachin-Jigar Saibo")
        // 2. "Title Artist" (e.g. "Saibo Sachin-Jigar")
        // 3. "Title" (fallback)
        val queries = buildList {
            val q1 = SpotifyMapper.buildSearchQuery(track).trim()
            if (q1.isNotBlank()) add(q1)
            val q2 = "${track.name} $firstArtist".trim()
            if (q2.isNotBlank() && q2 != q1) add(q2)
            if (track.name.isNotBlank() && track.name != q1 && track.name != q2) {
                add(track.name.trim())
            }
        }.distinct()

        var bestSong: SongItem? = null
        var bestScore = 0.0

        for (query in queries) {
            // First try anonymous search (setLogin = false) so personal watch history/trending does not hijack the result
            var searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG, setLogin = false).getOrNull()
            var candidates = searchResult?.items?.filterIsInstance<SongItem>()?.filter {
                !hideExplicit || !it.explicit
            }.orEmpty()

            // If anonymous search returned nothing, fallback to default search
            if (candidates.isEmpty()) {
                searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG, setLogin = null).getOrNull()
                candidates = searchResult?.items?.filterIsInstance<SongItem>()?.filter {
                    !hideExplicit || !it.explicit
                }.orEmpty()
            }

            for (candidate in candidates) {
                val titleSim = SpotifyMapper.titleSimilarity(track.name, candidate.title)
                // Filter out candidates whose titles do not resemble the track name (e.g. "Saibo" vs "Sajna")
                if (titleSim < 0.25) continue

                val score = SpotifyMapper.matchScore(
                    spotifyTitle = track.name,
                    spotifyArtist = artistNames,
                    spotifyDurationMs = track.durationMs,
                    candidateTitle = candidate.title,
                    candidateArtist = candidate.artists.joinToString(" ") { it.name },
                    candidateDurationSec = candidate.duration
                )

                if (score > bestScore) {
                    bestScore = score
                    bestSong = candidate
                }

                // If high confidence match found (>= 0.85), return immediately
                if (bestScore >= 0.85) {
                    return bestSong
                }
            }

            // If good match found (>= 0.55), don't query further
            if (bestScore >= 0.55) {
                return bestSong
            }
        }

        // Return best match only if confidence score is acceptable (>= 0.35)
        return if (bestScore >= 0.35) bestSong else null
    }

    fun getCached(trackId: String): SongItem? = synchronized(trackCache) { trackCache[trackId] }

    suspend fun resolveAndFilter(
        tracks: List<SpotifyTrack>,
        hideExplicit: Boolean
    ): List<SongItem> = coroutineScope {
        tracks
            .filter { !hideExplicit || !it.explicit }
            .map { track ->
                async {
                    resolveTrack(track, hideExplicit)
                }
            }.awaitAll().filterNotNull()
    }
}
