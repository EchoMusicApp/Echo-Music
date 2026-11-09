package android.app

import android.content.Context

// shortcut: a plain Context; there are no real activities on desktop (finish() and friends are no-ops).
open class Activity : Context() {
    fun finish() {}
    fun shouldShowRequestPermissionRationale(permission: String): Boolean = false
    var requestedOrientation: Int = 0
}
