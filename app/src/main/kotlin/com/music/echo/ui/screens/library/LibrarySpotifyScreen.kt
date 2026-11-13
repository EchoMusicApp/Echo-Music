package echo.music.iad1tya.ui.screens.library

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.CONTENT_TYPE_HEADER
import echo.music.iad1tya.constants.GridItemSize
import echo.music.iad1tya.constants.GridItemsSizeKey
import echo.music.iad1tya.constants.GridThumbnailHeight
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.extensions.reversed
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.ui.component.GridItem
import echo.music.iad1tya.ui.component.SortHeader
import echo.music.iad1tya.ui.component.shimmer.ListItemPlaceHolder
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.utils.rememberEnumPreference
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.launch

internal const val SPOTIFY_LIKED_SONGS_COVER = "https://misc.scdn.co/liked-songs/liked-songs-640.png"

private enum class SpotifyPlaylistSortType {
  DEFAULT,
  NAME,
  SONG_COUNT,
}

@Composable
fun LibrarySpotifyScreen(
  navController: NavController,
  filterContent: @Composable () -> Unit,
  viewModel: LibrarySpotifyViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val lifecycleOwner = LocalLifecycleOwner.current
  val lazyGridState = rememberLazyGridState()
  val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
  var spotifySortType by rememberSaveable { mutableStateOf(SpotifyPlaylistSortType.DEFAULT) }
  var spotifySortDescending by rememberSaveable { mutableStateOf(false) }
  val collator = remember {
    Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
  }
  val sortedPlaylists =
    remember(uiState.playlists, spotifySortType, spotifySortDescending, collator) {
      val base =
        when (spotifySortType) {
          SpotifyPlaylistSortType.DEFAULT -> uiState.playlists
          SpotifyPlaylistSortType.NAME ->
            uiState.playlists.sortedWith(compareBy(collator) { it.name })
          SpotifyPlaylistSortType.SONG_COUNT ->
            uiState.playlists.sortedBy { it.tracks?.total ?: 0 }
        }
      base.reversed(spotifySortDescending)
    }
  val backStackEntry by navController.currentBackStackEntryAsState()
  val scrollToTop =
    backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

  LaunchedEffect(scrollToTop?.value) {
    if (scrollToTop?.value == true) {
      lazyGridState.animateScrollToItem(0)
      backStackEntry?.savedStateHandle?.set("scrollToTop", false)
    }
  }

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
    LazyVerticalGrid(
      state = lazyGridState,
      columns =
        GridCells.Adaptive(
          minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
        ),
      modifier = Modifier.fillMaxSize(),
      contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
      item(
        key = "filter",
        span = { GridItemSpan(maxLineSpan) },
        contentType = CONTENT_TYPE_HEADER,
      ) {
        filterContent()
      }

      if (uiState.isLoading && uiState.playlists.isEmpty()) {
        item(
          key = "loading",
          span = { GridItemSpan(maxLineSpan) },
        ) {
          ShimmerHost(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            repeat(5) { ListItemPlaceHolder() }
          }
        }
      } else if (!uiState.isAuthenticated) {
        item(
          key = "empty_state",
          span = { GridItemSpan(maxLineSpan) },
        ) {
          SpotifyEmptyState()
        }
      } else {
        // Authenticated Spotify Profile Header
        item(
          key = "profile_header",
          span = { GridItemSpan(maxLineSpan) },
          contentType = CONTENT_TYPE_HEADER,
        ) {
          SpotifyProfileHeader(
            accountName = uiState.accountName ?: "Spotify User",
            avatarUrl = uiState.avatarUrl,
            isRefreshing = uiState.isLoading,
            onRefresh = {
              coroutineScope.launch {
                val success = viewModel.refreshAndAwait()
                Toast.makeText(
                  context,
                  context.getString(
                    if (success) R.string.spotify_sync_success else R.string.spotify_sync_failed
                  ),
                  Toast.LENGTH_SHORT,
                ).show()
              }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          )
        }

        // Header: Your Playlists
        item(
          key = "playlists_header",
          span = { GridItemSpan(maxLineSpan) },
          contentType = CONTENT_TYPE_HEADER,
        ) {
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

        item(
          key = "playlists_sort",
          span = { GridItemSpan(maxLineSpan) },
          contentType = CONTENT_TYPE_HEADER,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
          ) {
            SortHeader(
              sortType = spotifySortType,
              sortDescending = spotifySortDescending,
              onSortTypeChange = { spotifySortType = it },
              onSortDescendingChange = { spotifySortDescending = it },
              sortTypeText = { sortType ->
                when (sortType) {
                  SpotifyPlaylistSortType.DEFAULT -> R.string.default_
                  SpotifyPlaylistSortType.NAME -> R.string.sort_by_name
                  SpotifyPlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                }
              },
            )
          }
        }

        // Liked Songs Pinned Grid Item
        item(key = "liked_songs") {
          val songCount = uiState.likedSongsCount ?: 0
          GridItem(
            title = {
              Text(
                text = stringResource(R.string.spotify_liked_songs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            subtitle = {
              Text(
                text = pluralStringResource(R.plurals.n_song, songCount, songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            badges = {
              Icon(
                painter = painterResource(R.drawable.ic_push_pin),
                contentDescription = null,
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(14.dp).padding(end = 2.dp),
              )
            },
            thumbnailContent = {
              Box(
                modifier = Modifier
                  .fillMaxSize()
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
                  modifier = Modifier.size(32.dp),
                )
                AsyncImage(
                  model = SPOTIFY_LIKED_SONGS_COVER,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            },
            fillMaxWidth = true,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val titleEnc = Uri.encode("Liked Songs")
                val coverEnc = Uri.encode(SPOTIFY_LIKED_SONGS_COVER)
                navController.navigate("spotify_playlist/liked?title=$titleEnc&cover=$coverEnc")
              },
          )
        }

        if (sortedPlaylists.isNotEmpty()) {
          items(
            items = sortedPlaylists,
            key = { it.id },
          ) { playlist ->
            val coverUrl = SpotifyMapper.getPlaylistThumbnail(playlist)
            val countText = playlist.tracks?.total?.let { "$it songs" } ?: ""

            GridItem(
              title = {
                Text(
                  text = playlist.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              subtitle = {
                if (countText.isNotBlank()) {
                  Text(
                    text = countText,
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
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
              },
              fillMaxWidth = true,
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
private fun SpotifyEmptyState(modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp, vertical = 72.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_spotify),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(44.dp),
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = stringResource(R.string.spotify_empty_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = stringResource(R.string.spotify_empty_desc),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun SpotifyProfileHeader(
  accountName: String,
  avatarUrl: String?,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rotation = remember { Animatable(0f) }
  var spinRequested by remember { mutableStateOf(false) }

  LaunchedEffect(isRefreshing, spinRequested) {
    if (isRefreshing || spinRequested) {
      while (isRefreshing || spinRequested) {
        spinRequested = false
        rotation.animateTo(
          targetValue = rotation.value + 360f,
          animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        )
      }
      rotation.snapTo(rotation.value % 360f)
    }
  }

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

      IconButton(
        onClick = {
          spinRequested = true
          onRefresh()
        },
      ) {
        Icon(
          painter = painterResource(R.drawable.cached),
          contentDescription = "Refresh",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.graphicsLayer { rotationZ = rotation.value },
        )
      }
    }
  }
}
