package com.yt.ui.components.layout.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.yt.R

/**
 * The one shell action every root destination gets.
 *
 * Settings is the only button left in the bar: Subscriptions, Library and Notifications live
 * inside Settings now, so the bar can give its width to the search field instead.
 *
 * Renders nothing when the shell has not provided [LocalYTGlobalActions] — the case in previews
 * and in isolated Compose tests.
 */
@Composable
internal fun YTGlobalActionsRow() {
    val actions = LocalYTGlobalActions.current ?: return

    IconButton(onClick = actions.onOpenSettings) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings),
        )
    }
}

/**
 * One entry in a top bar overflow menu.
 *
 * Deliberately not `YTMenuItemData` — that models the card-list menus rendered inside screen
 * content, while this is a plain Material 3 `DropdownMenuItem`.
 */
@Immutable
data class YTTopBarMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val icon: ImageVector? = null,
)

/**
 * Collapses extra actions behind a single overflow button.
 *
 * A root destination has room for two screen-specific actions before the two global ones; anything
 * past that belongs here rather than competing with the title for width.
 */
@Composable
fun YTTopBarOverflow(
    items: List<YTTopBarMenuItem>,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.more_options),
) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = contentDescription,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    enabled = item.enabled,
                    leadingIcon =
                        item.icon?.let { icon ->
                            { Icon(imageVector = icon, contentDescription = null) }
                        },
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}
