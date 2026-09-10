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

    private val repoUrl = "https://raw.githubusercontent.com/itsmechinmoy/echo-extensions/main/echo_extensions.json"

    // Storage directory for installed extensions
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

                _availableExtensions.value = updatedList
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

                _availableExtensions.value = _availableExtensions.value.map {
                    if (it.packageName == extension.packageName) it.copy(isInstalled = true) else it
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
