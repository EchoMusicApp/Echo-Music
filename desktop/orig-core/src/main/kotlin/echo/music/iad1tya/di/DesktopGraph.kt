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

  @Suppress("UNCHECKED_CAST")
  fun <T : ViewModel> create(cls: KClass<T>): T =
    (viewModels[cls] ?: error("No factory registered for ${cls.simpleName}; add it in the desktop entry point")).invoke() as T
}
