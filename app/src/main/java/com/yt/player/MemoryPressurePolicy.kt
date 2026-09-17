package com.yt.player

import android.content.ComponentCallbacks2

/**
 * Android 14 stopped delivering every trim level except [ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN]
 * and [ComponentCallbacks2.TRIM_MEMORY_BACKGROUND]; Android 15 deprecated the rest. Neither
 * surviving level signals pressure — BACKGROUND arrives whenever the process lands on the LRU list
 * — so treating it as critical cleared a paused video a few minutes after backgrounding and lost
 * the user's position (#920). Recovery could not undo that either: it replays the already-extracted
 * stream URLs, which have expired by then.
 *
 * The levels below do mean the system is walking the LRU list to reclaim memory, and releasing
 * there still buys a low-memory device some headroom before it kills us outright. They are all
 * deprecated and only reach pre-34 devices, so on Android 14 and above this always returns false.
 */
object MemoryPressurePolicy {
    /**
     * Whether the player may drop its video pipeline for [trimLevel].
     *
     * [isVideoOutputOnScreen] is the second half of the answer. TRIM_MEMORY_RUNNING_CRITICAL is
     * delivered to a foreground activity, so on a low-memory device the release ran while the user
     * was watching: the video tracks went away under a live surface and the screen turned black
     * with the audio still playing, until something restored the output seconds later (#1074). A
     * surface the user is looking at on a lit display is not reclaimable memory, whatever the
     * system is asking for.
     */
    @Suppress("DEPRECATION")
    fun shouldReleaseVideoPlayback(
        trimLevel: Int,
        isVideoOutputOnScreen: Boolean = false,
    ): Boolean =
        !isVideoOutputOnScreen &&
            (
                trimLevel >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                    trimLevel in ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL until
                    ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            )
}
