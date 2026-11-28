package echo.music.iad1tya.echomusic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// shortcut: no output-device picker on desktop yet (it follows the Windows default device).
@Composable
fun AudioDeviceBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) { onDismiss() }
