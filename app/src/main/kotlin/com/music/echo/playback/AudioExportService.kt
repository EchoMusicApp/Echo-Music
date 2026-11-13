package echo.music.iad1tya.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.EnableExportAsMp3Key
import echo.music.iad1tya.constants.ExportConflictStrategyKey
import echo.music.iad1tya.constants.ExportMetadataKey
import echo.music.iad1tya.constants.ExportProgressKey
import echo.music.iad1tya.constants.ExportingSongIdsKey
import echo.music.iad1tya.db.entities.ArtistEntity
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SongEntity
import echo.music.iad1tya.export.ExportConflictStrategy
import echo.music.iad1tya.export.ExportEngine
import echo.music.iad1tya.export.ExportableSong
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber

class AudioExportService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val httpClient = OkHttpClient()
  private val exports = ExportTaskQueue(serviceScope, onIdle = ::finishExports)

  private var lastNotificationTimeMs = 0L
  private var lastNotificationPercent = -1

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ServiceEntryPoint {
    fun exportEngine(): ExportEngine
  }

  override fun onCreate() {
    super.onCreate()
    getSystemService<NotificationManager>()
      ?.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.export_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        )
      )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    val songId = intent?.getStringExtra(EXTRA_SONG_ID)
    Timber.d("AudioExportService onStartCommand: action=%s, songId=%s", action, songId)

    if (action == ACTION_CANCEL_ALL) {
      Timber.i("Cancelling all exports via AudioExportService action")
      exports.cancelAll()
      httpClient.dispatcher.cancelAll()
      if (exports.isIdle) finishExports()
      return START_NOT_STICKY
    }

    val targetDirectoryUri = intent?.getStringExtra(EXTRA_TARGET_DIRECTORY_URI)
    if (songId.isNullOrBlank() || targetDirectoryUri.isNullOrBlank()) {
      if (exports.isIdle) stopSelf(startId)
      return START_NOT_STICKY
    }
    val songTitle = intent.getStringExtra(EXTRA_SONG_TITLE).orEmpty()
    val songArtist = intent.getStringExtra(EXTRA_SONG_ARTIST).orEmpty()
    val songAlbum = intent.getStringExtra(EXTRA_SONG_ALBUM).orEmpty()
    val artworkUrl = intent.getStringExtra(EXTRA_ARTWORK_URL).orEmpty()

    updateNotification(songTitle, 0, force = true)

    val submitted =
      exports.submit(songId) {
        exportSong(
          songId = songId,
          songTitle = songTitle,
          songArtist = songArtist,
          songAlbum = songAlbum,
          artworkUrl = artworkUrl,
          targetDirectoryUri = targetDirectoryUri,
        )
      }

    if (!submitted) {
      Timber.w("Export for songId=%s was not submitted because it is already active", songId)
    }

    return START_NOT_STICKY
  }

  private fun updateNotification(songTitle: String, percent: Int, force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (
      !force &&
        percent < 100 &&
        (now - lastNotificationTimeMs) < 1500L &&
        percent == lastNotificationPercent
    ) {
      return
    }
    lastNotificationTimeMs = now
    lastNotificationPercent = percent

    val cancelIntent =
      PendingIntent.getService(
        this,
        8899,
        Intent(this, AudioExportService::class.java).setAction(ACTION_CANCEL_ALL),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.download)
        .setContentTitle(getString(R.string.exporting))
        .setContentText(songTitle)
        .setProgress(100, percent, false)
        .setOngoing(true)
        .addAction(R.drawable.close, getString(R.string.cancel_exports), cancelIntent)
        .build()

    startForeground(NOTIFICATION_ID, notification)
  }

  private suspend fun exportSong(
    songId: String,
    songTitle: String,
    songArtist: String,
    songAlbum: String,
    artworkUrl: String,
    targetDirectoryUri: String,
  ) {
    val entryPoint =
      EntryPointAccessors.fromApplication(
        applicationContext,
        ServiceEntryPoint::class.java,
      )
    val engine = entryPoint.exportEngine()

    val eligibleSongs = engine.getEligibleSongs().associateBy { it.id }
    val song = eligibleSongs[songId]

    val prefs = applicationContext.dataStore.data.first()
    val enableMp3 = prefs[EnableExportAsMp3Key] ?: false
    val conflictStrategy = ExportConflictStrategy.fromString(prefs[ExportConflictStrategyKey])
    val preserveMetadata = prefs[ExportMetadataKey] ?: true

    val exportable =
      song
        ?: ExportableSong(
          id = songId,
          title = songTitle,
          artist = songArtist,
          album = songAlbum,
          durationSeconds = 0,
          sizeBytes = 0L,
          thumbnailUrl = artworkUrl,
          isLocal = songId.startsWith("content://") || songId.startsWith("file://"),
          isDownloaded = true,
          song =
            Song(
              song =
                SongEntity(
                  id = songId,
                  title = songTitle,
                  duration = 0,
                  thumbnailUrl = artworkUrl,
                  albumName = songAlbum,
                ),
              artists = listOf(ArtistEntity(id = "artist_$songArtist", name = songArtist)),
            ),
        )

    engine.exportSingleSong(
      song = exportable,
      targetDirectoryUri = targetDirectoryUri,
      conflictStrategy = conflictStrategy,
      enableMp3 = enableMp3,
      preserveMetadata = preserveMetadata,
      onProgressUpdate = { _, percent ->
        updateNotification(songTitle, percent)
      },
    )
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun finishExports() {
    serviceScope.launch(NonCancellable) {
      try {
        dataStore.edit { preferences ->
          if (exports.isIdle) {
            preferences.remove(ExportingSongIdsKey)
            preferences.remove(ExportProgressKey)
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "Unable to clear export state")
      } finally {
        withContext(Dispatchers.Main.immediate) {
          if (exports.isIdle) stopSelf()
        }
      }
    }
  }

  override fun onTimeout(startId: Int, fgsType: Int) {
    exports.cancelAll()
    httpClient.dispatcher.cancelAll()
    stopSelf()
  }

  override fun onDestroy() {
    exports.cancelAll()
    httpClient.dispatcher.cancelAll()
    serviceScope.cancel()
    stopForeground(STOP_FOREGROUND_REMOVE)
    super.onDestroy()
  }

  companion object {
    private const val CHANNEL_ID = "music_export"
    private const val NOTIFICATION_ID = 889
    private const val ACTION_CANCEL_ALL = "echo.music.iad1tya.CANCEL_EXPORTS"
    private const val EXTRA_SONG_ID = "extra_song_id"
    private const val EXTRA_SONG_TITLE = "extra_song_title"
    private const val EXTRA_SONG_ARTIST = "extra_song_artist"
    private const val EXTRA_SONG_ALBUM = "extra_song_album"
    private const val EXTRA_ARTWORK_URL = "extra_artwork_url"
    private const val EXTRA_TARGET_DIRECTORY_URI = "extra_target_directory_uri"

    fun cancelAll(context: Context) {
      context.startService(
        Intent(context, AudioExportService::class.java).setAction(ACTION_CANCEL_ALL)
      )
    }

    fun start(
      context: Context,
      songId: String,
      songTitle: String,
      songArtist: String,
      songAlbum: String,
      artworkUrl: String,
      targetDirectoryUri: String,
    ) {
      val intent =
        Intent(context, AudioExportService::class.java).apply {
          putExtra(EXTRA_SONG_ID, songId)
          putExtra(EXTRA_SONG_TITLE, songTitle)
          putExtra(EXTRA_SONG_ARTIST, songArtist)
          putExtra(EXTRA_SONG_ALBUM, songAlbum)
          putExtra(EXTRA_ARTWORK_URL, artworkUrl)
          putExtra(EXTRA_TARGET_DIRECTORY_URI, targetDirectoryUri)
        }
      ContextCompat.startForegroundService(context, intent)
    }
  }
}
