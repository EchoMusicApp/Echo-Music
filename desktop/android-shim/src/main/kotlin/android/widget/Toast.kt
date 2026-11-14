package android.widget

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow

/** Messages from Toast.makeText(...).show(); the desktop window shows them in a small pill above the player. */
object ToastBus {
  val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
}

class Toast private constructor(private val text: CharSequence) {
  fun show() { println("[toast] $text"); ToastBus.messages.tryEmit(text.toString()) }
  companion object {
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1
    @JvmStatic fun makeText(context: Context, text: CharSequence, duration: Int) = Toast(text)
    @JvmStatic fun makeText(context: Context, resId: Int, duration: Int) = Toast(runCatching { context.getString(resId) }.getOrDefault("#$resId"))
  }
}
