package echo.music.iad1tya.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.echo.utils.hapticScrollEdge
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.utils.completed
import com.music.innertube.utils.parseCookieString
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.GridItemSize
import echo.music.iad1tya.constants.GridItemsSizeKey
import echo.music.iad1tya.constants.GridThumbnailHeight
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.ListItemHeight
import echo.music.iad1tya.constants.ListThumbnailSize
import echo.music.iad1tya.constants.RandomizeHomeOrderKey
import echo.music.iad1tya.constants.ShowHomeCommunityKey
import echo.music.iad1tya.constants.ShowHomeHeroCarouselKey
import echo.music.iad1tya.constants.ShowHomeMoodAndGenresKey
import echo.music.iad1tya.constants.ShowSpeedDialKey
import echo.music.iad1tya.constants.SmallGridThumbnailHeight
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.db.entities.Album
import echo.music.iad1tya.db.entities.Artist
import echo.music.iad1tya.db.entities.LocalItem
import echo.music.iad1tya.db.entities.Playlist
import echo.music.iad1tya.db.entities.PlaylistEntity
import echo.music.iad1tya.db.entities.PlaylistSongMap
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.playback.queues.YouTubeQueue
import echo.music.iad1tya.ui.component.AlbumGridItem
import echo.music.iad1tya.ui.component.ArtistGridItem
import echo.music.iad1tya.ui.component.ChipsRow
import echo.music.iad1tya.ui.component.HomeShortcutTile
import echo.music.iad1tya.ui.component.LocalBottomSheetPageState
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.NavigationTitle
import echo.music.iad1tya.ui.component.PlaylistGridItem
import echo.music.iad1tya.ui.component.podcastHome
import echo.music.iad1tya.ui.component.rememberEpisodesInProgress
import echo.music.iad1tya.ui.component.SongGridItem
import echo.music.iad1tya.ui.component.SongListItem
import echo.music.iad1tya.ui.component.YouTubeGridItem
import echo.music.iad1tya.ui.component.YouTubeListItem
import echo.music.iad1tya.ui.component.shimmer.GridItemPlaceHolder
import echo.music.iad1tya.ui.component.shimmer.ShimmerHost
import echo.music.iad1tya.ui.component.shimmer.TextPlaceholder
import echo.music.iad1tya.ui.menu.AlbumMenu
import echo.music.iad1tya.ui.menu.ArtistMenu
import echo.music.iad1tya.ui.menu.SongMenu
import echo.music.iad1tya.ui.menu.YouTubeAlbumMenu
import echo.music.iad1tya.ui.menu.YouTubeArtistMenu
import echo.music.iad1tya.ui.menu.YouTubePlaylistMenu
import echo.music.iad1tya.ui.menu.YouTubeSongMenu
import echo.music.iad1tya.ui.utils.SnapLayoutInfoProvider
import echo.music.iad1tya.ui.utils.resize
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.CommunityPlaylistItem
import echo.music.iad1tya.viewmodels.HomeViewModel
import echo.music.iad1tya.viewmodels.PodcastsViewModel
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun NavController.navigateToPlaylistItem(playlist: PlaylistItem) {
  when (val playlistId = playlist.id.removePrefix("VL")) {
    "LM" -> navigate("auto_playlist/liked")
    "SE" -> navigate("auto_playlist/downloaded")
    else -> navigate("online_playlist/$playlistId")
  }
}

private const val HomeShortcutCount = 8
private const val HomeSimilarSectionLimit = 2
/** Songs from which a section of songs shows as a list rather than covers. */
private const val HomeSongListMinSongs = 4

/** YouTube Music sections shown before the personal ones, as at the top of its own Home. */
private const val HomeLeadingYouTubeSections = 3

sealed class HomeSection(val id: String, val baseWeight: Int) {
  data object Shortcuts : HomeSection("shortcuts", 100)

  data object HeroCarousel : HomeSection("hero_carousel", 90)

  data object MadeForYou : HomeSection("made_for_you", 80)

  data object AccountPlaylists : HomeSection("account_playlists", 40)

  data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)

  data object FromTheCommunity : HomeSection("from_the_community", 20)

  data class SimilarRecommendation(val index: Int) :
    HomeSection("similar_recommendation_$index", 10)

  data class HomePageSection(val index: Int) : HomeSection("home_page_section_$index", 10)

  data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@Composable
fun CommunityPlaylistCard(
  item: CommunityPlaylistItem,
  onClick: () -> Unit,
  onSongClick: (SongItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val database = LocalDatabase.current
  val playerConnection = LocalPlayerConnection.current
  val scope = rememberCoroutineScope()
  val isDark = isSystemInDarkTheme()

  val containerColor =
    if (isDark) {
      MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

  val dbPlaylist by database.playlistByBrowseId(item.playlist.id).collectAsState(initial = null)
  val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

  Card(
    modifier = modifier.width(320.dp).height(420.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    shape = RoundedCornerShape(28.dp),
    onClick = onClick
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
          Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
              AsyncImage(
                model = item.songs.getOrNull(0)?.thumbnail?.resize(544, 544),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).fillMaxSize()
              )
              AsyncImage(
                model = item.songs.getOrNull(1)?.thumbnail?.resize(544, 544),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).fillMaxSize()
              )
            }
            Row(modifier = Modifier.weight(1f)) {
              AsyncImage(
                model = item.songs.getOrNull(2)?.thumbnail?.resize(544, 544),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).fillMaxSize()
              )
              AsyncImage(
                model = item.songs.getOrNull(3)?.thumbnail?.resize(544, 544),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).fillMaxSize()
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
          Text(
            text = item.playlist.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = item.playlist.author?.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1
          )
        }
      }

      Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
        item.songs.take(3).forEach { song ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = { onSongClick(song) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            AsyncImage(
              model = song.thumbnail.resize(544, 544),
              contentDescription = null,
              modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
      ) {
        IconButton(
          onClick = {
            item.playlist.playEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) }
          },
          modifier =
            Modifier.size(48.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape)
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_widget_play),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(24.dp)
          )
        }

        IconButton(
          onClick = {
            item.playlist.radioEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) }
          },
          modifier =
            Modifier.size(48.dp)
              .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                CircleShape
              )
        ) {
          Icon(
            painter = painterResource(R.drawable.radio),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp)
          )
        }

        IconButton(
          onClick = {
            scope.launch(Dispatchers.IO) {
              if (dbPlaylist?.playlist == null) {
                database.transaction {
                  val playlistEntity =
                    PlaylistEntity(
                        name = item.playlist.title,
                        browseId = item.playlist.id,
                        thumbnailUrl = item.playlist.thumbnail,
                        remoteSongCount =
                          item.playlist.songCountText?.split(" ")?.firstOrNull()?.toIntOrNull(),
                        playEndpointParams = item.playlist.playEndpoint?.params,
                        shuffleEndpointParams = item.playlist.shuffleEndpoint?.params,
                        radioEndpointParams = item.playlist.radioEndpoint?.params
                      )
                      .toggleLike()
                  insert(playlistEntity)
                  scope.launch(Dispatchers.IO) {
                    item.songs
                      .ifEmpty {
                        YouTube.playlist(item.playlist.id).completed().getOrNull()?.songs.orEmpty()
                      }
                      .map { it.toMediaMetadata() }
                      .onEach(::insert)
                      .mapIndexed { index, song ->
                        PlaylistSongMap(
                          songId = song.id,
                          playlistId = playlistEntity.id,
                          position = index,
                          setVideoId = song.setVideoId
                        )
                      }
                      .forEach(::insert)
                  }
                }
              } else {
                database.transaction {
                  val currentPlaylist = dbPlaylist!!.playlist
                  update(currentPlaylist.toggleLike())
                }
              }
            }
          },
          modifier =
            Modifier.size(48.dp)
              .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                CircleShape
              )
        ) {
          Icon(
            painter =
              painterResource(
                if (isBookmarked) R.drawable.library_add_check else R.drawable.library_add
              ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

@OptIn(
  ExperimentalFoundationApi::class,
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen(
  navController: NavController,
  snackbarHostState: SnackbarHostState,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val menuState = LocalMenuState.current
  val bottomSheetPageState = LocalBottomSheetPageState.current
  val database = LocalDatabase.current
  val playerConnection = LocalPlayerConnection.current ?: return
  val haptic = LocalHapticFeedback.current

  val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
  val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

  val quickPicks by viewModel.quickPicks.collectAsState()
  val heroSongs by viewModel.heroSongs.collectAsState()
  val aiRecommendedPlaylist by viewModel.aiRecommendedPlaylist.collectAsState()
  val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
  val similarRecommendations by viewModel.similarRecommendations.collectAsState()
  val accountPlaylists by viewModel.accountPlaylists.collectAsState()
  val homePage by viewModel.homePage.collectAsState()
  val explorePage by viewModel.explorePage.collectAsState()
  val dailyDiscover by viewModel.dailyDiscover.collectAsState()
  val communityPlaylists by viewModel.communityPlaylists.collectAsState()

  val allLocalItems by viewModel.allLocalItems.collectAsState()
  val allYtItems by viewModel.allYtItems.collectAsState()
  val speedDialItems by viewModel.speedDialItems.collectAsState()
  val selectedChip by viewModel.selectedChip.collectAsState()

  val isLoading: Boolean by viewModel.isLoading.collectAsState()
  val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isRandomizing by viewModel.isRandomizing.collectAsState()
  val pullRefreshState = rememberPullToRefreshState()

  val quickPicksLazyGridState = rememberLazyGridState()
  val forgottenFavoritesLazyGridState = rememberLazyGridState()

  val accountName by viewModel.accountName.collectAsState()
  val accountImageUrl by viewModel.accountImageUrl.collectAsState()
  val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
  val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, false)
  val (showSpeedDial) = rememberPreference(ShowSpeedDialKey, true)
  val (showHeroCarousel) = rememberPreference(ShowHomeHeroCarouselKey, true)
  val (showCommunity) = rememberPreference(ShowHomeCommunityKey, false)
  val (showMoodAndGenres) = rememberPreference(ShowHomeMoodAndGenresKey, false)

  val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
  val url = if (isLoggedIn) accountImageUrl else null

  val scope = rememberCoroutineScope()

  var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

  val lazylistState = rememberLazyListState()
  val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
  val currentGridHeight =
    if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
  val backStackEntry by navController.currentBackStackEntryAsState()
  val scrollToTop =
    backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

  var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

  LaunchedEffect(isRefreshing) {
    if (isRefreshing) {
      randomSeed = System.currentTimeMillis()
    }
  }

  val foundInSettings = stringResource(R.string.found_in_settings_content)

  LaunchedEffect(scrollToTop?.value) {
    if (scrollToTop?.value == true) {
      lazylistState.animateScrollToItem(0)
      backStackEntry?.savedStateHandle?.set("scrollToTop", false)
    }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
      .collect { lastVisibleIndex ->
        val len = lazylistState.layoutInfo.totalItemsCount
        if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
          viewModel.loadMoreYouTubeItems(homePage?.continuation)
        }
      }
  }

  NetworkReload(onReload = viewModel::refresh)

  if (selectedChip != null) {
    BackHandler { viewModel.toggleChip(selectedChip) }
  }

  val localGridItem: @Composable (LocalItem) -> Unit = {
    when (it) {
      is Song ->
        SongGridItem(
          song = it,
          modifier =
            Modifier.fillMaxWidth()
              .combinedClickable(
                onClick = {
                  if (it.id == mediaMetadata?.id) {
                    playerConnection.togglePlayPause()
                  } else {
                    playerConnection.playQueue(
                      YouTubeQueue.radio(it.toMediaMetadata()),
                    )
                  }
                },
                onLongClick = {
                  haptic.performHapticFeedback(
                    HapticFeedbackType.LongPress,
                  )
                  menuState.show {
                    SongMenu(
                      originalSong = it,
                      navController = navController,
                      onDismiss = menuState::dismiss,
                    )
                  }
                },
              ),
          isActive = it.id == mediaMetadata?.id,
          isPlaying = isPlaying,
        )
      is Album ->
        AlbumGridItem(
          album = it,
          isActive = it.id == mediaMetadata?.album?.id,
          isPlaying = isPlaying,
          coroutineScope = scope,
          modifier =
            Modifier.fillMaxWidth()
              .combinedClickable(
                onClick = { navController.navigate("album/${it.id}") },
                onLongClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  menuState.show {
                    AlbumMenu(
                      originalAlbum = it,
                      navController = navController,
                      onDismiss = menuState::dismiss
                    )
                  }
                }
              )
        )
      is Artist ->
        ArtistGridItem(
          artist = it,
          modifier =
            Modifier.fillMaxWidth()
              .combinedClickable(
                onClick = { navController.navigate("artist/${it.id}") },
                onLongClick = {
                  haptic.performHapticFeedback(
                    HapticFeedbackType.LongPress,
                  )
                  menuState.show {
                    ArtistMenu(
                      originalArtist = it,
                      coroutineScope = scope,
                      onDismiss = menuState::dismiss,
                    )
                  }
                },
              ),
        )
      is Playlist -> {}
    }
  }

  val openYtItem: (YTItem) -> Unit = { item ->
    when (item) {
      is SongItem ->
        playerConnection.playQueue(
          YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata())
        )
      is AlbumItem -> navController.navigate("album/${item.id}")
      is ArtistItem -> navController.navigate("artist/${item.id}")
      is PlaylistItem -> navController.navigateToPlaylistItem(item)
    }
  }

  val showYtItemMenu: (YTItem) -> Unit = { item ->
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    menuState.show {
      when (item) {
        is SongItem ->
          YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
        is AlbumItem ->
          YouTubeAlbumMenu(
            albumItem = item,
            navController = navController,
            onDismiss = menuState::dismiss
          )
        is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
        is PlaylistItem ->
          YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
      }
    }
  }

  val surpriseMe: () -> Unit = {
    if (isRandomizing) {
      randomizeJob?.cancel()
    } else {
      randomizeJob = scope.launch { viewModel.getRandomItem()?.let(openYtItem) }
    }
  }

  val ytGridItem: @Composable (YTItem) -> Unit = { item ->
    YouTubeGridItem(
      item = item,
      isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
      isPlaying = isPlaying,
      coroutineScope = scope,
      thumbnailRatio = 1f,
      forceCrop = true,
      modifier =
        Modifier.combinedClickable(
          onClick = { openYtItem(item) },
          onLongClick = { showYtItemMenu(item) }
        )
    )
  }

  val isPodcastPage =
    selectedChip != null && (
      selectedChip?.title?.contains("podcast", ignoreCase = true) == true ||
      homePage?.sections?.any { section ->
        section.items.any { (it is SongItem && it.isPodcastEpisode) || (it is PlaylistItem && it.isPodcast) }
      } == true
    )

  val homeSections =
    remember(
      randomizeHomeOrder,
      randomSeed,
      showSpeedDial,
      showHeroCarousel,
      showCommunity,
      showMoodAndGenres,
      speedDialItems,
      heroSongs,
      dailyDiscover,
      accountPlaylists,
      forgottenFavorites,
      communityPlaylists,
      similarRecommendations,
      homePage?.sections,
      explorePage?.moodAndGenres,
      aiRecommendedPlaylist,
      isPodcastPage,
      selectedChip
    ) {
      val list = mutableListOf<HomeSection>()

      if (isPodcastPage) {
        // Exclude hero carousel banner, speed dial, keep listening, and all music sections.
        // Only keep home page sections that contain podcast items.
        homePage?.sections?.indices?.forEach { i ->
          val section = homePage?.sections?.getOrNull(i)
          val hasPodcastItems = section?.items?.any {
            (it is SongItem && it.isPodcastEpisode) || (it is PlaylistItem && it.isPodcast)
          } == true
          if (hasPodcastItems) {
            list.add(HomeSection.HomePageSection(i))
          }
        }
        return@remember list
      }

      // A selected chip shows only that chip's content; the personal sections live on the main Home.
      if (selectedChip == null) {
        if (showSpeedDial && speedDialItems.isNotEmpty()) list.add(HomeSection.Shortcuts)
        if (showHeroCarousel && heroSongs.isNotEmpty()) list.add(HomeSection.HeroCarousel)
        if (
          dailyDiscover?.isNotEmpty() == true || aiRecommendedPlaylist?.second?.isNotEmpty() == true
        )
          list.add(HomeSection.MadeForYou)
        if (accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (showCommunity && communityPlaylists?.isNotEmpty() == true)
          list.add(HomeSection.FromTheCommunity)
        if (forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        similarRecommendations?.indices?.take(HomeSimilarSectionLimit)?.forEach { i ->
          list.add(HomeSection.SimilarRecommendation(i))
        }

        if (showMoodAndGenres && explorePage?.moodAndGenres != null)
          list.add(HomeSection.MoodAndGenres)
      }

      homePage?.sections?.indices?.forEach { i ->
        val section = homePage?.sections?.getOrNull(i)
        if (section != null && !section.isStationListenTogether) {
          list.add(HomeSection.HomePageSection(i))
        }
      }

      // Shortcuts and the carousel stay on top, and the YouTube Music sections stay last because
      // they keep loading more as you scroll; only the personal sections in between are ordered.
      val middleOrder =
        if (randomizeHomeOrder) {
          list.associateWith { section ->
            Random(randomSeed + section.id.hashCode()).nextInt(0, 1000)
          }
        } else {
          list.associateWith { section ->
            when (section) {
              HomeSection.MadeForYou -> 900
              HomeSection.AccountPlaylists -> 800
              HomeSection.FromTheCommunity -> 700
              is HomeSection.SimilarRecommendation -> 600 - section.index
              HomeSection.ForgottenFavorites -> 500
              HomeSection.MoodAndGenres -> 400
              else -> 0
            }
          }
        }

      // As on YouTube Music, its first sections ("Listen again", "Quick picks", ...) come right
      // after the shortcuts; the personal sections follow, then the rest of YouTube Music's, which
      // keep loading as you scroll.
      list.sortedByDescending { section ->
        when (section) {
          HomeSection.Shortcuts -> 20000
          HomeSection.HeroCarousel -> 19000
          is HomeSection.HomePageSection ->
            if (section.index < HomeLeadingYouTubeSections) 10000 - section.index
            else -section.index
          else -> 1000 + (middleOrder[section] ?: 0)
        }
      }
    }

  LaunchedEffect(quickPicks) { quickPicksLazyGridState.scrollToItem(0) }

  LaunchedEffect(forgottenFavorites) { forgottenFavoritesLazyGridState.scrollToItem(0) }

  PullToRefreshBox(
    state = pullRefreshState,
    isRefreshing = isRefreshing,
    onRefresh = viewModel::refresh,
    indicator = {
      PullToRefreshDefaults.LoadingIndicator(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        modifier =
          Modifier.align(Alignment.TopCenter)
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
      )
    }
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
      val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
      val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
      val quickPicksSnapLayoutInfoProvider =
        remember(quickPicksLazyGridState) {
          SnapLayoutInfoProvider(
            lazyGridState = quickPicksLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
              (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
            }
          )
        }
      val forgottenFavoritesSnapLayoutInfoProvider =
        remember(forgottenFavoritesLazyGridState) {
          SnapLayoutInfoProvider(
            lazyGridState = forgottenFavoritesLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
              (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
            }
          )
        }

      val episodesInProgress = rememberEpisodesInProgress(limit = 5)
      val podcastsViewModel: PodcastsViewModel = hiltViewModel()
      val followedPodcasts by podcastsViewModel.podcasts.collectAsState()
      val newEpisodes by podcastsViewModel.newEpisodes.collectAsState()
      LaunchedEffect(isPodcastPage, followedPodcasts) {
        if (isPodcastPage) podcastsViewModel.loadNewEpisodes()
      }

      LazyColumn(
        state = lazylistState,
        modifier = Modifier.hapticScrollEdge(lazylistState),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
      ) {
        // No greeting header: Home opens on its filters, as YouTube Music does, with "Surprise me"
        // as the first of them.
        item(key = "home_chips") {
          ChipsRow(
            chips =
              homePage
                ?.chips
                ?.filter { !it.title.equals("Uploaded", ignoreCase = true) }
                ?.map { it to it.title } ?: emptyList(),
            currentValue = selectedChip,
            onValueUpdate = { viewModel.toggleChip(it) },
            leading = {
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier.size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(enabled = !isRandomizing, onClick = surpriseMe)
              ) {
                if (isRandomizing) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                  Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.home_surprise_me),
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            },
          )
        }

        // The Podcast chip opens with the user's own podcasts; the music Home stays free of them.
        if (isPodcastPage) {
          podcastHome(
            followed = followedPodcasts,
            inProgress = episodesInProgress,
            newEpisodes = newEpisodes,
            navController = navController,
          )
        }

        if (isLoading && homePage?.chips.isNullOrEmpty()) {
          item(key = "chips_shimmer") {
            ShimmerHost {
              Row(
                modifier =
                  Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                repeat(5) {
                  TextPlaceholder(
                    height = 30.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(72.dp)
                  )
                }
              }
            }
          }
        }

        homeSections.forEach { section ->
          when (section) {
            HomeSection.Shortcuts -> {
              item(key = "shortcuts") {
                val columns = if (maxWidth >= 600.dp) 4 else 2
                Column(
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier =
                    Modifier.fillMaxWidth()
                      .windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                      )
                      .padding(horizontal = 12.dp, vertical = 8.dp)
                      .animateItem()
                ) {
                  speedDialItems.take(HomeShortcutCount).chunked(columns).forEach { rowItems ->
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      rowItems.forEach { item ->
                        val isPinned by
                          database.speedDialDao.isPinned(item.id).collectAsState(initial = false)
                        HomeShortcutTile(
                          item = item,
                          isPinned = isPinned,
                          isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                          isPlaying = isPlaying,
                          modifier =
                            Modifier.weight(1f)
                              .clip(RoundedCornerShape(8.dp))
                              .combinedClickable(
                                onClick = { openYtItem(item) },
                                onLongClick = { showYtItemMenu(item) }
                              )
                        )
                      }
                      repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                  }
                }
              }
            }
            HomeSection.MadeForYou -> {
              val discoverItems =
                dailyDiscover.orEmpty().map { it.recommendation }.distinctBy { it.id }
              val discoverSongs = discoverItems.filterIsInstance<SongItem>()
              val tastePlaylist = aiRecommendedPlaylist?.takeIf { it.second.isNotEmpty() }?.first
              item(key = "made_for_you_title") {
                val title = stringResource(R.string.home_made_for_you)
                NavigationTitle(
                  title = title,
                  onPlayAllClick =
                    if (discoverSongs.isNotEmpty()) {
                      {
                        playerConnection.playQueue(
                          ListQueue(
                            title = title,
                            items = discoverSongs.map { it.toMediaMetadata().toMediaItem() }
                          )
                        )
                      }
                    } else null,
                  modifier = Modifier.animateItem()
                )
              }
              item(key = "made_for_you_list") {
                LazyRow(
                  contentPadding =
                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                  modifier = Modifier.animateItem()
                ) {
                  tastePlaylist?.let { playlist ->
                    item(key = "taste_playlist_${playlist.id}") {
                      PlaylistGridItem(
                        playlist = playlist,
                        modifier =
                          Modifier.combinedClickable(
                            onClick = { navController.navigate("local_playlist/${playlist.id}") }
                          )
                      )
                    }
                  }
                  items(discoverItems, key = { it.id }) { item -> ytGridItem(item) }
                }
              }
            }
            HomeSection.HeroCarousel -> {
              heroSongs
                .takeIf { it.isNotEmpty() }
                ?.let { songs ->
                  item(key = "hero_carousel") {
                    val configuration = LocalConfiguration.current
                    val heroWidth = configuration.screenWidthDp.dp - 32.dp

                    val currentSongs by rememberUpdatedState(songs)
                    val carouselState = rememberCarouselState { currentSongs.size }
                    val songIds = remember(songs) { songs.map { it.id } }
                    LaunchedEffect(carouselState, songIds) {
                      if (carouselState.currentItem >= currentSongs.size) {
                        carouselState.scrollToItem(0)
                      }
                      while (true) {
                        delay(5000)
                        val size = currentSongs.size
                        if (size > 1 && !carouselState.isScrollInProgress) {
                          val nextIndex = (carouselState.currentItem + 1) % size
                          carouselState.animateScrollToItem(nextIndex)
                        }
                      }
                    }

                    HorizontalCenteredHeroCarousel(
                      state = carouselState,
                      maxItemWidth = heroWidth,
                      itemSpacing = 8.dp,
                      contentPadding = PaddingValues(horizontal = 16.dp),
                      modifier = Modifier.fillMaxWidth().height(360.dp).animateItem()
                    ) { index ->
                      val originalSong = songs.getOrNull(index) ?: return@HorizontalCenteredHeroCarousel
                      val localSong by
                        database.song(originalSong.id).collectAsState(initial = null)
                      val song = localSong?.toMediaMetadata() ?: originalSong.toMediaMetadata()
                      val isActive = song.id == mediaMetadata?.id

                      Box(
                        modifier =
                          Modifier.fillMaxSize()
                            .maskClip(MaterialTheme.shapes.extraLarge)
                            .maskBorder(
                              BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                              MaterialTheme.shapes.extraLarge
                            )
                            .focusable()
                            .combinedClickable(
                              onClick = {
                                if (isActive) {
                                  playerConnection.togglePlayPause()
                                } else if (localSong?.song?.isLocal == true) {
                                  playerConnection.playQueue(
                                    ListQueue(title = song.title, items = listOf(song.toMediaItem()))
                                  )
                                } else {
                                  playerConnection.playQueue(
                                    YouTubeQueue.radio(song)
                                  )
                                }
                              },
                              onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                  localSong?.let { savedSong ->
                                    SongMenu(
                                      originalSong = savedSong,
                                      navController = navController,
                                      onDismiss = menuState::dismiss
                                    )
                                  } ?: YouTubeSongMenu(
                                    song = originalSong,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                  )
                                }
                              }
                            )
                      ) {
                        AsyncImage(
                          model =
                            ImageRequest.Builder(LocalContext.current)
                              .data(localSong?.thumbnailUrl ?: song.thumbnailUrl)
                              .crossfade(true)
                              .build(),
                          contentDescription = null,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize()
                        )

                        Box(
                          modifier =
                            Modifier.fillMaxSize()
                              .background(
                                Brush.verticalGradient(
                                  colors =
                                    listOf(
                                      Color.Transparent,
                                      Color.Transparent,
                                      Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                              )
                        )

                        if (isActive && isPlaying) {
                          Box(
                            modifier =
                              Modifier.align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.onSurface, CircleShape),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(
                              painter = painterResource(R.drawable.volume_up),
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.onPrimary,
                              modifier = Modifier.size(18.dp)
                            )
                          }
                        }

                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                          Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = song.artists.joinToString { it.name },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }
                    }
                  }
                }
            }
            HomeSection.FromTheCommunity -> {
              communityPlaylists
                ?.takeIf { it.isNotEmpty() }
                ?.let { playlists ->
                  item(key = "community_playlists_title") {
                    NavigationTitle(
                      title = stringResource(R.string.from_the_community),
                      modifier = Modifier.animateItem()
                    )
                  }

                  item(key = "community_playlists_content") {
                    LazyRow(
                      contentPadding = PaddingValues(horizontal = 16.dp),
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                      modifier = Modifier.animateItem()
                    ) {
                      items(playlists.distinctBy { it.playlist.id }, key = { it.playlist.id }) {
                        item ->
                        CommunityPlaylistCard(
                          item = item,
                          onClick = { navController.navigateToPlaylistItem(item.playlist) },
                          onSongClick = { song ->
                            playerConnection.playQueue(
                              YouTubeQueue(
                                song.endpoint ?: WatchEndpoint(videoId = song.id),
                                song.toMediaMetadata()
                              )
                            )
                          }
                        )
                      }
                    }
                  }
                }
            }
            HomeSection.AccountPlaylists -> {
              accountPlaylists
                ?.takeIf { it.isNotEmpty() }
                ?.let { accountPlaylists ->
                  item(key = "account_playlists_title") {
                    NavigationTitle(
                      label = stringResource(R.string.your_youtube_playlists),
                      title = accountName,
                      thumbnail = {
                        if (url != null) {
                          AsyncImage(
                            model =
                              ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(url)
                                .crossfade(false)
                                .build(),
                            placeholder = painterResource(id = R.drawable.person),
                            error = painterResource(id = R.drawable.person),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                              Modifier.size(ListThumbnailSize)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                          )
                        } else {
                          Icon(
                            painter = painterResource(id = R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.size(ListThumbnailSize)
                          )
                        }
                      },
                      onClick = { navController.navigate("account") },
                      modifier = Modifier.animateItem()
                    )
                  }

                  item(key = "account_playlists_list") {
                    LazyRow(
                      contentPadding =
                        WindowInsets.systemBars
                          .only(WindowInsetsSides.Horizontal)
                          .asPaddingValues(),
                      modifier = Modifier.animateItem()
                    ) {
                      items(
                        items = accountPlaylists.distinctBy { it.id },
                        key = { it.id },
                      ) { item ->
                        ytGridItem(item)
                      }
                    }
                  }
                }
            }
            HomeSection.ForgottenFavorites -> {
              forgottenFavorites
                ?.takeIf { it.isNotEmpty() }
                ?.let { forgottenFavorites ->
                  item(key = "forgotten_favorites_title") {
                    val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
                    NavigationTitle(
                      title = forgottenFavoritesTitle,
                      modifier = Modifier.animateItem(),
                      onPlayAllClick = {
                        playerConnection.playQueue(
                          ListQueue(
                            title = forgottenFavoritesTitle,
                            items = forgottenFavorites.distinctBy { it.id }.map { it.toMediaItem() }
                          )
                        )
                      }
                    )
                  }

                  item(key = "forgotten_favorites_list") {
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                      state = forgottenFavoritesLazyGridState,
                      rows = GridCells.Fixed(rows),
                      contentPadding =
                        WindowInsets.systemBars
                          .only(WindowInsetsSides.Horizontal)
                          .asPaddingValues(),
                      flingBehavior =
                        rememberSnapFlingBehavior(forgottenFavoritesSnapLayoutInfoProvider),
                      modifier = Modifier.fillMaxWidth().height(ListItemHeight * rows).animateItem()
                    ) {
                      itemsIndexed(
                        items = forgottenFavorites.distinctBy { it.id },
                        key = { _, it -> it.id }
                      ) { index, originalSong ->
                        val song by
                          database.song(originalSong.id).collectAsState(initial = originalSong)

                        SongListItem(
                          song = song!!,
                          showInLibraryIcon = true,
                          isActive = song!!.id == mediaMetadata?.id,
                          isPlaying = isPlaying,
                          isSwipeable = false,
                          color = Color.Transparent,
                          shape = RoundedCornerShape(12.dp),
                          trailingContent = {
                            IconButton(
                              onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                  SongMenu(
                                    originalSong = song!!,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                  )
                                }
                              }
                            ) {
                              Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                              )
                            }
                          },
                          modifier =
                            Modifier.width(horizontalLazyGridItemWidth)
                              .combinedClickable(
                                onClick = {
                                  if (song!!.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                  } else {
                                    playerConnection.playQueue(
                                      YouTubeQueue.radio(song!!.toMediaMetadata())
                                    )
                                  }
                                },
                                onLongClick = {
                                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                  menuState.show {
                                    SongMenu(
                                      originalSong = song!!,
                                      navController = navController,
                                      onDismiss = menuState::dismiss
                                    )
                                  }
                                }
                              )
                        )
                      }
                    }
                  }
                }
            }
            is HomeSection.SimilarRecommendation -> {
              val recommendation = similarRecommendations?.getOrNull(section.index)
              recommendation?.let {
                item(key = "similar_to_title_${section.index}") {
                  NavigationTitle(
                    label = stringResource(R.string.similar_to),
                    title = recommendation.title.title,
                    thumbnail =
                      recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                        {
                          val shape = RoundedCornerShape(ThumbnailCornerRadius)
                          AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(ListThumbnailSize).clip(shape)
                          )
                        }
                      },
                    onClick = {
                      val recTitle = recommendation.title
                      when (recTitle) {
                        is Song -> navController.navigate("album/${recTitle.album!!.id}")
                        is Album -> navController.navigate("album/${recTitle.id}")
                        is Artist -> navController.navigate("artist/${recTitle.id}")
                        is Playlist -> {}
                      }
                    },
                    modifier = Modifier.animateItem()
                  )
                }

                item(key = "similar_to_list_${section.index}") {
                  LazyRow(
                    contentPadding =
                      WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                    modifier = Modifier.animateItem()
                  ) {
                    items(recommendation.items.distinctBy { it.id }, key = { it.id }) { item ->
                      ytGridItem(item)
                    }
                  }
                }
              }
            }
            is HomeSection.HomePageSection -> {
              val sectionData = homePage?.sections?.getOrNull(section.index)
              sectionData?.let {
                if (sectionData.isStationListenTogether) return@let
                val rawItems = if (isPodcastPage) {
                  sectionData.items.filter { (it is SongItem && it.isPodcastEpisode) || (it is PlaylistItem && it.isPodcast) }
                } else {
                  sectionData.items
                }
                if (rawItems.isEmpty()) return@let

                val sectionSongs = rawItems.filterIsInstance<SongItem>()
                val hasPlayableSongs = sectionSongs.isNotEmpty()

                // A list, as YouTube Music's "Quick picks", only for a section with enough songs to
                // fill it: a single song took a whole row and looked out of place. Fewer songs
                // show as covers, like the other sections.
                val isSongsOnlySection =
                  rawItems.isNotEmpty() &&
                    rawItems.all { it is SongItem } &&
                    sectionSongs.distinctBy { it.id }.size >= HomeSongListMinSongs

                item(key = "home_section_title_${section.index}") {
                  NavigationTitle(
                    title = sectionData.title,
                    label = sectionData.label,
                    // A picture beside the title only for a section about someone ("Similar to",
                    // an artist's picks), named by the line above the title: otherwise it was a
                    // copy of the covers right below it.
                    thumbnail =
                      sectionData.thumbnail?.takeIf { sectionData.label != null }?.let { thumbnailUrl ->
                        {
                          val shape = RoundedCornerShape(ThumbnailCornerRadius)
                          AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(ListThumbnailSize).clip(shape)
                          )
                        }
                      },
                    onClick =
                      sectionData.endpoint?.let { endpoint ->
                        {
                          when {
                            endpoint.browseId == "FEmusic_moods_and_genres" ->
                              navController.navigate("mood_and_genres")
                            endpoint.params != null ->
                              navController.navigate(
                                "youtube_browse/${endpoint.browseId}?params=${endpoint.params}"
                              )
                            else -> navController.navigate("browse/${endpoint.browseId}")
                          }
                        }
                      },
                    onPlayAllClick =
                      if (hasPlayableSongs) {
                        {
                          playerConnection.playQueue(
                            ListQueue(
                              title = sectionData.title,
                              items = sectionSongs.map { it.toMediaMetadata().toMediaItem() }
                            )
                          )
                        }
                      } else null,
                    modifier = Modifier.animateItem()
                  )
                }

                if (isSongsOnlySection) {

                  item(key = "home_section_list_${section.index}") {
                    val rowCount = min(4, sectionSongs.size).coerceAtLeast(1)
                    LazyHorizontalGrid(
                      state = rememberLazyGridState(),
                      rows = GridCells.Fixed(rowCount),
                      contentPadding =
                        WindowInsets.systemBars
                          .only(WindowInsetsSides.Horizontal)
                          .asPaddingValues(),
                      modifier =
                        Modifier.fillMaxWidth()
                          .height(ListItemHeight * rowCount)
                          .padding(bottom = 4.dp)
                          .animateItem()
                    ) {
                      itemsIndexed(
                        items = sectionSongs.distinctBy { it.id },
                        key = { _, it -> it.id }
                      ) { index, song ->
                        YouTubeListItem(
                          item = song,
                          isActive = song.id == mediaMetadata?.id,
                          isPlaying = isPlaying,
                          isSwipeable = false,
                          // Rows on the background, as on YouTube Music: a filled block with
                          // dividers made the list heavier than the covers around it.
                          color = Color.Transparent,
                          shape = RoundedCornerShape(12.dp),
                          trailingContent = {
                            IconButton(
                              onClick = {
                                menuState.show {
                                  YouTubeSongMenu(
                                    song = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                  )
                                }
                              }
                            ) {
                              Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                              )
                            }
                          },
                          modifier =
                            Modifier.width(horizontalLazyGridItemWidth)
                              .combinedClickable(
                                onClick = {
                                  if (song.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                  } else {
                                    playerConnection.playQueue(
                                      YouTubeQueue.radio(song.toMediaMetadata())
                                    )
                                  }
                                },
                                onLongClick = {
                                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                  menuState.show {
                                    YouTubeSongMenu(
                                      song = song,
                                      navController = navController,
                                      onDismiss = menuState::dismiss
                                    )
                                  }
                                }
                              )
                        )
                      }
                    }
                  }
                } else {

                  item(key = "home_section_list_${section.index}") {
                    LazyRow(
                      contentPadding =
                        WindowInsets.systemBars
                          .only(WindowInsetsSides.Horizontal)
                          .asPaddingValues(),
                      modifier = Modifier.animateItem()
                    ) {
                      items(rawItems.distinctBy { it.id }, key = { it.id }) { item ->
                        ytGridItem(item)
                      }
                    }
                  }
                }
              }
            }
            HomeSection.MoodAndGenres -> {
              explorePage?.moodAndGenres?.let { moodAndGenres ->
                item(key = "mood_and_genres_title") {
                  NavigationTitle(
                    title = stringResource(R.string.mood_and_genres),
                    onClick = { navController.navigate("mood_and_genres") },
                    modifier = Modifier.animateItem()
                  )
                }
                item(key = "mood_and_genres_list") {
                  LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    contentPadding = PaddingValues(6.dp),
                    modifier =
                      Modifier.height((MoodAndGenresButtonHeight + 12.dp) * 2 + 12.dp).animateItem()
                  ) {
                    items(moodAndGenres.distinctBy { it.title }, key = { it.title }) {
                      MoodAndGenresButton(
                        title = it.title,
                        onClick = {
                          navController.navigate(
                            "youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}"
                          )
                        },
                        modifier = Modifier.padding(6.dp).width(180.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (
          isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true
        ) {
          item(key = "loading_shimmer") {
            ShimmerHost(modifier = Modifier.animateItem()) {
              // 1. Quick Picks Skeleton
              Row(
                modifier =
                  Modifier.horizontalScroll(rememberScrollState())
                    .padding(
                      WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
                    )
              ) {
                repeat(3) {
                  Spacer(
                    modifier =
                      Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        .width(300.dp)
                        .height(360.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                  )
                }
              }

              // 2. Speed Dial Skeleton
              TextPlaceholder(
                height = 36.dp,
                modifier = Modifier.padding(12.dp).width(200.dp),
              )
              Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                repeat(2) {
                  Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(3) {
                      GridItemPlaceHolder(modifier = Modifier.weight(1f), fillMaxWidth = true)
                    }
                  }
                }
              }

              // 3. Generic Row Skeleton
              TextPlaceholder(
                height = 36.dp,
                modifier = Modifier.padding(12.dp).width(250.dp),
              )
              Row(
                modifier =
                  Modifier.horizontalScroll(rememberScrollState())
                    .padding(
                      WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
                    )
              ) {
                repeat(4) { GridItemPlaceHolder() }
              }
            }
          }
        }

        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(30.dp)) }
      }
    }
  }
}
