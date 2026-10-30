package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_PODCAST_EPISODE
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.utils.EpisodeProgress
import echo.music.iad1tya.utils.PodcastProgress
import echo.music.iad1tya.utils.listItemShape

/** Episodes started and not finished, most recently listened first. */
@Composable
fun rememberEpisodesInProgress(limit: Int = Int.MAX_VALUE): List<EpisodeProgress> {
  val entries by PodcastProgress.entries.collectAsState()
  return PodcastProgress.inProgress(entries).take(limit)
}

/** An episode as a [SongItem], so it is drawn and played like the episodes of a show. */
fun EpisodeProgress.toSongItem() =
  SongItem(
    id = id,
    title = title,
    artists = listOfNotNull(show?.let { Artist(name = it, id = showBrowseId) }),
    duration = (durationMs / 1000).toInt().takeIf { it > 0 },
    musicVideoType = MUSIC_VIDEO_TYPE_PODCAST_EPISODE,
    thumbnail = thumbnailUrl.orEmpty(),
  )

/**
 * "Continue listening": a title and one row per episode in [episodes]; tapping one continues it
 * where it was left.
 */
fun LazyListScope.continueListening(episodes: List<EpisodeProgress>, keyPrefix: String) {
  if (episodes.isEmpty()) return
  item(key = "${keyPrefix}_continue_listening_title") {
    NavigationTitle(title = stringResource(R.string.continue_listening))
  }
  itemsIndexed(episodes, key = { _, it -> "${keyPrefix}_continue_listening_${it.id}" }) {
    index,
    episode ->
    val playerConnection = LocalPlayerConnection.current
    val song = episode.toSongItem()
    YouTubeListItem(
      item = song,
      isSwipeable = false,
      shape = listItemShape(index = index, count = episodes.size),
      modifier =
        Modifier.clickable {
          playerConnection?.playQueue(
            ListQueue(
              title = episode.show,
              items = listOf(song.toMediaMetadata().toMediaItem()),
              position = episode.positionMs,
            )
          )
        },
    )
  }
}
