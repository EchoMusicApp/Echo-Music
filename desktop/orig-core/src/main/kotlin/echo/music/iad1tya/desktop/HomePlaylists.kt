package echo.music.iad1tya.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.NavigationRailItem
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.ui.screens.library.LibrarySpotifyViewModel
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
import echo.music.iad1tya.ui.screens.DesktopLazyRow
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
  val spotifyVm: LibrarySpotifyViewModel = hiltViewModel()
  val spotify by spotifyVm.uiState.collectAsState()
  LaunchedEffect(Unit) { spotifyVm.refresh() }
  val playlists by database.playlists(PlaylistSortType.CREATE_DATE, true).collectAsState(initial = emptyList())

  if (playlists.isEmpty() && spotify.playlists.isEmpty()) return
  Column {
    NavigationTitle(title = "Your playlists", onClick = { navController.navigate("library") })
    DesktopLazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      items(playlists.distinctBy { it.id }, key = { it.id }) { playlist ->
        LibraryPlaylistGridItem(navController = navController, menuState = menuState, coroutineScope = scope, playlist = playlist, modifier = Modifier.width(160.dp))
      }
      items(spotify.playlists, key = { "sp_" + it.id }) { pl ->
        val cover = SpotifyMapper.getPlaylistThumbnail(pl)
        Column(Modifier.width(160.dp).clickable {
          navController.navigate("spotify_playlist/${pl.id}?title=${Uri.encode(pl.name)}&cover=${Uri.encode(cover.orEmpty())}")
        }) {
          AsyncImage(model = cover, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.width(160.dp).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest))
          Text(pl.name, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
          Text("Spotify", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
      }
    }
  }
}

/** "New playlist" entry for the left rail. */
@Composable
fun RailNewPlaylistItem() {
  var creating by remember { mutableStateOf(false) }
  NavigationRailItem(selected = false, onClick = { creating = true },
    icon = { Icon(painterResource(R.drawable.add), null) }, label = { Text("New") })
  if (creating) CreatePlaylistDialog(onDismiss = { creating = false }, allowSyncing = false, isLocal = true)
}
