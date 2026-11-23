package echo.music.domain.usecase

import echo.music.domain.models.ArtistRef
import echo.music.domain.models.Song
import echo.music.domain.repositories.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ResolveLocalMixUseCaseTest {

  private class FakeSongRepository(
    private val songs: Map<String, Song>
  ) : SongRepository {
    override fun getSong(id: String): Flow<Song?> = flowOf(songs[id])
    override fun getLikedSongs(): Flow<List<Song>> = flowOf(songs.values.filter { it.liked })
    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> = flowOf(songs.values.toList().take(limit))
    override suspend fun updateLiked(id: String, isLiked: Boolean) {}
    override suspend fun incrementPlayTime(id: String, durationMs: Long) {}
  }

  @Test
  fun testSeedExpansionPrioritizesSharedArtistsAndExcludesSeed() = runTest {
    val seedArtist = ArtistRef("art_radiohead", "Radiohead")
    val otherArtist = ArtistRef("art_daftpunk", "Daft Punk")

    val seedSong = Song(
      id = "s_seed",
      title = "Karma Police",
      artists = listOf(seedArtist),
      album = null,
      durationSeconds = 260,
      thumbnailUrl = null,
      liked = true
    )

    val relatedSong = Song(
      id = "s_related",
      title = "Paranoid Android",
      artists = listOf(seedArtist),
      album = null,
      durationSeconds = 380,
      thumbnailUrl = null,
      liked = false
    )

    val unrelatedSong = Song(
      id = "s_unrelated",
      title = "Get Lucky",
      artists = listOf(otherArtist),
      album = null,
      durationSeconds = 240,
      thumbnailUrl = null,
      liked = true
    )

    val repo = FakeSongRepository(
      mapOf(
        seedSong.id to seedSong,
        relatedSong.id to relatedSong,
        unrelatedSong.id to unrelatedSong
      )
    )

    val useCase = ResolveLocalMixUseCase(repo)
    val mix = useCase.invoke(seedSongId = "s_seed", limit = 10)

    // Seed song must be excluded
    assertFalse(mix.any { it.id == "s_seed" })
    assertEquals(2, mix.size)
    // Related song (shared artist: +100) must rank ahead of unrelated song (liked: +30)
    assertEquals("s_related", mix[0].id)
    assertEquals("s_unrelated", mix[1].id)
  }
}
