/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package echo.music.iad1tya.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.MainActivity
import echo.music.iad1tya.R
import echo.music.iad1tya.db.MusicDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetQueueItem(
  val index: Int,
  val title: String,
  val artist: String,
  val isCurrent: Boolean
)

@Singleton
class EchoMusicWidgetManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val database: MusicDatabase,
  private val playlistWidgetManager: PlaylistWidgetManager,
) {
  private val imageLoader
    get() = context.imageLoader

  // Cache for album art and extracted dynamic accent to avoid reloading
  private var cachedArtworkUri: String? = null
  private var cachedAlbumArt: Bitmap? = null
  private var cachedCircularAlbumArt: Bitmap? = null
  private var cachedRoundedAlbumArt: Bitmap? = null
  private var cachedBannerAlbumArt: Bitmap? = null
  private var cachedAccentColor: Int = 0xFFA855F7.toInt()

  suspend fun updateWidgets(
    title: String,
    artist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long = 0,
    currentPosition: Long = 0,
    isShuffle: Boolean = false,
    repeatMode: Int = 0,
    isMuted: Boolean = false,
    queueItems: List<WidgetQueueItem> = emptyList()
  ) {
    val appWidgetManager = AppWidgetManager.getInstance(context)

    // Use cached album art if URI hasn't changed, otherwise load new one
    val albumArt: Bitmap?
    val circularAlbumArt: Bitmap?
    val roundedAlbumArt: Bitmap?
    val bannerAlbumArt: Bitmap?
    val accentColor: Int

    if (artworkUri != null && artworkUri == cachedArtworkUri && cachedAlbumArt != null) {
      albumArt = cachedAlbumArt
      circularAlbumArt = cachedCircularAlbumArt
      roundedAlbumArt = cachedRoundedAlbumArt
      bannerAlbumArt = cachedBannerAlbumArt
      accentColor = cachedAccentColor
    } else {
      albumArt = artworkUri?.let { loadAlbumArt(it, 400) }
      circularAlbumArt = albumArt?.let { getCircularBitmap(it) }
      roundedAlbumArt = albumArt?.let { getRoundedCornerBitmap(it, 36f) }
      bannerAlbumArt = albumArt?.let { getRoundedCornerBitmap(it, 44f) }
      accentColor = extractAccentColor(albumArt)
      // Update cache
      cachedArtworkUri = artworkUri
      cachedAlbumArt = albumArt
      cachedCircularAlbumArt = circularAlbumArt
      cachedRoundedAlbumArt = roundedAlbumArt
      cachedBannerAlbumArt = bannerAlbumArt
      cachedAccentColor = accentColor
    }

    // Update main music player widgets
    val componentName = ComponentName(context, MusicWidgetReceiver::class.java)
    val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
    if (widgetIds.isNotEmpty()) {
      widgetIds.forEach { widgetId ->
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val views =
          createRemoteViewsForSize(
            options,
            title,
            artist,
            albumArt,
            isPlaying,
            isLiked,
            duration,
            currentPosition
          )
        appWidgetManager.updateAppWidget(widgetId, views)
      }
    }

    // Update turntable widgets
    val turntableComponentName = ComponentName(context, TurntableWidgetReceiver::class.java)
    val turntableWidgetIds = appWidgetManager.getAppWidgetIds(turntableComponentName)
    if (turntableWidgetIds.isNotEmpty()) {
      val turntableViews = createTurntableRemoteViews(circularAlbumArt, isPlaying, isLiked)
      turntableWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, turntableViews)
      }
    }

    // 1. Echo Modern Flow (4x2)
    val modernComponent = ComponentName(context, EchoModernWidgetReceiver::class.java)
    val modernWidgetIds = appWidgetManager.getAppWidgetIds(modernComponent)
    if (modernWidgetIds.isNotEmpty()) {
      val modernViews =
        createModernFlowRemoteViews(
          title,
          artist,
          roundedAlbumArt,
          isPlaying,
          isLiked,
          duration,
          currentPosition,
          accentColor
        )
      modernWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, modernViews)
      }
    }

    // 3. Echo Compact Glow (2x2)
    val squareComponent = ComponentName(context, EchoSquareWidgetReceiver::class.java)
    val squareWidgetIds = appWidgetManager.getAppWidgetIds(squareComponent)
    if (squareWidgetIds.isNotEmpty()) {
      val squareViews =
        createCompactGlowRemoteViews(
          title,
          artist,
          roundedAlbumArt,
          isPlaying,
          accentColor
        )
      squareWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, squareViews)
      }
    }

    // 4. Echo Showcase Card (4x4)
    val showcaseComponent = ComponentName(context, EchoShowcaseWidgetReceiver::class.java)
    val showcaseWidgetIds = appWidgetManager.getAppWidgetIds(showcaseComponent)
    if (showcaseWidgetIds.isNotEmpty()) {
      val showcaseViews =
        createShowcaseCardRemoteViews(
          title,
          artist,
          bannerAlbumArt,
          isPlaying,
          isLiked,
          duration,
          currentPosition,
          accentColor
        )
      showcaseWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, showcaseViews)
      }
    }

    // 5. Echo Slim Pill Strip (4x1)
    val slimComponent = ComponentName(context, EchoSlimWidgetReceiver::class.java)
    val slimWidgetIds = appWidgetManager.getAppWidgetIds(slimComponent)
    if (slimWidgetIds.isNotEmpty()) {
      val slimViews =
        createSlimStripRemoteViews(
          title,
          artist,
          roundedAlbumArt,
          isPlaying,
          duration,
          currentPosition,
          accentColor
        )
      slimWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, slimViews)
      }
    }

    // Vinyl Rotation Calculation (33 1/3 RPM = 1 full spin every 1.8 seconds)
    val vinylAngle = if (isPlaying && duration > 0) {
      ((currentPosition % 1800L).toFloat() / 1800f * 360f)
    } else 0f

    val vinylDisc = createVinylDiscBitmap(cachedAlbumArt, vinylAngle, 320, false)
    val vintageVinylDisc = createVinylDiscBitmap(cachedAlbumArt, vinylAngle, 260, true)

    // 6. Echo Vinyl Square (2x2 / 3x3)
    val vinylComponent = ComponentName(context, EchoVinylWidgetReceiver::class.java)
    val vinylWidgetIds = appWidgetManager.getAppWidgetIds(vinylComponent)
    if (vinylWidgetIds.isNotEmpty()) {
      val vinylViews =
        createVinylSquareRemoteViews(
          title,
          artist,
          vinylDisc,
          isPlaying,
          duration,
          currentPosition,
          accentColor,
          vinylAngle
        )
      vinylWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, vinylViews)
      }
    }

    // 8. Echo Vintage Turntable (4x2)
    val vintageComponent = ComponentName(context, EchoVintageWidgetReceiver::class.java)
    val vintageWidgetIds = appWidgetManager.getAppWidgetIds(vintageComponent)
    if (vintageWidgetIds.isNotEmpty()) {
      val vintageViews =
        createVintageTurntableRemoteViews(
          title,
          artist,
          vintageVinylDisc,
          isPlaying,
          duration,
          currentPosition,
          vinylAngle
        )
      vintageWidgetIds.forEach { widgetId ->
        appWidgetManager.updateAppWidget(widgetId, vintageViews)
      }
    }

    // 9. Echo Adaptive Player (2-in-1 Compact to Expanded Queue)
    val adaptiveComponent = ComponentName(context, EchoAdaptiveWidgetReceiver::class.java)
    val adaptiveWidgetIds = appWidgetManager.getAppWidgetIds(adaptiveComponent)
    if (adaptiveWidgetIds.isNotEmpty()) {
      AdaptiveQueueWidgetService.queueData = queueItems
      val adaptiveAlbumArt = albumArt?.let { getRoundedCornerBitmap(it, 28f) }
      adaptiveWidgetIds.forEach { widgetId ->
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)

        val compactViews = createAdaptiveCompactRemoteViews(
          title,
          artist,
          adaptiveAlbumArt,
          isPlaying,
          isShuffle,
          repeatMode
        )

        val expandedViews = createAdaptiveExpandedRemoteViews(
          widgetId,
          title,
          artist,
          adaptiveAlbumArt,
          isPlaying,
          isShuffle,
          repeatMode,
          queueItems
        )

        val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          RemoteViews(
            mapOf(
              SizeF(200f, 80f) to compactViews,
              SizeF(200f, 150f) to expandedViews
            )
          )
        } else {
          if (minHeight >= 130) expandedViews else compactViews
        }

        appWidgetManager.updateAppWidget(widgetId, finalViews)
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_queue_list)
      }
    }



    playlistWidgetManager.updateWidgets(
      title = title,
      artist = artist,
      artworkUri = artworkUri,
      isPlaying = isPlaying,
      isLiked = isLiked,
      duration = duration,
      currentPosition = currentPosition,
    )
  }

  private fun createRemoteViewsForSize(
    options: Bundle,
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long,
    currentPosition: Long
  ): RemoteViews {
    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

    // Determine widget size category
    // 2x2: approximately 110dp x 110dp (compact square)
    // 4x1: approximately 250dp x 40dp (wide single row)
    // Full: approximately 250dp x 110dp (default)
    return when {
      minWidth < 180 && minHeight < 100 -> {
        // 2x2 Compact - Only play button with album art
        createCompactSquareRemoteViews(albumArt, isPlaying)
      }
      minWidth >= 180 && minHeight < 100 -> {
        // 4x1 Wide - Single row with album art, song info, like and play buttons
        createCompactWideRemoteViews(title, artist, albumArt, isPlaying, isLiked)
      }
      else -> {
        // Full layout
        createRemoteViews(title, artist, albumArt, isPlaying, isLiked, duration, currentPosition)
      }
    }
  }

  private fun createRemoteViews(
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long = 0,
    currentPosition: Long = 0
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_music_player)

    // Set song info
    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    // Set album art with rounded corners
    if (albumArt != null) {
      val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
      views.setImageViewBitmap(R.id.widget_album_art, roundedAlbumArt)
    } else {
      views.setImageViewBitmap(R.id.widget_album_art, getRoundedDefaultIcon(48f))
    }

    // Set play/pause icon
    val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
    views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

    // Set like icon - using nav style (purple) for main widget
    val likeIcon =
      if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
    views.setImageViewResource(R.id.widget_like_button, likeIcon)

    // Set Progress Level
    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt()
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
    }

    // Set click intents
    views.setOnClickPendingIntent(R.id.widget_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())

    return views
  }

  private suspend fun loadAlbumArt(artworkUri: String, size: Int = 200): Bitmap? {
    return withContext(Dispatchers.IO) {
      try {
        val request =
          ImageRequest.Builder(context)
            .data(artworkUri)
            .size(size, size)
            .allowHardware(false)
            .crossfade(300)
            .build()
        val result = imageLoader.execute(request)
        result.image?.toBitmap()
      } catch (e: Exception) {
        null
      }
    }
  }

  private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
    // Ensure the bitmap is square for thumbnails
    val size = minOf(bitmap.width, bitmap.height)
    val xOffset = (bitmap.width - size) / 2
    val yOffset = (bitmap.height - size) / 2
    val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint =
      Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
      }
    val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

    if (squareBitmap != bitmap) {
      squareBitmap.recycle()
    }

    return output
  }

  private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
    val size = minOf(bitmap.width, bitmap.height)

    // First crop to square
    val xOffset = (bitmap.width - size) / 2
    val yOffset = (bitmap.height - size) / 2
    val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint =
      Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
      }
    val radius = size / 2f
    canvas.drawCircle(radius, radius, radius, paint)

    if (squareBitmap != bitmap) {
      squareBitmap.recycle()
    }
    return output
  }

  private fun createCompactSquareRemoteViews(albumArt: Bitmap?, isPlaying: Boolean): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_compact_square)

    // Set album art with rounded corners
    if (albumArt != null) {
      val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
      views.setImageViewBitmap(R.id.widget_compact_album_art, roundedAlbumArt)
    } else {
      views.setImageViewBitmap(R.id.widget_compact_album_art, getRoundedDefaultIcon(48f))
    }

    // Set play/pause icon - using low style icons
    val playPauseIcon =
      if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
    views.setImageViewResource(R.id.widget_compact_play_pause, playPauseIcon)

    // Set click intents
    views.setOnClickPendingIntent(R.id.widget_compact_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_compact_play_container, getPlayPauseIntent())

    return views
  }

  private fun createCompactWideRemoteViews(
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)

    // Set song info
    views.setTextViewText(R.id.widget_wide_song_title, title)
    views.setTextViewText(R.id.widget_wide_artist_name, artist)

    // Set album art with rounded corners (48f to match 12dp at ~4x density for 48dp view)
    if (albumArt != null) {
      val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 48f)
      views.setImageViewBitmap(R.id.widget_wide_album_art, roundedAlbumArt)
    } else {
      // Create rounded default icon
      views.setImageViewBitmap(R.id.widget_wide_album_art, getRoundedDefaultIcon(48f))
    }

    // Set play/pause icon - using low style icons
    val playPauseIcon =
      if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
    views.setImageViewResource(R.id.widget_wide_play_pause, playPauseIcon)

    // Set like icon - using navigation style (purple)
    val likeIcon =
      if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
    views.setImageViewResource(R.id.widget_wide_like_button, likeIcon)

    // Set click intents
    views.setOnClickPendingIntent(R.id.widget_wide_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_wide_play_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_wide_like_button, getLikeIntent())

    return views
  }

  private fun createTurntableRemoteViews(
    circularAlbumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_turntable)

    // Set circular album art - create circular default icon if no album art
    if (circularAlbumArt != null) {
      views.setImageViewBitmap(R.id.widget_turntable_album_art, circularAlbumArt)
    } else {
      // Load and make the default icon circular
      views.setImageViewBitmap(R.id.widget_turntable_album_art, getCircularDefaultIcon())
    }

    // Set play/pause icon - using secondary color icons for turntable
    val playPauseIcon =
      if (isPlaying) R.drawable.ic_widget_pause_secondary else R.drawable.ic_widget_play_secondary
    views.setImageViewResource(R.id.widget_turntable_play_pause, playPauseIcon)

    // Set click intents
    views.setOnClickPendingIntent(R.id.widget_turntable_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(
      R.id.widget_turntable_play_container,
      getTurntablePlayPauseIntent()
    )
    views.setOnClickPendingIntent(R.id.widget_turntable_prev_button, getTurntablePreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_turntable_next_button, getTurntableNextIntent())

    return views
  }

  private fun getCircularDefaultIcon(): Bitmap {
    // Load the custom turntable default art drawable and convert to bitmap
    val drawable = context.getDrawable(R.drawable.widget_turntable_default_art)!!
    val size = 300
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
  }

  private fun getRoundedDefaultIcon(cornerRadius: Float): Bitmap {
    // Get the launcher icon and make it rounded
    val drawable = context.packageManager.getApplicationIcon(context.packageName)
    val size = 300
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return getRoundedCornerBitmap(bitmap, cornerRadius)
  }

  private fun getOpenAppIntent(): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
    return PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getPlayPauseIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_PLAY_PAUSE
      }
    return PendingIntent.getBroadcast(
      context,
      1,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getLikeIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_LIKE
      }
    return PendingIntent.getBroadcast(
      context,
      2,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getTurntablePlayPauseIntent(): PendingIntent {
    val intent =
      Intent(context, TurntableWidgetReceiver::class.java).apply {
        action = TurntableWidgetReceiver.ACTION_TURNTABLE_PLAY_PAUSE
      }
    return PendingIntent.getBroadcast(
      context,
      3,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getTurntableNextIntent(): PendingIntent {
    val intent =
      Intent(context, TurntableWidgetReceiver::class.java).apply {
        action = TurntableWidgetReceiver.ACTION_TURNTABLE_NEXT
      }
    return PendingIntent.getBroadcast(
      context,
      4,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getTurntablePreviousIntent(): PendingIntent {
    val intent =
      Intent(context, TurntableWidgetReceiver::class.java).apply {
        action = TurntableWidgetReceiver.ACTION_TURNTABLE_PREVIOUS
      }
    return PendingIntent.getBroadcast(
      context,
      5,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getPreviousIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_PREVIOUS
      }
    return PendingIntent.getBroadcast(
      context,
      10,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getNextIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_NEXT
      }
    return PendingIntent.getBroadcast(
      context,
      11,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getLibraryIntent(): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        action = "echo.music.iad1tya.action.LIBRARY"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    return PendingIntent.getActivity(
      context,
      12,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getNowPlayingIntent(): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_NOW_PLAYING
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    return PendingIntent.getActivity(
      context,
      14,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getQueueIntent(): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_QUEUE
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    return PendingIntent.getActivity(
      context,
      15,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getSongOptionsIntent(): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_SONG_OPTIONS
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    return PendingIntent.getActivity(
      context,
      16,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getOutputSwitcherIntent(): PendingIntent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        val panelIntent =
          Intent(MainActivity.ACTION_MEDIA_OUTPUT).apply {
            putExtra(MainActivity.EXTRA_MEDIA_OUTPUT_PACKAGE_NAME, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
        if (context.packageManager.resolveActivity(panelIntent, 0) != null) {
          return PendingIntent.getActivity(
            context,
            13,
            panelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )
        }
      } catch (_: Exception) {}
    }
    val intent =
      Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_OUTPUT_SWITCHER
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    return PendingIntent.getActivity(
      context,
      13,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getLyricsIntent(): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_LYRICS
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    return PendingIntent.getActivity(
      context,
      17,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun isSystemInDarkMode(): Boolean {
    val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
  }

  private fun getShuffleIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_SHUFFLE
      }
    return PendingIntent.getBroadcast(
      context,
      901,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getRepeatIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_REPEAT
      }
    return PendingIntent.getBroadcast(
      context,
      905,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getMuteIntent(): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_MUTE
      }
    return PendingIntent.getBroadcast(
      context,
      907,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getSkipToQueueIndexIntent(queueIndex: Int): PendingIntent {
    val intent =
      Intent(context, MusicWidgetReceiver::class.java).apply {
        action = MusicWidgetReceiver.ACTION_SKIP_TO_QUEUE_ITEM
        putExtra(MusicWidgetReceiver.EXTRA_QUEUE_INDEX, queueIndex)
      }
    return PendingIntent.getBroadcast(
      context,
      910 + queueIndex,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun createVinylDiscBitmap(
    centerArt: Bitmap?,
    rotationDegrees: Float,
    discSize: Int = 300,
    hasShadow: Boolean = false
  ): Bitmap {
    val bitmap = Bitmap.createBitmap(discSize, discSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = discSize / 2f
    val radius = center - if (hasShadow) 14f else 8f

    if (hasShadow) {
      val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x30000000.toInt()
      }
      canvas.drawCircle(center + 2f, center + 4f, radius, shadowPaint)
    }

    canvas.save()
    canvas.rotate(rotationDegrees, center, center)

    // Outer Vinyl Record Disc (Glossy deep obsidian)
    val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF141517.toInt()
      style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, radius, discPaint)

    // Concentric Grooves
    val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.2f
      color = 0x22FFFFFF.toInt()
    }
    val minGroove = radius * 0.48f
    val maxGroove = radius * 0.94f
    var r = minGroove
    while (r < maxGroove) {
      canvas.drawCircle(center, center, r, groovePaint)
      r += (radius * 0.05f)
    }

    // Specular light sheen highlight sweep across vinyl
    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = SweepGradient(
        center, center,
        intArrayOf(0x00FFFFFF, 0x1EFFFFFF.toInt(), 0x00FFFFFF, 0x1EFFFFFF.toInt(), 0x00FFFFFF),
        floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
      )
      style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, radius, sheenPaint)

    // Center circular album artwork label
    val labelRadius = radius * 0.44f
    if (centerArt != null) {
      val circularArt = getCircularBitmap(centerArt)
      val src = Rect(0, 0, circularArt.width, circularArt.height)
      val dst = RectF(center - labelRadius, center - labelRadius, center + labelRadius, center + labelRadius)
      val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      canvas.drawBitmap(circularArt, src, dst, artPaint)
    } else {
      val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C3AED.toInt()
      }
      canvas.drawCircle(center, center, labelRadius, fallbackPaint)
    }

    // Outer center ring rim
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.5f
      color = 0x55FFFFFF.toInt()
    }
    canvas.drawCircle(center, center, labelRadius, rimPaint)

    // Center spindle hole
    val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = 0xFF0D0E10.toInt()
    }
    val holeBezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2f
      color = 0x99D1D5DB.toInt()
    }
    val holeRadius = radius * 0.075f
    canvas.drawCircle(center, center, holeRadius, holePaint)
    canvas.drawCircle(center, center, holeRadius, holeBezelPaint)

    canvas.restore()
    return bitmap
  }

  private fun extractAccentColor(bitmap: Bitmap?): Int {
    if (bitmap == null) return 0xFFA855F7.toInt()
    return try {
      val palette = Palette.from(bitmap).maximumColorCount(16).generate()
      palette.vibrantSwatch?.rgb
        ?: palette.lightVibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: 0xFFA855F7.toInt()
    } catch (e: Exception) {
      0xFFA855F7.toInt()
    }
  }

  private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
  }

  private fun createModernFlowRemoteViews(
    title: String,
    artist: String,
    roundedAlbumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long,
    currentPosition: Long,
    accentColor: Int
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_modern_flow)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    views.setImageViewBitmap(
      R.id.widget_album_art,
      roundedAlbumArt ?: getRoundedDefaultIcon(36f)
    )

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    val likeIcon = if (isLiked) R.drawable.ic_widget_heart_filled_clean else R.drawable.ic_widget_heart_clean
    views.setImageViewResource(R.id.widget_like_icon, likeIcon)

    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
      views.setTextViewText(R.id.widget_time_current, formatDuration(currentPosition))
      views.setTextViewText(R.id.widget_time_duration, formatDuration(duration))
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
      views.setTextViewText(R.id.widget_time_current, "0:00")
      views.setTextViewText(R.id.widget_time_duration, "0:00")
    }

    views.setOnClickPendingIntent(R.id.widget_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_cast_button, getOutputSwitcherIntent())
    views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())
    views.setOnClickPendingIntent(R.id.widget_queue_button, getQueueIntent())

    return views
  }

  private fun createDeviceConnectRemoteViews(
    title: String,
    artist: String,
    roundedAlbumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long,
    currentPosition: Long,
    accentColor: Int
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_device_connect)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)
    views.setTextViewText(R.id.widget_device_name, context.getString(R.string.widget_this_phone))

    views.setImageViewBitmap(
      R.id.widget_album_art,
      roundedAlbumArt ?: getRoundedDefaultIcon(36f)
    )

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    val likeIcon = if (isLiked) R.drawable.ic_widget_heart_filled_clean else R.drawable.ic_widget_heart_clean
    views.setImageViewResource(R.id.widget_like_icon, likeIcon)

    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
      views.setTextViewText(R.id.widget_time_current, formatDuration(currentPosition))
      views.setTextViewText(R.id.widget_time_duration, formatDuration(duration))
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
      views.setTextViewText(R.id.widget_time_current, "0:00")
      views.setTextViewText(R.id.widget_time_duration, "0:00")
    }

    views.setOnClickPendingIntent(R.id.widget_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_device_badge, getOutputSwitcherIntent())
    views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())
    views.setOnClickPendingIntent(R.id.widget_cast_button, getOutputSwitcherIntent())

    return views
  }

  private fun createCompactGlowRemoteViews(
    title: String,
    artist: String,
    roundedAlbumArt: Bitmap?,
    isPlaying: Boolean,
    accentColor: Int
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_compact_glow)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    views.setImageViewBitmap(
      R.id.widget_album_art,
      roundedAlbumArt ?: getRoundedDefaultIcon(36f)
    )

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    views.setOnClickPendingIntent(R.id.widget_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

    return views
  }

  private fun createShowcaseCardRemoteViews(
    title: String,
    artist: String,
    bannerAlbumArt: Bitmap?,
    isPlaying: Boolean,
    isLiked: Boolean,
    duration: Long,
    currentPosition: Long,
    accentColor: Int
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_showcase_card)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    views.setImageViewBitmap(
      R.id.widget_album_art,
      bannerAlbumArt ?: getRoundedDefaultIcon(44f)
    )


    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    val likeIcon = if (isLiked) R.drawable.ic_widget_heart_filled_clean else R.drawable.ic_widget_heart_clean
    views.setImageViewResource(R.id.widget_like_icon, likeIcon)

    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
      views.setTextViewText(R.id.widget_time_current, formatDuration(currentPosition))
      views.setTextViewText(R.id.widget_time_duration, formatDuration(duration))
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
      views.setTextViewText(R.id.widget_time_current, "0:00")
      views.setTextViewText(R.id.widget_time_duration, "0:00")
    }

    views.setOnClickPendingIntent(R.id.widget_soundwave_logo, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_cast_button, getOutputSwitcherIntent())
    views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())
    views.setOnClickPendingIntent(R.id.widget_options_button, getSongOptionsIntent())

    return views
  }

  private fun createSlimStripRemoteViews(
    title: String,
    artist: String,
    roundedAlbumArt: Bitmap?,
    isPlaying: Boolean,
    duration: Long,
    currentPosition: Long,
    accentColor: Int
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_slim_strip)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    views.setImageViewBitmap(
      R.id.widget_album_art,
      roundedAlbumArt ?: getRoundedDefaultIcon(32f)
    )

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
      views.setTextViewText(R.id.widget_time_current, formatDuration(currentPosition))
      views.setTextViewText(R.id.widget_time_duration, formatDuration(duration))
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
      views.setTextViewText(R.id.widget_time_current, "0:00")
      views.setTextViewText(R.id.widget_time_duration, "0:00")
    }

    views.setOnClickPendingIntent(R.id.widget_album_art, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())
    views.setOnClickPendingIntent(R.id.widget_cast_button, getOutputSwitcherIntent())

    return views
  }

  private fun createVinylSquareRemoteViews(
    title: String,
    artist: String,
    vinylDisc: Bitmap,
    isPlaying: Boolean,
    duration: Long,
    currentPosition: Long,
    accentColor: Int,
    vinylAngle: Float
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_vinyl_square)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)
    views.setImageViewBitmap(R.id.widget_vinyl_disc, vinylDisc)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      views.setFloat(R.id.widget_vinyl_disc, "setRotation", vinylAngle)
      views.setFloat(R.id.widget_tonearm, "setRotation", if (isPlaying) 0f else -18f)
    }

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
      views.setTextViewText(R.id.widget_time_current, formatDuration(currentPosition))
      views.setTextViewText(R.id.widget_time_duration, formatDuration(duration))
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
      views.setTextViewText(R.id.widget_time_current, "0:00")
      views.setTextViewText(R.id.widget_time_duration, "0:00")
    }

    views.setOnClickPendingIntent(R.id.widget_vinyl_container, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_vinyl_disc, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

    return views
  }

  private fun createAdaptiveCompactRemoteViews(
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_adaptive_compact)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    if (albumArt != null) {
      views.setImageViewBitmap(R.id.widget_album_art, albumArt)
      views.setViewVisibility(R.id.widget_album_art, View.VISIBLE)
      views.setViewVisibility(R.id.widget_placeholder_icon, View.GONE)
    } else {
      views.setViewVisibility(R.id.widget_album_art, View.GONE)
      views.setViewVisibility(R.id.widget_placeholder_icon, View.VISIBLE)
    }

    val shuffleIcon = if (isShuffle) R.drawable.ic_widget_shuffle_on else R.drawable.ic_widget_shuffle_off
    views.setImageViewResource(R.id.widget_shuffle_icon, shuffleIcon)
    views.setOnClickPendingIntent(R.id.widget_shuffle_button, getShuffleIntent())

    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause_icon, playIcon)
    views.setOnClickPendingIntent(R.id.widget_play_pause_button, getPlayPauseIntent())

    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

    val repeatIcon = when (repeatMode) {
      Player.REPEAT_MODE_ALL -> R.drawable.ic_widget_repeat_all
      Player.REPEAT_MODE_ONE -> R.drawable.ic_widget_repeat_one
      else -> R.drawable.ic_widget_repeat_off
    }
    views.setImageViewResource(R.id.widget_repeat_icon, repeatIcon)
    views.setOnClickPendingIntent(R.id.widget_repeat_button, getRepeatIntent())

    views.setOnClickPendingIntent(R.id.widget_adaptive_root, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_album_art_container, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())

    return views
  }

  private fun createAdaptiveExpandedRemoteViews(
    widgetId: Int,
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    queueItems: List<WidgetQueueItem>
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_adaptive_expanded)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)

    if (albumArt != null) {
      views.setImageViewBitmap(R.id.widget_album_art, albumArt)
      views.setViewVisibility(R.id.widget_album_art, View.VISIBLE)
      views.setViewVisibility(R.id.widget_placeholder_icon, View.GONE)
    } else {
      views.setViewVisibility(R.id.widget_album_art, View.GONE)
      views.setViewVisibility(R.id.widget_placeholder_icon, View.VISIBLE)
    }

    val shuffleIcon = if (isShuffle) R.drawable.ic_widget_shuffle_on else R.drawable.ic_widget_shuffle_off
    views.setImageViewResource(R.id.widget_shuffle_icon, shuffleIcon)
    views.setOnClickPendingIntent(R.id.widget_shuffle_button, getShuffleIntent())

    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_bold_white else R.drawable.ic_widget_play_bold_white
    views.setImageViewResource(R.id.widget_play_pause_icon, playIcon)
    views.setOnClickPendingIntent(R.id.widget_play_pause_button, getPlayPauseIntent())

    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

    val repeatIcon = when (repeatMode) {
      Player.REPEAT_MODE_ALL -> R.drawable.ic_widget_repeat_all
      Player.REPEAT_MODE_ONE -> R.drawable.ic_widget_repeat_one
      else -> R.drawable.ic_widget_repeat_off
    }
    views.setImageViewResource(R.id.widget_repeat_icon, repeatIcon)
    views.setOnClickPendingIntent(R.id.widget_repeat_button, getRepeatIntent())

    views.setOnClickPendingIntent(R.id.widget_album_art_container, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())

    // Configure scrollable ListView with AdaptiveQueueWidgetService
    val serviceIntent = Intent(context, AdaptiveQueueWidgetService::class.java).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
      data = Uri.parse("widget://adaptive/queue/$widgetId")
    }
    views.setRemoteAdapter(R.id.widget_queue_list, serviceIntent)
    views.setEmptyView(R.id.widget_queue_list, R.id.widget_queue_empty)

    // Set PendingIntent template for ListView item clicks
    val clickIntent = Intent(context, MusicWidgetReceiver::class.java).apply {
      action = MusicWidgetReceiver.ACTION_SKIP_TO_QUEUE_ITEM
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val clickPendingIntent = PendingIntent.getBroadcast(
      context,
      920,
      clickIntent,
      flags
    )
    views.setPendingIntentTemplate(R.id.widget_queue_list, clickPendingIntent)

    return views
  }




  private fun createVintageTurntableRemoteViews(
    title: String,
    artist: String,
    vintageVinylDisc: Bitmap,
    isPlaying: Boolean,
    duration: Long,
    currentPosition: Long,
    vinylAngle: Float
  ): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_vintage_turntable)

    views.setTextViewText(R.id.widget_song_title, title)
    views.setTextViewText(R.id.widget_artist_name, artist)
    views.setImageViewBitmap(R.id.widget_vinyl_disc, vintageVinylDisc)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      views.setFloat(R.id.widget_vinyl_disc, "setRotation", vinylAngle)
      views.setFloat(R.id.widget_tonearm, "setRotation", if (isPlaying) 0f else -18f)
    }

    val playIcon = if (isPlaying) R.drawable.ic_widget_pause_brown else R.drawable.ic_widget_play_brown
    views.setImageViewResource(R.id.widget_play_pause, playIcon)

    if (duration > 0) {
      val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt().coerceIn(0, 10000)
      views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
      views.setTextViewText(R.id.widget_time_current, formatDuration(currentPosition))
      views.setTextViewText(R.id.widget_time_duration, formatDuration(duration))
    } else {
      views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
      views.setTextViewText(R.id.widget_time_current, "0:00")
      views.setTextViewText(R.id.widget_time_duration, "0:00")
    }

    views.setOnClickPendingIntent(R.id.widget_vinyl_disc, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_song_title, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_artist_name, getNowPlayingIntent())
    views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
    views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
    views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

    return views
  }
}
