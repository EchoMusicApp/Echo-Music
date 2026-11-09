@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AppIconSettingsScreen(navController: NavController, activity: Activity, snackbarHostState: SnackbarHostState) {
  Scaffold(topBar = { TopAppBar(title = { Text("App icon") }) }) { pad ->
    Column(Modifier.padding(pad).padding(16.dp)) {
      Text("Changing the app icon is an Android launcher feature and does not exist on desktop.")
      Button(onClick = { navController.navigateUp() }, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }
  }
}
