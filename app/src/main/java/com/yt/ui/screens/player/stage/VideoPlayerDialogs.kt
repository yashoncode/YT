package com.yt.ui.screens.player.stage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.util.UnstableApi
import com.yt.data.model.Comment
import com.yt.data.model.Video
import com.yt.ui.screens.player.dialogs.PlayerBottomSheetsContainer
import com.yt.ui.screens.player.dialogs.PlayerDialogsContainer
import com.yt.ui.screens.player.dialogs.SbSubmitSegmentDialog
import com.yt.ui.screens.player.state.MediaSheetHeights
import com.yt.ui.screens.player.state.PlayerCommentsUiState
import com.yt.ui.screens.player.state.PlayerLayoutMode
import com.yt.ui.screens.player.state.PlayerSheet
import com.yt.ui.screens.player.state.selectCommentSort

/** Every dialog and bottom sheet the player overlay raises above its own stage. */
@UnstableApi
@Composable
internal fun VideoPlayerDialogs(
    session: VideoPlayerStageSession,
    completeVideo: Video,
    mediaSheetHeights: MediaSheetHeights,
    onMediaSheetProgressChange: (Float) -> Unit,
    canUseFullscreenSidePanel: Boolean,
    playerLayoutMode: PlayerLayoutMode,
    commentsUiState: PlayerCommentsUiState,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToShorts: (String) -> Unit,
    onClose: () -> Unit,
) {
    val video = session.video
    val context = session.context
    val screenState = session.screenState
    val playerState = session.playerState
    val playerUiState = session.uiState
    val playerViewModel = session.viewModel
    val prefs = session.prefs
    val hostedInSidePanel = canUseFullscreenSidePanel || playerLayoutMode == PlayerLayoutMode.WIDE

    // Dialogs
    PlayerDialogsContainer(
        screenState = screenState,
        playerState = playerState,
        uiState = playerUiState,
        video = completeVideo,
        viewModel = playerViewModel,
        prefs = prefs,
        hostedInSidePanel = hostedInSidePanel,
        mediaSheetExpandedHeight = mediaSheetHeights.expanded,
        mediaSheetCollapsedHeight = mediaSheetHeights.collapsed,
        onMediaSheetProgressChange = onMediaSheetProgressChange,
    )

    // SB Submit dialog
    if (screenState.activeSheet == PlayerSheet.SbSubmit) {
        val initialPosition = remember { screenState.currentPosition }
        SbSubmitSegmentDialog(
            videoId = video.id,
            currentPositionMs = initialPosition,
            onDismiss = { screenState.closeSheet() },
        )
    }

    // Bottom Sheets
    PlayerBottomSheetsContainer(
        screenState = screenState,
        uiState = playerUiState,
        video = video,
        completeVideo = completeVideo,
        disableShortsPlayer = prefs.disableShortsPlayer,
        viewModel = playerViewModel,
        playerState = playerState,
        commentsUiState = commentsUiState,
        commentsEnabled = prefs.commentsEnabled,
        onLoadMoreComments = { videoId -> playerViewModel.loadMoreComments(videoId) },
        onSelectCommentSort = { filter ->
            commentsUiState.selectCommentSort(filter, video.id, screenState, playerViewModel)
        },
        mediaSheetExpandedHeight = mediaSheetHeights.expanded,
        mediaSheetCollapsedHeight = mediaSheetHeights.collapsed,
        context = context,
        onPlayAsShort = { videoId ->
            onClose()
            onNavigateToShorts(videoId)
        },
        onLoadReplies = { comment ->
            playerViewModel.loadCommentReplies(comment)
        },
        onLoadMoreReplies = { comment ->
            playerViewModel.loadMoreCommentReplies(comment)
        },
        onNavigateToChannel = { channelId ->
            onNavigateToChannel(channelId)
        },
        hostedInSidePanel = hostedInSidePanel,
        onMediaSheetProgressChange = onMediaSheetProgressChange,
    )
}
