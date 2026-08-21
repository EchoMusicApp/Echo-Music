package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.utils.parseCookieString
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AccountEmailKey
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.UseLoginForBrowse
import echo.music.iad1tya.constants.YtmSyncKey
import echo.music.iad1tya.constants.AudioQualityKey
import echo.music.iad1tya.constants.AudioQuality
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.utils.rememberEnumPreference
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDialoge(
    onDismissRequest: () -> Unit,
    onNavigate: (String) -> Unit,
    homeViewModel: HomeViewModel
) {
    val uriHandler = LocalUriHandler.current
    val (audioQuality) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.OPUS
    )
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val accountName by homeViewModel.accountName.collectAsState()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()

    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val primaryColor = MaterialTheme.colorScheme.onSurface
        val onSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                text = "Echo Music",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = primaryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // Account Group
            Material3SettingsGroup(
                title = "Account",
                compact = true,
                items = buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(if (isLoggedIn) accountName else "Anonymous") },
                            description = { Text(if (isLoggedIn) accountEmail.ifEmpty { "Logged In" } else "Not Logged In") },
                            icon = painterResource(R.drawable.account),
                            trailingContent = if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                                {
                                    AsyncImage(
                                        model = accountImageUrl,
                                        contentDescription = "Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            } else null,
                            onClick = {
                                onDismissRequest()
                                if (isLoggedIn) onNavigate("settings/account") else onNavigate("login") 
                            }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.ai_lyrics_translation)) },
                            description = { Text(androidx.compose.ui.res.stringResource(R.string.setting_desc_ai)) },
                            customIcon = {
                                Text(
                                    text = "Ai",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )
                            },
                            onClick = {
                                onDismissRequest()
                                onNavigate("settings/ai")
                            }
                        )
                    )
                }
            )

            if (isLoggedIn) {
                Material3SettingsGroup(
                    title = "Preferences",
                    compact = true,
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text("Use Account for Browsing") },
                            icon = painterResource(R.drawable.add_circle),
                            trailingContent = {
                                Switch(
                                    checked = useLoginForBrowse,
                                    onCheckedChange = {
                                        com.music.innertube.YouTube.useLoginForBrowse = it
                                        onUseLoginForBrowseChange(it)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            },
                            onClick = {
                                val newVal = !useLoginForBrowse
                                com.music.innertube.YouTube.useLoginForBrowse = newVal
                                onUseLoginForBrowseChange(newVal)
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text("YouTube Music Sync") },
                            icon = painterResource(R.drawable.cached),
                            trailingContent = {
                                Switch(
                                    checked = ytmSync,
                                    onCheckedChange = onYtmSyncChange,
                                    modifier = Modifier.scale(0.8f)
                                )
                            },
                            onClick = { onYtmSyncChange(!ytmSync) }
                        )
                    )
                )
            }

            Material3SettingsGroup(
                title = "App",
                compact = true,
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(androidx.compose.ui.res.stringResource(R.string.settings)) },
                        description = { Text(androidx.compose.ui.res.stringResource(R.string.setting_desc_settings_main)) },
                        icon = painterResource(R.drawable.settings),
                        onClick = { 
                            onDismissRequest()
                            onNavigate("settings") 
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text("About") },
                        icon = painterResource(R.drawable.info),
                        trailingContent = { Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { 
                            onDismissRequest()
                            onNavigate("settings/about") 
                        }
                    )
                )
            )

            // Footer Links
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSecondaryColor,
                    modifier = Modifier.clickable { uriHandler.openUri("https://echomusic.fun/p/privacy-policy") }.padding(4.dp)
                )
                Text(text = " • ", color = onSecondaryColor, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSecondaryColor,
                    modifier = Modifier.clickable { uriHandler.openUri("https://echomusic.fun/p/toc") }.padding(4.dp)
                )
            }
        }
    }
}
