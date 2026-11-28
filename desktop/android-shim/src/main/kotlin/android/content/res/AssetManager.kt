package android.content.res

import java.io.InputStream

// Desktop stand-ins: assets and raw resources are read from the classpath (assets/ and raw/ folders copied by the build).
class AssetManager {
    fun open(name: String): InputStream =
        Thread.currentThread().contextClassLoader.getResourceAsStream("assets/$name") ?: error("asset missing: $name")
}

class DisplayMetrics { var density = 1f; var densityDpi = 160; var widthPixels = 1280; var heightPixels = 720; var scaledDensity = 1f }

class Resources {
    var configuration = Configuration()
        private set
    val displayMetrics = DisplayMetrics()
    // shortcut: only remembers the locale (and sets the JVM default); strings are not re-read per locale yet.
    fun updateConfiguration(config: Configuration, metrics: DisplayMetrics?) { configuration = config; java.util.Locale.setDefault(config.locale) }
    fun getQuantityString(id: Int, quantity: Int, vararg args: Any): String {
        val table = Class.forName("echo.music.iad1tya.RTable").getField("INSTANCE").get(null)
        @Suppress("UNCHECKED_CAST")
        val p = (table.javaClass.getMethod("getPlurals").invoke(table) as Map<Int, Map<String, String>>)[id] ?: return "plural#$id"
        return String.format(p[if (quantity == 1) "one" else "other"] ?: p["other"] ?: "", *args)
    }
    fun getQuantityString(id: Int, quantity: Int): String = getQuantityString(id, quantity, quantity)
    // R.raw ids are generated into RTable (orig-core), read by reflection because that module sits above this one.
    fun openRawResource(id: Int): InputStream {
        val table = Class.forName("echo.music.iad1tya.RTable").getField("INSTANCE").get(null)
        @Suppress("UNCHECKED_CAST")
        val raws = table.javaClass.getMethod("getRaws").invoke(table) as Map<Int, String>
        val name = raws[id] ?: error("no raw resource with id $id")
        return Thread.currentThread().contextClassLoader.getResourceAsStream("raw/$name") ?: error("raw resource missing: $name")
    }
}
