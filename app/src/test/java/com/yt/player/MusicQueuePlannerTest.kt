package com.yt.player

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicQueuePlannerTest {
    private val queue = listOf("joseph", "sinTi", "quebec", "sinTi", "classic")

    @Test
    fun `start index keeps the requested copy of a duplicated track`() {
        assertEquals(3, MusicQueuePlanner.startIndex(queue, requestedIndex = 3, trackId = "sinTi"))
    }

    @Test
    fun `start index falls back to the first copy when the request is stale or unset`() {
        assertEquals(1, MusicQueuePlanner.startIndex(queue, requestedIndex = 2, trackId = "sinTi"))
        assertEquals(1, MusicQueuePlanner.startIndex(queue, requestedIndex = MusicQueuePlanner.INDEX_UNSET, trackId = "sinTi"))
        assertEquals(0, MusicQueuePlanner.startIndex(queue, requestedIndex = 9, trackId = "missing"))
    }

    @Test
    fun `index from only finds copies at or after the given position`() {
        assertEquals(3, MusicQueuePlanner.indexOfFrom(queue, fromIndex = 2, trackId = "sinTi"))
        assertEquals(1, MusicQueuePlanner.indexOfFrom(queue, fromIndex = 1, trackId = "sinTi"))
        assertEquals(MusicQueuePlanner.INDEX_UNSET, MusicQueuePlanner.indexOfFrom(queue, fromIndex = 1, trackId = "joseph"))
        assertEquals(MusicQueuePlanner.INDEX_UNSET, MusicQueuePlanner.indexOfFrom(queue, fromIndex = queue.size, trackId = "classic"))
    }
}
