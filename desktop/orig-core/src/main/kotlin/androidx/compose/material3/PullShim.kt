package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState

// Expressive LoadingIndicator is not in the desktop material3; the standard pull indicator does the same job.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopPullLoadingIndicator(state: PullToRefreshState, isRefreshing: Boolean, modifier: Modifier = Modifier) =
  PullToRefreshDefaults.Indicator(state = state, isRefreshing = isRefreshing, modifier = modifier)
