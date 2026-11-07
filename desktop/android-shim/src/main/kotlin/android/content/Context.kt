package android.content

import java.io.File
import java.util.Properties

// Desktop stand-in for android.content.Context: only app-private storage (SharedPreferences, files dir).
open class Context(val filesDir: File = File(System.getProperty("user.home"), ".echomusic").also { it.mkdirs() }) {
    val applicationContext: Context get() = this
    val assets = android.content.res.AssetManager()
    val resources = android.content.res.Resources()
    val packageName: String = "echo.music.desktop"
    val packageManager = android.content.pm.PackageManager()
    private val prefs = HashMap<String, SharedPreferences>()
    @Synchronized
    fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        prefs.getOrPut(name) { SharedPreferences(File(filesDir, "$name.properties")) }

    // Texts come from the generated string table (RTable), same as stringResource().
    // String tables are generated in modules above this one (app table in orig-core, lyrics table in lyrics-jvm), so they are read by reflection.
    // shortcut: reflection on every call, fine for occasional strings; cache if it shows up in a hot path.
    fun getString(id: Int): String {
        for (cls in listOf("echo.music.iad1tya.RTable", "com.music.echo.lyrics.RTable")) {
            val table = runCatching { Class.forName(cls).getField("INSTANCE").get(null) }.getOrNull() ?: continue
            @Suppress("UNCHECKED_CAST")
            val m = table.javaClass.getMethod("getStrings").invoke(table) as Map<Int, String>
            m[id]?.let { return it }
        }
        return "string#$id"
    }
    fun getString(id: Int, vararg args: Any): String = String.format(getString(id), *args)

    companion object { const val MODE_PRIVATE = 0 }
}

// shortcut: plain properties file rewritten on every change; fine for a handful of login fields.
class SharedPreferences(private val file: File) {
    private val props = Properties().apply { if (file.exists()) file.inputStream().use { load(it) } }
    fun getString(key: String, default: String?): String? = props.getProperty(key) ?: default
    fun getBoolean(key: String, default: Boolean): Boolean = props.getProperty(key)?.toBooleanStrictOrNull() ?: default
    fun edit() = Editor()

    inner class Editor {
        private val changes = HashMap<String, String?>()
        private var clear = false
        fun putString(key: String, value: String?) = apply { changes[key] = value }
        fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value.toString() }
        fun clear() = apply { clear = true }
        fun remove(key: String) = apply { changes[key] = null }
        fun apply() = synchronized(props) {
            if (clear) props.clear()
            changes.forEach { (k, v) -> if (v == null) props.remove(k) else props.setProperty(k, v) }
            file.outputStream().use { props.store(it, null) }
        }
    }
}
