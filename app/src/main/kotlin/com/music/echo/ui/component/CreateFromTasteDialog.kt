package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import echo.music.iad1tya.viewmodels.CreateAiPlaylistViewModel

@Composable
fun CreateFromTasteDialog(
    onDismiss: () -> Unit,
    onPlaylistCreated: (String) -> Unit,
    viewModel: CreateAiPlaylistViewModel = hiltViewModel()
) {
    val generationLog by viewModel.generationLog.collectAsState()
    val errorLog by viewModel.errorLog.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generateFromTaste(onPlaylistCreated)
    }

    Dialog(
        onDismissRequest = {
            if (!isGenerating) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorLog != null) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(text = errorLog ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = generationLog ?: "Analyzing taste...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
