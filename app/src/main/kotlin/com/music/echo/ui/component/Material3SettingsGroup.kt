package echo.music.iad1tya.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.ui.utils.scrollToOnHighlight

@Composable
fun Material3SettingsGroup(
  title: String? = null,
  compact: Boolean = false,
  scrollState: ScrollState? = null,
  items: List<Material3SettingsItem>
) {
  Column(Modifier.fillMaxWidth()) {
    title?.let {
      Text(
        text = it.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = if (compact) 4.dp else 8.dp, top = if (compact) 4.dp else 8.dp, start = 8.dp)
      )
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
      items.forEachIndexed { index, item ->
        Card(
          modifier = Modifier.fillMaxWidth().animateContentSize().staggerIn(index),
          shape = RoundedCornerShape(if (compact) 20.dp else 26.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp)
        ) {
          Material3SettingsItemRow(item, compact, scrollState)
        }
      }
    }
  }
}

@Composable
private fun Material3SettingsItemRow(item: Material3SettingsItem, compact: Boolean = false, scrollState: ScrollState? = null) {
  Row(
    modifier = Modifier.fillMaxWidth()
      .clickable(enabled = item.enabled && item.onClick != null) { item.onClick?.invoke() }
      .then(if (scrollState != null) Modifier.scrollToOnHighlight(scrollState, item.isHighlighted) else Modifier)
      .padding(horizontal = if (compact) 14.dp else 20.dp, vertical = if (compact) 10.dp else 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (item.customIcon != null) {
      Box(Modifier.size(if (compact) 34.dp else 40.dp).clip(item.iconShape ?: RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { item.customIcon.invoke() }
      Spacer(Modifier.width(if (compact) 14.dp else 20.dp))
    } else item.icon?.let { icon ->
      Box(Modifier.size(if (compact) 34.dp else 40.dp).clip(item.iconShape ?: RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        if (item.showBadge) {
          BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) { SettingIcon(item, icon, compact) }
        } else SettingIcon(item, icon, compact)
      }
      Spacer(Modifier.width(if (compact) 12.dp else 16.dp))
    }

    Column(Modifier.weight(1f)) {
      ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(color = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface)) { item.title() }
      item.description?.let {
        Spacer(Modifier.height(2.dp))
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant)) { it() }
      }
    }
    item.trailingContent?.let { Spacer(Modifier.width(8.dp)); it() }
  }
}

@Composable
private fun SettingIcon(item: Material3SettingsItem, icon: Painter, compact: Boolean) {
  if (item.tintIcon) {
    Icon(painter = icon, contentDescription = null,
      tint = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else if (item.isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
      modifier = Modifier.size(if (compact) 20.dp else 24.dp))
  } else {
    Image(painter = icon, contentDescription = null, modifier = Modifier.size(if (compact) 34.dp else 40.dp), contentScale = ContentScale.Crop)
  }
}

data class Material3SettingsItem(
  val icon: Painter? = null,
  val customIcon: (@Composable () -> Unit)? = null,
  val title: @Composable () -> Unit,
  val description: (@Composable () -> Unit)? = null,
  val trailingContent: (@Composable () -> Unit)? = null,
  val showBadge: Boolean = false,
  val isHighlighted: Boolean = false,
  val tintIcon: Boolean = true,
  val iconShape: Shape? = null,
  val enabled: Boolean = true,
  val onClick: (() -> Unit)? = null
)

/** Settings cards glide up and fade in one after another when a screen opens. */
@Composable
private fun Modifier.staggerIn(index: Int): Modifier {
  val progress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    delay(index.coerceAtMost(10) * 45L)
    progress.animateTo(1f, tween(420, easing = echo.music.iad1tya.ui.utils.PattukalEase))
  }
  return this.graphicsLayer {
    alpha = progress.value
    translationY = (1f - progress.value) * 28.dp.toPx()
  }
}
