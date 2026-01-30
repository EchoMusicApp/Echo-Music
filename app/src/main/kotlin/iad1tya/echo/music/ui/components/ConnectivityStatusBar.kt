package iad1tya.echo.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import iad1tya.echo.music.R
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.utils.BluetoothConnectivityObserver
import iad1tya.echo.music.utils.WiFiConnectivityObserver
import iad1tya.echo.music.viewmodels.ConnectivityViewModel

/**
 * A component that displays the current Bluetooth and WiFi connectivity status
 */
@Composable
fun ConnectivityStatusBar(
    viewModel: ConnectivityViewModel,
    modifier: Modifier = Modifier,
    showWhenConnected: Boolean = false
) {
    val isBluetoothEnabled by viewModel.bluetoothStatus.collectAsState()
    val isWifiEnabled by viewModel.wifiStatus.collectAsState()
    val connectedDevices by viewModel.connectedBluetoothDevices.collectAsState()
    val currentWifiConnection by viewModel.currentWifiConnection.collectAsState()
    
    val showBluetoothStatus = !isBluetoothEnabled || (showWhenConnected && connectedDevices.isNotEmpty())
    val showWifiStatus = !isWifiEnabled || (showWhenConnected && currentWifiConnection != null)
    
    AnimatedVisibility(
        visible = showBluetoothStatus || showWifiStatus,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Bluetooth status
            if (showBluetoothStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = if (isBluetoothEnabled) R.drawable.settings_outlined else R.drawable.error),
                        contentDescription = "Bluetooth status",
                        tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isBluetoothEnabled && connectedDevices.isNotEmpty()) {
                        val device = connectedDevices.first()
                        Text(
                            text = "Connected to ${device.name ?: "Unknown Device"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Bluetooth is ${if (isBluetoothEnabled) "enabled" else "disabled"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // WiFi status
            if (showWifiStatus) {
                if (showBluetoothStatus) {
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = if (isWifiEnabled) R.drawable.wifi_proxy else R.drawable.error),
                        contentDescription = "WiFi status",
                        tint = if (isWifiEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isWifiEnabled && currentWifiConnection != null) {
                        Text(
                            text = "Connected to ${currentWifiConnection?.ssid?.trim('"') ?: "Unknown Network"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "WiFi is ${if (isWifiEnabled) "enabled" else "disabled"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}