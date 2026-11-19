package echo.music.iad1tya.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

object Wikipedia {
  private val client by lazy {
    HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(
          Json {
            isLenient = true
            ignoreUnknownKeys = true
          }
        )
      }
      defaultRequest { url("https://en.wikipedia.org/api/rest_v1/") }
      expectSuccess = true
    }
  }

  @Serializable
  data class WikiThumbnail(val source: String? = null)

  @Serializable
  data class WikiSummary(
    val title: String? = null,
    val extract: String? = null,
    val description: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val type: String? = null,
  )

  suspend fun fetchSummary(title: String): WikiSummary? =
    runCatching {
        client
          .get("page/summary/${title.replace(" ", "_").encodeURLQueryComponent()}")
          .body<WikiSummary>()
      }
      .onFailure {
        if (it is ResponseException && it.response.status == HttpStatusCode.NotFound) {
          Timber.d("No Wikipedia summary found for: $title")
        } else {
          Timber.w("Failed to fetch Wikipedia summary for: $title (${it.message})")
        }
      }
      .getOrNull()
      ?.takeIf { it.extract != null && !it.extract.contains("may refer to", ignoreCase = true) }

  private suspend fun fetchPageSummary(title: String): String? = fetchSummary(title)?.extract

  suspend fun searchWikipedia(query: String, filterKeywords: List<String> = emptyList()): WikiSummary? =
    withContext(Dispatchers.IO) {
      runCatching {
        val enc = java.net.URLEncoder.encode(query, "UTF-8")
        val url = java.net.URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$enc&utf8=&format=json")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.setRequestProperty("User-Agent", "EchoMusic/1.0 (Android; support@echomusic.app)")
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val root = org.json.JSONObject(text).optJSONObject("query") ?: return@runCatching null
        val searchList = root.optJSONArray("search") ?: return@runCatching null
        for (i in 0 until minOf(searchList.length(), 5)) {
          val item = searchList.getJSONObject(i)
          val itemTitle = item.optString("title")
          val snippet = item.optString("snippet")
          val matchesKeyword = filterKeywords.isEmpty() || filterKeywords.any {
            snippet.contains(it, ignoreCase = true) || itemTitle.contains(it, ignoreCase = true)
          }
          if (matchesKeyword) {
            val summary = fetchSummary(itemTitle)
            if (summary != null && !summary.extract.isNullOrBlank()) {
              return@runCatching summary
            }
          }
        }
        null
      }.getOrNull()
    }

  suspend fun fetchArtistBio(artistName: String): WikiSummary? {
    val clean = artistName.replace(Regex("(?i)\\s*-\\s*Topic|\\s*\\(.*\\)"), "").trim()
    if (clean.isBlank()) return null
    val direct = fetchSummary("$clean (musician)")
      ?: fetchSummary("$clean (singer)")
      ?: fetchSummary("$clean (composer)")
      ?: fetchSummary(clean)?.takeIf {
        val desc = it.description.orEmpty().lowercase()
        val ext = it.extract.orEmpty().lowercase()
        val musicKeywords = listOf("music", "singer", "composer", "artist", "band", "song", "rapper", "producer", "playback")
        musicKeywords.any { kw -> desc.contains(kw) || ext.contains(kw) }
      }
    if (direct != null) return direct

    return searchWikipedia(clean, listOf("singer", "composer", "musician", "band", "music", "artist", "rapper", "soundtrack"))
  }

  suspend fun fetchSongStory(songTitle: String, artistName: String?, albumTitle: String?): WikiSummary? {
    val cleanTitle = songTitle.replace(Regex("(?i)\\s*\\(From.*?\\)|\\s*\\[.*?\\]|\\s*\\(Official.*?\\)|\\s*\\(Lyric.*?\\)"), "").trim()
    if (cleanTitle.isNotBlank()) {
      val direct = fetchSummary("$cleanTitle (song)")
        ?: if (artistName != null) fetchSummary("$cleanTitle ($artistName song)") else null
      if (direct != null) return direct
    }

    // Check for movie/soundtrack in title e.g. "(From OG)"
    val fromMatch = Regex("(?i)\\(From\\s+[\"']?(.*?)[\"']?\\)").find(songTitle)
    if (fromMatch != null) {
      val movieName = fromMatch.groupValues[1].trim()
      if (movieName.isNotBlank()) {
        val ostSummary = fetchSummary("$movieName (soundtrack)") ?: fetchSummary(movieName)
        if (ostSummary != null) return ostSummary
      }
    }

    if (!albumTitle.isNullOrBlank() && !albumTitle.equals("Unknown", ignoreCase = true)) {
      val albumSummary = fetchSummary("$albumTitle (soundtrack)")
        ?: fetchSummary("$albumTitle (album)")
        ?: fetchSummary(albumTitle)
      if (albumSummary != null) return albumSummary
    }

    if (cleanTitle.isNotBlank()) {
      return searchWikipedia("$cleanTitle ${artistName.orEmpty()}", listOf("song", "soundtrack", "album", "single", "composed", "film", "music"))
    }
    return null
  }

  suspend fun fetchAlbumInfo(albumTitle: String, artistName: String?): String? {
    if (artistName != null) {
      val preciseQueries = listOf("$albumTitle ($artistName album)", "$albumTitle ($artistName)")
      for (query in preciseQueries) {
        val summary = fetchPageSummary(query)
        if (summary != null && !summary.contains("may refer to", ignoreCase = true)) {
          return summary
        }
      }
    }

    val genericQueries = listOf("$albumTitle (album)", albumTitle)

    for (query in genericQueries) {
      val summary = fetchPageSummary(query)
      if (summary != null && !summary.contains("may refer to", ignoreCase = true)) {
        if (artistName != null) {
          if (summary.contains(artistName, ignoreCase = true)) {
            return summary
          }
        } else {
          return summary
        }
      }
    }

    return null
  }

  suspend fun fetchPlaylistInfo(playlistTitle: String): String? = fetchPageSummary(playlistTitle)
}
