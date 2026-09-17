package com.yt.ui

import com.yt.data.local.DEFAULT_NAV_TAB_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDestinationsTest {
    @Test
    fun hiddenHomeFallsBackToFirstVisibleDestination() {
        val visibility = NavigationVisibility(home = false, shorts = false, music = true)

        val resolved =
            resolveDefaultNavTabIndex(
                preferredIndex = 0,
                order = listOf(0, 2, 1, 5, 6),
                visibility = visibility,
            )

        assertEquals(2, resolved)
        assertFalse(visibleNavTabIndices(listOf(0, 2), visibility).contains(0))
    }

    @Test
    fun reEnabledHomeRestoresAHomeDefault() {
        val resolved =
            resolveDefaultNavTabIndex(
                preferredIndex = 0,
                order = listOf(2, 0, 1),
                visibility = NavigationVisibility(home = true),
            )

        assertEquals(0, resolved)
    }

    @Test
    fun channelLinksOpenTheChannelRoute() {
        assertEquals(
            "channel?url=https%3A%2F%2Fwww.youtube.com%2Fchannel%2FUCXuqSBlHAE6Xw-yeJA0Tunw",
            youtubeChannelDeepLinkRoute("https://www.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw"),
        )
        assertEquals(
            "channel?url=https%3A%2F%2Fwww.youtube.com%2Fchannel%2FUCXuqSBlHAE6Xw-yeJA0Tunw",
            youtubeChannelDeepLinkRoute("https://m.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw?si=abc"),
        )
    }

    @Test
    fun linksBrowseCannotOpenAreNotChannelRoutes() {
        assertEquals(null, youtubeChannelDeepLinkRoute("https://www.youtube.com/@LinusTechTips"))
        assertEquals(null, youtubeChannelDeepLinkRoute("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(null, youtubeChannelDeepLinkRoute("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(null, youtubeChannelDeepLinkRoute("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals(null, youtubeChannelDeepLinkRoute("https://www.youtube.com/c/LinusTechTips"))
    }

    @Test
    fun channelHandlesUseHandleUrls() {
        assertEquals("https://www.youtube.com/@flow", youtubeChannelUrl("@flow"))
        assertEquals("https://www.youtube.com/@flow", youtubeChannelUrl("flow"))
        assertEquals(
            "https://www.youtube.com/channel/UC123",
            youtubeChannelUrl("UC123"),
        )
    }

    @Test
    fun malformedHandleChannelUrlsAreRepaired() {
        assertEquals(
            "https://www.youtube.com/@flow",
            youtubeChannelUrl("https://youtube.com/channel/@flow"),
        )
        assertEquals(
            "https://www.youtube.com/@flow",
            youtubeChannelUrl("https://m.youtube.com/@flow/videos"),
        )
    }

    @Test
    fun channelRoutesEncodeCanonicalUrls() {
        assertEquals(
            "channel?url=https%3A%2F%2Fwww.youtube.com%2F%40flow",
            youtubeChannelRoute("@flow"),
        )
    }

    /**
     * Settings, Subscriptions and Library live in the top bar of every root destination, which only
     * works if a root destination always exists. Every tab is hideable now, so Home is the floor —
     * this pins that down so no visibility combination can leave the shell with nowhere to start.
     */
    @Test
    fun everyVisibilityCombinationKeepsAnUnhideableRootDestination() {
        val orders =
            listOf(
                DEFAULT_NAV_TAB_ORDER,
                listOf(6, 5, 2, 1, 0),
                listOf(2, 1),
                emptyList(),
            )

        for (bits in 0 until 32) {
            val visibility =
                NavigationVisibility(
                    home = bits and 1 != 0,
                    shorts = bits and 2 != 0,
                    music = bits and 4 != 0,
                    search = bits and 8 != 0,
                    categories = bits and 16 != 0,
                )

            for (order in orders) {
                val visible = visibleNavTabIndices(order, visibility)
                assertTrue(
                    "no visible tab for $visibility / $order",
                    visible.isNotEmpty(),
                )
                assertFalse(
                    "a hidden tab is still in the bar for $visibility / $order",
                    visible.contains(3) || visible.contains(4),
                )

                val resolved = resolveDefaultNavTabIndex(0, order, visibility)
                assertTrue(
                    "resolved default $resolved is not visible for $visibility / $order",
                    visible.contains(resolved),
                )
            }
        }
    }
}
