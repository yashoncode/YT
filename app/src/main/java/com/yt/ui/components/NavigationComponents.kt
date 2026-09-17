package com.yt.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

private const val MAX_VISIBLE_NAV_ITEMS = 5

// Bar metrics at scale 1f. Everything below is these numbers times the user's size setting.
private val NAV_ICON_SIZE = 24.dp
private val NAV_ITEM_VERTICAL_PADDING = 6.dp
private val NAV_BAR_VERTICAL_PADDING = 4.dp
private val NAV_LABEL_GAP = 2.dp
private const val NAV_LABEL_SP = 11f
private val NAV_LABEL_LINE_HEIGHT = 15.dp
private const val GLASS_BAR_ALPHA = 0.72f

/**
 * Height of the bar's own content at [barScale], without the system navigation-bar inset. Callers
 * reserve this much space at the bottom of the screen so the bar never covers content.
 */
fun bottomNavContentHeight(barScale: Float): Dp {
    val scale = barScale.coerceIn(BOTTOM_NAV_SCALE_RANGE)
    return (NAV_BAR_VERTICAL_PADDING + NAV_ITEM_VERTICAL_PADDING) * 2 * scale +
        (NAV_ICON_SIZE + NAV_LABEL_GAP + NAV_LABEL_LINE_HEIGHT) * scale
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        // Translucent instead of blurred: a real backdrop blur needs a frame capture of whatever is
        // behind the bar, which Compose cannot do on its own.
        // ponytail: translucent glass, swap in a backdrop-blur library if it has to blur for real.
        color =
            if (glass) {
                MaterialTheme.colorScheme.surface.copy(alpha = GLASS_BAR_ALPHA)
            } else {
                MaterialTheme.colorScheme.surface
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Translucency alone leaves the bar bleeding into the content scrolling under it.
            if (glass) {
                HorizontalDivider(
                    thickness = Dp.Hairline,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
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
