package iad1tya.echo.music.dlna

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DLNAManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "DLNAManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val ssdpDiscovery = SSDPDiscovery()
    private val mediaServer = DLNAMediaServer()
    
    private val _devices = MutableStateFlow<List<DLNADevice>>(emptyList())
    val devices: StateFlow<List<DLNADevice>> = _devices.asStateFlow()
    
    private val _selectedDevice = MutableStateFlow<DLNADevice?>(null)
    val selectedDevice: StateFlow<DLNADevice?> = _selectedDevice.asStateFlow()
    
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private var currentPlayer: DLNAPlayer? = null
    
    fun start() {
        if (_isEnabled.value) return
        
        try {
            // Start media server
            mediaServer.start()
            Log.d(TAG, "DLNA media server started on port 8080")
            
            // Start device discovery
            startDiscovery()
            
            _isEnabled.value = true
            Log.d(TAG, "DLNA service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DLNA service", e)
        }
    }
    
    fun stop() {
        if (!_isEnabled.value) return
        
        try {
            ssdpDiscovery.stopDiscovery()
            mediaServer.stop()
            _devices.value = emptyList()
            _selectedDevice.value = null
            currentPlayer = null
            _isEnabled.value = false
            Log.d(TAG, "DLNA service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop DLNA service", e)
        }
    }
    
    fun startDiscovery() {
        ssdpDiscovery.startDiscovery(scope) { device ->
            val currentDevices = _devices.value.toMutableList()
            if (!currentDevices.any { it.id == device.id }) {
                currentDevices.add(device)
                _devices.value = currentDevices
                Log.d(TAG, "DLNA device found: ${device.name}")
            }
        }
    }
    
    fun stopDiscovery() {
        ssdpDiscovery.stopDiscovery()
    }
    
    fun selectDevice(device: DLNADevice?) {
        _selectedDevice.value = device
        currentPlayer = device?.let { DLNAPlayer(it) }
        Log.d(TAG, if (device != null) "Device selected: ${device.name}" else "Device deselected")
    }
    
    fun playMedia(mediaUrl: String, title: String = "", artist: String = ""): Boolean {
        val player = currentPlayer ?: run {
            Log.w(TAG, "No DLNA device selected")
            return false
        }
        
        return try {
            // Get proxy URL for the media
            val proxyUrl = mediaServer.getProxyUrl(mediaUrl)
            
            scope.launch {
                try {
                    val setUriSuccess = player.setAVTransportURI(proxyUrl, title, artist)
                    if (setUriSuccess) {
                        val playSuccess = player.play()
                        if (playSuccess) {
                            Log.d(TAG, "Successfully started playback on DLNA device")
                        } else {
                            Log.e(TAG, "Failed to start playback on DLNA device")
                        }
                    } else {
                        Log.e(TAG, "Failed to set media URI on DLNA device")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing media on DLNA device", e)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing media for DLNA playback", e)
            false
        }
    }
    
    fun pause() {
        val player = currentPlayer ?: return
        scope.launch {
            try {
                player.pause()
                Log.d(TAG, "DLNA playback paused")
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing DLNA playback", e)
            }
        }
    }
    
    fun resume() {
        val player = currentPlayer ?: return
        scope.launch {
            try {
                player.play()
                Log.d(TAG, "DLNA playback resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming DLNA playback", e)
            }
        }
    }
    
    fun stopPlayback() {
        val player = currentPlayer ?: return
        scope.launch {
            try {
                player.stop()
                Log.d(TAG, "DLNA playback stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping DLNA playback", e)
            }
        }
    }
    
    fun seek(positionMs: Long) {
        val player = currentPlayer ?: return
        
        // Convert milliseconds to HH:MM:SS format
        val hours = positionMs / 3600000
        val minutes = (positionMs % 3600000) / 60000
        val seconds = (positionMs % 60000) / 1000
        val target = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        
        scope.launch {
            try {
                player.seek(target)
                Log.d(TAG, "DLNA seek to $target")
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking DLNA playback", e)
            }
        }
    }
    
    fun setVolume(volume: Int) {
        val player = currentPlayer ?: return
        scope.launch {
            try {
                // Convert 0-1 float to 0-100 int
                val volumePercent = (volume * 100).coerceIn(0, 100)
                player.setVolume(volumePercent)
                Log.d(TAG, "DLNA volume set to $volumePercent")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting DLNA volume", e)
            }
        }
    }
}
