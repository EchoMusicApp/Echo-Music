


package iad1tya.echo.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import iad1tya.echo.music.network.NetworkBannerUiState
import iad1tya.echo.music.network.ObserveNetworkBannerStateUseCase

@HiltViewModel
class NetworkBannerViewModel
@Inject
constructor(
    observeNetworkBannerStateUseCase: ObserveNetworkBannerStateUseCase,
) : ViewModel() {
    val bannerState: StateFlow<NetworkBannerUiState> =
        observeNetworkBannerStateUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NetworkBannerUiState.Hidden,
            )
}
