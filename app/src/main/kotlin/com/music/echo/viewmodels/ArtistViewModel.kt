package echo.music.iad1tya.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.pages.ArtistPage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.applecanvas.AppleMusicCanvasProvider
import echo.music.iad1tya.constants.HideExplicitKey
import echo.music.iad1tya.constants.HideVideoSongsKey
import echo.music.iad1tya.constants.HideYoutubeShortsKey
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.extensions.filterExplicit
import echo.music.iad1tya.extensions.filterExplicitAlbums
import echo.music.iad1tya.extensions.filterVideoSongs as filterVideoSongsLocal
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import echo.music.iad1tya.utils.reportException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
  database: MusicDatabase,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val artistId = savedStateHandle.get<String>("artistId")!!
  var artistPage by mutableStateOf<ArtistPage?>(null)

  private val _artistVideoUrl = MutableStateFlow<String?>(null)
  val artistVideoUrl: StateFlow<String?> = _artistVideoUrl

  private val _artistVideoSong = MutableStateFlow<com.music.innertube.models.SongItem?>(null)
  val artistVideoSong: StateFlow<com.music.innertube.models.SongItem?> = _artistVideoSong

  val libraryArtist = database.artist(artistId).stateIn(viewModelScope, SharingStarted.Lazily, null)
  val librarySongs =
    context.dataStore.data
      .map {
        ((try {
          it[HideExplicitKey]
        } catch (e: Exception) {
          null
        }) ?: false) to
          ((try {
            it[HideVideoSongsKey]
          } catch (e: Exception) {
            null
          }) ?: false)
      }
      .distinctUntilChanged()
      .flatMapLatest { (hideExplicit, hideVideoSongs) ->
        database.artistSongsPreview(artistId).map {
          it.filterExplicit(hideExplicit).filterVideoSongsLocal(hideVideoSongs)
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
  val libraryAlbums =
    context.dataStore.data
      .map {
        (try {
          it[HideExplicitKey]
        } catch (e: Exception) {
          null
        }) ?: false
      }
      .distinctUntilChanged()
      .flatMapLatest { hideExplicit ->
        database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  init {

    viewModelScope.launch {
      context.dataStore.data
        .map {
          Triple(
            (try {
              it[HideExplicitKey]
            } catch (e: Exception) {
              null
            }) ?: false,
            (try {
              it[HideVideoSongsKey]
            } catch (e: Exception) {
              null
            }) ?: false,
            (try {
              it[HideYoutubeShortsKey]
            } catch (e: Exception) {
              null
            }) ?: false,
          )
        }
        .distinctUntilChanged()
        .collect { fetchArtistsFromYTM() }
    }
  }

  fun fetchArtistsFromYTM() {
    if (artistId.startsWith("LOCAL_ARTIST_")) return
    viewModelScope.launch {
      val hideExplicit = context.dataStore.get(HideExplicitKey, false)
      val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
      val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
      YouTube.artist(artistId)
        .onSuccess { page ->
          val filteredSections =
            page.sections
              .map { section ->
                section.copy(
                  items =
                    section.items
                      .filterExplicit(hideExplicit)
                      .filterVideoSongs(hideVideoSongs)
                      .filterYoutubeShorts(hideYoutubeShorts),
                )
              }
              .filter { section -> section.items.isNotEmpty() }

          artistPage = page.copy(sections = filteredSections)

          val topSongsSection =
            page.sections.find { it.items.firstOrNull() is com.music.innertube.models.SongItem }
          // The first top song with an Apple Music canvas, a short looping video made to stand
          // behind the song: the search went on through every song, so the video and the song it
          // plays were those of the last one found. A YouTube music video is not used: cropped to
          // the header it showed stretched, letterboxed frames, and it streamed a whole video.
          topSongsSection?.items?.forEach { item ->
            if (_artistVideoUrl.value != null) return@forEach
            if (item is com.music.innertube.models.SongItem) {
              val canvasUrl =
                AppleMusicCanvasProvider.getBySongArtist(
                    song = item.title,
                    artist = page.artist?.title ?: "",
                  )
                  ?.preferredAnimationUrl

              if (canvasUrl != null) {
                _artistVideoUrl.value = canvasUrl
                _artistVideoSong.value = item
                return@forEach
              }
            }
          }
        }
        .onFailure { reportException(it) }
    }
  }
}
