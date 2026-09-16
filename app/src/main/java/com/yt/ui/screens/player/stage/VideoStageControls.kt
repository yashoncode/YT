package com.yt.ui.screens.player.stage

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.yt.R
import com.yt.player.EnhancedPlayerManager
import com.yt.player.PictureInPictureHelper
import com.yt.player.SleepTimerManager
import com.yt.player.dlna.DlnaCastManager
import com.yt.ui.components.videoplayer.controls.PlayerControlActions
import com.yt.ui.components.videoplayer.controls.PlayerControlsOverlay
import com.yt.ui.components.videoplayer.controls.PlayerControlsUiState
import com.yt.ui.components.videoplayer.controls.resolvePlayerQualityLabel
import com.yt.ui.components.videoplayer.overlay.AutoplayCountdownOverlay
import com.yt.ui.components.videoplayer.placedWhen
import com.yt.ui.components.videoplayer.settings.PlayerSettingsPage
import com.yt.ui.screens.player.state.PlayerSheet
import com.yt.ui.screens.player.state.SubtitleSelection

/** The transport controls the expanded player mounts once its surfaces are at rest. */
@UnstableApi
@Composable
internal fun VideoStageControls(
    session: VideoPlayerStageSession,
    expandedSurfacesPlaced: () -> Boolean,
    videoAspectRatio: Float,
    canGoPrevious: Boolean,
    isCommentsAvailable: Boolean,
    canUseFullscreenSidePanel: Boolean,
    showRemainingTime: Boolean,
    onToggleRemainingTime: () -> Unit,
    rememberSubtitleLanguage: (String) -> Unit,
    onSbSubmitClick: () -> Unit,
    onCastClick: () -> Unit,
) {
    val video = session.video
    val context = session.context
    val activity = session.activity
    val screenState = session.screenState
    val playerState = session.playerState
    val playerUiState = session.uiState
    val playerViewModel = session.viewModel
    val playerSheetState = session.sheetState
    val prefs = session.prefs
    val pipPreferences = session.pipPreferences

    // Mirrors the orientation FullscreenEffect is about to request, rather than the orientation the
    // window currently has: entering fullscreen on a landscape video rotates the activity, and
    // reading the live configuration would render one portrait frame before it turned. The drag
    // gesture sets isFullscreenPortrait and pins PORTRAIT; a vertical video gets SENSOR_PORTRAIT and
    // stays upright either way, which is why the fullscreen button used to land a short in the
    // landscape layout.
    val isPortraitFullscreenLayout =
        screenState.isFullscreen &&
            (screenState.isFullscreenPortrait || videoAspectRatio < 1f)

    // Buffered position advances on every position poll; quantised to 1% so
    // this scope recomposes on visible steps only.
    val bufferedFraction by remember(screenState) {
        derivedStateOf {
            val duration = screenState.duration
            val quantised =
                if (duration > 0) {
                    (screenState.bufferedPosition * 100L / duration).toInt() / 100f
                } else {
                    0f
                }
            quantised.coerceIn(0f, 1f)
        }
    }
    val controlsState =
        PlayerControlsUiState(
            isVisible = screenState.showControls || screenState.isTouchLocked,
            isPlaying = playerState.playWhenReady,
            hasEnded = playerState.hasEnded,
            isBuffering = playerState.isBuffering,
            duration = screenState.duration,
            qualityLabel =
                resolvePlayerQualityLabel(
                    currentQuality = playerState.currentQuality,
                    effectiveQuality = playerState.effectiveQuality,
                    autoLabel = context.getString(R.string.quality_auto),
                    autoWithHeightLabel =
                        context.getString(
                            R.string.quality_auto_template,
                            playerState.effectiveQuality,
                        ),
                ),
            videoTitle = playerUiState.streamInfo?.name ?: video.title,
            channelName = playerUiState.streamInfo?.uploaderName ?: video.channelName,
            playbackSpeed = playerState.playbackSpeed,
            resizeMode = screenState.resizeMode,
            isFullscreen = screenState.isFullscreen,
            isPortraitFullscreen = isPortraitFullscreenLayout,
            isPipSupported =
                PictureInPictureHelper.isPlayerPopupSupported(context) &&
                    pipPreferences.manualPipButtonEnabled,
            chapters = playerUiState.chapters,
            isSubtitlesEnabled = screenState.subtitlesEnabled,
            autoplayEnabled = playerUiState.autoplayEnabled,
            isLooping = playerState.isLooping,
            hasPrevious = playerState.hasPrevious || canGoPrevious,
            hasNext = playerState.hasNext || playerUiState.relatedVideos.isNotEmpty(),
            sbSubmitEnabled = prefs.sbSubmitEnabled,
            isCasting = DlnaCastManager.isCasting,
            isLive = !playerUiState.hlsUrl.isNullOrEmpty(),
            isLiveChatAvailable = playerUiState.isLiveChatAvailable,
            isCommentsAvailable = isCommentsAvailable,
            isCommentsPanelOpen = screenState.activeSheet is PlayerSheet.Comments,
            isSleepTimerActive = SleepTimerManager.isActive,
            showRemainingTime = showRemainingTime,
            isTouchLocked = screenState.isTouchLocked,
            lockModeEnabled = prefs.lockModeEnabled,
            lockOverlayRevealSignal = screenState.lockOverlayRevealSignal,
        )
    // The session is rebuilt every composition, so these lambdas are too; they read the values the
    // host observed this frame exactly as the parameter list they replace did.
    val controlsActions =
        PlayerControlActions(
            onPlayPause = {
                screenState.onInteraction()
                if (playerState.hasEnded) {
                    EnhancedPlayerManager.getInstance().replay()
                    playerViewModel.ensureNotificationServiceRunning()
                } else if (playerState.playWhenReady) {
                    EnhancedPlayerManager.getInstance().pause()
                } else {
                    EnhancedPlayerManager.getInstance().play()
                    playerViewModel.ensureNotificationServiceRunning()
                }
            },
            onPrevious = { playerViewModel.playPrevious() },
            onNext = { playerViewModel.playNext() },
            onBack = { playerSheetState.collapse() },
            onSettingsClick = { screenState.open(PlayerSheet.Settings()) },
            onQualityClick = { screenState.open(PlayerSheet.Settings(PlayerSettingsPage.Quality)) },
            onSpeedClick = { screenState.open(PlayerSheet.Settings(PlayerSettingsPage.Speed)) },
            onFullscreenClick = { screenState.toggleFullscreen() },
            onResizeClick = {
                screenState.onInteraction()
                screenState.cycleResizeMode()
            },
            onPipClick = {
                PictureInPictureHelper.requestPlayerPipMode(
                    activity = activity,
                    aspectRatio = videoAspectRatio,
                    isPlaying = playerState.isPlaying,
                )
            },
            onChapterClick = { screenState.open(PlayerSheet.Chapters) },
            onDescriptionClick = { screenState.open(PlayerSheet.Description) },
            onSubtitleClick = {
                if (screenState.subtitlesEnabled) {
                    SubtitleSelection.disable(screenState)
                } else {
                    val enabled =
                        SubtitleSelection.enable(
                            screenState = screenState,
                            subtitles = playerState.availableSubtitles,
                            languageTag = prefs.preferredSubtitleLanguage,
                            rememberLanguage = rememberSubtitleLanguage,
                        )
                    if (!enabled) screenState.open(PlayerSheet.Settings(PlayerSettingsPage.Subtitles))
                }
            },
            onSubtitleLongClick = { screenState.open(PlayerSheet.Settings(PlayerSettingsPage.Subtitles)) },
            onAutoplayToggle = { playerViewModel.toggleAutoplay(it) },
            onSbSubmitClick = onSbSubmitClick,
            onCastClick = onCastClick,
            onLiveClick = { EnhancedPlayerManager.getInstance().seekToLiveEdge(resetSpeed = true) },
            onLiveChatClick = {
                val fullscreenLiveChat = PlayerSheet.LiveChat(fullscreen = true)
                if (screenState.activeSheet == fullscreenLiveChat) {
                    screenState.closeSheet()
                } else {
                    screenState.open(fullscreenLiveChat)
                }
            },
            onCommentsClick = {
                val comments = PlayerSheet.Comments(fullscreen = canUseFullscreenSidePanel)
                if (canUseFullscreenSidePanel && screenState.activeSheet == comments) {
                    screenState.closeSheet()
                } else {
                    screenState.open(comments)
                }
            },
            onSleepTimerClick = { screenState.open(PlayerSheet.SleepTimer) },
            onToggleRemainingTime = onToggleRemainingTime,
            onTouchLockToggle = {
                if (prefs.lockModeEnabled || screenState.isTouchLocked) {
                    screenState.isTouchLocked = !screenState.isTouchLocked
                    screenState.showControls = true
                    if (screenState.isTouchLocked) {
                        screenState.revealLockOverlay()
                    }
                    screenState.onInteraction()
                }
            },
            onSeek = { newPosition ->
                screenState.onInteraction()
                val manager = EnhancedPlayerManager.getInstance()
                if (playerState.isLive) {
                    manager.seekToLiveTimeline(newPosition)
                } else {
                    manager.seekTo(newPosition)
                }
            },
            onScrubbingChange = { scrubbing ->
                screenState.isScrubbing = scrubbing
                screenState.onInteraction()
            },
        )
    PlayerControlsOverlay(
        state = controlsState,
        actions = controlsActions,
        currentPosition = { screenState.currentPosition },
        modifier = Modifier.placedWhen(expandedSurfacesPlaced),
        bufferedPercentage = bufferedFraction,
        windowInsets = WindowInsets(0, 0, 0, 0),
        isLayerPlaced = expandedSurfacesPlaced,
    )

    AutoplayCountdownOverlay()
}
