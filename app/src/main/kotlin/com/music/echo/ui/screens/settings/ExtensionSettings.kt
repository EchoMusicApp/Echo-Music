package com.music.echo.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.music.echo.extensions.AddonEndpoint
import com.music.echo.extensions.ExtensionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch




@HiltViewModel
class ExtensionSettingsViewModel @Inject constructor(
    private val extensionManager: ExtensionManager
) : ViewModel() {
    private val _addons = MutableStateFlow<List<AddonEndpoint>>(emptyList())
    val addons: StateFlow<List<AddonEndpoint>> = _addons

    init {
        loadAddons()
    }

    private fun loadAddons() {
        viewModelScope.launch {
            _addons.value = extensionManager.getAddons()
        }
    }

    fun addAddon(name: String, url: String) {
        viewModelScope.launch {
            extensionManager.addAddon(AddonEndpoint(UUID.randomUUID().toString(), name, url))
            loadAddons()
        }
    }

    fun toggleAddon(id: String, enabled: Boolean) {
        viewModelScope.launch {
            extensionManager.toggleAddon(id, enabled)
            loadAddons()
        }
    }

    fun removeAddon(id: String) {
        viewModelScope.launch {
            extensionManager.removeAddon(id)
            loadAddons()
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSettingsScreen(
    navController: NavController? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    viewModel: ExtensionSettingsViewModel = hiltViewModel()
) {
    val addons by viewModel.addons.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(echo.music.iad1tya.LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.windowInsetsPadding(echo.music.iad1tya.LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(16.dp))

        if (addons.isNotEmpty()) {
            Material3SettingsGroup(
                title = "Installed Addons",
                items = addons.map { addon ->
                    Material3SettingsItem(
                        title = { Text(addon.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        description = { Text(addon.url, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        customIcon = {
                            coil3.compose.AsyncImage(
                                model = "${addon.url.removeSuffix("/")}/favicon.ico",
                                contentDescription = null,
                                placeholder = androidx.compose.ui.res.painterResource(echo.music.iad1tya.R.drawable.extension),
                                error = androidx.compose.ui.res.painterResource(echo.music.iad1tya.R.drawable.extension),
                                fallback = androidx.compose.ui.res.painterResource(echo.music.iad1tya.R.drawable.extension),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Switch(
                                    checked = addon.enabled,
                                    onCheckedChange = { viewModel.toggleAddon(addon.id, it) },
                                    modifier = Modifier.scale(0.8f)
                                )
                                androidx.compose.material3.IconButton(
                                    onClick = { viewModel.removeAddon(addon.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.delete),
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        onClick = {}
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
        }

        Material3SettingsGroup(
            title = "Add new",
            items = listOf(
                Material3SettingsItem(
                    title = { Text("Add Custom Server") },
                    description = { Text("Configure a custom addon host") },
                    icon = painterResource(R.drawable.add),
                    onClick = { showDialog = true }
                )
            )
        )
        
        Spacer(Modifier.height(32.dp))

        if (showDialog) {
            var name by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
            var url by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
            
            echo.music.iad1tya.ui.component.TextFieldDialog(
                title = { Text("Add Extension") },
                textFields = listOf("Name" to name, "Server URL" to url),
                onTextFieldsChange = { index, value ->
                    if (index == 0) name = value
                    if (index == 1) url = value
                },
                onDismiss = { showDialog = false },
                onDoneMultiple = { inputs ->
                    val finalName = inputs.getOrNull(0) ?: ""
                    val finalUrl = inputs.getOrNull(1) ?: ""
                    if (finalName.isNotBlank() && finalUrl.isNotBlank()) {
                        viewModel.addAddon(finalName, finalUrl)
                        showDialog = false
                    }
                }
            )
        }
    }
    TopAppBar(
        title = { Text("Extension Sources") },
        navigationIcon = {
            if (navController != null) {
                echo.music.iad1tya.ui.component.IconButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = navController::backToMain
                ) {
                    Icon(
                        painterResource(echo.music.iad1tya.R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}