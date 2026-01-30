package iad1tya.echo.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.MaterialYouKey
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.component.fetchReleaseNotesText
import iad1tya.echo.music.ui.utils.backToMain
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val (enableMaterialYou) = iad1tya.echo.music.utils.rememberPreference(MaterialYouKey, defaultValue = false)

    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        // New Version Available - Show at top with release notes
        if (latestVersionName != BuildConfig.VERSION_NAME) {
            val coroutineScope = rememberCoroutineScope()
            var releaseNotes by remember { mutableStateOf<List<String>>(emptyList()) }
            var downloadProgress by remember { mutableStateOf<Float?>(null) }
            var downloadedApkUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var isDownloading by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                releaseNotes = fetchReleaseNotesText()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    BadgedBox(
                        badge = { Badge() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.new_version_available),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version $latestVersionName",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Show progress bar if downloading
                    if (isDownloading && downloadProgress != null) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { downloadProgress!! },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(downloadProgress!! * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Button(
                        onClick = {
                            if (downloadedApkUri != null) {
                                // Install APK
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(downloadedApkUri, "application/vnd.android.package-archive")
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } else if (!isDownloading) {
                                // Start download
                                isDownloading = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        // Fetch latest release info
                                        val client = okhttp3.OkHttpClient()
                                        val request = okhttp3.Request.Builder()
                                            .url("https://api.github.com/repos/iad1tya/Echo-Music/releases/latest")
                                            .build()
                                        
                                        val response = client.newCall(request).execute()
                                        val json = org.json.JSONObject(response.body?.string() ?: "{}")
                                        val assets = json.optJSONArray("assets")
                                        
                                        var downloadUrl: String? = null
                                        if (assets != null) {
                                            for (i in 0 until assets.length()) {
                                                val asset = assets.getJSONObject(i)
                                                val name = asset.optString("name", "")
                                                if (name.endsWith(".apk", ignoreCase = true)) {
                                                    downloadUrl = asset.optString("browser_download_url")
                                                    break
                                                }
                                            }
                                        }
                                        
                                        if (downloadUrl != null) {
                                            // Download APK
                                            val apkRequest = okhttp3.Request.Builder().url(downloadUrl).build()
                                            val apkResponse = client.newCall(apkRequest).execute()
                                            val body = apkResponse.body
                                            
                                            if (body != null) {
                                                val contentLength = body.contentLength()
                                                val inputStream = body.byteStream()
                                                
                                                // Save to cache directory
                                                val apkFile = java.io.File(context.cacheDir, "echo_update.apk")
                                                val outputStream = java.io.FileOutputStream(apkFile)
                                                
                                                val buffer = ByteArray(8192)
                                                var bytesRead: Int
                                                var totalBytesRead = 0L
                                                
                                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                                    outputStream.write(buffer, 0, bytesRead)
                                                    totalBytesRead += bytesRead
                                                    
                                                    if (contentLength > 0) {
                                                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                                        (context as? android.app.Activity)?.runOnUiThread {
                                                            downloadProgress = progress
                                                        }
                                                    }
                                                }
                                                
                                                outputStream.close()
                                                inputStream.close()
                                                
                                                // Get URI using FileProvider
                                                val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.FileProvider",
                                                    apkFile
                                                )
                                                
                                                (context as? android.app.Activity)?.runOnUiThread {
                                                    downloadedApkUri = apkUri
                                                    isDownloading = false
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            isDownloading = false
                                            downloadProgress = null
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading
                    ) {
                        Text(
                            when {
                                downloadedApkUri != null -> "Install Update"
                                isDownloading -> "Downloading..."
                                else -> stringResource(R.string.download_update)
                            }
                        )
                    }

                    // Release Notes Section
                    if (releaseNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.release_notes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        releaseNotes.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // User Interface Section
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_ui),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.appearance)) },
                    onClick = { navController.navigate("settings/appearance") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Player & Audio - Separate
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_player_content),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    onClick = { navController.navigate("settings/player") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Content - Separate
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.content)) },
                    onClick = { navController.navigate("settings/content") }
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // AI - Separate
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.ai_icon),
                    title = { Text("AI for Lyrics Translation") },
                    onClick = { navController.navigate("settings/ai") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Privacy & Security Section
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_privacy),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.security),
                    title = { Text(stringResource(R.string.privacy)) },
                    onClick = { navController.navigate("settings/privacy") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Storage - Separate
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_storage),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.storage)) },
                    onClick = { navController.navigate("settings/storage") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Backup & Restore - Separate
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text(stringResource(R.string.backup_restore)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Default Links - Separate (if Android 12+)
        if (isAndroid12OrLater) {
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_system),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.link),
                        title = { Text(stringResource(R.string.default_links)) },
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                    "package:${context.packageName}".toUri()
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                when (e) {
                                    is ActivityNotFoundException -> {
                                        Toast.makeText(
                                            context,
                                            R.string.open_app_settings_error,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    is SecurityException -> {
                                        Toast.makeText(
                                            context,
                                            R.string.open_app_settings_error,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    else -> {
                                        Toast.makeText(
                                            context,
                                            R.string.open_app_settings_error,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Updater - Separate
        Material3SettingsGroup(
            title = if (!isAndroid12OrLater) stringResource(R.string.settings_section_system) else null,
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.update),
                    title = { Text(stringResource(R.string.updater)) },
                    onClick = { navController.navigate("settings/updater") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // About - Separate
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.about)) },
                    onClick = { navController.navigate("settings/about") }
                )
            )
        )
        
        
        Spacer(modifier = Modifier.height(16.dp))
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
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            modifier = Modifier.zIndex(11f)
        )
    }
}
