package echo.music.iad1tya.generate

import echo.music.iad1tya.db.daos.SongPlayStatsDao
import echo.music.iad1tya.db.entities.SongPlayStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSongPlayStatsDao : SongPlayStatsDao {
  val map = mutableMapOf<String, SongPlayStatsEntity>()
  var shouldThrow = false
  var candidateQueryError: Throwable? = null
  var candidateQueryCount = 0

  override suspend fun upsert(entity: SongPlayStatsEntity) {
    if (shouldThrow) throw RuntimeException("Simulated SQLite write error")
    map[entity.trackKey] = entity
  }

  override suspend fun find(trackKey: String): SongPlayStatsEntity? = map[trackKey]

  override suspend fun findByKeys(trackKeys: List<String>): List<SongPlayStatsEntity> =
    trackKeys.mapNotNull {
      map[it]
    }

  override suspend fun mostPlayedSince(sinceMillis: Long, limit: Int): List<SongPlayStatsEntity> =
    map.values
      .filter { it.lastPlayedAtMillis >= sinceMillis }
      .sortedByDescending { it.playCount }
      .take(limit)

  override suspend fun automaticDownloadCandidatesSince(
    sinceMillis: Long,
    limit: Int,
    offset: Int,
  ): List<SongPlayStatsEntity> {
    candidateQueryCount++
    candidateQueryError?.let { throw it }
    return map.values
      .filter {
        it.lastPlayedAtMillis >= sinceMillis &&
          it.playCount >= 2 &&
          it.skipCount < it.playCount &&
          !it.videoId.isNullOrBlank() &&
          it.title.isNotBlank()
      }
      .sortedWith(
        compareByDescending<SongPlayStatsEntity> { it.playCount }
          .thenByDescending { it.totalPlayTimeMs }
          .thenBy { it.trackKey }
      )
      .drop(offset)
      .take(limit)
  }

  override suspend fun recentlyPlayed(limit: Int): List<SongPlayStatsEntity> =
    map.values.sortedByDescending { it.lastPlayedAtMillis }.take(limit)

  override fun observeTopPlayed(limit: Int): Flow<List<SongPlayStatsEntity>> =
    flowOf(map.values.sortedByDescending { it.totalPlayTimeMs }.take(limit))

  override suspend fun getAllTrackKeys(): List<String> = map.keys.toList()

  override suspend fun clearAll() {
    map.clear()
  }
}

class SongPlayStatsRepositoryTest {
  private lateinit var fakeDao: FakeSongPlayStatsDao
  private lateinit var repository: SongPlayStatsRepository

  @Before
  fun setup() {
    fakeDao = FakeSongPlayStatsDao()
    repository = SongPlayStatsRepository(fakeDao)
  }

  @Test
  fun recordListenedMsForNewTrackCreatesEntry() = runTest {
    repository.recordListenedMs(
      title = "Blinding Lights",
      artist = "The Weeknd",
      videoId = "4NRXx6U8ABQ",
      artworkUrl = "https://example.com/art.jpg",
      listenedMs = 180_000L,
      completed = true,
    )

    val key = trackKeyOf("Blinding Lights", "The Weeknd")
    val stats = fakeDao.find(key)
    assertNotNull(stats)
    assertEquals("Blinding Lights", stats?.title)
    assertEquals("The Weeknd", stats?.artist)
    assertEquals(180_000L, stats?.totalPlayTimeMs)
    assertEquals(1, stats?.playCount)
    assertEquals(0, stats?.skipCount)
    assertEquals("4NRXx6U8ABQ", stats?.videoId)
  }

  @Test
  fun recordListenedMsIncompleteDoesNotIncrementPlayCount() = runTest {
    repository.recordListenedMs(
      title = "Starboy",
      artist = "The Weeknd",
      videoId = "34Na4j8AVgA",
      artworkUrl = null,
      listenedMs = 45_000L,
      completed = false,
    )

    val key = trackKeyOf("Starboy", "The Weeknd")
    val stats = fakeDao.find(key)
    assertNotNull(stats)
    assertEquals(45_000L, stats?.totalPlayTimeMs)
    assertEquals(0, stats?.playCount)
    assertEquals(0, stats?.skipCount)
  }

  @Test
  fun recordListenedMsAccumulatesExistingStats() = runTest {
    repository.recordListenedMs(
      title = "Save Your Tears",
      artist = "The Weeknd",
      videoId = "v1",
      artworkUrl = null,
      listenedMs = 100_000L,
      completed = true,
    )

    repository.recordListenedMs(
      title = "Save Your Tears",
      artist = "The Weeknd",
      videoId = "v1",
      artworkUrl = null,
      listenedMs = 120_000L,
      completed = true,
    )

    val key = trackKeyOf("Save Your Tears", "The Weeknd")
    val stats = fakeDao.find(key)
    assertNotNull(stats)
    assertEquals(220_000L, stats?.totalPlayTimeMs)
    assertEquals(2, stats?.playCount)
    assertEquals(0, stats?.skipCount)
  }

  @Test
  fun recordSkipIncrementsSkipCountAndAccumulatesTime() = runTest {
    repository.recordListenedMs(
      title = "As It Was",
      artist = "Harry Styles",
      videoId = "h1",
      artworkUrl = null,
      listenedMs = 50_000L,
      completed = false,
    )

    repository.recordSkip(
      title = "As It Was",
      artist = "Harry Styles",
      videoId = "h1",
      artworkUrl = null,
      listenedMs = 15_000L,
    )

    val key = trackKeyOf("As It Was", "Harry Styles")
    val stats = fakeDao.find(key)
    assertNotNull(stats)
    assertEquals(65_000L, stats?.totalPlayTimeMs)
    assertEquals(0, stats?.playCount)
    assertEquals(1, stats?.skipCount)
  }

  @Test
  fun recordSkipOnNewTrackCreatesRowWithSkipCountOne() = runTest {
    repository.recordSkip(
      title = "Cruel Summer",
      artist = "Taylor Swift",
      videoId = "cs1",
      artworkUrl = null,
      listenedMs = 10_000L,
    )

    val key = trackKeyOf("Cruel Summer", "Taylor Swift")
    val stats = fakeDao.find(key)
    assertNotNull(stats)
    assertEquals(10_000L, stats?.totalPlayTimeMs)
    assertEquals(0, stats?.playCount)
    assertEquals(1, stats?.skipCount)
  }

  private fun candidate(key: String, playCount: Int, totalPlayTimeMs: Long = 1_000L) =
    SongPlayStatsEntity(
      trackKey = key,
      title = "Title $key",
      artist = "Artist",
      videoId = "vid_$key",
      totalPlayTimeMs = totalPlayTimeMs,
      playCount = playCount,
      skipCount = 0,
      lastPlayedAtMillis = 10_000L,
    )

  @Test
  fun automaticDownloadCandidatesPagesPastExcludedRows() = runTest {
    // 401 ranked rows; the top 400 are excluded (e.g. already downloaded).
    val rows = (0 until 401).map { i -> candidate(key = "k%03d".format(i), playCount = 1_000 - i) }
    rows.forEach { fakeDao.upsert(it) }
    val excluded = rows.take(400).mapNotNull { it.videoId }.toSet()

    val result =
      repository.automaticDownloadCandidates(
        sinceMillis = 0L,
        isExcluded = { it in excluded },
        limit = 1,
      )

    assertEquals(listOf(rows.last().trackKey), result.map { it.trackKey })
    assertTrue("should need more than one page", fakeDao.candidateQueryCount > 1)
  }

  @Test
  fun automaticDownloadCandidatesOrderEqualRankDeterministically() = runTest {
    listOf("c", "a", "b").forEach { fakeDao.upsert(candidate(key = it, playCount = 5)) }

    val result =
      repository.automaticDownloadCandidates(sinceMillis = 0L, isExcluded = { false }, limit = 3)

    assertEquals(listOf("a", "b", "c"), result.map { it.trackKey })
  }

  @Test
  fun automaticDownloadCandidatesPropagatesCancellation() = runTest {
    fakeDao.upsert(candidate(key = "a", playCount = 3))
    fakeDao.candidateQueryError = CancellationException("cancelled")

    try {
      repository.automaticDownloadCandidates(sinceMillis = 0L, isExcluded = { false }, limit = 1)
      fail("CancellationException should propagate")
    } catch (expected: CancellationException) {
      assertEquals("cancelled", expected.message)
    }
  }

  @Test
  fun errorInDaoDoesNotThrow() = runTest {
    fakeDao.shouldThrow = true
    // Must complete cleanly without uncaught exception
    repository.recordListenedMs(
      title = "Anti-Hero",
      artist = "Taylor Swift",
      videoId = "ah1",
      artworkUrl = null,
      listenedMs = 60_000L,
      completed = true,
    )
    repository.recordSkip(
      title = "Anti-Hero",
      artist = "Taylor Swift",
      videoId = "ah1",
      artworkUrl = null,
      listenedMs = 5_000L,
    )
    assertTrue(true)
  }
}
