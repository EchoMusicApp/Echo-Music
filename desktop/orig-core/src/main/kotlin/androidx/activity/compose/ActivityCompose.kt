package androidx.activity.compose

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// shortcut: desktop has no system back button, so BackHandler never fires; wire Escape/mouse-back here if screens need it.
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {}

@Composable
fun <I, O> rememberLauncherForActivityResult(contract: ActivityResultContract<I, O>, onResult: (O) -> Unit): ActivityResultLauncher<I> {
    val callback by rememberUpdatedState(onResult)
    return remember(contract) { ActivityResultLauncher(contract) { callback(it) } }
}
