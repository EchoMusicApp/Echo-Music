/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package echo.music.iad1tya.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import echo.music.iad1tya.playback.MusicService

open class MusicWidgetReceiver : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    // Only trigger update through MusicService if it's already running
    // This prevents BackgroundServiceStartNotAllowedException on Android 14+
    if (MusicService.isRunning) {
      val intent = Intent(context, MusicService::class.java).apply { action = ACTION_UPDATE_WIDGET }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        // Service might be restricted in background
      }
    }
    // If service is not running, widget shows default layout until user opens app
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    // Trigger widget update when size changes
    if (MusicService.isRunning) {
      val intent = Intent(context, MusicService::class.java).apply { action = ACTION_UPDATE_WIDGET }
      try {
        context.startService(intent)
      } catch (e: Exception) {
        // Service might be restricted in background
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)

    when (intent.action) {
      ACTION_PLAY_PAUSE,
      ACTION_LIKE,
      ACTION_NEXT,
      ACTION_PREVIOUS,
      ACTION_SHUFFLE,
      ACTION_REPEAT,
      ACTION_MUTE,
      ACTION_SKIP_TO_QUEUE_ITEM -> {
        // User interactions from widget buttons can start the service
        // Android allows starting FGS from widget PendingIntent clicks
        val serviceIntent =
          Intent(context, MusicService::class.java).apply {
            action = intent.action
            putExtras(intent)
          }
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startService(serviceIntent)
          } else {
            context.startService(serviceIntent)
          }
        } catch (e: Exception) {
          // Service might be restricted in background
        }
      }
    }
  }

  companion object {
    const val ACTION_PLAY_PAUSE = "com.music.echo.widget.PLAY_PAUSE"
    const val ACTION_LIKE = "com.music.echo.widget.LIKE"
    const val ACTION_NEXT = "com.music.echo.widget.NEXT"
    const val ACTION_PREVIOUS = "com.music.echo.widget.PREVIOUS"
    const val ACTION_SHUFFLE = "com.music.echo.widget.SHUFFLE"
    const val ACTION_REPEAT = "com.music.echo.widget.REPEAT"
    const val ACTION_MUTE = "com.music.echo.widget.MUTE"
    const val ACTION_SKIP_TO_QUEUE_ITEM = "com.music.echo.widget.SKIP_TO_QUEUE_ITEM"
    const val ACTION_UPDATE_WIDGET = "com.music.echo.widget.UPDATE_WIDGET"
    const val EXTRA_QUEUE_INDEX = "extra_queue_index"
  }
}
