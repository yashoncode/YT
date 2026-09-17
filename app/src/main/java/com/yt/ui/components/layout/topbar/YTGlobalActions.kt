package com.yt.ui.components.layout.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
/**
 * App-shell destinations that must stay reachable from every root screen.
 *
 * Settings is the last of them. Subscriptions, Library and Notifications used to need a shell
 * entry point of their own; they are rows inside Settings now, so reaching Settings reaches them.
 */
@Immutable
data class YTGlobalActions(
    val onOpenSettings: () -> Unit,
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
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val actions =
        remember(onOpenSettings) {
            YTGlobalActions(onOpenSettings = onOpenSettings)
        }
    CompositionLocalProvider(LocalYTGlobalActions provides actions, content = content)
}
