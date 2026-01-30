package iad1tya.echo.music.ui.screens.settings

import iad1tya.echo.music.LocalPlayerAwareWindowInsets

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.ExcludedScanPathsKey
import iad1tya.echo.music.constants.ScanPathsKey
import iad1tya.echo.music.constants.ScannerSensitivityKey
import iad1tya.echo.music.constants.ScannerStrictExtKey
import iad1tya.echo.music.constants.ScannerStrictFilePathsKey
import iad1tya.echo.music.ui.component.*
import iad1tya.echo.music.utils.rememberPreference
import iad1tya.echo.music.viewmodels.SettingsViewModel
import java.net.URLDecoder
import androidx.navigation.NavController
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

// Simple helpers
fun uriListFromString(str: String): List<Uri> {
    if (str.isBlank()) return emptyList()
    return str.split("\n").mapNotNull {
        try { Uri.parse(it) } catch (e: Exception) { null }
    }
}

fun stringFromUriList(list: List<Uri>): String {
    return list.joinToString("\n") { it.toString() }
}

fun absoluteFilePathFromUri(context: android.content.Context, uri: Uri): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!DocumentsContract.isTreeUri(uri)) return null
        if (uri.authority != "com.android.externalstorage.documents") return null

        val treeDocId = DocumentsContract.getTreeDocumentId(uri)
        val rootId: String
        val relativePath: String

        if (treeDocId.contains(":")) {
            val parts = treeDocId.split(":", limit = 2)
            rootId = parts[0]
            relativePath = parts[1]
        } else {
            rootId = treeDocId
            relativePath = ""
        }

        val storageManager = context.getSystemService(android.content.Context.STORAGE_SERVICE) as StorageManager

        val rootDir = if (rootId.equals("primary", ignoreCase = true)) {
            storageManager.primaryStorageVolume.directory
        } else {
            storageManager.storageVolumes.firstOrNull {
                it.uuid != null && it.uuid.equals(rootId, ignoreCase = true)
            }?.directory
        }

        return rootDir?.let { if (relativePath.isEmpty()) it.absolutePath else java.io.File(it, relativePath).absolutePath }
    } else {
        if (uri.authority != "com.android.externalstorage.documents") return null

        val docId = DocumentsContract.getTreeDocumentId(uri)
        val parts = docId.split(":")

        if (parts.size < 2) return null

        val type = parts[0]
        val relativePath = parts[1]

        val rootDir = when (type.lowercase()) {
            "primary" -> Environment.getExternalStorageDirectory()
            else -> {
                val secondaryStorage = "/storage/$type"
                if (java.io.File(secondaryStorage).exists()) {
                    java.io.File(secondaryStorage)
                } else {
                    null
                }
            }
        }

        return rootDir?.let { java.io.File(it, relativePath).absolutePath }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMediaSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isScanning by viewModel.isScanning.collectAsState()
    val scannerProgress by viewModel.scannerProgress.collectAsState()

    val (scanPaths, onScanPathsChange) = rememberPreference(ScanPathsKey, defaultValue = "")
    val (excludedScanPaths, onExcludedScanPathsChange) = rememberPreference(ExcludedScanPathsKey, defaultValue = "")
    
    // Advanced Settings
    val (scannerSensitivity, onScannerSensitivityChange) = rememberPreference(ScannerSensitivityKey, defaultValue = 1)
    val (strictExt, onStrictExtChange) = rememberPreference(ScannerStrictExtKey, defaultValue = false)
    val (strictFilePaths, onStrictFilePathsChange) = rememberPreference(ScannerStrictFilePathsKey, defaultValue = false)

    var showAddFolderDialog: Boolean? by remember { mutableStateOf(null) } // true=include, false=exclude

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startScan(
                     uriListFromString(scanPaths).mapNotNull { absoluteFilePathFromUri(context, it) },
                     uriListFromString(excludedScanPaths).mapNotNull { absoluteFilePathFromUri(context, it) },
                     scannerSensitivity,
                     strictExt,
                     strictFilePaths
                )
            } else {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )



        // Progress / Scan Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isScanning) return@Button

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                             permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                             return@Button
                        }
                    } else {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                             permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                             return@Button
                        }
                    }

                    viewModel.startScan(
                         uriListFromString(scanPaths).mapNotNull { absoluteFilePathFromUri(context, it) },
                         uriListFromString(excludedScanPaths).mapNotNull { absoluteFilePathFromUri(context, it) },
                         scannerSensitivity,
                         strictExt,
                         strictFilePaths
                    )
                }
            ) {
                Text(if (isScanning) "Scanning..." else "Scan Now")
            }

            if (isScanning) {
                Spacer(Modifier.width(16.dp))
                CircularProgressIndicator(
                    progress = { scannerProgress },
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                     text = "${(scannerProgress * 100).toInt()}%",
                     style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Manage Paths
        PreferenceEntry(
            title = { Text("Manage scan paths") },
            icon = { Icon(Icons.Rounded.Folder, null) },
            onClick = {
                showAddFolderDialog = true
            },
            description = "Select folders to include or exclude"
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Advanced Settings Controls

        ListPreference(
             title = { Text("Scanner Sensitivity") },
             selectedValue = scannerSensitivity,
             values = listOf(1, 2, 3),
             valueText = { when(it) {
                 1 -> "Standard (Artist, Album, Title)"
                 2 -> "Strict (Exact Match)"
                 3 -> "Loose (Fuzzy Match)"
                 else -> "Standard"
             }},
             onValueSelected = onScannerSensitivityChange,
             icon = { Icon(Icons.Rounded.WarningAmber, null) }
        )

        SwitchPreference(
            title = { Text("Strict Extensions") },
            description = "Only scan .mp3, .flac, .m4a, .wav, .ogg",
            checked = strictExt,
            onCheckedChange = onStrictExtChange
        )
        
        SwitchPreference(
            title = { Text("Use Filenames as Title") },
            description = "If metadata is missing, use filename",
            checked = strictFilePaths,
            onCheckedChange = onStrictFilePathsChange
        )
    }

    Box {
        // Blurred gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .zIndex(10f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                25f,
                                25f,
                                android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.local_media),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.zalando_sans_expanded)),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { navController.navigateUp() },
                ) {
                    Icon(
                        androidx.compose.ui.res.painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            modifier = Modifier.zIndex(11f)
        )
    }

    // Dialog logic
    if (showAddFolderDialog != null) {
        var tempScanPaths = remember { mutableStateListOf<Uri>() }
        
        // Initialize with current paths based on mode
        LaunchedEffect(showAddFolderDialog) {
            tempScanPaths.clear()
            val currentStr = if (showAddFolderDialog == true) scanPaths else excludedScanPaths
            tempScanPaths.addAll(uriListFromString(currentStr))
        }

        ActionPromptDialog(
            titleBar = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (showAddFolderDialog == true) "Include Folders" else "Exclude Folders",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = showAddFolderDialog!!,
                        onCheckedChange = { showAddFolderDialog = it }
                    )
                }
            },
            onDismiss = { showAddFolderDialog = null },
            onConfirm = {
                val newStr = stringFromUriList(tempScanPaths.toList())
                if (showAddFolderDialog == true) {
                    onScanPathsChange(newStr)
                } else {
                    onExcludedScanPathsChange(newStr)
                }
                showAddFolderDialog = null
            },
            onReset = { tempScanPaths.clear() },
            onCancel = { showAddFolderDialog = null }
        ) {
             val dirPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                 val contentResolver = context.contentResolver
                 val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                 try {
                     contentResolver.takePersistableUriPermission(uri, takeFlags)
                 } catch (e: Exception) {
                     Log.e("LocalMediaSettings", "Failed to take permission", e)
                 }
                 if (!tempScanPaths.contains(uri)) {
                     tempScanPaths.add(uri)
                 }
            }

            Text(
                text = "Select folders to ${if (showAddFolderDialog == true) "scan" else "ignore"}.",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(Modifier.height(8.dp))

            // Checkbox for include/exclude explanation or switch label could be better

            // Folders List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (tempScanPaths.isEmpty()) {
                    Text("No specific folders selected.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
                tempScanPaths.forEach { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = absoluteFilePathFromUri(context, uri) ?: uri.path ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { tempScanPaths.remove(uri) }) {
                             Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Button(onClick = { dirPickerLauncher.launch(null) }) {
                Text("Add Folder")
            }
        }
    }
}
