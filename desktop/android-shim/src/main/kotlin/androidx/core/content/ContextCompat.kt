package androidx.core.content

import android.content.Context

object ContextCompat {
    fun startActivity(context: Context, intent: android.content.Intent, options: Any?) = context.startActivity(intent)
    fun getSystemService(context: Context, cls: Class<*>): Any? = null
}

inline fun <reified T : Any> Context.getSystemService(): T? = getSystemService(T::class.java.simpleName.lowercase()) as? T
