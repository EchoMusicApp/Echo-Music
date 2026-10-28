package com.music.echo.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinApiService @Inject constructor() {

  companion object {
    private const val TAG = "JellyfinApi"
    private const val CLIENT_NAME = "EchoMusic"
    private const val CLIENT_VERSION = "1.0"
    private const val DEVICE_NAME = "Android"
    private const val DEVICE_ID = "EchoMusic-Android"
  }

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .build()

  suspend fun authenticate(
    serverUrl: String,
    username: String,
    password: String
  ): Result<JellyfinCredentials> = withContext(Dispatchers.IO) {
    try {
      val url = "${serverUrl.trim().trimEnd('/')}/Users/AuthenticateByName"
      val body =
        JSONObject().apply {
          put("Username", username)
          put("Pw", password)
        }.toString().toRequestBody("application/json".toMediaType())

      val authHeader =
        "MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", DeviceId=\"$DEVICE_ID\", Version=\"$CLIENT_VERSION\""

      val request =
        Request.Builder()
          .url(url)
          .header("Authorization", authHeader)
          .header("Content-Type", "application/json")
          .post(body)
          .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
        }

        val responseBody = response.body?.string().orEmpty()
        val json = JSONObject(responseBody)
        val accessToken = json.optString("AccessToken", "")
        val user = json.optJSONObject("User")
        val userId = user?.optString("Id", "") ?: ""
        val serverId = json.optString("ServerId", "")

        if (accessToken.isBlank() || userId.isBlank()) {
          return@withContext Result.failure(Exception("Invalid authentication response from Jellyfin"))
        }

        Result.success(
          JellyfinCredentials(
            serverUrl = serverUrl.trim().trimEnd('/'),
            username = username,
            accessToken = accessToken,
            userId = userId,
            serverId = serverId
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun ping(credentials: JellyfinCredentials): Result<String> = withContext(Dispatchers.IO) {
    try {
      val url = "${credentials.serverUrl}/System/Info"
      val request =
        Request.Builder()
          .url(url)
          .header("Authorization", authHeader(credentials.accessToken))
          .get()
          .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val json = JSONObject(response.body?.string().orEmpty())
          val serverName = json.optString("ServerName", "Jellyfin Server")
          val version = json.optString("Version", "")
          Result.success("$serverName (v$version)")
        } else {
          Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getPlaylists(credentials: JellyfinCredentials): Result<List<JellyfinPlaylist>> =
    withContext(Dispatchers.IO) {
      try {
        val userId = credentials.userId ?: return@withContext Result.failure(Exception("Missing User ID"))
        val url =
          "${credentials.serverUrl}/Users/$userId/Items".toHttpUrl().newBuilder()
            .addQueryParameter("includeItemTypes", "Playlist")
            .addQueryParameter("recursive", "true")
            .build()

        val request =
          Request.Builder()
            .url(url)
            .header("Authorization", authHeader(credentials.accessToken))
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
          }
          val json = JSONObject(response.body?.string().orEmpty())
          val items = json.optJSONArray("Items")
          val list = mutableListOf<JellyfinPlaylist>()
          if (items != null) {
            for (i in 0 until items.length()) {
              val it = items.getJSONObject(i)
              list.add(
                JellyfinPlaylist(
                  id = it.optString("Id", ""),
                  name = it.optString("Name", "Unknown Playlist"),
                  songCount = it.optInt("ChildCount", 0),
                  hasCover = it.optJSONObject("ImageTags")?.has("Primary") == true
                )
              )
            }
          }
          Result.success(list)
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun getPlaylistSongs(
    credentials: JellyfinCredentials,
    playlistId: String
  ): Result<List<JellyfinSong>> = withContext(Dispatchers.IO) {
    try {
      val userId = credentials.userId ?: return@withContext Result.failure(Exception("Missing User ID"))
      val url =
        "${credentials.serverUrl}/Playlists/$playlistId/Items".toHttpUrl().newBuilder()
          .addQueryParameter("userId", userId)
          .addQueryParameter("fields", "ItemCounts,PrimaryImageAspectRatio,Path,MediaSources")
          .build()

      val request =
        Request.Builder()
          .url(url)
          .header("Authorization", authHeader(credentials.accessToken))
          .get()
          .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
        }
        val json = JSONObject(response.body?.string().orEmpty())
        val items = json.optJSONArray("Items")
        Result.success(parseSongs(items))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getSongs(
    credentials: JellyfinCredentials,
    limit: Int = 100
  ): Result<List<JellyfinSong>> = withContext(Dispatchers.IO) {
    try {
      val userId = credentials.userId ?: return@withContext Result.failure(Exception("Missing User ID"))
      val url =
        "${credentials.serverUrl}/Users/$userId/Items".toHttpUrl().newBuilder()
          .addQueryParameter("includeItemTypes", "Audio")
          .addQueryParameter("recursive", "true")
          .addQueryParameter("sortBy", "DateCreated")
          .addQueryParameter("sortOrder", "Descending")
          .addQueryParameter("limit", limit.toString())
          .addQueryParameter("fields", "ItemCounts,PrimaryImageAspectRatio,Path,MediaSources")
          .build()

      val request =
        Request.Builder()
          .url(url)
          .header("Authorization", authHeader(credentials.accessToken))
          .get()
          .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
        }
        val json = JSONObject(response.body?.string().orEmpty())
        val items = json.optJSONArray("Items")
        Result.success(parseSongs(items))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun parseSongs(items: org.json.JSONArray?): List<JellyfinSong> {
    val list = mutableListOf<JellyfinSong>()
    if (items != null) {
      for (i in 0 until items.length()) {
        val it = items.getJSONObject(i)
        val runTimeTicks = it.optLong("RunTimeTicks", 0L)
        val durationMs = if (runTimeTicks > 0) runTimeTicks / 10_000L else 0L
        val mediaSources = it.optJSONArray("MediaSources")
        val firstSource = mediaSources?.optJSONObject(0)
        val bitRate = firstSource?.optInt("Bitrate")?.takeIf { it > 0 }
        val container = firstSource?.optString("Container").takeIf { !it.isNullOrBlank() }

        list.add(
          JellyfinSong(
            id = it.optString("Id", ""),
            title = it.optString("Name", "Unknown Song"),
            artist = it.optJSONArray("Artists")?.optString(0) ?: it.optString("AlbumArtist", "Unknown Artist"),
            album = it.optString("Album", "Unknown Album"),
            durationMs = durationMs,
            bitRate = bitRate,
            container = container,
            hasCover = it.optJSONObject("ImageTags")?.has("Primary") == true,
            year = it.optInt("ProductionYear").takeIf { it > 0 }
          )
        )
      }
    }
    return list
  }

  fun getStreamUrl(credentials: JellyfinCredentials, songId: String): String {
    val base = "${credentials.serverUrl}/Audio/$songId/stream"
    val builder = base.toHttpUrl().newBuilder().addQueryParameter("static", "true")
    if (!credentials.accessToken.isNullOrBlank()) {
      builder.addQueryParameter("api_key", credentials.accessToken)
    }
    return builder.build().toString()
  }

  fun getItemImageUrl(credentials: JellyfinCredentials, itemId: String): String {
    val base = "${credentials.serverUrl}/Items/$itemId/Images/Primary"
    val builder = base.toHttpUrl().newBuilder().addQueryParameter("quality", "90")
    if (!credentials.accessToken.isNullOrBlank()) {
      builder.addQueryParameter("api_key", credentials.accessToken)
    }
    return builder.build().toString()
  }

  private fun authHeader(token: String?): String {
    val tokenPart = if (!token.isNullOrBlank()) ", Token=\"$token\"" else ""
    return "MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", DeviceId=\"$DEVICE_ID\", Version=\"$CLIENT_VERSION\"$tokenPart"
  }
}
