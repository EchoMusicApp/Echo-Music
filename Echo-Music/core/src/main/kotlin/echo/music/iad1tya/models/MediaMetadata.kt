package echo.music.iad1tya.models

import androidx.compose.runtime.Immutable
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import java.io.Serializable
import java.time.LocalDateTime

enum class QueueItemSource {
  USER
}

@Immutable
data class MediaMetadata(
  val id: String,
  val title: String,
  val artists: List<Artist>,
  val duration: Int,
  val thumbnailUrl: String? = null,
  val album: Album? = null,
  val setVideoId: String? = null,
  val musicVideoType: String? = null,
  val explicit: Boolean = false,
  val liked: Boolean = false,
  val likedDate: LocalDateTime? = null,
  val inLibrary: LocalDateTime? = null,
  val libraryAddToken: String? = null,
  val libraryRemoveToken: String? = null,
  val suggestedBy: String? = null,
  val source: QueueItemSource = QueueItemSource.USER,
) : Serializable {
  val isVideoSong: Boolean
    get() = musicVideoType != null && musicVideoType != MUSIC_VIDEO_TYPE_ATV

  data class Artist(
    val id: String?,
    val name: String,
  ) : Serializable

  data class Album(
    val id: String,
    val title: String,
  ) : Serializable
}
