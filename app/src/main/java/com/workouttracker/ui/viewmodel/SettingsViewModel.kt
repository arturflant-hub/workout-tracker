package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.dao.BodyMeasurementDao
import com.workouttracker.data.db.dao.UserDao
import com.workouttracker.data.db.entities.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val user: User? = null,
    val age: Int? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bodyMeasurementDao: BodyMeasurementDao
) : ViewModel() {

    private val _profile = MutableStateFlow(ProfileState())
    val profile: StateFlow<ProfileState> = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            userDao.getUser().collect { user ->
                val latest = bodyMeasurementDao.getLatest()
                _profile.update { it.copy(user = user, age = latest?.age) }
            }
        }
    }

    fun saveProfile(name: String, gender: String, age: Int?) {
        viewModelScope.launch {
            val current = _profile.value.user ?: return@launch
            userDao.updateUser(current.copy(name = name, gender = gender))

            if (age != null) {
                val latest = bodyMeasurementDao.getLatest()
                if (latest != null) {
                    bodyMeasurementDao.update(latest.copy(age = age))
                }
            }
        }
    }
}
