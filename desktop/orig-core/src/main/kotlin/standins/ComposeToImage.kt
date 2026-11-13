package echo.music.iad1tya.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.text.Layout
import echo.music.iad1tya.ui.component.LyricsBackgroundStyle
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.RenderingHints
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The "share lyrics as image" card, laid out like the Android one: 2160x2160, solid / blurred-cover / gradient background,
 * cover + title + artist on top, lyrics shrunk to fit, app name at the bottom.
 * shortcut: the gradient takes its two colours from a simple saturation/lightness pick, not Android's Palette; the footer logo is a drawn note, not the launcher icon.
 */
object ComposeToImage {
  suspend fun createLyricsImage(
    context: Context,
    coverArtUrl: String?,
    songTitle: String,
    artistName: String,
    lyrics: String,
    width: Int,
    height: Int,
    backgroundColor: Int? = null,
    backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
    textColor: Int? = null,
    secondaryTextColor: Int? = null,
    lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
  ): Bitmap = withContext(Dispatchers.Default) {
    val size = 2160
    val bg = Color(backgroundColor ?: 0xFF121212.toInt(), true)
    val main = Color(textColor ?: 0xFFFFFFFF.toInt(), true)
    val second = Color(secondaryTextColor ?: 0xB3FFFFFF.toInt(), true)
    val cover: BufferedImage? = coverArtUrl?.let { runCatching { ImageIO.read(ByteArrayInputStream(java.net.URL(it).readBytes())) }.getOrNull() }

    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

    // background
    when {
      backgroundStyle == LyricsBackgroundStyle.BLUR && cover != null -> {
        g.color = Color.BLACK; g.fillRect(0, 0, size, size)
        g.drawImage(blurred(cover), 0, 0, size, size, null)
        g.color = Color(0, 0, 0, 0x4D); g.fillRect(0, 0, size, size)
      }
      backgroundStyle == LyricsBackgroundStyle.GRADIENT && cover != null -> {
        val (vibrant, dark) = vibrantColors(cover, bg)
        g.paint = java.awt.GradientPaint(0f, 0f, vibrant, size.toFloat(), size.toFloat(), dark)
        g.fillRect(0, 0, size, size)
      }
      else -> { g.color = bg; g.fillRect(0, 0, size, size) }
    }

    val scale = size / 340f
    g.color = Color(main.red, main.green, main.blue, (255 * 0.09).toInt())
    g.stroke = java.awt.BasicStroke(1f * scale)
    g.draw(RoundRectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat(), 40f * scale, 40f * scale))

    val pad = 28f * scale
    val coverSize = 64f * scale
    // cover
    if (cover != null) {
      val rect = RoundRectangle2D.Float(pad, pad, coverSize, coverSize, 6f * scale, 6f * scale)
      val old = g.clip
      g.clip = rect
      g.drawImage(cover, pad.toInt(), pad.toInt(), coverSize.toInt(), coverSize.toInt(), null)
      g.clip = old
      g.color = Color(main.red, main.green, main.blue, (255 * 0.16).toInt())
      g.draw(rect)
    }
    // title + artist, one line each, shortened with an ellipsis
    val textX = pad + coverSize + 16f * scale
    val textW = (size - textX - pad).toInt()
    val titleFont = Font("SansSerif", Font.BOLD, (20f * scale).toInt())
    val artistFont = Font("SansSerif", Font.PLAIN, (16f * scale).toInt())
    val tfm = g.getFontMetrics(titleFont); val afm = g.getFontMetrics(artistFont)
    val headerH = tfm.height + afm.height + 2f * scale
    val top = pad + coverSize / 2f - headerH / 2f
    g.font = titleFont; g.color = main
    g.drawString(ellipsize(songTitle, tfm, textW), textX, top + tfm.ascent)
    g.font = artistFont; g.color = second
    g.drawString(ellipsize(artistName, afm, textW), textX, top + tfm.height + 2f * scale + afm.ascent)

    // footer: round logo + app name
    val logoBox = 22f * scale
    val footerY = size - pad - logoBox
    g.color = second
    g.fill(Ellipse2D.Float(pad, footerY, logoBox, logoBox))
    val noteFont = Font("SansSerif", Font.BOLD, (15f * scale).toInt())
    g.font = noteFont; g.color = bg.let { if (backgroundStyle == LyricsBackgroundStyle.SOLID) it else Color(0, 0, 0, 200) }
    val nfm = g.fontMetrics
    g.drawString("♪", pad + (logoBox - nfm.stringWidth("♪")) / 2f, footerY + (logoBox - nfm.height) / 2f + nfm.ascent)
    val nameFont = Font("SansSerif", Font.BOLD, (14f * scale).toInt())
    g.font = nameFont; g.color = second
    val nameFm = g.fontMetrics
    g.drawString("Echo Music", pad + logoBox + 8f * scale, footerY + (logoBox - nameFm.height) / 2f + nameFm.ascent)

    // lyrics: biggest size that fits between the header and the footer
    val lyricsTop = pad + coverSize + 12f * scale
    val lyricsH = (footerY - 12f * scale) - lyricsTop
    val lyricsW = (size - pad * 2).toInt()
    var fontSize = 50f * scale
    val minSize = 13f * scale
    var lines: List<String>
    var fm: FontMetrics
    while (true) {
      fm = g.getFontMetrics(Font("SansSerif", Font.BOLD, fontSize.toInt()))
      lines = wrap(lyrics, fm, lyricsW)
      if (lines.size * fm.height * 1.2f <= lyricsH || fontSize <= minSize) break
      fontSize -= scale
    }
    g.font = Font("SansSerif", Font.BOLD, fontSize.toInt()); g.color = main
    val lineH = fm.height * 1.2f
    val total = lines.size * lineH
    var y = if (total < lyricsH) lyricsTop + (lyricsH - total) / 2f else lyricsTop
    for (line in lines) {
      if (y + lineH > lyricsTop + lyricsH + lineH * 0.5f) break
      val lw = fm.stringWidth(line)
      val x = when (lyricsAlignment) {
        Layout.Alignment.ALIGN_CENTER -> pad + (lyricsW - lw) / 2f
        Layout.Alignment.ALIGN_OPPOSITE -> pad + lyricsW - lw
        else -> pad
      }
      g.drawString(line, x, y + fm.ascent)
      y += lineH
    }
    g.dispose()
    Bitmap(img)
  }

  private fun ellipsize(text: String, fm: FontMetrics, maxW: Int): String {
    if (fm.stringWidth(text) <= maxW) return text
    var s = text
    while (s.isNotEmpty() && fm.stringWidth("$s…") > maxW) s = s.dropLast(1)
    return "$s…"
  }

  /** Words wrapped to the width; line breaks in the lyrics are kept. */
  private fun wrap(text: String, fm: FontMetrics, maxW: Int): List<String> {
    val out = ArrayList<String>()
    for (para in text.split("\n")) {
      if (para.isBlank()) { out += ""; continue }
      var line = ""
      for (word in para.split(" ")) {
        val next = if (line.isEmpty()) word else "$line $word"
        if (fm.stringWidth(next) > maxW && line.isNotEmpty()) { out += line; line = word } else line = next
      }
      out += line
    }
    return out
  }

  /** The cover shrunk to 216px, box-blurred, ready to be stretched over the card. */
  private fun blurred(src: BufferedImage): BufferedImage {
    val n = 216
    val small = BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB)
    val sg = small.createGraphics()
    sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    sg.drawImage(src, 0, 0, n, n, null); sg.dispose()
    var px = small.getRGB(0, 0, n, n, null, 0, n)
    repeat(3) { px = boxBlur(px, n, 6) }
    small.setRGB(0, 0, n, n, px, 0, n)
    return small
  }

  private fun boxBlur(p: IntArray, n: Int, r: Int): IntArray {
    fun pass(src: IntArray, horizontal: Boolean): IntArray {
      val dst = IntArray(src.size)
      for (a in 0 until n) for (b in 0 until n) {
        var rs = 0; var gs = 0; var bs = 0; var c = 0
        for (k in -r..r) {
          val bb = b + k
          if (bb < 0 || bb >= n) continue
          val v = if (horizontal) src[a * n + bb] else src[bb * n + a]
          rs += (v shr 16) and 255; gs += (v shr 8) and 255; bs += v and 255; c++
        }
        val o = (0xFF shl 24) or ((rs / c) shl 16) or ((gs / c) shl 8) or (bs / c)
        if (horizontal) dst[a * n + b] = o else dst[b * n + a] = o
      }
      return dst
    }
    return pass(pass(p, true), false)
  }

  /** A bright and a dark saturated colour from the cover (stand-in for Palette's vibrant / dark vibrant). */
  private fun vibrantColors(src: BufferedImage, fallback: Color): Pair<Color, Color> {
    val n = 48
    val small = BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB)
    val sg = small.createGraphics(); sg.drawImage(src, 0, 0, n, n, null); sg.dispose()
    val hsb = FloatArray(3)
    fun pick(minL: Float, maxL: Float): Color {
      var r = 0L; var g = 0L; var b = 0L; var c = 0
      for (y in 0 until n) for (x in 0 until n) {
        val v = small.getRGB(x, y)
        Color.RGBtoHSB((v shr 16) and 255, (v shr 8) and 255, v and 255, hsb)
        val l = hsb[2] * (1f - hsb[1] / 2f)
        if (hsb[1] >= 0.35f && l in minL..maxL) { r += (v shr 16) and 255; g += (v shr 8) and 255; b += v and 255; c++ }
      }
      return if (c == 0) fallback else Color((r / c).toInt(), (g / c).toInt(), (b / c).toInt())
    }
    return pick(0.3f, 0.7f) to pick(0.08f, 0.3f)
  }

  /** Saves the PNG in Pictures\Echo Music, copies it to the clipboard and shows it in Explorer (there is no Android share sheet here). */
  fun saveBitmapAsFile(context: Context, bitmap: Bitmap, filename: String): Uri {
    val dir = File(System.getProperty("user.home"), "Pictures/Echo Music").also { it.mkdirs() }
    val f = File(dir, "$filename.png")
    ImageIO.write(bitmap.image, "png", f)
    runCatching {
      val image = bitmap.image
      java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(object : Transferable {
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(fl: DataFlavor) = fl == DataFlavor.imageFlavor
        override fun getTransferData(fl: DataFlavor): Any = image
      }, null)
    }
    runCatching { ProcessBuilder("explorer.exe", "/select,${f.absolutePath}").start() }
    return Uri.fromFile(f)
  }
}
