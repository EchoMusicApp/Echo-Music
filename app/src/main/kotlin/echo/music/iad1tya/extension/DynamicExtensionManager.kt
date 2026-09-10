package echo.music.iad1tya.extension

import android.content.Context
import echo.music.iad1tya.extension.model.EchoExtensionItem
import echo.music.iad1tya.extension.model.EchoExtensionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DynamicExtensionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("savish_dynamic_extensions", Context.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _availableExtensions = MutableStateFlow<List<EchoExtensionItem>>(emptyList())
    val availableExtensions: StateFlow<List<EchoExtensionItem>> = _availableExtensions.asStateFlow()

    private val repoUrl = "https://raw.githubusercontent.com/vishallsinghh21/Savish-Music/main/echo_extensions.json"

    private val extensionsDir = File(context.filesDir, "installed_extensions").apply {
        if (!exists()) mkdirs()
    }

    suspend fun fetchRepository() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(repoUrl).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!body.isNullOrEmpty()) {
                val manifest = try {
                    json.decodeFromString<EchoExtensionManifest>(body)
                } catch (e: Exception) {
                    val list = json.decodeFromString<List<EchoExtensionItem>>(body)
                    EchoExtensionManifest(list)
                }

                val installedPackages = prefs.getStringSet("installed_packages", emptySet()) ?: emptySet()

                val updatedList = manifest.extensions.map { ext ->
                    val file = File(extensionsDir, "${ext.packageName}.apk")
                    ext.copy(isInstalled = installedPackages.contains(ext.packageName) || file.exists())
                }

                withContext(Dispatchers.Main) {
                    _availableExtensions.value = updatedList
                }
                return@withContext
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback Default Extensions agar internet fetch fail ho jaye
        val fallbackList = listOf(
            EchoExtensionItem(
                id = "spotify",
                name = "Spotify",
                packageName = "echo.extension.spotify",
                version = "1.0.0",
                description = "Spotify Music & Playlists Platform",
                downloadUrl = "https://raw.githubusercontent.com/vishallsinghh21/Savish-Music/main/apks/spotify.apk",
                colorHex = "#1DB954",
                requiresLogin = true
            ),
            EchoExtensionItem(
                id = "gaana",
                name = "Gaana",
                packageName = "echo.extension.gaana",
                version = "1.0.0",
                description = "Gaana Music Platform",
                downloadUrl = "https://raw.githubusercontent.com/vishallsinghh21/Savish-Music/main/apks/gaana.apk",
                colorHex = "#E7232A",
                requiresLogin = false
            ),
            EchoExtensionItem(
                id = "jiosaavn",
                name = "JioSaavn",
                packageName = "echo.extension.jiosaavn",
                version = "1.0.0",
                description = "JioSaavn Music Platform",
                downloadUrl = "https://raw.githubusercontent.com/vishallsinghh21/Savish-Music/main/apks/jiosaavn.apk",
                colorHex = "#282828",
                requiresLogin = false
            )
        )

        val installedPackages = prefs.getStringSet("installed_packages", emptySet()) ?: emptySet()
        val finalFallback = fallbackList.map { ext ->
            val file = File(extensionsDir, "${ext.packageName}.apk")
            ext.copy(isInstalled = installedPackages.contains(ext.packageName) || file.exists())
        }

        withContext(Dispatchers.Main) {
            _availableExtensions.value = finalFallback
        }
    }

    suspend fun downloadAndInstall(extension: EchoExtensionItem, onProgress: (Boolean) -> Unit) = withContext(Dispatchers.IO) {
        onProgress(true)
        try {
            val request = Request.Builder().url(extension.downloadUrl).build()
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream()

            if (inputStream != null) {
                val file = File(extensionsDir, "${extension.packageName}.apk")
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()

                val installedPackages = prefs.getStringSet("installed_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                installedPackages.add(extension.packageName)
                prefs.edit().putStringSet("installed_packages", installedPackages).apply()

                withContext(Dispatchers.Main) {
                    _availableExtensions.value = _availableExtensions.value.map {
                        if (it.packageName == extension.packageName) it.copy(isInstalled = true) else it
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            onProgress(false)
        }
    }

    fun uninstallExtension(extension: EchoExtensionItem) {
        val file = File(extensionsDir, "${extension.packageName}.apk")
        if (file.exists()) file.delete()

        val installedPackages = prefs.getStringSet("installed_packages", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        installedPackages.remove(extension.packageName)
        prefs.edit().putStringSet("installed_packages", installedPackages).apply()

        _availableExtensions.value = _availableExtensions.value.map {
            if (it.packageName == extension.packageName) it.copy(isInstalled = false) else it
        }
    }

    fun getInstalledExtensions(): List<EchoExtensionItem> {
        return _availableExtensions.value.filter { it.isInstalled }
    }
}
