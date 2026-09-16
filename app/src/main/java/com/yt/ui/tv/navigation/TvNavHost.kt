package com.yt.ui.tv.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yt.data.model.Video
import com.yt.ui.screens.home.HomeViewModel
import com.yt.ui.screens.music.MusicPlayerViewModel
import com.yt.ui.screens.music.MusicViewModel
import com.yt.ui.screens.search.SearchViewModel
import com.yt.ui.screens.subscriptions.SubscriptionsViewModel
import com.yt.ui.tv.screens.TvArtistScreen
import com.yt.ui.tv.screens.TvChannelScreen
import com.yt.ui.tv.screens.TvHomeScreen
import com.yt.ui.tv.screens.TvLibraryScreen
import com.yt.ui.tv.screens.TvMusicCollectionScreen
import com.yt.ui.tv.screens.TvMusicScreen
import com.yt.ui.tv.screens.TvPlaylistDetailScreen
import com.yt.ui.tv.screens.TvRemoteGuideScreen
import com.yt.ui.tv.screens.TvSearchScreen
import com.yt.ui.tv.screens.TvSettingsScreen
import com.yt.ui.tv.screens.TvSubscriptionsScreen
import com.yt.ui.tv.screens.TvSyncScreen

/** Top-level TV navigation graph plus detail routes (channel, …). */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TvNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    musicViewModel: MusicViewModel,
    musicPlayerViewModel: MusicPlayerViewModel,
    subscriptionsViewModel: SubscriptionsViewModel,
    searchViewModel: SearchViewModel,
    onPlayVideo: (Video) -> Unit,
    onPlayPlaylist: (List<Video>, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openChannel: (String) -> Unit = { channelRef ->
        navController.navigate(TvRoutes.channel(channelRef))
    }

    NavHost(
        navController = navController,
        startDestination = TvDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TvDestination.HOME.route) {
            TvHomeScreen(
                viewModel = homeViewModel,
                onVideoClick = onPlayVideo,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvDestination.MUSIC.route) {
            TvMusicScreen(
                viewModel = musicViewModel,
                onTrackClick = musicPlayerViewModel::loadAndPlayTrack,
                onOpenCollection = { navController.navigate(TvRoutes.musicCollection(it)) },
                onOpenArtist = { navController.navigate(TvRoutes.musicArtist(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(
            route = TvRoutes.MUSIC_ARTIST,
            arguments = listOf(navArgument(TvRoutes.MUSIC_ARTIST_ARG) { type = NavType.StringType }),
        ) { entry ->
            val artistChannelId = entry.arguments?.getString(TvRoutes.MUSIC_ARTIST_ARG).orEmpty()
            TvArtistScreen(
                channelId = artistChannelId,
                viewModel = musicViewModel,
                onTrackClick = musicPlayerViewModel::loadAndPlayTrack,
                onOpenCollection = { navController.navigate(TvRoutes.musicCollection(it)) },
                onOpenArtist = { navController.navigate(TvRoutes.musicArtist(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(
            route = TvRoutes.MUSIC_COLLECTION,
            arguments = listOf(navArgument(TvRoutes.MUSIC_COLLECTION_ARG) { type = NavType.StringType }),
        ) { entry ->
            val collectionId = entry.arguments?.getString(TvRoutes.MUSIC_COLLECTION_ARG).orEmpty()
            TvMusicCollectionScreen(
                collectionId = collectionId,
                viewModel = musicViewModel,
                onTrackClick = musicPlayerViewModel::loadAndPlayTrack,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvDestination.SUBSCRIPTIONS.route) {
            TvSubscriptionsScreen(
                viewModel = subscriptionsViewModel,
                onVideoClick = onPlayVideo,
                onChannelClick = openChannel,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvDestination.SEARCH.route) {
            TvSearchScreen(
                viewModel = searchViewModel,
                onVideoClick = onPlayVideo,
                onChannelClick = openChannel,
                onOpenPlaylist = { navController.navigate(TvRoutes.playlist(it)) },
                onPlayTrack = musicPlayerViewModel::loadAndPlayTrack,
                onOpenMusicCollection = { navController.navigate(TvRoutes.musicCollection(it)) },
                onOpenMusicArtist = { navController.navigate(TvRoutes.musicArtist(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvDestination.LIBRARY.route) {
            TvLibraryScreen(
                onVideoClick = onPlayVideo,
                onOpenPlaylist = { navController.navigate(TvRoutes.playlist(it)) },
                onPlayTrack = musicPlayerViewModel::loadAndPlayTrack,
                onOpenMusicCollection = { navController.navigate(TvRoutes.musicCollection(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvDestination.SETTINGS.route) {
            TvSettingsScreen(
                onOpenSync = { navController.navigate(TvRoutes.SYNC) },
                onOpenRemoteGuide = { navController.navigate(TvRoutes.REMOTE_GUIDE) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvRoutes.SYNC) {
            TvSyncScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(TvRoutes.REMOTE_GUIDE) {
            TvRemoteGuideScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = TvRoutes.CHANNEL,
            arguments = listOf(
                navArgument(TvRoutes.CHANNEL_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val channelRef = entry.arguments?.getString(TvRoutes.CHANNEL_ARG)
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                .orEmpty()
            TvChannelScreen(
                channelUrl = channelRef,
                onVideoClick = onPlayVideo,
                onOpenPlaylist = { navController.navigate(TvRoutes.playlist(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(
            route = TvRoutes.PLAYLIST,
            arguments = listOf(navArgument(TvRoutes.PLAYLIST_ARG) { type = NavType.StringType }),
        ) {
            TvPlaylistDetailScreen(
                onVideoClick = onPlayVideo,
                onPlayPlaylist = onPlayPlaylist,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
