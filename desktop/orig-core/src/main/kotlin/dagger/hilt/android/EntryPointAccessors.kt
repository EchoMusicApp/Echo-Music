package dagger.hilt.android

import echo.music.iad1tya.di.DesktopGraph

/**
 * Desktop stand-in: entry-point interfaces are looked up in the hand-wired [DesktopGraph].
 * One that nobody registered gets a proxy whose methods return the graph's singleton of the method's return type (what Hilt generated).
 */
object EntryPointAccessors {
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> fromApplication(context: Any?, cls: Class<T>): T =
    (DesktopGraph.entryPoints[cls.kotlin] ?: java.lang.reflect.Proxy.newProxyInstance(cls.classLoader, arrayOf(cls)) { _, m, _ ->
      DesktopGraph.resolve(m.returnType)
    }.also { DesktopGraph.entryPoints[cls.kotlin] = it }) as T
}
