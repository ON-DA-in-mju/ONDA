package com.mju.onda.driver.feature.vehicle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.vehicle.data.MockVehicleChange
import com.mju.onda.driver.feature.vehicle.data.VehicleChangeInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VehicleChangeUiState(
    val info: VehicleChangeInfo = MockVehicleChange.info,
    val hasUnreadAlarm: Boolean = MockOperationAlarms.hasUnread(),
)

sealed interface VehicleChangeEvent {
    data object ConfirmAndGoHome : VehicleChangeEvent
    data object NavigateBack : VehicleChangeEvent
    data object OpenAlarms : VehicleChangeEvent
    data object OpenHistory : VehicleChangeEvent
    data object OpenSettings : VehicleChangeEvent
}

class VehicleChangeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleChangeUiState())
    val uiState: StateFlow<VehicleChangeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VehicleChangeEvent>()
    val events: SharedFlow<VehicleChangeEvent> = _events.asSharedFlow()

    fun onConfirm() {
        viewModelScope.launch {
            _events.emit(VehicleChangeEvent.ConfirmAndGoHome)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _events.emit(VehicleChangeEvent.NavigateBack)
        }
    }

    fun onAlarmClick() {
        viewModelScope.launch {
            _events.emit(VehicleChangeEvent.OpenAlarms)
        }
    }

    fun onHistoryClick() {
        viewModelScope.launch {
            _events.emit(VehicleChangeEvent.OpenHistory)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _events.emit(VehicleChangeEvent.OpenSettings)
        }
    }

    fun onTodayClick() {
        viewModelScope.launch {
            _events.emit(VehicleChangeEvent.ConfirmAndGoHome)
        }
    }
}
