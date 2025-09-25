package iad1tya.echo.music.ui.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import iad1tya.echo.music.R
import iad1tya.echo.music.service.SimpleMediaServiceHandler
import iad1tya.echo.music.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@UnstableApi
class BasicWidget : BaseAppWidget() {
    /**
     * Initialize given widgets to default state, where we launch Music on default click and hide
     * actions if service not running.
     */

    override fun defaultAppWidget(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )

        appWidgetView.setViewVisibility(
            R.id.media_titles,
            View.INVISIBLE,
        )
        appWidgetView.setImageViewResource(R.id.image, R.mipmap.ic_launcher_round)
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            R.drawable.play_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_next,
            R.drawable.next_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_prev,
            R.drawable.previous_widget,
        )

//        linkButtons(context, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(
        context: Context,
        handler: SimpleMediaServiceHandler,
        appWidgetIds: IntArray?,
    ) {
        Log.d("BasicWidget", "performUpdate")
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )
        val isPlaying = handler.player.isPlaying
        val song = runBlocking { handler.nowPlaying.first() }
        Log.w("BasicWidget", "performUpdate: isPlaying=$isPlaying, song=${song?.mediaMetadata?.title}")

        // Set the titles and artwork
        if (song?.mediaMetadata?.title.isNullOrEmpty() && song?.mediaMetadata?.artist.isNullOrEmpty()) {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.INVISIBLE,
            )
            // When no song is playing, show app logo in main image area
            appWidgetView.setImageViewResource(R.id.image, R.mipmap.ic_launcher_round)
        } else {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.VISIBLE,
            )
            appWidgetView.setTextViewText(R.id.title, song?.mediaMetadata?.title)
            appWidgetView.setTextViewText(
                R.id.text,
                song?.mediaMetadata?.artist,
            )
            
            // SMART: Only set placeholder if this is a new song, not just a play state change
            // Check if this is the same song as before by comparing with stored state
            val currentSongId = song?.mediaId
            val lastSongId = getLastSongId(context)
            
            if (currentSongId != lastSongId) {
                // New song - set placeholder that will be replaced by album art
                appWidgetView.setImageViewResource(R.id.image, R.mipmap.ic_launcher_round)
                Log.w("BasicWidget", "performUpdate: New song detected, setting placeholder")
                // Store the new song ID
                setLastSongId(context, currentSongId)
            } else {
                // Same song - don't touch the image, preserve existing album art
                Log.w("BasicWidget", "performUpdate: Same song, preserving existing image")
            }
            
            // If we have an artwork URI, try to load it immediately
            val artworkUri = song?.mediaMetadata?.artworkUri
            if (artworkUri != null) {
                Log.w("BasicWidget", "performUpdate: Found artwork URI, will be loaded: $artworkUri")
            } else {
                Log.w("BasicWidget", "performUpdate: No artwork URI found for: ${song?.mediaMetadata?.title}")
            }
        }
        // Set prev/next button drawables
        appWidgetView.setImageViewResource(
            R.id.button_next,
            R.drawable.next_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_prev,
            R.drawable.previous_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            if (!isPlaying) R.drawable.play_widget else R.drawable.pause_widget,
        )

        // Link actions buttons to intents
        linkButtons(context, appWidgetView)

        // Load the album cover async and push the update on completion
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    fun updateImage(
        context: Context,
        bitmap: Bitmap,
    ) {
        Log.w("BasicWidget", "updateImage: Setting album art bitmap (${bitmap.width}x${bitmap.height})")
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )
        appWidgetView.setImageViewBitmap(R.id.image, bitmap)
        pushUpdatePartially(context, appWidgetView)
        
        // Controlled: Single additional update after a short delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            pushUpdatePartially(context, appWidgetView)
        }, 200)
    }

    fun updatePlayingState(
        context: Context,
        isPlaying: Boolean,
    ) {
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )
        // ONLY update the play/pause button, don't touch the image
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            if (!isPlaying) R.drawable.play_widget else R.drawable.pause_widget,
        )
        pushUpdatePartially(context, appWidgetView)
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    @UnstableApi
    private fun linkButtons(
        context: Context,
        views: RemoteViews,
    ) {
        val action = Intent(context, MainActivity::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )
        views.setOnClickPendingIntent(R.id.clickable_area, pendingIntent)

        // Previous track
        pendingIntent = buildPendingIntent(context, ACTION_REWIND)
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, ACTION_TOGGLE_PAUSE)
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, ACTION_SKIP)
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    companion object {
        const val NAME: String = "basic_widget"
        const val ACTION_TOGGLE_PAUSE = "iad1tya.echo.music.action.TOGGLE_PAUSE"
        const val ACTION_REWIND = "iad1tya.echo.music.action.REWIND"
        const val ACTION_SKIP = "iad1tya.echo.music.action.SKIP"
        private var mInstance: BasicWidget? = null
        
        private const val PREFS_NAME = "widget_state"
        private const val KEY_LAST_SONG_ID = "last_song_id"
        
        private fun getLastSongId(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LAST_SONG_ID, null)
        }
        
        private fun setLastSongId(context: Context, songId: String?) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_SONG_ID, songId).apply()
        }

        val instance: BasicWidget
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = BasicWidget()
                }
                return mInstance!!
            }
    }
}