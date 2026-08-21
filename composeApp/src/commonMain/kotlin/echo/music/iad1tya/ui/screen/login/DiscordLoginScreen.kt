package echo.music.iad1tya.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.expect.ui.DiscordWebView
import echo.music.iad1tya.expect.ui.rememberWebViewState
import echo.music.iad1tya.extension.getStringBlocking
import echo.music.iad1tya.ui.component.DevLogInBottomSheet
import echo.music.iad1tya.ui.component.DevLogInType
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.LogoDev
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.LogInViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.log_in_to_discord
import echomusic.composeapp.generated.resources.login_success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinInject(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    var devLoginSheet by rememberSaveable {
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

    val state = rememberWebViewState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Spacer(
                Modifier
                    .size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
            )
            // WebView for Discord login
            DiscordWebView(
                state,
                aboveContent = {
                    if (devLoginSheet) {
                        DevLogInBottomSheet(
                            onDismiss = {
                                devLoginSheet = false
                            },
                            onDone = { token ->
                                devLoginSheet = false
                                viewModel.saveDiscordToken(token)
                                viewModel.makeToast(getStringBlocking(Res.string.login_success))
                                navController.navigateUp()
                            },
                            type = DevLogInType.Discord,
                        )
                    }
                }
            ) { token ->
                viewModel.saveDiscordToken(token)
                viewModel.makeToast(getStringBlocking(Res.string.login_success))
                navController.navigateUp()
            }
        }
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter),
            title = {
                Text(
                    text = stringResource(Res.string.log_in_to_discord),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        echoIcons.ArrowBackIosNew,
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
                        echoIcons.LogoDev,
                        "Developer Mode",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}