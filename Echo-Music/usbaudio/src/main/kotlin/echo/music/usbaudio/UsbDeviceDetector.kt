package echo.music.usbaudio

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice

/**
 * Identifies USB audio devices, checks interfaces, and filters out non-audio peripherals.
 */
class UsbDeviceDetector {

    /**
     * Checks if a device has an audio interface (`USB_CLASS_AUDIO` = 1).
     */
    fun isAudioClassDevice(hasAudioInterface: Boolean): Boolean {
        return hasAudioInterface
    }

    /**
     * Inspects a connected [UsbDevice] to determine if it exposes an Audio Class interface.
     */
    fun isUsbAudioDevice(device: UsbDevice): Boolean {
        // Interface scanning: require USB_CLASS_AUDIO, AudioStreaming subclass (2), and at least one OUT endpoint
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO &&
                iface.interfaceSubclass == 2 /* AudioStreaming */
            ) {
                for (e in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(e)
                    if (ep.direction == UsbConstants.USB_DIR_OUT) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
