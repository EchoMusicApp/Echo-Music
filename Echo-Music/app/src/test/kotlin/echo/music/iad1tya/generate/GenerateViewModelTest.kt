package echo.music.iad1tya.generate

import echo.music.iad1tya.db.daos.RecommendationExclusionDao
import echo.music.iad1tya.db.entities.RecommendationExclusionEntity
import echo.music.iad1tya.viewmodels.GenerateUiState
import echo.music.iad1tya.viewmodels.GenerateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRecommendationExclusionDao : RecommendationExclusionDao {
  val map = mutableMapOf<String, RecommendationExclusionEntity>()

  override suspend fun upsert(entity: RecommendationExclusionEntity) {
    map[entity.trackKey] = entity
  }

  override suspend fun upsertAll(entities: List<RecommendationExclusionEntity>) {
    entities.forEach { map[it.trackKey] = it }
  }

  override suspend fun getAll(): List<RecommendationExclusionEntity> = map.values.toList()

  override fun observeAll(): Flow<List<RecommendationExclusionEntity>> = flowOf(map.values.toList())

  override suspend fun count(): Int = map.size

  override suspend fun isExcluded(trackKey: String): Boolean = map.containsKey(trackKey)

  override suspend fun delete(trackKey: String) {
    map.remove(trackKey)
  }

  override suspend fun clear() {
    map.clear()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeStatsDao: FakeSongPlayStatsDao
  private lateinit var fakeExclusionDao: FakeRecommendationExclusionDao

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeStatsDao = FakeSongPlayStatsDao()
    fakeExclusionDao = FakeRecommendationExclusionDao()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialUiStateIsIdle() {
    val fakeEngine = object : LocalTasteEngine(fakeStatsDao, fakeExclusionDao) {
      override suspend fun generate(count: Int): Result<String> = Result.success("p1")
    }
    val viewModel = GenerateViewModel(fakeEngine, fakeStatsDao, fakeExclusionDao)
    assertEquals(GenerateUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun successfulGenerationTransitionsToSuccess() = runTest(testDispatcher) {
    val fakeEngine = object : LocalTasteEngine(fakeStatsDao, fakeExclusionDao) {
      override suspend fun generate(count: Int): Result<String> = Result.success("taste_playlist_123")
    }
    val viewModel = GenerateViewModel(fakeEngine, fakeStatsDao, fakeExclusionDao)

    viewModel.generate()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state is GenerateUiState.Success)
    assertEquals("taste_playlist_123", (state as GenerateUiState.Success).playlistId)
  }

  @Test
  fun noListeningHistoryTransitionsToEmptyHistory() = runTest(testDispatcher) {
    val fakeEngine = object : LocalTasteEngine(fakeStatsDao, fakeExclusionDao) {
      override suspend fun generate(count: Int): Result<String> =
        Result.failure(IllegalStateException("No listening history available yet"))
    }
    val viewModel = GenerateViewModel(fakeEngine, fakeStatsDao, fakeExclusionDao)

    viewModel.generate()
    advanceUntilIdle()

    assertEquals(GenerateUiState.EmptyHistory, viewModel.uiState.value)
  }

  @Test
  fun allExcludedTransitionsToAllExcluded() = runTest(testDispatcher) {
    val fakeEngine = object : LocalTasteEngine(fakeStatsDao, fakeExclusionDao) {
      override suspend fun generate(count: Int): Result<String> =
        Result.failure(IllegalStateException("All candidate tracks have been excluded"))
    }
    val viewModel = GenerateViewModel(fakeEngine, fakeStatsDao, fakeExclusionDao)

    viewModel.generate()
    advanceUntilIdle()

    assertEquals(GenerateUiState.AllExcluded, viewModel.uiState.value)
  }

  @Test
  fun clearExclusionsClearsDaoAndResetsIdle() = runTest(testDispatcher) {
    fakeExclusionDao.upsert(
      RecommendationExclusionEntity(
        trackKey = "key1",
        excludedAtMillis = 1000L,
      )
    )
    val fakeEngine = object : LocalTasteEngine(fakeStatsDao, fakeExclusionDao) {
      override suspend fun generate(count: Int): Result<String> =
        Result.failure(IllegalStateException("All candidate tracks have been excluded"))
    }
    val viewModel = GenerateViewModel(fakeEngine, fakeStatsDao, fakeExclusionDao)
    viewModel.generate()
    advanceUntilIdle()
    assertEquals(GenerateUiState.AllExcluded, viewModel.uiState.value)

    viewModel.clearExclusions()
    advanceUntilIdle()

    assertTrue(fakeExclusionDao.getAll().isEmpty())
    assertEquals(GenerateUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun cancelGenerationResetsToIdle() = runTest(testDispatcher) {
    val fakeEngine = object : LocalTasteEngine(fakeStatsDao, fakeExclusionDao) {
      override suspend fun generate(count: Int): Result<String> {
        kotlinx.coroutines.delay(10_000)
        return Result.success("p1")
      }
    }
    val viewModel = GenerateViewModel(fakeEngine, fakeStatsDao, fakeExclusionDao)

    viewModel.generate()
    testScheduler.advanceTimeBy(100)
    assertTrue(viewModel.uiState.value is GenerateUiState.Generating)

    viewModel.cancelGeneration()
    assertEquals(GenerateUiState.Idle, viewModel.uiState.value)
  }
}
