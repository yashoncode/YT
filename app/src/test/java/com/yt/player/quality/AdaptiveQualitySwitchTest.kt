package com.yt.player.quality

import androidx.media3.common.util.UnstableApi
import com.yt.player.state.EnhancedPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.VideoStream

/**
 * The adaptive ladder's two stability rules. Both exist because a switch is not free: for a
 * progressive source it re-prepares the stream at the current position, which always costs a
 * rebuffer, so a switch that provokes the next one leaves playback flapping instead of settling.
 */
@UnstableApi
class AdaptiveQualitySwitchTest {
    private fun stream(
        id: String,
        resolution: String,
        format: MediaFormat,
    ): VideoStream =
        VideoStream
            .Builder()
            .setId(id)
            .setContent("https://example.invalid/$id", true)
            .setMediaFormat(format)
            .setResolution(resolution)
            .setIsVideoOnly(true)
            .build()

    private fun h264(
        id: String,
        resolution: String,
    ) = stream(id, resolution, MediaFormat.MPEG_4)

    private fun vp9(
        id: String,
        resolution: String,
    ) = stream(id, resolution, MediaFormat.WEBM)

    private fun manager(onSwitch: (VideoStream, Long) -> Unit = { _, _ -> }): QualityManager =
        QualityManager(
            bandwidthMeter = null,
            trackSelector = null,
            stateFlow = MutableStateFlow(EnhancedPlayerState()),
            onQualitySwitch = onSwitch,
        )

    @Test
    fun `a downgrade stays on the codec that is already playing`() {
        var switched: VideoStream? = null
        val manager = manager { stream, _ -> switched = stream }
        val current = vp9("current", "1080p")
        val lowerVp9 = vp9("lower-vp9", "720p")

        manager.setAvailableStreams(listOf(current, h264("lower-h264", "720p"), lowerVp9))
        manager.setCurrentStream(current)
        manager.checkAdaptiveQualityDowngrade(forceCheck = true, currentPosition = 0L)

        assertEquals(lowerVp9.id, switched?.id)
    }

    @Test
    fun `a downgrade takes another codec rather than staying at a quality it cannot afford`() {
        var switched: VideoStream? = null
        val manager = manager { stream, _ -> switched = stream }
        val current = vp9("current", "1080p")
        val lowerH264 = h264("lower-h264", "720p")

        manager.setAvailableStreams(listOf(current, lowerH264))
        manager.setCurrentStream(current)
        manager.checkAdaptiveQualityDowngrade(forceCheck = true, currentPosition = 0L)

        assertEquals(lowerH264.id, switched?.id)
    }

    @Test
    fun `buffering caused by a switch does not count towards the next one`() {
        val manager = manager()
        val current = vp9("current", "1080p")

        manager.setAvailableStreams(listOf(current, vp9("lower", "720p")))
        manager.setCurrentStream(current)
        manager.checkAdaptiveQualityDowngrade(forceCheck = true, currentPosition = 0L)

        repeat(10) { manager.incrementBufferingCount() }

        assertEquals(0, manager.consecutiveBufferingCount)
        assertFalse(manager.hasReachedBufferingThreshold())
    }

    @Test
    fun `buffering still counts when no switch has happened`() {
        val manager = manager()
        manager.setAvailableStreams(listOf(vp9("only", "720p")))

        repeat(3) { manager.incrementBufferingCount() }

        assertEquals(3, manager.consecutiveBufferingCount)
        assertTrue(manager.hasReachedBufferingThreshold())
    }
}
