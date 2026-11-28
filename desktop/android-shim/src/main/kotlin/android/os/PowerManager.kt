package android.os

class PowerManager {
    class WakeLock {
        private var held = false
        val isHeld get() = held
        fun acquire() { held = true }
        fun acquire(timeoutMs: Long) { held = true }
        fun release() { held = false }
    }
    fun newWakeLock(levelAndFlags: Int, tag: String) = WakeLock()
    companion object { const val PARTIAL_WAKE_LOCK = 1 }
}
