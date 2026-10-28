package com.music.echo.ui.screens.wrapped

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import echo.music.iad1tya.R
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.utils.ComposeToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Song?.artistsText: String
  get() = this?.artists?.joinToString(", ") { it.name } ?: "Various Artists"

/**
 * Authentic Spotify Wrapped colorway themes for recap posters.
 */
enum class WrappedPosterTheme(
  val displayName: String,
  val composeBg: Color,
  val composeAccent: Color,
  val composeHeadline: Color,
  val composeSecondary: Color,
  val bgInt: Int,
  val headlineInt: Int,
  val accentInt: Int,
  val secondaryInt: Int
) {
  VIOLET_MINT(
    displayName = "Neon Violet",
    composeBg = Color(0xFF2810C0),
    composeAccent = Color(0xFF00F5D4),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFD8B4FE),
    bgInt = 0xFF2810C0.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFF00F5D4.toInt(),
    secondaryInt = 0xFFD8B4FE.toInt()
  ),
  OBSIDIAN_GREEN(
    displayName = "Spotify Obsidian",
    composeBg = Color(0xFF121212),
    composeAccent = Color(0xFF1DB954),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFA7A7A7),
    bgInt = 0xFF121212.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFF1DB954.toInt(),
    secondaryInt = 0xFFA7A7A7.toInt()
  ),
  FIRE_RED(
    displayName = "Crimson Gold",
    composeBg = Color(0xFFC41E3A),
    composeAccent = Color(0xFFFFDF00),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFFECACA),
    bgInt = 0xFFC41E3A.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFFFFDF00.toInt(),
    secondaryInt = 0xFFFECACA.toInt()
  ),
  COBALT_BLUE(
    displayName = "Electric Cobalt",
    composeBg = Color(0xFF1E3A8A),
    composeAccent = Color(0xFF38BDF8),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFBFDBFE),
    bgInt = 0xFF1E3A8A.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFF38BDF8.toInt(),
    secondaryInt = 0xFFBFDBFE.toInt()
  ),
  SUNSET_ORANGE(
    displayName = "Sunset Tangerine",
    composeBg = Color(0xFFC2410C),
    composeAccent = Color(0xFFFFFBEB),
    composeHeadline = Color(0xFFFFFFFF),
    composeSecondary = Color(0xFFFED7AA),
    bgInt = 0xFFC2410C.toInt(),
    headlineInt = 0xFFFFFFFF.toInt(),
    accentInt = 0xFFFFFBEB.toInt(),
    secondaryInt = 0xFFFED7AA.toInt()
  )
}

object WrappedShareHelper {

  private fun Paint.truncate(text: String, maxWidth: Float): String {
    if (measureText(text) <= maxWidth) return text
    var len = text.length
    while (len > 0 && measureText(text.take(len) + "…") > maxWidth) {
      len--
    }
    return if (len > 0) text.take(len) + "…" else ""
  }

  private fun drawRoundedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    rect: RectF,
    cornerRadius: Float
  ) {
    val scale = maxOf(rect.width() / bitmap.width.toFloat(), rect.height() / bitmap.height.toFloat())
    val scaledWidth = bitmap.width * scale
    val scaledHeight = bitmap.height * scale
    val dx = rect.left + (rect.width() - scaledWidth) / 2f
    val dy = rect.top + (rect.height() - scaledHeight) / 2f

    val matrix = Matrix().apply {
      setScale(scale, scale)
      postTranslate(dx, dy)
    }
    val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
      setLocalMatrix(matrix)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.shader = shader
      isFilterBitmap = true
    }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
  }

  suspend fun shareWrappedSummary(
    context: Context,
    data: WrappedData,
    posterTheme: WrappedPosterTheme = WrappedPosterTheme.VIOLET_MINT,
    isStoryFormat: Boolean = true
  ) = withContext(Dispatchers.IO) {
    val width = 1080
    val height = 1920

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // 1. Full-Bleed Saturated Spotify Poster Canvas (Zero bento boxes)
    val bgPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.bgInt
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Margin Insets (Safe Story margins: 80px left/right, starts at Y=110px)
    val marginX = 80f
    val contentWidth = width - (marginX * 2f) // 920f
    val colWidth = 430f
    val colLeftX = marginX
    val colRightX = marginX + contentWidth - colWidth // 570f

    // Common Paints
    val headlinePaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val accentPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val secondaryPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.secondaryInt
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val dividerPaint = Paint().apply {
      isAntiAlias = true
      strokeWidth = 1.5f
      color = 0x24FFFFFF.toInt()
    }

    // 2. Header: Echo Music Logo + Wordmark + 2026 WRAPPED
    try {
      val logoDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_nobg)
      if (logoDrawable != null) {
        val logoSize = 56
        val logoBmp = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(logoBmp)
        logoDrawable.setBounds(0, 0, logoSize, logoSize)
        logoDrawable.setTint(posterTheme.headlineInt)
        logoDrawable.draw(logoCanvas)
        canvas.drawBitmap(logoBmp, marginX, 115f, null)
      }
    } catch (_: Exception) {}

    val wordmarkPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 38f
      letterSpacing = -0.04f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("Echo Music", marginX + 70f, 156f, wordmarkPaint)

    val yearPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      textSize = 28f
      textAlign = Paint.Align.RIGHT
      letterSpacing = 0.14f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("${data.yearLabel} WRAPPED", width - marginX, 156f, yearPaint)

    // 3. Editorial Section Headline
    val tagPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      textSize = 22f
      letterSpacing = 0.18f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("YOUR YEAR IN SOUND", marginX, 230f, tagPaint)

    val titlePaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 54f
      letterSpacing = -0.04f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("MY TOP TRACKS & ARTISTS", marginX, 292f, titlePaint)

    // 4. Hero Split: Top Track (Left) & Top Artist (Right)
    val heroY = 340f
    val heroArtSize = 145f

    // Load artwork bitmaps (robust Coil loading with cache policy fallback)
    suspend fun loadArtworkBitmap(url: String?): Bitmap? {
      if (url.isNullOrBlank()) return null
      return try {
        val req = ImageRequest.Builder(context)
          .data(url)
          .size(400, 400)
          .allowHardware(false)
          .build()
        var result = context.imageLoader.execute(req)
        if (result is ErrorResult) {
          result = context.imageLoader.execute(
            req.newBuilder().networkCachePolicy(CachePolicy.READ_ONLY).build()
          )
        }
        if (result is SuccessResult) {
          result.image.toBitmap()
        } else null
      } catch (_: Exception) {
        null
      }
    }

    val trackBitmap = loadArtworkBitmap(data.topSong?.thumbnailUrl)
    val artistBitmap = loadArtworkBitmap(data.topArtist?.thumbnailUrl)

    val heroLabelPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      textSize = 19f
      letterSpacing = 0.12f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val heroTitlePaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 32f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val heroSubPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.secondaryInt
      textSize = 23f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Left Hero: Top Track
    val trackRect = RectF(colLeftX, heroY, colLeftX + heroArtSize, heroY + heroArtSize)
    if (trackBitmap != null) {
      drawRoundedBitmap(canvas, trackBitmap, trackRect, 14f)
    } else {
      val fallbackPaint = Paint().apply {
        isAntiAlias = true
        color = 0x22FFFFFF.toInt()
      }
      canvas.drawRoundRect(trackRect, 14f, 14f, fallbackPaint)
      try {
        val musicDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.music_note)
        if (musicDrawable != null) {
          val iconSize = 64
          val iconLeft = (trackRect.left + (heroArtSize - iconSize) / 2f).toInt()
          val iconTop = (trackRect.top + (heroArtSize - iconSize) / 2f).toInt()
          musicDrawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
          musicDrawable.setTint(posterTheme.headlineInt)
          musicDrawable.draw(canvas)
        }
      } catch (_: Exception) {}
    }

    val trackInfoX = colLeftX + heroArtSize + 20f
    val trackTextWidth = colWidth - heroArtSize - 20f
    canvas.drawText("TOP TRACK", trackInfoX, heroY + 28f, heroLabelPaint)
    val sTitle = heroTitlePaint.truncate(data.topSong?.title ?: "Various", trackTextWidth)
    canvas.drawText(sTitle, trackInfoX, heroY + 66f, heroTitlePaint)
    val sArtists = heroSubPaint.truncate(data.topSong?.artistsText ?: "Various Artists", trackTextWidth)
    canvas.drawText(sArtists, trackInfoX, heroY + 98f, heroSubPaint)
    val playCount = data.topSongStats?.songCountListened ?: 12
    canvas.drawText("Played $playCount times", trackInfoX, heroY + 126f, heroSubPaint)

    // Right Hero: Top Artist
    val artistRect = RectF(colRightX, heroY, colRightX + heroArtSize, heroY + heroArtSize)
    if (artistBitmap != null) {
      drawRoundedBitmap(canvas, artistBitmap, artistRect, 14f)
    } else {
      val fallbackPaint = Paint().apply {
        isAntiAlias = true
        color = 0x22FFFFFF.toInt()
      }
      canvas.drawRoundRect(artistRect, 14f, 14f, fallbackPaint)
      try {
        val personDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.person)
        if (personDrawable != null) {
          val iconSize = 64
          val iconLeft = (artistRect.left + (heroArtSize - iconSize) / 2f).toInt()
          val iconTop = (artistRect.top + (heroArtSize - iconSize) / 2f).toInt()
          personDrawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
          personDrawable.setTint(posterTheme.headlineInt)
          personDrawable.draw(canvas)
        }
      } catch (_: Exception) {}
    }

    val artistInfoX = colRightX + heroArtSize + 20f
    val artistTextWidth = colWidth - heroArtSize - 20f
    canvas.drawText("TOP ARTIST", artistInfoX, heroY + 28f, heroLabelPaint)
    val aTitle = heroTitlePaint.truncate(data.topArtist?.title ?: "Various", artistTextWidth)
    canvas.drawText(aTitle, artistInfoX, heroY + 66f, heroTitlePaint)
    canvas.drawText("#1 In Heavy Rotation", artistInfoX, heroY + 98f, heroSubPaint)
    canvas.drawText("${data.uniqueArtistsCount} artists explored", artistInfoX, heroY + 126f, heroSubPaint)

    // Divider Line
    canvas.drawLine(marginX, 505f, width - marginX, 505f, dividerPaint)

    // 5. Two-Column Countdown: Top 5 Songs (Left) & Top 5 Artists (Right)
    val colHeaderPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      textSize = 24f
      letterSpacing = 0.16f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("MY TOP SONGS", colLeftX, 555f, colHeaderPaint)
    canvas.drawText("MY TOP ARTISTS", colRightX, 555f, colHeaderPaint)

    val rowStartOY = 620f
    val rowStep = 115f
    val colContentWidth = colWidth - 55f

    val rank01Paint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      textSize = 34f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rankOtherPaint = Paint().apply {
      isAntiAlias = true
      color = 0x88FFFFFF.toInt()
      textSize = 34f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val rowTitlePaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 31f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val rowSubPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.secondaryInt
      textSize = 22f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Render 5 Songs
    data.top5Songs.take(5).forEachIndexed { i, song ->
      val itemY = rowStartOY + i * rowStep
      val rank = "0${i + 1}"
      val rPaint = if (i == 0) rank01Paint else rankOtherPaint
      canvas.drawText(rank, colLeftX, itemY + 12f, rPaint)

      val sName = rowTitlePaint.truncate(song.title, colContentWidth)
      canvas.drawText(sName, colLeftX + 55f, itemY - 6f, rowTitlePaint)

      val sArtist = rowSubPaint.truncate(song.artistsText ?: "Various Artists", colContentWidth)
      canvas.drawText(sArtist, colLeftX + 55f, itemY + 28f, rowSubPaint)
    }

    // Render 5 Artists
    data.top5Artists.take(5).forEachIndexed { i, artist ->
      val itemY = rowStartOY + i * rowStep
      val rank = "0${i + 1}"
      val rPaint = if (i == 0) rank01Paint else rankOtherPaint
      canvas.drawText(rank, colRightX, itemY + 12f, rPaint)

      val aName = rowTitlePaint.truncate(artist.title, colContentWidth)
      canvas.drawText(aName, colRightX + 55f, itemY - 6f, rowTitlePaint)

      val sub = if (i == 0) "Top Rotation Artist" else "Featured Creator"
      canvas.drawText(sub, colRightX + 55f, itemY + 28f, rowSubPaint)
    }

    // Divider Line
    canvas.drawLine(marginX, 1220f, width - marginX, 1220f, dividerPaint)

    // 6. Typographic Stats Dual Showcase (Minutes & Sonic Persona)
    val statsY = 1270f

    // Left: Total Minutes
    canvas.drawText("TOTAL MINUTES", colLeftX, statsY, colHeaderPaint)
    val statNumberPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 92f
      letterSpacing = -0.04f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("${data.totalMinutes}", colLeftX, statsY + 90f, statNumberPaint)
    val hoursVal = String.format(java.util.Locale.US, "%.1f", data.totalMinutes / 60f)
    canvas.drawText("$hoursVal hours of uninterrupted soundtrack", colLeftX, statsY + 138f, rowSubPaint)

    // Right: Sonic Persona
    canvas.drawText("SONIC PERSONA", colRightX, statsY, colHeaderPaint)
    val personaTitlePaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 46f
      letterSpacing = -0.02f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(data.dominantVibe.uppercase(), colRightX, statsY + 74f, personaTitlePaint)

    val quote = when (data.dominantVibe) {
      "Musical Nomad" -> "Your taste spans worlds, moods, and eras."
      "Certified Audiophile" -> "Music listened with pure intent."
      "Infinite Playlist Curator" -> "Uncovering diamonds before the crowd."
      else -> "Deeply connected to the frequency."
    }
    canvas.drawText(quote, colRightX, statsY + 126f, rowSubPaint)

    // Divider Line
    canvas.drawLine(marginX, 1450f, width - marginX, 1450f, dividerPaint)

    // 7. Trio Secondary Stats
    val trioY = 1515f
    val statTrioPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.headlineInt
      textSize = 24f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val statTrioAccentPaint = Paint().apply {
      isAntiAlias = true
      color = posterTheme.accentInt
      textSize = 24f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("${data.uniqueSongsCount} SONGS PLAYED", marginX, trioY, statTrioPaint)
    canvas.drawText("${data.uniqueArtistsCount} ARTISTS DISCOVERED", marginX + 330f, trioY, statTrioPaint)
    canvas.drawText("ECHO MUSIC", marginX + 710f, trioY, statTrioAccentPaint)

    // 8. Watermark Footer
    val footerPaint = Paint().apply {
      isAntiAlias = true
      color = 0x88FFFFFF.toInt()
      textSize = 21f
      textAlign = Paint.Align.CENTER
      letterSpacing = 0.16f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(
      "CRAFTED WITH ECHO MUSIC • ${data.yearLabel} REPLAY • echomusic.app/wrapped",
      width / 2f,
      1750f,
      footerPaint
    )

    // Save Bitmap & Launch Share Intent
    try {
      val uri = try {
        ComposeToImage.saveBitmapAsFile(
          context = context,
          bitmap = bitmap,
          fileName = "Echo_Wrapped_${data.yearLabel}_${System.currentTimeMillis()}"
        )
      } catch (_: Exception) {
        val cachePath = java.io.File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = java.io.File(cachePath, "Echo_Wrapped_${System.currentTimeMillis()}.png")
        java.io.FileOutputStream(imageFile).use { out ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", imageFile)
      }

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
          Intent.EXTRA_TEXT,
          "Here is my ${data.yearLabel} Wrapped on Echo Music! 🎵\nTop Song: ${data.topSong?.title ?: "Various"}\nTop Artist: ${data.topArtist?.title ?: "Various"}\nPersona: ${data.dominantVibe}"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(intent, "Share Echo Wrapped").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
