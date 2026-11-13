package echo.music.iad1tya.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ChipSortTypeKey
import echo.music.iad1tya.constants.LibraryFilter
import echo.music.iad1tya.ui.component.ChipsRow
import echo.music.iad1tya.ui.component.DefaultDialog
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.utils.onlinePlaylistIdFromUrl
import echo.music.iad1tya.utils.onlinePlaylistRoute
import echo.music.iad1tya.utils.rememberEnumPreference
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private enum class LibrarySourceTab {
  IN_APP,
  SPOTIFY,
}

@Composable
fun LibraryScreen(navController: NavController) {
  var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
  var selectedSourceTab by rememberSaveable {
    mutableStateOf(
      if (filterType == LibraryFilter.SPOTIFY) LibrarySourceTab.SPOTIFY else LibrarySourceTab.IN_APP
    )
  }
  val coroutineScope = rememberCoroutineScope()
  val pagerState =
    rememberPagerState(initialPage = selectedSourceTab.ordinal) { LibrarySourceTab.entries.size }
  val effectiveFilter =
    if (filterType == LibraryFilter.SPOTIFY) LibraryFilter.LIBRARY else filterType

  LaunchedEffect(filterType) {
    if (filterType == LibraryFilter.SPOTIFY) {
      selectedSourceTab = LibrarySourceTab.SPOTIFY
      filterType = LibraryFilter.LIBRARY
    }
  }

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }
      .distinctUntilChanged()
      .collect { page ->
        val tab = LibrarySourceTab.entries[page]
        if (selectedSourceTab != tab) {
          selectedSourceTab = tab
        }
      }
  }

  LaunchedEffect(selectedSourceTab, effectiveFilter) {
    if (
      effectiveFilter == LibraryFilter.LIBRARY &&
        pagerState.currentPage != selectedSourceTab.ordinal &&
        !pagerState.isScrollInProgress
    ) {
      pagerState.animateScrollToPage(selectedSourceTab.ordinal)
    }
  }

  var showFabMenu by remember { mutableStateOf(false) }
  var showImportMenu by remember { mutableStateOf(false) }
  var showYoutubeImportDialog by remember { mutableStateOf(false) }
  var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
  var showCreatePlaylistOptionsDialog by rememberSaveable { mutableStateOf(false) }
  var showTasteGenerationDialog by rememberSaveable { mutableStateOf(false) }
  var showAiPlaylistDialog by rememberSaveable { mutableStateOf(false) }
  val context = LocalContext.current

  BackHandler(
    enabled = filterType != LibraryFilter.LIBRARY || selectedSourceTab == LibrarySourceTab.SPOTIFY
  ) {
    if (filterType != LibraryFilter.LIBRARY) {
      filterType = LibraryFilter.LIBRARY
    } else {
      selectedSourceTab = LibrarySourceTab.IN_APP
    }
  }

  val filterContent =
    @Composable {
      Row {
        ChipsRow(
          chips =
            listOf(
              LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
              LibraryFilter.SONGS to stringResource(R.string.filter_songs),
              LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
              LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
              LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts),
              LibraryFilter.LOCAL to stringResource(R.string.filter_local),
            ),
          currentValue =
            if (
              selectedSourceTab == LibrarySourceTab.SPOTIFY || filterType == LibraryFilter.SPOTIFY
            ) {
              LibraryFilter.LIBRARY
            } else {
              filterType
            },
          onValueUpdate = {
            if (selectedSourceTab == LibrarySourceTab.SPOTIFY) {
              selectedSourceTab = LibrarySourceTab.IN_APP
              filterType = it
            } else {
              filterType =
                if (filterType == it) {
                  LibraryFilter.LIBRARY
                } else {
                  it
                }
            }
          },
          modifier = Modifier.weight(1f),
        )
      }
    }

  val overviewHeaderContent =
    @Composable {
      Column(modifier = Modifier.fillMaxWidth()) {
        filterContent()

        LibrarySourceTabs(
          selectedTab = selectedSourceTab,
          indicatorFraction = pagerState.currentPage + pagerState.currentPageOffsetFraction,
          onSelectTab = { tab ->
            selectedSourceTab = tab
            if (pagerState.currentPage != tab.ordinal) {
              coroutineScope.launch { pagerState.animateScrollToPage(tab.ordinal) }
            }
          },
        )
      }
    }

  val currentInsets = LocalPlayerAwareWindowInsets.current
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  val fabHeightPlusSpacing = 128.dp
  val newInsets =
    remember(currentInsets, density, layoutDirection) {
      object : WindowInsets {
        override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
          currentInsets.getLeft(density, layoutDirection)

        override fun getTop(density: Density) = currentInsets.getTop(density)

        override fun getRight(density: Density, layoutDirection: LayoutDirection) =
          currentInsets.getRight(density, layoutDirection)

        override fun getBottom(density: Density) =
          currentInsets.getBottom(density) + with(density) { fabHeightPlusSpacing.roundToPx() }
      }
    }
  val pagerInsets =
    remember(newInsets, density, layoutDirection) {
      object : WindowInsets {
        override fun getLeft(density: Density, layoutDirection: LayoutDirection) =
          newInsets.getLeft(density, layoutDirection)

        override fun getTop(density: Density) = 0

        override fun getRight(density: Density, layoutDirection: LayoutDirection) =
          newInsets.getRight(density, layoutDirection)

        override fun getBottom(density: Density) = newInsets.getBottom(density)
      }
    }

  CompositionLocalProvider(LocalPlayerAwareWindowInsets provides newInsets) {
    Box(modifier = Modifier.fillMaxSize()) {
      when (effectiveFilter) {
        LibraryFilter.LIBRARY -> {
          val topPadding = with(density) { newInsets.getTop(density).toDp() }
          Column(modifier = Modifier.fillMaxSize().padding(top = topPadding)) {
            overviewHeaderContent()

            CompositionLocalProvider(LocalPlayerAwareWindowInsets provides pagerInsets) {
              HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
              ) { page ->
                when (LibrarySourceTab.entries[page]) {
                  LibrarySourceTab.IN_APP ->
                    LibraryMixScreen(
                      navController = navController,
                      filterContent = {},
                    )
                  LibrarySourceTab.SPOTIFY ->
                    LibrarySpotifyScreen(navController = navController, filterContent = {})
                }
              }
            }
          }
        }
        LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
        LibraryFilter.SONGS ->
          LibrarySongsScreen(navController, { filterType = LibraryFilter.LIBRARY })
        LibraryFilter.ALBUMS ->
          LibraryAlbumsScreen(navController, { filterType = LibraryFilter.LIBRARY })
        LibraryFilter.ARTISTS ->
          LibraryArtistsScreen(navController, { filterType = LibraryFilter.LIBRARY })
        LibraryFilter.LOCAL ->
          LocalSongScreen(
            navController = navController,
            onBack = { filterType = LibraryFilter.LIBRARY },
            isEmbedded = true,
            filterContent = filterContent,
          )
        LibraryFilter.SPOTIFY -> Unit
        LibraryFilter.PODCASTS ->
          LibraryPodcastsScreen(navController, { filterType = LibraryFilter.LIBRARY })
      }

      val bottomPadding = with(density) { currentInsets.getBottom(density).toDp() }
      Box(modifier = Modifier.fillMaxSize().padding(end = 16.dp, bottom = bottomPadding + 20.dp)) {
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
          androidx.compose.material3.FloatingActionButton(
            onClick = { showFabMenu = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
          ) {
            Icon(painter = painterResource(R.drawable.add), contentDescription = "Add")
          }
        }
      }
    }
  }

  if (showFabMenu) {
    @kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    androidx.compose.material3.ModalBottomSheet(
      onDismissRequest = { showFabMenu = false },
      sheetState =
        @kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      Column(modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp)) {
        Material3SettingsGroup(
          compact = true,
          items =
            listOf(
              Material3SettingsItem(
                title = { Text(stringResource(R.string.create_playlist)) },
                icon = painterResource(R.drawable.add),
                onClick = {
                  showFabMenu = false
                  showCreatePlaylistOptionsDialog = true
                },
              ),
              Material3SettingsItem(
                title = { Text("Create from Taste") },
                icon = painterResource(R.drawable.tast),
                tintIcon = false,
                iconShape = androidx.compose.foundation.shape.CircleShape,
                onClick = {
                  showFabMenu = false
                  showTasteGenerationDialog = true
                },
              ),
              Material3SettingsItem(
                title = { Text(stringResource(R.string.import_playlist)) },
                icon = painterResource(R.drawable.download),
                onClick = {
                  showFabMenu = false
                  showImportMenu = true
                },
              ),
            ),
        )
      }
    }
  }

  if (showImportMenu) {
    @kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    androidx.compose.material3.ModalBottomSheet(
      onDismissRequest = { showImportMenu = false },
      sheetState =
        @kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      Column(modifier = Modifier.padding(bottom = 24.dp)) {
        androidx.compose.material3.ListItem(
          headlineContent = { Text(stringResource(R.string.import_from_spotify)) },
          modifier =
            Modifier.clickable {
              showImportMenu = false
              navController.navigate("settings/spotify_import")
            },
        )
        androidx.compose.material3.ListItem(
          headlineContent = { Text(stringResource(R.string.import_from_youtube_music)) },
          modifier =
            Modifier.clickable {
              showImportMenu = false
              showYoutubeImportDialog = true
            },
        )
      }
    }
  }

  if (showYoutubeImportDialog) {
    var url by remember { mutableStateOf(TextFieldValue("")) }
    val invalidUrlMessage = stringResource(R.string.invalid_playlist_url)
    echo.music.iad1tya.ui.component.TextFieldDialog(
      icon = { Icon(painter = painterResource(R.drawable.link), contentDescription = null) },
      title = {
        Column {
          Text(text = stringResource(R.string.import_youtube_music_playlist_title))
          Text(
            text = stringResource(R.string.import_youtube_music_playlist_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
      },
      initialTextFieldValue = url,
      autoFocus = true,
      onDismiss = { showYoutubeImportDialog = false },
      onDone = { finalUrl ->
        val route = onlinePlaylistRoute(onlinePlaylistIdFromUrl(finalUrl))
        if (route != null) {
          navController.navigate(route)
        } else {
          android.widget.Toast.makeText(
              context,
              invalidUrlMessage,
              android.widget.Toast.LENGTH_SHORT,
            )
            .show()
        }
        showYoutubeImportDialog = false
      },
    )
  }

  if (showCreatePlaylistDialog) {
    echo.music.iad1tya.ui.component.CreatePlaylistDialog(
      onDismiss = { showCreatePlaylistDialog = false },
      initialTextFieldValue = null,
      allowSyncing = true,
      onPlaylistCreated = { playlistId ->
        showCreatePlaylistDialog = false
        navController.navigate("local_playlist/$playlistId")
      },
    )
  }

  if (showCreatePlaylistOptionsDialog) {
    DefaultDialog(
      onDismiss = { showCreatePlaylistOptionsDialog = false },
      title = { Text(stringResource(R.string.create_playlist)) },
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        androidx.compose.material3.Surface(
          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          modifier = Modifier.fillMaxWidth(),
        ) {
          echo.music.iad1tya.ui.component.PreferenceEntry(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(stringResource(R.string.create_playlist_normally)) },
            description = "Standard playlist creation",
            onClick = {
              showCreatePlaylistOptionsDialog = false
              showCreatePlaylistDialog = true
            },
          )
        }

        androidx.compose.material3.Surface(
          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          modifier = Modifier.fillMaxWidth(),
        ) {
          echo.music.iad1tya.ui.component.PreferenceEntry(
            icon = {
              androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ai_pfp),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape),
              )
            },
            title = { Text(stringResource(R.string.create_playlist_with_ai)) },
            description = "AI-powered playlist generation",
            onClick = {
              showCreatePlaylistOptionsDialog = false
              showAiPlaylistDialog = true
            },
          )
        }
      }
    }
  }

  if (showAiPlaylistDialog) {
    echo.music.iad1tya.ui.component.CreateAiPlaylistDialog(
      onDismiss = { showAiPlaylistDialog = false },
      onPlaylistCreated = { playlistId ->
        showAiPlaylistDialog = false
        navController.navigate("local_playlist/$playlistId")
      },
    )
  }

  if (showTasteGenerationDialog) {
    echo.music.iad1tya.ui.component.CreateFromTasteDialog(
      onDismiss = { showTasteGenerationDialog = false },
      onPlaylistCreated = { playlistId ->
        showTasteGenerationDialog = false
        navController.navigate("local_playlist/$playlistId")
      },
    )
  }
}

@Composable
private fun LibrarySourceTabs(
  selectedTab: LibrarySourceTab,
  indicatorFraction: Float,
  onSelectTab: (LibrarySourceTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clampedFraction = indicatorFraction.coerceIn(0f, 1f)
  val activeColor = MaterialTheme.colorScheme.onSurface
  val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        .padding(top = 4.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth()) {
      LibrarySourceTabItem(
        label = stringResource(R.string.in_app_library),
        selected = selectedTab == LibrarySourceTab.IN_APP,
        textColor = lerp(activeColor, inactiveColor, clampedFraction),
        onClick = { onSelectTab(LibrarySourceTab.IN_APP) },
        modifier = Modifier.weight(1f),
      )
      LibrarySourceTabItem(
        label = stringResource(R.string.filter_spotify),
        selected = selectedTab == LibrarySourceTab.SPOTIFY,
        textColor = lerp(inactiveColor, activeColor, clampedFraction),
        onClick = { onSelectTab(LibrarySourceTab.SPOTIFY) },
        modifier = Modifier.weight(1f),
      )
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val tabWidth = maxWidth / 2
      HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        modifier = Modifier.align(Alignment.BottomStart),
      )
      Box(
        modifier =
          Modifier.offset(x = tabWidth * clampedFraction)
            .width(tabWidth)
            .height(2.dp)
            .background(activeColor)
      )
    }
  }
}

@Composable
private fun LibrarySourceTabItem(
  label: String,
  selected: Boolean,
  textColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.clickable(onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      color = textColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
    )
  }
}
