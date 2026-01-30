package iad1tya.echo.music.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iad1tya.echo.music.utils.BluetoothConnectivityObserver
import iad1tya.echo.music.utils.WiFiConnectivityObserver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel that manages Bluetooth and WiFi connectivity observers
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val bluetoothObserver = BluetoothConnectivityObserver(application)
    private val wifiObserver = WiFiConnectivityObserver(application)
    
    // Expose Bluetooth status
    val bluetoothStatus = bluetoothObserver.bluetoothStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = bluetoothObserver.isBluetoothEnabled()
        )
    
    val connectedBluetoothDevices = bluetoothObserver.connectedDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val isBluetoothScanning = bluetoothObserver.isScanning
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Expose WiFi status
    val wifiStatus = wifiObserver.wifiStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = wifiObserver.isWifiEnabled()
        )
    
    val currentWifiConnection = wifiObserver.currentConnection
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val availableWifiNetworks = wifiObserver.availableNetworks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val isWifiScanning = wifiObserver.isScanning
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Start scanning for Bluetooth devices
     */
    fun startBluetoothScan() {
        bluetoothObserver.startScan()
    }
    
    /**
     * Stop scanning for Bluetooth devices
     */
    fun stopBluetoothScan() {
        bluetoothObserver.stopScan()
    }
    
    /**
     * Start scanning for WiFi networks
     */
    fun startWifiScan() {
        wifiObserver.startScan()
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothObserver.unregister()
        wifiObserver.unregister()
    }
}