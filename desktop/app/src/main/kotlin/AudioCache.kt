package echo.desktop

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads a song to a temp file in 1 MB range requests (the way YouTube expects), so VLC plays a
 * local file instead of fighting YouTube's server. Replays reuse the file.
 * shortcut: cache is never cleaned up; add a size limit when it matters.
 */
object AudioCache {
    private const val CHUNK = 1_048_576L
    private val dir = File(System.getProperty("java.io.tmpdir"), "echomusic-cache").apply { mkdirs() }
    private val http: HttpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build()

    suspend fun fetch(videoId: String, stream: ResolvedStream): File =
        withContext(Dispatchers.IO) {
            val out = File(dir, "$videoId-${if (Settings.lowQuality) "low" else "high"}.${stream.extension}")
            if (out.length() > 0) return@withContext out
            val part = File(dir, "$videoId.part")
            part.outputStream().use { sink ->
                var pos = 0L
                var total = Long.MAX_VALUE
                while (pos < total) {
                    val end = pos + CHUNK - 1
                    fun request(url: String, header: Boolean) =
                        HttpRequest.newBuilder(URI.create(url))
                            .header("User-Agent", stream.userAgent)
                            .apply { if (header) header("Range", "bytes=$pos-$end") }
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build()
                    var response = http.send(request(stream.url, true), HttpResponse.BodyHandlers.ofByteArray())
                    // Some URLs refuse the Range header but accept the same range as a query parameter.
                    if (response.statusCode() == 403) {
                        response = http.send(request("${stream.url}&range=$pos-$end", false), HttpResponse.BodyHandlers.ofByteArray())
                    }
                    val code = response.statusCode()
                    if (code !in 200..299) error("HTTP $code while downloading audio")
                    val body = response.body()
                    sink.write(body)
                    if (code == 200 || body.size < CHUNK) break // whole file arrived, or last chunk
                    total =
                        response.headers().firstValue("Content-Range").map { it.substringAfter('/').toLongOrNull() }.orElse(null)
                            ?: Long.MAX_VALUE
                    pos += body.size
                }
            }
            check(part.length() > 0) { "Downloaded file is empty" }
            part.renameTo(out)
            out
        }
}
