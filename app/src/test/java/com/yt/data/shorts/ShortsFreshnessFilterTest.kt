package com.yt.data.shorts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * An empty feed is the failure these pin. Filtering used to return whatever survived, so a page of
 * Shorts the viewer had already watched became an empty screen carrying a "No shorts found" error
 * that the source had never reported.
 */
class ShortsFreshnessFilterTest {
    @Test
    fun `the strict pass wins whenever it leaves anything`() {
        val result =
            freshestNonEmpty(
                items = listOf("fresh", "watched", "seen"),
                id = { it },
                watchedIds = setOf("watched"),
                seenIds = setOf("seen"),
            )

        assertThat(result).containsExactly("fresh")
    }

    @Test
    fun `a page of only-seen Shorts comes back rather than coming back empty`() {
        val result =
            freshestNonEmpty(
                items = listOf("seenA", "seenB"),
                id = { it },
                watchedIds = emptySet(),
                seenIds = setOf("seenA", "seenB"),
            )

        assertThat(result).containsExactly("seenA", "seenB").inOrder()
    }

    @Test
    fun `seen is relaxed before watched`() {
        val result =
            freshestNonEmpty(
                items = listOf("watched", "seen"),
                id = { it },
                watchedIds = setOf("watched"),
                seenIds = setOf("seen"),
            )

        // Merely shown last week is a weaker signal than actually watched, so it is given back
        // first and the watched one stays out while anything else remains.
        assertThat(result).containsExactly("seen")
    }

    @Test
    fun `everything watched still yields the page`() {
        val result =
            freshestNonEmpty(
                items = listOf("a", "b"),
                id = { it },
                watchedIds = setOf("a", "b"),
                seenIds = setOf("a", "b"),
            )

        assertThat(result).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `the Short asked for by name survives every pass`() {
        val result =
            freshestNonEmpty(
                items = listOf("asked", "fresh"),
                id = { it },
                watchedIds = setOf("asked"),
                seenIds = setOf("asked"),
                keepId = "asked",
            )

        assertThat(result).containsExactly("asked", "fresh").inOrder()
    }

    @Test
    fun `an empty source stays empty`() {
        val result =
            freshestNonEmpty(
                items = emptyList<String>(),
                id = { it },
                watchedIds = setOf("a"),
                seenIds = setOf("b"),
            )

        assertThat(result).isEmpty()
    }
}
