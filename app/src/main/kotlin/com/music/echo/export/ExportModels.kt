package echo.music.iad1tya.export

import echo.music.iad1tya.db.entities.Song

enum class ExportStage {
  QUEUED,
  RESOLVING,
  DOWNLOADING,
  VERIFYING_SOURCE,
  CONVERTING,
  VERIFYING_OUTPUT,
  SAVING,
  VERIFYING_DESTINATION,
  FINALIZING,
  COMPLETED,
  FAILED,
  CANCELLED,
}

enum class ExportStatus {
  PENDING,
  EXPORTING,
  COMPLETED,
  FAILED,
  SKIPPED,
  CANCELLED,
}

enum class ExportConflictStrategy {
  SKIP,
  REPLACE,
  RENAME;

  companion object {
    fun fromString(value: String?): ExportConflictStrategy {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SKIP
    }
  }
}

data class ExportableSong(
  val id: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationSeconds: Int,
  val sizeBytes: Long,
  val thumbnailUrl: String?,
  val isLocal: Boolean,
  val isDownloaded: Boolean,
  val status: ExportStatus = ExportStatus.PENDING,
  val stage: ExportStage = ExportStage.QUEUED,
  val errorMessage: String? = null,
  val song: Song,
)

data class ExportBatchState(
  val isExporting: Boolean = false,
  val currentSongId: String? = null,
  val currentSongTitle: String? = null,
  val currentStage: ExportStage = ExportStage.QUEUED,
  val completedCount: Int = 0,
  val failedCount: Int = 0,
  val skippedCount: Int = 0,
  val totalCount: Int = 0,
  val progressPercent: Int = 0,
  val exportedSizeBytes: Long = 0L,
  val isFinished: Boolean = false,
  val isCancelled: Boolean = false,
  val failedSongIds: List<String> = emptyList(),
)
