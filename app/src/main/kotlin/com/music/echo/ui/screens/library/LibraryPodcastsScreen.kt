package echo.music.iad1tya.ui.screens.library

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.CONTENT_TYPE_HEADER
import echo.music.iad1tya.constants.CONTENT_TYPE_PLAYLIST
import echo.music.iad1tya.ui.component.EmptyPlaceholder
import echo.music.iad1tya.ui.component.LibraryPlaylistListItem
import echo.music.iad1tya.ui.component.LocalMenuState
import echo.music.iad1tya.ui.component.NavigationTitle
import echo.music.iad1tya.ui.component.continueListening
import echo.music.iad1tya.ui.component.rememberEpisodesInProgress
import echo.music.iad1tya.viewmodels.PodcastsViewModel

/** Saved podcast shows; opening one shows its episodes. */
@Composable
fun LibraryPodcastsScreen(
  navController: NavController,
  onDeselect: () -> Unit,
  viewModel: PodcastsViewModel = hiltViewModel(),
) {
  val menuState = LocalMenuState.current
  val coroutineScope = rememberCoroutineScope()
  val podcasts by viewModel.podcasts.collectAsState()
  val episodesInProgress = rememberEpisodesInProgress()

  LazyColumn(contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
    item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(12.dp))
        FilterChip(
          label = { Text(stringResource(R.string.filter_podcasts)) },
          selected = true,
          colors =
            FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
          onClick = onDeselect,
          shape = RoundedCornerShape(16.dp),
          leadingIcon = {
            Icon(painter = painterResource(R.drawable.close), contentDescription = "")
          },
          modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
      }
    }

    continueListening(episodesInProgress, keyPrefix = "library")

    if (podcasts.isNotEmpty() && episodesInProgress.isNotEmpty()) {
      item(key = "podcasts_title") { NavigationTitle(title = stringResource(R.string.filter_podcasts)) }
    }

    if (podcasts.isEmpty() && episodesInProgress.isEmpty()) {
      item(key = "empty_placeholder") {
        EmptyPlaceholder(
          icon = R.drawable.radio,
          text = stringResource(R.string.library_podcast_empty),
          modifier = Modifier.animateItem(),
        )
      }
    }

    items(items = podcasts, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { podcast ->
      LibraryPlaylistListItem(
        navController = navController,
        menuState = menuState,
        coroutineScope = coroutineScope,
        playlist = podcast,
        modifier = Modifier.animateItem(),
      )
    }
  }
}
