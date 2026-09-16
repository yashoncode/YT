package com.yt.ui.screens.home

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.yt.data.local.HomeFeedColumns
import com.yt.ui.components.FeedGridLayout
import com.yt.ui.components.rememberFeedGridLayout

internal data class HomeLayoutConfig(
    val cells: GridCells,
    val columns: Int,
    val contentPadding: Dp,
    val cardSpacing: Dp,
    val shortsShelfAfterIndex: Int,
)

@Composable
internal fun rememberHomeLayoutConfig(
    maxWidth: Dp,
    columnPreference: HomeFeedColumns = HomeFeedColumns.AUTO,
): HomeLayoutConfig {
    val base = rememberFeedGridLayout(maxWidth, columnPreference)
    return remember(base) { resolveHomeLayoutConfig(base) }
}

internal fun resolveHomeLayoutConfig(base: FeedGridLayout): HomeLayoutConfig =
    HomeLayoutConfig(
        cells = base.cells,
        columns = base.columns,
        contentPadding = base.contentPadding,
        cardSpacing = base.cardSpacing,
        // The shelf spans every column, so it has to start on a fresh row or the row above it
        // renders with holes in it.
        shortsShelfAfterIndex = base.columns,
    )
