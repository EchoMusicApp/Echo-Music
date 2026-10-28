package com.music.echo.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavidromeApiService @Inject constructor() {

  companion object {
    private const val TAG = "NavidromeApi"
    private const val API_VERSION = "1.16.1"
    private const val CLIENT_ID = "EchoMusic"
  }

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .build()

  suspend fun ping(credentials: NavidromeCredentials): Result<NavidromeCredentials> =
    withContext(Dispatchers.IO) {
      val (result, effectiveMethod) = executeWithFallback(credentials, "ping")
      result.fold(
        onSuccess = { Result.success(credentials.copy(authMethod = effectiveMethod)) },
        onFailure = { Result.failure(it) }
      )
    }

  suspend fun getPlaylists(credentials: NavidromeCredentials): Result<List<NavidromePlaylist>> =
    withContext(Dispatchers.IO) {
      val (result, _) = executeWithFallback(credentials, "getPlaylists")
      result.map { json ->
        val playlistsJson =
          json.optJSONObject("subsonic-response")
            ?.optJSONObject("playlists")
            ?.optJSONArray("playlist")
        val list = mutableListOf<NavidromePlaylist>()
        if (playlistsJson != null) {
          for (i in 0 until playlistsJson.length()) {
            val item = playlistsJson.getJSONObject(i)
            list.add(
              NavidromePlaylist(
                id = item.optString("id", ""),
                name = item.optString("name", "Unknown Playlist"),
                songCount = item.optInt("songCount", 0),
                duration = item.optLong("duration", 0),
                coverArt = item.optString("coverArt").takeIf { it.isNotEmpty() }
              )
            )
          }
        }
        list
      }
    }

  suspend fun getPlaylistSongs(
    credentials: NavidromeCredentials,
    playlistId: String
  ): Result<List<NavidromeSong>> =
    withContext(Dispatchers.IO) {
      val (result, _) = executeWithFallback(credentials, "getPlaylist", mapOf("id" to playlistId))
      result.map { json ->
        val entries =
          json.optJSONObject("subsonic-response")
            ?.optJSONObject("playlist")
            ?.optJSONArray("entry")
        parseSongs(entries)
      }
    }

  suspend fun getRandomSongs(
    credentials: NavidromeCredentials,
    size: Int = 50
  ): Result<List<NavidromeSong>> =
    withContext(Dispatchers.IO) {
      val (result, _) = executeWithFallback(credentials, "getRandomSongs", mapOf("size" to size.toString()))
      result.map { json ->
        val entries =
          json.optJSONObject("subsonic-response")
            ?.optJSONObject("randomSongs")
            ?.optJSONArray("song")
        parseSongs(entries)
      }
    }

  private fun parseSongs(entries: org.json.JSONArray?): List<NavidromeSong> {
    val list = mutableListOf<NavidromeSong>()
    if (entries != null) {
      for (i in 0 until entries.length()) {
        val s = entries.getJSONObject(i)
        list.add(
          NavidromeSong(
            id = s.optString("id", ""),
            title = s.optString("title", "Unknown Title"),
            artist = s.optString("artist", "Unknown Artist"),
            album = s.optString("album", "Unknown Album"),
            duration = s.optLong("duration", 0),
            bitRate = s.optInt("bitRate").takeIf { it > 0 },
            mimeType = s.optString("contentType").takeIf { it.isNotEmpty() },
            suffix = s.optString("suffix").takeIf { it.isNotEmpty() },
            coverArt = s.optString("coverArt").takeIf { it.isNotEmpty() },
            year = s.optInt("year").takeIf { it > 0 },
            track = s.optInt("track").takeIf { it > 0 }
          )
        )
      }
    }
    return list
  }

  fun getStreamUrl(credentials: NavidromeCredentials, songId: String): String {
    val salt = randomSalt()
    val authParams = authParams(credentials.password, credentials.authMethod, salt)
    val urlBuilder =
      "${credentials.serverUrl.trim().trimEnd('/')}/rest/stream.view".toHttpUrl().newBuilder()
        .addQueryParameter("u", credentials.username)
        .addQueryParameter("v", API_VERSION)
        .addQueryParameter("c", CLIENT_ID)
        .addQueryParameter("f", "json")
        .addQueryParameter("id", songId)
        .addQueryParameter("format", "raw")
    authParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
    return urlBuilder.build().toString()
  }

  fun getCoverArtUrl(credentials: NavidromeCredentials, coverArtId: String): String {
    val salt = randomSalt()
    val authParams = authParams(credentials.password, credentials.authMethod, salt)
    val urlBuilder =
      "${credentials.serverUrl.trim().trimEnd('/')}/rest/getCoverArt.view".toHttpUrl().newBuilder()
        .addQueryParameter("u", credentials.username)
        .addQueryParameter("v", API_VERSION)
        .addQueryParameter("c", CLIENT_ID)
        .addQueryParameter("id", coverArtId)
        .addQueryParameter("size", "600")
    authParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
    return urlBuilder.build().toString()
  }

  private suspend fun executeWithFallback(
    credentials: NavidromeCredentials,
    endpoint: String,
    extraParams: Map<String, String> = emptyMap()
  ): Pair<Result<JSONObject>, NavidromeAuthMethod> {
    var method = credentials.authMethod
    val firstAttempt = request(credentials, endpoint, method, extraParams)
    if (firstAttempt.isSuccess) {
      return firstAttempt to method
    }

    val exception = firstAttempt.exceptionOrNull()
    if (method == NavidromeAuthMethod.TOKEN && exception is SubsonicApiException && exception.code == 41) {
      Timber.d("$TAG: Server rejected token auth (code 41), retrying with password auth")
      method = NavidromeAuthMethod.PASSWORD
      val secondAttempt = request(credentials, endpoint, method, extraParams)
      return secondAttempt to method
    }

    return firstAttempt to method
  }

  private fun request(
    credentials: NavidromeCredentials,
    endpoint: String,
    method: NavidromeAuthMethod,
    extraParams: Map<String, String>
  ): Result<JSONObject> {
    return try {
      val salt = randomSalt()
      val auth = authParams(credentials.password, method, salt)
      val urlBuilder =
        "${credentials.serverUrl.trim().trimEnd('/')}/rest/$endpoint.view".toHttpUrl().newBuilder()
          .addQueryParameter("u", credentials.username)
          .addQueryParameter("v", API_VERSION)
          .addQueryParameter("c", CLIENT_ID)
          .addQueryParameter("f", "json")
      auth.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
      extraParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

      val request = Request.Builder().url(urlBuilder.build()).get().build()
      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
        }
        val bodyString = response.body?.string().orEmpty()
        val json = JSONObject(bodyString)
        val subsonicResponse = json.optJSONObject("subsonic-response")
        val status = subsonicResponse?.optString("status")
        if (status == "ok") {
          Result.success(json)
        } else {
          val error = subsonicResponse?.optJSONObject("error")
          val code = error?.optInt("code", -1) ?: -1
          val message = error?.optString("message", "Subsonic API error") ?: "Subsonic API error"
          Result.failure(SubsonicApiException(code, message))
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun authParams(password: String, method: NavidromeAuthMethod, salt: String): Map<String, String> =
    when (method) {
      NavidromeAuthMethod.TOKEN ->
        mapOf(
          "t" to md5Hex(password + salt),
          "s" to salt
        )
      NavidromeAuthMethod.PASSWORD ->
        mapOf(
          "p" to "enc:" + password.toByteArray(Charsets.UTF_8).toHexString()
        )
    }

  private fun randomSalt(): String = UUID.randomUUID().toString().take(6)

  private fun md5Hex(input: String): String =
    MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8)).toHexString()

  private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}

class SubsonicApiException(val code: Int, val serverMessage: String) :
  Exception("API Error $code: $serverMessage")
