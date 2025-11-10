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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.component.fetchReleaseNotesText
import iad1tya.echo.music.ui.utils.backToMain

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
            var releaseNotes by remember { mutableStateOf<List<String>>(emptyList()) }

            LaunchedEffect(Unit) {
                releaseNotes = fetchReleaseNotesText()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1C1C1E)
                )
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
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.new_version_available),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version $latestVersionName",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { uriHandler.openUri("https://echomusic.fun") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.download_update))
                    }

                    // Release Notes Section
                    if (releaseNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.release_notes),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        releaseNotes.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Player & Content Section (moved up and combined with content)
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_player_content),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.play),
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    onClick = { navController.navigate("settings/player") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.content)) },
                    onClick = { navController.navigate("settings/content") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Storage & Data Section
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_storage),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.storage)) },
                    onClick = { navController.navigate("settings/storage") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text(stringResource(R.string.backup_restore)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // System & About Section
        Material3SettingsGroup(
            title = stringResource(R.string.settings_section_system),
            items = buildList {
                if (isAndroid12OrLater) {
                    add(
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
                }
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.update),
                        title = { Text(stringResource(R.string.updater)) },
                        onClick = { navController.navigate("settings/updater") }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text(stringResource(R.string.about)) },
                        onClick = { navController.navigate("settings/about") }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.favorite),
                        title = { Text("Supporter") },
                        onClick = { navController.navigate("settings/supporter") }
                    )
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }

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
            containerColor = MaterialTheme.colorScheme.background
        ),
        scrollBehavior = scrollBehavior
    )
}
