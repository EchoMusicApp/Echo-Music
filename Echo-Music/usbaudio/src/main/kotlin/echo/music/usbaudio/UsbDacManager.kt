package echo.music.usbaudio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import echo.music.usbaudio.model.DacCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class DacDeviceState {
    object Disconnected : DacDeviceState()
    data class PermissionRequired(val device: UsbDevice) : DacDeviceState()
    data class Connected(
        val device: UsbDevice,
        val connection: UsbDeviceConnection,
        val capabilities: DacCapabilities,
        val fileDescriptor: Int
    ) : DacDeviceState()
    data class Error(val message: String) : DacDeviceState()
}

/**
 * Manages USB host device lifecycle, permission negotiation, and dynamic attach/detach broadcasts.
 */
class UsbDacManager(
    private val context: Context,
    private val driver: UsbAudioDriver = UsbAudioDriver(),
    private val detector: UsbDeviceDetector = UsbDeviceDetector()
) {
    companion object {
        const val ACTION_USB_PERMISSION = "echo.music.usbaudio.USB_PERMISSION"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val _activeDacFlow = MutableStateFlow<DacDeviceState>(DacDeviceState.Disconnected)
    val activeDacFlow: StateFlow<DacDeviceState> = _activeDacFlow.asStateFlow()

    var onDeviceDetachedCallback: (() -> Unit)? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { handleDeviceAttached(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { handleDeviceDetached(it) }
                }
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (device != null && granted) {
                        connectDevice(device)
                    } else {
                        _activeDacFlow.value = DacDeviceState.Error("USB permission denied")
                    }
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        scanExistingDevices()
    }

    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
        disconnectCurrent()
    }

    fun scanExistingDevices() {
        val manager = usbManager ?: return
        for (device in manager.deviceList.values) {
            if (detector.isUsbAudioDevice(device)) {
                handleDeviceAttached(device)
                break
            }
        }
    }

    fun requestPermission(device: UsbDevice) {
        val manager = usbManager ?: return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            flags
        )
        manager.requestPermission(device, permissionIntent)
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        if (!detector.isUsbAudioDevice(device)) return
        val manager = usbManager ?: return
        if (manager.hasPermission(device)) {
            connectDevice(device)
        } else {
            _activeDacFlow.value = DacDeviceState.PermissionRequired(device)
            requestPermission(device)
        }
    }

    private fun handleDeviceDetached(device: UsbDevice) {
        val current = _activeDacFlow.value
        if (current is DacDeviceState.Connected && current.device.deviceId == device.deviceId) {
            disconnectCurrent()
            onDeviceDetachedCallback?.invoke()
        }
    }

    private fun connectDevice(device: UsbDevice) {
        val current = _activeDacFlow.value
        if (current is DacDeviceState.Connected) {
            if (current.device.deviceId == device.deviceId) {
                return
            }
            disconnectCurrent()
        }
        val manager = usbManager ?: return
        val connection = manager.openDevice(device)
        if (connection == null) {
            _activeDacFlow.value = DacDeviceState.Error("Failed to open UsbDeviceConnection")
            return
        }

        val rawDescriptors = connection.rawDescriptors ?: ByteArray(0)
        val caps = driver.parseDescriptors(rawDescriptors)
        val fd = connection.fileDescriptor

        _activeDacFlow.value = DacDeviceState.Connected(
            device = device,
            connection = connection,
            capabilities = caps,
            fileDescriptor = fd
        )
    }

    private fun disconnectCurrent() {
        val current = _activeDacFlow.value
        if (current is DacDeviceState.Connected) {
            try {
                driver.stopStream()
            } catch (_: Exception) {}
            try {
                current.connection.close()
            } catch (_: Exception) {}
        }
        _activeDacFlow.value = DacDeviceState.Disconnected
    }
}
