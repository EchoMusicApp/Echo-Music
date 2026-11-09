package echo.music.iad1tya.utils.potoken

// shortcut: the web PoToken needs a WebView; desktop gets its stream via yt-dlp instead.
class PoTokenResult(val playerRequestPoToken: String, val streamingDataPoToken: String)
class PoTokenGenerator {
  fun initialize() {}
  fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? = null
}
