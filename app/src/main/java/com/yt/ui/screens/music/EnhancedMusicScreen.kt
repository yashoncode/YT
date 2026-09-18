package com.yt.ui.screens.music

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yt.R
import com.yt.data.music.model.MusicItemType
import com.yt.data.music.model.MusicTrack
import com.yt.innertube.pages.MoodAndGenres
import com.yt.ui.TabScrollEventBus
import com.yt.ui.components.glow
import com.yt.ui.components.layout.topbar.YTTopBar
import com.yt.ui.components.music.common.LocalMusicMiniPlayerInset
import com.yt.ui.components.music.section.HomeSectionType
import com.yt.ui.components.music.section.musicHomeFeed
import com.yt.ui.components.music.sheet.MusicCollectionActionItem
import com.yt.ui.components.music.sheet.MusicCollectionQuickActionsSheet
import com.yt.ui.components.music.sheet.MusicQuickActionsSheet
import com.yt.ui.components.shared.MusicScreenShimmerLoading
import com.yt.ui.components.shared.YTErrorState
import com.yt.ui.components.shared.YTPullToRefreshBox
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import java.util.Random

private val RecognizeFabShape = CircleShape
private val FabBlurRadius = 26.dp
private val FabGlowRadius = 26.dp
private const val FAB_GLASS_ALPHA = 0.38f

private val TOP_BAR_BLUR_RADIUS = 24.dp
private const val TOP_BAR_GLASS_ALPHA = 0.5f

private val FeedBottomClearance = 96.dp

private fun MusicTrack.isAudioMusicCandidate(): Boolean {
    val usableDuration = duration == 0 || duration in 30..1200
    return itemType == MusicItemType.SONG && !isVideoSong && videoId.isNotBlank() && usableDuration
}

private fun List<MusicTrack>.audioMusicOnly(): List<MusicTrack> = filter { it.isAudioMusicCandidate() }.distinctBy { it.videoId }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EnhancedMusicScreen(
    onSongClick: (MusicTrack, List<MusicTrack>, String?) -> Unit,
    onVideoClick: (MusicTrack) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onRecognizeClick: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onMoodsClick: (MoodAndGenres.Item?) -> Unit = {},
    bottomNavOverlayPadding: () -> Dp = { 0.dp },
    viewModel: MusicViewModel = sharedMusicViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val musicListState = rememberLazyListState()
    val quickPicksGridState = rememberLazyGridState()

    // Planner-lite: the brain's maturity decides which sections lead the page —
    // a cold brain has nothing personal to say, a mature one leads with taste.
    val sectionOrder =
        remember(uiState.sessionSeed, uiState.brainMaturity) {
            val defaultOrder = HomeSectionType.entries
            val anchored =
                when (uiState.brainMaturity) {
                    "cold_start" -> {
                        listOf(
                            HomeSectionType.CHARTS,
                            HomeSectionType.MOODS_AND_GENRES,
                            HomeSectionType.NEW_RELEASES,
                        )
                    }

                    "mature" -> {
                        listOf(
                            HomeSectionType.QUICK_PICKS,
                            HomeSectionType.SIMILAR_TO,
                            HomeSectionType.FROM_COMMUNITY,
                        )
                    }

                    else -> {
                        listOf(
                            HomeSectionType.QUICK_PICKS,
                            HomeSectionType.FROM_COMMUNITY,
                            HomeSectionType.DAILY_DISCOVER,
                        )
                    }
                }
            val dynamicPool = defaultOrder - anchored
            anchored + dynamicPool.shuffled(Random(uiState.sessionSeed))
        }

    // Scroll to top and refresh when tapping the music tab while already on this screen
    LaunchedEffect(Unit) {
        TabScrollEventBus.scrollToTopEvents
            .filter { it == "music" }
            .collectLatest {
                musicListState.animateScrollToItem(0)
                viewModel.refresh()
            }
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var selectedCollection by remember { mutableStateOf<MusicCollectionActionItem?>(null) }

    if (showBottomSheet && selectedTrack != null) {
        MusicQuickActionsSheet(
            track = selectedTrack!!,
            onDismiss = { showBottomSheet = false },
            onViewArtist = { channelId ->
                if (channelId.isNotEmpty()) {
                    onArtistClick(channelId)
                }
            },
            onViewAlbum = { albumId ->
                if (albumId.isNotEmpty()) {
                    onAlbumClick(albumId)
                }
            },
            onShare = {
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, selectedTrack!!.title)
                        val text =
                            context.getString(
                                R.string.share_message_template,
                                selectedTrack!!.title,
                                selectedTrack!!.artist,
                                selectedTrack!!.videoId,
                            )
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_song)))
            },
        )
    }

    selectedCollection?.let { collection ->
        MusicCollectionQuickActionsSheet(
            item = collection,
            onDismiss = { selectedCollection = null },
            onOpen = { onAlbumClick(collection.id) },
        )
    }

    // Scoped to this screen's feed: the shell's state contains this whole screen, and an effect
    // cannot sample a source it is part of.
    val feedHazeState = rememberHazeState()
    val fabAccent = MaterialTheme.colorScheme.primary
    val fabSurface = MaterialTheme.colorScheme.surface
    val fabHazeStyle =
        remember(fabSurface, fabAccent) {
            HazeStyle(
                backgroundColor = fabSurface,
                tint = HazeTint(fabAccent.copy(alpha = FAB_GLASS_ALPHA)),
                blurRadius = FabBlurRadius,
            )
        }

    val bottomChrome = bottomNavOverlayPadding() + LocalMusicMiniPlayerInset.current
    val backdrop = MaterialTheme.colorScheme.background
    val topBarHazeStyle =
        remember(backdrop) {
            HazeStyle(
                backgroundColor = backdrop,
                tint = HazeTint(backdrop.copy(alpha = TOP_BAR_GLASS_ALPHA)),
                blurRadius = TOP_BAR_BLUR_RADIUS,
            )
        }

    val fabLift =
        animateDpAsState(
            targetValue = bottomChrome,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            label = "musicRecognizeFabLift",
        )

    Scaffold(
        topBar = {
            YTTopBar(
                modifier =
                    Modifier.hazeEffect(feedHazeState, topBarHazeStyle) {
                        // Solid across the bar, gone by the time it meets the feed, so there is no
                        // edge where the blur stops.
                        progressive =
                            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    },
                title = stringResource(R.string.screen_title_music),
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRecognizeClick,
                shape = RecognizeFabShape,
                // Transparent, because what fills it is the blurred feed behind it.
                containerColor = Color.Transparent,
                contentColor = fabAccent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier =
                    Modifier
                        .offset { IntOffset(x = 0, y = -fabLift.value.roundToPx()) }
                        .glow(color = fabAccent, shape = RecognizeFabShape, radius = FabGlowRadius)
                        .clip(RecognizeFabShape)
                        .hazeEffect(feedHazeState, fabHazeStyle)
                        .border(Dp.Hairline, fabAccent.copy(alpha = 0.4f), RecognizeFabShape),
            ) {
                Icon(Icons.Rounded.Mic, stringResource(R.string.recognize_music))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { paddingValues ->
        // No top inset: the feed runs full-bleed under the bar so the bar has something to blur.
        // Each branch below clears it through topContentPadding instead.
        val topContentPadding = paddingValues.calculateTopPadding()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(feedHazeState),
        ) {
            val isInitialLoading = uiState.isLoading && uiState.trendingSongs.isEmpty() && uiState.dynamicSections.isEmpty()

            when {
                isInitialLoading -> {
                    MusicScreenShimmerLoading(modifier = Modifier.padding(top = topContentPadding))
                }

                uiState.error != null && uiState.trendingSongs.isEmpty() -> {
                    YTErrorState(
                        error = uiState.error ?: stringResource(R.string.error_occurred),
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.padding(top = topContentPadding),
                    )
                }

                else -> {
                    val popularArtists =
                        remember(uiState.trendingSongs, uiState.newReleases) {
                            (uiState.trendingSongs + uiState.newReleases)
                                .distinctBy { it.artist }
                                .take(10)
                        }

                    // Raw concatenation is only the placeholder until the VM's
                    // brain-ranked speed dial lands.
                    val fallbackSpeedDial =
                        remember(uiState.history, uiState.forYouTracks, uiState.listenAgain) {
                            (uiState.history + uiState.forYouTracks + uiState.listenAgain)
                                .audioMusicOnly()
                                .take(26)
                        }
                    val speedDialTracks = uiState.speedDialTracks.ifEmpty { fallbackSpeedDial }
                    val quickPickTracks =
                        remember(uiState.forYouTracks) {
                            uiState.forYouTracks.audioMusicOnly().take(20)
                        }
                    val pullState = rememberPullToRefreshState()

                    YTPullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        state = pullState,
                        indicatorTopPadding = topContentPadding,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = musicListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    top = topContentPadding,
                                    bottom = bottomChrome + FeedBottomClearance,
                                ),
                        ) {
                            musicHomeFeed(
                                uiState = uiState,
                                sectionOrder = sectionOrder,
                                quickPickTracks = quickPickTracks,
                                speedDialTracks = speedDialTracks,
                                popularArtists = popularArtists,
                                quickPicksGridState = quickPicksGridState,
                                onSongClick = onSongClick,
                                onVideoClick = onVideoClick,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                                onMoodsClick = onMoodsClick,
                                onChipToggle = { viewModel.setHomeChip(it) },
                                onTrackMenu = { track ->
                                    selectedTrack = track
                                    showBottomSheet = true
                                },
                                onCollectionMenu = { collection -> selectedCollection = collection },
                                onLoadMore = { viewModel.loadMoreHomeContent() },
                            )
                        }
                    }
                }
            }
        }
    }
}
