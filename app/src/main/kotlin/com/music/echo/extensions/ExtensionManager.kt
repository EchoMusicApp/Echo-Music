package com.music.echo.extensions

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@Serializable
data class AddonEndpoint(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Serializable
data class AddonSearchResponse(
    val tracks: List<AddonTrack> = emptyList()
)

@Serializable
data class AddonTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Int? = null,
    val artworkURL: String? = null,
    val albumArtworkURL: String? = null
)

@Serializable
data class AddonStreamResponse(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    val streamQuality: String = "",
    val audioQuality: String = "",
    val codec: String? = null
)

@Serializable
data class AddonManifest(
    val id: String = "",
    val name: String = "",
    val version: String = ""
)

@Singleton
class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Addon requests run inside the player's blocking resolve path, so a
     * configured-but-unreachable host must fail fast instead of stacking several
     * default 10s timeouts per base and delaying the YouTube fallback.
     */
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .fastFallback(true)
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                val system = try {
                    okhttp3.Dns.SYSTEM.lookup(hostname)
                } catch (e: Exception) {
                    emptyList()
                }
                if (hostname.endsWith(".vercel.app", ignoreCase = true)) {
                    val vercelIps = listOfNotNull(
                        runCatching { java.net.InetAddress.getByName("76.76.21.21") }.getOrNull(),
                        runCatching { java.net.InetAddress.getByName("76.76.21.22") }.getOrNull()
                    )
                    return (vercelIps + system).distinct()
                }
                return system.ifEmpty { okhttp3.Dns.SYSTEM.lookup(hostname) }
            }
        })
        .build()
    private val ADDONS_KEY = stringPreferencesKey("extensions_addons_list")
    private val json = Json { ignoreUnknownKeys = true }

    fun sanitizeUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://$u"
        }
        u = u.removeSuffix("/")
        if (u.endsWith("/manifest.json", ignoreCase = true)) {
            u = u.substringBeforeLast("/manifest.json")
        }
        return u.removeSuffix("/")
    }

    suspend fun getAddons(): List<AddonEndpoint> {
        val jsonStr = context.dataStore.data.map { it[ADDONS_KEY] }.first() ?: "[]"
        return try {
            json.decodeFromString<List<AddonEndpoint>>(jsonStr).map {
                it.copy(url = sanitizeUrl(it.url))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun hasEnabledAddons(): Boolean = getAddons().any { it.enabled }

    suspend fun fetchAddonName(baseUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val manifestUrl = "${baseUrl.removeSuffix("/")}/manifest.json"
            val request = Request.Builder().url(manifestUrl).build()
            val response = httpClient.newCall(request).execute()
            response.use { res ->
                val body = res.body?.string()
                if (res.isSuccessful && !body.isNullOrEmpty()) {
                    val manifest = json.decodeFromString<AddonManifest>(body)
                    if (manifest.name.isNotBlank()) return@withContext manifest.name
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ExtensionManager", "Could not fetch manifest for $baseUrl: ${e.message}")
        }
        null
    }

    suspend fun addAddon(endpoint: AddonEndpoint) {
        val sanitizedUrl = sanitizeUrl(endpoint.url)
        val resolvedName = if (endpoint.name.isBlank()) {
            fetchAddonName(sanitizedUrl) ?: "Custom Addon"
        } else {
            endpoint.name
        }
        val current = getAddons().toMutableList()
        current.add(endpoint.copy(name = resolvedName, url = sanitizedUrl))
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }
    
    suspend fun toggleAddon(id: String, enabled: Boolean) {
        val current = getAddons().map { if (it.id == id) it.copy(enabled = enabled) else it }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun removeAddon(id: String) {
        val current = getAddons().filter { it.id != id }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    private fun baseUrlFor(addon: AddonEndpoint): String = sanitizeUrl(addon.url)

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    /**
     * Robust addon-track parsing. The public track id itself is base64url and
     * contains `_`/`-`, so a naive split("_") breaks. Match by addon id prefix.
     */
    private fun findAddonForFullId(
        fullId: String,
        addons: List<AddonEndpoint>
    ): Pair<AddonEndpoint, String>? {
        if (!fullId.startsWith("addon_")) return null
        val rest = fullId.removePrefix("addon_")
        // Longest addon-id match first so similar prefixes don't collide.
        for (addon in addons.sortedByDescending { it.id.length }) {
            if (rest == addon.id) return addon to ""
            if (rest.startsWith(addon.id + "_")) {
                return addon to rest.removePrefix(addon.id + "_")
            }
        }
        return null
    }

    private fun normalizeForMatch(value: String): String =
        value.lowercase()
            .replace(Regex("[\\p{Punct}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Strict replacement match. The old code returned the first addon result
     * when nothing matched, so track N+1 played track N audio. Now null means
     * "no usable replacement" and the caller falls back to YouTube.
     *
     * Three gates must pass before a catalogue track can replace the requested
     * one:
     *  - the base titles (title without version qualifiers/credits) must cover
     *    each other, so catalogue variants such as "Portami" vs "Portami via /
     *    Fabrizio Moro" still match;
     *  - any version qualifier the request carries ("remix", "live",
     *    "instrumental", ...) must also appear in the candidate, so the plain
     *    original is never substituted for a requested remix (and vice versa is
     *    still allowed, just scored lower);
     *  - when both sides name an artist, they must share an artist token. Artist
     *    agreement is a gate, not a bonus: this is what stops a same-titled song
     *    by a different artist from being matched.
     *
     * Plain substring containment is deliberately not used: "Yes" is a substring
     * of "Yesterday". A token pair matches only when one is a prefix of the other
     * and the shorter is at least [MIN_TOKEN_MATCH_LEN] characters. Duration only
     * breaks ties among otherwise equivalent candidates.
     */
    private fun selectBestMatch(
        tracks: List<AddonTrack>,
        title: String,
        artist: String,
        durationMs: Long?
    ): AddonTrack? {
        if (tracks.isEmpty()) return null
        val wantTokens = tokenize(title)
        if (wantTokens.isEmpty()) return null
        val wantQualifiers = wantTokens.filter { it in QUALIFIER_TOKENS }.toSet()
        val wantBase = wantTokens.filterNot { it in QUALIFIER_TOKENS || it in CREDIT_TOKENS }
        if (wantBase.isEmpty()) return null
        val artistTokens = tokenize(artist)

        var best: AddonTrack? = null
        var bestScore = Int.MIN_VALUE
        for (track in tracks) {
            if (track.id.isBlank() || track.title.isBlank()) continue
            val gotTokens = tokenize(track.title)
            if (gotTokens.isEmpty()) continue
            val gotQualifiers = gotTokens.filter { it in QUALIFIER_TOKENS }.toSet()
            val gotBase = gotTokens.filterNot { it in QUALIFIER_TOKENS || it in CREDIT_TOKENS }
            if (gotBase.isEmpty()) continue
            if (!coversAllTokens(gotBase, wantBase)) continue
            // A qualifier the user asked for must exist in the candidate: a remix
            // request must not be silently served the plain original.
            if (!wantQualifiers.all { wanted -> gotQualifiers.any { tokensMatch(it, wanted) } }) {
                continue
            }
            val artistMatched = artistMatches(artistTokens, tokenize(track.artist))
            // Never substitute a different artist when both sides name one.
            if (artistTokens.isNotEmpty() && !artistMatched) continue

            // Exact base agreement is the strongest signal; partial coverage is
            // tolerated for catalogue variants but ranks below it.
            var score =
                if (gotBase == wantBase) 100
                else (gotBase.count { got -> wantBase.any { tokensMatch(got, it) } }
                    .toFloat() / maxOf(gotBase.size, wantBase.size) * 60).toInt()
            if (artistMatched) score += 60
            if (gotQualifiers == wantQualifiers) score += 20
            else if (wantQualifiers.isEmpty()) score -= gotQualifiers.size * 5
            if (durationMs != null && durationMs > 0 && track.duration != null && track.duration > 0) {
                val diffMs = kotlin.math.abs(track.duration * 1000L - durationMs)
                if (diffMs <= 8_000L) score += 10 else if (diffMs <= 20_000L) score += 3
            }
            if (score > bestScore) {
                bestScore = score
                best = track
            }
        }
        return best
    }

    /** Normalizes then splits a value into matchable word tokens. */
    private fun tokenize(value: String): List<String> =
        normalizeForMatch(value).split(" ").filter { it.isNotBlank() }

    /**
     * True when the requested artist and the catalogue artist share at least one
     * significant token exactly. Generic function/credit markers ("the", "and",
     * "feat", ...) are ignored, otherwise "The Fray" and "The Band" would both
     * match on "the" and a same-titled song by another artist would pass. The
     * remaining tokens keep a 2-character minimum so meaningful short names such
     * as "dj", "mc", "lil" and "los" still match.
     */
    private fun artistMatches(want: List<String>, got: List<String>): Boolean {
        val wantSig = want.filter { it.length >= 2 && it !in ARTIST_STOP_TOKENS }
        val gotSig = got.filter { it.length >= 2 && it !in ARTIST_STOP_TOKENS }
        if (wantSig.isEmpty() || gotSig.isEmpty()) return false
        return wantSig.any { it in gotSig }
    }

    /**
     * True when every token of the shorter list is matched by a token of the
     * longer one. Extra words on the longer side are tolerated so catalogue
     * decorations ("Portami via /") do not prevent a match, but a token of the
     * shorter side with no counterpart does.
     */
    private fun coversAllTokens(shorter: List<String>, longer: List<String>): Boolean {
        if (shorter.isEmpty() || longer.isEmpty()) return false
        val (fewer, more) = if (shorter.size <= longer.size) shorter to longer else longer to shorter
        return fewer.all { token -> more.any { tokensMatch(token, it) } }
    }

    /**
     * Token match: equality, or a short inflection-style prefix ("love" vs
     * "lover", "run" vs "running"). A prefix must be at least
     * [MIN_TOKEN_MATCH_LEN] characters *and* the longer token may exceed it by at
     * most [MAX_PREFIX_SUFFIX_LEN] characters. The length minimum alone was not
     * enough: a three-character prefix like "yes" still matched "yesterday", so
     * the suffix bound is what actually rejects unrelated words.
     */
    private fun tokensMatch(a: String, b: String): Boolean {
        if (a == b) return true
        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length <= b.length) b else a
        if (shorter.length < MIN_TOKEN_MATCH_LEN) return false
        return longer.startsWith(shorter) &&
          longer.length - shorter.length <= MAX_PREFIX_SUFFIX_LEN
    }

    suspend fun search(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val addons = getAddons().filter { it.enabled }
        val results = mutableListOf<SongItem>()
        
        for (addon in addons) {
            try {
                val baseUrl = baseUrlFor(addon)
                val searchUrl = "${baseUrl.removeSuffix("/")}/search?q=${encodeQuery(query)}"
                val request = Request.Builder().url(searchUrl).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                    val addonResults = addonResponse.tracks
                    results.addAll(addonResults.map { 
                        SongItem(
                            id = "addon_${addon.id}_${it.id}",
                            title = it.title,
                            artists = listOf(Artist(name = it.artist, id = null)),
                            thumbnail = it.artworkURL ?: it.albumArtworkURL ?: "",
                            duration = it.duration
                        )
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results
    }

    suspend fun getStreamUrl(fullId: String): String? = withContext(Dispatchers.IO) {
        // fullId format: addon_{addonId}_{trackId}; trackId itself may contain "_".
        val addons = getAddons().filter { it.enabled }
        val (addon, trackId) = findAddonForFullId(fullId, addons) ?: return@withContext null
        if (trackId.isBlank()) return@withContext null
        
        try {
            val baseUrl = baseUrlFor(addon)
            // BitChord contract is /stream/:id (path param), not /stream?id=.
            // Encoded as a single path segment so ? # / in ids can't split the URL.
            val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/${Uri.encode(trackId)}?quality=LOSSLESS"
            val request = Request.Builder().url(streamUrlReq).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val streamRes: AddonStreamResponse = json.decodeFromString(body)
                return@withContext streamRes.url
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun cleanTitleForSearch(title: String): String {
        return title
            .replace(Regex("(?i)\\s*\\((feat\\.|ft\\.|featuring)\\s+[^)]+\\)"), "")
            .replace(Regex("(?i)\\s*\\[(feat\\.|ft\\.|featuring)\\s+[^]]+\\]"), "")
            .replace(Regex("(?i)\\s*-\\s*(official\\s+video|official\\s+audio|audio|video|lyric\\s+video)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?i)\\s*\\((official\\s+video|official\\s+audio|audio|video|lyrics?)\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?i)\\s*\\[(official\\s+video|official\\s+audio|audio|video|lyrics?)\\]", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Resolves a playable add-on stream for the requested track.
     *
     * When [preferredAddonId] identifies an enabled provider, only that provider is queried.
     * A missing or disabled preferred provider is treated as Automatic and searches all enabled
     * providers. A miss returns null rather than silently selecting a different source.
     */
    suspend fun resolveStream(
        title: String?,
        artist: String?,
        duration: Long?,
        videoId: String,
        preferredAddonId: String? = null,
    ): AddonStreamResponse? = withContext(Dispatchers.IO) {
        val addons = getAddons().filter { it.enabled }
        val preferredAddon = addons.firstOrNull { it.id == preferredAddonId }
        val selectedAddons = if (preferredAddon == null) addons else listOf(preferredAddon)
        if (selectedAddons.isEmpty()) return@withContext null

        if (videoId.startsWith("addon_")) {
            val directMatch = findAddonForFullId(videoId, selectedAddons)
            if (directMatch != null && directMatch.second.isNotBlank()) {
                val (targetAddon, trackId) = directMatch
                val streamUrlReq =
                    "${baseUrlFor(targetAddon).removeSuffix("/")}/stream/${Uri.encode(trackId)}?quality=LOSSLESS"
                return@withContext fetchStream(streamUrlReq)
            } else if (preferredAddonId == null) {
                return@withContext null
            }
            // When another provider is explicitly selected, the original add-on
            // ID cannot be reused. Continue below and match the queue metadata.
        }

        if (title == null || artist == null) return@withContext null

        val cleanTitle = cleanTitleForSearch(title)
        val primaryArtist = artist.split(",", "&", "•", ";", "/").firstOrNull()?.trim() ?: artist
        val candidateQueries = buildList {
            val q1 = "$cleanTitle $primaryArtist".trim()
            add(q1)
            val q2 = "$title $artist".trim()
            if (q2 != q1) add(q2)
        }

        for (addon in selectedAddons) {
            val baseUrl = baseUrlFor(addon)
            // BitChord source prefixes can fail independently. Preserve its
            // root-provider retry behavior for both automatic and manual picks.
            val searchBases = buildList {
                add(baseUrl)
                val hostRoot = stripSourcePrefix(baseUrl)
                if (hostRoot != null) {
                    BASE_ALTERNATIVES.forEach { alternative ->
                        val candidate = hostRoot + alternative
                        if (candidate != baseUrl && candidate !in this) add(candidate)
                    }
                }
            }

            for (searchBase in searchBases) {
                var match: AddonTrack? = null
                for (query in candidateQueries) {
                    android.util.Log.d("ExtensionManager", "Searching add-on $searchBase for $query")
                    val searchUrl = "${searchBase.removeSuffix("/")}/search?q=${encodeQuery(query)}"
                    val searchResponse = try {
                        httpClient.newCall(Request.Builder().url(searchUrl).build()).execute()
                    } catch (e: Exception) {
                        if (isHostUnreachable(e)) {
                            android.util.Log.e("ExtensionManager", "Add-on connection failed for $searchBase", e)
                            break
                        }
                        android.util.Log.e("ExtensionManager", "Add-on search request failed for $searchBase", e)
                        continue
                    }

                    val currentMatch = try {
                        searchResponse.use { response ->
                            val body = response.body?.string()
                            if (!response.isSuccessful || body.isNullOrEmpty()) {
                                android.util.Log.e("ExtensionManager", "Search failed: ${response.code} from $searchBase")
                                null
                            } else {
                                selectBestMatch(
                                    json.decodeFromString<AddonSearchResponse>(body).tracks,
                                    title,
                                    artist,
                                    duration,
                                )
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ExtensionManager", "Add-on response failed for $searchBase", e)
                        null
                    }

                    if (currentMatch != null) {
                        match = currentMatch
                        break
                    }
                }

                val matchedTrack = match
                if (matchedTrack == null) {
                    android.util.Log.e("ExtensionManager", "No reliable add-on match for $title - $artist on $searchBase")
                    continue
                }
                android.util.Log.d("ExtensionManager", "Matched track: ${matchedTrack.title} by ${matchedTrack.artist}. Fetching stream...")
                val streamUrlReq =
                    "${searchBase.removeSuffix("/")}/stream/${Uri.encode(matchedTrack.id)}?quality=LOSSLESS"
                val streamData = fetchStream(streamUrlReq)
                if (streamData != null) return@withContext streamData
            }
        }
        null
    }

    private fun fetchStream(url: String): AddonStreamResponse? =
        try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    json.decodeFromString<AddonStreamResponse>(body)
                } else {
                    android.util.Log.e("ExtensionManager", "Stream request failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExtensionManager", "Add-on stream request failed", e)
            null
        }

    /**
     * True when [e], thrown by `Call.execute()`, means the host itself could not be
     * reached (DNS, refused, no route, connect timeout). Only these apply to every
     * base on the same host; read timeouts, TLS and protocol errors come from a
     * reachable server and may not affect the other prefixes.
     */
    private fun isHostUnreachable(e: Exception): Boolean = when (e) {
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.NoRouteToHostException -> true
        // OkHttp reports a connect timeout as SocketTimeoutException("connect timed out"),
        // the same type as a read timeout, so tell them apart by message.
        is java.net.SocketTimeoutException -> e.message?.contains("connect", ignoreCase = true) == true
        else -> false
    }

    /**
     * Removes a trailing BitChord source prefix ("/a", "/b", "/c") from a base URL.
     *
     * Only the last path segment is considered: a plain substring search would
     * also match inside the scheme ("https://host" contains "/a" in "//a…"), which
     * truncated the URL to "https:/" and produced unusable candidates.
     */
    private fun stripSourcePrefix(baseUrl: String): String? {
        val trimmed = baseUrl.removeSuffix("/")
        val lastSlash = trimmed.lastIndexOf('/')
        if (lastSlash < 0) return null
        val schemeEnd = trimmed.indexOf("://")
        // Do not treat the scheme separator as a path segment boundary.
        if (schemeEnd >= 0 && lastSlash <= schemeEnd + 2) return null
        val lastSegment = trimmed.substring(lastSlash + 1)
        return if (SOURCE_PREFIXES.contains(lastSegment)) {
            trimmed.substring(0, lastSlash)
        } else {
            null
        }
    }

    private companion object {
        /** Isolated BitChord source prefixes tried when the configured one fails. */
        val BASE_ALTERNATIVES = listOf("", "/c", "/a")
        val SOURCE_PREFIXES = setOf("a", "b", "c")
        /** Shortest token pair accepted as a title match, so "yes" != "yesterday". */
        const val MIN_TOKEN_MATCH_LEN = 3

        /**
         * Longest suffix a longer token may have over a shorter prefix token for
         * the pair to still count as the same word ("love"/"lover" = 1,
         * "run"/"running" = 4). Bounds "yes" vs "yesterday" (6) out.
         */
        const val MAX_PREFIX_SUFFIX_LEN = 4

        /**
         * Tokens that describe a specific rendition of a song. Unless the user
         * requested the same qualifier, a candidate carrying it is not the same
         * audio, so the request must not be satisfied by a different version.
         */
        val QUALIFIER_TOKENS = setOf(
            "remix", "rmx", "remaster", "remastered", "live", "instrumental",
            "acoustic", "extended", "radio", "edit", "demo", "deluxe", "version",
            "sped", "slowed", "reverb", "cover", "karaoke", "mix", "unplugged",
        )

        /** Credit markers that decorate a title without changing the song. */
        val CREDIT_TOKENS = setOf("feat", "ft", "featuring", "prod")

        /**
         * Tokens that are too generic to prove two artists are the same. Excluded
         * from the artist gate so "The Fray" cannot match "The Band" on "the".
         */
        val ARTIST_STOP_TOKENS =
            setOf("the", "and", "of", "with", "feat", "ft", "featuring", "prod", "vs")
    }
}
