package echo.music.iad1tya.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat

/** Muted, looping video (the animated cover) drawn from a second VLC player's frame callback. */
@Composable
fun DesktopLoopingVideo(url: String, isPlaying: Boolean, modifier: Modifier = Modifier) {
  var frame by remember(url) { mutableStateOf<ImageBitmap?>(null) }
  val player = remember(url) { mutableStateOf<MediaPlayer?>(null) }
  DisposableEffect(url) {
    if (url.isBlank()) return@DisposableEffect onDispose {}
    var factory: MediaPlayerFactory? = null
    var mp: MediaPlayer? = null
    try {
      Bundled.configureVlc()
      factory = MediaPlayerFactory("--no-audio", "--input-repeat=65535", "--quiet")
      mp = factory.mediaPlayers().newMediaPlayer()
      var w = 0
      var h = 0
      val format = BufferFormatCallback { sw, sh ->
        w = sw; h = sh
        RV32BufferFormat(sw, sh)
      }
      val render = RenderCallback { _, buffers, bf ->
        val bw = bf.width; val bh = bf.height
        if (bw > 0 && bh > 0) {
          val bytes = ByteArray(bw * bh * 4)
          buffers[0].order(ByteOrder.nativeOrder()).get(bytes, 0, minOf(bytes.size, buffers[0].remaining()))
          buffers[0].rewind()
          val bmp = Bitmap()
          bmp.allocPixels(ImageInfo(bw, bh, ColorType.BGRA_8888, ColorAlphaType.OPAQUE))
          bmp.installPixels(bytes)
          frame = org.jetbrains.skia.Image.makeFromBitmap(bmp).toComposeImageBitmap()
        }
      }
      mp.videoSurface().set(factory.videoSurfaces().newVideoSurface(format, render, true))
      mp.controls().setRepeat(true)
      mp.media().play(url)
      player.value = mp
    } catch (e: Throwable) {
      println("animated cover failed: $e")
    }
    onDispose {
      player.value = null
      val m = mp; val f = factory
      Thread { runCatching { m?.release() }; runCatching { f?.release() } }.start()
    }
  }
  LaunchedEffect(isPlaying, player.value) { player.value?.controls()?.setPause(!isPlaying) }
  val alpha by animateFloatAsState(if (frame != null) 1f else 0f)
  frame?.let { Image(it, null, modifier = modifier.alpha(alpha), contentScale = ContentScale.Crop) }
}
