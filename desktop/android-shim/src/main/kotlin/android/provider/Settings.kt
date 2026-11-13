package android.provider

object Settings {
    const val ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ACTION_MANAGE_WRITE_SETTINGS = "android.settings.action.MANAGE_WRITE_SETTINGS"
    const val ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
    const val ACTION_BLUETOOTH_SETTINGS = "android.settings.BLUETOOTH_SETTINGS"
    const val ACTION_LOCATION_SOURCE_SETTINGS = "android.settings.LOCATION_SOURCE_SETTINGS"
    const val ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION = "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"
    const val ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    const val EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE"
    const val ACTION_APP_LOCALE_SETTINGS = "android.settings.APP_LOCALE_SETTINGS"
    const val ACTION_APP_OPEN_BY_DEFAULT_SETTINGS = "android.settings.APP_OPEN_BY_DEFAULT_SETTINGS"
    object Global { const val ANIMATOR_DURATION_SCALE = "animator_duration_scale"; fun getFloat(cr: Any?, name: String, def: Float) = def }
    object System { fun canWrite(c: android.content.Context) = true }
    object Secure
}
object DocumentsContract {
    fun isTreeUri(uri: android.net.Uri): Boolean = uri.path?.contains("/tree/") == true
    fun getTreeDocumentId(uri: android.net.Uri): String = uri.toString()
    fun buildDocumentUriUsingTree(tree: android.net.Uri, id: String): android.net.Uri = tree
}
