package androidx.core.content

import android.content.SharedPreferences

inline fun SharedPreferences.edit(commit: Boolean = false, action: SharedPreferences.Editor.() -> Unit) {
    val e = edit(); action(e); e.apply()
}
