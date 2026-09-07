package com.music.innertube.utils

import com.music.innertube.YouTube
import com.music.innertube.pages.LibraryPage
import com.music.innertube.pages.PlaylistPage
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest

// kotlin.runCatching does not special-case cancellation: a CancellationException thrown by a
// suspend call inside it is caught just like any other exception and returned as a failed
// Result, silently ending the coroutine's cancellation instead of propagating it. Both
// `completed()` overloads below run their runCatching block and then re-throw if what it
// caught was a CancellationException, so real cancellation still reaches the caller.
private fun <T> Result<T>.rethrowCancellation(): Result<T> = also {
    val exception = exceptionOrNull()
    if (exception is CancellationException) throw exception
}

@JvmName("completedLibrary")
suspend fun Result<PlaylistPage>.completed(): Result<PlaylistPage> = runCatching {
    val page = getOrThrow()
    val songs = page.songs.toMutableList()
    var continuation = page.songsContinuation
    val seenContinuations = mutableSetOf<String>()
    var requestCount = 0
    val maxRequests = 50
    var consecutiveEmptyResponses = 0
    
    while (continuation != null && requestCount < maxRequests) {
        if (continuation in seenContinuations) {
            break
        }
        seenContinuations.add(continuation)
        requestCount++
        
        // A transient failure here must not be mistaken for "no more pages" — that would
        // silently truncate the list, and callers that reconcile local state against it
        // (e.g. unliking songs missing from a resync) would wrongly treat the missing tail
        // as deliberately removed. Propagate the failure instead so the whole result fails.
        val continuationPage = YouTube.playlistContinuation(continuation).getOrElse {
            if (it is CancellationException) throw it
            throw IllegalStateException("Pagination failed while completing playlist", it)
        }

        if (continuationPage.songs.isEmpty()) {
            consecutiveEmptyResponses++
            if (consecutiveEmptyResponses >= 2) break
        } else {
            consecutiveEmptyResponses = 0
            songs += continuationPage.songs
        }
        
        continuation = continuationPage.continuation
    }
    PlaylistPage(
        playlist = page.playlist,
        songs = songs,
        songsContinuation = null,
        continuation = page.continuation
    )
}.rethrowCancellation()

@JvmName("completedPlaylist")
suspend fun Result<LibraryPage>.completed(): Result<LibraryPage> = runCatching {
    val page = getOrThrow()
    val items = page.items.toMutableList()
    var continuation = page.continuation
    val seenContinuations = mutableSetOf<String>()
    var requestCount = 0
    val maxRequests = 50
    var consecutiveEmptyResponses = 0
    
    while (continuation != null && requestCount < maxRequests) {
        if (continuation in seenContinuations) {
            break
        }
        seenContinuations.add(continuation)
        requestCount++
        
        // See the matching comment in the PlaylistPage overload above: don't let a
        // transient failure masquerade as "list complete" and truncate the result.
        val continuationPage = YouTube.libraryContinuation(continuation).getOrElse {
            if (it is CancellationException) throw it
            throw IllegalStateException("Pagination failed while completing library page", it)
        }

        if (continuationPage.items.isEmpty()) {
            consecutiveEmptyResponses++
            if (consecutiveEmptyResponses >= 2) break
        } else {
            consecutiveEmptyResponses = 0
            items += continuationPage.items
        }
        
        continuation = continuationPage.continuation
    }
    LibraryPage(
        items = items,
        continuation = null
    )
}.rethrowCancellation()

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split("; ")
        .filter { it.isNotEmpty() }
        .mapNotNull { part ->
            val splitIndex = part.indexOf('=')
            if (splitIndex == -1) null
            else part.substring(0, splitIndex) to part.substring(splitIndex + 1)
        }
        .toMap()

fun String.parseTime(): Int? {
    try {
        val parts = split(":").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

fun isPrivateId(browseId: String): Boolean {
    return browseId.contains("privately")
}
