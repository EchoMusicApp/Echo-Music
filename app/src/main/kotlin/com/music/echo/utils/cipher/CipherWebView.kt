package echo.music.iad1tya.utils.cipher

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CipherWebView private constructor(
    context: Context,
    private val playerJs: String,
    private val sigInfo: FunctionNameExtractor.SigFunctionInfo?,
    private val nFuncInfo: FunctionNameExtractor.NFunctionInfo?,
    initContinuation: Continuation<CipherWebView>,
) {
    private val webView = WebView(context)

    private var initContinuation: Continuation<CipherWebView>? = initContinuation
    private val sigSlot = RequestSlot<String>()
    private val nSlot = RequestSlot<String>()

    private class RequestSlot<T> {
        private var continuation: Continuation<T>? = null
        private var requestId = 0

        @Synchronized
        fun arm(cont: Continuation<T>): Int {
            continuation = cont
            return ++requestId
        }

        @Synchronized
        fun takeIfCurrent(id: Int): Continuation<T>? =
            if (id == requestId) continuation.also { continuation = null } else null

        @Synchronized
        fun takeAny(): Continuation<T>? = continuation.also { continuation = null }
    }

    @Volatile
    var isDead: Boolean = false
        private set

    @Volatile
    private var destroyed = false

    @Volatile
    var nFunctionAvailable: Boolean = false
        private set

    @Volatile
    var discoveredNFuncName: String? = null
        private set

    init {
        val settings = webView.settings
        @Suppress("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        settings.blockNetworkLoads = true

        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webViewClient = object : WebViewClient() {
            @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                Timber.tag(TAG).e("=== RENDER PROCESS GONE === didCrash=${runCatching { detail.didCrash() }.getOrNull()}")
                onRendererGone("WebView render process gone (didCrash=${runCatching { detail.didCrash() }.getOrNull()})")
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught") && !m.message().contains("is not defined")) {
                    Timber.tag(TAG).e("WebView JS error: ${m.message()} at ${m.sourceId()}:${m.lineNumber()}")
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    private fun onRendererGone(reason: String) {
        isDead = true
        val e = CipherRendererGoneException(reason)
        takeInitContinuation()?.resumeSafely { it.resumeWithException(e) }
        sigSlot.takeAny()?.resumeSafely { it.resumeWithException(e) }
        nSlot.takeAny()?.resumeSafely { it.resumeWithException(e) }
        destroyWebView()
    }

    @Synchronized
    private fun takeInitContinuation(): Continuation<CipherWebView>? =
        initContinuation.also { initContinuation = null }

    private inline fun <T> T.resumeSafely(block: (T) -> Unit) {
        runCatching { block(this) }
    }

    private fun loadPlayerJsFromFile() {
        val sigFuncName = sigInfo?.name
        val nFuncName = nFuncInfo?.name
        val nArrayIdx = nFuncInfo?.arrayIndex
        Timber.tag(TAG).d("Loading player JS from file (${playerJs.length} chars), exporting sig=$sigFuncName, nFunc=$nFuncName[$nArrayIdx]")

        val isHardcoded = sigInfo?.isHardcoded == true || nFuncInfo?.isHardcoded == true

        val exports = buildList {
            val sigJsExpr = sigInfo?.jsExpression
            if (sigJsExpr != null) {
                val expr = sigJsExpr.replace("INPUT", "sig")
                add("window._cipherSigFunc = function(sig) { try { return $expr; } catch(e) { return null; } };")
            } else if (sigFuncName != null) {
                add("window._cipherSigFunc = typeof $sigFuncName !== 'undefined' ? $sigFuncName : null;")
            }

            val nJsExpr = nFuncInfo?.jsExpression
            if (nJsExpr != null) {
                val expr = nJsExpr.replace("INPUT", "n")
                add("window._nTransformFunc = function(n) { try { return $expr; } catch(e) { return n; } };")
            } else if (nFuncName != null) {
                val nExpr = if (nArrayIdx != null) {
                    "$nFuncName[$nArrayIdx]"
                } else {
                    nFuncName
                }
                add("window._nTransformFunc = typeof $nFuncName !== 'undefined' ? $nExpr : null;")
            }
        }

        val modifiedJs = if (exports.isNotEmpty()) {
            val exportCode = "; " + exports.joinToString(" ")
            playerJs.replace("})(_yt_player);", "$exportCode })(_yt_player);")
        } else {
            playerJs
        }

        val cacheDir = File(webView.context.cacheDir, "cipher")
        cacheDir.mkdirs()
        File(cacheDir, "player.js").writeText(modifiedJs)

        val html = """<!DOCTYPE html>
<html><head><script>
function deobfuscateSig(reqId, funcName, constantArg, obfuscatedSig) {
    try {
        var func = window._cipherSigFunc;
        if (typeof func !== 'function') {
            CipherBridge.onSigError("Sig func not found on window (type: " + typeof func + ")");
            return;
        }
        var result;
        if (func.length === 1) {
            result = func(obfuscatedSig);
        } else if (constantArg !== null && constantArg !== undefined) {
            result = func(constantArg, obfuscatedSig);
        } else {
            result = func(obfuscatedSig);
        }
        if (result === undefined || result === null) {
            CipherBridge.onSigError(reqId, "Function returned null/undefined");
            return;
        }
        CipherBridge.onSigResult(reqId, String(result));
    } catch (error) {
        CipherBridge.onSigError(reqId, error + "\n" + (error.stack || ""));
    }
}

function transformN(reqId, nValue) {
    try {
        var func = window._nTransformFunc;
        if (typeof func !== 'function') {
            CipherBridge.onNError("N-transform func not available (type: " + typeof func + ")");
            return;
        }
        var result = func(nValue);
        if (result === undefined || result === null) {
            CipherBridge.onNError(reqId, "N-transform returned null/undefined");
            return;
        }
        CipherBridge.onNResult(reqId, String(result));
    } catch (error) {
        CipherBridge.onNError(reqId, error + "\n" + (error.stack || ""));
    }
}

function discoverAndInit() {
    var nFuncName = "";
    var info = "";
    if (typeof window._nTransformFunc === 'function') {
        try {
            var testResult = window._nTransformFunc("test_abc");
            if (typeof testResult === 'string' && testResult !== "test_abc") {
                nFuncName = "injected_from_iife";
                info = "exported_ok,test_result=" + testResult.substring(0, 30);
            } else {
                info = "exported_but_bad_result:" + typeof testResult;
                window._nTransformFunc = null;
            }
        } catch(e) {
            info = "exported_but_threw:" + e;
            window._nTransformFunc = null;
        }
    }

    if (!nFuncName) {
        try {
            var testInput = "T2Xw3pWQ_Wk0xbOg";
            var keys = Object.getOwnPropertyNames(window);
            var tested = 0;
            var candidates = [];
            for (var i = 0; i < keys.length; i++) {
                try {
                    var key = keys[i];
                    if (key.startsWith("webkit") || key === "CipherBridge" || key === "_cipherSigFunc" || key === "_nTransformFunc") continue;
                    var fn = window[key];
                    if (typeof fn !== 'function' || fn.length !== 1) continue;
                    tested++;
                    var result = fn(testInput);
                    if (typeof result === 'string' && result !== testInput && result.length > 5) {
                        candidates.push(key + ":" + result.substring(0, 50));
                        if (result.indexOf('_w8_') >= 0) {
                            window._nTransformFunc = fn;
                            nFuncName = key;
                            break;
                        }
                    }
                } catch(e) {}
            }
            info = "brute_force:tested=" + tested + "/" + keys.length;
            if (!nFuncName && candidates.length > 0) {
                info += ",near_misses=" + candidates.slice(0, 5).join("|");
            }
        } catch(e) {
            info = "brute_force_error:" + e;
        }
    }
    CipherBridge.onNDiscoveryDone(nFuncName, info);
    CipherBridge.onPlayerJsLoaded();
}
</script>
<script src="player.js"
    onload="discoverAndInit()"
    onerror="CipherBridge.onPlayerJsError('Failed to load player.js from file')">
</script>
</head><body></body></html>"""

        webView.loadDataWithBaseURL(
            "file://${cacheDir.absolutePath}/",
            html, "text/html", "utf-8", null
        )
    }

    @JavascriptInterface
    fun onNDiscoveryDone(funcName: String, info: String) {
        if (funcName.isNotEmpty()) {
            Timber.tag(TAG).d("N-function DISCOVERED: $funcName ($info)")
            discoveredNFuncName = funcName
            nFunctionAvailable = true
        } else {
            Timber.tag(TAG).e("N-function NOT found ($info)")
            nFunctionAvailable = false
        }
    }

    @JavascriptInterface
    fun onPlayerJsLoaded() {
        Timber.tag(TAG).d("Player JS loaded, n-func=${discoveredNFuncName ?: "none"}")
        takeInitContinuation()?.resumeSafely { it.resume(this) }
    }

    @JavascriptInterface
    fun onPlayerJsError(error: String) {
        Timber.tag(TAG).e("Player JS load error: $error")
        takeInitContinuation()?.resumeSafely {
            it.resumeWithException(CipherException("Player JS load failed: $error"))
        }
    }

    suspend fun deobfuscateSignature(obfuscatedSig: String): String {
        if (sigInfo == null) {
            throw CipherException("Signature function info not available")
        }

        throwIfDead()
        return try {
            withTimeout(EVAL_TIMEOUT_MS) {
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        val reqId = sigSlot.arm(cont)
                        val constArgJs = if (sigInfo.constantArg != null) "${sigInfo.constantArg}" else "null"
                        webView.evaluateJavascript(
                            "deobfuscateSig('$reqId', '${sigInfo.name}', $constArgJs, '${escapeJsString(obfuscatedSig)}')",
                            null
                        )
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).e("Sig deobfuscation timed out after ${EVAL_TIMEOUT_MS}ms")
            failAsRendererGone("Sig deobfuscation timed out after ${EVAL_TIMEOUT_MS}ms")
        }
    }

    @JavascriptInterface
    fun onSigResult(reqId: Int, result: String) {
        Timber.tag(TAG).d("Signature deobfuscated: ${result.take(30)}...")
        sigSlot.takeIfCurrent(reqId)?.resumeSafely { it.resume(result) }
    }

    @JavascriptInterface
    fun onSigError(reqId: Int, error: String) {
        Timber.tag(TAG).e("Signature deobfuscation error: $error")
        sigSlot.takeIfCurrent(reqId)?.resumeSafely { it.resumeWithException(CipherException("Sig deobfuscation failed: $error")) }
    }

    suspend fun transformN(nValue: String): String {
        if (!nFunctionAvailable) {
            throw CipherException("N-transform function not discovered")
        }

        throwIfDead()
        return try {
            withTimeout(EVAL_TIMEOUT_MS) {
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        val reqId = nSlot.arm(cont)
                        webView.evaluateJavascript(
                            "transformN('$reqId', '${escapeJsString(nValue)}')",
                            null
                        )
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).e("N-transform timed out after ${EVAL_TIMEOUT_MS}ms")
            failAsRendererGone("N-transform timed out after ${EVAL_TIMEOUT_MS}ms")
        }
    }

    @JavascriptInterface
    fun onNResult(reqId: Int, result: String) {
        Timber.tag(TAG).d("N-transform result: ${result.take(50)}...")
        nSlot.takeIfCurrent(reqId)?.resumeSafely { it.resume(result) }
    }

    @JavascriptInterface
    fun onNError(reqId: Int, error: String) {
        Timber.tag(TAG).e("N-transform error: $error")
        nSlot.takeIfCurrent(reqId)?.resumeSafely { it.resumeWithException(CipherException("N-transform failed: $error")) }
    }

    private fun throwIfDead() {
        if (isDead) {
            throw CipherRendererGoneException("CipherWebView renderer is gone")
        }
    }

    private fun failAsRendererGone(reason: String): Nothing {
        isDead = true
        sigSlot.takeAny()
        nSlot.takeAny()
        throw CipherRendererGoneException(reason)
    }

    fun close() {
        destroyWebView()
        Timber.tag(TAG).d("CipherWebView closed")
    }

    private fun destroyWebView() {
        if (destroyed) return
        destroyed = true
        runCatching {
            webView.clearHistory()
            webView.clearCache(true)
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }.onFailure { Timber.tag(TAG).w("WebView teardown threw: $it") }
    }

    private fun escapeJsString(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    companion object {
        private const val TAG = "echomusic_CipherWebView"
        private const val JS_INTERFACE = "CipherBridge"
        private const val CREATE_TIMEOUT_MS = 30_000L
        private const val EVAL_TIMEOUT_MS = 15_000L

        suspend fun create(
            context: Context,
            playerJs: String,
            sigInfo: FunctionNameExtractor.SigFunctionInfo?,
            nFuncInfo: FunctionNameExtractor.NFunctionInfo? = null,
        ): CipherWebView {
            var created: CipherWebView? = null
            try {
                return withTimeout(CREATE_TIMEOUT_MS) {
                    withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { cont ->
                            val wv = CipherWebView(context, playerJs, sigInfo, nFuncInfo, cont)
                            created = wv
                            wv.loadPlayerJsFromFile()
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.tag(TAG).e("CipherWebView init timed out after ${CREATE_TIMEOUT_MS}ms")
                destroyQuietly(created)
                throw CipherRendererGoneException("CipherWebView init timed out")
            } catch (e: CancellationException) {
                destroyQuietly(created)
                throw e
            }
        }

        private suspend fun destroyQuietly(wv: CipherWebView?) {
            if (wv == null) return
            withContext(NonCancellable + Dispatchers.Main) {
                wv.isDead = true
                wv.takeInitContinuation()
                wv.destroyWebView()
            }
        }
    }
}

class CipherException(message: String) : Exception(message)
class CipherRendererGoneException(message: String) : Exception(message)
