@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.recognition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun RecognitionScreen(navController: NavController) {
  Scaffold(topBar = { TopAppBar(title = { Text("Music recognition") }) }) { pad ->
    Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
      Text("Song recognition listens through the microphone with an Android-only service and is not available on desktop yet.")
      Button(onClick = { navController.navigateUp() }, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
    }
  }
}
