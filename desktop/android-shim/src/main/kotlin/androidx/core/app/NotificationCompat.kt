package androidx.core.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Context

object NotificationCompat {
    const val PRIORITY_HIGH = 1
    const val PRIORITY_DEFAULT = 0
    const val PRIORITY_LOW = -1

    class Builder(context: Context, channelId: String) {
        fun setSmallIcon(icon: Int) = this
        fun setContentTitle(t: CharSequence?) = this
        fun setContentText(t: CharSequence?) = this
        fun setPriority(p: Int) = this
        fun setAutoCancel(b: Boolean) = this
        fun setContentIntent(i: PendingIntent?) = this
        fun addAction(icon: Int, title: CharSequence?, intent: PendingIntent?) = this
        fun build() = Notification()
    }
}

class NotificationManagerCompat private constructor() {
    fun notify(id: Int, notification: Notification) {}
    fun cancel(id: Int) {}
    companion object { @JvmStatic fun from(context: Context) = NotificationManagerCompat() }
}

object ActivityCompat {
    // desktop has no runtime permissions: everything is granted
    @JvmStatic fun checkSelfPermission(context: Context, permission: String): Int = android.content.pm.PackageManager.PERMISSION_GRANTED
}
