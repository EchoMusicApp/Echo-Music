package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R

@Composable
fun NavigationTitle(
  title: String,
  modifier: Modifier = Modifier,
  label: String? = null,
  thumbnail: (@Composable () -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  onPlayAllClick: (() -> Unit)? = null,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        .clickable(enabled = onClick != null) { onClick?.invoke() }
        .padding(horizontal = 12.dp, vertical = 12.dp),
  ) {
    thumbnail?.invoke()

    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
      label?.let { label ->
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
      )
    }

    // A round play button instead of a "Play all" pill on every section: the titles stay light.
    onPlayAllClick?.let { playAllClick ->
      androidx.compose.material3.FilledIconButton(
        onClick = playAllClick,
        colors =
          androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
        modifier = Modifier.size(36.dp),
      ) {
        Icon(
          painter = painterResource(R.drawable.play),
          contentDescription = stringResource(R.string.play_all),
          modifier = Modifier.size(20.dp),
        )
      }
    }

    if (onClick != null) {
      Icon(
        painter = painterResource(R.drawable.arrow_forward),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
