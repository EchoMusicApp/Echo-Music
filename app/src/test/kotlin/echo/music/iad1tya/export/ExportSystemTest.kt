package echo.music.iad1tya.export

import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSystemTest {

  @Test
  fun testExportConflictStrategyFromString() {
    assertEquals(ExportConflictStrategy.SKIP, ExportConflictStrategy.fromString("skip"))
    assertEquals(ExportConflictStrategy.REPLACE, ExportConflictStrategy.fromString("REPLACE"))
    assertEquals(ExportConflictStrategy.RENAME, ExportConflictStrategy.fromString("rename"))
    assertEquals(ExportConflictStrategy.SKIP, ExportConflictStrategy.fromString("unknown"))
    assertEquals(ExportConflictStrategy.SKIP, ExportConflictStrategy.fromString(null))
  }

  @Test
  fun testExportBatchStateInitialValues() {
    val state = ExportBatchState()
    assertFalse(state.isExporting)
    assertFalse(state.isFinished)
    assertFalse(state.isCancelled)
    assertEquals(ExportStage.QUEUED, state.currentStage)
    assertEquals(0, state.completedCount)
    assertEquals(0, state.failedCount)
    assertEquals(0, state.skippedCount)
    assertEquals(0, state.totalCount)
    assertEquals(0, state.progressPercent)
  }

  @Test
  fun testExportStageOrder() {
    val stages = listOf(
      ExportStage.QUEUED,
      ExportStage.RESOLVING,
      ExportStage.DOWNLOADING,
      ExportStage.CONVERTING,
      ExportStage.SAVING,
      ExportStage.FINALIZING,
      ExportStage.COMPLETED
    )

    assertEquals(ExportStage.QUEUED, stages[0])
    assertEquals(ExportStage.RESOLVING, stages[1])
    assertEquals(ExportStage.DOWNLOADING, stages[2])
    assertEquals(ExportStage.CONVERTING, stages[3])
    assertEquals(ExportStage.SAVING, stages[4])
    assertEquals(ExportStage.FINALIZING, stages[5])
    assertEquals(ExportStage.COMPLETED, stages[6])
  }

  @Test
  fun testExportableSongState() {
    val dummySong = Song(
      song = SongEntity(
        id = "test_song_1",
        title = "Test Song",
        duration = 180,
        isDownloaded = true
      ),
      artists = emptyList()
    )

    val exportable = ExportableSong(
      id = "test_song_1",
      title = "Test Song",
      artist = "Test Artist",
      album = "Test Album",
      durationSeconds = 180,
      sizeBytes = 5_000_000L,
      thumbnailUrl = null,
      isLocal = false,
      isDownloaded = true,
      status = ExportStatus.PENDING,
      stage = ExportStage.QUEUED,
      song = dummySong
    )

    assertEquals("test_song_1", exportable.id)
    assertEquals(ExportStatus.PENDING, exportable.status)
    assertEquals(ExportStage.QUEUED, exportable.stage)
    assertTrue(exportable.isDownloaded)
    assertFalse(exportable.isLocal)
  }
}
