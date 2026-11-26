/*
 * EchoMusic (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Adapted from Vivi Music (vivimusic Project (C) 2026, GPL-3.0)
 * https://github.com/vivizzz007/vivi-music
 */

package echo.music.iad1tya.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import echo.music.iad1tya.spotify.SpotifyAuth

internal fun createSpotifyLoginWebView(
  container: FrameLayout,
  onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
  onActiveWebViewChanged: (WebView) -> Unit,
): WebView {
  var captured = false
  val cookieManager = CookieManager.getInstance()
  cookieManager.setAcceptCookie(true)

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

  return WebView(container.context).apply {
    cookieManager.setAcceptThirdPartyCookies(this, true)
    configureSpotifyLoginWebView()
    webViewClient = SpotifyLoginClient(captureCookies = ::captureCookies)
    webChromeClient =
      SpotifyLoginWebChromeClient(
        container = container,
        parentWebView = this,
        captureCookies = ::captureCookies,
        onActiveWebViewChanged = onActiveWebViewChanged,
      )
    onActiveWebViewChanged(this)
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
    loadUrl(SpotifyAuth.LOGIN_URL)
  }
}

internal fun WebView.destroySpotifyLoginWebView() {
  stopLoading()
  loadUrl("about:blank")
  (parent as? ViewGroup)?.removeView(this)
  destroy()
}

@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
internal fun WebView.configureSpotifyLoginWebView() {
  settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    javaScriptCanOpenWindowsAutomatically = true
    setSupportMultipleWindows(true)
    allowFileAccess = false
    allowContentAccess = false
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    loadWithOverviewMode = true
    useWideViewPort = true
    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    // Match the installed WebView Chrome version and drop the "; wv" embedded-WebView
    // marker. A hardcoded Chrome/129 UA is rejected by Google/Facebook as insecure.
    val defaultUa = userAgentString ?: WebSettings.getDefaultUserAgent(context)
    userAgentString =
      defaultUa.replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+\\s*"), "")
    hideRequestedWithHeader()
  }
}

@Suppress("DEPRECATION")
private fun WebSettings.hideRequestedWithHeader() {
  if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
    WebSettingsCompat.setRequestedWithHeaderOriginAllowList(this, emptySet())
  }
}

private class SpotifyLoginClient(
  private val captureCookies: (String?) -> Boolean,
) : WebViewClient() {
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

internal class SpotifyLoginWebChromeClient(
  private val container: FrameLayout,
  private val parentWebView: WebView,
  private val captureCookies: (String?) -> Boolean,
  private val onActiveWebViewChanged: (WebView) -> Unit,
) : WebChromeClient() {
  override fun onCreateWindow(
    view: WebView,
    isDialog: Boolean,
    isUserGesture: Boolean,
    resultMsg: Message,
  ): Boolean {
    closePopupWebViews()

    val popupWebView =
      WebView(view.context).apply {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        configureSpotifyLoginWebView()
        webViewClient = SpotifyLoginClient(captureCookies = captureCookies)
      }

    val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
    container.addView(
      popupWebView,
      FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
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

internal fun shouldOverrideSpotifyLoginUrl(
  view: WebView,
  url: String?,
  captureCookies: (String?) -> Boolean,
): Boolean {
  captureCookies(url)

  val targetUrl = url?.takeIf(String::isNotBlank) ?: return false
  if (targetUrl.isWebViewLoadableUrl()) return false

  targetUrl.intentBrowserFallbackUrl()?.let { fallbackUrl ->
    view.loadUrl(fallbackUrl)
    return true
  }
  return true
}

internal fun String.isWebViewLoadableUrl(): Boolean {
  val scheme = runCatching { Uri.parse(this).scheme?.lowercase() }.getOrNull()
  return scheme == "https" || scheme == "http"
}

internal fun String.intentBrowserFallbackUrl(): String? =
  runCatching { Intent.parseUri(this, Intent.URI_INTENT_SCHEME) }
    .getOrNull()
    ?.getStringExtra("browser_fallback_url")
    ?.takeIf { it.isWebViewLoadableUrl() }

internal fun readSpotifyCookies(
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

internal fun String.toSpotifyCookieOrigin(): String? {
  val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return null
  val host = uri.host?.lowercase() ?: return null
  if (host != "spotify.com" && !host.endsWith(".spotify.com")) return null
  val scheme =
    uri.scheme?.takeIf {
      it.equals("https", ignoreCase = true) || it.equals("http", ignoreCase = true)
    } ?: "https"
  return "$scheme://$host"
}
