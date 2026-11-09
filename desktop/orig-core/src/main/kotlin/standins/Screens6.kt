package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// shortcut: Android used an embedded WebView to read the Discord token; on desktop the token has to be pasted in.
@Composable
fun DiscordTokenWebView(onTokenExtracted: (String) -> Unit) {
  Text("In-app Discord login is not available on desktop yet.", Modifier.padding(24.dp))
}
