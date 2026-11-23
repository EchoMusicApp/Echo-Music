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
class BottomPlaylistViewModel
@Inject
constructor(
  @ApplicationContext context: Context,
  database: MusicDatabase,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val bottom = savedStateHandle.get<String>("bottom")!!

  val bottomPeriod = MutableStateFlow(MyTopFilter.ALL_TIME)

  @OptIn(ExperimentalCoroutinesApi::class)
  val bottomSongs =
    combine(
        bottomPeriod,
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
        database.leastPlayedSongs(
          period.toTimeMillis(),
          bottom.toInt(),
          hideVideoSongs = hideVideoSongs
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
