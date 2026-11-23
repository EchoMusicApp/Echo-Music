package echo.music.iad1tya.api

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object OpenRouterService {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(90, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
  private val JSON = "application/json; charset=utf-8".toMediaType()

  private const val ANTHROPIC_VERSION = "2023-06-01"

  private fun isAnthropic(url: String): Boolean =
    url.contains("api.anthropic.com", ignoreCase = true) && url.contains("/messages", ignoreCase = true)

  /** Builds a chat request for either the OpenAI-compatible format or Anthropic's native Messages API. */
  private fun buildChatRequest(
    url: String,
    apiKey: String,
    model: String,
    system: String?,
    user: String,
    maxTokens: Int,
    temperature: Double?
  ): Request {
    val anthropic = isAnthropic(url)
    val body =
      JSONObject().apply {
        if (model.isNotBlank()) put("model", model)
        if (anthropic) {
          if (!system.isNullOrBlank()) put("system", system)
          put(
            "messages",
            JSONArray().put(JSONObject().put("role", "user").put("content", user))
          )
        } else {
          val msgs = JSONArray()
          if (!system.isNullOrBlank()) msgs.put(JSONObject().put("role", "system").put("content", system))
          msgs.put(JSONObject().put("role", "user").put("content", user))
          put("messages", msgs)
        }
        if (temperature != null) put("temperature", temperature)
        put("max_tokens", maxTokens)
      }
    return Request.Builder()
      .url(url)
      .apply {
        if (apiKey.isNotBlank()) {
          if (anthropic) {
            addHeader("x-api-key", apiKey.trim())
          } else {
            addHeader("Authorization", "Bearer ${apiKey.trim()}")
          }
        }
        if (anthropic) {
          addHeader("anthropic-version", ANTHROPIC_VERSION)
        } else {
          addHeader("HTTP-Referer", "https://github.com/EchoMusicApp/Echo-Music")
          addHeader("X-Title", "echomusic")
        }
      }
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(JSON))
      .build()
  }

  /** Extracts the assistant text from an OpenAI-style or Anthropic-style response. */
  private fun extractContent(responseBody: String): String? {
    val json = JSONObject(responseBody)
    json.optJSONArray("content")?.let { parts ->
      val text =
        (0 until parts.length())
          .mapNotNull { parts.optJSONObject(it)?.optString("text") }
          .joinToString("")
          .trim()
      if (text.isNotBlank()) return text
    }
    return json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()
  }

  fun isUrlSafe(url: String): Boolean {
    if (url.isBlank()) return true
    val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "https" && scheme != "http") return false
    val host = uri.host?.lowercase() ?: return false
    if (host == "169.254.169.254" || host.startsWith("169.254.") || host == "metadata.google.internal" || host == "instance-data") {
      return false
    }
    return true
  }

  suspend fun translate(
    text: String,
    targetLanguage: String,
    apiKey: String,
    baseUrl: String,
    model: String,
    mode: String,
    maxRetries: Int = 3,
    sourceLanguage: String? = null,
    onLog: ((String) -> Unit)? = null
  ): Result<List<String>> =
    withContext(Dispatchers.IO) {
      val effectiveUrl = baseUrl.ifBlank { "https://openrouter.ai/api/v1/chat/completions" }
      if (!isUrlSafe(effectiveUrl)) {
        return@withContext Result.failure(SecurityException("Invalid or restricted endpoint destination"))
      }
      onLog?.invoke("Starting translation...")
      var currentAttempt = 0

      if (text.isBlank()) {
        return@withContext Result.failure(Exception("Input text is empty"))
      }

      val lines = text.lines()
      val lineCount = lines.size

      while (currentAttempt < maxRetries) {
        try {

          val systemPrompt =
            """You are a precise lyrics translation assistant. Your output must ALWAYS be a valid JSON array of strings.

CRITICAL RULES:
1. Output ONLY a JSON array: ["line1", "line2", "line3"]
2. NO explanations, NO questions, NO additional text
3. Each input line maps to exactly one output line
4. Preserve empty lines as empty strings ""
5. Return EXACTLY $lineCount items in the array
6. If uncertain, provide best approximation but maintain line count"""

          val userPrompt =
            when (mode) {
              "Romanized" ->
                """Romanize/transliterate the following $lineCount lines into simple Latin script using ONLY basic English letters (a-z, A-Z).

CRITICAL REQUIREMENTS:
- Use ONLY simple ASCII characters (a-z, A-Z, 0-9, basic punctuation)
- NO special characters like ā, ī, ū, ñ, ç, etc.
- NO diacritics or accent marks
- If text is already in Latin script, return it UNCHANGED
- For non-Latin scripts (Hindi, Chinese, Japanese, Korean, Cyrillic, etc.), provide simple romanization
- DO NOT translate meaning, only convert script to simple English letters
- Keep all punctuation and formatting
- Preserve line-by-line structure exactly

Examples of correct simple romanization:
- Sanskrit/Hindi "आ" → "aa" (not "ā")
- Japanese "東京" → "toukyou" or "tokyo" (not "tōkyō")
- Korean "서울" → "seoul" (not "sŏul")

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings using ONLY simple ASCII characters."""
              "Transcribed" ->
                """Transcribe/transliterate the following $lineCount lines phonetically into $targetLanguage script.

CRITICAL REQUIREMENTS:
- Convert the SOUND/PRONUNCIATION of the original text into $targetLanguage script
- DO NOT translate the meaning - only represent how the original words SOUND
- Use the native script of $targetLanguage (e.g., Devanagari for Hindi, Hangul for Korean, etc.)
- Preserve the original pronunciation as closely as possible in the target script
- Keep punctuation and formatting
- Preserve line-by-line structure exactly
- If text is already in $targetLanguage script, return it UNCHANGED

Examples:
- Japanese "こんにちは" to Hindi → "कोन्निचिवा" (phonetic, not translation)
- English "Hello" to Hindi → "हेलो" (phonetic)
- Korean "안녕하세요" to Hindi → "अन्न्योंग हासेयो" (phonetic)

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings in $targetLanguage script."""
              else ->
                """Translate the following $lineCount lines to $targetLanguage.

IMPORTANT:
- Provide natural, accurate translation
- Maintain poetic flow and meaning
- Keep punctuation appropriate for target language
- Preserve line-by-line structure exactly
- For song lyrics, prioritize singability

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings."""
            }

          val request =
            buildChatRequest(
              url = effectiveUrl,
              apiKey = apiKey,
              model = model,
              system = systemPrompt,
              user = userPrompt,
              maxTokens = lineCount * 100,
              temperature = 0.3
            )

          onLog?.invoke("Calling OpenRouter API (Attempt ${currentAttempt + 1})...")
          val response = client.newCall(request).execute()
          val responseBody = response.body?.string() ?: ""

          if (!response.isSuccessful) {

            if (response.code >= 500) {
              currentAttempt++
              kotlinx.coroutines.delay(1000L * currentAttempt)
              continue
            }

            val errorMsg =
              try {
                JSONObject(responseBody ?: "").optJSONObject("error")?.optString("message")
                  ?: "HTTP ${response.code}: ${response.message}"
              } catch (e: Exception) {
                "HTTP ${response.code}: ${response.message}"
              }
            return@withContext Result.failure(Exception("Translation failed: $errorMsg"))
          }

          run {
            var content = extractContent(responseBody)

            if (!content.isNullOrBlank()) {

              var translatedLines: List<String>? = null

              try {
                val jsonArray = JSONArray(content)
                translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
              } catch (e: Exception) {

                content = content.replace("```json", "").replace("```", "").trim()

                try {
                  val jsonArray = JSONArray(content)
                  translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                } catch (e2: Exception) {

                  val startIdx = content.indexOf('[')
                  val endIdx = content.lastIndexOf(']')

                  if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    val jsonString = content.substring(startIdx, endIdx + 1)
                    try {
                      val jsonArray = JSONArray(jsonString)
                      translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                    } catch (e3: Exception) {

                      translatedLines =
                        content
                          .lines()
                          .filter { it.trim().isNotEmpty() }
                          .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    }
                  }
                }
              }

              if (translatedLines != null) {
                onLog?.invoke("Successfully parsed ${translatedLines.size} lines")
                if (translatedLines.size == lineCount) {
                  return@withContext Result.success(translatedLines)
                } else if (translatedLines.size > lineCount) {

                  return@withContext Result.success(translatedLines.take(lineCount))
                } else {

                  val paddedLines = translatedLines.toMutableList()
                  while (paddedLines.size < lineCount) {
                    paddedLines.add("")
                  }
                  return@withContext Result.success(paddedLines)
                }
              }
            }
          }
        } catch (e: Exception) {
          if (currentAttempt == maxRetries - 1) {
            return@withContext Result.failure(e)
          }
        }
        currentAttempt++
        kotlinx.coroutines.delay(1000L * currentAttempt)
      }
      return@withContext Result.failure(Exception("Max retries exceeded"))
    }

  suspend fun testConnection(
    baseUrl: String,
    apiKey: String,
    model: String
  ): Result<String> =
    withContext(Dispatchers.IO) {
      val effectiveUrl = baseUrl.ifBlank { "https://openrouter.ai/api/v1/chat/completions" }
      if (!isUrlSafe(effectiveUrl)) {
        return@withContext Result.failure(SecurityException("Invalid or restricted endpoint destination"))
      }
      try {
        val request =
          buildChatRequest(
            url = effectiveUrl,
            apiKey = apiKey,
            model = model,
            system = null,
            user = "ping",
            maxTokens = 10,
            temperature = null
          )

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
          val errorMsg =
            try {
              val json = JSONObject(responseBody)
              json.optJSONObject("error")?.optString("message")
                ?: if (json.has("message")) json.getString("message")
                else "HTTP ${response.code}: ${response.message}"
            } catch (e: Exception) {
              "HTTP ${response.code}: ${response.message}"
            }
          Result.failure(Exception(errorMsg))
        } else {
          val reply =
            try {
              val text = extractContent(responseBody)
              if (!text.isNullOrBlank()) text else "Connection OK"
            } catch (e: Exception) {
              "Connection OK"
            }
          Result.success(reply)
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
}

