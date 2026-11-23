package echo.music.iad1tya.playback

import androidx.media3.datasource.DataSpec
import com.music.innertube.models.response.PlayerResponse
import echo.music.iad1tya.utils.YTPlayerUtils.PlaybackData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ResolvedStreamTest {
  @Test
  fun streamExpiryUsesTheAbsoluteTimeAndServerLifetime() {
    val playback = PlaybackData(
      audioConfig = null,
      videoDetails = null,
      playbackTracking = null,
      format = PlayerResponse.StreamingData.Format(
        itag = 251,
        url = null,
        mimeType = "audio/webm",
        bitrate = 128_000,
        width = null,
        height = null,
        contentLength = null,
        quality = "tiny",
        fps = null,
        qualityLabel = null,
        averageBitrate = null,
        audioQuality = null,
        approxDurationMs = null,
        audioSampleRate = 48_000,
        audioChannels = 2,
        loudnessDb = null,
        lastModified = null,
        signatureCipher = null,
        cipher = null,
        audioTrack = null,
      ),
      streamUrl = "https://example.com/audio",
      streamExpiresInSeconds = 60,
      headers = mapOf("User-Agent" to "media-client"),
    )
    val nowMs = 1_700_000_000_000L

    val stream = ResolvedStream.from(playback, nowMs)

    assertEquals(nowMs + 60_000L, stream.expiresAtMs)
    assertEquals(playback.headers, stream.headers)
  }

  @Test
  fun cachedStreamRetainsHeadersAndSeekRangeAcrossRequests() {
    val stream = ResolvedStream(
      "https://example.com/audio",
      Long.MAX_VALUE,
      mapOf("User-Agent" to "media-client", "Origin" to "https://example.com"),
    )
    val request = DataSpec.Builder()
      .setUri("https://example.com/song-id")
      .setKey("song-id_OPUS")
      .setPosition(1_048_576)
      .setLength(524_288)
      .setHttpRequestHeaders(mapOf("Accept" to "audio/*", "User-Agent" to "default"))
      .build()

    val resolved = stream.resolve(request)
    val reopened = stream.resolve(request.buildUpon().setPosition(2_097_152).build())

    assertEquals(stream.url, resolved.uri.toString())
    assertEquals("song-id_OPUS", resolved.key)
    assertEquals(1_048_576L, resolved.position)
    assertEquals(524_288L, resolved.length)
    assertEquals("media-client", resolved.httpRequestHeaders["User-Agent"])
    assertEquals("https://example.com", reopened.httpRequestHeaders["Origin"])
    assertEquals("audio/*", reopened.httpRequestHeaders["Accept"])
    assertEquals(2_097_152L, reopened.position)
  }
}
