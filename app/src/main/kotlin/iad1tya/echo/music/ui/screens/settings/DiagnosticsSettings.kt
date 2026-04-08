package iad1tya.echo.music.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.LocalDownloadUtil
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.PreferenceEntry
import iad1tya.echo.music.ui.component.PreferenceGroupTitle
import iad1tya.echo.music.ui.component.SwitchPreference
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.utils.DiagnosticsCenter
import iad1tya.echo.music.utils.rememberPreference
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.collectAsState
import iad1tya.echo.music.constants.UpdateNotificationsEnabledKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current

    val currentSong by playerConnection?.currentSong?.collectAsState(initial = null) ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val queueSize = playerConnection?.player?.mediaItemCount ?: 0
    val isPlaying = playerConnection?.player?.isPlaying ?: false
    val currentPosition = playerConnection?.player?.currentPosition ?: 0L
    val downloadCount by downloadUtil.downloads.map { it.size }.collectAsState(initial = 0)

    val (updateNotificationsEnabled, onUpdateNotificationsEnabled) = rememberPreference(UpdateNotificationsEnabledKey, true)

    val reportText = remember(currentSong, queueSize, isPlaying, currentPosition, downloadCount) {
        DiagnosticsCenter.buildReport(
            context = context,
            extraSections = listOf(
                "Playback" to "isPlaying=$isPlaying\npositionMs=$currentPosition\nqueueSize=$queueSize\ncurrentSong=${currentSong?.song?.id ?: "none"}",
                "Downloads" to "activeDownloads=$downloadCount",
                "Build" to "brand=${Build.BRAND}\nmodel=${Build.MODEL}\ndevice=${Build.DEVICE}",
            )
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        PreferenceGroupTitle(title = "Diagnostics")

        SwitchPreference(
            title = { Text("Update notifications") },
            description = "Keep update channel alerts enabled for troubleshooting update issues",
            icon = { Icon(painterResource(R.drawable.update), null) },
            checked = updateNotificationsEnabled,
            onCheckedChange = onUpdateNotificationsEnabled
        )

        PreferenceEntry(
            title = { Text("Copy diagnostics report") },
            description = "Copy full report with playback state and recent logs",
            icon = { Icon(painterResource(R.drawable.content_copy), null) },
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Echo Diagnostics", reportText))
            }
        )

        PreferenceEntry(
            title = { Text("Share diagnostics report") },
            description = "Share report text to Telegram, Discord, email, or notes",
            icon = { Icon(painterResource(R.drawable.share), null) },
            onClick = {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Echo Music Diagnostics")
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        },
                        "Share diagnostics"
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Recent logs are captured in-memory for this app session.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    Box {
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
                    } else Modifier
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                            Color.Transparent
                        )
                    )
                )
        )

        TopAppBar(
            title = { Text("Diagnostics & Bug Report") },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain
                ) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
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
