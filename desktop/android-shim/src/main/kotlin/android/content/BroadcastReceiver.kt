package android.content

abstract class BroadcastReceiver {
    abstract fun onReceive(context: Context, intent: Intent)
}
