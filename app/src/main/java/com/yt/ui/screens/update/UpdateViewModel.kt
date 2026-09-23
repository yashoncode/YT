package com.yt.ui.screens.update

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yt.BuildConfig
import com.yt.data.local.LocalDataManager
import com.yt.utils.UpdateInfo
import com.yt.utils.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val release: UpdateInfo? = null,
    val checking: Boolean = false,
    val upToDate: Boolean = false,
    val failed: Boolean = false,
)

/**
 * One activity-scoped owner for the update popup, so a check started from Settings and
 * the silent launch check land in the same sheet. Only a check the user asked for
 * reports "up to date" or a failure; the launch check stays quiet unless there is news.
 */
@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        private val dataManager: LocalDataManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow(UpdateUiState())
        val state: StateFlow<UpdateUiState> = _state.asStateFlow()

        private var launchCheckDone = false

        /** Activity recreation calls this again; the ViewModel outlives it, so only the first call counts. */
        fun checkOnLaunch() {
            if (launchCheckDone || BuildConfig.DEBUG || !BuildConfig.UPDATER_ENABLED) return
            launchCheckDone = true
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                if (now - dataManager.lastUpdateCheck.first() < LAUNCH_COOLDOWN_MS) return@launch
                run(announce = false)
            }
        }

        fun check() {
            viewModelScope.launch { run(announce = true) }
        }

        fun show(release: UpdateInfo) {
            _state.value = UpdateUiState(release = release)
        }

        fun dismiss() {
            _state.value = UpdateUiState()
        }

        private suspend fun run(announce: Boolean) {
            if (_state.value.checking) return
            _state.update { it.copy(checking = true, upToDate = false, failed = false) }
            _state.value =
                try {
                    val release = UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
                    dataManager.setLastUpdateCheck(System.currentTimeMillis())
                    UpdateUiState(release = release, upToDate = announce && release == null)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Update check failed", e)
                    UpdateUiState(failed = announce)
                }
        }

        private companion object {
            const val TAG = "UpdateViewModel"
            const val LAUNCH_COOLDOWN_MS = 24L * 60 * 60 * 1000
        }
    }

/** The activity's single [UpdateViewModel], shared by the launch popup and Settings. */
@Composable
fun sharedUpdateViewModel(): UpdateViewModel {
    val activity = LocalContext.current as? ComponentActivity
    return if (activity != null) hiltViewModel(activity) else hiltViewModel()
}
