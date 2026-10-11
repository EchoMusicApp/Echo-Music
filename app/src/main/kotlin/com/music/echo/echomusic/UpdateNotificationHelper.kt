package echo.music.iad1tya.echomusic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import echo.music.iad1tya.R

object UpdateNotificationHelper {
  private const val CHANNEL_ID = "updates"
  private const val LEGACY_NOTIFICATION_ID = 1001
  private const val NOTIFICATION_ID = 1003
  private const val UPDATE_PENDING_INTENT_REQUEST_CODE = 20703

  fun clearLegacyUpdateNotification(context: Context) {
    context.getSystemService(NotificationManager::class.java).cancel(LEGACY_NOTIFICATION_ID)
  }

  fun showUpdateNotification(context: Context, versionName: String) {
    val nm = context.getSystemService(NotificationManager::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.app_updates_title),
          NotificationManager.IMPORTANCE_DEFAULT
        )
      nm.createNotificationChannel(channel)
    }

    nm.cancel(LEGACY_NOTIFICATION_ID)

    val intent = Intent(context, echo.music.iad1tya.MainActivity::class.java).apply {
      action = echo.music.iad1tya.MainActivity.ACTION_UPDATE_APP
      data = android.net.Uri.parse("pattukal://update/${System.currentTimeMillis()}")
      setClassName(context, "echo.music.iad1tya.MainActivity")
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    // Give the notification an explicit app back stack and a fresh identity so an
    // older PendingIntent cannot keep routing taps to the previous website target.
    val pending =
      TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(intent)
        getPendingIntent(UPDATE_PENDING_INTENT_REQUEST_CODE, flags)
      }

    val notif =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_nobg)
        .setContentTitle(context.getString(R.string.update_available_title))
        .setContentText(versionName)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .build()

    if (
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
    ) {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif)
    }
  }
}
