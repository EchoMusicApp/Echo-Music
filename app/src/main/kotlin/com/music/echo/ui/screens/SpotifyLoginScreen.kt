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
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.spotify.SpotifyAuth
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain

private const val SPOTIFY_LOGIN_USER_AGENT =
  "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

@SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLoginScreen(
  navController: NavController,
  onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var captured by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.stopLoading()
      webViewRef?.loadUrl("about:blank")
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  AndroidView(
    modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).fillMaxSize(),
    factory = { context ->
      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
      cookieManager.removeAllCookies(null)
      cookieManager.flush()

      WebView(context).apply {
        layoutParams =
          android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
          )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.userAgentString = SPOTIFY_LOGIN_USER_AGENT
        cookieManager.setAcceptThirdPartyCookies(this, true)

        setOnTouchListener { v, event ->
          when (event.action) {
            android.view.MotionEvent.ACTION_DOWN,
            android.view.MotionEvent.ACTION_MOVE -> {
              v.parent.requestDisallowInterceptTouchEvent(true)
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
              v.parent.requestDisallowInterceptTouchEvent(false)
            }
          }
          false
        }

        webViewClient =
          object : WebViewClient() {
            private fun tryCapture(): Boolean {
              if (captured) return false
              cookieManager.flush()
              val cookiesStr = cookieManager.getCookie("https://open.spotify.com") ?: ""
              val cookies =
                cookiesStr.split(";").associate {
                  val parts = it.split("=")
                  val key = parts.firstOrNull()?.trim().orEmpty()
                  val value = parts.drop(1).joinToString("=").trim()
                  key to value
                }
              val spDc = cookies["sp_dc"].orEmpty()
              if (spDc.isBlank()) return false
              captured = true
              onCookiesCaptured(spDc, cookies["sp_key"].orEmpty())
              navController.navigateUp()
              return true
            }

            override fun shouldOverrideUrlLoading(
              view: WebView,
              request: WebResourceRequest,
            ): Boolean {
              tryCapture()
              return false
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(
              view: WebView,
              url: String?,
            ): Boolean {
              tryCapture()
              return false
            }

            override fun onPageStarted(
              view: WebView,
              url: String?,
              favicon: android.graphics.Bitmap?,
            ) {
              tryCapture()
            }

            override fun onPageFinished(
              view: WebView,
              url: String?,
            ) {
              tryCapture()
            }
          }

        webChromeClient = android.webkit.WebChromeClient()
        webViewRef = this
        loadUrl(SpotifyAuth.LOGIN_URL)
      }
    },
    update = { view -> webViewRef = view },
  )

  TopAppBar(
    title = { Text(stringResource(R.string.spotify_login_title)) },
    navigationIcon = {
      IconButton(
        onClick = navController::navigateUp,
        onLongClick = navController::backToMain,
      ) {
        Icon(
          painterResource(R.drawable.arrow_back),
          contentDescription = null,
        )
      }
    },
  )

  BackHandler(enabled = webViewRef?.canGoBack() == true) {
    webViewRef?.goBack()
  }
}
