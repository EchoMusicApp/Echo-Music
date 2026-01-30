package iad1tya.echo.music.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * WiFiConnectivityObserver for monitoring WiFi status and available networks
 */
class WiFiConnectivityObserver(private val context: Context) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    private val _wifiStatus = Channel<Boolean>(Channel.CONFLATED)
    val wifiStatus = _wifiStatus.receiveAsFlow()
    
    private val _currentConnection = MutableStateFlow<WifiInfo?>(null)
    val currentConnection = _currentConnection.asStateFlow()
    
    private val _availableNetworks = MutableStateFlow<List<ScanResult>>(emptyList())
    val availableNetworks = _availableNetworks.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    val isEnabled = state == WifiManager.WIFI_STATE_ENABLED
                    _wifiStatus.trySend(isEnabled)
                    
                    if (isEnabled) {
                        updateCurrentConnection()
                    } else {
                        _currentConnection.value = null
                    }
                }
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    updateCurrentConnection()
                }
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    if (success) {
                        updateAvailableNetworks()
                    }
                    _isScanning.value = false
                }
            }
        }
    }
    
    init {
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        }
        
        context.registerReceiver(wifiReceiver, filter)
        
        // Send initial state
        val isInitiallyEnabled = isWifiEnabled()
        _wifiStatus.trySend(isInitiallyEnabled)
        
        // Initialize current connection and available networks
        if (isInitiallyEnabled) {
            updateCurrentConnection()
            updateAvailableNetworks()
        }
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(wifiReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
    
    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }
    
    /**
     * Start scanning for WiFi networks
     */
    fun startScan() {
        if (!isWifiEnabled()) {
            return
        }
        
        _isScanning.value = true
        wifiManager.startScan()
    }
    
    /**
     * Update the current WiFi connection information
     */
    private fun updateCurrentConnection() {
        if (!isWifiEnabled()) {
            _currentConnection.value = null
            return
        }
        
        val connectionInfo = wifiManager.connectionInfo
        _currentConnection.value = connectionInfo
    }
    
    /**
     * Update the list of available WiFi networks
     */
    private fun updateAvailableNetworks() {
        if (!isWifiEnabled()) {
            _availableNetworks.value = emptyList()
            return
        }
        
        val scanResults = wifiManager.scanResults ?: emptyList()
        _availableNetworks.value = scanResults
    }
}