

package iad1tya.echo.music.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import iad1tya.echo.music.utils.BotDetectionMitigator
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.music.innertube.models.YouTubeClient.Companion.IOS
import com.music.innertube.models.YouTubeClient.Companion.IPADOS
import com.music.innertube.models.YouTubeClient.Companion.MOBILE
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.music.innertube.models.YouTubeClient.Companion.WEB
import com.music.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.music.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.music.innertube.models.response.PlayerResponse
import iad1tya.echo.music.constants.AudioQuality
import iad1tya.echo.music.utils.cipher.CipherDeobfuscator
import iad1tya.echo.music.utils.YTPlayerUtils.MAIN_CLIENT
import iad1tya.echo.music.utils.YTPlayerUtils.STREAM_FALLBACK_CLIENTS
import iad1tya.echo.music.utils.YTPlayerUtils.validateStatus
import iad1tya.echo.music.utils.potoken.PoTokenGenerator
import iad1tya.echo.music.utils.potoken.PoTokenResult
import iad1tya.echo.music.utils.sabr.EjsNTransformSolver
import iad1tya.echo.music.utils.PlaybackLogLevel
import iad1tya.echo.music.utils.PlaybackLogManager
import com.music.innertube.models.IpVersion
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.flow.first

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                return when (YouTube.ipVersion) {
                    IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                    IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                    IpVersion.AUTO -> addresses
                }
            }
        })
        .proxySelector(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = listOfNotNull(YouTube.proxy ?: Proxy.NO_PROXY)
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Timber.tag(TAG).e(ioe, "Proxy connection failed for URI: $uri")
            }
        })
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    // --- Qobuz lossless match cache -------------------------------------------
    // Resolving a lossless stream means searching Qobuz and scoring candidates,
    // i.e. several network round-trips per play. Cache the resolved track identity
    // per videoId so repeat plays skip the search. Stream URLs are short-lived, so
    // only the match (not the URL) is cached; the file URL is re-fetched on use.
    private data class QobuzMatch(
        val trackId: Long,
        val maximumBitDepth: Int,
        val maximumSamplingRate: Float,
    )

    private val qobuzMatchCache: MutableMap<String, QobuzMatch> =
        java.util.Collections.synchronizedMap(
            object : LinkedHashMap<String, QobuzMatch>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, QobuzMatch>): Boolean = size > 256
            }
        )

    private fun buildLosslessFormat(
        url: String,
        maximumBitDepth: Int,
        maximumSamplingRate: Float,
    ): PlayerResponse.StreamingData.Format = PlayerResponse.StreamingData.Format(
        itag = 0,
        mimeType = "audio/flac; codecs=\"flac\"",
        bitrate = (maximumSamplingRate * 1000 * maximumBitDepth * 2).toInt(),
        audioSampleRate = (maximumSamplingRate * 1000).toInt(),
        contentLength = 0L,
        url = url,
        cipher = null,
        signatureCipher = null,
        audioQuality = "LOSSLESS",
        fps = null,
        width = null,
        height = null,
        quality = "lossless",
        qualityLabel = null,
        averageBitrate = null,
        approxDurationMs = null,
        audioChannels = null,
        loudnessDb = null,
        lastModified = null,
        audioTrack = null,
    )

    
    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_1_43_32

    
    private val METADATA_CLIENT: YouTubeClient = WEB_REMIX

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_1_61_48,
        WEB_REMIX,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,  
        TVHTML5,
        ANDROID_CREATOR,
        IPADOS,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        IOS,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        val isSaavnStream: Boolean = false,
    )
    
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        context: android.content.Context? = null,
        knownArtist: String? = null,
        knownTitle: String? = null,
        knownDurationMs: Long? = null,
        isDownload: Boolean = false
    ): Result<PlaybackData> {
        val showFallbackToast = context?.let { 
            it.dataStore.data.first()[iad1tya.echo.music.constants.ShowAudioFallbackToastKey] 
        } ?: true

        var hasShownLosslessToast = false
        var hasShownSaavnToast = false
        var hasShownOpusToast = false

        suspend fun tryOpus(): Result<PlaybackData> {
            val firstAttempt = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager)
            if (firstAttempt.isFailure && YouTube.cookie == null) {
                Timber.tag(TAG).w("Playback failed for guest. Rotating session and retrying...")
                PlaybackLogManager.log(PlaybackLogLevel.BOT, "Playback failed for guest", "Triggering bot detection mitigation (rotating guest session)")
                BotDetectionMitigator.rotateGuestSession()
                val retryResult = resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager)
                retryResult.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
                return retryResult
            }
            firstAttempt.onSuccess { BotDetectionMitigator.notifyPlaybackSuccess() }
            return firstAttempt
        }

        suspend fun trySaavn(): Result<PlaybackData> {
            var saavnAttempt: Result<PlaybackData>? = null
            var lastException: Exception? = null
            
            Timber.tag(TAG).d("JioSaavn streaming enabled (via SAAVN) — trying Saavn for videoId=$videoId")
            try {
                saavnAttempt = kotlinx.coroutines.withTimeoutOrNull(15000L) {
                    val metadata = playerResponseForMetadata(videoId).getOrNull()
                    val title = knownTitle ?: metadata?.videoDetails?.title.orEmpty()
                    val artist = knownArtist ?: metadata?.videoDetails?.author?.replace(" - Topic", "").orEmpty()

                    if (title.isBlank()) throw Exception("Title is blank")

                    val query = "$title $artist"
                        .replace("&", " ")
                        .replace(",", " ")
                        .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    Timber.tag(TAG).d("Saavn search query: \"$query\" (original: \"$title $artist\")")

                    val songs = com.music.jiosaavn.SaavnService.searchSongs(query).getOrNull()
                    if (songs.isNullOrEmpty()) {
                        throw Exception("Saavn: no results for \"$query\"")
                    }

                    val ytDuration = knownDurationMs?.let { it / 1000L } ?: metadata?.videoDetails?.lengthSeconds?.toLongOrNull() ?: 0L

                    fun normalize(s: String): Set<String> =
                        s.lowercase()
                            .replace(Regex("[^a-z0-9\\s]"), " ")
                            .split(Regex("\\s+"))
                            .filter { it.length > 1 }
                            .toSet()

                    fun wordOverlapScore(a: String, b: String, maxPts: Int): Int {
                        val setA = normalize(a)
                        val setB = normalize(b)
                        if (setA.isEmpty() || setB.isEmpty()) return 0
                        val common = setA.intersect(setB).size
                        val ratio  = common.toDouble() / maxOf(setA.size, setB.size)
                        return (ratio * maxPts).toInt()
                    }

                    data class ScoredSong(val song: com.music.jiosaavn.SaavnSong, val score: Int)

                    val scored = songs.map { candidate ->
                        var score = 0
                        score += wordOverlapScore(title, candidate.name, maxPts = 50)
                        val saavnDuration = candidate.duration?.toLong() ?: 0L
                        if (ytDuration > 0 && saavnDuration > 0) {
                            val diff = Math.abs(ytDuration - saavnDuration)
                            score += when {
                                diff <= 5  -> 30
                                diff <= 15 -> 15
                                else       -> 0
                            }
                        }
                        val saavnArtists = candidate.artists.primary.joinToString(" ") { it.name }
                        score += wordOverlapScore(artist, saavnArtists, maxPts = 20)
                        if (candidate.explicitContent) score += 5
                        ScoredSong(candidate, score)
                    }

                    val MIN_CONFIDENCE = 40
                    val bestSong = scored.maxByOrNull { it.score }
                        ?.takeIf { it.score >= MIN_CONFIDENCE }
                        ?.song

                    if (bestSong == null) {
                        throw Exception("Saavn: best score below threshold $MIN_CONFIDENCE")
                    }

                    Timber.tag(TAG).d("Saavn best match: id=${bestSong.id}, name=${bestSong.name}")

                    val streamUrl = com.music.jiosaavn.SaavnService.getBestStreamUrl(bestSong.id, "320kbps")
                    if (streamUrl.isNullOrBlank()) {
                        throw Exception("Saavn: no stream URL for songId=${bestSong.id}")
                    }

                    val format = PlayerResponse.StreamingData.Format(
                        itag = 0,
                        url = streamUrl,
                        mimeType = "audio/mp4; codecs=\"mp4a.40.2\"",
                        bitrate = 320_000,
                        width = null,
                        height = null,
                        contentLength = null,
                        quality = "320kbps",
                        fps = null,
                        qualityLabel = null,
                        averageBitrate = null,
                        audioQuality = "320kbps",
                        approxDurationMs = null,
                        audioSampleRate = null,
                        audioChannels = null,
                        loudnessDb = null,
                        lastModified = null,
                        signatureCipher = null,
                        cipher = null,
                        audioTrack = null
                    )

                    val saavnImage = bestSong.image.lastOrNull()?.url ?: bestSong.image.firstOrNull()?.url
                    
                    val updatedVideoDetails = metadata?.videoDetails?.copy(
                        thumbnail = com.music.innertube.models.Thumbnails(
                            thumbnails = listOf(
                                com.music.innertube.models.Thumbnail(url = saavnImage ?: "", width = 500, height = 500)
                            )
                        )
                    ) ?: com.music.innertube.models.response.PlayerResponse.VideoDetails(
                        videoId = videoId, title = title, author = artist, lengthSeconds = ytDuration.toString(),
                        channelId = "", musicVideoType = null, viewCount = null,
                        thumbnail = com.music.innertube.models.Thumbnails(listOf(com.music.innertube.models.Thumbnail(url = saavnImage ?: "", width = 500, height = 500)))
                    )

                    val playbackData = PlaybackData(
                        audioConfig = metadata?.playerConfig?.audioConfig,
                        videoDetails = updatedVideoDetails,
                        playbackTracking = metadata?.playbackTracking,
                        format = format,
                        streamUrl = streamUrl,
                        streamExpiresInSeconds = 3600,
                        isSaavnStream = true
                    )
                    Result.success(playbackData)
                }
                
                if (saavnAttempt == null) {
                    lastException = Exception("Timeout fetching Saavn stream")
                }
            } catch (e: Exception) {
                lastException = e
            }
            
            return saavnAttempt ?: Result.failure(lastException ?: Exception("Saavn resolution failed"))
        }

        suspend fun tryLossless(): Result<PlaybackData> {
            // Fast path: reuse a previously matched Qobuz track for this video and
            // only re-fetch the short-lived file URL instead of re-running search.
            qobuzMatchCache[videoId]?.let { cached ->
                val cachedData = kotlinx.coroutines.withTimeoutOrNull(15000L) {
                    val url = runCatching {
                        iad1tya.echo.music.utils.qobuz.QobuzApiClient().getFileUrl(cached.trackId)
                    }.getOrNull()?.url ?: return@withTimeoutOrNull null
                    val metadata = playerResponseForMetadata(videoId).getOrNull()
                    PlaybackData(
                        audioConfig = null,
                        videoDetails = metadata?.videoDetails,
                        playbackTracking = null,
                        format = buildLosslessFormat(url, cached.maximumBitDepth, cached.maximumSamplingRate),
                        streamUrl = url,
                        streamExpiresInSeconds = 3600,
                    )
                }
                if (cachedData != null) return Result.success(cachedData)
                qobuzMatchCache.remove(videoId) // stale match — fall through to a fresh search
            }

            var qobuzAttempt: Result<PlaybackData>? = null
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    qobuzAttempt = kotlinx.coroutines.withTimeoutOrNull(15000L) {
                        val metadata = playerResponseForMetadata(videoId).getOrNull()
                        val title = knownTitle ?: metadata?.videoDetails?.title
                        val author = knownArtist ?: metadata?.videoDetails?.author?.replace(" - Topic", "")
                        if (title != null) {
                            val qobuzClient = iad1tya.echo.music.utils.qobuz.QobuzApiClient()
                            // Artist may be missing/blank — fall back to a title-only
                            // search. The weighted scorer no longer requires a matching
                            // artist to accept a strong title+duration match.
                            val queryArtist = author.orEmpty()
                            val queryTitle = title
                            val durationSeconds = metadata?.videoDetails?.lengthSeconds?.toLongOrNull()
                            val durationMs = knownDurationMs ?: (if (durationSeconds != null) durationSeconds * 1000L else null)
                            
                            var resolvedPlaybackData: PlaybackData? = null
                            for (term in qobuzSearchTerms(queryArtist, queryTitle)) {
                                val searchResult = runCatching { qobuzClient.search(term) }.getOrNull() ?: continue
                                val candidates = searchResult.tracks?.items ?: continue
                                val validCandidates = candidates.filter {
                                    val streamable = it.streamable ?: false
                                    val maxDepth = it.maximumBitDepth ?: 0
                                    streamable && maxDepth >= 16
                                }
                                val sorted = validCandidates.sortedByDescending { confidence(queryArtist, queryTitle, durationMs, it) }
                                for (candidate in sorted) {
                                    if (confidence(queryArtist, queryTitle, durationMs, candidate) >= 0.5f) {
                                        val downloadData = runCatching { qobuzClient.getFileUrl(candidate.id) }.getOrNull()
                                        val url = downloadData?.url
                                        if (url != null) {
                                            qobuzMatchCache[videoId] = QobuzMatch(
                                                trackId = candidate.id,
                                                maximumBitDepth = candidate.maximumBitDepth,
                                                maximumSamplingRate = candidate.maximumSamplingRate,
                                            )
                                            resolvedPlaybackData = PlaybackData(
                                                audioConfig = null,
                                                videoDetails = metadata?.videoDetails,
                                                playbackTracking = null,
                                                format = buildLosslessFormat(url, candidate.maximumBitDepth, candidate.maximumSamplingRate),
                                                streamUrl = url,
                                                streamExpiresInSeconds = 3600
                                            )
                                            break
                                        }
                                    }
                                }
                                if (resolvedPlaybackData != null) {
                                    break
                                }
                            }
    
                            if (resolvedPlaybackData != null) {
                                return@withTimeoutOrNull Result.success(resolvedPlaybackData)
                            } else {
                                throw Exception("No streamable match resolved on Qobuz")
                            }
                        } else {
                            throw Exception("Missing title for lookup")
                        }
                    }
                    if (qobuzAttempt == null) {
                        lastException = Exception("Timeout fetching Qobuz stream")
                    }
                } catch (e: Exception) {
                    lastException = e
                }
                
                if (qobuzAttempt != null && qobuzAttempt.isSuccess) {
                    break
                }
            }
            return qobuzAttempt ?: Result.failure(lastException ?: Exception("Qobuz resolution failed"))
        }

        fun showToastMsg(msg: String) {
            context?.let {
                if (showFallbackToast) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(it, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return when (audioQuality) {
            AudioQuality.LOSSLESS -> {
                val losslessRes = tryLossless()
                if (losslessRes.isSuccess) return losslessRes

                Timber.tag(TAG).e("Qobuz resolution failed, falling back to Saavn")
                if (!hasShownLosslessToast) {
                    hasShownLosslessToast = true
                    showToastMsg(if (isDownload) "Lossless download unavailable, falling back to Saavn (320kbps)" else "Lossless stream unavailable, falling back to Saavn (320kbps)")
                }

                val saavnRes = trySaavn()
                if (saavnRes.isSuccess) return saavnRes

                Timber.tag(TAG).e("Saavn resolution failed, falling back to YouTube Opus")
                if (!hasShownSaavnToast) {
                    hasShownSaavnToast = true
                    showToastMsg(if (isDownload) "Lossless & Saavn unavailable, downloading Opus" else "Lossless & Saavn unavailable, playing Opus")
                }

                tryOpus()
            }
            AudioQuality.SAAVN -> {
                val saavnRes = trySaavn()
                if (saavnRes.isSuccess) return saavnRes

                Timber.tag(TAG).e("Saavn resolution failed, falling back to YouTube Opus")
                if (!hasShownSaavnToast) {
                    hasShownSaavnToast = true
                    showToastMsg(if (isDownload) "Saavn unavailable, downloading Opus" else "Saavn unavailable, playing Opus")
                }

                tryOpus()
            }
            else -> {
                val opusRes = tryOpus()
                if (opusRes.isSuccess) return opusRes

                Timber.tag(TAG).e("Opus resolution failed, falling back to Saavn")
                if (!hasShownOpusToast) {
                    hasShownOpusToast = true
                    showToastMsg(if (isDownload) "Opus unavailable, downloading Saavn" else "Opus unavailable, playing Saavn")
                }

                trySaavn()
            }
        }
    }

    private suspend fun resolvePlaybackData(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        PlaybackLogManager.log(PlaybackLogLevel.INFO, "Resolving playback data", "Video: $videoId")
        
        
        val isUploadedTrack = playlistId == "MLPT" || playlistId?.contains("MLPT") == true

        val isLoggedIn = YouTube.cookie != null
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: ${signatureTimestamp.timestamp}")

        
        var poToken: PoTokenResult? = null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            Timber.tag(logTag).d("Generating PoToken for MAIN_CLIENT with sessionId")
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                if (poToken != null) {
                    Timber.tag(logTag).d("PoToken generated successfully")
                }
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "PoToken generation failed: ${e.message}")
            }
        }

        
        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying ${MAIN_CLIENT.clientName} (Main)")
        var mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp.timestamp, poToken?.playerRequestPoToken).getOrThrow()

        
        
        
        var metadataResponse: PlayerResponse? = null
        if (isLoggedIn) {
            Timber.tag(logTag).d("Fetching metadata from METADATA_CLIENT (WEB_REMIX) for authenticated tracking")
            try {
                
                var metaPoToken: PoTokenResult? = null
                val metaSessionId = YouTube.dataSyncId
                if (METADATA_CLIENT.useWebPoTokens && metaSessionId != null) {
                    try {
                        metaPoToken = poTokenGenerator.getWebClientPoToken(videoId, metaSessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Metadata PoToken generation failed")
                    }
                }
                metadataResponse = YouTube.player(
                    videoId, playlistId, METADATA_CLIENT,
                    signatureTimestamp.timestamp, metaPoToken?.playerRequestPoToken
                ).getOrNull()
                Timber.tag(logTag).d("Metadata response obtained: ${metadataResponse?.playabilityStatus?.status}")
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "Failed to fetch metadata from METADATA_CLIENT")
            }
        }

        
        var usedAgeRestrictedClient: YouTubeClient? = null
        val wasOriginallyAgeRestricted: Boolean

        
        
        
        
        
        val mainStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestrictedFromResponse = mainStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )
        wasOriginallyAgeRestricted = isAgeRestrictedFromResponse

        if (isAgeRestrictedFromResponse && isLoggedIn) {
            
            Timber.tag(logTag).d("Age-restricted detected, using WEB_CREATOR")
            Log.i(TAG, "Age-restricted: using WEB_CREATOR for videoId=$videoId")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("WEB_CREATOR works for age-restricted content")
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        
        if (mainPlayerResponse == null) {
            throw Exception("Failed to get player response")
        }

        
        
        val audioConfig = metadataResponse?.playerConfig?.audioConfig ?: mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = metadataResponse?.videoDetails ?: mainPlayerResponse.videoDetails
        val playbackTracking = metadataResponse?.playbackTracking ?: mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var retryMainPlayerResponse: PlayerResponse? = if (usedAgeRestrictedClient != null) mainPlayerResponse else null

        
        val currentStatus = mainPlayerResponse.playabilityStatus.status
        var isAgeRestricted = currentStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )

        if (isAgeRestricted) {
            Timber.tag(logTag).d("Content is still age-restricted (status: $currentStatus), will try fallback clients")
            Log.i(TAG, "Age-restricted content detected: videoId=$videoId, status=$currentStatus")
        }

        
        val isPrivateTrack = mainPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

        
        
        
        val startIndex = when {
            isPrivateTrack -> 1  
            isAgeRestricted -> 0
            else -> -1
        }

        for (clientIndex in (startIndex until STREAM_FALLBACK_CLIENTS.size)) {
            
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            
            val client: YouTubeClient
            if (clientIndex == -1) {
                
                client = MAIN_CLIENT
                streamPlayerResponse = retryMainPlayerResponse ?: mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag).d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying fallback [${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}]", client.clientName)

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                
                if (client.useWebPoTokens && poToken == null && sessionId != null) {
                    Timber.tag(logTag).d("Lazily generating PoToken for fallback web client: ${client.clientName}")
                    try {
                        poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Lazy PoToken generation failed")
                    }
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                
                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                
                val clientSigTimestamp = if (wasOriginallyAgeRestricted) null else signatureTimestamp.timestamp
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken).getOrNull()
            }

            
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.INFO, "Player response OK", if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName)

                
                val hasDirectUrls = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.url.isNullOrEmpty() } == true
                val hasSignatureCipher = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.signatureCipher.isNullOrEmpty() || !it.cipher.isNullOrEmpty() } == true

                Timber.tag(logTag).d("URL check: hasDirectUrls=$hasDirectUrls, hasSignatureCipher=$hasSignatureCipher")

                
                val responseToUse = streamPlayerResponse

                format =
                    findFormat(
                        responseToUse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Timber.tag(logTag).d("No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId, responseToUse, skipNewPipe = wasOriginallyAgeRestricted)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                
                val currentClient = if (clientIndex == -1) {
                    usedAgeRestrictedClient ?: MAIN_CLIENT
                } else {
                    STREAM_FALLBACK_CLIENTS[clientIndex]
                }

                
                val isPrivatelyOwnedTrack = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                
                if (currentClient.useWebPoTokens) {
                    try {
                        Timber.tag(logTag).d("Applying n-transform to stream URL for ${currentClient.clientName}")
                        val transformed = EjsNTransformSolver.transformNParamInUrl(streamUrl!!)
                        if (transformed != streamUrl) {
                            streamUrl = transformed
                            Timber.tag(logTag).d("N-transform applied successfully")
                        }
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "N-transform failed: ${e.message}")
                    }
                }

                
                
                if (currentClient.useWebPoTokens && poToken?.streamingDataPoToken != null) {
                    Timber.tag(logTag).d("Appending pot= parameter to stream URL")
                    val separator = if ("?" in streamUrl!!) "&" else "?"
                    streamUrl = "${streamUrl}${separator}pot=${poToken.streamingDataPoToken}"
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                
                val urlHost = try { java.net.URL(streamUrl).host } catch (e: Exception) { "unknown" }
                Timber.tag(logTag).d("Stream URL host: $urlHost, pot length: ${poToken?.streamingDataPoToken?.length ?: 0}")

                
                val isPrivatelyOwned = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1 || isPrivatelyOwned) {
                    
                    if (isPrivatelyOwned) {
                        Timber.tag(logTag).d("Skipping validation for privately owned track: ${currentClient.clientName}")
                    } else {
                        Timber.tag(logTag).d("Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    }
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId, private=$isPrivatelyOwned")
                    break
                }

                if (validateStatus(streamUrl!!)) {
                    
                    Timber.tag(logTag).d("Stream validated successfully with client: ${currentClient.clientName}")
                    PlaybackLogManager.log(PlaybackLogLevel.INFO, "Stream validated", currentClient.clientName)
                    
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId")
                    break
                } else {
                    Timber.tag(logTag).d("Stream validation failed for client: ${currentClient.clientName}")

                    
                    if (currentClient.useWebPoTokens) {
                        var nTransformWorked = false

                        
                        try {
                            val nTransformed = CipherDeobfuscator.transformNParamInUrl(streamUrl!!)
                            if (nTransformed != streamUrl) {
                                Timber.tag(logTag).d("CipherDeobfuscator n-transform applied, re-validating...")
                                if (validateStatus(nTransformed)) {
                                    Timber.tag(logTag).d("N-transformed URL VALIDATED OK!")
                                    streamUrl = nTransformed
                                    nTransformWorked = true
                                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId (cipher n-transform)")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(logTag).e(e, "CipherDeobfuscator n-transform error")
                        }

                        if (nTransformWorked) break
                    }
                }
            } else {
                val status = streamPlayerResponse?.playabilityStatus?.status ?: "Unknown"
                val reason = streamPlayerResponse?.playabilityStatus?.reason ?: "No reason"
                Timber.tag(logTag).d("Player response status not OK: $status, reason: $reason")
                PlaybackLogManager.log(PlaybackLogLevel.WARNING, "Client failed: ${client.clientName}", "$status: $reason")
                
                
                Timber.tag(logTag).d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }.onFailure { e ->
        Timber.tag(logTag).e(e, "Playback resolution failed")
        PlaybackLogManager.log(PlaybackLogLevel.ERROR, "Playback failed", "${e::class.simpleName}: ${e.message}")
    }
    
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) 
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.OPUS, AudioQuality.SAAVN, AudioQuality.LOSSLESS -> 1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) 
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }
    
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)

            
            YouTube.cookie?.let { cookie ->
                requestBuilder.addHeader("Cookie", cookie)
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    data class SignatureTimestampResult(
        val timestamp: Int?,
        val isAgeRestricted: Boolean
    )

    private fun getSignatureTimestampOrNull(videoId: String): SignatureTimestampResult {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        val result = NewPipeExtractor.getSignatureTimestamp(videoId)
        return result.fold(
            onSuccess = { timestamp ->
                Timber.tag(logTag).d("Signature timestamp obtained: $timestamp")
                SignatureTimestampResult(timestamp, isAgeRestricted = false)
            },
            onFailure = { error ->
                val isAgeRestricted = error.message?.contains("age-restricted", ignoreCase = true) == true ||
                    error.cause?.message?.contains("age-restricted", ignoreCase = true) == true
                if (isAgeRestricted) {
                    Timber.tag(logTag).d("Age-restricted content detected from NewPipe")
                    Log.i(TAG, "Age-restricted detected early via NewPipe: videoId=$videoId")
                } else {
                    Timber.tag(logTag).e(error, "Failed to get signature timestamp")
                    reportException(error)
                }
                SignatureTimestampResult(null, isAgeRestricted)
            }
        )
    }

    suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId, skipNewPipe: $skipNewPipe")

        
        if (!format.url.isNullOrEmpty()) {
            Timber.tag(logTag).d("Using URL from format directly")
            return format.url
        }

        
        val signatureCipher = format.signatureCipher ?: format.cipher
        if (!signatureCipher.isNullOrEmpty()) {
            Timber.tag(logTag).d("Format has signatureCipher, using custom deobfuscation")
            val customDeobfuscatedUrl = CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)
            if (customDeobfuscatedUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained via custom cipher deobfuscation")
                return customDeobfuscatedUrl
            }
            Timber.tag(logTag).d("Custom cipher deobfuscation failed")
        }

        
        if (skipNewPipe) {
            Timber.tag(logTag).d("Skipping NewPipe methods for age-restricted content")
            return null
        }

        
        val deobfuscatedUrl = NewPipeExtractor.getStreamUrl(format, videoId)
        if (deobfuscatedUrl != null) {
            Timber.tag(logTag).d("Stream URL obtained via NewPipe deobfuscation")
            return deobfuscatedUrl
        }

        
        Timber.tag(logTag).d("Trying StreamInfo fallback for URL")
        val streamUrls = YouTube.getNewPipeStreamUrls(videoId)
        if (streamUrls.isNotEmpty()) {
            val streamUrl = streamUrls.find { it.first == format.itag }?.second
            if (streamUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained from StreamInfo")
                return streamUrl
            }

            
            val audioStream = streamUrls.find { urlPair ->
                playerResponse.streamingData?.adaptiveFormats?.any {
                    it.itag == urlPair.first && it.isAudio
                } == true
            }?.second

            if (audioStream != null) {
                Timber.tag(logTag).d("Audio stream URL obtained from StreamInfo (different itag)")
                return audioStream
            }
        }

        Timber.tag(logTag).e("Failed to get stream URL")
        return null
    }

    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing for videoId: $videoId")
    }
}




fun cleanSearchTerm(term: String): String {
    return term
        .replace(Regex("(?i)\\b(official video|music video|official audio|lyric video|lyrics|audio|video|hq|high quality)\\b"), "")
        .replace(Regex("\\([^)]*\\)"), "")
        .replace(Regex("\\[[^]]*\\]"), "")
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun qobuzSearchTerms(artist: String, title: String): List<String> {
    val cleanArtist = cleanSearchTerm(artist)
    val cleanTitle = cleanSearchTerm(title)
    
    val terms = mutableListOf<String>()
    
    terms.add("$artist $title".trim())
    
    if (cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty()) {
        terms.add("$cleanArtist $cleanTitle".trim())
    }
    
    val primary = artist.substringBefore(",").trim()
    if (primary.isNotEmpty() && !primary.equals(artist.trim(), ignoreCase = true)) {
        terms.add("$primary $title".trim())
        val cleanPrimary = cleanSearchTerm(primary)
        if (cleanPrimary.isNotEmpty() && cleanTitle.isNotEmpty()) {
            terms.add("$cleanPrimary $cleanTitle".trim())
        }
    }
    
    return terms.distinct()
}

private fun normalize(s: String): String =
    s.lowercase()
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("\\[[^]]*\\]"), " ")
        .replace(Regex("(?i)\\b(feat\\.?|ft\\.?|featuring)\\b.*"), " ")
        .replace(Regex("[''`]"), "")
        .replace(Regex("[^\\p{L}\\p{N}\\p{S}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun jaccard(a: String, b: String): Float {
    val setA = a.split(" ").filter { it.isNotEmpty() }.toSet()
    val setB = b.split(" ").filter { it.isNotEmpty() }.toSet()
    if (setA.isEmpty() || setB.isEmpty()) return 0f
    val intersection = setA.intersect(setB).size.toFloat()
    val union = setA.union(setB).size.toFloat()
    return intersection / union
}

private fun artistSimilarity(a: String, b: String): Float {
    val setA = a.split(" ").filter { it.isNotEmpty() }.toSet()
    val setB = b.split(" ").filter { it.isNotEmpty() }.toSet()
    if (setA.isEmpty() || setB.isEmpty()) return 0f

    val intersection = setA.intersect(setB)
    val union = setA.union(setB)
    val jaccardScore = intersection.size.toFloat() / union.size.toFloat()

    val smallerSize = minOf(setA.size, setB.size)
    val smallerFullyCovered = intersection.size == smallerSize
    val hasDistinctiveOverlap = intersection.any { token ->
        token.length > 3 || token.any { ch -> !ch.isLetterOrDigit() }
    }

    val coverageScore = if (smallerFullyCovered && hasDistinctiveOverlap) 1.0f else 0f
    return maxOf(jaccardScore, coverageScore)
}

// Relative importance of each matching signal when scoring a Qobuz candidate.
// These are combined as a weighted SUM (not a product), so a single weak signal
// cannot veto an otherwise strong match. Weights sum to 1f, keeping the result
// in [0f, 1f] and the existing >= 0.5f acceptance threshold meaningful.
private const val TITLE_WEIGHT = 0.55f
private const val ARTIST_WEIGHT = 0.25f
private const val DURATION_WEIGHT = 0.20f

// Minimum title overlap for two tracks to be considered the same song at all.
private const val MIN_TITLE_SIMILARITY = 0.20f

// Score used for the duration signal when we can't verify length (missing
// metadata): neutral-but-not-perfect, so unknown durations aren't punished.
private const val UNKNOWN_DURATION_SCORE = 0.6f

fun confidence(queryArtist: String, queryTitle: String, queryDuration: Long?, candidate: iad1tya.echo.music.utils.qobuz.QobuzTrack): Float {
    if (!candidate.streamable) return 0f

    // Title is the primary signal. With essentially no title overlap this is not
    // the same song, no matter how well artist/duration happen to line up. This
    // floor guards against the wrong-song matches reported in #499 / #512.
    val titleSim = jaccard(normalize(queryTitle), normalize(candidate.title))
    if (titleSim < MIN_TITLE_SIMILARITY) return 0f

    val artistSim = artistSimilarity(
        normalize(queryArtist),
        normalize(candidate.performer?.name.orEmpty()),
    )

    // Duration acts as a graded signal and a hard gate: a known duration that
    // drifts far (e.g. an extended/sped-up edit or an unrelated track) is a hard
    // reject; an unknown duration stays neutral.
    val durationScore: Float = if (queryDuration == null || queryDuration <= 0 || candidate.duration <= 0) {
        UNKNOWN_DURATION_SCORE
    } else {
        val candidateMs = candidate.duration * 1000L
        val drift = kotlin.math.abs(queryDuration - candidateMs).toDouble() / queryDuration.toDouble()
        when {
            drift < 0.05 -> 1.0f
            drift < 0.10 -> 0.85f
            drift < 0.20 -> 0.6f
            drift < 0.35 -> 0.35f
            else -> 0f // wrong length -> not the same recording
        }
    }
    if (durationScore == 0f) return 0f

    // Weighted sum instead of a product. Previously this returned
    // titleSim * artistSim * durationFactor, so any single ~0 factor (most often
    // the artist string, which differs across providers due to ", " joins,
    // romanisation, "- Topic" channels or featured-artist ordering) zeroed the
    // whole score and silently dropped lossless playback to Saavn/Opus
    // (#540 / #505 / #487). The title floor and duration gate above keep us from
    // over-matching.
    return (TITLE_WEIGHT * titleSim) +
        (ARTIST_WEIGHT * artistSim) +
        (DURATION_WEIGHT * durationScore)
}
