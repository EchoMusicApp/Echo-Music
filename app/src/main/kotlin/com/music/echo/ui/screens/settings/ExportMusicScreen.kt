package echo.music.iad1tya.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.EnableExportAsMp3Key
import echo.music.iad1tya.constants.ExportConflictStrategyKey
import echo.music.iad1tya.constants.ExportDirectoryUriKey
import echo.music.iad1tya.constants.ExportMetadataKey
import echo.music.iad1tya.export.ExportConflictStrategy
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.ui.utils.formatFileSize
import echo.music.iad1tya.utils.launchSafely
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.ExportViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportMusicScreen(
  navController: NavController,
  viewModel: ExportViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val eligibleSongs by viewModel.eligibleSongs.collectAsState()
  val totalEligibleSize by viewModel.totalEligibleSize.collectAsState()
  val batchState by viewModel.batchState.collectAsState()

  val (exportDirectoryUri, onExportDirectoryUriChange) =
    rememberPreference(key = ExportDirectoryUriKey, defaultValue = "")
  val (enableExportAsMp3, onEnableExportAsMp3Change) =
    rememberPreference(key = EnableExportAsMp3Key, defaultValue = false)
  val (conflictStrategyRaw, onConflictStrategyChange) =
    rememberPreference(key = ExportConflictStrategyKey, defaultValue = "SKIP")
  val (exportMetadata, onExportMetadataChange) =
    rememberPreference(key = ExportMetadataKey, defaultValue = true)

  val conflictStrategy =
    remember(conflictStrategyRaw) {
      ExportConflictStrategy.fromString(conflictStrategyRaw)
    }

  val exportDirectoryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        } catch (e: SecurityException) {
          Timber.w(e, "Unable to persist export directory permission")
          Toast.makeText(context, R.string.system_picker_unavailable, Toast.LENGTH_LONG).show()
          return@rememberLauncherForActivityResult
        }
        onExportDirectoryUriChange(uri.toString())
      }
    }

  val pickerNotAvailableMsg = stringResource(R.string.picker_not_available)
  val exportDirUnavailableMsg = stringResource(R.string.export_directory_unavailable)

  val launchExportPicker: () -> Unit = {
    exportDirectoryLauncher.launchSafely(null) {
      val fallbackDir = context.getExternalFilesDir(null)
      if (fallbackDir != null) {
        onExportDirectoryUriChange(fallbackDir.toUri().toString())
        Toast.makeText(context, pickerNotAvailableMsg, Toast.LENGTH_LONG).show()
      } else {
        Timber.e("getExternalFilesDir returned null after picker launch failure")
        Toast.makeText(context, exportDirUnavailableMsg, Toast.LENGTH_LONG).show()
      }
    }
  }

  var showConflictDialog by remember { mutableStateOf(false) }

  if (showConflictDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showConflictDialog = false },
      title = { Text(stringResource(R.string.export_conflict_strategy)) },
      text = {
        Column {
          ExportConflictStrategy.entries.forEach { strategy ->
            val label =
              when (strategy) {
                ExportConflictStrategy.SKIP -> stringResource(R.string.export_conflict_skip)
                ExportConflictStrategy.REPLACE -> stringResource(R.string.export_conflict_replace)
                ExportConflictStrategy.RENAME -> stringResource(R.string.export_conflict_rename)
              }
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    onConflictStrategyChange(strategy.name)
                    showConflictDialog = false
                  }
                  .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = (strategy == conflictStrategy),
                onClick = null,
              )
              Spacer(Modifier.width(12.dp))
              Text(label, style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showConflictDialog = false }) {
          Text(stringResource(android.R.string.cancel))
        }
      },
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier.windowInsetsPadding(
            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
          )
          .verticalScroll(scrollState)
          .padding(horizontal = 16.dp)
    ) {
      Spacer(
        Modifier.windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
        )
      )
      Spacer(modifier = Modifier.height(16.dp))

      // Section 1: Storage / Destination Folder
      Material3SettingsGroup(
        scrollState = scrollState,
        title = stringResource(R.string.export_directory),
        items =
          listOf(
            Material3SettingsItem(
              icon = painterResource(R.drawable.folder_managed),
              title = { Text(stringResource(R.string.export_directory)) },
              description = {
                Text(
                  if (exportDirectoryUri.isBlank()) stringResource(R.string.not_set)
                  else exportDirectoryUri
                )
              },
              onClick = launchExportPicker,
            )
          ),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: Eligible Songs Summary
      Material3SettingsGroup(
        scrollState = scrollState,
        title = stringResource(R.string.export_eligible_songs),
        items =
          listOf(
            Material3SettingsItem(
              icon = painterResource(R.drawable.storage),
              title = {
                Text("${eligibleSongs.size} ${stringResource(R.string.export_eligible_songs)}")
              },
              description = {
                Text(stringResource(R.string.export_total_size, formatFileSize(totalEligibleSize)))
              },
            )
          ),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Section 3: Preferences
      Material3SettingsGroup(
        scrollState = scrollState,
        title = stringResource(R.string.export_music),
        items =
          listOf(
            Material3SettingsItem(
              icon = painterResource(R.drawable.file_export),
              title = { Text(stringResource(R.string.export_convert_mp3)) },
              description = { Text(stringResource(R.string.export_convert_mp3_desc)) },
              trailingContent = {
                Switch(
                  checked = enableExportAsMp3,
                  onCheckedChange = onEnableExportAsMp3Change,
                )
              },
              onClick = { onEnableExportAsMp3Change(!enableExportAsMp3) },
            ),
            Material3SettingsItem(
              icon = painterResource(R.drawable.edit),
              title = { Text(stringResource(R.string.export_conflict_strategy)) },
              description = {
                val desc =
                  when (conflictStrategy) {
                    ExportConflictStrategy.SKIP -> stringResource(R.string.export_conflict_skip)
                    ExportConflictStrategy.REPLACE ->
                      stringResource(R.string.export_conflict_replace)
                    ExportConflictStrategy.RENAME -> stringResource(R.string.export_conflict_rename)
                  }
                Text(desc)
              },
              onClick = { showConflictDialog = true },
            ),
            Material3SettingsItem(
              icon = painterResource(R.drawable.album),
              title = { Text(stringResource(R.string.export_preserve_metadata)) },
              description = { Text(stringResource(R.string.export_preserve_metadata_desc)) },
              trailingContent = {
                Switch(
                  checked = exportMetadata,
                  onCheckedChange = onExportMetadataChange,
                )
              },
              onClick = { onExportMetadataChange(!exportMetadata) },
            ),
          ),
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Section 4: Actions (Export All & Select Songs)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = {
            if (exportDirectoryUri.isBlank()) {
              Toast.makeText(context, R.string.export_directory_not_set, Toast.LENGTH_SHORT).show()
              launchExportPicker()
            } else {
              viewModel.startExportAll(context)
            }
          },
          enabled = eligibleSongs.isNotEmpty() && !batchState.isExporting,
          modifier = Modifier.weight(1f).height(52.dp),
          shape = RoundedCornerShape(24.dp),
          elevation = ButtonDefaults.buttonElevation(0.dp),
        ) {
          Icon(
            painter = painterResource(R.drawable.download),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            stringResource(R.string.export_all_songs),
            fontWeight = FontWeight.SemiBold,
          )
        }

        OutlinedButton(
          onClick = { navController.navigate("settings/export_music/select") },
          enabled = eligibleSongs.isNotEmpty() && !batchState.isExporting,
          modifier = Modifier.weight(1f).height(52.dp),
          shape = RoundedCornerShape(24.dp),
        ) {
          Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            stringResource(R.string.select_songs),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      // Active Export / Progress View
      AnimatedVisibility(
        visible = batchState.isExporting || (batchState.totalCount > 0 && !batchState.isFinished)
      ) {
        Column(
          modifier =
            Modifier.padding(top = 24.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.exporting),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${batchState.progressPercent}%",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Spacer(Modifier.height(8.dp))

          val stageText =
            when (batchState.currentStage) {
              echo.music.iad1tya.export.ExportStage.QUEUED ->
                stringResource(R.string.export_preparing)
              echo.music.iad1tya.export.ExportStage.RESOLVING ->
                stringResource(R.string.export_preparing)
              echo.music.iad1tya.export.ExportStage.DOWNLOADING ->
                stringResource(R.string.export_processing_audio)
              echo.music.iad1tya.export.ExportStage.VERIFYING_SOURCE ->
                stringResource(R.string.export_processing_audio)
              echo.music.iad1tya.export.ExportStage.CONVERTING ->
                stringResource(R.string.export_processing_tags)
              echo.music.iad1tya.export.ExportStage.VERIFYING_OUTPUT ->
                stringResource(R.string.export_processing_tags)
              echo.music.iad1tya.export.ExportStage.SAVING ->
                stringResource(R.string.export_writing_file)
              echo.music.iad1tya.export.ExportStage.FINALIZING ->
                stringResource(R.string.export_writing_file)
              echo.music.iad1tya.export.ExportStage.COMPLETED ->
                stringResource(R.string.export_completed)
              else -> ""
            }

          Text(
            text =
              listOfNotNull(batchState.currentSongTitle, stageText.takeIf { it.isNotBlank() })
                .joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
          )
          Spacer(Modifier.height(12.dp))

          LinearProgressIndicator(
            progress = { (batchState.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
          )

          Spacer(Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                "${batchState.completedCount + batchState.failedCount + batchState.skippedCount} / ${batchState.totalCount}",
              style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
              onClick = { viewModel.cancelExport(context) },
              shape = RoundedCornerShape(16.dp),
            ) {
              Text(stringResource(R.string.cancel_exports), fontSize = 12.sp)
            }
          }
        }
      }

      // Summary Card View (when batch export finished)
      AnimatedVisibility(visible = batchState.isFinished && batchState.totalCount > 0) {
        Column(
          modifier =
            Modifier.padding(top = 24.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(20.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              painter = painterResource(R.drawable.done),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.export_completed),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          Spacer(Modifier.height(12.dp))
          Text(
            "Successfully exported: ${batchState.completedCount}",
            style = MaterialTheme.typography.bodyMedium,
          )
          Text("Skipped: ${batchState.skippedCount}", style = MaterialTheme.typography.bodyMedium)
          Text("Failed: ${batchState.failedCount}", style = MaterialTheme.typography.bodyMedium)
          Text(
            "Total size: ${formatFileSize(batchState.exportedSizeBytes)}",
            style = MaterialTheme.typography.bodyMedium,
          )

          Spacer(Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (batchState.failedCount > 0) {
              OutlinedButton(
                onClick = { viewModel.retryFailed(context) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
              ) {
                Text(stringResource(R.string.retry_failed))
              }
            }
            Button(
              onClick = { viewModel.loadEligibleSongs() },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(20.dp),
              elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
              Text(stringResource(android.R.string.ok))
            }
          }
        }
      }

      Spacer(Modifier.padding(bottom = 40.dp))
      Spacer(
        Modifier.windowInsetsPadding(
          LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
        )
      )
    }

    TopAppBar(
      title = { Text(stringResource(R.string.export_music)) },
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
      },
    )
  }
}
