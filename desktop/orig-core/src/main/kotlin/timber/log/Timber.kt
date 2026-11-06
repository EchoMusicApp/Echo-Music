package timber.log

/** Desktop stand-in for Android's Timber logger (same call shapes the original files use). */
object Timber {
    @JvmStatic fun tag(tag: String): Timber = this
    @JvmStatic fun d(msg: String?, vararg args: Any?) = println("D ${format(msg, args)}")
    @JvmStatic fun i(msg: String?, vararg args: Any?) = println("I ${format(msg, args)}")
    @JvmStatic fun w(msg: String?, vararg args: Any?) = println("W ${format(msg, args)}")
    @JvmStatic fun e(msg: String?, vararg args: Any?) = System.err.println("E ${format(msg, args)}")
    @JvmStatic fun e(t: Throwable?, msg: String? = null, vararg args: Any?) = System.err.println("E ${format(msg, args)} $t")
    @JvmStatic fun w(t: Throwable?, msg: String? = null, vararg args: Any?) = println("W ${format(msg, args)} $t")
    @JvmStatic fun d(t: Throwable?, msg: String? = null, vararg args: Any?) = println("D ${format(msg, args)} $t")
    private fun format(m: String?, a: Array<out Any?>) = if (m == null) "" else if (a.isEmpty()) m else String.format(m, *a)
}
