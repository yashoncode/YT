package com.yt.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yt.data.local.PlaylistRepository
import com.yt.data.model.PlaylistInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel
    @Inject
    constructor(
        private val repository: PlaylistRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PlaylistsUiState(isLoading = true))
        val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

        init {
            observePlaylists()
        }

        private fun observePlaylists() {
            viewModelScope.launch {
                launch {
                    repository.getUserCreatedVideoPlaylistsFlow().collect { playlists ->
                        _uiState.update { it.copy(isLoading = false, playlists = playlists) }
                    }
                }
                launch {
                    repository.getSavedVideoPlaylistsFlow().collect { savedPlaylists ->
                        _uiState.update { it.copy(savedPlaylists = savedPlaylists) }
                    }
                }
            }
        }

        fun createPlaylist(
            name: String,
            description: String,
        ) {
            viewModelScope.launch {
                repository.createPlaylist(
                    playlistId = System.currentTimeMillis().toString(),
                    name = name,
                    description = description,
                    isPrivate = true,
                )
            }
        }

        fun deletePlaylist(playlistId: String) {
            viewModelScope.launch {
                repository.deletePlaylist(playlistId)
            }
        }
    }

data class PlaylistsUiState(
    val isLoading: Boolean = false,
    val playlists: List<PlaylistInfo> = emptyList(),
    val savedPlaylists: List<PlaylistInfo> = emptyList(),
)
