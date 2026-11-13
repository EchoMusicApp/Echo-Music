package echo.music.iad1tya.ui.screens.playlist

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.innertube.models.WatchEndpoint
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AppBarHeight
import echo.music.iad1tya.constants.ListThumbnailSize
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.spotify.SpotifyMapper
import echo.music.iad1tya.spotify.models.SpotifyTrack
import echo.music.iad1tya.ui.component.EmptyPlaceholder
import echo.music.iad1tya.ui.component.ListItem
import echo.music.iad1tya.ui.component.OnlineBlur
import echo.music.iad1tya.ui.component.PlayingIndicator
import echo.music.iad1tya.utils.joinByBullet
import echo.music.iad1tya.utils.listItemShape
import echo.music.iad1tya.utils.makeTimeString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistDetailScreen(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
  playlistId: String,
  playlistTitle: String,
  playlistCover: String,
  viewModel: SpotifyPlaylistDetailViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val playerConnection = LocalPlayerConnection.current
  val mediaMetadata by
    playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
  val isPlaying by
    playerConnection?.isEffectivelyPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val lazyListState = rememberLazyListState()
  val context = LocalContext.current

  var isSearching by rememberSaveable { mutableStateOf(false) }
  var query by
    rememberSaveable(stateSaver = TextFieldValue.Saver) {
      mutableStateOf(TextFieldValue())
    }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(isSearching) {
    if (isSearching) {
      focusRequester.requestFocus()
    }
  }

  if (isSearching) {
    BackHandler {
      isSearching = false
      query = TextFieldValue()
    }
  }

  val refreshRotation = remember { Animatable(0f) }
  var spinRequested by remember { mutableStateOf(false) }
  LaunchedEffect(uiState.isLoading, spinRequested) {
    if (uiState.isLoading || spinRequested) {
      while (uiState.isLoading || spinRequested) {
        spinRequested = false
        refreshRotation.animateTo(
          targetValue = refreshRotation.value + 360f,
          animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        )
      }
      refreshRotation.snapTo(refreshRotation.value % 360f)
    }
  }

  val filteredTracks =
    remember(uiState.tracks, query) {
      if (query.text.isEmpty()) {
        uiState.tracks
      } else {
        uiState.tracks.filter { track ->
          track.name.contains(query.text, ignoreCase = true) ||
            track.artists.any { it.name.contains(query.text, ignoreCase = true) }
        }
      }
    }
  val displayedTracks = if (isSearching) filteredTracks else uiState.tracks

  val effectiveTitle = uiState.title.ifBlank { playlistTitle }
  val effectiveCover = uiState.coverUrl ?: playlistCover

  val isPlaylistCurrent =
    uiState.tracks.any {
      it.name.equals(mediaMetadata?.title, ignoreCase = true)
    }
  val isPlaylistPlaying = isPlaying && isPlaylistCurrent

  val transparentAppBar by remember {
    derivedStateOf {
      lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 150
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (uiState.isLoading && uiState.tracks.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(36.dp),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    } else {
      LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
          LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
      ) {
        // Playlist Header Hero Section
        if (!isSearching) {
          item(key = "header") {
            SpotifyPlaylistHeader(
              title = effectiveTitle,
              coverUrl = effectiveCover,
              tracks = uiState.tracks,
              totalCount = if (uiState.totalCount > 0) uiState.totalCount else uiState.tracks.size,
              isPlaylistPlaying = isPlaylistPlaying,
              onPlayClick = {
                if (isPlaylistCurrent) {
                  playerConnection?.togglePlayPause()
                } else {
                  uiState.tracks.firstOrNull()?.let { firstTrack ->
                    viewModel.playTrack(
                      track = firstTrack,
                      onSuccess = { songItem ->
                        playerConnection?.playQueue(
                          YouTubeQueue(
                            endpoint = WatchEndpoint(videoId = songItem.id),
                            preloadItem = songItem.toMediaMetadata(),
                          )
                        )
                      },
                      onError = {
                        coroutineScope.launch {
                          snackbarHostState.showSnackbar("Unable to match stream on YouTube/Addon")
                        }
                      },
                    )
                  }
                }
              },
              onShuffle = {
                val shuffled = uiState.tracks.shuffled()
                shuffled.firstOrNull()?.let { firstTrack ->
                  viewModel.playTrack(
                    track = firstTrack,
                    onSuccess = { songItem ->
                      playerConnection?.playQueue(
                        YouTubeQueue(
                          endpoint = WatchEndpoint(videoId = songItem.id),
                          preloadItem = songItem.toMediaMetadata(),
                        )
                      )
                    },
                    onError = {
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Unable to match stream on YouTube/Addon")
                      }
                    },
                  )
                }
              },
              onShare = {
                val shareUrl =
                  if (playlistId == "liked") {
                    "https://open.spotify.com/collection/tracks"
                  } else {
                    "https://open.spotify.com/playlist/$playlistId"
                  }
                val intent =
                  Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$effectiveTitle: $shareUrl")
                  }
                context.startActivity(Intent.createChooser(intent, null))
              },
            )
          }
        }

        if (isSearching && displayedTracks.isEmpty()) {
          item(key = "empty_search") {
            EmptyPlaceholder(
              icon = R.drawable.search,
              text = stringResource(R.string.no_results_found),
              modifier = Modifier.padding(top = 64.dp),
            )
          }
        }

        // Track List Items (styled as grouped Nothing OS cards)
        itemsIndexed(
          items = displayedTracks,
          key = { index, track -> "${track.id}_$index" },
        ) { index, track ->
          val isCurrentSong = mediaMetadata?.title.equals(track.name, ignoreCase = true)
          val isResolving = uiState.resolvingTrackId == track.id
          val coverUrl = SpotifyMapper.getTrackThumbnail(track)
          val durationText =
            if (track.durationMs > 0) makeTimeString(track.durationMs.toLong()) else null

          ListItem(
            title = track.name,
            subtitle = {
              val artistsText = track.artists.joinToString(", ") { it.name }
              val sub = joinByBullet(artistsText, durationText)
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
              Box(
                modifier = Modifier.size(ListThumbnailSize),
                contentAlignment = Alignment.Center,
              ) {
                AsyncImage(
                  model = coverUrl,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier.fillMaxSize()
                      .clip(RoundedCornerShape(ThumbnailCornerRadius))
                      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )

                if (isResolving) {
                  Box(
                    modifier =
                      Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      strokeWidth = 2.dp,
                      color = Color.White,
                    )
                  }
                } else if (isCurrentSong) {
                  Box(
                    modifier =
                      Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (isPlaying) {
                      PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(20.dp),
                      )
                    } else {
                      Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                }
              }
            },
            trailingContent = {
              IconButton(
                onClick = {
                  val trackUrl = "https://open.spotify.com/track/${track.id}"
                  val intent =
                    Intent().apply {
                      action = Intent.ACTION_SEND
                      type = "text/plain"
                      putExtra(Intent.EXTRA_TEXT, "${track.name}: $trackUrl")
                    }
                  context.startActivity(Intent.createChooser(intent, null))
                }
              ) {
                Icon(
                  painter = painterResource(R.drawable.more_vert),
                  contentDescription = "More",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
              }
            },
            shape = listItemShape(index, displayedTracks.size),
            isActive = isCurrentSong,
            modifier =
              Modifier.fillMaxWidth().clickable {
                if (isCurrentSong) {
                  playerConnection?.togglePlayPause()
                } else {
                  viewModel.playTrack(
                    track = track,
                    onSuccess = { songItem ->
                      playerConnection?.playQueue(
                        YouTubeQueue(
                          endpoint = WatchEndpoint(videoId = songItem.id),
                          preloadItem = songItem.toMediaMetadata(),
                        )
                      )
                    },
                    onError = {
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                          "Could not match \"${track.name}\" to a playable stream"
                        )
                      }
                    },
                  )
                }
              },
          )
        }
      }
    }

    // Transparent TopAppBar overlaid directly inside Box over the LazyColumn
    TopAppBar(
      title = {
        if (isSearching) {
          TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
              Text(
                text = stringResource(R.string.search),
                style = MaterialTheme.typography.titleLarge,
              )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors =
              TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
              ),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
          )
        } else {
          AnimatedVisibility(
            visible = !transparentAppBar,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            Text(
              text = effectiveTitle,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      },
      navigationIcon = {
        IconButton(
          onClick = {
            if (isSearching) {
              isSearching = false
              query = TextFieldValue()
            } else {
              navController.navigateUp()
            }
          },
          modifier =
            Modifier.background(
              MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
              CircleShape,
            ),
        ) {
          Icon(
            painter = painterResource(R.drawable.arrow_back),
            contentDescription = "Back",
          )
        }
      },
      actions = {
        if (isSearching) {
          if (query.text.isNotEmpty()) {
            IconButton(onClick = { query = TextFieldValue() }) {
              Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Clear search",
              )
            }
          }
        } else {
          IconButton(
            onClick = { isSearching = true },
            modifier =
              Modifier.background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                CircleShape,
              ),
          ) {
            Icon(
              painter = painterResource(R.drawable.search),
              contentDescription = stringResource(R.string.search),
            )
          }
          Spacer(Modifier.width(6.dp))
          IconButton(
            onClick = {
              spinRequested = true
              coroutineScope.launch {
                val success = viewModel.refreshAndAwait()
                Toast.makeText(
                    context,
                    context.getString(
                      if (success) R.string.spotify_sync_success else R.string.spotify_sync_failed
                    ),
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              }
            },
            modifier =
              Modifier.background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                CircleShape,
              ),
          ) {
            Icon(
              painter = painterResource(R.drawable.cached),
              contentDescription = "Refresh",
              modifier = Modifier.graphicsLayer { rotationZ = refreshRotation.value },
            )
          }
        }
      },
      colors =
        TopAppBarDefaults.topAppBarColors(
          containerColor =
            if (transparentAppBar && !isSearching) Color.Transparent
            else MaterialTheme.colorScheme.surface
        ),
      scrollBehavior = scrollBehavior,
    )

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }
}

@Composable
private fun SpotifyPlaylistHeader(
  title: String,
  coverUrl: String?,
  tracks: List<SpotifyTrack>,
  totalCount: Int,
  isPlaylistPlaying: Boolean,
  onPlayClick: () -> Unit,
  onShuffle: () -> Unit,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
  val headerOffset = with(density) { -(systemBarsTopPadding + AppBarHeight).roundToPx() }

  Box(modifier = modifier.fillMaxWidth()) {
    // Ambient Blur Backdrop extending under status bar and TopAppBar
    OnlineBlur(
      thumbnailUrl = coverUrl,
      modifier = Modifier.matchParentSize().offset { IntOffset(0, headerOffset) },
    )

    Column(
      modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(16.dp))

      // Hero Artwork
      Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center,
      ) {
        AsyncImage(
          model = ImageRequest.Builder(LocalContext.current).data(coverUrl).build(),
          contentDescription = title,
          modifier =
            Modifier.then(
                if (LocalConfiguration.current.screenWidthDp > 600) Modifier.size(300.dp)
                else Modifier.fillMaxWidth().aspectRatio(1f)
              )
              .clip(RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Crop,
        )
      }

      Spacer(Modifier.height(28.dp))

      // Playlist Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Spacer(Modifier.height(8.dp))

      // Subtitle / Source
      Text(
        text = "Spotify Playlist",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
      )

      Spacer(Modifier.height(16.dp))

      // Metadata Badge
      val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs.toLong() } }
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 32.dp),
      ) {
        Text(
          text =
            buildString {
              append(pluralStringResource(R.plurals.n_song, totalCount, totalCount))
              if (totalDurationMs > 0) {
                append(" • ")
                val totalSec = totalDurationMs / 1000
                val hours = totalSec / 3600
                val minutes = (totalSec % 3600) / 60
                if (hours > 0) {
                  append("${hours}h ${minutes}m")
                } else {
                  append("${minutes}m")
                }
              }
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }

      Spacer(Modifier.height(24.dp))

      // Action Buttons (Play / Shuffle / Share)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Play Button (Uses primary color and toggles play/pause icon)
        Button(
          onClick = onPlayClick,
          shape = ButtonDefaults.shape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          contentPadding = PaddingValues(vertical = 12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              painter =
                painterResource(if (isPlaylistPlaying) R.drawable.pause else R.drawable.play),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text =
                if (isPlaylistPlaying) stringResource(R.string.pause)
                else stringResource(R.string.play),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        // Shuffle Button
        Button(
          onClick = onShuffle,
          shape = ButtonDefaults.shape,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          contentPadding = PaddingValues(vertical = 12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              painter = painterResource(R.drawable.shuffle),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.shuffle),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        // Share Button
        Surface(
          onClick = onShare,
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(48.dp),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              painter = painterResource(R.drawable.share),
              contentDescription = stringResource(R.string.share),
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
