package echo.music.iad1tya.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import echo.music.iad1tya.db.MusicDatabase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ArtistAlbumsViewModel
@Inject
constructor(
  database: MusicDatabase,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private val artistId = savedStateHandle.get<String>("artistId")!!
  val artist = database.artist(artistId).stateIn(viewModelScope, SharingStarted.Lazily, null)

  val albums =
    database
      .artistAlbumsPreview(artistId)
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
