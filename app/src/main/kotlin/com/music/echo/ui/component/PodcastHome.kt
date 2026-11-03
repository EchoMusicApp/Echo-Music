package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.SongItem
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.db.entities.Playlist
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.utils.EpisodeProgress
import echo.music.iad1tya.utils.listItemShape

/**
 * The top of the Podcast chip: followed shows, episodes in progress and the latest episodes of the
 * followed shows. YouTube's own podcast shelves follow below.
 */
fun LazyListScope.podcastHome(
  followed: List<Playlist>,
  inProgress: List<EpisodeProgress>,
  newEpisodes: List<SongItem>,
  navController: NavController,
) {
  if (followed.isNotEmpty()) {
    item(key = "podcast_home_followed_title") {
      NavigationTitle(title = stringResource(R.string.your_podcasts))
    }
    item(key = "podcast_home_followed") {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(followed, key = { it.id }) { show ->
          val browseId = show.playlist.browseId ?: return@items
          Column(
            Modifier.width(120.dp).clickable { navController.navigate("online_playlist/$browseId") }
          ) {
            AsyncImage(
              model = show.playlist.thumbnailUrl ?: show.thumbnails.firstOrNull(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier =
                Modifier.fillMaxWidth()
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
            Text(
              text = show.playlist.name,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(top = 6.dp),
            )
          }
        }
      }
    }
  }

  continueListening(inProgress, keyPrefix = "podcast_home")

  if (newEpisodes.isNotEmpty()) {
    item(key = "podcast_home_new_title") {
      NavigationTitle(title = stringResource(R.string.new_episodes))
    }
    itemsIndexed(newEpisodes, key = { _, it -> "podcast_home_new_${it.id}" }) { index, episode ->
      val playerConnection = LocalPlayerConnection.current
      val queueTitle = stringResource(R.string.new_episodes)
      YouTubeListItem(
        item = episode,
        isSwipeable = false,
        shape = listItemShape(index = index, count = newEpisodes.size),
        modifier =
          Modifier.clickable {
            // Continue with the next new episodes once this one ends.
            playerConnection?.playQueue(
              ListQueue(
                title = queueTitle,
                items = newEpisodes.map { it.toMediaMetadata().toMediaItem() },
                startIndex = index,
              )
            )
          },
      )
    }
  }
}
