package com.yt.ui.components.layout.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

/**
 * App-shell destinations that must stay reachable from every root screen.
 *
 * Before this existed, `notifications` had a single entry point — the Home top bar — so turning
 * Home off in navigation settings orphaned the screen entirely. Subscriptions and Library are here
 * for the same reason: they left the bottom bar, and the top bar of every backless destination is
 * now their only entry point.
 *
 * [unreadNotifications] is the flow rather than the current value so that a new notification
 * invalidates only the badge that collects it, instead of the whole shell that provides it.
 */
@Immutable
data class YTGlobalActions(
    val unreadNotifications: StateFlow<Int>,
    val onOpenNotifications: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenSubscriptions: () -> Unit,
    val onOpenLibrary: () -> Unit,
)

/**
 * Static because the provided value is remembered for the lifetime of the shell and never changes;
 * reading it must never invalidate a screen.
 */
val LocalYTGlobalActions = staticCompositionLocalOf<YTGlobalActions?> { null }

/**
 * Publishes the shell's global actions to every [YTTopBar] below it. Call once, around the
 * `NavHost`.
 */
@Composable
fun ProvideYTGlobalActions(
    unreadNotifications: StateFlow<Int>,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenLibrary: () -> Unit,
    content: @Composable () -> Unit,
) {
    val actions =
        remember(unreadNotifications, onOpenNotifications, onOpenSettings, onOpenSubscriptions, onOpenLibrary) {
            YTGlobalActions(
                unreadNotifications = unreadNotifications,
                onOpenNotifications = onOpenNotifications,
                onOpenSettings = onOpenSettings,
                onOpenSubscriptions = onOpenSubscriptions,
                onOpenLibrary = onOpenLibrary,
            )
        }
    CompositionLocalProvider(LocalYTGlobalActions provides actions, content = content)
}
