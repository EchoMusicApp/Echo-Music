package echo.music.iad1tya.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import echo.music.iad1tya.ai.AiPlaylistGenerator
import echo.music.iad1tya.ai.weather.LocationProvider
import echo.music.iad1tya.ai.weather.WeatherRepository
import echo.music.iad1tya.ai.weather.WeatherUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreateAiPlaylistViewModel @Inject constructor() : ViewModel() {

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _numSongs = MutableStateFlow(15f)
    val numSongs: StateFlow<Float> = _numSongs.asStateFlow()

    private val _weatherEnabled = MutableStateFlow(false)
    val weatherEnabled: StateFlow<Boolean> = _weatherEnabled.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationLog = MutableStateFlow("Initializing...")
    val generationLog: StateFlow<String> = _generationLog.asStateFlow()

    private val _errorLog = MutableStateFlow<String?>(null)
    val errorLog: StateFlow<String?> = _errorLog.asStateFlow()

    private var fetchWeatherJob: Job? = null

    fun onPromptChanged(newPrompt: String) {
        _prompt.value = newPrompt
    }

    fun onNumSongsChanged(newCount: Float) {
        _numSongs.value = newCount
    }

    fun clearError() {
        _errorLog.value = null
    }

    fun resetState() {
        fetchWeatherJob?.cancel()
        fetchWeatherJob = null
        _isGenerating.value = false
        _generationLog.value = "Initializing..."
        _errorLog.value = null
        _prompt.value = ""
        _weatherEnabled.value = false
        _weatherUiState.value = WeatherUiState.Idle
    }

    fun onWeatherToggled(
        enabled: Boolean,
        context: Context,
        hasPermission: Boolean,
        isPermanentlyDenied: Boolean = false
    ) {
        _weatherEnabled.value = enabled
        if (enabled) {
            fetchWeather(context, hasPermission)
        } else {
            fetchWeatherJob?.cancel()
            fetchWeatherJob = null
            _weatherUiState.value = WeatherUiState.Idle
        }
    }

    fun fetchWeather(context: Context, hasPermission: Boolean = true) {
        fetchWeatherJob?.cancel()
        _weatherUiState.value = WeatherUiState.Loading
        fetchWeatherJob = viewModelScope.launch {
            var lat: Double? = null
            var lon: Double? = null

            if (hasPermission) {
                val location = LocationProvider.getLocation(context)
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                }
            }

            // Fallback to IP Location if GPS location unavailable or permission denied
            if (lat == null || lon == null) {
                val ipResult = echo.music.iad1tya.ai.weather.IpLocationRepository.fetchIpLocation()
                ipResult.onSuccess { ipLoc ->
                    lat = ipLoc.latitude
                    lon = ipLoc.longitude
                }
            }

            val currentLat = lat
            val currentLon = lon
            if (currentLat != null && currentLon != null) {
                val result = WeatherRepository.fetchWeather(currentLat, currentLon)
                result.fold(
                    onSuccess = { weatherInfo ->
                        _weatherUiState.value = WeatherUiState.Success(weatherInfo)
                    },
                    onFailure = { error ->
                        _weatherUiState.value = WeatherUiState.Error(
                            message = error.localizedMessage ?: "Failed to fetch weather data. Check your connection."
                        )
                    }
                )
            } else {
                _weatherUiState.value = WeatherUiState.Error(
                    message = "Could not determine location from GPS or IP location."
                )
            }
        }
    }

    fun generatePlaylist(
        context: Context,
        onPlaylistCreated: (String) -> Unit
    ) {
        val currentPrompt = _prompt.value
        val weatherState = _weatherUiState.value
        val isWeatherActive = _weatherEnabled.value && weatherState is WeatherUiState.Success

        if (!isWeatherActive && currentPrompt.isBlank()) return

        _isGenerating.value = true
        _errorLog.value = null

        val activeWeatherInfo = if (isWeatherActive) (weatherState as WeatherUiState.Success).data else null

        viewModelScope.launch {
            val playlistId = AiPlaylistGenerator.generatePlaylist(
                context = context,
                userPrompt = currentPrompt,
                numberOfSongs = _numSongs.value.toInt(),
                weatherInfo = activeWeatherInfo,
                onLog = { log ->
                    withContext(Dispatchers.Main) {
                        _generationLog.value = log
                    }
                }
            )

            if (playlistId != null) {
                _isGenerating.value = false
                _generationLog.value = "Initializing..."
                onPlaylistCreated(playlistId)
            } else {
                _isGenerating.value = false
                _errorLog.value = "Failed to generate playlist. Check logs or settings."
            }
        }
    }
}
