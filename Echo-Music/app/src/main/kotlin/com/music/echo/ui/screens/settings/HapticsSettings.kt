package echo.music.iad1tya.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import echo.music.iad1tya.ui.component.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.HapticIntensity
import echo.music.iad1tya.constants.EnableClickHapticsKey
import echo.music.iad1tya.constants.EnableLongPressHapticsKey
import echo.music.iad1tya.constants.EnableScrollEdgeHapticsKey
import echo.music.iad1tya.constants.EnableSliderHapticsKey
import echo.music.iad1tya.constants.HapticIntensityKey
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.component.EnumDialog
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticsSettings(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
  activity: Activity,
  snackbarHostState: SnackbarHostState,
  highlightKey: String? = null
) {
  val scrollState = rememberScrollState()

  val (enableHaptics, onEnableHapticsChange) =
    rememberPreference(echo.music.iad1tya.constants.EnableHapticsKey, defaultValue = false)
  val (hapticIntensityName, onHapticIntensityChange) =
    rememberPreference(HapticIntensityKey, defaultValue = HapticIntensity.MEDIUM.name)
  val (enableClickHaptics, onEnableClickHapticsChange) =
    rememberPreference(EnableClickHapticsKey, defaultValue = true)
  val (enableLongPressHaptics, onEnableLongPressHapticsChange) =
    rememberPreference(EnableLongPressHapticsKey, defaultValue = true)
  val (enableScrollEdgeHaptics, onEnableScrollEdgeHapticsChange) =
    rememberPreference(EnableScrollEdgeHapticsKey, defaultValue = true)
  val (enableSliderHaptics, onEnableSliderHapticsChange) =
    rememberPreference(EnableSliderHapticsKey, defaultValue = true)

  val hapticIntensity =
    remember(hapticIntensityName) { HapticIntensity.fromName(hapticIntensityName) }
  var showHapticIntensityDialog by rememberSaveable { mutableStateOf(false) }

  if (showHapticIntensityDialog) {
    EnumDialog(
      onDismiss = { showHapticIntensityDialog = false },
      onSelect = {
        onHapticIntensityChange(it.name)
        showHapticIntensityDialog = false
      },
      title = stringResource(R.string.haptics_intensity),
      current = hapticIntensity,
      values = HapticIntensity.entries,
      valueText = {
        when (it) {
          HapticIntensity.LIGHT -> stringResource(R.string.haptic_intensity_light)
          HapticIntensity.MEDIUM -> stringResource(R.string.haptic_intensity_medium)
          HapticIntensity.STRONG -> stringResource(R.string.haptic_intensity_strong)
        }
      }
    )
  }


  Column(
    Modifier.windowInsetsPadding(
        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
      )
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(
      Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
    )
    Spacer(modifier = Modifier.height(16.dp))

    Material3SettingsGroup(
      scrollState = scrollState,
      title = stringResource(R.string.haptics),
      items = listOfNotNull(
        Material3SettingsItem(
          isHighlighted = (highlightKey == stringResource(R.string.enable_haptics)),
          icon = painterResource(R.drawable.vibration),
          title = { Text(stringResource(R.string.enable_haptics)) },
          description = { Text(stringResource(R.string.enable_haptics_desc)) },
          trailingContent = {
            Switch(
              checked = enableHaptics,
              onCheckedChange = onEnableHapticsChange,
              thumbContent = {
                Icon(
                  painter = painterResource(id = if (enableHaptics) R.drawable.check else R.drawable.close),
                  contentDescription = null,
                  modifier = Modifier.size(SwitchDefaults.IconSize)
                )
              }
            )
          },
          onClick = { onEnableHapticsChange(!enableHaptics) }
        ),
        if (enableHaptics) {
          Material3SettingsItem(
            isHighlighted = (highlightKey == stringResource(R.string.haptics_intensity)),
            icon = painterResource(R.drawable.tune),
            title = { Text(stringResource(R.string.haptics_intensity)) },
            description = { Text(stringResource(R.string.haptics_intensity_desc)) },
            trailingContent = {
              Text(
                text = when (hapticIntensity) {
                  HapticIntensity.LIGHT -> stringResource(R.string.haptic_intensity_light)
                  HapticIntensity.MEDIUM -> stringResource(R.string.haptic_intensity_medium)
                  HapticIntensity.STRONG -> stringResource(R.string.haptic_intensity_strong)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            onClick = { showHapticIntensityDialog = true }
          )
        } else null,
        if (enableHaptics) {
          Material3SettingsItem(
            isHighlighted = (highlightKey == stringResource(R.string.haptic_click_feedback)),
            icon = painterResource(R.drawable.radio_button_checked),
            title = { Text(stringResource(R.string.haptic_click_feedback)) },
            description = { Text(stringResource(R.string.haptic_click_feedback_desc)) },
            trailingContent = {
              Switch(
                checked = enableClickHaptics,
                onCheckedChange = onEnableClickHapticsChange,
                thumbContent = {
                  Icon(
                    painter = painterResource(id = if (enableClickHaptics) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                  )
                }
              )
            },
            onClick = { onEnableClickHapticsChange(!enableClickHaptics) }
          )
        } else null,
        if (enableHaptics) {
          Material3SettingsItem(
            isHighlighted = (highlightKey == stringResource(R.string.haptic_long_press_feedback)),
            icon = painterResource(R.drawable.drag_handle),
            title = { Text(stringResource(R.string.haptic_long_press_feedback)) },
            description = { Text(stringResource(R.string.haptic_long_press_feedback_desc)) },
            trailingContent = {
              Switch(
                checked = enableLongPressHaptics,
                onCheckedChange = onEnableLongPressHapticsChange,
                thumbContent = {
                  Icon(
                    painter = painterResource(id = if (enableLongPressHaptics) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                  )
                }
              )
            },
            onClick = { onEnableLongPressHapticsChange(!enableLongPressHaptics) }
          )
        } else null,
        if (enableHaptics) {
          Material3SettingsItem(
            isHighlighted = (highlightKey == stringResource(R.string.haptic_scroll_edge_feedback)),
            icon = painterResource(R.drawable.swipe),
            title = { Text(stringResource(R.string.haptic_scroll_edge_feedback)) },
            description = { Text(stringResource(R.string.haptic_scroll_edge_feedback_desc)) },
            trailingContent = {
              Switch(
                checked = enableScrollEdgeHaptics,
                onCheckedChange = onEnableScrollEdgeHapticsChange,
                thumbContent = {
                  Icon(
                    painter = painterResource(id = if (enableScrollEdgeHaptics) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                  )
                }
              )
            },
            onClick = { onEnableScrollEdgeHapticsChange(!enableScrollEdgeHaptics) }
          )
        } else null,
        if (enableHaptics) {
          Material3SettingsItem(
            isHighlighted = (highlightKey == stringResource(R.string.haptic_slider_feedback)),
            icon = painterResource(R.drawable.sliders),
            title = { Text(stringResource(R.string.haptic_slider_feedback)) },
            description = { Text(stringResource(R.string.haptic_slider_feedback_desc)) },
            trailingContent = {
              Switch(
                checked = enableSliderHaptics,
                onCheckedChange = onEnableSliderHapticsChange,
                thumbContent = {
                  Icon(
                    painter = painterResource(id = if (enableSliderHaptics) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                  )
                }
              )
            },
            onClick = { onEnableSliderHapticsChange(!enableSliderHaptics) }
          )
        } else null
      )
    )
    Spacer(modifier = Modifier.height(16.dp))
    Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)))
  }

  TopAppBar(
    title = { Text(stringResource(R.string.haptics)) },
    navigationIcon = {
      IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    },
    scrollBehavior = scrollBehavior
  )
}
