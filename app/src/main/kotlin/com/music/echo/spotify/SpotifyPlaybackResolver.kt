package com.music.echo.spotify

import com.music.innertube.YouTube
import echo.music.iad1tya.spotify.models.SpotifyTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import com.music.innertube.models.SongItem
import kotlin.math.abs

@Singleton
open class SpotifyPlaybackResolver @Inject constructor() {
    private val trackCache = object : LinkedHashMap<String, SongItem>(1000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongItem>?): Boolean = size > 1000
    }
    private val resolutionSemaphore = Semaphore(3)

    open suspend fun resolveTrack(
        track: SpotifyTrack,
        hideExplicit: Boolean = false
    ): SongItem? = try {
        resolutionSemaphore.withPermit {
            val cachedSong = synchronized(trackCache) { trackCache[track.id] }
            if (cachedSong != null) {
                return@withPermit cachedSong
            }

            val query = "${track.name} ${track.artists.firstOrNull()?.name.orEmpty()}".trim()
            val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val candidateSongs = searchResult?.items?.filterIsInstance<SongItem>().orEmpty()
            val bestSong = findBestMatch(candidateSongs, track, hideExplicit)

            if (bestSong != null) {
                synchronized(trackCache) { trackCache[track.id] = bestSong }
            }
            bestSong
        }
    } catch (e: Exception) {
        null
    }

    open fun getCached(trackId: String): SongItem? = synchronized(trackCache) { trackCache[trackId] }

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
