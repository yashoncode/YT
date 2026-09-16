package com.yt.ui.screens.sync

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.yt.sync.SyncManager
import com.yt.sync.SyncState
import com.yt.sync.protocol.SyncRole
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin ViewModel over the singleton [SyncManager] (which survives config changes). */
@HiltViewModel
class SyncViewModel
    @Inject
    constructor(
        private val manager: SyncManager,
    ) : ViewModel() {
        val state: StateFlow<SyncState> = manager.state

        fun host(
            role: SyncRole,
            collections: List<String>,
        ) = manager.host(role, collections)

        fun hostForTv(
            role: SyncRole,
            collections: List<String>,
        ) = manager.hostForTv(role, collections)

        fun join(
            role: SyncRole,
            qrText: String,
            collections: List<String>,
        ) = manager.join(role, qrText, collections)

        fun extendPairingForManualShare(): String? = manager.extendPairingForManualShare()

        fun confirmSas(matches: Boolean) = manager.confirmSas(matches)

        fun confirmConsent(accepted: Boolean) = manager.confirmConsent(accepted)

        fun cancel() = manager.cancel()

        fun reset() = manager.reset()
    }
