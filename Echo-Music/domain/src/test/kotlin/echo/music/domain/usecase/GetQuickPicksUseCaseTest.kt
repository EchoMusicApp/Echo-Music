package echo.music.domain.usecase

import echo.music.domain.models.ArtistRef
import echo.music.domain.models.Song
import echo.music.domain.repositories.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetQuickPicksUseCaseTest {

  private class FakeSongRepository(
    private val liked: List<Song> = emptyList(),
    private val recent: List<Song> = emptyList()
  ) : SongRepository {
    override fun getSong(id: String): Flow<Song?> = flowOf(null)
    override fun getLikedSongs(): Flow<List<Song>> = flowOf(liked)
    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> = flowOf(recent.take(limit))
    override suspend fun updateLiked(id: String, isLiked: Boolean) {}
    override suspend fun incrementPlayTime(id: String, durationMs: Long) {}
  }

  private fun createSong(id: String, title: String, liked: Boolean = false, playTimeMs: Long = 0L): Song {
    return Song(
      id = id,
      title = title,
      artists = listOf(ArtistRef("a_$id", "Artist $id")),
      album = null,
      durationSeconds = 200,
      thumbnailUrl = null,
      liked = liked,
      totalPlayTimeMs = playTimeMs
    )
  }

  @Test
  fun testQuickPicksRanksLikedAndHighPlaytimeHigher() = runTest {
    val s1 = createSong("1", "Low Play, Not Liked", liked = false, playTimeMs = 1000L)
    val s2 = createSong("2", "High Play, Not Liked", liked = false, playTimeMs = 3000000L) // 50 mins
    val s3 = createSong("3", "Liked Song", liked = true, playTimeMs = 60000L)

    val repo = FakeSongRepository(
      liked = listOf(s3),
      recent = listOf(s1, s2, s3)
    )

    val useCase = GetQuickPicksUseCase(repo)
    val results = useCase(limit = 3).first()

    assertEquals(3, results.size)
    // s2 has 50 mins = 100 points, s3 has liked(50) + 1 min(2) = 52 points, s1 has ~0 points
    assertEquals("2", results[0].id)
    assertEquals("3", results[1].id)
    assertEquals("1", results[2].id)
  }

  @Test
  fun testQuickPicksRespectsLimit() = runTest {
    val songs = (1..10).map { createSong("$it", "Song $it") }
    val repo = FakeSongRepository(recent = songs)

    val useCase = GetQuickPicksUseCase(repo)
    val results = useCase(limit = 4).first()

    assertEquals(4, results.size)
  }
}
