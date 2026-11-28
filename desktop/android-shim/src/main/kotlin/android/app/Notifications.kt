package android.app

import android.content.Context
import android.content.Intent

// shortcut: desktop has no notification shade; these types accept the calls and show nothing. Wire to a tray popup if wanted.
class Notification
class NotificationChannel(val id: String, val name: CharSequence, val importance: Int) { var description: String? = null }
class NotificationManager {
    private val channels = HashMap<String, NotificationChannel>()
    fun getNotificationChannel(id: String): NotificationChannel? = channels[id]
    fun createNotificationChannel(channel: NotificationChannel) { channels[channel.id] = channel }
    companion object { const val IMPORTANCE_HIGH = 4; const val IMPORTANCE_DEFAULT = 3; const val IMPORTANCE_LOW = 2 }
}

class PendingIntent private constructor() {
    companion object {
        const val FLAG_UPDATE_CURRENT = 0x08000000
        const val FLAG_IMMUTABLE = 0x04000000
        @JvmStatic fun getBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int) = PendingIntent()
        @JvmStatic fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int) = PendingIntent()
    }
}
