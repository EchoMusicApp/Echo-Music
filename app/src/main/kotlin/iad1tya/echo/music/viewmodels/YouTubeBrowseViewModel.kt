




package iad1tya.echo.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.innertube.YouTube
import iad1tya.echo.music.innertube.pages.BrowseResult
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.HideVideoKey
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import iad1tya.echo.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    val hideVideo = context.dataStore.get(HideVideoKey, false)
                    result.value = it.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(hideVideo)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
