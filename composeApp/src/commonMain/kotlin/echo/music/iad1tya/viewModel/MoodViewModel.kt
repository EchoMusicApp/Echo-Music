package echo.music.iad1tya.viewModel

import androidx.lifecycle.viewModelScope
import echo.music.iad1tya.common.SELECTED_LANGUAGE
import echo.music.iad1tya.domain.data.model.mood.moodmoments.MoodsMomentObject
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.repository.HomeRepository
import echo.music.iad1tya.domain.utils.Resource
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MoodViewModel(
    dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _moodsMomentObject: MutableStateFlow<MoodsMomentObject?> = MutableStateFlow(null)
    var moodsMomentObject: StateFlow<MoodsMomentObject?> = _moodsMomentObject
    var loading = MutableStateFlow<Boolean>(false)

    private var regionCode: String? = null
    private var language: String? = null

    /**
     * Params whose result is already in [_moodsMomentObject]. MoodScreen calls [getMood] from a
     * `LaunchedEffect(params)`, which fires again every time the screen re-enters composition —
     * i.e. on every back navigation — so without this the shelf was refetched each time.
     */
    private var loadedParams: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun getMood(params: String) {
        if (params == loadedParams && _moodsMomentObject.value != null) return
        loading.value = true
        viewModelScope.launch {
//            mainRepository.getMood(params, regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect{ values ->
//                _moodsMomentObject.value = values
//            }
            homeRepository.getMoodData(params).collect { values ->
                Logger.w("MoodViewModel", "getMood: $values")
                when (values) {
                    is Resource.Success -> {
                        _moodsMomentObject.value = values.data
                        // Only remember it once it actually succeeded, so a failed load retries.
                        loadedParams = params
                    }

                    is Resource.Error -> {
                        _moodsMomentObject.value = null
                        loadedParams = null
                    }
                }
            }
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
}