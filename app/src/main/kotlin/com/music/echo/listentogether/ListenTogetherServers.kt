

package iad1tya.echo.music.listentogether

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
    private const val SERVER_JSON_URL = "https://raw.githubusercontent.com/EchoMusicApp/Echo-Music/refs/heads/main/app/server.json"

    private val _servers = MutableStateFlow(
        listOf(
            ListenTogetherServer(
                name = "Echo Music Server",
                url = "wss://iad1tya-echomusic.hf.space/ws",
                location = "Global",
                operator = "ECHO"
            )
        )
    )
    
    val serversFlow: StateFlow<List<ListenTogetherServer>> = _servers

    val servers: List<ListenTogetherServer>
        get() = _servers.value

    init {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(SERVER_JSON_URL).build()
                val response = client.newCall(request).execute()
                response.body?.string()?.let { jsonString ->
                    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
                    val name = jsonObject["name"]?.jsonPrimitive?.content ?: "Hugging Face Sync"
                    val url = jsonObject["serverUrl"]?.jsonPrimitive?.content ?: "wss://devilmi-vivi-music-listen-together.hf.space"
                    val region = jsonObject["region"]?.jsonPrimitive?.content ?: "Global - VIVIDH"
                    
                    _servers.value = listOf(
                        ListenTogetherServer(
                            name = name,
                            url = url,
                            location = region,
                            operator = ""
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback implicitly retained
            }
        }
    }

    val defaultServerUrl: String
        get() = servers.first().url

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }
}
