package androidx.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// shortcut: plain map, no saving across process death (desktop has none); navigation arguments get put in by the desktop nav layer.
class SavedStateHandle(initial: Map<String, Any?> = emptyMap()) {
    private val values = HashMap<String, Any?>(initial)
    private val flows = HashMap<String, MutableStateFlow<Any?>>()
    @Suppress("UNCHECKED_CAST") operator fun <T> get(key: String): T? = values[key] as T?
    operator fun <T> set(key: String, value: T?) { values[key] = value; flows[key]?.value = value }
    operator fun contains(key: String) = key in values
    fun keys(): Set<String> = values.keys
    fun <T> remove(key: String): T? { @Suppress("UNCHECKED_CAST") return values.remove(key) as T? }
    @Suppress("UNCHECKED_CAST")
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> =
        flows.getOrPut(key) { MutableStateFlow(values.getOrPut(key) { initialValue }) } as StateFlow<T>
}
