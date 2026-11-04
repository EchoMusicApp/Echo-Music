package echo.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Properties

/** User options, kept in %APPDATA%\EchoMusic\settings.properties. Call [save] after changing one. */
object Settings {
    private val file = File(AppDir, "settings.properties")
    private val p = Properties().apply { runCatching { file.reader().use { load(it) } } }

    var lowQuality by mutableStateOf(p.getProperty("low") == "1")
    var glass by mutableStateOf(p.getProperty("glass") == "1")
    var accent by mutableStateOf(p.getProperty("accent", "D0BCFF"))
    var discordId by mutableStateOf(p.getProperty("discord", ""))
    var tasteLink by mutableStateOf(p.getProperty("taste", ""))
    var profileName by mutableStateOf(p.getProperty("name", ""))
    var avatarUrl by mutableStateOf(p.getProperty("avatar", ""))
    var cookie by mutableStateOf(p.getProperty("cookie", ""))

    fun save() {
        p.setProperty("low", if (lowQuality) "1" else "0")
        p.setProperty("glass", if (glass) "1" else "0")
        p.setProperty("accent", accent)
        p.setProperty("discord", discordId)
        p.setProperty("cookie", cookie)
        p.setProperty("name", profileName)
        p.setProperty("avatar", avatarUrl)
        p.setProperty("taste", tasteLink)
        runCatching {
            file.parentFile.mkdirs()
            file.writer().use { p.store(it, null) }
        }
    }
}
