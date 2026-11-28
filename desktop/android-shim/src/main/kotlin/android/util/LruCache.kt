package android.util

// Desktop stand-in for android.util.LruCache (size counted in entries).
open class LruCache<K : Any, V : Any>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > maxSize
    }
    @Synchronized operator fun get(key: K): V? = map[key]
    @Synchronized fun put(key: K, value: V): V? = map.put(key, value)
    @Synchronized fun remove(key: K): V? = map.remove(key)
    @Synchronized fun evictAll() = map.clear()
    @Synchronized fun size(): Int = map.size
}
