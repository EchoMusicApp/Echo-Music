package androidx.hilt.navigation.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import echo.music.iad1tya.di.DesktopGraph

/** Desktop version of Hilt's hiltViewModel(): builds the view model from the hand-wired [DesktopGraph]. */
@Composable
inline fun <reified VM : ViewModel> hiltViewModel(): VM = run {
  // like Hilt: a screen's view model gets its navigation arguments through SavedStateHandle
  val handle = (androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current as? androidx.navigation.NavBackStackEntry)?.savedStateHandle
  viewModel { DesktopGraph.create(VM::class, handle) }
}
