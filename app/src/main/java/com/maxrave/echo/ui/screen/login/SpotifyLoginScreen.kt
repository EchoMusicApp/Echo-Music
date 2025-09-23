package iad1tya.echo.music.ui.screen.login

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.LogoDev
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import iad1tya.echo.music.R
import iad1tya.echo.music.common.Config
import iad1tya.echo.music.ui.component.DevCookieLogInBottomSheet
import iad1tya.echo.music.ui.component.DevLogInBottomSheet
import iad1tya.echo.music.ui.component.DevLogInType
import iad1tya.echo.music.ui.component.RippleIconButton
import iad1tya.echo.music.ui.theme.typo
import iad1tya.echo.music.viewModel.LogInViewModel
import iad1tya.echo.music.viewModel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@SuppressLint("SetJavaScriptEnabled")
@UnstableApi
@Composable
fun SpotifyLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val context = LocalContext.current
    val hazeState = rememberHazeState()
    val spotifyStatus by viewModel.spotifyStatus.collectAsStateWithLifecycle()

    val fullSpotifyCookies by viewModel.fullSpotifyCookies.collectAsStateWithLifecycle()

    var devLoginSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showCookiesBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // Hide bottom navigation when entering this screen
    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    // Handle login success
    LaunchedEffect(spotifyStatus) {
        if (spotifyStatus) {
            // Set the login status immediately
            settingsViewModel.setSpotifyLogIn(true)
            // Enable Spotify Canvas by default when user logs in
            settingsViewModel.setSpotifyCanvas(true)
            Toast
                .makeText(
                    context,
                    R.string.login_success,
                    Toast.LENGTH_SHORT,
                ).show()
            // Navigate back after a short delay to ensure state is updated
            delay(200)
            navController.navigateUp()
        }
    }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column {
            Spacer(
                Modifier
                    .size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
            )
            // WebView for Spotify login
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?,
                                ) {
                                    // Always extract cookies on every page load
                                    CookieManager.getInstance().getCookie(url)?.let { cookie ->
                                        val cookies =
                                            cookie.split("; ").map {
                                                val (key, value) = it.split("=")
                                                key to value
                                            }
                                        viewModel.setFullSpotifyCookies(cookies)
                                        
                                        // Check if we have the sp_dc cookie (indicates successful login)
                                        val cookieMap = cookies.associate { it.first to it.second }
                                        val spdc = cookieMap["sp_dc"]
                                        
                                        if (spdc != null && spdc.isNotEmpty()) {
                                            // We have a valid sp_dc cookie, user is logged in
                                            viewModel.saveSpotifySpdc(cookie)
                                            // Immediately update the settings view model
                                            settingsViewModel.setSpotifyLogIn(true)
                                            // Enable Spotify Canvas by default when user logs in
                                            settingsViewModel.setSpotifyCanvas(true)
                                            // Also force set as fallback
                                            settingsViewModel.forceSetSpotifyLoginStatus(true)
                                            
                                            // Clear WebView data after successful login
                                            WebStorage.getInstance().deleteAllData()
                                            CookieManager.getInstance().removeAllCookies(null)
                                            CookieManager.getInstance().flush()
                                            clearCache(true)
                                            clearFormData()
                                            clearHistory()
                                            clearSslPreferences()
                                        }
                                    }
                                }
                            }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(Config.SPOTIFY_LOG_IN_URL)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = "Log in to Spotify",
                    style = typo.titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        R.drawable.baseline_arrow_back_ios_new_24,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        devLoginSheet = true
                    },
                ) {
                    Icon(
                        Icons.Default.LogoDev,
                        "Developer Mode",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )

        FloatingActionButton(
            onClick = {
                showCookiesBottomSheet = true
            },
            containerColor = Color(0xFF40D96A),
            modifier =
                Modifier
                    .align(
                        Alignment.BottomStart,
                    ).padding(innerPadding)
                    .padding(
                        25.dp,
                    ),
        ) {
            Icon(
                Icons.Default.Cookie,
                "Cookies",
            )
        }
    }
    if (devLoginSheet) {
        DevLogInBottomSheet(
            onDismiss = {
                devLoginSheet = false
            },
            onDone = { spdc ->
                devLoginSheet = false
                val spdcText = "sp_dc=$spdc"
                viewModel.saveSpotifySpdc(spdcText)
                // Immediately update the settings view model
                settingsViewModel.setSpotifyLogIn(true)
                // Enable Spotify Canvas by default when user logs in
                settingsViewModel.setSpotifyCanvas(true)
                // Also force set as fallback
                settingsViewModel.forceSetSpotifyLoginStatus(true)
                Toast
                    .makeText(
                        context,
                        R.string.login_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                navController.navigateUp()
            },
            type = DevLogInType.Spotify,
        )
    }

    if (showCookiesBottomSheet) {
        DevCookieLogInBottomSheet(
            onDismiss = {
                showCookiesBottomSheet = false
            },
            type = DevLogInType.Spotify,
            cookies = fullSpotifyCookies,
        )
    }
}