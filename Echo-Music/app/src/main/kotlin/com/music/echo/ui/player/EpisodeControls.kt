package echo.music.iad1tya.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import echo.music.iad1tya.utils.PodcastProgress

/**
 * Controls shown while a podcast episode plays: skip back and forward by seconds (next and
 * previous episode stay on the regular transport buttons) and the podcast speed, which a tap
 * changes and the app remembers for every episode.
 */
@Composable
fun EpisodeControls(enabled: Boolean, color: Color, onSeek: (Long) -> Unit) {
  val speed by PodcastProgress.speed.collectAsState()
  val colors = ButtonDefaults.textButtonColors(contentColor = color)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TextButton(
      onClick = { onSeek(-PodcastProgress.SKIP_BACK_MS) },
      enabled = enabled,
      colors = colors,
      modifier = Modifier.heightIn(min = 48.dp),
    ) {
      Text(stringResource(R.string.episode_skip_back))
    }
    TextButton(
      onClick = PodcastProgress::cycleSpeed,
      enabled = enabled,
      colors = colors,
      modifier = Modifier.heightIn(min = 48.dp),
    ) {
      Text(PodcastProgress.speedLabel(speed))
    }
    TextButton(
      onClick = { onSeek(PodcastProgress.SKIP_FORWARD_MS) },
      enabled = enabled,
      colors = colors,
      modifier = Modifier.heightIn(min = 48.dp),
    ) {
      Text(stringResource(R.string.episode_skip_forward))
    }
  }
}
