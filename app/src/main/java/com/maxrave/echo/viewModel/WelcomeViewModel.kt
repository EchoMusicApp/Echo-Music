package iad1tya.echo.music.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import iad1tya.echo.music.data.dataStore.DataStoreManager
import iad1tya.echo.music.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class WelcomeViewModel(
    application: Application,
) : BaseViewModel(application) {
    
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()
    
    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()
    
    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()
    
    init {
        checkFirstLaunch()
        loadUserName()
    }
    
    private fun checkFirstLaunch() {
        viewModelScope.launch {
            try {
                val hasCompleted = dataStoreManager.hasCompletedOnboarding.first()
                _hasCompletedOnboarding.value = hasCompleted
                _isFirstLaunch.value = !hasCompleted
                Log.d(tag, "First launch: ${_isFirstLaunch.value}, Has completed: $hasCompleted")
            } catch (e: Exception) {
                Log.e(tag, "Error checking first launch: ${e.message}")
                // Default to false (not first launch) to prevent getting stuck
                _isFirstLaunch.value = false
                _hasCompletedOnboarding.value = true
            }
        }
    }
    
    private fun loadUserName() {
        viewModelScope.launch {
            try {
                val name = dataStoreManager.userName.first()
                _userName.value = name
                Log.d(tag, "Loaded user name: $name")
            } catch (e: Exception) {
                Log.e(tag, "Error loading user name: ${e.message}")
            }
        }
    }
    
    fun setUserName(name: String) {
        viewModelScope.launch {
            try {
                dataStoreManager.setUserName(name)
                _userName.value = name
                Log.d(tag, "Set user name: $name")
            } catch (e: Exception) {
                Log.e(tag, "Error setting user name: ${e.message}")
            }
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                dataStoreManager.setHasCompletedOnboarding(true)
                _hasCompletedOnboarding.value = true
                _isFirstLaunch.value = false
                Log.d(tag, "Onboarding completed")
            } catch (e: Exception) {
                Log.e(tag, "Error completing onboarding: ${e.message}")
            }
        }
    }
    
    fun resetOnboarding() {
        viewModelScope.launch {
            try {
                dataStoreManager.setHasCompletedOnboarding(false)
                _hasCompletedOnboarding.value = false
                _isFirstLaunch.value = true
                Log.d(tag, "Onboarding reset")
            } catch (e: Exception) {
                Log.e(tag, "Error resetting onboarding: ${e.message}")
            }
        }
    }
    
    fun forceCompleteOnboarding() {
        viewModelScope.launch {
            try {
                dataStoreManager.setHasCompletedOnboarding(true)
                _hasCompletedOnboarding.value = true
                _isFirstLaunch.value = false
                Log.d(tag, "Onboarding force completed")
            } catch (e: Exception) {
                Log.e(tag, "Error force completing onboarding: ${e.message}")
            }
        }
    }
}
