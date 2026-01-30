package iad1tya.echo.music.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * BluetoothConnectivityObserver for monitoring Bluetooth status and connected devices
 */
class BluetoothConnectivityObserver(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private val _bluetoothStatus = Channel<Boolean>(Channel.CONFLATED)
    val bluetoothStatus = _bluetoothStatus.receiveAsFlow()
    
    private val _connectedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isEnabled = state == BluetoothAdapter.STATE_ON
                    _bluetoothStatus.trySend(isEnabled)
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            updateConnectedDevices()
                        }
                    } else {
                        updateConnectedDevices()
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            updateConnectedDevices()
                        }
                    } else {
                        updateConnectedDevices()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
                BluetoothDevice.ACTION_FOUND -> {
                    // We don't need to do anything here as we're only tracking connected devices
                }
            }
        }
    }
    
    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_FOUND)
        }
        
        context.registerReceiver(bluetoothReceiver, filter)
        
        // Send initial state
        val isInitiallyEnabled = isBluetoothEnabled()
        _bluetoothStatus.trySend(isInitiallyEnabled)
        
        // Initialize connected devices list
        if (isInitiallyEnabled) {
            updateConnectedDevices()
        }
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        
        if (isScanning.value) {
            stopScan()
        }
    }
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Start scanning for Bluetooth devices
     */
    fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        
        bluetoothAdapter?.startDiscovery()
    }
    
    /**
     * Stop scanning for Bluetooth devices
     */
    fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        bluetoothAdapter?.cancelDiscovery()
    }
    
    /**
     * Update the list of connected Bluetooth devices
     */
    private fun updateConnectedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val connectedDevices = bluetoothAdapter?.bondedDevices?.filter { device ->
            // For audio devices, we're interested in A2DP profiles
            // This is a simplified check - in a real app, you might want to check the actual connection state
            val deviceClass = device.bluetoothClass?.majorDeviceClass
            // Check if it's an audio device (major class 4)
            deviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
        } ?: emptyList()
        
        _connectedDevices.value = connectedDevices.toList()
    }
}