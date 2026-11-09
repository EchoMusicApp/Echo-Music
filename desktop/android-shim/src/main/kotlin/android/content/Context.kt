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

    // shortcut: only the clipboard service exists; other service names return null.
    fun startService(intent: Intent?): ComponentName? = null
    fun checkSelfPermission(permission: String): Int = 0
    fun registerReceiver(r: BroadcastReceiver?, f: IntentFilter?): Intent? = null
    fun unregisterReceiver(r: BroadcastReceiver?) {}
    fun getSystemService(name: String): Any? = when (name) {
        AUDIO_SERVICE -> android.media.AudioManager()
        CLIPBOARD_SERVICE -> ClipboardManager()
        CONNECTIVITY_SERVICE -> android.net.ConnectivityManager()
        POWER_SERVICE -> android.os.PowerManager()
        NOTIFICATION_SERVICE -> android.app.NotificationManager()
        else -> null
    }
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getSystemService(): T? = when (T::class) {
        ClipboardManager::class -> getSystemService(CLIPBOARD_SERVICE)
        android.net.ConnectivityManager::class -> getSystemService(CONNECTIVITY_SERVICE)
        android.os.PowerManager::class -> getSystemService(POWER_SERVICE)
        android.app.NotificationManager::class -> getSystemService(NOTIFICATION_SERVICE)
        else -> null
    } as T?
    val contentResolver = ContentResolver()
    val cacheDir: File get() = File(filesDir.parentFile ?: filesDir, "cache").also { it.mkdirs() }
    fun stopService(intent: Intent): Boolean = false
    fun <T> getSystemService(cls: Class<T>): T? = @Suppress("UNCHECKED_CAST") (getSystemService(cls.simpleName.lowercase().replace("manager", "").let { if (it == "notification") NOTIFICATION_SERVICE else it }) as T?)
    fun getExternalFilesDir(type: String?): File = File(filesDir, type ?: "files").also { it.mkdirs() }
    // shortcut: opens http(s)/mailto links in the default handler; any other intent is ignored.
    fun startActivity(intent: Intent?) {
        val u = intent?.data ?: return
        runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(u.toString())) }
    }

    companion object {
        const val MODE_PRIVATE = 0
        const val CLIPBOARD_SERVICE = "clipboard"
        const val AUDIO_SERVICE = "audio"
        const val CONNECTIVITY_SERVICE = "connectivity"
        const val POWER_SERVICE = "power"
        const val NOTIFICATION_SERVICE = "notification"
    }
}

// shortcut: plain properties file rewritten on every change; fine for a handful of login fields.
class SharedPreferences(private val file: File) {
    private val props = Properties().apply { if (file.exists()) file.inputStream().use { load(it) } }
    fun getString(key: String, default: String?): String? = props.getProperty(key) ?: default
    fun getBoolean(key: String, default: Boolean): Boolean = props.getProperty(key)?.toBooleanStrictOrNull() ?: default
    fun getInt(key: String, default: Int): Int = props.getProperty(key)?.toIntOrNull() ?: default
    fun getLong(key: String, default: Long): Long = props.getProperty(key)?.toLongOrNull() ?: default
    fun getFloat(key: String, default: Float): Float = props.getProperty(key)?.toFloatOrNull() ?: default
    fun contains(key: String): Boolean = props.containsKey(key)
    fun edit() = Editor()

    inner class Editor {
        private val changes = HashMap<String, String?>()
        private var clear = false
        fun putString(key: String, value: String?) = apply { changes[key] = value }
        fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value.toString() }
        fun putInt(key: String, value: Int) = apply { changes[key] = value.toString() }
        fun putLong(key: String, value: Long) = apply { changes[key] = value.toString() }
        fun putFloat(key: String, value: Float) = apply { changes[key] = value.toString() }
        fun clear() = apply { clear = true }
        fun remove(key: String) = apply { changes[key] = null }
        fun apply() = synchronized(props) {
            if (clear) props.clear()
            changes.forEach { (k, v) -> if (v == null) props.remove(k) else props.setProperty(k, v) }
            file.outputStream().use { props.store(it, null) }
        }
    }
}
