package echo.music.iad1tya.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Desktop stand-in: always reports "connected"; requests simply fail on their own when offline.
// shortcut: no real connectivity watching, add a java.net check if offline handling matters.
class NetworkConnectivityObserver(context: Context) {
    val networkStatus: StateFlow<Boolean> = MutableStateFlow(true)
    fun unregister() {}
    fun isCurrentlyConnected(): Boolean = true
}
