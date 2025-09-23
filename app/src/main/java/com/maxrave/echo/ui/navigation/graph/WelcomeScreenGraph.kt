package iad1tya.echo.music.ui.navigation.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import iad1tya.echo.music.ui.navigation.destination.welcome.UserNameDestination
import iad1tya.echo.music.ui.navigation.destination.welcome.WelcomeDestination
import iad1tya.echo.music.ui.screen.welcome.UserNameScreen
import iad1tya.echo.music.ui.screen.welcome.WelcomeScreen
import iad1tya.echo.music.viewModel.WelcomeViewModel
import org.koin.androidx.compose.koinViewModel

@UnstableApi
fun NavGraphBuilder.welcomeScreenGraph(
    navController: NavController,
    onComplete: () -> Unit
) {
    composable<WelcomeDestination> {
        WelcomeScreen(
            onAnimationComplete = {
                navController.navigate(UserNameDestination)
            }
        )
    }
    
    composable<UserNameDestination> {
        val welcomeViewModel: WelcomeViewModel = koinViewModel()
        UserNameScreen(
            onNameEntered = { name ->
                welcomeViewModel.setUserName(name)
                welcomeViewModel.completeOnboarding()
                onComplete()
            },
            onSkip = {
                welcomeViewModel.completeOnboarding()
                onComplete()
            }
        )
    }
    
}
