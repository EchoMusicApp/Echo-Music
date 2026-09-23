package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.playback.queues.LocalAlbumRadio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import echo.music.iad1tya.utils.reportException

@Composable
fun SpeedDialGridItem(
  item: YTItem,
  isPinned: Boolean,
  modifier: Modifier = Modifier,
  isActive: Boolean = false,
  isPlaying: Boolean = false,
) {
  val database = LocalDatabase.current
  val playerConnection = LocalPlayerConnection.current
  val scope = rememberCoroutineScope()

  Box(
    modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(ThumbnailCornerRadius)),
    contentAlignment = Alignment.Center
  ) {
    ItemThumbnail(
      thumbnailUrl = item.thumbnail,
      isActive = isActive,
      isPlaying = isPlaying,
      shape = RoundedCornerShape(ThumbnailCornerRadius),
      forceCrop = true,
      modifier = Modifier.fillMaxSize()
    )

    if (item is SongItem && !isActive) {
      OverlayPlayButton(visible = true)
    }

    if (item is AlbumItem && !isActive) {
      AlbumPlayButton(
        visible = true,
        onClick = {
          if (playerConnection == null) return@AlbumPlayButton
          scope.launch(Dispatchers.IO) {
            var albumWithSongs = database.albumWithSongs(item.id).first()
            if (albumWithSongs?.songs.isNullOrEmpty()) {
              YouTube.album(item.id)
                .onSuccess { albumPage ->
                  database.transaction { insert(albumPage) }
                  albumWithSongs = database.albumWithSongs(item.id).first()
                }
                .onFailure { reportException(it) }
            }
            albumWithSongs?.let {
              withContext(Dispatchers.Main) { playerConnection.playQueue(LocalAlbumRadio(it)) }
            }
          }
        }
      )
    }

    if (isPinned) {
      Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.BottomEnd
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_push_pin),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
