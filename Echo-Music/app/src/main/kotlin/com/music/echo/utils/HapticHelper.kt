package com.music.echo.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import echo.music.iad1tya.constants.EnableClickHapticsKey
import echo.music.iad1tya.constants.EnableHapticsKey
import echo.music.iad1tya.constants.EnableLongPressHapticsKey
import echo.music.iad1tya.constants.EnableScrollEdgeHapticsKey
import echo.music.iad1tya.constants.EnableSliderHapticsKey
import echo.music.iad1tya.constants.HapticIntensity
import echo.music.iad1tya.constants.HapticIntensityKey
import echo.music.iad1tya.utils.rememberPreference
import kotlinx.coroutines.flow.distinctUntilChanged

enum class HapticType {
  CLICK,
  LONG_PRESS,
  SCROLL_EDGE,
  SLIDER_TICK
}

class HapticHelper(
  private val context: Context,
  private val view: View?,
  val masterEnabled: Boolean,
  val intensity: HapticIntensity,
  val clickEnabled: Boolean,
  val longPressEnabled: Boolean,
  val scrollEdgeEnabled: Boolean,
  val sliderEnabled: Boolean,
) {
  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun performHaptic(type: HapticType) {
    if (!masterEnabled) return

    val categoryEnabled =
      when (type) {
        HapticType.CLICK -> clickEnabled
        HapticType.LONG_PRESS -> longPressEnabled
        HapticType.SCROLL_EDGE -> scrollEdgeEnabled
        HapticType.SLIDER_TICK -> sliderEnabled
      }

    if (!categoryEnabled) return

    val (durationMs, baseAmplitude, feedbackConstant) =
      when (type) {
        HapticType.CLICK -> Triple(12L, 110, HapticFeedbackConstants.KEYBOARD_TAP)
        HapticType.LONG_PRESS -> Triple(35L, 200, HapticFeedbackConstants.LONG_PRESS)
        HapticType.SCROLL_EDGE -> Triple(18L, 140, HapticFeedbackConstants.CONFIRM)
        HapticType.SLIDER_TICK -> Triple(8L, 80, HapticFeedbackConstants.CLOCK_TICK)
      }

    val scaledAmplitude = (baseAmplitude * intensity.scaleFactor).toInt().coerceIn(1, 255)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
      try {
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, scaledAmplitude))
        return
      } catch (_: Exception) {
        // Fallback to View haptics if Vibrator fails
      }
    }

    view?.performHapticFeedback(feedbackConstant)
  }
}

@Composable
fun rememberHapticHelper(): HapticHelper {
  val context = LocalContext.current
  val view = LocalView.current
  val (masterEnabled) = rememberPreference(EnableHapticsKey, defaultValue = false)
  val (intensityName) =
    rememberPreference(HapticIntensityKey, defaultValue = HapticIntensity.MEDIUM.name)
  val (clickEnabled) = rememberPreference(EnableClickHapticsKey, defaultValue = true)
  val (longPressEnabled) = rememberPreference(EnableLongPressHapticsKey, defaultValue = true)
  val (scrollEdgeEnabled) = rememberPreference(EnableScrollEdgeHapticsKey, defaultValue = true)
  val (sliderEnabled) = rememberPreference(EnableSliderHapticsKey, defaultValue = true)

  val intensity = remember(intensityName) { HapticIntensity.fromName(intensityName) }

  return remember(
    context,
    view,
    masterEnabled,
    intensity,
    clickEnabled,
    longPressEnabled,
    scrollEdgeEnabled,
    sliderEnabled
  ) {
    HapticHelper(
      context = context,
      view = view,
      masterEnabled = masterEnabled,
      intensity = intensity,
      clickEnabled = clickEnabled,
      longPressEnabled = longPressEnabled,
      scrollEdgeEnabled = scrollEdgeEnabled,
      sliderEnabled = sliderEnabled,
    )
  }
}

private fun Modifier.hapticScrollEdgeInternal(
  key: Any,
  isScrollInProgress: () -> Boolean,
  canScrollBackward: () -> Boolean,
  canScrollForward: () -> Boolean,
): Modifier = composed {
  val hapticHelper = rememberHapticHelper()

  if (hapticHelper.masterEnabled && hapticHelper.scrollEdgeEnabled) {
    LaunchedEffect(key) {
      var atTopEdge = false
      var atBottomEdge = false

      snapshotFlow {
          val isScrolling = isScrollInProgress()
          val top = isScrolling && !canScrollBackward()
          val bottom = isScrolling && !canScrollForward()
          Pair(top, bottom)
        }
        .distinctUntilChanged()
        .collect { (top, bottom) ->
          if (top && !atTopEdge) {
            hapticHelper.performHaptic(HapticType.SCROLL_EDGE)
            atTopEdge = true
          } else if (!top) {
            atTopEdge = false
          }

          if (bottom && !atBottomEdge) {
            hapticHelper.performHaptic(HapticType.SCROLL_EDGE)
            atBottomEdge = true
          } else if (!bottom) {
            atBottomEdge = false
          }
        }
    }
  }

  this
}

fun Modifier.hapticScrollEdge(lazyListState: LazyListState): Modifier =
  hapticScrollEdgeInternal(
    key = lazyListState,
    isScrollInProgress = { lazyListState.isScrollInProgress },
    canScrollBackward = { lazyListState.canScrollBackward },
    canScrollForward = { lazyListState.canScrollForward }
  )

fun Modifier.hapticScrollEdge(scrollState: ScrollState): Modifier =
  hapticScrollEdgeInternal(
    key = scrollState,
    isScrollInProgress = { scrollState.isScrollInProgress },
    canScrollBackward = { scrollState.canScrollBackward },
    canScrollForward = { scrollState.canScrollForward }
  )

fun Modifier.hapticScrollEdge(lazyGridState: LazyGridState): Modifier =
  hapticScrollEdgeInternal(
    key = lazyGridState,
    isScrollInProgress = { lazyGridState.isScrollInProgress },
    canScrollBackward = { lazyGridState.canScrollBackward },
    canScrollForward = { lazyGridState.canScrollForward }
  )
