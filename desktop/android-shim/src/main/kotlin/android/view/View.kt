package android.view

// shortcut: windows/system bars do not exist on desktop; these classes accept the calls and do nothing.
open class View { val context: android.content.Context get() = android.content.Context() }
class Window {
    val decorView = View()
    fun addFlags(f: Int) {}
    fun clearFlags(f: Int) {}
    fun setFlags(f: Int, mask: Int) {}
}
class WindowManager {
    class LayoutParams {
        companion object {
            const val FLAG_KEEP_SCREEN_ON = 128
            const val FLAG_FULLSCREEN = 1024
            const val FLAG_SECURE = 8192
        }
    }
}
