package iad1tya.echo.music.ui.screens.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.eq.EqualizerService
import iad1tya.echo.music.eq.data.EQProfileRepository
import iad1tya.echo.music.eq.data.ParametricEQParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject


@HiltViewModel
class EQViewModel @Inject constructor(
    private val eqProfileRepository: EQProfileRepository,
    private val equalizerService: EqualizerService
) : ViewModel() {

    private val _state = MutableStateFlow(EQState())
    val state: StateFlow<EQState> = _state.asStateFlow()

    init {
        loadProfiles()
    }

    
    private fun loadProfiles() {
        
        viewModelScope.launch {
            eqProfileRepository.profiles.collect { _ ->
                val sortedProfiles = eqProfileRepository.getSortedProfiles()
                _state.update {
                    it.copy(profiles = sortedProfiles)
                }
            }
        }

        
        viewModelScope.launch {
            eqProfileRepository.activeProfile.collect { activeProfile ->
                _state.update {
                    it.copy(activeProfileId = activeProfile?.id)
                }
            }
        }
    }

    
    fun selectProfile(profileId: String?) {
        viewModelScope.launch {
            if (profileId == null) {
                
                equalizerService.disable()
                eqProfileRepository.setActiveProfile(null)
            } else {
                
                val profile = _state.value.profiles.find { it.id == profileId }
                if (profile != null) {
                    val result = equalizerService.applyProfile(profile)
                    result.onSuccess {
                        eqProfileRepository.setActiveProfile(profileId)
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message ?: "Unknown error") }
                    }
                }
            }
        }
    }

    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            eqProfileRepository.deleteProfile(profileId)
        }
    }

    
    fun importCustomProfile(
        fileName: String,
        inputStream: InputStream,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                
                val content = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                
                val parametricEQ = ParametricEQParser.parseText(content)

                
                val validationErrors = ParametricEQParser.validate(parametricEQ)
                if (validationErrors.isNotEmpty()) {
                    onError(Exception("Invalid EQ file: ${validationErrors.first()}"))
                    return@launch
                }

                
                val profileName = fileName.removeSuffix(".txt")

                
                eqProfileRepository.importCustomProfile(profileName, parametricEQ)

                _state.update { it.copy(importStatus = "Successfully imported $profileName") }
                onSuccess()
            } catch (e: Exception) {
                onError(Exception("Failed to import EQ profile: ${e.message}"))
            }
        }
    }
}