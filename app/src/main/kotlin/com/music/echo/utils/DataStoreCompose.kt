package echo.music.iad1tya.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import echo.music.iad1tya.extensions.toEnum
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state =
        remember {
            context.dataStore.data
                .map { prefs -> 
                    val value = try { prefs[key] } catch(e: Exception) { null }
                    if (value != null && defaultValue != null && (defaultValue !is Set<*>) && value::class != defaultValue::class) {
                        defaultValue
                    } else {
                        (value ?: defaultValue) as T
                    }
                }
                .distinctUntilChanged()
        }.collectAsState(
            run {
                val value = try { context.dataStore.get(key) } catch(e: Exception) { null }
                if (value != null && defaultValue != null && (defaultValue !is Set<*>) && value::class != defaultValue::class) {
                    defaultValue
                } else {
                    (value ?: defaultValue) as T
                }
            }
        )

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val initialValue = run {
        val value = try { context.dataStore.get(key) } catch (e: Exception) { null }
        (if (value != null && value !is String) null else value as String?).toEnum(defaultValue = defaultValue)
    }
    
    val state =
        remember {
            context.dataStore.data
                .map { prefs ->
                    val value = try { prefs[key] } catch (e: Exception) { null }
                    (if (value != null && value !is String) null else value as String?).toEnum(defaultValue = defaultValue)
                }
                .distinctUntilChanged()
        }.collectAsState(initialValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value.name
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
