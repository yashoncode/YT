package com.yt.ui.screens.shorts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Swipe-down-to-refresh for the Shorts pager.
 *
 * Material's `PullToRefreshBox` is a nested scroll *parent* that consumes part of the drag to move
 * its indicator. That is fine above a list, but above a full-screen snapping pager it competes for
 * the same vertical gesture the pager needs, and the pager is the whole screen. This connection
 * only watches: every callback returns zero, so the pager receives the drag exactly as it would
 * with no parent at all.
 *
 * [onPostScroll] sees leftover downward drag only when the child could not use it — which on a
 * vertical pager means the first short is on screen and there is nothing above it. So no explicit
 * page check is needed here.
 */
internal class ShortsPullToRefreshConnection(
    private val thresholdPx: Float,
    private val onRefresh: () -> Unit,
) : NestedScrollConnection {
    private var pulled = 0f

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero

        // Any upward movement means the user is browsing, not pulling. Reset rather than letting a
        // slow back-and-forth drag creep over the threshold.
        if (available.y <= 0f) {
            pulled = 0f
            return Offset.Zero
        }

        pulled += available.y
        if (pulled >= thresholdPx) {
            pulled = 0f
            onRefresh()
        }
        return Offset.Zero
    }

    /** A finished gesture starts the next pull from nothing, whatever it left on the counter. */
    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        pulled = 0f
        return Velocity.Zero
    }
}
