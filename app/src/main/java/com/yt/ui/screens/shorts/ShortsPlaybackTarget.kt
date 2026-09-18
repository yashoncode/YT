package com.yt.ui.screens.shorts

import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.yt.data.local.VideoQuality
import com.yt.player.config.PlayerConfig
import com.yt.utils.NetworkState

/**
 * The height a Short is resolved at.
 *
 * This is not merely a display preference — it forms part of the key of the repository's
 * playback-stream cache, so the pager and the ViewModel's prefetch have to agree on it exactly or
 * the prefetch resolves one entry and the pager then misses it and resolves the stream a second
 * time. Both therefore derive it here rather than each computing it their own way.
 *
 * [autoHeight] is what [VideoQuality.AUTO] resolves to, supplied by the caller from
 * [ShortsAutoQuality] rather than read in here, so this stays a pure function of its inputs.
 */
internal fun shortsTargetHeight(
    isWifi: Boolean,
    wifiQuality: VideoQuality,
    cellularQuality: VideoQuality,
    autoHeight: Int,
): Int {
    val quality = if (isWifi) wifiQuality else cellularQuality
    return if (quality == VideoQuality.AUTO) autoHeight else quality.height
}

/**
 * What [VideoQuality.AUTO] resolves to for Shorts: a height picked from the measured bandwidth.
 *
 * AUTO used to mean *maximum* — its height is 0, and the repository reads 0 as "take the best
 * stream there is" — so the one setting a viewer picks expecting the app to judge for them was the
 * worst possible choice on a weak connection.
 *
 * The estimate is media3's process-wide meter, which the Shorts pool's players already feed: they
 * are built without an explicit meter, and `ExoPlayer.Builder` then defaults to this singleton.
 */
internal object ShortsAutoQuality {
    // ponytail: a 10 s snapshot, not a rolling estimate. It exists because the height is a cache
    // key: the pager and the prefetch call this milliseconds apart and must get the identical
    // number, which a live reading cannot promise. Revisit if stalls show up in practice.
    private const val SNAPSHOT_MS = 10_000L

    @Volatile
    private var height = 0

    @Volatile
    private var measuredAt = 0L

    @OptIn(UnstableApi::class)
    fun targetHeight(context: Context): Int {
        val now = SystemClock.elapsedRealtime()
        val snapshot = height
        if (snapshot > 0 && now - measuredAt < SNAPSHOT_MS) return snapshot

        val estimate = DefaultBandwidthMeter.getSingletonInstance(context).bitrateEstimate
        // The ladder is the main player's, so a device that plays 720p video does not get handed
        // 1080p reels. Its steps are 2-3x apart, which is also what keeps this from flapping.
        val next = PlayerConfig.calculateInitialQualityTarget(estimate)
        height = next
        measuredAt = now
        return next
    }
}

/**
 * Reads the active transport synchronously. Callers must not substitute a placeholder while a
 * network callback settles: a wrong answer here silently changes the cache key above.
 */
internal fun isOnWifi(context: Context): Boolean = NetworkState.isOnWifi(context)
