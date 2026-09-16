package com.yt.ui.screens.player.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yt.R
import com.yt.data.model.Comment
import com.yt.data.model.Video
import com.yt.player.EnhancedMusicPlayerManager
import com.yt.player.EnhancedPlayerManager
import com.yt.player.EnhancedPlayerState
import com.yt.player.SleepTimerManager
import com.yt.ui.components.VideoQuickActionsBottomSheet
import com.yt.ui.components.shared.CommentSortFilter
import com.yt.ui.components.shared.YTCommentsBottomSheet
import com.yt.ui.components.shared.rememberVideoShareAction
import com.yt.ui.components.videoplayer.sheet.YTLiveChatBottomSheet
import com.yt.ui.components.videoplayer.sheet.YTPlaylistQueueBottomSheet
import com.yt.ui.screens.player.VideoPlayerViewModel
import com.yt.ui.screens.player.state.PlayerCommentsUiState
import com.yt.ui.screens.player.state.PlayerScreenState
import com.yt.ui.screens.player.state.PlayerSheet
import com.yt.ui.screens.player.state.VideoPlayerUiState
import com.yt.ui.screens.player.state.transcriptTrackUrl
import com.yt.ui.screens.player.state.visibleComments

@Composable
internal fun PlayerBottomSheetsContainer(
    screenState: PlayerScreenState,
    uiState: VideoPlayerUiState,
    video: Video,
    completeVideo: Video,
    disableShortsPlayer: Boolean,
    showShortsPlayerPrompt: Boolean,
    viewModel: VideoPlayerViewModel,
    playerState: EnhancedPlayerState,
    commentsUiState: PlayerCommentsUiState,
    commentsEnabled: Boolean = true,
    onLoadMoreComments: (videoId: String) -> Unit = {},
    onSelectCommentSort: (CommentSortFilter) -> Unit = {},
    mediaSheetExpandedHeight: Dp? = null,
    mediaSheetCollapsedHeight: Dp = 0.dp,
    context: Context,
    onPlayAsShort: (String) -> Unit,
    onLoadReplies: (Comment) -> Unit = {},
    onLoadMoreReplies: (Comment) -> Unit = {},
    onNavigateToChannel: ((String) -> Unit)? = null,
    hostedInSidePanel: Boolean = false,
    onMediaSheetProgressChange: (Float) -> Unit = {},
) {
    val shareVideoAction = rememberVideoShareAction()

    val visibleComments = commentsUiState.visibleComments(screenState)

    val handleSeek: (Long) -> Unit =
        remember {
            { positionMs -> EnhancedPlayerManager.getInstance().seekTo(positionMs) }
        }

    LaunchedEffect(Unit) {
        SleepTimerManager.attachToPlayer(
            player = EnhancedPlayerManager.getInstance().getPlayer(),
        ) {
            EnhancedPlayerManager.getInstance().pause()
        }
        SleepTimerManager.attachExitCallback {
            EnhancedPlayerManager.getInstance().pause()
            EnhancedMusicPlayerManager.stop()
            context.stopService(
                android.content.Intent(context, com.yt.service.VideoPlayerService::class.java),
            )
            context.stopService(
                android.content.Intent(context, com.yt.service.Media3MusicService::class.java),
            )
            (context as? android.app.Activity)?.finishAndRemoveTask()
        }
    }

    // Quick actions sheet
    if (screenState.activeSheet == PlayerSheet.QuickActions) {
        VideoQuickActionsBottomSheet(
            video = completeVideo,
            onDismiss = { screenState.closeSheet() },
            onShare = {
                screenState.closeSheet()
                shareVideoAction(completeVideo.id, completeVideo.title)
            },
            onDownload = {
                screenState.open(PlayerSheet.Download)
            },
            onNotInterested = {
                screenState.closeSheet()
                Toast.makeText(context, context.getString(R.string.video_marked_not_interested), Toast.LENGTH_SHORT).show()
            },
            onChannelClick = onNavigateToChannel,
        )
    }

    // Comments Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Comments() && commentsEnabled && !hostedInSidePanel) {
        YTCommentsBottomSheet(
            comments = visibleComments,
            isLoading = commentsUiState.isLoading,
            selectedFilter = screenState.commentSortFilter,
            totalText = commentsUiState.totalText,
            artworkUrl = video.thumbnailUrl,
            timedOnly = screenState.commentsTimedOnly,
            onTimedChange = { screenState.commentsTimedOnly = it },
            onFilterChanged = onSelectCommentSort,
            onLoadReplies = onLoadReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onSeekMs = handleSeek,
            isLoadingMore = commentsUiState.isLoadingMore,
            hasMore = commentsUiState.hasMore,
            onLoadMore = { onLoadMoreComments(video.id) },
            onAuthorClick = { authorChannelRef ->
                screenState.closeSheet()
                onNavigateToChannel?.invoke(authorChannelRef)
            },
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
            onDismiss = { screenState.closeSheet() },
        )
    }

    if (screenState.activeSheet == PlayerSheet.LiveChat() && uiState.isLiveChatAvailable) {
        YTLiveChatBottomSheet(
            messages = uiState.liveChatMessages,
            isLoading = uiState.isLiveChatLoading,
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
            onDismiss = { screenState.closeSheet() },
        )
    }

    // Description Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Description && !hostedInSidePanel) {
        PlayerDescriptionSheetHost(
            video = video,
            uiState = uiState,
            viewModel = viewModel,
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            hasTranscriptTrack = transcriptTrackUrl(playerState, screenState) != null,
            onChaptersClick = { screenState.open(PlayerSheet.Chapters) },
            onTranscriptClick = { screenState.open(PlayerSheet.Transcript) },
            onChannelClick = { channelId ->
                screenState.closeSheet()
                onNavigateToChannel?.invoke(channelId)
            },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    if (screenState.activeSheet == PlayerSheet.Transcript && !hostedInSidePanel) {
        PlayerTranscriptSheetHost(
            viewModel = viewModel,
            screenState = screenState,
            trackUrl = transcriptTrackUrl(playerState, screenState),
            artworkUrl = video.thumbnailUrl,
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    // Chapters Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Chapters && !hostedInSidePanel) {
        PlayerChaptersSheetHost(
            screenState = screenState,
            chapters = uiState.chapters,
            thumbnailUrl = video.thumbnailUrl,
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    // Playlist Queue Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Queue) {
        val queueVideos by EnhancedPlayerManager.getInstance().queueVideos.collectAsStateWithLifecycle(initialValue = emptyList())
        val currentQueueIndex by EnhancedPlayerManager.getInstance().currentQueueIndexState.collectAsStateWithLifecycle(initialValue = -1)
        val playerState by EnhancedPlayerManager.getInstance().playerState.collectAsStateWithLifecycle()

        YTPlaylistQueueBottomSheet(
            queueVideos = queueVideos,
            currentQueueIndex = currentQueueIndex,
            playlistTitle = playerState.queueTitle,
            isLooping = playerState.isQueueLooping,
            isShuffled = playerState.isQueueShuffled,
            onLoopToggle = EnhancedPlayerManager.getInstance()::toggleQueueLoop,
            onShuffleToggle = EnhancedPlayerManager.getInstance()::toggleQueueShuffle,
            onPlayVideoAtIndex = { index ->
                EnhancedPlayerManager.getInstance().playVideoAtIndex(index, loadStreamsInPlayer = false)
            },
            onRemoveVideoAtIndex = EnhancedPlayerManager.getInstance()::removeVideoAtIndex,
            onMoveVideoAtIndex = EnhancedPlayerManager.getInstance()::moveVideoAtIndex,
            onDismiss = { screenState.closeSheet() },
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    if (screenState.activeSheet == PlayerSheet.SleepTimer && !hostedInSidePanel) {
        PlayerSleepTimerSheetHost(
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    // Shorts Suggestion Dialog
    if (screenState.showShortsPrompt && !disableShortsPlayer && showShortsPlayerPrompt) {
        ShortsSuggestionDialog(
            onPlayAsShort = {
                screenState.showShortsPrompt = false
                onPlayAsShort(completeVideo.id)
            },
            onDismiss = { screenState.showShortsPrompt = false },
        )
    }
}

/**
 * Dialog suggesting to play a short video in the Shorts player.
 */
@Composable
private fun ShortsSuggestionDialog(
    onPlayAsShort: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SmartDisplay, null) },
        title = {
            Text(
                text = stringResource(R.string.play_mode_suggestion_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(stringResource(R.string.play_mode_suggestion_body))
        },
        confirmButton = {
            TextButton(onClick = onPlayAsShort) {
                Text(stringResource(R.string.shorts_player))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
