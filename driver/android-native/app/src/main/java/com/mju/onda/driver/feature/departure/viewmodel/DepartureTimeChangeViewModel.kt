package com.mju.onda.driver.feature.departure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.departure.data.DepartureTimeChangeInfo
import com.mju.onda.driver.feature.departure.data.MockDepartureTimeChange
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DepartureTimeChangeUiState(
    val info: DepartureTimeChangeInfo = MockDepartureTimeChange.info,
    val hasUnreadAlarm: Boolean = MockOperationAlarms.hasUnread(),
)

sealed interface DepartureTimeChangeEvent {
    data object ConfirmAndGoHome : DepartureTimeChangeEvent
    data object NavigateBack : DepartureTimeChangeEvent
    data object OpenAlarms : DepartureTimeChangeEvent
    data object OpenHistory : DepartureTimeChangeEvent
    data object OpenSettings : DepartureTimeChangeEvent
}

class DepartureTimeChangeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DepartureTimeChangeUiState())
    val uiState: StateFlow<DepartureTimeChangeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DepartureTimeChangeEvent>()
    val events: SharedFlow<DepartureTimeChangeEvent> = _events.asSharedFlow()

    fun onConfirm() {
        viewModelScope.launch {
            _events.emit(DepartureTimeChangeEvent.ConfirmAndGoHome)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _events.emit(DepartureTimeChangeEvent.NavigateBack)
        }
    }

    fun onAlarmClick() {
        viewModelScope.launch {
            _events.emit(DepartureTimeChangeEvent.OpenAlarms)
        }
    }

    fun onHistoryClick() {
        viewModelScope.launch {
            _events.emit(DepartureTimeChangeEvent.OpenHistory)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _events.emit(DepartureTimeChangeEvent.OpenSettings)
        }
    }

    fun onTodayClick() {
        viewModelScope.launch {
            _events.emit(DepartureTimeChangeEvent.ConfirmAndGoHome)
        }
    }
}
