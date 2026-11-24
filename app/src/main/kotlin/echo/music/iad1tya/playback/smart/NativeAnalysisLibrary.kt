package echo.music.iad1tya.playback.smart

import timber.log.Timber

/** Loads the analyser's native library if available. */
object NativeAnalysisLibrary {

  private var loader: (() -> Unit)? = null

  fun install(loader: () -> Unit) {
    this.loader = loader
  }

  val available: Boolean by lazy {
    runCatching {
      val install = loader
      if (install != null) install() else System.loadLibrary(LIBRARY_NAME)
    }
      .onFailure {
        Timber.tag("NativeAnalysisLibrary").d("Analysis native library not loaded: %s", it.message)
      }
      .isSuccess
  }

  const val LIBRARY_NAME = "echo_analysis"
}
