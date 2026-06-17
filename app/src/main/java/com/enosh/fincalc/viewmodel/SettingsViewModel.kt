package com.enosh.fincalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.utils.UserUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CloudSyncUiState(
    val isLoading: Boolean = false,
    val status: String = "Pending",
    val lastError: String? = null
)

class SettingsViewModel : ViewModel() {
    private val _cloudSyncState = MutableStateFlow(CloudSyncUiState())
    val cloudSyncState = _cloudSyncState.asStateFlow()

    fun uploadProfileToCloud() {
        viewModelScope.launch {
            _cloudSyncState.value = CloudSyncUiState(isLoading = true, status = "Uploading...")
            try {
                UserUtils.uploadCurrentUser()
                _cloudSyncState.value = CloudSyncUiState(
                    isLoading = false,
                    status = "Success",
                    lastError = null
                )
            } catch (e: Exception) {
                _cloudSyncState.value = CloudSyncUiState(
                    isLoading = false,
                    status = "Failed",
                    lastError = e.message ?: e.toString()
                )
            }
        }
    }
}
