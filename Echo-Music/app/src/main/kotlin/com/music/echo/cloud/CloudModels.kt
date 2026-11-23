package com.music.echo.cloud

enum class NavidromeAuthMethod {
  TOKEN,
  PASSWORD
}

data class NavidromeCredentials(
  val serverUrl: String,
  val username: String,
  val password: String,
  val authMethod: NavidromeAuthMethod = NavidromeAuthMethod.TOKEN
) {
  val isValid: Boolean
    get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

data class NavidromeSong(
  val id: String,
  val title: String,
  val artist: String,
  val album: String,
  val duration: Long,
  val bitRate: Int? = null,
  val mimeType: String? = null,
  val suffix: String? = null,
  val coverArt: String? = null,
  val year: Int? = null,
  val track: Int? = null
)

data class NavidromePlaylist(
  val id: String,
  val name: String,
  val songCount: Int = 0,
  val duration: Long = 0,
  val coverArt: String? = null
)

data class JellyfinCredentials(
  val serverUrl: String,
  val username: String,
  val accessToken: String? = null,
  val userId: String? = null,
  val serverId: String? = null
) {
  val isValid: Boolean
    get() = serverUrl.isNotBlank() && username.isNotBlank() && !accessToken.isNullOrBlank()
}

data class JellyfinSong(
  val id: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val bitRate: Int? = null,
  val container: String? = null,
  val hasCover: Boolean = false,
  val year: Int? = null
)

data class JellyfinPlaylist(
  val id: String,
  val name: String,
  val songCount: Int = 0,
  val hasCover: Boolean = false
)

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface CloudEntryPoint {
  fun navidromeRepository(): NavidromeRepository
  fun jellyfinRepository(): JellyfinRepository
}

