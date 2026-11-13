package com.music.paxsenix

import android.content.Context
import com.music.paxsenix.models.LyricsResponse
import com.music.paxsenix.models.SearchResponse
import com.music.paxsenix.models.SearchResult
import echo.music.iad1tya.betterlyrics.TTMLParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import timber.log.Timber

object Paxsenix {
  private const val LOG_TAG = "EchoLyrics"
  @Volatile private var client: HttpClient? = null
  private var appVersion: String = "Unknown"

  fun init(context: Context) {
    if (client != null) return

    synchronized(this) {
      if (client != null) return

      appVersion =
        try {
          context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (_: Exception) {
          "Unknown"
        }

      Timber.tag(LOG_TAG).d("[Paxsenix] Initializing client with app version: $appVersion")

      client =
        HttpClient(CIO) {
          install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 4_000
          }
          install(ContentNegotiation) {
            json(
              Json {
                isLenient = true
                ignoreUnknownKeys = true
              }
            )
          }

          defaultRequest {
            url("https://lyrics.paxsenix.org")
            header(
              "User-Agent",
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            )
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
          }

          expectSuccess = true
        }

      Timber.tag(LOG_TAG).d("[Paxsenix] HTTP client initialized successfully")
    }
  }

  private val httpClient: HttpClient
    get() =
      client ?: throw IllegalStateException("Paxsenix.init() must be called before using Paxsenix")

  private suspend fun search(query: String): List<SearchResult> {
    val startTime = System.currentTimeMillis()
    return try {
      Timber.tag(LOG_TAG)
        .d("[Paxsenix] [search] Requesting endpoint '/apple-music/search' with query='%s'", query)
      val response =
        httpClient.get("/apple-music/search") { parameter("q", query) }.body<SearchResponse>()

      val duration = System.currentTimeMillis() - startTime
      Timber.tag(LOG_TAG)
        .d(
          "[Paxsenix] [search] Success (200 OK) in %d ms, results count: %d",
          duration,
          response.size,
        )
      response
    } catch (e: ResponseException) {
      val duration = System.currentTimeMillis() - startTime
      val statusCode = e.response.status.value
      Timber.tag(LOG_TAG)
        .w(
          "[Paxsenix] [search] Request failed with HTTP %d in %d ms: %s",
          statusCode,
          duration,
          e.message,
        )
      emptyList()
    } catch (e: Exception) {
      val duration = System.currentTimeMillis() - startTime
      Timber.tag(LOG_TAG).e(e, "[Paxsenix] [search] Exception in %d ms: %s", duration, e.message)
      emptyList()
    }
  }

  suspend fun getLyrics(
    title: String,
    artist: String,
    duration: Int,
    album: String? = null,
  ): Result<String> = runCatching {
    val (cleanedTitle, cleanedArtist) =
      com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery()

    Timber.tag(LOG_TAG)
      .d(
        "[Paxsenix] [getLyrics] Start: title='%s', artist='%s', duration=%d",
        title,
        artist,
        duration,
      )

    val searchQueries = buildList {
      add("$cleanedTitle $cleanedArtist")
      add(cleanedTitle)
      if (!album.isNullOrBlank()) {
        add("$cleanedTitle $cleanedArtist $album")
      }
    }

    var allResults: List<Pair<SearchResult, Double>> = emptyList()

    for (query in searchQueries) {
      if (allResults.isEmpty()) {
        val searchResults = search(query)
        if (searchResults.isNotEmpty()) {
          allResults = scoreAndFilterResults(searchResults, title, artist, duration)
        }
      }
    }

    if (allResults.isEmpty()) {
      Timber.tag(LOG_TAG)
        .w("[Paxsenix] [getLyrics] No tracks found for title='%s', artist='%s'", title, artist)
      throw IllegalStateException("No tracks found on Paxsenix")
    }

    val candidates = allResults.take(5)
    val scope = CoroutineScope(Dispatchers.IO)
    val jobs = candidates.map { (result, score) ->
      scope.async {
        Timber.tag(LOG_TAG)
          .d(
            "[Paxsenix] [getLyrics] Fetching candidate: '%s' (ID: %s, score: %.1f)",
            result.displayName,
            result.id,
            score,
          )
        result to runCatching { fetchLyricsForTrackWithType(result.id) }.getOrDefault("" to false)
      }
    }

    var plainFallback: String? = null
    try {
      val remaining = jobs.toMutableList()
      while (remaining.isNotEmpty()) {
        val (_, lrcPair) = select { remaining.forEach { deferred -> deferred.onAwait { it } } }
        remaining.removeAll { it.isCompleted }
        val (lrc, hasWordTimings) = lrcPair
        if (lrc.isNotEmpty()) {
          if (hasWordTimings) {
            Timber.tag(LOG_TAG).i("[Paxsenix] [getLyrics] Synced lyrics found!")
            return Result.success(lrc)
          } else if (plainFallback == null) {
            plainFallback = lrc
          }
        }
      }
    } finally {
      scope.coroutineContext.cancelChildren()
    }

    plainFallback?.let {
      Timber.tag(LOG_TAG).d("[Paxsenix] [getLyrics] Unsynced plain lyrics fallback used")
      return Result.success(it)
    }

    Timber.tag(LOG_TAG).w("[Paxsenix] [getLyrics] No valid lyrics content retrieved")
    return Result.failure(IllegalStateException("No lyrics available from Paxsenix"))
  }

  private fun scoreAndFilterResults(
    results: List<SearchResult>,
    title: String,
    artist: String,
    duration: Int,
  ): List<Pair<SearchResult, Double>> {
    val durationMs = duration * 1000
    val cleanupRegex = Regex("""\s*\(.*?\)|\s*\[.*?\]""")

    val (queryTitle, queryArtist) =
      com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery()
    val cleanedTitle = queryTitle.replace(cleanupRegex, "").lowercase().trim()
    val cleanedArtist = queryArtist.lowercase()

    val targetIsMixed = title.contains("mixed", ignoreCase = true)
    val targetIsRemix = title.contains("remix", ignoreCase = true)

    return results
      .map { result ->
        var score = 0.0

        result.duration?.let { d ->
          val diff = abs(d - durationMs)
          when {
            diff <= 2000 -> score += 100
            diff <= 5000 -> score += 50
            diff <= 10000 -> score += 10
            else -> score -= 50
          }
        }

        val resultTitleCleaned = result.displayName.replace(cleanupRegex, "").lowercase().trim()
        when {
          resultTitleCleaned == cleanedTitle -> score += 80
          resultTitleCleaned.contains(cleanedTitle) || cleanedTitle.contains(resultTitleCleaned) ->
            score += 40
        }

        val resultIsMixed = result.displayName.contains("mixed", ignoreCase = true)
        val resultIsRemix = result.displayName.contains("remix", ignoreCase = true)
        if (resultIsMixed && !targetIsMixed) score -= 60
        if (resultIsRemix && !targetIsRemix) score -= 40

        val resultArtistLower = result.displayArtist.lowercase()
        when {
          resultArtistLower.contains(cleanedArtist) -> score += 50
          else -> {
            val artistWords = cleanedArtist.split(Regex("\\s+")).filter { it.length > 2 }
            if (artistWords.any { resultArtistLower.contains(it) }) score += 25
          }
        }

        result to score
      }
      .sortedByDescending { it.second }
      .filter { it.second > 0 }
      .take(10)
  }

  private suspend fun fetchLyricsForTrackWithType(id: String): Pair<String, Boolean> {
    val result = fetchLyricsForTrack(id)
    if (result.isSuccess) {
      val lrc = result.getOrNull()!!
      val hasWordTimings = lrc.contains("<") && lrc.contains(">")
      return lrc to hasWordTimings
    }
    return "" to false
  }

  private suspend fun fetchLyricsForTrack(id: String): Result<String> = runCatching {
    val startTime = System.currentTimeMillis()
    Timber.tag(LOG_TAG).d("[Paxsenix] [fetchLyricsForTrack] Requesting ID: $id")

    val response =
      httpClient.get("/apple-music/lyrics") { parameter("id", id) }.body<LyricsResponse>()

    val duration = System.currentTimeMillis() - startTime
    val lyricsType = response.type
    Timber.tag(LOG_TAG)
      .d("[Paxsenix] [fetchLyricsForTrack] Success in %d ms, type=%s", duration, lyricsType)

    if (!response.ttmlContent.isNullOrBlank()) {
      val lrc = convertTTMLToAppFormat(response.ttmlContent)
      if (lrc.isNotEmpty()) {
        return@runCatching lrc
      }
    }

    if (!response.elrcMultiPerson.isNullOrBlank()) {
      return@runCatching response.elrcMultiPerson
    }
    if (!response.elrc.isNullOrBlank()) {
      return@runCatching response.elrc
    }
    if (!response.plain.isNullOrBlank()) {
      return@runCatching response.plain
    }

    if (response.content.isEmpty()) {
      throw IllegalStateException("No lyrics content found for track $id")
    }

    val hasWordLevel = lyricsType == "Syllable"

    if (!hasWordLevel) {
      val plain =
        response.content
          .map { line -> line.text.joinToString(" ") { it.text } }
          .filter { it.isNotBlank() }
          .joinToString("\n")
      return@runCatching plain
    }

    val lrc = buildString {
      response.content.forEach { line ->
        val timeMs = line.timestamp
        val minutes = timeMs / 1000 / 60
        val seconds = (timeMs / 1000) % 60
        val centiseconds = (timeMs % 1000) / 10

        val agent =
          when {
            line.background -> "{bg}"
            line.oppositeTurn -> "{agent:v2}"
            else -> "{agent:v1}"
          }

        val lineText = line.text.joinToString(" ") { it.text }

        if (lineText.isNotBlank()) {
          appendLine(
            String.format(
              Locale.US,
              "[%02d:%02d.%02d]%s%s",
              minutes,
              seconds,
              centiseconds,
              agent,
              lineText,
            )
          )
          if (line.text.isNotEmpty()) {
            val wordsData =
              line.text.joinToString("|") { word ->
                "${word.text}:${word.timestamp.toDouble() / 1000}:${word.endtime.toDouble() / 1000}"
              }
            if (wordsData.isNotEmpty()) appendLine("<$wordsData>")
          }
        }
      }
    }

    return@runCatching lrc
  }

  suspend fun getAllLyrics(
    title: String,
    artist: String,
    duration: Int,
    album: String? = null,
    callback: (String) -> Unit,
  ) {
    val (cleanedTitle, cleanedArtist) =
      com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery()

    val searchQueries = listOf("$cleanedTitle $cleanedArtist", cleanedTitle)
    var plainFallback: String? = null
    var scoredResults: List<Pair<SearchResult, Double>> = emptyList()

    for (query in searchQueries) {
      val results = search(query)
      if (results.isEmpty()) continue
      val filtered = scoreAndFilterResults(results, title, artist, duration)
      if (filtered.isNotEmpty()) {
        scoredResults = filtered
        break
      }
    }

    val candidates = scoredResults.take(3)
    val scope = CoroutineScope(Dispatchers.IO)
    val jobs = candidates.map { (result, _) ->
      scope.async {
        runCatching { fetchLyricsForTrackWithType(result.id) }.getOrDefault("" to false)
      }
    }
    try {
      val remaining = jobs.toMutableList()
      while (remaining.isNotEmpty()) {
        val (lrc, hasWordTimings) =
          select { remaining.forEach { deferred -> deferred.onAwait { it } } }
        remaining.removeAll { it.isCompleted }
        if (lrc.isNotEmpty()) {
          if (hasWordTimings) {
            callback(lrc)
            return
          } else if (plainFallback == null) {
            plainFallback = lrc
          }
        }
      }
    } finally {
      scope.coroutineContext.cancelChildren()
    }

    plainFallback?.let {
      callback(it)
    }
  }

  private fun convertTTMLToAppFormat(ttml: String): String {
    return try {
      val parsedLines = TTMLParser.parseTTML(ttml)
      TTMLParser.toLRC(parsedLines)
    } catch (e: Exception) {
      Timber.tag(LOG_TAG).e(e, "TTML conversion failed: ${e.message}")
      ""
    }
  }
}
