package androidx.core.view

import android.view.View
import android.view.Window

class WindowInsetsControllerCompat(window: Window, view: View) {
    var isAppearanceLightStatusBars = false
    var isAppearanceLightNavigationBars = false
    var systemBarsBehavior = 0
    fun hide(types: Int) {}
    fun show(types: Int) {}
    companion object { const val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2 }
}
object WindowCompat {
    fun getInsetsController(window: Window, view: View) = WindowInsetsControllerCompat(window, view)
    fun setDecorFitsSystemWindows(window: Window, fits: Boolean) {}
}
class WindowInsetsCompat {
    object Type { fun systemBars() = 1; fun statusBars() = 2; fun navigationBars() = 4 }
}
