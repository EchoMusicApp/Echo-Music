package echo.music.iad1tya.viewmodels

// Copied from BackupRestoreViewModel.kt: the CSV import dialog needs these two classes, but the view model itself
// (zip backup via Android Uri/MusicService) is not ported yet. Delete this file when it is.
data class CsvImportState(
  val previewRows: List<List<String>> = emptyList(),
  val artistColumnIndex: Int = 0,
  val titleColumnIndex: Int = 1,
  val urlColumnIndex: Int = -1,
  val hasHeader: Boolean = true,
)

data class ConvertedSongLog(
  val title: String,
  val artists: String,
)
