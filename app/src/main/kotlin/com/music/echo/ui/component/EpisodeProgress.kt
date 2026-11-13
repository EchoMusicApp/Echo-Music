package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.innertube.models.SongItem
import echo.music.iad1tya.R
import echo.music.iad1tya.utils.PodcastProgress
import echo.music.iad1tya.utils.joinByBullet
import echo.music.iad1tya.utils.makeTimeString

/** What an episode row shows about listening: a progress bar, or that it was played. */
data class EpisodeState(val fraction: Float, val finished: Boolean, val remainingMs: Long?)

/**
 * The listening state of [item] if it is a podcast episode: from this device's progress when it was
 * played here, otherwise from what YouTube reports for the account.
 */
@Composable
fun rememberEpisodeState(item: SongItem): EpisodeState? {
  val entries by PodcastProgress.entries.collectAsState()
  val local = entries[item.id]
  if (local != null) {
    local.fraction?.let {
      return EpisodeState(it, local.finished, local.remainingMs)
    }
  }
  if (!item.isPodcastEpisode) return null
  val percent = item.playedPercent?.takeIf { it > 0 } ?: return null
  val durationMs = item.duration?.times(1000L)
  return EpisodeState(
    fraction = percent / 100f,
    finished = percent >= PodcastProgress.REMOTE_PLAYED_PERCENT,
    remainingMs = durationMs?.let { it - it * percent / 100 },
  )
}

/** Subtitle of an episode row: date, then remaining time, "played" or the length. */
@Composable
fun episodeSubtitle(item: SongItem, state: EpisodeState?): String =
  joinByBullet(
    item.publishedText ?: item.artists.joinToString { it.name },
    when {
      state?.finished == true -> stringResource(R.string.episode_played)
      state?.remainingMs != null ->
        stringResource(R.string.episode_remaining, makeTimeString(state.remainingMs))
      else -> makeTimeString(item.duration?.times(1000L))
    },
  )

/** Darkens the thumbnail of a played episode and marks it with a check. */
@Composable
fun BoxScope.EpisodePlayedOverlay(state: EpisodeState) {
  if (!state.finished) return
  Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
  Icon(
    painter = painterResource(R.drawable.check),
    contentDescription = stringResource(R.string.episode_played),
    tint = Color.White,
    modifier =
      Modifier.align(Alignment.Center)
        .size(20.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape)
        .padding(2.dp),
  )
}

/** How much of the episode was played, under its subtitle; full once it was played. */
@Composable
fun EpisodeProgressBar(state: EpisodeState, modifier: Modifier = Modifier) {
  Box(
    modifier
      .fillMaxWidth()
      .height(4.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
  ) {
    Box(
      Modifier.fillMaxWidth(if (state.finished) 1f else state.fraction.coerceAtLeast(0.02f))
        .fillMaxHeight()
        .clip(CircleShape)
        .background(
          if (state.finished) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
          else MaterialTheme.colorScheme.primary
        )
    )
  }
}
