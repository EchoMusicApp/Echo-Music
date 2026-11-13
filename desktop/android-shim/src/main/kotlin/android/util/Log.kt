package android.util

// Desktop stand-in for android.util.Log: prints to the console.
object Log {
    fun d(tag: String, msg: String): Int { println("D/$tag: $msg"); return 0 }
    fun i(tag: String, msg: String): Int { println("I/$tag: $msg"); return 0 }
    fun w(tag: String, msg: String): Int { println("W/$tag: $msg"); return 0 }
    fun e(tag: String, msg: String): Int { System.err.println("E/$tag: $msg"); return 0 }
    fun e(tag: String, msg: String, t: Throwable?): Int { System.err.println("E/$tag: $msg $t"); return 0 }
}
