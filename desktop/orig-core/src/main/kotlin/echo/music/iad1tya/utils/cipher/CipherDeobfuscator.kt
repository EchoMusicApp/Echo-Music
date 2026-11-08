package echo.music.iad1tya.utils.cipher

import android.content.Context

// shortcut: the Android original runs YouTube's player JS in a WebView; desktop will get stream URLs from yt-dlp instead, so this only holds the context.
object CipherDeobfuscator {
  lateinit var appContext: Context
}
