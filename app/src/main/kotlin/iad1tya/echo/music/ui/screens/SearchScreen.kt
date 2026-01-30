package iad1tya.echo.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SearchScreen(
    navController: NavController,
    onSearchBarClick: () -> Unit
) {
    // Automatically open search bar when entering search screen
    LaunchedEffect(Unit) {
        onSearchBarClick()
    }

    // Navigate back to home when pressing back on this screen
    BackHandler {
        navController.navigate(Screens.Home.route) {
            popUpTo(Screens.Home.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Start typing to search...",
            modifier = Modifier.padding(16.dp)
        )
    }
}
