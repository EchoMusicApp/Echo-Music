


@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package iad1tya.echo.music.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.EnableUpdateNotificationKey
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.PreferenceGroupTitle
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.GitCommit
import iad1tya.echo.music.utils.UpdateNotificationManager
import iad1tya.echo.music.utils.Updater
import iad1tya.echo.music.utils.rememberPreference
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (enableUpdateNotification, onEnableUpdateNotificationChange) = rememberPreference(
        EnableUpdateNotificationKey,
        defaultValue = false
    )

    var latestVersion by remember { mutableStateOf<String?>(null) }
    var showEnableUpdateNotificationConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val isUpdateAvailable by remember(latestVersion) {
        derivedStateOf {
            latestVersion?.let { !Updater.isSameVersion(it, BuildConfig.VERSION_NAME) } ?: false
        }
    }

    var isDownloading by rememberSaveable { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var hasInstallPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        )
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
        hasInstallPermission = granted
        if (granted && downloadedApkFile != null) {
            Updater.installApk(context, downloadedApkFile!!)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            onEnableUpdateNotificationChange(true)
            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
        }
    }

    if (showEnableUpdateNotificationConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEnableUpdateNotificationConfirmDialog = false },
            title = { Text(stringResource(R.string.enable_update_notification)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Echo Music provides stable releases from official GitHub Releases.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "These versions are tested and recommended for all users.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableUpdateNotificationConfirmDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onEnableUpdateNotificationChange(true)
                            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableUpdateNotificationConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        Updater.getLatestVersionName().onSuccess {
            latestVersion = it
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.updates),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {},
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                UpdateSummaryCard(
                    currentVersion = BuildConfig.VERSION_NAME,
                    latestVersion = latestVersion,
                    isUpdateAvailable = isUpdateAvailable,
                    onOpenChangelog = { navController.navigate("settings/changelog") },
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    downloadedApkFile = downloadedApkFile,
                    onDownloadClick = {
                        isDownloading = true
                        downloadProgress = 0f
                        coroutineScope.launch {
                            Updater.downloadLatestApk { progress ->
                                downloadProgress = progress
                            }.onSuccess { apkFile ->
                                downloadedApkFile = apkFile
                                isDownloading = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasInstallPermission) {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    installPermissionLauncher.launch(intent)
                                } else {
                                    Updater.installApk(context, apkFile)
                                }
                            }.onFailure {
                                isDownloading = false
                            }
                        }
                    },
                    onInstallClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasInstallPermission) {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            installPermissionLauncher.launch(intent)
                        } else {
                            Updater.installApk(context, downloadedApkFile!!)
                        }
                    }
                )
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.notification_settings))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(R.string.enable_update_notification))
                        },
                        supportingContent = {
                            Text(text = stringResource(R.string.enable_update_notification_desc))
                        },
                        leadingContent = {
                            FeatureIcon(
                                iconRes = R.drawable.new_release,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enableUpdateNotification,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showEnableUpdateNotificationConfirmDialog = true
                                    } else {
                                        onEnableUpdateNotificationChange(false)
                                        UpdateNotificationManager.cancelPeriodicUpdateCheck(context)
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }



            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun UpdateSummaryCard(
    currentVersion: String,
    latestVersion: String?,
    isUpdateAvailable: Boolean,
    onOpenChangelog: () -> Unit,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadedApkFile: java.io.File?,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
) {
    val supportingText = when {
        latestVersion == null -> stringResource(R.string.updates_status_checking)
        isUpdateAvailable -> stringResource(R.string.latest_version_format, latestVersion)
        else -> stringResource(R.string.updates_status_current)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ListItem(
                overlineContent = {
                    Text(text = stringResource(R.string.current_version))
                },
                headlineContent = {
                    Text(
                        text = currentVersion,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                supportingContent = {
                    Text(text = supportingText)
                },
                leadingContent = {
                    FeatureIcon(
                        iconRes = R.drawable.update,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (isUpdateAvailable) {
                if (downloadedApkFile == null) {
                    androidx.compose.material3.Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            if (downloadProgress > 0f) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp), 
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val percent = (downloadProgress * 100).toInt()
                            Text(text = "Downloading $percent%")
                        } else {
                            Text(text = stringResource(R.string.download))
                        }
                    }
                } else {
                    androidx.compose.material3.Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.update_button))
                    }
                }
            }

            FilledTonalButton(
                onClick = onOpenChangelog,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.view_changelog))
            }
        }
    }
}

@Composable
private fun FeatureIcon(
    @DrawableRes iconRes: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .padding(12.dp)
                .size(22.dp),
        )
    }
}

