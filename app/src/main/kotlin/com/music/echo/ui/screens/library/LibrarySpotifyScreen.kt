package echo.music.iad1tya.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ListThumbnailSize
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.ui.component.ListItem
import echo.music.iad1tya.utils.joinByBullet
import echo.music.iad1tya.utils.listItemShape

internal const val SPOTIFY_LIKED_SONGS_COVER = "https://misc.scdn.co/liked-songs/liked-songs-640.png"

@Composable
fun LibrarySpotifyScreen(
  navController: NavController,
  filterContent: @Composable () -> Unit,
  viewModel: LibrarySpotifyViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
      item(key = "filter") {
        filterContent()
      }

      if (uiState.isLoading && uiState.playlists.isEmpty()) {
        item(key = "loading") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(36.dp),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      } else if (!uiState.isAuthenticated) {
        item(key = "connect_card") {
          SpotifyConnectBanner(
            onConnect = {
              navController.navigate("settings/spotify_import")
            },
            onRefresh = {
              viewModel.refresh()
            }
          )
        }
      } else {
        // Authenticated Spotify Profile Header
        item(key = "profile_header") {
          SpotifyProfileHeader(
            accountName = uiState.accountName ?: "Spotify User",
            avatarUrl = uiState.avatarUrl,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          )
        }

        // Header: Your Playlists
        item(key = "playlists_header") {
          val totalPlaylists = uiState.playlists.size + 1
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.spotify_playlists_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = pluralStringResource(R.plurals.n_playlist, totalPlaylists, totalPlaylists),
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }

        // Liked Songs Pinned Item (uses native ListItem and matches Spotify aesthetic)
        item(key = "liked_songs") {
          val totalPlaylists = uiState.playlists.size + 1
          val songCount = uiState.likedSongsCount ?: 0

          ListItem(
            title = stringResource(R.string.spotify_liked_songs),
            subtitle = {
              val sub = joinByBullet(
                "Spotify",
                pluralStringResource(R.plurals.n_song, songCount, songCount),
              )
              Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            thumbnailContent = {
              Box(
                modifier = Modifier
                  .size(ListThumbnailSize)
                  .clip(RoundedCornerShape(ThumbnailCornerRadius))
                  .background(
                    Brush.linearGradient(
                      listOf(
                        Color(0xFF450AF5),
                        Color(0xFF8E8EE5),
                        Color(0xFFABEDD8),
                      )
                    )
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  painter = painterResource(R.drawable.favorite),
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp),
                )
                AsyncImage(
                  model = SPOTIFY_LIKED_SONGS_COVER,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            },
            trailingContent = {
              Icon(
                painter = painterResource(R.drawable.ic_push_pin),
                contentDescription = "Pinned",
                tint = Color(0xFF1DB954),
                modifier = Modifier
                  .size(18.dp)
                  .padding(end = 4.dp),
              )
            },
            shape = listItemShape(0, totalPlaylists),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val titleEnc = Uri.encode("Liked Songs")
                val coverEnc = Uri.encode(SPOTIFY_LIKED_SONGS_COVER)
                navController.navigate("spotify_playlist/liked?title=$titleEnc&cover=$coverEnc")
              },
          )
        }

        if (uiState.playlists.isNotEmpty()) {
          itemsIndexed(
            items = uiState.playlists,
            key = { _, pl -> pl.id }
          ) { index, playlist ->
            val coverUrl = SpotifyMapper.getPlaylistThumbnail(playlist)
            val owner = playlist.owner?.displayName?.takeIf { it.isNotBlank() }
            val countText = playlist.tracks?.total?.let { "$it songs" }
            val totalPlaylists = uiState.playlists.size + 1

            ListItem(
              title = playlist.name,
              subtitle = {
                val sub = joinByBullet(owner, countText)
                if (sub.isNotBlank()) {
                  Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              },
              thumbnailContent = {
                AsyncImage(
                  model = coverUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
              },
              trailingContent = {
                Icon(
                  painter = painterResource(R.drawable.more_vert),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier.padding(end = 4.dp),
                )
              },
              shape = listItemShape(index + 1, totalPlaylists),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  val titleEnc = Uri.encode(playlist.name)
                  val coverEnc = Uri.encode(coverUrl.orEmpty())
                  navController.navigate("spotify_playlist/${playlist.id}?title=$titleEnc&cover=$coverEnc")
                },
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun SpotifyConnectBanner(
  onConnect: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(Color(0xFF1DB954).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_spotify),
          contentDescription = null,
          tint = Color(0xFF1DB954),
          modifier = Modifier.size(36.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.spotify_live_library),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.spotify_live_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onConnect,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(0.85f),
      ) {
        Text(
          text = stringResource(R.string.spotify_connect_account),
          fontWeight = FontWeight.SemiBold,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      TextButton(
        onClick = onRefresh,
      ) {
        Icon(
          painter = painterResource(R.drawable.cached),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Already connected? Tap to refresh",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun SpotifyProfileHeader(
  accountName: String,
  avatarUrl: String?,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        if (!avatarUrl.isNullOrBlank()) {
          AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape),
          )
        } else {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              painter = painterResource(R.drawable.person),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(24.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = accountName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFF1DB954))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Spotify Connected",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      IconButton(onClick = onRefresh) {
        Icon(
          painter = painterResource(R.drawable.cached),
          contentDescription = "Refresh",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
