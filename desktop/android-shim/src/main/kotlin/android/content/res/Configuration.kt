package android.content.res

class Configuration(var screenWidthDp: Int = 1280, var screenHeightDp: Int = 720, var orientation: Int = ORIENTATION_LANDSCAPE) {
    constructor(other: Configuration) : this(other.screenWidthDp, other.screenHeightDp, other.orientation)
    var locale: java.util.Locale = java.util.Locale.getDefault()
    fun setLocale(l: java.util.Locale) { locale = l }
    companion object {
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
    }
}
