package android.content

class IntentFilter(action: String? = null) {
    val actions = mutableListOf<String>().also { if (action != null) it += action }
    fun addAction(a: String) { actions += a }
}
