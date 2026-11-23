package echo.music.iad1tya.ui.utils

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarScrollBehavior(
  state: TopAppBarState = rememberTopAppBarState(),
  canScroll: () -> Boolean = { true },
  snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
  flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): TopAppBarScrollBehavior =
  AppBarScrollBehavior(
    state = state,
    snapAnimationSpec = snapAnimationSpec,
    flingAnimationSpec = flingAnimationSpec,
    canScroll = canScroll,
  )

@ExperimentalMaterial3Api
class AppBarScrollBehavior(
  override val state: TopAppBarState,
  override val snapAnimationSpec: AnimationSpec<Float>?,
  override val flingAnimationSpec: DecayAnimationSpec<Float>?,
  val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
  override val isPinned: Boolean = true
  override var nestedScrollConnection =
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Return Offset.Zero so the AppBar remains pinned and never hides
        return Offset.Zero
      }

      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
      ): Offset {
        // Return Offset.Zero so the AppBar remains pinned and never hides
        return Offset.Zero
      }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun TopAppBarState.resetHeightOffset() {
  if (heightOffset != 0f) {
    animate(
      initialValue = heightOffset,
      targetValue = 0f,
    ) { value, _ ->
      heightOffset = value
    }
  }
}
