package echo.music.iad1tya.canvas

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import java.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AppleMusicTokenProvider {
  private var cachedToken: String? = null
  private var tokenExpiry: Long = 0L
  private val mutex = Mutex()

  private const val FALLBACK_TOKEN =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzkzOTAwNzA4LCJleHAiOjE3OTk5NDg3MDgsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.7L-PYJHjO8PpFdhKS-OSkf_snyFvg5cq-JDERUs_N_UcUX9Vk0kf1WLatgDeEvhB_Arg1b5Tl-99G1v6hUwH0A"

  private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private val httpClient =
    HttpClient(OkHttp) {
      install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
      }
      expectSuccess = false
    }

  suspend fun clearToken() {
    mutex.withLock {
      cachedToken = null
      tokenExpiry = 0L
    }
  }

  suspend fun getToken(): String {
    return mutex.withLock {
      val now = System.currentTimeMillis()
      if (cachedToken != null && tokenExpiry > now + 300_000L) {
        return@withLock cachedToken!!
      }
      try {
        val token = fetchLiveToken()
        if (!token.isNullOrBlank()) {
          cachedToken = token
          return@withLock token
        }
      } catch (e: Exception) {
        // Fall back to known good token
      }
      cachedToken = FALLBACK_TOKEN
      tokenExpiry = now + 86_400_000L
      FALLBACK_TOKEN
    }
  }

  private suspend fun fetchLiveToken(): String? {
    val candidateUrls = listOf("https://music.apple.com/us/browse", "https://beta.music.apple.com")
    val bundleRegex = Regex("""/assets/index[^"'\s]*\.js""")

    for (pageUrl in candidateUrls) {
      try {
        val htmlResponse =
          httpClient.get(pageUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "text/html")
          }
        val htmlBody = htmlResponse.bodyAsText()
        val bundleMatch = bundleRegex.find(htmlBody)?.value ?: continue
        val jsUrl =
          if (bundleMatch.startsWith("http")) bundleMatch else "https://music.apple.com$bundleMatch"

        val jsResponse =
          httpClient.get(jsUrl) {
            header("User-Agent", USER_AGENT)
          }
        val jsBody = jsResponse.bodyAsText()

        val tokenRegex = Regex("""eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
        val now = System.currentTimeMillis()

        val validToken =
          tokenRegex
            .findAll(jsBody)
            .mapNotNull { match ->
              val candidate = match.value
              runCatching {
                val parts = candidate.split('.')
                if (parts.size != 3) return@runCatching null
                val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
                val obj = json.parseToJsonElement(payloadJson).jsonObject
                val iss = obj["iss"]?.jsonPrimitive?.content
                val exp = obj["exp"]?.jsonPrimitive?.content?.toLongOrNull()?.times(1000L) ?: 0L
                if (iss == "AMPWebPlay" && exp > now + 300_000L) {
                  candidate to exp
                } else null
              }
                .getOrNull()
            }
            .maxByOrNull { it.second }

        if (validToken != null) {
          tokenExpiry = validToken.second
          return validToken.first
        }
      } catch (_: Exception) {
        continue
      }
    }
    return null
  }
}
