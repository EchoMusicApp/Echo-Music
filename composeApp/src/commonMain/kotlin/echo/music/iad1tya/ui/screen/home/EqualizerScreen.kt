package echo.music.iad1tya.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.equalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: echo.music.iad1tya.viewModel.SettingsViewModel = org.koin.compose.viewmodel.koinViewModel(),
) {
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getEqualizer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.equalizer), style = typo().titleMedium) },
                navigationIcon = {
                    RippleIconButton(
                        onClick = { navController.popBackStack() },
                        imageVector = echoIcons.ArrowBackIosNew,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    androidx.compose.material3.Switch(
                        checked = equalizerEnabled,
                        onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = equalizerEnabled,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                EqualizerSection(viewModel)
            }
        }
    }
}
