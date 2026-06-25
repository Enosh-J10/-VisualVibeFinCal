package com.enosh.fincalc.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.utils.BackupUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val _isBackupLoading = MutableStateFlow(false)
    val isBackupLoading: StateFlow<Boolean> = _isBackupLoading

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage

    fun clearMessage() {
        _backupMessage.value = null
    }

    fun localExport(uri: Uri) {
        viewModelScope.launch {
            _isBackupLoading.value = true
            val success = BackupUtils.exportData(getApplication(), uri)
            _backupMessage.value = if (success) "Backup exported successfully!" else "Export failed."
            _isBackupLoading.value = false
        }
    }

    fun localImport(uri: Uri) {
        viewModelScope.launch {
            _isBackupLoading.value = true
            val success = BackupUtils.importData(getApplication(), uri, overwrite = true)
            _backupMessage.value = if (success) "Backup restored successfully!" else "Restore failed."
            _isBackupLoading.value = false
        }
    }

    fun cloudBackup() {
        viewModelScope.launch {
            _isBackupLoading.value = true
            try {
                val success = BackupUtils.cloudBackup(getApplication())
                _backupMessage.value = if (success) "Cloud backup completed." else "Cloud backup failed."
            } catch (e: Exception) {
                _backupMessage.value = "Cloud backup failed: ${e.message}"
            }
            _isBackupLoading.value = false
        }
    }

    fun cloudRestore() {
        viewModelScope.launch {
            _isBackupLoading.value = true
            try {
                val success = BackupUtils.cloudRestore(getApplication())
                _backupMessage.value = if (success) "Cloud restore completed." else "No cloud backup found."
            } catch (e: Exception) {
                _backupMessage.value = "Cloud restore failed: ${e.message}"
            }
            _isBackupLoading.value = false
        }
    }
}
