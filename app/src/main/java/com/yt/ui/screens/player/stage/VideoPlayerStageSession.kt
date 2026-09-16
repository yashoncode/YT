package com.yt.ui.screens.player.stage

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Stable
import com.yt.data.model.Video
import com.yt.player.state.EnhancedPlayerState
import com.yt.ui.components.videoplayer.PlayerDraggableState
import com.yt.ui.screens.player.VideoPlayerViewModel
import com.yt.ui.screens.player.effects.AudioSystemInfo
import com.yt.ui.screens.player.effects.PipPreferences
import com.yt.ui.screens.player.state.PlayerScreenState
import com.yt.ui.screens.player.state.VideoPlayerPreferencesState
import com.yt.ui.screens.player.state.VideoPlayerUiState
import kotlinx.coroutines.CoroutineScope

/**
 * Everything the player's stage surfaces read about the session they are drawing. Built fresh by
 * the host on each composition, so the values it carries are exactly the ones the host itself
 * observed that frame.
 */
@Stable
internal class VideoPlayerStageSession(
    val video: Video,
    val context: Context,
    val activity: ComponentActivity,
    val scope: CoroutineScope,
    val screenState: PlayerScreenState,
    val playerState: EnhancedPlayerState,
    val uiState: VideoPlayerUiState,
    val viewModel: VideoPlayerViewModel,
    val sheetState: PlayerDraggableState,
    val prefs: VideoPlayerPreferencesState,
    val audioSystemInfo: AudioSystemInfo,
    val pipPreferences: PipPreferences,
)
