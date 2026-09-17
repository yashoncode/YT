package com.yt.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import com.yt.R
import com.yt.ui.components.FloatingBottomNavBar
import com.yt.ui.utils.LocalWindowSizeClass
import com.yt.ui.utils.isExpandedWidth
import com.yt.ui.utils.isMediumHeight
import dev.chrisbanes.haze.HazeState

const val YT_NAV_BAR_TAG = "yt_nav_bar"
const val YT_NAV_RAIL_TAG = "yt_nav_rail"

internal data class YTNavItemSpec(
    val index: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val labelRes: Int,
)

/**
 * Whether this window is wide enough to navigate from the start edge instead of the bottom. The
 * decision belongs to the window's size class, so a phone in a narrow split window keeps the bar
 * even on a large display.
 */
@Composable
fun flowUsesNavigationRail(): Boolean = LocalWindowSizeClass.current.let { it.isExpandedWidth && it.isMediumHeight }

@Composable
internal fun rememberYTNavItems(
    isHomeEnabled: Boolean,
    isShortsEnabled: Boolean,
    isMusicEnabled: Boolean,
    isSearchEnabled: Boolean,
    isCategoriesEnabled: Boolean,
    navOrder: List<Int>,
): List<YTNavItemSpec> {
    val shortsIcon = ImageVector.vectorResource(id = R.drawable.ic_shorts)
    return remember(isHomeEnabled, isShortsEnabled, isMusicEnabled, isSearchEnabled, isCategoriesEnabled, navOrder) {
        val items =
            buildList {
                if (isHomeEnabled) add(YTNavItemSpec(0, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home))
                if (isShortsEnabled) add(YTNavItemSpec(1, shortsIcon, shortsIcon, R.string.nav_shorts))
                if (isMusicEnabled) add(YTNavItemSpec(2, Icons.Filled.MusicNote, Icons.Outlined.MusicNote, R.string.nav_music))
                if (isSearchEnabled) add(YTNavItemSpec(5, Icons.Filled.Search, Icons.Outlined.Search, R.string.nav_search))
                if (isCategoriesEnabled) add(YTNavItemSpec(6, Icons.Filled.Explore, Icons.Outlined.Explore, R.string.nav_explore))
            }
        val order = navOrder.withIndex().associate { it.value to it.index }
        items.sortedBy { order[it.index] ?: Int.MAX_VALUE }
    }
}

/**
 * The app's primary navigation surface: the floating bar along the bottom on compact and medium
 * windows, the Material 3 wide navigation rail along the start edge once the window is expanded.
 *
 * The rail is laid out beside the content rather than over it, so it reports its measured width
 * through [onRailWidthChanged] for the caller to reserve.
 */
@Composable
fun BoxScope.YTNavigationChrome(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    visible: Boolean,
    scrolledAway: Boolean,
    onRailWidthChanged: (Dp) -> Unit,
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
    if (flowUsesNavigationRail()) {
        val items =
            rememberYTNavItems(
                isHomeEnabled = isHomeEnabled,
                isShortsEnabled = isShortsEnabled,
                isMusicEnabled = isMusicEnabled,
                isSearchEnabled = isSearchEnabled,
                isCategoriesEnabled = isCategoriesEnabled,
                navOrder = navOrder,
            )
        if (visible) {
            YTNavigationRail(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = onItemSelected,
                onWidthChanged = onRailWidthChanged,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    } else {
        AnimatedVisibility(
            visible = visible && !scrolledAway,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter =
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 320f),
                ) + fadeIn(animationSpec = tween(160, delayMillis = 40)),
            exit =
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
                ) + fadeOut(animationSpec = tween(120)),
        ) {
            FloatingBottomNavBar(
                selectedIndex = selectedIndex,
                onItemSelected = onItemSelected,
                modifier = Modifier.testTag(YT_NAV_BAR_TAG),
                isHomeEnabled = isHomeEnabled,
                isShortsEnabled = isShortsEnabled,
                isMusicEnabled = isMusicEnabled,
                isSearchEnabled = isSearchEnabled,
                isCategoriesEnabled = isCategoriesEnabled,
                navOrder = navOrder,
                barScale = barScale,
                glass = glass,
                hapticsEnabled = hapticsEnabled,
                hazeState = hazeState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun YTNavigationRail(
    items: List<YTNavItemSpec>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onWidthChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    WideNavigationRail(
        modifier =
            modifier
                .fillMaxHeight()
                .testTag(YT_NAV_RAIL_TAG)
                .onSizeChanged { size -> onWidthChanged(with(density) { size.width.toDp() }) },
    ) {
        items.forEach { spec ->
            val selected = selectedIndex == spec.index
            WideNavigationRailItem(
                selected = selected,
                onClick = { onItemSelected(spec.index) },
                icon = {
                    Icon(
                        imageVector = if (selected) spec.filledIcon else spec.outlinedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(spec.labelRes)) },
                railExpanded = false,
            )
        }
    }
}
