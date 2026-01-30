package iad1tya.echo.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import iad1tya.echo.music.R
import iad1tya.echo.music.playback.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MusicWidgetProvider : AppWidgetProvider() {
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val color = 0xff424242.toInt()
        val paint = Paint()
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)
        
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val srcRect = Rect(left, top, left + size, top + size)
        
        canvas.drawBitmap(bitmap, srcRect, rect, paint)
        
        return output
    }

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
            
            // Set up play/pause button click
            val playPauseIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    2,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    2,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
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

            // Set up previous button click
            val previousIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PREVIOUS
            }
            val previousPendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    3,
                    previousIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    3,
                    previousIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            views.setOnClickPendingIntent(R.id.widget_previous, previousPendingIntent)

            // Set up next button click
            val nextIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_NEXT
            }
            val nextPendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    5,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    5,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)
            
            // First update widget with default logo
            views.setImageViewResource(R.id.widget_album_art, R.drawable.echo_logo)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Load album art asynchronously if available
            if (!albumArtUrl.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        android.util.Log.d("MusicWidget", "Loading album art from: $albumArtUrl")
                        
                        // Download image from URL
                        val url = URL(albumArtUrl)
                        val connection = url.openConnection()
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.connect()
                        
                        val inputStream = connection.getInputStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            android.util.Log.d("MusicWidget", "Bitmap loaded successfully: ${bitmap.width}x${bitmap.height}")
                            
                            // Scale bitmap if too large
                            val scaledBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
                                Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                            } else {
                                bitmap
                            }
                            
                            val circularBitmap = getCircularBitmap(scaledBitmap)
                            
                            withContext(Dispatchers.Main) {
                                views.setImageViewBitmap(R.id.widget_album_art, circularBitmap)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                                android.util.Log.d("MusicWidget", "Widget updated with album art")
                            }
                            
                            if (scaledBitmap != bitmap) {
                                bitmap.recycle()
                            }
                        } else {
                            android.util.Log.e("MusicWidget", "Failed to decode bitmap")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicWidget", "Failed to load album art: ${e.message}", e)
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
