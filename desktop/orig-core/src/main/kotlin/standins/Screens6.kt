package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.desktop.BrowserLogin
import javax.swing.SwingUtilities

/** Discord login in the person's own browser (the app's embedded browser cannot be used: sites call it insecure). The token is read from the signed-in Discord web app. */
@Composable
fun DiscordTokenWebView(onTokenExtracted: (String) -> Unit) {
  var problem by remember { mutableStateOf<String?>(null) }
  DisposableEffect(Unit) {
    val js = "(function(){try{var i=document.createElement('iframe');document.head.append(i);var pd=Object.getOwnPropertyDescriptor(i.contentWindow,'localStorage');i.remove();Object.defineProperty(window,'localStorage',pd);return window.localStorage.getItem('token')||''}catch(e){return ''}})()"
    val close = BrowserLogin.openAndRead("https://discord.com/login", listOf("discord.com/channels", "discord.com/app"), js,
      { t -> SwingUtilities.invokeLater { onTokenExtracted(t) } }, { m -> problem = m })
    onDispose { close() }
  }
  Text(problem ?: "Your browser has opened. Sign in to Discord there; this connects on its own once you are in. (It uses a fresh browser profile, so you sign in again.)", Modifier.padding(24.dp))
}
