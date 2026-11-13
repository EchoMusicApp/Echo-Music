package android.content

import android.net.Uri

// shortcut: an Intent here is only a bag of data; Context.startActivity opens links/mail in the system handler and ignores the rest.
open class Intent(var action: String? = null, var data: Uri? = null) {
    constructor(action: String) : this(action, null)
    constructor(context: Context, cls: Class<*>) : this(null, null)
    var type: String? = null
    var flags: Int = 0
    var clipData: ClipData? = null
    var `package`: String? = null
    var component: ComponentName? = null
    val extras = HashMap<String, Any?>()
    fun putExtra(name: String, value: Any?) = apply { extras[name] = value }
    fun getStringExtra(name: String): String? = extras[name] as? String
    fun getIntExtra(name: String, default: Int): Int = extras[name] as? Int ?: default
    fun getBooleanExtra(name: String, default: Boolean): Boolean = extras[name] as? Boolean ?: default
    fun getLongExtra(name: String, default: Long): Long = extras[name] as? Long ?: default
    fun addFlags(f: Int) = apply { flags = flags or f }
    fun setFlags(f: Int) = apply { flags = f }
    fun setPackage(p: String?) = apply { `package` = p }
    fun setType(t: String?) = apply { type = t }
    fun setData(u: Uri?) = apply { data = u }
    fun setAction(a: String?) = apply { action = a }

    companion object {
        fun makeRestartActivityTask(c: ComponentName?) = Intent()
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_SENDTO = "android.intent.action.SENDTO"
        const val ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_GRANT_READ_URI_PERMISSION = 1
        const val FLAG_GRANT_WRITE_URI_PERMISSION = 2
        const val ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH"
        fun createChooser(target: Intent, title: CharSequence?): Intent = target
    }
}
