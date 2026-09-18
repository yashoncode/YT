// ==================================================================================================
// This implementation was based on metrolist's (https://github.com/MetrolistGroup/Metrolist)
// ==================================================================================================

package com.yt.data.lyrics

import android.content.Context
import android.util.Log
import com.yt.data.local.PlayerPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class LyricsHelper(
    private val context: Context,
) {
    companion object {
        private const val TAG = "LyricsHelper"

        // Back to 8s after 5s proved too tight: a provider on a weak connection needs longer than
        // one on a fast one, and cutting the deadline turned a slow answer into no answer at all.
        private const val PER_PROVIDER_TIMEOUT_MS = 8_000L
        private const val MAX_TOTAL_TIMEOUT_MS = 20_000L
        private const val PROVIDER_COOLDOWN_MS = 10 * 60 * 1000L

        /**
         * How many providers are in flight at once.
         *
         * Not all of them: eight simultaneous requests share one connection, so on a weak link each
         * is slower than it would have been alone and they can all miss their deadline together —
         * the first provider queried serially at least had the whole pipe to itself. Three overlaps
         * enough to hide the latency of a provider that has nothing without starving the one that
         * does.
         */
        private const val PROVIDER_BATCH = 3
    }

    private val registry = LyricsProviderRegistry.default()
    private val playerPreferences = PlayerPreferences(context)
    private val cache = mutableMapOf<String, List<LyricsEntry>>()

    private val providerCooldowns = java.util.concurrent.ConcurrentHashMap<String, Long>()

    suspend fun forceRefresh(
        videoId: String,
        ctx: Context? = null,
    ) {
        cache.remove(videoId)
        try {
            LyricsCacheManager.evictLyrics(ctx ?: context, videoId)
        } catch (e: Exception) {
            Log.w(TAG, "Disk cache evict failed: ${e.message}")
        }
        providerCooldowns.clear()
        Log.d(TAG, "Force refresh requested for $videoId — caches and cooldowns cleared")
    }

    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        ctx: Context? = null,
    ): Pair<List<LyricsEntry>, String>? {
        val targetContext = ctx ?: context

        cache[videoId]?.let { cached ->
            val normalized = normalizeEntries(cached)
            if (normalized.none { !it.words.isNullOrEmpty() } || hasWordSync(normalized)) {
                Log.d(TAG, "Returning in-memory cached lyrics for $videoId")
                return normalized to "MemoryCache"
            }
            Log.d(TAG, "Ignoring weak in-memory word-sync cache for $videoId")
            cache.remove(videoId)
        }

        // Any cached lyrics with sane timestamps counts as a hit, word-synced or not. Requiring
        // word-sync here meant every line-synced song - which is most of them, LrcLib and KuGou
        // publish line timings only - re-ran the whole provider sweep before it could show what it
        // already had on disk. Upgrading to word-sync is what the refresh button is for.
        val diskCached = LyricsCacheManager.getLyrics(targetContext, videoId)
        val normalizedDiskCached = diskCached?.let { normalizeEntries(it.sorted()) }
        if (!normalizedDiskCached.isNullOrEmpty() && hasReasonableTimestamps(normalizedDiskCached, duration)) {
            cache[videoId] = normalizedDiskCached
            return normalizedDiskCached to "DiskCache"
        } else if (!diskCached.isNullOrEmpty()) {
            Log.d(TAG, "Disk cache has invalid timestamps for $videoId, discarding")
            LyricsCacheManager.evictLyrics(targetContext, videoId)
        }

        val cleanedTitle = LyricsUtils.cleanTitle(title)
        val cleanedArtist = LyricsUtils.cleanArtist(artist)
        Log.d(TAG, "Cache miss. Querying providers: cleanedTitle=\"$cleanedTitle\", cleanedArtist=\"$cleanedArtist\"")

        val orderString = playerPreferences.lyricsProviderOrder.first()
        val enabledStates = playerPreferences.allLyricsProviderEnabledStates().first()
        val orderedProviders =
            registry
                .getOrderedProviders(orderString)
                .filter { enabledStates[it.name] != false }

        Log.d(TAG, "Enabled providers in order: ${orderedProviders.joinToString { it.name }}")

        val now = System.currentTimeMillis()

        var unsyncedFallback: Pair<List<LyricsEntry>, String>? = null

        val liveProviders =
            orderedProviders.filter { provider ->
                val cooldownUntil = providerCooldowns[provider.name]
                if (cooldownUntil != null && cooldownUntil > now) {
                    Log.d(TAG, "Skipping ${provider.name} (auth cooldown for ${(cooldownUntil - now) / 1000}s)")
                    false
                } else {
                    true
                }
            }

        // Providers are queried [PROVIDER_BATCH] at a time, in the configured order, so the
        // preference still picks the winner and only the waiting inside a batch is shared. Serially
        // this cost the sum of every provider that missed ahead of the one that hit.
        val syncedResult =
            withTimeoutOrNull(MAX_TOTAL_TIMEOUT_MS) {
                var winner: Pair<List<LyricsEntry>, String>? = null
                var extraBatchesAfterFallback = 0

                for (batch in liveProviders.chunked(PROVIDER_BATCH)) {
                    // Something showable is already in hand. Spend one more batch looking for a
                    // synced version of it, then stop rather than working through the whole ladder
                    // for an upgrade that most songs do not have.
                    if (unsyncedFallback != null) {
                        if (extraBatchesAfterFallback >= 1) break
                        extraBatchesAfterFallback++
                    }

                    winner =
                        coroutineScope {
                            val pending =
                                batch.map { provider ->
                                    provider to
                                        async {
                                            Log.d(TAG, "Querying provider: ${provider.name}")
                                            fetchFromProvider(
                                                provider,
                                                videoId,
                                                cleanedTitle,
                                                cleanedArtist,
                                                duration,
                                                album,
                                            )
                                        }
                                }

                            var batchWinner: Pair<List<LyricsEntry>, String>? = null
                            for ((provider, deferred) in pending) {
                                val providerResult = deferred.await()

                                if (providerResult != null && providerResult.isSuccess) {
                                    var entries = providerResult.getOrNull()
                                    if (!entries.isNullOrEmpty()) {
                                        entries = LyricsUtils.filterCreditLines(normalizeEntries(entries.sorted()))
                                        if (entries.isNotEmpty() && hasReasonableTimestamps(entries, duration)) {
                                            if (entriesAreSynced(entries)) {
                                                Log.d(TAG, "Got ${entries.size} SYNCED lines from ${provider.name} - using these")
                                                batchWinner = entries to provider.name
                                                break
                                            } else if (unsyncedFallback == null) {
                                                Log.d(
                                                    TAG,
                                                    "${provider.name} returned ${entries.size} UNSYNCED lines - keeping as fallback",
                                                )
                                                unsyncedFallback = entries to provider.name
                                            }
                                        } else if (entries.isNotEmpty()) {
                                            Log.w(TAG, "${provider.name} returned lyrics with unreasonable timestamps, skipping")
                                        }
                                    }
                                } else {
                                    val errorMsg = providerResult?.exceptionOrNull()?.message ?: "timeout or exception"
                                    if (isAuthFailure(errorMsg)) {
                                        providerCooldowns[provider.name] = now + PROVIDER_COOLDOWN_MS
                                        Log.w(TAG, "${provider.name} auth failure ($errorMsg) - cooling down for 10 min")
                                    } else {
                                        Log.w(TAG, "${provider.name} failed: $errorMsg")
                                    }
                                }
                            }

                            // Cancelled explicitly: coroutineScope otherwise waits for every child
                            // before it returns, which would hand back the slowest provider in the
                            // batch instead of the winner. Cancelling a finished one is a no-op.
                            pending.forEach { (_, deferred) -> deferred.cancel() }
                            batchWinner
                        }

                    if (winner != null) break
                }

                winner
            }

        if (syncedResult != null) {
            cache[videoId] = syncedResult.first
            try {
                LyricsCacheManager.saveLyrics(targetContext, videoId, syncedResult.first)
            } catch (e: Exception) {
                Log.w(TAG, "Disk cache save failed: ${e.message}")
            }
            return syncedResult
        }

        unsyncedFallback?.let { fallback ->
            Log.d(TAG, "No synced lyrics across providers — falling back to unsynced from ${fallback.second}")
            cache[videoId] = fallback.first
            try {
                LyricsCacheManager.saveLyrics(targetContext, videoId, fallback.first)
            } catch (e: Exception) {
                Log.w(TAG, "Disk cache save failed: ${e.message}")
            }
            return fallback
        }

        Log.w(TAG, "No lyrics found for $videoId")
        return null
    }

    /** One provider call, capped at [PER_PROVIDER_TIMEOUT_MS]; null on timeout or throw. */
    private suspend fun fetchFromProvider(
        provider: LyricsProvider,
        videoId: String,
        cleanedTitle: String,
        cleanedArtist: String,
        duration: Int,
        album: String?,
    ): Result<List<LyricsEntry>>? =
        try {
            withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                provider.getLyrics(videoId, cleanedTitle, cleanedArtist, duration, album)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "${provider.name} threw: ${e.message}")
            null
        }

    private fun hasWordSync(entries: List<LyricsEntry>): Boolean {
        val wordLines = entries.filter { !it.words.isNullOrEmpty() }
        if (wordLines.isEmpty()) return false

        val lineCount = entries.size.coerceAtLeast(1)
        val ratio = wordLines.size.toFloat() / lineCount
        if (lineCount > 5 && wordLines.size < 3) return false
        if (lineCount > 10 && ratio < 0.25f) return false

        val validTimingLines =
            wordLines.count { entry ->
                val words = entry.words.orEmpty().sortedBy { it.startTime }
                words.isNotEmpty() &&
                    words.last().endTime > words.first().startTime &&
                    words.zipWithNext().all { (current, next) ->
                        current.endTime >= current.startTime && next.startTime >= current.startTime
                    }
            }
        if (validTimingLines < minOf(2, wordLines.size)) return false

        val firstWordMs = wordLines.minOf { it.words!!.first().startTime }
        val lastWordMs = wordLines.maxOf { it.words!!.last().endTime }
        if (lineCount > 5 && lastWordMs - firstWordMs < 10_000L) return false

        return true
    }

    private fun hasReasonableTimestamps(
        entries: List<LyricsEntry>,
        durationSec: Int,
    ): Boolean {
        val timed = entries.filter { it.time > 0 }
        if (timed.size < 2) return true
        val firstMs = timed.first().time
        val lastMs = timed.last().time
        if (lastMs - firstMs < 10_000L) return false
        if (durationSec > 0 && firstMs > durationSec * 1000L + 10_000L) return false
        return true
    }

    private fun isAuthFailure(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return message.contains(" 401") || message.contains(" 403") ||
            message.contains("Unauthorized", ignoreCase = true) ||
            message.contains("Forbidden", ignoreCase = true)
    }

    fun entriesAreSynced(entries: List<LyricsEntry>): Boolean {
        if (entries.size < 2) return false
        val main = entries.filter { !it.isBackground }
        val list = if (main.size >= 2) main else entries
        val times = list.map { it.time }.distinct()
        if (times.size < 2) return false
        val firstPositive = times.firstOrNull { it > 0L } ?: return false
        val maxTime = list.maxOf { it.time }
        if (maxTime - firstPositive < 5_000L) return false
        val hasWordTimings = entries.any { !it.words.isNullOrEmpty() }
        if (hasWordTimings) return true
        val distinctTimedLines = list.count { it.time > 0L }
        return distinctTimedLines >= (list.size * 0.5).toInt().coerceAtLeast(2)
    }

    private fun normalizeEntries(entries: List<LyricsEntry>): List<LyricsEntry> =
        entries.map { entry ->
            entry.copy(
                text = LyricsUtils.decodeHtmlEntities(entry.text),
                words =
                    entry.words?.map { word ->
                        word.copy(text = LyricsUtils.decodeHtmlEntities(word.text))
                    },
            )
        }

    fun clearCache(videoId: String? = null) {
        if (videoId != null) cache.remove(videoId) else cache.clear()
    }

    /**
     * Streams a result from every enabled provider (in the user's configured order) so the UI can
     * offer alternatives when the automatic pick is wrong. Unlike [getLyrics] it never stops at
     * the first synced hit and never touches the caches.
     */
    suspend fun getAllLyrics(
        videoId: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        onCandidate: suspend (LyricsCandidate) -> Unit,
    ) {
        val cleanedTitle = LyricsUtils.cleanTitle(title)
        val cleanedArtist = LyricsUtils.cleanArtist(artist)
        val orderString = playerPreferences.lyricsProviderOrder.first()
        val enabledStates = playerPreferences.allLyricsProviderEnabledStates().first()
        val orderedProviders =
            registry
                .getOrderedProviders(orderString)
                .filter { enabledStates[it.name] != false }
        for (provider in orderedProviders) {
            val providerResult = fetchFromProvider(provider, videoId, cleanedTitle, cleanedArtist, duration, album)
            val entries = providerResult?.getOrNull()
            if (!entries.isNullOrEmpty()) {
                val cleaned = LyricsUtils.filterCreditLines(normalizeEntries(entries.sorted()))
                if (cleaned.isNotEmpty() && hasReasonableTimestamps(cleaned, duration)) {
                    onCandidate(LyricsCandidate(provider.name, cleaned, entriesAreSynced(cleaned)))
                }
            }
        }
    }

    /**
     * Makes a user-chosen or user-edited set of entries the lyrics of record for [videoId] by
     * overwriting both the in-memory and disk caches, so it survives sheet reopens and restarts.
     */
    suspend fun applyManualLyrics(
        videoId: String,
        entries: List<LyricsEntry>,
        ctx: Context? = null,
    ) {
        if (entries.isEmpty()) return
        cache[videoId] = entries
        try {
            LyricsCacheManager.saveLyrics(ctx ?: context, videoId, entries)
        } catch (e: Exception) {
            Log.w(TAG, "Manual lyrics save failed: ${e.message}")
        }
    }
}

data class LyricsCandidate(
    val providerName: String,
    val entries: List<LyricsEntry>,
    val synced: Boolean,
)
