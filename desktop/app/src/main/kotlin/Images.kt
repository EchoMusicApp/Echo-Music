package echo.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

// shortcut: unbounded in-memory cache, add an LRU if memory use grows.
private val cache = ConcurrentHashMap<String, ImageBitmap>()
private val http: HttpClient = HttpClient.newHttpClient()

/** Asks YouTube's image server for a bigger version of a thumbnail. */
fun hires(url: String?): String? = url?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")

@Composable
fun RemoteImage(url: String?, modifier: Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    var bitmap by remember(url) { mutableStateOf(url?.let { cache[it] }) }
    LaunchedEffect(url) {
        if (url == null || bitmap != null) return@LaunchedEffect
        bitmap =
            withContext(Dispatchers.IO) {
                runCatching {
                        val bytes =
                            http.send(
                                    HttpRequest.newBuilder(URI.create(url)).build(),
                                    HttpResponse.BodyHandlers.ofByteArray(),
                                )
                                .body()
                        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap().also { cache[url] = it }
                    }
                    .getOrNull()
            }
    }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** Average colour of a cached cover (waits briefly for it to load); [fallback] until then. */
@Composable
fun rememberCoverColor(url: String?, fallback: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
    var color by remember(url) { mutableStateOf(fallback) }
    LaunchedEffect(url) {
        repeat(40) {
            val bmp = url?.let { cache[it] }
            if (bmp != null) {
                color = withContext(Dispatchers.Default) {
                    val px = bmp.toPixelMap()
                    var r = 0L; var g = 0L; var b = 0L; var n = 0
                    for (y in 0 until px.height step 8) for (x in 0 until px.width step 8) {
                        val c = px[x, y]; r += (c.red * 255).toInt(); g += (c.green * 255).toInt(); b += (c.blue * 255).toInt(); n++
                    }
                    if (n == 0) fallback else androidx.compose.ui.graphics.Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
                }
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(250)
        }
    }
    return color
}
