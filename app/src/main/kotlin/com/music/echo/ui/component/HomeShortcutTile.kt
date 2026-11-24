package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.models.YTItem
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.utils.resize

/** Compact tile for the shortcut grid at the top of Home: cover on the left, title on the right. */
@Composable
fun HomeShortcutTile(
  item: YTItem,
  isPinned: Boolean,
  isActive: Boolean,
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier =
      modifier
        .height(56.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          if (isActive) MaterialTheme.colorScheme.secondaryContainer
          else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
  ) {
    AsyncImage(
      model = item.thumbnail?.resize(226, 226),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant),
    )
    Text(
      text = item.title,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color =
        if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Box(
      modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      when {
        isActive && isPlaying ->
          Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
        isPinned ->
          Icon(
            painter = painterResource(R.drawable.ic_push_pin),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
      }
    }
  }
}
