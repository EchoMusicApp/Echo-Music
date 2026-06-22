

package iad1tya.echo.music.ui.screens.settings

import iad1tya.echo.music.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.screens.Screens
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.echomusic.updater.getUpdateAvailableState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
highlightKey: String? = null) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isUpdateAvailable = getUpdateAvailableState(context) && iad1tya.echo.music.echomusic.updater.getAutoUpdateCheckSetting(context)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchLower = searchQuery.lowercase()

    val accountText = stringResource(R.string.account)
    val appearanceText = stringResource(R.string.appearance)
    val playerText = stringResource(R.string.player_and_audio)
    val listenTogetherText = stringResource(R.string.listen_together)
    val contentText = stringResource(R.string.content)
    val aiLyricsText = stringResource(R.string.ai_lyrics_translation)
    val privacyText = stringResource(R.string.privacy)
    val storageText = stringResource(R.string.storage)
    val backupText = stringResource(R.string.backup_restore)
    val systemUpdateText = stringResource(R.string.system_update)
    val aboutText = stringResource(R.string.about)

    val scrollState = rememberScrollState()
    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
        )

        val itemsList = buildList {
            if (accountText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == accountText),
                        icon = painterResource(R.drawable.account),
                        title = { Text(accountText) },
                        onClick = { navController.navigate("settings/account") }
                    )
                )
            }
            if ("echo brain".contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == "Echo Brain (Beta)"),
                        icon = rememberVectorPainter(Icons.Outlined.AutoAwesome),
                        title = { Text("Echo Brain (Beta)") },
                        onClick = { navController.navigate("settings/echo_brain") }
                    )
                )
            }
            if (appearanceText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == appearanceText),
                        icon = painterResource(R.drawable.palette),
                        title = { Text(appearanceText) },
                        onClick = { navController.navigate("settings/appearance") }
                    )
                )
            }
            if (playerText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == playerText),
                        icon = painterResource(R.drawable.play),
                        title = { Text(playerText) },
                        onClick = { navController.navigate("settings/player") }
                    )
                )
            }
            if (listenTogetherText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == listenTogetherText),
                        icon = painterResource(R.drawable.group),
                        title = { Text(listenTogetherText) },
                        onClick = { navController.navigate(Screens.ListenTogether.route) }
                    )
                )
            }
            if (contentText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == contentText),
                        icon = painterResource(R.drawable.language),
                        title = { Text(contentText) },
                        onClick = { navController.navigate("settings/content") }
                    )
                )
            }
            if (aiLyricsText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == aiLyricsText),
                        icon = painterResource(R.drawable.translate),
                        title = { Text(aiLyricsText) },
                        onClick = { navController.navigate("settings/ai") }
                    )
                )
            }
            if (privacyText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == privacyText),
                        icon = painterResource(R.drawable.security),
                        title = { Text(privacyText) },
                        onClick = { navController.navigate("settings/privacy") }
                    )
                )
            }
            if (storageText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == storageText),
                        icon = painterResource(R.drawable.storage),
                        title = { Text(storageText) },
                        onClick = { navController.navigate("settings/storage") }
                    )
                )
            }
            if (backupText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == backupText),
                        icon = painterResource(R.drawable.restore),
                        title = { Text(backupText) },
                        onClick = { navController.navigate("settings/backup_restore") }
                    )
                )
            }
            if (systemUpdateText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == systemUpdateText),
                        icon = painterResource(if (isUpdateAvailable) R.drawable.ic_launcher_nobg else R.drawable.update),
                        title = { Text(systemUpdateText) },
                        description = if (isUpdateAvailable) {
                            {
                                Text(
                                    text = stringResource(R.string.update_available),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null,
                        onClick = { navController.navigate("settings/update") }
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if ("supported links".contains(searchLower)) {
                    add(
                        Material3SettingsItem(
                            isHighlighted = (highlightKey == "supported links"),
                            icon = painterResource(R.drawable.link),
                            title = { Text("Supported Links") },
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    when (e) {
                                        is ActivityNotFoundException, is SecurityException -> {
                                            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                                        }
                                        else -> {
                                            Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
            }
            if (aboutText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == aboutText),
                        icon = painterResource(R.drawable.info),
                        title = { Text(aboutText) },
                        onClick = { navController.navigate("settings/about") }
                    )
                )
            }
        }

        val finalItemsList = if (searchQuery.isNotEmpty()) {
            val subSettings = getAllSearchableSettings()

            val matchedSubSettings = subSettings
                .filter { it.first.lowercase().contains(searchLower) }
                .groupBy { it.second }
                .map { (parentTitle, settingsInPage) ->
                    val route = settingsInPage.first().third
                    val title = settingsInPage.first().first
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.search),
                        title = { Text(parentTitle) },
                        description = { Text("Contains ${settingsInPage.size} matching setting(s)") },
                        onClick = { 
                            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                            val finalRoute = if (route.contains("?")) "$route&highlightKey=$encodedTitle" else "$route?highlightKey=$encodedTitle"
                            navController.navigate(finalRoute)
                        }
                    )
                }
            
            itemsList + matchedSubSettings
        } else {
            itemsList
        }

        if (finalItemsList.isEmpty() && searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "No settings found for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        } else {
            Material3SettingsGroup(scrollState = scrollState, items = finalItemsList)
        }
        
        Spacer(modifier = Modifier.height(50.dp))
    }

    TopAppBar(
        title = {
            androidx.compose.animation.AnimatedVisibility(
                visible = scrollState.value > 100,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge
                )
            }
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
        }
    )
}
