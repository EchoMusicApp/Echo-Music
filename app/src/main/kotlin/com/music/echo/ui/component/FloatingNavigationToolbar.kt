

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
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
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val toolbarContainerColor = floatingToolbarContainerColor(pureBlack = pureBlack)
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = toolbarContainerColor,
    )
    val hasOverflowMenu = (onShuffleClick != null && shuffleIconRes != null) || onMusicRecognitionClick != null
    val hasFabAction = onFabClick != null && fabIconRes != null

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val showSelectedLabels = false

        if (hasOverflowMenu) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarOverflowMenuButton(
                        pureBlack = pureBlack,
                        onShuffleClick = onShuffleClick,
                        shuffleEnabled = shuffleEnabled,
                        shuffleIconRes = shuffleIconRes,
                        onAiHubClick = onAiHubClick,
                        aiHubIconRes = aiHubIconRes,
                        aiHubContentDescription = aiHubContentDescription,
                    )
                },
                modifier = Modifier.widthIn(max = 480.dp),
                colors = toolbarColors,
                scrollBehavior = scrollBehavior,
                animationSpec = FloatingToolbarDefaults.animationSpec(),
            ) {
                ToolbarItemsContainer(
                    items = items,
                    pureBlack = pureBlack,
                    showSelectedLabels = showSelectedLabels,
                    onMusicRecognitionClick = onMusicRecognitionClick,
                    musicRecognitionContentDescription = musicRecognitionContentDescription,
                    isSelected = isSelected,
                    onItemClick = onItemClick
                )
            }
        } else if (hasFabAction) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarFabAction(
                        pureBlack = pureBlack,
                        onClick = onFabClick,
                        iconRes = fabIconRes,
                        contentDescription = fabContentDescription,
                    )
                },
                modifier = Modifier.widthIn(max = 480.dp),
                colors = toolbarColors,
                scrollBehavior = scrollBehavior,
                animationSpec = FloatingToolbarDefaults.animationSpec(),
            ) {
                ToolbarItemsContainer(
                    items = items,
                    pureBlack = pureBlack,
                    showSelectedLabels = showSelectedLabels,
                    onMusicRecognitionClick = onMusicRecognitionClick,
                    musicRecognitionContentDescription = musicRecognitionContentDescription,
                    isSelected = isSelected,
                    onItemClick = onItemClick
                )
            }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.widthIn(max = 420.dp),
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
                    onItemClick = onItemClick
                )
            }
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
    onItemClick: (Screens, Boolean) -> Unit
) {
    val density = LocalDensity.current
    val itemWidths = remember { mutableStateMapOf<Screens, Dp>() }
    val itemPositions = remember { mutableStateMapOf<Screens, Dp>() }

    val activeScreen = items.find { isSelected(it) }
    val targetWidth = itemWidths[activeScreen] ?: 0.dp
    val targetPosition = itemPositions[activeScreen] ?: 0.dp

    val slidingPillWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillWidth"
    )

    val slidingPillOffset by animateDpAsState(
        targetValue = targetPosition,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillOffset"
    )

    Box(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.matchParentSize()) {
            if (targetWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = slidingPillOffset)
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
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        itemWidths[screen] = with(density) { coordinates.size.width.toDp() }
                        itemPositions[screen] = with(density) { coordinates.positionInParent().x.toDp() }
                    }
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
        FloatingToolbarDefaults.VibrantFloatingActionButton(
            onClick = { showSheet = true },
            shape = CircleShape,
            containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
            contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = stringResource(R.string.more_label),
            )
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.more_options),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Material3SettingsGroup(
                        items = buildList {
                            if (onShuffleClick != null && shuffleIconRes != null) {
                                add(
                                    Material3SettingsItem(
                                        title = { Text(stringResource(R.string.shuffle)) },
                                        icon = painterResource(shuffleIconRes),
                                        trailingContent = {
                                            Switch(
                                                checked = shuffleEnabled,
                                                onCheckedChange = {
                                                    onShuffleClick()
                                                },
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        },
                                        onClick = {
                                            onShuffleClick()
                                        }
                                    )
                                )
                            }

                            if (onAiHubClick != null && aiHubIconRes != null) {
                                add(
                                    Material3SettingsItem(
                                        title = { Text(aiHubContentDescription) },
                                        icon = painterResource(aiHubIconRes),
                                        onClick = {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
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

    FloatingToolbarDefaults.VibrantFloatingActionButton(
        onClick = onClick,
        containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
        contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription =
                contentDescription.ifEmpty {
                    stringResource(R.string.create_playlist)
                },
        )
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    showSelectedLabel: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val showLabel = selected && showSelectedLabel
    val transition = updateTransition(targetState = selected, label = "navItem_${screen.route}")

    val contentColor by transition.animateColor(
        transitionSpec = { spring(stiffness = Spring.StiffnessMedium) },
        label = "contentColor",
    ) { isSelected ->
        if (isSelected) floatingToolbarSelectedItemContentColor(pureBlack)
        else floatingToolbarItemContentColor(pureBlack)
    }

    val iconScale by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "iconScale",
    ) { isSelected -> if (isSelected) 1.12f else 1.0f }

    val horizontalPadding by transition.animateDp(
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
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )

    Row(
        modifier = modifier
            .scale(pressScale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .widthIn(min = 48.dp)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(
            targetState = selected,
            animationSpec = spring(
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
            enter = fadeIn(
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                expandFrom = Alignment.Start,
            ),
            exit = fadeOut(
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + shrinkHorizontally(
                animationSpec = spring(
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
    return MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean): Color {
    return MaterialTheme.colorScheme.onPrimaryContainer
}

@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean): Color {
    return MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean): Color {
    return MaterialTheme.colorScheme.onSecondaryContainer
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
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )

    Row(
        modifier = modifier
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
