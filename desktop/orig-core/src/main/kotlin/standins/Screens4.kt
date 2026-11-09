@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.equalizer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// shortcut: the system equalizer screen drives Android's AudioEffect API; desktop has no equivalent yet.
@Composable
fun EqScreen(navController: NavController? = null) {
  AlertDialog(
    onDismissRequest = { navController?.navigateUp() },
    confirmButton = { TextButton(onClick = { navController?.navigateUp() }) { Text("OK") } },
    title = { Text("Equalizer") },
    text = { Text("The system equalizer is Android-only. Desktop equalizing will come through the audio engine.") },
  )
}
