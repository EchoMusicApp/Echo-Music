package echo.music.iad1tya.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.ExportDirectoryUriKey
import echo.music.iad1tya.export.ExportStatus
import echo.music.iad1tya.export.ExportableSong
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.ui.utils.formatFileSize
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSelectionScreen(
  navController: NavController,
  viewModel: ExportViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val filteredSongs by viewModel.filteredSongs.collectAsState()
  val selectedIds by viewModel.selectedSongIds.collectAsState()
  val selectedSize by viewModel.selectedTotalSize.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val batchState by viewModel.batchState.collectAsState()

  val (exportDirectoryUri) = rememberPreference(key = ExportDirectoryUriKey, defaultValue = "")

  val allFilteredSelected = filteredSongs.isNotEmpty() && filteredSongs.all { it.id in selectedIds }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
        )
    ) {
      Spacer(
        Modifier.windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
        )
      )
      Spacer(modifier = Modifier.height(60.dp))

      // Search Bar
      PaddingBox {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.searchQuery.value = it },
          placeholder = { Text(stringResource(R.string.search_export_songs)) },
          leadingIcon = {
            Icon(
              painter = painterResource(R.drawable.search),
              contentDescription = null
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.searchQuery.value = "" }, onLongClick = {}) {
                Icon(
                  painter = painterResource(R.drawable.close),
                  contentDescription = null
                )
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Selection Header Row
      PaddingBox {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = {
              if (allFilteredSelected) viewModel.deselectAll() else viewModel.selectAll()
            }
          ) {
            Text(
              if (allFilteredSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
              fontWeight = FontWeight.SemiBold
            )
          }

          Text(
            text = "${selectedIds.size} selected (${formatFileSize(selectedSize)})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Song List
      if (filteredSongs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Text(
            stringResource(R.string.no_eligible_songs),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
        ) {
          items(
            items = filteredSongs,
            key = { it.id }
          ) { song ->
            val isSelected = song.id in selectedIds
            ExportableSongRow(
              song = song,
              isSelected = isSelected,
              onToggle = { viewModel.toggleSelection(song.id) }
            )
          }

          item {
            Spacer(modifier = Modifier.height(100.dp))
          }
        }
      }
    }

    // Bottom Action Bar (Export Selected)
    Surface(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
        ),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Button(
          onClick = {
            if (exportDirectoryUri.isBlank()) {
              Toast.makeText(context, R.string.export_directory_not_set, Toast.LENGTH_SHORT).show()
            } else {
              viewModel.startExportSelected(context)
              navController.navigateUp()
            }
          },
          enabled = selectedIds.isNotEmpty() && !batchState.isExporting,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          shape = RoundedCornerShape(26.dp),
          elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
          Icon(
            painter = painterResource(R.drawable.download),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(10.dp))
          Text(
            text = if (selectedIds.isNotEmpty()) {
              "${stringResource(R.string.export_selected)} (${selectedIds.size} • ${formatFileSize(selectedSize)})"
            } else {
              stringResource(R.string.export_selected)
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    TopAppBar(
      title = { Text(stringResource(R.string.select_songs)) },
      navigationIcon = {
        IconButton(
          onClick = navController::navigateUp,
          onLongClick = navController::backToMain,
        ) {
          Icon(
            painterResource(R.drawable.arrow_back),
            contentDescription = null,
          )
        }
      }
    )
  }
}

@Composable
private fun ExportableSongRow(
  song: ExportableSong,
  isSelected: Boolean,
  onToggle: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle)
      .padding(vertical = 8.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = song.thumbnailUrl,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
      error = painterResource(R.drawable.music_note),
      placeholder = painterResource(R.drawable.music_note)
    )

    Spacer(Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = "${song.artist} • ${formatFileSize(song.sizeBytes)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(Modifier.width(8.dp))

    // Status Chip if completed/skipped/failed
    if (song.status != ExportStatus.PENDING) {
      val statusText = when (song.status) {
        ExportStatus.COMPLETED -> stringResource(R.string.export_status_completed)
        ExportStatus.SKIPPED -> stringResource(R.string.export_status_skipped)
        ExportStatus.FAILED -> stringResource(R.string.export_status_failed)
        ExportStatus.EXPORTING -> stringResource(R.string.export_status_exporting)
        else -> ""
      }
      val statusColor = when (song.status) {
        ExportStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        ExportStatus.SKIPPED -> MaterialTheme.colorScheme.outline
        ExportStatus.FAILED -> MaterialTheme.colorScheme.error
        ExportStatus.EXPORTING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
      }

      Box(
        modifier = Modifier
          .padding(end = 8.dp)
          .clip(CircleShape)
          .background(statusColor.copy(alpha = 0.15f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelSmall,
          color = statusColor,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Checkbox(
      checked = isSelected,
      onCheckedChange = { onToggle() }
    )
  }
}

@Composable
private fun PaddingBox(content: @Composable () -> Unit) {
  Box(modifier = Modifier.padding(horizontal = 16.dp)) {
    content()
  }
}
