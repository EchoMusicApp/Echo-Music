package echo.music.iad1tya.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import echo.music.iad1tya.R
import echo.music.iad1tya.playback.AudioExportService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class ExportWorker(
  private val appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ExportWorkerEntryPoint {
    fun exportEngine(): ExportEngine
  }

  override suspend fun doWork(): Result {
    val entryPoint = EntryPointAccessors.fromApplication(
      appContext,
      ExportWorkerEntryPoint::class.java
    )
    val exportEngine = entryPoint.exportEngine()

    val rawSongIds = inputData.getString(KEY_SONG_IDS)
    val songIds = rawSongIds?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
      ?: exportEngine.getEligibleSongs().map { it.id }

    if (songIds.isEmpty()) {
      return Result.success()
    }

    createNotificationChannel()

    val foregroundInfo = createForegroundInfo(
      currentSong = appContext.getString(R.string.export_preparing),
      completed = 0,
      total = songIds.size,
      progressPercent = 0
    )
    runCatching { setForeground(foregroundInfo) }

    var lastNotificationTimeMs = 0L
    var lastNotificationPercent = -1

    val batchResult = exportEngine.exportBatch(
      songIds = songIds,
      onProgress = { state ->
        if (isStopped) return@exportBatch
        val now = System.currentTimeMillis()
        val percentChanged = state.progressPercent != lastNotificationPercent
        val timeElapsed = (now - lastNotificationTimeMs) >= 1500L
        val isFinished = state.isFinished || state.progressPercent == 100

        if (isFinished || (percentChanged && timeElapsed)) {
          lastNotificationTimeMs = now
          lastNotificationPercent = state.progressPercent
          runCatching {
            val info = createForegroundInfo(
              currentSong = state.currentSongTitle ?: appContext.getString(R.string.exporting),
              completed = state.completedCount + state.failedCount + state.skippedCount,
              total = state.totalCount,
              progressPercent = state.progressPercent
            )
            setForeground(info)
          }
        }
      }
    )

    return if (batchResult.isCancelled || isStopped || batchResult.failedCount > 0) {
      Result.failure()
    } else {
      Result.success()
    }
  }

  private fun createForegroundInfo(
    currentSong: String,
    completed: Int,
    total: Int,
    progressPercent: Int
  ): ForegroundInfo {
    val cancelIntent = PendingIntent.getService(
      appContext,
      8900,
      Intent(appContext, AudioExportService::class.java).setAction("echo.music.iad1tya.CANCEL_EXPORTS"),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.download)
      .setContentTitle(appContext.getString(R.string.exporting))
      .setContentText("$currentSong ($completed/$total)")
      .setProgress(100, progressPercent, false)
      .setOngoing(true)
      .addAction(R.drawable.close, appContext.getString(R.string.cancel_exports), cancelIntent)
      .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      ForegroundInfo(NOTIFICATION_ID, notification)
    }
  }

  private fun createNotificationChannel() {
    appContext.getSystemService<NotificationManager>()?.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        appContext.getString(R.string.export_channel_name),
        NotificationManager.IMPORTANCE_LOW
      )
    )
  }

  companion object {
    private const val WORK_NAME = "BulkAudioExportWork"
    private const val CHANNEL_ID = "bulk_music_export"
    private const val NOTIFICATION_ID = 890
    private const val KEY_SONG_IDS = "key_song_ids"

    fun startBulkExport(context: Context, songIds: List<String>? = null) {
      val dataBuilder = Data.Builder()
      if (!songIds.isNullOrEmpty()) {
        dataBuilder.putString(KEY_SONG_IDS, songIds.joinToString(","))
      }

      val request = OneTimeWorkRequestBuilder<ExportWorker>()
        .setInputData(dataBuilder.build())
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        WORK_NAME,
        ExistingWorkPolicy.KEEP,
        request
      )
    }

    fun cancelBulkExport(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
  }
}
