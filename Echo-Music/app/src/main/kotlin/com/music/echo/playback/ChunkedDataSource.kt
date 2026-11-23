package com.music.echo.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import echo.music.iad1tya.utils.InnerTubeXResolver
import echo.music.iad1tya.utils.PlayerClient
import java.io.InterruptedIOException

/**
 * Fetches a stream as a run of bounded ranges instead of one open-ended read.
 *
 * googlevideo paces a continuous response down to roughly playback speed: ask for a whole track in
 * one request and it arrives at about 15kB/s, barely ahead of the rate it is being consumed at. Ask
 * for the same bytes as bounded ranges and each one is served at line rate — measured on the same
 * track, over the same connection, seconds apart:
 * ```
 *   GET (unbounded)               15.5 kB/s
 *   Range: bytes=0-2097151         5.7 MB/s
 * ```
 *
 * That is the difference between a player that can build a buffer and one that can only just keep
 * up, and it is why a stall never recovers into a cushion: there is no spare bandwidth to catch up
 * with.
 *
 * [AudioCache] already fetches this way — it is why read-ahead can put a whole track on disk in the
 * time between songs. This puts the player's own reads on the same footing, which matters for the
 * track being listened to right now: read-ahead deliberately never touches it (Media3 locks a cache
 * entry to one writer), so without this it is the one track that always streams throttled.
 *
 * Transparent to everything above it.
 * [CacheDataSource][androidx.media3.datasource.cache.CacheDataSource] sees one continuous stream of
 * the length it asked for. Bounded requests are split too, so no caller can exceed
 * [PlayerClient.rangeBytesFor].
 */
@UnstableApi
class ChunkedDataSource(
  private val upstream: DataSource,
  private val chunkBytes: Long,
) : DataSource {

  private var baseSpec: DataSpec? = null
  private var position = 0L
  private var bytesRemaining = 0L
  private var chunkRemaining = 0L
  private var chunkOpen = false
  private var rangeBytes = chunkBytes

  /** Set when the request can't be improved on, and is simply forwarded. */
  private var passthrough = false

  override fun addTransferListener(transferListener: TransferListener) {
    upstream.addTransferListener(transferListener)
  }

  /**
   * The total size has to be known up front, or there is no way to say where the last range ends.
   * Every progressive googlevideo URL carries it as `clen`, which costs nothing to read; anything
   * else is forwarded as-is.
   */
  override fun open(dataSpec: DataSpec): Long {
    baseSpec = dataSpec
    position = dataSpec.position

    val total = dataSpec.uri.getQueryParameter("clen")?.toLongOrNull()
    if (total == null) {
      passthrough = true
      chunkOpen = true
      // Reported, not just thrown. Nothing but googlevideo carries `clen`,
      // so every module stream comes through this branch — and this was
      // the one open in the app whose refusal was neither logged nor
      // handed anywhere. A dead Tidal URL surfaced as a bare
      // ExoPlaybackException with not one line naming the server that
      // produced it or the status it produced.
      try {
        return upstream.open(dataSpec)
      } catch (e: Exception) {
        if (e !is InterruptedIOException) {
          // `host` is null exactly when the URL was too broken to
          // parse, which is the case most in need of naming — so fall
          // back to the string itself rather than logging "null".
          val who = dataSpec.uri.host ?: dataSpec.uri.toString().take(120)
          timber.log.Timber.w(TAG, "$who refused the stream: ${e.message}")
          report(dataSpec, e)
        }
        throw e
      }
    }

    passthrough = false
    val end =
      if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
        total
      } else {
        minOf(total, position + dataSpec.length)
      }
    bytesRemaining = (end - position).coerceAtLeast(0L)
    rangeBytes = minOf(chunkBytes, PlayerClient.rangeBytesFor(dataSpec.uri.toString()))
    if (bytesRemaining > 0) openChunk()
    return bytesRemaining
  }

  private fun openChunk() {
    val length = minOf(rangeBytes, bytesRemaining)
    val spec = requireNotNull(baseSpec).buildUpon().setPosition(position).setLength(length).build()
    try {
      upstream.open(spec)
    } catch (e: Exception) {
      // Which client minted the URL is the first thing worth knowing when
      // a range is refused, and it isn't recoverable from anywhere else by
      // the time this surfaces as a playback error. A cancelled read-ahead
      // arrives here too and means nothing — see [AudioCache.prefetchQueue].
      if (e !is InterruptedIOException) {
        timber.log.Timber.w(
          TAG,
          "range $position-${position + length - 1} refused for " +
            "${spec.uri.getQueryParameter("c")}: ${e.message}",
        )
        // This is the only place a refusal of the *real* fetch is seen.
        // Left here it is just a failed track; handed back, it is the
        // one piece of evidence that retiring the client rests on.
        report(spec, e)
      }
      throw e
    }
    chunkRemaining = length
    chunkOpen = true
  }

  /**
   * Hands a refusal back to whoever minted the URL that was refused.
   *
   * [StreamResolver] sorts out whether the URL was one of its own — see
   * [InnerTubeXResolver.onRefused]. Anything that isn't a refusal, or isn't from a client it
   * models, costs nothing here.
   */
  private fun report(spec: DataSpec, e: Exception) {
    if (e is HttpDataSource.InvalidResponseCodeException) {
      InnerTubeXResolver.onRefused(spec.uri.toString())
    }
  }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    if (passthrough) return upstream.read(buffer, offset, length)
    if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

    // A range that ends early is re-opened for the part that didn't
    // arrive, which is also how the step to the next range happens. The
    // attempt limit is what stops a server that has decided to send
    // nothing from spinning here forever.
    repeat(MAX_EMPTY_RANGES) {
      if (chunkRemaining == 0L) {
        closeChunk()
        openChunk()
      }
      val read = upstream.read(buffer, offset, minOf(length.toLong(), chunkRemaining).toInt())
      if (read != C.RESULT_END_OF_INPUT) {
        position += read
        chunkRemaining -= read
        bytesRemaining -= read
        return read
      }
      chunkRemaining = 0L
    }
    return C.RESULT_END_OF_INPUT
  }

  private fun closeChunk() {
    if (chunkOpen) {
      upstream.close()
      chunkOpen = false
    }
  }

  override fun getUri(): Uri? = upstream.uri ?: baseSpec?.uri

  override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

  override fun close() {
    closeChunk()
    baseSpec = null
    bytesRemaining = 0L
    chunkRemaining = 0L
  }

  private companion object {
    const val TAG = "EchoMusic"

    /** Enough to ride out a truncated range, not enough to hang on a dead one. */
    const val MAX_EMPTY_RANGES = 3
  }

  /** Wraps [upstream]'s sources so everything opened through it is ranged. */
  class Factory(
    private val upstream: DataSource.Factory,
    private val chunkBytes: Long,
  ) : DataSource.Factory {
    override fun createDataSource(): DataSource =
      ChunkedDataSource(upstream.createDataSource(), chunkBytes)
  }
}
