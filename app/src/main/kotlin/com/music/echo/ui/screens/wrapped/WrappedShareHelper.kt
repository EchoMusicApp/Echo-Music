package com.music.echo.ui.screens.wrapped

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import echo.music.iad1tya.utils.ComposeToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WrappedShareHelper {

  suspend fun shareWrappedSummary(
    context: Context,
    data: WrappedData,
    isStoryFormat: Boolean = true
  ) = withContext(Dispatchers.IO) {
    val width = 1080
    val height = 1920

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // 1. Base Deep Obsidian Background
    val bgPaint = Paint().apply {
      isAntiAlias = true
      color = 0xFF08090E.toInt()
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // 2. Rich Aurora Blooms (Multi-center luminous radials)
    val bloomPaint = Paint().apply { isAntiAlias = true }

    // Top-Right Electric Violet bloom
    bloomPaint.shader = RadialGradient(
      width * 0.82f,
      height * 0.16f,
      width * 0.75f,
      intArrayOf(0xAA7C3AED.toInt(), 0x446D28D9.toInt(), 0x00000000),
      floatArrayOf(0f, 0.45f, 1f),
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bloomPaint)

    // Mid-Left Cyan/Emerald bloom
    bloomPaint.shader = RadialGradient(
      width * 0.12f,
      height * 0.50f,
      width * 0.70f,
      intArrayOf(0x880D9488.toInt(), 0x33059669.toInt(), 0x00000000),
      floatArrayOf(0f, 0.5f, 1f),
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bloomPaint)

    // Bottom-Right Rose bloom
    bloomPaint.shader = RadialGradient(
      width * 0.85f,
      height * 0.82f,
      width * 0.70f,
      intArrayOf(0x88DB2777.toInt(), 0x33BE185D.toInt(), 0x00000000),
      floatArrayOf(0f, 0.5f, 1f),
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bloomPaint)

    // 3. Subtle Nothing OS Dot Grid Matrix
    val dotPaint = Paint().apply {
      isAntiAlias = true
      color = 0x12FFFFFF
    }
    val gridStep = 48f
    var gx = 48f
    while (gx < width) {
      var gy = 48f
      while (gy < height) {
        canvas.drawCircle(gx, gy, 1.5f, dotPaint)
        gy += gridStep
      }
      gx += gridStep
    }

    // 4. Outer Glass Border Frame
    val frameBorderPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 2f
      color = 0x22FFFFFF
    }
    canvas.drawRoundRect(RectF(32f, 32f, width - 32f, height - 32f), 40f, 40f, frameBorderPaint)

    // 5. Header: Monospace Branding & Kinetic Title
    val badgeDotPaint = Paint().apply {
      isAntiAlias = true
      color = 0xFF34D399.toInt()
    }
    canvas.drawCircle(72f, 110f, 7f, badgeDotPaint)

    val badgeTextPaint = Paint().apply {
      isAntiAlias = true
      color = 0xFF34D399.toInt()
      textSize = 24f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.drawText("ECHO MUSIC // 2026 WRAPPED", 90f, 118f, badgeTextPaint)

    val headerTitlePaint = Paint().apply {
      isAntiAlias = true
      color = 0xFFFFFFFF.toInt()
      textSize = 66f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("YOUR YEAR IN SOUND", 64f, 195f, headerTitlePaint)

    val headerSubPaint = Paint().apply {
      isAntiAlias = true
      color = 0x99FFFFFF.toInt()
      textSize = 26f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    canvas.drawText("Every beat, replay, and obsession that defined your year.", 64f, 238f, headerSubPaint)

    // Card common styling paints
    val cardFillPaint = Paint().apply {
      isAntiAlias = true
      color = 0xCC111424.toInt() // Frosted deep obsidian
    }
    val cardBorderPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = 0x33FFFFFF
    }
    val cardTagPaint = Paint().apply {
      isAntiAlias = true
      textSize = 24f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    // 6. Fetch Album Artwork for Top Song
    var coverArtBitmap: Bitmap? = null
    val coverUrl = data.topSong?.thumbnailUrl
    if (!coverUrl.isNullOrBlank()) {
      try {
        val loader = context.imageLoader
        val req = ImageRequest.Builder(context)
          .data(coverUrl)
          .size(600)
          .allowHardware(false)
          .build()
        coverArtBitmap = loader.execute(req).image?.toBitmap()
      } catch (_: Exception) {}
    }

    // 7. Hero Top Track Card
    val songCardRect = RectF(60f, 280f, width - 60f, 660f)
    canvas.drawRoundRect(songCardRect, 32f, 32f, cardFillPaint)
    canvas.drawRoundRect(songCardRect, 32f, 32f, cardBorderPaint)

    cardTagPaint.color = 0xFFF472B6.toInt()
    canvas.drawText("★  TOP TRACK OF THE YEAR", 92f, 335f, cardTagPaint)

    // Render Cover Art with rounded corners
    val artLeft = 92f
    val artTop = 365f
    val artSize = 250f
    val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

    if (coverArtBitmap != null) {
      val shader = BitmapShader(coverArtBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
      val matrix = Matrix()
      val scaleX = artSize / coverArtBitmap.width
      val scaleY = artSize / coverArtBitmap.height
      matrix.setScale(scaleX, scaleY)
      matrix.postTranslate(artLeft, artTop)
      shader.setLocalMatrix(matrix)

      val artPaint = Paint().apply {
        isAntiAlias = true
        this.shader = shader
      }
      canvas.drawRoundRect(artRect, 28f, 28f, artPaint)
    } else {
      val placeholderPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFF1E1B4B.toInt()
      }
      canvas.drawRoundRect(artRect, 28f, 28f, placeholderPaint)
    }

    // Song details beside artwork
    val textX = artLeft + artSize + 36f
    val songTitlePaint = Paint().apply {
      isAntiAlias = true
      color = 0xFFFFFFFF.toInt()
      textSize = 44f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rawTitle = data.topSong?.title ?: "No Track"
    val displayTitle = if (rawTitle.length > 20) rawTitle.take(18) + "…" else rawTitle
    canvas.drawText(displayTitle, textX, artTop + 65f, songTitlePaint)

    val songArtistPaint = Paint().apply {
      isAntiAlias = true
      color = 0xB3FFFFFF.toInt()
      textSize = 32f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val rawArtist = data.topSong?.artists?.joinToString(", ") { it.name } ?: "Unknown Artist"
    val displayArtist = if (rawArtist.length > 22) rawArtist.take(20) + "…" else rawArtist
    canvas.drawText(displayArtist, textX, artTop + 125f, songArtistPaint)

    // Stream count pill
    val playCount = data.topSongStats?.songCountListened ?: 1
    val countPillRect = RectF(textX, artTop + 175f, textX + 260f, artTop + 230f)
    val pillBgPaint = Paint().apply {
      isAntiAlias = true
      color = 0x2610B981
    }
    val pillBorderPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = 0x5510B981
    }
    val pillTextPaint = Paint().apply {
      isAntiAlias = true
      color = 0xFF34D399.toInt()
      textSize = 24f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
    }
    canvas.drawRoundRect(countPillRect, 24f, 24f, pillBgPaint)
    canvas.drawRoundRect(countPillRect, 24f, 24f, pillBorderPaint)
    canvas.drawText("Played $playCount times", countPillRect.centerX(), countPillRect.centerY() + 8f, pillTextPaint)

    // 8. Side-by-Side Bento Grid: Total Minutes & Top Artist
    val statY = 690f
    val statH = 220f
    val halfW = (width - 120f - 24f) / 2f

    // 8A. Minutes Box
    val minRect = RectF(60f, statY, 60f + halfW, statY + statH)
    canvas.drawRoundRect(minRect, 28f, 28f, cardFillPaint)
    canvas.drawRoundRect(minRect, 28f, 28f, cardBorderPaint)

    cardTagPaint.color = 0xFF34D399.toInt()
    canvas.drawText("TOTAL MINUTES", minRect.left + 32f, minRect.top + 50f, cardTagPaint)

    val bigNumPaint = Paint().apply {
      isAntiAlias = true
      color = 0xFFFFFFFF.toInt()
      textSize = 68f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("${data.totalMinutes}", minRect.left + 32f, minRect.top + 130f, bigNumPaint)

    val minSubPaint = Paint().apply {
      isAntiAlias = true
      color = 0x99FFFFFF.toInt()
      textSize = 24f
    }
    val hours = (data.totalMinutes / 60f).let { String.format("%.1f", it) }
    canvas.drawText("≈ $hours hours listened", minRect.left + 32f, minRect.top + 180f, minSubPaint)

    // 8B. Top Artist Box
    val artistRect = RectF(minRect.right + 24f, statY, width - 60f, statY + statH)
    canvas.drawRoundRect(artistRect, 28f, 28f, cardFillPaint)
    canvas.drawRoundRect(artistRect, 28f, 28f, cardBorderPaint)

    cardTagPaint.color = 0xFFFFD54F.toInt()
    canvas.drawText("TOP ARTIST", artistRect.left + 32f, artistRect.top + 50f, cardTagPaint)

    val artistName = data.topArtist?.title ?: "Various"
    val displayTopArt = if (artistName.length > 14) artistName.take(12) + "…" else artistName
    val artistTitlePaint = Paint().apply {
      isAntiAlias = true
      color = 0xFFFFFFFF.toInt()
      textSize = 48f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText(displayTopArt, artistRect.left + 32f, artistRect.top + 125f, artistTitlePaint)
    canvas.drawText("Most listened creator", artistRect.left + 32f, artistRect.top + 180f, minSubPaint)

    // 9. Top 5 Tracks Countdown Bento
    val top5Y = 940f
    val top5H = 460f
    val top5Rect = RectF(60f, top5Y, width - 60f, top5Y + top5H)
    canvas.drawRoundRect(top5Rect, 32f, 32f, cardFillPaint)
    canvas.drawRoundRect(top5Rect, 32f, 32f, cardBorderPaint)

    cardTagPaint.color = 0xFFA78BFA.toInt()
    canvas.drawText("YOUR TOP 5 TRACKS", 92f, top5Y + 54f, cardTagPaint)

    val rankNumPaint = Paint().apply {
      isAntiAlias = true
      textSize = 30f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val rowTitlePaint = Paint().apply {
      isAntiAlias = true
      color = 0xFFFFFFFF.toInt()
      textSize = 30f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rowArtistPaint = Paint().apply {
      isAntiAlias = true
      color = 0x88FFFFFF.toInt()
      textSize = 24f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val songs = data.top5Songs.take(5)
    var rowY = top5Y + 115f
    val rowSpacing = 70f
    for (i in 0 until 5) {
      val s = songs.getOrNull(i)
      val rankStr = String.format("%02d", i + 1)
      if (i == 0) {
        rankNumPaint.color = 0xFFFFD54F.toInt()
      } else {
        rankNumPaint.color = 0x66FFFFFF
      }
      canvas.drawText(rankStr, 92f, rowY, rankNumPaint)

      if (s != null) {
        val sTitle = if (s.title.length > 24) s.title.take(22) + "…" else s.title
        canvas.drawText(sTitle, 160f, rowY, rowTitlePaint)

        val sArtist = s.artists.joinToString(", ") { it.name }
        val dArtist = if (sArtist.length > 26) sArtist.take(24) + "…" else sArtist
        canvas.drawText(dArtist, width - 92f - rowArtistPaint.measureText(dArtist), rowY, rowArtistPaint)
      } else {
        canvas.drawText("—", 160f, rowY, rowArtistPaint)
      }
      rowY += rowSpacing
    }

    // 10. Musical Persona Holographic Card
    val personaY = 1430f
    val personaH = 210f
    val personaRect = RectF(60f, personaY, width - 60f, personaY + personaH)
    canvas.drawRoundRect(personaRect, 32f, 32f, cardFillPaint)

    // Holographic Sweep Gradient Border
    val sweepBorder = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 2f
      shader = SweepGradient(
        personaRect.centerX(),
        personaRect.centerY(),
        intArrayOf(
          0xFF5EEAD4.toInt(),
          0xFFA78BFA.toInt(),
          0xFFF472B6.toInt(),
          0xFFFFD54F.toInt(),
          0xFF5EEAD4.toInt()
        ),
        null
      )
    }
    canvas.drawRoundRect(personaRect, 32f, 32f, sweepBorder)

    cardTagPaint.color = 0xFF5EEAD4.toInt()
    canvas.drawText("SONIC IDENTITY ARCHETYPE", 92f, personaY + 54f, cardTagPaint)

    val personaTitlePaint = Paint().apply {
      isAntiAlias = true
      color = 0xFFFFFFFF.toInt()
      textSize = 42f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("★  ${data.dominantVibe.uppercase()}", 92f, personaY + 115f, personaTitlePaint)

    val personaDescPaint = Paint().apply {
      isAntiAlias = true
      color = 0xB3FFFFFF.toInt()
      textSize = 24f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val quote = when (data.dominantVibe) {
      "Musical Nomad" -> "You cross sonic borders effortlessly. Your taste spans worlds and horizons."
      "Certified Audiophile" -> "Music is your sanctuary, fuel, and frequency—listened with true intent."
      "Infinite Playlist Curator" -> "Always uncovering diamonds before the crowd catches on."
      else -> "Moving with the rhythm, deeply connected to whatever moves your spirit."
    }
    canvas.drawText(quote, 92f, personaY + 165f, personaDescPaint)

    // 11. Minimal Footer Watermark
    val footerPaint = Paint().apply {
      isAntiAlias = true
      color = 0x66FFFFFF
      textSize = 22f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText("CRAFTED WITH ECHO MUSIC • 2026 REPLAY", width / 2f, 1820f, footerPaint)

    // Save Bitmap & Launch Share Intent
    try {
      val uri = try {
        ComposeToImage.saveBitmapAsFile(
          context = context,
          bitmap = bitmap,
          fileName = "Echo_Wrapped_${data.yearLabel}_${System.currentTimeMillis()}"
        )
      } catch (e: Exception) {
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
          "Here is my ${data.yearLabel} Wrapped on Echo Music! 🎵\nTop Song: ${data.topSong?.title ?: "Various"}\nPersona: ${data.dominantVibe}"
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
