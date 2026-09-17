package com.yt.ui.components.musicplayer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yt.R

/**
 * The Song / Video switch at the top of the full music player.
 *
 * Song is the music player itself, so it is always the selected half. Picking Video hands the
 * track — and its playhead — to the video player, which is where every video feature already
 * lives (quality, captions, PiP); the music sheet is dismissed behind it.
 */
@Composable
internal fun SongVideoSwitch(
    contentColor: Color,
    onSwitchToVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = contentColor.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwitchPill(
                label = stringResource(R.string.music_switch_song),
                selected = true,
                contentColor = contentColor,
                onClick = {},
            )
            SwitchPill(
                label = stringResource(R.string.music_switch_video),
                selected = false,
                contentColor = contentColor,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSwitchToVideo()
                },
            )
        }
    }
}

@Composable
private fun SwitchPill(
    label: String,
    selected: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) contentColor else Color.Transparent,
        label = "songVideoPillBackground",
    )
    val labelColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.surface
            } else {
                contentColor.copy(alpha = 0.75f)
            },
        label = "songVideoPillLabel",
    )

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = labelColor,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(background)
                .clickable(enabled = !selected, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 7.dp),
    )
}
