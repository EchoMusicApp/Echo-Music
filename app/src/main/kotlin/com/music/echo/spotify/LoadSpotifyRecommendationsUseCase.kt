package com.music.echo.spotify

import echo.music.iad1tya.spotifyimport.SpotifyImportRepository
import echo.music.iad1tya.spotify.Spotify
import echo.music.iad1tya.spotify.models.SpotifyTrack
import echo.music.iad1tya.db.MusicDatabase
import com.music.echo.models.SimilarRecommendation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadSpotifyRecommendationsUseCase @Inject constructor(
    private val spotifyImportRepository: SpotifyImportRepository,
    private val database: MusicDatabase,
    private val spotifyPlaybackResolver: SpotifyPlaybackResolver
) {
    suspend operator fun invoke(
        hideExplicit: Boolean,
        fromTimeStamp: Long
    ): List<SimilarRecommendation>? = withContext(Dispatchers.IO) {
        val session = spotifyImportRepository.restoreSession()
        if (!session.isAuthenticated) return@withContext null

        val localArtists = database.mostPlayedArtists(fromTimeStamp, limit = 10).first().filter { it.artist.isYouTubeArtist }.shuffled().take(3)
        val localSongs = database.mostPlayedSongs(fromTimeStamp, limit = 10).first().filter { it.album != null }.shuffled().take(2)

        val recommendations = mutableListOf<SimilarRecommendation>()

        coroutineScope {
            localArtists.map { artist ->
                async {
                    try {
                        val spotifyArtistId = getSpotifyArtistId(artist.artist.name) ?: return@async null
                        val result = Spotify.recommendations(seedArtistIds = listOf(spotifyArtistId), limit = 8).getOrNull()
                        val tracks = result?.tracks.orEmpty()
                        if (tracks.isEmpty()) return@async null
                        val resolved = spotifyPlaybackResolver.resolveAndFilter(tracks, hideExplicit)
                        if (resolved.isEmpty()) return@async null
                        SimilarRecommendation(title = artist.artist.name, items = resolved)
                    } catch (e: Exception) { null }
                }
            }.awaitAll().filterNotNull().let { recommendations.addAll(it) }
        }

        coroutineScope {
            localSongs.map { song ->
                async {
                    try {
                        val spotifyTrackId = getSpotifyTrackId(song.song.title, song.artists.firstOrNull()?.name) ?: return@async null
                        val result = Spotify.recommendations(seedTrackIds = listOf(spotifyTrackId), limit = 8).getOrNull()
                        val tracks = result?.tracks.orEmpty()
                        if (tracks.isEmpty()) return@async null
                        val resolved = spotifyPlaybackResolver.resolveAndFilter(tracks, hideExplicit)
                        if (resolved.isEmpty()) return@async null
                        SimilarRecommendation(title = song.song.title, items = resolved)
                    } catch (e: Exception) { null }
                }
            }.awaitAll().filterNotNull().let { recommendations.addAll(it) }
        }

        if (recommendations.isEmpty()) {
            val topArtists = Spotify.topArtists(limit = 4).getOrNull()?.items.orEmpty()
            coroutineScope {
                topArtists.map { topArtist ->
                    async {
                        try {
                            val result = Spotify.recommendations(seedArtistIds = listOf(topArtist.id), limit = 8).getOrNull()
                            val tracks = result?.tracks.orEmpty()
                            if (tracks.isEmpty()) return@async null
                            val resolved = spotifyPlaybackResolver.resolveAndFilter(tracks, hideExplicit)
                            if (resolved.isEmpty()) return@async null
                            SimilarRecommendation(title = topArtist.name, items = resolved)
                        } catch (e: Exception) { null }
                    }
                }.awaitAll().filterNotNull().let { recommendations.addAll(it) }
            }
        }

        return@withContext recommendations.takeIf { it.isNotEmpty() }
    }

    private suspend fun getSpotifyArtistId(artistName: String): String? {
        return Spotify.search(artistName, "artist").getOrNull()?.artists?.items?.firstOrNull()?.id
    }

    private suspend fun getSpotifyTrackId(title: String, artist: String?): String? {
        val query = if (artist != null) "$title $artist" else title
        return Spotify.search(query, "track").getOrNull()?.tracks?.items?.firstOrNull()?.id
    }
}
