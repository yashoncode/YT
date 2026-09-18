package com.yt.ui.screens.shorts

import com.yt.data.local.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The target height is part of the playback-stream cache key, so the pager and the ViewModel's
 * prefetch have to agree on it exactly. These pin the shared derivation both now use; if they ever
 * compute it separately again, a prefetch silently resolves one entry and the pager misses it.
 */
class ShortsPlaybackTargetTest {
    @Test
    fun `wifi and cellular select their own configured quality`() {
        assertEquals(
            1080,
            shortsTargetHeight(
                isWifi = true,
                wifiQuality = VideoQuality.Q_1080P,
                cellularQuality = VideoQuality.Q_480P,
                autoHeight = MEASURED,
            ),
        )
        assertEquals(
            480,
            shortsTargetHeight(
                isWifi = false,
                wifiQuality = VideoQuality.Q_1080P,
                cellularQuality = VideoQuality.Q_480P,
                autoHeight = MEASURED,
            ),
        )
    }

    @Test
    fun `the transport is what decides, not which quality is higher`() {
        val onCellular =
            shortsTargetHeight(
                isWifi = false,
                wifiQuality = VideoQuality.Q_360P,
                cellularQuality = VideoQuality.Q_1080P,
                autoHeight = MEASURED,
            )

        assertEquals(1080, onCellular)
    }

    @Test
    fun `auto takes the measured height, not the unconstrained one`() {
        val wifi =
            shortsTargetHeight(
                isWifi = true,
                wifiQuality = VideoQuality.AUTO,
                cellularQuality = VideoQuality.Q_480P,
                autoHeight = MEASURED,
            )

        // Auto used to fall through as AUTO.height, which is 0, and 0 means "no cap" downstream --
        // so the setting a viewer picks to let the app judge asked for the largest stream there
        // was, on any connection.
        assertEquals(MEASURED, wifi)
    }

    @Test
    fun `a measured height only applies where auto is configured`() {
        val pinned =
            shortsTargetHeight(
                isWifi = true,
                wifiQuality = VideoQuality.Q_720P,
                cellularQuality = VideoQuality.AUTO,
                autoHeight = 240,
            )

        assertEquals(720, pinned)
    }

    private companion object {
        /** Stands in for whatever the bandwidth meter last reported. */
        const val MEASURED = 360
    }
}
