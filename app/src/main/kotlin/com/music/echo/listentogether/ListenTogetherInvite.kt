package echo.music.iad1tya.listentogether

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

internal object ListenTogetherInvite {
  private const val INVITE_HOST = "metroserverx.meowery.eu"
  private val roomCode = Regex("[A-Za-z0-9]{8}")

  fun link(code: String): String = "https://$INVITE_HOST/listen?code=$code"

  fun roomCodeFromQr(payload: String): String? {
    val value = payload.trim()
    if (value.length > 512) return null
    normalizeCode(value)?.let {
      return it
    }
    val uri =
      try {
        URI(value)
      } catch (_: URISyntaxException) {
        return null
      }
    if (uri.userInfo != null || uri.port != -1) return null
    val segments = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
    val pathCode =
      when {
        uri.scheme.equals("https", ignoreCase = true) &&
          uri.host.equals(INVITE_HOST, ignoreCase = true) &&
          segments.firstOrNull() == "listen" &&
          segments.size <= 2 -> segments.getOrNull(1)
        uri.scheme.equals("echomusic", ignoreCase = true) &&
          uri.host.equals("listen", ignoreCase = true) &&
          segments.isEmpty() -> null
        else -> return null
      }
    val queryCodes =
      uri.query.orEmpty().split('&').mapNotNull {
        val entry = it.split('=', limit = 2)
        entry.getOrNull(1)?.takeIf { entry.first() == "code" || entry.first() == "room" }
      }
    if (queryCodes.size > 1) return null
    return normalizeCode(queryCodes.singleOrNull() ?: pathCode.orEmpty())
  }

  private fun normalizeCode(value: String): String? =
    value.takeIf(roomCode::matches)?.uppercase(Locale.ROOT)
}
