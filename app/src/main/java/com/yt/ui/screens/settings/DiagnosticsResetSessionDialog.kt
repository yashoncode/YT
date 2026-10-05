package com.yt.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yt.R
import com.yt.player.stream.ClientGateTracker
import com.yt.utils.cipher.CipherDeobfuscator
import com.yt.utils.cipher.PlayerJsFetcher
import com.yt.utils.potoken.WebPoTokenSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Re-attesting alone cannot lift a verdict GVS has already reached about this visitor, so the
// identity itself has to go — the supported form of the "clear app data" workaround for videos
// that stall a minute in.
internal suspend fun resetYouTubeSession() {
    WebPoTokenSession.resetIdentity()
    ClientGateTracker.clear()
    CipherDeobfuscator.invalidateSignatureTimestamp()
    withContext(Dispatchers.IO) { PlayerJsFetcher.invalidateCache() }
}

@Composable
internal fun DiagnosticsResetSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
        title = { Text(stringResource(R.string.diagnostics_reset_session_confirm_title)) },
        text = { Text(stringResource(R.string.diagnostics_reset_session_confirm_body)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.reset)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
