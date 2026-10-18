package echo.music.iad1tya.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
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
  val scheme = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(if (compact) 22.dp else 28.dp)
  Column(Modifier.fillMaxWidth()) {
    title?.let {
      Text(
        text = it.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 1.4.sp),
        color = scheme.primary.copy(alpha = 0.9f),
        modifier = Modifier.padding(bottom = if (compact) 4.dp else 8.dp, top = if (compact) 4.dp else 8.dp, start = 14.dp)
      )
    }

    // One liquid-glass panel per group; rows are separated by hairlines instead of separate cards.
    Column(
      Modifier.fillMaxWidth()
        .animateContentSize()
        .staggerIn(0)
        .clip(shape)
        .background(
          Brush.verticalGradient(
            listOf(scheme.surfaceContainerHigh.copy(alpha = 0.72f), scheme.surfaceContainer.copy(alpha = 0.60f))
          )
        )
        .border(
          1.dp,
          Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.20f), scheme.outlineVariant.copy(alpha = 0.10f), scheme.primary.copy(alpha = 0.22f))
          ),
          shape,
        )
    ) {
      items.forEachIndexed { index, item ->
        Material3SettingsItemRow(item, compact, scrollState)
        if (index < items.lastIndex) {
          Box(
            Modifier.fillMaxWidth()
              .padding(start = if (compact) 62.dp else 76.dp, end = 18.dp)
              .height(1.dp)
              .background(scheme.outlineVariant.copy(alpha = 0.22f))
          )
        }
      }
    }
  }
}

@Composable
private fun Material3SettingsItemRow(item: Material3SettingsItem, compact: Boolean = false, scrollState: ScrollState? = null) {
  val scheme = MaterialTheme.colorScheme
  val iconBox = if (compact) 36.dp else 42.dp
  Row(
    modifier = Modifier.fillMaxWidth()
      .clickable(enabled = item.enabled && item.onClick != null) { item.onClick?.invoke() }
      .then(if (scrollState != null) Modifier.scrollToOnHighlight(scrollState, item.isHighlighted) else Modifier)
      .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 9.dp else 13.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (item.customIcon != null) {
      Box(Modifier.size(iconBox).clip(item.iconShape ?: CircleShape), contentAlignment = Alignment.Center) { item.customIcon.invoke() }
      Spacer(Modifier.width(14.dp))
    } else item.icon?.let { icon ->
      // icons sit in a soft tinted glass bubble
      Box(
        Modifier.size(iconBox)
          .clip(if (item.tintIcon) CircleShape else (item.iconShape ?: CircleShape))
          .then(
            if (item.tintIcon)
              Modifier.background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.24f), scheme.primary.copy(alpha = 0.07f))))
                .border(1.dp, scheme.primary.copy(alpha = 0.25f), CircleShape)
            else Modifier
          ),
        contentAlignment = Alignment.Center
      ) {
        if (item.showBadge) {
          BadgedBox(badge = { Badge(containerColor = scheme.error) }) { SettingIcon(item, icon, compact) }
        } else SettingIcon(item, icon, compact)
      }
      Spacer(Modifier.width(14.dp))
    }

    Column(Modifier.weight(1f)) {
      ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(color = if (!item.enabled) scheme.onSurface.copy(alpha = 0.38f) else scheme.onSurface)) { item.title() }
      item.description?.let {
        Spacer(Modifier.height(1.dp))
        ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = if (!item.enabled) scheme.onSurface.copy(alpha = 0.38f) else scheme.onSurfaceVariant)) { it() }
      }
    }
    if (item.trailingContent != null) {
      Spacer(Modifier.width(8.dp)); item.trailingContent.invoke()
    } else if (item.onClick != null) {
      Icon(
        painter = painterResource(echo.music.iad1tya.R.drawable.chevron_right_px),
        contentDescription = null,
        tint = scheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.size(18.dp),
      )
    }
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
