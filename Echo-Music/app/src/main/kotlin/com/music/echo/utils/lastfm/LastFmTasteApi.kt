package echo.music.iad1tya.utils.lastfm

import echo.music.iad1tya.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

data class SimilarTrack(
  val name: String,
  val artist: String,
  val match: Double? = null,
)

data class SimilarArtist(
  val name: String,
  val match: Double? = null,
)

data class Tag(
  val name: String,
  val count: Int = 0,
)

interface LastFmTasteApi {
  suspend fun getSimilarTracks(artist: String, track: String): Result<List<SimilarTrack>>
  suspend fun getSimilarArtists(artist: String): Result<List<SimilarArtist>>
  suspend fun getTopTags(artist: String): Result<List<Tag>>
  suspend fun getUserTopArtists(user: String, limit: Int = 30): Result<List<String>>
  suspend fun getUserTopTracks(user: String, limit: Int = 50): Result<List<SimilarTrack>>
  suspend fun getUserTopTags(user: String, limit: Int = 15): Result<List<Tag>>
}

@Singleton
open class RealLastFmTasteApi @Inject constructor() : LastFmTasteApi {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  protected open fun getHttpClient(): HttpClient = LastFM.getKtorClient()

  protected open fun getApiKey(): String =
    LastFM.getApiKey().ifBlank { BuildConfig.LASTFM_API_KEY }

  private fun asObjectList(element: JsonElement?): List<JsonObject> {
    return when (element) {
      is JsonArray -> element.filterIsInstance<JsonObject>()
      is JsonObject -> listOf(element)
      else -> emptyList()
    }
  }

  override suspend fun getSimilarTracks(artist: String, track: String): Result<List<SimilarTrack>> =
    runCatching {
      val apiKey = getApiKey()
      if (apiKey.isBlank() || artist.isBlank() || track.isBlank()) return@runCatching emptyList()

      val response = getHttpClient().get {
        parameter("method", "track.getsimilar")
        parameter("artist", artist)
        parameter("track", track)
        parameter("limit", "30")
        parameter("api_key", apiKey)
        parameter("format", "json")
      }
      val text = response.bodyAsText()
      val root = json.parseToJsonElement(text).jsonObject
      if (root.containsKey("error")) return@runCatching emptyList()

      val tracksElement = root["similartracks"]?.jsonObject?.get("track")
      asObjectList(tracksElement).mapNotNull { item ->
        val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val artistName = when (val a = item["artist"]) {
          is JsonObject -> a["name"]?.jsonPrimitive?.content
          else -> a?.jsonPrimitive?.content
        } ?: return@mapNotNull null
        val match = item["match"]?.jsonPrimitive?.doubleOrNull
          ?: item["match"]?.jsonPrimitive?.content?.toDoubleOrNull()
        SimilarTrack(name = name, artist = artistName, match = match)
      }
    }.onFailure { Timber.d(it, "Failed to get similar tracks for $artist - $track") }

  override suspend fun getSimilarArtists(artist: String): Result<List<SimilarArtist>> =
    runCatching {
      val apiKey = getApiKey()
      if (apiKey.isBlank() || artist.isBlank()) return@runCatching emptyList()

      val response = getHttpClient().get {
        parameter("method", "artist.getsimilar")
        parameter("artist", artist)
        parameter("limit", "20")
        parameter("api_key", apiKey)
        parameter("format", "json")
      }
      val text = response.bodyAsText()
      val root = json.parseToJsonElement(text).jsonObject
      if (root.containsKey("error")) return@runCatching emptyList()

      val artistsElement = root["similarartists"]?.jsonObject?.get("artist")
      asObjectList(artistsElement).mapNotNull { item ->
        val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val match = item["match"]?.jsonPrimitive?.doubleOrNull
          ?: item["match"]?.jsonPrimitive?.content?.toDoubleOrNull()
        SimilarArtist(name = name, match = match)
      }
    }.onFailure { Timber.d(it, "Failed to get similar artists for $artist") }

  override suspend fun getTopTags(artist: String): Result<List<Tag>> =
    runCatching {
      val apiKey = getApiKey()
      if (apiKey.isBlank() || artist.isBlank()) return@runCatching emptyList()

      val response = getHttpClient().get {
        parameter("method", "artist.gettoptags")
        parameter("artist", artist)
        parameter("api_key", apiKey)
        parameter("format", "json")
      }
      val text = response.bodyAsText()
      val root = json.parseToJsonElement(text).jsonObject
      if (root.containsKey("error")) return@runCatching emptyList()

      val tagsElement = root["toptags"]?.jsonObject?.get("tag")
      asObjectList(tagsElement).mapNotNull { item ->
        val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val count = item["count"]?.jsonPrimitive?.intOrNull
          ?: item["count"]?.jsonPrimitive?.content?.toIntOrNull()
          ?: 0
        Tag(name = name, count = count)
      }
    }.onFailure { Timber.d(it, "Failed to get top tags for $artist") }

  override suspend fun getUserTopArtists(user: String, limit: Int): Result<List<String>> =
    runCatching {
      val apiKey = getApiKey()
      if (apiKey.isBlank() || user.isBlank()) return@runCatching emptyList()

      val response = getHttpClient().get {
        parameter("method", "user.gettopartists")
        parameter("user", user)
        parameter("period", "overall")
        parameter("limit", limit.toString())
        parameter("api_key", apiKey)
        parameter("format", "json")
      }
      val text = response.bodyAsText()
      val root = json.parseToJsonElement(text).jsonObject
      if (root.containsKey("error")) return@runCatching emptyList()

      val artistsElement = root["topartists"]?.jsonObject?.get("artist")
      asObjectList(artistsElement).mapNotNull { item ->
        item["name"]?.jsonPrimitive?.content
      }
    }.onFailure { Timber.d(it, "Failed to get user top artists for $user") }

  override suspend fun getUserTopTracks(user: String, limit: Int): Result<List<SimilarTrack>> =
    runCatching {
      val apiKey = getApiKey()
      if (apiKey.isBlank() || user.isBlank()) return@runCatching emptyList()

      val response = getHttpClient().get {
        parameter("method", "user.gettoptracks")
        parameter("user", user)
        parameter("period", "overall")
        parameter("limit", limit.toString())
        parameter("api_key", apiKey)
        parameter("format", "json")
      }
      val text = response.bodyAsText()
      val root = json.parseToJsonElement(text).jsonObject
      if (root.containsKey("error")) return@runCatching emptyList()

      val tracksElement = root["toptracks"]?.jsonObject?.get("track")
      asObjectList(tracksElement).mapNotNull { item ->
        val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val artistName = when (val a = item["artist"]) {
          is JsonObject -> a["name"]?.jsonPrimitive?.content
          else -> a?.jsonPrimitive?.content
        } ?: return@mapNotNull null
        SimilarTrack(name = name, artist = artistName)
      }
    }.onFailure { Timber.d(it, "Failed to get user top tracks for $user") }

  override suspend fun getUserTopTags(user: String, limit: Int): Result<List<Tag>> =
    runCatching {
      val apiKey = getApiKey()
      if (apiKey.isBlank() || user.isBlank()) return@runCatching emptyList()

      val response = getHttpClient().get {
        parameter("method", "user.gettoptags")
        parameter("user", user)
        parameter("limit", limit.toString())
        parameter("api_key", apiKey)
        parameter("format", "json")
      }
      val text = response.bodyAsText()
      val root = json.parseToJsonElement(text).jsonObject
      if (root.containsKey("error")) return@runCatching emptyList()

      val tagsElement = root["toptags"]?.jsonObject?.get("tag")
      asObjectList(tagsElement).mapNotNull { item ->
        val name = item["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val count = item["count"]?.jsonPrimitive?.intOrNull
          ?: item["count"]?.jsonPrimitive?.content?.toIntOrNull()
          ?: 0
        Tag(name = name, count = count)
      }
    }.onFailure { Timber.d(it, "Failed to get user top tags for $user") }
}
