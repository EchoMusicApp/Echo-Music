package echo.music.iad1tya.ui.utils

import java.net.URI
import java.util.Locale

internal fun isTrustedHttpsUrl(
  url: String?,
  trustedHost: String,
): Boolean {
  val uri = runCatching { url?.let(::URI) }.getOrNull() ?: return false
  val host = uri.host?.lowercase(Locale.ROOT) ?: return false
  val expectedHost = trustedHost.lowercase(Locale.ROOT)
  return uri.scheme.equals("https", ignoreCase = true) &&
    (host == expectedHost || host.endsWith(".$expectedHost"))
}
