package android.widget

import android.content.Context

// shortcut: desktop has no toast yet; messages go to stdout. Replace with a Compose snackbar host when the main window exists.
class Toast private constructor(private val text: CharSequence) {
  fun show() { println("[toast] $text") }
  companion object {
    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1
    @JvmStatic fun makeText(context: Context, text: CharSequence, duration: Int) = Toast(text)
    @JvmStatic fun makeText(context: Context, resId: Int, duration: Int) = Toast(runCatching { context.getString(resId) }.getOrDefault("#$resId"))
  }
}
