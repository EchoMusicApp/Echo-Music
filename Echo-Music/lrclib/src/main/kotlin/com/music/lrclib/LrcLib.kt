package com.music.lrclib

import com.music.lrclib.models.Track
import com.music.lrclib.models.bestMatchingFor
import com.music.lrclib.models.bestMatchingForRelaxed
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlin.math.abs
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json

object LrcLib {
  private val client by lazy {
    HttpClient(CIO) {
      install(ContentNegotiation) {
        json(
          Json {
            isLenient = true
            ignoreUnknownKeys = true
          },
        )
      }

      defaultRequest { url("https://lrclib.net") }

      expectSuccess = true
    }
  }

  private fun cleanTitle(title: String, artist: String = ""): String {
    return com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery().first
  }

  private fun cleanArtist(artist: String, title: String = ""): String {
    return com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery().second
  }

  private suspend fun queryLyricsWithParams(
    trackName: String? = null,
    artistName: String? = null,
    albumName: String? = null,
    query: String? = null,
  ): List<Track> =
    runCatching {
        client
          .get("/api/search") {
            if (query != null) parameter("q", query)
            if (trackName != null) parameter("track_name", trackName)
            if (artistName != null) parameter("artist_name", artistName)
            if (albumName != null) parameter("album_name", albumName)
          }
          .body<List<Track>>()
      }
      .getOrDefault(emptyList())

  private suspend fun queryLyrics(
    artist: String,
    title: String,
    album: String? = null,
  ): List<Track> {
    val cleaned = com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist)
    val (cleanedTitle, cleanedArtist) = cleaned.toSearchQuery()
    val primaryArtist = cleaned.primaryArtist

    // Strategy 1: Search with cleaned title and primary artist
    var results =
      queryLyricsWithParams(trackName = cleanedTitle, artistName = primaryArtist, albumName = album)
        .filter { it.syncedLyrics != null || it.plainLyrics != null }

    if (results.isNotEmpty()) return results

    // Strategy 1b: Fallback to full cleaned artist if different from primary artist
    if (primaryArtist != cleanedArtist) {
      results =
        queryLyricsWithParams(trackName = cleanedTitle, artistName = cleanedArtist, albumName = album)
          .filter { it.syncedLyrics != null || it.plainLyrics != null }

      if (results.isNotEmpty()) return results
    }

    // Strategy 2: Search with cleaned title only (artist might be different)
    results =
      queryLyricsWithParams(trackName = cleanedTitle).filter {
        it.syncedLyrics != null || it.plainLyrics != null
      }

    if (results.isNotEmpty()) return results

    // Strategy 3: Use q parameter with combined search
    results =
      queryLyricsWithParams(query = "$cleanedArtist $cleanedTitle").filter {
        it.syncedLyrics != null || it.plainLyrics != null
      }

    if (results.isNotEmpty()) return results

    // Strategy 4: Use q parameter with just title
    results =
      queryLyricsWithParams(query = cleanedTitle).filter {
        it.syncedLyrics != null || it.plainLyrics != null
      }

    if (results.isNotEmpty()) return results

    // Strategy 5: Try original title if different from cleaned
    if (cleanedTitle != title.trim()) {
      results =
        queryLyricsWithParams(trackName = title.trim(), artistName = artist.trim()).filter {
          it.syncedLyrics != null || it.plainLyrics != null
        }
    }

    return results
  }

  suspend fun getLyrics(
    title: String,
    artist: String,
    duration: Int,
    album: String? = null,
  ) = runCatching {
    val (cleanedTitle, cleanedArtist) = com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery()
    val tracks = queryLyrics(artist, title, album)

    val res =
      when {
        duration == -1 -> {
          tracks
            .bestMatchingFor(duration, cleanedTitle, cleanedArtist)
            ?.let { track -> track.syncedLyrics ?: track.plainLyrics }
            ?.let(LrcLib::Lyrics)
        }
        else -> {
          // Try with relaxed duration matching (±5 seconds instead of ±2)
          tracks
            .bestMatchingForRelaxed(duration)
            ?.let { track -> track.syncedLyrics ?: track.plainLyrics }
            ?.let(LrcLib::Lyrics)
        }
      }

    if (res != null) {
      return@runCatching res.text
    } else {
      throw IllegalStateException("Lyrics unavailable")
    }
  }

  suspend fun getAllLyrics(
    title: String,
    artist: String,
    duration: Int,
    album: String? = null,
    callback: (String) -> Unit,
  ) {
    val (cleanedTitle, cleanedArtist) = com.music.echo.metadata.cleaner.MetadataCleaner.clean(title, artist).toSearchQuery()
    val tracks = queryLyrics(artist, title, album)
    var count = 0
    var plain = 0

    val sortedTracks =
      when {
        duration == -1 -> {
          tracks.sortedByDescending { track ->
            var score = 0.0

            if (track.syncedLyrics != null) score += 1.0

            val titleSimilarity = calculateStringSimilarity(cleanedTitle, track.trackName)
            val artistSimilarity = calculateStringSimilarity(cleanedArtist, track.artistName)
            score += (titleSimilarity + artistSimilarity) / 2.0

            score
          }
        }
        else -> {
          tracks.sortedBy { abs(it.duration.toInt() - duration) }
        }
      }

    sortedTracks.forEach { track ->
      currentCoroutineContext().ensureActive()
      if (count <= 4) {
        if (track.syncedLyrics != null && duration == -1) {
          count++
          track.syncedLyrics.let(callback)
        } else {
          // Relaxed duration matching (±5 seconds)
          if (track.syncedLyrics != null && abs(track.duration.toInt() - duration) <= 5) {
            count++
            track.syncedLyrics.let(callback)
          }
          if (
            track.plainLyrics != null && abs(track.duration.toInt() - duration) <= 5 && plain == 0
          ) {
            count++
            plain++
            track.plainLyrics.let(callback)
          }
        }
      }
    }
  }

  private fun calculateStringSimilarity(str1: String, str2: String): Double {
    val s1 = str1.trim().lowercase()
    val s2 = str2.trim().lowercase()

    if (s1 == s2) return 1.0
    if (s1.isEmpty() || s2.isEmpty()) return 0.0

    return when {
      s1.contains(s2) || s2.contains(s1) -> 0.8
      else -> {
        val maxLength = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        1.0 - (distance.toDouble() / maxLength)
      }
    }
  }

  private fun levenshteinDistance(str1: String, str2: String): Int {
    val len1 = str1.length
    val len2 = str2.length
    val matrix = Array(len1 + 1) { IntArray(len2 + 1) }

    for (i in 0..len1) matrix[i][0] = i
    for (j in 0..len2) matrix[0][j] = j

    for (i in 1..len1) {
      for (j in 1..len2) {
        val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
        matrix[i][j] =
          minOf(
            matrix[i - 1][j] + 1, // deletion
            matrix[i][j - 1] + 1, // insertion
            matrix[i - 1][j - 1] + cost // substitution
          )
      }
    }

    return matrix[len1][len2]
  }

  suspend fun lyrics(
    artist: String,
    title: String,
  ) = runCatching { queryLyrics(artist = artist, title = title, album = null) }

  @JvmInline
  value class Lyrics(
    val text: String,
  ) {
    val sentences
      get() =
        runCatching {
            buildMap {
              put(0L, "")
              text
                .trim()
                .lines()
                .filter { it.length >= 10 }
                .forEach {
                  put(
                    it[8].digitToInt() * 10L +
                      it[7].digitToInt() * 100 +
                      it[5].digitToInt() * 1000 +
                      it[4].digitToInt() * 10000 +
                      it[2].digitToInt() * 60 * 1000 +
                      it[1].digitToInt() * 600 * 1000,
                    it.substring(10),
                  )
                }
            }
          }
          .getOrNull()
  }
}
