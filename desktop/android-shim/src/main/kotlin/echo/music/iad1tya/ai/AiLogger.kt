package echo.music.iad1tya.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

object AiLogger {
    private val LogsKey = stringPreferencesKey("ai_action_logs")

    suspend fun log(context: Context, action: String, details: String) {
        try {
            val currentLogsStr = context.dataStore.data.map { it[LogsKey] ?: "[]" }.first()
            val logsArray = try { JSONArray(currentLogsStr) } catch(e: Exception) { JSONArray() }
            
            val newLog = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("action", action)
                put("details", details)
            }
            
            logsArray.put(newLog)
            
            val finalArray = if (logsArray.length() > 50) {
                val arr = JSONArray()
                for (i in logsArray.length() - 50 until logsArray.length()) {
                    arr.put(logsArray.get(i))
                }
                arr
            } else {
                logsArray
            }

            context.dataStore.edit {
                it[LogsKey] = finalArray.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
