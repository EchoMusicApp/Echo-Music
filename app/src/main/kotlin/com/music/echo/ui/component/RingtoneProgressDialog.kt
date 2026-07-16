package iad1tya.echo.music.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.R

@Composable
fun RingtoneProgressDialog(
    isVisible: Boolean,
    progress: Float,
    statusMessage: String,
    isComplete: Boolean,
    isSuccess: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = {
            if (isComplete) onDismiss()
        },
        title = {
            Text(
                if (isComplete) {
                    if (isSuccess) "Success!" else "Failed"
                } else {
                    "Setting Ringtone..."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isComplete && !isSuccess) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isComplete) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (isComplete) {
                Button(
                    onClick = {
                        if (isSuccess) onOpenSettings() else onDismiss()
                    },
                ) {
                    Text(if (isSuccess) "Open Settings" else stringResource(R.string.close))
                }
            }
        },
        dismissButton = {
            if (isComplete && isSuccess) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    )
}
