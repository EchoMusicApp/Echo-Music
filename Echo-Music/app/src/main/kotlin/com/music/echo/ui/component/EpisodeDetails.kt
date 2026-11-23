package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.models.SongItem
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.utils.makeTimeString

/** A chapter from an episode's description: a line such as "12:23 Title". */
data class EpisodeChapter(val startMs: Long, val title: String)

private val CHAPTER_LINE = Regex("""^\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\s*[-–—:.]?\s+(.+)$""")

/** The description's timestamps, when it lists at least two in increasing order. */
fun parseEpisodeChapters(description: String?): List<EpisodeChapter> {
  val chapters =
    description.orEmpty().lines().mapNotNull { line ->
      val match = CHAPTER_LINE.find(line) ?: return@mapNotNull null
      val (hours, minutes, seconds, title) = match.destructured
      EpisodeChapter(
        startMs =
          ((hours.toLongOrNull() ?: 0L) * 3600 + minutes.toLong() * 60 + seconds.toLong()) * 1000,
        title = title.trim(),
      )
    }
  val ordered = chapters.zipWithNext().all { (a, b) -> a.startMs < b.startMs }
  return if (chapters.size >= 2 && ordered) chapters else emptyList()
}

/** An episode's description and chapters; tapping a chapter plays the episode from there. */
@Composable
fun EpisodeDetails(episode: SongItem, onDismiss: () -> Unit) {
  val playerConnection = LocalPlayerConnection.current ?: return
  val chapters = remember(episode.description) { parseEpisodeChapters(episode.description) }
  LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    item(key = "title") {
      Text(
        text = episode.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
    episode.publishedText?.let { date ->
      item(key = "date") {
        Text(
          text = date,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
    }
    if (chapters.isNotEmpty()) {
      item(key = "chapters_title") {
        Text(
          text = stringResource(R.string.episode_chapters),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
      }
      items(chapters, key = { "chapter_${it.startMs}" }) { chapter ->
        Row(
          Modifier.fillMaxWidth()
            .clickable {
              if (playerConnection.player.currentMediaItem?.mediaId == episode.id) {
                playerConnection.seekTo(chapter.startMs)
              } else {
                playerConnection.playQueue(
                  ListQueue(
                    title = episode.artists.firstOrNull()?.name,
                    items = listOf(episode.toMediaMetadata().toMediaItem()),
                    position = chapter.startMs,
                  )
                )
              }
              onDismiss()
            }
            .padding(vertical = 10.dp)
        ) {
          Text(
            text = makeTimeString(chapter.startMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp),
          )
          Text(
            text = chapter.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    episode.description?.let { description ->
      item(key = "description") {
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
      }
    }
  }
}
