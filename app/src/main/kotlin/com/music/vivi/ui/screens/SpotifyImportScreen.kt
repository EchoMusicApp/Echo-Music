@file:OptIn(ExperimentalMaterial3Api::class)

package iad1tya.echo.music.ui.screens

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.R
import iad1tya.echo.music.spotifyimport.SpotifyImportViewModel
import iad1tya.echo.music.spotifyimport.SpotifyImportUiState
import iad1tya.echo.music.spotifyimport.SpotifyImportSummaryUi
import iad1tya.echo.music.spotifyimport.SpotifyImportSourceUi
import iad1tya.echo.music.spotifyimport.SpotifyImportSourceType
import iad1tya.echo.music.ui.component.DefaultDialog
import iad1tya.echo.music.ui.component.IconButton
import iad1tya.echo.music.ui.component.Material3SettingsGroup
import iad1tya.echo.music.ui.component.Material3SettingsItem
import iad1tya.echo.music.ui.utils.backToMain
import iad1tya.echo.music.spotify.SpotifyAuth
import android.net.Uri

@Composable
fun SpotifyImportScreen(
    navController: NavController,
    spotifyImportViewModel: SpotifyImportViewModel = hiltViewModel(),
) {
    val state by spotifyImportViewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    var showSpotifyLogin by remember { mutableStateOf(false) }
    var showSpotifySources by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.spotify_import_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                ),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val items = if (!state.isAuthenticated) {
                    listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.spotify_connect)) },
                            description = { Text(stringResource(R.string.spotify_not_connected)) },
                            icon = painterResource(R.drawable.ic_spotify),
                            enabled = state.progress == null && !state.isLoading,
                            onClick = { showSpotifyLogin = true }
                        )
                    )
                } else {
                    listOf(
                        Material3SettingsItem(
                            title = { 
                                Text(
                                    if (state.accountName.isNotBlank()) stringResource(R.string.spotify_connected_as, state.accountName)
                                    else stringResource(R.string.spotify_account)
                                )
                            },
                            description = if (state.isLoading) {
                                { Text(stringResource(R.string.spotify_loading_library)) }
                            } else null,
                            icon = painterResource(R.drawable.ic_spotify),
                            enabled = true,
                            onClick = null
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.spotify_select_sources)) },
                            description = { 
                                Text(
                                    if (state.hasSources) stringResource(R.string.spotify_available_count, state.sources.size)
                                    else stringResource(R.string.spotify_no_sources)
                                ) 
                            },
                            icon = painterResource(R.drawable.playlist_play),
                            enabled = state.hasSources && state.progress == null,
                            onClick = { showSpotifySources = true }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.spotify_import_selected)) },
                            description = { Text(stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size)) },
                            icon = painterResource(R.drawable.playlist_add),
                            enabled = state.canImport,
                            onClick = { spotifyImportViewModel.importSelectedSources() }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.spotify_refresh)) },
                            description = { Text(stringResource(R.string.spotify_import_desc)) },
                            icon = painterResource(R.drawable.sync),
                            enabled = !state.isLoading && state.progress == null,
                            onClick = { spotifyImportViewModel.loadSources() }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.action_logout)) },
                            icon = painterResource(R.drawable.logout),
                            enabled = !state.isLoading && state.progress == null,
                            onClick = { spotifyImportViewModel.logout() }
                        )
                    )
                }
                
                Material3SettingsGroup(
                    title = "Spotify Import",
                    items = items
                )
            }
        }
    }

    if (showSpotifyLogin) {
        SpotifyLoginSheet(
            onDismiss = { showSpotifyLogin = false },
            onCookiesCaptured = { spDc, spKey ->
                showSpotifyLogin = false
                spotifyImportViewModel.connectWithCookies(spDc = spDc, spKey = spKey)
            },
        )
    }

    if (showSpotifySources && state.isAuthenticated) {
        SpotifySourcePickerSheet(
            state = state,
            onDismiss = { showSpotifySources = false },
            onToggleSource = spotifyImportViewModel::toggleSource,
            onSelectAll = spotifyImportViewModel::selectAllSources,
            onClearSelection = spotifyImportViewModel::clearSelection,
            onImport = {
                showSpotifySources = false
                spotifyImportViewModel.importSelectedSources()
            },
        )
    }

    state.errorMessage?.let { error ->
        SpotifyErrorDialog(
            message = error,
            onDismiss = { spotifyImportViewModel.dismissError() }
        )
    }

    state.summary?.let { summary ->
        SpotifyImportSummaryDialog(
            summary = summary,
            onDismiss = { spotifyImportViewModel.dismissSummary() }
        )
    }

    state.progress?.let { progress ->
        DefaultDialog(
            onDismiss = { spotifyImportViewModel.cancelImport() },
            title = { Text(stringResource(R.string.spotify_import_in_progress)) },
            buttons = {
                TextButton(onClick = { spotifyImportViewModel.cancelImport() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.spotify_import_progress_step, progress.sourceTitle, progress.completedSources, progress.totalSources, progress.matchedTracks, progress.totalTracks))
                LinearProgressIndicator(
                    progress = { progress.percent.toFloat() / 100f },
                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun SpotifyLoginSheet(
    onDismiss: () -> Unit,
    onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var captured by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
            webView = null
        }
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.spotify_login_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.spotify_waiting_for_login),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large),
                factory = { context ->
                    WebView(context).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = object : WebViewClient() {
                            private fun captureCookies(url: String?): Boolean {
                                if (captured) return true
                                val cookies = readSpotifyCookies(cookieManager, url)
                                val spDc = cookies["sp_dc"].orEmpty()
                                if (spDc.isBlank()) return false
                                captured = true
                                cookieManager.flush()
                                onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                                return true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean = captureCookies(request.url?.toString())

                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: android.graphics.Bitmap?,
                            ) {
                                captureCookies(url)
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                captureCookies(url)
                            }
                        }
                        webView = this
                        cookieManager.removeAllCookies(null)
                        cookieManager.flush()
                        loadUrl(SpotifyAuth.LOGIN_URL)
                    }
                },
                update = { view ->
                    webView = view
                },
            )
        }
    }
}

private fun readSpotifyCookies(
    cookieManager: CookieManager,
    currentUrl: String?,
): Map<String, String> {
    val urls = linkedSetOf(
        "https://open.spotify.com",
        "https://accounts.spotify.com",
        "https://spotify.com",
    )
    currentUrl?.toSpotifyCookieOrigin()?.let(urls::add)
    val cookies = linkedMapOf<String, String>()
    cookieManager.flush()
    urls.forEach { url ->
        cookieManager.getCookie(url)
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.forEach { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@forEach
                val key = part.substring(0, separator).trim()
                val value = part.substring(separator + 1).trim()
                if (key.isNotBlank()) {
                    cookies[key] = value
                }
            }
    }
    return cookies
}

private fun String.toSpotifyCookieOrigin(): String? {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    if (host != "spotify.com" && !host.endsWith(".spotify.com")) return null
    val scheme = uri.scheme
        ?.takeIf { it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true) }
        ?: "https"
    return "$scheme://$host"
}

@Composable
private fun SpotifySourcePickerSheet(
    state: SpotifyImportUiState,
    onDismiss: () -> Unit,
    onToggleSource: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onImport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.spotify_select_sources),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onClearSelection,
                ) {
                    Text(stringResource(R.string.spotify_clear_selection))
                }
                TextButton(
                    onClick = onSelectAll,
                ) {
                    Text(stringResource(R.string.spotify_select_all))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
            ) {
                items(
                    items = state.sources,
                    key = { it.id },
                    contentType = { it.type },
                ) { source ->
                    SpotifySourceRow(
                        source = source,
                        selected = source.id in state.selectedSourceIds,
                        onClick = { onToggleSource(source.id) },
                    )
                }
            }

            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                enabled = state.canImport,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.spotify_import_selected))
            }
        }
    }
}

@Composable
private fun SpotifySourceRow(
    source: SpotifyImportSourceUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val subtitle = when {
        source.subtitle.isNotBlank() -> source.subtitle
        source.type == SpotifyImportSourceType.LIKED_SONGS -> stringResource(R.string.spotify_liked_songs_desc)
        else -> stringResource(R.string.spotify_account)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SpotifySourceThumbnail(source)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source.trackCount?.let { stringResource(R.string.spotify_track_count, it) } ?: subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
            )
        }
    }
}

@Composable
private fun SpotifySourceThumbnail(source: SpotifyImportSourceUi) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!source.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = source.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(
                    if (source.type == SpotifyImportSourceType.LIKED_SONGS) {
                        R.drawable.favorite
                    } else {
                        R.drawable.playlist_play
                    },
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SpotifyErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text("Import failed") },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SpotifyImportSummaryDialog(
    summary: SpotifyImportSummaryUi,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.spotify_import_complete)) },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(
                    R.string.spotify_import_summary,
                    summary.sourceCount,
                    summary.importedTracks,
                    summary.failedTracks,
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            summary.sources.forEach { source ->
                Text(
                    text = stringResource(
                        R.string.spotify_source_summary,
                        source.title,
                        source.importedTracks,
                        source.totalTracks,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
