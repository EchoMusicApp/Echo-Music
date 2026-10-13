package echo.music.iad1tya.utils

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun validOnlinePlaylistId(playlistId: String?): String? =
  playlistId?.takeIf {
    // Navigation's StringType parses the literal "null" as a null argument.
    it.isNotBlank() && it.trim() != "null"
  }

fun onlinePlaylistIdFromUrl(url: String): String? {
  val playlistId =
    runCatching {
        URI(url.trim())
          .rawQuery
          ?.split('&')
          ?.firstOrNull { it.substringBefore('=') == "list" }
          ?.substringAfter('=', "")
          ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
      }
      .getOrNull()
  return validOnlinePlaylistId(playlistId)
}

fun onlinePlaylistRoute(playlistId: String?): String? =
  validOnlinePlaylistId(playlistId)?.let { "online_playlist/${encodePlaylistId(it)}" }

fun libraryPlaylistRoute(
  localPlaylistId: String,
  browseId: String?,
  isEditable: Boolean,
  songCount: Int,
  remoteSongCount: Int?,
): String {
  val onlineRoute = onlinePlaylistRoute(browseId)
  return if (!isEditable && songCount == 0 && remoteSongCount != 0 && onlineRoute != null) {
    onlineRoute
  } else {
    "local_playlist/${encodePlaylistId(localPlaylistId)}"
  }
}

private fun encodePlaylistId(playlistId: String): String =
  URLEncoder.encode(playlistId, StandardCharsets.UTF_8.name()).replace("+", "%20")
