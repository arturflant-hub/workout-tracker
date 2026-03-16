package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.BodyMeasurement
import com.workouttracker.data.repository.BodyTrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log10

data class BodyMeasurementUi(
    val measurement: BodyMeasurement,
    val bodyFatNavy: Float?,
    val waistToHeight: Float?
)

@HiltViewModel
class BodyTrackerViewModel @Inject constructor(
    private val repository: BodyTrackerRepository
) : ViewModel() {

    val measurements: StateFlow<List<BodyMeasurementUi>> = repository.getAll()
        .map { list -> list.map { m -> BodyMeasurementUi(m, calcBodyFat(m), calcWaistToHeight(m)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _firstMeasurement = MutableStateFlow<BodyMeasurement?>(null)

    init {
        viewModelScope.launch {
            _firstMeasurement.value = repository.getFirst()
        }
    }

    fun weightChangeFromStart(current: BodyMeasurement): Float? {
        val first = _firstMeasurement.value ?: return null
        return if (current.id == first.id) null else current.weight - first.weight
    }

    fun waistChangeFromStart(current: BodyMeasurement): Float? {
        val first = _firstMeasurement.value ?: return null
        val cw = current.waist ?: return null
        val fw = first.waist ?: return null
        return if (current.id == first.id) null else cw - fw
    }

    fun insert(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.insert(measurement)
            _firstMeasurement.value = repository.getFirst()
        }
    }

    fun delete(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.delete(measurement)
            _firstMeasurement.value = repository.getFirst()
        }
    }

    private fun calcBodyFat(m: BodyMeasurement): Float? {
        val waist = m.waist ?: return null
        val neck = m.neck ?: return null
        val diff = waist - neck
        if (diff <= 0f || m.height <= 0f) return null
        return (495.0 / (1.0324 - 0.19077 * log10(diff.toDouble()) + 0.15456 * log10(m.height.toDouble())) - 450.0).toFloat()
    }

    private fun calcWaistToHeight(m: BodyMeasurement): Float? {
        val waist = m.waist ?: return null
        if (m.height <= 0f) return null
        return waist / m.height
    }
}
