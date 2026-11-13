package androidx.hilt.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel as navHiltViewModel

// Newer Hilt versions moved hiltViewModel to this package; the app imports it from here.
@Composable
inline fun <reified VM : ViewModel> hiltViewModel(): VM = navHiltViewModel()
