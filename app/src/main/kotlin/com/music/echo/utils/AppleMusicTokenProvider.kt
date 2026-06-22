package iad1tya.echo.music.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AppleMusicTokenProvider {
    private var cachedToken: String? = null
    private val mutex = Mutex()

    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = true
    }

    suspend fun getToken(): String {
        return mutex.withLock {
            cachedToken?.let { return@withLock it }

            try {
                // 1. Fetch main page
                val htmlResponse = httpClient.get("https://beta.music.apple.com")
                val htmlBody = htmlResponse.bodyAsText()

                // 2. Find index.js URI
                val indexJsRegex = Regex("""src="(/assets/index-[^"]+\.js)"""")
                val match = indexJsRegex.find(htmlBody) 
                    ?: throw Exception("Could not find index.js in Apple Music web player")
                val indexJsUri = match.groupValues[1]

                // 3. Fetch index.js
                val indexJsResponse = httpClient.get("https://beta.music.apple.com$indexJsUri")
                val indexJsBody = indexJsResponse.bodyAsText()

                // 4. Extract JWT token using the updated regex
                val tokenRegex = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
                val tokenMatch = tokenRegex.find(indexJsBody)
                    ?: throw Exception("Could not find token in index.js")
                
                val token = tokenMatch.value
                cachedToken = token
                token
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to the old hardcoded token if extraction fails
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"
            }
        }
    }
}
