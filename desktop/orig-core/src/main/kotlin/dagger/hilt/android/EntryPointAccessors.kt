package dagger.hilt.android

import echo.music.iad1tya.di.DesktopGraph

/** Desktop stand-in: entry-point interfaces are looked up in the hand-wired [DesktopGraph]. */
object EntryPointAccessors {
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> fromApplication(context: Any?, cls: Class<T>): T =
    (DesktopGraph.entryPoints[cls.kotlin] ?: error("No entry point registered for ${cls.simpleName}")) as T
}
