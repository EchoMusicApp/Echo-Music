package echo.music.iad1tya.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.TransferListener
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkingDataSourceTest {

  private class MockUpstreamDataSource(
    private val totalData: ByteArray,
    private val prematureEofAt: Int? = null,
    private val throw416AtOrAfter: Int? = null
  ) : DataSource {

    private var currentDataSpec: DataSpec? = null
    private var offsetInStream = 0
    private var bytesReadInCurrentSpec = 0
    private var hasFailedPrematurely = false

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
      this.currentDataSpec = dataSpec
      this.offsetInStream = dataSpec.position.toInt()
      this.bytesReadInCurrentSpec = 0

      if (throw416AtOrAfter != null && offsetInStream >= throw416AtOrAfter) {
        throw androidx.media3.datasource.DataSourceException(androidx.media3.datasource.DataSourceException.POSITION_OUT_OF_RANGE)
      }

      val remaining = totalData.size - offsetInStream
      if (remaining <= 0) return 0L
      return if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
        minOf(dataSpec.length, remaining.toLong())
      } else {
        remaining.toLong()
      }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
      if (offsetInStream >= totalData.size) return C.RESULT_END_OF_INPUT

      if (!hasFailedPrematurely && prematureEofAt != null && offsetInStream >= prematureEofAt) {
        hasFailedPrematurely = true
        return C.RESULT_END_OF_INPUT // Simulate sudden premature stream close
      }

      val maxCanRead = minOf(readLength, totalData.size - offsetInStream)
      if (maxCanRead <= 0) return C.RESULT_END_OF_INPUT

      System.arraycopy(totalData, offsetInStream, buffer, offset, maxCanRead)
      offsetInStream += maxCanRead
      bytesReadInCurrentSpec += maxCanRead
      return maxCanRead
    }

    override fun getUri(): Uri? = null

    override fun close() {
      currentDataSpec = null
    }
  }

  @Test
  fun testNormalFullRead() {
    val sampleData = "Hello World! This is a complete audio stream for testing chunking.".toByteArray()
    val upstream = MockUpstreamDataSource(sampleData)
    val chunkingSource = ChunkingDataSource(upstream, chunkSize = 16)

    val spec = DataSpec.Builder().setUri(Uri.parse("https://example.com/audio")).setLength(sampleData.size.toLong()).build()
    chunkingSource.open(spec)

    val output = ByteArray(sampleData.size)
    var totalRead = 0
    val buf = ByteArray(10)
    while (true) {
      val read = chunkingSource.read(buf, 0, buf.size)
      if (read == C.RESULT_END_OF_INPUT) break
      System.arraycopy(buf, 0, output, totalRead, read)
      totalRead += read
    }
    chunkingSource.close()

    assertEquals(sampleData.size, totalRead)
    assertArrayEquals(sampleData, output)
  }

  @Test
  fun testPrematureEofTriggersRetryAndCompletes() {
    val sampleData = ByteArray(100) { it.toByte() }
    // Simulate premature EOF at byte 30 (before 100 bytes are read)
    val upstream = MockUpstreamDataSource(sampleData, prematureEofAt = 30)
    val chunkingSource = ChunkingDataSource(upstream, chunkSize = 20)

    val spec = DataSpec.Builder().setUri(Uri.parse("https://example.com/audio")).setLength(100L).build()
    chunkingSource.open(spec)

    val output = ByteArray(100)
    var totalRead = 0
    val buf = ByteArray(15)
    while (true) {
      val read = chunkingSource.read(buf, 0, buf.size)
      if (read == C.RESULT_END_OF_INPUT) break
      System.arraycopy(buf, 0, output, totalRead, read)
      totalRead += read
    }
    chunkingSource.close()

    assertEquals(100, totalRead)
    assertArrayEquals(sampleData, output)
  }

  @Test
  fun testHttp416ReturnsEndOfInputCleanly() {
    val sampleData = ByteArray(50) { it.toByte() }
    val upstream = MockUpstreamDataSource(sampleData, throw416AtOrAfter = 50)
    val chunkingSource = ChunkingDataSource(upstream, chunkSize = 25)

    val spec = DataSpec.Builder().setUri(Uri.parse("https://example.com/audio")).setLength(C.LENGTH_UNSET.toLong()).build()
    chunkingSource.open(spec)

    val output = ByteArray(50)
    var totalRead = 0
    val buf = ByteArray(25)
    while (true) {
      val read = chunkingSource.read(buf, 0, buf.size)
      if (read == C.RESULT_END_OF_INPUT) break
      System.arraycopy(buf, 0, output, totalRead, read)
      totalRead += read
    }
    chunkingSource.close()

    assertEquals(50, totalRead)
  }

  @Test
  fun testTransientSocketExceptionRetriesAndRecovers() {
    val sampleData = ByteArray(100) { it.toByte() }
    var throwCount = 2
    val upstream = object : DataSource {
      var offset = 0
      override fun addTransferListener(transferListener: TransferListener) {}
      override fun open(dataSpec: DataSpec): Long {
        offset = dataSpec.position.toInt()
        val remaining = (sampleData.size - offset).toLong()
        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) minOf(dataSpec.length, remaining) else remaining
      }
      override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (this.offset >= sampleData.size) return C.RESULT_END_OF_INPUT
        if (throwCount > 0 && this.offset >= 30) {
          throwCount--
          throw IOException("Connection reset by peer")
        }
        val canRead = minOf(readLength, sampleData.size - this.offset)
        if (canRead <= 0) return C.RESULT_END_OF_INPUT
        System.arraycopy(sampleData, this.offset, buffer, offset, canRead)
        this.offset += canRead
        return canRead
      }
      override fun getUri(): Uri? = null
      override fun close() {}
    }

    val chunkingSource = ChunkingDataSource(upstream, chunkSize = 20)
    val spec = DataSpec.Builder().setUri(Uri.parse("https://example.com/audio")).setLength(100L).build()
    chunkingSource.open(spec)

    val output = ByteArray(100)
    var totalRead = 0
    val buf = ByteArray(15)
    while (true) {
      val read = chunkingSource.read(buf, 0, buf.size)
      if (read == C.RESULT_END_OF_INPUT) break
      System.arraycopy(buf, 0, output, totalRead, read)
      totalRead += read
    }
    chunkingSource.close()

    assertEquals(100, totalRead)
    assertArrayEquals(sampleData, output)
  }

  @Test(expected = IOException::class)
  fun testPrematureEofExhaustsRetriesAndThrowsIOException() {
    val sampleData = ByteArray(100) { it.toByte() }
    val upstream = object : DataSource {
      var offset = 0
      override fun addTransferListener(transferListener: TransferListener) {}
      override fun open(dataSpec: DataSpec): Long {
        offset = dataSpec.position.toInt()
        val remaining = (sampleData.size - offset).toLong()
        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) minOf(dataSpec.length, remaining) else remaining
      }
      override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        // Always simulate premature EOF at 40 bytes
        if (this.offset >= 40) return C.RESULT_END_OF_INPUT
        val canRead = minOf(readLength, 40 - this.offset)
        System.arraycopy(sampleData, this.offset, buffer, offset, canRead)
        this.offset += canRead
        return canRead
      }
      override fun getUri(): Uri? = null
      override fun close() {}
    }

    val chunkingSource = ChunkingDataSource(upstream, chunkSize = 20, maxRetries = 2)
    val spec = DataSpec.Builder().setUri(Uri.parse("https://example.com/audio")).setLength(100L).build()
    chunkingSource.open(spec)

    val buf = ByteArray(15)
    while (true) {
      val read = chunkingSource.read(buf, 0, buf.size)
      if (read == C.RESULT_END_OF_INPUT) break
    }
  }
}
