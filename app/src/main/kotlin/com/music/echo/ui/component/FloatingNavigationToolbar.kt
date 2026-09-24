@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package echo.music.iad1tya.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.screens.Screens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNavigationToolbar(
  items: List<Screens>,
  pureBlack: Boolean,
  modifier: Modifier = Modifier,
  onFabClick: (() -> Unit)? = null,
  fabIconRes: Int? = null,
  fabContentDescription: String = "",
  onShuffleClick: (() -> Unit)? = null,
  shuffleEnabled: Boolean = false,
  shuffleIconRes: Int? = null,
  shuffleContentDescription: String = "",
  onMusicRecognitionClick: (() -> Unit)? = null,
  musicRecognitionContentDescription: String = "",
  onAiHubClick: (() -> Unit)? = null,
  aiHubIconRes: Int? = null,
  aiHubContentDescription: String = "",
  scrollBehavior: FloatingToolbarScrollBehavior? = null,
  onSearchLongClick: (() -> Unit)? = null,
  isSelected: (Screens) -> Boolean,
  onItemClick: (Screens, Boolean) -> Unit,
) {
  val toolbarContainerColor =
    if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
  android.util.Log.d(
    "COLOR_MATCH",
    "NavBar - pureBlack parameter: $pureBlack, toolbarContainerColor: $toolbarContainerColor"
  )
  val toolbarColors =
    FloatingToolbarDefaults.standardFloatingToolbarColors(
      toolbarContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    )

  val outlineColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
  val toolbarModifier =
    androidx.compose.ui.Modifier.clip(
        androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
      )
      .background(toolbarContainerColor)
      .border(
        1.dp,
        outlineColor.copy(alpha = 0.3f),
        androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
      )

  val hasOverflowMenu =
    (onShuffleClick != null && shuffleIconRes != null) || onMusicRecognitionClick != null
  val hasFabAction = onFabClick != null && fabIconRes != null

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    val showSelectedLabels = true

    HorizontalFloatingToolbar(
      expanded = true,
      modifier = Modifier.widthIn(max = 480.dp).then(toolbarModifier),
      colors = toolbarColors,
      scrollBehavior = scrollBehavior,
    ) {
      ToolbarItemsContainer(
        items = items,
        pureBlack = pureBlack,
        showSelectedLabels = showSelectedLabels,
        onMusicRecognitionClick = onMusicRecognitionClick,
        musicRecognitionContentDescription = musicRecognitionContentDescription,
        isSelected = isSelected,
        onItemClick = onItemClick,
        hasOverflowMenu = hasOverflowMenu,
        onShuffleClick = onShuffleClick,
        shuffleEnabled = shuffleEnabled,
        shuffleIconRes = shuffleIconRes,
        onAiHubClick = onAiHubClick,
        aiHubIconRes = aiHubIconRes,
        aiHubContentDescription = aiHubContentDescription,
        hasFabAction = hasFabAction,
        onFabClick = onFabClick,
        fabIconRes = fabIconRes,
        fabContentDescription = fabContentDescription,
        onSearchLongClick = onSearchLongClick,
      )
    }
  }
}

@Composable
private fun ToolbarItemsContainer(
  items: List<Screens>,
  pureBlack: Boolean,
  showSelectedLabels: Boolean,
  onMusicRecognitionClick: (() -> Unit)?,
  musicRecognitionContentDescription: String,
  isSelected: (Screens) -> Boolean,
  onItemClick: (Screens, Boolean) -> Unit,
  hasOverflowMenu: Boolean = false,
  onShuffleClick: (() -> Unit)? = null,
  shuffleEnabled: Boolean = false,
  shuffleIconRes: Int? = null,
  onAiHubClick: (() -> Unit)? = null,
  aiHubIconRes: Int? = null,
  aiHubContentDescription: String = "",
  hasFabAction: Boolean = false,
  onFabClick: (() -> Unit)? = null,
  fabIconRes: Int? = null,
  fabContentDescription: String = "",
  onSearchLongClick: (() -> Unit)? = null
) {
  val density = LocalDensity.current
  val itemWidths = remember { mutableStateMapOf<Screens, Dp>() }
  val itemPositions = remember { mutableStateMapOf<Screens, Dp>() }

  val activeScreen = items.find { isSelected(it) }
  val targetWidth = itemWidths[activeScreen] ?: 0.dp
  val targetPosition = itemPositions[activeScreen] ?: 0.dp

  val slidingPillWidth by
    animateDpAsState(
      targetValue = targetWidth,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "pillWidth"
    )

  val slidingPillOffset by
    animateDpAsState(
      targetValue = targetPosition,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "pillOffset"
    )

  Box(modifier = Modifier.height(IntrinsicSize.Min)) {
    Box(modifier = Modifier.matchParentSize()) {
      if (targetWidth > 0.dp) {
        Box(
          modifier =
            Modifier.offset(x = slidingPillOffset)
              .width(slidingPillWidth)
              .fillMaxHeight()
              .background(
                color = floatingToolbarSelectedItemContainerColor(pureBlack),
                shape = RoundedCornerShape(24.dp)
              )
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      items.forEach { screen ->
        if (screen == Screens.Library && onMusicRecognitionClick != null) {
          FloatingNavigationToolbarActionItem(
            iconRes = R.drawable.mic,
            contentDescription = musicRecognitionContentDescription,
            pureBlack = pureBlack,
            onClick = onMusicRecognitionClick
          )
        }

        val selected = isSelected(screen)
        FloatingNavigationToolbarItem(
          screen = screen,
          selected = selected,
          showSelectedLabel = showSelectedLabels,
          pureBlack = pureBlack,
          onClick = { onItemClick(screen, selected) },
          onLongClick = if (screen == Screens.Search) onSearchLongClick else null,
          modifier =
            Modifier.onGloballyPositioned { coordinates ->
              itemWidths[screen] = with(density) { coordinates.size.width.toDp() }
              itemPositions[screen] = with(density) { coordinates.positionInParent().x.toDp() }
            }
        )
      }

      if (hasOverflowMenu) {
        FloatingToolbarOverflowMenuButton(
          pureBlack = pureBlack,
          onShuffleClick = onShuffleClick,
          shuffleEnabled = shuffleEnabled,
          shuffleIconRes = shuffleIconRes,
          onAiHubClick = onAiHubClick,
          aiHubIconRes = aiHubIconRes,
          aiHubContentDescription = aiHubContentDescription
        )
      } else if (hasFabAction && fabIconRes != null && onFabClick != null) {
        FloatingToolbarFabAction(
          pureBlack = pureBlack,
          onClick = onFabClick,
          iconRes = fabIconRes,
          contentDescription = fabContentDescription
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingToolbarOverflowMenuButton(
  pureBlack: Boolean,
  onShuffleClick: (() -> Unit)?,
  shuffleEnabled: Boolean,
  shuffleIconRes: Int?,
  onAiHubClick: (() -> Unit)?,
  aiHubIconRes: Int?,
  aiHubContentDescription: String,
) {
  var showSheet by rememberSaveable { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  Box {
    FloatingNavigationToolbarActionItem(
      iconRes = R.drawable.more_horiz,
      contentDescription = stringResource(R.string.more_label),
      pureBlack = pureBlack,
      onClick = { showSheet = true },
    )

    if (showSheet) {
      ModalBottomSheet(
        onDismissRequest = { showSheet = false },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = stringResource(R.string.more_options),
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          )

          Material3SettingsGroup(
            compact = true,
            items =
              buildList {
                if (onShuffleClick != null && shuffleIconRes != null) {
                  add(
                    Material3SettingsItem(
                      title = { Text(stringResource(R.string.shuffle)) },
                      icon = painterResource(shuffleIconRes),
                      trailingContent = {
                        Switch(
                          checked = shuffleEnabled,
                          onCheckedChange = { onShuffleClick() },
                          modifier = Modifier.scale(0.8f),
                          colors =
                            androidx.compose.material3.SwitchDefaults.colors(
                              checkedThumbColor = MaterialTheme.colorScheme.surface,
                              checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                              uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                              uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                      },
                      onClick = { onShuffleClick() }
                    )
                  )
                }

                if (onAiHubClick != null && aiHubIconRes != null) {
                  add(
                    Material3SettingsItem(
                      title = { Text(aiHubContentDescription) },
                      icon = painterResource(aiHubIconRes),
                      onClick = {
                        scope
                          .launch { sheetState.hide() }
                          .invokeOnCompletion {
                            if (!sheetState.isVisible) {
                              showSheet = false
                              onAiHubClick()
                            }
                          }
                      }
                    )
                  )
                }
              }
          )
        }
      }
    }
  }
}

@Composable
private fun FloatingToolbarFabAction(
  pureBlack: Boolean,
  onClick: (() -> Unit)?,
  iconRes: Int?,
  contentDescription: String,
) {
  if (onClick == null || iconRes == null) return

  FloatingNavigationToolbarActionItem(
    iconRes = iconRes,
    contentDescription = contentDescription.ifEmpty { stringResource(R.string.create_playlist) },
    pureBlack = pureBlack,
    onClick = onClick
  )
}

@Composable
private fun FloatingNavigationToolbarItem(
  screen: Screens,
  selected: Boolean,
  showSelectedLabel: Boolean,
  pureBlack: Boolean,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(24.dp)
  val showLabel = selected && showSelectedLabel
  val transition = updateTransition(targetState = selected, label = "navItem_${screen.route}")

  val contentColor by
    transition.animateColor(
      transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
      label = "contentColor",
    ) { isSelected ->
      if (isSelected) floatingToolbarSelectedItemContentColor(pureBlack)
      else floatingToolbarItemContentColor(pureBlack)
    }

  val iconScale by
    transition.animateFloat(
      transitionSpec = {
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        )
      },
      label = "iconScale",
    ) { isSelected ->
      if (isSelected) 1.12f else 1.0f
    }

  val horizontalPadding by
    transition.animateDp(
      transitionSpec = {
        spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow,
        )
      },
      label = "horizontalPadding",
    ) { isSelected ->
      if (isSelected && showSelectedLabel) 16.dp else 12.dp
    }

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.91f else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
      label = "pressScale",
    )

  Row(
    modifier =
      modifier
        .scale(pressScale)
        .clip(shape)
        .combinedClickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          role = Role.Tab,
          onClick = onClick,
          onLongClick = onLongClick,
        )
        .widthIn(min = 48.dp)
        .padding(horizontal = horizontalPadding, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Crossfade(
      targetState = selected,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "iconCrossfade",
      modifier = Modifier.scale(iconScale),
    ) { isSelected ->
      Icon(
        painter = painterResource(if (isSelected) screen.iconIdActive else screen.iconIdInactive),
        contentDescription = stringResource(screen.titleId),
        tint = contentColor,
      )
    }

    AnimatedVisibility(
      visible = showLabel,
      enter =
        fadeIn(
          spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) +
          expandHorizontally(
            animationSpec =
              spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
              ),
            expandFrom = Alignment.Start,
          ),
      exit =
        fadeOut(
          spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
        ) +
          shrinkHorizontally(
            animationSpec =
              spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
              ),
            shrinkTowards = Alignment.Start,
          ),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = stringResource(screen.titleId),
          color = contentColor,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun floatingToolbarContainerColor(pureBlack: Boolean): Color {
  return if (pureBlack) {
    Color.Black
  } else {
    MaterialTheme.colorScheme.surfaceContainer
  }
}

@Composable
private fun floatingToolbarFabContainerColor(pureBlack: Boolean): Color {
  return MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
}

@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean): Color {
  return MaterialTheme.colorScheme.onSurface
}

@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean): Color {
  return MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
}

@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean): Color {
  return MaterialTheme.colorScheme.onSurface
}

@Composable
private fun floatingToolbarItemContentColor(pureBlack: Boolean): Color {
  return MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun FloatingNavigationToolbarActionItem(
  iconRes: Int,
  contentDescription: String,
  pureBlack: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.91f else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
      label = "pressScale",
    )

  Row(
    modifier =
      modifier
        .scale(pressScale)
        .clip(RoundedCornerShape(24.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          role = Role.Button,
          onClick = onClick,
        )
        .widthIn(min = 48.dp)
        .padding(horizontal = 12.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = floatingToolbarItemContentColor(pureBlack),
    )
  }
}
