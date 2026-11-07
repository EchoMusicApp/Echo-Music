package android.content.res

import java.io.InputStream

// Desktop stand-ins: assets and raw resources are read from the classpath (assets/ and raw/ folders copied by the build).
class AssetManager {
    fun open(name: String): InputStream =
        Thread.currentThread().contextClassLoader.getResourceAsStream("assets/$name") ?: error("asset missing: $name")
}

class Resources {
    // R.raw ids are generated into RTable (orig-core), read by reflection because that module sits above this one.
    fun openRawResource(id: Int): InputStream {
        val table = Class.forName("echo.music.iad1tya.RTable").getField("INSTANCE").get(null)
        @Suppress("UNCHECKED_CAST")
        val raws = table.javaClass.getMethod("getRaws").invoke(table) as Map<Int, String>
        val name = raws[id] ?: error("no raw resource with id $id")
        return Thread.currentThread().contextClassLoader.getResourceAsStream("raw/$name") ?: error("raw resource missing: $name")
    }
}
