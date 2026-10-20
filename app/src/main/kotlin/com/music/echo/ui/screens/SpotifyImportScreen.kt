@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.spotifyimport.SpotifyImportProgressUi
import echo.music.iad1tya.spotifyimport.SpotifyImportSourceType
import echo.music.iad1tya.spotifyimport.SpotifyImportSourceUi
import echo.music.iad1tya.spotifyimport.SpotifyImportSummaryUi
import echo.music.iad1tya.spotifyimport.SpotifyImportUiState
import echo.music.iad1tya.spotifyimport.SpotifyImportViewModel
import echo.music.iad1tya.ui.component.DefaultDialog
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain

@Composable
fun SpotifyImportScreen(
  navController: NavController,
  spotifyImportViewModel: SpotifyImportViewModel = hiltViewModel(),
) {
  val state by spotifyImportViewModel.uiState.collectAsStateWithLifecycle()
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  var showSpotifySources by remember { mutableStateOf(false) }
  var showAddByLink by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.spotify_import_title)) },
        navigationIcon = {
          IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
            Icon(painterResource(R.drawable.arrow_back), null)
          }
        },
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(
            LocalPlayerAwareWindowInsets.current.only(
              WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
          ),
      contentPadding =
        PaddingValues(
          top = innerPadding.calculateTopPadding(),
          bottom = innerPadding.calculateBottomPadding() + 32.dp,
          start = 16.dp,
          end = 16.dp
        ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
          title = "Spotify Import",
          items =
            spotifyImportItems(
              state = state,
              viewModel = spotifyImportViewModel,
              onSelectSources = { showSpotifySources = true },
              onAddByLink = { showAddByLink = true },
            ),
        )
      }
    }
  }

  SpotifyImportDialogs(
    state = state,
    viewModel = spotifyImportViewModel,
    showAddByLink = showAddByLink,
    showSpotifySources = showSpotifySources,
    onDismissAddByLink = { showAddByLink = false },
    onDismissSources = { showSpotifySources = false },
  )
}

@Composable
private fun spotifyImportItems(
  state: SpotifyImportUiState,
  viewModel: SpotifyImportViewModel,
  onSelectSources: () -> Unit,
  onAddByLink: () -> Unit,
): List<Material3SettingsItem> {
  if (!state.isAuthenticated) {
    return listOf(
      Material3SettingsItem(
        title = { Text(stringResource(R.string.spotify_connect)) },
        description = { Text(stringResource(R.string.spotify_login_temporarily_disabled)) },
        icon = painterResource(R.drawable.ic_spotify),
        enabled = false,
        onClick = null,
      ),
      Material3SettingsItem(
        title = { Text(stringResource(R.string.spotify_import_by_link)) },
        description = { Text(stringResource(R.string.spotify_import_by_link_desc)) },
        icon = painterResource(R.drawable.link),
        enabled = state.progress == null && !state.isLoading,
        onClick = onAddByLink,
      ),
    )
  }

  val idle = !state.isLoading && state.progress == null
  return listOf(
    Material3SettingsItem(
      title = {
        Text(
          if (state.accountName.isNotBlank())
            stringResource(R.string.spotify_connected_as, state.accountName)
          else stringResource(R.string.spotify_account)
        )
      },
      description =
        if (state.isLoading) {
          { Text(stringResource(R.string.spotify_loading_library)) }
        } else null,
      icon = painterResource(R.drawable.ic_spotify),
      enabled = true,
      onClick = null,
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_select_sources)) },
      description = {
        Text(
          if (state.hasSources) stringResource(R.string.spotify_available_count, state.sources.size)
          else stringResource(R.string.spotify_no_sources)
        )
      },
      icon = painterResource(R.drawable.playlist_play),
      enabled = state.hasSources && state.progress == null,
      onClick = onSelectSources,
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_import_by_link)) },
      description = { Text(stringResource(R.string.spotify_import_by_link_desc)) },
      icon = painterResource(R.drawable.link),
      enabled = idle,
      onClick = onAddByLink,
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_import_selected)) },
      description = {
        Text(stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size))
      },
      icon = painterResource(R.drawable.playlist_add),
      enabled = state.canImport,
      onClick = { viewModel.importSelectedSources() },
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_refresh)) },
      description = { Text(stringResource(R.string.spotify_import_desc)) },
      icon = painterResource(R.drawable.sync),
      enabled = idle,
      onClick = { viewModel.loadSources() },
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.action_logout)) },
      description = { Text(stringResource(R.string.action_logout_desc)) },
      icon = painterResource(R.drawable.logout),
      enabled = idle,
      onClick = { viewModel.logout() },
    ),
  )
}

@Composable
private fun SpotifyImportDialogs(
  state: SpotifyImportUiState,
  viewModel: SpotifyImportViewModel,
  showAddByLink: Boolean,
  showSpotifySources: Boolean,
  onDismissAddByLink: () -> Unit,
  onDismissSources: () -> Unit,
) {
  if (showAddByLink) {
    SpotifyAddByLinkDialog(
      enabled = !state.isLoading && state.progress == null,
      onDismiss = onDismissAddByLink,
      onAdd = { link ->
        onDismissAddByLink()
        viewModel.addPlaylistByUrl(link)
      },
    )
  }

  if (showSpotifySources && state.isAuthenticated) {
    SpotifySourcePickerSheet(
      state = state,
      onDismiss = onDismissSources,
      onToggleSource = viewModel::toggleSource,
      onSelectAll = viewModel::selectAllSources,
      onClearSelection = viewModel::clearSelection,
      onImport = {
        onDismissSources()
        viewModel.importSelectedSources()
      },
    )
  }

  state.errorMessage?.let { error ->
    SpotifyErrorDialog(
      message = error,
      onDismiss = { viewModel.dismissError() },
    )
  }

  state.summary?.let { summary ->
    SpotifyImportSummaryDialog(
      summary = summary,
      onDismiss = { viewModel.dismissSummary() },
    )
  }

  state.progress?.let { progress ->
    SpotifyImportProgressDialog(
      progress = progress,
      onCancel = { viewModel.cancelImport() },
    )
  }
}

@Composable
private fun SpotifyImportProgressDialog(
  progress: SpotifyImportProgressUi,
  onCancel: () -> Unit,
) {
  DefaultDialog(
    onDismiss = onCancel,
    title = { Text(stringResource(R.string.spotify_import_in_progress)) },
    buttons = { TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) } },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        stringResource(
          R.string.spotify_import_progress_step,
          progress.sourceTitle,
          progress.completedSources,
          progress.totalSources,
          progress.matchedTracks,
          progress.totalTracks
        )
      )
      LinearProgressIndicator(
        progress = { progress.percent.toFloat() / 100f },
        modifier = Modifier.fillMaxWidth().clip(CircleShape),
      )
    }
  }
}

@Composable
private fun SpotifyAddByLinkDialog(
  enabled: Boolean,
  onDismiss: () -> Unit,
  onAdd: (String) -> Unit,
) {
  var link by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

  echo.music.iad1tya.ui.component.TextFieldDialog(
    icon = { Icon(painter = painterResource(R.drawable.link), contentDescription = null) },
    title = {
      Column {
        Text(text = stringResource(R.string.spotify_import_by_link))
        Text(
          text = stringResource(R.string.spotify_import_by_link_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    },
    initialTextFieldValue = link,
    autoFocus = true,
    placeholder = { Text(stringResource(R.string.spotify_import_by_link_hint)) },
    onDismiss = onDismiss,
    onDone = { finalUrl ->
      if (enabled && finalUrl.isNotBlank()) {
        onAdd(finalUrl)
      }
    }
  )
}

@Composable
private fun SpotifySourcePickerSheet(
  state: SpotifyImportUiState,
  onDismiss: () -> Unit,
  onToggleSource: (String) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onImport: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    modifier = Modifier.fillMaxHeight(),
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 20.dp).padding(bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.spotify_select_sources),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(
          onClick = onClearSelection,
        ) {
          Text(stringResource(R.string.spotify_clear_selection))
        }
        TextButton(
          onClick = onSelectAll,
        ) {
          Text(stringResource(R.string.spotify_select_all))
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 6.dp),
      ) {
        items(
          items = state.sources,
          key = { it.id },
          contentType = { it.type },
        ) { source ->
          SpotifySourceRow(
            source = source,
            selected = source.id in state.selectedSourceIds,
            onClick = { onToggleSource(source.id) },
          )
        }
      }

      Button(
        onClick = onImport,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = state.canImport,
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(stringResource(R.string.spotify_import_selected))
      }
    }
  }
}

@Composable
private fun SpotifySourceRow(
  source: SpotifyImportSourceUi,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val subtitle =
    when {
      source.subtitle.isNotBlank() -> source.subtitle
      source.type == SpotifyImportSourceType.LIKED_SONGS ->
        stringResource(R.string.spotify_liked_songs_desc)
      else -> stringResource(R.string.spotify_account)
    }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color =
      if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
    onClick = onClick,
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SpotifySourceThumbnail(source)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = source.title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            source.trackCount?.let { stringResource(R.string.spotify_track_count, it) } ?: subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Checkbox(
        checked = selected,
        onCheckedChange = { onClick() },
      )
    }
  }
}

@Composable
private fun SpotifySourceThumbnail(source: SpotifyImportSourceUi) {
  Box(
    modifier =
      Modifier.size(48.dp)
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    contentAlignment = Alignment.Center,
  ) {
    if (!source.thumbnailUrl.isNullOrBlank()) {
      AsyncImage(
        model = source.thumbnailUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Icon(
        painter =
          painterResource(
            if (source.type == SpotifyImportSourceType.LIKED_SONGS) {
              R.drawable.favorite
            } else {
              R.drawable.playlist_play
            },
          ),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
private fun SpotifyErrorDialog(
  message: String,
  onDismiss: () -> Unit,
) {
  DefaultDialog(
    onDismiss = onDismiss,
    title = { Text("Import failed") },
    buttons = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SpotifyImportSummaryDialog(
  summary: SpotifyImportSummaryUi,
  onDismiss: () -> Unit,
) {
  DefaultDialog(
    onDismiss = onDismiss,
    title = { Text(stringResource(R.string.spotify_import_complete)) },
    buttons = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text =
          stringResource(
            R.string.spotify_import_summary,
            summary.sourceCount,
            summary.importedTracks,
            summary.failedTracks,
          ),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
      )
      summary.sources.forEach { source ->
        Text(
          text =
            stringResource(
              R.string.spotify_source_summary,
              source.title,
              source.importedTracks,
              source.totalTracks,
            ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
