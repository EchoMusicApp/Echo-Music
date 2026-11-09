package echo.music.iad1tya.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.Layout
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// shortcut: plain Java2D card (background, title, artist, wrapped lyrics); no cover art and no blurred/gradient styles.
object ComposeToImage {
  suspend fun createLyricsImage(
    context: Context,
    coverArtUrl: String?,
    songTitle: String,
    artistName: String,
    lyrics: String,
    width: Int,
    height: Int,
    backgroundColor: Int,
    backgroundStyle: Any? = null,
    textColor: Int,
    secondaryTextColor: Int,
    lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
  ): Bitmap {
    val w = width.coerceIn(400, 2000)
    val h = height.coerceIn(400, 3000)
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.color = Color(backgroundColor, true); g.fillRect(0, 0, w, h)
    val pad = w / 12
    g.color = Color(textColor, true); g.font = Font("SansSerif", Font.BOLD, w / 18)
    var y = pad + g.fontMetrics.ascent
    g.drawString(songTitle, pad, y)
    g.color = Color(secondaryTextColor, true); g.font = Font("SansSerif", Font.PLAIN, w / 26)
    y += g.fontMetrics.height + 8
    g.drawString(artistName, pad, y)
    g.color = Color(textColor, true); g.font = Font("SansSerif", Font.BOLD, w / 22)
    val fm = g.fontMetrics
    y += fm.height * 2
    for (para in lyrics.lines()) {
      var line = ""
      for (word in para.split(" ")) {
        val next = if (line.isEmpty()) word else "$line $word"
        if (fm.stringWidth(next) > w - 2 * pad && line.isNotEmpty()) { y = draw(g, line, w, pad, y, lyricsAlignment); line = word } else line = next
      }
      y = draw(g, line, w, pad, y, lyricsAlignment)
      if (y > h - pad) break
    }
    g.dispose()
    return Bitmap(img)
  }

  private fun draw(g: java.awt.Graphics2D, line: String, w: Int, pad: Int, y: Int, a: Layout.Alignment): Int {
    val lw = g.fontMetrics.stringWidth(line)
    val x = when (a) { Layout.Alignment.ALIGN_CENTER -> (w - lw) / 2; Layout.Alignment.ALIGN_OPPOSITE -> w - pad - lw; else -> pad }
    g.drawString(line, x, y)
    return y + g.fontMetrics.height
  }

  fun saveBitmapAsFile(context: Context, bitmap: Bitmap, filename: String): Uri {
    val f = File(context.cacheDir, "$filename.png")
    ImageIO.write(bitmap.image, "png", f)
    return Uri.fromFile(f)
  }
}
