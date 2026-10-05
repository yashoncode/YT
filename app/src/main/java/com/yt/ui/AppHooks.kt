package com.yt.ui

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.yt.R
import com.yt.data.shorts.queue.ShortsQueueSource
import com.yt.utils.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Suspends until the NavHost has set its graph. The NavHost is composed only once the onboarding
 * check resolves, so a fresh activity has a window where navigate() throws.
 */
suspend fun NavController.awaitGraph() {
    currentBackStackEntryFlow.first()
}

@Composable
fun HandleDeepLinks(
    deeplinkVideoId: String?,
    isShort: Boolean,
    navController: NavController,
    onDeeplinkConsumed: () -> Unit,
) {
    LaunchedEffect(deeplinkVideoId, isShort) {
        val videoId = deeplinkVideoId ?: return@LaunchedEffect
        navController.awaitGraph()
        if (isShort) {
            val src = Uri.encode(ShortsQueueSource.SeededFeed(videoId).encode())
            navController.navigate("shorts?src=$src") {
                launchSingleTop = true
            }
        } else {
            navController.navigate("player/$videoId") {
                launchSingleTop = true
            }
        }
        onDeeplinkConsumed()
    }
}

private const val OFFLINE_NOTICE_DELAY_MS = 3_000L

@Composable
fun OfflineMonitor(
    context: Context,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    currentRoute: State<String>,
) {
    val connectivity = remember(context) { NetworkConnectivityObserver(context) }
    val isConnected by remember(connectivity) { connectivity.observeConnectivity() }
        .collectAsStateWithLifecycle(initialValue = true)
    val route = currentRoute.value

    LaunchedEffect(isConnected, route) {
        if (isConnected) return@LaunchedEffect

        val isSafeRoute =
            route == "downloads" ||
                route.startsWith("player") ||
                route.startsWith("musicPlayer") ||
                route == "settings"
        if (isSafeRoute) return@LaunchedEffect

        delay(OFFLINE_NOTICE_DELAY_MS)

        val result =
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.error_no_internet_found),
                actionLabel = context.getString(R.string.downloads_title),
                duration = SnackbarDuration.Short,
            )
        if (result == SnackbarResult.ActionPerformed) {
            navController.navigate("downloads") {
                launchSingleTop = true
            }
        }
    }
}
