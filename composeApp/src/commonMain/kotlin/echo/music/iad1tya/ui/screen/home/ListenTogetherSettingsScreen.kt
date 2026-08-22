package echo.music.iad1tya.ui.screen.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.component.SettingItem
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.Check
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.ListenTogetherSettingsViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun ListenTogetherSettingsScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherSettingsViewModel = koinViewModel(),
) {
    val usingCustom by viewModel.usingCustomServer.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val autoJoins by viewModel.autoApproveJoins.collectAsStateWithLifecycle()
    val blocked by viewModel.blockedNames.collectAsStateWithLifecycle()
    var draftUrl by remember(serverUrl) { mutableStateOf(serverUrl) }

    val hazeState = rememberHazeState(blurEnabled = true)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        ) {
            item {
                Spacer(Modifier.height(64.dp))
            }
            
            item {
                Text(
                    text = stringResource(Res.string.lt_server),
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp, top = 16.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.useDefaultServer() }.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(Res.string.lt_default_server_name), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(text = stringResource(Res.string.lt_default_server_location), style = typo().bodyMedium)
                    }
                    if (!usingCustom) {
                        Spacer(Modifier.width(12.dp))
                        Icon(echoIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { if (!usingCustom) viewModel.setServerUrl(draftUrl.ifBlank { "wss://" }) }.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(Res.string.lt_custom_server), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(text = stringResource(Res.string.lt_custom_server_desc), style = typo().bodyMedium)
                    }
                    if (usingCustom) {
                        Spacer(Modifier.width(12.dp))
                        Icon(echoIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            if (usingCustom) {
                item {
                    OutlinedTextField(
                        value = draftUrl,
                        onValueChange = { draftUrl = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        label = { Text("Server URL") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                        )
                    )
                    if (draftUrl != serverUrl) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.setServerUrl(draftUrl) }) {
                                Text(stringResource(Res.string.lt_save_server))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(Res.string.lt_as_host),
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp, top = 24.dp)
                )
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.lt_auto_approve_joins),
                    subtitle = stringResource(Res.string.lt_auto_approve_joins_desc),
                    switch = (autoJoins to { viewModel.setAutoApproveJoins(it) })
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp, top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.lt_blocked),
                        style = typo().titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${blocked.size}", style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            if (blocked.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.lt_blocked_empty),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                items(blocked.size) { index ->
                    val name = blocked[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                name.trim().firstOrNull()?.uppercase() ?: "?",
                                style = typo().titleSmall,
                                color = MaterialTheme.colorScheme.surface,
                            )
                        }
                        Text(
                            name,
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.unblock(name) }) {
                            Text(stringResource(Res.string.lt_unblock))
                        }
                    }
                }
            }

            item {
                EndOfPage()
            }
        }
        
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.settings),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        echoIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            modifier = Modifier.hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )
    }
}
