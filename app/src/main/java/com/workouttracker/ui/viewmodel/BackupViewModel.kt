package com.workouttracker.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.workouttracker.data.backup.BackupFileInfo
import com.workouttracker.data.backup.GoogleDriveBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val statusMessage: String? = null,
    val error: String? = null,
    val showNotFoundDialog: Boolean = false,
    val showRestoreConfirmDialog: Boolean = false,
    val showBackupListDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val backupFiles: List<BackupFileInfo> = emptyList(),
    val selectedBackupId: String? = null,
    val selectedBackupName: String? = null,
    val deleteTargetId: String? = null,
    val deleteTargetName: String? = null,
    val needsSignIn: Boolean = false,
    val recoveryIntent: Intent? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: GoogleDriveBackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        checkSignIn()
    }

    fun checkSignIn() {
        val account = backupManager.getSignedInAccount()
        _state.update {
            it.copy(
                isSignedIn = account != null,
                accountEmail = account?.email
            )
        }
    }

    fun getSignInIntent(): Intent = backupManager.getSignInIntent()

    fun onSignInResult(data: Intent?) {
        val account = backupManager.handleSignInResult(data)
        _state.update {
            it.copy(
                isSignedIn = account != null,
                accountEmail = account?.email,
                needsSignIn = false,
                error = if (account == null) "Не удалось войти в Google аккаунт" else null
            )
        }
    }

    fun createBackup() {
        if (!_state.value.isSignedIn) {
            _state.update { it.copy(needsSignIn = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Создание резервной копии...", error = null, statusMessage = null) }
            val result = backupManager.createBackup()
            result.fold(
                onSuccess = { fileName ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Резервная копия создана: $fileName"
                        )
                    }
                },
                onFailure = { e ->
                    handleError(e, "Ошибка при создании резервной копии")
                }
            )
        }
    }

    fun requestRestore() {
        if (!_state.value.isSignedIn) {
            _state.update { it.copy(needsSignIn = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Поиск резервных копий...", error = null, statusMessage = null) }
            val result = backupManager.listBackups()
            result.fold(
                onSuccess = { files ->
                    if (files.isEmpty()) {
                        _state.update { it.copy(isLoading = false, showNotFoundDialog = true) }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                backupFiles = files,
                                showBackupListDialog = true
                            )
                        }
                    }
                },
                onFailure = { e ->
                    handleError(e, "Ошибка при поиске резервных копий")
                }
            )
        }
    }

    fun selectBackupForRestore(backup: BackupFileInfo) {
        _state.update {
            it.copy(
                showBackupListDialog = false,
                selectedBackupId = backup.id,
                selectedBackupName = backup.name,
                showRestoreConfirmDialog = true
            )
        }
    }

    fun confirmRestore() {
        val fileId = _state.value.selectedBackupId ?: return
        _state.update { it.copy(showRestoreConfirmDialog = false) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Восстановление данных...", error = null, statusMessage = null) }
            val result = backupManager.restoreBackup(fileId)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, statusMessage = "Данные восстановлены. Перезапуск...") }
                    backupManager.restartApp()
                },
                onFailure = { e ->
                    handleError(e, "Ошибка при восстановлении")
                }
            )
        }
    }

    fun requestDelete(backup: BackupFileInfo) {
        _state.update {
            it.copy(
                deleteTargetId = backup.id,
                deleteTargetName = backup.name,
                showDeleteConfirmDialog = true
            )
        }
    }

    fun confirmDelete() {
        val fileId = _state.value.deleteTargetId ?: return
        _state.update { it.copy(showDeleteConfirmDialog = false) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Удаление...", error = null) }
            val result = backupManager.deleteBackup(fileId)
            result.fold(
                onSuccess = {
                    val updatedFiles = _state.value.backupFiles.filter { it.id != fileId }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            backupFiles = updatedFiles,
                            statusMessage = "Резервная копия удалена",
                            showBackupListDialog = updatedFiles.isNotEmpty()
                        )
                    }
                },
                onFailure = { e ->
                    handleError(e, "Ошибка при удалении")
                }
            )
        }
    }

    fun dismissDialogs() {
        _state.update {
            it.copy(
                showNotFoundDialog = false,
                showRestoreConfirmDialog = false,
                showBackupListDialog = false,
                showDeleteConfirmDialog = false,
                statusMessage = null,
                error = null,
                recoveryIntent = null,
                needsSignIn = false
            )
        }
    }

    fun switchAccount() {
        backupManager.signOut()
        _state.update {
            it.copy(
                isSignedIn = false,
                accountEmail = null,
                needsSignIn = true
            )
        }
    }

    fun onRecoveryResult(data: Intent?) {
        onSignInResult(data)
        _state.update { it.copy(recoveryIntent = null) }
    }

    private fun handleError(e: Throwable, defaultMessage: String) {
        if (e is UserRecoverableAuthIOException) {
            _state.update { it.copy(isLoading = false, recoveryIntent = e.intent) }
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: defaultMessage
                )
            }
        }
    }
}
