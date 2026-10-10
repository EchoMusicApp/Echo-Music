package echo.music.iad1tya.api

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiRouteTranslationTest {
  private lateinit var server: HttpServer
  private val requestBody = AtomicReference<JSONObject>()
  private val authorization = AtomicReference<String>()
  private val translatedLines = listOf("Hello", "World")
  private val endpoint: String
    get() = "http://127.0.0.1:${server.address.port}/v1/chat/completions"

  @Before
  fun setUp() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/v1/chat/completions") { exchange ->
      val body = JSONObject(exchange.requestBody.bufferedReader().use { it.readText() })
      requestBody.set(body)
      authorization.set(exchange.requestHeaders.getFirst("Authorization"))
      val content = JSONArray(translatedLines).toString()
      val streaming = body.optBoolean("stream")
      val choice =
        JSONObject()
          .put(
            if (streaming) "delta" else "message",
            JSONObject().put("content", content),
          )
      val payload = JSONObject().put("choices", JSONArray().put(choice)).toString()
      val response = if (streaming) "data: $payload\n\ndata: [DONE]\n\n" else payload
      val bytes = response.toByteArray(Charsets.UTF_8)
      exchange.responseHeaders.set(
        "Content-Type",
        if (streaming) "text/event-stream" else "application/json",
      )
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
  }

  @After
  fun tearDown() {
    server.stop(0)
  }

  @Test
  fun compatibleEndpointPreservesModelAndParsesTranslation() = runBlocking {
    val result =
      OpenRouterService.translate(
        "你好\n世界",
        "en",
        "test-api-route-key",
        endpoint,
        "claude-fable-5-1",
        "Literal",
        maxRetries = 1,
      )
    assertEquals(translatedLines, result.getOrThrow())
    assertEquals("claude-fable-5-1", requestBody.get().getString("model"))
    assertEquals("Bearer test-api-route-key", authorization.get())
    assertFalse(requestBody.get().optBoolean("stream"))
  }

  @Test
  fun compatibleEndpointParsesStreamingTranslation() = runBlocking {
    val chunks =
      OpenRouterStreamingService.streamTranslation(
          "你好\n世界",
          "en",
          "test-api-route-key",
          endpoint,
          "claude-fable-5-1",
          "Literal",
        )
        .toList()
    assertTrue(chunks.none { it is OpenRouterStreamingService.StreamChunk.Error })
    assertEquals(
      translatedLines,
      (chunks.last() as OpenRouterStreamingService.StreamChunk.Complete).translatedLines,
    )
    assertEquals("claude-fable-5-1", requestBody.get().getString("model"))
    assertEquals("Bearer test-api-route-key", authorization.get())
    assertTrue(requestBody.get().getBoolean("stream"))
  }
}
