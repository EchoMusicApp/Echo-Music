@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

// shortcut: the comment list/detail layout needs the Material3 adaptive library, which is not wired on desktop yet.
@Composable
fun CommentSheet(videoId: String, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Text("Comments are not available on desktop yet.", Modifier.padding(24.dp))
  }
}
