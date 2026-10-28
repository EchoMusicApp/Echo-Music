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
class NavidromeRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val database: MusicDatabase,
  val api: NavidromeApiService
) {
  companion object {
    private const val TAG = "NavidromeRepo"
    private const val PREFS_NAME = "navidrome_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_AUTH_METHOD = "auth_method"
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
    val user = prefs.getString(KEY_USERNAME, null)
    _isLoggedInFlow.value = connected && !server.isNullOrBlank() && !user.isNullOrBlank()
    _serverUrlFlow.value = server
    _usernameFlow.value = user
  }

  fun getCredentials(): NavidromeCredentials? {
    val server = prefs.getString(KEY_SERVER_URL, null) ?: return null
    val user = prefs.getString(KEY_USERNAME, null) ?: return null
    val pass = prefs.getString(KEY_PASSWORD, null) ?: return null
    val methodStr = prefs.getString(KEY_AUTH_METHOD, NavidromeAuthMethod.TOKEN.name)
    val method =
      try {
        NavidromeAuthMethod.valueOf(methodStr ?: NavidromeAuthMethod.TOKEN.name)
      } catch (e: Exception) {
        NavidromeAuthMethod.TOKEN
      }
    return NavidromeCredentials(serverUrl = server, username = user, password = pass, authMethod = method)
  }

  suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val initialCreds =
        NavidromeCredentials(
          serverUrl = serverUrl.trim().trimEnd('/'),
          username = username.trim(),
          password = password,
          authMethod = NavidromeAuthMethod.TOKEN
        )
      val pingResult = api.ping(initialCreds)
      pingResult.fold(
        onSuccess = { verifiedCreds ->
          prefs.edit()
            .putString(KEY_SERVER_URL, verifiedCreds.serverUrl)
            .putString(KEY_USERNAME, verifiedCreds.username)
            .putString(KEY_PASSWORD, verifiedCreds.password)
            .putString(KEY_AUTH_METHOD, verifiedCreds.authMethod.name)
            .putBoolean(KEY_CONNECTED, true)
            .apply()

          _isLoggedInFlow.value = true
          _serverUrlFlow.value = verifiedCreds.serverUrl
          _usernameFlow.value = verifiedCreds.username
          Timber.d("$TAG: Connected to Subsonic server: ${verifiedCreds.serverUrl} as ${verifiedCreds.username}")
          Result.success(Unit)
        },
        onFailure = {
          Timber.e(it, "$TAG: Connection failed to $serverUrl")
          Result.failure(it)
        }
      )
    }

  fun logout() {
    prefs.edit().clear().apply()
    _isLoggedInFlow.value = false
    _serverUrlFlow.value = null
    _usernameFlow.value = null
    Timber.d("$TAG: Logged out from Subsonic")
  }

  suspend fun ping(): Result<String> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.ping(creds).map { "Subsonic Server Online (${it.serverUrl})" }
  }

  suspend fun getPlaylists(): Result<List<NavidromePlaylist>> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.getPlaylists(creds)
  }

  suspend fun getPlaylistSongs(playlistId: String): Result<List<NavidromeSong>> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.getPlaylistSongs(creds, playlistId)
  }

  suspend fun getRandomSongs(size: Int = 50): Result<List<NavidromeSong>> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    api.getRandomSongs(creds, size)
  }

  fun getStreamUrl(songId: String): String? {
    val creds = getCredentials() ?: return null
    return api.getStreamUrl(creds, songId)
  }

  fun getCoverArtUrl(coverArtId: String): String? {
    val creds = getCredentials() ?: return null
    return api.getCoverArtUrl(creds, coverArtId)
  }

  suspend fun syncPlaylistsAndSongs(): Result<Int> = withContext(Dispatchers.IO) {
    val creds = getCredentials() ?: return@withContext Result.failure(Exception("Not logged in"))
    try {
      var syncedSongsCount = 0
      val playlistsResult = api.getPlaylists(creds)
      val playlists = playlistsResult.getOrNull() ?: emptyList()

      for (pl in playlists) {
        val playlistId = "navidrome_pl_${pl.id}"
        database.query {
          val plEntity = PlaylistEntity(
            id = playlistId,
            name = "[Subsonic] ${pl.name}",
            thumbnailUrl = pl.coverArt?.let { api.getCoverArtUrl(creds, it) },
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
          val mediaId = "navidrome_${song.id}"
          val coverUrl = song.coverArt?.let { api.getCoverArtUrl(creds, it) }

          database.query {
            insert(
              SongEntity(
                id = mediaId,
                title = song.title,
                duration = song.duration.toInt(),
                thumbnailUrl = coverUrl,
                albumName = song.album,
                year = song.year,
                inLibrary = LocalDateTime.now()
              )
            )

            val artistId = "navidrome_artist_${song.artist.hashCode()}"
            insert(
              ArtistEntity(
                id = artistId,
                name = song.artist
              )
            )

            insert(
              SongArtistMap(
                songId = mediaId,
                artistId = artistId,
                position = 0
              )
            )

            insert(
              PlaylistSongMap(
                playlistId = playlistId,
                songId = mediaId,
                position = index
              )
            )
          }
          syncedSongsCount++
        }
      }

      // Also fetch a batch of library songs if no playlists existed
      if (playlists.isEmpty()) {
        val randomResult = api.getRandomSongs(creds, 50)
        val randomSongs = randomResult.getOrNull() ?: emptyList()
        val playlistId = "navidrome_pl_library"
        database.query {
          val plEntity = PlaylistEntity(
            id = playlistId,
            name = "[Subsonic] Library",
            bookmarkedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            lastUpdateTime = LocalDateTime.now(),
            isEditable = false
          )
          insert(plEntity)
          update(plEntity)
        }
        randomSongs.forEachIndexed { index, song ->
          val mediaId = "navidrome_${song.id}"
          val coverUrl = song.coverArt?.let { api.getCoverArtUrl(creds, it) }
          database.query {
            insert(
              SongEntity(
                id = mediaId,
                title = song.title,
                duration = song.duration.toInt(),
                thumbnailUrl = coverUrl,
                albumName = song.album,
                year = song.year,
                inLibrary = LocalDateTime.now()
              )
            )
            val artistId = "navidrome_artist_${song.artist.hashCode()}"
            insert(ArtistEntity(id = artistId, name = song.artist))
            insert(SongArtistMap(songId = mediaId, artistId = artistId, position = 0))
            insert(PlaylistSongMap(playlistId = playlistId, songId = mediaId, position = index))
          }
          syncedSongsCount++
        }
      }

      Result.success(syncedSongsCount)
    } catch (e: Exception) {
      Timber.e(e, "$TAG: Failed to sync Subsonic playlists")
      Result.failure(e)
    }
  }
}
