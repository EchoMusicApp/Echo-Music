package android.os

// Desktop stand-in for android.os.Bundle: a map with typed getters.
class Bundle {
    private val map = HashMap<String, Any?>()
    fun putString(key: String, value: String?) { map[key] = value }
    fun putInt(key: String, value: Int) { map[key] = value }
    fun putLong(key: String, value: Long) { map[key] = value }
    fun putBoolean(key: String, value: Boolean) { map[key] = value }
    fun putFloat(key: String, value: Float) { map[key] = value }
    fun putDouble(key: String, value: Double) { map[key] = value }
    fun putAll(other: Bundle) { map.putAll(other.map) }
    fun containsKey(key: String) = map.containsKey(key)
    fun keySet(): Set<String> = map.keys
    fun isEmpty() = map.isEmpty()
    fun getString(key: String): String? = map[key] as? String
    fun getString(key: String, default: String): String = map[key] as? String ?: default
    fun getInt(key: String, default: Int = 0): Int = map[key] as? Int ?: default
    fun getLong(key: String, default: Long = 0L): Long = map[key] as? Long ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = map[key] as? Boolean ?: default
    fun getFloat(key: String, default: Float = 0f): Float = map[key] as? Float ?: default
    fun getDouble(key: String, default: Double = 0.0): Double = map[key] as? Double ?: default
    override fun toString() = map.toString()
}
