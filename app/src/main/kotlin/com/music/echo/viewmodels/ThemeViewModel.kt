

package iad1tya.echo.music.viewmodels

import androidx.lifecycle.ViewModel
import iad1tya.echo.music.ui.screens.settings.DarkMode
import iad1tya.echo.music.ui.theme.DefaultThemeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel : ViewModel() {
    
    private val _darkMode = MutableStateFlow(DarkMode.AUTO)
    val darkMode: StateFlow<DarkMode> = _darkMode.asStateFlow()

    private val _pureBlack = MutableStateFlow(false)
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _selectedThemeColorInt = MutableStateFlow(DefaultThemeColor.hashCode())
    val selectedThemeColorInt: StateFlow<Int> = _selectedThemeColorInt.asStateFlow()

    fun updateDarkMode(mode: DarkMode) {
        _darkMode.value = mode
    }

    fun updatePureBlack(enabled: Boolean) {
        _pureBlack.value = enabled
    }

    fun updateThemeColor(colorInt: Int) {
        _selectedThemeColorInt.value = colorInt
    }
}
