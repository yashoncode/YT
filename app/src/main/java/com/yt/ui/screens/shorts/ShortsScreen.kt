package com.yt.ui.screens.shorts

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yt.R
import com.yt.data.local.PlayerPreferences
import com.yt.data.model.ShortVideo
import com.yt.data.model.toVideo
import com.yt.data.shorts.queue.ShortsQueueSource
import com.yt.player.GlobalPlayerState
import com.yt.player.shorts.ShortsPlayerPool
import com.yt.ui.components.shared.CommentSortFilter
import com.yt.ui.components.shared.YTCommentsBottomSheet
import com.yt.ui.components.shared.YTDescriptionBottomSheet
import com.yt.ui.components.shared.applyVideoCommentFilters
import com.yt.ui.components.shared.videoCommentSortFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** How far the first short must be dragged down before the queue reloads. */
private val ShortsRefreshPullThreshold = 140.dp

/** Clears the status bar, so the spinner is not half behind it on a full-bleed page. */
private val ShortsRefreshIndicatorPadding = 48.dp

/** Silent retries of an empty feed before the viewer is shown a retry button instead. */
private const val MAX_EMPTY_AUTO_RETRIES = 1
private const val EMPTY_AUTO_RETRY_DELAY_MS = 600L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShortsScreen(
    source: ShortsQueueSource,
    onBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    bottomNavOverlayPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: ShortsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val audioLangPref = remember(context) { PlayerPreferences(context) }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val isInPip by GlobalPlayerState.isInPipMode.collectAsState()
    ShortsPipActionEffect()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearSnackbar()
        }
    }

    // Seeded synchronously rather than defaulting to false: the ViewModel's prefetch reads the
    // transport synchronously too, and the two must agree or they key the playback-stream cache
    // differently and the prefetch is wasted.
    var isWifi by remember { mutableStateOf(isOnWifi(context)) }
    DisposableEffect(context) {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        fun update() {
            isWifi = cm
                .getNetworkCapabilities(cm.activeNetwork)
                ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        update()
        val networkCallback =
            object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: android.net.Network,
                    caps: android.net.NetworkCapabilities,
                ) = update()

                override fun onLost(network: android.net.Network) {
                    update()
                }

                override fun onAvailable(network: android.net.Network) {
                    update()
                }
            }
        cm.registerDefaultNetworkCallback(networkCallback)
        onDispose { cm.unregisterNetworkCallback(networkCallback) }
    }
    // Null until DataStore has actually emitted. Resolving against a placeholder height would key
    // the playback-stream cache differently from the ViewModel's prefetch (which reads the real
    // preference), guaranteeing a miss — and the correction would then re-resolve and reload all
    // three pooled players on every entry to the screen.
    val shortsQualityPair by remember(audioLangPref) {
        audioLangPref.shortsQualityWifi.combine(audioLangPref.shortsQualityCellular, ::Pair)
    }.collectAsState(initial = null)
    val shortsTargetHeight by remember(isWifi, shortsQualityPair) {
        derivedStateOf {
            shortsQualityPair?.let { (wifi, cellular) ->
                shortsTargetHeight(isWifi, wifi, cellular, ShortsAutoQuality.targetHeight(context))
            }
        }
    }
    val prevShortsTargetHeight = remember { mutableStateOf<Int?>(null) }

    // Bottom sheet states
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var commentSortFilter by remember { mutableStateOf(CommentSortFilter.TOP) }
    var commentsTimedOnly by remember { mutableStateOf(false) }
    val comments by viewModel.commentsState.collectAsState()
    val isLoadingComments by viewModel.isLoadingComments.collectAsState()
    val commentSortOptions by viewModel.commentSortOptions.collectAsState()
    val commentTotalText by viewModel.commentTotalText.collectAsState()

    val visibleComments =
        remember(comments, commentSortFilter, commentsTimedOnly) {
            applyVideoCommentFilters(comments, commentSortFilter, commentsTimedOnly)
        }

    LaunchedEffect(source) {
        viewModel.load(source)
    }

    // Release the pool on the way out — unless a later Shorts screen has claimed it in the meantime.
    // An external /shorts/ link arriving while the Shorts tab is open pushes a second destination,
    // and the outgoing screen's dispose runs after the incoming one has already prepared its players.
    DisposableEffect(Unit) {
        val playerPool = ShortsPlayerPool.getInstance()
        val hostToken = playerPool.acquireHost()
        onDispose { playerPool.releaseIfHost(hostToken) }
    }

    val sheetInsets = rememberShortsSheetInsetState()
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { ShortsRefreshPullThreshold.toPx() }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        val canShrinkReel = maxHeight > maxWidth
        val sheetExpandedHeight = (maxHeight * SHORTS_SHEET_HEIGHT_FRACTION).takeIf { canShrinkReel }
        val sheetExpandedHeightPx = with(density) { (sheetExpandedHeight ?: 0.dp).toPx() }
        SideEffect {
            sheetInsets.containerHeightPx = constraints.maxHeight.toFloat()
            sheetInsets.shrinkEnabled = canShrinkReel
        }
        val screenSheetOpen = showCommentsSheet || showDescriptionSheet

        // One silent retry before the screen admits defeat. A page that lands empty is usually
        // transient -- a continuation that returned nothing, a request that failed -- and making the
        // viewer press a button for it is how a feed that would have worked reads as broken.
        // Counted outside the branch below, because that branch leaves composition while the retry
        // is in flight and a flag held inside it would reset into an endless retry.
        var emptyRetries by remember(source) { mutableIntStateOf(0) }
        LaunchedEffect(uiState.shorts.isEmpty(), uiState.isLoading, uiState.isRefreshing) {
            if (uiState.shorts.isNotEmpty()) {
                emptyRetries = 0
                return@LaunchedEffect
            }
            if (uiState.isLoading || uiState.isRefreshing || emptyRetries >= MAX_EMPTY_AUTO_RETRIES) {
                return@LaunchedEffect
            }
            emptyRetries++
            delay(EMPTY_AUTO_RETRY_DELAY_MS)
            viewModel.retry(source)
        }

        when {
            uiState.isLoading && uiState.shorts.isEmpty() -> {
                ShortsLoadingState(modifier = Modifier.align(Alignment.Center))
            }

            // One branch for every way of having nothing to show. A source that returns an empty
            // page without failing used to match no branch at all: the screen rendered black with
            // no control left on it to load anything.
            uiState.shorts.isEmpty() -> {
                ShortsErrorState(
                    error = uiState.error ?: stringResource(R.string.no_shorts_found),
                    onRetry = { viewModel.retry(source) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                val pagerState =
                    rememberPagerState(
                        initialPage = uiState.currentIndex,
                        pageCount = { uiState.shorts.size },
                    )

                // Track page changes. Keyed on the queue size as well: an append that lands while
                // the viewer is already near the end has to be re-evaluated, or the next request
                // waits on a page change that a short feed may never produce.
                LaunchedEffect(pagerState.currentPage, uiState.shorts.size) {
                    viewModel.updateCurrentIndex(pagerState.currentPage)
                }

                // Load likes and metadata for the current short
                LaunchedEffect(pagerState.currentPage) {
                    delay(750)
                    uiState.shorts.getOrNull(pagerState.currentPage)?.let {
                        viewModel.loadShortDetails(it.id)
                    }
                }

                // Track settled page for player pool management
                val settledShortId = uiState.shorts.getOrNull(pagerState.settledPage)?.id
                // A short whose streams would not resolve. Held by id, not by index, so paging past
                // it and back does not resurrect a stale failure on a different reel.
                var failedShortId by remember { mutableStateOf<String?>(null) }
                var streamRetryToken by remember { mutableIntStateOf(0) }
                LaunchedEffect(pagerState.settledPage, settledShortId, shortsTargetHeight, streamRetryToken) {
                    val targetHeight = shortsTargetHeight ?: return@LaunchedEffect
                    val settled = pagerState.settledPage
                    val playerPool = ShortsPlayerPool.getInstance()
                    playerPool.initialize(context)
                    playerPool.setCurrentVideo(uiState.shorts.getOrNull(settled))

                    val preferredLang = audioLangPref.preferredAudioLanguage.first()

                    suspend fun prepareShort(
                        index: Int,
                        short: ShortVideo,
                        shouldPlay: Boolean,
                    ) {
                        try {
                            val streams = viewModel.getPlaybackStreams(short.id, targetHeight, preferredLang)
                            if (streams != null) {
                                playerPool.prepare(
                                    index = index,
                                    videoId = short.id,
                                    videoUrl = streams.videoUrl,
                                    audioUrl = streams.audioUrl,
                                    shouldPlay = shouldPlay,
                                    videoDashManifest = streams.videoDashManifest,
                                    audioDashManifest = streams.audioDashManifest,
                                )
                                if (failedShortId == short.id) failedShortId = null
                            } else {
                                Log.w("ShortsScreen", "No stream URL resolved for ${short.id}")
                                // Only the visible page gets an error: a neighbour that fails is
                                // re-prepared when it becomes the settled one anyway.
                                if (shouldPlay) failedShortId = short.id
                            }
                        } catch (e: Exception) {
                            Log.e("ShortsScreen", "Failed to prepare player for ${short.id}", e)
                            if (shouldPlay) failedShortId = short.id
                        }
                    }

                    playerPool.activatePlayer(settled)

                    // Awaited, not launched alongside the neighbours. Each resolve mints a BotGuard
                    // PoToken, and those serialise on one process-wide WebView — so firing all of
                    // them at once can leave the short the user is looking at queued behind two it
                    // cannot see.
                    uiState.shorts.getOrNull(settled)?.let { currentShort ->
                        prepareShort(settled, currentShort, shouldPlay = true)
                    }

                    playerPool.releaseUnusedPlayers(settled)

                    uiState.shorts.getOrNull(settled + 1)?.let { nextShort ->
                        launch { prepareShort(settled + 1, nextShort, shouldPlay = false) }
                    }
                    uiState.shorts.getOrNull(settled - 1)?.let { prevShort ->
                        launch { prepareShort(settled - 1, prevShort, shouldPlay = false) }
                    }
                    // Two and three ahead: resolved only, never handed to a player — the pool holds
                    // three, and its slots belong to settled-1, settled and settled+1. Resolving
                    // further ahead is what makes a fast scroll find the stream already in the
                    // repository cache instead of waiting on the network mid-swipe. Last, so it
                    // never competes with the visible short.
                    listOfNotNull(
                        uiState.shorts.getOrNull(settled + 2),
                        uiState.shorts.getOrNull(settled + 3),
                    ).forEach { preloadShort ->
                        launch {
                            runCatching {
                                viewModel.getPlaybackStreams(preloadShort.id, targetHeight, preferredLang)
                            }
                        }
                    }
                }

                LaunchedEffect(shortsTargetHeight) {
                    val newHeight = shortsTargetHeight ?: return@LaunchedEffect
                    val previous = prevShortsTargetHeight.value
                    prevShortsTargetHeight.value = newHeight
                    // The first non-null value is the preference loading, not the user changing it.
                    // The settle effect above already prepares at that height.
                    if (previous == null || newHeight == previous) return@LaunchedEffect

                    val settled = pagerState.settledPage
                    val playerPool = ShortsPlayerPool.getInstance()
                    val preferredLang = audioLangPref.preferredAudioLanguage.first()

                    val currentShort = uiState.shorts.getOrNull(settled) ?: return@LaunchedEffect
                    try {
                        val streams = viewModel.getPlaybackStreams(currentShort.id, newHeight, preferredLang)
                        if (streams != null) {
                            playerPool.reloadWithVideoUrl(
                                settled,
                                currentShort.id,
                                streams.videoUrl,
                                streams.videoDashManifest,
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ShortsScreen", "Quality change: failed to reload ${currentShort.id}", e)
                    }

                    uiState.shorts.getOrNull(settled + 1)?.let { nextShort ->
                        launch {
                            runCatching {
                                val streams = viewModel.getPlaybackStreams(nextShort.id, newHeight, preferredLang)
                                if (streams != null) {
                                    playerPool.reloadWithVideoUrl(
                                        settled + 1,
                                        nextShort.id,
                                        streams.videoUrl,
                                        streams.videoDashManifest,
                                    )
                                }
                            }
                        }
                    }
                    uiState.shorts.getOrNull(settled - 1)?.let { prevShort ->
                        launch {
                            runCatching {
                                val streams = viewModel.getPlaybackStreams(prevShort.id, newHeight, preferredLang)
                                if (streams != null) {
                                    playerPool.reloadWithVideoUrl(
                                        settled - 1,
                                        prevShort.id,
                                        streams.videoUrl,
                                        streams.videoDashManifest,
                                    )
                                }
                            }
                        }
                    }
                }

                val refreshConnection =
                    remember(source, refreshThresholdPx) {
                        ShortsPullToRefreshConnection(refreshThresholdPx) { viewModel.refresh(source) }
                    }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().nestedScroll(refreshConnection),
                    beyondViewportPageCount = 1,
                    key = { uiState.shorts[it].id },
                ) { page ->
                    val short = uiState.shorts[page]
                    val isActive = page == pagerState.currentPage

                    ShortVideoPage(
                        video = short.toVideo(),
                        isActive = isActive,
                        pageIndex = page,
                        viewModel = viewModel,
                        bottomNavOverlayPadding = bottomNavOverlayPadding,
                        sheetInsets = sheetInsets,
                        screenSheetOpen = screenSheetOpen,
                        actions =
                            ShortVideoPageActions(
                                onChannelClick = { onChannelClick(short.channelId) },
                                onCommentsClick = {
                                    viewModel.loadComments(short.id)
                                    showCommentsSheet = true
                                },
                                onDescriptionClick = {
                                    scope.launch { viewModel.loadShortDetails(short.id) }
                                    showDescriptionSheet = true
                                },
                                onShareClick = {
                                    val sendIntent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                context.getString(R.string.share_short_template, short.id),
                                            )
                                            type = "text/plain"
                                        }
                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                },
                                onWantMore = { viewModel.wantMoreLikeThis(short) },
                                onNotInterested = { viewModel.notInterested(short) },
                                onVideoEnded = {
                                    scope.launch {
                                        if (page < pagerState.pageCount - 1) {
                                            pagerState.animateScrollToPage(page + 1)
                                        }
                                    }
                                },
                            ),
                    )
                }

                if (uiState.isRefreshing && !isInPip) {
                    LoadingIndicator(
                        color = Color.White,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = ShortsRefreshIndicatorPadding)
                                .size(38.dp),
                    )
                }

                if (failedShortId != null && failedShortId == settledShortId && !isInPip) {
                    ShortsErrorState(
                        error = stringResource(R.string.error_short_load),
                        onRetry = {
                            failedShortId = null
                            streamRetryToken++
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                // Loading more indicator at bottom
                if (uiState.isLoadingMore && !isInPip) {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Comments Sheet
        if (showCommentsSheet) {
            DisposableEffect(Unit) { onDispose { sheetInsets.release() } }
            YTCommentsBottomSheet(
                comments = visibleComments,
                isLoading = isLoadingComments,
                selectedFilter = commentSortFilter,
                totalText = commentTotalText,
                timedOnly = commentsTimedOnly,
                onTimedChange = { commentsTimedOnly = it },
                onFilterChanged = { filter ->
                    commentSortFilter = filter
                    videoCommentSortFor(commentSortOptions, filter)?.let { sort ->
                        uiState.shorts.getOrNull(uiState.currentIndex)?.id?.let { videoId ->
                            viewModel.selectCommentSort(videoId, sort)
                        }
                    }
                },
                onLoadReplies = { viewModel.loadCommentReplies(it) },
                onAuthorClick = { authorChannelRef ->
                    showCommentsSheet = false
                    onChannelClick(authorChannelRef)
                },
                expandedHeight = sheetExpandedHeight,
                onSheetProgressChange = { progress -> sheetInsets.follow(sheetExpandedHeightPx * progress) },
                dismissOnOutsideTap = true,
                shape = SHORTS_SHEET_SHAPE,
                onDismiss = { showCommentsSheet = false },
            )
        }

        // Description Sheet
        if (showDescriptionSheet && uiState.shorts.isNotEmpty()) {
            DisposableEffect(Unit) { onDispose { sheetInsets.release() } }
            val safeIndex = uiState.currentIndex.coerceIn(0, uiState.shorts.size - 1)
            YTDescriptionBottomSheet(
                video = uiState.shorts[safeIndex].toVideo(),
                expandedHeight = sheetExpandedHeight,
                onSheetProgressChange = { progress -> sheetInsets.follow(sheetExpandedHeightPx * progress) },
                dismissOnOutsideTap = true,
                shape = SHORTS_SHEET_SHAPE,
                onDismiss = { showDescriptionSheet = false },
            )
        }

        // Top Bar Overlay
        ShortsTopBar(
            visible = uiState.shorts.isNotEmpty() && !isInPip,
            showBackButton = source != ShortsQueueSource.Feed,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (isInPip) return@BoxWithConstraints
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}

@Composable
private fun ShortsTopBar(
    visible: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = Color.White,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.shorts),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShortsLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LoadingIndicator(
            color = Color.White,
            modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(R.string.loading_shorts),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ShortsErrorState(
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = error ?: stringResource(R.string.error_short_load),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
        )
        FilledTonalButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
