package echo.music.iad1tya.desktop

import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Login in the person's own Chrome/Edge/Brave (sites refuse embedded browsers as "insecure"):
 * opens it with a throw-away profile and a local debugging port, watches that browser's cookies and, once
 * [ready] says the wanted ones exist, closes it and hands them over. Returns a function that cancels.
 * shortcut: Windows paths only, no Firefox, fresh profile each time (the person signs in again).
 */
object BrowserLogin {
  private val candidates = listOfNotNull(
    System.getenv("ProgramFiles(x86)")?.let { "$it\\Microsoft\\Edge\\Application\\msedge.exe" },
    System.getenv("ProgramFiles")?.let { "$it\\Microsoft\\Edge\\Application\\msedge.exe" },
    System.getenv("ProgramFiles")?.let { "$it\\Google\\Chrome\\Application\\chrome.exe" },
    System.getenv("ProgramFiles(x86)")?.let { "$it\\Google\\Chrome\\Application\\chrome.exe" },
    System.getenv("LOCALAPPDATA")?.let { "$it\\Google\\Chrome\\Application\\chrome.exe" },
    System.getenv("ProgramFiles")?.let { "$it\\BraveSoftware\\Brave-Browser\\Application\\brave.exe" },
  )

  fun open(url: String, domain: String, ready: (Map<String, String>) -> Boolean, onDone: (Map<String, String>) -> Unit, onFail: (String) -> Unit): () -> Unit {
    val exe = candidates.firstOrNull { File(it).isFile } ?: run { onFail("No Chrome, Edge or Brave found on this computer."); return {} }
    val port = ServerSocket(0).use { it.localPort }
    val profile = Files.createTempDirectory("echomusic-login").toFile()
    val proc = ProcessBuilder(exe, "--remote-debugging-port=$port", "--remote-allow-origins=*", "--user-data-dir=${profile.absolutePath}", "--no-first-run", "--no-default-browser-check", url).start()
    val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
    val stop = {
      cancelled.set(true)
      runCatching { proc.toHandle().descendants().forEach { it.destroy() }; proc.destroy() }
      Thread { Thread.sleep(2000); runCatching { profile.deleteRecursively() } }.start()
    }
    Thread {
      val http = HttpClient.newHttpClient()
      val deadline = System.currentTimeMillis() + 10 * 60_000
      while (!cancelled.get() && System.currentTimeMillis() < deadline) {
        Thread.sleep(1500)
        if (!proc.isAlive && proc.toHandle().descendants().findAny().isEmpty) { /* launcher may exit while the real browser runs */ }
        val got = runCatching {
          val v = http.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port/json/version")).build(), HttpResponse.BodyHandlers.ofString()).body()
          val ws = Json.parseToJsonElement(v).jsonObject["webSocketDebuggerUrl"]!!.jsonPrimitive.content
          cookies(http, ws, domain)
        }.getOrNull() ?: continue
        if (ready(got)) { stop(); onDone(got); return@Thread }
      }
      if (!cancelled.get()) { stop(); onFail("Sign-in timed out.") }
    }.apply { isDaemon = true }.start()
    return stop
  }

  /** One DevTools round trip: Storage.getCookies on the browser endpoint returns every cookie, HttpOnly ones included. */
  private fun cookies(http: HttpClient, wsUrl: String, domain: String): Map<String, String> {
    val reply = CompletableFuture<String>()
    val buf = StringBuilder()
    val ws = http.newWebSocketBuilder().buildAsync(URI(wsUrl), object : WebSocket.Listener {
      override fun onText(w: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        buf.append(data); if (last) reply.complete(buf.toString()); w.request(1); return null
      }
      override fun onError(w: WebSocket, error: Throwable) { reply.completeExceptionally(error) }
    }).get(3, TimeUnit.SECONDS)
    ws.sendText("""{"id":1,"method":"Storage.getCookies"}""", true)
    val body = reply.get(3, TimeUnit.SECONDS)
    runCatching { ws.sendClose(WebSocket.NORMAL_CLOSURE, "") }
    val arr: JsonArray = Json.parseToJsonElement(body).jsonObject["result"]!!.jsonObject["cookies"]!!.jsonArray
    return arr.map { it as JsonObject }.filter { domain in it["domain"]!!.jsonPrimitive.content }
      .associate { it["name"]!!.jsonPrimitive.content to it["value"]!!.jsonPrimitive.content }
  }
}
