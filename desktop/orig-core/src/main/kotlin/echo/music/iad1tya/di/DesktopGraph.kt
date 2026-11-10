package echo.music.iad1tya.di

import androidx.lifecycle.ViewModel
import echo.music.iad1tya.lyrics.LyricsHelper
import kotlin.reflect.KClass

/**
 * Hand-wired replacement for Hilt: the desktop entry point fills this in at startup.
 * hiltViewModel<T>() asks [viewModels] for a factory; singletons that Hilt entry points used to hand out live here.
 */
object DesktopGraph {
  lateinit var lyricsHelper: LyricsHelper
  /** Hilt entry-point interfaces (e.g. CloudEntryPoint) -> the object the desktop entry point built for them. */
  val entryPoints = HashMap<KClass<*>, Any>()
  val viewModels = HashMap<KClass<out ViewModel>, () -> ViewModel>()

  private val instances = HashMap<Class<*>, Any>()
  fun provide(cls: Class<*>, value: Any) { instances[cls] = value }

  /** The singleton for [cls]: a provided one, else built by reflection from its biggest public constructor (what Hilt's @Inject did). */
  @Synchronized
  @Suppress("UNCHECKED_CAST")
  fun <T> resolve(cls: Class<T>): T {
    instances[cls]?.let { return it as T }
    val made = construct(cls)
    instances[cls] = made as Any
    return made
  }

  private fun <T> construct(cls: Class<T>): T {
    if (cls == androidx.lifecycle.SavedStateHandle::class.java) return androidx.lifecycle.SavedStateHandle() as T
    val ctor = cls.constructors.filter { !it.isSynthetic }.maxByOrNull { it.parameterCount }
      ?: error("Cannot build ${cls.simpleName}: no public constructor and not provided")
    try { return ctor.newInstance(*ctor.parameterTypes.map { resolve(it) }.toTypedArray()) as T }
    catch (e: java.lang.reflect.InvocationTargetException) { throw e.targetException ?: e }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : ViewModel> create(cls: KClass<T>, handle: androidx.lifecycle.SavedStateHandle? = null): T {
    viewModels[cls]?.let { return it.invoke() as T }
    // view models are not singletons: build a fresh one, but its dependencies are shared
    val java = cls.java
    val ctor = java.constructors.filter { !it.isSynthetic }.maxByOrNull { it.parameterCount } ?: error("No constructor for ${cls.simpleName}")
    try {
      return ctor.newInstance(*ctor.parameterTypes.map { if (it == androidx.lifecycle.SavedStateHandle::class.java) (handle ?: androidx.lifecycle.SavedStateHandle()) else resolve(it) }.toTypedArray()) as T
    } catch (e: java.lang.reflect.InvocationTargetException) {
      throw e.targetException ?: e // show the real failure, not the reflection wrapper
    }
  }
}
