package com.yt.ui.screens.player.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yt.data.model.Video
import com.yt.player.EnhancedPlayerManager
import com.yt.player.dlna.DlnaCastManager
import com.yt.player.state.EnhancedPlayerState
import com.yt.ui.components.shared.MediaDownloadDialog
import com.yt.ui.components.shared.MediaDownloadDialogCompact
import com.yt.ui.components.videoplayer.DlnaDevicePickerDialog
import com.yt.ui.screens.player.VideoPlayerViewModel
import com.yt.ui.screens.player.state.PlayerScreenState
import com.yt.ui.screens.player.state.PlayerSheet
import com.yt.ui.screens.player.state.VideoPlayerPreferencesState
import com.yt.ui.screens.player.state.VideoPlayerUiState
import kotlinx.coroutines.launch

@Composable
internal fun PlayerDialogsContainer(
    screenState: PlayerScreenState,
    playerState: EnhancedPlayerState,
    uiState: VideoPlayerUiState,
    video: Video,
    viewModel: VideoPlayerViewModel,
    prefs: VideoPlayerPreferencesState,
    hostedInSidePanel: Boolean = false,
    mediaSheetExpandedHeight: Dp? = null,
    mediaSheetCollapsedHeight: Dp = 0.dp,
    onMediaSheetProgressChange: (Float) -> Unit = {},
) {
    val playerPreferences = prefs.preferences
    val coroutineScope = rememberCoroutineScope()

    // Download Quality Dialog
    if (screenState.activeSheet == PlayerSheet.Download) {
        when (prefs.downloadDialogStyle) {
            com.yt.data.local.DownloadDialogStyle.COMPACT -> {
                MediaDownloadDialogCompact(
                    streamInfo = uiState.streamInfo,
                    streamSizes = uiState.streamSizes,
                    innerTubeVideoFormats = uiState.innerTubeVideoFormats,
                    innerTubeAudioFormats = uiState.innerTubeAudioFormats,
                    video = video,
                    currentPlayingHeight = playerState.effectiveQuality,
                    onDismiss = { screenState.closeSheet() },
                )
            }

            com.yt.data.local.DownloadDialogStyle.FULL -> {
                MediaDownloadDialog(
                    streamInfo = uiState.streamInfo,
                    streamSizes = uiState.streamSizes,
                    innerTubeVideoFormats = uiState.innerTubeVideoFormats,
                    innerTubeAudioFormats = uiState.innerTubeAudioFormats,
                    video = video,
                    onDismiss = { screenState.closeSheet() },
                )
            }

            null -> { }
        }
    }

    if (screenState.isSettingsOpen && !hostedInSidePanel) {
        PlayerSettingsSheetHost(
            screenState = screenState,
            playerState = playerState,
            uiState = uiState,
            viewModel = viewModel,
            playerPreferences = playerPreferences,
            scope = coroutineScope,
            rememberPlaybackSpeed = prefs.rememberPlaybackSpeed,
            ambientModeEnabled = prefs.ambientModeEnabled,
            groupedQualitySelectorEnabled = prefs.groupedQualitySelectorEnabled,
            rememberSubtitleLanguage = { language ->
                coroutineScope.launch { playerPreferences.setPreferredSubtitleLanguage(language) }
            },
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            pipAspectRatio = null,
            onSheetProgressChange = onMediaSheetProgressChange,
            onDismiss = { screenState.closeSheet() },
        )
    }

    if (screenState.activeSheet == PlayerSheet.Dlna) {
        val dlnaDevices by DlnaCastManager.devices.collectAsStateWithLifecycle()
        val isDlnaDiscovering by DlnaCastManager.isDiscovering.collectAsStateWithLifecycle()
        DlnaDevicePickerDialog(
            devices = dlnaDevices,
            isDiscovering = isDlnaDiscovering,
            isCasting = DlnaCastManager.isCasting,
            videoTitle = video.title,
            onDeviceSelected = { device ->
                val currentPlayerUrl =
                    EnhancedPlayerManager
                        .getInstance()
                        .getPlayer()
                        ?.currentMediaItem
                        ?.localConfiguration
                        ?.uri
                        ?.toString()
                DlnaCastManager.castStreamInfo(
                    device = device,
                    title = video.title,
                    streamInfo = uiState.streamInfo,
                    currentPlayerUrl = currentPlayerUrl,
                )
                screenState.closeSheet()
            },
            onStopCasting = {
                DlnaCastManager.disconnect()
                screenState.closeSheet()
            },
            onDismiss = {
                DlnaCastManager.stopDiscovery()
                screenState.closeSheet()
            },
        )
    }
}
