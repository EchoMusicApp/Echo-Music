package com.music.echo.cloud

import android.content.Context
import android.content.SharedPreferences
import echo.music.iad1tya.db.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.db.entities.ArtistEntity
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.SongArtistMap
import echo.music.iad1tya.db.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val database: MusicDatabase,
  val api: JellyfinApiService
) {
  companion object {
    private const val TAG = "JellyfinRepo"
    private const val PREFS_NAME = "jellyfin_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_SERVER_ID = "server_id"
    private const val KEY_CONNECTED = "connected"
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _isLoggedInFlow = MutableStateFlow(false)
  val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

  private val _serverUrlFlow = MutableStateFlow<String?>(null)
  val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()

  private val _usernameFlow = MutableStateFlow<String?>(null)
  val usernameFlow: StateFlow<String?> = _usernameFlow.asStateFlow()

  init {
    val connected = prefs.getBoolean(KEY_CONNECTED, false)
    val server = prefs.getString(KEY_SERVER_URL, null)
    val token = prefs.getString(KEY_ACCESS_TOKEN, null)
    val user = prefs.getString(KEY_USERNAME, null)
    _isLoggedInFlow.value = connected && !server.isNullOrBlank() && !token.isNullOrBlank()
    _serverUrlFlow.value = server
    _usernameFlow.value = user
  }

  fun getCredentials(): JellyfinCredentials? {
    val server = prefs.getString(KEY_SERVER_URL, null) ?: return null
    val user = prefs.getString(KEY_USERNAME, null) ?: return null
    val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
    val userId = prefs.getString(KEY_USER_ID, null)
    val serverId = prefs.getString(KEY_SERVER_ID, null)
    return JellyfinCredentials(serverUrl = server, username = user, accessToken = token, userId = userId, serverId = serverId)
  }

  suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val authResult = api.authenticate(serverUrl, username, password)
      authResult.fold(
        onSuccess = { creds ->
          prefs.edit()
            .putString(KEY_SERVER_URL, creds.serverUrl)
            .putString(KEY_USERNAME, creds.username)
            .putString(KEY_ACCESS_TOKEN, creds.accessToken)
            .putString(KEY_USER_ID, creds.userId)
            .putString(KEY_SERVER_ID, creds.serverId)
            .putBoolean(KEY_CONNECTED, true)
            .apply()

          _isLoggedInFlow.value = true
          _serverUrlFlow.value = creds.serverUrl
          _usernameFlow.value = creds.username
          Timber.d("$TAG: Connected to Jellyfin server: ${creds.serverUrl} as ${creds.username}")
          Result.success(Unit)
        },
        onFailure = {
          Timber.e(it, "$TAG: Authentication failed to $serverUrl")
          Result.failure(it)
        }
      )
    }

  fun logout() {
    prefs.edit().clear().apply()
    _isLoggedInFlow.value = false
    _serverUrlFlow.value = null
    _usernameFlow.value = null
    Timber.d("$TAG: Logged out from Jellyfin")
  }

  suspend fun ping(): Result<String> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.ping(creds)
  }

  suspend fun getPlaylists(): Result<List<JellyfinPlaylist>> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.getPlaylists(creds)
  }

  suspend fun getPlaylistSongs(playlistId: String): Result<List<JellyfinSong>> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.getPlaylistSongs(creds, playlistId)
  }

  suspend fun getSongs(limit: Int = 100): Result<List<JellyfinSong>> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.getSongs(creds, limit)
  }

  fun getStreamUrl(songId: String): String? {
    val creds = getCredentials() ?: return null
    return api.getStreamUrl(creds, songId)
  }

  fun getItemImageUrl(itemId: String): String? {
    val creds = getCredentials() ?: return null
    return api.getItemImageUrl(creds, itemId)
  }

  suspend fun syncPlaylistsAndSongs(): Result<Int> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    try {
      var syncedSongsCount = 0
      val playlistsResult = api.getPlaylists(creds)
      val playlists = playlistsResult.getOrNull() ?: emptyList()

      for (pl in playlists) {
        val playlistId = "jellyfin_pl_${pl.id}"
        val coverUrl = if (pl.hasCover) api.getItemImageUrl(creds, pl.id) else null

        database.query {
          val plEntity = PlaylistEntity(
            id = playlistId,
            name = "[Jellyfin] ${pl.name}",
            thumbnailUrl = coverUrl,
            bookmarkedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            lastUpdateTime = LocalDateTime.now(),
            isEditable = false
          )
          insert(plEntity)
          update(plEntity)
        }

        val songsResult = api.getPlaylistSongs(creds, pl.id)
        val songs = songsResult.getOrNull() ?: emptyList()

        songs.forEachIndexed { index, song ->
          val mediaId = "jellyfin_${song.id}"
          val songCoverUrl = if (song.hasCover) api.getItemImageUrl(creds, song.id) else null

          database.query {
            insert(
              SongEntity(
                id = mediaId,
                title = song.title,
                duration = (song.durationMs / 1000L).toInt(),
                thumbnailUrl = songCoverUrl,
                albumName = song.album,
                year = song.year,
                inLibrary = LocalDateTime.now()
              )
            )

            val artistId = "jellyfin_artist_${song.artist.hashCode()}"
            insert(ArtistEntity(id = artistId, name = song.artist))
            insert(SongArtistMap(songId = mediaId, artistId = artistId, position = 0))
            insert(PlaylistSongMap(playlistId = playlistId, songId = mediaId, position = index))
          }
          syncedSongsCount++
        }
      }

      // If no playlists, sync library audio items directly
      if (playlists.isEmpty()) {
        val songsResult = api.getSongs(creds, 100)
        val songs = songsResult.getOrNull() ?: emptyList()
        val playlistId = "jellyfin_pl_library"

        database.query {
          val plEntity = PlaylistEntity(
            id = playlistId,
            name = "[Jellyfin] Library",
            bookmarkedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            lastUpdateTime = LocalDateTime.now(),
            isEditable = false
          )
          insert(plEntity)
          update(plEntity)
        }

        songs.forEachIndexed { index, song ->
          val mediaId = "jellyfin_${song.id}"
          val songCoverUrl = if (song.hasCover) api.getItemImageUrl(creds, song.id) else null

          database.query {
            insert(
              SongEntity(
                id = mediaId,
                title = song.title,
                duration = (song.durationMs / 1000L).toInt(),
                thumbnailUrl = songCoverUrl,
                albumName = song.album,
                year = song.year,
                inLibrary = LocalDateTime.now()
              )
            )
            val artistId = "jellyfin_artist_${song.artist.hashCode()}"
            insert(ArtistEntity(id = artistId, name = song.artist))
            insert(SongArtistMap(songId = mediaId, artistId = artistId, position = 0))
            insert(PlaylistSongMap(playlistId = playlistId, songId = mediaId, position = index))
          }
          syncedSongsCount++
        }
      }

      Result.success(syncedSongsCount)
    } catch (e: Exception) {
      Timber.e(e, "$TAG: Failed to sync Jellyfin items")
      Result.failure(e)
    }
  }
}
