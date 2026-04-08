package iad1tya.echo.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.MaxImageCacheSizeKey
import iad1tya.echo.music.constants.MaxSongCacheSizeKey
import iad1tya.echo.music.extensions.tryOrNull
import iad1tya.echo.music.ui.component.ActionPromptDialog
import iad1tya.echo.music.ui.component.DefaultDialog
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.ui.utils.formatFileSize
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(MaxImageCacheSizeKey, 512)
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(MaxSongCacheSizeKey, 1024)

    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var songCacheSizeDialog by remember { mutableStateOf(false) }
    var imageCacheSizeDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(tryOrNull { playerCache.cacheSpace } ?: 0L) }
    var downloadCacheSize by remember { mutableStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0L) }

    val imageCacheProgress by animateFloatAsState(
        targetValue = (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f),
        label = "imageCacheProgress"
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(0f, 1f),
        label = "playerCacheProgress"
    )

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) coroutineScope.launch(Dispatchers.IO) { imageDiskCache.clear() }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) coroutineScope.launch(Dispatchers.IO) {
            playerCache.keys.forEach { playerCache.removeResource(it) }
        }
    }
    LaunchedEffect(imageDiskCache) { while (isActive) { delay(500); imageCacheSize = imageDiskCache.size } }
    LaunchedEffect(playerCache)   { while (isActive) { delay(500); playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0L } }
    LaunchedEffect(downloadCache) { while (isActive) { delay(500); downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0L } }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (clearDownloads) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_all_downloads),
            onDismiss = { clearDownloads = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) { downloadCache.keys.forEach { downloadCache.removeResource(it) } }
                clearDownloads = false
            },
            onCancel = { clearDownloads = false },
            content = { Text(stringResource(R.string.clear_downloads_dialog)) }
        )
    }
    if (clearCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_song_cache),
            onDismiss = { clearCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) { playerCache.keys.forEach { playerCache.removeResource(it) } }
                clearCacheDialog = false
            },
            onCancel = { clearCacheDialog = false },
            content = { Text(stringResource(R.string.clear_song_cache_dialog)) }
        )
    }
    if (clearImageCacheDialog) {
        ActionPromptDialog(
            title = stringResource(R.string.clear_image_cache),
            onDismiss = { clearImageCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) { imageDiskCache.clear() }
                clearImageCacheDialog = false
            },
            onCancel = { clearImageCacheDialog = false },
            content = { Text(stringResource(R.string.clear_image_cache_dialog)) }
        )
    }

    val songCacheSizes = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1)
    val imageCacheSizes = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192)

    if (songCacheSizeDialog) {
        DefaultDialog(
            onDismiss = { songCacheSizeDialog = false },
            content = {
                Column {
                    Text(
                        text = stringResource(R.string.max_cache_size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                    songCacheSizes.forEach { size ->
                        val selected = size == maxSongCacheSize
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onMaxSongCacheSizeChange(size); songCacheSizeDialog = false }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                                    else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp).background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = when (size) {
                                    0  -> stringResource(R.string.disable)
                                    -1 -> stringResource(R.string.unlimited)
                                    else -> formatFileSize(size * 1024 * 1024L)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            },
            buttons = { TextButton(onClick = { songCacheSizeDialog = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    if (imageCacheSizeDialog) {
        DefaultDialog(
            onDismiss = { imageCacheSizeDialog = false },
            content = {
                Column {
                    Text(
                        text = stringResource(R.string.max_cache_size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                    imageCacheSizes.forEach { size ->
                        val selected = size == maxImageCacheSize
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onMaxImageCacheSizeChange(size); imageCacheSizeDialog = false }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                                    else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp).background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = if (size == 0) stringResource(R.string.disable) else formatFileSize(size * 1024 * 1024L),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            },
            buttons = { TextButton(onClick = { imageCacheSizeDialog = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    // ── Screen content ────────────────────────────────────────────────────────

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(8.dp))

        // Overview stats card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageStatCell(R.drawable.download, stringResource(R.string.downloaded_songs), formatFileSize(downloadCacheSize), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(52.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)))
                StorageStatCell(R.drawable.library_music, stringResource(R.string.song_cache), formatFileSize(playerCacheSize), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(52.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)))
                StorageStatCell(R.drawable.insert_photo, stringResource(R.string.image_cache), formatFileSize(imageCacheSize), Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Downloads ─────────────────────────────────────────────────────────
        StorageSectionLabel(stringResource(R.string.downloaded_songs))
        StorageCard {
            StorageInfoRow(R.drawable.download, stringResource(R.string.downloaded_songs), stringResource(R.string.size_used, formatFileSize(downloadCacheSize)))
            StorageDivider()
            StorageActionRow(R.drawable.delete, stringResource(R.string.clear_all_downloads)) { clearDownloads = true }
        }

        Spacer(Modifier.height(12.dp))

        // ── Song Cache ────────────────────────────────────────────────────────
        StorageSectionLabel(stringResource(R.string.song_cache))
        StorageCard {
            StorageInfoRow(
                iconRes = R.drawable.library_music,
                title = stringResource(R.string.song_cache),
                subtitle = when {
                    maxSongCacheSize == 0  -> stringResource(R.string.disable)
                    maxSongCacheSize == -1 -> stringResource(R.string.size_used, formatFileSize(playerCacheSize))
                    else -> "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"
                }
            )
            if (maxSongCacheSize != 0 && maxSongCacheSize != -1) {
                LinearProgressIndicator(
                    progress = { playerCacheProgress },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            StorageDivider()
            StorageSettingRow(
                iconRes = R.drawable.storage,
                title = stringResource(R.string.max_cache_size),
                value = when (maxSongCacheSize) {
                    0  -> stringResource(R.string.disable)
                    -1 -> stringResource(R.string.unlimited)
                    else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                },
                onClick = { songCacheSizeDialog = true }
            )
            StorageDivider()
            StorageActionRow(R.drawable.delete, stringResource(R.string.clear_song_cache)) { clearCacheDialog = true }
        }

        Spacer(Modifier.height(12.dp))

        // ── Image Cache ───────────────────────────────────────────────────────
        StorageSectionLabel(stringResource(R.string.image_cache))
        StorageCard {
            StorageInfoRow(
                iconRes = R.drawable.insert_photo,
                title = stringResource(R.string.image_cache),
                subtitle = if (maxImageCacheSize == 0) stringResource(R.string.disable)
                           else "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
            )
            if (maxImageCacheSize > 0) {
                LinearProgressIndicator(
                    progress = { imageCacheProgress },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            StorageDivider()
            StorageSettingRow(
                iconRes = R.drawable.storage,
                title = stringResource(R.string.max_cache_size),
                value = if (maxImageCacheSize == 0) stringResource(R.string.disable) else formatFileSize(maxImageCacheSize * 1024 * 1024L),
                onClick = { imageCacheSizeDialog = true }
            )
            StorageDivider()
            StorageActionRow(R.drawable.delete, stringResource(R.string.clear_image_cache)) { clearImageCacheDialog = true }
        }

        Spacer(Modifier.height(20.dp))
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    Box {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .zIndex(10f)
                    .graphicsLayer {
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(25f, 25f, android.graphics.Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 1.0f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.storage),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily(Font(R.font.zalando_sans_expanded)),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            modifier = Modifier.zIndex(11f)
        )
    }
}

// ── Private composable helpers ────────────────────────────────────────────────

@Composable
private fun StorageSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 28.dp, bottom = 6.dp)
    )
}

@Composable
private fun StorageCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun StorageDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun StorageStatCell(iconRes: Int, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun StorageInfoRow(iconRes: Int, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StorageSettingRow(iconRes: Int, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        Icon(painterResource(R.drawable.navigate_next), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StorageActionRow(iconRes: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
    }
}
