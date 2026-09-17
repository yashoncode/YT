package com.yt.ui.screens.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yt.R
import com.yt.data.local.HomeFeedColumns
import com.yt.data.local.HomeViewMode
import com.yt.data.local.PlayerPreferences
import com.yt.data.local.VideoHistoryEntry
import com.yt.data.model.Video
import com.yt.data.shorts.queue.ShortsQueueSource
import com.yt.player.DeepYTManager
import com.yt.ui.TabScrollEventBus
import com.yt.ui.components.layout.topbar.YTTopBar
import com.yt.ui.components.layout.topbar.YTTopBarSearchField
import com.yt.ui.components.shared.YTErrorState
import com.yt.ui.components.shared.YTPullToRefreshBox
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private const val IMPRESSION_DEBOUNCE_MS = 500L
private const val MILLIS_PER_SECOND = 1000L
private val TOP_BAR_BLUR_RADIUS = 24.dp
private const val TOP_BAR_GLASS_ALPHA = 0.5f

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun HomeScreen(
    onVideoClick: (Video) -> Unit,
    onShortClick: (ShortsQueueSource) -> Unit,
    onSearchClick: () -> Unit,
    onChannelClick: (String) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onOpenShortsFeed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = remember(context) { PlayerPreferences(context) }
    val homeViewMode by preferences.homeViewMode.collectAsStateWithLifecycle(initialValue = HomeViewMode.GRID)
    val homeFeedColumns by preferences.homeFeedColumns.collectAsStateWithLifecycle(initialValue = HomeFeedColumns.AUTO)
    val homeFeedEnabled by preferences.homeFeedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val refreshHomeOnReselect by preferences.refreshHomeOnReselect.collectAsStateWithLifecycle(initialValue = true)
    val showAppLogoIcon by preferences.showAppLogoIcon.collectAsStateWithLifecycle(initialValue = true)
    val deepYTActive by preferences.deepYTActive.collectAsStateWithLifecycle(initialValue = false)

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    LifecycleStartEffect(viewModel, homeFeedEnabled) {
        if (homeFeedEnabled) {
            viewModel.onHomeVisible()
        } else {
            viewModel.onHomeHidden()
        }
        onStopOrDispose { viewModel.onHomeHidden() }
    }

    val videoIndexById =
        remember(uiState.videos) {
            buildMap(uiState.videos.size) {
                uiState.videos.forEachIndexed { index, video -> put(video.id, index) }
            }
        }
    // Pairing the index with the scroll state matters: a user parked on the last item keeps the
    // same index, so an index-only flow can never re-arm a prefetch that came back empty. Each
    // further scroll attempt toggles isScrollInProgress and gives the feed another chance.
    LaunchedEffect(gridState, videoIndexById) {
        snapshotFlow {
            var lastVisibleVideoIndex = -1
            gridState.layoutInfo.visibleItemsInfo.forEach { item ->
                val index = videoIndexById[item.key as? String] ?: return@forEach
                if (index > lastVisibleVideoIndex) lastVisibleVideoIndex = index
            }
            lastVisibleVideoIndex to gridState.isScrollInProgress
        }.distinctUntilChanged()
            .collect { (lastVisibleVideoIndex, _) ->
                viewModel.onHomeViewportChanged(lastVisibleVideoIndex)
            }
    }

    // Viewport impressions: only items dwelt in view are recorded as "shown".
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String } }
            .debounce(IMPRESSION_DEBOUNCE_MS)
            .collect { viewModel.recordImpressions(it) }
    }

    LaunchedEffect(refreshHomeOnReselect) {
        TabScrollEventBus.scrollToTopEvents
            .filter { it == "home" }
            .collectLatest {
                gridState.animateScrollToItem(0)
                if (refreshHomeOnReselect) {
                    viewModel.refreshFeed()
                }
            }
    }

    // The feed runs full-bleed under the top bar; the bar blurs it rather than hiding it. The
    // state is local to this screen because the shell's own state already contains this bar, and
    // an effect may not sample a source it is part of.
    val feedHazeState = rememberHazeState()
    val backdrop = MaterialTheme.colorScheme.background
    val topBarHazeStyle =
        remember(backdrop) {
            HazeStyle(
                backgroundColor = backdrop,
                tint = HazeTint(backdrop.copy(alpha = TOP_BAR_GLASS_ALPHA)),
                blurRadius = TOP_BAR_BLUR_RADIUS,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            YTTopBar(
                modifier =
                    Modifier.hazeEffect(feedHazeState, topBarHazeStyle) {
                        // Solid across the bar, gone by the time it meets the feed, so there is no
                        // edge where the blur stops.
                        progressive =
                            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    },
                title = {
                    YTTopBarSearchField(
                        onClick = onSearchClick,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                },
                leading =
                    if (showAppLogoIcon) {
                        {
                            YTHeaderLogoIcon(
                                isDeepYTActive = deepYTActive,
                                onToggleDeepYT = {
                                    coroutineScope.launch {
                                        DeepYTManager.toggle(context)
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    } else {
                        null
                    },
            )
        },
    ) { padding ->
        ResettableHomePullToRefreshBox(
            resetKey = uiState.isLoading && uiState.videos.isEmpty(),
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFeed() },
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .hazeSource(feedHazeState),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isListView = homeViewMode == HomeViewMode.LIST
                val layoutConfig = rememberHomeLayoutConfig(maxWidth, homeFeedColumns)

                when {
                    !homeFeedEnabled -> {
                        HomeFeedDisabledState(modifier = Modifier.fillMaxSize())
                    }

                    uiState.isLoading && uiState.videos.isEmpty() -> {
                        HomeFeedShimmer(layoutConfig = layoutConfig, isListView = isListView)
                    }

                    uiState.error != null && uiState.videos.isEmpty() -> {
                        YTErrorState(
                            error = uiState.error ?: stringResource(R.string.error_occurred),
                            onRetry = { viewModel.retry() },
                        )
                    }

                    else -> {
                        ReportDrawnWhen {
                            uiState.videos.isNotEmpty() || uiState.shorts.isNotEmpty()
                        }

                        HomeFeedGrid(
                            uiState = uiState,
                            layoutConfig = layoutConfig,
                            isListView = isListView,
                            gridState = gridState,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onEnrichChannelMetadata = viewModel::enrichChannelMetadataIfMissing,
                            onShortClick = { shelf, tapped ->
                                onShortClick(viewModel.shortsShelfSource(shelf, tapped))
                            },
                            onSeeAllHistory = onNavigateToHistory,
                            onOpenShortsFeed = onOpenShortsFeed,
                            onRefresh = { viewModel.refreshFeed() },
                            topContentPadding = padding.calculateTopPadding(),
                        )
                    }
                }
            }
        }
    }
}

// Deliberately not data/model's VideoHistoryEntry.toVideo(): that one carries isShort, which would
// reroute a resumed short to the Shorts player instead of the video player.
private fun VideoHistoryEntry.toResumeVideo(): Video =
    Video(
        id = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = thumbnailUrl,
        duration = (duration / MILLIS_PER_SECOND).toInt(),
        viewCount = 0L,
        uploadDate = "",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResettableHomePullToRefreshBox(
    resetKey: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    key(resetKey) {
        YTPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            content = content,
        )
    }
}
