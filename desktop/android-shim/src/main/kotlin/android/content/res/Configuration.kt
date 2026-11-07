package android.content.res

class Configuration(var screenWidthDp: Int = 1280, var screenHeightDp: Int = 720, var orientation: Int = ORIENTATION_LANDSCAPE) {
    companion object {
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
    }
}
