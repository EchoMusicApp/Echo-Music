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
 * shortcut: Windows paths only, no Firefox; one private profile is kept in ~/.echomusic/browser-profile so a later connect needs no new sign-in.
 */
object BrowserLogin {
  /** The login browser's own profile, kept between runs (not the person's normal browser profile). */
  private fun loginProfile(): java.io.File = java.io.File(System.getProperty("user.home"), ".echomusic/browser-profile").also { it.mkdirs() }

  private val candidates = listOfNotNull(
    System.getenv("ProgramFiles(x86)")?.let { "$it\\Microsoft\\Edge\\Application\\msedge.exe" },
    System.getenv("ProgramFiles")?.let { "$it\\Microsoft\\Edge\\Application\\msedge.exe" },
    System.getenv("ProgramFiles")?.let { "$it\\Google\\Chrome\\Application\\chrome.exe" },
    System.getenv("ProgramFiles(x86)")?.let { "$it\\Google\\Chrome\\Application\\chrome.exe" },
    System.getenv("LOCALAPPDATA")?.let { "$it\\Google\\Chrome\\Application\\chrome.exe" },
    System.getenv("ProgramFiles")?.let { "$it\\BraveSoftware\\Brave-Browser\\Application\\brave.exe" },
  )

  private fun reg(vararg args: String): String = runCatching {
    ProcessBuilder(listOf("reg", "query") + args).redirectErrorStream(true).start().let { val o = it.inputStream.bufferedReader().readText(); it.waitFor(); o }
  }.getOrDefault("")

  /** The person's default browser (Windows registry), if it is Chromium based; Firefox has no compatible debugging protocol. */
  private fun defaultBrowserExe(): String? {
    val prog = Regex("ProgId\\s+REG_SZ\\s+(\\S+)").find(reg("HKCU\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\https\\UserChoice", "/v", "ProgId"))?.groupValues?.get(1) ?: return null
    val cmd = Regex("REG_SZ\\s+(.+)").find(reg("HKCR\\$prog\\shell\\open\\command", "/ve"))?.groupValues?.get(1)?.trim() ?: return null
    var exe = Regex("^\"([^\"]+)\"").find(cmd)?.groupValues?.get(1) ?: cmd.substringBefore(" --").trim()
    if (exe.endsWith("msedge_proxy.exe", ignoreCase = true)) exe = File(File(exe).parentFile, "msedge.exe").path // Edge's launcher stub ignores debugging flags
    return exe.takeIf { File(it).isFile && "firefox" !in it.lowercase() }
  }

  fun open(url: String, domain: String, ready: (Map<String, String>) -> Boolean, onDone: (Map<String, String>) -> Unit, onFail: (String) -> Unit): () -> Unit {
    val exe = (defaultBrowserExe() ?: candidates.firstOrNull { File(it).isFile }) ?: run { onFail("No Chrome, Edge, Brave or similar browser found on this computer."); return {} }
    val port = ServerSocket(0).use { it.localPort }
    val profile = loginProfile()
    val proc = ProcessBuilder(exe, "--remote-debugging-port=$port", "--remote-allow-origins=*", "--user-data-dir=${profile.absolutePath}", "--no-first-run", "--no-default-browser-check", url).start()
    val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
    val stop: () -> Unit = {
      cancelled.set(true)
      runCatching { proc.toHandle().descendants().forEach { it.destroy() }; proc.destroy() }
      Unit
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

  /**
   * Like [open], but instead of cookies it waits for a page whose address contains [pageHas] and runs [js] in it;
   * a non-blank string result is handed to [onDone] (used to read the Discord web app's token, as Android's WebView did).
   */
  fun openAndRead(url: String, pageHas: List<String>, js: String, onDone: (String) -> Unit, onFail: (String) -> Unit): () -> Unit {
    val exe = (defaultBrowserExe() ?: candidates.firstOrNull { File(it).isFile }) ?: run { onFail("No Chrome, Edge, Brave or similar browser found on this computer."); return {} }
    val port = ServerSocket(0).use { it.localPort }
    val profile = loginProfile()
    val proc = ProcessBuilder(exe, "--remote-debugging-port=$port", "--remote-allow-origins=*", "--user-data-dir=${profile.absolutePath}", "--no-first-run", "--no-default-browser-check", url).start()
    val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
    val stop: () -> Unit = {
      cancelled.set(true)
      runCatching { proc.toHandle().descendants().forEach { it.destroy() }; proc.destroy() }
      Unit
    }
    Thread {
      val http = HttpClient.newHttpClient()
      val deadline = System.currentTimeMillis() + 10 * 60_000
      while (!cancelled.get() && System.currentTimeMillis() < deadline) {
        Thread.sleep(2000)
        val token = runCatching {
          val tabs = Json.parseToJsonElement(http.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port/json")).build(), HttpResponse.BodyHandlers.ofString()).body()).jsonArray
          val tab = tabs.map { it.jsonObject }.firstOrNull { t -> t["type"]?.jsonPrimitive?.content == "page" && pageHas.any { it in t["url"]!!.jsonPrimitive.content } } ?: return@runCatching null
          val params = Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), kotlinx.serialization.json.buildJsonObject {
            put("expression", kotlinx.serialization.json.JsonPrimitive(js)); put("returnByValue", kotlinx.serialization.json.JsonPrimitive(true))
          })
          val r = cdp(http, tab["webSocketDebuggerUrl"]!!.jsonPrimitive.content, "Runtime.evaluate", params)
          r["result"]!!.jsonObject["result"]!!.jsonObject["value"]?.jsonPrimitive?.content?.trim('"')
        }.getOrNull()
        if (!token.isNullOrBlank() && token != "null") { stop(); onDone(token); return@Thread }
      }
      if (!cancelled.get()) { stop(); onFail("Sign-in timed out.") }
    }.apply { isDaemon = true }.start()
    return stop
  }

  private fun cdp(http: HttpClient, wsUrl: String, method: String, params: String = "{}"): JsonObject {
    val reply = CompletableFuture<String>()
    val buf = StringBuilder()
    val ws = http.newWebSocketBuilder().buildAsync(URI(wsUrl), object : WebSocket.Listener {
      override fun onText(w: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        buf.append(data); if (last) reply.complete(buf.toString()); w.request(1); return null
      }
      override fun onError(w: WebSocket, error: Throwable) { reply.completeExceptionally(error) }
    }).get(3, TimeUnit.SECONDS)
    ws.sendText("""{"id":1,"method":"$method","params":$params}""", true)
    val body = reply.get(5, TimeUnit.SECONDS)
    runCatching { ws.sendClose(WebSocket.NORMAL_CLOSURE, "") }
    return Json.parseToJsonElement(body).jsonObject
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
