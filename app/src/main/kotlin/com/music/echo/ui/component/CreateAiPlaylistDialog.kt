package echo.music.iad1tya.ui.component

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import echo.music.iad1tya.R
import echo.music.iad1tya.ai.weather.WeatherUiState
import echo.music.iad1tya.viewmodels.CreateAiPlaylistViewModel

@Composable
fun CreateAiPlaylistDialog(
    onDismiss: () -> Unit,
    onPlaylistCreated: (String) -> Unit,
    viewModel: CreateAiPlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val prompt by viewModel.prompt.collectAsState()
    val numSongs by viewModel.numSongs.collectAsState()
    val weatherEnabled by viewModel.weatherEnabled.collectAsState()
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationLog by viewModel.generationLog.collectAsState()
    val errorLog by viewModel.errorLog.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val isGranted = fineGranted || coarseGranted

        val activity = context as? Activity
        val isPermanentlyDenied = !isGranted && activity?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } ?: false

        viewModel.onWeatherToggled(
            enabled = true,
            context = context,
            hasPermission = isGranted,
            isPermanentlyDenied = isPermanentlyDenied
        )
    }

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    val handleDismiss = {
        viewModel.resetState()
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (!isGenerating) {
                handleDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create with AI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                if (!isGenerating && errorLog == null) {
                    // "Tune into Weather" Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                val target = !weatherEnabled
                                if (target) {
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasFine || hasCoarse) {
                                        viewModel.onWeatherToggled(
                                            enabled = true,
                                            context = context,
                                            hasPermission = true,
                                            isPermanentlyDenied = false
                                        )
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                } else {
                                    viewModel.onWeatherToggled(
                                        enabled = false,
                                        context = context,
                                        hasPermission = false,
                                        isPermanentlyDenied = false
                                    )
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tune into Weather",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Match playlist vibes to local weather",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = weatherEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasFine || hasCoarse) {
                                        viewModel.onWeatherToggled(
                                            enabled = true,
                                            context = context,
                                            hasPermission = true,
                                            isPermanentlyDenied = false
                                        )
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                } else {
                                    viewModel.onWeatherToggled(
                                        enabled = false,
                                        context = context,
                                        hasPermission = false,
                                        isPermanentlyDenied = false
                                    )
                                }
                            }
                        )
                    }

                    // Weather Preview / Rationale Card
                    if (weatherEnabled) {
                        when (val state = weatherUiState) {
                            is WeatherUiState.Loading -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Fetching local weather atmosphere...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is WeatherUiState.Success -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = state.data.summaryBadgeText,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            is WeatherUiState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        if (state.isPermissionDenied) {
                                            Button(
                                                onClick = {
                                                    if (state.isPermanentlyDenied) {
                                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                            data = Uri.fromParts("package", context.packageName, null)
                                                        }
                                                        context.startActivity(intent)
                                                    } else {
                                                        locationPermissionLauncher.launch(
                                                            arrayOf(
                                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                                            )
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text(if (state.isPermanentlyDenied) "Open Settings" else "Enable Location")
                                            }
                                        }
                                    }
                                }
                            }

                            WeatherUiState.Idle -> {}
                        }
                    }

                    // Prompt Input Field
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = viewModel::onPromptChanged,
                        label = {
                            Text(
                                if (weatherEnabled) "Extra vibes or specific artists (Optional)"
                                else "What kind of playlist do you want? (Required)"
                            )
                        },
                        placeholder = {
                            Text(
                                if (weatherEnabled) "Optional: Add extra vibes or leave blank to let the weather decide"
                                else "e.g. upbeat workout pop songs"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Song Count Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Number of songs: ${numSongs.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = numSongs,
                            onValueChange = viewModel::onNumSongsChanged,
                            valueRange = 5f..50f,
                            steps = 44
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = handleDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        val canGenerate = if (weatherEnabled) {
                            weatherUiState is WeatherUiState.Success || prompt.isNotBlank()
                        } else {
                            prompt.isNotBlank()
                        }

                        Button(
                            onClick = {
                                viewModel.generatePlaylist(context, onPlaylistCreated)
                            },
                            enabled = canGenerate
                        ) {
                            Text("Generate")
                        }
                    }
                } else if (isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            text = generationLog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (errorLog != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorLog!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Last Log: $generationLog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = viewModel::clearError) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                }
            }
        }
    }
}
