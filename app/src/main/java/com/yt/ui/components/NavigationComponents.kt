package com.yt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yt.R
import com.yt.data.local.BOTTOM_NAV_SCALE_RANGE
import com.yt.ui.components.layout.YTNavItemSpec
import com.yt.ui.components.layout.rememberYTNavItems
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private const val MAX_VISIBLE_NAV_ITEMS = 5

// Bar metrics at scale 1f. Everything below is these numbers times the user's size setting.
private val NAV_ICON_SIZE = 24.dp
private val NAV_ITEM_VERTICAL_PADDING = 6.dp
private val NAV_BAR_VERTICAL_PADDING = 4.dp
private val NAV_LABEL_GAP = 2.dp
private const val NAV_LABEL_SP = 11f
private val NAV_LABEL_LINE_HEIGHT = 15.dp

// The bar floats: it is inset from the screen edges and rounded on every corner, so content
// passing underneath stays visible around it.
private val NAV_BAR_SIDE_MARGIN = 14.dp
private val NAV_BAR_BOTTOM_MARGIN = 6.dp
private val NAV_BAR_SHADOW = 10.dp
private val NAV_BAR_CORNER = 28.dp
private val NAV_BAR_BLUR_RADIUS = 28.dp
private const val GLASS_TINT_ALPHA = 0.55f
private const val GLASS_BORDER_ALPHA = 0.25f
private const val NAV_BAR_WOBBLE_SQUASH = 0.94f

/**
 * Height the bar occupies at [barScale], without the system navigation-bar inset. Callers reserve
 * this much space at the bottom of the screen so the bar never covers content.
 */
fun bottomNavContentHeight(barScale: Float): Dp {
    val scale = barScale.coerceIn(BOTTOM_NAV_SCALE_RANGE)
    return (NAV_BAR_VERTICAL_PADDING + NAV_ITEM_VERTICAL_PADDING) * 2 * scale +
        (NAV_ICON_SIZE + NAV_LABEL_GAP + NAV_LABEL_LINE_HEIGHT) * scale +
        NAV_BAR_BOTTOM_MARGIN
}

@Composable
fun FloatingBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isHomeEnabled: Boolean = true,
    isShortsEnabled: Boolean = true,
    isMusicEnabled: Boolean = true,
    isSearchEnabled: Boolean = false,
    isCategoriesEnabled: Boolean = false,
    navOrder: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6),
    barScale: Float = 1f,
    glass: Boolean = false,
    hapticsEnabled: Boolean = true,
    hazeState: HazeState? = null,
) {
    val scale = barScale.coerceIn(BOTTOM_NAV_SCALE_RANGE)
    val haptic = LocalHapticFeedback.current
    val selectTab: (Int) -> Unit = { index ->
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onItemSelected(index)
    }
    val enabledItems =
        rememberYTNavItems(
            isHomeEnabled = isHomeEnabled,
            isShortsEnabled = isShortsEnabled,
            isMusicEnabled = isMusicEnabled,
            isSearchEnabled = isSearchEnabled,
            isCategoriesEnabled = isCategoriesEnabled,
            navOrder = navOrder,
        )

    val visibleItems: List<YTNavItemSpec>
    val overflowItems: List<YTNavItemSpec>
    if (enabledItems.size <= MAX_VISIBLE_NAV_ITEMS) {
        visibleItems = enabledItems
        overflowItems = emptyList()
    } else {
        visibleItems = enabledItems.take(MAX_VISIBLE_NAV_ITEMS - 1)
        overflowItems = enabledItems.drop(MAX_VISIBLE_NAV_ITEMS - 1)
    }

    val isOverflowSelected = overflowItems.any { it.index == selectedIndex }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Squash and spring back on every tab change. Skipped for the first composition, which is a
    // selection too but not a switch, and would otherwise wobble the bar on every cold start.
    val wobble = remember { Animatable(1f) }
    var wobbleArmed by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        if (!wobbleArmed) {
            wobbleArmed = true
            return@LaunchedEffect
        }
        wobble.snapTo(NAV_BAR_WOBBLE_SQUASH)
        wobble.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.3f, stiffness = 900f),
        )
    }

    val shape = RoundedCornerShape(NAV_BAR_CORNER)
    // Blurring the real backdrop needs the frame behind the bar, which only the shell can record;
    // without a state to read it from, the glass setting degrades to a translucent bar.
    val blurred = glass && hazeState != null
    val surfaceColor = MaterialTheme.colorScheme.surface
    val glowColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "navBarGlow",
    )
    val hazeStyle =
        HazeStyle(
            backgroundColor = surfaceColor,
            tint = HazeTint(surfaceColor.copy(alpha = GLASS_TINT_ALPHA)),
            blurRadius = NAV_BAR_BLUR_RADIUS,
        )

    Surface(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = wobble.value
                    scaleY = 1f + (1f - wobble.value) * 0.7f
                }.windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = NAV_BAR_SIDE_MARGIN)
                .padding(bottom = NAV_BAR_BOTTOM_MARGIN)
                .fillMaxWidth()
                .shadow(elevation = NAV_BAR_SHADOW, shape = shape, clip = false)
                .then(
                    // Glass has no fill to carry colour, so the bar takes the accent of whichever
                    // tab is selected and wears it as a halo instead.
                    if (blurred) Modifier.glow(color = glowColor, shape = shape) else Modifier,
                ).clip(shape)
                .then(if (hazeState != null && blurred) Modifier.hazeEffect(hazeState, hazeStyle) else Modifier),
        shape = shape,
        color = if (blurred) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        border =
            if (blurred) {
                BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant.copy(alpha = GLASS_BORDER_ALPHA))
            } else {
                null
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = NAV_BAR_VERTICAL_PADDING * scale),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleItems.forEach { spec ->
                BottomNavItem(
                    modifier = Modifier.weight(1f),
                    icon = if (selectedIndex == spec.index) spec.filledIcon else spec.outlinedIcon,
                    label = stringResource(spec.labelRes),
                    selected = selectedIndex == spec.index,
                    scale = scale,
                    onClick = { selectTab(spec.index) },
                )
            }

            if (overflowItems.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    BottomNavItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = if (isOverflowSelected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                        label = stringResource(R.string.nav_more),
                        selected = isOverflowSelected,
                        scale = scale,
                        onClick = { showMoreMenu = true },
                    )
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        offset = DpOffset(x = 0.dp, y = (-8).dp),
                    ) {
                        overflowItems.forEach { spec ->
                            val isSelected = selectedIndex == spec.index
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(spec.labelRes),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) spec.filledIcon else spec.outlinedIcon,
                                        contentDescription = stringResource(spec.labelRes),
                                        tint =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    selectTab(spec.index)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val pressScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "scale",
    )

    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconTint",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .scale(pressScale)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, radius = 28.dp),
                        onClick = onClick,
                    ).padding(horizontal = 12.dp, vertical = NAV_ITEM_VERTICAL_PADDING * scale),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(NAV_ICON_SIZE * scale),
            )

            Spacer(modifier = Modifier.height(NAV_LABEL_GAP * scale))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = iconTint,
                fontSize = (NAV_LABEL_SP * scale).sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
