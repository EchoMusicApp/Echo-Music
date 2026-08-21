package echo.music.iad1tya.ui.screens.settings

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordTokenWebView(
    onTokenExtracted: (String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface
                    fun onToken(token: String?) {
                        if (!token.isNullOrBlank() && token != "null") {
                            val cleanToken = token.replace("\"", "")
                            if (cleanToken.isNotBlank()) {
                                post { onTokenExtracted(cleanToken) }
                            }
                        }
                    }
                }, "DiscordInterface")

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        if (url.contains("discord.com/channels") || url.contains("discord.com/app")) {
                            view.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        var iframe = document.createElement('iframe');
                                        document.head.append(iframe);
                                        var pd = Object.getOwnPropertyDescriptor(iframe.contentWindow, 'localStorage');
                                        iframe.remove();
                                        Object.defineProperty(window, 'localStorage', pd);
                                        var t = window.localStorage.getItem('token');
                                        if (t) {
                                            DiscordInterface.onToken(t);
                                        }
                                    } catch(e) {
                                        DiscordInterface.onToken(null);
                                    }
                                })();
                                """.trimIndent(),
                                null
                            )
                        }
                    }
                }
                loadUrl("https://discord.com/login")
            }
        },
        update = { webView ->
            // Update block if needed
        }
    )
}
