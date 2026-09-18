package com.yt.ui.screens.shorts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsPullToRefreshConnectionTest {
    private var refreshes = 0
    private val connection = ShortsPullToRefreshConnection(thresholdPx = 100f) { refreshes++ }

    private fun drag(
        dy: Float,
        source: NestedScrollSource = NestedScrollSource.UserInput,
    ): Offset = connection.onPostScroll(Offset.Zero, Offset(0f, dy), source)

    @Test
    fun `never consumes any of the drag`() {
        assertEquals(Offset.Zero, drag(80f))
        assertEquals(Offset.Zero, drag(80f))
        assertEquals(Offset.Zero, drag(-80f))
    }

    @Test
    fun `refreshes once the pull passes the threshold`() {
        drag(60f)
        assertEquals(0, refreshes)
        drag(60f)
        assertEquals(1, refreshes)
    }

    @Test
    fun `an upward drag resets the pull`() {
        drag(90f)
        drag(-10f)
        drag(90f)
        assertEquals(0, refreshes)
    }

    @Test
    fun `ignores scrolling the user did not do`() {
        drag(200f, NestedScrollSource.SideEffect)
        assertEquals(0, refreshes)
    }

    @Test
    fun `starts from nothing after the threshold fires`() {
        drag(120f)
        assertEquals(1, refreshes)
        drag(90f)
        assertEquals(1, refreshes)
    }
}
