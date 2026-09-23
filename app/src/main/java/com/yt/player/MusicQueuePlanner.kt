package com.yt.player

internal object MusicQueuePlanner {
    const val INDEX_UNSET = -1

    fun currentQueueIndex(
        queueIds: List<String>,
        playerIndex: Int,
        currentTrackId: String?,
    ): Int {
        if (playerIndex in queueIds.indices && queueIds[playerIndex] == currentTrackId) {
            return playerIndex
        }

        if (currentTrackId == null) return INDEX_UNSET

        return queueIds.indexOf(currentTrackId)
    }

    fun playNextInsertionIndex(
        queueIds: List<String>,
        playerIndex: Int,
        currentTrackId: String?,
    ): Int {
        val currentIndex = currentQueueIndex(queueIds, playerIndex, currentTrackId)
        return if (currentIndex == INDEX_UNSET) {
            queueIds.size
        } else {
            (currentIndex + 1).coerceIn(0, queueIds.size)
        }
    }

    fun indexOfFrom(
        queueIds: List<String>,
        fromIndex: Int,
        trackId: String,
    ): Int {
        for (index in fromIndex.coerceAtLeast(0) until queueIds.size) {
            if (queueIds[index] == trackId) return index
        }
        return INDEX_UNSET
    }

    /** A requested start index is trusted only when it still points at the requested track. */
    fun startIndex(
        queueIds: List<String>,
        requestedIndex: Int,
        trackId: String,
    ): Int =
        if (queueIds.getOrNull(requestedIndex) == trackId) {
            requestedIndex
        } else {
            queueIds.indexOf(trackId).coerceAtLeast(0)
        }

    fun shouldForcePendingPlayNext(
        isAutomaticTransition: Boolean,
        pendingMediaId: String?,
        pendingPlayerIndex: Int,
        actualMediaId: String?,
        actualPlayerIndex: Int,
    ): Boolean {
        val pendingIndexMissed = pendingPlayerIndex != INDEX_UNSET && pendingPlayerIndex != actualPlayerIndex
        return isAutomaticTransition &&
            pendingMediaId != null &&
            actualMediaId != null &&
            (actualMediaId != pendingMediaId || pendingIndexMissed)
    }
}
