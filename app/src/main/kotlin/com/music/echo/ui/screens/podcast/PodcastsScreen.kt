@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ThumbnailCornerRadius
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.models.toMediaMetadata
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.ui.component.EmptyPlaceholder
import echo.music.iad1tya.ui.component.NavigationTitle
import echo.music.iad1tya.ui.component.podcastHome
import echo.music.iad1tya.ui.component.rememberEpisodesInProgress
import echo.music.iad1tya.viewmodels.PodcastsViewModel

@Composable
fun PodcastsScreen(
  navController: NavController,
  viewModel: PodcastsViewModel = hiltViewModel(),
) {
  val playerConnection = LocalPlayerConnection.current
  val podcasts by viewModel.podcasts.collectAsState()
  val newEpisodes by viewModel.newEpisodes.collectAsState()
  val exploreSections by viewModel.exploreSections.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isLoadingMore by viewModel.isLoadingMore.collectAsState()
  val episodesInProgress = rememberEpisodesInProgress(limit = 10)

  val lazyListState = rememberLazyListState()
  val pullRefreshState = rememberPullToRefreshState()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val scrollToTop =
    backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

  LaunchedEffect(scrollToTop?.value) {
    if (scrollToTop?.value == true) {
      lazyListState.animateScrollToItem(0)
      backStackEntry?.savedStateHandle?.set("scrollToTop", false)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.loadNewEpisodes()
    viewModel.loadPodcastSections()
  }

  LaunchedEffect(lazyListState) {
    snapshotFlow {
      val firstIndex = lazyListState.firstVisibleItemIndex
      val firstOffset = lazyListState.firstVisibleItemScrollOffset
      val lastIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      val total = lazyListState.layoutInfo.totalItemsCount
      Triple(firstIndex, firstOffset, lastIndex >= total - 4 && total > 0)
    }
      .collect { (firstIndex, firstOffset, nearEnd) ->
        if (firstIndex > 0 || firstOffset > 20 || nearEnd) {
          viewModel.loadMorePodcastSections()
        }
      }
  }

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
    },
    modifier = Modifier.fillMaxSize(),
  ) {
    LazyColumn(
      state = lazyListState,
      contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
      modifier = Modifier.fillMaxSize(),
    ) {
      podcastHome(
        followed = podcasts,
        inProgress = episodesInProgress,
        newEpisodes = newEpisodes,
        navController = navController,
      )

      exploreSections.forEachIndexed { sectionIndex, section ->
        if (section.items.isNotEmpty()) {
          item(key = "podcast_explore_title_$sectionIndex") {
            NavigationTitle(
              title = section.title,
              label = section.label,
              onClick =
                section.endpoint?.let { endpoint ->
                  {
                    when {
                      endpoint.params != null ->
                        navController.navigate(
                          "youtube_browse/${endpoint.browseId}?params=${endpoint.params}",
                        )
                      else -> navController.navigate("browse/${endpoint.browseId}")
                    }
                  }
                },
            )
          }

          item(key = "podcast_explore_row_$sectionIndex") {
            val sectionSongs =
              remember(section.items) { section.items.filterIsInstance<SongItem>() }
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              itemsIndexed(
                section.items,
                key = { _, item -> "section_${sectionIndex}_${item.id}" },
              ) { itemIndex, item ->
                when (item) {
                  is PlaylistItem -> {
                    Column(
                      Modifier.width(140.dp).clickable {
                        navController.navigate("online_playlist/${item.id}")
                      },
                    ) {
                      AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                          Modifier.fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                      )
                      Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                      )
                      item.author?.name?.let { author ->
                        Text(
                          text = author,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                  is SongItem -> {
                    Column(
                      Modifier.width(140.dp).clickable {
                        val startIndex = sectionSongs.indexOf(item).coerceAtLeast(0)
                        playerConnection?.playQueue(
                          ListQueue(
                            title = section.title,
                            items = sectionSongs.map { it.toMediaMetadata().toMediaItem() },
                            startIndex = startIndex,
                          ),
                        )
                      },
                    ) {
                      AsyncImage(
                        model = item.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                          Modifier.fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                      )
                      Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                      )
                      if (item.artists.isNotEmpty()) {
                        Text(
                          text = item.artists.joinToString { it.name },
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                  else -> Unit
                }
              }
            }
          }
        }
      }

      if (isLoadingMore) {
        item(key = "podcast_loading_more") {
          Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
          }
        }
      }

      if (isLoading && exploreSections.isEmpty() && podcasts.isEmpty()) {
        item(key = "podcast_loading") {
          Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      } else if (
        !isLoading &&
          exploreSections.isEmpty() &&
          podcasts.isEmpty() &&
          episodesInProgress.isEmpty()
      ) {
        item(key = "podcast_empty") {
          EmptyPlaceholder(
            icon = R.drawable.radio,
            text = stringResource(R.string.library_podcast_empty),
            modifier = Modifier.animateItem(),
          )
        }
      }
    }
  }
}
