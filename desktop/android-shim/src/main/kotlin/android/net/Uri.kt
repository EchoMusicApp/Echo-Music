package android.net

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

// Desktop stand-in for android.net.Uri, backed by java.net.URI where the text parses and by plain string work where it does not.
class Uri private constructor(private val text: String) {
    private val parsed: URI? = runCatching { URI(text) }.getOrNull()

    val scheme: String? get() = parsed?.scheme ?: text.substringBefore("://", "").takeIf { it.isNotEmpty() && "://" in text }
    val host: String? get() = parsed?.host
    val authority: String? get() = parsed?.authority
    val path: String? get() = parsed?.path
    val query: String? get() = parsed?.query ?: text.substringAfter('?', "").takeIf { it.isNotEmpty() }
    val lastPathSegment: String? get() = path?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
    val schemeSpecificPart: String? get() = parsed?.schemeSpecificPart

    fun getQueryParameter(key: String): String? =
        query?.split('&')?.firstNotNullOfOrNull { p ->
            val k = p.substringBefore('=')
            if (URLDecoder.decode(k, "UTF-8") == key) URLDecoder.decode(p.substringAfter('=', ""), "UTF-8") else null
        }

    fun buildUpon(): Builder = Builder(text)

    override fun toString() = text
    override fun equals(other: Any?) = other is Uri && other.text == text
    override fun hashCode() = text.hashCode()

    // shortcut: the builder only appends query parameters and replaces the path, which is all the original code uses.
    class Builder internal constructor(private var text: String) {
        constructor() : this("")
        fun appendQueryParameter(key: String, value: String): Builder {
            val sep = if ('?' in text) "&" else "?"
            text += sep + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
            return this
        }
        fun build(): Uri = Uri(text)
    }

    companion object {
        val EMPTY = Uri("")
        @JvmStatic fun parse(s: String): Uri = Uri(s)
        @JvmStatic fun encode(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        @JvmStatic fun decode(s: String): String = URLDecoder.decode(s, "UTF-8")
    }
}
