package android.media

import android.os.Handler

// shortcut: system volume is not wired to Windows; reports a fixed 0..100 range at full volume.
class AudioManager {
    fun getStreamVolume(stream: Int): Int = 100
    fun getStreamMaxVolume(stream: Int): Int = 100
    fun setStreamVolume(stream: Int, index: Int, flags: Int) {}
    fun registerAudioDeviceCallback(cb: AudioDeviceCallback, handler: Handler?) {}
    fun unregisterAudioDeviceCallback(cb: AudioDeviceCallback) {}
    fun getDevices(flags: Int): Array<AudioDeviceInfo> = emptyArray()
    fun adjustStreamVolume(stream: Int, direction: Int, flags: Int) {}
    companion object {
        const val ADJUST_RAISE = 1
        const val ADJUST_LOWER = -1
        const val STREAM_MUSIC = 3
        const val ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG"
        const val GET_DEVICES_OUTPUTS = 2
        const val FLAG_SHOW_UI = 1
    }
}
abstract class AudioDeviceCallback {
    open fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {}
    open fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {}
}
class AudioDeviceInfo { val type: Int = 0; val productName: CharSequence = "" }
