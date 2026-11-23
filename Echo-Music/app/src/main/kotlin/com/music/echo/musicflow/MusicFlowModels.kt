package echo.music.iad1tya.musicflow

import androidx.compose.runtime.Immutable
import com.music.innertube.models.Album
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.models.MediaMetadata

enum class MusicFlowSource {
  NOW_PLAYING,
  HISTORY,
  LIKED,
  QUICK_PICKS,
  CATALOG,
  OFFLINE,
}

@Immutable
data class MusicFlowItem(
  val song: SongItem,
  val source: MusicFlowSource,
) {
  val songId: String
    get() = song.id

  val primaryArtistKey: String?
    get() = song.artists.firstOrNull()?.let { it.id ?: it.name.lowercase() }
}

sealed interface MusicFlowPlaybackStatus {
  data object Idle : MusicFlowPlaybackStatus

  data object Switching : MusicFlowPlaybackStatus

  data object Synced : MusicFlowPlaybackStatus

  data class Blocked(val reason: TakeoverBlock) : MusicFlowPlaybackStatus

  data object ExternalOverride : MusicFlowPlaybackStatus
}

enum class TakeoverBlock {
  CASTING,
  LISTEN_TOGETHER_GUEST,
}

enum class MusicFlowFeedError {
  OFFLINE,
  EMPTY,
  FAILED,
}

@Immutable
data class MusicFlowUiState(
  val items: List<MusicFlowItem> = emptyList(),
  val selectedIndex: Int = 0,
  val isInitialLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val error: MusicFlowFeedError? = null,
  val hasMore: Boolean = true,
  val playbackStatus: MusicFlowPlaybackStatus = MusicFlowPlaybackStatus.Idle,
  val canRestorePreviousQueue: Boolean = false,
  val isOfflineFeed: Boolean = false,
) {
  val selectedSongId: String?
    get() = items.getOrNull(selectedIndex)?.songId
}

fun MediaMetadata.toMusicFlowSongItem() =
  SongItem(
    id = id,
    title = title,
    artists = artists.map { Artist(name = it.name, id = it.id) },
    album = album?.let { Album(name = it.title, id = it.id) },
    duration = duration.takeIf { it > 0 },
    thumbnail = thumbnailUrl.orEmpty(),
    explicit = explicit,
    musicVideoType = musicVideoType,
  )

fun Song.toMusicFlowSongItem() =
  SongItem(
    id = id,
    title = title,
    artists = artists.map { Artist(name = it.name, id = it.id) },
    album = album?.let { Album(name = it.title, id = it.id) },
    duration = song.duration,
    thumbnail = thumbnailUrl.orEmpty(),
    explicit = song.explicit,
    musicVideoType = if (song.isVideo) "MUSIC_VIDEO_TYPE_OMV" else null,
    libraryAddToken = song.libraryAddToken,
    libraryRemoveToken = song.libraryRemoveToken,
  )
