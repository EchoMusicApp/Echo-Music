package echo.music.iad1tya.ui.component.musicflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.R
import echo.music.iad1tya.musicflow.MusicFlowItem
import echo.music.iad1tya.musicflow.MusicFlowUiState
import echo.music.iad1tya.ui.component.Material3MenuGroup
import echo.music.iad1tya.ui.component.Material3MenuItemData

@Composable
fun MusicFlowTopMenu(
  state: MusicFlowUiState,
  selectedItem: MusicFlowItem?,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onOpenPlayer: () -> Unit,
  onRestoreQueue: () -> Unit,
  onNotInterested: (MusicFlowItem, Boolean) -> Unit,
  onLessLikeThis: (MusicFlowItem) -> Unit,
  onSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Menu Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
      ) {
        Icon(
          painter = painterResource(R.drawable.swipe_vertical),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.music_flow),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.music_flow_settings_description),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Group 1: Navigation & Playback Controls
    val navigationItems = buildList {
      add(
        Material3MenuItemData(
          icon = {
            Icon(
              painter = painterResource(R.drawable.expand_less),
              contentDescription = null,
              modifier = Modifier.size(24.dp),
            )
          },
          title = { Text(stringResource(R.string.music_flow_open_player)) },
          description = { Text(stringResource(R.string.now_playing)) },
          onClick = {
            onDismiss()
            onOpenPlayer()
          },
        )
      )

      if (state.canRestorePreviousQueue) {
        add(
          Material3MenuItemData(
            icon = {
              Icon(
                painter = painterResource(R.drawable.history),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
              )
            },
            title = { Text(stringResource(R.string.music_flow_restore_queue)) },
            description = { Text(stringResource(R.string.action_restore)) },
            onClick = {
              onDismiss()
              onRestoreQueue()
            },
          )
        )
      }

      if (state.selectedIndex > 0) {
        add(
          Material3MenuItemData(
            icon = {
              Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
              )
            },
            title = { Text(stringResource(R.string.music_flow_previous)) },
            onClick = {
              onDismiss()
              onPrevious()
            },
          )
        )
      }

      if (state.selectedIndex < state.items.lastIndex) {
        add(
          Material3MenuItemData(
            icon = {
              Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
              )
            },
            title = { Text(stringResource(R.string.music_flow_next)) },
            onClick = {
              onDismiss()
              onNext()
            },
          )
        )
      }
    }

    if (navigationItems.isNotEmpty()) {
      Material3MenuGroup(items = navigationItems)
    }

    // Group 2: Recommendation Tuning / Feedback
    selectedItem?.let { item ->
      val feedbackItems =
        listOf(
          Material3MenuItemData(
            icon = {
              Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
              )
            },
            title = { Text(stringResource(R.string.music_flow_less_like_this)) },
            description = { Text(stringResource(R.string.music_flow_feedback)) },
            onClick = {
              onDismiss()
              onLessLikeThis(item)
            },
          ),
          Material3MenuItemData(
            icon = {
              Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
              )
            },
            title = { Text(stringResource(R.string.music_flow_not_interested)) },
            description = { Text(stringResource(R.string.music_flow_skip)) },
            onClick = {
              onDismiss()
              onNotInterested(item, false)
            },
          ),
          Material3MenuItemData(
            icon = {
              Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
              )
            },
            title = { Text(stringResource(R.string.music_flow_never_recommend)) },
            description = { Text(stringResource(R.string.remove_from_history)) },
            onClick = {
              onDismiss()
              onNotInterested(item, true)
            },
          ),
        )
      Material3MenuGroup(items = feedbackItems)
    }

    // Group 3: Settings
    val settingsItems =
      listOf(
        Material3MenuItemData(
          icon = {
            Icon(
              painter = painterResource(R.drawable.settings),
              contentDescription = null,
              modifier = Modifier.size(24.dp),
            )
          },
          title = { Text(stringResource(R.string.settings)) },
          description = { Text(stringResource(R.string.music_flow_settings_description)) },
          onClick = {
            onDismiss()
            onSettings()
          },
        )
      )
    Material3MenuGroup(items = settingsItems)
  }
}
