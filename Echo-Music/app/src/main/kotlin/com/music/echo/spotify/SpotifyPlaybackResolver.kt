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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
open class SpotifyPlaybackResolver @Inject constructor() {
    private val trackCache = object : LinkedHashMap<String, SongItem>(1000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongItem>?): Boolean = size > 1000
    }
    private val resolutionSemaphore = Semaphore(6)

    fun clearCache() {
        synchronized(trackCache) {
            trackCache.clear()
        }
    }

    open suspend fun resolveTrack(
        track: SpotifyTrack,
        hideExplicit: Boolean = false
    ): SongItem? = try {
        val fastCached = getCached(track.id)
        if (fastCached != null) {
            return fastCached
        }

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

    open fun getCached(trackId: String): SongItem? = synchronized(trackCache) { trackCache[trackId] }

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

    open suspend fun resolveTracks(
        tracks: List<SpotifyTrack>,
        hideExplicit: Boolean = false
    ): Map<String, SongItem> = coroutineScope {
        val uniqueTracks = tracks.distinctBy { it.id }
        val resultMap = ConcurrentHashMap<String, SongItem>()

        uniqueTracks.map { track ->
            async {
                val resolved = resolveTrack(track, hideExplicit)
                if (resolved != null) {
                    resultMap[track.id] = resolved
                }
            }
        }.awaitAll()

        resultMap
    }

    open suspend fun resolveAndFilter(
        tracks: List<SpotifyTrack>,
        hideExplicit: Boolean
    ): List<SongItem> {
        val map = resolveTracks(tracks, hideExplicit)
        return tracks
            .filter { !hideExplicit || !it.explicit }
            .mapNotNull { map[it.id] }
    }

    open fun findBestMatch(
        candidates: List<SongItem>,
        track: SpotifyTrack,
        hideExplicit: Boolean = false
    ): SongItem? {
        val allowed = candidates.filter { !hideExplicit || !it.explicit }
        if (allowed.isEmpty()) return null

        val trackNameLower = track.name.trim().lowercase()
        val trackArtistsLower = track.artists.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }
        val targetDurationSec = if (track.durationMs > 0) track.durationMs / 1000 else null

        var bestCandidate: SongItem? = null
        var highestScore = Int.MIN_VALUE

        for (candidate in allowed) {
            var score = 0
            val candidateTitleLower = candidate.title.trim().lowercase()
            val candidateArtistsLower = candidate.artists.map { it.name.trim().lowercase() }

            // 1. Title match
            if (candidateTitleLower == trackNameLower) {
                score += 30
            } else if (candidateTitleLower.contains(trackNameLower) || trackNameLower.contains(candidateTitleLower)) {
                score += 15
            }

            // 2. Artist match
            val hasArtistMatch = trackArtistsLower.any { spArtist ->
                candidateArtistsLower.any { ytArtist ->
                    ytArtist == spArtist || ytArtist.contains(spArtist) || spArtist.contains(ytArtist)
                }
            }
            if (hasArtistMatch) {
                score += 25
            }

            // 3. Duration match (if available)
            val candidateDuration = candidate.duration
            if (targetDurationSec != null && candidateDuration != null && candidateDuration > 0) {
                val diffSec = abs(candidateDuration - targetDurationSec)
                when {
                    diffSec <= 3 -> score += 20
                    diffSec <= 8 -> score += 12
                    diffSec <= 20 -> score += 5
                    diffSec > 60 -> score -= 25 // strong penalty for extended mixes / snippets
                }
            }

            // 4. Penalties for unwanted versions if original track doesn't specify them
            val unwantedKeywords = listOf("karaoke", "instrumental", "cover", "tribute", "parody")
            for (keyword in unwantedKeywords) {
                if (!trackNameLower.contains(keyword) && candidateTitleLower.contains(keyword)) {
                    score -= 30
                }
            }
            if (!trackNameLower.contains("live") && candidateTitleLower.contains("live")) {
                score -= 15
            }

            if (score > highestScore) {
                highestScore = score
                bestCandidate = candidate
            }
        }

        return if (highestScore > 0) bestCandidate else allowed.firstOrNull()
    }
}
