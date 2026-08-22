package echo.music.iad1tya.utils.cipher

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object CipherDeobfuscator {
    private const val TAG = "echomusic_CipherDeobfusc"

    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        PlayerConfigStore.initialize(appContext)
        PlayerConfigStore.scheduleStartupRefresh()
    }

    private val rendererRecoveryPolicy = RendererRecoveryPolicy()
    private var cipherWebView: CipherWebView? = null
    private var currentPlayerHash: String? = null
    private var builtConfigEpoch = -1

    suspend fun prewarm() {
        try {
            getOrCreateWebView(forceRefresh = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: CipherRendererGoneException) {
            onRendererGone(e, "prewarm")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cipher WebView prewarm failed")
        }
    }

    suspend fun deobfuscateStreamUrl(signatureCipher: String, videoId: String): String? {
        return try {
            deobfuscateInternal(signatureCipher, videoId, isRetry = false)
                ?.also { rendererRecoveryPolicy.onSuccess() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: CipherRendererGoneException) {
            onRendererGone(e, "deobfuscate")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cipher deobfuscation failed, retrying with fresh JS: ${e.message}")
            try {
                PlayerJsFetcher.invalidateCache()
                closeWebView()
                deobfuscateInternal(signatureCipher, videoId, isRetry = true)
                    ?.also { rendererRecoveryPolicy.onSuccess() }
            } catch (retryE: CancellationException) {
                throw retryE
            } catch (retryE: CipherRendererGoneException) {
                onRendererGone(retryE, "deobfuscate-retry")
                null
            } catch (retryE: Exception) {
                Timber.tag(TAG).e(retryE, "Cipher deobfuscation retry also failed: ${retryE.message}")
                null
            }
        }
    }

    suspend fun onStreamRejected(): Boolean = PlayerConfigStore.refreshAfterStreamRejection()

    private suspend fun deobfuscateInternal(signatureCipher: String, videoId: String, isRetry: Boolean): String? {
        
        val params = parseQueryParams(signatureCipher)
        val obfuscatedSig = params["s"]
        val sigParam = params["sp"] ?: "signature"
        val baseUrl = params["url"]

        if (obfuscatedSig == null || baseUrl == null) {
            Timber.tag(TAG).e("Could not parse signatureCipher params: s=${obfuscatedSig != null}, url=${baseUrl != null}")
            return null
        }

        Timber.tag(TAG).d("Deobfuscating cipher for $videoId: sig=${obfuscatedSig.take(20)}..., sp=$sigParam")

        val webView = getOrCreateWebView(forceRefresh = isRetry)
            ?: return null

        
        val deobfuscatedSig = webView.deobfuscateSignature(obfuscatedSig)

        
        val separator = if ("?" in baseUrl) "&" else "?"
        val finalUrl = "$baseUrl${separator}${sigParam}=${Uri.encode(deobfuscatedSig)}"

        Timber.tag(TAG).d("Custom cipher deobfuscation succeeded for $videoId")
        return finalUrl
    }

    
    suspend fun transformNParamInUrl(url: String): String {
        return try {
            transformNInternal(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: CipherRendererGoneException) {
            onRendererGone(e, "n-transform")
            url
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "N-transform failed, returning original URL: ${e.message}")
            url
        }
    }

    private suspend fun transformNInternal(url: String): String {
        
        val nMatch = Regex("[?&]n=([^&]+)").find(url)
        if (nMatch == null) {
            Timber.tag(TAG).d("No 'n' parameter found in URL, skipping transform")
            return url
        }
        val nValue = Uri.decode(nMatch.groupValues[1])
        Timber.tag(TAG).d("N-param found: $nValue")

        val webView = getOrCreateWebView(forceRefresh = false) ?: return url

        if (!webView.nFunctionAvailable) {
            Timber.tag(TAG).e("N-transform function was not discovered at init time")
            return url
        }

        val transformedN = webView.transformN(nValue)
        rendererRecoveryPolicy.onSuccess()
        Timber.tag(TAG).d("N-param transformed: $nValue -> $transformedN")

        
        return url.replaceFirst(
            Regex("([?&])n=[^&]+"),
            "$1n=${Uri.encode(transformedN)}"
        )
    }

    private suspend fun onRendererGone(e: CipherRendererGoneException, where: String) {
        rendererRecoveryPolicy.onFailure(SystemClock.elapsedRealtime())
        Timber.tag(TAG).e(e, "WebView renderer gone during $where (consecutive failures: ${rendererRecoveryPolicy.consecutiveFailures}) — dropping cipher WebView")
        closeWebView()
    }

    private suspend fun getOrCreateWebView(forceRefresh: Boolean): CipherWebView? {
        if (cipherWebView?.isDead == true) {
            Timber.tag(TAG).w("Cached cipher WebView renderer is dead — discarding")
            closeWebView()
        }

        if (!rendererRecoveryPolicy.shouldAttempt(SystemClock.elapsedRealtime())) {
            Timber.tag(TAG).w("Skipping cipher WebView creation: ${rendererRecoveryPolicy.consecutiveFailures} consecutive renderer deaths, in backoff window")
            return null
        }

        val epochAtStart = PlayerConfigStore.configEpoch
        if (!forceRefresh && cipherWebView != null && builtConfigEpoch == epochAtStart) {
            return cipherWebView
        }

        var builtEpoch = epochAtStart

        
        if (cipherWebView != null) {
            closeWebView()
        }

        
        val result = PlayerJsFetcher.getPlayerJs(forceRefresh = forceRefresh)
        if (result == null) {
            Timber.tag(TAG).e("Failed to get player JS")
            return null
        }
        val (playerJs, hash) = result
        val playerHash = FunctionNameExtractor.extractPlayerHash(playerJs)
        var sigInfo = FunctionNameExtractor.extractSigFunctionInfo(playerJs)
        var nFuncInfo = FunctionNameExtractor.extractNFunctionInfo(playerJs)

        if (playerHash != null) {
            val hardcoded = FunctionNameExtractor.getHardcodedConfig(playerHash)
            if (hardcoded != null) {
                sigInfo = FunctionNameExtractor.SigFunctionInfo(
                    name = hardcoded.sigFuncName,
                    constantArg = hardcoded.sigConstantArg,
                    jsExpression = hardcoded.sigJsExpression,
                    isHardcoded = true
                )
                nFuncInfo = FunctionNameExtractor.NFunctionInfo(
                    name = hardcoded.nFuncName,
                    arrayIndex = hardcoded.nArrayIndex,
                    jsExpression = hardcoded.nJsExpression,
                    isHardcoded = true
                )
            } else if (!forceRefresh) {
                Timber.tag(TAG).w("Extraction not fully config-backed for player $playerHash — forcing remote config refresh")
                val healed = PlayerConfigStore.forceRefresh(missingHash = playerHash)
                if (healed) {
                    val newHardcoded = FunctionNameExtractor.getHardcodedConfig(playerHash)
                    if (newHardcoded != null) {
                        sigInfo = FunctionNameExtractor.SigFunctionInfo(
                            name = newHardcoded.sigFuncName,
                            constantArg = newHardcoded.sigConstantArg,
                            jsExpression = newHardcoded.sigJsExpression,
                            isHardcoded = true
                        )
                        nFuncInfo = FunctionNameExtractor.NFunctionInfo(
                            name = newHardcoded.nFuncName,
                            arrayIndex = newHardcoded.nArrayIndex,
                            jsExpression = newHardcoded.nJsExpression,
                            isHardcoded = true
                        )
                        builtEpoch = PlayerConfigStore.configEpoch
                    }
                }
            }
        }

        if (sigInfo == null) {
            Timber.tag(TAG).e("Could not extract signature function info from player JS")
            return null
        }

        val webView = CipherWebView.create(
            context = appContext,
            playerJs = playerJs,
            sigInfo = sigInfo,
            nFuncInfo = nFuncInfo,
        )

        cipherWebView = webView
        currentPlayerHash = playerHash ?: hash
        builtConfigEpoch = builtEpoch
        return webView
    }

    private suspend fun closeWebView() {
        withContext(Dispatchers.Main) {
            runCatching { cipherWebView?.close() }
                .onFailure { Timber.tag(TAG).w("closeWebView threw: $it") }
        }
        cipherWebView = null
        currentPlayerHash = null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = Uri.decode(pair.substring(0, idx))
                val value = Uri.decode(pair.substring(idx + 1))
                result[key] = value
            }
        }
        return result
    }
}
