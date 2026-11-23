package echo.music.domain.usecase

import echo.music.domain.models.Song
import echo.music.domain.repositories.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetQuickPicksUseCase(
  private val songRepository: SongRepository
) {

  operator fun invoke(limit: Int = 20): Flow<List<Song>> {
    return combine(
      songRepository.getLikedSongs(),
      songRepository.getRecentlyPlayed(limit * 2)
    ) { likedSongs, recentSongs ->
      val likedIds = likedSongs.map { it.id }.toSet()
      
      // Candidate pool: distinct union of recently played and liked songs
      val allCandidates = (recentSongs + likedSongs).distinctBy { it.id }

      allCandidates
        .sortedByDescending { song ->
          val likedBonus = if (likedIds.contains(song.id)) 50.0 else 0.0
          val listeningSeconds = (song.totalPlayTimeMs / 1000.0).coerceAtMost(3600.0)
          val listeningScore = (listeningSeconds / 60.0) * 2.0 // +2 points per minute played up to 60 mins

          likedBonus + listeningScore
        }
        .take(limit)
    }
  }
}
