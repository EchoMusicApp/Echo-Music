package android.os

object SystemClock {
    fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
}
