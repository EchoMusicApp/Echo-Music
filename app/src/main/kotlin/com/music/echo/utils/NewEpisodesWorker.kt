package echo.music.iad1tya.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.music.innertube.YouTube
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import echo.music.iad1tya.MainActivity
import echo.music.iad1tya.R
import echo.music.iad1tya.db.MusicDatabase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Checks the followed podcast shows and notifies about episodes published since the last check.
 * A show's first check only records its latest episode, so following one does not notify about
 * what was already out.
 */
class NewEpisodesWorker(appContext: Context, workerParams: WorkerParameters) :
  CoroutineWorker(appContext, workerParams) {

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface Dependencies {
    fun database(): MusicDatabase

    fun podcastShows(): PodcastShowStore
  }

  override suspend fun doWork(): Result {
    val dependencies =
      EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
    val showIds = dependencies.podcastShows().ids.value
    val followed =
      dependencies.database().playlistsByCreateDateAsc().first().filter {
        it.playlist.bookmarkedAt != null && it.playlist.browseId in showIds
      }
    val seen = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    followed.forEach { show ->
      val showId = show.playlist.browseId ?: return@forEach
      val episodes = YouTube.podcast(showId).getOrNull()?.songs ?: return@forEach
      val ids = episodes.map { it.id }
      val known = seen.getStringSet(showId, null)
      if (known == null) {
        if (ids.isNotEmpty()) seen.edit { putStringSet(showId, ids.toSet()) }
        return@forEach
      }
      // By id, not just the latest: deleting an episode must not make an older one look new.
      val latest = episodes.firstOrNull { it.id !in known } ?: return@forEach
      // Without permission nothing is recorded, so it can still notify once granted.
      if (notifyNewEpisode(showId, show.playlist.name, latest.title)) {
        seen.edit { putStringSet(showId, known + ids) }
      }
    }
    return Result.success()
  }

  private fun notifyNewEpisode(showId: String, showName: String, episodeTitle: String): Boolean {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
          applicationContext,
          Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }
    val manager = applicationContext.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          applicationContext.getString(R.string.new_episodes),
          NotificationManager.IMPORTANCE_DEFAULT,
        )
      )
    }
    // MainActivity opens playlist links, and a show is a playlist.
    val intent =
      Intent(
        Intent.ACTION_VIEW,
        "https://music.youtube.com/playlist?list=$showId".toUri(),
        applicationContext,
        MainActivity::class.java,
      )
    val pending =
      PendingIntent.getActivity(
        applicationContext,
        showId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    manager.notify(
      showId.hashCode(),
      NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_nobg)
        .setContentTitle(showName)
        .setContentText(episodeTitle)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .build(),
    )
    return true
  }

  companion object {
    private const val WORK_NAME = "NewEpisodesWorker"
    private const val PREFS_NAME = "podcast_known_episodes"
    private const val CHANNEL_ID = "podcast_episodes"

    fun schedule(context: Context) {
      try {
        val request =
          PeriodicWorkRequestBuilder<NewEpisodesWorker>(6, TimeUnit.HOURS)
            .setConstraints(
              Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
          .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
      } catch (e: Exception) {
        Timber.e(e, "Failed to schedule NewEpisodesWorker")
      }
    }
  }
}
