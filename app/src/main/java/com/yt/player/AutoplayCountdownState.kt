package com.yt.player

data class AutoplayCountdownState(
    val isActive: Boolean = false,
    val secondsRemaining: Int = 0,
    val totalSeconds: Int = 0,
    val isPaused: Boolean = false,
    val nextVideoTitle: String? = null,
    val nextVideoChannel: String? = null,
    val nextVideoThumbnailUrl: String? = null,
)
