package com.yt.ui.components.videoplayer.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yt.R
import com.yt.player.EnhancedPlayerState
import com.yt.player.audio.AudioEffectsController
import com.yt.ui.components.shared.YTNavRow
import com.yt.ui.components.shared.YTRowGroup
import com.yt.ui.components.shared.YTSectionHeader
import com.yt.ui.components.shared.YTSwitchRow
import com.yt.ui.components.shared.flowRowGroupShape
import com.yt.ui.components.shared.playbackSpeedLabel

private const val VIDEO_ROWS = 1
private const val PLAYBACK_ROWS = 3
private const val AUDIO_ROWS = 1
private const val CAPTION_ROWS = 2
private const val OVERLAY_ROWS = 3
private const val EFFECT_ROWS = 3
private const val DISPLAY_ROWS = 1

@Composable
internal fun PlayerSettingsMainPage(
    playerState: EnhancedPlayerState,
    autoplayEnabled: Boolean,
    subtitlesEnabled: Boolean,
    ambientModeEnabled: Boolean,
    onNavigateToPage: (PlayerSettingsPage) -> Unit,
    onShowSubtitleStyle: () -> Unit,
    onCastClick: () -> Unit,
    onPipClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit,
    onStableVolumeToggle: (Boolean) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
) {
    YTSectionHeader(stringResource(R.string.video))
    YTRowGroup {
        YTNavRow(
            leadingIcon = Icons.Filled.HighQuality,
            title = stringResource(R.string.quality),
            trailingText =
                if (playerState.currentQuality == 0) {
                    stringResource(R.string.quality_auto)
                } else {
                    "${playerState.currentQuality}p"
                },
            shape = flowRowGroupShape(0, VIDEO_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Quality) },
        )
    }

    YTSectionHeader(stringResource(R.string.playback_header))
    YTRowGroup {
        YTNavRow(
            leadingIcon = Icons.Filled.Speed,
            title = stringResource(R.string.playback_speed),
            trailingText = playbackSpeedLabel(playerState.playbackSpeed),
            shape = flowRowGroupShape(0, PLAYBACK_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Speed) },
        )
        YTSwitchRow(
            leadingIcon = Icons.Rounded.Repeat,
            title = stringResource(R.string.loop_video),
            checked = playerState.isLooping,
            shape = flowRowGroupShape(1, PLAYBACK_ROWS),
            onCheckedChange = onLoopToggle,
        )
        YTSwitchRow(
            leadingIcon = Icons.Filled.SkipNext,
            title = stringResource(R.string.autoplay_next),
            checked = autoplayEnabled,
            enabled = !playerState.isLooping,
            shape = flowRowGroupShape(2, PLAYBACK_ROWS),
            onCheckedChange = onAutoplayToggle,
        )
    }

    YTSectionHeader(stringResource(R.string.audio_settings_title))
    YTRowGroup {
        YTNavRow(
            leadingIcon = Icons.Filled.AudioFile,
            title = stringResource(R.string.audio_track),
            trailingText =
                audioTrackDisplayLabel(
                    playerState.availableAudioTracks.getOrNull(playerState.currentAudioTrack),
                    playerState.currentAudioTrack,
                ),
            shape = flowRowGroupShape(0, AUDIO_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Audio) },
        )
    }

    YTSectionHeader(stringResource(R.string.captions))
    YTRowGroup {
        YTNavRow(
            leadingIcon = Icons.Filled.Subtitles,
            title = stringResource(R.string.filter_subtitles),
            trailingText =
                if (subtitlesEnabled) stringResource(R.string.on) else stringResource(R.string.off),
            shape = flowRowGroupShape(0, CAPTION_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Subtitles) },
        )
        YTNavRow(
            leadingIcon = Icons.Filled.Tune,
            title = stringResource(R.string.subtitle_style),
            shape = flowRowGroupShape(1, CAPTION_ROWS),
            onClick = onShowSubtitleStyle,
        )
    }

    YTSectionHeader(stringResource(R.string.player_settings_overlay_controls))
    YTRowGroup {
        YTNavRow(
            leadingIcon = Icons.Filled.Cast,
            title = stringResource(R.string.cast_to_tv),
            shape = flowRowGroupShape(0, OVERLAY_ROWS),
            onClick = onCastClick,
        )
        YTNavRow(
            leadingIcon = Icons.Filled.PictureInPicture,
            title = stringResource(R.string.pip_mode),
            shape = flowRowGroupShape(1, OVERLAY_ROWS),
            onClick = onPipClick,
        )
        YTNavRow(
            leadingIcon = Icons.Filled.Bedtime,
            title = stringResource(R.string.sleep_timer),
            shape = flowRowGroupShape(2, OVERLAY_ROWS),
            onClick = onSleepTimerClick,
        )
    }

    YTSectionHeader(stringResource(R.string.audio_effects))
    YTRowGroup {
        val eqProfile by AudioEffectsController.eqProfileName.collectAsStateWithLifecycle()
        YTNavRow(
            leadingIcon = Icons.Filled.Equalizer,
            title = stringResource(R.string.equalizer),
            trailingText = eqProfile,
            shape = flowRowGroupShape(0, EFFECT_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Equalizer) },
        )
        YTSwitchRow(
            leadingIcon = Icons.Rounded.GraphicEq,
            title = stringResource(R.string.player_settings_skip_silence),
            checked = playerState.isSkipSilenceEnabled,
            shape = flowRowGroupShape(1, EFFECT_ROWS),
            onCheckedChange = onSkipSilenceToggle,
        )
        YTSwitchRow(
            leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.player_settings_stable_voice),
            checked = playerState.isStableVolumeEnabled,
            shape = flowRowGroupShape(2, EFFECT_ROWS),
            onCheckedChange = onStableVolumeToggle,
        )
    }

    YTSectionHeader(stringResource(R.string.player_settings_display))
    YTRowGroup {
        YTSwitchRow(
            leadingIcon = ImageVector.vectorResource(R.drawable.ic_ambient_mode),
            title = stringResource(R.string.player_settings_ambient_mode),
            checked = ambientModeEnabled,
            shape = flowRowGroupShape(0, DISPLAY_ROWS),
            onCheckedChange = onAmbientModeToggle,
        )
    }
}
