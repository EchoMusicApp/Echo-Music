package iad1tya.echo.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import iad1tya.echo.music.R
import iad1tya.echo.music.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicWidgetProvider : AppWidgetProvider() {
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        const val ACTION_PLAY_PAUSE = "iad1tya.echo.music.widget.PLAY_PAUSE"
        const val ACTION_PREVIOUS = "iad1tya.echo.music.widget.PREVIOUS"
        const val ACTION_NEXT = "iad1tya.echo.music.widget.NEXT"
        const val ACTION_UPDATE_WIDGET = "iad1tya.echo.music.widget.UPDATE_WIDGET"
        
        fun updateWidget(
            context: Context,
            songTitle: String?,
            artistName: String?,
            albumArtUrl: String?,
            isPlaying: Boolean
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val provider = MusicWidgetProvider()
            provider.updateWidgets(
                context,
                appWidgetManager,
                appWidgetIds,
                songTitle,
                artistName,
                albumArtUrl,
                isPlaying
            )
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds, null, null, null, false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(context.packageName)
                    putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    ))
                }
                context.sendBroadcast(mediaButtonIntent)
            }
            ACTION_PREVIOUS -> {
                val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(context.packageName)
                    putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    ))
                }
                context.sendBroadcast(mediaButtonIntent)
            }
            ACTION_NEXT -> {
                val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    setPackage(context.packageName)
                    putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                    ))
                }
                context.sendBroadcast(mediaButtonIntent)
            }
            ACTION_UPDATE_WIDGET -> {
                val songTitle = intent.getStringExtra("songTitle")
                val artistName = intent.getStringExtra("artistName")
                val albumArtUrl = intent.getStringExtra("albumArtUrl")
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MusicWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                updateWidgets(
                    context,
                    appWidgetManager,
                    appWidgetIds,
                    songTitle,
                    artistName,
                    albumArtUrl,
                    isPlaying
                )
            }
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        songTitle: String?,
        artistName: String?,
        albumArtUrl: String?,
        isPlaying: Boolean
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)
            
            // Set song title
            views.setTextViewText(
                R.id.widget_song_title,
                songTitle ?: "No song playing"
            )
            
            // Set artist name
            views.setTextViewText(
                R.id.widget_artist_name,
                artistName ?: "Unknown artist"
            )
            
            // Set play/pause icon
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.pause else R.drawable.play
            )
            
            // Set default album art first
            if (albumArtUrl == null) {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.echo_logo)
            }
            
            // Set up play/pause button click
            val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
            
            // Set up widget click to open app
            val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                4,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_song_info, openAppPendingIntent)
            
            // Update widget immediately with button handlers
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Load album art asynchronously
            if (albumArtUrl != null) {
                scope.launch {
                    try {
                        val imageLoader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(albumArtUrl)
                            .size(256, 256)
                            .build()
                        
                        val result = imageLoader.execute(request)
                        val bitmap = result.image?.toBitmap()
                        
                        if (bitmap != null) {
                            val updatedViews = RemoteViews(context.packageName, R.layout.widget_music_player)
                            
                            // Re-set all the data
                            updatedViews.setTextViewText(R.id.widget_song_title, songTitle ?: "No song playing")
                            updatedViews.setTextViewText(R.id.widget_artist_name, artistName ?: "Unknown artist")
                            updatedViews.setImageViewResource(
                                R.id.widget_play_pause,
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            )
                            updatedViews.setImageViewBitmap(R.id.widget_album_art, bitmap)
                            
                            // Re-attach click handlers
                            updatedViews.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
                            updatedViews.setOnClickPendingIntent(R.id.widget_song_info, openAppPendingIntent)
                            
                            appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        job.cancel()
    }
}
