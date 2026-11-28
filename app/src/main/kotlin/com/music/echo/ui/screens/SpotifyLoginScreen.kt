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
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
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
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLoginScreen(
  navController: NavController,
  onCookiesCaptured: (spDc: String, spKey: String) -> Unit,
) {
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.destroySpotifyLoginWebView()
      webViewRef = null
    }
  }

  AndroidView(
    modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).fillMaxSize(),
    factory = { context ->
      val container = FrameLayout(context)
      val loginWebView =
        createSpotifyLoginWebView(
          container = container,
          onCookiesCaptured = { spDc, spKey ->
            onCookiesCaptured(spDc, spKey)
            navController.navigateUp()
          },
          onActiveWebViewChanged = { active -> webViewRef = active },
        )
      loginWebView.setOnTouchListener { v, event ->
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
      container.addView(
        loginWebView,
        FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        ),
      )
      container
    },
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
