package echo.music.iad1tya.utils.potoken

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import com.music.innertube.YouTube
import echo.music.iad1tya.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PoTokenWebView private constructor(
    context: Context,
    
    private val continuation: Continuation<PoTokenWebView>,
) {
    private val webView = WebView(context)
    private val scope = MainScope()
    private val initResumed = AtomicBoolean(false)
    @Volatile
    private var closed = false
    @Volatile
    var isDead: Boolean = false
        private set
    private val poTokenContinuations =
        Collections.synchronizedMap(ArrayMap<Int, Continuation<String>>())
    private var nextReqId = 0

    @Synchronized
    private fun getNextReqId(): Int = ++nextReqId
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        onInitializationErrorCloseAndCancel(t)
    }
    private lateinit var expirationInstant: Instant

    
    init {
        val webViewSettings = webView.settings
        
        webViewSettings.javaScriptEnabled = true
        webViewSettings.userAgentString = USER_AGENT
        webViewSettings.blockNetworkLoads = true 

        
        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                val msg = m.message()
                
                when (m.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> Timber.tag(TAG).e("JS: $msg")
                    ConsoleMessage.MessageLevel.WARNING -> Timber.tag(TAG).w("JS: $msg")
                    else -> Timber.tag(TAG).d("JS: $msg")
                }

                if (msg.contains("Uncaught")) {
                    val fmt = "\"$msg\", source: ${m.sourceId()} (${m.lineNumber()})"
                    val exception = BadWebViewException(fmt)
                    Timber.tag(TAG).e("This WebView implementation is broken: $fmt")

                    onInitializationErrorCloseAndCancel(exception)
                    popAllPoTokenContinuations().forEach { (_, cont) -> cont.resumeWithException(exception) }
                }
                return super.onConsoleMessage(m)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                val didCrash = runCatching { detail.didCrash() }.getOrNull()
                Timber.tag(TAG).e("PoToken WebView render process gone (didCrash=$didCrash)")
                isDead = true
                val exception = PoTokenException("WebView render process gone (didCrash=$didCrash)")
                onInitializationErrorCloseAndCancel(exception)
                popAllPoTokenContinuations().forEach { (_, cont) ->
                    runCatching { cont.resumeWithException(exception) }
                }
                return true
            }
        }
    }

    
    private fun loadHtmlAndObtainBotguard() {
        Timber.tag(TAG).d("loadHtmlAndObtainBotguard() called")

        scope.launch(exceptionHandler) {
            val html = withContext(Dispatchers.IO) {
                webView.context.assets.open("po_token.html").bufferedReader().use { it.readText() }
            }

            
            val data = html.replaceFirst("</script>", "\n$JS_INTERFACE.downloadAndRunBotguard()</script>")
            webView.loadDataWithBaseURL("https://www.youtube.com", data, "text/html", "utf-8", null)
        }
    }

    
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        Timber.tag(TAG).d("downloadAndRunBotguard() called")

        makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/Create",
            "[ \"$REQUEST_KEY\" ]",
        ) { responseBody ->
            val parsedChallengeData = parseChallengeData(responseBody)
            webView.evaluateJavascript(
                """try {
                    data = $parsedChallengeData
                    runBotGuard(data).then(function (result) {
                        this.webPoSignalOutput = result.webPoSignalOutput
                        $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                    }, function (error) {
                        $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                    })
                } catch (error) {
                    $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                }""",
                null
            )
        }
    }

    
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG).e("Initialization error from JavaScript: $error")
        }
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(error))
    }

    
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        Timber.tag(TAG).d("botguardResponse: $botguardResponse")
        makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/GenerateIT",
            "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]",
        ) { responseBody ->
            Timber.tag(TAG).d("GenerateIT response: $responseBody")
            try {
                val (integrityToken, expirationTimeInSeconds) = parseIntegrityTokenData(responseBody)
                Timber.tag(TAG).d("Parsed integrityToken (${integrityToken.take(50)}...), expires in $expirationTimeInSeconds sec")

                
                expirationInstant = Instant.now().plusSeconds(expirationTimeInSeconds).minus(10, ChronoUnit.MINUTES)

                
                
                Timber.tag(TAG).d("Evaluating createPoTokenMinter JavaScript...")
                webView.evaluateJavascript(
                    """try {
                        console.log('[JS] Setting integrityToken and calling createPoTokenMinter...');
                        this.integrityToken = $integrityToken
                        console.log('[JS] integrityToken set, now calling createPoTokenMinter...');
                        createPoTokenMinter(webPoSignalOutput, integrityToken).then(function() {
                            console.log('[JS] createPoTokenMinter .then() resolved!');
                            $JS_INTERFACE.onMinterCreated()
                        }).catch(function(error) {
                            console.log('[JS] createPoTokenMinter .catch() error: ' + error);
                            $JS_INTERFACE.onJsInitializationError(error + "\n" + (error.stack || ''))
                        })
                    } catch (error) {
                        console.log('[JS] createPoTokenMinter SYNC error: ' + error);
                        $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                    }""",
                    null
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to parse integrity token data: ${e.message}")
                onInitializationErrorCloseAndCancel(PoTokenException("parseIntegrityTokenData failed: ${e.message}"))
            }
        }
    }
    
    @JavascriptInterface
    fun onMinterCreated() {
        Timber.tag(TAG).d("poToken minter created successfully, initialization complete")
        if (initResumed.compareAndSet(false, true)) {
            continuation.resume(this)
        }
    }
    

    
    suspend fun generatePoToken(identifier: String): String {
        if (isDead || closed) {
            throw PoTokenException("PoToken WebView is dead/closed")
        }
        return try {
            withTimeout(GENERATE_TIMEOUT_MS) {
                generatePoTokenInternal(identifier)
            }
        } catch (e: TimeoutCancellationException) {
            isDead = true
            Timber.tag(TAG).e("generatePoToken($identifier) timed out")
            throw PoTokenException("poToken generation timed out")
        }
    }

    private suspend fun generatePoTokenInternal(identifier: String): String {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                Timber.tag(TAG).d("generatePoToken() called with identifier $identifier")
                val reqId = getNextReqId()
                addPoTokenEmitter(reqId, cont)
                cont.invokeOnCancellation { popPoTokenContinuation(reqId) }
                
                webView.evaluateJavascript(
                    """try {
                        identifier = "$identifier"
                        u8Identifier = ${stringToU8(identifier)}
                        obtainPoToken(u8Identifier).then(function(poTokenU8) {
                            poTokenU8String = poTokenU8.join(",")
                            $JS_INTERFACE.onObtainPoTokenResult($reqId, identifier, poTokenU8String)
                        }).catch(function(error) {
                            $JS_INTERFACE.onObtainPoTokenError($reqId, identifier, error + "\n" + (error.stack || ''))
                        })
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError($reqId, identifier, error + "\n" + error.stack)
                    }""",
                    null
                )
            }
        }
    }

    
    @JavascriptInterface
    fun onObtainPoTokenError(reqId: Int, identifier: String, error: String) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG).e("obtainPoToken error from JavaScript: $error")
        }
        popPoTokenContinuation(reqId)?.resumeWithException(buildExceptionForJsError(error))
    }

    
    @JavascriptInterface
    fun onObtainPoTokenResult(reqId: Int, identifier: String, poTokenU8: String) {
        Timber.tag(TAG).d("Generated poToken (before decoding): identifier=$identifier poTokenU8=$poTokenU8")
        val poToken = try {
            u8ToBase64(poTokenU8)
        } catch (t: Throwable) {
            popPoTokenContinuation(reqId)?.resumeWithException(t)
            return
        }

        Timber.tag(TAG).d("Generated poToken: identifier=$identifier poToken=$poToken")
        popPoTokenContinuation(reqId)?.resume(poToken)
    }

    val isExpired: Boolean
        get() = Instant.now().isAfter(expirationInstant)
    

    
    private fun addPoTokenEmitter(reqId: Int, continuation: Continuation<String>) {
        poTokenContinuations[reqId] = continuation
    }

    private fun popPoTokenContinuation(reqId: Int): Continuation<String>? {
        return poTokenContinuations.remove(reqId)
    }

    private fun popAllPoTokenContinuations(): Map<Int, Continuation<String>> {
        val result = poTokenContinuations.toMap()
        poTokenContinuations.clear()
        return result
    }
    

    
    private fun makeBotguardServiceRequest(
        url: String,
        data: String,
        handleResponseBody: (String) -> Unit,
    ) {
        scope.launch(exceptionHandler) {
            val requestBuilder = okhttp3.Request.Builder()
                .post(data.toRequestBody())
                .headers(mapOf(
                    "User-Agent" to USER_AGENT,
                    "Accept" to "application/json",
                    "Content-Type" to "application/json+protobuf",
                    "x-goog-api-key" to GOOGLE_API_KEY,
                    "x-user-agent" to "grpc-web-javascript/0.1",
                ).toHeaders())
                .url(url)
            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(requestBuilder.build()).execute()
            }
            val httpCode = response.code
            if (httpCode != 200) {
                onInitializationErrorCloseAndCancel(PoTokenException("Invalid response code: $httpCode"))
            } else {
                val body = withContext(Dispatchers.IO) {
                    response.body!!.string()
                }
                handleResponseBody(body)
            }
        }
    }

    private fun onInitializationErrorCloseAndCancel(error: Throwable) {
        close()
        if (initResumed.compareAndSet(false, true)) {
            runCatching { continuation.resumeWithException(error) }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()

        val teardown = Runnable {
            runCatching {
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }.onFailure { Timber.tag(TAG).w("WebView teardown threw: $it") }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            teardown.run()
        } else {
            Handler(Looper.getMainLooper()).post(teardown)
        }
    }
    

    companion object {
        private const val TAG = "PoTokenWebView"
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"
        private const val GENERATE_TIMEOUT_MS = 10_000L

        private val httpClient = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()

        suspend fun getNewPoTokenGenerator(context: Context): PoTokenWebView {
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val potWv = PoTokenWebView(context, cont)
                    potWv.loadHtmlAndObtainBotguard()
                }
            }
        }
    }
}
