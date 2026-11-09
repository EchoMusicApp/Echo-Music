package android.os

class Looper { companion object { private val main = Looper(); fun getMainLooper() = main } }
class Handler(val looper: Looper = Looper.getMainLooper()) {
    fun post(r: Runnable): Boolean { r.run(); return true }
    fun postDelayed(r: Runnable, delayMs: Long): Boolean { Thread { Thread.sleep(delayMs); r.run() }.start(); return true }
}
