package echo.music.iad1tya.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.echo.spotify.LoadSpotifyRecommendationsUseCase
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterPodcasts
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.utils.completed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.HideExplicitKey
import echo.music.iad1tya.constants.BlockedArtistsKey
import echo.music.iad1tya.constants.HideVideoSongsKey
import echo.music.iad1tya.constants.HideYoutubeShortsKey
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.QuickPicks
import echo.music.iad1tya.constants.QuickPicksKey
import echo.music.iad1tya.constants.RecommendationSourceKey
import echo.music.iad1tya.constants.RecommendationSource
import echo.music.iad1tya.constants.SongSortType
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.db.entities.Album
import echo.music.iad1tya.db.entities.LocalItem
import echo.music.iad1tya.db.entities.Song
import echo.music.iad1tya.db.entities.SpeedDialItem
import echo.music.iad1tya.extensions.filterVideoSongs
import echo.music.iad1tya.extensions.toEnum
import echo.music.iad1tya.models.SimilarRecommendation
import echo.music.iad1tya.utils.SyncUtils
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import echo.music.iad1tya.utils.filterQuickPickSongs
import echo.music.iad1tya.utils.reportException
import echo.music.iad1tya.utils.selectHeroCarouselSongs
import echo.music.iad1tya.utils.toCarouselSong
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

data class DailyDiscoverItem(
  val seed: Song,
  val recommendation: YTItem,
  val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(val playlist: PlaylistItem, val songs: List<SongItem>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
  @ApplicationContext val context: Context,
  val database: MusicDatabase,
  val syncUtils: SyncUtils,
  val loadSpotifyRecommendationsUseCase: LoadSpotifyRecommendationsUseCase,
) : ViewModel() {
  val isRefreshing = MutableStateFlow(false)
  val isLoading = MutableStateFlow(false)
  val isRandomizing = MutableStateFlow(false)

  private val quickPicksEnum =
    context.dataStore.data
      .map {
        (try {
            it[QuickPicksKey]
          } catch (e: Exception) {
            null
          })
          .toEnum(QuickPicks.QUICK_PICKS)
      }
      .distinctUntilChanged()

  private val quickPicksSource = MutableStateFlow<List<Song>?>(null)
  val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
  val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
  val keepListening = MutableStateFlow<List<LocalItem>?>(null)
  val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
  val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
  val homePage = MutableStateFlow<HomePage?>(null)
  val explorePage = MutableStateFlow<ExplorePage?>(null)
  val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
  val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
  private val previousHomePage = MutableStateFlow<HomePage?>(null)

  private val carouselAccountSongs = MutableStateFlow<List<SongItem>>(emptyList())
  private val accountPlaylistMutex = Mutex()
  private val carouselRadioSongs = MutableStateFlow<List<SongItem>>(emptyList())
  private val carouselShuffleSeed = MutableStateFlow(Random.nextInt())
  private val carouselLibrary =
    combine(database.carouselLibrarySongs(), carouselAccountSongs, carouselShuffleSeed) {
        local, account, seed ->
      local.map { it.toCarouselSong() }.shuffled(Random(seed)) to
        account.shuffled(Random(seed))
    }
  private val carouselFilters =
    combine(context.dataStore.data, database.hiddenFromQuickPicksSongIds()) { preferences, hidden ->
      preferences to hidden.toSet()
    }
  val quickPicks: StateFlow<List<Song>?> =
    combine(quickPicksSource, carouselFilters) { songs, filters ->
      val (preferences, hidden) = filters
      songs?.let {
        filterQuickPickSongs(
          it, hidden, preferences[BlockedArtistsKey].orEmpty(),
          preferences[HideExplicitKey] ?: false, preferences[HideVideoSongsKey] ?: false,
        )
      }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
  private val carouselRecommendations =
    combine(carouselRadioSongs, similarRecommendations, dailyDiscover, homePage) {
        radio, similar, discover, home ->
      (radio +
          similar.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>() +
          discover.orEmpty().mapNotNull { it.recommendation as? SongItem } +
          home?.sections.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>())
        .distinctBy { it.id }
    }

  val heroSongs: StateFlow<List<SongItem>> =
    combine(
        database.recentlyPlayedSongs(),
        carouselLibrary,
        carouselRecommendations,
        carouselFilters,
      ) { recent, library, recommendations, filters ->
        val (preferences, hidden) = filters
        selectHeroCarouselSongs(
          recentlyPlayed = recent.map { it.toCarouselSong() },
          library = library.first,
          account = library.second,
          excludedSongIds = hidden,
          blockedArtists = preferences[BlockedArtistsKey] ?: emptySet(),
          recommendations = recommendations,
          hideExplicit = preferences[HideExplicitKey] ?: false,
          hideVideoSongs = preferences[HideVideoSongsKey] ?: false,
        )
      }
      .flowOn(Dispatchers.IO)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val aiRecommendedPlaylist =
    database
      .playlistsByNameAsc()
      .map { playlists -> playlists.find { it.playlist.name == "From your taste by Echo Music" } }
      .flatMapLatest { playlist ->
        if (playlist != null && playlist.songCount > 0) {
          database.playlistSongs(playlist.playlist.id).map { playlistSongs ->
            playlist to playlistSongs.map { it.song }
          }
        } else {
          flowOf(null)
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
  val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

  val speedDialItems: StateFlow<List<YTItem>> =
    combine(database.speedDialDao.getAll(), keepListening, quickPicks) {
        pinned,
        keepListening,
        quick ->
        val pinnedItems = pinned.map { it.toYTItem() }
        val filled = pinnedItems.toMutableList()
        val targetSize = 27

        if (filled.size < targetSize) {

          keepListening?.let { k ->
            val needed = targetSize - filled.size
            val available =
              k.filter { item -> filled.none { p -> p.id == item.id } }
                .mapNotNull { item ->
                  when (item) {
                    is Song ->
                      SongItem(
                        id = item.id,
                        title = item.title,
                        artists = item.artists.map { Artist(name = it.name, id = it.id) },
                        thumbnail = item.thumbnailUrl ?: "",
                        explicit = false
                      )
                    is Album ->
                      AlbumItem(
                        browseId = item.id,
                        playlistId = item.album.playlistId ?: "",
                        title = item.title,
                        artists = item.artists.map { Artist(name = it.name, id = it.id) },
                        year = item.album.year,
                        thumbnail = item.thumbnailUrl ?: ""
                      )
                    else -> null
                  }
                }
            filled.addAll(available.take(needed))
          }
        }

        if (filled.size < targetSize) {

          quick?.let { q ->
            val needed = targetSize - filled.size
            val available =
              q.filter { song -> filled.none { p -> p.id == song.id } }
                .map { song ->
                  SongItem(
                    id = song.id,
                    title = song.title,
                    artists = song.artists.map { Artist(name = it.name, id = it.id) },
                    thumbnail = song.thumbnailUrl ?: "",
                    explicit = false
                  )
                }
            filled.addAll(available.take(needed))
          }
        }

        filled.take(targetSize)
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  suspend fun getRandomItem(): YTItem? {
    try {
      isRandomizing.value = true

      kotlinx.coroutines.delay(1000)

      val userSongs = mutableListOf<YTItem>()
      val otherSources = mutableListOf<YTItem>()

      quickPicks.value?.let { songs ->
        userSongs.addAll(
          songs.map { song ->
            SongItem(
              id = song.id,
              title = song.title,
              artists = song.artists.map { Artist(name = it.name, id = it.id) },
              thumbnail = song.thumbnailUrl ?: "",
              explicit = false
            )
          }
        )
      }

      keepListening.value?.let { items ->
        items.forEach { item ->
          when (item) {
            is Song ->
              userSongs.add(
                SongItem(
                  id = item.id,
                  title = item.title,
                  artists = item.artists.map { Artist(name = it.name, id = it.id) },
                  thumbnail = item.thumbnailUrl ?: "",
                  explicit = false
                )
              )
            is Album ->
              otherSources.add(
                AlbumItem(
                  browseId = item.id,
                  playlistId = item.album.playlistId ?: "",
                  title = item.title,
                  artists = item.artists.map { Artist(name = it.name, id = it.id) },
                  year = item.album.year,
                  thumbnail = item.thumbnailUrl ?: ""
                )
              )
            else -> {}
          }
        }
      }

      otherSources.addAll(allYtItems.value)

      val item =
        if (userSongs.isNotEmpty() && (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
          userSongs.distinctBy { it.id }.shuffled().firstOrNull()
        } else {
          otherSources.distinctBy { it.id }.shuffled().firstOrNull()
        } ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()

      return item
    } finally {
      isRandomizing.value = false
    }
  }

  val accountName = MutableStateFlow("Guest")
  val accountImageUrl = MutableStateFlow<String?>(null)

  fun togglePin(item: YTItem) {
    viewModelScope.launch(Dispatchers.IO) {
      val speedDialItem = SpeedDialItem.fromYTItem(item)
      val isPinned = database.speedDialDao.isPinned(speedDialItem.id).first()
      if (isPinned) {
        database.speedDialDao.delete(speedDialItem.id)
      } else {
        database.speedDialDao.insert(speedDialItem)
      }
    }
  }

  private suspend fun getDailyDiscover() {
    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
    val likedSongs = database.likedSongsByCreateDateAsc().first()
    if (likedSongs.isEmpty()) return

    val seeds = likedSongs.shuffled().distinctBy { it.id }.take(5)

    val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

    kotlinx.coroutines.coroutineScope {
      seeds
        .map { seed ->
          launch(Dispatchers.IO) {
            val endpoint =
              YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
            if (endpoint != null) {
              YouTube.related(endpoint).onSuccess { page ->
                val recommendations =
                  page.songs
                    .filter { item ->
                      if (hideVideoSongs && item.isVideoSong) return@filter false
                      if (item.explicit) return@filter false
                      true
                    }
                    .shuffled()

                val recommendation = recommendations.firstOrNull { rec -> rec.id != seed.id }

                if (recommendation != null) {
                  items.add(
                    DailyDiscoverItem(
                      seed = seed,
                      recommendation = recommendation,
                      relatedEndpoint = endpoint
                    )
                  )
                }
              }
            }
          }
        }
        .forEach { it.join() }
    }

    dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }.shuffled()
  }

  private suspend fun getQuickPicks() {
    val preferences = context.dataStore.data.first()
    val hideVideoSongs = preferences[HideVideoSongsKey] ?: false
    val hidden = database.hiddenFromQuickPicksSongIds().first().toSet()
    fun List<Song>.eligible() = filterQuickPickSongs(
      this, hidden, preferences[BlockedArtistsKey].orEmpty(),
      preferences[HideExplicitKey] ?: false, hideVideoSongs,
    )
    when (quickPicksEnum.first()) {
      QuickPicks.QUICK_PICKS -> {
        val relatedSongs = database.quickPicks().first().eligible()
        val forgotten =
          database.forgottenFavorites().first().eligible().take(8)

        val recentSong = database.events().first().firstOrNull()?.song
        val ytSimilarSongs = mutableListOf<Song>()

        if (recentSong != null) {
          val endpoint =
            YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
          if (endpoint != null) {
            YouTube.related(endpoint).onSuccess { page ->
              page.songs.take(10).forEach { ytSong ->
                database.song(ytSong.id).first()?.let { localSong ->
                  if (!hideVideoSongs || !localSong.song.isVideo) {
                    ytSimilarSongs.add(localSong)
                  }
                }
              }
            }
          }
        }

        val combined =
          (relatedSongs + forgotten + ytSimilarSongs).eligible().shuffled().take(20)

        quickPicksSource.value =
          combined
            .ifEmpty { relatedSongs.shuffled().take(20) }
            .ifEmpty {
              database
                .songs(SongSortType.CREATE_DATE, true)
                .first()
                .eligible()
                .shuffled()
                .take(20)
            }
      }
      QuickPicks.LAST_LISTEN -> {
        val song = database.events().first().firstOrNull()?.song
        if (song != null && database.hasRelatedSongs(song.id)) {
          quickPicksSource.value =
            database
              .getRelatedSongs(song.id)
              .first()
              .eligible()
              .shuffled()
              .take(20)
        } else {
          val fallbackSongs = database.quickPicks().first().eligible()
          quickPicksSource.value =
            fallbackSongs.shuffled().take(20).ifEmpty {
              database
                .songs(SongSortType.CREATE_DATE, true)
                .first()
                .eligible()
                .shuffled()
                .take(20)
            }
        }
      }
    }
  }

  private suspend fun getCommunityPlaylists() {
    val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 4
    val artistSeeds =
      database
        .mostPlayedArtists(fromTimeStamp, limit = 10)
        .first()
        .filter { it.artist.isYouTubeArtist }
        .shuffled()
        .take(3)
    val songSeeds = database.mostPlayedSongs(fromTimeStamp, limit = 5).first().shuffled().take(2)

    val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

    kotlinx.coroutines.coroutineScope {
      artistSeeds.map { seed ->
        launch(Dispatchers.IO) {
          YouTube.artist(seed.id).onSuccess { page ->
            page.sections.forEach { section ->
              section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                if (
                  playlist.author?.name != "YouTube Music" &&
                    playlist.author?.name != "YouTube" &&
                    playlist.author?.name != "Playlist" &&
                    playlist.author?.name != seed.artist.name &&
                    !playlist.id.startsWith("RD") &&
                    !playlist.id.startsWith("OLAK")
                ) {
                  candidatePlaylists.add(playlist)
                }
              }
            }
          }
        }
      }

      songSeeds.map { seed ->
        launch(Dispatchers.IO) {
          val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
          if (endpoint != null) {
            YouTube.related(endpoint).onSuccess { page ->
              page.playlists.forEach { playlist ->
                if (
                  playlist.author?.name != "YouTube Music" &&
                    playlist.author?.name != "YouTube" &&
                    playlist.author?.name != "Playlist" &&
                    !playlist.id.startsWith("RD") &&
                    !playlist.id.startsWith("OLAK")
                ) {
                  candidatePlaylists.add(playlist)
                }
              }
            }
          }
        }
      }
    }

    val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)

    val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

    kotlinx.coroutines.coroutineScope {
      uniqueCandidates
        .map { playlist ->
          launch(Dispatchers.IO) {
            YouTube.playlist(playlist.id).onSuccess { page ->
              val songs = page.songs.take(10)
              if (songs.isNotEmpty()) {

                val songCountText = page.playlist.songCountText ?: playlist.songCountText
                val updatedPlaylist = playlist.copy(songCountText = songCountText)
                playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
              }
            }
          }
        }
        .forEach { it.join() }
    }

    communityPlaylists.value = playlists.shuffled()
  }

  private suspend fun loadLocalDataPhase() {
    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

    getQuickPicks()

    forgottenFavorites.value =
      database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).shuffled().take(20)

    val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2
    val keepListeningSongs =
      database
        .mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
        .first()
        .filterVideoSongs(hideVideoSongs)
        .shuffled()
        .take(10)
    val keepListeningAlbums =
      database
        .mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
        .first()
        .filter { it.album.thumbnailUrl != null }
        .shuffled()
        .take(5)
    val keepListeningArtists =
      database
        .mostPlayedArtists(fromTimeStamp)
        .first()
        .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
        .shuffled()
        .take(5)
    keepListening.value =
      (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

    allLocalItems.value =
      (quickPicks.value.orEmpty() +
          forgottenFavorites.value.orEmpty() +
          keepListening.value.orEmpty())
        .filter { it is Song || it is Album }
  }

  private suspend fun loadSimilarRecommendations() {
    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
    val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2

            val recommendationSource =
                context.dataStore.data.first()[RecommendationSourceKey]
                    .toEnum(RecommendationSource.YOUTUBE)

            if (recommendationSource == RecommendationSource.SPOTIFY) {
                val spotifyRecs = try {
                        loadSpotifyRecommendationsUseCase(
                            hideExplicit = hideExplicit,
                            fromTimeStamp = fromTimeStamp,
                        )
                    } catch (e: Exception) { null }

                if (!spotifyRecs.isNullOrEmpty()) {
                    similarRecommendations.value = spotifyRecs.shuffled()
                    // updateAllYtItems()
                    return
                }
            }


    coroutineScope {
      val artistDeferreds =
        database
          .mostPlayedArtists(fromTimeStamp, limit = 15)
          .first()
          .filter { it.artist.isYouTubeArtist }
          .shuffled()
          .take(4)
          .map { artist ->
            async(Dispatchers.IO) {
              val items = mutableListOf<YTItem>()
              YouTube.artist(artist.id).onSuccess { page ->
                page.sections.takeLast(3).forEach { section -> items += section.items }
              }
              SimilarRecommendation(
                title = artist,
                items =
                  items
                    .distinctBy { item -> item.id }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .shuffled()
                    .take(12)
                    .ifEmpty {
                      return@async null
                    }
              )
            }
          }

      val songDeferreds =
        database
          .mostPlayedSongs(fromTimeStamp, limit = 15)
          .first()
          .filter { it.album != null }
          .shuffled()
          .take(3)
          .map { song ->
            async(Dispatchers.IO) {
              val endpoint =
                YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                  ?: return@async null
              val page = YouTube.related(endpoint).getOrNull() ?: return@async null
              SimilarRecommendation(
                title = song,
                items =
                  (page.songs.shuffled().take(10) +
                      page.albums.shuffled().take(5) +
                      page.artists.shuffled().take(3) +
                      page.playlists.shuffled().take(3))
                    .distinctBy { it.id }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .shuffled()
                    .ifEmpty {
                      return@async null
                    }
              )
            }
          }

      val albumDeferreds =
        database
          .mostPlayedAlbums(fromTimeStamp, limit = 10)
          .first()
          .filter { it.album.thumbnailUrl != null }
          .shuffled()
          .take(2)
          .map { album ->
            async(Dispatchers.IO) {
              val items = mutableListOf<YTItem>()
              YouTube.album(album.id).onSuccess { page -> page.otherVersions.let { items += it } }
              album.artists.firstOrNull()?.id?.let { artistId ->
                YouTube.artist(artistId).onSuccess { page ->
                  page.sections.lastOrNull()?.items?.let { items += it }
                }
              }
              SimilarRecommendation(
                title = album,
                items =
                  items
                    .distinctBy { it.id }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .shuffled()
                    .take(10)
                    .ifEmpty {
                      return@async null
                    }
              )
            }
          }

      val results = (artistDeferreds + songDeferreds + albumDeferreds).awaitAll()
      similarRecommendations.value = results.filterNotNull().shuffled()
    }
  }

  private suspend fun loadNetworkDataPhase() {
    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

    coroutineScope {
      launch(Dispatchers.IO) { loadCarouselRadioSongs() }
      launch(Dispatchers.IO) { getDailyDiscover() }
      launch(Dispatchers.IO) { getCommunityPlaylists() }
      launch(Dispatchers.IO) { loadSimilarRecommendations() }
      launch(Dispatchers.IO) {
        YouTube.home()
          .onSuccess { page ->
            homePage.value =
              page.copy(
                sections =
                  page.sections.mapNotNull { section ->
                    // Podcasts have their own chip; keep the music Home free of them.
                    val filteredItems =
                      section.items
                        .filterPodcasts()
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                  }
              )
          }
          .onFailure { reportException(it) }
      }
      launch(Dispatchers.IO) {
        YouTube.explore()
          .onSuccess { page ->
            explorePage.value =
              page.copy(newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit))
          }
          .onFailure { reportException(it) }
      }
      if (YouTube.cookie != null) {
        launch(Dispatchers.IO) { loadAccountPlaylists() }
      }
    }

    allYtItems.value =
      similarRecommendations.value?.flatMap { it.items }.orEmpty() +
        homePage.value?.sections?.flatMap { it.items }.orEmpty()
  }

  private suspend fun load() {
    isLoading.value = true

    loadLocalDataPhase()

    loadNetworkDataPhase()

    isLoading.value = false
  }

  private val _isLoadingMore = MutableStateFlow(false)

  fun loadMoreYouTubeItems(continuation: String?) {
    if (continuation == null || _isLoadingMore.value) return
    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

    viewModelScope.launch(Dispatchers.IO) {
      _isLoadingMore.value = true
      var currentContinuation = continuation
      var hasNewItems = false

      while (currentContinuation != null && !hasNewItems) {
        val nextSections = YouTube.home(currentContinuation).getOrNull() ?: break
        currentContinuation = nextSections.continuation

        val isMainHome = selectedChip.value == null
        val newSections =
          nextSections.sections.mapNotNull { section ->
            val filteredItems =
              section.items
                .let { if (isMainHome) it.filterPodcasts() else it }
                .filterExplicit(hideExplicit)
                .filterVideoSongs(hideVideoSongs)
                .filterYoutubeShorts(hideYoutubeShorts)
            if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
          }

        if (newSections.isNotEmpty()) {
          hasNewItems = true
        }

        homePage.value =
          nextSections.copy(
            chips = homePage.value?.chips,
            continuation = currentContinuation,
            sections = homePage.value?.sections.orEmpty() + newSections
          )
      }
      _isLoadingMore.value = false
    }
  }

  fun toggleChip(chip: HomePage.Chip?) {
    if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
      homePage.value = previousHomePage.value
      previousHomePage.value = null
      selectedChip.value = null
      return
    }

    if (selectedChip.value == null) {
      previousHomePage.value = homePage.value
    }

    viewModelScope.launch(Dispatchers.IO) {
      val hideExplicit = context.dataStore.get(HideExplicitKey, false)
      val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
      val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
      val browseId = chip.endpoint?.browseId ?: "FEmusic_home"
      val nextSections =
        YouTube.home(browseId = browseId, params = chip.endpoint?.params).getOrNull()
          ?: return@launch

      homePage.value =
        nextSections.copy(
          chips = homePage.value?.chips,
          sections =
            nextSections.sections.map { section ->
              section.copy(
                items =
                  section.items
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .filterYoutubeShorts(hideYoutubeShorts)
              )
            }
        )
      selectedChip.value = chip
    }
  }

  private suspend fun loadAccountPlaylists() = accountPlaylistMutex.withLock {
    fetchAccountPlaylists()
  }

  private suspend fun fetchAccountPlaylists() {
    val cookie = YouTube.cookie ?: return
    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
    YouTube.library("FEmusic_liked_playlists")
      .completed()
      .onSuccess {
        if (YouTube.cookie != cookie) return@onSuccess
        accountPlaylists.value =
          it.items
            .filterIsInstance<PlaylistItem>()
            .filterNot { it.id == "SE" }
            .filterYoutubeShorts(hideYoutubeShorts)
      }
      .onFailure { reportException(it) }

    if (YouTube.cookie != cookie) return
    val playlists = accountPlaylists.value.orEmpty()
    val semaphore = Semaphore(3)
    val songs = coroutineScope {
      val library = async {
        val result = withTimeoutOrNull(15000) {
          YouTube.library("FEmusic_liked_videos").completed().getOrNull()
            ?.items.orEmpty().filterIsInstance<SongItem>()
        }.orEmpty()
        addCarouselAccountSongs(cookie, result)
        result
      }
      val playlistSongs = (listOf("LM") + playlists.map { it.id })
        .distinct()
        .filterNot { it == "SE" || (hideYoutubeShorts && it.startsWith("SS")) }
        .map { id ->
          async {
            semaphore.withPermit {
              var firstSongs = emptyList<SongItem>()
              withTimeoutOrNull(15000) {
                val page = YouTube.playlist(id)
                firstSongs = page.getOrNull()?.songs.orEmpty()
                addCarouselAccountSongs(cookie, firstSongs)
                val allSongs = page.completed().getOrNull()?.songs ?: firstSongs
                addCarouselAccountSongs(cookie, allSongs)
                allSongs
              } ?: firstSongs
            }
          }
        }
        .awaitAll()
      library.await() + playlistSongs.flatten()
    }
    if (YouTube.cookie == cookie) {
      carouselAccountSongs.update { current -> (current + songs).distinctBy { it.id } }
      if (carouselRadioSongs.value.isEmpty() && songs.isNotEmpty()) {
        loadCarouselRadioSongs()
      }
    }
  }

  private fun addCarouselAccountSongs(cookie: String, songs: List<SongItem>) {
    if (YouTube.cookie == cookie) {
      carouselAccountSongs.update { current -> (current + songs).distinctBy { it.id } }
    }
  }

  private suspend fun loadCarouselRadioSongs() {
    val seeds =
      selectHeroCarouselSongs(
        recentlyPlayed = database.recentlyPlayedSongs().first()
          .filterNot { it.song.isLocal }.map { it.toCarouselSong() },
        library = database.carouselLibrarySongs().first()
          .filterNot { it.song.isLocal }.map { it.toCarouselSong() },
        account = carouselAccountSongs.value,
        excludedSongIds = database.hiddenFromQuickPicksSongIds().first().toSet(),
        blockedArtists = context.dataStore.get(BlockedArtistsKey, emptySet()),
        recommendations = emptyList(),
        hideExplicit = context.dataStore.get(HideExplicitKey, false),
        hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false),
        limit = 3,
      )
    carouselRadioSongs.value = coroutineScope {
      seeds.map { seed ->
        async {
          withTimeoutOrNull(15000) {
            val next = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()
            val related = next?.relatedEndpoint?.let { YouTube.related(it).getOrNull()?.songs }
            (related.orEmpty() + next?.items.orEmpty()).filterNot { it.id == seed.id }
          }.orEmpty()
        }
      }.awaitAll().flatten().distinctBy { it.id }
    }
  }

  fun refresh() {
    if (isRefreshing.value) return
    carouselShuffleSeed.value = Random.nextInt()
    viewModelScope.launch(Dispatchers.IO) {
      try {
        isRefreshing.value = true
        load()
      } finally {
        isRefreshing.value = false
      }
    }

    viewModelScope.launch(Dispatchers.IO) { syncUtils.tryAutoSync() }
  }

  init {

    viewModelScope.launch(Dispatchers.IO) {
      context.dataStore.data
        .map {
          (try {
            it[InnerTubeCookieKey]
          } catch (e: Exception) {
            null
          })
        }
        .distinctUntilChanged()
        .first()

      load()
    }

    viewModelScope.launch(Dispatchers.IO) { syncUtils.tryAutoSync() }

    viewModelScope.launch(Dispatchers.IO) {
      context.dataStore.data
        .map {
          (try {
            it[InnerTubeCookieKey]
          } catch (e: Exception) {
            null
          })
        }
        .distinctUntilChanged()
        .collectLatest { cookie ->
          carouselAccountSongs.value = emptyList()
          accountPlaylists.value = null
          if (!cookie.isNullOrEmpty()) {
            YouTube.cookie = cookie
            YouTube.accountInfo()
              .onSuccess { info ->
                accountName.value = info.name
                accountImageUrl.value = info.thumbnailUrl
              }
              .onFailure { reportException(it) }
            loadAccountPlaylists()
          } else {
            YouTube.cookie = null
            accountName.value = "Guest"
            accountImageUrl.value = null
          }
        }
    }

    viewModelScope.launch(Dispatchers.IO) {
      context.dataStore.data
        .map {
          (try {
            it[HideYoutubeShortsKey]
          } catch (e: Exception) {
            null
          }) ?: false
        }
        .distinctUntilChanged()
        .collect {
          if (YouTube.cookie != null && accountPlaylists.value != null) {
            loadAccountPlaylists()
          }
        }
    }
  }
}
