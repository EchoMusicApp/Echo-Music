package echo.music.iad1tya.echomusic

import android.content.Context

// shortcut: no Bluetooth detection on desktop; the name helpers are copied unchanged.
fun isBluetoothHeadphoneConnected(context: Context): Boolean = false
fun getConnectedBluetoothDeviceName(context: Context): String? = null

fun isBuds(name: String?): Boolean {
  val n = name?.lowercase() ?: return false
  return listOf("buds", "airpods", "earpods", "earphone", "freebuds", "pods").any { it in n }
}

fun isSpeaker(name: String?): Boolean {
  val n = name?.lowercase() ?: return false
  return listOf("speaker", "soundbar", "homepod", "echo", "boombox", "audio system", "sound", "audio", "stereo", "music", "box", "party", "waves").any { it in n }
}
