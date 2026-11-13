package android.app

import android.content.Context

// shortcut: a plain Context; there are no real activities on desktop (finish() and friends are no-ops).
open class Activity : Context() {
    val window = android.view.Window()
    fun finish() {}
    companion object { const val RESULT_OK = -1; const val RESULT_CANCELED = 0 }
    fun shouldShowRequestPermissionRationale(permission: String): Boolean = false
    var requestedOrientation: Int = 0
}
