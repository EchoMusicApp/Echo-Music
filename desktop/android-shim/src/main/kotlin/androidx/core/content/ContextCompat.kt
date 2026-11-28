package androidx.core.content

import android.content.Context

object ContextCompat {
    @JvmStatic fun checkSelfPermission(context: Context, permission: String): Int = android.content.pm.PackageManager.PERMISSION_GRANTED
    fun startActivity(context: Context, intent: android.content.Intent, options: Any?) = context.startActivity(intent)
    fun getSystemService(context: Context, cls: Class<*>): Any? = null
}

inline fun <reified T : Any> Context.getSystemService(): T? = getSystemService(T::class.java.simpleName.lowercase()) as? T
