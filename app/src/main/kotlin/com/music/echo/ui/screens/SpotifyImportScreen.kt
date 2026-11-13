@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.spotify.SpotifyAuth
import echo.music.iad1tya.spotifyimport.SpotifyImportProgressUi
import echo.music.iad1tya.spotifyimport.SpotifyImportSourceType
import echo.music.iad1tya.spotifyimport.SpotifyImportSourceUi
import echo.music.iad1tya.spotifyimport.SpotifyImportSummaryUi
import echo.music.iad1tya.spotifyimport.SpotifyImportUiState
import echo.music.iad1tya.spotifyimport.SpotifyImportViewModel
import echo.music.iad1tya.ui.component.DefaultDialog
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain

@Composable
fun SpotifyImportScreen(
  navController: NavController,
  spotifyImportViewModel: SpotifyImportViewModel = hiltViewModel(),
) {
  val state by spotifyImportViewModel.uiState.collectAsStateWithLifecycle()
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  var showSpotifyLogin by remember { mutableStateOf(false) }
  var showSpotifySources by remember { mutableStateOf(false) }
  var showAddByLink by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.spotify_import_title)) },
        navigationIcon = {
          IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
            Icon(painterResource(R.drawable.arrow_back), null)
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(
            LocalPlayerAwareWindowInsets.current.only(
              WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
          ),
      contentPadding =
        PaddingValues(
          top = innerPadding.calculateTopPadding(),
          bottom = innerPadding.calculateBottomPadding() + 32.dp,
          start = 16.dp,
          end = 16.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
          title = "Spotify Import",
          items =
            spotifyImportItems(
              state = state,
              viewModel = spotifyImportViewModel,
              onConnect = { showSpotifyLogin = true },
              onSelectSources = { showSpotifySources = true },
              onAddByLink = { showAddByLink = true },
            ),
        )
      }
    }
  }

  SpotifyImportDialogs(
    state = state,
    viewModel = spotifyImportViewModel,
    showSpotifyLogin = showSpotifyLogin,
    showAddByLink = showAddByLink,
    showSpotifySources = showSpotifySources,
    onDismissLogin = { showSpotifyLogin = false },
    onDismissAddByLink = { showAddByLink = false },
    onDismissSources = { showSpotifySources = false },
  )
}

@Composable
private fun spotifyImportItems(
  state: SpotifyImportUiState,
  viewModel: SpotifyImportViewModel,
  onConnect: () -> Unit,
  onSelectSources: () -> Unit,
  onAddByLink: () -> Unit,
): List<Material3SettingsItem> {
  if (!state.isAuthenticated) {
    return listOf(
      Material3SettingsItem(
        title = { Text(stringResource(R.string.spotify_connect)) },
        description = { Text(stringResource(R.string.spotify_not_connected)) },
        icon = painterResource(R.drawable.ic_spotify),
        enabled = state.progress == null && !state.isLoading,
        onClick = onConnect,
      ),
      Material3SettingsItem(
        title = { Text(stringResource(R.string.spotify_import_by_link)) },
        description = { Text(stringResource(R.string.spotify_import_by_link_desc)) },
        icon = painterResource(R.drawable.link),
        enabled = state.progress == null && !state.isLoading,
        onClick = onAddByLink,
      ),
    )
  }

  val idle = !state.isLoading && state.progress == null
  return listOf(
    Material3SettingsItem(
      title = {
        Text(
          if (state.accountName.isNotBlank())
            stringResource(R.string.spotify_connected_as, state.accountName)
          else stringResource(R.string.spotify_account)
        )
      },
      description =
        if (state.isLoading) {
          { Text(stringResource(R.string.spotify_loading_library)) }
        } else null,
      icon = painterResource(R.drawable.ic_spotify),
      enabled = true,
      onClick = null,
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
      onClick = onSelectSources,
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_import_by_link)) },
      description = { Text(stringResource(R.string.spotify_import_by_link_desc)) },
      icon = painterResource(R.drawable.link),
      enabled = idle,
      onClick = onAddByLink,
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_import_selected)) },
      description = {
        Text(stringResource(R.string.spotify_selected_count, state.selectedSourceIds.size))
      },
      icon = painterResource(R.drawable.playlist_add),
      enabled = state.canImport,
      onClick = { viewModel.importSelectedSources() },
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.spotify_refresh)) },
      description = { Text(stringResource(R.string.spotify_import_desc)) },
      icon = painterResource(R.drawable.sync),
      enabled = idle,
      onClick = { viewModel.loadSources() },
    ),
    Material3SettingsItem(
      title = { Text(stringResource(R.string.action_logout)) },
      description = { Text(stringResource(R.string.action_logout_desc)) },
      icon = painterResource(R.drawable.logout),
      enabled = idle,
      onClick = { viewModel.logout() },
    ),
  )
}

@Composable
private fun SpotifyImportDialogs(
  state: SpotifyImportUiState,
  viewModel: SpotifyImportViewModel,
  showSpotifyLogin: Boolean,
  showAddByLink: Boolean,
  showSpotifySources: Boolean,
  onDismissLogin: () -> Unit,
  onDismissAddByLink: () -> Unit,
  onDismissSources: () -> Unit,
) {
  if (showSpotifyLogin) {
    SpotifyLoginSheet(
      onDismiss = onDismissLogin,
      onCookiesCaptured = { spDc, spKey ->
        onDismissLogin()
        viewModel.connectWithCookies(spDc = spDc, spKey = spKey)
      },
    )
  }

  if (showAddByLink) {
    SpotifyAddByLinkDialog(
      enabled = !state.isLoading && state.progress == null,
      onDismiss = onDismissAddByLink,
      onAdd = { link ->
        onDismissAddByLink()
        viewModel.addPlaylistByUrl(link)
      },
    )
  }

  if (showSpotifySources && state.isAuthenticated) {
    SpotifySourcePickerSheet(
      state = state,
      onDismiss = onDismissSources,
      onToggleSource = viewModel::toggleSource,
      onSelectAll = viewModel::selectAllSources,
      onClearSelection = viewModel::clearSelection,
      onImport = {
        onDismissSources()
        viewModel.importSelectedSources()
      },
    )
  }

  state.errorMessage?.let { error ->
    SpotifyErrorDialog(
      message = error,
      onDismiss = { viewModel.dismissError() },
    )
  }

  state.summary?.let { summary ->
    SpotifyImportSummaryDialog(
      summary = summary,
      onDismiss = { viewModel.dismissSummary() },
    )
  }

  state.progress?.let { progress ->
    SpotifyImportProgressDialog(
      progress = progress,
      onCancel = { viewModel.cancelImport() },
    )
  }
}

@Composable
private fun SpotifyImportProgressDialog(
  progress: SpotifyImportProgressUi,
  onCancel: () -> Unit,
) {
  DefaultDialog(
    onDismiss = onCancel,
    title = { Text(stringResource(R.string.spotify_import_in_progress)) },
    buttons = { TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) } },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        stringResource(
          R.string.spotify_import_progress_step,
          progress.sourceTitle,
          progress.completedSources,
          progress.totalSources,
          progress.matchedTracks,
          progress.totalTracks,
        )
      )
      LinearProgressIndicator(
        progress = { progress.percent.toFloat() / 100f },
        modifier = Modifier.fillMaxWidth().clip(CircleShape),
      )
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
      modifier =
        Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 20.dp).padding(bottom = 20.dp),
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
        modifier = Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large),
        factory = { context ->
          val container = android.widget.FrameLayout(context)
          var mainWebView: WebView? = null
          val spotifyWebView =
            WebView(context).apply {
              val cookieManager = CookieManager.getInstance()
              cookieManager.setAcceptCookie(true)
              cookieManager.setAcceptThirdPartyCookies(this, true)
              configureSpotifyLoginWebView()

              fun captureCookies(url: String?): Boolean {
                if (captured) return true
                val cookies = readSpotifyCookies(cookieManager, url)
                val spDc = cookies["sp_dc"].orEmpty()
                if (spDc.isBlank()) return false
                captured = true
                cookieManager.flush()
                onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
                return true
              }

              webViewClient =
                object : WebViewClient() {
                  override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                  ): Boolean =
                    shouldOverrideSpotifyLoginUrl(
                      view = view,
                      url = request.url?.toString(),
                      captureCookies = { url -> captureCookies(url) },
                    )

                  @Deprecated("Deprecated in Java")
                  override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String?,
                  ): Boolean =
                    shouldOverrideSpotifyLoginUrl(
                      view = view,
                      url = url,
                      captureCookies = { targetUrl -> captureCookies(targetUrl) },
                    )

                  override fun onPageStarted(
                    view: WebView,
                    url: String?,
                    favicon: android.graphics.Bitmap?,
                  ) {
                    captureCookies(url)
                  }

                  override fun onPageFinished(
                    view: WebView,
                    url: String?,
                  ) {
                    captureCookies(url)
                  }
                }

              webChromeClient =
                SpotifyLoginWebChromeClient(
                  container = container,
                  parentWebView = this,
                  captureCookies = { url -> captureCookies(url) },
                  onActiveWebViewChanged = { activeWebView -> webView = activeWebView },
                )

              webView = this
              mainWebView = this
              cookieManager.removeAllCookies(null)
              cookieManager.flush()
              loadUrl(echo.music.iad1tya.spotify.SpotifyAuth.LOGIN_URL)
            }
          container.addView(
            spotifyWebView,
            android.widget.FrameLayout.LayoutParams(
              android.view.ViewGroup.LayoutParams.MATCH_PARENT,
              android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            ),
          )
          container
        },
        update = { view ->
          // Handled by onActiveWebViewChanged callback
        },
      )
    }
  }
}

private const val SpotifyLoginUserAgent =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

private fun WebView.destroySpotifyLoginWebView() {
  stopLoading()
  loadUrl("about:blank")
  (parent as? android.view.ViewGroup)?.removeView(this)
  destroy()
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureSpotifyLoginWebView() {
  settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    javaScriptCanOpenWindowsAutomatically = true
    setSupportMultipleWindows(true)
    allowFileAccess = false
    allowContentAccess = false
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
    userAgentString = SpotifyLoginUserAgent
  }
}

private class SpotifyLoginWebChromeClient(
  private val container: android.widget.FrameLayout,
  private val parentWebView: WebView,
  private val captureCookies: (String?) -> Boolean,
  private val onActiveWebViewChanged: (WebView) -> Unit,
) : android.webkit.WebChromeClient() {
  override fun onCreateWindow(
    view: WebView,
    isDialog: Boolean,
    isUserGesture: Boolean,
    resultMsg: android.os.Message,
  ): Boolean {
    closePopupWebViews()

    val popupWebView =
      WebView(view.context).apply {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        configureSpotifyLoginWebView()
        webViewClient =
          object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              view: WebView,
              request: WebResourceRequest,
            ): Boolean =
              shouldOverrideSpotifyLoginUrl(
                view = view,
                url = request.url?.toString(),
                captureCookies = captureCookies,
              )

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(
              view: WebView,
              url: String?,
            ): Boolean =
              shouldOverrideSpotifyLoginUrl(view = view, url = url, captureCookies = captureCookies)

            override fun onPageStarted(
              view: WebView,
              url: String?,
              favicon: android.graphics.Bitmap?,
            ) {
              captureCookies(url)
            }

            override fun onPageFinished(
              view: WebView,
              url: String?,
            ) {
              captureCookies(url)
            }
          }
      }

    val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
    container.addView(
      popupWebView,
      android.widget.FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
      ),
    )
    popupWebView.bringToFront()
    popupWebView.requestFocus()
    onActiveWebViewChanged(popupWebView)
    transport.webView = popupWebView
    resultMsg.sendToTarget()
    return true
  }

  override fun onCloseWindow(window: WebView) {
    window.destroySpotifyLoginWebView()
    onActiveWebViewChanged(parentWebView)
  }

  private fun closePopupWebViews() {
    for (index in container.childCount - 1 downTo 0) {
      val child = container.getChildAt(index) as? WebView ?: continue
      if (child !== parentWebView) {
        child.destroySpotifyLoginWebView()
      }
    }
    onActiveWebViewChanged(parentWebView)
  }
}

private fun shouldOverrideSpotifyLoginUrl(
  view: WebView,
  url: String?,
  captureCookies: (String?) -> Boolean,
): Boolean {
  if (captureCookies(url)) return true

  val targetUrl = url?.takeIf(String::isNotBlank) ?: return false
  if (targetUrl.isWebViewLoadableUrl()) return false

  targetUrl.intentBrowserFallbackUrl()?.let { fallbackUrl -> view.loadUrl(fallbackUrl) }
  return true
}

private fun String.isWebViewLoadableUrl(): Boolean {
  val scheme = runCatching { android.net.Uri.parse(this).scheme?.lowercase() }.getOrNull()
  return scheme == "https" || scheme == "http"
}

private fun String.intentBrowserFallbackUrl(): String? = runCatching {
  android.content.Intent.parseUri(this, android.content.Intent.URI_INTENT_SCHEME)
}
  .getOrNull()
  ?.getStringExtra("browser_fallback_url")
  ?.takeIf { it.isWebViewLoadableUrl() }

private fun readSpotifyCookies(
  cookieManager: CookieManager,
  currentUrl: String?,
): Map<String, String> {
  val urls =
    linkedSetOf(
      "https://open.spotify.com",
      "https://accounts.spotify.com",
      "https://spotify.com",
    )
  currentUrl?.toSpotifyCookieOrigin()?.let(urls::add)
  val cookies = linkedMapOf<String, String>()
  cookieManager.flush()
  urls.forEach { url ->
    cookieManager
      .getCookie(url)
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
  val scheme =
    uri.scheme?.takeIf {
      it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true)
    } ?: "https"
  return "$scheme://$host"
}

@Composable
private fun SpotifyAddByLinkDialog(
  enabled: Boolean,
  onDismiss: () -> Unit,
  onAdd: (String) -> Unit,
) {
  var link by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

  echo.music.iad1tya.ui.component.TextFieldDialog(
    icon = { Icon(painter = painterResource(R.drawable.link), contentDescription = null) },
    title = {
      Column {
        Text(text = stringResource(R.string.spotify_import_by_link))
        Text(
          text = stringResource(R.string.spotify_import_by_link_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
    },
    initialTextFieldValue = link,
    autoFocus = true,
    placeholder = { Text(stringResource(R.string.spotify_import_by_link_hint)) },
    onDismiss = onDismiss,
    onDone = { finalUrl ->
      if (enabled && finalUrl.isNotBlank()) {
        onAdd(finalUrl)
      }
    },
  )
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
      modifier =
        Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 20.dp).padding(bottom = 20.dp),
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
        TextButton(onClick = onClearSelection) {
          Text(stringResource(R.string.spotify_clear_selection))
        }
        TextButton(onClick = onSelectAll) {
          Text(stringResource(R.string.spotify_select_all))
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
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
  val subtitle =
    when {
      source.subtitle.isNotBlank() -> source.subtitle
      source.type == SpotifyImportSourceType.LIKED_SONGS ->
        stringResource(R.string.spotify_liked_songs_desc)
      else -> stringResource(R.string.spotify_account)
    }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color =
      if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
    onClick = onClick,
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 14.dp, vertical = 12.dp),
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
          text =
            source.trackCount?.let { stringResource(R.string.spotify_track_count, it) } ?: subtitle,
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
    modifier =
      Modifier.size(48.dp)
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
        painter =
          painterResource(
            if (source.type == SpotifyImportSourceType.LIKED_SONGS) {
              R.drawable.favorite
            } else {
              R.drawable.playlist_play
            }
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
    buttons = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
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
    buttons = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        text =
          stringResource(
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
          text =
            stringResource(
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
