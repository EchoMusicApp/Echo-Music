package echo.music.iad1tya.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.PlaylistSortType
import echo.music.iad1tya.ui.component.CreatePlaylistDialog
import echo.music.iad1tya.ui.component.LibraryPlaylistGridItem
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.NavigationTitle

/** Home row with the user's own playlists, always starting with a "New playlist" card. */
@Composable
fun HomeMyPlaylists(navController: NavController) {
  val database = LocalDatabase.current
  val menuState = LocalMenuState.current
  val scope = rememberCoroutineScope()
  val playlists by database.playlists(PlaylistSortType.CREATE_DATE, true).collectAsState(initial = emptyList())
  var creating by remember { mutableStateOf(false) }

  Column {
    NavigationTitle(title = "Your playlists", onClick = { navController.navigate("library") })
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      item(key = "new_playlist") {
        val shape = RoundedCornerShape(16.dp)
        Column(Modifier.width(160.dp)) {
          Box(
            Modifier.width(160.dp).aspectRatio(1f).clip(shape)
              .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant), shape)
              .clickable { creating = true },
            contentAlignment = Alignment.Center,
          ) {
            Icon(painterResource(R.drawable.add), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text("New playlist", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
      }
      items(playlists.distinctBy { it.id }, key = { it.id }) { playlist ->
        LibraryPlaylistGridItem(navController = navController, menuState = menuState, coroutineScope = scope, playlist = playlist, modifier = Modifier.width(160.dp))
      }
    }
  }
  if (creating) CreatePlaylistDialog(onDismiss = { creating = false }, allowSyncing = false, isLocal = true)
}
