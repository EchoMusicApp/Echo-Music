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

// The Android "system equalizer" dialog is replaced by the app's own equalizer page (profiles + parametric bands), which drives VLC.
@Composable
fun EqScreen(navController: NavController? = null) {
  androidx.compose.runtime.LaunchedEffect(Unit) {
    navController?.popBackStack()
    navController?.navigate("settings/equalizer")
  }
}
