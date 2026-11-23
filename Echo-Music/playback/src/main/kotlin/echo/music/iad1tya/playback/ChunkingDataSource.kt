package echo.music.iad1tya.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.TransferListener
import java.io.IOException

class ChunkingDataSource(
  private val upstream: DataSource,
  private val chunkSize: Long,
  private val maxRetries: Int = 3
) : DataSource {

  private var dataSpec: DataSpec? = null
  private var bytesToRead: Long = C.LENGTH_UNSET.toLong()
  private var bytesReadTotal: Long = 0
  private var isOpened = false

  override fun addTransferListener(transferListener: TransferListener) {
    upstream.addTransferListener(transferListener)
  }

  override fun open(dataSpec: DataSpec): Long {
    this.dataSpec = dataSpec
    this.bytesReadTotal = 0
    this.bytesToRead = dataSpec.length
    this.isOpened = true

    openNextChunk()

    return bytesToRead
  }

  private fun openNextChunk() {
    val currentDataSpec = this.dataSpec ?: throw IOException("DataSpec is null")
    val position = currentDataSpec.position + bytesReadTotal

    val length =
      if (bytesToRead == C.LENGTH_UNSET.toLong()) {
        chunkSize
      } else {
        val remaining = bytesToRead - bytesReadTotal
        if (remaining <= 0L) return
        minOf(chunkSize, remaining)
      }

    val chunkDataSpec = currentDataSpec.buildUpon().setPosition(position).setLength(length).build()
    upstream.open(chunkDataSpec)
  }

  override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
    if (readLength == 0) return 0
    if (!isOpened) return C.RESULT_END_OF_INPUT
    if (bytesToRead != C.LENGTH_UNSET.toLong() && bytesReadTotal >= bytesToRead) {
      return C.RESULT_END_OF_INPUT
    }

    var attempts = 0
    var lastException: Exception? = null

    while (attempts <= maxRetries) {
      val bytes =
        try {
          upstream.read(buffer, offset, readLength)
        } catch (e: java.io.InterruptedIOException) {
          throw e
        } catch (e: Exception) {
          lastException = e
          -1
        }

      if (bytes != C.RESULT_END_OF_INPUT && bytes > 0) {
        bytesReadTotal += bytes
        return bytes
      }

      // Upstream returned EOF or error. Check if we actually reached the expected end.
      if (bytesToRead != C.LENGTH_UNSET.toLong() && bytesReadTotal >= bytesToRead) {
        return C.RESULT_END_OF_INPUT
      }

      // We need more bytes. Try opening the next chunk at current position.
      try {
        upstream.close()
      } catch (_: Exception) {}

      val opened =
        try {
          openNextChunk()
          true
        } catch (e: InvalidResponseCodeException) {
          if (e.responseCode == 416) {
            return C.RESULT_END_OF_INPUT
          }
          attempts++
          lastException = e
          if (attempts > maxRetries) throw e
          false
        } catch (e: androidx.media3.datasource.DataSourceException) {
          @Suppress("DEPRECATION")
          if (e.reason == androidx.media3.datasource.DataSourceException.POSITION_OUT_OF_RANGE) {
            return C.RESULT_END_OF_INPUT
          }
          attempts++
          lastException = e
          if (attempts > maxRetries) throw e
          false
        } catch (e: Exception) {
          attempts++
          lastException = e
          if (attempts > maxRetries) {
            throw IOException("Failed to reconnect chunk after $maxRetries retries", e)
          }
          false
        }

      if (opened) {
        val nextBytes =
          try {
            upstream.read(buffer, offset, readLength)
          } catch (e: java.io.InterruptedIOException) {
            throw e
          } catch (e: Exception) {
            lastException = e
            -1
          }

        if (nextBytes != C.RESULT_END_OF_INPUT && nextBytes > 0) {
          bytesReadTotal += nextBytes
          return nextBytes
        }

        if (bytesToRead != C.LENGTH_UNSET.toLong() && bytesReadTotal >= bytesToRead) {
          return C.RESULT_END_OF_INPUT
        }

        attempts++
      }

      if (attempts <= maxRetries) {
        try {
          Thread.sleep((attempts * 50L).coerceAtMost(250L))
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
          throw java.io.InterruptedIOException("Interrupted during chunk retry backoff").apply {
            initCause(e)
          }
        }
      }
    }

    if (bytesToRead != C.LENGTH_UNSET.toLong() && bytesReadTotal < bytesToRead) {
      throw IOException(
        "Premature EOF: expected $bytesToRead bytes, but only received $bytesReadTotal bytes after $maxRetries retries",
        lastException
      )
    }

    lastException?.let { throw IOException("Failed to read chunk after $maxRetries retries", it) }

    return C.RESULT_END_OF_INPUT
  }

  override fun getUri(): Uri? = upstream.uri

  override fun close() {
    isOpened = false
    upstream.close()
  }
}

class ChunkingDataSourceFactory(
  private val upstreamFactory: DataSource.Factory,
  private val chunkSize: Long = 5L * 1024 * 1024 // 5MB chunks
) : DataSource.Factory {
  override fun createDataSource(): DataSource {
    return ChunkingDataSource(upstreamFactory.createDataSource(), chunkSize)
  }
}
