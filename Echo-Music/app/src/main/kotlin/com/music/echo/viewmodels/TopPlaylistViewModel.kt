package echo.music.iad1tya.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.constants.HideVideoSongsKey
import echo.music.iad1tya.constants.MyTopFilter
import echo.music.iad1tya.db.MusicDatabase
import echo.music.iad1tya.utils.dataStore
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TopPlaylistViewModel
@Inject
constructor(
  @ApplicationContext context: Context,
  database: MusicDatabase,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val top = savedStateHandle.get<String>("top")!!

  val topPeriod = MutableStateFlow(MyTopFilter.ALL_TIME)

  @OptIn(ExperimentalCoroutinesApi::class)
  val topSongs =
    combine(
        topPeriod,
        context.dataStore.data
          .map {
            (try {
              it[HideVideoSongsKey]
            } catch (e: Exception) {
              null
            }) ?: false
          }
          .distinctUntilChanged()
      ) { period, hideVideoSongs ->
        period to hideVideoSongs
      }
      .flatMapLatest { (period, hideVideoSongs) ->
        database.mostPlayedSongs(
          period.toTimeMillis(),
          top.toInt(),
          hideVideoSongs = hideVideoSongs
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
