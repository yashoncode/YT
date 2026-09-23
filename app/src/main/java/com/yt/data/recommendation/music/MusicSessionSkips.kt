package com.yt.data.recommendation.music

/**
 * Artists the user skipped during the current listening session. A skip pushes that
 * artist to the back of every list ranked until the session goes quiet for
 * [MusicBrainParams.SESSION_GAP_MS] or the user listens to them properly again.
 * Ephemeral and never persisted: long-term dislike is the affinity score's job.
 */
internal class MusicSessionSkips {
    private val skippedAt = HashMap<String, Long>()

    @Volatile
    var snapshot: Set<String> = emptySet()
        private set

    /** Not thread-safe; callers hold the engine mutex. */
    fun record(
        artistKey: String,
        tier: MusicSkipTier,
        nowMs: Long,
    ) {
        if (tier == MusicSkipTier.NONE) skippedAt.remove(artistKey) else skippedAt[artistKey] = nowMs
        active(nowMs)
    }

    /** Not thread-safe; callers hold the engine mutex. Also refreshes [snapshot]. */
    fun active(nowMs: Long): Set<String> {
        skippedAt.entries.removeAll { nowMs - it.value >= MusicBrainParams.SESSION_GAP_MS }
        snapshot = skippedAt.keys.toSet()
        return snapshot
    }
}
