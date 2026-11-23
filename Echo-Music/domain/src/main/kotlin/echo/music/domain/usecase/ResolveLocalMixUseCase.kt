package echo.music.domain.usecase

import echo.music.domain.models.Song
import echo.music.domain.repositories.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ResolveLocalMixUseCase(
  private val songRepository: SongRepository
) {

  /**
   * Resolves a smart queue mix seeded by a specific song.
   * Expands candidate tracks by shared artists, high play count, and like status,
   * while filtering out the seed song from downstream recommendations.
   */
  suspend fun invoke(seedSongId: String, limit: Int = 25): List<Song> {
    val seedSong = songRepository.getSong(seedSongId).first() ?: return emptyList()
    val seedArtistIds = seedSong.artists.mapNotNull { it.id }.toSet()
    val seedArtistNames = seedSong.artists.map { it.name.trim().lowercase() }.toSet()

    val likedSongs = songRepository.getLikedSongs().first()
    val recentSongs = songRepository.getRecentlyPlayed(limit * 3).first()

    val candidatePool = (likedSongs + recentSongs)
      .filter { it.id != seedSongId }
      .distinctBy { it.id }

    return candidatePool
      .sortedByDescending { candidate ->
        // Bonus if candidate shares an artist with the seed song
        val sharesArtist = candidate.artists.any { artist ->
          if (artist.id != null && seedArtistIds.contains(artist.id)) {
            true
          } else {
            seedArtistNames.contains(artist.name.trim().lowercase())
          }
        }
        val artistBonus = if (sharesArtist) 100.0 else 0.0
        val likedBonus = if (candidate.liked) 30.0 else 0.0
        val playTimeBonus = (candidate.totalPlayTimeMs / 60000.0).coerceAtMost(60.0)

        artistBonus + likedBonus + playTimeBonus
      }
      .take(limit)
  }
}
