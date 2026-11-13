package echo.music.iad1tya.lyrics

import android.content.Context
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.FetchFasterLyricsKey
import echo.music.iad1tya.constants.LyricsProviderOrderKey
import echo.music.iad1tya.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import echo.music.iad1tya.models.MediaMetadata
import echo.music.iad1tya.playback.LyricsWithProvider
import echo.music.iad1tya.utils.NetworkConnectivityObserver
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import echo.music.iad1tya.utils.reportException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LyricsHelper
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val networkConnectivity: NetworkConnectivityObserver,
) {

  private suspend fun resolveLyricsProviders(): List<LyricsProvider> {
    val orderString = context.dataStore[LyricsProviderOrderKey].orEmpty()
    return LyricsProviderRegistry.orderedProviderNames(orderString)
      .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
      .filter { it.isEnabled(context) }
  }

  private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
  private val inFlightRequests = ConcurrentHashMap<String, Deferred<LyricsWithProvider>>()
  private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var currentLyricsJob: Job? = null

  suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
    currentLyricsJob?.cancel()

    val cached = cache.get(mediaMetadata.id)?.firstOrNull()
    if (cached != null) {
      return LyricsWithProvider(cached.lyrics, cached.providerName)
    }

    val isNetworkAvailable =
      try {
        networkConnectivity.isCurrentlyConnected()
      } catch (e: Exception) {
        true
      }

    if (!isNetworkAvailable) {
      return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
    }

    // Shared fetches live in fetchScope, not the first caller's scope, so one caller being
    // cancelled (e.g. collectLatest on a song switch) can't fail the others awaiting it.
    val created =
      fetchScope.async(start = CoroutineStart.LAZY) { fetchFirstSyncedLyrics(mediaMetadata) }
    val existing = inFlightRequests.putIfAbsent(mediaMetadata.id, created)
    val shared =
      existing
        ?: created.also {
          it.invokeOnCompletion { inFlightRequests.remove(mediaMetadata.id, created) }
          it.start()
        }
    if (existing != null) created.cancel()
    return shared.await()
  }

  private suspend fun fetchFirstSyncedLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
    val providers = resolveLyricsProviders()
    if (providers.isEmpty()) return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")

    return coroutineScope {
      val outcomes = ConcurrentHashMap<String, ProviderOutcome>()
      val lyricsByProvider = ConcurrentHashMap<String, String>()
      val channel = Channel<Unit>(providers.size)
      val order = providers.map { it.name }

      providers.forEach { provider ->
        launch {
          val lyrics = fetchLyricsFromProvider(provider, mediaMetadata)
          val usable = lyrics?.takeIf { it != LYRICS_NOT_FOUND && it.isNotBlank() }
          if (usable == null) {
            outcomes[provider.name] = ProviderOutcome.FAILED
          } else {
            lyricsByProvider[provider.name] = usable
            outcomes[provider.name] =
              if (LyricsUtils.hasSyncedLyrics(usable)) ProviderOutcome.SYNCED
              else ProviderOutcome.UNSYNCED
          }
          channel.send(Unit)
        }
      }

      repeat(providers.size) {
        channel.receive()
        when (val choice = LyricsProviderRegistry.chooseProvider(order, outcomes)) {
          is ProviderChoice.Selected -> {
            val lyrics = lyricsByProvider[choice.name] ?: return@repeat
            coroutineContext.cancelChildren()
            cache.put(mediaMetadata.id, listOf(LyricsResult(choice.name, lyrics)))
            return@coroutineScope LyricsWithProvider(lyrics, choice.name)
          }
          ProviderChoice.None ->
            return@coroutineScope LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
          ProviderChoice.Pending -> Unit
        }
      }

      LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
    }
  }

  private suspend fun fetchLyricsFromProvider(
    provider: LyricsProvider,
    mediaMetadata: MediaMetadata,
  ): String? =
    withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
      try {
        provider
          .getLyrics(
            mediaMetadata.id,
            mediaMetadata.title,
            mediaMetadata.artists.joinToString { it.name },
            mediaMetadata.duration,
            mediaMetadata.album?.title,
          )
          .getOrElse {
            if (it is CancellationException) throw it
            reportException(it)
            null
          }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        reportException(e)
        null
      }
    }

  suspend fun getAllLyrics(
    mediaId: String,
    songTitle: String,
    songArtists: String,
    duration: Int,
    album: String? = null,
    callback: (LyricsResult) -> Unit,
  ) {
    currentLyricsJob?.cancel()

    val cacheKey = "$songArtists-$songTitle".replace(" ", "")
    cache.get(cacheKey)?.let { results ->
      results.forEach { callback(it) }
      return
    }

    val isNetworkAvailable =
      try {
        networkConnectivity.isCurrentlyConnected()
      } catch (e: Exception) {
        true
      }

    if (!isNetworkAvailable) {
      return
    }

    val allResult = java.util.concurrent.CopyOnWriteArrayList<LyricsResult>()
    val providers = resolveLyricsProviders()
    currentLyricsJob =
      CoroutineScope(SupervisorJob()).launch {
        val jobs =
          providers.mapNotNull { provider ->
            if (provider.isEnabled(context)) {
              launch {
                try {
                  provider.getAllLyrics(mediaId, songTitle, songArtists, duration, album) { lyrics
                    ->
                    val result = LyricsResult(provider.name, lyrics)
                    allResult += result
                    callback(result)
                  }
                } catch (e: Exception) {
                  reportException(e)
                }
              }
            } else null
          }
        jobs.forEach { it.join() }
        cache.put(cacheKey, allResult.toList())
      }

    currentLyricsJob?.join()
  }

  fun cancelCurrentLyricsJob() {
    currentLyricsJob?.cancel()
    currentLyricsJob = null
  }

  companion object {
    private const val MAX_CACHE_SIZE = 20
    private const val PROVIDER_TIMEOUT_MS = 5_000L
  }
}

data class LyricsResult(
  val providerName: String,
  val lyrics: String,
)
