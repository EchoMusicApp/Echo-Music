package androidx.lifecycle

/** Desktop: the process is always "resumed" while its window exists. */
object ProcessLifecycleOwner : LifecycleOwner {
  override val lifecycle = LifecycleRegistry(this).also { it.currentState = Lifecycle.State.RESUMED }
  fun get(): LifecycleOwner = this
}
