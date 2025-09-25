package iad1tya.echo.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.os.Process
import iad1tya.echo.music.extension.isNetworkAvailable

@Composable
fun NetworkAwareContent(
    isOnline: Boolean,
    onRetry: () -> Unit,
    onSeeDownloads: () -> Unit,
    showNoInternetMessage: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var hasShownOfflineScreen by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }
    var initialCheckDone by remember { mutableStateOf(false) }

    // Check if this is the initial app launch without internet
    LaunchedEffect(Unit) {
        if (!initialCheckDone) {
            initialCheckDone = true
            if (!isOnline) {
                hasShownOfflineScreen = true
            }
        }
    }

    // Show offline screen if:
    // 1. User is offline AND
    // 2. Either initial launch without internet OR user clicked retry
    val shouldShowOfflineScreen = !isOnline && (hasShownOfflineScreen || isRetrying)

    if (shouldShowOfflineScreen) {
        OfflineScreen(
            onRetry = {
                isRetrying = true
                onRetry()
            },
            onSeeDownloads = onSeeDownloads,
            showNoInternetMessage = showNoInternetMessage
        )
    } else {
        // Don't automatically reset states - let user control via retry button
        content()
    }
}

@Composable
fun OfflineScreen(
    onRetry: () -> Unit,
    onSeeDownloads: () -> Unit,
    showNoInternetMessage: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title - changes based on retry state
            Text(
                text = if (showNoInternetMessage) {
                    "No internet connection"
                } else {
                    "Sorry, you might have lost your internet connection"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Retry button - Filled white
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // See Downloads button - Filled white
            Button(
                onClick = onSeeDownloads,
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "See Your Downloads",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun NetworkConnectivityManager(
    onRefresh: (() -> Unit)? = null,
    content: @Composable (Boolean, () -> Unit, () -> Unit, Boolean) -> Unit
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
    var hasShownOfflineScreen by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }
    var showNoInternetMessage by remember { mutableStateOf(false) }

    // Check network connectivity periodically
    LaunchedEffect(Unit) {
        while (true) {
            val currentNetworkStatus = isNetworkAvailable(context)
            
            // Don't automatically reset states when network comes back
            // Let the user decide when to retry by clicking the button
            isOnline = currentNetworkStatus
            kotlinx.coroutines.delay(2000) // Check every 2 seconds
        }
    }

    content(
        isOnline,
        { 
            // Retry button clicked
            isRetrying = true
            val currentNetworkStatus = isNetworkAvailable(context)
            
            if (currentNetworkStatus) {
                // If network is available, refresh the home page
                showNoInternetMessage = false
                hasShownOfflineScreen = false
                onRefresh?.invoke()
            } else {
                // If still offline, show no internet message
                showNoInternetMessage = true
            }
        },
        { /* This will be handled by the parent */ },
        showNoInternetMessage
    )
}

private fun restartApp(context: android.content.Context) {
    try {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Process.killProcess(Process.myPid())
    } catch (e: Exception) {
        // Fallback: just finish the current activity
        if (context is android.app.Activity) {
            context.finish()
        }
    }
}
