package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun SupportProjectDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Support the Project",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "If you enjoy EchoMusic, please consider supporting its development!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = { uriHandler.openUri("https://buymeacoffee.com/iad1tya") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    )
                ) {
                    Text("Buy me a coffee", color = MaterialTheme.colorScheme.background)
                }
                
                Button(
                    onClick = { uriHandler.openUri("https://support.iad1tya.cyou/") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    )
                ) {
                    Text("Support with UPI/Crypto", color = MaterialTheme.colorScheme.background)
                }

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://github.com/EchoMusicApp/Echo-Music") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Star on GitHub")
                }

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://instagram.com/iad1tya") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Follow on Instagram")
                }

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://x.com/xad1tya") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Follow on X")
                }

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://github.com/iad1tya") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Follow on GitHub")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Don't show again")
            }
        }
    )
}
