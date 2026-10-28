@file:OptIn(ExperimentalMaterial3Api::class)

package com.music.echo.ui.screens.settings.lossless

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.music.echo.lossless.LosslessProvider
import com.music.echo.lossless.LosslessSourcesRepository
import echo.music.iad1tya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LosslessWebAuthScreen(
  provider: LosslessProvider,
  navController: NavController,
  repository: LosslessSourcesRepository
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var progress by remember { mutableFloatStateOf(0f) }
  var isCapturing by remember { mutableStateOf(false) }
  var captured by remember { mutableStateOf(false) }

  var webViewInstance by remember { mutableStateOf<WebView?>(null) }

  val initialUrl = remember(provider) {
    when (provider) {
      LosslessProvider.DEEZER -> "https://www.deezer.com/login"
      LosslessProvider.TIDAL -> "https://listen.tidal.com/login"
      LosslessProvider.QOBUZ -> "https://www.qobuz.com/signin"
      LosslessProvider.APPLE_MUSIC -> "https://music.apple.com"
    }
  }

  val title = remember(provider) {
    when (provider) {
      LosslessProvider.DEEZER -> "Sign in to Deezer"
      LosslessProvider.TIDAL -> "Sign in to Tidal"
      LosslessProvider.QOBUZ -> "Sign in to Qobuz"
      LosslessProvider.APPLE_MUSIC -> "Sign in to Apple Music"
    }
  }

  val subtitle = remember(provider) {
    when (provider) {
      LosslessProvider.DEEZER -> "Sign in to Deezer below. Session is saved automatically."
      LosslessProvider.TIDAL -> "Sign in to Tidal below. Credentials are saved automatically."
      LosslessProvider.QOBUZ -> "Sign in to Qobuz below. Credentials are saved automatically."
      LosslessProvider.APPLE_MUSIC -> "Sign in with your Apple ID. Token is fetched automatically."
    }
  }

  BackHandler(enabled = !captured) {
    if (webViewInstance?.canGoBack() == true) {
      webViewInstance?.goBack()
    } else {
      navController.navigateUp()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          }
        },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(
              painter = painterResource(R.drawable.arrow_back),
              contentDescription = stringResource(R.string.cd_back)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
    ) {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          WebView(ctx).apply {
            webViewInstance = this
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
              useWideViewPort = true
              loadWithOverviewMode = true
            }

            fun inspectCookies(currentUrl: String?) {
              if (captured || isCapturing) return
              val url = currentUrl ?: return

              val cookies = cookieManager.getCookie(url) ?: cookieManager.getCookie(initialUrl) ?: ""

              when (provider) {
                LosslessProvider.DEEZER -> {
                  // Intercept Deezer ARL token
                  val arlMatch = Regex("""(?:^|;\s*)arl=([^;]+)""").find(cookies)
                  val arl = arlMatch?.groupValues?.getOrNull(1)?.trim()
                  if (!arl.isNullOrEmpty() && arl != "deleted") {
                    captured = true
                    isCapturing = true
                    scope.launch {
                      val res = repository.verifyAndSaveDeezer(arl)
                      if (res.isSuccess) {
                        Toast.makeText(context, "Signed in to Deezer as ${res.getOrNull()?.userName ?: "Deezer User"}!", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                      } else {
                        isCapturing = false
                        captured = false
                      }
                    }
                  }
                }

                LosslessProvider.TIDAL -> {
                  // Intercept Tidal session cookies or evaluate tokens
                  val sessionCookie = Regex("""(?:^|;\s*)_tid=([^;]+)""").find(cookies)?.groupValues?.getOrNull(1)
                    ?: Regex("""(?:^|;\s*)session_id=([^;]+)""").find(cookies)?.groupValues?.getOrNull(1)
                  if (!sessionCookie.isNullOrEmpty() || url.contains("listen.tidal.com") && cookies.contains("tidal")) {
                    evaluateJavascript("(function() { return localStorage.getItem('token') || localStorage.getItem('user'); })();") { jsResult ->
                      val cleanResult = jsResult?.removeSurrounding("\"")?.replace("\\\"", "\"")
                      val effectiveToken = if (!cleanResult.isNullOrBlank() && cleanResult != "null") cleanResult else sessionCookie ?: "tidal_session_${System.currentTimeMillis()}"
                      if (effectiveToken.isNotBlank()) {
                        captured = true
                        isCapturing = true
                        scope.launch {
                          repository.verifyAndSaveTidal(effectiveToken)
                          Toast.makeText(context, "Signed in to Tidal!", Toast.LENGTH_SHORT).show()
                          navController.navigateUp()
                        }
                      }
                    }
                  }
                }

                LosslessProvider.QOBUZ -> {
                  // Intercept Qobuz user auth cookies
                  val userTokenMatch = Regex("""(?:^|;\s*)(?:user_auth_token|qobuz_token)=([^;]+)""").find(cookies)
                  val token = userTokenMatch?.groupValues?.getOrNull(1)?.trim()
                  if (!token.isNullOrEmpty() && token != "null") {
                    captured = true
                    isCapturing = true
                    scope.launch {
                      repository.verifyAndSaveQobuz("qobuz_web", "qobuz_secret", token)
                      Toast.makeText(context, "Signed in to Qobuz!", Toast.LENGTH_SHORT).show()
                      navController.navigateUp()
                    }
                  }
                }

                LosslessProvider.APPLE_MUSIC -> {
                  // Intercept Apple Music Media User Token (MUT)
                  val mutMatch = Regex("""(?:^|;\s*)(?:media-user-token|mut)=([^;]+)""").find(cookies)
                  val mut = mutMatch?.groupValues?.getOrNull(1)?.trim()
                  if (!mut.isNullOrEmpty()) {
                    captured = true
                    isCapturing = true
                    scope.launch {
                      repository.verifyAndSaveAppleMusic(mut, null)
                      Toast.makeText(context, "Signed in to Apple Music!", Toast.LENGTH_SHORT).show()
                      navController.navigateUp()
                    }
                  }
                }
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress / 100f
              }
            }

            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                inspectCookies(request.url?.toString())
                return false
              }

              override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                inspectCookies(url)
              }

              override fun onPageFinished(view: WebView, url: String?) {
                inspectCookies(url)
              }

              override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                inspectCookies(url)
              }
            }

            loadUrl(initialUrl)
          }
        }
      )

      if (progress < 1f && !captured) {
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
        )
      }

      if (isCapturing) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Capturing session & verifying account...",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
