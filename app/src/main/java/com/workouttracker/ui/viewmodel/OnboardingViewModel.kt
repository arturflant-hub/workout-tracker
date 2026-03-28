package com.workouttracker.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.workouttracker.data.backup.BackupFileInfo
import com.workouttracker.data.backup.GoogleDriveBackupManager
import com.workouttracker.data.db.dao.BodyMeasurementDao
import com.workouttracker.data.db.dao.UserDao
import com.workouttracker.data.db.entities.BodyMeasurement
import com.workouttracker.data.db.entities.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val currentStep: Int = 1,
    val name: String = "",
    val gender: String = "",
    val age: String = "",
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val error: String? = null,
    val needsSignIn: Boolean = false,
    val recoveryIntent: Intent? = null,
    val showBackupListDialog: Boolean = false,
    val showNotFoundDialog: Boolean = false,
    val backupFiles: List<BackupFileInfo> = emptyList(),
    val showRestoreConfirmDialog: Boolean = false,
    val selectedBackupId: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val backupManager: GoogleDriveBackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun selectGender(gender: String) {
        _state.update { it.copy(gender = gender) }
    }

    fun updateAge(age: String) {
        _state.update { it.copy(age = age.filter { c -> c.isDigit() }) }
    }

    fun nextStep() {
        _state.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun prevStep() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val s = _state.value
            val user = User(
                name = s.name.trim(),
                gender = s.gender
            )
            userDao.insertUser(user)

            val ageInt = s.age.toIntOrNull()
            if (ageInt != null) {
                val measurement = BodyMeasurement(
                    date = System.currentTimeMillis(),
                    weight = 0f,
                    height = 0f,
                    age = ageInt,
                    chest = null,
                    waist = null,
                    hips = null,
                    thigh = null,
                    arm = null,
                    neck = null
                )
                bodyMeasurementDao.insert(measurement)
            }

            _state.update { it.copy(isComplete = true) }
        }
    }

    // Restore from backup
    fun getSignInIntent(): Intent = backupManager.getSignInIntent()

    fun onSignInResult(data: Intent?) {
        val account = backupManager.handleSignInResult(data)
        if (account != null) {
            _state.update { it.copy(needsSignIn = false) }
            searchBackups()
        } else {
            _state.update { it.copy(needsSignIn = false, error = "Не удалось войти в Google аккаунт") }
        }
    }

    fun requestRestore() {
        val account = backupManager.getSignedInAccount()
        if (account == null) {
            _state.update { it.copy(needsSignIn = true) }
            return
        }
        searchBackups()
    }

    private fun searchBackups() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Поиск резервных копий...", error = null) }
            val result = backupManager.listBackups()
            result.fold(
                onSuccess = { files ->
                    if (files.isEmpty()) {
                        _state.update { it.copy(isLoading = false, showNotFoundDialog = true) }
                    } else {
                        _state.update { it.copy(isLoading = false, backupFiles = files, showBackupListDialog = true) }
                    }
                },
                onFailure = { e ->
                    if (e is UserRecoverableAuthIOException) {
                        _state.update { it.copy(isLoading = false, recoveryIntent = e.intent) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = e.message ?: "Ошибка поиска") }
                    }
                }
            )
        }
    }

    fun selectBackupForRestore(backup: BackupFileInfo) {
        _state.update { it.copy(showBackupListDialog = false, selectedBackupId = backup.id, showRestoreConfirmDialog = true) }
    }

    fun confirmRestore() {
        val fileId = _state.value.selectedBackupId ?: return
        _state.update { it.copy(showRestoreConfirmDialog = false) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingMessage = "Восстановление данных...") }
            val result = backupManager.restoreBackup(fileId)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    backupManager.restartApp()
                },
                onFailure = { e ->
                    if (e is UserRecoverableAuthIOException) {
                        _state.update { it.copy(isLoading = false, recoveryIntent = e.intent) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = e.message ?: "Ошибка восстановления") }
                    }
                }
            )
        }
    }

    fun onRecoveryResult(data: Intent?) {
        onSignInResult(data)
        _state.update { it.copy(recoveryIntent = null) }
    }

    fun dismissDialogs() {
        _state.update {
            it.copy(
                showNotFoundDialog = false,
                showBackupListDialog = false,
                showRestoreConfirmDialog = false,
                error = null,
                needsSignIn = false,
                recoveryIntent = null
            )
        }
    }
}
